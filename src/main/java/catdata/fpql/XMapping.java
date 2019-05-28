package catdata.fpql;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import com.google.common.base.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.fpql.XExp.XMapConst;
import catdata.fpql.XExp.XTransConst;
import catdata.fpql.XPoly.Block;
import catdata.ide.CodeTextPanel;
import catdata.ide.DefunctGlobalOptions;
import catdata.ide.GuiUtil;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;

public class XMapping<C, D> implements XObject {
	public Map<Pair<List<C>, List<C>>, String> unprovable = new HashMap<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping(XCtx C, String kind) {
		this.kind = kind;
		src = C;
		dst = C;
		em = new HashMap();
		for (Object c : C.allTerms()) {
			List l = new LinkedList();
			l.add(c);
			em.put((C) c, l);
		}
		validate();
	}

	public <X> XMapping(XMapping<C, X> F, XMapping<X, D> G) {
		if (!F.kind().equals(G.kind())) {
			throw new RuntimeException("Mismatch on " + F + " and " + G);
		}
		if (!F.dst.equals(G.src)) {
			throw new RuntimeException("Dom/Cod mismatch on " + F + " and " + G);
		}
		kind = G.kind;
		src = F.src;
		dst = G.dst;
		em = new HashMap<>();
		for (C c : src.allTerms()) {
			List<X> l1 = F.em.get(c);
			List<D> l2 = l1.stream().flatMap(x -> G.em.get(x).stream())
					.collect(Collectors.toList());
			em.put(c, l2);
		}
		validate();
	}

	@Override
	public String kind() {
		return kind;
	}

	XCtx<C> src;
	XCtx<D> dst;
	Map<C, List<D>> em;
	private String kind = "TODO";

	public XMapping(XCtx<C> src, XCtx<D> dst, Map<C, List<D>> em, String kind) {
		this.src = src;
		this.dst = dst;
		this.em = em;
		this.kind = kind;
		validate();
	}

	// : make sure this is the identity for global and schema
    private void validate() {
		unprovable = new HashMap<>();
		for (C c : em.keySet()) {
			if (!src.allTerms().contains(c)) {
				throw new RuntimeException("Extraneous: " + c);
			}
		}
		for (C c : src.allTerms()) {
			if (!em.containsKey(c)) {
				throw new RuntimeException("Missing: " + c);
			}
			List<D> x = em.get(c);
			dst.type(x);
		}

		for (C c : src.terms()) {
			Pair<C, C> src_t = src.type(c); // c : t1 -> t2
			Pair<D, D> dst_t = dst.type(em.get(c)); // Fc : tx -> ty
			Pair<List<D>, List<D>> t = new Pair<>(em.get(src_t.first), em.get(src_t.second));
			if (t.first.size() != 1 || t.second.size() != 1) {
				throw new RuntimeException();
			}
			D t1 = t.first.get(0);
			D t2 = t.second.get(0);
			if (!dst_t.first.equals(t1) || !dst_t.second.equals(t2)) {
				throw new RuntimeException("On " + c + ", source type is " + src_t + " mapsto "
						+ em.get(c) + " whose type is " + dst_t + ", which is not " + t
						+ ", as expected.");
			}
		}

		for (Pair<List<C>, List<C>> eq : src.allEqs()) {
			List<D> lhs = apply(eq.first);
			List<D> rhs = apply(eq.second);
			if (!dst.type(lhs).equals(dst.type(rhs))) {
				throw new RuntimeException();
			}
			try {
				boolean b = dst.getKB().equiv(lhs, rhs);
				if (!b) {
					throw new RuntimeException("cannot prove " + eq);
				}
				unprovable.put(eq, "true");
			} catch (Exception ex) {
				throw new RuntimeException("cannot prove " + eq + " in " + dst);
			}
		}
	}

	public List<D> apply(List<C> p) {
		List<D> ret = p.stream().flatMap(x -> {
			List<D> xxx = em.get(x);
			if (xxx == null) {
				throw new RuntimeException("Does not map " + x);
			}
			return xxx.stream();
		}).collect(Collectors.toList());
		dst.type(ret);
		return ret;
	}

	// is identity on non-mapped elements
	@SuppressWarnings({ "unchecked" })
    List<D> applyAlsoId(List<C> p) {
		List<D> ret = p.stream().flatMap(x -> {
			List<D> r = em.get(x);
			if (r == null) {
				r = new LinkedList<>();
				D ddd = (D) x;
				r.add(ddd);
			}
			return r.stream();
		}).collect(Collectors.toList());
		// dst.type(ret); can't do type here
		return ret;
	}

	 @Override
	public JComponent display() {
		JTabbedPane pane = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fpql.x_text) {
			String ret = toString();
			pane.addTab("Text", new CodeTextPanel(BorderFactory.createEtchedBorder(), "", ret));
		}

		if (src.schema != null) {
			if (DefunctGlobalOptions.debug.fpql.x_tables) {
				pane.addTab("Tables", makeTables());
			}
		} else {
			if (DefunctGlobalOptions.debug.fpql.x_tables) {
				pane.addTab("Tables", makeTables2());
			}
		}
		
		if (DefunctGlobalOptions.debug.fpql.x_graph && src.schema == null) {
			pane.addTab("Graph", makeGraph());
		}

		return pane;
	}

	private Component makeTables2() {
		try {
			List<Object[]> rowData = new LinkedList<>(); // Object[src.cat().arrows().size()][2];
			Object[] colNames = new Object[] { "src", "dst" };

			// int i = 0;
			for (Triple<C, C, List<C>> k : src.cat().arrows()) {
				List<C> a = new LinkedList<>(k.third);
				a.add(0, k.first);
				List<D> applied = apply(a);
				Pair<D, D> t = dst.type(applied);
				if (a.equals(applied)) {
					continue;
				}
				for (Triple<D, D, List<D>> cand : dst.cat().hom(t.first, t.second)) {
					if (dst.getKB().equiv(cand.third, applied)) {
						List<C> z = new LinkedList<>(k.third);
						z.add(0, k.first);
						Object[] row = new Object[2];
						row[0] = Util.sep(z, ".");
						List<D> y = new LinkedList<>(cand.third);
						y.add(0, cand.first);
						row[1] = Util.sep(y, ".");
						rowData.add(row);
					}
				}
				// i++;
			}

			return GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "",
					rowData.toArray(new Object[0][0]), colNames);
		} catch (Exception e) {
			e.printStackTrace();
			return new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "ERROR:\n\n"
					+ e.getMessage());
		}

	}

	@SuppressWarnings("unchecked")
	private Component makeTables() {
		try {
			List<JComponent> grid = new LinkedList<>();
			// Map<C, Map<List<C>, List<D>>> m = new HashMap<>();

			for (C id : src.schema.ids) {
				Map<List<C>, List<D>> map = new HashMap<>();
				for (Triple<C, C, List<C>> arr : src.cat().hom((C) "_1", id)) { // arrows())
																				// {
					List<C> toApply = new LinkedList<>(arr.third);
					toApply.add(0, arr.first);
					List<D> applied = apply(toApply);
					for (Triple<D, D, List<D>> cand : dst.cat().hom((D) "_1", (D) id)) {
						if (dst.getKB().equiv(cand.third, applied)) {
							map.put(arr.third, cand.third);
						}
					}
				}
				Object[][] rowData = new Object[map.size()][2];
				Object[] colNames = new Object[] { "src", "dst" };
				int i = 0;
				for (Entry<List<C>, List<D>> k : map.entrySet()) {
					rowData[i][0] = Util.sep(k.getKey(), ".");
					rowData[i][1] = Util.sep(k.getValue(), ".");
					i++;
				}
				JPanel tbl = GuiUtil.makeTable(BorderFactory.createEtchedBorder(),
						id + " (" + map.size() + " rows)", rowData, colNames);
				grid.add(tbl);
			}

			return GuiUtil.makeGrid(grid);
		} catch (Exception e) {
			return new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "ERROR:\n\n"
					+ e.getMessage());
		}
	}

	private String toString = null;

	@Override
	public String toString() {
		if (toString != null) {
			return toString;
		}
		String ret = Util.sep(
				src.allTerms().stream().map(x -> x + " -> " + Util.sep(em.get(x), "."))
						.collect(Collectors.toList()), "\n");
		ret += "\n";
		for (Pair<List<C>, List<C>> eq : src.allEqs()) {
			List<D> lhs = apply(eq.first);
			List<D> rhs = apply(eq.second);
			ret += "\nEquation: " + Util.sep(eq.first, ".") + " = " + Util.sep(eq.second, ".");
			ret += "\nTransformed: " + Util.sep(lhs, ".") + " = " + Util.sep(rhs, ".");
			ret += "\nProvable: " + unprovable.get(eq);
			ret += "\n";
		}

		ret = ret.trim();
		toString = ret;
		return ret;
	}

	/**
	 * Sigma
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public XCtx<D> apply0(XCtx<C> I) {
		// XCtx<D> ret = dst.copy();
		// ret.local = new HashSet<>(); //(Set<D>) I.local;
		Map<D, Pair<D, D>> ret = new HashMap<>();
		Set<Pair<List<D>, List<D>>> eqs = new HashSet<>();
		for (C c : I.terms()) {
			// ret.consts.add((D)c);
			// if (I.local.contains(c)) {
			// ret.local.add((D)c);
			// }
			Pair<C, C> t = I.type(c);
			List<D> lhs = em.get(t.first);
			if (lhs == null) {
				throw new RuntimeException("No edge mapping for " + t.first + " in " + em);
			}
			List<D> rhs = em.get(t.second);
			if (rhs == null) {
				throw new RuntimeException("No edge mapping for " + t.second + " in " + em);
			}
			if (lhs.size() != 1 || rhs.size() != 1) {
				throw new RuntimeException();
			}
			ret.put((D) c, new Pair<>(lhs.get(0), rhs.get(0)));
		}

		for (Pair<List<C>, List<C>> eq : I.eqs) {
			if (src.eqs.contains(eq)) {
				continue;
			}
			List<D> lhs = applyAlsoId(eq.first);
			List<D> rhs = applyAlsoId(eq.second);
			eqs.add(new Pair<>(lhs, rhs));
		}

		return new XCtx(new HashSet<>(), ret, eqs, I.global, dst, "instance");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static XMapping<String, String> make(@SuppressWarnings("unused") XEnvironment eNV, XCtx<String> src,
			XCtx<String> dst, XMapConst m) {
		Map<String, List<String>> ret = new HashMap<>();
		List<String> one = new LinkedList<>();
		one.add("_1");
		ret.put("_1", one);
		List<String> ggg = new LinkedList<>();
		ggg.add("!__1");
		ret.put("!__1", ggg);

		for (Pair<String, String> k : m.nm) {
			if (!src.ids.contains(k.first)) {
				throw new RuntimeException("Source does not contain node " + k.first);
			}
			if (!dst.ids.contains(k.second)) {
				throw new RuntimeException("Target does not contain node " + k.second);
			}
			if (ret.containsKey(k.first)) {
				throw new RuntimeException("Duplicate node mapping for " + k.first);
			}
			if (!src.terms().contains(k.first)) {
				throw new RuntimeException("Not-local: " + k.first);
			}
			List<String> l = new LinkedList<>();
			l.add(k.second);
			ret.put(k.first, l);
		}
		for (String l : src.allIds()) {
			// if (l.equals("_1")) {
			// continue;
			// }
			List h = new LinkedList();
			if (ret.get(l) == null) {
				h.add("!_" + l);
			} else {
				h.add("!_" + ret.get(l).get(0));
			}
			ret.put("!_" + l, h);
			// }

			if (!src.terms().contains(l)) {
				continue;
			}
			if (!ret.keySet().contains(l)) {
				throw new RuntimeException("No mapping for node " + l);
			}
			// em.put("1_" + nm.get(l));

		}

		for (Pair<String, List<String>> k : m.em) {
			if (!src.terms().contains(k.first)) {
				throw new RuntimeException("Source does not contain edge " + k.first);
			}
			if (ret.containsKey(k.first)) {
				throw new RuntimeException("Duplicate node mapping for " + k.first);
			}
			if (src.ids.contains(k.first)) {
				throw new RuntimeException("Cannot re-map node " + k.first);
			}
			ret.put(k.first, k.second);
		}
		for (String l : src.terms()) {
			if (!ret.keySet().contains(l)) {
				throw new RuntimeException("No mapping for edge " + l);
			}
		}
		for (String l : src.global.terms()) {
			if (l.startsWith("!") || l.startsWith("_1")) {
				continue;
			}
			List ls = new LinkedList<>();
			ls.add(l);
			ret.put(l, ls);
		}

		return new XMapping<>(src, dst, ret, "mapping");
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "unlikely-arg-type" })
	public static XMapping<String, String> make(XCtx<String> src, XCtx<String> dst, XTransConst e) {
		if (src.schema == null) {
			throw new RuntimeException("source is not an instance");
		}
		if (dst.schema == null) {
			throw new RuntimeException("target is not an instance");
		}
		if (!src.schema.equals(dst.schema)) {
			throw new RuntimeException("not on same schema");
		}

		Map ret = new HashMap<>(); 
		for (String c : src.schema.allTerms()) {
			List<String> l = new LinkedList<>();
			l.add(c);
			ret.put(c, l);
		}

		// will mutate in place
		for (Pair<Pair<String, String>, List<String>> k : e.vm) {
			boolean leftok = false;
			boolean rightok = false;
			Pair lhs = new Pair<>(k.first.first, k.first.second);
			if (k.first.second == null) {
				Set cands = new HashSet();
				for (Object o : src.terms()) {
					if (((Pair) o).first.equals(lhs.first)) {
						cands.add(o);
					}
				}
				if (cands.size() == 1) {
					lhs = (Pair) new LinkedList<>(cands).get(0);
					leftok = true;
				}
			}
			List rhs = new LinkedList<>(k.second);
			Set sofar = new HashSet();
			//List l = new LinkedList();
			sofar.add(new LinkedList<>());
			Set<List> rhs0 = XCtx.expand(sofar, rhs, dst.schema, dst);
			if (rhs0.size() == 1) {
				rhs = (List) new LinkedList(rhs0).get(0);
				rightok = true;
			}
			// v:t -> u:?
			if (!rightok && leftok && rhs.size() == 1 && dst.terms().contains(rhs.get(0))) {
				//List rhsX = new LinkedList<>();
				//rhsX.add(new Pair<>(((Pair) rhs.get(0)).first, lhs.second));
				rightok = true;
			}
			// v:? -> p:t
			if (!leftok && rightok) {
				Pair p = dst.type(rhs);
				lhs = new Pair<>(lhs.first, p.second);
//				lhs.second = p.second;  aug 2018 changed for immutable pairs
				//leftok = true;
			}

			if (!src.terms().contains(lhs)) {
				throw new RuntimeException("Source does not contain variable " + k.first);
			}
			if (src.schema.allTerms().contains(lhs)) {
				throw new RuntimeException("Not a variable: " + k.first);
			}
			if (ret.containsKey(lhs)) {
				throw new RuntimeException("Duplicate node mapping for " + k.first);
			}
			ret.put(lhs, rhs);
		}
		for (Object c : src.allTerms()) {
			if (!ret.containsKey(c)) {
				throw new RuntimeException("Does not map " + c);
			}
		}
		return new XMapping<>(src, dst, ret, "homomorphism");
	}

	// on transforms
	public XObject apply(XMapping<C, C> t) {
		XCtx<D> src0 = apply0(t.src);
		XCtx<D> dst0 = apply0(t.dst);

		Map<D, List<D>> ret = new HashMap<>();
		for (D c : src0.allTerms()) {
			// if (src0.parent.consts.contains(c)) {
			// continue;
			// }
			List<C> p = new LinkedList<>();
			@SuppressWarnings("unchecked")
			C ccc = (C) c;
			p.add(ccc);
			List<C> k = t.applyAlsoId(p);
			ret.put(c, applyAlsoId(k));
		}

		// this = f
		// t : I => J
		// t': sigma_F I => sigma_F J
		// return new XMapping()
		return new XMapping<>(src0, dst0, ret, "homomorphism");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping<Pair<Triple<D, D, List<D>>, C>, Pair<Triple<D, D, List<D>>, C>> deltaT(
			XMapping<D, D> t) {
		XCtx<Pair<Triple<D, D, List<D>>, C>> dsrc = delta(t.src);
		XCtx<Pair<Triple<D, D, List<D>>, C>> ddst = delta(t.dst);

		Map m = new HashMap();

		for (Pair<Triple<D, D, List<D>>, C> k0 : dsrc.terms()) {
			// boolean found = false;
			Pair<Triple<D, D, List<D>>, C> k = new Pair<>(new Triple<>(k0.first.first,
					k0.first.second, t.applyAlsoId(k0.first.third)), k0.second);
			Triple<D, D, List<D>> v = t.dst.find_fast(k.first);
			if (v == null) {
				throw new RuntimeException();
			}

			if (dst.global.ids.contains(v.second)
					&& dst.global.cat().hom((D) "_1", v.second).contains(v)) {
				List l = new LinkedList<>();
				l.add(v.first);
				l.addAll(v.third);
				m.put(k0, l);
			} else {
				List<Pair<Triple<D, D, List<D>>, C>> l = new LinkedList<>();
				l.add(new Pair<>(v, k.second));
				m.put(k0, l);
			}

		}

		for (Object o : dsrc.allTerms()) {
			if (m.containsKey(o)) {
				continue;
			}
			List l = new LinkedList();
			l.add(o);
			m.put(o, l);
		}

		return new XMapping<>(dsrc, ddst, m, "homomorphism");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping<C, Pair<Triple<D, D, List<D>>, C>> unit(XCtx<C> I) {
		XCtx<D> FI = apply0(I);
		XCtx<Pair<Triple<D, D, List<D>>, C>> FFI = delta(FI);
		Map m = new HashMap<>();

		for (C c : I.terms()) {
			List<D> Fc;
			Fc = new LinkedList<>();
			Fc.add((D) c);
			Pair<D, D> t = FI.type(Fc);
			boolean found = false;
			for (Triple<D, D, List<D>> v : FI.cat().hom(t.first, t.second)) {
				/*
				 * if (!v.first.equals(t.first)) { continue; } if
				 * (!v.second.equals(t.second)) { continue; }
				 */
				if (FI.getKB().equiv(v.third, Fc)) {
					List l = new LinkedList();
					l.add(new Pair<>(v, I.type(c).second));
					m.put(c, l);
					found = true;
					break;
				}
			}
			if (!found) {
				throw new RuntimeException("No equiv for " + Fc + " in " + FI);
			}
		}

		for (Object o : I.allTerms()) {
			if (m.containsKey(o)) {
				continue;
			}
			List l = new LinkedList();
			l.add(o);
			m.put(o, l);
		}

		return new XMapping<>(I, FFI, m, "homomorphism");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping<Pair<Triple<D, D, List<D>>, C>, D> counit(XCtx<D> I) {
		XCtx<Pair<Triple<D, D, List<D>>, C>> FI = delta(I);
		XMapping<Pair<Triple<D, D, List<D>>, C>, Pair<Triple<D, D, List<D>>, C>> f = (XMapping<Pair<Triple<D, D, List<D>>, C>, Pair<Triple<D, D, List<D>>, C>>) this;
		XCtx<Pair<Triple<D, D, List<D>>, C>> FFI = f.apply0(FI);

		Map m = new HashMap<>();

		for (Pair<Triple<D, D, List<D>>, C> c : FFI.terms()) {
			List<D> l = new LinkedList<>(c.first.third);
			l.add(0, c.first.first);
			m.put(c, l);
		}

		for (Object o : FFI.allTerms()) {
			if (m.containsKey(o)) {
				continue;
			}
			List l = new LinkedList();
			l.add(o);
			m.put(o, l);
		}

		return new XMapping<>(FFI, I, m, "homomorphism");
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked", "unlikely-arg-type" })
	public XCtx<Pair<Triple<D, D, List<D>>, C>> delta(XCtx<D> I) {
		Set<Pair<Triple<D, D, List<D>>, C>> ids = new HashSet<>();
		Map<Pair<Triple<D, D, List<D>>, C>, Pair<Pair<Triple<D, D, List<D>>, C>, Pair<Triple<D, D, List<D>>, C>>> types = new HashMap<>();
		Set<Pair<List<Pair<Triple<D, D, List<D>>, C>>, List<Pair<Triple<D, D, List<D>>, C>>>> eqs = new HashSet<>();

		for (C c : src.allIds()) {
			for (Triple<D, D, List<D>> arr : I.cat().hom((D) "_1", em.get(c).get(0))) {
				if (I.global.cat().objects().contains(c)) {
					if (I.global.cat().hom((D) "_1", (D) c).contains(arr)) {
						continue;
					}
				}
				Pair tr = new Pair(arr, c);
				types.put(tr, new Pair("_1", c));
			}
		}

		for (Pair<Triple<D, D, List<D>>, C> t1 : types.keySet()) {
			for (C c : src.allTerms()) { // f
				if (src.ids.contains(c)) {
					continue;
				}

				Pair<C, C> t = src.type(c);
				D dsrc = t1.first.first;
				D ddst = em.get(t.second).get(0);

				if (t1.second.equals(t.first)) {
					List lhs = new LinkedList<>();
					lhs.add(t1);
					lhs.add(c);

					List j = new LinkedList<>(t1.first.third);
					j.addAll(em.get(c));
					Triple rhs = new Triple(dsrc, ddst, j);
					// Util.sep(I.cat().hom(dsrc, ddst), "\n\n"));
					Triple rhsX = I.find_fast(rhs);
					List g = new LinkedList<>();
					g.add(new Pair<>(rhsX, t.second));

					if (I.global.allIds().contains(t.second)) { // a' in G
						Triple ooo = I.global.find_fast(rhsX);
						if (ooo != null) {
							List lll = new LinkedList();
							if (((Collection) ooo.third).isEmpty()) {
								lll.add(ooo.first);
							}
							lll.addAll((Collection) ooo.third);

							eqs.add(new Pair(lhs, lll));
						} else {
							eqs.add(new Pair<>(lhs, g));
						}
					} else {
						eqs.add(new Pair<>(lhs, g));
					}
				}
			}
		}

		XCtx ret = new XCtx<>(ids, types, eqs,
				(XCtx<Pair<Triple<D, D, List<D>>, C>>) src.global,
				(XCtx<Pair<Triple<D, D, List<D>>, C>>) src, "instance");
		ret.saturated = true; // I.saturated;
		return ret;
	}



	@SuppressWarnings({ "rawtypes" })
	private Triple<C, C, List<C>> getWrapper(@SuppressWarnings("unused") XCtx I,
			Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta,
			Pair<C, Triple<D, D, List<D>>> key) {
		//Triple<D, D, List<D>> f = key.second;
		//C c = key.first;

		Triple<C, C, List<C>> ret = theta.get(key);
		if (ret != null) {
			return ret;
		}

		//if (!isSkipKey(c, f)) {
			return null;
		/*}

		if (f.first.equals("_1")) {
			return (Triple) f;
		}

		if (!f.third.get(0).equals("!_" + f.first)) {
			throw new RuntimeException();
		}
		Triple newfound = new Triple("_1", f.second, f.third.subList(1, f.third.size()));
		if (!I.cat().arrows().contains(newfound)) {
			throw new RuntimeException();
		}
		return newfound;*/
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XCtx<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> pi(XCtx<C> I) {
	
		
		Pair<Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>>, Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>>> zzz = makeThetas2(I);
		Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>> thetas_d = zzz.first;
		Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>> bad_thetas = zzz.second;
		
		Map types = new HashMap<>();
		for (D d : dst.allIds()) {
			for (Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta : thetas_d.get(d)) {
					types.put(theta, new Pair<>("_1", d));
				}
		}

		Set<Pair<List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>>> eqs = new HashSet<>();
		for (D h : dst.allTerms()) {
			Pair<D, D> t = dst.type(h);
			//what if t.first is a type? don't have a theta
			//what if t.second is a type? have a theta but won't build a theta
			for (Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta : thetas_d
					.get(t.first)) {
				Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta0 = new HashMap<>();

				for (C c : src.allIds()) {
					for (Triple<D, D, List<D>> f : dst.cat().hom(t.second, em.get(c).get(0))) {
						List<D> f0 = new LinkedList<>();
						f0.add(h);
						f0.addAll(f.third);
						Triple<D, D, List<D>> key = dst.find_fast(new Triple<>(t.first, f.second,
								f0));
						if (key == null) {
							throw new RuntimeException("Cannot find null key in "
									+ dst.cat().arrows());
						}
						Triple<C, C, List<C>> toStore = getWrapper(I, theta, new Pair<>(c, key));
						if (toStore == null) {
							throw new RuntimeException("Cannot find " + new Pair<>(c, key) + " in "
									+ theta);
						}
						theta0.put(new Pair<>(c, f), toStore);
					}
				}

				List lhs = new LinkedList<>();
				lhs.add(theta);
				lhs.add(h);

				if (I.global.allIds().contains((C)t.second)) {
					List rhs = new LinkedList<>();
					rhs.add(theta0);

					if (bad_thetas.get(t.second).contains(theta0)) {
						Object o1 = t.second;
						Object o2 = new Triple<>(t.second, t.second, new LinkedList<>());
						Pair key = new Pair(o1, o2);
						Triple<C, C, List<C>> val = getWrapper(I, theta0, key); // theta0.get(key);
						if (val == null) {
							throw new RuntimeException();
						}
						List lll = new LinkedList();
						if (val.third.isEmpty()) {
							lll.add(val.first);
						} else {
							lll.addAll((val.third)); 
						}
						eqs.add(new Pair(lhs, lll));
					} else {
						eqs.add(new Pair<>(lhs, rhs));
					}
				} else {
					boolean found = false;
					for (Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> thetaX : thetas_d
							.get(t.second)) {
						if (theta0.equals(thetaX)) {
							found = true;
							break;
						}
					}
					if (!found) {
						throw new RuntimeException("At h=" + h + ": " + t
								+ ", Constructed theta^prime " + theta0 + " not found in\n\n"
								+ Util.sep(thetas_d.get(t.second), "\n"));
					}

					List rhs = new LinkedList<>();
					rhs.add(theta0);
					eqs.add(new Pair<>(lhs, rhs));
				}
			}
		}
		XCtx ret = new XCtx(new HashSet<>(), types, eqs, src.global, dst, "instance");
		ret.saturated = true; 
		return ret;
	}

	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>, Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> piT(
			XMapping<C, C> t) {
		
		XCtx<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> src = pi(t.src);
		XCtx<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> dst = pi(t.dst);

		Map em = new HashMap<>();

		for (Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta : src.terms()) {
			Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta2 = new HashMap<>();
			
			for (Pair<C, Triple<D, D, List<D>>> cf : theta.keySet()) {
				Triple<C, C, List<C>> i = theta.get(cf);
				List<C> i2 = new LinkedList<>(i.third);
				i2.add(0, i.first);
				List<C> i2ap = t.apply(i2);
				Triple<C, C, List<C>> i2ap0 = new Triple<>(i.first, i.second, i2ap);
				Triple<C, C, List<C>> found = t.dst.find_fast(i2ap0);
				if (found == null) {
					throw new RuntimeException("not found " + i2ap0 + " in "
							+ t.src.cat().hom(i.first, i.second));
				}
				theta2.put(cf, found);
			}
			List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> l = new LinkedList<>();
			if (dst.terms().contains(theta2)) {
				l.add(theta2);
				em.put(theta, l);
			}
		}

		for (Object o : src.allTerms()) {
			if (em.containsKey(o)) {
				continue;
			}
			List l = new LinkedList();
			l.add(o);
			em.put(o, l);
		}

		return new XMapping<>(src, dst, em, "homomorphism");
	}

	

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean fill(XCtx<C> I, Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta,
                         Map<Pair<C, Triple<D, D, List<D>>>, Pair<C, Triple<D, D, List<D>>>> theta2,
                         Pair<C, Triple<D, D, List<D>>> tag, Triple<C, C, List<C>> y) {

		C c = tag.first;
		Triple<D, D, List<D>> f = tag.second;
		for (C c0 : src.allIds()) {
			for (Triple<C, C, List<C>> g : src.cat().hom(c, c0)) {
				List g0 = new LinkedList<>(g.third);
				g0.add(0, g.first);
				List<D> fFg = apply(g0);
				fFg.addAll(0, f.third);
				fFg.add(0, f.first);
				D t = em.get(c0).get(0);
				Triple<D, D, List<D>> found = dst.find_fast(new Triple<>(f.first, t, fFg)); // morally
																							// fFg
				if (found == null) {
					throw new RuntimeException();
				}

				// Triple<C, C, List<C>> val = theta.get(new Pair<>(c0, found));
				Triple<C, C, List<C>> val = getWrapper(I, theta, new Pair<>(c0, found));
				List<C> yIg = new LinkedList<>(y.third);
				yIg.add(0, y.first);
				yIg.addAll(g.third);
				Triple<C, C, List<C>> found2 = I.find_fast(new Triple<>((C) "_1", g.second, yIg)); // morally
																									// yIg
				if (found2 == null) {
					throw new RuntimeException();
				}

				if (val == null) {
					theta.put(new Pair<>(c0, found), found2);
					theta2.put(new Pair<>(c0, found), tag);
				} else if (!I.getKB().equiv(val.third, found2.third)) {
					return false;
				}
			}
		}
		return true;
	}

	// 
    private void cleanup(Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta,
                         Map<Pair<C, Triple<D, D, List<D>>>, Pair<C, Triple<D, D, List<D>>>> theta2,
                         Pair<C, Triple<D, D, List<D>>> tag, @SuppressWarnings("unused") Triple<C, C, List<C>> y) {
		for (Pair<C, Triple<D, D, List<D>>> k : theta.keySet()) {
			Pair<C, Triple<D, D, List<D>>> v = theta2.get(k);
			if (v == null) {
				continue;
			}
			if (v.equals(tag) /* && theta.get(k).equals(y) */) {
				theta.put(k, null);
				theta2.put(k, null);
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
    private void try_branch(XCtx<C> I,
                            List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> thetas,
                            Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta,
                            Map<Pair<C, Triple<D, D, List<D>>>, Pair<C, Triple<D, D, List<D>>>> theta2,
                            Pair<C, Triple<D, D, List<D>>> tag, Triple<C, C, List<C>> y,
                            List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> bad_thetas) {
		C c = tag.first;
		Triple<D, D, List<D>> f = tag.second;
		if (!theta.keySet().contains(new Pair<>(c, f))) {
			throw new RuntimeException();
		}
		theta.put(new Pair<>(c, f), y);
		theta2.put(new Pair<>(c, f), new Pair<>(c, f));
		boolean contra = fill(I, theta, theta2, tag, y);
		if (contra) {
			Pair<C, Triple<D, D, List<D>>> c0f0 = next(theta, tag);
			if (c0f0 != null) {
				C c0 = c0f0.first;
				//Triple<D, D, List<D>> f0 = c0f0.second;
				for (Triple<C, C, List<C>> x0 : I.cat().hom((C) "_1", c0)) {
					try_branch(I, thetas, theta, theta2, c0f0, x0, bad_thetas);
				}
			} else {
				for (Pair<C, Triple<D, D, List<D>>> k : theta.keySet()) {
					if (theta.get(k) == null) {
						throw new RuntimeException("Tried to add " + theta);
					}
				}
				D d = f.first;
				if (I.global.allIds().contains((C)d)) {
					if (!bad_thetas.contains(theta)) {
						thetas.add(new LinkedHashMap<>(theta)); // ok,
																// irrelevent
					} 
				} else {
					thetas.add(new LinkedHashMap<>(theta)); // ok, irrelevent
				}
			}
		}
		cleanup(theta, theta2, tag, y);
	}

	private Pair<C, Triple<D, D, List<D>>> next(
			Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta,
			Pair<C, Triple<D, D, List<D>>> tag) {
		boolean seen = false;
		for (Pair<C, Triple<D, D, List<D>>> k : theta.keySet()) {
			if (k.equals(tag)) {
				seen = true;
			}
			if (seen) {
				if (theta.get(k) == null) {
					return k;
				}
			}
		}
		if (!seen) {
			throw new RuntimeException(tag + " not seen in " + theta);
		}
		return null;
	}

	// /////////////////////////////////////

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Pair<Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>>, Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>>> makeThetas2(
			XCtx<C> I) {
		Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>> ret = new HashMap<>();
		Map<D, List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>>> ret2 = new HashMap<>();
		
		for (D d : dst.allIds()) { //was allIds
			List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> bad_thetas = new LinkedList<>();
			if (dst.global.allIds().contains(d)) {
				for (Triple<D, D, List<D>> constant : dst.global.cat().hom((D) "_1", d)) {
					Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> bad_theta = new LinkedHashMap<>();
					for (C c : src.allIds()) {
						for (Triple<D, D, List<D>> f : dst.cat().hom(d, em.get(c).get(0))) {
							Triple composed = dst.global.cat().compose(constant, f);
							bad_theta.put(new Pair<>(c, f), composed);
						}
					}
					bad_thetas.add(bad_theta);
				}
			}
			ret2.put(d, bad_thetas);
	
			List<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> thetas = new LinkedList<>();
			Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>> theta = new LinkedHashMap<>();
			Map<Pair<C, Triple<D, D, List<D>>>, Pair<C, Triple<D, D, List<D>>>> theta2 = new LinkedHashMap<>();
			for (C c : src.allIds()) {
				for (Triple<D, D, List<D>> f : dst.cat().hom(d, em.get(c).get(0))) {
					theta.put(new Pair<>(c, f), null);
					theta2.put(new Pair<>(c, f), null);
				}
			}

			if (theta.keySet().isEmpty()) {
				thetas.add(new HashMap<>());
			} else {
				for (Pair<C, Triple<D, D, List<D>>> cf : theta.keySet()) {
					for (Triple<C, C, List<C>> x : I.cat().hom((C) "_1", cf.first)) {
						try_branch(I, thetas, theta, theta2, cf, x, bad_thetas);
					}
					break; // this is fine - just do first key
				}
			}
			ret.put(d, thetas);
		}
		
		return new Pair<>(ret, ret2);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping pi_unit(XCtx<D> J) {
		
		XCtx<Pair<Triple<D, D, List<D>>, C>> deltaI = delta(J); 
		XCtx pideltaI = pi((XCtx) deltaI);
	
		Map m = new HashMap();

		for (D x : J.terms()) {
			D d = J.type(x).second;
			if (!J.type(x).first.equals("_1")) {
				throw new RuntimeException();
			}

			Map theta = new HashMap();
			for (C c : src.allIds()) {
				for (Triple<D, D, List<D>> f : dst.cat().hom(d, em.get(c).get(0))) {
					List<D> tofind = new LinkedList<>();
					tofind.add(x);
					tofind.addAll(f.third);
					Triple<D, D, List<D>> found = J.find_fast(new Triple<>((D) "_1", f.second,
							tofind));
					if (found == null) {
						throw new RuntimeException();
					}
					List<Pair<Triple<D, D, List<D>>, C>> g = new LinkedList<>();
					Pair<Triple<D, D, List<D>>, C> pr = new Pair<>(found, c);
					g.add(pr);

					if (src.global.allIds().contains(pr.second)
							&& src.global.cat().arrows().contains(pr.first)) {
						theta.put(new Pair<>(c, f), pr.first);
					} else {
						Triple tr = new Triple<>("_1", c, g);
						Object xxx = deltaI.find_fast(tr);
						if (xxx == null) {
							throw new RuntimeException("cannot find " + tr);
						}
						theta.put(new Pair<>(c, f), xxx);
					}
				}
			}
			List l = new LinkedList();
			l.add(theta);
			m.put(x, l);
		}

		for (Object o : dst.allTerms()) {
			if (m.containsKey(o)) {
				continue;
			}
			List l = new LinkedList();
			l.add(o);
			m.put(o, l);
		}

		return new XMapping(J, pideltaI, m, "homomorphism");
	}

	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping pi_counit(XCtx<C> I) {
		
		
		XCtx<Map<Pair<C, Triple<D, D, List<D>>>, Triple<C, C, List<C>>>> piI = pi(I);
		XCtx deltapiI = delta((XCtx) piI);

		Map m = new HashMap();

		for (Object x : deltapiI.terms()) {
			Pair<Triple<?, ?, List>, C> x0 = (Pair<Triple<?, ?, List>, C>) x;
			if (src.cat().arrows().contains(x0.first)) {
				List l = new LinkedList();
				l.add(x0.first.first);
				l.addAll(x0.first.third);
				m.put(x, l);
				continue;
			}

			Map theta;
			if (deltapiI.saturated && DefunctGlobalOptions.debug.fpql.fast_amalgams) {
				if (x0.first.third.size() != 2) {
					throw new RuntimeException();
				}
				if (!x0.first.third.get(0).equals("!__1")) {
					throw new RuntimeException();
				}
				theta = (Map) x0.first.third.get(1);
			} else {
				if (x0.first.third.size() != 1) {
					throw new RuntimeException();
				}
				theta = (Map) x0.first.third.get(0);
			}
			Object o = theta.get(new Pair<>(x0.second, new Triple<>(em.get(x0.second).get(0), em
					.get(x0.second).get(0), new LinkedList<>())));
			if (o == null) {
				throw new RuntimeException();
			}
			List l = new LinkedList();
			l.add(((Triple) o).first);
			l.addAll((Collection) ((Triple) o).third);
			m.put(x, l);
		}

		for (Object o : src.allTerms()) {
			if (m.containsKey(o)) {
				continue;
			}
			List l = new LinkedList();
			l.add(o);
			m.put(o, l);
		}

		return new XMapping(deltapiI, I, m, "homomorphism");
	}

	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XMapping<C, D> rel() {
		Map m = new HashMap<>();

		for (Triple<C, C, List<C>> arr : src.cat().arrows()) {
			if (!arr.first.equals("_1")) {
				continue;
			}
			Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> lx;
			if (src.schema.ids.contains(arr.second)) {
				lx = src.obs(arr);
			} else {
				if (src.global.cat().hom((C) "_1", arr.second).contains(arr)) {
					continue;
				} 
					lx = new HashMap<>();
					lx.put(new Triple<>(arr.second, arr.second, new LinkedList<>()), arr);
				
			}

			List<C> j = new LinkedList<>();
			j.add(arr.first);
			j.addAll(arr.third);
			List<D> x = apply(j);

			Triple<D, D, List<D>> y = dst.find_fast(new Triple<>((D) arr.first, (D) arr.second, x));

			Map<Triple<D, D, List<D>>, Triple<D, D, List<D>>> rx;
			if (dst.schema.ids.contains(y.second)) {
				rx = dst.obs(y);
			} else {
				if (dst.global.cat().hom((D) "_1", y.second).contains(y)) {
					List<D> y2 = new LinkedList<>();
					y2.add(y.first);
					y2.addAll(y.third);
					m.put(lx, y2);
					continue;
				} 
					rx = new HashMap<>();
					rx.put(new Triple<>(y.second, y.second, new LinkedList<>()), y);
				
			}

			if (m.containsKey(lx)) {
				if (!m.get(lx).equals(Collections.singletonList(rx))) {
					throw new RuntimeException();
				}
			}
			m.put(lx, Collections.singletonList(rx));
		}

		for (Object o : src.schema.allTerms()) {
			if (m.containsKey(o)) {
				continue;
			}
			List l = new LinkedList();
			l.add(o);
			m.put(o, l);
		}
		return new XMapping<>(src.rel(), dst.rel(), m, "homomorphism");
	}
	/*
	 * public XMapping<C, D> rel() { Map m = new HashMap<>();
	 * 
	 * for (C c : src.terms()) { Pair<C, C> t = src.type(c); if
	 * (src.schema.allTerms().contains(c)) {
	 * 
	 * continue; }
	 * 
	 * List<C> l = new LinkedList<>(); l.add(c); List<Map<Triple<C, C, List<C>>,
	 * Triple<C, C, List<C>>>> lx = src.obs(l);
	 * 
	 * List<Map<Triple<D, D, List<D>>, Triple<D, D, List<D>>>> rx =
	 * dst.obs(em.get(c)); dst.rel().type((List<D>)rx); //sanity check
	 * 
	 * m.put(lx.get(0), rx); }
	 * 
	 * for (Object o : src.schema.allTerms()) { if (m.containsKey(o)) {
	 * continue; } List l = new LinkedList(); l.add(o); m.put(o, l); }
	 * 
	 * return new XMapping<>(src.rel(), dst.rel(), m, "homomorphism"); }
	 */
	
	@SuppressWarnings("unchecked")
	public XPoly<C, D> uber() {
		Map<Object, Pair<D, Block<C, D>>> map = new HashMap<>();
		Map<D, XCtx<Pair<Triple<D, D, List<D>>, C>>> dfys = new HashMap<>();
		Map<D, XCtx<D>> ys = new HashMap<>();
		for (D d : dst.allIds()) {
			XCtx<D> y = dst.y((D)"u_u", d);
			XCtx<Pair<Triple<D, D, List<D>>, C>> dfy = delta(y);
			dfys.put(d, dfy);
			ys.put(d, y);
		}
		Map<D, XMapping<Pair<Triple<D, D, List<D>>, C>,Pair<Triple<D, D, List<D>>, C>>> dfys_t = new HashMap<>();
		for (D e : dst.allTerms()) {
			if (dst.allIds().contains(e)) {
				continue;
			}
			Pair<D, D> t = dst.type(e);
			D d = t.first;
			D d0= t.second;
			XMapping<D, D> h = uber_sub((D)"u_u", (D)"u_u", e, ys.get(d), ys.get(d0));
			dfys_t.put(e, deltaT(h));
		}
		
		for (D d : dst.ids) {
			XCtx<Pair<Triple<D, D, List<D>>, C>> dfy = dfys.get(d);
			Map<Object, C> from = new HashMap<>();
			for (Pair<Triple<D, D, List<D>>, C> cf : dfy.terms()) {
				from.put(cf, cf.second);
			}
			@SuppressWarnings("rawtypes")
			Set where = new HashSet<>(dfy.eqs);
			Map<D, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
			Map<D, List<Object>> attrs = new HashMap<>();
			for (D e : dst.terms()) {
				if (dst.allIds().contains(e)) {
					continue;
				}
				Pair<D, D> t = dst.type(e);
				if (!t.first.equals(d) || t.second.equals("_1")) {
					continue;
				}
				D d0 = t.second;
				XMapping<Pair<Triple<D, D, List<D>>, C>, Pair<Triple<D, D, List<D>>, C>> dfh = dfys_t.get(e);
				if (dfh == null) {
					throw new RuntimeException("missing: edge " + e + " in " + dfys_t.keySet());
				}
				XCtx<Pair<Triple<D, D, List<D>>, C>> dfy0 = dfys.get(d0);
				if (dfy0 == null) {
					throw new RuntimeException();
				}
				if (dst.ids.contains(d0)) {
					@SuppressWarnings("rawtypes")
					Map edge_m = new HashMap<>();
					dfy0.terms();
					for (Pair<Triple<D, D, List<D>>, C> cf : dfy0.terms()) {
						edge_m.put(cf, dfh.em.get(cf));
					}
					edges.put(e, new Pair<>("q" + d0, edge_m));
				} else {
					@SuppressWarnings("rawtypes")
					List lll = dfh.em.get(new Pair<>(new Triple<>((D)"_1", d0, Collections.singletonList((D)"u_u")), (C)d0));
					if (lll == null) {
						throw new RuntimeException();
					}
					attrs.put(e, lll);
				}
			}
			
			Block<C, D> block = new Block<>(from, where, attrs, edges);
			map.put("q" + d, new Pair<>(d, block));
		}
		
		XPoly<C, D> ret = new XPoly<>(src, dst, map);
		return ret;
	}

	//must reverse
	private static <D> XMapping<D, D> uber_sub(D d, D d0, D e, XCtx<D> I, XCtx<D> J) {
		Map<D, List<D>> m = new HashMap<>();
		List<D> l = new LinkedList<>();
		l.add(d0);
		l.add(e);
		m.put(d, l);
		for (D x : J.allTerms()) {
			if (!m.containsKey(x)) {
				m.put(x, Collections.singletonList(x));
			}
		}
		return new XMapping<>(J, I, m, "homomorphism");
	}

	public static <X> boolean transform_eq(XMapping<X,X> h1, XMapping<X,X> h2) {
		if (!h1.src.equals(h2.src)) {
			throw new RuntimeException("Not equal:\n\n" + h1.src + "\n\nand\n\n" + h2.src);
		}
		if (!h1.dst.equals(h2.dst)) {
			throw new RuntimeException();
		}
		for (X x : h1.src.terms()) {
			List<X> y1 = h1.em.get(x);
			List<X> y2 = h2.em.get(x);
			if (!h1.dst.getKB().equiv(y1, y2)) {
				return false;
			}
		}
		return true;
	}
	
	private Graph<Chc<C,D>, Object> buildFromSig() {
		String pre = "_38u5n";
		int i = 0;
		Graph<Chc<C,D>, Object> G = new DirectedSparseMultigraph<>();
		for (C n : src.ids) {
			G.addVertex(Chc.inLeft(n));
			G.addEdge(pre + i++, Chc.inLeft(n), Chc.inRight(em.get(n).get(0)));
		}
		for (D n : dst.ids) {
			G.addVertex(Chc.inRight(n));
		}
		
		for (C e : src.terms()) {
			if (src.allIds().contains(e)) {
				continue;
			}
			if (e.toString().startsWith("!")) {
				continue;
			}
			Pair<C,C> t = src.type(e);
			if (src.global.ids.contains(t.first) || src.global.ids.contains(t.second)) {
				continue;
			}
			G.addEdge(Chc.inLeft(e), Chc.inLeft(t.first), Chc.inLeft(t.second));
		}
		for (D e : dst.terms()) {
			if (dst.allIds().contains(e)) {
				continue;
			}
			if (e.toString().startsWith("!")) {
				continue;
			}
			Pair<D,D> t = dst.type(e);
			if (dst.global.ids.contains(t.first) || dst.global.ids.contains(t.second)) {
				continue;
			}
			G.addEdge(Chc.inRight(e), Chc.inRight(t.first), Chc.inRight(t.second));
		}
		
		return G;
	}
	
	private JComponent makeGraph() {
		if (src.allTerms().size() > 128) {
			return new JTextArea("Too large to display");
		}
		Graph<Chc<C, D>, Object> sgv = buildFromSig();

		Layout<Chc<C, D>, Object> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Chc<C, D>, Object> vv = new VisualizationViewer<>(layout);
		vv.getRenderContext().setLabelOffset(20);
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);

		Function<Chc<C, D>, Paint> vertexPaint = x -> {
			if (x.left) {
				return Color.BLUE;
			}
			return Color.GREEN;
		};
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		
		Function<Chc<C, D>, String> ttt = arg0 -> {
			if (arg0.left) {
				return arg0.l.toString();
			}
			return arg0.r.toString();
		};
		Function<Object, String> hhh = arg0 -> {
			if (!(arg0 instanceof Chc)) {
				return "";
			}
			@SuppressWarnings("rawtypes")
			Chc xxx = (Chc) arg0;
			if (xxx.left) {
				return xxx.l.toString();
			}
			return xxx.r.toString();
		};
		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(hhh);
		
		float dash[] = { 10.0f };
		Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
		Stroke bs = new BasicStroke();

		Function<Object, Stroke> edgeStrokeTransformer = (Object s) -> {
                    if (!(s instanceof Chc)) {
                        return edgeStroke;
                    }
                    return bs;
                };
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;		
	}

	
}

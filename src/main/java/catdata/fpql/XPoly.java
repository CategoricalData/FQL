package catdata.fpql;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.ide.CodeTextPanel;
import catdata.ide.DefunctGlobalOptions;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XPoly<C,D> extends XExp implements XObject {
	
	public static <X> XPoly<X,X> id(XCtx<X> S) {
		Map<Object, Pair<X, Block<X, X>>> bs = new HashMap<>();
		for (X x : S.ids) {
			Map<X, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
			Map<X, List<Object>> attrs = new HashMap<>();
			Map<Object, X> from = new HashMap<>();
			from.put("q_v", x);
			for (X term : S.terms()) {
				Pair<X, X> t = S.type(term);
				if (!t.first.equals(x) || t.second.equals("_1")) {
					continue;
				}
				List<Object> l = new LinkedList<>();
				l.add("q_v");
				l.add(term);
				if (S.ids.contains(t.second)) {
					Map<Object, List<Object>> m = new HashMap<>();
					m.put("q_v", l);
					edges.put(term, new Pair<>("q" + t.second, m));
				} else {
					attrs.put(term, l);
				}
			}
			Block<X,X> b = new Block<>(from, new HashSet<>(), attrs, edges);
			bs.put("q" + x, new Pair<>(x, b));
		}
		
		XPoly<X,X> ret = new XPoly<>(S, S, bs);
		return ret;
	}

	
	public XPoly(XCtx src, XCtx dst, Map<Object, Pair<D, Block<C, D>>> blocks) {
		this.src = src;
		this.dst = dst;
		this.blocks = blocks;
		validate();
	}

	public XPoly(XExp src, XExp dst, Map<Object, Pair<D, Block<C, D>>> blocks) {
        src_e = src;
        dst_e = dst;
		this.blocks = blocks;
	}
	
	public static class Block<C, D> {

		//should inherit from parent
		public XCtx<C> frozen(XCtx<C> src) {
			Set<C> ids = new HashSet<>();
			Map<C, Pair<C,C>> types = new HashMap<>();
			Set eqs = new HashSet<>();
			
			for (Object k0 : from.entrySet()) {
				Entry k = (Entry) k0;
				types.put((C)k.getKey(), new Pair<>((C)"_1", (C) k.getValue()));
			}
			eqs.addAll(where);
			
			XCtx<C> ret = new XCtx<>(ids, types, eqs, src.global, src, "instance");
			
			return ret;
		}
		
		public Block(Map<Object, C> from, Set<Pair<List<Object>, List<Object>>> where, Map<D, List<Object>> attrs,
				Map<D, Pair<Object, Map<Object, List<Object>>>> edges) {
			this.from = from;
			this.where = where;
			this.attrs = attrs;
			this.edges = edges;
			this.from = DefunctGlobalOptions.debug.fpql.reorder_joins ? sort(from) : from;
		}
		
		private static void count(List<Object> first, Map counts) {
			for (Object s : first) {
				Integer i = (Integer) counts.get(s);
				if (i == null) {
					continue;
				}
				counts.put(s, i+1);
			}
		}

		public Map<Object, C> sort(Map m) {
			Map count = new HashMap<>();
			for (Object s : m.keySet()) {
				count.put(s, 0);
			}
			for (Pair<List<Object>, List<Object>> k : where) {
				count(k.first, count);
				count(k.first, count);
			}
			List l = new LinkedList<>(m.keySet());
			l.sort((Object o1, Object o2) -> ((Integer)count.get(o2)) - ((Integer)count.get(o1)));
			Map ret = new LinkedHashMap<>();
			for (Object s : l) {
				ret.put(s, m.get(s));
			}
			return ret;
		}
		

		Map<Object, C> from = new HashMap<>(); 
		Set<Pair<List<Object>, List<Object>>> where = new HashSet<>();
		Map<D, List<Object>> attrs = new HashMap<>();
		Map<D, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
		/*{ for a:A;
           where a.attA=1;
           attributes attA = a.attA;
           edges f = {b=a.f} : qB;
           } */
		@Override
		public String toString() {
			String for_str = printFor();
			String where_str = printWhere();
			String attr_str = printAttrs();
			String edges_str = printEdges();
			
			return "{for " + for_str + "; where " + where_str + "; attributes " 
			+ attr_str + "; edges " + edges_str + ";}" ;
		}

		private String printEdges() {
			boolean first = false;
			String ret = "";
			for (Entry<D, Pair<Object, Map<Object, List<Object>>>> k : edges.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + " = {" + printSub(k.getValue().second) + "} : " + k.getValue().first;
			}			
			return ret;
		}

		private static <C,D> String printSub(Map<D, List<C>> map) {
			boolean first = false;
			String ret = "";
			for (Entry<D, List<C>> k : map.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + " = " + Util.sep(k.getValue(), ".");
			}			
			return ret;

		}

		private String printAttrs() {
			boolean first = false;
			String ret = "";
			for (Entry<D, List<Object>> k : attrs.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + " = " + Util.sep(k.getValue(), ".");
			}			
			return ret;

		}

		private String printWhere() {
			boolean first = false;
			String ret = "";
			for (Pair<List<Object>, List<Object>> k : where) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += Util.sep(k.first, ".") + " = " + Util.sep(k.second, ".");
			}			
			return ret;
		}

		private String printFor() {
			boolean first = false;
			String ret = "";
			for (Entry<Object, C> k : from.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + ":" + k.getValue();
			}			
			return ret;
		}		
	}
	
	XExp src_e, dst_e;
	
	XCtx<C> src;
	XCtx<D> dst;
	Map<Object, Pair<D, Block<C,D>>> blocks = new HashMap<>();
	
	@Override
	public String kind() {
		return "polynomial";
	}

	@Override
	public String toString() {
		return "polynomial {" + printBlocks() + "\n} : " + src_e + " -> " + dst_e;
	}

	private String printBlocks() {
		boolean first = false;
		String ret = "";
		for (Entry<Object, Pair<D, Block<C, D>>> k : blocks.entrySet()) {
			if (first) {
				ret += ", ";
			}
			first = true;
			ret += "\n  " + k.getKey() + " = " + k.getValue().second + " : " + k.getValue().first;
		}
		
		return ret;
	}

	@Override
	public JComponent display() {
		JTabbedPane ret = new JTabbedPane();
		
		ret.addTab("Text", new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString()));

		ret.addTab("Hat", new CodeTextPanel(BorderFactory.createEtchedBorder(), "", hat().toString()));
		
		ret.addTab("GrothO", new CodeTextPanel(BorderFactory.createEtchedBorder(), "", grotho().toString()));

		ret.addTab("Tilde", new CodeTextPanel(BorderFactory.createEtchedBorder(), "", tilde().toString()));
		
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XPoly<?,?> other = (XPoly<?,?>) obj;
		if (blocks == null) {
			if (other.blocks != null)
				return false;
		} else if (!blocks.equals(other.blocks))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (dst_e == null) {
			if (other.dst_e != null)
				return false;
		} else if (!dst_e.equals(other.dst_e))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (src_e == null) {
			if (other.src_e != null)
				return false;
		} else if (!src_e.equals(other.src_e))
			return false;
		return true;
	}

	@Override
	public <R, E> R accept(E env, XExpVisitor<R, E> v) {
		return v.visit(env, this);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((dst_e == null) ? 0 : dst_e.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		result = prime * result + ((src_e == null) ? 0 : src_e.hashCode());
		return result;
	}
	
	XPoly<C,D> tilde() {
		Map<Object, Pair<D, Block<C, D>>> bs = new HashMap<>();
		for (Object l : blocks.keySet()) {
			Pair<D, Block<C, D>> b2 = blocks.get(l);
			D d = b2.first;
			Block<C, D> block = b2.second;
			
			Map<D, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
			for (D e : block.edges.keySet()) {
				Pair<Object, Map<Object, List<Object>>> p = block.edges.get(e);
				edges.put((D)new Pair(l, e), p);
			}
			Map<D, List<Object>> attrs = new HashMap<>();
			for (D e : block.attrs.keySet()) {
				List<Object> p = block.attrs.get(e);
				attrs.put((D)new Pair(l, e), p);
			}
			Object ooo = "!__1"; // using toString for ! is very bad
			attrs.put((D)new Pair(l, "!_" + d), Collections.singletonList(ooo));
			
			Block<C, D> newblock = new Block<>(block.from, block.where, attrs, edges);
			bs.put(l, new Pair(new Pair(l, d), newblock));
		}
		return new XPoly<>(src, grotho(), bs);
	}
	
	public XCtx<D> grotho() {
		Set new_ids = new HashSet<>();
		Map new_types = new HashMap();
		for (Object l : blocks.keySet()) {
			new_ids.add(new Pair(l, blocks.get(l).first));
			new_types.put(new Pair(l, blocks.get(l).first), new Pair(new Pair(l, blocks.get(l).first), new Pair(l, blocks.get(l).first)));
		}
		for (Object l : blocks.keySet()) {
			for (D e : blocks.get(l).second.edges.keySet()) {
				Pair<D,D> t = dst.type(e);
				new_types.put(new Pair(l, e), new Pair(new Pair(l, t.first), new Pair(blocks.get(l).second.edges.get(e).first, t.second)));				
			}
			for (D e : blocks.get(l).second.attrs.keySet()) {
				Pair<D,D> t = dst.type(e);
				new_types.put(new Pair(l, e), new Pair(new Pair(l, t.first), t.second));				
			}
			//necessary
			new_types.put(new Pair(l, "!_" + blocks.get(l).first), new Pair(new Pair(l, blocks.get(l).first), "_1"));				
		}
		Set new_eqs = new HashSet();
		for (Pair<List<D>, List<D>> p : dst.eqs) {
			List<D> lhs = p.first;
			List<D> rhs = p.second;
			Pair<D, D> t = dst.type(lhs);
			D d = t.first;
			//D d0= t.second;
			for (Object l : blocks.keySet()) {
				if (!blocks.get(l).first.equals(d)) {
					continue;
				}
				Function<List<D>, List> follow = list -> {
					List ret = new LinkedList();
					Object l0 = l;
				//	D x = list.get(0);
					for (D y : list) { //.subList(1, list.size())) {
						Pair<D,D> ty = dst.type(y);
						if (dst.global.ids.contains(ty.first)) {
							ret.add(y);
							continue;
						}
						if (dst.global.ids.contains(ty.second)) {
							ret.add(new Pair(l0, y));
							l0 = null;
						} else {
							if (l0 == null) {
								throw new RuntimeException();
							}
							ret.add(new Pair(l0, y));
							if (!dst.ids.contains(y)) {
								l0 = blocks.get(l0).second.edges.get(y).first;
							}
						}
					}
					return ret;
				};
				List lhs0 = follow.apply(lhs);
				List rhs0 = follow.apply(rhs);
				new_eqs.add(new Pair(lhs0, rhs0));
			}
		}
		return new XCtx<>(new_ids, new_types, new_eqs, dst.global, null, "schema");
	}
	
	private XCtx<D> o_cache = null;
	public XCtx<D> o() {
		if (o_cache != null) {
			return o_cache;
		}
		Map<D, Pair<D, D>> types = new HashMap<>();
		Set<Pair<List<D>, List<D>>> eqs = new HashSet<>();
		for (Object l : blocks.keySet()) {
			D d = blocks.get(l).first;
			types.put((D)l, new Pair<>((D)"_1", d));
			Block<C, D> b = blocks.get(l).second;
			for (D e : b.edges.keySet()) {
				Pair<Object, Map<Object, List<Object>>> k = b.edges.get(e);
				Object l0 = k.first;
				List<D> lhs = new LinkedList<>();
				lhs.add((D)l);
				lhs.add(e);
				List<D> rhs = new LinkedList<>();
				rhs.add((D)l0);
				eqs.add(new Pair<>(lhs, rhs));
			}
		}
		
		o_cache = new XCtx<>(new HashSet<>(), types, eqs, XCtx.empty_global(), dst.hat(), "instance");
		return o_cache;
	}

	private Map<Object, D> conj1 = null;
	private Map<D, Object> conj2 = null;
	private XCtx<C> T(D d) {
		initConjs();
		return freeze().apply(new Pair<>(conj2.get(d), Collections.singletonList(d))).src;
	}
	private XMapping<C, C> T(List<D> d) {
		initConjs();
		D t = dst.type(d).first;
		return conj2.containsKey(t) ? freeze().apply(new Pair<>(conj2.get(t), d)) : freeze().apply(new Pair<>(t, d));
	}
	
	private void initConjs() {
		if (conj1 != null && conj2 != null) {
			return;
		}
		conj1 = new HashMap<>();
		conj2 = new HashMap<>();
		for (Object l : blocks.keySet()) {
			if (conj1.containsKey(l)) {
				throw new RuntimeException("Duplicate label + l");
			}
			D d = blocks.get(l).first;
			if (conj2.containsKey(d)) {
				throw new RuntimeException("Not conjunctive on " + d);
			}
			conj1.put(l, d);
			conj2.put(d, l);			
		}
	}

	private Function<Pair<Object, List<D>>, XMapping<C,C>> frozen = null;
	Function<Pair<Object, List<D>>, XMapping<C,C>> freeze() {
		if (frozen != null) {
			return frozen;
		}
		
		Map<Object, XCtx<C>> frozens = new HashMap<>();
		for (Object l : blocks.keySet()) {
			frozens.put(l, blocks.get(l).second.frozen(src));
		}
		for (C t : src.global.ids) {
			frozens.put(t, src.y((C)"y_a", t));
		}
		
		Map<D, XMapping<C,C>> transforms2 = new HashMap<>();
		for (D e : dst.global.terms()) {
			Pair<D,D> t = dst.global.type(e);
			XCtx<C> srcX = frozens.get(t.first);
			XCtx<C> dstX = frozens.get(t.second);
			Map em = new HashMap<>();
			List v2 = new LinkedList<>();
			v2.add("y_a");
			v2.add(e);
			em.put("y_a", v2);
			for (Object o : dstX.allTerms()) {
				if (em.containsKey(o)) {
					continue;
				}
				em.put(o, Collections.singletonList(o));
			}
			XMapping<C,C> m = new XMapping(dstX, srcX, em, "homomorphism");
			transforms2.put(e, m);
		}
		
		Map<Pair<Object, D>, XMapping<C,C>> transforms = new HashMap<>();
		
		for (Object k : blocks.keySet()) {
			Pair<D, Block<C, D>> b = blocks.get(k);
			XCtx<C> srcX = frozens.get(k);
			
			//just validates
			for (D term : dst.terms()) {
				Pair<D, D> t = dst.type(term);
				if (!t.first.equals(b.first)) {
					continue;
				}
				if (!dst.ids.contains(term) && dst.ids.contains(t.second) && !b.second.edges.containsKey(term)) {
					throw new RuntimeException("Missing mapping for edge " + term + " in " + k);
				} else if (!t.second.equals("_1") && dst.global.ids.contains(t.second) && !b.second.attrs.containsKey(term)){
					throw new RuntimeException("Missing mapping for attr " + term + " in " + k);
				}
			}

			for (D k2 : b.second.edges.keySet()) {
				Pair<Object, Map<Object, List<Object>>> v2 = b.second.edges.get(k2);
				XCtx<C> dstX = frozens.get(v2.first);
				if (dstX == null) {
					throw new RuntimeException("Subquery not found: " + v2.first);
				}
				Map em = new HashMap<>(v2.second);
				for (Object o : dstX.schema.allTerms()) {
					if (em.containsKey(o)) {
						continue;
					}
					em.put(o, Collections.singletonList(o));
				}
				try {
					XMapping<C,C> mmm = new XMapping(dstX, srcX, em, "homomorphism");
					transforms.put(new Pair<>(k,k2),mmm);
				} catch (RuntimeException rex) {
					rex.printStackTrace();
					throw new RuntimeException("Error in block " + k + " edge " + k2 + " is " + rex.getMessage());
				}
			}			
			
			for (D k2 : b.second.attrs.keySet()) {
				List v2 = b.second.attrs.get(k2);
				XCtx<C> dstX = frozens.get(dst.type(k2).second);
				Map em = new HashMap<>();
				em.put("y_a", v2);
				for (Object o : dstX.allTerms()) {
					if (em.containsKey(o)) {
						continue;
					}
					em.put(o, Collections.singletonList(o));
				}
				XMapping<C,C> mmm = new XMapping(dstX, srcX, em, "homomorphism");

				transforms.put(new Pair<>(k,k2), mmm);
			}	
			
			D k2 = (D) ("!_" + b.first);
			List v2 = Collections.singletonList("_1"); 
			XCtx<C> dstX = frozens.get(dst.type(k2).second);
			Map em = new HashMap<>();
			em.put("y_a", v2);
			for (Object o : dstX.allTerms()) {
				if (em.containsKey(o)) {
					continue;
				}
				em.put(o, Collections.singletonList(o));
			}
			XMapping<C,C> m = new XMapping(dstX, srcX, em, "homomorphism");
			transforms.put(new Pair<>(k, k2), m);
			XMapping mmm = new XMapping<>(srcX, "homomorphism");
			transforms.put(new Pair<>(k, b.first), mmm);				
		}

		Map<Pair<Object, List<D>>, XMapping<C,C>> frozen_cache = new HashMap<>();
		frozen = p -> {
			if (frozen_cache.containsKey(p)) {
				return frozen_cache.get(p);
			}
			Object l = p.first;
			D e = p.second.get(0);
			XMapping<C,C> h = transforms.get(new Pair<>(l, e));
			if (h == null) {
				h = transforms2.get(e);
				if (h == null) {
					throw new RuntimeException("(" + l + "," + e + ") not in " + transforms.keySet() + " or " + transforms2.keySet());
				}
			}

			for (D eX : p.second.subList(1, p.second.size())) {
				XMapping<C, C> hX;
				if (transforms2.containsKey(eX)) {
					l = null;
					hX = transforms2.get(eX);
					if (hX == null) {
						throw new RuntimeException();
					}
				} else {
					if (l == null) {
						throw new RuntimeException();
					}
					Object lX;
					lX = dst.ids.contains(e) ? l : blocks.get(l).second.edges.get(e).first;
					hX = transforms.get(new Pair<>(lX, eX));
					if (hX == null) {
						throw new RuntimeException();
					}
					l = lX;
				}
				e = eX;
				h = new XMapping<>(hX, h);
			}
			frozen_cache.put(p, h);
			return h;
		};
		return frozen;
	}
	
	public void validate() {
		freeze();
		for (Pair<List<D>, List<D>> eq : dst.allEqs()) {
			List<D> p = eq.first;
			List<D> q = eq.second;
			Pair<D,D> t = dst.type(p);
			D d = t.first;
			D d0= t.second;
			for (Object l : blocks.keySet()) {
				Pair<D, Block<C, D>> block = blocks.get(l);
				if (!block.first.equals(d)) {
					continue;
				}
				List p2 = new LinkedList<>(p);
				p2.add(0, l);
				List q2 = new LinkedList<>(q);
				q2.add(0, l);
				//are not paths in o
				if (!dst.global.allIds().contains(d0) && !o().getKB().equiv(p2, q2)) {
					throw new RuntimeException("Not respected on " + eq + " in " + o());
				}
				XMapping<C, C> p3 = freeze().apply(new Pair<>(l, p));
				XMapping<C, C> q3 = freeze().apply(new Pair<>(l, q));
				if (!XMapping.transform_eq(p3, q3)) {
					throw new RuntimeException("on block " + l + " equation " + eq + " becomes \n\n" + p3 + " \n\n=\n\n " + q3 + " \n\nbut are not equal");
				}
			}
		} 
	}
	
	public XMapping<C, C> coapply(XMapping<D,D> h) {
		XCtx<C> src0 = coapply(h.src);
		XCtx<C> dst0 = coapply(h.dst);
		
		Map<C, List<C>> em0 = new HashMap<>();
		
		for (C c : src0.allTerms()) {
			if (em0.containsKey(c)) {
				continue;
			}
			em0.put(c, Collections.singletonList(c));
		}
		return new XMapping<>(src0, dst0, em0 , "homomorphism");
	}
	
	XCtx<C> coapply(XCtx<D> I) {
		Map types = new HashMap<>();
		Set eqs = new HashSet<>();
 
		for (D d : dst.allIds()) {
			for (D x : I.terms()) {
				Pair<D, D> t = I.type(x);
				if (!t.first.equals("_1")) {
					throw new RuntimeException();
				}
				if (!t.second.equals(d)) {
					continue;
				}
				for (C k : T(d).terms()) {
					Pair<C, C> u = T(d).type(k);
					if (!u.first.equals("_1")) {
						throw new RuntimeException();
					}
					C c = u.second;
					Triple<D, D, C> gen = new Triple<>(d, x, k);
					types.put(gen, new Pair("_1", c));
				}
				for (Pair<List<C>, List<C>> eq : T(d).eqs) {
					Function prepend = y -> {
						if (T(d).terms().contains(y)) {
							return new Triple(d, x, y);
						}
						return y;
					};
					if (eq.first.stream().filter(T(d).terms()::contains).count() > 1) {
						throw new RuntimeException("Has too many variables: " + eq.first);
					}
					if (eq.second.stream().filter(T(d).terms()::contains).count() > 1) {
						throw new RuntimeException("Has too many variables: " + eq.second);
					}
					List<C> lhs = (List<C>) eq.first.stream().map(prepend).collect(Collectors.toList());
					List<C> rhs = (List<C>) eq.second.stream().map(prepend).collect(Collectors.toList());
					eqs.add(new Pair(lhs, rhs));
				}
			}
		}
		for (Pair<List<D>, List<D>> eq : I.eqs) {
			D d0 = I.type(eq.first).second;
			Pair<D, List<D>> lhs = trySplit(eq.first, dst, d0); //must be nonempty
			Pair<D, List<D>> rhs = trySplit(eq.second, dst, d0);	
			D d1 = I.type(lhs.second).first;
			D d2 = I.type(rhs.second).first;
	
			XMapping<C, C> Tq1 = T(lhs.second);
			XMapping<C, C> Tq2 = T(rhs.second);
			for (C Tjd0 : T(d0).terms()) {
				List<C> a1 = Tq1.em.get(Tjd0);
				List<C> a2 = Tq2.em.get(Tjd0);
				
				Pair<C, List<C>> a1x = trySplit(a1, src, Tq1.dst.type(a1).second);
				Pair<C, List<C>> a2x = trySplit(a2, src, Tq2.dst.type(a2).second);
				
				List newlhs;
				List newrhs;
				if (lhs.first == null) {
					newlhs = eq.first;
				} else if (a1x.first == null) {
					newlhs = a1;
				} else {
					newlhs = new LinkedList();
					newlhs.add(new Triple(d1, lhs.first, a1x.first));
					newlhs.addAll(a1x.second);
				}
				if (rhs.first == null) {
					newrhs = eq.second;
				} else if (a2x.first == null) {
					newrhs = a2;
				} else {
					newrhs = new LinkedList();
					newrhs.add(new Triple(d2, rhs.first, a2x.first));
					newrhs.addAll(a2x.second);
				}
				eqs.add(new Pair(newlhs, newrhs));
			}
		}
		
		return new XCtx<>(new HashSet<>(), types, eqs, src.global, src, "instance");
	}



	private static <C> Pair<C, List<C>> trySplit(List<C> l, XCtx<C> S, C o) {
		C ret = null;
		int i = 0;
		int j = 0;
		for (C c : l) {
			i++;
			if (!S.allTerms().contains(c)) {
				if (ret != null) {
					throw new RuntimeException("Too many vars in try split " + l);
				}
				j = i;
				ret = c;
			}
		}
		List retl = l.subList(j, l.size());
		if (retl.isEmpty()) {
			retl = Collections.singletonList(o);
		}
		return new Pair<>(ret, retl);

	}
	
	XPoly<C,D> hat() {
		Map<Object, Pair<D, Block<C, D>>> bs = new HashMap<>();
		for (Object l : blocks.keySet()) {
			Pair<D, Block<C, D>> b = blocks.get(l);
			Map<Object, C> from = new HashMap<>();
			for (Entry<Object, C> fr : b.second.from.entrySet()) {
				if (!src.global.allIds().contains(fr.getValue())) {
					from.put(fr.getKey(), fr.getValue());
				} 
			}
			Set<Pair<List<Object>, List<Object>>> where = new HashSet<>();
			for (Pair<List<Object>, List<Object>> p : b.second.where) {
				if (!containsType(b.second.from, src, p.first) && !containsType(b.second.from, src, p.second)) {
					where.add(p);
				}
			}
			Map<D, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
			for (Entry<D, Pair<Object, Map<Object, List<Object>>>> fr : b.second.edges.entrySet()) {
				Map<Object, List<Object>> xxx = new HashMap<>();
				for (Entry<Object, List<Object>> hh : fr.getValue().second.entrySet()) {
					Object t = blocks.get(fr.getValue().first).second.from.get(hh.getKey());
					if (!src.global.allIds().contains(t)) {
						xxx.put(hh.getKey(), hh.getValue());
					}
				}
				edges.put(fr.getKey(), new Pair<>(fr.getValue().first, xxx));
			}
			Block<C, D> b2 = new Block<>(from, where, new HashMap<>(), edges);
			bs.put(l, new Pair<>(b.first, b2));
		}
		
		return new XPoly<>(src.hat(), dst.hat(), bs);
	}
	
	private static <C> boolean containsType(Map<Object, C> F, XCtx<C> S, List<Object> p) {
		for (Object o : p) {
			if (F.containsKey(o)) {
				C c = F.get(o);
				if (S.global.allIds().contains(c)) {
					return true;
				}
			} else {
				Pair<C, C> v = S.type((C)o);
				if (S.global.allIds().contains(v.first) || S.global.allIds().contains(v.second)) {
					return true;
				}
			}
		}
		
		return false;
	}

	XCtx<Pair<Object, Map<Object, Triple<C, C, List<C>>>>> apply(XCtx<C> I) {
		return XProd.uberflower(this, I);
	}
	
	public static <C,D,E> XPoly<C,E> compose(XPoly<C,D> Q, XPoly<D,E> Q0) {
		
		XCtx<D> Qo = Q.o();
		XPoly<D, E> Q0hat = Q0.hat();
		XCtx<Pair<Object, Map<Object, Triple<D, D, List<D>>>>> labels = Q0hat.apply(Qo);
		for (Pair<Object, Map<Object, Triple<D, D, List<D>>>> label : labels.terms()) {
			Object l = label.first;
			Object A = labels.type(label).second;
			Map<Object, Triple<D, D, List<D>>> valuation = label.second;

			XCtx<D> frc = Q0hat.freeze().apply(new Pair(l, Collections.singletonList(A))).src;			
			Map<D, List<D>> map = new HashMap<>();
			for (Object k : valuation.keySet()) {
				List<D> list = new LinkedList<>(valuation.get(k).third);
				list.add(0, valuation.get(k).first);
				map.put((D)k, list);
			}
			for (Object o : frc.allTerms()) {
				if (map.containsKey(o)) {
					continue;
				}
				map.put((D)o, Collections.singletonList((D)o));
			}
			XMapping<D,D> h = new XMapping<>(frc, Qo, map, "homomorphism");
		
			XCtx<D> intQo = Q.grotho();
			Map<D, Pair<D, D>> types = new HashMap<>();
			for (D gen : frc.terms()) {
				D ty = frc.type(gen).second;
				List<D> hgen = h.em.get(gen);
				if (hgen.size() != 2) {
					throw new RuntimeException(gen + " mapsto " + hgen);
				}
				types.put(gen, new Pair("_1", new Pair(hgen.get(1), ty)));
			}
			XCtx<D> inth_pre = new XCtx<>(new HashSet<>(), types, new HashSet<>(), intQo.global, intQo, "instance");
			Set<Pair<List<D>, List<D>>> new_eqs = new HashSet<>();
			for (Pair<List<D>, List<D>> eq : h.src.eqs) {
				try { //morally should transform !, but can't so hack
					inth_pre.type(eq.first);
					inth_pre.type(eq.second);
					new_eqs.add(eq);
				} catch (Exception ex) {
				}
			}
			XCtx<D> inth = new XCtx<>(new HashSet<>(), types, new_eqs, intQo.global, intQo, "instance");
			/*XCtx<C> fr = */ Q.tilde().coapply(inth);
		}
		
		return (XPoly<C, E>) Q;
	}
	
	
	
}



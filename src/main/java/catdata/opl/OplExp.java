package catdata.opl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.google.common.base.Function;

import catdata.Chc;
import catdata.Environment;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.fqlpp.cat.FinSet;
import catdata.ide.CodeTextPanel;
import catdata.ide.DefunctGlobalOptions;
import catdata.ide.GuiUtil;
import catdata.opl.OplParser.DoNotIgnore;
import catdata.opl.OplQuery.Agg;
import catdata.opl.OplQuery.Block;
import catdata.provers.KBExp;
import catdata.provers.KBFO;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public abstract class OplExp implements OplObject {

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

	@Override
	public JComponent display() {
		CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
		JTabbedPane ret = new JTabbedPane();
		ret.add(p, "Text");
		return ret;
	}

	public abstract <R, E> R accept(E env, OplExpVisitor<R, E> v);

	public static class OplDistinct<S, C, V, X> extends OplExp {
		final String str;

		public OplDistinct(String str) {
			this.str = str;
		}

		public OplInst<S, C, V, X> validate(OplInst<S, C, V, X> inst) {
			List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> eqs = new LinkedList<>(inst.P.equations);

			Quad<OplSetInst<S, C, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplPres<S, C, V, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<C, X>, V>>> satX = inst.saturate();
			OplSetInst<S, C, OplTerm<Chc<C, X>, V>> sat = satX.first;

			for (S s : inst.S.entities) {
				Set<C> outEdges = inst.S.outEdges(s);
				for (OplTerm<Chc<C, X>, V> term1 : sat.sorts.get(s)) {

					outer: for (OplTerm<Chc<C, X>, V> term2 : sat.sorts.get(s)) {
						if (term1.equals(term2)) {
							continue;
						}
						for (C c : outEdges) {
							OplTerm<Chc<C, X>, V> lhs = new OplTerm<>(Chc.inLeft(c), Collections.singletonList(term1)); // pres
																											// is
																											// broken
							OplTerm<Chc<C, X>, V> rhs = new OplTerm<>(Chc.inLeft(c), Collections.singletonList(term2));
							if (!inst.P.toSig.getKB().nf(lhs).equals(inst.P.toSig.getKB().nf(rhs))) {
								continue outer;
							}
						}
						eqs.add(new Pair<>(term1, term2));
					}
				}
			}

			OplPres<S, C, V, X> pres = new OplPres<>(inst.P.prec, inst.S0, inst.S.sig, inst.P.gens, eqs);

			// OplInst0<S,C,V,X> ret = new OplInst0<S,C,V,X>(pres);

			OplInst<S, C, V, X> ret2 = new OplInst<>(inst.S0, "?", inst.P0);
			ret2.validate(inst.S, pres, inst.J);
			return ret2;
		}

		@Override
		public String toString() {
			return "distinct " + str;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((str == null) ? 0 : str.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplDistinct other = (OplDistinct) obj;
			if (str == null) {
				if (other.str != null)
					return false;
			} else if (!str.equals(other.str))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class OplGraph<N, E> extends OplExp {
		final Set<N> nodes;
		Map<E, Pair<N, N>> edges;

		public OplGraph(Set<N> nodes, Map<E, Pair<N, N>> edges) {
			this.nodes = nodes;
			this.edges = edges;
			for (E e : edges.keySet()) {
				if (!this.nodes.contains(edges.get(e).first)) {
					throw new RuntimeException("Not a node: " + edges.get(e).first);
				}
				if (!this.nodes.contains(edges.get(e).second)) {
					throw new RuntimeException("Not a node: " + edges.get(e).second);
				}
			}
		}

		public OplGraph(List<N> nodes, List<Triple<E, N, N>> edges) {
			this.nodes = new HashSet<>(nodes);
			if (this.nodes.size() != nodes.size()) {
				throw new RuntimeException("Duplicate element in " + nodes);
			}

			this.edges = new HashMap<>();
			for (Triple<E, N, N> e : edges) {
				if (this.edges.containsKey(e.first)) {
					throw new RuntimeException("Duplicate element: " + e.first);
				}
				if (!this.nodes.contains(e.second)) {
					throw new RuntimeException("Not a node: " + e.second);
				}
				if (!this.nodes.contains(e.third)) {
					throw new RuntimeException("Not a node: " + e.third);
				}
				this.edges.put(e.first, new Pair<>(e.second, e.third));
			}
		}

		@Override
		public String toString() {
			List<String> l = new LinkedList<>();
			for (E e : edges.keySet()) {
				l.add(e + ": " + edges.get(e).first + " -> " + edges.get(e).second);
			}
			return "graph {\n\tnodes\n\t\t" + Util.sep(nodes, ",") + ";\n\tedges\n\t\t" + Util.sep(l, ",\n\t\t") + ";\n}";
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((edges == null) ? 0 : edges.hashCode());
			result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplGraph other = (OplGraph) obj;
			if (edges == null) {
				if (other.edges != null)
					return false;
			} else if (!edges.equals(other.edges))
				return false;
			if (nodes == null) {
				if (other.nodes != null)
					return false;
			} else if (!nodes.equals(other.nodes))
				return false;
			return true;
		}

		@Override
		public <Rx, Ex> Rx accept(Ex env, OplExpVisitor<Rx, Ex> v) {
			return v.visit(env, this);
		}

	}

	public static class OplGround extends OplExp {
		final Map<String, Set<String>> entities = new HashMap<>();
		Map<String, Map<String, String>> symbols;
		String sch;

		public OplGround(Map<String, List<String>> entities, Map<String, Map<String, String>> symbols, String sch) {
			for (String k : entities.keySet()) {
				this.entities.put(k, new HashSet<>(entities.get(k)));
				if (this.entities.get(k).size() != entities.get(k).size()) {
					throw new RuntimeException("Duplicate entity element in " + entities);
				}
			}
			this.symbols = symbols;
			this.sch = sch;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((entities == null) ? 0 : entities.hashCode());
			result = prime * result + ((sch == null) ? 0 : sch.hashCode());
			result = prime * result + ((symbols == null) ? 0 : symbols.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplGround other = (OplGround) obj;
			if (entities == null) {
				if (other.entities != null)
					return false;
			} else if (!entities.equals(other.entities))
				return false;
			if (sch == null) {
				if (other.sch != null)
					return false;
			} else if (!sch.equals(other.sch))
				return false;
			if (symbols == null) {
				if (other.symbols != null)
					return false;
			} else if (!symbols.equals(other.symbols))
				return false;
			return true;
		}

		public OplInst0<String, String, String, String> validate(OplSchema<String, String, String> sch) {
			Map<String, String> gens = new HashMap<>();
			List<Pair<OplTerm<Chc<String, String>, String>, OplTerm<Chc<String, String>, String>>> equations = new LinkedList<>();

			for (String k : entities.keySet()) {
				if (!sch.entities.contains(k)) {
					throw new RuntimeException("Extra entity set for " + k);
				}
			}

			for (String k : sch.entities) {
				if (!entities.containsKey(k)) {
					throw new RuntimeException("Missing entity set for " + k);
				}
				for (String gen : entities.get(k)) {
					gens.put(gen, k);
				}
			}

			for (String f : symbols.keySet()) {
				if (!sch.projEA().symbols.containsKey(f)) {
					throw new RuntimeException("Extra edge/attr " + f);
				}
			}

			for (String f : sch.projEA().symbols.keySet()) {
				Pair<List<String>, String> st = sch.projEA().symbols.get(f);
				String s = st.first.get(0);
				String t = st.second;

				Set<String> s0 = entities.get(s);
				Set<String> t0 = entities.get(t);

				Map<String, String> f0 = symbols.get(f);
				if (f0 == null) {
					throw new RuntimeException("Missing edge/attr: " + f);
				}

				for (String x : f0.keySet()) {
					if (!s0.contains(x)) {
						throw new RuntimeException("Error on " + f + ", " + x + " is not in " + s);
					}
					String y = f0.get(x);
					if (y == null) {
						throw new RuntimeException("Error on " + f + ", no action specified for " + x);
					}
					if (t0 != null && !t0.contains(y)) {
						throw new RuntimeException("Error on " + f + ", " + y + " is not in " + t);
					}
					/*
					 * List<OplTerm<Chc<String, String>, String>> left_args =
					 * new LinkedList<>(); left_args.add(new
					 * OplTerm<>(Chc.inRight(x), new LinkedList<>()));
					 * OplTerm<Chc<String, String>, String> left = new
					 * OplTerm<>(Chc.inLeft(f),left_args); Chc<String, String> r
					 * = sch.projE().symbols.containsKey(f) ? Chc.inRight(y) :
					 * Chc.inLeft(y); OplTerm<Chc<String, String>, String> right
					 * = new OplTerm<>(r, new LinkedList<>());
					 */
					List<OplTerm> left_args = new LinkedList<>();
					left_args.add(new OplTerm<>(x, new LinkedList<>()));
					OplTerm left = new OplTerm(f, left_args);
					// Chc<String, String> r =
					// sch.projE().symbols.containsKey(f) ? Chc.inRight(y) :
					// Chc.inLeft(y);
					OplTerm right = new OplTerm(y, new LinkedList<>());

					equations.add(new Pair<>(left, right));
				}

			}

			OplPres<String, String, String, String> p = new OplPres<>(new HashMap<>(), this.sch, sch.sig, gens, equations);
			p.toSig();
			OplInst0<String, String, String, String> ret = new OplInst0<>(p);
			return ret;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class OplString extends OplExp {
		public final String string;

		public OplString(String string) {
			this.string = string;
		}

		@Override
		public String toString() {
			return string;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplString other = (OplString) obj;
			if (string == null) {
				if (other.string != null)
					return false;
			} else if (!string.equals(other.string))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((string == null) ? 0 : string.hashCode());
			return result;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			throw new RuntimeException();
		}
	}

	public static class OplChaseExp extends OplExp {
		final int limit;
		final String I;
		final List<String> EDs;

		public OplChaseExp(int limit, String i, List<String> eDs) {
			this.limit = limit;
			I = i;
			EDs = eDs;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((EDs == null) ? 0 : EDs.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + limit;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplChaseExp other = (OplChaseExp) obj;
			if (EDs == null) {
				if (other.EDs != null)
					return false;
			} else if (!EDs.equals(other.EDs))
				return false;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
            return limit == other.limit;
        }

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return "OplChase [limit=" + limit + ", I=" + I + ", EDs=" + EDs + "]";
		}

	}

	public static class OplPragma extends OplExp {
		final Map<String, String> map;

		public OplPragma(Map<String, String> map) {
			this.map = map;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return map.toString();
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((map == null) ? 0 : map.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplPragma other = (OplPragma) obj;
			if (map == null) {
				if (other.map != null)
					return false;
			} else if (!map.equals(other.map))
				return false;
			return true;
		}

	}

	public static class OplVar extends OplExp {
		String name;

		public OplVar(String c) {
			if (c == null) {
				throw new RuntimeException();
			}
			name = c;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplVar other = (OplVar) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}

	public static class OplPivot<S, C, V, X> extends OplExp {
		final String I0;
		OplInst<S, C, V, X> I;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I0 == null) ? 0 : I0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplPivot other = (OplPivot) obj;
			if (I0 == null) {
				if (other.I0 != null)
					return false;
			} else if (!I0.equals(other.I0))
				return false;
			return true;
		}

		public OplPivot(String I0) {
			this.I0 = I0;
		}

		public void validate(OplInst<S, C, V, X> I) {
			this.I = I;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public JComponent display() {
			JTabbedPane ret = new JTabbedPane();

			Pair<OplSchema<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V>, OplInst<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V, OplTerm<Chc<C, X>, V>>> xxx = pivot();

			ret.add(xxx.first.display(), "Schema");

			ret.add(xxx.second.display(), "Instance");

			return ret;
		}

		// : I pivots to a schema with a type side isomorphic but not equal
		// to I's type side.
		// schemas should be presentations on signatures, not signatures with a
		// set of entities.
		// so the mapping doesn't work because is not identity on type side
		public Pair<OplSchema<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V>, OplInst<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V, OplTerm<Chc<C, X>, V>>> pivot() {
			Quad<OplSetInst<S, C, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplPres<S, C, V, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<C, X>, V>>> quadX = I.saturate();

			Set<Chc<S, OplTerm<Chc<C, X>, V>>> sorts = new HashSet<>();
			Set<Chc<S, OplTerm<Chc<C, X>, V>>> entities = new HashSet<>();
			Map<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, Pair<List<Chc<S, OplTerm<Chc<C, X>, V>>>, Chc<S, OplTerm<Chc<C, X>, V>>>> symbols = new HashMap<>();
			List<Triple<OplCtx<Chc<S, OplTerm<Chc<C, X>, V>>, V>, OplTerm<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V>, OplTerm<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V>>> oldeqs = new LinkedList<>();

			Map<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>> gens = new HashMap<>();
			List<Pair<OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V>, OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V>>> eqs = new LinkedList<>();

			//Map<Chc<S, OplTerm<Chc<C, X>, V>>, S> sortMapping = new HashMap<>();
			// Map<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C,
			// X>, V>>, C>>, Pair<OplCtx<S, V>, OplTerm<C, V>>> symbolMapping =
			// new HashMap<>();

			Map<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, String> jmap = new HashMap<>();
			for (S s : I.S.projT().sorts) {
				sorts.add(Chc.inLeft(s));
		//		sortMapping.put(Chc.inLeft(s), s);
			}
			for (C c : I.S.projT().symbols.keySet()) {
				Pair<List<S>, S> t = I.S.projT().symbols.get(c);
				List<Chc<S, OplTerm<Chc<C, X>, V>>> l1 = new LinkedList<>();
				for (S s : t.first) {
					l1.add(Chc.inLeft(s));
				}
				symbols.put(Chc.inLeft(c), new Pair<>(l1, Chc.inLeft(t.second)));
				if (I.J != null) {
					jmap.put(Chc.inLeft(c), I.J.defs.get(c));
				}
			}
			if (I.J != null && I.J.defs.containsKey("_preamble")) {
				((Map) jmap).put("_preamble", I.J.defs.get("_preamble"));
			}
			if (I.J != null && I.J.defs.containsKey("_compose")) {
				((Map) jmap).put("_compose", I.J.defs.get("_compose"));
			}
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : I.S.projT().equations) {
				List<Pair<V, Chc<S, OplTerm<Chc<C, X>, V>>>> m = new LinkedList<>();
				OplCtx<Chc<S, OplTerm<Chc<C, X>, V>>, V> ctx = new OplCtx<>(m);
				OplTerm<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V> l = eq.second.inLeft();
				OplTerm<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V> r = eq.third.inLeft();
				oldeqs.add(new Triple<>(ctx, l, r));
			}

			for (S s : I.S.entities) {
				for (OplTerm<Chc<C, X>, V> term : quadX.fourth.sorts.get(s)) {
					Chc<S, OplTerm<Chc<C, X>, V>> term0 = Chc.inRight(term);
					sorts.add(term0);
					entities.add(term0);
					gens.put(term, term0);

					for (C c : I.S.projA().symbols.keySet()) {
						Pair<List<S>, S> ty = I.S.projA().symbols.get(c);
						if (ty.first.size() != 1) {
							throw new RuntimeException();
						}
						if (!ty.first.get(0).equals(s)) {
							continue;
						}

						Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C> attr = new Triple<>(term, Chc.inLeft(ty.second), c);
						Pair<List<Chc<S, OplTerm<Chc<C, X>, V>>>, Chc<S, OplTerm<Chc<C, X>, V>>> attr_t = new Pair<>(Collections.singletonList(term0), Chc.inLeft(ty.second));
						symbols.put(Chc.inRight(attr), attr_t);

						OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V> genAsTerm = new OplTerm<>(Chc.inRight(term), new LinkedList<>());

						OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V> lhs = new OplTerm<>(Chc.inLeft(Chc.inRight(attr)), Collections.singletonList(genAsTerm));

						OplTerm<Chc<C, X>, V> result = quadX.fourth.symbols.get(c).get(Collections.singletonList(term));
						OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V> rhs = convert(result);

						eqs.add(new Pair<>(lhs, rhs));

					}

				}
			}
			// entities only, attrs done above
			for (C c : I.S.projE().symbols.keySet()) {
				Pair<List<S>, S> ty = I.S.projE().symbols.get(c);
				if (ty.first.size() != 1) {
					throw new RuntimeException();
				}
				for (List<OplTerm<Chc<C, X>, V>> row : quadX.fourth.symbols.get(c).keySet()) {
					OplTerm<Chc<C, X>, V> input = row.get(0);
					Chc<S, OplTerm<Chc<C, X>, V>> output = Chc.inRight(quadX.fourth.symbols.get(c).get(row));
					Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>> key = Chc.inRight(new Triple<>(input, output, c));
					Pair<List<Chc<S, OplTerm<Chc<C, X>, V>>>, Chc<S, OplTerm<Chc<C, X>, V>>> value = new Pair<>(Collections.singletonList(Chc.inRight(input)), output);
					symbols.put(key, value);

					Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C> fk = new Triple<>(input, output, c);

					OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V> genAsTerm = new OplTerm<>(Chc.inRight(input), new LinkedList<>());

					OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V> lhs = new OplTerm<>(Chc.inLeft(Chc.inRight(fk)), Collections.singletonList(genAsTerm));

					OplTerm<Chc<C, X>, V> result = quadX.fourth.symbols.get(c).get(Collections.singletonList(input));

					OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V> rhs = new OplTerm<>(Chc.inRight(result), new LinkedList<>());

					eqs.add(new Pair<>(lhs, rhs));
				}

			}

			OplSchema<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V> retSch = new OplSchema<>("?", entities);
			OplSig<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V> retSig = new OplSig<>(I.P.sig.fr, new HashMap<>(), sorts, symbols, oldeqs);
			retSch.validate(retSig);

			OplJavaInst<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V> J0 = null;
			if (I.J != null) {
				J0 = new OplJavaInst<>(jmap, "?");
				J0.validate(retSch.projT(), I.J.ENV);
			}

			OplPres<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V, OplTerm<Chc<C, X>, V>> P = new OplPres<>(new HashMap<>(), "?", retSig, gens, eqs);
			OplInst<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, V, OplTerm<Chc<C, X>, V>> retInst = new OplInst<>("?", "?", "?");
			retInst.validate(retSch, P, J0);

			// OplMapping<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C,
			// Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>,
			// V, S, C>
			// retMap0 = new OplMapping<>(sortMapping, symbolMapping, "?",
			// I.S.sig0);
			// OplTyMapping<Chc<S, OplTerm<Chc<C, X>, V>>, Chc<C,
			// Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>,
			// V, S, C> retMapping
			// = new OplTyMapping<Chc<S,OplTerm<Chc<C, X>, V>>,
			// Chc<C,Triple<OplTerm<Chc<C, X>, V>, Chc<S,OplTerm<Chc<C, X>,
			// V>>,C>>, V, S, C>("?", I.S.sig0, retSch, I.S, retMap0);

			return new Pair<>(retSch, retInst);
		}

		private OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V> convert(OplTerm<Chc<C, X>, V> e) {
			if (e.var != null) {
				throw new RuntimeException();
			}
			List<OplTerm<Chc<Chc<C, Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C>>, OplTerm<Chc<C, X>, V>>, V>> list = new LinkedList<>();
			for (OplTerm<Chc<C, X>, V> arg : e.args) {
				list.add(convert(arg));
			}
			if (e.head.left) {
				C c = e.head.l;
				if (I.S.projE().symbols.containsKey(c)) {
					Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C> k = new Triple<>(e.args.get(0), Chc.inRight(e), c);
					return new OplTerm<>(Chc.inLeft(Chc.inRight(k)), list);
				} else if (I.S.projA().symbols.containsKey(c)) {
					S s = I.S.projA().symbols.get(c).second;
					Triple<OplTerm<Chc<C, X>, V>, Chc<S, OplTerm<Chc<C, X>, V>>, C> k = new Triple<>(e.args.get(0), Chc.inLeft(s), c);
					return new OplTerm<>(Chc.inLeft(Chc.inRight(k)), list);

				} else {
					return new OplTerm<>(Chc.inLeft(Chc.inLeft(c)), list);
				}
			}
				if (!list.isEmpty()) {
					throw new RuntimeException();
				}
				return new OplTerm<>(Chc.inRight(e), new LinkedList<>());
			
		}
	}

	// like pivot, iso type side, needs to change
	public static class OplPushoutSch<S, C, V, S1, C1, S2, C2> extends OplExp {

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((s1 == null) ? 0 : s1.hashCode());
			result = prime * result + ((s2 == null) ? 0 : s2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplPushoutSch other = (OplPushoutSch) obj;
			if (s1 == null) {
				if (other.s1 != null)
					return false;
			} else if (!s1.equals(other.s1))
				return false;
			if (s2 == null) {
				if (other.s2 != null)
					return false;
			} else if (!s2.equals(other.s2))
				return false;
			return true;
		}

		final String s1;
        final String s2;
		OplTyMapping<S, C, V, S1, C1> F1;
		OplTyMapping<S, C, V, S2, C2> F2;

		@Override
		public JComponent display() {
			return pushout().display();
		}

		public OplPushoutSch(String s1, String s2) {
			this.s1 = s1;
			this.s2 = s2;
		}

		public void validate(OplTyMapping<S, C, V, S1, C1> F1, OplTyMapping<S, C, V, S2, C2> F2) {
			this.F1 = F1;
			this.F2 = F2;
			if (!F1.src.equals(F2.src)) {
				throw new RuntimeException("Sources do not match:\n\n" + F1.src + "\n\n---------\n\n" + F2.src);
			}
		}

		private Chc<S, Chc<S1, S2>> substSort(S2 s2, S1 s1, Chc<S, Chc<S1, S2>> chc) {
			if (chc.left) {
				return chc;
			}
			Chc<S1, S2> r = chc.r;
			if (r.left) {
				return chc;
			}
			if (r.r.equals(s2)) {
				return Chc.inRight(Chc.inLeft(s1));
			}
			return chc;
		}

		private final Set<Chc<S, Chc<S1, S2>>> entities = new HashSet<>();
		private final Set<Chc<S, Chc<S1, S2>>> sorts = new HashSet<>();
		private Map<Chc<C, Chc<C1, C2>>, Pair<List<Chc<S, Chc<S1, S2>>>, Chc<S, Chc<S1, S2>>>> symbols = new HashMap<>();
		private List<Triple<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>>> equations = new LinkedList<>();
		private final Map<S1, Chc<S, Chc<S1, S2>>> sorts1 = new HashMap<>();
		private final Map<S2, Chc<S, Chc<S1, S2>>> sorts2 = new HashMap<>();
		private final Map<C1, Pair<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>>> symbols1 = new HashMap<>();
		private Map<C2, Pair<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>>> symbols2 = new HashMap<>();

		private void substEdge(C2 c2, Pair<OplCtx<S1, V>, OplTerm<C1, V>> p1) {
			symbols.remove(Chc.inRight(Chc.inRight(c2)));

			OplCtx<Chc<S1, S2>, V> ctx0 = p1.first.inLeft();
			// OplCtx<Chc<S, Chc<S1, S2>>, V> ctx = ctx0.inRight();

			OplTerm<Chc<C1, C2>, V> e1 = p1.second.inLeft();
			// OplTerm<Chc<C, Chc<C1, C2>>, V> e = e1.inRight();

			List<Triple<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>>> equations0 = equations;
			equations = new LinkedList<>();
			for (Triple<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>> eq : equations0) {
				equations.add(new Triple<>(eq.first, substTerm(c2, p1, eq.second), substTerm(c2, p1, eq.third)));
			}
			symbols2.put(c2, new Pair<>(inject1(p1.first), inject1(p1.second)));

		}

		private OplTerm<Chc<C, Chc<C1, C2>>, V> substTerm(C2 c2, Pair<OplCtx<S1, V>, OplTerm<C1, V>> pp, OplTerm<Chc<C, Chc<C1, C2>>, V> term) {
			if (term.var != null) {
				return term;
			}
			List<OplTerm<Chc<C, Chc<C1, C2>>, V>> args = new LinkedList<>();
			for (OplTerm<Chc<C, Chc<C1, C2>>, V> arg : term.args) {
				args.add(substTerm(c2, pp, arg));
			}

			if (term.head.left) {
				return new OplTerm<>(Chc.inLeft(term.head.l), args);
			}
			if (term.head.r.left) {
				return new OplTerm<>(Chc.inRight(Chc.inLeft(term.head.r.l)), args);
			}
			if (!term.head.r.r.equals(c2)) {
				return new OplTerm<>(Chc.inRight(Chc.inRight(term.head.r.r)), args);
			}
			Map<V, OplTerm<Chc<C, Chc<C1, C2>>, V>> map = new HashMap<>();
			int i = 0;
			for (Pair<V, S1> v : pp.first.values2()) {
				map.put(v.first, args.get(i));
				i++;
			}
			return inject1(pp.second).subst(map);
		}

		private OplCtx<Chc<S, Chc<S1, S2>>, V> substCtx(S2 s2, S1 s1, OplCtx<Chc<S, Chc<S1, S2>>, V> ctx) {
			List<Pair<V, Chc<S, Chc<S1, S2>>>> ret = ctx.values2().stream().map(x -> new Pair<>(x.first, substSort(s2, s1, x.second))).collect(Collectors.toList());
			return new OplCtx<>(ret);
		}

		private void substSort(S2 s2, S1 s1) {
			entities.remove(Chc.inRight(Chc.inRight(s2)));
			sorts.remove(Chc.inRight(Chc.inRight(s2)));
			Map<Chc<C, Chc<C1, C2>>, Pair<List<Chc<S, Chc<S1, S2>>>, Chc<S, Chc<S1, S2>>>> symbols0 = symbols;
			symbols = new HashMap<>();
			for (Chc<C, Chc<C1, C2>> c : symbols0.keySet()) {
				Pair<List<Chc<S, Chc<S1, S2>>>, Chc<S, Chc<S1, S2>>> t = symbols0.get(c);
				List<Chc<S, Chc<S1, S2>>> l = t.first.stream().map(x -> substSort(s2, s1, x)).collect(Collectors.toList());
				symbols.put(c, new Pair<>(l, substSort(s2, s1, t.second)));
			}
			List<Triple<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>>> equations0 = equations;
			equations = new LinkedList<>();
			for (Triple<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>> eq : equations0) {
				equations.add(new Triple<>(substCtx(s2, s1, eq.first), eq.second, eq.third));
			}
			sorts2.put(s2, Chc.inRight(Chc.inLeft(s1)));
			Map<C2, Pair<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>>> symbols2x = symbols2;
			symbols2 = new HashMap<>();
			for (C2 c2 : symbols2x.keySet()) {
				Pair<OplCtx<Chc<S, Chc<S1, S2>>, V>, OplTerm<Chc<C, Chc<C1, C2>>, V>> y = symbols2x.get(c2);
				symbols2.put(c2, new Pair<>(substCtx(s2, s1, y.first), y.second));
			}
		}

		// the substituting is sensitive to the order, favoring left over
		// right.
		// really should make canonical things - sorts are equiv classes of
		// sorts, have all edges
		// then, provide separate step to simplify based on some policy, and get
		// explicit map out
		@SuppressWarnings("unlikely-arg-type")
		public OplSchema<Chc<S, Chc<S1, S2>>, Chc<C, Chc<C1, C2>>, V> pushout() {
			for (S s : F1.src.projT().sorts) {
				sorts.add(Chc.inLeft(s));
			}
			for (C c : F1.src.projT().symbols.keySet()) {
				Pair<List<S>, S> t = F1.src.projT().symbols.get(c);
				symbols.put(Chc.inLeft(c), new Pair<>(Chc.inLeft(t.first), Chc.inLeft(t.second)));
			}
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : F1.src.projT().equations) {
				equations.add(new Triple<>(eq.first.inLeft(), eq.second.inLeft(), eq.third.inLeft()));
			}
			for (S1 s1 : F1.dst.entities) {
				entities.add(Chc.inRight(Chc.inLeft(s1)));
				sorts.add(Chc.inRight(Chc.inLeft(s1)));
				sorts1.put(s1, Chc.inRight(Chc.inLeft(s1)));
			}
			for (S2 s2 : F2.dst.entities) {
				entities.add(Chc.inRight(Chc.inRight(s2)));
				sorts.add(Chc.inRight(Chc.inRight(s2)));
				sorts2.put(s2, Chc.inRight(Chc.inRight(s2)));
			}
			for (C1 c1 : F1.dst.projE().symbols.keySet()) {
				Pair<List<S1>, S1> t = F1.dst.projE().symbols.get(c1);
				symbols.put(Chc.inRight(Chc.inLeft(c1)), new Pair<>(Chc.inRight(Chc.inLeft(t.first)), Chc.inRight(Chc.inLeft(t.second))));

				Pair<OplCtx<S1, V>, OplTerm<C1, V>> term = F1.dst.projEA().surround(c1);
				OplCtx<Chc<S1, S2>, V> ctx = term.first.inLeft();
				OplTerm<Chc<C1, C2>, V> term2 = term.second.inLeft();
				symbols1.put(c1, new Pair<>(ctx.inRight(), term2.inRight()));
			}
			for (C1 c1 : F1.dst.projA().symbols.keySet()) {
				Pair<List<S1>, S1> t = F1.dst.projA().symbols.get(c1);
				symbols.put(Chc.inRight(Chc.inLeft(c1)), new Pair<>(Chc.inRight(Chc.inLeft(t.first)), Chc.inLeft((S) t.second)));

				Pair<OplCtx<S1, V>, OplTerm<C1, V>> term = F1.dst.projA().surround(c1);
				OplCtx<Chc<S1, S2>, V> ctx = term.first.inLeft();
				OplTerm<Chc<C1, C2>, V> term2 = term.second.inLeft();
				symbols1.put(c1, new Pair<>(ctx.inRight(), term2.inRight()));
			}
			for (C2 c2 : F2.dst.projE().symbols.keySet()) {
				Pair<List<S2>, S2> t = F2.dst.projE().symbols.get(c2);
				symbols.put(Chc.inRight(Chc.inRight(c2)), new Pair<>(Chc.inRight(Chc.inRight(t.first)), Chc.inRight(Chc.inRight(t.second))));

				Pair<OplCtx<S2, V>, OplTerm<C2, V>> term = F2.dst.projEA().surround(c2);
				OplCtx<Chc<S1, S2>, V> ctx = term.first.inRight();
				OplTerm<Chc<C1, C2>, V> term2 = term.second.inRight();
				symbols2.put(c2, new Pair<>(ctx.inRight(), term2.inRight()));
			}
			for (C2 c2 : F2.dst.projA().symbols.keySet()) {
				Pair<List<S2>, S2> t = F2.dst.projA().symbols.get(c2);
				symbols.put(Chc.inRight(Chc.inRight(c2)), new Pair<>(Chc.inRight(Chc.inRight(t.first)), Chc.inLeft((S) t.second)));

				Pair<OplCtx<S2, V>, OplTerm<C2, V>> term = F2.dst.projA().surround(c2);
				OplCtx<Chc<S1, S2>, V> ctx = term.first.inRight();
				OplTerm<Chc<C1, C2>, V> term2 = term.second.inRight();
				symbols2.put(c2, new Pair<>(ctx.inRight(), term2.inRight()));
			}
			for (Triple<OplCtx<S1, V>, OplTerm<C1, V>, OplTerm<C1, V>> eq : F1.dst.sig.equations) {
				OplCtx<Chc<S, Chc<S1, S2>>, V> ctx = inject1(eq.first);
				OplTerm<Chc<C, Chc<C1, C2>>, V> lhs = inject1(eq.second);
				OplTerm<Chc<C, Chc<C1, C2>>, V> rhs = inject1(eq.third);
				equations.add(new Triple<>(ctx, lhs, rhs));
			}
			for (Triple<OplCtx<S2, V>, OplTerm<C2, V>, OplTerm<C2, V>> eq : F2.dst.sig.equations) {
				OplCtx<Chc<S, Chc<S1, S2>>, V> ctx = inject2(eq.first);
				OplTerm<Chc<C, Chc<C1, C2>>, V> lhs = inject2(eq.second);
				OplTerm<Chc<C, Chc<C1, C2>>, V> rhs = inject2(eq.third);
				equations.add(new Triple<>(ctx, lhs, rhs));
			}
			for (C c : F1.src.projEA().symbols.keySet()) {
				Pair<OplCtx<S1, V>, OplTerm<C1, V>> d = F1.m.symbols.get(c);
				V v = d.first.names().get(0);
				Map<V, Chc<S, Chc<S1, S2>>> vars = new HashMap<>();
				Chc<S, Chc<S1, S2>> x = Chc.inRight(Chc.inLeft(d.first.get(v)));
				vars.put(v, x);
				OplCtx<Chc<S, Chc<S1, S2>>, V> ctx = new OplCtx<>(vars);

				Pair<OplCtx<S2, V>, OplTerm<C2, V>> e = F2.m.symbols.get(c);
				V v2 = e.first.names().get(0);
				Map<V, OplTerm<C2, V>> map = new HashMap<>();
				map.put(v2, new OplTerm<>(v));
				OplTerm<C2, V> rhs = e.second.subst(map);

				equations.add(new Triple<>(ctx, inject1(d.second), inject2(rhs)));
			}

			Map<S2, S1> substed = new HashMap<>();
			for (S s : F1.src.entities) {
				S1 s1 = F1.m.sorts.get(s);
				S2 s2 = F2.m.sorts.get(s);
				substSort(s2, s1);
				substed.put(s2, s1);
			}
			// simplifies the presentation, favoring the left over the right
			if (DefunctGlobalOptions.debug.opl.opl_pushout_simpl) {
				for (C c : F1.src.projEA().symbols.keySet()) {
					Pair<OplCtx<S1, V>, OplTerm<C1, V>> p1 = F1.m.symbols.get(c);
					Pair<OplCtx<S2, V>, OplTerm<C2, V>> p2 = F2.m.symbols.get(c);
					if (p1.first.values2().get(0).second.equals(substed.get(p2.first.values2().get(0).second))) {
						if (p2.second.var != null) {
							continue;
						}
						List<OplTerm<C2, V>> l = Collections.singletonList(new OplTerm<>(p2.first.values2().get(0).first));
						if (!p2.second.args.equals(l)) {
							continue;
						}
						S1 t1 = p1.second.type(F1.dst.sig, p1.first);
						S2 t2 = p2.second.type(F2.dst.sig, p2.first);
						if (F1.src.projT().sorts.contains(t1) || t1.equals(substed.get(t2))) {
							substEdge(p2.second.head, p1);
						}
					}
				}
				equations.removeIf(x -> x.second.equals(x.third));
			}

			OplSchema<Chc<S, Chc<S1, S2>>, Chc<C, Chc<C1, C2>>, V> ret = new OplSchema<>("?", entities);
			OplSig<Chc<S, Chc<S1, S2>>, Chc<C, Chc<C1, C2>>, V> sig = new OplSig<>(F1.src.sig.fr, new HashMap<>(), sorts, symbols, equations);
			ret.validate(sig);

			OplMapping<S1, C1, V, Chc<S, Chc<S1, S2>>, Chc<C, Chc<C1, C2>>> g1 = new OplMapping<>(sorts1, symbols1, "?", "?");
			OplMapping<S2, C2, V, Chc<S, Chc<S1, S2>>, Chc<C, Chc<C1, C2>>> g2 = new OplMapping<>(sorts2, symbols2, "?", "?");

			return ret;
		}

		@SuppressWarnings("unlikely-arg-type")
		private OplTerm<Chc<C, Chc<C1, C2>>, V> inject2(OplTerm<C2, V> term) {
			if (term.var != null) {
				return new OplTerm<>(term.var);
			}
			List<OplTerm<Chc<C, Chc<C1, C2>>, V>> args = new LinkedList<>();
			for (OplTerm<C2, V> arg : term.args) {
				args.add(inject2(arg));
			}

			return F1.src.projT().symbols.containsKey(term.head) ? new OplTerm<>(Chc.inLeft((C) term.head), args) : new OplTerm<>(Chc.inRight(Chc.inRight(term.head)), args);
		}

		@SuppressWarnings("unlikely-arg-type")
		private OplTerm<Chc<C, Chc<C1, C2>>, V> inject1(OplTerm<C1, V> term) {
			if (term.var != null) {
				return new OplTerm<>(term.var);
			}
			List<OplTerm<Chc<C, Chc<C1, C2>>, V>> args = new LinkedList<>();
			for (OplTerm<C1, V> arg : term.args) {
				args.add(inject1(arg));
			}

			return F1.src.projT().symbols.containsKey(term.head) ? new OplTerm<>(Chc.inLeft((C) term.head), args) : new OplTerm<>(Chc.inRight(Chc.inLeft(term.head)), args);
		}

		@SuppressWarnings("unlikely-arg-type")
		private OplCtx<Chc<S, Chc<S1, S2>>, V> inject2(OplCtx<S2, V> ctx) {
			List<Pair<V, Chc<S, Chc<S1, S2>>>> ret = new LinkedList<>();

			for (Pair<V, S2> p : ctx.values2()) {
				if (F1.src.projT().sorts.contains(p.second)) {
					ret.add(new Pair<>(p.first, Chc.inLeft((S) p.second)));
				}
				ret.add(new Pair<>(p.first, Chc.inRight(Chc.inRight(p.second))));
			}

			return new OplCtx<>(ret);
		}

		private OplCtx<Chc<S, Chc<S1, S2>>, V> inject1(OplCtx<S1, V> ctx) {
			List<Pair<V, Chc<S, Chc<S1, S2>>>> ret = new LinkedList<>();

			for (Pair<V, S1> p : ctx.values2()) {
				if (F1.src.projT().sorts.contains((S)p.second)) {
					ret.add(new Pair<>(p.first, Chc.inLeft((S) p.second)));
				}
				ret.add(new Pair<>(p.first, Chc.inRight(Chc.inLeft(p.second))));
			}

			return new OplCtx<>(ret);
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class OplPushoutBen extends OplExp {

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((s1 == null) ? 0 : s1.hashCode());
			result = prime * result + ((s2 == null) ? 0 : s2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplPushoutBen other = (OplPushoutBen) obj;
			if (s1 == null) {
				if (other.s1 != null)
					return false;
			} else if (!s1.equals(other.s1))
				return false;
			if (s2 == null) {
				if (other.s2 != null)
					return false;
			} else if (!s2.equals(other.s2))
				return false;
			return true;
		}

		final String s1;
        final String s2;
		OplMapping<String, String, String, String, String> F1;
		OplMapping<String, String, String, String, String> F2;

		@Override
		public JComponent display() {
			return pushout().display();
		}

		public OplPushoutBen(String s1, String s2) {
			this.s1 = s1;
			this.s2 = s2;
		}

		public void validate(OplMapping<String, String, String, String, String> F1, OplMapping<String, String, String, String, String> F2) {
			this.F1 = F1;
			this.F2 = F2;
			if (!F1.src.equals(F2.src)) {
				throw new RuntimeException("Sources do not match:\n\n" + F1.src + "\n\n---------\n\n" + F2.src);
			}
			if (!Collections.disjoint(F1.dst.sorts, F2.dst.sorts)) {
				throw new RuntimeException("Sorts not disjoint");
			}
			if (!Collections.disjoint(F1.dst.symbols.keySet(), F2.dst.symbols.keySet())) {
				throw new RuntimeException("Symbols not disjoint");
			}
		}

		public OplSig<String, String, String> pushout() {
			Set<String> sorts = new HashSet<>();
			sorts.addAll(F1.dst.sorts);
			sorts.addAll(F2.dst.sorts);

			Map<String, Pair<List<String>, String>> symbols = new HashMap<>();
			symbols.putAll(F1.dst.symbols);
			symbols.putAll(F2.dst.symbols);

			List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> equations = new LinkedList<>();
			equations.addAll(F1.dst.equations);
			equations.addAll(F2.dst.equations);
			for (String e : F1.src.symbols.keySet()) {
				Pair<OplCtx<String, String>, OplTerm<String, String>> x = F1.symbols.get(e);
				Pair<OplCtx<String, String>, OplTerm<String, String>> y = F2.symbols.get(e);

				Map<String, OplTerm<String, String>> m = new HashMap<>();
				int i = 0;
				for (Pair<String, String> k : y.first.values2()) {
					m.put(k.first, new OplTerm<>(x.first.values2().get(i++).first));
				}
				OplTerm<String, String> z = y.second.subst(m);
				equations.add(new Triple<>(x.first, x.second, z));
			}

			for (String s : F1.src.sorts) {
				String s1 = F1.sorts.get(s);
				String s2 = F2.sorts.get(s);
				sorts.remove(s2);
				Map<String, Pair<List<String>, String>> symbols0 = symbols;
				symbols = new HashMap<>();
				for (String c : symbols0.keySet()) {
					Pair<List<String>, String> t = symbols0.get(c);
					List<String> l = t.first.stream().map(x -> x.equals(s2) ? s1 : x).collect(Collectors.toList());
					symbols.put(c, new Pair<>(l, t.second.equals(s2) ? s1 : t.second));
				}
				List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> equations0 = equations;
				equations = new LinkedList<>();
				for (Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>> eq : equations0) {
					List<Pair<String, String>> l = new LinkedList<>();
					for (Pair<String, String> p : eq.first.values2()) {
						l.add(new Pair<>(p.first, p.second.equals(s2) ? s1 : p.second));
					}
					OplCtx<String, String> ctx2 = new OplCtx<>(l);
					equations.add(new Triple<>(ctx2, eq.second, eq.third));
				}
			}

			OplSig<String, String, String> ret = new OplSig<>(F1.src.fr, new HashMap<>(), sorts, symbols, equations);
			ret.validate();

			return ret;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class OplPushout<S, C, V, X, Y, Z> extends OplExp {

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((s1 == null) ? 0 : s1.hashCode());
			result = prime * result + ((s2 == null) ? 0 : s2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplPushout other = (OplPushout) obj;
			if (s1 == null) {
				if (other.s1 != null)
					return false;
			} else if (!s1.equals(other.s1))
				return false;
			if (s2 == null) {
				if (other.s2 != null)
					return false;
			} else if (!s2.equals(other.s2))
				return false;
			return true;
		}

		final String s1;
        final String s2;
		OplPresTrans<S, C, V, X, Y> h1;
		OplPresTrans<S, C, V, X, Z> h2;

		@Override
		public JComponent display() {
			return pushout().first.display();
		}

		public OplPushout(String s1, String s2) {
			this.s1 = s1;
			this.s2 = s2;
		}

		public void validate(OplPresTrans<S, C, V, X, Y> h1, OplPresTrans<S, C, V, X, Z> h2) {
			this.h1 = h1;
			this.h2 = h2;
			if (!h1.src.equals(h2.src)) {
				throw new RuntimeException("Sources do not match:\n\n" + h1.src + "\n\n---------\n\n" + h2.src);
			}
		}

		// : in colimit, should do 0-based assignments
		public Triple<OplInst<S, C, V, Chc<Y, Z>>, OplPresTrans<S, C, V, Y, Chc<Y, Z>>, OplPresTrans<S, C, V, Z, Chc<Y, Z>>> pushout() {
			Map<Chc<Y, Z>, Integer> prec = new HashMap<>();
			Map<Chc<Y, Z>, S> gens = new HashMap<>();
			List<Pair<OplTerm<Chc<C, Chc<Y, Z>>, V>, OplTerm<Chc<C, Chc<Y, Z>>, V>>> eqs = new LinkedList<>();

			Map<S, Map<Y, OplTerm<Chc<C, Chc<Y, Z>>, V>>> map1 = new HashMap<>();
			Map<S, Map<Z, OplTerm<Chc<C, Chc<Y, Z>>, V>>> map2 = new HashMap<>();
			Map<S, Map<Y, OplTerm<Chc<C, Chc<Y, Z>>, V>>> ytm = new HashMap<>();
			Map<S, Map<Z, OplTerm<Chc<C, Chc<Y, Z>>, V>>> ztm = new HashMap<>();

			for (S s : h1.dst.sig.sorts) {
				map1.put(s, new HashMap<>());
				map2.put(s, new HashMap<>());
				ytm.put(s, new HashMap<>());
				ztm.put(s, new HashMap<>());
			}
			int precIdx = 0;
			for (Y y : h1.dst.gens.keySet()) {
				S s = h1.dst.gens.get(y);
				gens.put(Chc.inLeft(y), s);
				OplTerm<Chc<C, Chc<Y, Z>>, V> term = new OplTerm<>(Chc.inRight(Chc.inLeft(y)), new LinkedList<>());
				map1.get(s).put(y, term);
				ytm.get(s).put(y, term);
				if (DefunctGlobalOptions.debug.opl.opl_prover_force_prec) {
					prec.put(Chc.inLeft(y), precIdx++);
				}
			}
			for (Z z : h2.dst.gens.keySet()) {
				S s = h2.dst.gens.get(z);
				gens.put(Chc.inRight(z), h2.dst.gens.get(z));
				OplTerm<Chc<C, Chc<Y, Z>>, V> term = new OplTerm<>(Chc.inRight(Chc.inRight(z)), new LinkedList<>());
				map2.get(s).put(z, term);
				ztm.get(s).put(z, term);
			}
			for (Pair<OplTerm<Chc<C, Y>, V>, OplTerm<Chc<C, Y>, V>> eq : h1.dst.equations) {
				OplTerm<Chc<C, Chc<Y, Z>>, V> lhs = OplPresTrans.apply(h1.dst, map1, eq.first);
				OplTerm<Chc<C, Chc<Y, Z>>, V> rhs = OplPresTrans.apply(h1.dst, map1, eq.second);
				eqs.add(new Pair<>(lhs, rhs));
			}
			for (Pair<OplTerm<Chc<C, Z>, V>, OplTerm<Chc<C, Z>, V>> eq : h2.dst.equations) {
				OplTerm<Chc<C, Chc<Y, Z>>, V> lhs = OplPresTrans.apply(h2.dst, map2, eq.first);
				OplTerm<Chc<C, Chc<Y, Z>>, V> rhs = OplPresTrans.apply(h2.dst, map2, eq.second);
				eqs.add(new Pair<>(lhs, rhs));
			}
			for (X x : h1.src.gens.keySet()) {
				OplTerm<Chc<C, Y>, V> y = h1.map.get(h1.src.gens.get(x)).get(x);
				OplTerm<Chc<C, Z>, V> z = h2.map.get(h2.src.gens.get(x)).get(x);
				OplTerm<Chc<C, Chc<Y, Z>>, V> lhs = OplPresTrans.apply(h1.dst, map1, y);
				OplTerm<Chc<C, Chc<Y, Z>>, V> rhs = OplPresTrans.apply(h2.dst, map2, z);
				eqs.add(new Pair<>(lhs, rhs));
			}

			OplPres<S, C, V, Chc<Y, Z>> P = new OplPres<>(prec, h1.dst1.S0, h1.src.sig, gens, new LinkedList<>(new HashSet<>(eqs)));
			OplInst<S, C, V, Chc<Y, Z>> ret = new OplInst<>(h1.dst1.S0, "?", "?");
			ret.validate(h1.src1.S, P, h1.src1.J);
			OplPresTrans<S, C, V, Y, Chc<Y, Z>> yt = new OplPresTrans<>(ytm, "?", "?", h1.dst, P);
			yt.validateNotReally(h1.dst1, ret);
			OplPresTrans<S, C, V, Z, Chc<Y, Z>> zt = new OplPresTrans<>(ztm, "?", "?", h2.dst, P);
			zt.validateNotReally(h2.dst1, ret);

			return new Triple<>(ret, yt, zt);
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class OplFlower<S, C, V, X, Z> extends OplExp {
		final Map<Z, OplTerm<C, V>> select;
		final Map<V, S> from;
		final List<Pair<OplTerm<C, V>, OplTerm<C, V>>> where;
		OplSetInst<S, C, X> I;
		final String I0;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I0 == null) ? 0 : I0.hashCode());
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((select == null) ? 0 : select.hashCode());
			result = prime * result + ((where == null) ? 0 : where.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplFlower other = (OplFlower) obj;
			if (I0 == null) {
				if (other.I0 != null)
					return false;
			} else if (!I0.equals(other.I0))
				return false;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (select == null) {
				if (other.select != null)
					return false;
			} else if (!select.equals(other.select))
				return false;
			if (where == null) {
				if (other.where != null)
					return false;
			} else if (!where.equals(other.where))
				return false;
			return true;
		}

		public OplFlower(Map<Z, OplTerm<C, V>> select, Map<V, S> from, List<Pair<OplTerm<C, V>, OplTerm<C, V>>> where, String i0) {
			this.select = select;
			this.from = from;
			this.where = where;
			I0 = i0;
		}

		public void validate() {
			OplSig<S, C, V> sig = (OplSig<S, C, V>) I.sig0;

			for (V v : from.keySet()) {
				S s = from.get(v);
				if (!sig.sorts.contains(s)) {
					throw new RuntimeException("Bad sort: " + s);
				}
			}
			OplCtx<S, V> ctx = new OplCtx<>(from);
			for (Pair<OplTerm<C, V>, OplTerm<C, V>> eq : where) {
				eq.first.type(sig, ctx);
				eq.second.type(sig, ctx);
			}
			for (Z z : select.keySet()) {
				OplTerm<C, V> e = select.get(z);
				e.type(sig, ctx);
			}
		}

		public OplSetTrans<String, String, String> eval(OplSetTrans<S, C, X> h) {
			OplSetInst<S, C, X> I = h.src0;
			OplSetInst<S, C, X> J = h.dst0;

			Pair<Map<Integer, OplCtx<X, Z>>, OplSetInst<String, String, String>> I0X = eval(I);
			Pair<Map<Integer, OplCtx<X, Z>>, OplSetInst<String, String, String>> J0X = eval(J);
			Map<Integer, OplCtx<X, Z>> Im = I0X.first;
			Map<Integer, OplCtx<X, Z>> Jm = J0X.first;
			OplSetInst<String, String, String> QI = I0X.second;
			OplSetInst<String, String, String> QJ = J0X.second;

			OplSig<S, C, V> sig = (OplSig<S, C, V>) I.sig0;
			OplCtx<S, V> ctx = new OplCtx<>(from);

			Map<String, Map<String, String>> sorts = new HashMap<>();
			for (S k : I.sig0.sorts) {
				if (k.toString().equals("_Q")) {
					continue;
				}
				Set<X> X = I.sorts.get(k);
				Map<String, String> m = new HashMap<>();
				for (X x : X) {
					m.put(x.toString(), h.sorts.get(k).get(x).toString());
				}
				sorts.put(k.toString(), m);
			}
			Map<String, String> m = new HashMap<>();
			for (Integer i : Im.keySet()) {
				OplCtx<X, Z> t = Im.get(i);
				Map<Z, X> n = new HashMap<>();
				for (Z z : t.vars0.keySet()) {
					X x = t.vars0.get(z);
					S s = select.get(z).type(sig, ctx);
					X x2 = h.sorts.get(s).get(x);
					n.put(z, x2);
				}
				OplCtx<X, Z> t2 = new OplCtx<>(n);
				Integer j = Util.revLookup(Jm, t2);
				if (j == null) {
					throw new RuntimeException("Not found: " + t2 + " in " + Jm);
				}
				m.put("_" + Integer.toString(i), "_" + Integer.toString(j));
			}
			sorts.put("_Q", m);

			OplSetTrans<String, String, String> ret = new OplSetTrans<>(sorts, "?", "?");
			ret.validate(QI.sig0, QI, QJ);
			return ret;
		}

		public Pair<Map<Integer, OplCtx<X, Z>>, OplSetInst<String, String, String>> eval(OplSetInst<S, C, X> I) {
			this.I = I;
			validate();

			OplSig<S, C, V> sig = (OplSig<S, C, V>) I.sig0;
			OplCtx<S, V> ctx = new OplCtx<>(from);

			Set<OplCtx<X, V>> tuples = new HashSet<>();
			tuples.add(new OplCtx<>());

			for (V v : from.keySet()) {
				S s = from.get(v);
				Set<X> dom = I.sorts.get(s);
				tuples = extend(tuples, dom, v);
				tuples = filter(tuples, where, I);
			}
			if (from.keySet().isEmpty()) {
				tuples = filter(tuples, where, I);
			}

			Set<String> ret_sorts = new HashSet<>();
			for (S c : sig.sorts) {
				ret_sorts.add(c.toString());
			}
			ret_sorts.add("_Q");
			Map<String, Pair<List<String>, String>> ret_symbols = new HashMap<>();
			for (Z z : select.keySet()) {
				OplTerm<C, V> k = select.get(z);
				ret_symbols.put(z.toString(), new Pair<>(Collections.singletonList("_Q"), k.type(sig, ctx).toString()));
			}
			OplSig<String, String, V> ret_sig = new OplSig<>(sig.fr, new HashMap<>(), ret_sorts, ret_symbols, new LinkedList<>());

			Map<String, Set<String>> ret_sorts2 = new HashMap<>();
			Set<String> projected = new HashSet<>();
			Map<String, Map<List<String>, String>> ret_symbols2 = new HashMap<>();
			Map<Integer, OplCtx<X, Z>> m = new HashMap<>();
			int i = 0;
			for (OplCtx<X, V> env : tuples) {
				Map<Z, X> tuple = new HashMap<>();
				for (Z z : select.keySet()) {
					tuple.put(z, select.get(z).eval(sig, I, env));
				}
				projected.add("_" + Integer.toString(i));
				m.put(i, new OplCtx<>(tuple));
				i++;
			}
			ret_sorts2.put("_Q", projected);
			for (S s : sig.sorts) {
				ret_sorts2.put(s.toString(), I.sorts.get(s).stream().map(Object::toString).collect(Collectors.toSet()));
			}

			for (Z z : select.keySet()) {
				Map<List<String>, String> n = new HashMap<>();
				for (int j = 0; j < i; j++) {
					OplCtx<X, Z> tuple = m.get(j);
					n.put(Collections.singletonList("_" + j), tuple.get(z).toString());
				}
				ret_symbols2.put(z.toString(), n);
			}

			OplSetInst<String, String, String> ret = new OplSetInst<>(ret_sorts2, ret_symbols2, "?");
			ret.validate(ret_sig);
			return new Pair<>(m, ret);
		}

		private static <X, C, S, V> Set<OplCtx<X, V>> filter(Set<OplCtx<X, V>> tuples, List<Pair<OplTerm<C, V>, OplTerm<C, V>>> where, OplSetInst<S, C, X> I) {
			Set<OplCtx<X, V>> ret = new HashSet<>();
			outer: for (OplCtx<X, V> tuple : tuples) {
				for (Pair<OplTerm<C, V>, OplTerm<C, V>> eq : where) {
					if (eq.first.isGround(tuple) && eq.second.isGround(tuple)) {
						X l = eq.first.eval((OplSig<S, C, V>) I.sig0, I, tuple);
						X r = eq.second.eval((OplSig<S, C, V>) I.sig0, I, tuple);
						if (!l.equals(r)) {
							continue outer;
						}
					}
				}
				ret.add(tuple);
			}
			return ret;
		}

		private static <X, V> Set<OplCtx<X, V>> extend(Set<OplCtx<X, V>> tuples, Set<X> dom, V v) {
			Set<OplCtx<X, V>> ret = new HashSet<>();

			for (OplCtx<X, V> tuple : tuples) {
				for (X x : dom) {
					Map<V, X> m = new HashMap<>(tuple.vars0);
					m.put(v, x);
					OplCtx<X, V> new_tuple = new OplCtx<>(m);
					ret.add(new_tuple);
				}
			}

			return ret;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class OplUnSat extends OplExp {

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplUnSat other = (OplUnSat) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		final String I;

		public OplUnSat(String I) {
			this.I = I;
		}

		// to use with saturate_easy, should turn back into theory
		// this type signature is totally wrong, is not inverse of
		// saturate_easy
		// not sure what this is, but can only desaturate things that were
		// first saturted, not arbitrary models
		public static <S, C, V, X> OplPres<S, C, V, X> desaturate(String S, OplSetInst<S, C, X> I) {
			OplSig<S, C, V> sig = (OplSig<S, C, V>) I.sig0;

			Map<X, S> gens = new HashMap<>();
			for (S s : sig.sorts) {
				for (X c : I.sorts.get(s)) {
					gens.put(c, s);
				}
			}

			List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> equations = new LinkedList<>();
			for (C f : sig.symbols.keySet()) {
				List<S> arg_ts = sig.symbols.get(f).first;
				if (arg_ts.isEmpty()) {
					continue;
				}
				Map<Integer, List<X>> l = new HashMap<>();
				int i = 0;
				for (S t : arg_ts) {
					l.put(i, new LinkedList<>(I.sorts.get(t)));
					i++;
				}
				List<LinkedHashMap<Integer, X>> m = FinSet.homomorphs(l);
				for (LinkedHashMap<Integer, X> a : m) {
					List<X> arg1 = new LinkedList<>();
					List<OplTerm<Chc<C, X>, V>> arg2 = new LinkedList<>();
					for (int j = 0; j < i; j++) {
						arg1.add(a.get(j));
						arg2.add(new OplTerm<>(Chc.inRight(a.get(j)), new LinkedList<>()));
					}
					OplTerm<Chc<C, X>, V> termA = new OplTerm<>(Chc.inLeft(f), arg2);
					OplTerm<Chc<C, X>, V> termB = new OplTerm<>(Chc.inRight(I.symbols.get(f).get(arg1)), new LinkedList<>());
					equations.add(new Pair<>(termA, termB));
				}
			}

			OplPres<S, C, V, X> ret = new OplPres<>(new HashMap<>(), S, sig, gens, equations);
			return ret;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class OplUberSat extends OplExp {

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((P == null) ? 0 : P.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplUberSat other = (OplUberSat) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (P == null) {
				if (other.P != null)
					return false;
			} else if (!P.equals(other.P))
				return false;
			return true;
		}

		final String I;
        final String P;

		public OplUberSat(String I, String P) {
			this.I = I;
			this.P = P;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		private static <S, C, V, X, Y> OplSetInst<S, C, KBExp<Chc<Chc<C, X>, JSWrapper>, V>> inject(OplSetInst<S, C, OplTerm<Chc<C, X>, V>> I, OplJavaInst I0) {
			Map<S, Set<KBExp<Chc<Chc<C, X>, JSWrapper>, V>>> sorts = new HashMap<>();
			for (S s : I.sorts.keySet()) {
				Set<KBExp<Chc<Chc<C, X>, JSWrapper>, V>> set = new HashSet<>();
				for (OplTerm<Chc<C, X>, V> e : I.sorts.get(s)) {
					OplTerm<Chc<Chc<C, X>, JSWrapper>, V> kkk = e.inLeft();
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> jjj = OplToKB.convert(kkk);
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> z = OplToKB.redBy(I0, jjj);
					set.add(z);
				}
				sorts.put(s, set);
			}

			Map<C, Map<List<KBExp<Chc<Chc<C, X>, JSWrapper>, V>>, KBExp<Chc<Chc<C, X>, JSWrapper>, V>>> symbols = new HashMap<>();
			for (C c : I.symbols.keySet()) {
				Map<List<OplTerm<Chc<C, X>, V>>, OplTerm<Chc<C, X>, V>> m = I.symbols.get(c);
				Map<List<KBExp<Chc<Chc<C, X>, JSWrapper>, V>>, KBExp<Chc<Chc<C, X>, JSWrapper>, V>> n = new HashMap<>();
				for (List<OplTerm<Chc<C, X>, V>> a : m.keySet()) {
					List<KBExp<Chc<Chc<C, X>, JSWrapper>, V>> l = new LinkedList<>();
					for (OplTerm<Chc<C, X>, V> e : a) {
						OplTerm<Chc<Chc<C, X>, JSWrapper>, V> kkk = e.inLeft();
						KBExp<Chc<Chc<C, X>, JSWrapper>, V> jjj = OplToKB.convert(kkk);
						KBExp<Chc<Chc<C, X>, JSWrapper>, V> z = OplToKB.redBy(I0, jjj);
						l.add(z);
					}
					OplTerm<Chc<Chc<C, X>, JSWrapper>, V> kkk = m.get(a).inLeft();
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> jjj = OplToKB.convert(kkk);
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> z = OplToKB.redBy(I0, jjj);
					n.put(l, z);
				}
				symbols.put(c, n);
			}

			OplSetInst<S, C, KBExp<Chc<Chc<C, X>, JSWrapper>, V>> ret = new OplSetInst<>(sorts, symbols, I.sig);
			ret.validate(I.sig0);
			return ret;
		}

		public static <S, C, V, X> OplSetInst<S, C, KBExp<Chc<Chc<C, X>, JSWrapper>, V>> saturate(OplJavaInst I0, OplPres<S, C, V, X> P0) {
			OplSetInst<S, C, OplTerm<Chc<C, X>, V>> I = OplSat.saturate(P0);
			OplSetInst<S, C, KBExp<Chc<Chc<C, X>, JSWrapper>, V>> J = inject(I, I0);

			return J;
		}

	}

	public static class OplSat extends OplExp {

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSat other = (OplSat) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		final String I;

		public OplSat(String I) {
			this.I = I;
		}

		// works fine
		// public static <S, C, V, X> OplSetInst<S, Chc<C, X>, OplTerm<Chc<C,
		// X>, V>> saturateEasy(Thread[] threads, OplPres<S, C, V, X> P) {
		// OplSig<S, Chc<C, X>, V> sig = P.toSig();
		// return sig.saturate(P.S);
		// }

		public static <S, C, V, X> OplSetInst<S, C, OplTerm<Chc<C, X>, V>> saturate(OplPres<S, C, V, X> P) { // P
																												// is
																												// projEA
			OplSig<S, Chc<C, X>, V> sig = P.toSig();
			OplToKB<S, Chc<C, X>, V> kb = sig.getKB();

			Map<S, Set<OplTerm<Chc<C, X>, V>>> sorts = kb.doHoms();

			Map<C, Map<List<OplTerm<Chc<C, X>, V>>, OplTerm<Chc<C, X>, V>>> symbols = new HashMap<>();
			for (C f : P.sig.symbols.keySet()) {
				Pair<List<S>, S> ty = P.sig.symbols.get(f);
				Map<Integer, List<OplTerm<Chc<C, X>, V>>> args = new HashMap<>();
				int i = 0;
				for (S t : ty.first) {
					args.put(i++, new LinkedList<>(sorts.get(t)));
				}
				List<LinkedHashMap<Integer, OplTerm<Chc<C, X>, V>>> cands = FinSet.homomorphs(args);
				Map<List<OplTerm<Chc<C, X>, V>>, OplTerm<Chc<C, X>, V>> out = new HashMap<>();
				for (LinkedHashMap<Integer, OplTerm<Chc<C, X>, V>> cand : cands) {
					List<OplTerm<Chc<C, X>, V>> actual = new LinkedList<>();
					for (int j = 0; j < i; j++) {
						actual.add(cand.get(j));
					}
					OplTerm<Chc<C, X>, V> to_red = new OplTerm<>(Chc.inLeft(f), actual);
					OplTerm<Chc<C, X>, V> red = OplToKB.convert(kb.nf(OplToKB.convert(to_red)));
					out.put(actual, red);
				}
				symbols.put(f, out);
			}

			OplSetInst<S, C, OplTerm<Chc<C, X>, V>> ret = new OplSetInst<>(sorts, symbols, P.S);
			ret.validate(P.sig);
			return ret;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class OplColim extends OplExp {
		final String name;
		final String base;
		final Map<String, OplObject> compiled = new HashMap<>();

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public OplColim(String name, String base) {
			this.name = name;
			this.base = base;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((base == null) ? 0 : base.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplColim other = (OplColim) obj;
			if (base == null) {
				if (other.base != null)
					return false;
			} else if (!base.equals(other.base))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public JComponent display() {
			if (compiled.containsKey("ColimitInstance")) {
				JTabbedPane ret = new JTabbedPane();
				CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
				ret.add("Text", p);
				ret.add("Tables", compiled.get("ColimitInstance").display());
				return ret;
			}
			return super.display();
		}

		@Override
		public String toString() {
			String ret = "";
			for (String k : compiled.keySet()) {
				ret += k + " = " + compiled.get(k) + "\n\n";
			}
			return ret;
		}

	}

	public static class OplSCHEMA0<S, C, V> extends OplExp {
		public Map<C, Integer> prec;
		public final Set<S> entities;
		public final Map<C, Pair<List<S>, S>> edges;
        public final Map<C, Pair<List<S>, S>> attrs;
		public final List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> pathEqs;
        public final List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> obsEqs;
		public String typeSide;
		public final Set<S> initEntities;

		public final List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> pathEqsInit;
        public final List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> obsEqsInit;
		Set<S> imports = new HashSet<>();

		public OplSCHEMA0(Map<C, Integer> prec, Set<S> entities, Map<C, Pair<List<S>, S>> edges, Map<C, Pair<List<S>, S>> attrs, List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> pathEqs, List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> obsEqs, String typeSide) {
			this.prec = prec;
			this.entities = entities;
			this.edges = edges;
			this.attrs = attrs;
			this.pathEqs = pathEqs;
			this.obsEqs = obsEqs;
			this.typeSide = typeSide;
			initEntities = new HashSet<>(entities);
			pathEqsInit = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> x : pathEqs) {
				pathEqsInit.add(new Triple<>(new OplCtx<>(x.first.vars0), x.second, x.third));
			}
			obsEqsInit = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> x : obsEqs) {
				obsEqsInit.add(new Triple<>(new OplCtx<>(x.first.vars0), x.second, x.third));
			}
		}

		public void validate(OplSig<S, C, V> sig) {
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : pathEqs) {
				if (eq.first.vars0.keySet().size() != 1) {
					throw new RuntimeException("Non-1 context size for " + eq);
				}
				if (!entities.contains(eq.first.values().get(0))) {
					throw new RuntimeException("Non-entity in context for " + eq);
				}
				S lhs_t = eq.second.type(sig, eq.first);
				S rhs_t = eq.third.type(sig, eq.first);
				if (!entities.contains(lhs_t) || !entities.contains(rhs_t)) {
					throw new RuntimeException("Path equalities must end at an entity and in " + eq + " ends at " + lhs_t + " " + rhs_t);
				}
			}
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : obsEqs) {
				if (eq.first.vars0.keySet().size() != 1) {
					throw new RuntimeException("Non-1 context size for " + eq);
				}
				if (!entities.contains(eq.first.values().get(0))) {
					throw new RuntimeException("Non-entity in context for " + eq);
				}
				S lhs_t = eq.second.type(sig, eq.first);
				S rhs_t = eq.third.type(sig, eq.first);
				if (entities.contains(lhs_t) || entities.contains(rhs_t)) {
					throw new RuntimeException("Obs equalities must end at type and in " + eq + " ends at " + lhs_t + " " + rhs_t);
				}
			}

		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((attrs == null) ? 0 : attrs.hashCode());
			result = prime * result + ((edges == null) ? 0 : edges.hashCode());
			result = prime * result + ((imports == null) ? 0 : imports.hashCode());
			result = prime * result + ((initEntities == null) ? 0 : initEntities.hashCode());
			result = prime * result + ((obsEqsInit == null) ? 0 : obsEqsInit.hashCode());
			result = prime * result + ((pathEqsInit == null) ? 0 : pathEqsInit.hashCode());
			result = prime * result + ((prec == null) ? 0 : prec.hashCode());
			result = prime * result + ((typeSide == null) ? 0 : typeSide.hashCode());
			return result;
		}

		@Override
		public String toString() {
			String ret = "\tentities\n";
			List<String> sorts0 = entities.stream().map(x -> Util.maybeQuote(OplTerm.strip(x.toString()))).collect(Collectors.toList());
			ret += "\t\t" + Util.sep(sorts0, ", ") + ";\n";

			List<String> slist = new LinkedList<>();
			for (C k : edges.keySet()) {
				Pair<List<S>, S> v = edges.get(k);
				String s;
				if (v.first.isEmpty()) {
					s = Util.maybeQuote(OplTerm.strip(k.toString())) + " : " + Util.maybeQuote(OplTerm.strip(v.second.toString()));
				} else {
					List<String> f = v.first.stream().map(x -> Util.maybeQuote(OplTerm.strip(x.toString()))).collect(Collectors.toList());
					s = Util.maybeQuote(OplTerm.strip(k.toString())) + " : " + Util.sep(f, ", ") + " -> " + Util.maybeQuote(OplTerm.strip(v.second.toString()));
				}
				slist.add(s);
			}
			List<String> slist2 = new LinkedList<>();
			for (C k : attrs.keySet()) {
				Pair<List<S>, S> v = attrs.get(k);
				String s;
				if (v.first.isEmpty()) {
					s = Util.maybeQuote(OplTerm.strip(k.toString())) + " : " + Util.maybeQuote(OplTerm.strip(v.second.toString()));
				} else {
					List<String> f = v.first.stream().map(x -> Util.maybeQuote(OplTerm.strip(x.toString()))).collect(Collectors.toList());
					s = Util.maybeQuote(OplTerm.strip(k.toString())) + " : " + Util.sep(f, ", ") + " -> " + Util.maybeQuote(OplTerm.strip(v.second.toString()));
				}
				slist2.add(s);
			}
			Comparator<String> comp = (o1, o2) -> {
                int i = o1.indexOf(":");
                int j = o2.indexOf(":");
                if (i == -1 || j == -1) {
                    return o1.compareTo(o2);
                }
                int z = o1.substring(i).compareTo(o2.substring(j));
                if (z == 0) {
                    return o1.compareTo(o2);
                }
                return z;
            };
			slist.sort(comp);
			slist2.sort(comp);
			ret += "\tedges\n";
			ret += "\t\t" + Util.sep(slist, ",\n\t\t") + ";\n";
			ret += "\tattributes\n";
			ret += "\t\t" + Util.sep(slist2, ",\n\t\t") + ";\n";

			ret += "\tpathEqualities\n";
			List<String> elist = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> k : pathEqs) {
				String z = k.first.vars0.isEmpty() ? "" : "forall ";
				String y = k.first.vars0.isEmpty() ? "" : ". ";
				String s = z + k.first + y + OplTerm.strip(k.second.toString()) + " = " + OplTerm.strip(k.third.toString());
				elist.add(s);
			}
			elist.sort(comp);
			ret += "\t\t" + Util.sep(elist, ",\n\t\t") + ";\n";

			ret += "\tobsEqualities\n";
			List<String> elist2 = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> k : obsEqs) {
				String z = k.first.vars0.isEmpty() ? "" : "forall ";
				String y = k.first.vars0.isEmpty() ? "" : ". ";
				String s = z + k.first + y + OplTerm.strip(k.second.toString()) + " = " + OplTerm.strip(k.third.toString());
				elist2.add(s);
			}
			elist2.sort(comp);
			ret += "\t\t" + Util.sep(elist2, ",\n\t\t") + ";\n";

			return "SCHEMA {\n" + ret + "} : " + typeSide; // + "\n\nprec: " +
															// prec;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSCHEMA0 other = (OplSCHEMA0) obj;
			if (attrs == null) {
				if (other.attrs != null)
					return false;
			} else if (!attrs.equals(other.attrs))
				return false;
			if (edges == null) {
				if (other.edges != null)
					return false;
			} else if (!edges.equals(other.edges))
				return false;
			if (imports == null) {
				if (other.imports != null)
					return false;
			} else if (!imports.equals(other.imports))
				return false;
			if (initEntities == null) {
				if (other.initEntities != null)
					return false;
			} else if (!initEntities.equals(other.initEntities))
				return false;
			if (obsEqsInit == null) {
				if (other.obsEqsInit != null)
					return false;
			} else if (!obsEqsInit.equals(other.obsEqsInit))
				return false;
			if (pathEqsInit == null) {
				if (other.pathEqsInit != null)
					return false;
			} else if (!pathEqsInit.equals(other.pathEqsInit))
				return false;
			if (prec == null) {
				if (other.prec != null)
					return false;
			} else if (!prec.equals(other.prec))
				return false;
			if (typeSide == null) {
				if (other.typeSide != null)
					return false;
			} else if (!typeSide.equals(other.typeSide))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class OplSig<S, C, V> extends OplExp {

		private OplToKB<S, C, V> kb;

		public Set<String> imports = new HashSet<>();

		public OplToKB<S, C, V> getKB() {
			if (kb != null) {
				return kb;
			}
			kb = new OplToKB<>(fr, this);
			return kb;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((equations == null) ? 0 : equations.hashCode());
			result = prime * result + ((implications == null) ? 0 : implications.hashCode());
			result = prime * result + ((imports == null) ? 0 : imports.hashCode());
			result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
			result = prime * result + ((symbols == null) ? 0 : symbols.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSig other = (OplSig) obj;
			if (equations == null) {
				if (other.equations != null)
					return false;
			} else if (!equations.equals(other.equations))
				return false;
			if (implications == null) {
				if (other.implications != null)
					return false;
			} else if (!implications.equals(other.implications))
				return false;
			if (imports == null) {
				if (other.imports != null)
					return false;
			} else if (!imports.equals(other.imports))
				return false;
			if (sorts == null) {
				if (other.sorts != null)
					return false;
			} else if (!sorts.equals(other.sorts))
				return false;
			if (symbols == null) {
				if (other.symbols != null)
					return false;
			} else if (!symbols.equals(other.symbols))
				return false;
			return true;
		}

		public Pair<OplCtx<S, V>, OplTerm<C, V>> surround(C c) {
			List<S> argts = symbols.get(c).first;
			List<Pair<V, S>> l = new LinkedList<>();
			List<OplTerm<C, V>> args = new LinkedList<>();
			for (S s : argts) {
				V v = fr.next();
				l.add(new Pair<>(v, s));
				args.add(new OplTerm<>(v));
			}
			OplTerm<C, V> ret = new OplTerm<>(c, args);
			OplCtx<S, V> ret2 = new OplCtx<>(l);
			return new Pair<>(ret2, ret);
		}

		public <X> OplSig<S, Chc<C, X>, V> inject() {
			Map<Chc<C, X>, Pair<List<S>, S>> symbols0 = new HashMap<>();
			Map<Chc<C, X>, Integer> prec0 = new HashMap<>();
			for (C f : symbols.keySet()) {
				Pair<List<S>, S> s = symbols.get(f);
				symbols0.put(Chc.inLeft(f), s);
				if (prec.containsKey(f)) {
					prec0.put(Chc.inLeft(f), prec.get(f));
				}
			}
			List<Triple<OplCtx<S, V>, OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> equations0 = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : equations) {
				equations0.add(new Triple<>(eq.first, eq.second.inLeft(), eq.third.inLeft()));
			}
			List<Triple<OplCtx<S, V>, List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>>, List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>>>> implications0 = new LinkedList<>();
			for (Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>> impl : implications) {
				List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> lhs = new LinkedList<>();
				List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> rhs = new LinkedList<>();
				for (Pair<OplTerm<C, V>, OplTerm<C, V>> l : impl.second) {
					lhs.add(new Pair<>(l.first.inLeft(), l.second.inLeft()));
				}
				for (Pair<OplTerm<C, V>, OplTerm<C, V>> r : impl.third) {
					rhs.add(new Pair<>(r.first.inLeft(), r.second.inLeft()));
				}
				implications0.add(new Triple<>(impl.first, lhs, rhs));
			}
			return new OplSig<>(fr, prec0, sorts, symbols0, equations0, implications0);
		}

		@Override
		public JComponent display() {
			return display(true);
		}

		public JComponent display(Boolean b) {
			JTabbedPane ret = new JTabbedPane();

			CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
			ret.add(p, "Text");

			try {
				JPanel pp = makeReducer(b);
				ret.add(pp, "KB");
				try {
					JPanel qq = makeHomSet();
					ret.add(qq, "Hom");
				} catch (Exception ex) {
					ex.printStackTrace();
					p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "exception: " + ex.getMessage());
					ret.add(p, "Hom");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "exception: " + ex.getMessage());
				ret.add(p, "KB");
			}

			return ret;
		}

		JPanel makeReducer(boolean b) {
			OplToKB kb = getKB();
			JPanel ret = new JPanel(new GridLayout(1, 1));
			JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			ret.add(pane);

			JPanel top = new JPanel();

			JTextField src = new JTextField(16);
			JTextField dst = new JTextField(16);

			JButton go = new JButton("Reduce");
			go.addActionListener(x -> {
				try {
					OplTerm t = OplParser.parse_term(symbols, src.getText());
					dst.setText(kb.nf(OplToKB.convert(t)).toString());
				} catch (Exception ex) {
					dst.setText(ex.getMessage());
				}
			});

			top.add(new JLabel("Input term:"));
			top.add(src);
			top.add(go);
			top.add(new JLabel("Result:"));
			top.add(dst);

			CodeTextPanel bot = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Re-write rules", OplTerm.strip(kb.printKB()));

			pane.add(top);
			pane.add(bot);

			return b ? ret : bot;

		}

		@SuppressWarnings("deprecation")
		JPanel makeHomSet() {
			OplToKB kb = getKB();
			JPanel ret = new JPanel(new GridLayout(1, 1));
			JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			ret.add(pane);

			JPanel top = new JPanel(new GridLayout(3, 1));
			JPanel p1 = new JPanel();
			JPanel p2 = new JPanel();
			JPanel p3 = new JPanel();

			JTextField src = new JTextField(32);
			JTextField dst = new JTextField(32);
			p1.add(new JLabel("source (sep by ,):"));
			p1.add(src);
			p2.add(new JLabel("target:"));
			p2.add(dst);

			CodeTextPanel bot = new CodeTextPanel(BorderFactory.createEtchedBorder(), "Result", "");

			JButton go = new JButton("Compute hom set");
			go.addActionListener(x -> {
				String[] l = src.getText().split(",");
				String r = dst.getText();
				List<String> l0 = new LinkedList<>();
				for (String j : l) {
					String j2 = j.trim();
					if (!j2.isEmpty()) {
						l0.add(j2);
					}
				}
				Runnable runnable = () -> {
					try {

						Collection<Pair<OplCtx<S, V>, OplTerm<C, V>>> z = kb.hom0(Thread.currentThread(), l0, r);
						List<String> u = z.stream().map(o -> OplTerm.strip(o.first + " |- " + OplToKB.convert(o.second))).collect(Collectors.toList());
						if (u.isEmpty()) {
							bot.setText("empty");
						} else {
							bot.setText(Util.sep(u, "\n\n"));
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						bot.setText(ex.getMessage());
					}
					// finished = true;
				};
				Thread t = new Thread(runnable);
				try {
					t.start();
					t.join(DefunctGlobalOptions.debug.opl.opl_saturate_timeout);

					t.stop();
					if (bot.getText().equals("")) {
						bot.setText("Timeout");
					}

				} catch (Exception ex) {
					ex.printStackTrace();
					throw new RuntimeException("Timout");
				}

			});
			p3.add(go);
			top.add(p1);
			top.add(p2);
			top.add(p3);

			pane.add(top);
			pane.add(bot);

			return ret;
		}

		public Map<C, Integer> prec;
		public final Set<S> sorts;
		public final Map<C, Pair<List<S>, S>> symbols;
		public final List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations;
		public final List<Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>>> implications;

		@Override
		public String toString() {
			String ret = "\tsorts\n";
			List<String> sorts0 = sorts.stream().map(x -> Util.maybeQuote(OplTerm.strip(x.toString()))).collect(Collectors.toList());
			ret += "\t\t" + Util.sep(sorts0, ", ") + ";\n";

			ret += "\tsymbols\n";
			List<String> slist = new LinkedList<>();
			for (C k : symbols.keySet()) {
				Pair<List<S>, S> v = symbols.get(k);
				String s;
				if (v.first.isEmpty()) {
					s = Util.maybeQuote(OplTerm.strip(k.toString())) + " : " + Util.maybeQuote(OplTerm.strip(v.second.toString()));
				} else {
					List<String> f = v.first.stream().map(x -> Util.maybeQuote(OplTerm.strip(x.toString()))).collect(Collectors.toList());
					s = Util.maybeQuote(OplTerm.strip(k.toString())) + " : " + Util.sep(f, ", ") + " -> " + Util.maybeQuote(OplTerm.strip(v.second.toString()));
				}
				slist.add(s);
			}
			Comparator<String> comp = (o1, o2) -> {
                int i = o1.indexOf(" : ");
                int j = o2.indexOf(" : ");

                return o1.substring(i).compareTo(o2.substring(j));
            };
			slist.sort(comp);
			ret += "\t\t" + Util.sep(slist, ",\n\t\t") + ";\n";

			ret += "\tequations\n";
			List<String> elist = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> k : equations) {
				String z = k.first.vars0.isEmpty() ? "" : "forall ";
				String y = k.first.vars0.isEmpty() ? "" : ". ";
				String s = z + k.first + y + OplTerm.strip(k.second.toString()) + " = " + OplTerm.strip(k.third.toString());
				elist.add(s);
			}
			// elist.sort(comp); can't sort because not all equations have
			// forall x:E prefixes
			ret += "\t\t" + Util.sep(elist, ",\n\t\t") + ";\n";

			if (!implications.isEmpty()) {
				ret += "\timplications\n";
				elist = new LinkedList<>();
				for (Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>> k : implications) {
					String z = k.first.vars0.isEmpty() ? "" : "forall ";
					String y = k.first.vars0.isEmpty() ? "" : ". ";
					List<String> xxx = new LinkedList<>();
					for (Pair<OplTerm<C, V>, OplTerm<C, V>> xx : k.second) {
						xxx.add(xx.first + " = " + xx.second);
					}
					List<String> yyy = new LinkedList<>();
					for (Pair<OplTerm<C, V>, OplTerm<C, V>> xx : k.third) {
						yyy.add(xx.first + " = " + xx.second);
					}

					String s = z + k.first + y + OplTerm.strip(Util.sep(xxx, ", ")) + " -> " + OplTerm.strip(Util.sep(yyy, ","));
					elist.add(s);
				}
				ret += "\t\t" + Util.sep(elist, ",\n\t\t") + ";\n";
			}

			return "theory {\n" + ret + "}"; // + "\n\nprec: " + prec;
		}

		public OplSig(Iterator<V> fr, Map<C, Integer> prec, Set<S> sorts, Map<C, Pair<List<S>, S>> symbols, List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations) {
			this(fr, prec, sorts, symbols, equations, new LinkedList<>());
		}

		public OplSig(Iterator<V> fr, Map<C, Integer> prec, Set<S> sorts, Map<C, Pair<List<S>, S>> symbols, List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations, List<Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>>> implications) {
			this.sorts = sorts;
			this.symbols = symbols;
			this.equations = equations;
			this.prec = prec;
			this.fr = fr;
			this.implications = implications;
			// validate(); breaks imports
		}

		public final Iterator<V> fr;

		void validate() {
			if (!DefunctGlobalOptions.debug.opl.opl_allow_horn && !implications.isEmpty()) {
				throw new DoNotIgnore("Implications in theories disabled in options menu.");
			}
			for (C k : symbols.keySet()) {
				Pair<List<S>, S> v = symbols.get(k);
				if (!sorts.contains(v.second)) {
					throw new DoNotIgnore("Bad codomain " + v.second + " for " + k + " in " + this);
				}
				for (S a : v.first) {
					if (!sorts.contains(a)) {
						throw new DoNotIgnore("Bad argument sort " + a + " for " + k);
					}
				}
			}
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq0 : equations) {
				Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq = new Triple<>(inf(eq0), eq0.second, eq0.third);
				eq0.first = eq.first;
				eq.first.validate(this);
				S t1 = eq.second.type(this, eq.first);
				S t2 = eq.third.type(this, eq.first);
				if (!t1.equals(t2)) {
					throw new DoNotIgnore("Domains do not agree in " + eq.second + " = " + eq.third + ", are " + t1 + " and " + t2);
				}
			}
			for (Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>> eq0 : implications) {
				Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>> eq = new Triple<>(inf2(eq0), eq0.second, eq0.third);
				eq0.first = eq.first;
				eq.first.validate(this);
				for (Pair<OplTerm<C, V>, OplTerm<C, V>> conjunct : eq.second) {
					S t1 = conjunct.first.type(this, eq.first);
					S t2 = conjunct.second.type(this, eq.first);
					if (!t1.equals(t2)) {
						throw new DoNotIgnore("Domains do not agree in " + conjunct.first + " = " + conjunct.second + ", are " + t1 + " and " + t2);
					}
				}
				for (Pair<OplTerm<C, V>, OplTerm<C, V>> conjunct : eq.third) {
					S t1 = conjunct.first.type(this, eq.first);
					S t2 = conjunct.second.type(this, eq.first);
					if (!t1.equals(t2)) {
						throw new DoNotIgnore("Domains do not agree in " + conjunct.first + " = " + conjunct.second + ", are " + t1 + " and " + t2);
					}
				}
			}
			if (prec.keySet().size() != new HashSet<>(prec.values()).size()) {
				throw new RuntimeException("Cannot duplicate precedence: " + prec);
			}
			// getKB(); don't usually want to do this, because a lot of sigs
			// won't have precedences
		}

		private OplCtx<S, V> inf(Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq0) {
			try {
				KBExp<C, V> lhs = OplToKB.convert(eq0.second);
				KBExp<C, V> rhs = OplToKB.convert(eq0.third);
				Map<V, S> m = new HashMap<>(eq0.first.vars0);
				S l = KBFO.typeInf(lhs, (symbols), m);
				S r = KBFO.typeInf(rhs, (symbols), m);
				if (l == null && r == null) {
					throw new DoNotIgnore("Cannot infer sorts for " + lhs + " and " + rhs);
				}
				if (l == null && lhs.isVar()) {
					m.put(lhs.getVar(), r);
				}
				if (r == null && rhs.isVar()) {
					m.put(rhs.getVar(), l);
				}
				return new OplCtx<>(m);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new DoNotIgnore(ex.getMessage() + "\n\n in " + this);
			}
		}

		private OplCtx<S, V> inf2(Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>> eq0) {
			try {
				Map<V, S> m = new HashMap<>(eq0.first.vars0);
				for (Pair<OplTerm<C, V>, OplTerm<C, V>> eq : eq0.second) {
					KBExp<C, V> lhs = OplToKB.convert(eq.first);
					KBExp<C, V> rhs = OplToKB.convert(eq.second);
					S l = KBFO.typeInf(lhs, (symbols), m);
					S r = KBFO.typeInf(rhs, (symbols), m);
					if (l == null && lhs.isVar()) {
						m.put(lhs.getVar(), r);
					}
					if (r == null && rhs.isVar()) {
						m.put(rhs.getVar(), l);
					}
				}
				for (Pair<OplTerm<C, V>, OplTerm<C, V>> eq : eq0.third) {
					KBExp<C, V> lhs = OplToKB.convert(eq.first);
					KBExp<C, V> rhs = OplToKB.convert(eq.second);
					S l = KBFO.typeInf(lhs,(symbols), m);
					S r = KBFO.typeInf(rhs,(symbols), m);
					if (l == null && lhs.isVar()) {
						m.put(lhs.getVar(), r);
					}
					if (r == null && rhs.isVar()) {
						m.put(rhs.getVar(), l);
					}
				}
				for (V v : eq0.first.names()) {
					S c = m.get(v);
					if (c == null) {
						throw new DoNotIgnore("Cannot infer sort for " + v);
					}
				}
				return new OplCtx<>(m);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new DoNotIgnore(ex.getMessage() + "\n\n in " + this);
			}
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public Pair<List<S>, S> getSymbol(C var) {
			Pair<List<S>, S> ret = symbols.get(var);
			if (ret == null) {
				throw new DoNotIgnore("Unknown symbol " + var);
			}
			return ret;
		}

		public OplSetInst<S, C, OplTerm<C, V>> saturate(String name) {

			Map<S, Set<OplTerm<C, V>>> sorts0 = getKB().doHoms();

			Map<C, Map<List<OplTerm<C, V>>, OplTerm<C, V>>> symbols0 = new HashMap<>();
			for (C f : symbols.keySet()) {
				Pair<List<S>, S> ty = symbols.get(f);
				Map<Integer, List<OplTerm<C, V>>> args = new HashMap<>();
				int i = 0;
				for (S t : ty.first) {
					args.put(i++, new LinkedList<>(sorts0.get(t)));
				}
				List<LinkedHashMap<Integer, OplTerm<C, V>>> cands = FinSet.homomorphs(args);
				Map<List<OplTerm<C, V>>, OplTerm<C, V>> out = new HashMap<>();
				for (LinkedHashMap<Integer, OplTerm<C, V>> cand : cands) {
					List<OplTerm<C, V>> actual = new LinkedList<>();
					for (int j = 0; j < i; j++) {
						actual.add(cand.get(j));
					}
					OplTerm<C, V> to_red = new OplTerm<>(f, actual);
					OplTerm<C, V> red = OplToKB.convert(kb.nf(OplToKB.convert(to_red)));
					out.put(actual, red);
				}
				symbols0.put(f, out);
			}

			OplSetInst<S, C, OplTerm<C, V>> ret = new OplSetInst<>(sorts0, symbols0, name);
			ret.validate(this);
			return ret;
		}

		public int largestPrec() {
			int ret = 0;
			for (Integer k : prec.values()) {
				if (k == null) {
					continue; // ?
				}
				if (k > ret) {
					ret = k;
				}
			}
			return ret;
		}

	}

	public static class OplPres<S, C, V, X> extends OplExp {

		public final String S;
		public Map<X, Integer> prec;
		public final Map<X, S> gens;
		public final List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> equations;
		public final OplSig<S, C, V> sig;
		public OplSig<S, Chc<C, X>, V> toSig;

		private static <S, C, X, V> OplTerm<Chc<C, X>, V> conv(Map<X, S> gens, OplTerm<Object, V> e) {
			if (e.var != null) {
				return new OplTerm<>(e.var);
			}
			List<OplTerm<Chc<C, X>, V>> args0 = new LinkedList<>();
			for (OplTerm<Object, V> arg : e.args) {
				args0.add(conv(gens, arg));
			}
			return gens.get(e.head) != null ? new OplTerm<>(Chc.inRight((X) e.head), args0) : new OplTerm<>(Chc.inLeft((C) e.head), args0);
		}

		public static <S, C, V, X> OplPres<S, C, V, X> OplPres0(Map<X, Integer> prec, String S, OplSig<S, C, V> sig, Map<X, S> gens, List<Pair<OplTerm<Object, V>, OplTerm<Object, V>>> equations) {

			List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> eqs = new LinkedList<>();
			for (Pair<OplTerm<Object, V>, OplTerm<Object, V>> eq : equations) {
				eqs.add(new Pair<>(conv(gens, eq.first), conv(gens, eq.second)));
			}

			return new OplPres<>(prec, S, sig, gens, eqs);
		}

		public OplSig<S, Chc<C, X>, V> toSig() {
			if (toSig != null) {
				return toSig;
			}
			sig.validate();
			OplSig<S, Chc<C, X>, V> sig0 = sig.inject();

			Map<Chc<C, X>, Pair<List<S>, S>> symbols0 = new HashMap<>(sig0.symbols);
			for (X k : gens.keySet()) {
				if (sig.symbols.keySet().contains((C)k)) {
					throw new RuntimeException("Presentation contains a generator that is also found in schema/typeside");
				}
				symbols0.put(Chc.inRight(k), new Pair<>(new LinkedList<>(), gens.get(k)));
			}

			List<Triple<OplCtx<S, V>, OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> equations0 = new LinkedList<>(sig0.equations);
			for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> eq : equations) {
				equations0.add(new Triple<>(new OplCtx<>(), eq.first, eq.second));
			}

			Map<Chc<C, X>, Integer> m = new HashMap<>();
			for (C c : sig.symbols.keySet()) {
				Integer i = sig.prec.get(c);
				if (i != null) {
					m.put(Chc.inLeft(c), i);
				}
			}
			for (X x : gens.keySet()) {
				Integer i = prec.get(x);
				if (i != null) {
					m.put(Chc.inRight(x), i);
				}
			}

			toSig = new OplSig<>(sig.fr, m, sig.sorts, symbols0, equations0, sig0.implications);
			return toSig;
		}

		@Override
		public JComponent display() {
			JTabbedPane ret = new JTabbedPane();

			CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
			ret.add(p, "Text");

			CodeTextPanel q = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toSig().toString());
			ret.add(q, "Full Theory");

			return ret;
		}

		// : this is broken. the equations that go in expected too few chcs
		public OplPres(Map<X, Integer> prec, String S, OplSig<S, C, V> sig, Map<X, S> gens, List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> equations) {
			this.S = S;
			this.gens = gens;
			this.equations = equations;
			this.sig = sig;
			this.prec = prec;
			// toSig();
		}

		@Override
		public String toString() {
			String ret = "";
			ret += "\tgenerators\n";
			List<String> slist = new LinkedList<>();
			for (X k : gens.keySet()) {
				String v = Util.maybeQuote(OplTerm.strip(gens.get(k).toString()));
				String s = Util.maybeQuote(OplTerm.strip(k.toString())) + " : " + v;
				slist.add(s);
			}
			ret += "\t\t" + Util.sep(slist, ",\n\t\t") + ";\n";

			ret += "\tequations\n";
			List<String> elist = new LinkedList<>();
			for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> k : equations) {
				String s = k.first + " = " + k.second;
				elist.add(s);
			}

			ret += "\t\t" + Util.sep(elist, ",\n\t\t") + ";\n";

			return "presentation {\n" + ret + "} : " + S; // + "\n\n" + toSig();
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((S == null) ? 0 : S.hashCode());
			result = prime * result + ((equations == null) ? 0 : equations.hashCode());
			result = prime * result + ((gens == null) ? 0 : gens.hashCode());
			result = prime * result + ((sig == null) ? 0 : sig.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplPres<?, ?, ?, ?> other = (OplPres<?, ?, ?, ?>) obj;
			if (S == null) {
				if (other.S != null)
					return false;
			} else if (!S.equals(other.S))
				return false;
			if (equations == null) {
				if (other.equations != null)
					return false;
			} else if (!equations.equals(other.equations))
				return false;
			if (gens == null) {
				if (other.gens != null)
					return false;
			} else if (!gens.equals(other.gens))
				return false;
			if (sig == null) {
				if (other.sig != null)
					return false;
			} else if (!sig.equals(other.sig))
				return false;
			return true;
		}

		OplPres<S, C, V, X> simplified = null;

		public OplPres<S, C, V, X> simplify() {
			if (simplified != null) {
				return simplified;
			}

			Map<X, Integer> new_prec = new HashMap<>(prec);
			Map<X, S> new_gens = new HashMap<>(gens);
			List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> new_eqs = new LinkedList<>();
			for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> eq : equations) {
				new_eqs.add(new Pair<>(eq.first, eq.second));
			}

			while (simplify1(new_prec, new_gens, new_eqs));

			new_eqs.removeIf(eq -> eq.first.equals(eq.second));

			simplified = new OplPres<>(new_prec, S, sig, new_gens, new_eqs);
			return simplified;
		}

		public static <S, C, V, X> boolean simplify1(Map<X, Integer> new_prec, Map<X, S> new_gens, List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> new_eqs) {
			Set<X> gens = new HashSet<>(new_gens.keySet());
			for (X gen : gens) {
				OplTerm<Chc<C, X>, V> gen0 = new OplTerm<>(Chc.inRight(gen), new LinkedList<>());
				OplTerm<Chc<C, X>, V> replacee = null;
				for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> eq : new_eqs) {
					if (eq.first.equals(gen0) && !eq.second.contains(gen0)) {
						replacee = eq.second;
						break;
					}
					if (eq.second.equals(gen0) && !eq.first.contains(gen0)) {
						replacee = eq.first;
						break;
					}
				}
				if (replacee == null) {
					continue;
				}

				new_prec.remove(gen);
				new_gens.remove(gen);
				for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> eq : new_eqs) {
					eq = new Pair<>(eq.first.replace(gen0, replacee),eq.second.replace(gen0, replacee));
				}
				return true;
			}

			return false;
		}

		public static <S, C, V, X> void checkFreeExtension(OplPres<S, C, V, X> I) {

			OplPres<S, C, V, X> J = I.simplify();
			OplSig<S, Chc<C, X>, V> kb = J.sig.inject();
			// just check that there are no equations that aren't provable in
			// the schema
			//for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> eq : J.equations) {
			//	if (!kb.getKB().nf(eq.first).equals(kb.getKB().nf(eq.second))) {
			//		throw new RuntimeException("Not free extension (possibly inconsistent), cannot prove " + eq.first + " = " + eq.second + " using type side and schema");
			//	}

			//}
		}

	}

	public static class OplMapping<S1, C1, V, S2, C2> extends OplExp {
		final Map<S1, S2> sorts;
		final Map<S1, S2> sortsInit;
		final Map<C1, Pair<OplCtx<S2, V>, OplTerm<C2, V>>> symbols;
		final Map<C1, Pair<OplCtx<S2, V>, OplTerm<C2, V>>> symbolsInit;

		final String src0;
        final String dst0;
		OplSig<S1, C1, V> src;
		OplSig<S2, C2, V> dst;

		Set<String> imports = new HashSet<>();

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst0 == null) ? 0 : dst0.hashCode());
			result = prime * result + ((imports == null) ? 0 : imports.hashCode());
			result = prime * result + ((sortsInit == null) ? 0 : sortsInit.hashCode());
			result = prime * result + ((src0 == null) ? 0 : src0.hashCode());
			result = prime * result + ((symbolsInit == null) ? 0 : symbolsInit.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplMapping other = (OplMapping) obj;
			if (dst0 == null) {
				if (other.dst0 != null)
					return false;
			} else if (!dst0.equals(other.dst0))
				return false;
			if (imports == null) {
				if (other.imports != null)
					return false;
			} else if (!imports.equals(other.imports))
				return false;
			if (sortsInit == null) {
				if (other.sortsInit != null)
					return false;
			} else if (!sortsInit.equals(other.sortsInit))
				return false;
			if (src0 == null) {
				if (other.src0 != null)
					return false;
			} else if (!src0.equals(other.src0))
				return false;
			if (symbolsInit == null) {
				if (other.symbolsInit != null)
					return false;
			} else if (!symbolsInit.equals(other.symbolsInit))
				return false;
			return true;
		}

		@Override
		public JComponent display() {
			JTabbedPane jtp = new JTabbedPane();

			JComponent text = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
			jtp.addTab("Text", text);

			JComponent tables = makeTables();
			jtp.addTab("Tables", tables);

			return jtp;
		}

		private JComponent makeTables() {
			List<JComponent> list = new LinkedList<>();

			List<Object[]> rs = new LinkedList<>();
			for (S1 n : sorts.keySet()) {
				S2 f = sorts.get(n);
				rs.add(new Object[] { n, f });
			}
			list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Sorts", rs.toArray(new Object[][] {}), src0 + " (in)", dst0 + " (out)"));

			List<Object[]> rows = new LinkedList<>();
			for (C1 n : symbols.keySet()) {
				Pair<OplCtx<S2, V>, OplTerm<C2, V>> f = symbols.get(n);
				Object[] row = new Object[] { OplTerm.strip(n.toString()), OplTerm.strip("forall " + f.first + ". " + f.second) };
				rows.add(row);
			}
			list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), "Symbols", rows.toArray(new Object[][] {}), src0 + " (in)", dst0 + " (out)"));

			return GuiUtil.makeGrid(list);
		}

		@Override
		public String toString() {
			String ret = "\tsorts\n";
			List<String> sortsX = new LinkedList<>();
			for (S1 k : sorts.keySet()) {
				S2 v = sorts.get(k);
				sortsX.add(k + " -> " + v);
			}
			ret += "\t\t" + Util.sep(sortsX, ",\n\t\t") + ";\n";

			ret += "\tsymbols\n";
			List<String> symbolsX = new LinkedList<>();
			for (C1 k : symbols.keySet()) {
				Pair<OplCtx<S2, V>, OplTerm<C2, V>> v = symbols.get(k);
				String z = v.first.vars0.isEmpty() ? "" : "forall ";
				String y = v.first.vars0.isEmpty() ? "" : " . ";
				symbolsX.add(OplTerm.strip(k.toString()) + " -> " + z + v.first + y + v.second);
			}
			ret += "\t\t" + Util.sep(symbolsX, ",\n\t\t") + ";\n";

			return "mapping {\n" + ret + "} : " + src0 + " -> " + dst0;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public void validate(OplSig<S1, C1, V> src, OplSig<S2, C2, V> dst) {
			this.src = src;
			this.dst = dst;
			for (S1 k : sorts.keySet()) {
				if (!src.sorts.contains(k)) {
					throw new RuntimeException("Extra sort: " + k);
				}
				S2 v = sorts.get(k);
				if (!dst.sorts.contains(v)) {
					throw new RuntimeException("Bad target sort " + v + " from " + k);
				}
			}
			for (S1 k : src.sorts) {
				if (!sorts.keySet().contains(k)) {
					throw new RuntimeException("Missing sort: " + k);
				}
			}
			for (C1 k : src.symbols.keySet()) {
				if (!symbols.keySet().contains(k)) {
					throw new RuntimeException("missing symbol: " + k);
				}
			}
			for (C1 k : symbols.keySet()) {
				if (!src.symbols.containsKey(k)) {
					throw new RuntimeException("Extra symbol: " + k);
				}
				Pair<OplCtx<S2, V>, OplTerm<C2, V>> v = symbols.get(k);
				v = new Pair<>(v.first, replaceVarsByConsts(v.first, v.second));
				v = new Pair<>(inf(v), v.second);
				
				S2 t = v.second.type(dst, v.first);
				if (t == null) {
					throw new RuntimeException("Cannot type " + v.second + " in context [" + v.first + "]. ");
				}

				if (!t.equals(sorts.get(src.symbols.get(k).second))) {
					throw new RuntimeException("Symbol " + k + " returns a " + src.symbols.get(k).second + " but transforms to " + t);
				}
				List<S2> trans_t = src.symbols.get(k).first.stream().map(sorts::get).collect(Collectors.toList());
				if (!v.first.values().equals(trans_t)) {
					throw new RuntimeException("Symbol " + k + " inputs a " + v.first.values() + " but transforms to " + trans_t + " in \n\n " + this + "\n\nvalidate ctx " + v.first + "\nvalidate values " + v.first.values());
				}
			}

			for (Triple<OplCtx<S1, V>, OplTerm<C1, V>, OplTerm<C1, V>> eq : src.equations) {
				OplTerm<C2, V> l = subst(eq.second);
				OplTerm<C2, V> r = subst(eq.third);
				if (DefunctGlobalOptions.debug.opl.opl_validate) {
					KBExp<C2, V> l0 = dst.getKB().nf(OplToKB.convert(l));
					KBExp<C2, V> r0 = dst.getKB().nf(OplToKB.convert(r));
					if (!l0.equals(r0)) {
						throw new RuntimeException("Eq not preserved: " + l + " = " + r + " transforms to " + l0 + " = " + r0);
					}
				}
			}

			// : cannot actually check mapping validity for horn clauses

		}

		private OplCtx<S2, V> inf(Pair<OplCtx<S2, V>, OplTerm<C2, V>> eq0) {
			KBExp<C2, V> lhs = OplToKB.convert(eq0.second);
			Map<V, S2> m = new LinkedHashMap<>(eq0.first.vars0);
			KBFO.typeInf(lhs, (dst.symbols), m);
			return new OplCtx<>(m);
		}

		@SuppressWarnings("unlikely-arg-type")
		private OplTerm<C2, V> replaceVarsByConsts(OplCtx<S2, V> g, OplTerm<C2, V> e) {
			if (e.var != null) {
				if (dst.symbols.containsKey(e.var)) {
					if (g.vars0.containsKey(e.var)) {
						throw new RuntimeException("Attempt to shadow " + e.var);
					}
					C2 c2 = (C2) e.var;
					return new OplTerm<>(c2, new LinkedList<>());
				}
				return e;
			}
			return new OplTerm<>(e.head, e.args.stream().map(x -> replaceVarsByConsts(g, x)).collect(Collectors.toList()));
		}

		public OplMapping(Map<S1, S2> sorts, Map<C1, Pair<OplCtx<S2, V>, OplTerm<C2, V>>> symbols, String src0, String dst0) {
			this.sorts = sorts;
			this.symbols = symbols;
			this.src0 = src0;
			this.dst0 = dst0;
			symbolsInit = new HashMap<>();
			for (C1 c1 : symbols.keySet()) {
				symbolsInit.put(c1, new Pair<>(new OplCtx<>(symbols.get(c1).first.vars0), symbols.get(c1).second));
			}
			sortsInit = new HashMap<>();
			for (S1 c1 : sorts.keySet()) {
				sortsInit.put(c1, sorts.get(c1));
			}
		}

		public <X, Y> OplPresTrans<S2, C2, V, X, Y> sigma(OplPresTrans<S1, C1, V, X, Y> h) {
			OplPres<S1, C1, V, X> I = h.src;

			OplPres<S2, C2, V, X> I0 = sigma(h.src);
			OplPres<S2, C2, V, Y> J0 = sigma(h.dst);

			Map<S2, Map<X, OplTerm<Chc<C2, Y>, V>>> map = new HashMap<>();
			for (S2 s : I0.sig.sorts) {
				map.put(s, new HashMap<>());
			}
			for (S1 s : I.sig.sorts) {
				for (X x : I.gens.keySet()) {
					S1 t = I.gens.get(x);
					if (!s.equals(t)) {
						continue;
					}
					map.get(sorts.get(s)).put(x, sigma(h.map.get(s).get(x)));
				}
			}

			// validates
			OplPresTrans<S2, C2, V, X, Y> ret = new OplPresTrans<>(map, "?", "?", I0, J0);
			return ret;
		}

		public <X> OplSetTrans<S1, C1, X> delta(OplSetTrans<S2, C2, X> h) {
			if (!h.src0.sig.equals(dst0)) {
				throw new RuntimeException("Source of transform, " + h.src0 + " does not have theory " + dst0);
			}
			if (!h.dst0.sig.equals(dst0)) {
				throw new RuntimeException("Target of transform, " + h.src0 + " does not have theory " + dst0);
			}

			OplSetInst<S1, C1, X> srcX = delta(h.src0);
			OplSetInst<S1, C1, X> dstX = delta(h.dst0);

			Map<S1, Map<X, X>> sortsX = new HashMap<>();
			for (S1 s : src.sorts) {
				Map<X, X> m = new HashMap<>();
				for (X v : srcX.sorts.get(s)) {
					m.put(v, h.sorts.get(sorts.get(s)).get(v));
				}
				sortsX.put(s, m);
			}

			OplSetTrans<S1, C1, X> ret = new OplSetTrans<>(sortsX, "?", "?");
			ret.validate(src, srcX, dstX);
			return ret;
		}

		public <X> OplPres<S2, C2, V, X> sigma(OplPres<S1, C1, V, X> I) {
			if (!src.equals(I.sig)) {
				throw new RuntimeException("Source of mapping " + src0 + " does not match " + I.S);
			}

			Map<X, S2> sym = new HashMap<>();
			for (X c : I.gens.keySet()) {
				S1 t = I.gens.get(c);
				sym.put(c, sorts.get(t));
			}

			List<Pair<OplTerm<Chc<C2, X>, V>, OplTerm<Chc<C2, X>, V>>> eqs = new LinkedList<>();
			for (Pair<OplTerm<Chc<C1, X>, V>, OplTerm<Chc<C1, X>, V>> eq : I.equations) {
				eqs.add(new Pair<>(sigma(eq.first), sigma(eq.second)));
			}

			OplPres<S2, C2, V, X> ret = new OplPres<>(I.prec, dst0, dst, sym, eqs);
			return ret;
		}

		private OplTerm<C2, V> subst(OplTerm<C1, V> t) {
			if (t.var != null) {
				return new OplTerm<>(t.var);
			}
				List<OplTerm<C2, V>> l = new LinkedList<>();
				for (OplTerm<C1, V> a : t.args) {
					l.add(subst(a));
				}

				Pair<OplCtx<S2, V>, OplTerm<C2, V>> h = symbols.get(t.head);
				if (h == null) {
					throw new RuntimeException();
				}

				Map<V, OplTerm<C2, V>> s = new HashMap<>();
				List<Pair<V, S2>> r = h.first.values2();
				int i = 0;
				for (Pair<V, S2> p : r) {
					s.put(p.first, l.get(i++));
				}

				OplTerm<C2, V> ret = h.second;
				return ret.subst(s);
			
			
		}

		private <X> OplTerm<Chc<C2, X>, V> sigma(OplTerm<Chc<C1, X>, V> t) {
			if (t.var != null) {
				return new OplTerm<>(t.var);
			} 
				List<OplTerm<Chc<C2, X>, V>> l = new LinkedList<>();
				for (OplTerm<Chc<C1, X>, V> a : t.args) {
					l.add(sigma(a));
				}

				if (!t.head.left) {
					return new OplTerm<>(Chc.inRight(t.head.r), l);
				}

				Pair<OplCtx<S2, V>, OplTerm<C2, V>> h = symbols.get(t.head.l);

				Map<V, OplTerm<Chc<C2, X>, V>> s = new HashMap<>();
				List<Pair<V, S2>> r = h.first.values2();
				int i = 0;
				for (Pair<V, S2> p : r) {
					s.put(p.first, l.get(i++));
				}

				OplTerm<Chc<C2, X>, V> ret = h.second.inLeft();
				return ret.subst(s);
			
		}

		public <X> OplSetInst<S1, C1, X> delta(OplSetInst<S2, C2, X> J) {
			if (!dst0.equals(J.sig)) {
				throw new RuntimeException("Target of mapping " + dst + " does not match " + J.sig);
			}
			Map<S1, Set<X>> sortsX = new HashMap<>();
			for (S1 s : src.sorts) {
				S2 s0 = sorts.get(s);
				sortsX.put(s, J.sorts.get(s0));
			}

			Map<C1, Map<List<X>, X>> symbolsX = new HashMap<>();
			for (C1 n : src.symbols.keySet()) {
				Pair<List<S1>, S1> f_t = src.symbols.get(n);
				List<Set<X>> f_a = f_t.first.stream().map(sortsX::get).collect(Collectors.toList());
				List<List<X>> inputs = Util.prod(f_a);
				Pair<OplCtx<S2, V>, OplTerm<C2, V>> f = symbols.get(n);
				Map<List<X>, X> map = new HashMap<>();
				for (List<X> input : inputs) {
					X output = f.second.eval(dst, J, f.first.makeEnv(input));
					if (map.containsKey(input)) {
						throw new RuntimeException();
					}
					map.put(input, output);
				}
				symbolsX.put(n, map);
			}

			OplSetInst<S1, C1, X> I = new OplSetInst<>(sortsX, symbolsX, src0);
			I.validate(src);
			return I;
		}

	}

	public static class OplDelta0<S1, C1, V, S2, C2> extends OplExp {
		final String F0;
		OplTyMapping<S1, C1, V, S2, C2> F;

		public OplDelta0(String f) {
			F0 = f;
		}

		@Override
		public String toString() {
			return "OplDelta0 [F0=" + F0 + "]";
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public void validate(OplTyMapping<S1, C1, V, S2, C2> F) {
			this.F = F;
		}

		private OplQuery<S2, C2, V, S1, C1, V> Q;

		public OplQuery<S2, C2, V, S1, C1, V> toQuery() {
			if (Q != null) {
				return Q;
			}

			Map<Object, Pair<S1, Block<S2, C2, V, S1, C1, V>>> blocks = new HashMap<>();
			Map<S1, V> vars = new HashMap<>();
			for (S1 s1 : F.src.entities) {
				vars.put(s1, F.src.sig.fr.next());
			}
			for (S1 s1 : F.src.entities) {
				LinkedHashMap<V, S2> from = new LinkedHashMap<>();
				from.put(vars.get(s1), F.m.sorts.get(s1));

				Map<C1, Chc<Agg<S2, C2, V, S1, C1, V>, OplTerm<C2, V>>> attrs = new HashMap<>();
				for (C1 c1 : F.src.projA().symbols.keySet()) {
					if (!F.src.projA().symbols.get(c1).first.get(0).equals(s1)) {
						continue;
					}
					OplTerm<C2, V> t = F.m.subst(new OplTerm<>(c1, Collections.singletonList(new OplTerm<>(vars.get(s1)))));
					attrs.put(c1, Chc.inRight(t));
				}

				Map<C1, Pair<Object, Map<V, OplTerm<C2, V>>>> edges = new HashMap<>();
				for (C1 c1 : F.src.projE().symbols.keySet()) {
					Pair<List<S1>, S1> st = F.src.projE().symbols.get(c1);
					if (!st.first.get(0).equals(s1)) {
						continue;
					}
					Map<V, OplTerm<C2, V>> map = new HashMap<>();
					OplTerm<C2, V> t = F.m.subst(new OplTerm<>(c1, Collections.singletonList(new OplTerm<>(vars.get(s1)))));
					map.put(vars.get(st.second), t);
					edges.put(c1, new Pair<>(st.second, map));
				}

				Block<S2, C2, V, S1, C1, V> block = new Block<>(from, new HashSet<>(), attrs, edges);
				blocks.put(s1, new Pair<>(s1, block));
			}

			Q = new OplQuery<>(F.src0, F.dst0, blocks);
			Q.validate(F.dst, F.src);
			return Q;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F0 == null) ? 0 : F0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplDelta0 other = (OplDelta0) obj;
			if (F0 == null) {
				if (other.F0 != null)
					return false;
			} else if (!F0.equals(other.F0))
				return false;
			return true;
		}

	}

	public static class OplDelta extends OplExp {
		final String F;
        final String I;

		public OplDelta(String f, String i) {
			F = f;
			I = i;
		}

		@Override
		public String toString() {
			return "delta " + F + " " + I;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplDelta other = (OplDelta) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

	}

	public static class OplSigma extends OplExp {
		final String F;
        final String I;

		public OplSigma(String f, String i) {
			F = f;
			I = i;
		}

		@Override
		public String toString() {
			return "sigma " + F + " " + I;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSigma other = (OplSigma) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

	}

	public static class OplEval extends OplExp {
		final String I;
		final OplTerm e;

		public OplEval(String i, OplTerm e) {
			I = i;
			this.e = e;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((e == null) ? 0 : e.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplEval other = (OplEval) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (e == null) {
				if (other.e != null)
					return false;
			} else if (!e.equals(other.e))
				return false;
			return true;
		}

	}

	public static class OplSetInst<S, C, X> extends OplExp {

		public int size() {
			int ret = 0;
			for (S s : sorts.keySet()) {
				ret += sorts.get(s).size();
			}
			return ret;
		}

		public final Map<S, Set<X>> sorts;
		final String sig;
		public final Map<C, Map<List<X>, X>> symbols;
		public OplSig<S, C, ?> sig0;

		@Override
		public String toString() {
			String ret = "\tsorts\n";
			List<String> sortsX = new LinkedList<>();
			for (S k : sorts.keySet()) {
				sortsX.add(k + " -> {" + Util.sep(sorts.get(k).stream().map(x -> OplTerm.strip(x.toString())).collect(Collectors.toList()), ", ") + "}");
			}
			ret += "\t\t" + Util.sep(sortsX, ", ") + ";\n";

			ret += "\tsymbols\n";
			List<String> slist = new LinkedList<>();
			for (C k : symbols.keySet()) {
				Map<List<X>, X> v = symbols.get(k);
				List<String> u = new LinkedList<>();
				for (List<X> i : v.keySet()) {
					X j = v.get(i);
					u.add("((" + Util.sep(i.stream().map(x -> OplTerm.strip(x.toString())).collect(Collectors.toList()), ",") + "), " + OplTerm.strip(j.toString()) + ")");
				}
				String s = k + " -> {" + Util.sep(u, ", ") + "}";
				slist.add(s);
			}
			ret += "\t\t" + Util.sep(slist, ",\n\t\t") + ";\n";

			return "model {\n" + ret + "} : " + sig;
		}

		public String toHtml(Set skip) {
			Map<Object, Pair<List<String>, List<Object[]>>> xxx = makeTables(new HashSet<>(), true).second;
			String ret = "<div>";
			for (Object t : xxx.keySet()) {
				if (skip.contains(t)) {
					continue;
				}
				ret += "<table style=\"float: left\" border=\"1\" cellpadding=\"3\" cellspacing=\"1\">";

				List<String> cols = xxx.get(t).first;
				List<Object[]> rows = xxx.get(t).second;
				ret += "<tr>";
				for (String col : cols) {
					ret += "<th>" + col + "</th>";
				}
				ret += "</tr>";
				for (Object[] row : rows) {
					ret += "<tr>";
					for (Object col : row) {
						ret += "<td>" + col + "</td>";
					}
					ret += "</tr>";
				}
				ret += "</table>";
			}
			return ret + "</div><br style=\"clear:both;\">";
		}

		@Override
		public JComponent display() {
			JTabbedPane jtp = new JTabbedPane();

			JComponent text = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
			jtp.addTab("Text", text);

			Pair<JComponent, Map<Object, Pair<List<String>, List<Object[]>>>> xxx = makeTables(new HashSet<>(), false);
			JComponent tables = xxx.first;
			jtp.addTab("Tables", tables);

			return jtp;
		}

		public OplSetInst<S, C, X> number(Set<S> types) {
			Map<S, Set<X>> sorts2 = new HashMap<>();
			Map<S, Map<X, X>> inj = new HashMap<>();

			int i = 0;
			for (S s : sorts.keySet()) {
				if (types.contains(s)) {
					sorts2.put(s, sorts.get(s));
				} else {
					Set<X> m = new HashSet<>();
					Map<X, X> inj2 = new HashMap<>();
					for (X x : sorts.get(s)) {
						X j = (X) new Integer(i);
						inj2.put(x, j);
						m.add(j);
						i++;
					}
					inj.put(s, inj2);
					sorts2.put(s, m);
				}
			}

			Map<C, Map<List<X>, X>> symbols2 = new HashMap<>();
			for (C c : symbols.keySet()) {
				Map<List<X>, X> x = symbols.get(c);
				Map<List<X>, X> x2 = new HashMap<>();
				for (List<X> args : x.keySet()) {
					int col = 0;
					List<X> newargs = new LinkedList<>();
					for (X arg : args) {
						S ty = sig0.symbols.get(c).first.get(col);
						if (!types.contains(ty)) {
							X newarg = inj.get(ty).get(arg);
							newargs.add(newarg);
						}
						col++;
					}
					X res = x.get(args);
					S ty = sig0.symbols.get(c).second;
					if (!types.contains(ty)) {
						res = inj.get(ty).get(res);
					}
					x2.put(newargs, res);
				}
				symbols2.put(c, x2);
			}
			OplSetInst<S, C, X> ret = new OplSetInst<>(sorts2, symbols2, sig);
			ret.validate(sig0);
			return ret;
		}

		public Pair<JComponent, Map<Object, Pair<List<String>, List<Object[]>>>> makeTables(Set<S> types, boolean skipGUI) {

			Map<Object, Pair<List<String>, List<Object[]>>> forHtml = new HashMap<>();
			Set<String> atts = new HashSet<>();
			for (C c : sig0.symbols.keySet()) {
				Pair<List<S>, S> v = sig0.symbols.get(c);
				if (types.contains(v.second)) {
					atts.add(OplTerm.strip(c.toString()));
				}
			}

			Set<String> skip = types.stream().map(Object::toString).collect(Collectors.toSet());
			List<JComponent> list = new LinkedList<>();

			Map<String, JComponent> all = new HashMap<>();

			List<C> keys2 = new LinkedList<>(symbols.keySet());

			Comparator<Object> comp = Comparator.comparing(Object::toString);

			for (S n : sorts.keySet()) {
				List<S> t0 = Collections.singletonList(n);
				List<C> set = new LinkedList<>();
				for (C f : symbols.keySet()) {
					Pair<List<S>, S> t = sig0.symbols.get(f);
					if (t.first.equals(t0)) {
						set.add(f);
						keys2.remove(f);
					}
				}

				Comparator<Object> comp2 = (o1, o2) -> {
                    if (atts.contains(o1.toString()) && !atts.contains(o2.toString())) {
                        return 1;
                    } else if (!atts.contains(o1.toString()) && atts.contains(o2.toString())) {
                        return -1;
                    }
                    return o1.toString().compareTo(o2.toString());
                };
				set.sort(comp2);

				List<Object[]> rows = new LinkedList<>();
				List<String> cols = new LinkedList<>();

				for (X arg : sorts.get(n)) {
					List<Object> row = new LinkedList<>();
					cols = new LinkedList<>();
					cols.add(OplTerm.strip(n.toString()));
					row.add(arg);
					for (C f : set) {
						row.add(symbols.get(f).get(Collections.singletonList(arg)));
						cols.add(OplTerm.strip(f.toString()));
					}
					rows.add(row.toArray(new Object[] {}));
				}
				if (!skipGUI) {
					all.put(n.toString(), JSWrapper.makePrettyTables(atts, BorderFactory.createEmptyBorder(), OplTerm.strip(n.toString()) + " (" + rows.size() + ")", rows.toArray(new Object[][] {}), cols.toArray(new String[] {})));
				}
				forHtml.put(n, new Pair<>(cols, rows));

			}
			for (C n : keys2) {
				if (sig0.symbols.get(n).first.isEmpty()) {
					continue;
				}
				Map<List<X>, X> f = symbols.get(n);
				List<Object[]> rows = new LinkedList<>();
				for (List<X> arg : f.keySet()) {
					List<Object> argX = new LinkedList<>(arg);
					argX.add(f.get(arg));
					Object[] row = argX.toArray(new Object[] {});
					rows.add(row);
				}
				List<String> l = new LinkedList<>((Collection<String>) sig0.symbols.get(n).first);
				l.add(sig0.symbols.get(n).second.toString());

				if (!skipGUI) {
					all.put(n.toString(), JSWrapper.makePrettyTables(atts, BorderFactory.createEmptyBorder(), OplTerm.strip(n.toString()) + " (" + rows.size() + ")", rows.toArray(new Object[][] {}), l.toArray(new String[] {})));
				}
				forHtml.put(n, new Pair<>(l, rows));

			}
			List<String> xxx = new LinkedList<>(all.keySet());
			xxx.sort(comp);
			for (String n : xxx) {
				if (skip.contains(n) && DefunctGlobalOptions.debug.opl.opl_suppress_dom) {
					continue;
				}
				if (!skipGUI) {
					list.add(all.get(n));
				}
			}
			return new Pair<>(skipGUI ? null : GuiUtil.makeGrid(list), forHtml);
		}

		public <V> void validate(OplSig<S, C, V> sig) {
			for (S s : sig.sorts) {
				if (!sorts.containsKey(s)) {
					throw new RuntimeException("No data for " + s + " in " + this);
				}
			}
			for (S s : sorts.keySet()) {
				if (!sig.sorts.contains(s)) {
					throw new RuntimeException("Extra data for " + s);
				}
			}
			for (C f : sig.symbols.keySet()) {
				if (!symbols.containsKey(f)) {
					throw new RuntimeException("No data for " + f);
				}
				List<S> arg_ts = sig.symbols.get(f).first;
				List<Set<X>> arg_ds = arg_ts.stream().map(sorts::get).collect(Collectors.toList());
				List<List<X>> args = Util.prod(arg_ds);
				for (List<X> arg : args) {
					X at = symbols.get(f).get(arg);
					if (at == null) {
						throw new RuntimeException("Missing on argument " + arg + " in " + f);
					}
					if (!sorts.get(sig.symbols.get(f).second).contains(at)) {
						throw new RuntimeException("In " + f + ", return value " + at + " not in correct sort " + sorts.get(sig.symbols.get(f).second));
					}
				}
				for (List<X> gt : symbols.get(f).keySet()) {
					if (!args.contains(gt)) {
						throw new RuntimeException("Superfluous arg " + gt + " in " + f + " notin " + args);
					}
				}
			}
			for (C f : symbols.keySet()) {
				if (!sig.symbols.keySet().contains(f)) {
					throw new RuntimeException("Extra data for " + f);
				}
			}
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : sig.equations) {
				List<S> arg_ts = eq.first.values();
				List<Set<X>> arg_ds = arg_ts.stream().map(sorts::get).collect(Collectors.toList());
				List<List<X>> args = Util.prod(arg_ds);
				for (List<X> env : args) {
					OplCtx<X, V> env2 = eq.first.makeEnv(env);
					X x = eq.second.eval(sig, this, env2);
					X y = eq.third.eval(sig, this, env2);
					if (!x.equals(y)) {
						throw new RuntimeException("Equation " + eq.second + " = " + eq.third + " not respected on " + env + ", lhs=" + x + " and rhs=" + y);
					}
				}
			}
			outer: for (Triple<OplCtx<S, V>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>> eq : sig.implications) {
				List<S> arg_ts = eq.first.values();
				List<Set<X>> arg_ds = arg_ts.stream().map(sorts::get).collect(Collectors.toList());
				List<List<X>> args = Util.prod(arg_ds);
				for (List<X> env : args) {
					OplCtx<X, V> env2 = eq.first.makeEnv(env);
					for (Pair<OplTerm<C, V>, OplTerm<C, V>> conjunct : eq.second) {
						X x = conjunct.first.eval(sig, this, env2);
						X y = conjunct.second.eval(sig, this, env2);
						if (!x.equals(y)) {
							continue outer;
						}
					}
					for (Pair<OplTerm<C, V>, OplTerm<C, V>> conjunct : eq.third) {
						X x = conjunct.first.eval(sig, this, env2);
						X y = conjunct.second.eval(sig, this, env2);
						if (!x.equals(y)) {
							throw new RuntimeException("Implication " + Util.sep(eq.second, ",") + " -> " + conjunct.first + "=" + conjunct.second + " not respected on " + env + ", lhs=" + x + " and rhs=" + y);
						}
					}
				}
			}

			sig0 = sig;
		}

		public OplSetInst(Map<S, Set<X>> sorts, Map<C, Map<List<X>, X>> symbols, String sig) {
			this.sorts = sorts;
			this.sig = sig;
			this.symbols = symbols;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public Map<List<X>, X> getSymbol(C head) {
			Map<List<X>, X> ret = symbols.get(head);
			if (ret == null) {
				throw new RuntimeException("Unknown symbol " + head);
			}
			return ret;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
			result = prime * result + ((symbols == null) ? 0 : symbols.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSetInst<?, ?, ?> other = (OplSetInst<?, ?, ?>) obj;
			if (sorts == null) {
				if (other.sorts != null)
					return false;
			} else if (!sorts.equals(other.sorts))
				return false;
			if (symbols == null) {
				if (other.symbols != null)
					return false;
			} else if (!symbols.equals(other.symbols))
				return false;
			return true;
		}

	}

	public static class OplJavaInst<S, C, V> extends OplExp {

		public final Map<C, String> defs;

		public ScriptEngine engine;

		final String sig;

		OplSig<S, C, V> sig0;

		Environment<OplObject> ENV;

		public OplJavaInst(Map<C, String> defs, String sig) {
			this.defs = defs;
			this.sig = sig;
		}

		public void validate(OplSig<S, C, V> sig, Environment<OplObject> ENV) {
			this.ENV = ENV;
			sig0 = sig;
			for (C k : defs.keySet()) {
				if (k.equals("_preamble") || k.equals("_compose")) {
					continue;
				}
				if (!sig.symbols.containsKey(k)) {
					throw new RuntimeException("Extra symbol " + k + " in " + defs.keySet() + " but not in " + sig.symbols);
				}
			}
			for (Object k : sig.symbols.keySet()) {
				if (!defs.keySet().contains(k)) {
					throw new RuntimeException("Missing symbol " + k + " in " + defs.keySet());
				}
			}

			engine = new ScriptEngineManager().getEngineByName("nashorn");
			for (String key : ENV.keys()) {
				engine.put(key, ENV.get(key));
			}
			String ret = "";
			if (defs.containsKey("_preamble")) {
				ret += defs.get("_preamble") + "\n\n";
			}
			for (C k : defs.keySet()) {
				if (k.equals("_preamble")) {
					continue;
				}
				String v = defs.get(k);
				ret += "function " + Util.stripChcs(k).second + "(input) { " + v + " }\n\n";
			}

			try {
				engine.eval(ret);
			} catch (ScriptException e) {
				e.printStackTrace();
				throw new RuntimeException(e.getMessage());
			}

		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			String ret = "\tsymbols\n";
			List<String> slist = new LinkedList<>();
			for (C k : defs.keySet()) {
				String s = k + " -> \"" + defs.get(k) + "\"";
				slist.add(s);
			}
			ret += "\t\t" + Util.sep(slist, ",\n\t\t") + ";\n";

			return "javascript {\n" + ret + "} : " + sig;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((defs == null) ? 0 : defs.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplJavaInst other = (OplJavaInst) obj;
			if (defs == null) {
				if (other.defs != null)
					return false;
			} else if (!defs.equals(other.defs))
				return false;
			return true;
		}

	}

	// : pretty print
	public static class OplPresTrans<S, C, V, X, Y> extends OplExp {
		Map<S, Map<X, OplTerm<Chc<C, Y>, V>>> map = new HashMap<>();
		Map<S, Map<X, OplTerm<Object, V>>> pre_map;
		String initMap;
		final String src0;
        final String dst0;

		OplPres<S, C, V, X> src;
		OplPres<S, C, V, Y> dst;

		OplInst<S, C, V, X> src1;
		OplInst<S, C, V, Y> dst1;

		Set<String> imports = new HashSet<>();

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst0 == null) ? 0 : dst0.hashCode());
			result = prime * result + ((imports == null) ? 0 : imports.hashCode());
			result = prime * result + ((initMap == null) ? 0 : initMap.hashCode());
			result = prime * result + ((src0 == null) ? 0 : src0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplPresTrans other = (OplPresTrans) obj;
			if (dst0 == null) {
				if (other.dst0 != null)
					return false;
			} else if (!dst0.equals(other.dst0))
				return false;
			if (imports == null) {
				if (other.imports != null)
					return false;
			} else if (!imports.equals(other.imports))
				return false;
			if (initMap == null) {
				if (other.initMap != null)
					return false;
			} else if (!initMap.equals(other.initMap))
				return false;
			if (src0 == null) {
				if (other.src0 != null)
					return false;
			} else if (!src0.equals(other.src0))
				return false;
			return true;
		}

		public static <S, C, V, X, Y> OplTerm<Chc<C, Y>, V> apply(OplPres<S, C, V, X> src, Map<S, Map<X, OplTerm<Chc<C, Y>, V>>> map, OplTerm<Chc<C, X>, V> e) {
			if (e.var != null) {
				throw new RuntimeException();
			}
			List<OplTerm<Chc<C, Y>, V>> args0 = new LinkedList<>();
			for (OplTerm<Chc<C, X>, V> arg : e.args) {
				args0.add(apply(src, map, arg));
			}
			if (e.head.left) {
				return new OplTerm<>(Chc.inLeft(e.head.l), args0);
			} 
				if (!e.args.isEmpty()) {
					throw new RuntimeException();
				}
				OplTerm<Chc<C, Y>, V> e0 = map.get(e.type(src.toSig(), new OplCtx<>())).get(e.head.r);
				if (e0 == null) {
					throw new RuntimeException();
				}
				return e0;
			
		}

		public OplTerm<Chc<C, Y>, V> apply(OplTerm<Chc<C, X>, V> e) {
			return apply(src, map, e);
		}

		@Override
		public String toString() {
			String ret = "";
			List<String> sortsX = new LinkedList<>();

			Map touse;
			if (pre_map != null && (map == null || map.isEmpty())) {
				touse = pre_map;
			} else if (map != null && (pre_map == null || pre_map.isEmpty())) {
				touse = map;
			} else {
				throw new RuntimeException("Report to Ryan");
			}

			for (Object s : touse.keySet()) {
				Map m = (Map) touse.get(s);

				List<String> symbolsX = new LinkedList<>();
				for (Object k : m.keySet()) {
					String v = m.get(k).toString();
					symbolsX.add("(" + OplTerm.strip(k.toString()) + ", " + OplTerm.strip(v) + ")");
				}
				sortsX.add(s + " -> {" + Util.sep(symbolsX, ", ") + "}");
			}
			ret += "\t\t" + Util.sep(sortsX, ",\n\t\t") + ";\n";

			return "transpres {\n sorts\n" + ret + "} : " + src0 + " -> " + dst0;
		}

		@Override
		public JComponent display() {
			return toMapping().display();
		}

		private OplTerm<Chc<C, Y>, V> conv(OplTerm<Object, V> e) {
			if (e.var != null) {
				return new OplTerm<>(e.var);
			}
			List<OplTerm<Chc<C, Y>, V>> args0 = new LinkedList<>();
			for (OplTerm<Object, V> arg : e.args) {
				args0.add(conv(arg));
			}
			if (dst.gens.get(e.head) != null) {
				Y y = (Y) e.head;
				return new OplTerm<>(Chc.inRight(y), args0);
			} 
				C c = (C) e.head;
				return new OplTerm<>(Chc.inLeft(c), args0);
			
		}

		OplMapping<S, Chc<C, X>, V, S, Chc<C, Y>> mapping;

		public OplMapping<S, Chc<C, X>, V, S, Chc<C, Y>> toMapping() {
			if (mapping != null) {
				return mapping;
			}
			Map<S, S> sorts = Util.id(src.sig.sorts);

			Map<Chc<C, X>, Pair<OplCtx<S, V>, OplTerm<Chc<C, Y>, V>>> symbols = new HashMap<>();
			for (C c : src.sig.symbols.keySet()) {
				Pair<List<S>, S> t = src.sig.symbols.get(c);

				List<Pair<V, S>> l = new LinkedList<>();
				List<OplTerm<Chc<C, Y>, V>> r = new LinkedList<>();
				for (S s : t.first) {
					V v = src.sig.fr.next();
					l.add(new Pair<>(v, s));
					r.add(new OplTerm<>(v));
				}

				OplCtx<S, V> g = new OplCtx<>(l);
				OplTerm<Chc<C, Y>, V> e = new OplTerm<>(Chc.inLeft(c), r);

				Pair<OplCtx<S, V>, OplTerm<Chc<C, Y>, V>> p = new Pair<>(g, e);
				symbols.put(Chc.inLeft(c), p);
			}

			for (X x : src.gens.keySet()) {
				S s = src.gens.get(x);
				if (!map.containsKey(s)) {
					throw new RuntimeException(s + " not a key in " + map);
				}
				if (!map.get(s).containsKey(x)) {
					throw new RuntimeException(x + " not a key in " + map.get(s) + " \n\n" + this);
				}
				OplTerm<Chc<C, Y>, V> y = map.get(s).get(x);
				symbols.put(Chc.inRight(x), new Pair<>(new OplCtx<>(), y));
			}

			mapping = new OplMapping<>(sorts, symbols, src0, dst0);
			mapping.validate(src.toSig(), dst.toSig());
			return mapping;
		}

		public void validateNotReally(OplInst<S, C, V, X> src, OplInst<S, C, V, Y> dst) {
			if (pre_map == null) {
				return; // throw exn?
			}
			src1 = src;
			dst1 = dst;
			for (S s : src.P.sig.sorts) {
				if (!pre_map.containsKey(s)) {
					pre_map.put(s, new HashMap<>());
				}
			}

			validateNotReally(src.P, dst.P);
		}

		public void validateNotReally(OplPres<S, C, V, X> src, OplPres<S, C, V, Y> dst) {
			if (pre_map == null) {
				return;
			}
			this.src = src;
			this.dst = dst;

			if (!src.sig.equals(dst.sig)) {
				throw new RuntimeException("Signatures do not match");
			}

			for (S s : src.sig.sorts) {
				if (!pre_map.containsKey(s)) {
					throw new RuntimeException("Missing sort: " + s);
				}
			}
			for (S s : pre_map.keySet()) {
				if (!src.sig.sorts.contains(s)) {
					throw new RuntimeException("Extra sort: " + s);
				}
			}

			for (S s : src.sig.sorts) {
				Map<X, OplTerm<Chc<C, Y>, V>> m = new HashMap<>();
				for (X x : pre_map.get(s).keySet()) {
					OplTerm<Object, V> n = pre_map.get(s).get(x);
					OplTerm<Chc<C, Y>, V> u = conv(n);
					if (!src.gens.keySet().contains(x)) {
						throw new RuntimeException("Not a generator: " + x);
					}
					u.type(dst.toSig(), new OplCtx<>());
					m.put(x, u);
				}
				map.put(s, m);
			}

			toMapping();

		}

		public OplPresTrans(Map<S, Map<X, OplTerm<Object, V>>> m, String src0, String dst0) {
			pre_map = m;
			this.src0 = src0;
			this.dst0 = dst0;
			initMap = m.toString();
		}

		// validates
		public OplPresTrans(Map<S, Map<X, OplTerm<Chc<C, Y>, V>>> map, String src0, String dst0, OplPres<S, C, V, X> src, OplPres<S, C, V, Y> dst) {
			this.map = map;
			this.src0 = src0;
			this.dst0 = dst0;
			this.src = src;
			this.dst = dst;
			toMapping();
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class OplSetTrans<S, C, X> extends OplExp {
		final Map<S, Map<X, X>> sorts;
		final String src;
        final String dst;

		OplSig<S, C, ?> sig;
		OplSetInst<S, C, X> src0, dst0;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSetTrans other = (OplSetTrans) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (sorts == null) {
				if (other.sorts != null)
					return false;
			} else if (!sorts.equals(other.sorts))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		@Override
		public JComponent display() {
			JTabbedPane jtp = new JTabbedPane();

			JComponent text = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
			jtp.addTab("Text", text);

			JComponent tables = makeTables();
			jtp.addTab("Tables", tables);

			return jtp;
		}

		private JComponent makeTables() {
			List<JComponent> list = new LinkedList<>();
			for (S n : sorts.keySet()) {
				Map<X, X> f = sorts.get(n);
				List<Object[]> rows = new LinkedList<>();
				for (X arg : f.keySet()) {
					Object[] row = new Object[2];
					row[0] = arg;
					row[1] = f.get(arg);
					rows.add(row);
				}
				list.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), n + " (" + rows.size() + ")", rows.toArray(new Object[][] {}), src + " (in)", dst + " (out)"));
			}

			return GuiUtil.makeGrid(list);
		}

		public OplSetTrans(Map<S, Map<X, X>> sorts, String src, String dst) {
			this.sorts = sorts;
			this.src = src;
			this.dst = dst;
		}

		public void validate(OplSig<S, C, ?> sig, OplSetInst<S, C, X> src0, OplSetInst<S, C, X> dst0) {
			this.sig = sig;
			this.src0 = src0;
			this.dst0 = dst0;

			for (Object s : sig.sorts) {
				if (!sorts.containsKey(s)) {
					throw new RuntimeException("Missing sort: " + s);
				}
			}
			for (S s : sorts.keySet()) {
				if (!sig.sorts.contains(s)) {
					throw new RuntimeException("Extra sort: " + s);
				}
			}
			for (Object s : sig.sorts) {
				Map<X, X> h = sorts.get(s);
				for (X x : h.keySet()) {
					X y = h.get(x);
					if (!src0.sorts.get(s).contains(x)) {
						throw new RuntimeException("Value " + x + " is not in source for sort " + s);
					}
					if (!dst0.sorts.get(s).contains(y)) {
						throw new RuntimeException("Value " + y + " is not in target for sort " + s);
					}
				}
			}
			for (C f : sig.symbols.keySet()) {
				Pair<List<S>, S> a = sig.symbols.get(f);
				List<S> arg_ts = a.first;
				List<Set<X>> arg_ds = arg_ts.stream().map(src0.sorts::get).collect(Collectors.toList());
				List<List<X>> args = Util.prod(arg_ds);

				for (List<X> arg : args) {
					X r = src0.getSymbol(f).get(arg);
					X lhs = sorts.get(a.second).get(r);

					List<X> l = new LinkedList<>();
					int i = 0;
					for (S at : arg_ts) {
						X v = arg.get(i);
						X u = sorts.get(at).get(v);
						l.add(u);
						i++;
					}
					X rhs = dst0.getSymbol(f).get(l);
					if (!lhs.equals(rhs)) {
						throw new RuntimeException("Compatibility condition failure for " + f + " on " + arg + ", lhs=" + lhs + " but rhs=" + rhs);
					}
				}

			}
		}

		@Override
		public String toString() {
			String ret = "\tsorts\n";
			List<String> sortsX = new LinkedList<>();
			for (S k : sorts.keySet()) {
				Map<X, X> v = sorts.get(k);
				List<String> l = new LinkedList<>();
				for (X x : v.keySet()) {
					X y = v.get(x);
					l.add("(" + x + ", " + y + ")");
				}
				sortsX.add(k + " -> {" + Util.sep(l, ", ") + "}");
			}
			ret += "\t\t" + Util.sep(sortsX, ", ") + ";\n";

			return "transform {\n" + ret + "} : " + src + " -> " + dst;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	private static class OplJavaTrans {

	}

	public static class OplSchema<S, C, V> extends OplExp {
		final String sig0;
		OplSig<S, C, V> sig;
		final Set<S> entities;

		String forSchema0;

		@Override
		public JComponent display() {
			JTabbedPane jtp = new JTabbedPane();

			JComponent text = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
			jtp.addTab("Text", text);

			JComponent th = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", sig.toString());
			jtp.addTab("Theory", th);

			JComponent graph = doSchemaView(Color.RED, buildFromSig(projEA()), entities);
			jtp.addTab("Graph", graph);

			return jtp;
		}

		public Set<C> outEdges(S s) {
			Set<C> ret = new HashSet<>();
			for (C c : projEA().symbols.keySet()) {
				Pair<List<S>, S> t = projEA().symbols.get(c);
				if (t.first.get(0).equals(s)) {
					ret.add(c);
				}
			}
			return ret;
		}

		public OplSchema(String sig0, Set<S> entities) {
			this.sig0 = sig0;
			this.entities = entities;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public void validate(OplSig<S, C, V> sig) {
			this.sig = sig;
			sig.validate();
			for (S s : entities) {
				if (!sig.sorts.contains(s)) {
					throw new RuntimeException("Not a sort: " + s);
				}
			}
			for (C f : sig.symbols.keySet()) {
				Pair<List<S>, S> t = sig.symbols.get(f);

				if (t.first.size() == 1 && entities.contains(t.first.get(0)) && entities.contains(t.second)) {
					continue; // is foreign key
				}
				if (t.first.size() == 1 && entities.contains(t.first.get(0)) && !entities.contains(t.second)) {
					continue; // is attribute
				}
				boolean hitEntity = false;
				for (S k : t.first) {
					if (entities.contains(k)) {
						hitEntity = true;
						break;
					}
				}
				if (!hitEntity && !entities.contains(t.second)) {
					continue; // is typeside function
				}
				throw new RuntimeException("Does not pass entity/typeside check: " + f);
			}
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : sig.equations) {
				for (S k : eq.first.values()) {
					if (entities.contains(k)) {
						if (eq.first.values().size() != 1) {
							throw new RuntimeException(eq + " has ctx with > 1 variable");
						}
						break;
					}
				}
			}

		}

		OplSig<S, C, V> cache_E, cache_A, cache_T;

		public OplSig<S, C, V> projE() {
			if (cache_E != null) {
				return cache_E;
			}
			Map<C, Pair<List<S>, S>> symbols = new HashMap<>();
			for (C f : sig.symbols.keySet()) {
				Pair<List<S>, S> t = sig.symbols.get(f);

				if (entities.containsAll(t.first) && entities.contains(t.second)) {
					symbols.put(f, t);
				}
			}

			List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : sig.equations) {
				if (eq.first.vars0.size() != 1) {
					continue;
				}
				if (!entities.contains(new LinkedList<>(eq.first.vars0.values()).get(0))) {
					continue;
				}
				if (!symbols.keySet().containsAll(eq.second.symbols()) || !symbols.keySet().containsAll(eq.third.symbols())) {
					continue;
				}
				equations.add(eq);
			}

			cache_E = new OplSig<>(sig.fr, sig.prec, new HashSet<>(entities), symbols, equations);

			return cache_E;
		}

		public OplSig<S, C, V> projT() {
			if (cache_T != null) {
				return cache_T;
			}
			Set<S> types = new HashSet<>(sig.sorts);
			types.removeAll(entities);

			Map<C, Pair<List<S>, S>> symbols = new HashMap<>();
			outer: for (C f : sig.symbols.keySet()) {
				Pair<List<S>, S> t = sig.symbols.get(f);

				if (entities.contains(t.second)) {
					continue;
				}
				for (S s : t.first) {
					if (entities.contains(s)) {
						continue outer;
					}
				}

				symbols.put(f, t);
			}

			List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : sig.equations) {
				if (!types.containsAll(eq.first.vars0.values())) {
					continue;
				}
				if (!symbols.keySet().containsAll(eq.second.symbols()) || !symbols.keySet().containsAll(eq.third.symbols())) {
					continue;
				}
				equations.add(eq);
			}

			cache_T = new OplSig<>(sig.fr, sig.prec, types, symbols, equations);
			return cache_T;
		}

		OplSig<S, C, V> cache_EA;

		public List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> obsEqs() {
			List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations = new LinkedList<>();
			equations.addAll(sig.equations);
			equations.removeAll(projT().equations);
			equations.removeAll(projE().equations);
			return equations;
		}

		public OplSig<S, C, V> projEA() {
			if (cache_EA != null) {
				return cache_EA;
			}

			Set<S> types = new HashSet<>();
			types.addAll(sig.sorts);
			Map<C, Pair<List<S>, S>> symbols = new HashMap<>();
			symbols.putAll(projA().symbols);
			symbols.putAll(projE().symbols);
			List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations = new LinkedList<>();
			equations.addAll(projA().equations);
			equations.addAll(projE().equations);

			cache_EA = new OplSig<>(sig.fr, sig.prec, types, symbols, equations);
			return cache_EA;
		}

		public OplSig<S, C, V> projA() {
			if (cache_A != null) {
				return cache_A;
			}
			Map<C, Pair<List<S>, S>> symbols = new HashMap<>();
			for (C f : sig.symbols.keySet()) {
				Pair<List<S>, S> t = sig.symbols.get(f);

				if (entities.contains(t.second)) {
					continue;
				}
				if (t.first.size() != 1) {
					continue;
				}
				if (!entities.contains(t.first.get(0))) {
					continue;
				}

				symbols.put(f, t);
			}

			List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> equations = new LinkedList<>();
			for (Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>> eq : sig.equations) {
				if (!symbols.keySet().containsAll(eq.second.symbols()) || !symbols.keySet().containsAll(eq.third.symbols())) {
					continue;
				}
				equations.add(eq);
			}

			cache_A = new OplSig<>(sig.fr, sig.prec, new HashSet<>(sig.sorts), symbols, equations);
			return cache_A;
		}

		OplSCHEMA0<S, C, V> schema0;

		public OplSCHEMA0<S, C, V> toSchema0() {
			if (sig == null) {
				throw new RuntimeException("Report this error to Ryan.");
			}
			if (schema0 != null) {
				return schema0;
			}
			List<Triple<OplCtx<S, V>, OplTerm<C, V>, OplTerm<C, V>>> set = new LinkedList<>(projEA().equations);
			set.removeAll(projE().equations);
			schema0 = new OplSCHEMA0<>(sig == null ? new HashMap<>() : sig.prec, entities, projE().symbols, projA().symbols, projE().equations, obsEqs(), sig0 == null ? "?" : sig0);
			schema0.typeSide = forSchema0;
			return schema0;
		}

		@Override
		public String toString() {
			List<String> l = entities.stream().map(x -> Util.maybeQuote(OplTerm.strip(x.toString()))).collect(Collectors.toList());
			if (sig == null) {
				return "schema {\n entities\n  " + Util.sep(l, ", ") + ";\n} : " + sig0;
			}
			return toSchema0().toString();
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((entities == null) ? 0 : entities.hashCode());
			result = prime * result + ((sig0 == null) ? 0 : sig0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSchema other = (OplSchema) obj;
			if (entities == null) {
				if (other.entities != null)
					return false;
			} else if (!entities.equals(other.entities))
				return false;
			if (sig0 == null) {
				if (other.sig0 != null)
					return false;
			} else if (!sig0.equals(other.sig0))
				return false;
			return true;
		}

	}

	public static class OplSchemaProj<S, C, V> extends OplExp {
		final String which;
		final String sch0;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((sch0 == null) ? 0 : sch0.hashCode());
			result = prime * result + ((which == null) ? 0 : which.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplSchemaProj other = (OplSchemaProj) obj;
			if (sch0 == null) {
				if (other.sch0 != null)
					return false;
			} else if (!sch0.equals(other.sch0))
				return false;
			if (which == null) {
				if (other.which != null)
					return false;
			} else if (!which.equals(other.which))
				return false;
			return true;
		}

		public OplSchemaProj(String sch0, String which) {
			this.sch0 = sch0;
			this.which = which;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public OplSig<S, C, V> proj(OplSchema<S, C, V> sig) {
			switch (which) {
				case "E":
					return sig.projE();
				case "A":
					return sig.projA();
				case "T":
					return sig.projT();
				case "EA":
					return sig.projEA();
			default:
				break;
			}
			throw new RuntimeException();
		}
	}

	public static class OplInst0<S, C, V, X> extends OplExp {

		final OplPres<S, C, V, X> P;

		public Set<String> imports = new HashSet<>();
		final String initP;

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public OplInst0(OplPres<S, C, V, X> p) {
			P = p;
			initP = p.toString();
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((imports == null) ? 0 : imports.hashCode());
			result = prime * result + ((initP == null) ? 0 : initP.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplInst0 other = (OplInst0) obj;
			if (imports == null) {
				if (other.imports != null)
					return false;
			} else if (!imports.equals(other.imports))
				return false;
			if (initP == null) {
				if (other.initP != null)
					return false;
			} else if (!initP.equals(other.initP))
				return false;
			return true;
		}

		@Override
		public String toString() {
			String x = P.toString();
			int j = "presentation ".length();
			return "INSTANCE " + x.substring(j);
		}

	}

	public static class OplInst<S, C, V, X> extends OplExp {
		final String P0;
        final String J0;
        final String S0;
		OplPres<S, C, V, X> P;
		OplJavaInst<S, C, V> J;
		OplSchema<S, C, V> S;

		public OplPres<S, C, V, X> projE() {
			return proj(S.projE());
		}

		public OplPres<S, C, V, X> projEA() {
			return proj(S.projEA());
		}

		public OplPres<S, C, V, X> proj(OplSig<S, C, V> sig) {
			// OplSig<S, C, V> sig = S.projE();

			Map<X, S> gens = new HashMap<>();
			for (X x : P.gens.keySet()) {
				S s = P.gens.get(x);
				// if (S.entities.contains(s)) {
				gens.put(x, s);
				// }
			}

			List<Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>>> equations = new LinkedList<>();
			for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> eq : P.equations) {
				Set<Chc<C, X>> set = new HashSet<>();
				set.addAll(eq.first.symbols());
				set.addAll(eq.second.symbols());

				Set<C> set1 = new HashSet<>();
				Set<X> set2 = new HashSet<>();
				for (Chc<C, X> cx : set) {
					if (cx.left) {
						set1.add(cx.l);
					} else {
						set2.add(cx.r);
					}
				}

				if (!gens.keySet().containsAll(set2)) {
					continue;
				}
				if (!sig.symbols.keySet().containsAll(set1)) {
					continue;
				}
				equations.add(eq);
			}

			OplPres<S, C, V, X> ret = new OplPres<>(P.prec, "?", sig, gens, equations);

			return ret;
		}

		public OplInst(String S0, String P0, String J0) {
			this.P0 = P0;
			this.J0 = J0;
			this.S0 = S0;
		}

		void validate(OplSchema<S, C, V> S, OplPres<S, C, V, X> P, OplJavaInst J) {
			if (!S.sig.equals(P.sig)) {
				throw new RuntimeException("Presentation not on expected theory: \n\nschema sig " + S.sig + "\n\npres sig " + P.sig);
			}
			if (J != null && !J.sig0.equals(S.projT())) {
				throw new RuntimeException("JS model not on expected theory:\n\n" + S.projT() + "\n\nvs\n\n" + J.sig0);
			}
			if (J != null) {
				if (DefunctGlobalOptions.debug.opl.opl_safe_java && !S.projT().equations.isEmpty()) {
					throw new RuntimeException("With safe java option enabled, type sides cannot have equations");
				}
			}

			this.P = P;
			this.J = J;
			this.S = S;
			P.toSig().validate();
			saturate();
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			String x = DefunctGlobalOptions.debug.opl.opl_print_simplified_presentations ? P.simplify().toString() : P.toString();

			int j = "presentation ".length();
			return "INSTANCE " + x.substring(j);
		}

		@Override
		public String toHtml() {
			return OplTerm.strip(saturate().fourth.toHtml(S.projT().sorts));
		}

		@Override
		public JComponent display() {
			JTabbedPane ret = new JTabbedPane();

			CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", toString());
			ret.add(p, "Presentation");

			try {
				Quad<OplSetInst<S, C, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplPres<S, C, V, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<C, X>, V>>> xxx = saturate();

				String xxxthird = DefunctGlobalOptions.debug.opl.opl_print_simplified_presentations ? xxx.third.simplify().toString() : xxx.third.toString();

				ret.add(new CodeTextPanel(BorderFactory.createEtchedBorder(), "", xxxthird), "Type Algebra");

				ret.add(xxx.first.makeTables(S.projT().sorts, false).first, "Saturation");

				if (DefunctGlobalOptions.debug.opl.opl_display_fresh_ids) {
					Pair<JComponent, Map<Object, Pair<List<String>, List<Object[]>>>> ggg = xxx.fourth.number(S.projT().sorts).makeTables(S.projT().sorts, false);
					ret.add(ggg.first, "Normalized");
					ret.add(new CodeTextPanel("", makeCsv(ggg.second, S.projT().sorts)), "CSV");
				} else {
					Pair<JComponent, Map<Object, Pair<List<String>, List<Object[]>>>> ggg = xxx.fourth.makeTables(S.projT().sorts, false);
					ret.add(ggg.first, "Normalized");
					ret.add(new CodeTextPanel("", makeCsv(ggg.second, S.projT().sorts)), "CSV");
				}
				if (xxx.second != null) {
					ret.add(xxx.second.makeTables(S.projT().sorts, false).first, "Image");
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				ret.add(new CodeTextPanel(BorderFactory.createEtchedBorder(), "Exception", ex.getMessage()), "Error");
			}
			return ret;

		}

		Quad<OplSetInst<S, C, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplPres<S, C, V, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<C, X>, V>>> saturation = null;

		public

				Quad<OplSetInst<S, C, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplPres<S, C, V, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<C, X>, V>>> saturate() {
			if (saturation != null) {
				return saturation;
			}
			saturation = saturate(J, projEA(), S, P);
			return saturation;
		}

		/**
		 * saturate, js image, typeAlg, normalized
		 */
		public static <S, C, V, X> Quad<OplSetInst<S, C, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplPres<S, C, V, OplTerm<Chc<C, X>, V>>, OplSetInst<S, C, OplTerm<Chc<C, X>, V>>> saturate(OplJavaInst I0, OplPres<S, C, V, X> P0, OplSchema<S, C, V> S, OplPres<S, C, V, X> P) {

			OplSetInst<S, C, OplTerm<Chc<C, X>, V>> I = OplSat.saturate(P0); // P0
																				// =
																				// projEA

			OplPres<S, C, V, OplTerm<Chc<C, X>, V>> T = typeAlg(I, S, P, P0);

			if (I0 != null && DefunctGlobalOptions.debug.opl.opl_safe_java) {
				OplPres.checkFreeExtension(T);
			}

			OplSetInst<S, C, OplTerm<Chc<C, X>, V>> X = reduce(I, P); // bulk of
																		// time
																		// is
																		// spent
																		// doing
																		// KB
																		// completion
																		// for n

			OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>> J = null;
			if (I0 != null) {
				J = inject(X, I0);
			}

			return new Quad<>(I, J, T, X);
		}

		public static <S, C, V, X> OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>> inject(OplSetInst<S, C, OplTerm<Chc<C, X>, V>> I, OplJavaInst I0) {
			if (I0 == null) {
				throw new RuntimeException();
			}
			Map<S, Set<OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>> sorts = new HashMap<>();
			for (S s : I.sorts.keySet()) {
				Set<OplTerm<Chc<Chc<C, X>, JSWrapper>, V>> set = new HashSet<>();
				for (OplTerm<Chc<C, X>, V> e : I.sorts.get(s)) {
					OplTerm<Chc<Chc<C, X>, JSWrapper>, V> kkk = e.inLeft();
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> jjj = OplToKB.convert(kkk);
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> z = OplToKB.redBy(I0, jjj);
					OplTerm<Chc<Chc<C, X>, JSWrapper>, V> ee = OplToKB.convert(z);
					set.add(ee);
				}
				sorts.put(s, set);
			}

			Map<C, Map<List<OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>> symbols = new HashMap<>();
			for (C c : I.symbols.keySet()) {
				Map<List<OplTerm<Chc<Chc<C, X>, JSWrapper>, V>>, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>> map = new HashMap<>();
				for (List<OplTerm<Chc<C, X>, V>> args : I.symbols.get(c).keySet()) {
					List<OplTerm<Chc<Chc<C, X>, JSWrapper>, V>> args0 = new LinkedList<>();
					for (OplTerm<Chc<C, X>, V> arg : args) {
						OplTerm<Chc<Chc<C, X>, JSWrapper>, V> kkk = arg.inLeft();
						KBExp<Chc<Chc<C, X>, JSWrapper>, V> jjj = OplToKB.convert(kkk);
						KBExp<Chc<Chc<C, X>, JSWrapper>, V> z = OplToKB.redBy(I0, jjj);
						OplTerm<Chc<Chc<C, X>, JSWrapper>, V> arg0 = OplToKB.convert(z);
						args0.add(arg0);
					}
					OplTerm<Chc<Chc<C, X>, JSWrapper>, V> kkk = I.symbols.get(c).get(args).inLeft();
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> jjj = OplToKB.convert(kkk);
					KBExp<Chc<Chc<C, X>, JSWrapper>, V> z = OplToKB.redBy(I0, jjj);
					OplTerm<Chc<Chc<C, X>, JSWrapper>, V> ee = OplToKB.convert(z);
					map.put(args0, ee);
				}
				symbols.put(c, map);
			}

			OplSetInst<S, C, OplTerm<Chc<Chc<C, X>, JSWrapper>, V>> ret = new OplSetInst<>(sorts, symbols, I.sig);
			ret.validate(I.sig0);
			return ret;

		}

		// morally, should reduce by type algebra, but this should be equivalent
		public static <S, C, V, X> OplSetInst<S, C, OplTerm<Chc<C, X>, V>> reduce(OplSetInst<S, C, OplTerm<Chc<C, X>, V>> I, OplPres<S, C, V, X> P) {
			Map<S, Set<OplTerm<Chc<C, X>, V>>> sorts = new HashMap<>();
			for (S s : I.sorts.keySet()) {
				Set<OplTerm<Chc<C, X>, V>> set = new HashSet<>();
				for (OplTerm<Chc<C, X>, V> e : I.sorts.get(s)) {
					set.add(P.toSig().getKB().nf(e));
				}
				sorts.put(s, set);
			}

			Map<C, Map<List<OplTerm<Chc<C, X>, V>>, OplTerm<Chc<C, X>, V>>> symbols = new HashMap<>();
			for (C c : I.symbols.keySet()) {
				Map<List<OplTerm<Chc<C, X>, V>>, OplTerm<Chc<C, X>, V>> map = new HashMap<>();
				for (List<OplTerm<Chc<C, X>, V>> args : I.symbols.get(c).keySet()) {
					List<OplTerm<Chc<C, X>, V>> args0 = new LinkedList<>();
					for (OplTerm<Chc<C, X>, V> arg : args) {
						OplTerm<Chc<C, X>, V> arg0 = P.toSig().getKB().nf(arg);
						args0.add(arg0);
					}
					map.put(args0, P.toSig().getKB().nf(I.symbols.get(c).get(args)));
				}
				symbols.put(c, map);
			}

			OplSetInst<S, C, OplTerm<Chc<C, X>, V>> ret = new OplSetInst<>(sorts, symbols, I.sig);
			ret.validate(I.sig0);
			return ret;
		}

		public static <S, C, V, X> OplPres<S, C, V, OplTerm<Chc<C, X>, V>> typeAlg(OplSetInst<S, C, OplTerm<Chc<C, X>, V>> I, OplSchema<S, C, V> S, OplPres<S, C, V, X> P, OplPres<S, C, V, X> P0) {
			Map<OplTerm<Chc<C, X>, V>, S> gens = new HashMap<>();
			List<Pair<OplTerm<Chc<C, OplTerm<Chc<C, X>, V>>, V>, OplTerm<Chc<C, OplTerm<Chc<C, X>, V>>, V>>> eqs = new LinkedList<>();

			List<OplTerm<Chc<C, X>, V>> allgens = new LinkedList<>();
			for (S s : S.projT().sorts) {
				for (OplTerm<Chc<C, X>, V> e : I.sorts.get(s)) {
					gens.put(e, s);
					allgens.add(e);
				}
			}

			for (Pair<OplTerm<Chc<C, X>, V>, OplTerm<Chc<C, X>, V>> eq : P.equations) {
				P.toSig();
				S.projT();
				if (!S.projT().sorts.contains(eq.first.type(P.toSig(), new OplCtx<>()))) {
					continue; // only process equations at types
				}

				eqs.add(new Pair<>(conv(S, eq.first, P0), conv(S, eq.second, P0)));
			}

			allgens.sort((o1, o2) -> {
				if (o1.equals(o2)) {
					return 0;
				}
				OplKB<Chc<C, X>, V> kb = P0.toSig.getKB().KB;
				if (kb.gt.apply(new Pair<>(OplToKB.convert(o1), OplToKB.convert(o2)))) {
					return 1;
				}
				return 0;
			});
			int j = S.projT().largestPrec();
			Map<OplTerm<Chc<C, X>, V>, Integer> prec = new HashMap<>();
			for (int i = 0; i < allgens.size(); i++) {
				prec.put(allgens.get(i), j + 1 + i);
			}
			OplPres<S, C, V, OplTerm<Chc<C, X>, V>> ret = new OplPres<>(prec, "?", S.projT(), gens, eqs);

			ret.toSig();

			if (DefunctGlobalOptions.debug.opl.opl_require_consistency) {
				OplPres.checkFreeExtension(ret);
			}

			return ret;
		}

		// this is important and should be called out somehow
		public static <S, C, V, X> OplTerm<Chc<C, OplTerm<Chc<C, X>, V>>, V> conv(OplSchema<S, C, V> S, OplTerm<Chc<C, X>, V> e0, OplPres<S, C, V, X> P0) {
			OplTerm<Chc<C, X>, V> e = P0.toSig().getKB().nf(e0);
			if (e.var != null) {
				throw new RuntimeException();
			}

			Chc<C, X> h = e.head;
			// base case, generator in instance, skolem term
			if (!h.left) {
				if (!e.args.isEmpty()) {
					throw new RuntimeException();
				}
				return new OplTerm<>(Chc.inRight(e), new LinkedList<>());
			}

			C c = h.l;
			if (S.projT().symbols.containsKey(c)) {
				List<OplTerm<Chc<C, OplTerm<Chc<C, X>, V>>, V>> l = new LinkedList<>();
				for (OplTerm<Chc<C, X>, V> arg : e.args) {
					OplTerm<Chc<C, OplTerm<Chc<C, X>, V>>, V> arg0 = conv(S, arg, P0);
					l.add(arg0);
				}
				return new OplTerm<>(Chc.inLeft(c), l);
			} else if (S.projA().symbols.containsKey(c)) {
				return new OplTerm<>(Chc.inRight(e), new LinkedList<>());
			} else {
				throw new RuntimeException("Cannot find " + c + " in type side");
			}

		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((J0 == null) ? 0 : J0.hashCode());
			result = prime * result + ((P0 == null) ? 0 : P0.hashCode());
			result = prime * result + ((S0 == null) ? 0 : S0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplInst other = (OplInst) obj;
			if (J0 == null) {
				if (other.J0 != null)
					return false;
			} else if (!J0.equals(other.J0))
				return false;
			if (P0 == null) {
				if (other.P0 != null)
					return false;
			} else if (!P0.equals(other.P0))
				return false;
			if (S0 == null) {
				if (other.S0 != null)
					return false;
			} else if (!S0.equals(other.S0))
				return false;
			return true;
		}

	}

	public static class OplApply extends OplExp {
		final String Q0;
        final String I0;
		OplQuery Q;

		public OplApply(String Q0, String I0) {
			this.Q0 = Q0;
			this.I0 = I0;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I0 == null) ? 0 : I0.hashCode());
			result = prime * result + ((Q == null) ? 0 : Q.hashCode());
			result = prime * result + ((Q0 == null) ? 0 : Q0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplApply other = (OplApply) obj;
			if (I0 == null) {
				if (other.I0 != null)
					return false;
			} else if (!I0.equals(other.I0))
				return false;
			if (Q == null) {
				if (other.Q != null)
					return false;
			} else if (!Q.equals(other.Q))
				return false;
			if (Q0 == null) {
				if (other.Q0 != null)
					return false;
			} else if (!Q0.equals(other.Q0))
				return false;
			return true;
		}

	}

	public static class OplId extends OplExp {
		final String s;
        final String kind;

		public OplId(String s, String kind) {
			this.s = s;
			this.kind = kind;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((kind == null) ? 0 : kind.hashCode());
			result = prime * result + ((s == null) ? 0 : s.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplId other = (OplId) obj;
			if (kind == null) {
				if (other.kind != null)
					return false;
			} else if (!kind.equals(other.kind))
				return false;
			if (s == null) {
				if (other.s != null)
					return false;
			} else if (!s.equals(other.s))
				return false;
			return true;
		}

	}

	public static class OplUnion extends OplExp {
		final Set<String> names;
		String base;

		public OplUnion(List<String> names, String base) {
			this.names = new HashSet<>(names);
			if (names.size() != this.names.size()) {
				throw new RuntimeException("Error: duplicates in " + names);
			}
			this.base = base;
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((base == null) ? 0 : base.hashCode());
			result = prime * result + ((names == null) ? 0 : names.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplUnion other = (OplUnion) obj;
			if (base == null) {
				if (other.base != null)
					return false;
			} else if (!base.equals(other.base))
				return false;
			if (names == null) {
				if (other.names != null)
					return false;
			} else if (!names.equals(other.names))
				return false;
			return true;
		}

	}

	public static class OplTyMapping<S1, C1, V, S2, C2> extends OplExp {
		final String src0;
        final String dst0;
		final OplSchema<S1, C1, V> src;
		final OplSchema<S2, C2, V> dst;
		final OplMapping<S1, C1, V, S2, C2> m;

		Set<String> imports = new HashSet<>();

		public OplTyMapping(String src0, String dst0, OplSchema<S1, C1, V> src, OplSchema<S2, C2, V> dst, OplMapping<S1, C1, V, S2, C2> m) {
			this.src0 = src0;
			this.dst0 = dst0;
			this.src = src;
			this.dst = dst;
			this.m = m;
			validate();
		}

		private void validate() {
			if (!src.projT().equals(dst.projT())) {
				throw new RuntimeException("Differing type sides");
			}
			extend().validate(src.sig, dst.sig);
		}

		private OplMapping<S1, C1, V, S2, C2> cache;

		public static <S, C, V> OplTyMapping<S, C, V, S, C> id(String n, OplSchema<S, C, V> sig) {

			Map<S, S> sorts = new HashMap<>();
			Map<C, Pair<OplCtx<S, V>, OplTerm<C, V>>> symbols = new HashMap<>();

			for (S s : sig.entities) {
				sorts.put(s, s);
			}

			for (C c : sig.projEA().symbols.keySet()) {
				Pair<List<S>, S> t = sig.projEA().symbols.get(c);
				List<Pair<V, S>> l = new LinkedList<>();
				List<OplTerm<C, V>> vs = new LinkedList<>();
				for (S s1 : t.first) {
					V v = sig.sig.fr.next();
					vs.add(new OplTerm<>(v));
					S s = s1;
					l.add(new Pair<>(v, s));
				}
				OplCtx<S, V> ctx = new OplCtx<>(l);
				OplTerm<C, V> value = new OplTerm<>(c, vs);
				symbols.put(c, new Pair<>(ctx, value));
			}
			OplMapping<S, C, V, S, C> mapping = new OplMapping<>(sorts, symbols, n, n);

			OplTyMapping<S, C, V, S, C> tm = new OplTyMapping<>(n, n, sig, sig, mapping);
			tm.extend().validate(sig.sig, sig.sig);

			return tm;
		}

		public OplMapping<S1, C1, V, S2, C2> extend() {
			if (cache != null) {
				return cache;
			}

			Map<S1, S2> sorts = new HashMap<>(m.sorts);
			Map<C1, Pair<OplCtx<S2, V>, OplTerm<C2, V>>> symbols = new HashMap<>(m.symbols);

			for (S1 s1 : src.projT().sorts) {
				S2 s2 = (S2) s1;
				sorts.put(s1, s2);
			}

			for (C1 c1 : src.projT().symbols.keySet()) {
				Pair<List<S1>, S1> t = src.projT().symbols.get(c1);
				List<Pair<V, S2>> l = new LinkedList<>();
				List<OplTerm<C2, V>> vs = new LinkedList<>();
				for (S1 s1 : t.first) {
					V v = src.sig.fr.next();
					vs.add(new OplTerm<>(v));
					S2 s2 = (S2) s1;
					l.add(new Pair<>(v, s2));
				}
				OplCtx<S2, V> ctx = new OplCtx<>(l);
				C2 c2 = (C2) c1;
				OplTerm<C2, V> value = new OplTerm<>(c2, vs);
				symbols.put(c1, new Pair<>(ctx, value));
			}
			cache = new OplMapping<>(sorts, symbols, src0, dst0);

			return cache;
		}

		@Override
		public JComponent display() {
			return extend().display();
		}

		@Override
		public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((cache == null) ? 0 : cache.hashCode());
			result = prime * result + ((dst0 == null) ? 0 : dst0.hashCode());
			result = prime * result + ((m == null) ? 0 : m.hashCode());
			result = prime * result + ((src0 == null) ? 0 : src0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OplTyMapping other = (OplTyMapping) obj;
			if (cache == null) {
				if (other.cache != null)
					return false;
			} else if (!cache.equals(other.cache))
				return false;
			if (dst0 == null) {
				if (other.dst0 != null)
					return false;
			} else if (!dst0.equals(other.dst0))
				return false;
			if (m == null) {
				if (other.m != null)
					return false;
			} else if (!m.equals(other.m))
				return false;
			if (src0 == null) {
				if (other.src0 != null)
					return false;
			} else if (!src0.equals(other.src0))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "OplTyMapping [src0=" + src0 + ", dst0=" + dst0 + ", src=" + src + ", dst=" + dst + ", m=" + m + "]";
		}

	}

	public interface OplExpVisitor<R, E> {
		R visit(E env, OplTyMapping e);

		R visit(E env, OplId e);

		R visit(E env, OplApply e);

		R visit(E env, OplSig e);

		R visit(E env, OplPres e);

		R visit(E env, OplSetInst e);

		R visit(E env, OplEval e);

		R visit(E env, OplVar e);

		R visit(E env, OplSetTrans e);

		R visit(E env, OplJavaInst e);

		R visit(E env, OplMapping e);

		R visit(E env, OplDelta e);

		R visit(E env, OplSigma e);

		R visit(E env, OplSat e);

		R visit(E env, OplUnSat e);

		R visit(E env, OplPresTrans e);

		R visit(E env, OplUberSat e);

		R visit(E env, OplFlower e);

		R visit(E env, OplSchema e);

		R visit(E env, OplSchemaProj e);

		R visit(E env, OplInst e);

		R visit(E env, OplQuery e);

		R visit(E env, OplSCHEMA0 e);

		R visit(E env, OplInst0 e);

		R visit(E env, OplPushout e);

		R visit(E env, OplPushoutSch e);

		R visit(E env, OplDelta0 e);

		R visit(E env, OplPivot e);

		R visit(E env, OplPushoutBen e);

		R visit(E env, OplUnion e);

		R visit(E env, OplPragma e);

		R visit(E env, OplColim e);

		R visit(E env, OplChaseExp e);

		R visit(E env, OplGround e);

		R visit(E env, OplGraph e);

		R visit(E env, OplDistinct e);
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("serial")
	static class NonEditableModel extends DefaultTableModel {

		NonEditableModel(Object[][] data, String... columnNames) {
			super(data, columnNames);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return true;
		}
	}

	private static JComponent doSchemaView(Color clr, Graph sgv, Set entities) {
		if (sgv.getVertexCount() == 0) {
			return new JPanel();
		}
		Layout layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer vv = new VisualizationViewer<>(layout);
		Function vertexPaint = x -> entities.contains(x) ? clr : null;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function ttt = arg0 -> OplTerm.strip(arg0.toString());
		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(ttt);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	private static String makeCsv(Map<Object, Pair<List<String>, List<Object[]>>> map, Set<?> skip) {
		List<String> tables = new LinkedList<>();

		for (Object o : map.keySet()) {
			if (skip.contains(o)) {
				continue;
			}
			List<String> rows = new LinkedList<>();
			Pair<List<String>, List<Object[]>> x = map.get(o);
			rows.add(Util.sep(x.first, ","));
			for (Object[] row : x.second) {
				rows.add(Util.sep(Arrays.asList(row), ","));
			}
			tables.add(Util.sep(rows, "\n"));
		}

		return OplTerm.strip(Util.sep(tables, "\n\n"));
	}

	private static <S, C, V> Graph<S, C> buildFromSig(OplSig<S, C, V> c) {
		Graph<S, C> g2 = new DirectedSparseMultigraph<>();
		for (S n : c.sorts) {
			g2.addVertex(n);
		}
		for (C e : c.symbols.keySet()) {
			Pair<List<S>, S> t = c.symbols.get(e);
			if (t.first.size() != 1) {
				throw new RuntimeException();
			}
			g2.addEdge(e, t.first.get(0), t.second);
		}
		return g2;
	}

}

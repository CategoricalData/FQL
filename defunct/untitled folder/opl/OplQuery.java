package catdata.opl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.ide.CodeTextPanel;
import catdata.ide.DefunctGlobalOptions;
import catdata.opl.OplParser.DoNotIgnore;
import catdata.provers.KBExp;


public class OplQuery<S1, C1, V1, S2, C2, V2> extends OplExp {

	OplSchema<S1, C1, V1> src;
	private OplSchema<S2, C2, V2> dst;

	final String src_e;
    final String dst_e;

	Map<Object, Pair<S2, Block<S1, C1, V1, S2, C2, V2>>> blocks = new HashMap<>();

	public OplQuery(String src_e, String dst_e,
			Map<Object, Pair<S2, Block<S1, C1, V1, S2, C2, V2>>> blocks) {
		this.src_e = src_e;
		this.dst_e = dst_e;
		this.blocks = blocks;
	}

	public Map<Object, OplPres<S1, C1, V1, V1>> fI;
	//private Map<Pair<Object, C2>, OplPresTrans<S1, C1, V1, V1, V1>> fE;

	private static <C, V> OplTerm<Chc<C, V>, V> freeze(OplTerm<C, V> t) {
		if (t.var != null) {
			return new OplTerm<>(Chc.inRight(t.var), new LinkedList<>());
		}
		List<OplTerm<Chc<C, V>, V>> args = new LinkedList<>();
		for (OplTerm<C, V> arg : t.args) {
			args.add(freeze(arg));
		}
		return new OplTerm<>(Chc.inLeft(t.head), args);
	}

	private void freeze() { 
		fI = new HashMap<>();
		for (Object l : blocks.keySet()) {
			Block<S1, C1, V1, S2, C2, V2> block = blocks.get(l).second;
			
			List<Pair<OplTerm<Chc<C1, V1>, V1>, OplTerm<Chc<C1, V1>, V1>>> eqs = new LinkedList<>();
			for (Pair<OplTerm<C1, V1>, OplTerm<C1, V1>> eq : block.where) {
				eqs.add(new Pair<>(freeze(eq.first), freeze(eq.second)));
			}

			OplPres<S1, C1, V1, V1> xx = new OplPres<>(new HashMap<>(), "?", src.sig,
					new HashMap<>(block.from), eqs);
			xx.toSig(); // validates
			fI.put(l, xx);
		}
		//fE = new HashMap<>();
		for (Object l : blocks.keySet()) {
			Block<S1, C1, V1, S2, C2, V2> block = blocks.get(l).second;
			//S2 s2 = blocks.get(l).first;
			for (C2 c2 : block.edges.keySet()) {
				Pair<Object, Map<V1, OplTerm<C1, V1>>> l0f = block.edges.get(c2);

				Map<S1, Map<V1, OplTerm<Chc<C1, V1>, V1>>> map = new HashMap<>();
				for (V1 v1 : l0f.second.keySet()) {
					OplTerm<C1, V1> t = l0f.second.get(v1);
					S1 s1 = fI.get(l0f.first).gens.get(v1);
                    Map<V1, OplTerm<Chc<C1, V1>, V1>> m = map.computeIfAbsent(s1, k -> new HashMap<>());
                    m.put(v1, freeze(t));
				
				}
				// validates
				try {
					@SuppressWarnings("unused")
					OplPresTrans<S1, C1, V1, V1, V1> xx = new OplPresTrans<>(map,
						"?", "?", fI.get(l0f.first), fI.get(l));
			
					//fE.put(new Pair<>(l, c2), xx);
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new RuntimeException("Error in block " + l + " edge " + c2 + " " + ex.getMessage());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void validate(OplSchema<S1, C1, V1> src, OplSchema<S2, C2, V2> dst) {
		this.src = src;
		this.dst = dst;

		if (!src.projT().equals(dst.projT())) {
			throw new RuntimeException("Source and Target of query have Different type sides.\n\nsrc=" + src.projT() + "\n\ndst=" + dst.projT());
		}

		for (Object b : blocks.keySet()) {
			Pair<S2, Block<S1, C1, V1, S2, C2, V2>> block0 = blocks.get(b);
			S2 s2 = block0.first;
			Block<S1, C1, V1, S2, C2, V2> block = block0.second;

			if (!dst.projE().sorts.contains(s2)) {
				throw new RuntimeException("In block " + b + ", " + s2 + " is not a target entity.");
			}

			for (V1 v1 : block.from.keySet()) {
				S1 s1 = block.from.get(v1);
				if (!src.projE().sorts.contains(s1)) {
					throw new RuntimeException("In block " + b + ", " + s1
							+ " is not a source entity.");
				}
			}
		}

		for (Object b : blocks.keySet()) {
			Pair<S2, Block<S1, C1, V1, S2, C2, V2>> block0 = blocks.get(b);
			S2 s2 = block0.first;
			Block<S1, C1, V1, S2, C2, V2> block = block0.second;

			OplCtx<S1, V1> ctx = new OplCtx<>(block.from);

			for (Pair<OplTerm<C1, V1>, OplTerm<C1, V1>> eq : block.where) {
				S1 l = eq.first.type(src.sig, ctx);
				S1 r = eq.second.type(src.sig, ctx);
				if (!l.equals(r)) {
					throw new RuntimeException("In checking block " + b + ", different types for "
							+ eq.first + " = " + eq.second + ", " + l + " and " + r);
				}
			}

			for (C2 a : block.attrs.keySet()) {
				Pair<List<S2>, S2> t = dst.projA().symbols.get(a);
				if (t == null) {
					throw new RuntimeException("In checking block " + b + ", " + a
							+ " is not an attribute in " + dst_e);
				}
				Chc<Agg<S1, C1, V1, S2, C2, V2>, OplTerm<C1, V1>> ee = block.attrs.get(a); 
				S1 s1;
				try {
					if (ee.left) {
						ee.l.validate();
						s1 = ee.l.type(src.sig, ctx);
						Pair<List<S1>, S1> zero_t = src.sig.getSymbol(ee.l.zero);
						Pair<List<S1>, S1> plus_t = src.sig.getSymbol(ee.l.plus);
						if (!zero_t.first.isEmpty()) {
							throw new RuntimeException(ee.l.zero + " is not zero-ary");
						}
						if (plus_t.first.size() != 2) {
							throw new RuntimeException(ee.l.plus + " is not binary");
						}
						if (!zero_t.second.equals(s1)) {
							throw new RuntimeException("type of " + ee.l.zero + " is " + zero_t.first.get(0) + " but " + ee.l.att + " has type " + s1);
						}
						if (!plus_t.first.get(0).equals(s1) || !plus_t.first.get(1).equals(s1) || !plus_t.second.equals(s1)) {
							throw new RuntimeException("type of " + ee.l.plus + " is " + plus_t + " but " + ee.l.att + " has type " + s1);
						}
					} else {
						s1 = ee.r.type(src.sig, ctx);
					}
				} catch (RuntimeException ex) {
					ex.printStackTrace();
					throw new RuntimeException("In checking block " + b + " and attr " + a + ", " + ex.getMessage());
				}
				if (!s1.equals((S1)t.second)) {
					throw new RuntimeException("In checking block " + b + ", " + ee.toStringMash() + " has type "
							+ s1 + " but should be " + t.second);
				}
				if (!t.first.get(0).equals(s2)) {
					throw new RuntimeException("In checking att " + a + " in block " + b
							+ ", the att does not belong in the block ");
				}
			}
			for (C2 a : dst.projA().symbols.keySet()) {
				Pair<List<S2>, S2> t = dst.projA().symbols.get(a);
				if (t.first.size() != 1) {
					throw new RuntimeException("Internal error, report to Ryan");
				}
				if (!t.first.get(0).equals(s2)) {
					continue;
				}
				if (!block.attrs.containsKey(a)) {
					throw new RuntimeException("In checking block " + b + ", missing attribute: "
							+ a);
				}
			}
			for (C2 a : dst.projE().symbols.keySet()) {
				Pair<List<S2>, S2> t = dst.projE().symbols.get(a);
				if (t.first.size() != 1) {
					throw new RuntimeException("In checking block " + b + ", Internal error, report to Ryan");
				}
				if (!t.first.get(0).equals(s2)) {
					continue;
				}
				if (!block.edges.containsKey(a)) {
					throw new RuntimeException("In checking block " + b + ", Missing edge: " + a);
				}
			}

			for (C2 a : block.edges.keySet()) {
				Pair<Object, Map<V1, OplTerm<C1, V1>>> e = block.edges.get(a);
				
				if (e.first == null) {
					e = new Pair<>(dst.sig.symbols.get(a).second, e.second);
					block.edges.put(a, e);
				}
				
				Pair<S2, Block<S1, C1, V1, S2, C2, V2>> tgt = blocks.get(e.first);
				
				if (tgt == null) {
					throw new RuntimeException("In checking block " + b + ", Not a sub-query: " + e.first);
				}
				Pair<List<S2>, S2> t = dst.projE().symbols.get(a);
				if (t == null) {
					throw new RuntimeException("In checking block " + b + ", " + a
							+ " is not an edge in " + dst_e);
				}
				if (!t.second.equals(tgt.first)) {
					throw new RuntimeException("In checking edge " + a + " in block " + b
							+ ", the target entity for label " + e.first + " is " + tgt.first
							+ ", not " + t.second + " as expected");
				}

				OplCtx<S1, V1> tgtCtx = new OplCtx<>(tgt.second.from);
				Map<V1, S1> xxx = new HashMap<>();
				for (V1 v1 : e.second.keySet()) {
					xxx.put(v1, e.second.get(v1).type(src.sig, ctx));
				}
				OplCtx<S1, V1> tgtCtx2 = new OplCtx<>(xxx);
				if (!tgtCtx.equals(tgtCtx2)) {
					throw new RuntimeException("In checking edge " + a + " in block " + b
							+ ", the context for target block is " + tgtCtx + " but you provided bindings "
							+ tgtCtx2);
				}
				
				if (!t.first.get(0).equals(s2)) {
					throw new RuntimeException("In checking edge " + a + " in block " + b
							+ ", the edge does not belong in the block ");
				}
			}
		}

		freeze();
		if (DefunctGlobalOptions.debug.opl.opl_query_check_eqs) {
			checkPaths();
		}
	}
	
	private void checkPaths() {
		for (Triple<OplCtx<S2, V2>, OplTerm<C2, V2>, OplTerm<C2, V2>> eq : dst.sig.equations) {
			if (eq.first.vars0.size() != 1) {
				continue;
			}
			Pair<V2, S2> aA = eq.first.values2().get(0);
			S2 A = aA.second;
//			V2 a = aA.first;
			if (!dst.entities.contains(A)) {
				continue;
			} 
			for (Object l2 : blocks.keySet()) {
				Pair<S2, Block<S1, C1, V1, S2, C2, V2>> block20 = blocks.get(l2);
				if (!block20.first.equals(A)) {
					continue;
				}

				S2 B = eq.second.type(dst.sig, eq.first);
				
				if (dst.entities.contains(B)) {
					for (Object l : blocks.keySet()) {
						Pair<S2, Block<S1, C1, V1, S2, C2, V2>> block0 = blocks.get(l);
						if (!block0.first.equals(B)) {
							continue;
						}
					
						for (V1 b : block0.second.from.keySet()) {
							OplTerm<C1, V1> lhs = convPath(new OplTerm<>(b), l2, eq.second);
							OplTerm<C1, V1> rhs = convPath(new OplTerm<>(b), l2, eq.third);
							OplTerm<Chc<C1, V1>, V1> lhs0 = fI.get(l2).toSig().getKB().nf(lhs.inLeft());
							OplTerm<Chc<C1, V1>, V1> rhs0 = fI.get(l2).toSig().getKB().nf(rhs.inLeft());
							
							Map<V1, OplTerm<Chc<C1, V1>, V1>> map = new HashMap<>();
							map.put(b, new OplTerm<>(Chc.inRight(b), new LinkedList<>()));
							OplTerm<Chc<C1, V1>, V1> lhs1 = lhs0.subst(map);
							OplTerm<Chc<C1, V1>, V1> rhs1 = rhs0.subst(map);
							if (!lhs1.equals(rhs1)) {
								throw new RuntimeException("equality " + eq.second + " = " + eq.third + " not preserved; becomes " + lhs1 + " = " + rhs1);
							}
						}
					}
				} else {
					OplTerm<C1, V1> lhs = convTerm(l2, eq.second);
					OplTerm<C1, V1> rhs = convTerm(l2, eq.third); 
					
					OplTerm<Chc<C1, V1>, V1> lhs0 = fI.get(l2).toSig().getKB().nf(squish(lhs.inLeft()));
					OplTerm<Chc<C1, V1>, V1> rhs0 = fI.get(l2).toSig().getKB().nf(squish(rhs.inLeft()));	
					
					if (!lhs0.equals(rhs0)) {
						throw new RuntimeException("on label " + l2 + " tgt equality " + eq.second + " = " + eq.third + 
								" not preserved; becomes " + lhs0 + " = " + rhs0 + " and eqs are " + fI.get(l2).toSig().getKB().printKB());
					}				
				}
			}
		}
		
	}
	
	private OplTerm<Chc<C1, V1>, V1> squish(OplTerm<Chc<C1, V1>, V1> t) {
		if (t.var != null) {
			return new OplTerm<>(Chc.inRight(t.var), new LinkedList<>());
		}
		List<OplTerm<Chc<C1, V1>, V1>> ret = new LinkedList<>();
		for (OplTerm<Chc<C1, V1>, V1> arg : t.args) {
			ret.add(squish(arg));
		}
		return new OplTerm<>(t.head, ret);
	}

	private OplTerm<C1, V1> findBlock(C2 att) {
		OplTerm<C1, V1> ret = null;
		for (Object l : blocks.keySet()) {
			if (blocks.get(l).second.attrs.containsKey(att)) {
				if (ret != null) {
					throw new RuntimeException("Cannot check path equalities for non-conjunctive queries. (Disable option to proceed)");
				} 
					ret = blocks.get(l).second.attrs.get(att).r; 
				

			}
		}
		if (ret == null) {
			throw new RuntimeException("Cannot check path equalities for non-conjunctive queries.  (Disable option to proceed)");
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
    private OplTerm<C1, V1> convTerm(Object l2, OplTerm<C2, V2> t) {
		if (t.var != null) {
			throw new RuntimeException();
		}
		if (dst.projA().symbols.containsKey(t.head)) {
			OplTerm<C1, V1> xxx = findBlock(t.head);
			return convPath(xxx, l2, t.args.get(0));
		}
		List<OplTerm<C1, V1>> args0 = new LinkedList<>();
		for (OplTerm<C2, V2> arg : t.args) {
			args0.add(convTerm(l2, arg));
		}
		return new OplTerm<>((C1)t.head, args0);
	}
	
	private OplTerm<C1, V1> convPath(OplTerm<C1, V1> base, Object l2, OplTerm<C2, V2> eqs) {
		if (base == null) { throw new RuntimeException(); }
		return subst(base, Util.reverse(trace(l2, eqs)));
	} 
	
	private static <X, Y> List<X> linearize(OplTerm<X, Y> t) {
		if (t.var != null) {
			return new LinkedList<>();
		}
		List<X> ret = new LinkedList<>(linearize(t.args.get(0)));
		ret.add(t.head);
		return ret;
	}

	private List<Pair<C2, Object>> trace(Object l, OplTerm<C2, V2> t) {
		List<Pair<C2, Object>> ret = new LinkedList<>();
		List<C2> order = linearize(t);
		
		for (C2 c2 : order) {
			ret.add(new Pair<>(c2, l));
			Pair<S2, Block<S1, C1, V1, S2, C2, V2>> b = blocks.get(l);
			l = b.second.edges.get(c2).first;
		}
		
		return ret;
	} 
	
	private OplTerm<C1, V1> subst(OplTerm<C1, V1> b, List<Pair<C2, Object>> ll) {
		if (b == null) { throw new RuntimeException(); }
		for (Pair<C2, Object> l : ll) {
			Pair<Object, Map<V1, OplTerm<C1, V1>>> nb = blocks.get(l.second).second.edges.get(l.first);
			if (nb == null) {
				throw new RuntimeException("No " + l.first + " at " + l.second);
			}
			b = b.subst(nb.second);
		}
		
		return b;
	}

	private List<V1> order(Block<S1, C1, V1, S2, C2, V2> block) {
		Map<V1, Integer> counts = new HashMap<>();
		List<V1> ret = new LinkedList<>();
		for (V1 v1 : block.from.keySet()) {
			counts.put(v1, 0);
			ret.add(v1);
		}
		if (!DefunctGlobalOptions.debug.opl.opl_reorder_joins) {
			return ret;
		}
		for (Pair<OplTerm<C1, V1>, OplTerm<C1, V1>> eq : block.where) {
			inc(eq.first, counts);
			inc(eq.second, counts);
		}
		ret.sort((V1 o1, V1 o2) -> counts.get(o2).compareTo(counts.get(o1)));
		return ret;
	}

	private void inc(OplTerm<C1, V1> t, Map<V1, Integer> counts) {
		if (t.var != null) {
			counts.put(t.var, counts.get(t.var) + 1);
			return;
		}
		for (OplTerm<C1, V1> arg : t.args) {
			inc(arg, counts);
		}
	}

	@SuppressWarnings("unused")
	public static class Agg<S1, C1, V1, S2, C2, V2> {
		final String orig;

		final C1 zero;
        final C1 plus;
		final LinkedHashMap<V1, S1> from;
		final Set<Pair<OplTerm<C1, V1>, OplTerm<C1, V1>>> where;
		final OplTerm<C1, V1> att;
		
		public Agg(C1 zero, C1 plus, LinkedHashMap<V1, S1> from, Set<Pair<OplTerm<C1, V1>, OplTerm<C1, V1>>> where, OplTerm<C1, V1> att) {
			this.zero = zero;
			this.plus = plus;
			this.from = from;
			this.where = where;
			this.att = att;
			orig = toString();
		}
		
		@SuppressWarnings("static-method")
		public void validate() {
			if (!DefunctGlobalOptions.debug.opl.opl_secret_agg) {
				throw new DoNotIgnore("To use ad-hoc aggregation, enable opl_secret_agg");
			}
		}

		public S1 type(OplSig<S1, C1, V1> sig, OplCtx<S1, V1> ctx) {
			LinkedHashMap<V1, S1> map = new LinkedHashMap<>(ctx.vars0);
			map.putAll(from);
			OplCtx<S1, V1> ctx2 = new OplCtx<>(map);
			return att.type(sig, ctx2);
		}

		@Override
		public String toString() {
			return "Agg [orig=" + orig + ", zero=" + zero + ", plus=" + plus + ", from=" + from + ", where=" + where + ", att=" + att + "]";
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((att == null) ? 0 : att.hashCode());
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((orig == null) ? 0 : orig.hashCode());
			result = prime * result + ((plus == null) ? 0 : plus.hashCode());
			result = prime * result + ((where == null) ? 0 : where.hashCode());
			result = prime * result + ((zero == null) ? 0 : zero.hashCode());
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
			Agg<?,?,?,?,?,?> other = (Agg<?,?,?,?,?,?>) obj;
			if (att == null) {
				if (other.att != null)
					return false;
			} else if (!att.equals(other.att))
				return false;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (orig == null) {
				if (other.orig != null)
					return false;
			} else if (!orig.equals(other.orig))
				return false;
			if (plus == null) {
				if (other.plus != null)
					return false;
			} else if (!plus.equals(other.plus))
				return false;
			if (where == null) {
				if (other.where != null)
					return false;
			} else if (!where.equals(other.where))
				return false;
			if (zero == null) {
				if (other.zero != null)
					return false;
			} else if (!zero.equals(other.zero))
				return false;
			return true;
		}
		
		
		
	}
	
	public static class Block<S1, C1, V1, S2, C2, V2> {

		final String orig;
		
		final LinkedHashMap<V1, S1> from;
		final Set<Pair<OplTerm<C1, V1>, OplTerm<C1, V1>>> where;
		final Map<C2, Chc<Agg<S1, C1, V1, S2, C2, V2>, OplTerm<C1, V1>>> attrs;
		final Map<C2, Pair<Object, Map<V1, OplTerm<C1, V1>>>> edges;

		public Block(LinkedHashMap<V1, S1> from, Set<Pair<OplTerm<C1, V1>, OplTerm<C1, V1>>> where,
				Map<C2, Chc<Agg<S1, C1, V1, S2, C2, V2>, OplTerm<C1, V1>>> attrs,
				Map<C2, Pair<Object, Map<V1, OplTerm<C1, V1>>>> edges) {
			this.from = from;
			this.where = where;
			this.attrs = attrs;
			this.edges = edges;
			orig = toString();
		}

		@Override
		public String toString() {
			String for_str = printFor();
			String where_str = printWhere();
			String attr_str = printAttrs();
			String edges_str = printEdges();

			return "{for " + for_str + "; where " + where_str + "; return " + attr_str
					+ "; keys " + edges_str + ";}";
		}

		private String printEdges() {
			boolean first = false;
			String ret = "";
			for (Entry<C2, Pair<Object, Map<V1, OplTerm<C1, V1>>>> k : edges.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + " = {" + printSub(k.getValue().second) + "} : "
						+ k.getValue().first;
			}
			return ret;
		}

		private String printSub(Map<V1, OplTerm<C1, V1>> second) {
			boolean first = false;
			String ret = "";
			for (Entry<V1, OplTerm<C1, V1>> k : second.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + " = " + k.getValue();
			}
			return ret;

		}

		private String printAttrs() {
			boolean first = false;
			String ret = "";
			for (Entry<C2, Chc<Agg<S1, C1, V1, S2, C2, V2>, OplTerm<C1, V1>>> k : attrs.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + " = " + k.getValue().toStringMash(); 
			}
			return ret;

		}

		private String printWhere() {
			boolean first = false;
			String ret = "";
			for (Pair<OplTerm<C1, V1>, OplTerm<C1, V1>> k : where) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.first + " = " + k.second;
			}
			return ret;
		}

		private String printFor() {
			boolean first = false;
			String ret = "";
			for (Entry<V1, S1> k : from.entrySet()) {
				if (first) {
					ret += ", ";
				}
				first = true;
				ret += k.getKey() + ":" + k.getValue();
			}
			return ret;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((orig == null) ? 0 : orig.hashCode());
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
			Block<?,?,?,?,?,?> other = (Block<?,?,?,?,?,?>) obj;
			if (orig == null) {
				if (other.orig != null)
					return false;
			} else if (!orig.equals(other.orig))
				return false;
			return true;
		}
		
		
	}

	@Override
	public String toString() {
		return "query {" + printBlocks() + "\n} : " + src_e + " -> " + dst_e;
	}

	private String printBlocks() {
		boolean first = false;
		String ret = "";
		for (Entry<Object, Pair<S2, Block<S1, C1, V1, S2, C2, V2>>> k : blocks.entrySet()) {
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

		return ret;
	}

	@Override
	public <R, E> R accept(E env, OplExpVisitor<R, E> v) {
		return v.visit(env, this);
	}

	@SuppressWarnings("unchecked")
	public static <S, C, V> OplQuery<S, C, V, S, C, V> id(String str, OplSchema<S, C, V> S) {
		Map<Object, Pair<S, Block<S, C, V, S, C, V>>> bs = new HashMap<>();
		for (S x : S.projE().sorts) {
			LinkedHashMap<V, S> from = new LinkedHashMap<>();
			Map<C, Chc<Agg<S, C, V, S, C, V>, OplTerm<C, V>>> attrs = new HashMap<>();
			Map<C, Pair<Object, Map<V, OplTerm<C, V>>>> edges = new HashMap<>();
			from.put((V) "q_v", x);
			for (C term : S.projEA().symbols.keySet()) {
				Pair<List<S>, S> t0 = S.projEA().symbols.get(term);
				Pair<S, S> t = new Pair<>(t0.first.get(0), t0.second);
				if (!t.first.equals(x)) {
					continue;
				}
				OplTerm<C, V> l = new OplTerm<>(term,
						Collections.singletonList(new OplTerm<>((V) "q_v")));
				if (S.projE().symbols.containsKey(term)) {
					Map<V, OplTerm<C, V>> m = new HashMap<>();
					m.put((V) "q_v", l);
					edges.put(term, new Pair<>("q" + t.second, m));
				} else {
					attrs.put(term, Chc.inRight(l));
				}
			}
			Block<S, C, V, S, C, V> b = new Block<>(from, new HashSet<>(), attrs, edges);
			bs.put("q" + x, new Pair<>(x, b));
		}

		OplQuery<S, C, V, S, C, V> ret = new OplQuery<>(str, str, bs);
		ret.validate(S, S);
		return ret;
	}

	private <X> Set<Map<V1, OplTerm<Chc<C1, X>, V1>>> filter(
			Set<Map<V1, OplTerm<Chc<C1, X>, V1>>> tuples,
			Set<Pair<OplTerm<Chc<C1, X>, V1>, OplTerm<Chc<C1, X>, V1>>> where,
			OplInst<S1, C1, V1, X> I0) {
		Set<Map<V1, OplTerm<Chc<C1, X>, V1>>> ret = new HashSet<>();
		outer: for (Map<V1, OplTerm<Chc<C1, X>, V1>> tuple0 : tuples) {
			OplCtx<OplTerm<Chc<C1, X>, V1>, V1> tuple = new OplCtx<>(tuple0);
			for (Pair<OplTerm<Chc<C1, X>, V1>, OplTerm<Chc<C1, X>, V1>> eq : where) {
				if (eq.first.isGround(tuple) && eq.second.isGround(tuple)) {
					OplTerm<Chc<C1, X>, V1> l = eq.first.subst(tuple0);
					OplTerm<Chc<C1, X>, V1> r = eq.second.subst(tuple0);
					OplTerm<Chc<C1, X>, V1> l0 = I0.P.toSig().getKB().nf(l);
					OplTerm<Chc<C1, X>, V1> r0 = I0.P.toSig().getKB().nf(r);
					if (!l0.equals(r0)) {
						if (I0.J == null) {
							continue outer;
						}
						OplTerm<Chc<Chc<C1, X>, JSWrapper>, V1> l1 = l0.inLeft();
						OplTerm<Chc<Chc<C1, X>, JSWrapper>, V1> r1 = r0.inLeft();
						KBExp<Chc<Chc<C1, X>, JSWrapper>, V1> l2 = OplToKB.convert(l1);
						KBExp<Chc<Chc<C1, X>, JSWrapper>, V1> r2 = OplToKB.convert(r1);
						KBExp<Chc<Chc<C1, X>, JSWrapper>, V1> l3 = OplToKB.redBy(I0.J, l2);
						KBExp<Chc<Chc<C1, X>, JSWrapper>, V1> r3 = OplToKB.redBy(I0.J, r2);
						if (!l3.equals(r3)) {
							continue outer;
						}
					}
				}
			}
			ret.add(tuple0);
		}
		return ret;
	}

	private static <X, V> Set<Map<V, X>> extend(Set<Map<V, X>> tuples, Set<X> dom, V v) {
		Set<Map<V, X>> ret = new HashSet<>();

		for (Map<V, X> tuple : tuples) {
			for (X x : dom) {
				Map<V, X> m = new HashMap<>(tuple);
				m.put(v, x);
				ret.add(m);
			}
		}

		return ret;
	}


	private static <S1, C1, C2, V1, V2, X>
	OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2>
	              conv(OplInst<S1, C1, V1, X> i0, OplTerm<Chc<C1, OplTerm<Chc<C1, X>, V1>>, V1> e) {
		if (e.var != null) {
			throw new RuntimeException();
		}
		List<OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2>> l = new LinkedList<>();
		for (OplTerm<Chc<C1, OplTerm<Chc<C1, X>, V1>>, V1> arg : e.args) {
			l.add(conv(i0, arg));
		}
		
		if (!e.head.left) {
			if (!l.isEmpty()) {
				throw new RuntimeException();
			}
			return new OplTerm<>(Chc.inRight(Chc.inLeft(e.head.r)), new LinkedList<>());
		}
		C1 c1 = e.head.l;
		if (i0.S.projT().symbols.keySet().contains(c1)) {
			@SuppressWarnings("unchecked")
			C2 c2 = (C2) c1; //is type symbol
			return new OplTerm<>(Chc.inLeft(c2), l);
		}  //is attribute
			throw new RuntimeException("New Impossible");
		
	}

	private <X> int guessPrec(@SuppressWarnings("unused") OplInst<S1, C1, V1, X> I0, int last, Map<Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>, Integer> prec) {
		while (true) {
			last++;
			if (prec.containsValue(last)) {
				continue;
			}
			//boolean inInst = I0.P.prec.containsValue(last);
			boolean inSch = dst.sig.prec.containsValue(last);
			if (!inSch) {
				return last;
			}
		}
	}
	
	@SuppressWarnings({"unchecked"})
	public <X> Pair<OplInst<S2, C2, V2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>,
	Map<Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>> eval(
			OplInst<S1, C1, V1, X> I0) {
		if (!I0.S.equals(src)) {
			throw new RuntimeException("Instance not on correct schema");
		}

		Map<Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>> em = new HashMap<>();

		
		Map<Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>, S2> gens = new HashMap<>();
		List<Pair<OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2>, OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2>>> equations = new LinkedList<>();

		Quad<OplSetInst<S1, C1, OplTerm<Chc<C1, X>, V1>>, OplSetInst<S1, C1, OplTerm<Chc<Chc<C1, X>, JSWrapper>, V1>>, OplPres<S1, C1, V1, OplTerm<Chc<C1, X>, V1>>, OplSetInst<S1, C1, OplTerm<Chc<C1, X>, V1>>> yyy = I0.saturate();
		
		OplSetInst<S1, C1, OplTerm<Chc<C1, X>, V1>> I = yyy.fourth; //OplSat.saturate(I0.projEA());
		Map<Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>, Integer> prec = new HashMap<>();

		int guess = -1;
		int newIdsStart = 20000;
		
		if (DefunctGlobalOptions.debug.opl.opl_prover_force_prec) {
			for (OplTerm<Chc<C1, X>, V1> gen : yyy.third.gens.keySet()) {
				S2 s2 = (S2) yyy.third.gens.get(gen);
				gens.put(Chc.inLeft(gen), s2);
				prec.put(Chc.inLeft(gen), newIdsStart);
				newIdsStart++;
			}
		} else { 
			for (OplTerm<Chc<C1, X>, V1> gen : yyy.third.gens.keySet()) {
				S2 s2 = (S2) yyy.third.gens.get(gen);
				gens.put(Chc.inLeft(gen), s2);
				guess = guessPrec(I0, guess, prec); //increment guess
				prec.put(Chc.inLeft(gen), yyy.third.prec.get(gen));
			}
		}
		newIdsStart = 0;
		
		for (Pair<OplTerm<Chc<C1, OplTerm<Chc<C1, X>, V1>>, V1>, OplTerm<Chc<C1, OplTerm<Chc<C1, X>, V1>>, V1>> eq : yyy.third.equations) {
			equations.add(new Pair<>(conv(I0, eq.first), conv(I0, eq.second)));
		}
		OplPres<S2, C2, V2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>> pre_P1 = new OplPres<>(
				prec, dst.sig0, dst.sig, gens, equations);
		pre_P1.toSig();
		
		for (Object label : blocks.keySet()) {
			Pair<S2, Block<S1, C1, V1, S2, C2, V2>> xxx = blocks.get(label);
			S2 tgt = xxx.first;
			Block<S1, C1, V1, S2, C2, V2> block = xxx.second;

			Set<Map<V1, OplTerm<Chc<C1, X>, V1>>> tuples = new HashSet<>();
			tuples.add(new HashMap<>());

			Set<Pair<OplTerm<Chc<C1, X>, V1>, OplTerm<Chc<C1, X>, V1>>> where = new HashSet<>();
			for (Pair<OplTerm<C1, V1>, OplTerm<C1, V1>> eq : block.where) {
				where.add(new Pair<>(eq.first.inLeft(), eq.second.inLeft()));
			}

			List<V1> ordered = order(block);
			for (V1 v : ordered) {
				S1 s = block.from.get(v);
				Set<OplTerm<Chc<C1, X>, V1>> dom = I.sorts.get(s);
				tuples = extend(tuples, dom, v);
				tuples = filter(tuples, where, I0);
			}
			if (block.from.keySet().isEmpty()) {
				tuples = filter(tuples, where, I0);
			}

			for (Map<V1, OplTerm<Chc<C1, X>, V1>> tuple : tuples) {
				gens.put(Chc.inRight(new Pair<>(label, tuple)), tgt);
				if (DefunctGlobalOptions.debug.opl.opl_prover_force_prec) {
					newIdsStart++;
					prec.put(Chc.inRight(new Pair<>(label, tuple)), newIdsStart);
				} else {
					guess = guessPrec(I0, guess, prec); 
					prec.put(Chc.inRight(new Pair<>(label, tuple)), guess);
				}
			}

			for (C2 c2 : block.attrs.keySet()) {
				Chc<Agg<S1, C1, V1, S2, C2, V2>, OplTerm<C1, V1>> ee = block.attrs.get(c2); //.r.inLeft(); 
				
				for (Map<V1, OplTerm<Chc<C1, X>, V1>> tuple : tuples) {
					OplTerm<Chc<C1, X>, V1> a;

					if (ee.left) {
						Agg<S1, C1, V1, S2, C2, V2> agg = ee.l;
						Set<Pair<OplTerm<Chc<C1, X>, V1>, OplTerm<Chc<C1, X>, V1>>> whereX = new HashSet<>();
						for (Pair<OplTerm<C1, V1>, OplTerm<C1, V1>> eq : agg.where) {
							whereX.add(new Pair<>(eq.first.inLeft(), eq.second.inLeft()));
						}
						Set<Map<V1, OplTerm<Chc<C1, X>, V1>>> tuplesX = new HashSet<>();
						tuplesX.add(tuple);
						for (V1 v : agg.from.keySet()) {
							S1 s = agg.from.get(v);
							Set<OplTerm<Chc<C1, X>, V1>> dom = I.sorts.get(s);
							tuplesX = extend(tuplesX, dom, v);
							tuplesX = filter(tuplesX, whereX, I0);
						}
						if (agg.from.keySet().isEmpty()) {
							tuplesX = filter(tuplesX, whereX, I0);
						}
						a = new OplTerm<>(Chc.inLeft(agg.zero), new LinkedList<>());
						for (Map<V1, OplTerm<Chc<C1, X>, V1>> tupleX : tuplesX) {
							OplTerm<Chc<C1, X>, V1> e = agg.att.inLeft();
							a = new OplTerm<>(Chc.inLeft(agg.plus), Util.list(a, e.subst(tupleX)));
						}
						//a = term.subst(tuple);
					} else {
						OplTerm<Chc<C1, X>, V1> e = ee.r.inLeft();
						a = e.subst(tuple);
					}

					OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2> lhs = new OplTerm<>(
							Chc.inLeft(c2), Collections.singletonList(new OplTerm<>(Chc.inRight(Chc
									.inRight(new Pair<>(label, tuple))), new LinkedList<>())));

					OplTerm<Chc<C1,OplTerm<Chc<C1,X>,V1>>,V1> term1 = OplInst.conv(I0.S, a, I0.P);
					OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2> term2 = conv(I0, term1);
					equations.add(new Pair<>(lhs, term2));
				}
			} 

			
			for (C2 c2 : block.edges.keySet()) {
				Object tgt_label = block.edges.get(c2).first;
				Map<V1, OplTerm<C1, V1>> tgt_ctx = block.edges.get(c2).second;
				for (Map<V1, OplTerm<Chc<C1, X>, V1>> tuple : tuples) {
					Map<V1, OplTerm<Chc<C1, X>, V1>> substed = new HashMap<>();
					for (V1 v1 : tgt_ctx.keySet()) {
						OplTerm<Chc<C1, X>, V1> uuu = tgt_ctx.get(v1).inLeft();
						OplTerm<Chc<C1, X>, V1> vvv = uuu.subst(tuple);
						substed.put(v1, I0.P.toSig().getKB().nf(vvv)); //seems to be necessary
					}

					OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2> lhs = new OplTerm<>(
							Chc.inLeft(c2), Collections.singletonList(new OplTerm<>(Chc.inRight(Chc
									.inRight(new Pair<>(label, tuple))), new LinkedList<>())));

					OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>>, V2> rhs = 
							new OplTerm<>(Chc.inRight(Chc
									.inRight(new Pair<>(tgt_label, substed))), new LinkedList<>());
					
					equations.add(new Pair<>(lhs, rhs));
					em.put(new Pair<>(label, tuple), new Pair<>(tgt_label, substed));
				}
			}
			
		}
	
		OplPres<S2, C2, V2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>> P = new OplPres<>(
				prec, dst_e, dst.sig, gens, equations);
		
		if (DefunctGlobalOptions.debug.opl.opl_prover_simplify_instances) {
			P = P.simplify();
		}
		
		P.toSig();

		OplInst<S2, C2, V2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>> retX = new OplInst<>(
				dst_e, "?", I0.J0);

		retX.validate(dst, P, I0.J);
		
		return new Pair<>(retX, em);
	}
	
	public <X, Y> OplPresTrans<S2, C2, V2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>, Chc<OplTerm<Chc<C1, Y>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, Y>, V1>>>>> eval(
			OplPresTrans<S1, C1, V1, X, Y> h) {
		OplInst<S2,C2,V2,Chc<OplTerm<Chc<C1,X>,V1>,Pair<Object,Map<V1,OplTerm<Chc<C1,X>,V1>>>>> QI = eval(h.src1).first;
		OplInst<S2,C2,V2,Chc<OplTerm<Chc<C1,Y>,V1>,Pair<Object,Map<V1,OplTerm<Chc<C1,Y>,V1>>>>> QJ = eval(h.dst1).first;

		Map<S2, Map<Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>, OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, Y>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, Y>, V1>>>>>, V2>>> m = new HashMap<>();

		for (S2 s2 : dst.sig.sorts) {
			m.put(s2, new HashMap<>());
		}
		for (Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>> gen : QI.P.gens.keySet()) {
			S2 s2 = QI.P.gens.get(gen);
			OplTerm<Chc<C2, Chc<OplTerm<Chc<C1, Y>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, Y>, V1>>>>>, V2> value;
			
			if (gen.left) { //skolem
				OplTerm<Chc<C1, Y>, V1> y = h.apply(gen.l); 
				OplTerm<Chc<C1,OplTerm<Chc<C1,Y>,V1>>,V1> x = OplInst.conv(h.dst1.S, y, h.dst);
				value = conv(h.dst1, x);
			} else {
				Map<V1, OplTerm<Chc<C1, Y>, V1>> map = new HashMap<>();
				for (V1 v1 : gen.r.second.keySet()) {
					map.put(v1, h.dst1.P.toSig().getKB().nf(h.apply(gen.r.second.get(v1))));
				}
				value = new OplTerm<>(Chc.inRight(Chc.inRight(new Pair<>(gen.r.first, map))), new LinkedList<>()); 
			}
			m.get(s2).put(gen, value);
		}
		OplPresTrans<S2, C2, V2, Chc<OplTerm<Chc<C1, X>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, X>, V1>>>>, Chc<OplTerm<Chc<C1, Y>, V1>, Pair<Object, Map<V1, OplTerm<Chc<C1, Y>, V1>>>>> 
		  ret = new OplPresTrans<>(m, "?", "?", QI.P, QJ.P);
		
		ret.src1 = QI;
		ret.dst1 = QJ;
		
		return ret;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
		result = prime * result + ((dst_e == null) ? 0 : dst_e.hashCode());
		result = prime * result + ((src_e == null) ? 0 : src_e.hashCode());
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
		OplQuery<?,?,?,?,?,?> other = (OplQuery<?,?,?,?,?,?>) obj;
		if (blocks == null) {
			if (other.blocks != null)
				return false;
		} else if (!blocks.equals(other.blocks))
			return false;
		if (dst_e == null) {
			if (other.dst_e != null)
				return false;
		} else if (!dst_e.equals(other.dst_e))
			return false;
		if (src_e == null) {
			if (other.src_e != null)
				return false;
		} else if (!src_e.equals(other.src_e))
			return false;
		return true;
	}
	
	

	// knuth bendix precedence should favor rewriting into type side rather
	// than entity side

	// using a separate type for generators was sound. However, in a typed
	// setting, there should be two kinds of
	// generators, so that the types for generators at type can be preserved
	// across queries, and let the types at
	// entities change (.e.g, to hashmaps). As it stands now, the type for
	// attribute generators must change along with entities.

}

package catdata.fqlpp.cat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Util;
import catdata.ide.DefunctGlobalOptions;

@SuppressWarnings("serial")
class LeftKan<O1, A1, O2, A2> implements Serializable {

	private final Signature<O1, A1> A;
	private final Signature<O2, A2> B;
	public final Mapping<O1, A1, O2, A2> F;
	private final Instance<O1, A1> X;
	private int fresh;

	private boolean gamma() {
		boolean ret = false;

        while (true) {
            Pair<Signature<O2, A2>.Node, Pair<Integer, Integer>> k = gamma0();
            if (k == null) {
                return ret;
            }
            ret = true;
            gamma1(k.first, k.second);
        }
	}

	private static void filter(Set<Pair<Integer, Integer>> set, Integer d) {
        set.removeIf(p -> p.first.equals(d) || p.second.equals(d));
	}

	private void gamma1(Signature<O2, A2>.Node b1, Pair<Integer, Integer> xy) {
		if (xy.first.equals(xy.second)) {
			Sb.get(b1).remove(xy);
			return;
		}
		Integer x, y;
		if (xy.first > xy.second) {
			x = xy.second;
			y = xy.first;
		} else {
			x = xy.first;
			y = xy.second;
		}

		Pb.get(b1).remove(new Pair<>(y, y));

		replace(x, y);

		Set<Pair<Integer, Integer>> set0 = new HashSet<>(Sb.get(b1));
		for (Pair<Integer, Integer> k : Sb.get(b1)) {
			if (k.first.equals(y)) {
				set0.add(new Pair<>(x, k.second));
			}
			if (k.second.equals(y)) {
				set0.add(new Pair<>(k.first, x));
			}
		}
		filter(set0, y);
		Sb.put(b1, set0);

		for (Signature<O2, A2>.Edge g : Pg.keySet()) {
			Set<Pair<Integer, Integer>> set = Pg.get(g);
			Set<Pair<Integer, Integer>> a = new HashSet<>();
			if (g.source.equals(b1) && g.target.equals(b1)) {
				for (Pair<Integer, Integer> k : set) {
					if (k.first.equals(y) && k.second.equals(y)) {
						a.add(new Pair<>(x, x));
					}
					if (k.first.equals(y) && !k.second.equals(y)) {
						a.add(new Pair<>(x, k.second));
					}
					if (k.second.equals(y) && !k.first.equals(y)) {
						a.add(new Pair<>(k.first, x));
					}
				}
			} else if (g.source.equals(b1)) {
				for (Pair<Integer, Integer> k : set) {
					if (k.first.equals(y) && !k.second.equals(y)) {
						a.add(new Pair<>(x, k.second));
					}
				}
			} else if (g.target.equals(b1)) {
				for (Pair<Integer, Integer> k : set) {
					if (k.second.equals(y) && !k.first.equals(y)) {
						a.add(new Pair<>(k.first, x));
					}
				}
			}
			set.addAll(a);
			filter(set, y);
		}

		lineage.remove(y); 
	}

	private void replace(Integer x, Integer y) {
		for (Set<Pair<Object, Integer>> a : ua.values()) {
			for (Pair<Object, Integer> s : a) {
				if (s.second.equals(y)) {
					s.setSecond(x);
				}
			}
		}
		if (alpha != null) {
			for (Signature<O2, A2>.Node k : utables.keySet()) {
				Map<Integer, Object> v = utables.get(k);
				v.remove(y);
			}
		}

	}

	private Pair<Signature<O2, A2>.Node, Pair<Integer, Integer>> gamma0() {
		for (Entry<Signature<O2, A2>.Node, Set<Pair<Integer, Integer>>> c : Sb.entrySet()) {
			if (c.getValue().isEmpty()) {
				continue;
			}
			for (Pair<Integer, Integer> p0 : c.getValue()) {
				return new Pair<>(c.getKey(), p0);
			}
		}

		return null;
	}

	private boolean bgd() {
		return beta1() || beta2() || delta() || gamma();
	}

	private boolean step() {
		boolean ret = false;
		while (bgd()) {
			ret = true;
		}
		return ret || alpha();
	}

	// true = success
	public boolean compute() {
		for (int i = 0; i < DefunctGlobalOptions.debug.fqlpp.MAX_DENOTE_ITERATIONS; i++) {
			if (!step()) {
				substLineage();
				return true;
			}
		}
		return false;
	}

	// beta, delta, gamma

	private boolean beta2() {
		boolean ret = false;

		for (Signature<O1, A1>.Edge e : A.edges) {
			Signature<O2, A2>.Path g = F.apply(A.path(e));
			if (X.em.get(e) == null) {
				throw new RuntimeException("can't find " + e + " in " + X.nm);
			}
			Set<Pair<Object, Integer>> lhs = Util.compose(Util.convert(X.em.get(e)),
					ua.get(e.target));
			Set<Pair<Object, Integer>> rhs = Util.compose(ua.get(e.source), eval(g));
			Signature<O2, A2>.Node n = g.target;
			ret = ret || addCoincidences(lhs, rhs, n);
		}

		return ret;
	}

	private boolean beta1() {
		boolean ret = false;
		for (Signature<O2, A2>.Eq eq : B.eqs) {
			Set<Pair<Integer, Integer>> lhs = eval(eq.lhs);
			Set<Pair<Integer, Integer>> rhs = eval(eq.rhs);
			Signature<O2, A2>.Node n = eq.lhs.target;
			ret = ret || addCoincidences(lhs, rhs, n);
		}
		return ret;
	}

	private <X> boolean addCoincidences(Set<Pair<X, Integer>> lhs, Set<Pair<X, Integer>> rhs,
			Signature<O2, A2>.Node n) {
		boolean ret = false;
		for (Pair<?, Integer> l : lhs) {
			for (Pair<?, Integer> r : rhs) {
				if (!l.first.equals(r.first)) {
					continue;
				}
				if (l.second.equals(r.second)) {
					continue;
				}
				ret = Sb.get(n).add(new Pair<>(l.second, r.second)) || ret;
				ret = Sb.get(n).add(new Pair<>(r.second, l.second)) || ret;
			}
		}
		return ret;
	}

	private Set<Pair<Integer, Integer>> eval(Signature<O2, A2>.Path p) {
		Set<Pair<Integer, Integer>> ret = Pb.get(p.source);
		for (Signature<O2, A2>.Edge e : p.path) {
			ret = Util.compose(ret, Pg.get(e));
		}
		return ret;
	}

	public Set<Pair<Object, Object>> eval2(Signature<O2, A2>.Path p) {
		Set<Pair<Object, Object>> ret = Pb2.get(p.source);
		for (Signature<O2, A2>.Edge e : p.path) {
			ret = Util.compose(ret, Pg2.get(e));
		}
		return ret;
	}

	private Integer fresh() {
		return ++fresh;
	}

	private Pair<Integer, Signature<O2, A2>.Edge> smallest() {
		Pair<Integer, Signature<O2, A2>.Edge> ret = null;
		for (Signature<O2, A2>.Edge g : Pg.keySet()) {
			Set<Pair<Integer, Integer>> pg = Pg.get(g);
			outer: for (Pair<Integer, Integer> xx : Pb.get(g.source)) {
				Integer x = xx.first;
				for (Pair<Integer, Integer> p : pg) {
					if (p.first.equals(x)) {
						continue outer;
					}
				}
				if (ret == null || x < ret.first) {
					ret = new Pair<>(x, g);
				}
			}
		}
		return ret;
	}

	private boolean alpha() {
		Pair<Integer, Signature<O2, A2>.Edge> p = smallest();
		if (p == null) {
			return false;
		}
		Integer x = p.first;

		Signature<O2, A2>.Edge g = p.second;
		Signature<O2, A2>.Node b2 = g.target;
		Integer y = fresh();

		Pb.get(b2).add(new Pair<>(y, y));
		Pg.get(g).add(new Pair<>(x, y));

		updateLineage(g, x, y);

		if (alpha != null) {
			Object xxx = J.em.get(p.second).get(utables.get(p.second.source).get(p.first));
			utables.get(p.second.target).put(y, xxx);
		}

		return true;
	}

	private boolean delta() {
		boolean ret = false;
		for (Signature<O2, A2>.Edge g : B.edges) {
			for (Pair<Integer, Integer> x : Pb.get(g.source)) {
				Integer y = null;
				Iterator<Pair<Integer, Integer>> it = Pg.get(g).iterator();
				while (it.hasNext()) {
					Pair<Integer, Integer> z = it.next();
					if (!x.first.equals(z.first)) {
						continue;
					}
					if (y == null) {
						y = z.second;
						continue;
					}
					// if (z.second.equals(y)) {
					ret = true;
					it.remove();
					Sb.get(g.target).add(new Pair<>(y, z.second));
					Sb.get(g.target).add(new Pair<>(z.second, y));
					// }
				}
			}
		}
		return ret;
	}

	private void updateLineage(Signature<O2, A2>.Edge col, Object old, Object nw) {
		if (!lineage.containsKey(old)) {
			lineage.put(old, new LinkedList<>());
		}
		List<Pair<Signature<O2, A2>.Edge, Object>> l = new LinkedList<>(lineage.get(old));
	 //else {
			l.add(new Pair<>(col, old)); //
		//}
		lineage.put(nw, l);
	}

	private final Instance<O2, A2> J;
	private final FPTransform<O1, A1> alpha;

	//private int initFresh;
	public LeftKan(int fresh, Mapping<O1, A1, O2, A2> f, Instance<O1, A1> x) {
		this(fresh, f, x, null, null);
	}

	public LeftKan(int fresh, Mapping<O1, A1, O2, A2> f, Instance<O1, A1> x,
			FPTransform<O1, A1> alpha, Instance<O2, A2> J) {
		A = f.source;
		B = f.target;
		F = f;
		X = x;
		this.fresh = fresh;
		//this.initFresh = fresh;
		this.J = J;
		this.alpha = alpha;

		for (Signature<O2, A2>.Node n : B.nodes) {
			Pb.put(n, new HashSet<>());
			Sb.put(n, new HashSet<>());
			if (alpha != null) {
				utables.put(n, new HashMap<>());
			}
		}
		for (Signature<O2, A2>.Edge e : B.edges) {
			Pg.put(e, new HashSet<>());
		}
		
		for (Signature<O1, A1>.Node n : A.nodes) {
			Set<Pair<Object, Integer>> j = new HashSet<>();
			Set<Pair<Integer, Integer>> i = Pb.get(F.nm.get(n));
			Set<Object> k = X.nm.get(n);
			for (Object v : k) {
				//  be careful: in fql++ input IDs are not unique.
				
				int id = fresh();
				// rank.add(v);
				j.add(new Pair<>(v, id));
				i.add(new Pair<>(id, id));
				updateLineage(null, new Pair<>(n, v), id); // v is
																		// not
																		// globally
																		// unique
																		// here.

				if (alpha != null) {
					utables.get(F.nm.get(n)).put(id, alpha.data.get(n).get(v));
				}
			}
			ua.put(n, j);
		}
	}

	@Override
	public String toString() {
		return "LeftKan [Pb=" + Pb + ", Pg=" + Pg + ", Sb=" + Sb + "]";
	}

	@SuppressWarnings("unchecked")
	private void substLineage() {
		//could make getLineage aware of Fresh, but this avoids copying
		if (DefunctGlobalOptions.debug.fqlpp.useLineage.equals("Fresh IDs")) {
			Pb2 = (Map<Signature<O2, A2>.Node, Set<Pair<Object, Object>>>) ((Object) Pb);
			Pg2 = (Map<Signature<O2, A2>.Edge, Set<Pair<Object, Object>>>) ((Object) Pg);
			ua2 = (Map<Signature<O1, A1>.Node, Set<Pair<Object, Object>>>) ((Object) ua);
			utables2 = (Map<Signature<O2, A2>.Node, Map<Object, Object>>) ((Object) utables);
			lineage2 = (lineage);
		} else {
			for (Entry<Signature<O2, A2>.Node, Set<Pair<Integer, Integer>>> k : Pb.entrySet()) {
				Pb2.put(k.getKey(),
						k.getValue()
								.stream()
								.map(x -> new Pair<>(getLineage(x.first), getLineage
										(x.second))).collect(Collectors.toSet()));
			}
			for (Entry<Signature<O2, A2>.Edge, Set<Pair<Integer, Integer>>> k : Pg.entrySet()) {
				Pg2.put(k.getKey(),
						k.getValue()
								.stream()
								.map(x -> new Pair<>(getLineage(x.first), getLineage
										(x.second))).collect(Collectors.toSet()));
			}
			for (Entry<Signature<O1, A1>.Node, Set<Pair<Object, Integer>>> k : ua.entrySet()) {
				ua2.put(k.getKey(),
						k.getValue().stream()
								.map(x -> new Pair<>(x.first, getLineage(x.second)))
								.collect(Collectors.toSet()));
			}
			for (Entry<Signature<O2, A2>.Node, Map<Integer, Object>> k : utables.entrySet()) {
				Map<Object, Object> ret = new HashMap<>();
				for (Integer i : k.getValue().keySet()) {
					ret.put(getLineage(i), k.getValue().get(i));
				}
				utables2.put(k.getKey(), ret);
			}
			for (Entry<Object, List<Pair<Signature<O2, A2>.Edge, Object>>> k : lineage.entrySet()) {
				lineage2.put(getLineage(k.getKey()), k.getValue());
			}
		} 
	}

	private Object getLineage(Object i) {
		List<Pair<Signature<O2, A2>.Edge, Object>> l = lineage.get(i);
		
		if (DefunctGlobalOptions.debug.fqlpp.useLineage.equals("Lineage as ID")) {
			return l;
		}

		Iterator<Pair<Signature<O2, A2>.Edge, Object>> it = l.iterator();
		
		if (!it.hasNext()) {
			return "";
		}
		Pair<Signature<O2, A2>.Edge, Object> first = it.next();
		
		@SuppressWarnings("unchecked")
		Pair<Signature<O2, A2>.Node, Object> firstX = (Pair<Signature<O2, A2>.Node, Object>) first.second;
		
		String ret = "[" + firstX.first + ":" + firstX.second ;
		
		while (it.hasNext()) {
			first = it.next();
			ret += ", " + first.first.name;
		}
		
		
		ret += "]";
		return ret;
	}

	private final Map<Signature<O2, A2>.Node, Set<Pair<Integer, Integer>>> Pb = new HashMap<>();
	private final Map<Signature<O2, A2>.Edge, Set<Pair<Integer, Integer>>> Pg = new HashMap<>();
	private final Map<Signature<O1, A1>.Node, Set<Pair<Object, Integer>>> ua = new HashMap<>();
	private final Map<Signature<O2, A2>.Node, Map<Integer, Object>> utables = new HashMap<>();
	private final Map<Object, List<Pair<Signature<O2, A2>.Edge, Object>>> lineage = new HashMap<>();

	public Map<Signature<O2, A2>.Node, Set<Pair<Object, Object>>> Pb2 = new HashMap<>();
	public Map<Signature<O2, A2>.Edge, Set<Pair<Object, Object>>> Pg2 = new HashMap<>();
	public Map<Signature<O1, A1>.Node, Set<Pair<Object, Object>>> ua2 = new HashMap<>();
	public Map<Signature<O2, A2>.Node, Map<Object, Object>> utables2 = new HashMap<>();
	public Map<Object, List<Pair<Signature<O2, A2>.Edge, Object>>> lineage2 = new HashMap<>();

	private final Map<Signature<O2, A2>.Node, Set<Pair<Integer, Integer>>> Sb = new HashMap<>();

}

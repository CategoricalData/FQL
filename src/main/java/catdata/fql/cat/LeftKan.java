package catdata.fql.cat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.Pair;
import catdata.fql.FQLException;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Eq;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;
import catdata.ide.DefunctGlobalOptions;

class LeftKan {

	
	private final Signature A;
	private final Signature B;
	public final Mapping F;
	private final Instance X;
	//Map<Object, Integer> rank = new HashMap<>(); //assumes all IDs globally unique
	public int fresh; 	
	
	private boolean gamma() {
		boolean ret = false;

        while (true) {
            Pair<Node, Pair<Integer, Integer>> k = gamma0();
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
	
	private void gamma1(Node b1, Pair<Integer, Integer> xy) {
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

		Pb.get(b1).remove(new Pair<>(y,y));
		
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
		
		for (Edge g : Pg.keySet()) {
			Set<Pair<Integer, Integer>> set = Pg.get(g);
			Set<Pair<Integer, Integer>> a = new HashSet<>();
			if (g.source.equals(b1) && g.target.equals(b1)) {
				for (Pair<Integer, Integer> k : set) {
					if (k.first.equals(y) && k.second.equals(y)) {
						a.add(new Pair<>(x,x));
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
	
		//: not needed?
		lineage.remove(y);
	}

	private void replace(Integer x, Integer y) {
		for (Set<Pair<Object, Integer>> a : ua.values()) {
			Iterator<Pair<Object, Integer>> it = a.iterator();
			List<Pair<Object, Integer>> l = new LinkedList<>();
			while (it.hasNext()) {
				Pair<Object, Integer> s = it.next();
				if (s.second.equals(y)) {
					it.remove();
					l.add(new Pair<>(s.first, x));
				} 
			}
			a.addAll(l);
		}
		if (alpha != null) {
			for (Node k : utables.keySet()) {
				Map<Integer, Object> v = utables.get(k);
				v.remove(y);
			}
		}

	}
	
	private Pair<Node, Pair<Integer, Integer>> gamma0() {
		for (Entry<Node, Set<Pair<Integer, Integer>>> c : Sb.entrySet()) {
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
	
	//true = success
	public boolean compute() {
		for (int i = 0; i < DefunctGlobalOptions.debug.fql.MAX_DENOTE_ITERATIONS; i++) {
			//System.out.println(i + "FQL: " + toString());

			if (!step()) {
				return true;
			}
		}
		return false;
	}
	
	//beta, delta, gamma
	
	private boolean beta2() {
		boolean ret = false;
		try {
			for (Edge e : A.edges) {
				Path g = F.appy(B, new Path(A, e));
				Set<Pair<Object, Integer>> lhs = Instance.compose(X.data.get(e.name), ua.get(e.target));
				Set<Pair<Object, Integer>> rhs = Instance.compose(ua.get(e.source), eval(g));
			//	System.out.println(lhs);
			//	System.out.println(rhs);
			
				Node n = g.target;
				ret = ret || addCoincidences(lhs, rhs, n);
			}
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
		
		return ret;
	}
	
	private boolean beta1() {
		boolean ret = false;
		for (Eq eq : B.eqs) {
			Set<Pair<Integer, Integer>> lhs = eval(eq.lhs);
			Set<Pair<Integer, Integer>> rhs = eval(eq.rhs);
			Node n = eq.lhs.target;
			ret = ret || addCoincidences(lhs, rhs, n);
		}		
		return ret;
	}
	
	private <X> boolean addCoincidences(
			Set<Pair<X, Integer>> lhs, Set<Pair<X, Integer>> rhs,
			Node n) {
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
	
	
	public Set<Pair<Integer, Integer>> eval(Path p) {
		Set<Pair<Integer, Integer>> ret = Pb.get(p.source);
		for (Edge e : p.path) {
			ret = Instance.compose3(ret, Pg.get(e));
		}
		return ret;
	}
	
	private Integer fresh() {
		return ++fresh;
	}
	
	private Pair<Integer, Edge> smallest() {
		Pair<Integer, Edge> ret = null;
		for (Edge g : Pg.keySet()) {
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
		Pair<Integer, Edge> p = smallest();
		if (p == null) {
			return false;
		}
		Integer x = p.first;
		
		Edge g = p.second;
		Node b2 = g.target;
		Integer y = fresh();
		
		Pb.get(b2).add(new Pair<>(y,y));
		Pg.get(g ).add(new Pair<>(x,y));
		
		updateLineage(g.name, x, y);
		
		if (alpha != null) {
			Object xxx = lookup(J.data.get(p.second.name),
				utables.get(p.second.source).get(p.first));
			utables.get(p.second.target).put(y, xxx);
		}		
		return true;
	}
	
	private boolean delta() {
		boolean ret = false;
		for (Edge g : B.edges) {
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
//					if (z.second.equals(y)) {
						ret = true;
						it.remove();
						Sb.get(g.target).add(new Pair<>(y, z.second));
						Sb.get(g.target).add(new Pair<>(z.second, y));
	//				}
				}
			}
		}
		return ret;
	}
	
	
	public final Map<Object, List<Pair<String, Object>>> lineage = new HashMap<>();
	
	private void updateLineage(String col, Object old, Object nw) {
		if (!lineage.containsKey(old)) {
			lineage.put(old, new LinkedList<>());
		}
		List<Pair<String, Object>> l = new LinkedList<>(lineage.get(old));
		l.add(new Pair<>(col, old));
		lineage.put(nw, l);
	}

	private final Instance J;
	private final Transform alpha;
	
	public LeftKan(int fresh, Mapping f, Instance x) {
		this(fresh, f, x, null, null);
	}
	public LeftKan(int fresh, Mapping f, Instance x, Transform alpha,
			Instance J) {
		A = f.source;
		B = f.target;
		F = f;
		X = x;
		this.fresh = fresh;
		this.J = J;
		this.alpha = alpha;
		
		for (Node n : B.nodes) {
			Pb.put(n, new HashSet<>());
			Sb.put(n, new HashSet<>());
			if (alpha != null) {
				utables.put(n, new HashMap<>());
			}
		}
		for (Edge e : B.edges) {
			Pg.put(e, new HashSet<>());
		}
		Set<Object> rank = new HashSet<>();

		for (Node n : A.nodes) {
			Set<Pair<Object, Integer>> j = new HashSet<>();
			Set<Pair<Integer, Integer>> i = Pb.get(F.nm.get(n));
			Set<Pair<Object, Object>> k = X.data.get(n.string);
			for (Pair<Object, Object> v : k) {
				if (rank.contains(v.first)) {
					throw new RuntimeException("Contains non-unique ID " + v.first + ": " + x);
				}
				int id = fresh();
				rank.add(v.first);
				j.add(new Pair<>(v.first, id));
				i.add(new Pair<>(id, id));
				updateLineage(n.string, v.first, id);

				if (alpha != null) {
					utables.get(F.nm.get(n)).put(id,
						lookup(alpha.data.get(n.string), v.first));
				}
			}
			ua.put(n, j);
		}
		
		//System.out.println("init: " + toString());
	}
	
	private static Object lookup(Set<Pair<Object, Object>> set, Object i) {
		if (i == null) {
			throw new RuntimeException();
		}
		if (set == null) {
			throw new RuntimeException();
		}
		for (Pair<Object, Object> k : set) {
			if (k.first.equals(i.toString())) {
				return k.second;
			}
		}
		throw new RuntimeException("Cannot find " + i + " in " + set);
	}
	
	@Override
	public String toString() {
		return "LeftKan [Pb=" + Pb + ", Pg=" + Pg
				+ ", ua=" + ua + ", Sb=" + Sb + "]";
	}

	public final Map<Node, Set<Pair<Integer, Integer>>> Pb = new HashMap<>();
	public final Map<Edge, Set<Pair<Integer, Integer>>> Pg = new HashMap<>();
	public final Map<Node, Set<Pair<Object , Integer>>> ua = new HashMap<>();
	public final Map<Node, Map<Integer, Object>> utables = new HashMap<>();

	private final Map<Node, Set<Pair<Integer, Integer>>> Sb = new HashMap<>();
}

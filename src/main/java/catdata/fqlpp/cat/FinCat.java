package catdata.fqlpp.cat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.fqlpp.FUNCTION;

@SuppressWarnings("serial")
public class FinCat extends Category<Category<?, ?>, Functor<?, ?, ?, ?>> {

	private FinCat() { }
	
	@Override
	public boolean isInfinite() {
		return true;
	}

	@Override
	public String toString() {
		return "Cat";
	}

	@Override
	public boolean isObject(Category<?,?> o) {
		return !o.isInfinite();
	}

	@Override
	public boolean isArrow(Functor<?,?,?,?> a) {
			Functor<?,?,?,?> f = a;
			return isObject(f.source) && isObject(f.target);
	}

	public static final FinCat FinCat = new FinCat();


	@Override
	public Set<Category<?, ?>> objects() {
		throw new RuntimeException("Cannot enumerate objects of Cat.");
	}

	@Override
	public Set<Functor<?, ?, ?, ?>> arrows() {
		throw new RuntimeException("Cannot enumerate arrows of Cat.");
	}

	@Override
	public Category<?,?> source(Functor<?,?,?,?> a) {
		return a.source;
	}

	@Override
	public Category<?,?> target(Functor<?,?,?,?> a) {
		return a.target;
	}

	@Override
	public Functor<?,?,?,?> identity(Category<?,?> o) {
		return Functor.identity(o);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Functor<?,?,?,?> compose(Functor a1, Functor a2) {
		return Functor.compose(a1, a2);
	}

	// /////

	public static Category<Unit, Unit> terminal() {
		return new FiniteCategory<>(Unit.unit, Unit.unit);
	}

	public static <X, Y> Functor<X, Y, Unit, Unit> terminal(Category<X, Y> o) {
		return new Functor<>(o, terminal(), x -> Unit.unit, x -> Unit.unit);
	}

	public static <O1, A1, O2, A2> Category<Pair<O1, O2>, Pair<A1, A2>> product(
			Category<O1, A1> o1, Category<O2, A2> o2) {
		Set<Pair<O1, O2>> os = new HashSet<>();
		Set<Pair<A1, A2>> as = new HashSet<>();
		Map<Pair<A1, A2>, Pair<O1, O2>> srcs = new HashMap<>();
		Map<Pair<A1, A2>, Pair<O1, O2>> dsts = new HashMap<>();
		Map<Pair<Pair<A1, A2>, Pair<A1, A2>>, Pair<A1, A2>> comp = new HashMap<>();
		Map<Pair<O1, O2>, Pair<A1, A2>> ids = new HashMap<>();
		for (O1 k : o1.objects()) {
			for (O2 v : o2.objects()) {
				os.add(new Pair<>(k, v));
				ids.put(new Pair<>(k, v),
						new Pair<>(o1.identity(k), o2.identity(v)));
			}
		}
		for (A1 k : o1.arrows()) {
			for (A2 v : o2.arrows()) {
				as.add(new Pair<>(k, v));
				srcs.put(new Pair<>(k, v),
						new Pair<>(o1.source(k), o2.source(v)));
				dsts.put(new Pair<>(k, v),
						new Pair<>(o1.target(k), o2.target(v)));
			}
		}
		for (Pair<A1, A2> a1 : as) {
			for (Pair<A1, A2> a2 : as) {
				if (!dsts.get(a1).equals(srcs.get(a2))) {
					continue;
				}
				comp.put(
						new Pair<>(a1, a2),
						new Pair<>(o1.compose(a1.first, a2.first), o2.compose(
								a1.second, a2.second)));
			}
		}
		return new FiniteCategory<>(os, as, srcs, dsts, comp, ids);
	}

	public static <O1, A1, O2, A2> Functor<Pair<O1, O2>, Pair<A1, A2>, O1, A1> first(
			Category<O1, A1> o1, Category<O2, A2> o2) {
		return new Functor<>(product(o1, o2), o1, x -> x.first, x -> x.first);
	}

	public static <O1, A1, O2, A2> Functor<Pair<O1, O2>, Pair<A1, A2>, O2, A2> second(
			Category<O1, A1> o1, Category<O2, A2> o2) {
		return new Functor<>(product(o1, o2), o2, x -> x.second, x -> x.second);
	}

	public static <O, A, O1, A1, O2, A2> Functor<O, A, Pair<O1, O2>, Pair<A1, A2>> pair(
			Functor<O, A, O1, A1> a1, Functor<O, A, O2, A2> a2) {
		if (!a1.source.equals(a2.source)) {
			throw new RuntimeException();
		}
		return new Functor<>(a1.source, product(a1.target, a2.target),
				x -> new Pair<>(a1.applyO(x), a2.applyO(x)), x -> new Pair<>(
						a1.applyA(x), a2.applyA(x)));
	}

	public static Category<Void, Void> initial() {
		return new FiniteCategory<>();
	}

	public static <O, A> Functor<Void, Void, O, A> initial(Category<O, A> o) {
		return new Functor<>(initial(), o, x -> {
			throw new RuntimeException();
		}, x -> {
			throw new RuntimeException();
		});
	}

	public static <O1, A1, O2, A2> Category<Chc<O1, O2>, Chc<A1, A2>> coproduct(
			Category<O1, A1> o1, Category<O2, A2> o2) {
		Set<Chc<O1, O2>> os = new HashSet<>();
		Set<Chc<A1, A2>> as = new HashSet<>();
		Map<Chc<A1, A2>, Chc<O1, O2>> src = new HashMap<>();
		Map<Chc<A1, A2>, Chc<O1, O2>> dst = new HashMap<>();
		Map<Pair<Chc<A1, A2>, Chc<A1, A2>>, Chc<A1, A2>> comp = new HashMap<>();
		Map<Chc<O1, O2>, Chc<A1, A2>> id = new HashMap<>();
		for (O1 o : o1.objects()) {
			os.add(Chc.inLeftNC(o));
			id.put(Chc.inLeftNC(o), Chc.inLeftNC(o1.identity(o)));
		}
		for (O2 o : o2.objects()) {
			os.add(Chc.inRightNC(o));
			id.put(Chc.inRightNC(o), Chc.inRightNC(o2.identity(o)));
		}
		for (A1 o : o1.arrows()) {
			as.add(Chc.inLeftNC(o));
			src.put(Chc.inLeftNC(o), Chc.inLeftNC(o1.source(o)));
			dst.put(Chc.inLeftNC(o), Chc.inLeftNC(o1.target(o)));
		}
		for (A2 o : o2.arrows()) {
			as.add(Chc.inRightNC(o));
			src.put(Chc.inRightNC(o), Chc.inRightNC(o2.source(o)));
			dst.put(Chc.inRightNC(o), Chc.inRightNC(o2.target(o)));
		}
		for (Chc<A1, A2> k : as) {
			for (Chc<A1, A2> v : as) {
				if (!dst.get(k).equals(src.get(v))) {
					continue;
				}
				if (k.left) {
					comp.put(new Pair<>(k, v), Chc.inLeftNC(o1.compose(k.l, v.l)));
				} else {
					comp.put(new Pair<>(k, v),
							Chc.inRightNC(o2.compose(k.r, v.r)));
				}
			}
		}
		return new FiniteCategory<>(os, as, src, dst, comp, id);
	}

	public static <O1, A1, O2, A2> Functor<O1, A1, Chc<O1, O2>, Chc<A1, A2>> inLeftNC(
			Category<O1, A1> o1, Category<O2, A2> o2) {
		return new Functor<>(o1, coproduct(o1, o2), Chc::inLeftNC,
                Chc::inLeftNC);
	}

	public static <O1, A1, O2, A2> Functor<O2, A2, Chc<O1, O2>, Chc<A1, A2>> inRightNC(
			Category<O1, A1> o1, Category<O2, A2> o2) {
		return new Functor<>(o2, coproduct(o1, o2), Chc::inRightNC,
                Chc::inRightNC);
	}

	public static <O, A, O1, A1, O2, A2> Functor<Chc<O1, O2>, Chc<A1, A2>, O, A> match(
			Functor<O1, A1, O, A> a1, Functor<O2, A2, O, A> a2) {
		if (!a1.target.equals(a2.target)) {
			throw new RuntimeException();
		}
		FUNCTION<Chc<O1, O2>, O> o = x -> x.left ? a1.applyO(x.l) : a2
				.applyO(x.r);
		FUNCTION<Chc<A1, A2>, A> a = x -> x.left ? a1.applyA(x.l) : a2
				.applyA(x.r);
		return new Functor<>(coproduct(a1.source, a2.source), a1.target, o, a);
	}

	private final Map<Pair<Category<Object, Object>, Category<Object, Object>>, Set<Functor<Object, Object, Object, Object>>> cached = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <O1, A1, O2, A2> Set<Functor<O1, A1, O2, A2>> hom(Category<O1, A1> A,
			Category<O2, A2> B) {
		Pair<Category<O1, A1>, Category<O2, A2>> p = new Pair<>(A, B);
		Set<Functor<O1, A1, O2, A2>> retX = (Set<Functor<O1, A1, O2, A2>>) ((Object) cached
				.get(p));
		if (retX != null) {
			return retX;
		}
		Set<Functor<O1, A1, O2, A2>> ret = exp(B, A).objects();
		cached.put(
				(Pair<Category<Object, Object>, Category<Object, Object>>) ((Object) p),
				(Set<Functor<Object, Object, Object, Object>>) ((Object) ret));
		return ret;
	}

	public static <O1, A1, O2, A2> Category<Functor<O1, A1, O2, A2>, Transform<O1, A1, O2, A2>> exp(
			Category<O2, A2> base, Category<O1, A1> exp) {
	
		Set<Functor<O1, A1, O2, A2>> mappings = new HashSet<>();
		Set<Transform<O1, A1, O2, A2>> arrows = new HashSet<>();
		Map<Transform<O1, A1, O2, A2>, Functor<O1, A1, O2, A2>> src = new HashMap<>();
		Map<Transform<O1, A1, O2, A2>, Functor<O1, A1, O2, A2>> dst = new HashMap<>();
		Map<Pair<Transform<O1, A1, O2, A2>, Transform<O1, A1, O2, A2>>, Transform<O1, A1, O2, A2>> comp = new HashMap<>();
		Map<Functor<O1, A1, O2, A2>, Transform<O1, A1, O2, A2>> id = new HashMap<>();

		List<LinkedHashMap<O1, O2>> nms = FinSet.homomorphs(new LinkedList<>(
				exp.objects()), new LinkedList<>(base.objects()));

		for (LinkedHashMap<O1, O2> nm : nms) {
			LinkedHashMap<A1, List<A2>> ems = new LinkedHashMap<>(); // arrow ->
																		// arrow
			for (A1 e : exp.arrows()) {
				List<A2> k = new LinkedList<>(base.hom(nm.get(exp.source(e)),
						nm.get(exp.target(e))));
				ems.put(e, k);
			}
			List<LinkedHashMap<A1, A2>> ems0 = FinSet.homomorphs(ems);
			for (LinkedHashMap<A1, A2> em : ems0) {
				try {
					Functor<O1, A1, O2, A2> m = new Functor<>(exp, base, nm::get, em::get);
					mappings.add(m);
					id.put(m, new Transform<>(m, m, x -> base.identity(nm.get(x))));
				} catch (Exception e) {
				}
			}
		}
		for (Functor<O1, A1, O2, A2> s : mappings) {
			for (Functor<O1, A1, O2, A2> t : mappings) {
				Map<O1, List<A2>> map = new HashMap<>();
				for (O1 n : exp.objects()) {
					map.put(n,
							new LinkedList<>(base.hom(s.applyO(n), t.applyO(n))));
				}
				List<LinkedHashMap<O1, A2>> map0 = FinSet.homomorphs(map);
				for (LinkedHashMap<O1, A2> em : map0) {
					try {
						Transform<O1, A1, O2, A2> m = new Transform<>(s, t, em::get);
						arrows.add(m);
						src.put(m, s);
						dst.put(m, t);
					} catch (Exception e) {
					}
				}
			}
		}
		for (Transform<O1, A1, O2, A2> s : arrows) {
			for (Transform<O1, A1, O2, A2> t : arrows) {
				if (!s.target.equals(t.source)) {
					continue;
				}
				comp.put(new Pair<>(s, t), Transform.compose(s, t));
			}
		}
		return new FiniteCategory<>(mappings, arrows, src, dst, comp, id);
	}

	// A^B * B -> A
	public static <AO, AA, BO, BA> Functor<Pair<Functor<BO, BA, AO, AA>, BO>, Pair<Transform<BO, BA, AO, AA>, BA>, AO, AA> eval(
			Category<AO, AA> A, Category<BO, BA> B) {
		FUNCTION<Pair<Functor<BO, BA, AO, AA>, BO>, AO> f = x -> {
			AO ret = x.first.applyO(x.second);
			return ret;
		};
		FUNCTION<Pair<Transform<BO, BA, AO, AA>, BA>, AA> g = x -> {
			AA ff = x.first.source.applyA(x.second);
			AA gg = x.first.apply(B.target(x.second));
			return A.compose(ff, gg);
		};
		return new Functor<>(product(exp(A, B), B), A, f, g);
	}

	// A*B -> C --> A -> C^B
	public static <AO, AA, BO, BA, CO, CA> Functor<AO, AA, Functor<BO, BA, CO, CA>, Transform<BO, BA, CO, CA>> curry(
			Functor<Pair<AO, BO>, Pair<AA, BA>, CO, CA> F) {
		Pair<Category<AO, AA>, Category<BO, BA>> w = split(F.source);
		Category<AO, AA> A = w.first;
		Category<BO, BA> B = w.second;
		Category<CO, CA> C = F.target;
		FUNCTION<AO, Functor<BO, BA, CO, CA>> p = a -> {
			FUNCTION<BO, CO> f1 = b -> F.applyO(new Pair<>(a, b));
			FUNCTION<BA, CA> f2 = g -> F.applyA(new Pair<>(A.identity(a), g));
			return new Functor<>(B, C, f1, f2);
		};
		FUNCTION<AA, Transform<BO, BA, CO, CA>> q = f -> {
			FUNCTION<BO, CA> t = b -> F.applyA(new Pair<>(f, B.identity(b)));
			return new Transform<>(p.apply(A.source(f)), p.apply(A.target(f)), t);
		};
		return new Functor<>(A, exp(C, B), p, q);
	}

	public static <O1, A1, O2, A2> Pair<Category<O1, A1>, Category<O2, A2>> split(
			Category<Pair<O1, O2>, Pair<A1, A2>> ab) {
		Set<O1> ao = new HashSet<>();
		Set<A1> aa = new HashSet<>();
		Map<A1, O1> as = new HashMap<>();
		Map<A1, O1> at = new HashMap<>();
		Map<Pair<A1, A1>, A1> ac = new HashMap<>();
		Map<O1, A1> ai = new HashMap<>();
		Set<A2> ba = new HashSet<>();
		Set<O2> bo = new HashSet<>();
		Map<O2, A2> bi = new HashMap<>();
		Map<Pair<A2, A2>, A2> bc = new HashMap<>();
		Map<A2, O2> bt = new HashMap<>();
		Map<A2, O2> bs = new HashMap<>();
		for (Pair<O1, O2> p : ab.objects()) {
			ao.add(p.first);
			bo.add(p.second);
			ai.put(p.first, ab.identity(p).first);
			bi.put(p.second, ab.identity(p).second);
		}
		for (Pair<A1, A2> p : ab.arrows()) {
			aa.add(p.first);
			ba.add(p.second);
			as.put(p.first, ab.source(p).first);
			bs.put(p.second, ab.source(p).second);
			at.put(p.first, ab.target(p).first);
			bt.put(p.second, ab.target(p).second);
		}
		for (Pair<A1, A2> p : ab.arrows()) {
			for (Pair<A1, A2> q : ab.arrows()) {
				if (!ab.target(p).equals(ab.source(q))) {
					continue;
				}
				ac.put(new Pair<>(p.first, q.first), ab.compose(p, q).first);
				bc.put(new Pair<>(p.second, q.second), ab.compose(p, q).second);
			}
		}
		Category<O1, A1> a = new FiniteCategory<>(ao, aa, as, at, ac, ai);
		Category<O2, A2> b = new FiniteCategory<>(bo, ba, bs, bt, bc, bi);
		return new Pair<>(a, b);
	}

	public static <O1, A1, O2, A2> Optional<Pair<Functor<O1, A1, O2, A2>, Functor<O2, A2, O1, A1>>> iso(
			Category<O1, A1> exp, Category<O2, A2> base) {

		if (base.objects().size() != exp.objects().size()) {
			return Optional.empty();
		}
		if (base.arrows().size() != exp.arrows().size()) {
			return Optional.empty();
		}

		List<LinkedHashMap<O1, O2>> nms = FinSet.bijections(new LinkedList<>(
				exp.objects()), new LinkedList<>(base.objects()));

		outer: for (LinkedHashMap<O1, O2> nm : nms) {
			Map<Pair<O1, O1>, Function<A1, A2>> map1 = new HashMap<>();
			Map<Pair<O2, O2>, Function<A2, A1>> map2 = new HashMap<>();
			for (O1 o1 : exp.objects()) {
				inner: for (O1 o2 : exp.objects()) {
					Set<A1> b1 = exp.hom(o1, o2);
					Set<A2> b2 = base.hom(nm.get(o1), nm.get(o2));
					if (b1.size() != b2.size()) {
						continue outer;
					}
					List<LinkedHashMap<A1, A2>> k = FinSet.bijections(
							new LinkedList<>(b1), new LinkedList<>(b2));
					for (LinkedHashMap<A1, A2> em : k) {
						try {
							Function<A1, A2> m1 = em::get;
							Function<A2, A1> m2 = invget(em);
							map1.put(new Pair<>(o1, o2), m1);
							map2.put(new Pair<>(nm.get(o1), nm.get(o2)), m2);
							continue inner;
						} catch (Exception e) {
						}
					}
					continue outer;
				}
			}
			if (map1.size() != exp.objects().size() * exp.objects().size()) {
				throw new RuntimeException();
			}
			if (map2.size() != exp.objects().size() * exp.objects().size()) {
				throw new RuntimeException();
			}
			Functor<O1, A1, O2, A2> f1 = new Functor<>(exp, base, nm::get, a -> map1.get(
					new Pair<>(exp.source(a), exp.target(a))).apply(a));
			Functor<O2, A2, O1, A1> f2 = new Functor<>(base, exp, invget(nm), a -> {
					A1 ret = map2.get(
							new Pair<>(base.source(a), base.target(a)))
							.apply(a);
					return ret;
				});
			return Optional.of(new Pair<>(f1, f2));
		}

		return Optional.empty();
	}
	
	private static <K, V> FUNCTION<V, K> invget(Map<K, V> m) {
		return v -> {
			for (Entry<K, V> e : m.entrySet()) {
				if (e.getValue().equals(v)) {
					return e.getKey();
				}
			}
			throw new RuntimeException("Cannot inverse lookup " + v + " in " + m);
		};
	}
	
	public static <O1,O2,O3,O4,A1,A2,A3,A4> Functor<Pair<O1,O3>,Pair<A1,A3>,Pair<O2,O4>,Pair<A2,A4>> pairF(Functor<O1,A1,O2,A2> a1, Functor<O3,A3,O4,A4> a2) {
		return pair(Functor.compose(first (a1.source, a2.source), a1), Functor.compose(second(a1.source, a2.source), a2));
	}
	
	public static <O1,O2,O3,O4,A1,A2,A3,A4> Functor<Chc<O1,O3>,Chc<A1,A3>,Chc<O2,O4>,Chc<A2,A4>> matchF(Functor<O1,A1,O2,A2> a1, Functor<O3,A3,O4,A4> a2) {
		return match(Functor.compose(a1, inLeftNC (a1.target, a2.target)), Functor.compose(a2, inRightNC(a1.target, a2.target)));
	}


}

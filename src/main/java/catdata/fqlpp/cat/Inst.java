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
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.fqlpp.FUNCTION;
import catdata.fqlpp.cat.FinSet.Fn;

@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class Inst<O, A> extends Category<Functor<O, A, Set, Fn>, Transform<O, A, Set, Fn>> {

	@Override
	public boolean equals(Object o) {
        return o instanceof Inst && ((Inst<?, ?>) o).cat.equals(cat);
    }

	@Override
	public int hashCode() {
		return 0;
	}

	private static final Map<Object, Object> map = new HashMap<>();
	public static <O,A> Inst<O,A> get(Category<O,A> cat) {
		if (map.containsKey(cat)) {
			return (Inst<O, A>) map.get(cat);
		}
		map.put(cat, new Inst(cat));
		return (Inst<O, A>) map.get(cat);
	}
	
	private Inst(Category<O, A> cat) {
		if (cat.isInfinite()) {
			throw new RuntimeException("Cannot construct Set^C for infinite C.  Was given C=" + cat);
		}
		this.cat = cat;
	}

	public Category<O, A> cat;

	@Override
	public boolean isInfinite() {
		return true;
	}

	@Override
	public String toString() {
		return "(Set ^ " + cat + ")";
	}

	@Override
	public void validate() {
	}

	@Override
	public Set<Functor<O, A, Set, Fn>> objects() {
		throw new RuntimeException("Cannot enumerate objects of " + this);
	}

	@Override
	public Set<Transform<O, A, Set, Fn>> arrows() {
		throw new RuntimeException("Cannot enumerate arrows of " + this);
	}
	
	@Override
	public boolean isObject(Functor<O, A, Set, Fn> o) {
		return o.source.equals(cat) && o.target.equals(FinSet.FinSet);
	}
	@Override
	public boolean isArrow(Transform<O, A, Set, Fn> a) {
		return isObject(a.source) && isObject(a.target);
	}

	@Override
	public Functor<O, A, Set, Fn> source(Transform<O, A, Set, Fn> a) {
		return a.source;
	}

	@Override
	public Functor<O, A, Set, Fn> target(Transform<O, A, Set, Fn> a) {
		return a.target;
	}

	@Override
	public Transform<O, A, Set, Fn> identity(Functor<O, A, Set, Fn> o) {
		return Transform.id(o);
	}

	@Override
	public Transform<O, A, Set, Fn> compose(Transform<O, A, Set, Fn> f, Transform<O, A, Set, Fn> g) {
		return Transform.compose(f, g);
	}

	public Functor<O, A, Set, Fn> terminal() {
		Set<Unit> ret = new HashSet<>();
		ret.add(Unit.unit);
		Fn<Unit, Unit> fn = new Fn<>(ret, ret, x -> Unit.unit);
		return new Functor<>(cat, FinSet.FinSet, x -> ret, x -> fn);
	}

	public  Transform<O, A, Set, Fn> terminal(Functor<O, A, Set, Fn> o) {
		Set<Unit> ret = new HashSet<>();
		ret.add(Unit.unit);
		FUNCTION<O, Fn> fn = x -> new Fn(o.applyO(x), ret, y -> Unit.unit);
		return new Transform<>(o, terminal(), fn);
	}

	public Functor<O, A, Set, Fn> product(Functor<O, A, Set, Fn> f, Functor<O, A, Set, Fn> g) {
		FUNCTION<O, Set> h = x -> FinSet.product(f.applyO(x), g.applyO(x));
		FUNCTION<A, Fn> i = x -> FinSet.pairF(f.applyA(x), g.applyA(x));
		return new Functor<>(cat, FinSet.FinSet, h, i);
	} 
	

	public Transform<O, A, Set, Fn> first(Functor<O, A, Set, Fn> o1, Functor<O, A, Set, Fn> o2) {
		FUNCTION<O, Fn> f = (O o) -> new Fn(FinSet.product(o1.applyO(o),
				o2.applyO(o)), o1.applyO(o), x -> ((Pair)x).first);
		return new Transform<>(product(o1, o2), o1, f);
	}

	public Transform<O, A, Set, Fn> second(Functor<O, A, Set, Fn> o1, Functor<O, A, Set, Fn> o2) {
		FUNCTION<O, Fn> f = (O o) -> new Fn(FinSet.product(o1.applyO(o),
				o2.applyO(o)), o2.applyO(o), x -> ((Pair)x).second);
		return new Transform<>(product(o1, o2), o2, f);
	}

	public Transform<O, A, Set, Fn> pair(Transform<O, A, Set, Fn> f, Transform<O, A, Set, Fn> g) {
		if (!f.source.equals(g.source)) {
			throw new RuntimeException();
		}
		FUNCTION<O, Fn> fn = o -> new Fn<>(f.source.applyO(o), FinSet.product(
				f.target.applyO(o), g.target.applyO(o)), x -> new Pair<>(f.apply(o).apply(x), g
				.apply(o).apply(x)));
		return new Transform<>(f.source, product(f.target, g.target), fn);
	}
	
	public Transform<O, A, Set, Fn> initial(Functor<O, A, Set, Fn> o) {
		Set ret = new HashSet<>();
		FUNCTION<O, Fn> fn = x -> new Fn<>(ret, o.applyO(x), y -> { throw new RuntimeException(); });
		return new Transform<>(initial(), o, fn);
	}
	
	public Functor<O, A, Set, Fn> initial() {
		Set ret = new HashSet<>();
		Fn fn = new Fn<>(ret, ret, x -> { throw new RuntimeException(); });
		return new Functor<>(cat, FinSet.FinSet, x -> ret, x -> fn);
	}
		 
	
	
	public Functor<O, A, Set, Fn> coproduct(Functor<O, A, Set, Fn> f, Functor<O, A, Set, Fn> g) {
		FUNCTION<O, Set> h = x -> FinSet.coproduct(f.applyO(x), g.applyO(x));
		FUNCTION<A, Fn> i = x -> FinSet.matchF(f.applyA(x), g.applyA(x));
		return new Functor<>(cat, FinSet.FinSet, h, i);
	}

	public Transform<O, A, Set, Fn> inleft(Functor<O, A, Set, Fn> o1, Functor<O, A, Set, Fn> o2) {
		FUNCTION<O, Fn<?, Chc>> f = (O o) -> new Fn(o1.applyO(o), FinSet.coproduct(o1.applyO(o),
				o2.applyO(o)), Chc::inLeft);
		return new Transform(o1, coproduct(o1, o2), f);
	}

	public Transform<O, A, Set, Fn> inright(Functor<O, A, Set, Fn> o1, Functor<O, A, Set, Fn> o2) {
		FUNCTION<O, Fn<?, Chc>> f = (O o) -> new Fn(o2.applyO(o), FinSet.coproduct(o1.applyO(o),
				o2.applyO(o)), Chc::inRight);
		return new Transform(o2, coproduct(o1, o2), f);
	}
	
	public Transform<O, A, Set, Fn> match(Transform<O, A, Set, Fn> f, Transform<O, A, Set, Fn> g) {
		if (!f.target.equals(g.target)) {
			throw new RuntimeException();
		}
		FUNCTION<O, Fn<Chc, ?>> fn = (O o) -> new Fn(FinSet.coproduct(f.source.applyO(o), g.source.applyO(o)), 
				f.target.applyO(o), x -> ((Chc)x).left ? f.apply(o).apply(((Chc)x).l) : g.apply(o).apply(((Chc)x).r));
		return new Transform(coproduct(f.source, g.source), f.target, fn);
	}
	
	//A -> Set^B  ~~>  A*B -> Set
	public static <O1,A1,O2,A2> Functor<Pair<O1,O2>, Pair<A1,A2>, Set, Fn> 
	  UNCURRY(Functor<O1,A1,Functor<O2,A2,Set,Fn>, Transform<O2,A2,Set,Fn>> F) {
		
		if (!(F.target instanceof Inst)) {
			throw new RuntimeException("Target category is not Set^C, for some C");
		}
		
		Inst<O2,A2> Ft = (Inst<O2,A2>) F.target;
		Category<O2, A2> B = Ft.cat;
		Category<O1, A1> A = F.source;
		Category<Pair<O1,O2>,Pair<A1,A2>> AB = FinCat.product(A, B);
		
		FUNCTION<Pair<O1,O2>, Set> f = x -> F.applyO(x.first).applyO(x.second);
		FUNCTION<Pair<A1,A2>, Fn>  g = x -> { 
			Fn k = F.applyA(x.first).apply(B.source(x.second));
			Fn l = F.applyO(A.target(x.first)).applyA(x.second);
			return Fn.compose(k, l);
		};
		Category<Set, Fn> fs = FinSet.FinSet;
		
		return new Functor<>(AB, fs, f, g);
	}
	
	public static <O1,A1,O2,A2> Transform<O1,A1,Functor<O2,A2,Set,Fn>, Transform<O2,A2,Set,Fn>>
	  CURRY(Transform<Pair<O1,O2>, Pair<A1,A2>, Set, Fn> t) {
		Functor<O1,A1,Functor<O2,A2,Set,Fn>, Transform<O2,A2,Set,Fn>> I = CURRY(t.source);
		Functor<O1,A1,Functor<O2,A2,Set,Fn>, Transform<O2,A2,Set,Fn>> J = CURRY(t.target);
		
		return new Transform<>(I, J, c -> new Transform<>(I.applyO(c), J.applyO(c), d -> t.apply(new Pair<>(c,d))));
	}
	
	public static <O1,A1,O2,A2> Functor<O1,A1,Functor<O2,A2,Set,Fn>, Transform<O2,A2,Set,Fn>>
	  CURRY(Functor<Pair<O1,O2>, Pair<A1,A2>, Set, Fn> F) {
		
		if (!F.target.equals(FinSet.FinSet)) {
			throw new RuntimeException("Target category is not Set");
		}
		
		Pair<Category<O1,A1>,Category<O2,A2>> AB = FinCat.split(F.source);
		
		Category<O2, A2> B = AB.second;
		Category<O1, A1> A = AB.first;
		
		Category<Functor<O2,A2,Set,Fn>, Transform<O2,A2,Set,Fn>> SetB = get(B);
		
		FUNCTION<O1, Functor<O2,A2,Set,Fn>> f 
		  = o1 -> new Functor<>(B, FinSet.FinSet, o2 -> F.applyO(new Pair<>(o1,o2)), a2 -> F.applyA(new Pair<>(A.identity(o1), a2))); 
		FUNCTION<A1, Transform<O2,A2,Set,Fn>> g 
		  = a1 -> new Transform<>(f.apply(A.source(a1)), f.apply(A.target(a1)), o2 -> F.applyA(new Pair<>(a1,B.identity(o2)))); 
		
		return new Functor<>(A, SetB, f, g);
	}
	
	public Optional<Pair<Transform<O, A, Set, Fn>, Transform<O, A, Set, Fn>>> iso(
			Functor<O, A, Set, Fn> exp, Functor<O, A, Set, Fn> base) {
		if (!exp.source.equals(base.source)) {
			throw new RuntimeException("Source categories do not match.");
		}

		if (base.source.objects().isEmpty()) {
			Transform<O, A, Set, Fn> t1 = new Transform<>(base, exp, x -> { throw new RuntimeException(); });
			Transform<O, A, Set, Fn> t2 = new Transform<>(exp, base, x -> { throw new RuntimeException(); });
			return Optional.of(new Pair<>(t1, t2));
		}
		
		Map<O, List<LinkedHashMap<Object, Object>>> m = new HashMap<>();
		for (O n : base.source.objects()) {
			if (base.applyO(n).size() != exp.applyO(n).size()) {
				return Optional.empty();
			}
			List<LinkedHashMap<Object, Object>> bijs = FinSet.bijections(
					new LinkedList<>(exp.applyO(n)), new LinkedList<>(base.applyO(n)));
			m.put(n, bijs);
		}
		List<LinkedHashMap<O, LinkedHashMap<Object, Object>>> m0 = FinSet.homomorphs(m);
		for (LinkedHashMap<O, LinkedHashMap<Object, Object>> k : m0) {
			Map<O, Map<Object,Object>> data1 = new HashMap<>();
			Map<O, Map<Object,Object>> data2 = new HashMap<>();
			for (O n : base.source.objects()) {
				LinkedHashMap<Object, Object> v = k.get(n);
				Map<Object, Object> d1 = new HashMap<>();
				Map<Object, Object> d2 = new HashMap<>();
				for (Entry<Object,Object> u : v.entrySet()) {
					if (d1.containsKey(u.getKey())) {
						throw new RuntimeException("Report to Ryan.");
					}
					d1.put(u.getKey(), u.getValue());
					if (d2.containsKey(u.getValue())) {
						throw new RuntimeException("Report to Ryan.");
					}
					d2.put(u.getValue(), u.getKey());
				}
				data1.put(n, d1);
				data2.put(n, d2);
			}
			try {
				Transform<O,A,Set,Fn> t1 = new Transform<>(exp, base, n -> new Fn<>(exp.applyO(n), base.applyO(n), data1.get(n)::get));
				Transform<O,A,Set,Fn> t2 = new Transform<>(base, exp, n -> new Fn<>(base.applyO(n), exp.applyO(n), data2.get(n)::get));
				Functor<O,A,Set,Fn> b0 = apply(t1, exp);
				Functor<O,A,Set,Fn> a0 = apply(t2, base);
				if (exp.equals(a0) && base.equals(b0)) {
					return Optional.of(new Pair<>(t1, t2));
				}
			} catch (Exception re) {
			}
		}
		return Optional.empty();
	}
	
	private Functor<O,A,Set,Fn> apply(Transform<O, A, Set, Fn> t1, Functor<O, A, Set, Fn> I) {
		
		FUNCTION<O, Set> f = o -> (Set) I.applyO(o).stream().map(i -> t1.apply(o).apply(i)).collect(Collectors.toSet());
		
		FUNCTION<A, Fn> g = a -> { 
			Map<Object,Object> m = Util.reify(I.applyA(a)::apply, I.applyO(I.source.source(a)));
			Map n = new HashMap<>();
			for (Entry e : m.entrySet()) {
				n.put(t1.apply(I.source.source(a)).apply(e.getKey()), t1.apply(I.source.target(a)).apply(e.getValue()));
			}
			return new Fn<>(f.apply(I.source.source(a)), f.apply(I.source.target(a)), n::get);
		};
				
		return new Functor<>(I.source, FinSet.FinSet, f, g);
	}
	

	@Override
	public Set<Transform<O,A,Set,Fn>> hom(Functor<O,A,Set,Fn> exp, Functor<O,A,Set,Fn> base) {
		if (!exp.source.equals(base.source)) {
			throw new RuntimeException("Source categories do not match.");
		}
		Set<Transform<O,A,Set,Fn>> ret = new HashSet<>();
		if (base.source.objects().isEmpty()) {
			Transform<O, A, Set, Fn> t1 = new Transform<>(base, exp, x -> { throw new RuntimeException(); });
			ret.add(t1);
			return ret;
		}
		
		Map<O, List<LinkedHashMap<Object, Object>>> m = new HashMap<>();
		for (O n : base.source.objects()) {
			List<LinkedHashMap<Object, Object>> bijs = FinSet.homomorphs(
					new LinkedList<>(exp.applyO(n)), new LinkedList<>(base.applyO(n)));
			m.put(n, bijs);
		}
		List<LinkedHashMap<O, LinkedHashMap<Object, Object>>> m0 = FinSet.homomorphs(m);
		for (LinkedHashMap<O, LinkedHashMap<Object, Object>> k : m0) {
			Map<O, Map<Object,Object>> data1 = new HashMap<>();
			for (O n : base.source.objects()) {
				LinkedHashMap<Object, Object> v = k.get(n);
				Map<Object, Object> d1 = new HashMap<>();
				for (Entry<Object,Object> u : v.entrySet()) {
					if (d1.containsKey(u.getKey())) {
						throw new RuntimeException("Report to Ryan.");
					}
					d1.put(u.getKey(), u.getValue());
				}
				data1.put(n, d1);
			}
			try {
				Transform<O,A,Set,Fn> t1 = new Transform<>(exp, base, n -> new Fn<>(exp.applyO(n), base.applyO(n), data1.get(n)::get));
				ret.add(t1);
			} catch (Exception re) {
			}
		}
		return ret;
	}
	
	public Functor<O, A, Set<Transform<O, A, Set, Fn>>, Fn<Transform<O, A, Set, Fn>, Transform<O, A, Set, Fn>>> exp(
			 Functor<O,A,Set,Fn> J, Functor<O,A,Set,Fn> I) { //J^I
		if (!J.source.equals(I.source)) {
			throw new RuntimeException();
		}
		
		Pair<Map<O, Functor<O, A, Set, Fn>>, Map<A, Transform<O, A, Set, Fn>>> xxx = (Pair<Map<O, Functor<O, A, Set, Fn>>, Map<A, Transform<O, A, Set, Fn>>>) ((Object)I.source.repX());
		Map<O, Functor<O, A, Set, Fn>> nm = xxx.first;
		Map<A, Transform<O, A, Set, Fn>> em = xxx.second;

		Map<O, Set<Transform<O,A,Set,Fn>>> data1 = new HashMap<>();
		Map<A, Fn<Transform<O,A,Set,Fn>,Transform<O,A,Set,Fn>>> data2 = new HashMap<>();
		
		for (O n : I.source.objects()) {
			Functor<O, A, Set, Fn> yyy = product(I, nm.get(n));
			data1.put(n, hom(yyy, J));
		}
		for (A e : I.source.arrows()) {
			Map<Transform<O,A,Set,Fn>,Transform<O,A,Set,Fn>> d = new HashMap<>();
			for (Transform<O,A,Set,Fn> k : data1.get(I.source.source(e))) {
				Transform<O,A,Set,Fn> h0 = em.get(e);
				Transform<O,A,Set,Fn> h = prod(I, h0);
				Transform<O,A,Set,Fn> t = Transform.compose(h, k);
				d.put(k, t);
			}
			data2.put(e, new Fn<>(data1.get(I.source.source(e)), data1.get(I.source.target(e)), d::get));
		}

		Functor<O,A,Set<Transform<O,A,Set,Fn>>,Fn<Transform<O,A,Set,Fn>,Transform<O,A,Set,Fn>>> IJ = new Functor<>(cat, FinSet.FinSet0(), data1::get, data2::get);

		return IJ;
	} 
	
	private Transform<O,A,Set,Fn> prod(Functor<O,A,Set,Fn> I, Transform<O,A,Set,Fn> h0) {
		Map<O, Fn> d = new HashMap<>();

		Functor<O,A,Set,Fn> X = product(I, h0.source);
		Functor<O,A,Set,Fn> Y = product(I, h0.target);
		
		for (O n : I.source.objects()) {
			Set<Pair<Object, Object>> v = X.applyO(n);
			Map<Pair<Object, Object>, Pair<Object, Object>> l = new HashMap<>();
			for (Pair<Object, Object> p : v) {
				l.put(p, new Pair<>(p.first, h0.apply(n).apply(p.second)));
			}
			d.put(n, new Fn<>(X.applyO(n), Y.applyO(n), l::get));
		}

		return new Transform(X, Y, d::get);
	}
	
	public Transform<O,A,Set,Fn> eval(Functor<O,A,Set,Fn> a, Functor<O,A,Set,Fn> b) {
		if (!a.source.equals(b.source)) {
			throw new RuntimeException("Source schemas do not match");
		}

		Functor<O,A,Set,Fn> ab = (Functor<O,A,Set,Fn>) ((Object)exp(a,b));
		Functor<O,A,Set,Fn> abb= product(ab,b);

		Map<O, Fn> data = new HashMap<>();
		for (O n : a.source.objects()) {
			Map<Object, Object> d = new HashMap<>();
			for (Object id : abb.applyO(n)) {
				Object id_ab = ((Pair)id).first; 
				Object x = ((Pair)id).second; 
				Transform<O,A,Set,Fn> t = (Transform) id_ab; 
				Object y = a.source.identity(n); 
				Object p = new Pair<>(x,y); 
				Object f = t.apply(n).apply(p); //lookup(t.data.get(n.string), p);
				d.put(id, f);
			}
			data.put(n, new Fn<>(abb.applyO(n), a.applyO(n), d::get));
		}
		Transform<O,A,Set,Fn> eval = new Transform<>(abb, a, data::get);
		return eval;
	}
	
	public Transform<O,A,Set,Fn> curry(Transform<O,A,Set,Fn> t) {
		Functor<O,A,Set,Fn> IJ = t.source;
		Functor<O,A,Set,Fn> K  = t.target;

		Pair<Functor<O,A,Set,Fn>, Functor<O,A,Set,Fn>> IJx = split(IJ);
		Functor<O,A,Set,Fn> I = IJx.first;
		Functor<O,A,Set,Fn> J = IJx.second;

		Functor<O, A, Set<Transform<O, A, Set, Fn>>, Fn<Transform<O, A, Set, Fn>, Transform<O, A, Set, Fn>>> JK = exp(K, J);
		
		Map<O, Fn> l = new HashMap<>();
		for (O c : I.source.objects()) {

			Map<Object, Transform<O,A,Set,Fn>> s = new HashMap<>();
			for (Object x : I.applyO(c)) {
				// construct transform depending on x

				Map<O, Map<Object, Object>> tx = new HashMap<>();
				for (O d : I.source.objects()) {
					Map<Object, Object> tx0 = new HashMap<>();
					for (A f : I.source.hom(c, d)) {
						for (Object y : J.applyO(d)) {
							Object Ifx = I.applyA(f).apply(x);
							Object v = t.apply(d).apply(new Pair<>(Ifx, y));
							tx0.put(new Pair<>(y, f), v);
						}
					}
					tx.put(d, tx0); 
				}
				Functor<O, A, Set, Fn> ppp = (Functor<O, A, Set, Fn>) ((Object)I.source.repX().first.get(c));
				Functor<O, A, Set, Fn> qqq = product(J, ppp);
				Transform<O,A,Set, Fn> xxx = new Transform<>(qqq, K, o -> new Fn<>(qqq.applyO(o), K.applyO(o), tx.get(o)::get)); //Hc * J -> K
				s.put(x, xxx);
			}
			l.put(c, new Fn(I.applyO(c), JK.applyO(c), s::get));
		}
		Transform zzz = new Transform(I, JK, l::get);
		return zzz;

	}
	
	private Pair<Functor<O,A,Set,Fn>, Functor<O,A,Set,Fn>> split(Functor<O,A,Set,Fn> I) {
		Map<O, Set> nm1 = new HashMap<>();
		Map<A, Fn> em1 = new HashMap<>();
		Map<O, Set> nm2 = new HashMap<>();
		Map<A, Fn> em2 = new HashMap<>();
		
		for (O o : I.source.objects()) {
			Set s1 = new HashSet();
			Set s2 = new HashSet();
			for (Object x : I.applyO(o)) {
				Pair p = (Pair) x;
				s1.add(p.first);
				s2.add(p.second);
			}
			nm1.put(o, s1);
			nm2.put(o, s2);
		}
		for (A a : I.source.arrows()) {
			Map s1 = new HashMap();
			Map s2 = new HashMap();
			for (Object o : I.applyO(I.source.source(a))) {
				Pair q = (Pair) o;
				Pair p = (Pair) I.applyA(a).apply(o);
				s1.put(q.first, p.first);
				s2.put(q.second, p.second);
			}
			Fn f1 = new Fn(nm1.get(I.source.source(a)), nm1.get(I.source.target(a)), s1::get);
			Fn f2 = new Fn(nm2.get(I.source.source(a)), nm2.get(I.source.target(a)), s2::get);
			em1.put(a, f1);
			em2.put(a, f2);
		}

		Functor<O,A,Set,Fn> fst = new Functor<>(cat, FinSet.FinSet, nm1::get, em1::get);
		Functor<O,A,Set,Fn> snd = new Functor<>(cat, FinSet.FinSet, nm2::get, em2::get);
		
		return new Pair<>(fst, snd);
	}
	
	private Functor<O, A, Set<Functor<O, A, Set, Fn>>, Fn<Functor<O, A, Set, Fn>, Functor<O, A, Set, Fn>>> prop;
	public Functor<O, A, Set<Functor<O, A, Set, Fn>>, Fn<Functor<O, A, Set, Fn>, Functor<O, A, Set, Fn>>> prop() {
		if (prop != null) {
			return prop;
		}
		Pair<Map<O, Functor<O, A, Set<A>, Fn<A, A>>>, Map<A, Transform<O, A, Set<A>, Fn<A, A>>>> k = cat.repX();
		
		Map<O, Set<Functor<O, A, Set, Fn>>> nm = new HashMap<>();
		for (O o : cat.objects()) {
			List<Functor<O, A, Set, Fn>> subs = k.first.get(o).subInstances();
			nm.put(o, new HashSet<>(subs));
		}
		Map<A, Fn<Functor<O, A, Set, Fn>,Functor<O, A, Set, Fn>>> em = new HashMap<>(); 
		for (A f : cat.arrows()) {
			Map<Functor<O, A, Set, Fn>,Functor<O, A, Set, Fn>> map = new HashMap<>();
			for (Functor<O, A, Set, Fn> J : nm.get(cat.source(f))) {
				Functor<O,A,Set,Fn> I = Transform.preimage((Transform<O,A,Set,Fn>)((Object)k.second.get(f)), J);	
				map.put(J, I);
			}
			em.put(f, new Fn<>(nm.get(cat.source(f)), nm.get(cat.target(f)), map::get));
		}
		
		prop = new Functor<>(cat, FinSet.FinSet0(), nm::get, em::get);
		return prop;
	}
	
	public Transform<O,A,Set,Fn> tru() {
		Set e = new HashSet<>();
		e.add(Unit.unit);
		Functor<O,A,Set,Fn> prp = (Functor<O,A,Set,Fn>) ((Object)prop());
		Pair<Map<O, Functor<O, A, Set<A>, Fn<A, A>>>, Map<A, Transform<O, A, Set<A>, Fn<A, A>>>> k = cat.repX();
		
		return new Transform<>(terminal(), prp, x -> new Fn<>(e, prp.applyO(x), z -> k.first.get(x)));
	}
	public Transform<O,A,Set,Fn> fals() {
		Set e = new HashSet<>();
		e.add(Unit.unit);
		Functor<O,A,Set,Fn> prp = (Functor<O,A,Set,Fn>) ((Object)prop());
		
		return new Transform<>(terminal(), prp, x -> new Fn<>(e, prp.applyO(x), z -> initial()));
	}
	
	
	public static <X, Y> Set<Y> image(Set<X> set, Function<X, Y> f) {
		return set.stream().map(f).collect(Collectors.toSet());
	}

	public Transform<O,A,Set,Fn> chr(Transform<O,A,Set,Fn> t) {
		Functor<O,A,Set,Fn> I = t.source;
		Functor<O,A,Set,Fn> J = t.target;
		Pair<Map<O, Functor<O, A, Set<A>, Fn<A, A>>>, Map<A, Transform<O, A, Set<A>, Fn<A, A>>>> HH = I.source.repX();
		Map<O,Functor<O,A,Set,Fn>> H = ((Map<O,Functor<O,A,Set,Fn>>)((Object)HH.first));
		Map<O, Fn> map = new HashMap<>();
		Functor<O,A,Set,Fn> prop = (Functor<O,A,Set,Fn>) ((Object)prop());
		for (O c : I.source.objects()) {
			Map<Object, Functor<O,A,Set,Fn>> m = new HashMap<>();
			for (Object x : J.applyO(c)) {
				Transform<O,A,Set,Fn> xx = new Transform<>(H.get(c), J, d -> new Fn<>(H.get(c).applyO(d), J.applyO(d), f -> J.applyA((A)f).apply(x)));
				Map<O, Set> pb1 = new HashMap<>();
				Map<A, Fn> pb2 = new HashMap<>();
				for (O d : I.source.objects()) {
					Set pb1x = new HashSet<>();
					for (Object y : H.get(c).applyO(d)) {
						Set im = image(t.apply(d).source, t.apply(d)::apply);
						if (im.contains(xx.apply(d).apply(y))) {
							pb1x.add(y);
						}
					}
					pb1.put(d, pb1x);
				}
				for (A f : I.source.arrows()) {
					Fn pb2x = new Fn(pb1.get(I.source.source(f)), pb1.get(I.source.target(f)), q -> H.get(c).applyA(f).apply(q));
					pb2.put(f, pb2x);
				}
				m.put(x, new Functor<>(cat, FinSet.FinSet, pb1::get, pb2::get));
			}
			map.put(c , new Fn(J.applyO(c), prop().applyO(c), m::get));
		}
		
		return new Transform<>(J, prop, map::get);
	}
		
	public Transform<O,A,Set,Fn> kernel(Transform<O,A,Set,Fn> L) {
		Functor<O,A,Set,Fn> J = L.source;
		
		Map<O,Set> nm = new HashMap<>();
		Map<A,Fn>  em = new HashMap<>();
		Map<O,Fn>  t  = new HashMap<>(); 
		
		for (O d : J.source.objects()) {
			Set s = new HashSet<>();
			for (Object x : J.applyO(d)) {
				if (L.apply(d).apply(x).equals(tru().apply(d).apply(Unit.unit))) {
					s.add(x);
				}
			}
			nm.put(d, s);
			t.put(d, new Fn<>(s, J.applyO(d), x -> x));
		}
		
		for (A f : J.source.arrows()) {
			em.put(f, new Fn<>(nm.get(J.source.source(f)), nm.get(J.source.target(f)), x -> J.applyA(f).apply(x)));
		}
		
		Functor<O,A,Set,Fn> I = new Functor<>(cat, FinSet.FinSet, nm::get, em::get);
		
		return new Transform<>(I, J, t::get);
	}
	
	public Transform<O,A,Set,Fn> andOrImplies(String which) {
		Map<O, Fn> map = new HashMap<>();
		
		Functor<O,A,Set,Fn> prp = (Functor<O,A,Set,Fn>) ((Object)prop());
		Functor<O,A,Set,Fn> prpprp = product(prp, prp);
		
		
		for (O n : cat.objects()) {
			Map<Object, Object> m = new HashMap<>();
			for (Object o : prpprp.applyO(n)) {
				Pair<Functor<O,A,Set,Fn>, Functor<O,A,Set,Fn>> I = (Pair<Functor<O,A,Set,Fn>, Functor<O,A,Set,Fn>>) o;
				
				Functor<O,A,Set,Fn> J;
				switch (which) {
					case "and":
						J = isect(I.first, I.second);
						break;
					case "or":
						J = union(I.first, I.second);
						break;
					case "implies":
						J = implies(n, I.first, I.second);
						break;
					default:
						throw new RuntimeException("Report this error to Ryan.");
				}
				m.put(I, J);
			}
			map.put(n, new Fn<>(prpprp.applyO(n), prp.applyO(n), m::get));
		}
	
		return new Transform<>(prpprp, prp, map::get);
	}
	
	private Functor<O,A,Set,Fn> isect(Functor<O,A,Set,Fn> a, Functor<O,A,Set,Fn> b) {
		Map<O, Set> nm = new HashMap<>();
		Map<A, Fn> em  = new HashMap<>();
		
		for (O n : cat.objects()) {
			Set set = new HashSet<>();
			for (Object p : a.applyO(n)) {
				if (b.applyO(n).contains(p)) {
					set.add(p);
				}
			}
			nm.put(n, set);
		}
		for (A n : cat.arrows()) {
			FUNCTION g = x -> {
				Object p = a.applyA(n).apply(x);
				Object q = b.applyA(n).apply(x);
				if (!p.equals(q)) {
					throw new RuntimeException("Report to Ryan: problem in intersection computation.");
				}
				return p;
			};
			Fn f = new Fn<>(nm.get(cat.source(n)), nm.get(cat.target(n)), g);
			em.put(n, f);
		}
		
		return new Functor<>(cat, FinSet.FinSet, nm::get, em::get);
	}
	
	private Functor<O,A,Set,Fn> union(Functor<O,A,Set,Fn> a, Functor<O,A,Set,Fn> b) {
		Map<O, Set> nm = new HashMap<>();
		Map<A, Fn> em  = new HashMap<>();
		
		for (O n : cat.objects()) {
			Set set = new HashSet<>();
			set.addAll(a.applyO(n));
			set.addAll(a.applyO(n));
			nm.put(n, set);
		}
		for (A n : cat.arrows()) {
			FUNCTION g = x -> {
				Object a_out = null;
				Object b_out = null;
				if (a.applyA(n).source.contains(x)) {
					a_out = a.applyA(n).apply(x);
				}
				if (b.applyA(n).source.contains(x)) {
					b_out = a.applyA(n).apply(x);
				}
				if (a_out != null && b_out != null && !a_out.equals(b_out)) {
					throw new RuntimeException("Report to Ryan: problem in union computation.");
				}
				if (a_out != null) {
					return a_out;
				}
				if (b_out != null) {
					return b_out;
				}
				throw new RuntimeException("Report to Ryan: problem in union computation.");
			};
			Fn f = new Fn<>(nm.get(cat.source(n)), nm.get(cat.target(n)), g);
			em.put(n, f);
		}
		
		return new Functor<>(cat, FinSet.FinSet, nm::get, em::get);
	}
	
	
	
	public Transform<O,A,Set,Fn> not() {
		Map<O, Fn> map = new HashMap<>();
		
		for (O n : cat.objects()) {
			Map<Functor<O,A,Set,Fn>, Functor<O,A,Set,Fn>> m = new HashMap<>();
			for (Functor<O,A,Set,Fn> I : prop().applyO(n)) {
				Functor<O,A,Set,Fn> J = not(cat.repX().first.get(n), I);
				m.put(I, J);
			}
			map.put(n, new Fn<>(prop().applyO(n), prop().applyO(n), m::get));
		}
		
		return new Transform<>((Functor<O,A,Set,Fn>)((Object)prop()), ((Functor<O,A,Set,Fn>)((Object)prop())), map::get);
	}

	private Functor<O, A, Set, Fn> not(Functor<O, A, Set<A>, Fn<A, A>> Hc,
			Functor<O, A, Set, Fn> I) {
		Map<O, Set<A>> nm = new HashMap<>();
		for (O d : cat.objects()) {
			Set<A> s = new HashSet<>();
			xxx : for (A ff : Hc.applyO(d)) {
				for (O d0 : cat.objects()) {
					for (A g : cat.hom(d, d0)) {
						A fg = cat.compose(ff, g);
						if (I.applyO(d0).contains(fg)) {
							continue xxx;
						} 
					}
				}
				s.add(ff);
			}
			nm.put(d, s);
		}
		Map<A, Fn> em = new HashMap<>();
		for (A h : cat.arrows()) {
			Map<Object,Object> dd = new HashMap<>();
			for (A ff : nm.get(cat.source(h))) { 
				A fg = cat.compose(ff, h);
				dd.put(ff, fg);
			}
			em.put(h, new Fn(nm.get(cat.source(h)), nm.get(cat.target(h)), dd::get));
		}		
		
		return new Functor<>(cat, FinSet.FinSet, nm::get, em::get);
	}
	
	private Functor<O,A,Set,Fn> implies(O c,
//			 Pair<Map<O, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>> H1 , 
		//	Triple<Instance, Map<Object, Path>, Map<Path, Object>> Hc, 
			Functor<O,A,Set,Fn> A, Functor<O,A,Set,Fn> B) {
		Map<O, Set<A>> nm = new HashMap<>();
		Map<A, Fn> em = new HashMap<>();
		Pair<Map<O, Functor<O, A, Set<A>, Fn<A, A>>>, Map<A, Transform<O, A, Set<A>, Fn<A, A>>>> H = cat.repX();
		for (O d : cat.objects()) {
			Set<A> dd = new HashSet<>();
			xxx : for (A ff : H.first.get(c).applyO(d)) {
				for (O d0 : cat.objects()) {
					for (A g : cat.hom(d, d0)) {
						A fg = cat.compose(ff, g);
						if (!A.applyO(d0).contains(fg) || B.applyO(d0).contains(fg)) {
						} else {
							continue xxx;
						}
					}
				}
				dd.add(ff);
			}			
			nm.put(d, dd);
		}
		for (A h : cat.arrows()) {
			Fn<A,A> f = new Fn<>(nm.get(cat.source(h)), nm.get(cat.target(h)), ff -> cat.compose(ff, h));
			em.put(h, f);
		}				
		Functor<O,A,Set,Fn> ret = new Functor<>(cat, FinSet.FinSet, nm::get, em::get);
		return ret;
	}


}




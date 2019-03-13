package catdata.fqlpp.cat;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.fqlpp.cat.FinSet.Fn;
import catdata.ide.DefunctGlobalOptions;

@SuppressWarnings("serial")
public abstract class Category<O, A> implements Serializable {

	@SuppressWarnings("static-method")
	public boolean isInfinite() {
		return false;
	}

	public boolean isObject(O o) {
		return objects().contains(o);
	}

	public boolean isArrow(A a) {
		return arrows().contains(a);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Category)) {
			return false;
		}
		Category<O, A> other = (Category<O, A>) obj;
		if (isInfinite() || other.isInfinite()) {
			return this == obj;
		}

		if (!objects().equals(other.objects())) {
			return false;
		}
		if (!arrows().equals(other.arrows())) {
			return false;
		}
		for (O o : objects()) {
			if (!identity(o).equals(other.identity(o))) {
				return false;
			}
		}
		for (A a : arrows()) {
			if (!source(a).equals(other.source(a))) {
				return false;
			}
			if (!target(a).equals(other.target(a))) {
				return false;
			}
		}
		for (A a : arrows()) {
			for (A b : arrows()) {
				if (!target(a).equals(source(b))) {
					continue;
				}
				if (!compose(a, b).equals(other.compose(a, b))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	public abstract Set<O> objects();

	public abstract Set<A> arrows();

	public abstract O source(A a);

	public abstract O target(A a);

	public abstract A identity(O o);

	public abstract A compose(A a1, A a2);

	private String toString_cache = null;

	@Override
	public String toString() {
		if (isInfinite()) {
			return "(cannot print)";
		}
		if (toString_cache != null) {
			return toString_cache;
		}
		String o = "";
		if (objects().size() < 2048) {
			for (O oo : objects()) {
				o += "\t" + oo + "\n";
			}
		} else {
			o = "too many to print";
		}

		String a = "";
		if (arrows().size() < 2048) {
			for (A aa : arrows()) {
				a += "\t" + aa + " : " + source(aa) + " -> " + target(aa) + "\n";
			}
		} else {
			a += "too many to print";
		}

		String c = "";
		int count = 0;
		//String count_str = "";
		outer: for (A a1 : arrows()) {
			for (A a2 : arrows()) {
				if (!target(a1).equals(source(a2))) {
					continue;
				}
				A a3 = compose(a1, a2);
				c += "\t" + a1 + " ; " + a2 + " = " + a3 + "\n";
				count++;
				if (count > 2048) {
					c = "too many to print";
					count = -1;
					break outer;
				}
			}
		}

		String j = "";
		if (objects().size() < 2048) {
			for (O oo : objects()) {
				j += "\t" + "id " + oo + " = " + identity(oo) + "\n";
			}
		} else {
			j = "too many to print";
		}

		String ret = "objects (" + objects().size() + "):\n" + o + "\n\narrows (" + arrows().size()
				+ "):\n" + a + "\n\ncomposition (" + (count == -1 ? "> 2048" : count) + "):\n" + c
				+ "\n\nidentities (" + objects().size() + "):\n" + j;
		toString_cache = ret;
		return ret;
	}

	 public void validate() {
		if (!DefunctGlobalOptions.debug.fqlpp.VALIDATE) {
			return;
		}
		if (isInfinite()) {
			return;
		}
		if (arrows().size() < objects().size()) {
			throw new RuntimeException("Missing arrows: " + this);
		}
		for (A a : arrows()) {
			if (source(a) == null) {
				throw new RuntimeException(a + " has no source " + this);
			}
			if (target(a) == null) {
				throw new RuntimeException(a + " has no dst ");
			}
			if (!isObject(source(a))) {
				throw new RuntimeException(a + " has a bad source " + this);
			}
			if (!isObject(target(a))) {
				throw new RuntimeException(a + " has a bad target " + this);
			}
		}
		for (O o : objects()) {
			A i = identity(o);
			if (i == null) {
				throw new RuntimeException(o + " has no identity in " + this);
			}
			for (A a : arrows()) {
				if (source(a).equals(o)) {
					if (!a.equals(compose(i, a))) {
						throw new RuntimeException("Identity compose error1\n identity for " + o
								+ " is " + i + " but " + a);
					}
				}
				if (target(a).equals(o)) {
					if (!a.equals(compose(a, i))) {
						throw new RuntimeException("Identity compose error2 " + i + o + a);
					}
				}
			}
		}

		for (A a : arrows()) {
			for (A b : arrows()) {
				if (target(a).equals(source(b))) {
					A c = compose(a, b);
					if (!arrows().contains(c)) {
						throw new RuntimeException("Not closed under composition " + a + b + c
								+ this);
					}
					if (!source(a).equals(source(c))) {
						throw new RuntimeException("Composition type error1 " + a + b + c + this);
					}
					if (!target(b).equals(target(c))) {
						throw new RuntimeException("Composition type error2 " + a + b + c + this);
					}
					for (A cc : arrows()) {
						if (source(cc).equals(target(b))) {
							A xxx = compose(a, compose(b, cc));
							if (xxx == null) {
								throw new RuntimeException("Not closed under composition " + a
										+ " then (" + b + " then " + cc + ")\nthis: " + this);
							}
							if (!xxx.equals(compose(compose(a, b), cc))) {
								throw new RuntimeException("Not associative " + a + b + cc + "\n\n"
										+ xxx + "\n\n" + compose(compose(a, b), cc));
							}
						}
					}
				}
			}
		}
	}

	private final Map<Pair<O, O>, Set<A>> cached = new HashMap<>();

	public Set<A> hom(O A, O B) {
		if (!objects().contains(A)) {
			throw new RuntimeException(A + " not in " + objects() + " (src)");
		}
		if (!objects().contains(B)) {
			throw new RuntimeException(B + " not in " + objects() + " (dst)");
		}
		Pair<O, O> p = new Pair<>(A, B);
		Set<A> retX = cached.get(p);
		if (retX != null) {
			return retX;
		}
		Set<A> ret = new HashSet<>();
		for (A a : arrows()) {
			if (source(a).equals(A) && target(a).equals(B)) {
				ret.add(a);
			}
		}
		cached.put(p, ret);
		return ret;
	}

	private final Map<O, Set<A>> arrowsFrom = new HashMap<>();

	public Set<A> arrowsFrom(O a) {
		if (arrowsFrom.containsKey(a)) {
			return arrowsFrom.get(a);
		}
		Set<A> ret = new HashSet<>();
		for (A arr : arrows()) {
			if (source(arr).equals(a)) {
				ret.add(arr);
			}
		}
		arrowsFrom.put(a, ret);
		return ret;
	}

	private final Map<O, Set<A>> arrowsTo = new HashMap<>();

	public Set<A> arrowsTo(O a) {
		if (arrowsTo.containsKey(a)) {
			return arrowsTo.get(a);
		}
		Set<A> ret = new HashSet<>();
		for (A arr : arrows()) {
			if (target(arr).equals(a)) {
				ret.add(arr);
			}
		}
		arrowsTo.put(a, ret);
		return ret;
	}

	public static <X> Category<X, X> fromSet(Set<X> set) {
		return new Category<>() {

			@Override
			public Set<X> objects() {
				return set;
			}

			@Override
			public Set<X> arrows() {
				return set;
			}

			@Override
			public X source(X a) {
				return a;
			}

			@Override
			public X target(X a) {
				return a;
			}

			@Override
			public X identity(X o) {
				return o;
			}

			@Override
			public X compose(X a1, X a2) {
				if (a1.equals(a2)) {
					return a1;
				}
				throw new RuntimeException("Cannot compose " + a1 + " and " + a2);
			}
		};
	}

	public int compositionSize() {
		int i = 0;
		for (A o1 : arrows()) {
			for (A o2 : arrows()) {
				if (!target(o1).equals(source(o2))) {
					continue;
				}
				i++;
			}
		}
		return i;
	}

	private Signature<O, A> sig;

	public Signature<O, A> toSig() {
		if (sig != null) {
			return sig;
		}
		sig = toSigX();
		return sig;
	}

	public boolean isId(A a) {
		return identity(source(a)).equals(a);
	}

	private Signature<O, A> toSigX() {
		Set<Triple<A, O, O>> a = new HashSet<>();
		Set<Pair<Pair<O, List<A>>, Pair<O, List<A>>>> e = new HashSet<>();

		for (A x : arrows()) {
			if (isId(x)) {
				continue; 			}
			a.add(new Triple<>(x, source(x), target(x)));
		}
		for (A x : arrows()) {
			for (A y : arrows()) {
				if (!target(x).equals(source(y))) {
					continue;
				}
				A z = compose(x, y);
				List<A> l = new LinkedList<>();
				if (!isId(x)) {
					l.add(x);
				}
				if (!isId(y)) {
					l.add(y);
				}
				List<A> r = new LinkedList<>();
				if (!isId(z)) {
					r.add(z);
				}
				Pair<O, List<A>> lhs = new Pair<>(source(x), l);
				Pair<O, List<A>> rhs = new Pair<>(source(x), r);
				Pair<Pair<O, List<A>>, Pair<O, List<A>>> p = new Pair<>(lhs, rhs);
				if (!lhs.equals(rhs) && !e.contains(new Pair<>(rhs, lhs))) {
					e.add(p);
				}
			}
		}

		Signature<O,A> ret = new Signature<>(objects(), a, e);
		return ret;
	}

	private final Map<O, Functor<O, A, Set<A>, Fn<A, A>>> repMap = new HashMap<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Functor<O, A, Set<A>, Fn<A, A>> rep(O c) {
		if (repMap.containsKey(c)) {
			return repMap.get(c);
		}
		Map<O, Set<A>> m1 = new HashMap<>();
		Map<A, Map<A, A>> m2 = new HashMap<>();

		for (O a : objects()) {
			Set<A> p = new HashSet<>();
			for (A arr : hom(c, a)) {
				p.add(arr);
			}
			m1.put(a, p);
		}
		for (A arr : arrows()) {
			Map<A, A> m = new HashMap<>();
			for (A p : m1.get(source(arr))) {
				m.put(p, compose(p, arr));
			}
			m2.put(arr, m);
		}

		Functor<O, A, Set, Fn> f = new Functor<>(this, FinSet.FinSet, m1::get, x -> new Fn<>(
				m1.get(source(x)), m1.get(target(x)), i -> m2.get(x).get(i)));
		repMap.put(c, (Functor<O, A, Set<A>, Fn<A, A>>) ((Object) f));
		return repMap.get(c);
	}

	private Pair<Map<O, Functor<O, A, Set<A>, Fn<A, A>>>, Map<A, Transform<O, A, Set<A>, Fn<A, A>>>> repX;

	Pair<Map<O, Functor<O, A, Set<A>, Fn<A, A>>>, Map<A, Transform<O, A, Set<A>, Fn<A, A>>>> repX() {
		if (repX != null) {
			return repX;
		}
		Map<O, Functor<O, A, Set<A>, Fn<A, A>>> ret = new HashMap<>();
		for (O n : objects()) {
			ret.put(n, rep(n));
		}
		Map<A, Transform<O, A, Set<A>, Fn<A, A>>> ret0 = new HashMap<>();
		for (A e : arrows()) {
			Functor<O, A, Set<A>, Fn<A, A>> s = ret.get(source(e));
			Functor<O, A, Set<A>, Fn<A, A>> t = ret.get(target(e));

			ret0.put(e, new Transform<>(t, s, o -> new Fn<>(t.applyO(o), s.applyO(o),
					id -> compose(e, id))));
		}
		repX = new Pair<>(ret, ret0);
		return repX;
	}

	public List<O> order() {
		List<O> ret = new LinkedList<>(objects());
		Comparator<O> c = Comparator.comparingInt(o -> arrowsFrom(o).size());
		ret.sort(c);
		return ret;
	}

}

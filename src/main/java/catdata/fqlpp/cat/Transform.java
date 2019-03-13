package catdata.fqlpp.cat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Util;
import catdata.fqlpp.FUNCTION;
import catdata.fqlpp.cat.FinSet.Fn;
import catdata.ide.DefunctGlobalOptions;

@SuppressWarnings("serial")
public class Transform<O1, A1, O2, A2> implements Serializable {
	public final Functor<O1, A1, O2, A2> source;
	public final Functor<O1, A1, O2, A2> target;
	private final FUNCTION<O1, A2> t;

	public Transform(Functor<O1, A1, O2, A2> source, Functor<O1, A1, O2, A2> target,
			FUNCTION<O1, A2> t) {
		this.source = source;
		this.target = target; 
		this.t = t;
		validate();
	}

	public A2 apply(O1 o) {
		if (!source.source.isObject(o)) {
			throw new RuntimeException("Cannot apply " + this + " to " + o
					+ " as it is not in " + source.source);
		}
		A2 ret = t.apply(o);
		if (!source.target.isArrow(ret)) {
			throw new RuntimeException("Applying " + this + " to " + o
					+ " yields " + ret + " which is not in "
					+ source.target);
		}
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Transform)) {
			return false;
		}
		Transform other = (Transform) o;
		if (!source.equals(other.source)) {
			return false;
		}
		if (!target.equals(other.target)) {
			return false;
		}
		for (O1 k : source.source.objects()) {
			if (!t.apply(k).equals(other.t.apply(k))) {
				return false;
			}
		}
		return true;
	}

	public String toStringLong() {
		try {
			String l = source.toString();
			String r = target.toString();
			String z1 = Util.sep(
					source.source.objects().stream()
							.map(x -> new Pair<>(x, apply(x)).toString())
							.iterator(), ", ");
			String a1 = "[" + z1 + "]\n";
			return a1 + "  :\n" + l + "\n  ->\n" + r;
		} catch (Exception e) {
			return "(Cannot print)";
		}
	}

	@Override
	public String toString() {
		try {
			String z1 = Util.sep(
					source.source.objects().stream()
							.map(x -> new Pair<>(x, apply(x)).toString())
							.iterator(), ", ");
			return "[" + z1 + "]\n";
		} catch (Exception e) {
			return "(Cannot print)";
		}
	}

	@Override
	public int hashCode() {
		return 0;
	}

	 private void validate() {
		if (!DefunctGlobalOptions.debug.fqlpp.VALIDATE) {
			return;
		}
		if (source == null) {
			throw new RuntimeException("Null source functor in " + this);
		}
		if (target == null) {
			throw new RuntimeException("Null target functor in " + this);
		}
		if (t == null) {
			throw new RuntimeException("Null function in " + this);
		}
		Category<O1, A1> s1 = source.source;
		Category<O2, A2> t1 = source.target;
		Category<O1, A1> s2 = target.source;
		Category<O2, A2> t2 = target.target;
		Functor<O1, A1, O2, A2> F = source;
		Functor<O1, A1, O2, A2> G = target;
		if (!s1.equals(s2)) {
			throw new RuntimeException("Source categories do not match");
		}
		if (!t1.equals(t2)) {
			throw new RuntimeException("Target categories do not match");
		}
		if (s1.isInfinite()) {
			return;
		}
		for (O1 n : s1.objects()) {
			A2 v = apply(n);
			if (!t1.isArrow(v)) {
				throw new RuntimeException("Target does not contain " + v);
			}
			if (!t1.source(v).equals(source.applyO(n))) {
				throw new RuntimeException(n + " maps to " + v + " with source " + t1.source(v) + " instead of " + source.applyO(n));
			}
			if (!t1.target(v).equals(target.applyO(n))) {
				throw new RuntimeException(n + " maps to " + v + " with target " + t1.target(v) + " instead of " + target.applyO(n));
			}
		}
		for (A1 a : s1.arrows()) {
		//	try {

				O1 x = s1.source(a);
				O1 y = s1.target(a);
				Object lhs = t1.compose(apply(x), G.applyA(a));
				Object rhs = t1.compose(F.applyA(a), apply(y));

				if (!lhs.equals(rhs)) {
					throw new RuntimeException("Not respected on " + a
							+ " in " + this + "lhs: " +lhs + ", rhs: " + rhs);
				}
			/* } catch (Exception eee) {
				eee.printStackTrace();
				throw new RuntimeException(
						"Could not validate naturality condition on source arrow "
								+ a + ", error was: "
								+ eee.getLocalizedMessage());
			} */
		}

	}

	public static <O1, A1, O2, A2> Transform<O1, A1, O2, A2> compose(
			Transform<O1, A1, O2, A2> f, Transform<O1, A1, O2, A2> g) {
		if (!f.target.equals(g.source) && !f.target.source.isInfinite()) {
			throw new RuntimeException("Dom/Cod mismatch when composing "
					+ f + " and " + g);
		}
		return new Transform<>(f.source, g.target, o -> f.target.target.compose(
				f.apply(o), g.apply(o)));
	}

	public static <O1, A1, O2, A2> Transform<O1, A1, O2, A2> id(
			Functor<O1, A1, O2, A2> k) {
		return new Transform<>(k, k, o -> k.target.identity(k.applyO(o)));
	}
	
	private FPTransform<O1,A1> fptrans;
	public  FPTransform<O1,A1> toFPTransform() {
		if (fptrans != null) {
			return fptrans;
		}
		fptrans = toFPTransX();
		return fptrans;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private FPTransform<O1,A1> toFPTransX() {
		if (source.source.isInfinite() || !source.target.equals(FinSet.FinSet)) {
			throw new RuntimeException("Cannot create FP transform from " + this);
		}
	
		Map<Signature<O1,A1>.Node, Map<Object,Object>> data = new HashMap<>();
		for (Signature<O1,A1>.Node n: source.source.toSig().nodes) {
			Fn f = (Fn) t.apply(n.name);
			data.put(n, Util.reify(f::apply, f.source));
		}
		
		return new FPTransform<>(source.toInstance(), target.toInstance(), data);
	}
	
	public static <CO,CA,DO,DA,EO,EA> Transform<CO,CA,EO,EA> leftWhisker(Functor<CO,CA,DO,DA> t, Transform<DO,DA,EO,EA> e) {
		Functor<DO, DA, EO, EA> A = e.source;
		Functor<DO, DA, EO, EA> B = e.target;
		
		return new Transform<>(Functor.compose(t, A), Functor.compose(t, B), c -> e.apply(t.applyO(c)));
	}
	
	public static <CO,CA,DO,DA,EO,EA> Transform<CO,CA,EO,EA> rightWhisker(Functor<DO,DA,EO,EA> t, Transform<CO,CA,DO,DA> e) {
		Functor<CO, CA, DO, DA> A = e.source;
		Functor<CO, CA, DO, DA> B = e.target;

		return new Transform<>(Functor.compose(A, t), Functor.compose(B, t), c -> t.applyA(e.apply(c)));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <O,A> Functor<O,A,Set,Fn> preimage(Transform<O,A,Set,Fn> t, Functor<O,A,Set,Fn> a) {
		if (t.source.source.isInfinite() || !t.source.target.equals(FinSet.FinSet)) {
			throw new RuntimeException();
		}

		Map<O, Set> map1 = new HashMap<>();
		Map<A, Fn> map2 = new HashMap<>();
		
		for (O n : t.source.source.objects()) {
			Set<Object> kx = new HashSet<>();
			for (Object i : t.source.applyO(n)) {
				Object v = t.apply(n).apply(i);
				if (a.applyO(n).contains(v)) {
					kx.add(i);
				}
			}
			map1.put(n, kx);
		}

		for (A n : t.source.source.arrows()) {
			map2.put(n, new Fn<>(map1.get(t.source.source.source(n)), map1.get(t.source.source.target(n)), x -> t.source.applyA(n).apply(x)));
		}

		return new Functor<>(a.source, FinSet.FinSet, map1::get, map2::get);		
	}
	

}
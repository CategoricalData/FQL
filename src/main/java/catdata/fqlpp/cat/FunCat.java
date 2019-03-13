package catdata.fqlpp.cat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.fqlpp.FUNCTION;

@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class FunCat<O, A> extends Category<Functor<O, A, Category, Functor>, Transform<O, A, Category, Functor>> {

	@Override
	public boolean equals(Object o) {
        return o instanceof Inst && ((Inst<?, ?>) o).cat.equals(cat);
    }

    @Override
	public int hashCode() {
		return 0;
	}

	private static final Map map = new HashMap<>();
	public static <O,A> FunCat<O,A> get(Category<O,A> cat) {
		if (map.containsKey(cat)) {
			return (FunCat<O, A>) map.get(cat);
		}
		map.put(cat, new FunCat(cat));
		return (FunCat<O, A>) map.get(cat);
	}
	
	private FunCat(Category<O, A> cat) {
		if (cat.isInfinite()) {
			throw new RuntimeException("Cannot construct Cat^C for infinite C, given " + cat);
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
		return "(Cat ^ " + cat + ")";
	}

	@Override
	public void validate() {
	}

	@Override
	public Set<Functor<O, A, Category, Functor>> objects() {
		throw new RuntimeException("Cannot enumerate objects of " + this);
	}

	@Override
	public Set<Transform<O, A, Category, Functor>> arrows() {
		throw new RuntimeException("Cannot enumerate arrows of " + this);
	}

	@Override
	public Functor<O, A, Category, Functor> source(Transform<O, A, Category, Functor> a) {
		return a.source;
	}

	@Override
	public Functor<O, A, Category, Functor> target(Transform<O, A, Category, Functor> a) {
		return a.target;
	}

	@Override
	public Transform<O, A, Category, Functor> identity(Functor<O, A, Category, Functor> o) {
		return Transform.id(o);
	}

	@Override
	public Transform<O, A, Category, Functor> compose(Transform<O, A, Category, Functor> f, Transform<O, A, Category, Functor> g) {
		return Transform.compose(f, g);
	}

	public Functor<O, A, Category, Functor> terminal() {
		return new Functor(cat, FinCat.FinCat, x -> FinCat.terminal(), x -> FinCat.terminal(FinCat.terminal()));
	}

	public Transform<O, A, Category, Functor> terminal(Functor<O, A, Category, Functor> o) {
		FUNCTION<O, Functor> fn = x -> new Functor(o.applyO(x), FinCat.terminal(), y -> Unit.unit, y -> Unit.unit);
		return new Transform(o, terminal(), fn);
	}
	
	public Functor<O, A, Category, Functor> initial() {
		return new Functor(cat, FinCat.FinCat, x -> FinCat.initial(), x -> FinCat.initial(FinCat.initial()));
	}
		 
	public Transform<O, A, Category, Functor> initial(Functor<O, A, Category, Functor> o) {
		FUNCTION<O, Functor> fn = x -> new Functor<>(FinCat.initial(), o.applyO(x), y -> { throw new RuntimeException(); }, y -> { throw new RuntimeException(); });
		return new Transform(initial(), o, fn);
	}

	public Functor<O, A, Category, Functor> product(Functor<O, A, Category, Functor> f, Functor<O, A, Category, Functor> g) {
		FUNCTION<O, Category> h = x -> FinCat.product(f.applyO(x), g.applyO(x));
		FUNCTION<A, Functor> i = x -> FinCat.pairF(f.applyA(x), g.applyA(x));
		return new Functor(cat, FinCat.FinCat, h, i);
	}
	
	public Functor<O, A, Category, Functor> coproduct(Functor<O, A, Category, Functor> f, Functor<O, A, Category, Functor> g) {
		FUNCTION<O, Category> h = x -> FinCat.coproduct(f.applyO(x), g.applyO(x));
		FUNCTION<A, Functor> i = x -> FinCat.matchF(f.applyA(x), g.applyA(x));
		return new Functor(cat, FinCat.FinCat, h, i);
	}
	
	public Transform<O, A, Category, Functor> first(Functor<O, A, Category, Functor> o1, Functor<O, A, Category, Functor> o2) {
		FUNCTION<O, Functor> f = o -> new Functor(FinCat.product(o1.applyO(o),
				o2.applyO(o)), o1.applyO(o), x -> ((Pair)x).first, x -> ((Pair)x).first);
		return new Transform(product(o1, o2), o1, f);
	}
	public Transform<O, A, Category, Functor> second(Functor<O, A, Category, Functor> o1, Functor<O, A, Category, Functor> o2) {
		FUNCTION<O, Functor> f = o -> new Functor(FinCat.product(o1.applyO(o),
				o2.applyO(o)), o2.applyO(o), x -> ((Pair)x).second, x -> ((Pair)x).second);
		return new Transform(product(o1, o2), o2, f);
	}
	
	public Transform<O, A, Category, Functor> inleft(Functor<O, A, Category, Functor> o1, Functor<O, A, Category, Functor> o2) {
		FUNCTION<O, Functor> f = o -> new Functor(o1.applyO(o), FinCat.coproduct(o1.applyO(o),
				o2.applyO(o)), Chc::inLeft, Chc::inLeft);
		return new Transform(o1, coproduct(o1, o2), f);
	}

	public Transform<O, A, Category, Functor> inright(Functor<O, A, Category, Functor> o1, Functor<O, A, Category, Functor> o2) {
		FUNCTION<O, Functor> f = o -> new Functor(o2.applyO(o), FinCat.coproduct(o1.applyO(o),
				o2.applyO(o)), Chc::inRight, Chc::inRight);
		return new Transform(o2, coproduct(o1, o2), f);
	}
	
	public Transform<O, A, Category, Functor> pair(Transform<O, A, Category, Functor> f, Transform<O, A, Category, Functor> g) {
		if (!f.source.equals(g.source)) {
			throw new RuntimeException();
		} 
		FUNCTION<O, Functor> fn = o -> 
		  new Functor<>(f.source.applyO(o), FinCat.product(f.target.applyO(o), g.target.applyO(o)), 
				  x -> new Pair<>(f.apply(o).applyO(x), g.apply(o).applyO(x)), 
				  x -> new Pair<>(f.apply(o).applyA(x), g.apply(o).applyA(x)));
		return new Transform(f.source, product(f.target, g.target), fn);
	}
	public Transform<O, A, Category, Functor> match(Transform<O, A, Category, Functor> f, Transform<O, A, Category, Functor> g) {
		if (!f.target.equals(g.target)) {
			throw new RuntimeException();
		}
		FUNCTION<O, Functor> fn = o -> new Functor<>(FinCat.coproduct(f.source.applyO(o), g.source.applyO(o)), 
				f.target.applyO(o), 
				x -> ((Chc)x).left ? f.apply(o).applyO(((Chc)x).l) : g.apply(o).applyO(((Chc)x).r),
				x -> ((Chc)x).left ? f.apply(o).applyA(((Chc)x).l) : g.apply(o).applyA(((Chc)x).r));
		return new Transform(coproduct(f.source, g.source), f.target, fn);
	}

	
}

package catdata.fqlpp.cat;

import java.util.HashSet;
import java.util.Set;

import catdata.Pair;

public class Monad<O,A> {
	
	private final Functor<O,A,O,A> F;    //F : C -> C
	private final Transform<O,A,O,A> unit; //unit: Id_C => F
	private final Transform<O,A,O,A> join; //join: F;F => F
	
	public Monad(Functor<O, A, O, A> F, Transform<O, A, O, A> unit, Transform<O, A, O, A> join) {
		this.F = F;
		this.unit = unit;
		this.join = join;
		validate();
	}
	
	@SuppressWarnings("serial")
	public Category<O,Pair<A,O>> kleisli() {
		return new Category<>() {		

			@Override
			public boolean isInfinite() {
				return F.source.isInfinite();
			}
			
			@Override
			public boolean isObject(O o) {
				return F.source.isObject(o);
			}
			
			@Override
			public boolean isArrow(Pair<A,O> a) {
				return F.source.isArrow(a.first) && F.source.target(a.first).equals(F.applyO(a.second));
			}

			@Override
			public Set<O> objects() {
				return F.source.objects();
			}
			
			@Override
			public Set<Pair<A,O>> hom(O s, O t) {
				Set<Pair<A,O>> ret = new HashSet<>();
				for (A a : F.source.hom(s, F.applyO(t))) {
					ret.add(new Pair<>(a, t));
				}
				return ret;
			}

			@Override
			public Set<Pair<A, O>> arrows() {
				Set<Pair<A,O>> ret = new HashSet<>();
				for (O o1 : objects()) {
					for (O o2 : objects()) {
						ret.addAll(hom(o1,o2));
					}
				}
				return ret;
			}

			@Override
			public O source(Pair<A, O> a) {
				return F.source.source(a.first);
			}

			@Override
			public O target(Pair<A, O> a) {
				return a.second;
			}

			@Override
			public Pair<A, O> identity(O o) {
				return new Pair<>(unit.apply(o), o);
			}

			@Override
			public Pair<A, O> compose(Pair<A, O> f, Pair<A, O> g) {
				A fg = F.source.compose(f.first, F.applyA(g.first));
				A x = F.source.compose(fg, join.apply(g.second));
				return new Pair<>(x, g.second);
			}
			
		};
	}
	
	
	
	private void validate() {
		Category<O,A> C = F.source;
		if (C.isInfinite()) {
			return;
		}
		if (!C.equals(F.target)) {
			throw new RuntimeException(F + " is not an endofunctor.");
		}
		Functor<O,A,O,A> idC = Functor.identity(C);
		if (!unit.source.equals(idC)) {
			throw new RuntimeException(unit + " domain is not identity.");
		}
		if (!unit.target.equals(F)) {
			throw new RuntimeException(unit + " codomain is not " + F);
		}
		Functor<O,A,O,A> FF = Functor.compose(F, F);
		if (!join.source.equals(F)) {
			throw new RuntimeException(join + " codomain is not " + F);			
		}
		if (!join.target.equals(FF)) {
			throw new RuntimeException(join + " domain is not squared " + F);
		}
		for (O X : C.objects()) {
			A lhs1 = C.compose(join.apply(F.applyO(X)), join.apply(X));
			A rhs1 = C.compose(F.applyA(join.apply(X)), join.apply(X));
			if (!lhs1.equals(rhs1)) {
				throw new RuntimeException(this + " does not validate on " + X + "\n(1)");
			}
			if (!C.compose(F.applyA(unit.apply(X)), join.apply(X)).equals(C.identity(F.applyO(X)))){
				throw new RuntimeException(this + " does not validate on " + X + "\n(2)");
			}
			if (!C.compose(unit.apply(F.applyO(X)), join.apply(X)).equals(C.identity(F.applyO(X)))){
				throw new RuntimeException(this + " does not validate on " + X + "\n(3)");
			}
		}
	}
	
}

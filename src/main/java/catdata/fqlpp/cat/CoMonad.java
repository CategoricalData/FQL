package catdata.fqlpp.cat;

import java.util.HashSet;
import java.util.Set;

import catdata.Pair;

public class CoMonad<O,A> {
	
	private final Functor<O,A,O,A> F;    //F : C -> C
	private final Transform<O,A,O,A> counit; //unit: F => Id_C
	private final Transform<O,A,O,A> cojoin; //join: F => F;F
	
	public CoMonad(Functor<O, A, O, A> F, Transform<O, A, O, A> unit, Transform<O, A, O, A> join) {
		this.F = F;
        counit = unit;
        cojoin = join;
		validate();
	}
	
	@SuppressWarnings("serial")
	public Category<O,Pair<A,O>> cokleisli() {
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
				return F.source.isArrow(a.first) && F.source.source(a.first).equals(F.applyO(a.second));
			}

			@Override
			public Set<O> objects() {
				return F.source.objects();
			}
			
			@Override
			public Set<Pair<A,O>> hom(O s, O t) {
				Set<Pair<A,O>> ret = new HashSet<>();
				for (A a : F.source.hom(F.applyO(s), t)) {
					ret.add(new Pair<>(a, s));
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
				return a.second;
			}

			@Override
			public O target(Pair<A, O> a) {
				return F.source.target(a.first);
			}

			@Override
			public Pair<A, O> identity(O o) {
				return new Pair<>(counit.apply(o), o);
			}

			@Override
			public Pair<A, O> compose(Pair<A, O> f, Pair<A, O> g) {
				A fg = F.source.compose(F.applyA(f.first), g.first);
				A x = F.source.compose(cojoin.apply(f.second), fg);
				return new Pair<>(x, f.second);
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
		if (!counit.target.equals(idC)) {
			throw new RuntimeException(counit + " codomain is not identity.");
		}
		if (!counit.source.equals(F)) {
			throw new RuntimeException(counit + " domain is not identity.");
		}
		Functor<O,A,O,A> FF = Functor.compose(F, F);
		if (!cojoin.source.equals(F)) {
			throw new RuntimeException(cojoin + " domain is not " + F);			
		}
		if (!cojoin.target.equals(FF)) {
			throw new RuntimeException(cojoin + " codomain is not squared " + F);
		}
		for (O X : C.objects()) {
			A lhs1 = C.compose(cojoin.apply(X), cojoin.apply(F.applyO(X)));
			A rhs1 = C.compose(cojoin.apply(X), F.applyA(cojoin.apply(X)));
			if (!lhs1.equals(rhs1)) {
				throw new RuntimeException(this + " does not validate on " + X + "\n(1)");
			}
			if (!C.compose(cojoin.apply(X), F.applyA(counit.apply(X))).equals(C.identity(F.applyO(X)))){
				throw new RuntimeException(this + " does not validate on " + X + "\n(2)");
			}
			if (!C.compose(cojoin.apply(X), counit.apply(F.applyO(X))).equals(C.identity(F.applyO(X)))){
				throw new RuntimeException(this + " does not validate on " + X + "\n(3)");
			}
		}
	}
	
}

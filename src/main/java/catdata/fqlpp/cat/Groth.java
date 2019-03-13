package catdata.fqlpp.cat;

import java.util.HashSet;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.fqlpp.cat.FinSet.Fn;

public class Groth {

	/*
    Objects are pairs (A,a) where A \in \mathop{\rm Ob}(C) and a \in FA.
    An arrow (A,a) \to (B,b) is an arrow f: A \to B in C such that (Ff)a = b.
	 */
	
	/*Given a functor F:C-->D, define 
depivot F = SIGMA F unit,
where unit is the terminal instance on C.*/
	
	@SuppressWarnings("rawtypes")
	public static <O1,A1,O2,A2> Functor <O2,A2,Set,Fn> unpivot(Functor<O1,A1,O2,A2> F) {
		return LeftKanSigma.fullSigma(F, Inst.get(F.source).terminal(), null, null).first;
	}
	
	public static <O,A,X> Functor<Pair<O,X>,Triple<Pair<O,X>,Pair<O,X>,A>,O,A> pivot(Functor<O,A,Set<X>,Fn<X,X>> F) {
		Category<Pair<O,X>,Triple<Pair<O,X>,Pair<O,X>,A>> C = pivotX(F);
		return new Functor<>(C, F.source, x -> x.first, x -> x.third);
	}
	
	@SuppressWarnings("serial")
    private static <O,A,X> Category<Pair<O,X>,Triple<Pair<O,X>,Pair<O,X>,A>> pivotX(Functor<O, A, Set<X>, Fn<X, X>> F) {
		
		Set<Pair<O, X>> objects = new HashSet<>();
		Set<Triple<Pair<O,X>, Pair<O,X>, A>> arrows = new HashSet<>();
		
		for (O o : F.source.objects()) {
			for (X x : F.applyO(o)) {
				objects.add(new Pair<>(o,x));
			}
		}
		for (Pair<O, X> s : objects) {
			for (Pair<O, X> t : objects) {
				for (A f : F.source.hom(s.first, t.first)) {
					X rhs = F.applyA(f).apply(s.second);
					X lhs = t.second;
					if (rhs.equals(lhs)) {
						arrows.add(new Triple<>(s, t, f));
					}
				}
			}	
		}
		
		return new Category<>() {

			@Override
			public Set<Pair<O, X>> objects() {
				return objects;
			}

			@Override
			public Set<Triple<Pair<O, X>, Pair<O, X>, A>> arrows() {
				return arrows;
			}

			@Override
			public Pair<O, X> source(Triple<Pair<O, X>, Pair<O, X>, A> a) {
				return a.first;
			}

			@Override
			public Pair<O, X> target(Triple<Pair<O, X>, Pair<O, X>, A> a) {
				return a.second;
			}

			@Override
			public Triple<Pair<O, X>, Pair<O, X>, A> identity(Pair<O, X> o) {
				return new Triple<>(o, o, F.source.identity(o.first));
			}

			@Override
			public Triple<Pair<O, X>, Pair<O, X>, A> compose(Triple<Pair<O, X>, Pair<O, X>, A> a1,
					Triple<Pair<O, X>, Pair<O, X>, A> a2) {
				if (!target(a1).equals(source(a2))) {
					throw new RuntimeException("Dom/Cod mismatch on (groth): " + a1 + " and " + a2);
				}
				return new Triple<>(source(a1), target(a2), F.source.compose(a1.third, a2.third));
			}

			
		};
				
	}
	
}

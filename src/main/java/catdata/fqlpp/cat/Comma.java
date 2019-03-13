package catdata.fqlpp.cat;

import java.util.HashSet;
import java.util.Set;

import catdata.Quad;
import catdata.Triple;

// in future, don't have arrows(); only have arrows(node, node)
class Comma {

	public static <OA, AA, OB, AB, OC, AC> Triple<
	Category<Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>> ,
	Functor< Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>, OA, AA> ,
	Functor< Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>, OB, AB>> 
	comma(Category<OA, AA> A, Category<OB, AB> B, Category<OC, AC> C, 
		   Functor<OA, AA, OC, AC> F, Functor<OB, AB, OC, AC> G) {
		
		@SuppressWarnings("serial")
		Category<Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>> cat = new Category<> () {

			Set<Triple<OA, OB, AC>> objects;
			@Override
			public Set<Triple<OA, OB, AC>> objects() {
				if (objects != null) {
					return objects;
				}
				objects = new HashSet<>();
				for (OA OA : A.objects()) {
					for (OB OB : B.objects()) {
						OC s = F.applyO(OA);
						OC t = G.applyO(OB);
						for (AC AC : C.arrows()) {
							if (C.source(AC).equals(s) && C.target(AC).equals(t)) {
								objects.add(new Triple<>(OA, OB, AC));
							}
						}
					}
				}
				return objects;
			}

			Set<Quad<AA, AB, AC, AC>> arrows;
			@Override
			public Set<Quad<AA, AB, AC, AC>> arrows() {
				if (arrows != null) {
					return arrows;
				}
				arrows = new HashSet<>();
				for (Triple<OA, OB, AC> obj1 : objects) {
					for (Triple<OA, OB, AC> obj2 : objects) {
						OA a1 = obj1.first;
						OB b1 = obj1.second;
						AC c1 = obj1.third;
						OA a2 = obj2.first;
						OB b2 = obj2.second;
						AC c2 = obj2.third;
						for (AA m : A.arrows()) {
							if (!(A.source(m).equals(a1) && A.target(m).equals(a2))) {
								continue;
							}
							for (AB n : B.arrows()) {
								if (!(B.source(n).equals(b1) && B.target(n).equals(b2))) {
									continue;
								}
								AC lhs = C.compose(F.applyA(m), c2);
								AC rhs = C.compose(c1,G.applyA(n));
								if (lhs.equals(rhs)) {
									Quad<AA, AB, AC, AC> arr = new Quad<>(m, n, c1, c2);
									arrows.add(arr);
								}
							}
						}
					}
				}
				return arrows;
			}

			@Override
			public Triple<OA, OB, AC> source(Quad<AA, AB, AC, AC> a) {
				return new Triple<>(A.source(a.first), B.source(a.second), a.third);
			}

			@Override
			public Triple<OA, OB, AC> target(Quad<AA, AB, AC, AC> a) {
				return new Triple<>(A.target(a.first), B.target(a.second), a.fourth);
			}

			@Override
			public Quad<AA, AB, AC, AC> identity(Triple<OA, OB, AC> o) {
				return new Quad<>(A.identity(o.first), B.identity(o.second), o.third, o.third);
			}

			@Override
			public Quad<AA, AB, AC, AC> compose(Quad<AA, AB, AC, AC> a1, Quad<AA, AB, AC, AC> a2) {
//				return new Quad<>(A.compose(a1.first, a1.first), B.compose(a1.second, a2.second), C.compose(a1.third, a2.third), C.compose(a1.fourth, a2.fourth));
				return new Quad<>(A.compose(a1.first, a2.first), B.compose(a1.second, a2.second), a1.third, a2.fourth);
			}
			
		};
		
		Functor<Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>, OA, AA> projA 
		 = new Functor<>(cat, A, o -> o.first, a -> a.first);
		
		Functor<Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>, OB, AB> projB 
		 = new Functor<>(cat, B, o -> o.second, a -> a.second);
		
		return new Triple<>(cat, projA, projB);
		
	}

}

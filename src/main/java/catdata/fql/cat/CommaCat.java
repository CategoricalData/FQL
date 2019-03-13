package catdata.fql.cat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import catdata.Pair;
import catdata.Triple;
import catdata.ide.DefunctGlobalOptions;

/**
 * 
 * @author ryan
 * 
 *         Implementation of comma categories.
 * 
 * @param <ObjA>
 *            Left
 * @param <ArrowA>
 *            Left
 * @param <ObjB>
 *            Right
 * @param <ArrowB>
 *            Right
 * @param <ObjC>
 *            Middle
 * @param <ArrowC>
 *            Middle
 */
public class CommaCat<ObjA, ArrowA, ObjB, ArrowB, ObjC, ArrowC>
		extends
		FinCat<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>> {

	private final FinCat<ObjA, ArrowA> A;
	private final FinCat<ObjB, ArrowB> B;
	@SuppressWarnings("unused")
	private final FinCat<ObjC, ArrowC> C;
	@SuppressWarnings("unused")
	private final FinFunctor<ObjA, ArrowA, ObjC, ArrowC> F;
	@SuppressWarnings("unused")
	private final FinFunctor<ObjB, ArrowB, ObjC, ArrowC> G;
	public FinFunctor<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>, ObjA, ArrowA> projA;
	public FinFunctor<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>, ObjB, ArrowB> projB;

	public CommaCat(FinCat<ObjA, ArrowA> A, FinCat<ObjB, ArrowB> B,
			FinCat<ObjC, ArrowC> C, FinFunctor<ObjA, ArrowA, ObjC, ArrowC> F,
			FinFunctor<ObjB, ArrowB, ObjC, ArrowC> G) {
		this.A = A;
		this.B = B;
		this.C = C;
		this.F = F;
		this.G = G;

		objects = new LinkedList<>();
		for (ObjA objA : A.objects) {
			for (ObjB objB : B.objects) {
				ObjC s = F.objMapping.get(objA);
				ObjC t = G.objMapping.get(objB);
				for (Arr<ObjC, ArrowC> arrowC : C.arrows) {
					if (arrowC.src.equals(s) && arrowC.dst.equals(t)) {
						objects.add(new Triple<>(objA, objB, arrowC));
					}
				}
			}
		}

		for (Triple<ObjA, ObjB, Arr<ObjC, ArrowC>> obj1 : objects) {
			for (Triple<ObjA, ObjB, Arr<ObjC, ArrowC>> obj2 : objects) {
				ObjA a1 = obj1.first;
				ObjB b1 = obj1.second;
				Arr<ObjC, ArrowC> c1 = obj1.third;
				ObjA a2 = obj2.first;
				ObjB b2 = obj2.second;
				Arr<ObjC, ArrowC> c2 = obj2.third;
				for (Arr<ObjA, ArrowA> m : A.arrows) {
					if (!(m.src.equals(a1) && m.dst.equals(a2))) {
						continue;
					}
					for (Arr<ObjB, ArrowB> n : B.arrows) {
						if (!(n.src.equals(b1) && n.dst.equals(b2))) {
							continue;
						}
			
						// note composition is backwards - is ; not o
						Arr<ObjC, ArrowC> lhs = C.compose(
								F.arrowMapping.get(m), c2);
						assert (lhs != null);
							Arr<ObjC, ArrowC> rhs = C.compose(c1,
								G.arrowMapping.get(n));

						assert (rhs != null);
						if (lhs.equals(rhs)) {
							Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>> arr = new Pair<>(
									m, n);
							arrows.add(new Arr<>(arr, obj1, obj2));
						}
					}
				}
			}
		}
		for (Triple<ObjA, ObjB, Arr<ObjC, ArrowC>> obj : objects) {
			identities.put(obj,
					new Arr<>(new Pair<>(A.identities.get(obj.first),
							B.identities.get(obj.second)), obj, obj));
		}

		for (Arr<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>> arrow1 : arrows) {
			for (Arr<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>> arrow2 : arrows) {
				if (arrow1.dst.equals(arrow2.src)) {
					Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>> c = new Pair<>(
							A.compose(arrow1.arr.first, arrow2.arr.first),
							B.compose(arrow1.arr.second, arrow2.arr.second));
					composition.put(new Pair<>(arrow1, arrow2), new Arr<>(c,
							arrow1.src, arrow2.dst));
				}
			}
		}

		projA();
		projB();

		if (DefunctGlobalOptions.debug.fql.VALIDATE) {
			validate();
		}
	}

	private void projA() {
		Map<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, ObjA> objMapping = new HashMap<>();
		Map<Arr<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>>, Arr<ObjA, ArrowA>> arrowMapping = new HashMap<>();

		for (Triple<ObjA, ObjB, Arr<ObjC, ArrowC>> obj : objects) {
			objMapping.put(obj, obj.first);
		}
		for (Arr<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>> arr : arrows) {
			arrowMapping.put(arr, arr.arr.first);
		}

		projA = new FinFunctor<>(objMapping, arrowMapping, this, A);
	}

	private void projB() {
		Map<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, ObjB> objMapping = new HashMap<>();
		Map<Arr<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>>, Arr<ObjB, ArrowB>> arrowMapping = new HashMap<>();

		for (Triple<ObjA, ObjB, Arr<ObjC, ArrowC>> obj : objects) {
			objMapping.put(obj, obj.second);
		}
		for (Arr<Triple<ObjA, ObjB, Arr<ObjC, ArrowC>>, Pair<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>>> arr : arrows) {
			arrowMapping.put(arr, arr.arr.second);
		}

		projB = new FinFunctor<>(objMapping, arrowMapping, this, B);
	}

}

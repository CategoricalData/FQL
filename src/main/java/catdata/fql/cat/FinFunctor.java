package catdata.fql.cat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.FQLException;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Type;
import catdata.ide.DefunctGlobalOptions;

/**
 * 
 * @author ryan
 * 
 *         Implementation of finite functors
 * @param <ObjA>
 *            source objects
 * @param <ArrowA>
 *            source arrows
 * @param <ObjB>
 *            target objects
 * @param <ArrowB>
 *            target arrows
 */
public class FinFunctor<ObjA, ArrowA, ObjB, ArrowB> {
	
	public Map<Attribute<ObjA>, Attribute<ObjB>> am;

	/**
	 * Apply to an object.
	 */
	public ObjB applyO(ObjA a) {
		return objMapping.get(a);
	}

	/**
	 * Apply to an arrow.
	 */
	public Arr<ObjB, ArrowB> applyA(Arr<ObjA, ArrowA> a) {
		return arrowMapping.get(a);
	}

	public final Map<ObjA, ObjB> objMapping;
	public final Map<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>> arrowMapping;
	public final FinCat<ObjA, ArrowA> srcCat;
	public final FinCat<ObjB, ArrowB> dstCat;

	/**
	 * Constructs a functor. Does not copy inputs.
	 */
	public FinFunctor(Map<ObjA, ObjB> objMapping,
			Map<Arr<ObjA, ArrowA>, Arr<ObjB, ArrowB>> arrowMapping,
			FinCat<ObjA, ArrowA> srcCat, FinCat<ObjB, ArrowB> dstCat) {
		this.objMapping = objMapping;
		this.arrowMapping = arrowMapping;
		this.srcCat = srcCat;
		this.dstCat = dstCat;
		if (DefunctGlobalOptions.debug.fql.VALIDATE) {
			validate();
		}
	}

	// sort data in instances, transforms
	
	/**
	 * Constructs a singleton functor
	 */
	public static <Obj, Arrow> FinFunctor<Obj, Arrow, Obj, Arrow> singleton(
			FinCat<Obj, Arrow> D, Obj d, Arr<Obj, Arrow> i) {
		FinCat<Obj, Arrow> C = new FinCat<>(d, i);
		Map<Obj, Obj> objMapping = new HashMap<>();
		Map<Arr<Obj, Arrow>, Arr<Obj, Arrow>> arrowMapping = new HashMap<>();
		objMapping.put(d, d);
		arrowMapping.put(i, i);

		return new FinFunctor<>(objMapping, arrowMapping, C, D);
	}

	private void validate() {
		for (ObjA o : srcCat.objects) {
			if (!objMapping.containsKey(o)) {
				throw new RuntimeException("Functor does not map " + o + this);
			}
			if (!dstCat.objects.contains(objMapping.get(o))) {
				if (!dstCat.arrows.contains(arrowMapping.get(o))) {
					throw new RuntimeException("Functor maps to bad object "
							+ o + arrowMapping.get(o) + this);
				}

			}
		}
		for (Arr<ObjA, ArrowA> a : srcCat.arrows) {
			if (!arrowMapping.containsKey(a)) {
				throw new RuntimeException("Functor does not map " + a + this);
			}
			if (!dstCat.arrows.contains(arrowMapping.get(a))) {
				throw new RuntimeException("Functor maps to bad arrow " + a
						+ "\n\n" + this + "\n\n dstCat " + dstCat);
			}
			for (Arr<ObjA, ArrowA> b : srcCat.arrows) {
				if (!a.dst.equals(b.src)) {
					continue;
				}
				Arr<ObjA, ArrowA> c = srcCat.compose(a, b);
				//if (c == null) {
				//	continue;
				//}
				Arr<ObjB, ArrowB> a0 = arrowMapping.get(a);
				Arr<ObjB, ArrowB> b0 = arrowMapping.get(b);
				Arr<ObjB, ArrowB> c0 = arrowMapping.get(c);
				if (!c0.equals(dstCat.compose(a0, b0))) {
					throw new RuntimeException("Func does not preserve:\na is "
							+ a + "\nb is " + b + "\nc is " + c + "\na0 is "
							+ a0 + "\nb0 is " + b0 + "\nc0 is " + c0
							+ "\ncomp is " + dstCat.compose(a0, b0)
							+ "\nsrcCat:\n" + srcCat + "\ndstcat:\n" + dstCat);
				}
			}
		}
		
	}

	@Override
	public String toString() {
		return "FinFunctor [objMapping=\n" + objMapping + "\n\narrowMapping=\n"
				+ arrowMapping + "\n]";
	}

	@Override
	public boolean equals(Object obj) {
		throw new RuntimeException("Cannot equate functors");
	}

	/**
	 * Converts a functor to a mapping by converting the source and target
	 * categories
	 * 
	 * @return the mapping, and a bunch of isomorphisms
	 * @throws FQLException
	 */
	public Triple<Mapping, Quad<Signature, Pair<Map<ObjA, String>, Map<String, ObjA>>, Pair<Map<Arr<ObjA, ArrowA>, String>, Map<String, Arr<ObjA, ArrowA>>>, Pair<Map<Attribute<ObjA>, String>, Map<String, Attribute<ObjA>>>>, Quad<Signature, Pair<Map<ObjB, String>, Map<String, ObjB>>, Pair<Map<Arr<ObjB, ArrowB>, String>, Map<String, Arr<ObjB, ArrowB>>>, Pair<Map<Attribute<ObjB>, String>, Map<String, Attribute<ObjB>>>>> 
	toMapping(Map<String, Type> types) throws FQLException {
		Quad<Signature, Pair<Map<ObjA, String>, Map<String, ObjA>>, Pair<Map<Arr<ObjA, ArrowA>, String>, Map<String, Arr<ObjA, ArrowA>>>, Pair<Map<Attribute<ObjA>, String>, Map<String, Attribute<ObjA>>>> src = srcCat
				.toSig(types);
		Quad<Signature, Pair<Map<ObjB, String>, Map<String, ObjB>>, Pair<Map<Arr<ObjB, ArrowB>, String>, Map<String, Arr<ObjB, ArrowB>>>, Pair<Map<Attribute<ObjB>, String>, Map<String, Attribute<ObjB>>>> dst = dstCat
				.toSig(types);

		Signature srcSig = src.first;
		Signature dstSig = dst.first;

		Map<Arr<ObjA, ArrowA>, String> srcM = src.third.first;
		Map<ObjA, String> srcM2 = src.second.first;
		Map<Attribute<ObjA>, String> srcMA = src.fourth.first;	
		
		Map<Arr<ObjB, ArrowB>, String> dstM = dst.third.first;
		Map<ObjB, String> dstM2 = dst.second.first;
		Map<Attribute<ObjB>, String> dstMA = dst.fourth.first;
		
		List<Pair<String, String>> nm = new LinkedList<>();
		List<Pair<String, List<String>>> em = new LinkedList<>();
		for (Arr<ObjA, ArrowA> a : srcCat.arrows) {
			if (srcCat.isId(a)) {
				ObjA o = a.src;
				ObjB u = objMapping.get(o);
				nm.add(new Pair<>(srcM2.get(o), dstM2.get(u)));
			} else {
				Arr<ObjB, ArrowB> u = arrowMapping.get(a);
				List<String> t = new LinkedList<>();
				t.add(dstM2.get(u.src));
				if (!dstCat.isId(u)) {
					t.add(dstM.get(u));
				}
				em.add(new Pair<>(srcM.get(a), t));
			}
		}
		
		List<Pair<String, String>> am0 = new LinkedList<>(); 
		for (Attribute<ObjA> k : am.keySet()) {
			am0.add(new Pair<>(srcMA.get(k), dstMA.get(am.get(k))));
		}
		
		Mapping m = new Mapping(/*n,*/ srcSig, dstSig, nm, am0, em);
		return new Triple<>(m, src, dst);
	}

	@Override
	public int hashCode() {
		throw new RuntimeException("Cannot has functors");
	}

	/**
	 * Compose two mappings (is ; not o)
	 * 
	 * @param h
	 *            left
	 * @param G
	 *            right
	 * @return the composition
	 */
	public static <ObjC1, ArrowC1, ObjC2, ArrowC2, ObjT, ArrowT> FinFunctor<ObjC1, ArrowC1, ObjT, ArrowT> compose(
			FinFunctor<ObjC1, ArrowC1, ObjC2, ArrowC2> h,
			FinFunctor<ObjC2, ArrowC2, ObjT, ArrowT> G) {

		Map<ObjC1, ObjT> ret1 = new HashMap<>();
		Map<Arr<ObjC1, ArrowC1>, Arr<ObjT, ArrowT>> ret2 = new HashMap<>();

		for (ObjC1 c1 : h.srcCat.objects) {
			ret1.put(c1, G.applyO(h.applyO(c1)));
		}

		for (Arr<ObjC1, ArrowC1> c1 : h.srcCat.arrows) {
			ret2.put(c1, G.applyA(h.applyA(c1)));
		}
		
		FinFunctor<ObjC1, ArrowC1, ObjT, ArrowT> ret = new FinFunctor<>(ret1, ret2, h.srcCat, G.dstCat);

		if (h.am == null && G.am == null) {
			//need empty block for correctness
		} else if (h.am != null && G.am != null) {
			
			Map<Attribute<ObjC1>, Attribute<ObjT>> ret3 = new HashMap<>();
			for (Attribute<ObjC1> c1 : h.srcCat.attrs) {
				ret3.put(c1, G.am.get(h.am.get(c1)));
			}
			ret.am = ret3;			
			
		} else {
			throw new RuntimeException("h.am " + h.am + "\nG.am " + G.am);
		}
		
		return ret;
	}

	/**
	 * 3-ary composition
	 */
	public static <O1, A1, O2, A2, O3, A3, O4, A4> FinFunctor<O1, A1, O4, A4> compose(
			FinFunctor<O1, A1, O2, A2> h, FinFunctor<O2, A2, O3, A3> G,
			FinFunctor<O3, A3, O4, A4> H) {

		return compose(compose(h, G), H);
	}

	public static <ObjC, ArrowC, ObjD, ArrowD> boolean isDiscreteOpFib(
			FinFunctor<ObjC, ArrowC, ObjD, ArrowD> F) {
		for (ObjC c : F.srcCat.objects) {
			for (Arr<ObjD, ArrowD> g : F.dstCat.arrows) {
				if (F.applyO(c).equals(g.src)) {
					boolean found = false;
					for (ObjC c0 : F.srcCat.objects) {
						for (Arr<ObjC, ArrowC> g0 : F.srcCat.hom(c, c0)) {
							if (F.applyA(g0).equals(g)) {
								if (found) {
									return false;
								}
								found = true;
							}
						}
					}
					if (!found) {
						return false;
					}
				}
			}
		}
		return true;
	}

}

package catdata.fql.cat;

import java.util.Map;
import java.util.Objects;

import catdata.ide.DefunctGlobalOptions;

/**
 * @author ryan Implementation of finite natural transformations.
 */
class FinTrans<Obj1, Arrow1, Obj2, Arrow2> {

	private final Map<Obj1, Arr<Obj2, Arrow2>> eta;
	private final FinFunctor<Obj1, Arrow1, Obj2, Arrow2> F;
    private final FinFunctor<Obj1, Arrow1, Obj2, Arrow2> G;

	/**
	 * Construct a new natural transformation. Does not copy inputs.
	 */
	public FinTrans(Map<Obj1, Arr<Obj2, Arrow2>> eta,
			FinFunctor<Obj1, Arrow1, Obj2, Arrow2> F,
			FinFunctor<Obj1, Arrow1, Obj2, Arrow2> G) {
		this.eta = eta;
		this.F = F;
		this.G = G;
		if (DefunctGlobalOptions.debug.fql.VALIDATE) {
			validate();
		}
	}

	private Arr<Obj2, Arrow2> eta(Obj1 X) {
		return eta.get(X);
	}

	private void validate() {
		if (!Objects.equals(F.srcCat, G.srcCat)) {
			throw new RuntimeException("SrcCat FinTrans mismath " + F.srcCat
					+ " and " + G.srcCat);
		}
		if (!Objects.equals(F.dstCat, G.dstCat)) {
			throw new RuntimeException("DstCat FinTrans mismath " + F.dstCat
					+ " and " + G.dstCat);
		}
		for (Arr<Obj1, Arrow1> f : F.srcCat.arrows) {
			Arr<Obj2, Arrow2> lhs = F.dstCat.compose(F.applyA(f), eta(f.dst));
			Arr<Obj2, Arrow2> rhs = F.dstCat.compose(eta(f.src), G.applyA(f));
			if (!lhs.equals(rhs)) {
				throw new RuntimeException("Bad nat trans " + f + " in " + this);
			}
		}

	}

	@SuppressWarnings("static-method")
	public boolean equals() {
		throw new RuntimeException("Equality of FinTrans");
	}

	@Override
	public String toString() {
		return "FinTrans [eta=\n" + eta + "]";
	}

}

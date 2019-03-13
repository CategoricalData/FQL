package catdata.fqlpp.cat;

import catdata.ide.DefunctGlobalOptions;
public class Adjunction<CO, CA, DO, DA> {

	private final Functor<DO, DA, CO, CA> F;
	private final Functor<CO, CA, DO, DA> G;
	public final Transform<CO, CA, CO, CA> counit;
	public final Transform<DO, DA, DO, DA> unit;

	public Adjunction(Functor<DO, DA, CO, CA> f, Functor<CO, CA, DO, DA> g,
			Transform<CO, CA, CO, CA> counit, Transform<DO, DA, DO, DA> unit) {
		F = f;
		G = g;
		this.counit = counit;
		this.unit = unit;
		validate();
	}

	private void validate() {
		if (!DefunctGlobalOptions.debug.fqlpp.VALIDATE) {
			return;
		}

		Category<CO, CA> C = G.source;
		Category<DO, DA> D = G.target;
		if (!C.equals(F.target)) {
			throw new RuntimeException("Source of " + G + " is not target of " + F);
		}
		if (!D.equals(F.source)) {
			throw new RuntimeException("Target of " + G + " is not source of " + F);
		}
		Functor<CO, CA, CO, CA> FG = Functor.compose(G, F);
		Functor<DO, DA, DO, DA> GF = Functor.compose(F, G);
		Functor<CO, CA, CO, CA> IdC = Functor.identity(C);
		Functor<DO, DA, DO, DA> IdD = Functor.identity(D);
		if (!FG.source.isInfinite()) {
			if (!counit.source.equals(FG)) {
				throw new RuntimeException("Source of " + counit + " is not " + FG);
			}
			if (!unit.target.equals(FG)) {
				throw new RuntimeException("Target of " + unit + " is not " + GF);
			}
		}
		if (!C.isInfinite()) {
			if (!counit.target.equals(IdC)) {
				throw new RuntimeException("Target of " + counit + " is not identity");
			}
		}
		if (!D.isInfinite()) {
			if (!unit.source.equals(IdD)) {
				throw new RuntimeException("Source of " + unit + " is not identity");
			}
		}
		if (!C.isInfinite()) {
			for (CO X : C.objects()) {
				if (!D.identity(G.applyO(X)).equals(
						D.compose(unit.apply(G.applyO(X)), G.applyA(counit.apply(X))))) {
					throw new RuntimeException("Counit-unit equation does not validate on " + X);
				}
			}
		}
		if (!D.isInfinite()) {
			for (DO Y : D.objects()) {
				if (!C.identity(F.applyO(Y)).equals(
						C.compose(F.applyA(unit.apply(Y)), counit.apply(F.applyO(Y))))) {
					throw new RuntimeException("Counit-unit equation does not validate on " + Y);
				}
			}
		}

	} 

// --Commented out by Inspection START (12/24/16, 10:43 PM):
//	public Monad<DO, DA> monad() {
//		Functor<DO, DA, DO, DA> GF = Functor.compose(F, G);
//		Functor<DO, DA, DO, DA> GFGF = Functor.compose(GF, GF);
//		Transform<DO, DA, DO, DA> u = new Transform<>(GFGF, GF, x -> G.applyA(counit.apply(F.applyO(x))));
//		return new Monad<>(GF, unit, u);
//	}
// --Commented out by Inspection STOP (12/24/16, 10:43 PM)

// --Commented out by Inspection START (12/24/16, 10:43 PM):
//	public CoMonad<CO, CA> comonad() {
//		Functor<CO, CA, CO, CA> FG = Functor.compose(G, F);
//		Functor<CO, CA, CO, CA> FGFG = Functor.compose(FG, FG);
//		Transform<CO, CA, CO, CA> u = new Transform<>(FG, FGFG, x -> F.applyA(unit.apply(G.applyO(x))));
//		return new CoMonad<>(FG, counit, u);
//	}
// --Commented out by Inspection STOP (12/24/16, 10:43 PM)

}

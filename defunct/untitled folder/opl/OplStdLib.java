package catdata.opl;

import catdata.ide.Example;
import catdata.ide.Language;

public class OplStdLib extends Example {

	@Override
	public Language lang() {
		return Language.OPL;
	}
	
	@Override
	public String getName() {
		return "Std Lib";
	}

	@Override
	public String getText() {
		return s;
	}
	
	private final String s = "preamble = pragma {"
			+ "\n	options"
			+ "\n		\"opl_prover_force_prec\" = \"true\";"
			+ "\n}"
			+ "\n"
			+ "\n//bools, computationally (note: not boolean algebra)"
			+ "\nBool = theory {"
			+ "\n	sorts"
			+ "\n		Bool;"
			+ "\n	symbols"
			+ "\n		true, false : Bool,"
			+ "\n		aand, or, implies : Bool, Bool -> Bool,"
			+ "\n		not : Bool -> Bool;"
			+ "\n	equations"
			+ "\n		aand(true, true) = true,"
			+ "\n		aand(true, false) = false,"
			+ "\n		aand(false, true) = false,"
			+ "\n		aand(false, false) = false,"
			+ "\n"
			+ "\n		or(true, true) = true,"
			+ "\n		or(true, false) = true,"
			+ "\n		or(false, true) = true,"
			+ "\n		or(false, false) = false,"
			+ "\n"
			+ "\n		implies(true, true) = true,"
			+ "\n		implies(true, false) = false,"
			+ "\n		implies(false, true) = true,"
			+ "\n		implies(false, false) = true,"
			+ "\n"
			+ "\n		not(true) = false,"
			+ "\n		not(false) = true;"
			+ "\n}"
			+ "\n"
			+ "\n//nats, computationally (note: not commutative ring)"
			+ "\nNat = theory {"
			+ "\n	sorts"
			+ "\n		Nat;"
			+ "\n	symbols"
			+ "\n		plus : Nat, Nat -> Nat,"
			+ "\n		zero : Nat, "
			+ "\n		succ : Nat -> Nat;"
			+ "\n	equations"
			+ "\n		forall x. plus(zero, x) = x,"
			+ "\n		forall x, y. plus(succ(x), y) = succ(plus(x,y));"
			+ "\n}"
			+ "\n"
			+ "\n//strings, computationally (note: not free monoid)"
			+ "\nString = theory {"
			+ "\n	sorts"
			+ "\n		Char, String;"
			+ "\n	symbols	"
			+ "\n		a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z : Char,"
			+ "\n		\"\" : Char, String -> String,"
			+ "\n		\" \" : String;"
			+ "\n	equations;"
			+ "\n}"
			+ "\n"
			+ "\nStdLib = theory {"
			+ "\n	imports"
			+ "\n		Bool, Nat, String;"
			+ "\n	sorts"
			+ "\n		Void;"
			+ "\n	symbols"
			+ "\n		length : String -> Nat;"
			+ "\n	equations"
			+ "\n		length(\" \") = zero,"
			+ "\n		forall x, y. length(\"\"(x, y)) = succ(length(y));"
			+ "\n}"
			+ "\n";



}

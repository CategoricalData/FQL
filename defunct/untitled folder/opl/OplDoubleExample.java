package catdata.opl;

import catdata.ide.Example;
import catdata.ide.Language;

public class OplDoubleExample extends Example {
	
	@Override
	public Language lang() {
		return Language.OPL;
	}

	@Override
	public String getName() {
		return "Java Double";
	}

	@Override
	public String getText() {
		return s;
	}
	
	private final String s = "T = theory { "
			+ "\n sorts"
			+ "\n 	Double, Boolean;"
			+ "\n symbols"
			+ "\n 	tru : Boolean,"
			+ "\n  	plus : Double,Double -> Double,"
			+ "\n  	gt : Double,Double -> Boolean,"
			+ "\n 	pi, e, zero, one : Double;"
			+ "\n equations;"
			+ "\n}"
			+ "\n"
			+ "\nM = javascript {"
			+ "\n	symbols"
			+ "\n		\"tru\" -> \"return true\","
			+ "\n	     \"pi\" -> \"return 3.14\","
			+ "\n	     \"e\" -> \"return 2.71\","
			+ "\n	     \"zero\" -> \"return 0\","
			+ "\n	     \"one\" -> \"return 1\","
			+ "\n		\"plus\" -> \"return (input[0] + input[1])\","
			+ "\n		\"gt\" -> \"return (input[0] > input[1])\";"
			+ "\n} : T"
			+ "\n"
			+ "\nS = SCHEMA {"
			+ "\n	entities "
			+ "\n		Person;"
			+ "\n	edges;	"
			+ "\n	attributes"
			+ "\n		age : Person -> Double;"
			+ "\n	pathEqualities;"
			+ "\n	obsEqualities;	"
			+ "\n} : T"
			+ "\n"
			+ "\nI0 = presentation {"
			+ "\n	generators "
			+ "\n		p1, p2, p3, p4 : Person;"
			+ "\n	equations "
			+ "\n		p1.age = pi, p2.age = e, p3.age = zero, p4.age = one;"
			+ "\n} : S"
			+ "\nI = instance S I0 M"
			+ "\n"
			+ "\n"
			+ "\nQ1 = query {"
			+ "\n PersonQ = "
			+ "\n {for p:Person; "
			+ "\n  where ; "
			+ "\n  return age = plus(p.age, one); "
			+ "\n  keys; } : Person"
			+ "\n} : S -> S "
			+ "\n"
			+ "\nJ = apply Q1 I"
			+ "\n"
			+ "\n"
			+ "\nQ2 = query {"
			+ "\n PersonQ = "
			+ "\n {for p:Person; "
			+ "\n  where gt(age(p), pi) = tru; "
			+ "\n  return age = age(p); "
			+ "\n  keys; } : Person"
			+ "\n} : S -> S "
			+ "\n"
			+ "\nK = apply Q2 J"
			+ "\n";



}

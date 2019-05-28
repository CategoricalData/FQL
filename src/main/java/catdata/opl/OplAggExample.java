package catdata.opl;

import catdata.ide.Example;
import catdata.ide.Language;

public class OplAggExample extends Example {

	@Override
	public Language lang() {
		return Language.OPL;
	}
	
	@Override
	public String getName() {
		return "Aggregation";
	}

	@Override
	public String getText() {
		return s;
	}

	private final String s = "preamble = pragma {"
			+ "\n	options"
			+ "\n		\"opl_secret_agg\" = \"true\";"
			+ "\n}"
			+ "\n"
			+ "\nTy = theory { "
			+ "\n sorts"
			+ "\n 	Double;"
			+ "\n symbols"
			+ "\n  	plus : Double,Double -> Double,"
			+ "\n 	zero, ten, one, fifty, two : Double;"
			+ "\n equations;"
			+ "\n}"
			+ "\n"
			+ "\nM = javascript {"
			+ "\n	symbols"
			+ "\n		\"zero\" -> \"return 0\","
			+ "\n	     \"ten\" -> \"return 10\","
			+ "\n	     \"fifty\" -> \"return 50\","
			+ "\n	     \"two\" -> \"return 2\","
			+ "\n	     \"one\" -> \"return 1\","
			+ "\n		\"plus\" -> \"return (input[0] + input[1])\";"
			+ "\n} : Ty"
			+ "\n"
			+ "\nS = SCHEMA {"
			+ "\n	entities "
			+ "\n		Emp, Dept;"
			+ "\n	edges;	"
			+ "\n	attributes"
			+ "\n		salary : Emp -> Double,"
			+ "\n		worksIn : Emp -> Dept;"
			+ "\n	pathEqualities;"
			+ "\n	obsEqualities;	"
			+ "\n} : Ty"
			+ "\n"
			+ "\nT = SCHEMA {"
			+ "\n	entities"
			+ "\n		Dept;"
			+ "\n	edges;"
			+ "\n	attributes"
			+ "\n		totalCost : Dept -> Double;"
			+ "\n	pathEqualities;"
			+ "\n	obsEqualities;"
			+ "\n} : Ty"
			+ "\n"
			+ "\nI0 = presentation {"
			+ "\n	generators "
			+ "\n		p1, p2, p3, p4 : Emp,"
			+ "\n		d1, d2: Dept;"
			+ "\n	equations "
			+ "\n		p1.salary = ten, p2.salary = one, p3.salary = fifty, p4.salary = two,"
			+ "\n		p1.worksIn = d1, p2.worksIn = d1, p3.worksIn = d2, p4.worksIn = d2;"
			+ "\n} : S"
			+ "\nI = instance S I0 M"
			+ "\n"
			+ "\nQ1 = query {"
			+ "\n Q = "
			+ "\n {for d:Dept; "
			+ "\n  where ; "
			+ "\n  return totalCost = agg zero plus {"
			+ "\n  					for p:Emp;"
			+ "\n  					where p.worksIn = d;"
			+ "\n  	                    return p.salary "
			+ "\n                     };"
			+ "\n  keys; } : Dept"
			+ "\n} : S -> T"
			+ "\n"
			+ "\nJ = apply Q1 I"
			+ "\n";


}

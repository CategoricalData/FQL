package catdata.fqlpp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.fqlpp.CatExp.Const;
import catdata.fqlpp.FunctorExp.CatConst;
import catdata.fqlpp.FunctorExp.MapConst;
import catdata.fqlpp.FunctorExp.Var;

class Ben {

	public static Const colim(FQLPPProgram env, CatConst f) {
		if (!(f.sig instanceof CatExp.Var)) {
			throw new RuntimeException(f.sig + " is not variable, is " + f.sig.getClass());
		}
		
		CatExp c = env.cats.get(((CatExp.Var)f.sig).v);
		if (!(c instanceof Const)) {
			throw new RuntimeException(c + " is not finitely presented, is " + c.getClass());
		}
		Const src = (Const) c;
		
		Map<String, Const> obMapping = new HashMap<>();
		Map<String, MapConst> arrMapping = new HashMap<>();
		
		for (String src_ob : f.nm.keySet()) {
			CatExp C = f.nm.get(src_ob);
			if (!(C instanceof CatExp.Var)) {
				throw new RuntimeException(C + " is not a variable");
			}
			CatExp D = env.cats.get(((CatExp.Var)C).v);
			if (!(D instanceof Const)) {
				throw new RuntimeException(D + " is not finitely presented");
			}
			obMapping.put(src_ob, (Const) D);
		}
		
		for (String src_arr : f.em.keySet()) {
			FunctorExp C = f.em.get(src_arr);
			if (!(C instanceof Var)) {
				throw new RuntimeException(C + " is not a variable");
			}
			FunctorExp D = env.ftrs.get(((Var)C).v);
			if (!(D instanceof MapConst)) {
				throw new RuntimeException(D + " is not finitely presented");
			}
			arrMapping.put(src_arr, (MapConst) D);
		}
		
		return sandbox(src, obMapping, arrMapping);
	}

	/**
	 * @param src the source finitely presented category
	 * @param obMapping the action of the functor on objects
	 * @param arrMapping the action of the functor on generating arrows.  
	 * @return a finitely presented category
	 */
	private static Const sandbox(Const src, Map<String, Const> obMapping, Map<String, MapConst> arrMapping) {
		//the objects of the colimit category
		Set<String> objects = new HashSet<>();
		
		//the generating arrows of the colimit in the form (arrowname, source, target)
		Set<Triple<String, String, String>> arrows = new HashSet<>(); //(arrowname, source, target)
		
		//the equations of the colimit in the form (objectname, arrow1, arrow2, ...) = (objectname, arrow1, ...)
		Set<Pair<Pair<String, List<String>>, Pair<String, List<String>>>> eqs = new HashSet<>();

		//: implement colimit here
		
		return new Const(objects, arrows, eqs);
	}
	
}

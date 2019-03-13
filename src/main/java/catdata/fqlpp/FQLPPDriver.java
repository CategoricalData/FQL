package catdata.fqlpp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.LineException;
import catdata.fqlpp.cat.Category;
import catdata.fqlpp.cat.Functor;
import catdata.fqlpp.cat.Transform;
import catdata.fqlpp.cat.FinSet.Fn;

class FQLPPDriver {

	public static FQLPPEnvironment makeEnv(String str, FQLPPProgram init, String... toUpdate) {
		Map<String, Fn<?,?>> fns = new HashMap<>();
		Map<String, Set<?>> sets = new HashMap<>();
		Map<String, Category<?,?>> cats = new HashMap<>();
		Map<String, Functor<?,?,?,?>> ftrs = new HashMap<>();
		Map<String, Transform<?,?,?,?>> trans = new HashMap<>();
		FQLPPEnvironment ret = new FQLPPEnvironment(init, str, sets, fns, cats, ftrs, trans);

		for (Entry<String, CatExp> k : init.cats.entrySet()) {
			init.cats.put(k.getKey(), k.getValue().accept(init, new PreProcessor()));
		}
		
		
		for (String k : init.order) {
			SetExp se = init.sets.get(k);
			if (se != null) {
				try {
					Set<?> xxx = se.accept(init, new SetOps(ret));
					sets.put(k, xxx);
				} catch (Throwable t) {
					t.printStackTrace();
					throw new LineException(t.getLocalizedMessage(), k, "set");
				}
				toUpdate[0] = "Last Processed: " + k;
			}
			FnExp fe = init.fns.get(k);
			if (fe != null) {
				try {
					Fn<?,?> xxx = fe.accept(init, new SetOps(ret));
					fns.put(k, xxx);
					toUpdate[0] = "Last Processed: " + k;
				} catch (Throwable t) {
					t.printStackTrace();
					throw new LineException(t.getLocalizedMessage(), k, "function");
				}
			}
			CatExp ce = init.cats.get(k);
//			CatExp ce = ce0.accept(init, new PreProcessor());
			if (ce != null) {
				try {
					Category<?,?> xxx = ce.accept(init, new CatOps(ret));
					cats.put(k, xxx);
					toUpdate[0] = "Last Processed: " + k;
				} catch (Throwable t) {
					t.printStackTrace();
					throw new LineException(t.getLocalizedMessage(), k, "category");
				}
			}
			FunctorExp FE = init.ftrs.get(k);
			if (FE != null) {
				try {
					Functor<?,?,?,?> xxx = FE.accept(init, new CatOps(ret));
						ftrs.put(k, xxx);		
					toUpdate[0] = "Last Processed: " + k;
				} catch (Throwable t) {
					t.printStackTrace();
					throw new LineException(t.getLocalizedMessage(), k, "functor");
				}
			}
			TransExp te = init.trans.get(k);
			if (te != null) {
				try {
					Transform<?,?,?,?> xxx = te.accept(init, new CatOps(ret));
					trans.put(k, xxx);	
					toUpdate[0] = "Last Processed: " + k;
				} catch (Throwable t) {
					t.printStackTrace();
					throw new LineException(t.getLocalizedMessage(), k, "transform");
				}
			}
		}
		
		return ret;
	}

}

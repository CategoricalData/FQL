package catdata.opl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import catdata.Environment;
import catdata.LineException;
import catdata.Program;
import catdata.ide.DefunctGlobalOptions;
import catdata.opl.OplExp.OplInst0;
import catdata.opl.OplExp.OplPragma;
import catdata.opl.OplExp.OplSCHEMA0;
import catdata.opl.OplExp.OplSig;

@SuppressWarnings({"unused","unchecked","rawtypes"})
class OplDriver {

	// : let x be the position of the first change between old and new
	// program.
	// a definition y is 'safe' if definition y+1 begins before x.
	// each code editor's driver should copy over the safe definitions and start
	// the driver
	// at the appropriate spot
	
	
	

	public static Environment<OplObject> makeEnv(String str,
			Program<OplExp> init, String[] toUpdate, String last_str,
			Program<OplExp> last_prog, Environment<OplObject> last_env) {
		Environment<OplObject> ret = new Environment<>();
		//Map<String, Integer> extra = new HashMap<>();

		boolean usesPragma = false;
		for (String k : init.order) {
			
			OplExp se = init.exps.get(k);
			if (se instanceof OplPragma) {
				se.accept(init, new OplOps(ret));
				ret.put(k, se);
				usesPragma = true;
			}
		}
		
		if (DefunctGlobalOptions.debug.opl.opl_lazy_gui && usesPragma) {
			throw new RuntimeException("Pragmas and lazy guis are not compatible");
		}

		if (DefunctGlobalOptions.debug.opl.opl_prover_force_prec) {
			inferPrec(init);
		}

		Set<String> unchanged = new HashSet<>();
		int i = 0;
		if (last_str != null && DefunctGlobalOptions.debug.opl.opl_cache_gui) {
			for (String k : init.order) {
				if (i >= last_prog.order.size()) {
					break;
				}
				String v = last_prog.order.get(i);
				if (!v.equals(k)) {
					break;
				} 
				OplExp a = init.exps.get(k);
				OplExp b = last_prog.exps.get(k);
				if (!a.equals(b)) {
					break;
				}
				unchanged.add(k);
				i++;
			}
		}

		// int i = 0;
		// toUpdate[0] = "Processed:";
		for (String k : init.order) {
			OplExp se = init.exps.get(k);
			if (se instanceof OplPragma) {
				continue;
			}

			if (unchanged.contains(k)) {
				ret.put(k, last_env.get(k));
				continue;
			}
			try {
				OplObject xxx = se.accept(init, new OplOps(ret));
				ret.put(k, xxx);
				if (toUpdate != null) {
					toUpdate[0] = "Last Processed: " + k;
				}
				// i++;
			} catch (Throwable t) {
				t.printStackTrace();
				throw new LineException(t.getLocalizedMessage(), k, "");
			}
		}

		//int j = 0;
		//for (Entry<String, Integer> e : extra.entrySet()) {
	//		init.order.add(e.getValue() + j, e.getKey());
	//		j++;
	//	}

		// : add to order

		return ret;

	}

	private static void inferPrec(Program<OplExp> init) {
		int curPrec = 10000;
		// constants < generators < attributes < foreign keys
		for (String k : init.order) {
			OplExp v = init.exps.get(k);
			if (v instanceof OplSig) {
				OplSig sig = (OplSig) v;
				sig.prec = new HashMap<>();
				for (Object c : sig.symbols.keySet()) {
					sig.prec.put(c, curPrec++);
				}
			}
		}
		for (String k : init.order) {
			OplExp v = init.exps.get(k);
			if (v instanceof OplInst0) {
				OplInst0 sig = (OplInst0) v;
				sig.P.prec = new HashMap<>();
				for (Object c : sig.P.gens.keySet()) {
					sig.P.prec.put(c, curPrec++);
				}
			}
		}
		for (String k : init.order) {
			OplExp v = init.exps.get(k);
			if (v instanceof OplSCHEMA0) {
				OplSCHEMA0 sig = (OplSCHEMA0) v;
				sig.prec = new HashMap<>();
				for (Object c : sig.attrs.keySet()) {
					sig.prec.put(c, curPrec++);
				}
			}
		}
		for (String k : init.order) {
			OplExp v = init.exps.get(k);
			if (v instanceof OplSCHEMA0) {
				OplSCHEMA0 sig = (OplSCHEMA0) v;
				for (Object c : sig.edges.keySet()) {
					sig.prec.put(c, curPrec++);
				}
			}
		}

	}

}

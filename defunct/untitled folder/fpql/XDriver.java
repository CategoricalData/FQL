package catdata.fpql;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import catdata.LineException;
import catdata.fpql.XExp.Flower;
import catdata.ide.DefunctGlobalOptions;


class XDriver {

	@SuppressWarnings({ "rawtypes" })
	public static XEnvironment makeEnv(String str, XProgram init, String... toUpdate) {
		if (DefunctGlobalOptions.debug.fpql.x_typing) {
			init.doTypes();
		}
		XEnvironment ret = new XEnvironment(init, str);
		Map<String, Integer> extra = new HashMap<>();
		
		int i = 0;
		for (String k : init.order) {
			XExp se = init.exps.get(k);
			try {
				XObject xxx = se.accept(init, new XOps(ret));
				if (xxx == null) {
					throw new RuntimeException();
				}
				if (se instanceof Flower) {
					Flower f = (Flower) se;
					if (ret.objs.containsKey(f.ty)) {
						throw new RuntimeException("Duplicate: " + f.ty);
					}
					XCtx c = (XCtx) xxx;
					if (f.ty != null) {
						ret.objs.put(f.ty, c.schema);
						extra.put(f.ty, i);
					}
				} 
				if (ret.objs.containsKey(k)) {
					throw new RuntimeException("Duplicate: " + k);
				}
				ret.objs.put(k, xxx);
				if (toUpdate != null && toUpdate.length > 0) {
					toUpdate[0] = "Last Processed: " + k ;
				}
				i++;
			} catch (Throwable t) {
				t.printStackTrace();
				throw new LineException(t.getLocalizedMessage(), k, "");
			}
		}
		
		int j = 0;
		for (Entry<String, Integer> e : extra.entrySet()) {
			init.order.add(e.getValue() + j, e.getKey()); 
			j++;
		}
		
		//: add to order
		return ret;
	}

}

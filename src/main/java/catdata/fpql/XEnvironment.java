package catdata.fpql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.Pair;
import catdata.fpql.XExp.XConst;
import catdata.fpql.XExp.XEq;
import catdata.fpql.XExp.XFn;
import catdata.fpql.XExp.XTy;

public class XEnvironment {

	XCtx<String> global;
	private final XProgram prog;
	private final String str;
	public final Map<String, XObject> objs;
//	public Set<String> types;
//	public Map<String, Pair<String, String>> fns;
//	public Set<Pair<List<String>, List<String>>> eqs = new HashSet<>();
	// a.b.c = p.q.r
	
	public XEnvironment(XProgram prog, String str) {
		this.prog = prog;
		this.str = str;
		
		Set<String> types;
		Map<String, Pair<String, String>> fns;
		Set<Pair<List<String>, List<String>>> eqs = new HashSet<>();

		objs = new HashMap<>();
		
		types = new HashSet<>();
		types.add("_1");
		fns = new HashMap<>();
		fns.put("_1", new Pair<>("_1", "_1"));

		for (Entry<String, XExp> k : prog.exps.entrySet()) {
			XExp e = k.getValue();
			if (e instanceof XTy) {
				if (k.getKey().equals("_1")) {
					throw new RuntimeException("Type 1 re-bound by user");
				}
				types.add(k.getKey());
				fns.put(k.getKey(), new Pair<>(k.getKey(), k.getKey()));
//				eqs.add(new Pair)
			}
			if (e instanceof XFn) {
				if (!types.contains(((XFn) e).src)) {
					throw new RuntimeException("Unknown type: " + ((XFn) e).src);
				}
				if (!types.contains(((XFn) e).dst)) {
					throw new RuntimeException("Unknown type: " + ((XFn) e).dst);
				}
				fns.put(k.getKey(), new Pair<>(((XFn) e).src, ((XFn) e).dst));
			}
			if (e instanceof XConst) {
				if (!types.contains(((XConst) e).dst)) {
					throw new RuntimeException("Unknown type: " + ((XConst) e).dst);
				}
				fns.put(k.getKey(), new Pair<>("_1", ((XConst) e).dst));
			}
			//: check these here?
			if (e instanceof XEq) {
				eqs.add(new Pair<>(((XEq) e).lhs, ((XEq) e).rhs));
			}
		}
		
		global = new XCtx<>(types, fns, eqs, null, null, "global");		
	}

	@Override
	public String toString() {
		return "XEnvironment [global=" + global + ", prog=" + prog + ", str=" + str + ", objs="
				+ objs + "]";
	}
	
}

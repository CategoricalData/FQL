package catdata.fql.sql;

import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.fql.decl.Transform;
import catdata.fql.FQLException;
import catdata.fql.cat.Inst;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Signature;

public class PSMIso extends PSM {
	
	private final boolean lToR;
	private final String l;
    private final String r;
	private final Signature sig;
	private final String pre;

	public PSMIso(boolean lToR, String pre, String l, String r, Signature sig) {
        this.lToR = lToR;
		this.l = l;
		this.r = r;
		this.sig = sig;
		this.pre = pre;
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		//throw new RuntimeException("Iso finder is not working right now");
		 try {
			Instance li = new Instance(sig, PSMGen.gather(l, sig, state));
			Instance ri = new Instance(sig, PSMGen.gather(r, sig, state));
			
			Pair<Transform, Transform> k = Inst.iso(li, ri);
			if (k == null) {
				throw new RuntimeException("Cannot find iso between " + l + " and " + r);
			}
			
			if (lToR) {
				PSMGen.shred(pre, k.first, state);
			} else {
				PSMGen.shred(pre, k.second, state);
			}
			
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		} 
		
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for iso.");
	}
	
	@Override
	public String toString() {
        return lToR ? "iso1 " + l + " " + r : "iso2 " + l + " " + r;
	}
	
	@Override
	public String isSql() {
		return pre;
	}


}

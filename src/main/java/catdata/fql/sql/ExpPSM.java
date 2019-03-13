package catdata.fql.sql;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.IntRef;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

/**
 * 
 * @author ryan
 *
 * Exponentials of instances
 */
public class ExpPSM extends PSM {

	private final String pre;
    private final String I;
    private final String J;
	private final Signature sig;
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((J == null) ? 0 : J.hashCode());
		result = prime * result + ((pre == null) ? 0 : pre.hashCode());
		result = prime * result + ((sig == null) ? 0 : sig.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpPSM other = (ExpPSM) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (J == null) {
			if (other.J != null)
				return false;
		} else if (!J.equals(other.J))
			return false;
		if (pre == null) {
			if (other.pre != null)
				return false;
		} else if (!pre.equals(other.pre))
			return false;
		if (sig == null) {
			if (other.sig != null)
				return false;
		} else if (!sig.equals(other.sig))
			return false;
		return true;
	}

	public ExpPSM(String pre, String i, String j, Signature sig) {
        this.pre = pre;
		I = i;
		J = j;
		this.sig = sig;
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		try {
			Instance Ix = new Instance(sig, PSMGen.gather(I, sig, state));
			Instance Jx = new Instance(sig, PSMGen.gather(J, sig, state));
			IntRef idx = new IntRef(interp.guid);
			Quad<Instance, Map<Pair<Node, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>>>, Map<Node, Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>>>, Map<Node, Map<Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>, Object>>> ret = Instance.exp2(idx, Ix, Jx);
			interp.guid = idx.i;
			interp.exps2.put(pre, ret);
			PSMGen.shred(pre, ret.first, state);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}
	
	@Override
	public String toString() {
		return pre + " = (" + I + "^" + J + ")";
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for exponentials.");
	}
	
	@Override
	public String isSql() {
		return pre;
	}


}

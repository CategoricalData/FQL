package catdata.fql.sql;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.cat.FinCat;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

public class PSMEval extends PSM {

	private final String pre;
    private final String A;
    private final String B;
    private final String AB;
    private final String ABB;
	private final Signature sig;


	@Override
	public void exec(PSMInterp interp, Map<String, Set<Map<Object, Object>>> state) {
		try {
			Quad<Instance, Map<Pair<Node, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>>>, Map<Node, Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>>>, Map<Node, Map<Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>, Object>>> xxx = interp.exps2
					.get(AB);
			Instance Jw = xxx.first;
			Map<Pair<Node, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>>> map = xxx.second;
			Map<Node, Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>>> map2 = xxx.third;
		
			FinCat<Node, Path> cat = Jw.thesig.toCategory2().first;

			Instance abb = new Instance(sig, PSMGen.gather(ABB, sig, state));
			// Instance ab = new Instance(sig, PSMGen.gather(AB, sig, state));
			// //already have in interp
			Instance a = new Instance(sig, PSMGen.gather(A, sig, state));
			Instance b = new Instance(sig, PSMGen.gather(B, sig, state));

			Transform fst = new Transform(abb, Jw, PSMGen.gather(ABB + "_fst", sig, state));
			Transform snd = new Transform(abb, b, PSMGen.gather(ABB + "_snd", sig, state));

			List<Pair<String, List<Pair<Object, Object>>>> data = new LinkedList<>();
			for (Node n : sig.nodes) {
				List<Pair<Object, Object>> d = new LinkedList<>();
				Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>> m2 = map2
						.get(n);
		
				for (Pair<Object, Object> id : abb.data.get(n.string)) {
					Object id_ab = lookup(fst.data.get(n.string), id.first);
					Object x = lookup(snd.data.get(n.string), id.first);
					Transform t = m2.get(id_ab).second;
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> w = m2.get(id_ab).first;
					Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>> m = map
							.get(new Pair<>(n, w));
					Object y = m.third.get(n).get(new Pair<>(cat.id(n), x));
					Object f = lookup(t.data.get(n.string), y);
					d.add(new Pair<>(id.first, f));
				}

				data.add(new Pair<>(n.string, d));
			}
			Transform curry = new Transform(abb, a, data);
			PSMGen.shred(pre, curry, state);

		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}

	private static Object lookup(Set<Pair<Object, Object>> data, Object first) {
		for (Pair<Object, Object> k : data) {
			if (k.first.equals(first)) {
				return k.second;
			}
		}
		throw new RuntimeException("Cannot find " + first + " in " + data);
	}

	@Override
	public String toString() {
		return ABB + ".curry";
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((A == null) ? 0 : A.hashCode());
		result = prime * result + ((AB == null) ? 0 : AB.hashCode());
		result = prime * result + ((ABB == null) ? 0 : ABB.hashCode());
		result = prime * result + ((B == null) ? 0 : B.hashCode());
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
		PSMEval other = (PSMEval) obj;
		if (A == null) {
			if (other.A != null)
				return false;
		} else if (!A.equals(other.A))
			return false;
		if (AB == null) {
			if (other.AB != null)
				return false;
		} else if (!AB.equals(other.AB))
			return false;
		if (ABB == null) {
			if (other.ABB != null)
				return false;
		} else if (!ABB.equals(other.ABB))
			return false;
		if (B == null) {
			if (other.B != null)
				return false;
		} else if (!B.equals(other.B))
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

	public PSMEval(String pre, String a, String b, String aB, String aBB, Signature sig) {
        this.pre = pre;
		A = a;
		B = b;
		AB = aB;
		ABB = aBB;
		this.sig = sig;
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for eval.");
	}

	@Override
	public String isSql() {
		return pre;
	}

}

package catdata.fql.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.Pair;
import catdata.fql.cat.Arr;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

public class PSMUnChi extends PSM {

	private final Signature sig;
	private final String pre;
    private final String b;
    private final String prop;
    private final String f;
	Signature fullSig;

	@Override
	public String isSql() {
		return pre;
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		try {
			Map<Object, Object> m1 = new HashMap<>();
			Map<Object, Object> m2 = new HashMap<>();
			
			Instance B = new Instance(sig, PSMGen.gather(b, sig, state));
			Instance P = new Instance(sig, PSMGen.gather(prop, sig, state));
	//		Instance P = interp.prop2.get(prop).first;
			Transform F = new Transform(B, P, PSMGen.gather(f, sig, state));

			Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();
			List<Pair<String, List<Pair<Object, Object>>>> data2 = new LinkedList<>();

			for (Node d : sig.nodes) {
				Instance tofind = interp.prop1.get(prop).first.get(d).first;
				Object found = interp.prop2.get(prop).second.get(d).second.get(tofind);
				Set<Pair<Object, Object>> dta = new HashSet<>();
				List<Pair<Object, Object>> dta2 = new LinkedList<>();
				for (Pair<Object, Object> k : B.data.get(d.string)) {
					Object v = lookup(F.data.get(d.string), k.first);
					
					Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>> vv = interp.prop3.get(prop).get(d).get(v);
					
					if (vv.first.equals(found)) {
						Object u = Integer.toString(++interp.guid);
						m1.put(u, k.first);
						m2.put(k.first, u);
						dta.add(new Pair<>(u,u));
						dta2.add(new Pair<>(u, k.first));
					}
				}
				data.put(d.string, dta);
				data2.add(new Pair<>(d.string, dta2));
			}
			for (Edge e : sig.edges) {
				Set<Pair<Object, Object>> dta = new HashSet<>();
				for (Pair<Object, Object> k : data.get(e.source.string)) {
					Object v = m1.get(k.first);
					Object vx = lookup(B.data.get(e.name), v);
					Object vz = m2.get(vx);
					dta.add(new Pair<>(k.first, vz));
				}
				data.put(e.name, dta);
			}
			for (Attribute<Node> e : sig.attrs) {
				Set<Pair<Object, Object>> dta = new HashSet<>();
				for (Pair<Object, Object> k : data.get(e.source.string)) {
					Object v = m1.get(k.first);
					Object vx = lookup(B.data.get(e.name), v);
					dta.add(new Pair<>(k.first, vx));
				}
				data.put(e.name, dta);
			}
			Instance A = new Instance(sig, data);
			Transform T = new Transform(A, B, data2);
			PSMGen.shred(pre, A, state);
			PSMGen.shred(pre + "_trans", T, state);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}
	
	private static <X,Y> Y lookup(Set<Pair<X,Y>> set, X x) {
		for (Pair<X, Y> k : set) {
			if (k.first.equals(x)) {
				return k.second;
			}
		}
		throw new RuntimeException();
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for kernel.");
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		result = prime * result + ((f == null) ? 0 : f.hashCode());
		result = prime * result + ((pre == null) ? 0 : pre.hashCode());
		result = prime * result + ((prop == null) ? 0 : prop.hashCode());
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
		PSMUnChi other = (PSMUnChi) obj;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		if (f == null) {
			if (other.f != null)
				return false;
		} else if (!f.equals(other.f))
			return false;
		if (pre == null) {
			if (other.pre != null)
				return false;
		} else if (!pre.equals(other.pre))
			return false;
		if (prop == null) {
			if (other.prop != null)
				return false;
		} else if (!prop.equals(other.prop))
			return false;
		if (sig == null) {
			if (other.sig != null)
				return false;
		} else if (!sig.equals(other.sig))
			return false;
		return true;
	}

	public PSMUnChi(Signature sig, String pre, String b, String prop,
			String f) {
        this.sig = sig;
		//this.fullSig = sig;
		//this.sig = new Signature(sig.nodes, sig.edges, new LinkedList<Attribute<Node>>(), sig.eqs);
		this.pre = pre;
		this.b = b;
		this.prop = prop;
		this.f = f;
	}

}

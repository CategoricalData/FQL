package catdata.fql.sql;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.Pair;
import catdata.fql.cat.Arr;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.InstExp.Const;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

public class PSMBool extends PSM {

	public PSMBool(boolean bool, String unit, String prop, Signature sig,
			String pre, @SuppressWarnings("unused") Const unitX,
			Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>> m1, 
			@SuppressWarnings("unused") Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>> m2) {
	//	this.unitX = unitX;
		this.bool = bool;
		this.unit = unit;
		this.prop = prop;
		this.sig = sig;
		this.pre = pre;
		this.m1 = m1;
//		this.m2 = m2;
	}

	private final boolean bool;
	private final String unit;
    private final String prop;
	private final Signature sig;
	private final String pre;
	private final Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>> m1;
//	private final Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>> m2;
//	private final Const unitX;
	
	@Override
	public String isSql() {
		return pre;
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		try {	
			
			Signature sig0 = new Signature(sig.nodes, sig.edges, new LinkedList<>(), sig.eqs);
			
			Instance unitI = new Instance(sig, PSMGen.gather(unit, sig, state));
			Instance propI = new Instance(sig, PSMGen.gather(prop, sig, state));

			Map<Node, Map<Object, Object>> subst_inv = new HashMap<>();
			for (Node n : sig.nodes) {
				Map<Object, Object> m = new HashMap<>();
				Set<Map<Object, Object>> g = state.get(unit + "_" + n + "_subst_inv");
				for (Map<Object, Object> j : g) {
					m.put(j.get("c0"), j.get("c1"));
				}
				subst_inv.put(n, m);
			}
			
			List<Pair<String, List<Pair<Object, Object>>>> data = new LinkedList<>();
			for (Node n : sig.nodes) {				
				List<Pair<Object, Object>> set = new LinkedList<>();
				for (Pair<Object, Object> k : unitI.data.get(n.string)) {
					Object k0 = subst_inv.get(n).get(k.first);
					if (k0 == null) {
						throw new RuntimeException();
					}
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> v = m1.get(n).get(k0);
					
					Instance tofind = bool ? interp.prop1.get(prop).first.get(n).first : new Instance(sig0);
					Object found = interp.prop2.get(prop).second.get(n).second.get(tofind);
					Object r = interp.prop4.get(prop).get(n).get(new Pair<>(found, v));
					set.add(new Pair<>(k.first, r));
				}
				data.add(new Pair<>(n.string, set));
			}
			
			Transform ret = new Transform(unitI, propI, data);
			PSMGen.shred(pre, ret, state);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}

	@Override
	public String toPSM() {
		return pre;
	}
	
	@Override
	public String toString() {
		return pre + " := " + prop + "." + bool + " " + unit;
	}

}

package catdata.fql.sql;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.fql.Fn;
import catdata.IntRef;
import catdata.Pair;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.cat.FinCat;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

public class PropPSM extends PSM {
	
	private final String pre;
	private final Signature sig;

	@Override
	public String isSql() {
		return pre;
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		try {
			IntRef ref = new IntRef(interp.guid);
		
			Signature sigX = new Signature(sig.nodes, sig.edges, new LinkedList<>(), sig.eqs);
			
			Map<Node, List<Pair<Arr<Node, Path>, Attribute<Node>>>> obs = sig.obs();
			
			Pair<FinCat<Node, Path>, Fn<Path, Arr<Node, Path>>> ooo = sig.toCategory2();
			Fn<Path, Arr<Node, Path>> fn = ooo.second;
			
			Pair<Pair<Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>>, Pair<Instance, Map<Node, Pair<Map<Object, Instance>, Map<Instance, Object>>>>> xxx = sigX.omega(ref);
			interp.prop1.put(pre, xxx.first);
			interp.prop2.put(pre, xxx.second);
			Instance old = xxx.second.first;
			Map<Node, List<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>> m = sig.obsbar();
	
			Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();			
			Map<Node, Map<Object, Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>> m1 = new HashMap<>();
			Map<Node, Map<Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Object>> m2 = new HashMap<>();
			for (Node n : sig.nodes) {
				Map<Object, Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>> map1 = new HashMap<>();
				Map<Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Object> map2 = new HashMap<>();
				Set<Pair<Object, Object>> set = new HashSet<>();
				m1.put(n, map1);
				m2.put(n, map2);				
				for (Pair<Object, Object> i1 : old.data.get(n.string)) {
					for (LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> i2 : m.get(n)) {
							Object o = Integer.toString(++ref.i);
							map1.put(o, new Pair<>(i1.first, i2));
							map2.put(new Pair<>(i1.first, i2), o);
							set.add(new Pair<>(o, o));
					}
				}
				data.put(n.string, set);
			}
			for (Attribute<Node> a : sig.attrs) {
				Set<Pair<Object, Object>> set = new HashSet<>();
				for (Pair<Object, Object> k : data.get(a.source.string)) {
					Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>> kk = m1.get(a.source).get(k.first);
					//Object old_id = kk.first;
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id = kk.second;
					set.add(new Pair<>(k.first, new_id.get(new Pair<>(new Arr<>(new Path(sig, a.source), a.source, a.source), a))));
				}
				data.put(a.name, set);
			}
			for (Edge a : sig.edges) {
				Set<Pair<Object, Object>> set = new HashSet<>();
				for (Pair<Object, Object> k : data.get(a.source.string)) {
					Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>> kk = m1.get(a.source).get(k.first);
					Object old_id = kk.first;
					Object old_id0 = lookup(old.data.get(a.name), old_id);
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id = kk.second;
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id0 = truncate2(sig, new_id, fn.of(new Path(sig, a)), obs.get(a.target));
					Object o = m2.get(a.target).get(new Pair<>(old_id0, new_id0));
					set.add(new Pair<>(k.first, o));
				}
				data.put(a.name, set);
			}
			interp.prop3.put(pre, m1);
			interp.prop4.put(pre, m2);
			Instance ne = new Instance(sig, data);	
			PSMGen.shred(pre, ne, state);
			interp.guid = ref.i;		
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	} 

	
	public static LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> truncate2(Signature sig,
			LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id, Arr<Node, Path> p,
			List<Pair<Arr<Node, Path>, Attribute<Node>>> obsD
			) throws FQLException {
		LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> ret = new LinkedHashMap<>();
		
		for (Pair<Arr<Node, Path>, Attribute<Node>> obD : obsD) {
			Pair<Arr<Node, Path>, Attribute<Node>> q = new Pair<>(sig.toCategory2().first.compose(p, obD.first), obD.second);
			ret.put(obD, new_id.get(q));
		}

		return ret;
	}
	

	public static <X,Y> Y lookup(Collection<Pair<X,Y>> set, X x) {
		for (Pair<X, Y> k : set) {
			if (k.first.equals(x)) {
				return k.second;
			}
		}
		throw new RuntimeException("Cannot find " + x + " in " + set);
	}
	
	public PropPSM(String pre, Signature sig) {
        this.pre = pre;
		this.sig = sig;
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for prop.");
	}
	
	@Override
	public String toString() {
		return "prop " + sig;
	}

}

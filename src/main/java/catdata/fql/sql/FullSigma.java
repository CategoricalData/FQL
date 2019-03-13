package catdata.fql.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Quad;
import catdata.fql.cat.LeftKanSigma;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Node;
import catdata.fql.decl.Signature;

/**
 * 
 * @author ryan
 *
 * PSM for full sigma.  Note that this cannot
 * actually be implemented by a real RDBMS.
 */
public class FullSigma extends PSM {
	
	private final Mapping f;
	private final String pre;
	private final String inst;
	
	@Override 
	public String toString() {
		return pre + " := SIGMA " + f + " : " + f.source + " -> " + f.target + " " + inst;
	}

	public FullSigma(Mapping f, String pre, String inst) {
		this.f = f;
		this.pre = pre;
		this.inst = inst;
	}

	@Override
	public void exec(PSMInterp interp, Map<String, Set<Map<Object, Object>>> state) {
		Signature C = f.source;
		Signature D = f.target;
		List<Pair<String, List<Pair<Object, Object>>>> I0 = PSMGen.gather(inst, C, state);

		try {
			Instance I = new Instance(C, I0);
			interp.sigmas.put(pre, interp.guid);
			Quad<Instance, Map<Node, Map<Object, Integer>>, Map<Node, Map<Integer, Object>>, Map<Object, List<Pair<String, Object>>>> xxx = LeftKanSigma.fullSigmaWithAttrs(interp, f, I, null, null, null);
			interp.sigmas2.put(pre, interp.guid);
			Instance J = xxx.first;
			Map<Node, Map<Object, Integer>> yyy = xxx.second;

			for (Node n : C.nodes) {
				state.put(pre + "_" + n.string + "_e", conv2(yyy.get(n)));				
			}

			for (Node n : D.nodes) {
				state.put(pre + "_" + n.string, conv(J.data.get(n.string)));
			}
			for (Edge n : D.edges) {
				state.put(pre + "_" + n.name, conv(J.data.get(n.name)));
			}
			for (Attribute<Node> n : D.attrs) {
				state.put(pre + "_" + n.name, conv(J.data.get(n.name)));
			}
			
			Set<Map<Object, Object>> l = new HashSet<>();
			for (Object k : xxx.fourth.keySet()) {
				List<Pair<String, Object>> v = xxx.fourth.get(k);
				if (v.isEmpty()) {
					continue;
				}
				Map<Object, Object> m = new HashMap<>();
				m.put("c0", k);
				boolean first = true;
				String rest = "";
				for (Pair<String, Object> p : v) {
					if (first) {
						first = false;
						m.put("c1", p.first);
						m.put("c2", p.second);
					} else {
						rest += p.first;
					}
				}
				m.put("c3", rest);
				l.add(m);
			}
			state.put(pre + "_lineage", l);
						
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException("Error in instance " + pre + ": " + e.getLocalizedMessage());
		}
		
	}

	private static Set<Map<Object, Object>> conv2(Map<Object, Integer> map) {
		Set<Map<Object, Object>> ret = new HashSet<>();
		
		for (Object k : map.keySet()) {
			Integer v = map.get(k);
			Map<Object, Object> m = new HashMap<>();
			m.put("c0", k.toString());
			m.put("c1", v.toString());
			ret.add(m);
		}
		
		return ret;
	}

	private static Set<Map<Object, Object>> conv(Set<Pair<Object, Object>> set) {
		Set<Map<Object, Object>> ret = new HashSet<>();
		for (Pair<Object, Object> p : set) {
			Map<Object, Object> m = new HashMap<>();
			m.put("c0", p.first);
			m.put("c1", p.second);
			ret.add(m);
		}
		return ret;
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for full sigma");
	}
	
	@Override
	public String isSql() {
		throw new RuntimeException();
	}

}

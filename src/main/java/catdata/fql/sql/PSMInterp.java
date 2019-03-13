package catdata.fql.sql;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Transform;
import catdata.ide.DefunctGlobalOptions;

/**
 * 
 * @author ryan
 * 
 *         Interpreter for PSM
 */
public class PSMInterp {

	public int guid = 0; 

	//keeps track of where guid was when sigma computations start,
	//so we can replay later for sigma on transform
	public final Map<String, Integer> sigmas = new HashMap<>();
	public final Map<String, Integer> sigmas2 = new HashMap<>();

	Map<String, Quad<Instance, Map<Node, Map<Object, Transform>>, Map<Node, Triple<Instance, Map<Object, Pair<Object, Object>>, Map<Pair<Object, Object>, Object>>>, Pair<Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>>>> exps = new HashMap<>();
	final Map<String, Quad<Instance, Map<Pair<Node, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>>>, Map<Node, Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>>>, Map<Node, Map<Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>, Object>>>> exps2 = new HashMap<>();
	
	final Map<String, Pair<Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>>> prop1 = new HashMap<>();
	final Map<String, Pair<Instance, Map<Node, Pair<Map<Object, Instance>, Map<Instance, Object>>>>> prop2 = new HashMap<>();

	final Map<String, Map<Node, Map<Object, Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>>> prop3 = new HashMap<>();
	final Map<String, Map<Node, Map<Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Object>>> prop4 = new HashMap<>();

	
	public  Pair<Map<String, Set<Map<Object, Object>>>, List<Throwable>> interpX(List<PSM> prog,
			Map<String, Set<Map<Object, Object>>> state) {
		List<Throwable> ret = new LinkedList<>();
		for (PSM cmd : prog) {
			try {
				cmd.exec(this, state);
				//check(cmd, state);
			} catch (Throwable t) {
				if (DefunctGlobalOptions.debug.fql.continue_on_error) {
					ret.add(t);
				} else {
					throw t;
				}
			}
		}
		return new Pair<>(state, ret);
	}
	
	@SuppressWarnings("unused")
	private static void check(PSM p, Map<String, Set<Map<Object, Object>>> state) {
		for (String k : state.keySet()) {
			for (Map<Object, Object> v : state.get(k)) {
				if (v.get("c0") != null && v.get("c0") instanceof Integer && !k.contains("lineage")) {
					throw new RuntimeException("bad: " + p + " table " + k);
				}
			}
		}
	}

	public Pair<Map<String, Set<Map<Object, Object>>>, List<Throwable>> interp(List<PSM> prog) {
		guid = 0;
		return interpX(prog, new HashMap<>());
	}
	
	

}

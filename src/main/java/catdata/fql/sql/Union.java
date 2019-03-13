package catdata.fql.sql;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         SQL Union
 */
public class Union extends SQL {

	private final List<Flower> flowers;

	public Union(List<Flower> flowers) {
		if (flowers.size() < 2) {
			throw new RuntimeException();
		}
		this.flowers = flowers;
	}

	@Override
	public String toPSM() {
		String ret = "";
		for (int i = 0; i < flowers.size(); i++) {
			if (i > 0) {
				ret += " UNION ";
			}
			ret += flowers.get(i);
		}

		return ret;
	}

	@Override
	public Set<Map<Object, Object>> eval(
			Map<String, Set<Map<Object, Object>>> state) {
		Set<Map<Object, Object>> ret = new HashSet<>();
		for (Flower f : flowers) {
			ret.addAll(f.eval(state));
		}
		return ret;
	}

}

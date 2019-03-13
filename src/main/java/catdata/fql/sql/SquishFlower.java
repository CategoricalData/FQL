package catdata.fql.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         Syntax for projecting out and doubling up a guid column
 */
public class SquishFlower extends Flower {

	private final String name;

	public SquishFlower(String name) {
        this.name = name;
	}

	@Override
	public String toPSM() {
		return "SELECT guid AS c0, guid as c1 FROM " + name;
	}

	@Override
	public Set<Map<Object, Object>> eval(
			Map<String, Set<Map<Object, Object>>> state) {
		Set<Map<Object, Object>> v = state.get(name);

		Set<Map<Object, Object>> ret = new HashSet<>();

		for (Map<Object, Object> row : v) {
			Map<Object, Object> newrow = new HashMap<>();
			newrow.put("c0", row.get("guid"));
			newrow.put("c1", row.get("guid"));
			ret.add(newrow);
		}
		return ret;
	}

}

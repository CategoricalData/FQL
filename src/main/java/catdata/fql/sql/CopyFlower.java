package catdata.fql.sql;

import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 *
 *         A Select-from-where that just copies a table.
 */
public class CopyFlower extends Flower {

	private final String name;
	private final String c0;
	private final String c1;

	public CopyFlower(String name, String c0, String c1) {
        this.name = name;
		this.c0 = c0;
		this.c1 = c1;
	}

	@Override
	public String toPSM() {
		return "SELECT " + c0 + "," + c1 + " FROM " + name;
	}

	@Override
	public Set<Map<Object, Object>> eval(Map<String, Set<Map<Object, Object>>> state) {
		return state.get(name);
	}
}

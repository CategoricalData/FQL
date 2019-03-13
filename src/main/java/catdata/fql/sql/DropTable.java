package catdata.fql.sql;

import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         Drop table statements.
 */
public class DropTable extends PSM {

	private final String name;

	public DropTable(String name) {
		this.name = name;
	}

	@Override
	public String toPSM() {
		return "DROP TABLE " + name;
	}

	@Override
	public void exec(PSMInterp interp, Map<String, Set<Map<Object, Object>>> state) {
		if (state.containsKey(name) && state.get(name) == null) {
			throw new RuntimeException("Table does not exist: " + name);
		}
		if (!state.containsKey(name)) {
			throw new RuntimeException("No table to drop: " + name);			
		}
		state.remove(name);
	}
	
	@Override
	public String isSql() {
		return null;
	}


}

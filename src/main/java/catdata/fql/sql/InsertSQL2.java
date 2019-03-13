package catdata.fql.sql;

import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.parse.PrettyPrinter;

/**
 * 
 * @author ryan
 * 
 *         Insert the result of a SQL query into a table.
 */
public class InsertSQL2 extends PSM {

	private final String name;
	private final SQL sql;
	private final List<String> cols;

	public InsertSQL2(String name, SQL sql, List<String> cols) {
		this.name = name;
		this.sql = sql;
		this.cols = cols;
	}

	@Override
	public String toPSM() {
		return "INSERT INTO " + name + "(" + PrettyPrinter.sep0(",", cols) + ") " + sql.toPSM();
	}

	@Override
	public void exec(PSMInterp interp, Map<String, Set<Map<Object, Object>>> state) {
		if (!state.containsKey(name)) {
			throw new RuntimeException("does not contain key " + name + "\n\n" + state + " sql was " + this);
		}
		if (!state.get(name).isEmpty()) {
			throw new RuntimeException(name + ": already " + state.get(name) + " in " + this);
		}
		state.put(name, sql.eval(state));
	}
	
	@Override
	public String isSql() {
		return null;
	}


}

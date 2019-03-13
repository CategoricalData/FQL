package catdata.fql.sql;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         Simple create table statements.
 */
public class SimpleCreateTable extends PSM {

	private final String name;
	private final String attr;
	private final boolean suppress;

	public SimpleCreateTable(String name, String attr, boolean suppress) {
		this.name = name;
		this.attr = attr;
		this.suppress = suppress;
		if (!(attr.equals(PSM.VARCHAR()) || attr.equals(PSM.INTEGER) || attr.equals(PSM.FLOAT))) {
			throw new RuntimeException("bad attribute in " + this + ": " + attr);
		}

	}

	@Override
	public String toPSM() {
		if (suppress) {
			return "";
		}

		return "CREATE TABLE " + name + "(c0 " + PSM.VARCHAR()
				+ " PRIMARY KEY, c1 " + attr + ")";
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		if (state.get(name) != null) {
			throw new RuntimeException("table already exists: " + name + " in "
					+ state);
		}
		state.put(name, new HashSet<>());
	}
	
	@Override
	public String isSql() {
		return null;
	}


}

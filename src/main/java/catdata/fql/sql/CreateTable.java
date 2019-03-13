package catdata.fql.sql;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         Create table statements.
 */
public class CreateTable extends PSM {

	private final String name;
	private final Map<String, String> attrs;

	public CreateTable(String name, Map<String, String> attrs, boolean suppress) {
		this.name = name;
		this.attrs = attrs;
		this.suppress = suppress;
		for (String k : attrs.values()) {
			if (!(k.equals(PSM.VARCHAR()) || k.equals(PSM.INTEGER) || k.equals(PSM.FLOAT))) {
				throw new RuntimeException("bad attribute in " + this + " are " + attrs
						+ " problematic: " + k);
			}
		}
	}

	private final boolean suppress;

	@Override
	public String toPSM() {
		if (suppress) {
			return "";
		}
		String s = "";
		List<String> keys = new LinkedList<>(attrs.keySet());

		for (int i = 0; i < keys.size(); i++) {
			if (i > 0) {
				s += ", ";
			}
			s += keys.get(i) + " " + attrs.get(keys.get(i));
		}

		return "CREATE TABLE " + name + "(" + s + ")";
	}

	@Override
	public void exec(PSMInterp interp, Map<String, Set<Map<Object, Object>>> state) {
		if (state.get(name) != null) {
			throw new RuntimeException("table already exists: " + name + " in " + state);
		}
		state.put(name, new HashSet<>());
	}

	@Override
	public String isSql() {
		return null;
	}

}

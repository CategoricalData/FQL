package catdata.fql.sql;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         Insert values syntax
 */
public class InsertValues extends PSM {

	private final String name;
	private final List<String> attrs;
	private final Set<Map<Object, Object>> values;

	public InsertValues(String name, List<String> attrs,
			Set<Map<Object, Object>> values) {
		this.name = name;
		this.attrs = attrs;
		this.values = values;
		if (values.isEmpty()) {
			throw new RuntimeException();
		}
	}

	/*
	 * INSERT INTO TABLE (column-a, [column-b, ...]) VALUES ('value-1a',
	 * ['value-1b', ...]), ('value-2a', ['value-2b', ...]),
	 */
	@Override
	public String toPSM() {
		
		String attrsStr = "";
		for (int i = 0; i < attrs.size(); i++) {
			if (i > 0) {
				attrsStr += ", ";
			}
			attrsStr += attrs.get(i);
		}

		String pre = "INSERT INTO " + name + "(" + attrsStr + ") VALUES ";

		String ret = "";
		boolean b = false;
		for (Map<Object, Object> row : values) {
			String rowStr = "";
			for (int j = 0; j < attrs.size(); j++) {
				if (j > 0) {
					rowStr += ", ";
				}
				rowStr += "'" + row.get(attrs.get(j)) + "'";
			}
			if (b) {
				ret += ",";
			}
			b = true;
			ret += " (" + rowStr + ") ";
		}

		return pre + ret.trim();
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		if (!state.get(name).isEmpty()) {
			throw new RuntimeException("table not empty: " + name);
		}
		state.put(name, values);
	}

	@Override
	public String isSql() {
		return null;
	}

}

package catdata.fql.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         PSM for keygeneration
 */
public class InsertKeygen extends PSM {

	String name;
	private String col;
	private String r;
	private List<String> attrs;

	InsertKeygen() {

	}

	public InsertKeygen(String name, String col, String r, List<String> attrs) {
		this.col = col;
		this.r = r;
		this.name = name;
		this.attrs = attrs;
	}

	@Override
	public String toPSM() {
		if (attrs.isEmpty()) {
			return "INSERT INTO " + name + "(" + col
					+ ") VALUES (@guid := @guid+1)";
		}
		String a = "";
		int i = 0;
		for (String attr : attrs) {
			if (i++ > 0) {
				a += ",";
			}
			a += attr;
		}

		return "INSERT INTO " + name + "(" + a + ", " + col + " ) SELECT " + a
				+ ", @guid:=@guid+1 AS " + col + " FROM " + r;

	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		if (!state.containsKey(name)) {
			throw new RuntimeException(name + "\n\n" + state);
		}
		if (state.get(r) == null) {
			throw new RuntimeException(r + "\n\n" + state);
		}
		if (!state.get(name).isEmpty()) {
			throw new RuntimeException(toString());
		}

		Set<Map<Object, Object>> ret = new HashSet<>();
		if (attrs.isEmpty()) {
			Map<Object, Object> m = new HashMap<>();
			ret.add(m);
			m.put(col, Integer.toString(++interp.guid));
			state.put(name, ret);
		} else {
			for (Map<Object, Object> row : state.get(r)) {
				Map<Object, Object> row0 = new HashMap<>();
				for (Object s : row.keySet()) {
					row0.put(s, row.get(s));
				}
				row0.put(col, Integer.toString(++interp.guid));
				ret.add(row0);
			}
			state.put(name, ret);
		}
	}
	
	@Override
	public String isSql() {
		return null;
	}


}

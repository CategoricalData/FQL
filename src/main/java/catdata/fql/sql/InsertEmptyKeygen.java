package catdata.fql.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 * 
 *         The unit for keygen, since SQL doesn't allow empty select-from-wheres
 */
public class InsertEmptyKeygen extends InsertKeygen {

	public InsertEmptyKeygen(String name) {
		this.name = name;
	}

	@Override
	public String toPSM() {
		return "INSERT INTO " + name + " VALUES (@guid:=@guid+1)";
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		if (!state.containsKey(name)) {
			throw new RuntimeException(toString());
		}
		if (!state.get(name).isEmpty()) {
			throw new RuntimeException(toString());
		}
		Set<Map<Object, Object>> ret = new HashSet<>();
		Map<Object, Object> m = new HashMap<>();
		m.put("guid", Integer.toString(++interp.guid));
		ret.add(m);

		state.put(name, ret);
	}

}

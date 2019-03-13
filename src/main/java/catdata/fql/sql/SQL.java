package catdata.fql.sql;

import java.util.Map;
import java.util.Set;

/**
 * 
 * @author ryan
 *
 * SQL expressions
 */
public abstract class SQL {

	public abstract Set<Map<Object, Object>> eval(
			Map<String, Set<Map<Object, Object>>> state);

	public abstract String toPSM();

	@Override
	public String toString() {
		return toPSM();
	}

}

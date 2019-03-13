package catdata.fql.sql;

import java.util.Map;
import java.util.Set;

import catdata.ide.DefunctGlobalOptions;

/**
 * 
 * @author ryan
 *
 * Class for PSM, which raps SQL.
 */
public abstract class PSM {
	
	public abstract String isSql();
	
	public static final String INTEGER = "INTEGER";
	public static final String FLOAT = "FLOAT";
	public static String VARCHAR() {
		return "VARCHAR(" + DefunctGlobalOptions.debug.fql.varlen + ")";
	}

	public  abstract void exec(PSMInterp interp, Map<String, Set<Map<Object, Object>>> state);
	
	public  abstract String toPSM(); 

	@Override
	public String toString() {
		return toPSM();
	}
}

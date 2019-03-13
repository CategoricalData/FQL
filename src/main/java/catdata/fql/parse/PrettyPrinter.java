package catdata.fql.parse;

import java.util.List;

/**
 * 
 * @author ryan
 * 
 *         Some helper pretty printing methods
 */
public class PrettyPrinter {

	public static Object q(Object o) {
		if (o == null) {
			return "!!!NULL!!!";
		}
		String s = o.toString();
		if ((s.contains("\t") || s.contains("\n") || s.contains("\r") || s.contains(" ") || s.contains("-") || s.isEmpty()) && !s.contains("\"")) {
			return "\"" + s + "\"";
		}
		return s;
	}
	
	public static String sep0(String delim, List<String> o) {
		if (o.isEmpty()) {
			return "";
		}
		if (o.size() == 1) {
			return o.get(0);
		}
		String s = o.get(0);
		for (int i = 1; i < o.size(); i++) {
			s += delim + o.get(i);
		}
		return s;
	}

	

}

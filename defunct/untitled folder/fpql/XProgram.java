package catdata.fpql;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.LineException;
import catdata.Pair;
import catdata.Prog;
import catdata.Triple;

public class XProgram implements Prog {
	
	public Map<String, String> kinds() {
		Map<String, String> ret = new HashMap<>();
		for (String k : order) {
			String x = exps.get(k).kind(exps);
			ret.put(k, x);
		}
		return ret;
	}
	
	private Map<String, Pair<String, String>> types() {
		Map<String, Pair<String, String>> ret = new HashMap<>();
		for (String k : order) {
			try {
				String s1 = "?";
				String s2 = "?";
				Pair<XExp, XExp> x = exps.get(k).type(exps);
				if (x != null) {
					String s1x = x.first.toString();
					String s2x = x.second.toString();
					if (s1x.length() > 32) {
						s1x = "...";
					}
					if (s2x.length() > 32) {
						s2x = "...";
					}
					s1 = s1x;
					s2 = s2x;
				}
				ret.put(k, new Pair<>(s1, s2));
		
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException("Type Error in " + k + ":" + ex.getMessage());
			}
		}
		return ret;
	}
	
	public String typeReport() {
		String ret = "";
		Map<String, Pair<String, String>> types = types();
		for (String k : order) {
			Pair<String, String> v = types.get(k);
			ret += k + " : " + v.first + " -> " + v.second + "\n";
		}
		return ret;
	} 

	
	public final List<String> order = new LinkedList<>();
	private final LinkedHashMap<String, Integer> lines = new LinkedHashMap<>();
	public final LinkedHashMap<String, XExp> exps = new LinkedHashMap<>();
	
	public XProgram(List<Triple<String, Integer, XExp>> decls) {
			Set<String> seen = new HashSet<>();
			for (Triple<String, Integer, XExp> decl : decls) { 
				checkDup(seen, decl.first);
				exps.put(decl.first, decl.third);
				lines.put(decl.first, decl.second);
				order.add(decl.first);				
			}
	}

	private static void checkDup(Set<String> seen, String name)
			throws LineException {
		if (seen.contains(name)) {
			throw new RuntimeException("Duplicate name: " + name);
		}
		seen.add(name);
	}

	@Override
	public Integer getLine(String s) {
		return lines.get(s);
	}
	
	private Map<String, Pair<String, String>> tys = null;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Pair getType(String s) {
		if (tys == null) {
			return new Pair("?", "?");
			
		}
		Pair p = tys.get(s);
		if (p == null) {
			return new Pair("?", "?");
		}
		return tys.get(s);
	}

	public void doTypes() {
		tys = types();
	}

	@Override
	public Collection<String> keySet() {
		return order;
	}

}

package catdata.fqlpp;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import catdata.LineException;
import catdata.Prog;


@SuppressWarnings("serial")
public class FQLPPProgram implements Serializable, Prog {


	public static class NewDecl {
		TransExp trans;
		final String name;
		SetExp set;
		FnExp fn;
		CatExp cat;
		FunctorExp ftr;		
		final Integer line;
		
		public static NewDecl transDecl(String name, Integer line, TransExp trans) {
			NewDecl ret = new NewDecl(name, line);
			ret.trans = trans;
			return ret;
		}

		public static NewDecl setDecl(String name, Integer line, SetExp set) {
			NewDecl ret = new NewDecl(name, line);
			ret.set = set;
			return ret;
		}

		public static NewDecl fnDecl(String name, Integer line, FnExp fn) {
			NewDecl ret = new NewDecl(name, line);
			ret.fn = fn;
			return ret;
		}
		
		public static NewDecl catDecl(String name, Integer line, CatExp cat) {
			NewDecl ret = new NewDecl(name, line);
			ret.cat = cat;
			return ret;
		}
		
		public static NewDecl ftrDecl(String name, Integer line, FunctorExp ftr) {
			NewDecl ret = new NewDecl(name, line);
			ret.ftr = ftr;
			return ret;
		}


		public NewDecl(String name, Integer line) {
			this.name = name;
			this.line = line;
		}
	}
	
	public LinkedHashMap<String, SetExp> sets = new LinkedHashMap<>();
	public LinkedHashMap<String, FnExp> fns = new LinkedHashMap<>();
	public LinkedHashMap<String, CatExp> cats = new LinkedHashMap<>();
	public LinkedHashMap<String, FunctorExp> ftrs = new LinkedHashMap<>();
	public LinkedHashMap<String, TransExp> trans = new LinkedHashMap<>();
	public List<String> order = new LinkedList<>();
	private LinkedHashMap<String, Integer> lines = new LinkedHashMap<>();
	//copies
	public FQLPPProgram(FQLPPProgram p) {
        sets = new LinkedHashMap<>(p.sets);
        fns = new LinkedHashMap<>(p.fns);
        cats = new LinkedHashMap<>(p.cats);
        ftrs = new LinkedHashMap<>(p.ftrs);
        order = new LinkedList<>(p.order);
        lines = new LinkedHashMap<>(p.lines);
        trans = new LinkedHashMap<>(p.trans);
	}

	public FQLPPProgram(List<NewDecl> decls) {
		Set<String> seen = new HashSet<>();
		
		for (NewDecl decl : decls) {
			if (decl.set != null) {
				checkDup(seen, decl.name, "set");
				sets.put(decl.name, decl.set);
			} else if (decl.fn != null) {
				checkDup(seen, decl.name, "function");
				fns.put(decl.name, decl.fn);
			} else if (decl.cat != null) {
				checkDup(seen, decl.name, "category");
				cats.put(decl.name, decl.cat);
			} else if (decl.ftr != null) {
				checkDup(seen, decl.name, "functor");
				ftrs.put(decl.name, decl.ftr);
			} else if (decl.trans != null) {
				checkDup(seen, decl.name, "transform");
				trans.put(decl.name, decl.trans);
			} else {
				throw new RuntimeException("decl was " + decl);
			}
			lines.put(decl.name, decl.line);
			order.add(decl.name);				
		}
	}


	@Override
	public String toString() {
		return "FQLProgram [sets=" + sets + ", fns=" + fns + ", order=" + order
				+ ", lines=" + lines + "]";
	}


	private static void checkDup(Set<String> seen, String name, @SuppressWarnings("unused") String s)
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

	@Override
	public Collection<String> keySet() {
		return order;
	}

}

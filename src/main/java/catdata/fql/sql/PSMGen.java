package catdata.fql.sql;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.fql.Fn;
import catdata.Pair;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.cat.CommaCat;
import catdata.fql.cat.FinCat;
import catdata.fql.cat.FinFunctor;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

/**
 * 
 * @author ryan
 * 
 *         PSM generator.
 */
public class PSMGen {

	public static List<PSM> guidify(String pre0, Signature sig) {
		return guidify(pre0, sig, false);
	}

	public static List<PSM> guidify(String pre0, Signature sig, boolean remember) {
		List<PSM> ret = new LinkedList<>();

		Map<String, String> guid_attrs = new HashMap<>();
		Map<String, String> twocol_attrs = new HashMap<>();

		twocol_attrs.put("c0", PSM.VARCHAR());
		twocol_attrs.put("c1", PSM.VARCHAR());
		guid_attrs.put("c0", PSM.VARCHAR());
		guid_attrs.put("c1", PSM.VARCHAR());
		guid_attrs.put("guid", PSM.VARCHAR());

		List<String> attrs_foo = new LinkedList<>();
		attrs_foo.add("c0");
		attrs_foo.add("c1");

		for (Node n : sig.nodes) {
			String pre = pre0 + "_" + n;

			// make new table with GUID
			ret.add(new CreateTable(pre + "_guid", guid_attrs, false));
			ret.add(new InsertKeygen(pre + "_guid", "guid", pre, attrs_foo));

			// make a substitution table
			ret.add(new CreateTable(pre + "_subst", twocol_attrs, false));
			ret.add(new InsertSQL(pre + "_subst", makeSubst(pre0, n), "c0", "c1"));

			ret.add(new CreateTable(pre + "_subst_inv", twocol_attrs, false));
			ret.add(new InsertSQL(pre + "_subst_inv", invertSubst(pre0, n), "c0", "c1"));

			// create a new table that applies the substitution
			ret.add(new CreateTable(pre + "_applied", twocol_attrs, false));
			ret.add(new InsertSQL(pre + "_applied", makeApplyNode(pre0, n), "c0", "c1"));

			// drop guid table
			ret.add(new DropTable(pre + "_guid"));

			// drop original table
			ret.add(new DropTable(pre));

			// copy the new table
			ret.add(new SimpleCreateTable(pre, PSM.VARCHAR(), false));
//			ret.add(new CreateTable(pre, twocol_attrs, false));
			ret.add(new InsertSQL(pre, new CopyFlower(pre + "_applied", "c0", "c1"), "c0", "c1"));

			// drop the new table
			ret.add(new DropTable(pre + "_applied"));
		}

		for (Edge e : sig.edges) {
			String pre = pre0 + "_" + e.name;

			// create a new table that applies the substitution
			ret.add(new CreateTable(pre + "_applied", twocol_attrs, false));
			ret.add(new InsertSQL(pre + "_applied", makeApplyEdge(pre0, e), "c0", "c1"));

			// drop original table
			ret.add(new DropTable(pre));

			// copy the new table
			ret.add(new SimpleCreateTable(pre, PSM.VARCHAR(), false));
//			ret.add(new CreateTable(pre, twocol_attrs, false));
			ret.add(new InsertSQL(pre, new CopyFlower(pre + "_applied", "c0", "c1"), "c0", "c1"));

			// drop the new table
			ret.add(new DropTable(pre + "_applied"));

		}

		for (Attribute<Node> a : sig.attrs) {
			String pre = pre0 + "_" + a.name;

			// create a new table that applies the substitution

			ret.add(new CreateTable(pre + "_applied", colattrs(a), false));
			ret.add(new InsertSQL(pre + "_applied", makeAttr(pre0, a), "c0", "c1"));

			// drop original table
			ret.add(new DropTable(pre));

			// copy the new table
			ret.add(new SimpleCreateTable(pre, a.target.psm(), false));
//			ret.add(new CreateTable(pre, twocol_attrs, false));
			ret.add(new InsertSQL(pre, new CopyFlower(pre + "_applied", "c0", "c1"), "c0", "c1"));

			// drop the new table
			ret.add(new DropTable(pre + "_applied"));

		}

		// same for attributes, but one sided

		if (!remember) {
			for (Node n : sig.nodes) {
				String pre = pre0 + "_" + n;
				ret.add(new DropTable(pre + "_subst"));
				ret.add(new DropTable(pre + "_subst_inv"));
			}
		}
		return ret;
	}

	private static Map<String, String> colattrs(Attribute<Node> a) {
		Map<String, String> twocol_attrs = new HashMap<>();
		twocol_attrs.put("c0", PSM.VARCHAR());
		twocol_attrs.put("c1", a.target.psm());
		return twocol_attrs;
	}

	private static SQL invertSubst(String pre0, Node n) {
		String pre = pre0 + "_" + n;
		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
		select.put("c0", new Pair<>(pre + "_subst", "c1"));
		select.put("c1", new Pair<>(pre + "_subst", "c0"));

		Map<String, String> from = new HashMap<>();
		from.put(pre + "_subst", pre + "_subst");

		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();

		Flower f = new Flower(select, from, where);

		return f;
	}

	private static SQL makeApplyEdge(String i, Edge e) {
		String src = e.source.string;
		String dst = e.target.string;

		SQL f = compose(i + "_" + src + "_subst_inv", i + "_" + e.name, i + "_" + dst + "_subst");

		return f;
	}

	private static SQL makeAttr(String i, Attribute<Node> a) {
		String src = a.source.string;

		SQL f = compose(i + "_" + src + "_subst_inv", i + "_" + a.name);

		return f;
	}

	private static SQL makeApplyNode(String i, Node n) {
		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();

		Map<String, String> from = new HashMap<>();
		from.put(i + "_" + n.string + "_guid", i + "_" + n.string + "_guid");

		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
		select.put("c0", new Pair<>(i + "_" + n.string + "_guid", "guid"));
		select.put("c1", new Pair<>(i + "_" + n.string + "_guid", "guid"));

		return new Flower(select, from, where);
	}

	private static SQL makeSubst(String i, Node n) {
		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();

		Map<String, String> from = new HashMap<>();
		from.put(i + "_" + n.string + "_guid", i + "_" + n.string + "_guid");

		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
		select.put("c0", new Pair<>(i + "_" + n.string + "_guid", "c0"));
		select.put("c1", new Pair<>(i + "_" + n.string + "_guid", "guid"));

		return new Flower(select, from, where);
	}

	public static String prettyPrint(List<PSM> l) {
		String ret = "";
		for (PSM p : l) {
			String s = p.toPSM();
			if (s.trim().isEmpty()) {
				continue;
			}
			ret += p.toPSM() + ";\n\n";
		}
		return ret;
	}

	public static List<PSM> dropTables(String name, Signature sig) {
		List<PSM> ret = new LinkedList<>();

		for (Node n : sig.nodes) {
			ret.add(new DropTable(name + "_" + n.string));
		}
		for (Edge e : sig.edges) {
			ret.add(new DropTable(name + "_" + e.name));
		}
		for (Attribute<Node> a : sig.attrs) {
			ret.add(new DropTable(name + "_" + a.name));
		}

		return ret;
	}

	public static List<PSM> doConst(String dst, Signature sig,
			List<Pair<String, List<Pair<Object, Object>>>> data) {
		List<PSM> ret = new LinkedList<>();

		for (Node n : sig.nodes) {
			PSM x = populateTable(dst, n.string, lookup(data, n.string));
			if (x != null) {
				ret.add(x);
			}
		}
		for (Edge e : sig.edges) {
			PSM x = populateTable(dst, e.name, lookup(data, e.name));
			if (x != null) {
				ret.add(x);
			}
		}
		for (Attribute<Node> a : sig.attrs) {
			PSM x = populateTable(dst, a.name, lookup(data, a.name));
			if (x != null) {
				ret.add(x);
			}
		}

		return ret;
	}

	private static Set<Pair<Object, Object>> lookup(
            List<Pair<String, List<Pair<Object, Object>>>> data, String str) {
		for (Pair<String, List<Pair<Object, Object>>> k : data) {
			if (k.first.equals(str)) {
				return new HashSet<>(k.second);
			}
		}
		throw new RuntimeException();
	}

	public static List<PSM> doExternal(Signature sig, String in, String out) {
		List<PSM> ret = new LinkedList<>();

		ret.addAll(makeTables(in, sig, true));
		// ret.addAll(makeTables(out, sig, false));

		for (Node n : sig.nodes) {
			ret.add(new InsertSQL(out + "_" + n.string, new CopyFlower(in + "_"
					+ n.string, "c0", "c1"), "c0", "c1"));
		}
		for (Edge e : sig.edges) {
			ret.add(new InsertSQL(out + "_" + e.name, new CopyFlower(in + "_"
					+ e.name, "c0", "c1"), "c0", "c1"));
		}
		for (Attribute<Node> a : sig.attrs) {
			ret.add(new InsertSQL(out + "_" + a.name, new CopyFlower(in + "_"
					+ a.name, "c0", "c1"), "c0", "c1"));
		}

		return ret;
	}

	private static PSM populateTable(String iname, String tname,
			Set<Pair<Object, Object>> data) {

		List<String> attrs = new LinkedList<>();
		attrs.add("c0");
		attrs.add("c1");
		Set<Map<Object, Object>> values = new HashSet<>();

		for (Pair<Object, Object> row : data) {
			Map<Object, Object> m = new HashMap<>();
			m.put("c0", row.first);
			m.put("c1", row.second);
			values.add(m);
		}
		if (!values.isEmpty()) {
			return new InsertValues(iname + "_" + tname, attrs, values);
		}
		return null;
	}

	
	public static List<PSM> makeTables(String name, Signature sig,
			boolean suppress) {
		List<PSM> ret = new LinkedList<>();

		for (Node n : sig.nodes) {
			ret.add(new SimpleCreateTable(name + "_" + n.string, PSM.VARCHAR(), suppress));
		}
		for (Edge e : sig.edges) {
			ret.add(new SimpleCreateTable(name + "_" + e.name, PSM.VARCHAR(), suppress));
		}
		for (Attribute<Node> a : sig.attrs) {
			ret.add(new SimpleCreateTable(name + "_" + a.name, a.target.psm(), suppress));
		}

		return ret;
	} 
	

	public static List<PSM> delta(Mapping m, String src, String dst) {
		Map<String, SQL> ret = new HashMap<>();
		for (Entry<Node, Node> n : m.nm.entrySet()) {
			ret.put(dst + "_" + n.getKey().string,
					new CopyFlower(src + "_" + n.getValue().string, "c0", "c1"));
		}
		for (Entry<Edge, Path> e : m.em.entrySet()) {
			ret.put(dst + "_" + e.getKey().name, compose(src, e.getValue()));
		}
		for (Entry<Attribute<Node>, Attribute<Node>> a : m.am.entrySet()) {
			ret.put(dst + "_" + a.getKey().name,
					new CopyFlower(src + "_" + a.getValue().name, "c0", "c1"));
		}
		List<PSM> ret0 = new LinkedList<>();
		for (String k : ret.keySet()) {
			SQL v = ret.get(k);
			ret0.add(new InsertSQL(k, v, "c0", "c1"));
		}

		return ret0;
	}

	public static Flower compose(String... p) {
		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
		Map<String, String> from = new HashMap<>();
		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();

		from.put("t0", p[0]);

		for (int i = 1; i < p.length; i++) {
			from.put("t" + i, p[i]);
			where.add(new Pair<>(new Pair<>("t" + (i - 1), "c1"), new Pair<>(
					"t" + i, "c0")));
		}

		select.put("c0", new Pair<>("t0", "c0"));
		select.put("c1", new Pair<>("t" + (p.length - 1), "c1"));

		return new Flower(select, from, where);
	}

	public static Flower compose(String pre, Path p) {
		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
		Map<String, String> from = new HashMap<>();
		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();

		from.put("t0", pre + "_" + p.source.string);

		int i = 1;
		for (Edge e : p.path) {
			from.put("t" + i, pre + "_" + e.name);
			where.add(new Pair<>(new Pair<>("t" + (i - 1), "c1"), new Pair<>(
					"t" + i, "c0")));
			i++;
		}

		select.put("c0", new Pair<>("t0", "c0"));
		select.put("c1", new Pair<>("t" + (i - 1), "c1"));

		return new Flower(select, from, where);
	}

	public static List<PSM> SIGMA(Mapping F, String pre, String inst) {
		List<PSM> ret = new LinkedList<>();

		ret.add(new FullSigma(F, pre, inst));

		return ret;
	}

	public static List<PSM> sigma(Mapping F, String pre, String inst)
			throws FQLException {
		Signature C = F.source;
		Signature D = F.target;
		List<PSM> ret = new LinkedList<>();

		if (!FinFunctor.isDiscreteOpFib(F.toFunctor2().first)) {
			throw new FQLException("Not a discrete op-fibration" /* + F */);
		}

		for (Node d : D.nodes) {
			List<Flower> tn = new LinkedList<>();
			for (Node c : C.nodes) {
				if (F.nm.get(c).equals(d)) {
					tn.add(new CopyFlower(inst + "_" + c.string, "c0", "c1"));
				}
			}

			if (tn.isEmpty()) {
				continue;
			}
			SQL y = foldUnion(tn);
			ret.add(new InsertSQL(pre + "_" + d.string, y, "c0", "c1"));
		}

		for (Edge e : D.edges) {
			Node d = e.source;
			// Node d0 = e.target;
			List<Flower> tn = new LinkedList<>();
			for (Node c : C.nodes) {
				if (F.nm.get(c).equals(d)) {
					Path pc = findEquiv(c, F, e);
					Flower q = compose(inst, pc);
					tn.add(q);
				}
			}
			if (tn.isEmpty()) {
				continue;
			}
			SQL y = foldUnion(tn);
			ret.add(new InsertSQL(pre + "_" + e.name, y, "c0", "c1"));
		}

		for (Attribute<Node> a : D.attrs) {
			Node d = a.source;
			// Node d0 = e.target;
			List<Flower> tn = new LinkedList<>();
			for (Node c : C.nodes) {
				if (F.nm.get(c).equals(d)) {
					Attribute<Node> pc = findEquiv(c, F, a);
					Flower q = new CopyFlower(inst + "_" + pc.name, "c0", "c1");
					tn.add(q);
				}
			}

			if (tn.isEmpty()) {
				continue;
			}
			SQL y = foldUnion(tn);
			ret.add(new InsertSQL(pre + "_" + a.name, y, "c0", "c1"));
		}

		return ret;
	}

	private static SQL foldUnion(List<Flower> tn) {
		if (tn.isEmpty()) {
			throw new RuntimeException("Empty Union");
		}
		if (tn.size() == 1) {
			return tn.get(0);
		}
		return new Union(tn);
	}

	private static Attribute<Node> findEquiv(Node c, Mapping f,
			Attribute<Node> a) throws FQLException {
		// Signature C = f.source;
		for (Attribute<Node> peqc : f.source.attrs) {
			if (!peqc.source.equals(c)) {
				continue;
			}
			if (f.am.get(peqc).equals(a)) {
				return peqc;
			}
		}
		throw new FQLException("Could not find attribute mapping to " + a);
	}

	private static Path findEquiv(Node c, Mapping f, Edge e)
			throws FQLException {
		Signature C = f.source;
		Signature D = f.target;
		FinCat<Node, Path> C0 = C.toCategory2().first;
		for (Arr<Node, Path> peqc : C0.arrows) {
			Path path = peqc.arr;
			// Path path = new Path(f.source, p);
			if (!path.source.equals(c)) {
				continue;
			}
			Path path_f = f.appy(D, path);
			Fn<Path, Arr<Node, Path>> F = D.toCategory2().second;
			if (F.of(path_f).equals(F.of(new Path(D, e)))) {
				return path;
			}
		}
		throw new FQLException("Could not find path mapping to " + e);
	}

	public static Pair<List<PSM>, Map<String, Triple<Node, Node, Arr<Node, Path>>[]>> pi(
			Mapping F0, String src, String dst) throws FQLException {
		tempTables = 0;
		Signature D0 = F0.target;
		Signature C0 = F0.source;
		Pair<FinCat<Node, Path>, Fn<Path, Arr<Node, Path>>> kkk = D0
				.toCategory2();
		FinCat<Node, Path> D = kkk.first;
		FinCat<Node, Path> C = C0.toCategory2().first;
		FinFunctor<Node, Path, Node, Path> F = F0.toFunctor2().first;
		List<PSM> ret = new LinkedList<>();

		Map<String, Triple<Node, Node, Arr<Node, Path>>[]> colmap = new HashMap<>();
		Map<String, Attribute<Node>[]> amap = new HashMap<>();
		List<Node> doNotDrop = new LinkedList<>();
		for (Node d0 : D.objects) {
			CommaCat<Node, Path, Node, Path, Node, Path> B = doComma(D, C, F,
					d0, D0);
		
			Map<Triple<Node, Node, Arr<Node, Path>>, String> xxx1 = new HashMap<>();
			Map<Pair<Arr<Node, Path>, Arr<Node, Path>>, String> xxx2 = new HashMap<>();
			List<PSM> xxx3 = deltaX(src, xxx1, xxx2, B.projB);
			ret.addAll(xxx3);

			Triple<Flower, Triple<Node, Node, Arr<Node, Path>>[], Attribute<Node>[]> xxx = lim(
					src, C0, D, B, xxx1, xxx2);
	
			// comma cat is empty, need unit for product
			if (xxx == null) {
				doNotDrop.add(d0);
				Map<String, String> attrs2 = new HashMap<>();
				attrs2.put("guid", PSM.VARCHAR());

				ret.add(new CreateTable(dst + "_" + d0.string + "_limit",
						attrs2, false));
				ret.add(new InsertEmptyKeygen(dst + "_" + d0.string + "_limit"));
				ret.add(new InsertSQL(dst + "_" + d0.string, new SquishFlower(
						dst + "_" + d0.string + "_limit"), "c0", "c1"));

				@SuppressWarnings("unchecked")
				Triple<Node, Node, Arr<Node, Path>>[] cols = new Triple[0];
				colmap.put(d0.string, cols);
				continue;
			}

			Triple<Node, Node, Arr<Node, Path>>[] cols = xxx.second;

			Flower r = xxx.first;
			for (Attribute<Node> a : D0.attrsFor(d0)) {
				List<Attribute<Node>> ls = new LinkedList<>();
				for (Attribute<Node> aa : C0.attrs) {
					if (F.am.get(aa).equals(a)) {
						ls.add(aa);
					}
				}
				for (int jj = 1; jj < ls.size(); jj++) {
					int xxx02 = cnamelkp(xxx.third, ls.get(0));
					int xxx04 = cnamelkp(xxx.third, ls.get(jj));
					r.where.add(new Pair<>(new Pair<>("t"
							+ (xxx02 + xxx.second.length), "c1"), new Pair<>(
							"t" + (xxx04 + xxx.second.length), "c1")));
				}
			}

			colmap.put(d0.string, cols);
			amap.put(d0.string, xxx.third);

			Map<String, String> attrs1 = new HashMap<>();
			for (int i = 0; i < xxx.second.length; i++) {
				attrs1.put("c" + i, PSM.VARCHAR());
			}
			for (int j = 0; j < xxx.third.length; j++) {
				attrs1.put("c" + (xxx.second.length + j),
						xxx.third[j].target.psm());
			}
			Map<String, String> attrs2 = new HashMap<>(attrs1);
			attrs2.put("guid", PSM.VARCHAR());

			List<String> attcs = new LinkedList<>(attrs1.keySet());

			ret.add(new CreateTable(dst + "_" + d0.string + "_limnoguid",
					attrs1, false));
			ret.add(new InsertSQL2(dst + "_" + d0.string + "_limnoguid", r, new LinkedList<>(r.select.keySet())));

			ret.add(new CreateTable(dst + "_" + d0.string + "_limit", attrs2,
					false));
			ret.add(new InsertKeygen(dst + "_" + d0.string + "_limit", "guid",
					dst + "_" + d0.string + "_limnoguid", attcs));

			// craeted by createTables
			// ret.add(new CreateTable(dst + "_" + d0.string, twocol_attrs));
			ret.add(new InsertSQL(dst + "_" + d0.string, new SquishFlower(dst
					+ "_" + d0.string + "_limit"), "c0", "c1"));
		}

		for (Edge s : F0.target.edges) {
			Node dA = s.source;

			Node dB = s.target;

			String q2 = dB.string;
			String q1 = dA.string;

			Triple<Node, Node, Arr<Node, Path>>[] q2cols = colmap.get(q2);
			Triple<Node, Node, Arr<Node, Path>>[] q1cols = colmap.get(q1);

			if (q2cols == null) {
				throw new RuntimeException("Cannot find " + q2 + " in "
						+ colmap);
			}

			List<Pair<Pair<String, String>, Pair<String, String>>> where = subset(
					D, kkk.second.of(new Path(D0, s)), dst, q2cols, q1cols, q2,
					q1);
			Map<String, String> from = new HashMap<>();
			from.put(dst + "_" + q1 + "_limit_1", dst + "_" + q1 + "_limit");
			from.put(dst + "_" + q2 + "_limit_2", dst + "_" + q2 + "_limit");

			LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
			select.put("c0", new Pair<>(dst + "_" + q1 + "_limit_1", "guid"));
			select.put("c1", new Pair<>(dst + "_" + q2 + "_limit_2", "guid"));

			Flower f = new Flower(select, from, where);

			ret.add(new InsertSQL(dst + "_" + s.name, f, "c0", "c1"));

		}

		for (Attribute<Node> a : F0.target.attrs) {
			int i = colmap.get(a.source.string).length;
			Attribute<Node>[] y = amap.get(a.source.string);
			if (y == null) {
				throw new FQLException("Attribute mapping not surjective "
						+ a.source.string);
			}
			boolean found = false;
			int u = 0;
			// int j = -1;
			List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
			LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
			Map<String, String> from = new HashMap<>();
			List<Integer> xxx = new LinkedList<>();
			for (Attribute<Node> b : y) {
				if (!F0.am.get(b).equals(a)) {
					u++;
					continue;
				}
				found = true;
				xxx.add(u);
				u++;
			}
			if (!found) {
				throw new FQLException("Attribute mapping not found " + a);
			}
			from.put(dst + "_" + a.source + "_limit", dst + "_" + a.source
					+ "_limit");
			select.put("c0",
					new Pair<>(dst + "_" + a.source + "_limit", "guid"));
			for (int jj = 1; jj < xxx.size(); jj++) {
				where.add(new Pair<>(
						new Pair<>(dst + "_" + a.source + "_limit", "c"
								+ (xxx.get(0) + i)), new Pair<>(dst + "_"
								+ a.source + "_limit", "c" + (xxx.get(jj) + i))));
			}
			select.put("c1", new Pair<>(dst + "_" + a.source + "_limit", "c"
					+ (xxx.get(0) + i)));
			Flower f = new Flower(select, from, where);

			ret.add(new InsertSQL(dst + "_" + a.name, f, "c0", "c1"));
			// project guid and u+i
		}

		for (Node d0 : D.objects) {
			if (doNotDrop.contains(d0)) {
				continue;
			}
			ret.add(new DropTable(dst + "_" + d0.string + "_limnoguid"));
		}

		for (int ii = 0; ii < tempTables; ii++) {
			ret.add(new DropTable("temp" + ii));
		}

		return new Pair<>(ret, colmap);
	}

	private static List<Pair<Pair<String, String>, Pair<String, String>>> subset(
			FinCat<Node, Path> cat, Arr<Node, Path> e, String pre,
			Triple<Node, Node, Arr<Node, Path>>[] q2cols,
			Triple<Node, Node, Arr<Node, Path>>[] q1cols, String q2name,
			String q1name) {
		List<Pair<Pair<String, String>, Pair<String, String>>> ret = new LinkedList<>();
		// turn e into arrow e', compute e' ; q2col, look for that
		/* a: */for (int i = 0; i < q2cols.length; i++) {
			boolean b = false;
			for (int j = 0; j < q1cols.length; j++) {
				Triple<Node, Node, Arr<Node, Path>> q2c = q2cols[i];
				Triple<Node, Node, Arr<Node, Path>> q1c = q1cols[j];
		
				if (q1c.third.equals(cat.compose(e, q2c.third))
						&& q1c.second.equals(q2c.second)) {
					Pair<Pair<String, String>, Pair<String, String>> retadd = new Pair<>(
							new Pair<>(pre + "_" + q1name + "_limit_1", "c" + j),
							new Pair<>(pre + "_" + q2name
									+ "_limit_2", "c" + i));
					ret.add(retadd);
					b = true;
				}
			}
			if (b)
				continue;
			String xxx = "";
			for (Triple<Node, Node, Arr<Node, Path>> yyy : q1cols) {
				xxx += ", " + yyy;
			}
			throw new RuntimeException("No col " + q2cols[i] + " in " + xxx
					+ " pre " + pre);

		}
		return ret;
	}

	private static CommaCat<Node, Path, Node, Path, Node, Path> doComma(
			FinCat<Node, Path> d2, FinCat<Node, Path> c,
			FinFunctor<Node, Path, Node, Path> f, Node d0, @SuppressWarnings("unused") Signature S) {
		FinFunctor<Node, Path, Node, Path> d = FinFunctor.singleton(d2, d0,
				new Arr<>(d2.identities.get(d0).arr, d0, d0));
		CommaCat<Node, Path, Node, Path, Node, Path> B = new CommaCat<>(
				d.srcCat, c, d2, d, f);
		return B;
	}

	public static Flower squish(String s) {
		return new SquishFlower(s);
	}

	@SuppressWarnings("unchecked")
    private static Triple<Flower, Triple<Node, Node, Arr<Node, Path>>[], Attribute<Node>[]> lim(
            String pre, Signature sig, FinCat<Node, Path> cat,
            CommaCat<Node, Path, Node, Path, Node, Path> b,
            Map<Triple<Node, Node, Arr<Node, Path>>, String> map,
            Map<Pair<Arr<Node, Path>, Arr<Node, Path>>, String> map2)
			throws FQLException {

		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
		Map<String, String> from = new HashMap<>();
		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();

		int m = b.objects.size();
	
		if (m == 0) {
			return null;
		}

		int temp = 0;
		Triple<Node, Node, Arr<Node, Path>>[] cnames = new Triple[m];

		List<Attribute<Node>> anames0 = new LinkedList<>();

		for (Triple<Node, Node, Arr<Node, Path>> n : b.objects) {
			from.put("t" + temp, map.get(n));
			cnames[temp] = n;

			select.put("c" + temp, new Pair<>("t" + temp, "c0"));
			temp++;
		}

		for (Triple<Node, Node, Arr<Node, Path>> n : b.objects) {
			if (cat.isId(n.third)) {
				for (Attribute<Node> a : sig.attrsFor(n.second)) {
					anames0.add(a);
					from.put("t" + temp, pre + "_" + a.name);
			
					select.put("c" + temp, new Pair<>("t" + temp, "c1"));

					where.add(new Pair<>(new Pair<>("t" + cnamelkp(cnames, n),
							"c0"), new Pair<>("t" + temp, "c0")));
					temp++;
				}
			}
		}

		// temp = 0; VERY VERY BAD
		for (Arr<Triple<Node, Node, Arr<Node, Path>>, Pair<Arr<Node, Path>, Arr<Node, Path>>> e : b.arrows) {
			if (b.isId(e)) {
				continue;
			}
			from.put("t" + temp, map2.get(e.arr));

			where.add(new Pair<>(new Pair<>("t" + temp, "c0"), new Pair<>("t"
					+ cnamelkp(cnames, e.src), "c0")));
			where.add(new Pair<>(new Pair<>("t" + temp, "c1"), new Pair<>("t"
					+ cnamelkp(cnames, e.dst), "c0")));

			temp++;
		}

		Flower f = new Flower(select, from, where);
	
		return new Triple<>(f, cnames,
				anames0.toArray((Attribute<Node>[]) new Attribute[] {}));

	}

	private static <Obj> int cnamelkp(Obj[] cnames, Obj s) throws FQLException {
		for (int i = 0; i < cnames.length; i++) {
			if (s.equals(cnames[i])) {
				return i;
			}
		}
		throw new FQLException("Cannot lookup position of " + s + " in "
				+ Arrays.toString(cnames));
	}

	private static int tempTables = 0;

	private static List<PSM> deltaX(
			String pre,
			Map<Triple<Node, Node, Arr<Node, Path>>, String> ob,
			Map<Pair<Arr<Node, Path>, Arr<Node, Path>>, String> ar,
			FinFunctor<Triple<Node, Node, Arr<Node, Path>>, Pair<Arr<Node, Path>, Arr<Node, Path>>, Node, Path> projB) {
		Map<String, String> twocol_attrs = new HashMap<>();

		twocol_attrs.put("c0", PSM.VARCHAR());
		twocol_attrs.put("c1", PSM.VARCHAR());
		List<PSM> ret = new LinkedList<>();

		for (Entry<Triple<Node, Node, Arr<Node, Path>>, Node> p : projB.objMapping
				.entrySet()) {
			ob.put(p.getKey(), pre + "_" + p.getKey().second.string);
		}
		for (Entry<Arr<Triple<Node, Node, Arr<Node, Path>>, Pair<Arr<Node, Path>, Arr<Node, Path>>>, Arr<Node, Path>> p : projB.arrowMapping
				.entrySet()) {
			Path x = p.getKey().arr.second.arr;
			ret.add(new CreateTable("temp" + tempTables, twocol_attrs, false));
			ret.add(new InsertSQL("temp" + tempTables, compose(pre, x), "c0", "c1"));
			ar.put(p.getKey().arr, "temp" + tempTables++);
		}
		return ret;
	}

	private static List<Pair<Object, Object>> gather0(Set<Map<Object, Object>> v) {
		List<Pair<Object, Object>> ret = new LinkedList<>();

		for (Map<Object, Object> o : v) {
			ret.add(new Pair<>(o.get("c0"), o.get("c1")));
		}

		return ret;
	}

	public static List<Pair<String, List<Pair<Object, Object>>>> gather(
			String pre, Signature sig,
			Map<String, Set<Map<Object, Object>>> state) {
		List<Pair<String, List<Pair<Object, Object>>>> ret = new LinkedList<>();

		for (Node n : sig.nodes) {
			Set<Map<Object, Object>> v = state.get(pre + "_" + n.string);
			if (v == null) {
				throw new RuntimeException("Missing: " + pre + "_" + n.string
						+ " in " + state.keySet());
			}
			ret.add(new Pair<>(n.string, gather0(v)));
		}
		for (Edge e : sig.edges) {
			Set<Map<Object, Object>> v = state.get(pre + "_" + e.name);
			ret.add(new Pair<>(e.name, gather0(v)));
		}
		for (Attribute<Node> a : sig.attrs) {
			Set<Map<Object, Object>> v = state.get(pre + "_" + a.name);
			ret.add(new Pair<>(a.name, gather0(v)));
		}

		return ret;
	}

	public static void shred(String pre, Instance I,
			Map<String, Set<Map<Object, Object>>> state) {
		for (Node n : I.thesig.nodes) {
			Set<Map<Object, Object>> m = new HashSet<>();
			for (Pair<Object, Object> k : I.data.get(n.string)) {
				Map<Object, Object> map = new HashMap<>();
				map.put("c0", k.first);
				map.put("c1", k.second);
				m.add(map);
			}
			state.put(pre + "_" + n.string, m);
		}
		for (Edge n : I.thesig.edges) {
			Set<Map<Object, Object>> m = new HashSet<>();
			for (Pair<Object, Object> k : I.data.get(n.name)) {
				Map<Object, Object> map = new HashMap<>();
				map.put("c0", k.first);
				map.put("c1", k.second);
				m.add(map);
			}
			state.put(pre + "_" + n.name, m);
		}
		for (Attribute<Node> n : I.thesig.attrs) {
			Set<Map<Object, Object>> m = new HashSet<>();
			for (Pair<Object, Object> k : I.data.get(n.name)) {
				Map<Object, Object> map = new HashMap<>();
				map.put("c0", k.first);
				map.put("c1", k.second);
				m.add(map);
			}
			state.put(pre + "_" + n.name, m);
		}	
	}
	
	public static void shred(String pre, Transform I,
			Map<String, Set<Map<Object, Object>>> state) {
		for (Node n : I.src.thesig.nodes) {
			Set<Map<Object, Object>> m = new HashSet<>();
			for (Pair<Object, Object> k : I.data.get(n.string)) {
				Map<Object, Object> map = new HashMap<>();
				map.put("c0", k.first);
				map.put("c1", k.second);
				m.add(map);
			}
			state.put(pre + "_" + n.string, m);
		}
		for (Edge n : I.src.thesig.edges) {
			state.put(pre + "_" + n.name, new HashSet<>());
		}
		for (Attribute<Node> n : I.src.thesig.attrs) {
			state.put(pre + "_" + n.name, new HashSet<>());
		}	
	}

}

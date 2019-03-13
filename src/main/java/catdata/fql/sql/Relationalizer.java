package catdata.fql.sql;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import catdata.fql.FQLException;
import catdata.fql.Fn;
import catdata.Pair;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.cat.FinCat;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.FQLProgram;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.SigExp;
import catdata.fql.decl.Signature;
import catdata.fql.decl.InstExp.Const;

/**
 * 
 * @author ryan
 * 
 *         Implements relationalization and observation using SQL. The terminal
 *         instance construction is here also.
 */
public class Relationalizer {

	private static final Map<Pair<FQLProgram, SigExp.Const>, Triple<Const,
	Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>,
	Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>>> > cache = new HashMap<>();
	
	public static Triple<Const,
	Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>,
	Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>>> 
	terminal(FQLProgram prog, SigExp.Const sig0) {
		Triple<Const, Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>, Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>>> jjj = cache.get(new Pair<>(prog, sig0));
		if (jjj != null) {
			return jjj; //so do not have to recompute when doing omega operations
		}
		
		try {
			Signature sig = sig0.toSig(prog);
			Pair<FinCat<Node, Path>, Fn<Path, Arr<Node, Path>>> start = sig
					.toCategory2();
			//FinCat<Node, Path> cat = start.first;
			Fn<Path, Arr<Node, Path>> map = start.second;
			Map<Node, List<Pair<Arr<Node, Path>, Attribute<Node>>>> obs = sig.obs();
			
			Map<Node, List<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>> m = sig.obsbar();
			
			List<Pair<String, List<Pair<Object, Object>>>> nodes = new LinkedList<>();
			List<Pair<String, List<Pair<Object, Object>>>> attrs = new LinkedList<>();
			List<Pair<String, List<Pair<Object, Object>>>> arrows = new LinkedList<>();
			
		//	Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();			
			Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>> m1 = new HashMap<>();
			Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>> m2 = new HashMap<>();

			int i = 0;

			for (Node n : sig.nodes) {
				Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>> map1 = new HashMap<>();
				Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object> map2 = new HashMap<>();
				List<Pair<Object, Object>> set = new LinkedList<>();
				m1.put(n, map1);
				m2.put(n, map2);				
				for (LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> i2 : m.get(n)) {
					Object o = Integer.toString(++i);
					map1.put(o, i2);
					map2.put(i2, o);
					set.add(new Pair<>(o, o));
				}
				nodes.add(new Pair<>(n.string, set));
			}
			for (Attribute<Node> a : sig.attrs) {
				List<Pair<Object, Object>> set = new LinkedList<>();
				for (Pair<Object, Object> k : PropPSM.lookup(nodes, a.source.string)) {
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id = m1.get(a.source).get(k.first);
					set.add(new Pair<>(k.first, new_id.get(new Pair<>(map.of(new Path(sig, a.source)), a))));
				}
				attrs.add(new Pair<>(a.name, set));
			}
			for (Edge a : sig.edges) {
				List<Pair<Object, Object>> set = new LinkedList<>();
				for (Pair<Object, Object> k : PropPSM.lookup(nodes, a.source.string)) {
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id = m1.get(a.source).get(k.first);
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id0 = PropPSM.truncate2(sig, new_id, new Arr<>(new Path(sig, a), a.source, a.target), obs.get(a.target));
				//	LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> new_id0 = PropPSM.truncate(sig, new_id, a, m.get(a.target));
					Object o = m2.get(a.target).get(new_id0);
					set.add(new Pair<>(k.first, o));
				}
				arrows.add(new Pair<>(a.name, set));
			}
			//			Instance ret0 = new Instance(sig, data);
			Const retX = new Const(nodes, attrs, arrows, sig.toConst());
			Triple<Const, Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>, Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>>> ret = new Triple<>(retX, m1, m2);

			cache.put(new Pair<>(prog, sig0), ret);
			return ret;
		} catch (FQLException fe) {
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	
	public static Pair<Map<Node, List<Pair<Path, Attribute<Node>>>>, List<PSM>> observations(
			Signature sig, String out, String in, boolean relationalize)
			throws FQLException {
		List<PSM> ret = new LinkedList<>();
		Map<Node, List<Pair<Path, Attribute<Node>>>> attrs = new HashMap<>();
		//attrs = new HashMap<Node, List<Pair<Path, Attribute<Node>>>>();
		Map<String, String> edge_types = new HashMap<>();
		edge_types.put("c0", PSM.VARCHAR());
		edge_types.put("c1", PSM.VARCHAR());

		// copy in to out, to start with
		ret.addAll(copy(sig, out, in));

		FinCat<Node, Path> cat = sig.toCategory2().first;
		for (Node n : sig.nodes) {
			attrs.put(n, new LinkedList<>());
			int count = 0;
			List<Map<String, String>> alltypes = new LinkedList<>();
			for (Arr<Node, Path> p : cat.arrows) {
				// if (cat.isId(p)) {
				// continue;
				// }
				// need identity path to get attributes from n
				if (!p.src.equals(n)) {
					continue;
				}
				Flower f = PSMGen.compose(in, p.arr);

				ret.add(new CreateTable(out + "_" + n.string + "tempNoAttrs"
						+ count, edge_types, false));
				InsertSQL f0 = new InsertSQL(out + "_" + n.string
						+ "tempNoAttrs" + count, f, "c0", "c1");
				ret.add(f0);

				LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
				Map<String, String> from = new HashMap<>();
				List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
				List<Attribute<Node>> l = sig.attrsFor(p.arr.target);
				from.put(n.string, out + "_" + n.string + "tempNoAttrs" + count);
				select.put("c0", new Pair<>(n.string, "c0"));
				// select.put("c1", new Pair<>(n.string + "tempNoAttrs", "c1"));
				int i = 1;
				LinkedHashMap<String, String> types = new LinkedHashMap<>();
				types.put("c0", PSM.VARCHAR());
				// types.put("c1", PSM.VARCHAR());
				for (Attribute<Node> a : l) {
					from.put(a.name, in + "_" + a.name);
					Pair<String, String> lhs = new Pair<>(n.string, "c1");
					Pair<String, String> rhs = new Pair<>(a.name, "c0");
					where.add(new Pair<>(lhs, rhs));
					select.put("c" + i, new Pair<>(a.name, "c1"));
					types.put("c" + i, a.target.psm());
					attrs.get(n).add(new Pair<>(p.arr, a));
					i++;
				}
				alltypes.add(types);
				Flower g = new Flower(select, from, where);
			
				ret.add(new CreateTable(out + "_" + n.string + "temp" + count,
						types, false));
				ret.add(new InsertSQL2(out + "_" + n.string + "temp" + count,
						g, new LinkedList<>(types.keySet())));
				count++;

			}

			LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
			List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
			Map<String, String> from = new HashMap<>();
			from.put(in + "_" + n.string, in + "_" + n.string);
			Map<String, String> ty = new HashMap<>();
			int u = 0;
			ty.put("id", PSM.VARCHAR());
			select.put("id", new Pair<>(in + "_" + n.string, "c0"));
			for (int i = 0; i < count; i++) {
				Map<String, String> types = alltypes.get(i);
				for (int v = 0; v < types.size() - 1; v++) {
					ty.put("c" + u, types.get("c" + (v + 1))); // was c1
					select.put("c" + u, new Pair<>(n.string + "temp" + i, "c"
							+ (v + 1)));
					u++;
				}
				from.put(n.string + "temp" + i, out + "_" + n.string + "temp"
						+ i);
				where.add(new Pair<>(new Pair<>(n.string + "temp" + i, "c0"),
						new Pair<>(in + "_" + n.string, "c0")));

			}
			if (select.isEmpty()) {
				throw new RuntimeException("No observable for " + n.string);
			}
			Flower j = new Flower(select, from, where);
			ret.add(new CreateTable(out + "_" + n.string + "_observables", ty,
					false));
			ret.add(new InsertSQL2(out + "_" + n.string + "_observables", j,
					new LinkedList<>(j.select.keySet())));

			if (relationalize) {
				ret.addAll(relationalize(select, from, where, sig, out, ty, n,
						u, edge_types));
			}

			for (int count0 = 0; count0 < count; count0++) {
				ret.add(new DropTable(out + "_" + n.string + "temp" + count0));
				ret.add(new DropTable(out + "_" + n.string + "tempNoAttrs"
						+ count0));
			}

		}

		return new Pair<>(attrs, ret);
	}

	// suppress = true in the jpanel, so we can leave the observables table
	// around
	// suppress = false for the compiler, since we just want the relationalized
	// result
	public static Pair<Map<Node, List<Pair<Path, Attribute<Node>>>>, List<PSM>> compile(
			Signature sig, String out, String in) throws FQLException {
		return observations(sig, out, in, true);
	}

	private static List<PSM> relationalize(
            LinkedHashMap<String, Pair<String, String>> select,
            Map<String, String> from,
            List<Pair<Pair<String, String>, Pair<String, String>>> where,
            Signature sig, String out, Map<String, String> ty, Node n, int u,
            Map<String, String> edge_types) {
		List<PSM> ret = new LinkedList<>();

		LinkedHashMap<String, Pair<String, String>> select0 = new LinkedHashMap<>(
				select);
		Map<String, String> ty0 = new HashMap<>(ty);
		ty0.remove("id");
		select0.remove("id");
		Flower j0 = new Flower(select0, from, where);
		ret.add(new CreateTable(out + "_" + n.string + "_observables_proj",
				ty0, false));
		if (!ty0.isEmpty()) {
			ret.add(new InsertSQL2(out + "_" + n.string + "_observables_proj",
					j0, new LinkedList<>(j0.select.keySet())));
		}
		ret.add(new CreateTable(out + "_" + n.string + "_observables_guid", ty,
				false));
		ret.add(new InsertKeygen(out + "_" + n.string + "_observables_guid",
				"id", out + "_" + n.string + "_observables_proj",
				new LinkedList<>(ty0.keySet())));

		select = new LinkedHashMap<>();
		where = new LinkedList<>();
		from = new HashMap<>();
		from.put(n.string + "_observables", out + "_" + n.string
				+ "_observables");
		from.put(n.string + "_observables_guid", out + "_" + n.string
				+ "_observables_guid");
		for (int u0 = 0; u0 < u; u0++) {
			where.add(new Pair<>(
					new Pair<>(n.string + "_observables", "c" + u0),
					new Pair<>(n.string + "_observables_guid", "c" + u0)));
		}
		select.put("c0", new Pair<>(n.string + "_observables", "id"));
		select.put("c1", new Pair<>(n.string + "_observables_guid", "id"));

		Flower k = new Flower(select, from, where);
		ret.add(new CreateTable(out + "_" + n.string + "_squash", edge_types,
				false));
		ret.add(new InsertSQL2(out + "_" + n.string + "_squash", k,
				new LinkedList<>(k.select.keySet())));

		ret.addAll(applySubst(sig, n, out));

		return ret;
	}

	private static Collection<PSM> applySubst(Signature sig, Node N, String out) {
		List<PSM> ret = new LinkedList<>();

		Map<String, String> attrs = new HashMap<>();
		attrs.put("c0", PSM.VARCHAR());
		attrs.put("c1", PSM.VARCHAR());

		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
		Map<String, String> from = new HashMap<>();
		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();

		from.put(N + "_squash", out + "_" + N + "_squash");
		select.put("c0", new Pair<>(N + "_squash", "c1"));
		select.put("c1", new Pair<>(N + "_squash", "c1"));

		Flower f = new Flower(select, from, where);

		ret.add(new CreateTable(out + "_" + N + "_relationalize_temp", attrs,
				false));
		ret.add(new InsertSQL(out + "_" + N + "_relationalize_temp", f, "c0",
				"c1"));

		ret.add(new DropTable(out + "_" + N.string));
		ret.add(new CreateTable(out + "_" + N.string, attrs, false));
		ret.add(new InsertSQL(out + "_" + N.string, new CopyFlower(out + "_"
				+ N + "_relationalize_temp", "c0", "c1"), "c0", "c1"));

		ret.add(new DropTable(out + "_" + N + "_relationalize_temp"));

		for (Edge n : sig.edges) {
			if (!n.source.equals(N)) {
				continue;
			}

			where = new LinkedList<>();
			from = new HashMap<>();
			select = new LinkedHashMap<>();

			from.put(N + "_squash", out + "_" + N + "_squash");
			from.put(out + "_" + n.name, out + "_" + n.name);
			where.add(new Pair<>(new Pair<>(N + "_squash", "c0"), new Pair<>(
					out + "_" + n.name, "c0")));
			select.put("c0", new Pair<>(N + "_squash", "c1"));
			select.put("c1", new Pair<>(out + "_" + n.name, "c1"));

			f = new Flower(select, from, where);

			ret.add(new CreateTable(out + "_" + n.name + "_relationalize_temp",
					attrs, false));
			ret.add(new InsertSQL(out + "_" + n.name + "_relationalize_temp",
					f, "c0", "c1"));

			ret.add(new DropTable(out + "_" + n.name));
			ret.add(new CreateTable(out + "_" + n.name, attrs, false));
			ret.add(new InsertSQL(out + "_" + n.name, new CopyFlower(out + "_"
					+ n.name + "_relationalize_temp", "c0", "c1"), "c0", "c1"));

			ret.add(new DropTable(out + "_" + n.name + "_relationalize_temp"));
		}
		for (Attribute<Node> n : sig.attrs) {
			if (!n.source.equals(N)) {
				continue;
			}

			where = new LinkedList<>();
			from = new HashMap<>();
			select = new LinkedHashMap<>();

			from.put(N + "_squash", out + "_" + N + "_squash");
			from.put(out + "_" + n.name, out + "_" + n.name);
			where.add(new Pair<>(new Pair<>(N + "_squash", "c0"), new Pair<>(
					out + "_" + n.name, "c0")));
			select.put("c0", new Pair<>(N + "_squash", "c1"));
			select.put("c1", new Pair<>(out + "_" + n.name, "c1"));

			f = new Flower(select, from, where);

			ret.add(new CreateTable(out + "_" + "relationalize_temp", attrs,
					false));
			ret.add(new InsertSQL(out + "_" + "relationalize_temp", f, "c0",
					"c1"));

			ret.add(new DropTable(out + "_" + n.name));
			ret.add(new CreateTable(out + "_" + n.name, attrs, false));
			ret.add(new InsertSQL(out + "_" + n.name, new CopyFlower(out + "_"
					+ "relationalize_temp", "c0", "c1"), "c0", "c1"));

			ret.add(new DropTable(out + "_" + "relationalize_temp"));
		}
		for (Edge n : sig.edges) {
			if (!n.target.equals(N)) {
				continue;
			}

			where = new LinkedList<>();
			from = new HashMap<>();
			select = new LinkedHashMap<>();

			from.put(N + "_squash", out + "_" + N + "_squash");
			from.put(out + "_" + n.name, out + "_" + n.name);
			where.add(new Pair<>(new Pair<>(N + "_squash", "c0"), new Pair<>(
					out + "_" + n.name, "c1")));
			select.put("c0", new Pair<>(out + "_" + n.name, "c0"));
			select.put("c1", new Pair<>(N + "_squash", "c1"));

			f = new Flower(select, from, where);

			ret.add(new CreateTable(out + "_" + "relationalize_temp", attrs,
					false));
			ret.add(new InsertSQL(out + "_" + "relationalize_temp", f, "c0",
					"c1"));

			ret.add(new DropTable(out + "_" + n.name));
			ret.add(new CreateTable(out + "_" + n.name, attrs, false));
			ret.add(new InsertSQL(out + "_" + n.name, new CopyFlower(out + "_"
					+ "relationalize_temp", "c0", "c1"), "c0", "c1"));

			ret.add(new DropTable(out + "_" + "relationalize_temp"));
		}

		return ret;
	}

	private static List<PSM> copy(Signature sig, String out, String in) {
		List<PSM> ret = new LinkedList<>();

		for (Node n : sig.nodes) {
		//	Map<String, String> attrs = new HashMap<>();
		//	attrs.put("c0", PSM.VARCHAR());
		//	attrs.put("c1", PSM.VARCHAR());
			// ret.add(new CreateTable(out + "_" + n.string, attrs, false));
			ret.add(new InsertSQL(out + "_" + n.string, new CopyFlower(in + "_"
					+ n.string, "c0", "c1"), "c0", "c1"));
		}
		for (Attribute<Node> n : sig.attrs) {
		//	Map<String, String> attrs = new HashMap<>();
		//	attrs.put("c0", PSM.VARCHAR());
		//	attrs.put("c1", n.target.psm());
			// ret.add(new CreateTable(out + "_" + n.name, attrs, false));
			ret.add(new InsertSQL(out + "_" + n.name, new CopyFlower(in + "_"
					+ n.name, "c0", "c1"), "c0", "c1"));
		}
		for (Edge n : sig.edges) {
		//	Map<String, String> attrs = new HashMap<>();
		//	attrs.put("c0", PSM.VARCHAR());
		//	attrs.put("c1", PSM.VARCHAR());
			// ret.add(new CreateTable(out + "_" + n.name, attrs, false));
			ret.add(new InsertSQL(out + "_" + n.name, new CopyFlower(in + "_"
					+ n.name, "c0", "c1"), "c0", "c1"));
		}

		return ret;
	}

}

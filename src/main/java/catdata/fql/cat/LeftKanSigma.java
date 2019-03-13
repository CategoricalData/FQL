package catdata.fql.cat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.Pair;
import catdata.Quad;
import catdata.fql.FQLException;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;
import catdata.fql.decl.Type.Varchar;
import catdata.fql.sql.PSMInterp;
import catdata.ide.DefunctGlobalOptions;

public class LeftKanSigma {

	public static Quad<Instance, Map<Node, Map<Object, Integer>>, Map<Node, Map<Integer, Object>>, Map<Object, List<Pair<String, Object>>>> fullSigmaWithAttrs(
			PSMInterp inter, Mapping f, Instance i, Transform t, Instance JJJ,
			Integer xxx) throws FQLException {

		Mapping F = deAttr(f);
		Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> I = deAttr(inter, i, F.source);
		Integer kkk = inter.guid;
		Instance JJJ0 = null;
		Transform ttt = null;

		if (JJJ != null) {
			inter.guid = xxx;

			Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> JJJ0X = deAttr(
					inter, JJJ, F.target);

			Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> qqq = delta(
					f, F, JJJ0X);

			ttt = deAttr(f.source, I, qqq, t);

			JJJ0 = JJJ0X.first;
			inter.guid = kkk;
		}
//System.out.println("!!!!!!!!!!!!!!!");
		LeftKan D = new LeftKan(inter.guid, F, I.first, ttt, JJJ0);

		Pair<Instance, Map<Object, List<Pair<String, Object>>>> hhh = sigma(D);
		inter.guid = D.fresh;
		
		Map<Node, Map<Object, Integer>> etables = makeE(D.ua);
		
		Instance j = hhh.first;
		Instance ret = reAttr(etables, f.target, j, I.second);
		return new Quad<>(ret, etables, D.utables, hhh.second);
	}
	
	private static Map<Node, Map<Object, Integer>> makeE(Map<Node, Set<Pair<Object, Integer>>> p) {
		Map<Node, Map<Object, Integer>> ret = new HashMap<>();
		
		for (Entry<Node, Set<Pair<Object, Integer>>> k : p.entrySet()) {
			Map<Object, Integer> m = new HashMap<>();
			for (Pair<Object, Integer> v : k.getValue()) {
				m.put(v.first, v.second);
			}
			ret.put(k.getKey(), m);
		}
		
		return ret;
	}

	private static Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> delta(
			Mapping f0, Mapping f,
			Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> p)
			throws FQLException {

		Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();
		for (Node n : f.source.nodes) {
			data.put(n.string, p.first.data.get(f.nm.get(n).string));
		}
		for (Edge e : f.source.edges) {
			data.put(e.name, p.first.evaluate(f.em.get(e)));
		}

		Instance J = new Instance(f.source, data);

		Map<Attribute<Node>, Map<Object, Object>> m = new HashMap<>();
		for (Attribute<Node> a : f0.source.attrs) {
			m.put(a, p.second.get(f0.am.get(a)));
		}

		return new Pair<>(J, m);
	}

	// maps fresh ID to attribute
	private static Transform deAttr(Signature sig,
			Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> src,
			Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> dst,
			Transform trans) {
		List<Pair<String, List<Pair<Object, Object>>>> data = new LinkedList<>(
				trans.data());

		for (Attribute<Node> k : sig.attrs) {
			List<Pair<Object, Object>> list = new LinkedList<>();
			// Node n = k.source;
			Set<Pair<Object, Object>> s = src.first.data.get(k.name);
			Map<Object, Object> t = dst.second.get(k);
			if (t == null) {
				throw new RuntimeException();
			}
			for (Pair<Object, Object> i : s) {
				Object v = src.second.get(k).get(i.first); // v is the constant
				if (v == null) {
					throw new RuntimeException();
				}
				Object v0 = revLookup(t, v);
				if (v0 == null) {
					throw new RuntimeException();
				}
				list.add(new Pair<>(i.first, v0));
			}
			data.add(new Pair<>(k.name, list));
		}

		return new Transform(src.first, dst.first, data);
	}

	private static Instance reAttr(Map<Node, Map<Object, Integer>> D, Signature thesig, Instance i,
			Map<Attribute<Node>, Map<Object, Object>> map0) throws FQLException {
		Map<String, Set<Pair<Object, Object>>> d = new HashMap<>();

		for (Node k : i.thesig.nodes) {
			d.put(k.string, i.data.get(k.string));
		}
		for (Edge k : thesig.edges) {
			d.put(k.name, i.data.get(k.name));
		}
		Map<Object, Object> map = new HashMap<>();
		for (Attribute<Node> k : map0.keySet()) {
			Map<Object, Object> v = map0.get(k);
			for (Object k0 : v.keySet()) {
				Object v0 = v.get(k0);
				if (map.containsKey(k0)) {
					throw new RuntimeException();
				}
				map.put(k0, v0);
			}
		}

		for (Attribute<Node> k : thesig.attrs) {
			Set<Pair<Object, Object>> t = new HashSet<>();
			for (Pair<Object, Object> v : i.data.get(k.name + "_edge")) {
				Object v1 = getFrom(k, D, map /* ().get(k) */,
						v.second.toString());
				t.add(new Pair<>(v.first, v1));
			}
			d.put(k.name, t);
		}
		return new Instance(thesig, d);
	}

	private static Object getFrom(Attribute<Node> attr, Map<Node, Map<Object, Integer>> etables,
			Map<Object, Object> saved, String newkey) {
		List<Pair<Object, Object>> pre = new LinkedList<>();

		for (Node kkk : etables.keySet()) {
			Map<Object, Integer> nt = etables.get(kkk);
			for (Object k : nt.keySet()) {
				if (nt.get(k).toString().equals(newkey)) {
					if (saved.get(k.toString()) == null) {
						throw new RuntimeException();
					}
					pre.add(new Pair<>(k, saved.get(k.toString())));
				}
			}
		}
		if (pre.isEmpty()) {
			if (!DefunctGlobalOptions.debug.fql.ALLOW_NULLS) {
				throw new RuntimeException(
						"Full sigma not surjective: transform is " + etables
								+ " saved " + saved + " new key " + newkey);
			}
		}
		Set<Object> x = new HashSet<>();
		for (Pair<Object, Object> i : pre) {
			x.add(i.second);
		}
		if (x.size() > 1) {
			throw new RuntimeException("Full sigma not unique: transform is "
					+ etables + " saved " + saved + " new key " + newkey);
		}
		for (Object ret : x) {
			return ret;
		}
		if (DefunctGlobalOptions.debug.fql.ALLOW_NULLS) {
			if (!(attr.target instanceof Varchar)) {
				throw new RuntimeException(
						"Cannot create nulls for any type but string");
			}
			return "NULL" + newkey; //  null hack
		}
		throw new RuntimeException();
	}

	private static Pair<Instance, Map<Attribute<Node>, Map<Object, Object>>> deAttr(
			PSMInterp inter, Instance i, Signature sig) throws FQLException {
		Map<String, Set<Pair<Object, Object>>> d = new HashMap<>();
		Map<Attribute<Node>, Map<Object, Object>> ret = new HashMap<>();

		for (Node k : i.thesig.nodes) {
			d.put(k.string, i.data.get(k.string));
		}
		for (Edge k : i.thesig.edges) {
			d.put(k.name, i.data.get(k.name));
		}
		for (Attribute<Node> k : i.thesig.attrs) {
			Map<Object, Object> ret0 = new HashMap<>();
			Set<Pair<Object, Object>> tn = new HashSet<>();
			Set<Pair<Object, Object>> te = new HashSet<>();
			for (Pair<Object, Object> v : i.data.get(k.name)) {
				Object x = revLookup(ret0, v.second);
				if (x == null) {
					x = ++inter.guid;
					ret0.put(x.toString(), v.second);
				}
				tn.add(new Pair<>(x.toString(), x.toString()));
				te.add(new Pair<>(v.first, x.toString()));
			}
			ret.put(k, ret0);
			d.put(k.name, tn);
			d.put(k.name + "_edge", te);
		}
		return new Pair<>(new Instance(sig, d), ret);
	}

	private static Object revLookup(Map<Object, Object> map, Object x) {
		if (x == null) {
			throw new RuntimeException();
		}
		for (Object k : map.keySet()) {
			if (map.get(k).equals(x)) {
				return k;
			}
		}
		return null;
	}

	private static Mapping deAttr(Mapping f) throws FQLException {
		Mapping ret = f.clone();
		deAttr(ret.source);
		deAttr(ret.target);

		for (Attribute<Node> k : ret.am.keySet()) {
			Attribute<Node> v = ret.am.get(k);
			Node src = new Node(k.name);
			Node dst = new Node(v.name);
			Edge srcE = new Edge(k.name + "_edge", k.source, src);
			Edge dstE = new Edge(v.name + "_edge", v.source, dst);
			ret.nm.put(src, dst);
			ret.em.put(srcE, new Path(ret.target, dstE));
		}
		ret.am.clear();

		return ret;
	}

	private static void deAttr(Signature source) {
		for (Attribute<Node> a : source.attrs) {
			Node dst = new Node(a.name);
			source.nodes.add(dst);
			source.edges.add(new Edge(a.name + "_edge", a.source, dst));
		}
		source.attrs.clear();
//		source.doColors();
	}

	private static Set<Pair<Object, Object>> conc(Set<Pair<Integer, Integer>> t) {
		Set<Pair<Object, Object>> ret = new HashSet<>();

		for (Pair<Integer, Integer> i : t) {
			ret.add(new Pair<>(i.first.toString(), i.second
                    .toString()));
		}

		return ret;
	}

	private static Pair<Instance, Map<Object, List<Pair<String, Object>>>> sigma(
			LeftKan lk) throws FQLException {
		
		if (!lk.compute()) {
			throw new FQLException("Too many sigma iterations.");
		}

		Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();
		for (Node e : lk.Pb.keySet()) {
			Set<Pair<Integer, Integer>> t = lk.Pb.get(e);
			data.put(e.string, conc(t));
		}
		for (Edge e : lk.Pg.keySet()) {
			Set<Pair<Integer, Integer>> t = lk.Pg.get(e);
			data.put(e.name, conc(t));
		}

		Instance ret = new Instance(lk.F.target, data);

		return new Pair<>(ret, lk.lineage);
	}

}

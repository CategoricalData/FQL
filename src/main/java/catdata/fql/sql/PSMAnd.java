package catdata.fql.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.Pair;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

public class PSMAnd extends PSM {

	private final String pre;
    private final String prop;
    private final String prod;
	private final Signature sig;
	private final String kind;

	public PSMAnd(Signature sig, String pre, String prod, String prop, String kind) {
		this.sig = sig;
		this.pre = pre;
		this.prop = prop;
		this.prod = prod;
		this.kind = kind;
	}

	@Override
	public String isSql() {
		return pre;
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		try {

			Signature sig0 = new Signature(sig.nodes, sig.edges, new LinkedList<>(), sig.eqs);
			
			Pair<Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>> H1 = interp.prop1
					.get(prop);
			Pair<Instance, Map<Node, Pair<Map<Object, Instance>, Map<Instance, Object>>>> H2 = interp.prop2
					.get(prop);
			//Instance old = H2.first;

			Instance prp = new Instance(sig, PSMGen.gather(prop, sig, state));
			Instance prd = new Instance(sig, PSMGen.gather(prod, sig, state));
			Transform fst = new Transform(prd, prp, PSMGen.gather(prod + "_fst", sig, state));
			Transform snd = new Transform(prd, prp, PSMGen.gather(prod + "_snd", sig, state));

			Map<Node, Map<Object, Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>> I1 = interp.prop3
					.get(prop);
			Map<Node, Map<Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Object>> I2 = interp.prop4
					.get(prop);

			List<Pair<String, List<Pair<Object, Object>>>> data = new LinkedList<>();
			
			for (Node c : sig.nodes) {
				List<Pair<Object, Object>> data0 = new LinkedList<>();
				Triple<Instance, Map<Object, Path>, Map<Path, Object>> Hc = H1.first.get(c);
				
				for (Object idp : prd.getNode(c)) {
					Object id0 = lookup(fst.data.get(c.string), idp);
					Object id1 = lookup(snd.data.get(c.string), idp);
					
					Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>> idl = I1.get(c).get(id0);
					Instance A = H2.second.get(c).first.get(idl.first);

					Pair<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>> idr = I1.get(c).get(id1);
					Instance B = H2.second.get(c).first.get(idr.first);
					
					if (!idl.second.equals(idr.second)) {
						throw new RuntimeException("bad");
					}

					Instance nA;
                    switch (kind) {
                        case "and":
                            nA = isect(A, B);
                            break;
                        case "or":
                            nA = union(A, B);
                            break;
                        case "implies":
                            nA = implies(sig0, H1, Hc, A, B);
                            break;
                        default:
                            throw new RuntimeException();
                    }
					
					
					Object notId = H2.second.get(c).second.get(nA);
			
					Object x = I2.get(c).get(new Pair<>(notId, idl.second));
					data0.add(new Pair<>(idp, x));
					
				}
				data.add(new Pair<>(c.string, data0));
			}
			
			Transform ret = new Transform(prd, prp, data);
			PSMGen.shred(pre, ret, state);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}

	private static Instance isect(Instance a, Instance b) throws FQLException {
		Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();
		
		for (Node n : a.thesig.nodes) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			for (Pair<Object, Object> p : a.data.get(n.string)) {
				if (b.data.get(n.string).contains(p)) {
					set.add(p);
				}
			}
			data.put(n.string, set);
		}
		for (Edge n : a.thesig.edges) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			for (Pair<Object, Object> p : a.data.get(n.name)) {
				if (b.data.get(n.name).contains(p)) {
					set.add(p);
				}
			}
			data.put(n.name, set);
		}
		
		return new Instance(a.thesig, data);
	}

	private static Instance union(Instance a, Instance b) throws FQLException {
		Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();
		
		for (Node n : a.thesig.nodes) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			set.addAll(a.data.get(n.string));
			set.addAll(b.data.get(n.string));			
			data.put(n.string, set);
		}
		for (Edge n : a.thesig.edges) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			set.addAll(a.data.get(n.name));
			set.addAll(b.data.get(n.name));			
			data.put(n.name, set);
		}
		
		return new Instance(a.thesig, data);
	}
	
	private Instance implies(
			Signature sig0,
		     Pair<Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>> H1 , 
			Triple<Instance, Map<Object, Path>, Map<Path, Object>> Hc, Instance A, Instance B)
			throws FQLException {
		Map<String, Set<Pair<Object, Object>>> notA_data = new HashMap<>();
		for (Node d : sig.nodes) {
			Set<Pair<Object,Object>> dd = new HashSet<>();
			xxx : for (Object f : Hc.first.getNode(d)) {
				Path ff = Hc.second.get(f);
				for (Node d0 : sig.nodes) {
					for (Arr<Node, Path> g : sig.toCategory2().first.hom(d, d0)) {
						Arr<Node, Path> fg = sig.toCategory2().first.compose(sig.toCategory2().second.of(ff), g);
						Object xxx = H1.first.get(d0).third.get(fg.arr);
						if (xxx == null) {
							throw new RuntimeException();
						}
						if (!A.getNode(d0).contains(xxx) || B.getNode(d0).contains(xxx)) {
						} else {
							continue xxx;
						}
					}
				}
				dd.add(new Pair<>(f, f));
			}
			
			notA_data.put(d.string, dd);
		}
		for (Edge h : sig.edges) {
			Set<Pair<Object,Object>> dd = new HashSet<>();
			for (Object f : notA_data.get(h.source.string)) { 
				Path ff = Hc.second.get(f);
				Arr<Node, Path> fg = sig.toCategory2().first.compose(sig.toCategory2().second.of(ff), sig.toCategory2().second.of(new Path(sig, h)));
				Object xxx = Hc.third.get(fg.arr);
				dd.add(new Pair<>(f, xxx));
			}
			notA_data.put(h.name, dd);
		}				
		Instance notA = new Instance(sig0, notA_data);
		return notA;
	}

	
	
	private static <X,Y> Y lookup(Set<Pair<X,Y>> set, X x) {
		for (Pair<X, Y> k : set) {
			if (k.first.equals(x)) {
				return k.second;
			}
		}
		throw new RuntimeException();
	}

	@Override
	public String toPSM() {
		return pre + " := " + prop + "." + kind;
	}

}

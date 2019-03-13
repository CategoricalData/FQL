package catdata.fql.sql;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;

public class PSMCurry extends PSM {

	private final String ret;
    private final String inst_src;
    private final String inst_dst;
	private final String trans;
	private final String trans_src;
	private final String trans_dst;
	private final String exp;
	private final Signature sig;

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		try {

			Instance IJ = new Instance(sig, PSMGen.gather(trans_src,
					sig, state));
			Instance K = new Instance(sig, PSMGen.gather(trans_dst,
					sig, state));
			Transform t = new Transform(IJ, K, PSMGen.gather(
					trans, sig, state));

			Instance I = new Instance(sig, PSMGen.gather(inst_src, sig, state));
			Instance J = new Instance(sig, PSMGen.gather(exp, sig, state));

			Instance JK = new Instance(sig, PSMGen.gather(inst_dst, sig,
					state));

			Transform trans_src0_fst = new Transform(IJ, I,
					PSMGen.gather(trans_src + "_fst", sig, state));
			Transform trans_src0_snd = new Transform(IJ, J,
					PSMGen.gather(trans_src + "_snd", sig, state));
			
			Map<Node, List<Pair<Arr<Node, Path>, Attribute<Node>>>> obs = I.thesig.obs();

			List<Pair<String, List<Pair<Object, Object>>>> l = new LinkedList<>();
			Quad<Instance, Map<Pair<Node, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>>>, Map<Node, Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>>>, Map<Node, Map<Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>, Object>>> kkk = interp.exps2.get(inst_dst);
			for (Node c : sig.nodes) {
				List<Pair<Object, Object>> s = new LinkedList<>();
				for (Pair<Object, Object> xx : I.data.get(c.string)) {
					Object x = xx.first;
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> w = I.flag(c, x);
					Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>> HcJ = kkk.second.get(new Pair<>(c, w));
					
					// construct transform depending on x, lookup in kkk.second

					List<Pair<String, List<Pair<Object, Object>>>> tx = new LinkedList<>();
					for (Node d : sig.nodes) {
						List<Pair<Object, Object>> tx0 = new LinkedList<>();
						for (Arr<Node, Path> f : sig.toCategory2().first.hom(c, d)) {
							for (Pair<Object, Object> y : J.data.get(d.string)) {
								//only if y(p.a) = w(p.a)
								if (!PropPSM.truncate2(I.thesig, w, f, obs.get(d)).equals(J.flag(d, y.first))) {
									continue;
								}
								Object Ifx = lookup(I.evaluate(f.arr), x);
//								Object Ifx = lookup(HcJ.first.evaluate(f.arr), x);
								Object u = find(d, trans_src0_fst,
										trans_src0_snd, Ifx, y.first);
								Object v = lookup(t.data.get(d.string), u);
								Object iii = HcJ.third.get(d).get(new Pair<>(f, y.first));
								tx0.add(new Pair<>(iii, v)); 
							}
						}
						tx.add(new Pair<>(d.string, tx0)); //I*J -> K
					}
					Transform xxx = new Transform(HcJ.first, K, tx); 
					Object yyy = kkk.fourth.get(c).get(new Pair<>(w, xxx));
					s.add(new Pair<>(x, yyy));
				}
				l.add(new Pair<>(c.string, s));
			}
			Transform zzz = new Transform(I, JK, l);
			PSMGen.shred(ret, zzz, state);

		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}
	

	private static Object find(Node d, Transform fst, Transform snd, Object a, Object b) {
		for (Pair<Object, Object> o : fst.data.get(d.string)) {
			Object j = lookup(fst.data.get(d.string), o.first);
			Object k = lookup(snd.data.get(d.string), o.first);
			if (j.equals(a) && k.equals(b)) {
				return o.first;
			}
		}
		throw new RuntimeException("Cannot find (" + a + "," + b + ") in " + fst + " and " + snd);
	}

	private static Object lookup(Set<Pair<Object, Object>> s, Object t) {
		for (Pair<Object, Object> k : s) {
			if (k.first.equals(t)) {
				return k.second;
			}
		}
		throw new RuntimeException(t + " not found in " + s);
	}

	@Override
	public String toString() {
		return inst_dst + ".curry " + trans;
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for curry.");
	}

	public PSMCurry(String ret, String inst_src, String inst_dst, String trans,
			String trans_src, String trans_dst, String exp, Signature sig) {
        this.ret = ret;
		this.inst_src = inst_src;
		this.inst_dst = inst_dst;
		this.trans = trans;
		this.trans_src = trans_src;
		this.trans_dst = trans_dst;
		this.sig = sig;
		this.exp = exp;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((exp == null) ? 0 : exp.hashCode());
		result = prime * result
				+ ((inst_dst == null) ? 0 : inst_dst.hashCode());
		result = prime * result
				+ ((inst_src == null) ? 0 : inst_src.hashCode());
		result = prime * result + ((ret == null) ? 0 : ret.hashCode());
		result = prime * result + ((sig == null) ? 0 : sig.hashCode());
		result = prime * result + ((trans == null) ? 0 : trans.hashCode());
		result = prime * result
				+ ((trans_dst == null) ? 0 : trans_dst.hashCode());
		result = prime * result
				+ ((trans_src == null) ? 0 : trans_src.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PSMCurry other = (PSMCurry) obj;
		if (exp == null) {
			if (other.exp != null)
				return false;
		} else if (!exp.equals(other.exp))
			return false;
		if (inst_dst == null) {
			if (other.inst_dst != null)
				return false;
		} else if (!inst_dst.equals(other.inst_dst))
			return false;
		if (inst_src == null) {
			if (other.inst_src != null)
				return false;
		} else if (!inst_src.equals(other.inst_src))
			return false;
		if (ret == null) {
			if (other.ret != null)
				return false;
		} else if (!ret.equals(other.ret))
			return false;
		if (sig == null) {
			if (other.sig != null)
				return false;
		} else if (!sig.equals(other.sig))
			return false;
		if (trans == null) {
			if (other.trans != null)
				return false;
		} else if (!trans.equals(other.trans))
			return false;
		if (trans_dst == null) {
			if (other.trans_dst != null)
				return false;
		} else if (!trans_dst.equals(other.trans_dst))
			return false;
		if (trans_src == null) {
			if (other.trans_src != null)
				return false;
		} else if (!trans_src.equals(other.trans_src))
			return false;
		return true;
	}
	
	@Override
	public String isSql() {
		return ret;
	}


}

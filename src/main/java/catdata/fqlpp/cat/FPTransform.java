package catdata.fqlpp.cat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.Util;
import catdata.fqlpp.FUNCTION;
import catdata.fqlpp.cat.FinSet.Fn;

@SuppressWarnings("serial")
public class FPTransform<O, A> implements Serializable {

	public final Instance<O, A> src;
    public final Instance<O, A> dst;
	public final Map<Signature<O, A>.Node, Map<Object, Object>> data;
	

	public FPTransform(Instance<O, A> src, Instance<O, A> dst,
			Map<Signature<O, A>.Node, Map<Object, Object>> data) {
		this.src = src;
		this.dst = dst;
		this.data = data;
		validate();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FPTransform other = (FPTransform) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String nm = "\n objects\n";
		boolean b = false;
		for (Entry<Signature<O, A>.Node, Map<Object, Object>> k : data
				.entrySet()) {
			if (b) {
				nm += ", \n";
			}
			b = true;

			boolean c = false;
			nm += "  " + k.getKey() + " -> " + "{";

			for (Entry<Object, Object> k0 : k.getValue().entrySet()) {
				if (c) {
					nm += ", ";
				}
				c = true;
				nm += "(" + Util.q(k0.getKey()) + ", " + Util.q(k0.getValue())
						+ ")";
			}
			nm += "}";
		}
		nm = nm.trim();
		nm += ";\n";

		return "{\n " + nm + "}";
	}

	private void validate() {
		if (!src.thesig.equals(dst.thesig)) {
			throw new RuntimeException("Signatures do not match: " + this);
		}
		if (!data.keySet().equals(src.thesig.nodes)) {
			throw new RuntimeException("Domain does not match signature: "
					+ this);
		}
		for (Signature<O, A>.Node n : src.thesig.nodes) {
			Map<Object, Object> v = data.get(n);
			if (!v.keySet().equals(src.nm.get(n))) {
				throw new RuntimeException("Bad or missing domain value in "
						+ n + " in " + this);
			}
			if (!dst.nm.get(n).containsAll(v.values())) {
				throw new RuntimeException("Bad or missing codomain value in "
						+ n + " in " + this);
			}
		}
		for (Signature<O, A>.Edge f : src.thesig.edges) {
			Map<Object, Object> lhs = Util.compose0(data.get(f.source),
					dst.em.get(f));
			Map<Object, Object> rhs = Util.compose0(src.em.get(f),
					data.get(f.target));

			if (!lhs.equals(rhs)) {
				throw new RuntimeException("Not respected on " + f + " in "
						+ this);
			}
		}
	}
	
	public static <O,A> FPTransform<O,A> id(Instance<O,A> I) {
		Map<Signature<O,A>.Node, Map<Object,Object>> m = new HashMap<>();
		for (Signature<O,A>.Node k : I.thesig.nodes) {
			Map<Object, Object> p = new HashMap<>();
			for (Object o : I.nm.get(k)) {
				p.put(o, o);
			}
			m.put(k, p);
		}
		return new FPTransform<>(I, I, m);
	}

	public static <O, A> FPTransform<O, A> compose(
			FPTransform<O, A> f, FPTransform<O, A> g) {
		if (!f.dst.equals(g.src)) {
			throw new RuntimeException("Dom/Cod mismatch when composing "
					+ f + " and " + g);
		}
		Map<Signature<O,A>.Node, Map<Object,Object>> m = new HashMap<>();
		for (Signature<O,A>.Node k : f.src.thesig.nodes) {
			m.put(k, Util.compose0(f.data.get(k), g.data.get(k)));
		}
		return new FPTransform<>(f.src, g.dst, m);
	}
	
	@SuppressWarnings({ "rawtypes" })
    private
    Transform<Signature<O,A>.Node,Signature<O,A>.Path,Set,Fn> transform;

	@SuppressWarnings("rawtypes")
	public Transform<Signature<O,A>.Node,Signature<O,A>.Path,Set,Fn> toTransform() {
		if (transform != null) {
			return transform;
		}
		transform = toTransformX();
		return transform; 
	}
	@SuppressWarnings({ "rawtypes" })
	private Transform<Signature<O,A>.Node,Signature<O,A>.Path,Set,Fn> toTransformX() {
		FUNCTION<Signature<O,A>.Node, Fn> f = n ->
		  new Fn<>(src.nm.get(n), dst.nm.get(n), x -> data.get(n).get(x));
		return new Transform<>(src.toFunctor(), dst.toFunctor(), f); 
	}
}

package catdata.fqlpp.cat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import catdata.Unit;
import catdata.Util;
import catdata.fqlpp.FUNCTION;
import catdata.fqlpp.cat.FinSet.Fn;

@SuppressWarnings("serial")
public class Instance<O, A> implements Serializable{

	public final Signature<O, A> thesig;
	public Map<Signature<O, A>.Node, Set<Object>> nm = new HashMap<>();
	public Map<Signature<O, A>.Edge, Map<Object, Object>> em = new HashMap<>();

	public Instance(Signature<O, A> thesig, Map<O, Set<Object>> objs,
			Map<A, Map<Object, Object>> arrows) {
		this.thesig = thesig;
		for (Entry<O, Set<Object>> k : objs.entrySet()) {
			nm.put(thesig.getNode(k.getKey()), k.getValue());
		}
		for (Entry<A, Map<Object, Object>> k : arrows.entrySet()) {
			em.put(thesig.getEdge(k.getKey()), k.getValue());
		}
		validate();
	}
	
	public Instance(Map<Signature<O, A>.Node, Set<Object>> nm,
			Map<Signature<O, A>.Edge, Map<Object, Object>> em, Signature<O, A> thesig) {
		this.thesig = thesig;
		this.nm = nm;
		this.em = em;
		validate();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((em == null) ? 0 : em.hashCode());
		result = prime * result + ((nm == null) ? 0 : nm.hashCode());
		result = prime * result + ((thesig == null) ? 0 : thesig.hashCode());
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
		Instance other = (Instance) obj;
		if (em == null) {
			if (other.em != null)
				return false;
		} else if (!em.equals(other.em))
			return false;
		if (nm == null) {
			if (other.nm != null)
				return false;
		} else if (!nm.equals(other.nm))
			return false;
		if (thesig == null) {
			if (other.thesig != null)
				return false;
		} else if (!thesig.equals(other.thesig))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String x = "\n objects\n";
		boolean b = false;

		for (Signature<O, A>.Node k0 : thesig.nodes) {
			Set<Object> k = nm.get(k0);
			if (b) {
				x += ", \n";
			}
			b = true;
			x += "  " + k0.name + " -> {";
			boolean d = false;
			for (Object v : k) {
				if (d) {
					x += ", ";
				}
				d = true;
				x += Util.q(v);
			}
			x += "}";
		}
		x = x.trim();
		x += ";\n";

		x += " arrows\n";
		b = false;
		for (Signature<O, A>.Edge k0 : thesig.edges) {
			Map<Object, Object> k = em.get(k0);
			if (b) {
				x += ", \n";
			}
			b = true;
			x += "  " + k0.name + " -> {";
			boolean d = false;
			for (Entry<Object, Object> v : k.entrySet()) {
				if (d) {
					x += ", ";
				}
				d = true;
				x += "(" + Util.q(v.getKey()) + ", " + Util.q(v.getValue())
						+ ")";
			}
			x += "}";
		}
		x = x.trim();

		return "{\n " + x + ";\n}";
	}

	private void validate() {

		for (Signature<O, A>.Node n : thesig.nodes) {
			Set<Object> i = nm.get(n);
			if (i == null) {
				throw new RuntimeException("Missing object table " + n.name
						+ " in " + this);
			}
		}

		for (Signature<O, A>.Edge e : thesig.edges) {
			Map<Object, Object> i = em.get(e);
			if (i == null) {
				throw new RuntimeException("Missing arrow table " + e.name
						+ " in " + this);
			}

			if (!i.keySet().equals(nm.get(e.source))) {
				throw new RuntimeException("Domain of " + e.name
						+ " has non foreign key (or is missing key) in " + this);
			}

			if (!nm.get(e.target).containsAll(i.values())) {
				throw new RuntimeException("Codomain of " + e.name
						+ " has non foreign key in " + this);
			}
		}

		for (Signature<O, A>.Eq eq : thesig.eqs) {
			Map<Object, Object> lhs = evaluate(eq.lhs);
			Map<Object, Object> rhs = evaluate(eq.rhs);
			if (!lhs.equals(rhs)) {
				throw new RuntimeException("Violates constraints: " + thesig
						+ "\n\n eq is " + eq + "\nlhs is " + lhs
						+ "\n\nrhs is " + rhs);
			}
		}
	}

	private final Map<Signature<O, A>.Path, Map<Object, Object>> cache = new HashMap<>();
	public Map<Object, Object> evaluate(Signature<O, A>.Path p) {
		if (!nm.containsKey(p.source)) {
			throw new RuntimeException("Couldnt find " + p.source);
		}
		if (cache.containsKey(p)) {
			return cache.get(p);
		}
		Signature<O, A>.Path px = p.head();
		Map<Object, Object> x = cache.get(px);
		if (x == null) {
			x = new HashMap<>();
			for (Object o : nm.get(p.source)) {
				x.put(o,o);
			}
			cache.put(px, x);
		}
		for (Signature<O, A>.Edge e : p.path) {
			if (em.get(e) == null) {
				throw new RuntimeException("Couldnt find " + e.name);
			}
			px = px.sig().path(px, e);
			if (cache.containsKey(px)) {
				x = cache.get(px);
			} else {
				x = Util.compose0(x, em.get(e));
				cache.put(px, x);
			}
		}
		//cache.put(p, x);
		return x;
	}
	
	public static <X,Y> Instance<X,Y> terminal(Signature<X,Y> sig) {
		Map<Signature<X, Y>.Node, Set<Object>> objs = new HashMap<>();
		Map<Signature<X, Y>.Edge, Map<Object, Object>> arrs = new HashMap<>();

		Set<Object> s = new HashSet<>();
		s.add(Unit.unit);
		for (Signature<X, Y>.Node k : sig.nodes) {
			objs.put(k, s);
		}
		
		Map<Object, Object> t = new HashMap<>();
		t.put(Unit.unit, Unit.unit);
		for (Signature<X, Y>.Edge k : sig.edges) {
			arrs.put(k, t);
		}
		
		return new Instance<>(objs, arrs, sig);
	}
	@SuppressWarnings({ "rawtypes" })
    private
    Functor<Signature<O,A>.Node,Signature<O,A>.Path,Set,Fn> functor;
	
	@SuppressWarnings({ "rawtypes" })
	public Functor<Signature<O,A>.Node,Signature<O,A>.Path,Set,Fn> toFunctor() {
		if (functor != null) {
			return functor;
		}
		functor = toFunctorX();
		return functor; 
	}
	
	
	@SuppressWarnings({ "rawtypes" })
	private Functor<Signature<O,A>.Node,Signature<O,A>.Path,Set,Fn> toFunctorX() {
		FUNCTION<Signature<O,A>.Path, Fn> f = p ->
		  new Fn<>(nm.get(p.source), nm.get(p.target), x -> evaluate(p).get(x));
		return new Functor<>(thesig.toCat(), FinSet.FinSet, nm::get, f); 
	}

}

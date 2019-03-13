package catdata.fqlpp.cat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public class Mapping<O1, A1, O2, A2> implements Serializable {

	public final Signature<O1, A1> source;
	public final Signature<O2, A2> target;
	public Map<Signature<O1, A1>.Node, Signature<O2, A2>.Node> nm = new HashMap<>();
	private Map<Signature<O1, A1>.Edge, Signature<O2, A2>.Path> em = new HashMap<>();

	public Signature<O2, A2>.Path apply(Signature<O1, A1>.Path path) {
		Signature<O2, A2>.Path ret = target.path(nm.get(path.source));
		for (Signature<O1, A1>.Edge e : path.path) {
			Signature<O2, A2>.Path p = em.get(e);
			if (p == null) {
				throw new RuntimeException("No mapping for " + e + " in "
						+ this);
			}
			ret = target.path(ret, p);
		}
		return ret;
	}

	public Mapping(
			Map<Signature<O1, A1>.Node, Signature<O2, A2>.Node> nm,
			Map<Signature<O1, A1>.Edge, Signature<O2, A2>.Path> em,
			Signature<O1, A1> source, Signature<O2, A2> target) {
		this.source = source;
		this.target = target;
		this.nm = nm;
		this.em = em;
		validate(); 
	}

	private void validate() {
		for (Signature<O1, A1>.Node n : nm.keySet()) {
			if (!source.nodes.contains(n)) {
				throw new RuntimeException("Mapping contains object, " + n + " that is not in source schema " + source);
			}
		}
		for (Signature<O1, A1>.Node n : source.nodes) {
			if (!nm.keySet().contains(n)) {
				throw new RuntimeException("Mapping does not contain object mapping for " + n);
			}
			Signature<O2, A2>.Node m = nm.get(n);
			if (!target.nodes.contains(m)) {
				throw new RuntimeException("Object " + n + " maps to " + m + " is not in target schema.");
			}			
		}
		for (Signature<O1, A1>.Edge n : em.keySet()) {
			if (!source.edges.contains(n)) {
				throw new RuntimeException("Mapping contains arrow, " + n + " that is not in source schema.");
			}
		}
		for (Signature<O1, A1>.Edge n : source.edges) {
			if (!em.keySet().contains(n)) {
				throw new RuntimeException("Mapping does not contain arrow mapping for " + n);
			}
			Signature<O2, A2>.Path m = em.get(n);
			if (m == null) {
				throw new RuntimeException("Arrow " + n + " maps to null.");
			}
		}
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((em == null) ? 0 : em.hashCode());
		result = prime * result + ((nm == null) ? 0 : nm.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		Mapping other = (Mapping) obj;
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
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String nm = "\n objects\n";
		boolean b = false;
		for (Entry<Signature<O1, A1>.Node, Signature<O2, A2>.Node> k : this.nm
				.entrySet()) {
			if (b) {
				nm += ",\n";
			}
			b = true;
			nm += "  " + k.getKey() + " -> " + k.getValue();
		}
		nm = nm.trim();
		nm += ";\n";

		nm += " arrows\n";
		b = false;
		for (Entry<Signature<O1, A1>.Edge, Signature<O2, A2>.Path> k : em
				.entrySet()) {
			if (b) {
				nm += ",\n";
			}
			b = true;
			nm += "  " + k.getKey().name + " -> " + k.getValue();
		}
		nm = nm.trim();
		nm += ";\n";

		String ret = "{\n " + nm + "}";

		ret += (" : " + source);
		ret += (" \n\n -> \n\n ");
		ret += (target.toString());
		return ret;
	}

	public static <O,A> Mapping<O, A, O, A> identity(Signature<O, A> o) {
		Map<Signature<O,A>.Node, Signature<O,A>.Node> n = new HashMap<>();
		Map<Signature<O,A>.Edge, Signature<O,A>.Path> e = new HashMap<>();
		return new Mapping<>(n, e, o, o);
	}
	
	public static <O1,A1,O2,A2,O3,A3> Mapping<O1,A1,O3,A3> compose(Mapping<O1,A1,O2,A2> l, Mapping<O2,A2,O3,A3> r) {
		if (!l.target.equals(r.source)) {
			throw new RuntimeException(l.target + "\n\n" + r.source);
		}

		Map<Signature<O1,A1>.Node, Signature<O3,A3>.Node> xxx = new HashMap<>();
		Map<Signature<O1,A1>.Edge, Signature<O3,A3>.Path> yyy = new HashMap<>();

		for (Signature<O1, A1>.Node n : l.source.nodes) {
			xxx.put(n, r.nm.get(l.nm.get(n)));
		}

		for (Signature<O1, A1>.Edge e : l.source.edges) {
			Signature<O2, A2>.Path p = l.em.get(e);
			yyy.put(e, r.apply(p));
		}

		return new Mapping<>(xxx, yyy, l.source, r.target);
	}
	
	private Functor<Signature<O1,A1>.Node,Signature<O1,A1>.Path,Signature<O2,A2>.Node,Signature<O2,A2>.Path> functor;
	public Functor<Signature<O1,A1>.Node,Signature<O1,A1>.Path,Signature<O2,A2>.Node,Signature<O2,A2>.Path> toFunctor() {
		if (functor != null) {
			return functor;
		}
		functor = toFunctorX();
		return functor; 
	}
	
	private Functor<Signature<O1,A1>.Node,Signature<O1,A1>.Path,Signature<O2,A2>.Node,Signature<O2,A2>.Path> toFunctorX() {
		return new Functor<>(source.toCat(), target.toCat(), nm::get, this::apply); 
	}

}

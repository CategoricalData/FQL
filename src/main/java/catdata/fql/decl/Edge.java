package catdata.fql.decl;


/**
 * 
 * @author ryan
 * 
 *         Class for edges in a signature.
 */
public class Edge {

	@Override
	public String toString() {
		return name + " : " + source + " -> " + target;
	}

	public final String name;
	public final Node source;
	public final Node target;

	public Object morphism;

	public Edge(String name, Node source, Node target) {
		this.name = name;
		this.source = source;
		this.target = target;
		if (source == null || target == null || name == null) {
			throw new RuntimeException();
		}
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + source.hashCode();
		result = prime * result + target.hashCode();
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
		Edge other = (Edge) obj;
		return name.equals(other.name) && source.equals(other.source) && target.equals(other.target);
	}

	public String tojson() {
		String s = "{";
		s += "\"source\" : " + source.tojson();
		s += ", \"target\" : " + target.tojson();
		s += ", \"label\" : \"" + name + "\"}";
		return s;
	}

}

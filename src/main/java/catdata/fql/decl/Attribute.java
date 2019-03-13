package catdata.fql.decl;

/**
 * 
 * @author ryan
 * 
 *         Implentation of atomic attributes.
 */
@SuppressWarnings("hiding")
public class Attribute<Node> implements Comparable<Attribute<Node>> {

	public Attribute(String name, Node source, Type target) {
		this.name = name;
		this.source = source;
		this.target = target;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Attribute other = (Attribute) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
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
		return name; // + " : " + source + " -> " + target;
	}
	
//	public String toString() {
	//	return name + " : " + source + " -> " + target;
	//}

	public final String name;
	public final Node source;
	public final Type target;
	@Override
	public int compareTo(Attribute<Node> o) {
		return name.compareTo(o.name);
	}

}

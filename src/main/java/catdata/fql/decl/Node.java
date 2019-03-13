package catdata.fql.decl;


/**
 * 
 * @author ryan
 * 
 *         Nodes in a signature.
 */
public class Node implements Comparable<Node> {

	@Override
	public String toString() {
		return string;
	}

	public String string;

	public Node(String string) {
		assert (string != null);
		this.string = string;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((string == null) ? 0 : string.hashCode());
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
		Node other = (Node) obj;
		if (string == null) {
			if (other.string != null)
				return false;
		} else if (!string.equals(other.string))
			return false;
		return true;
	}

	public String tojson() {
		return "\"" + string + "\"";
	}

	@Override
	public int compareTo(Node o) {
		return string.compareTo(o.string);
	}

}

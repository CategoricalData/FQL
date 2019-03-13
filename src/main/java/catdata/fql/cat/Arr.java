package catdata.fql.cat;

/**
 * 
 * @author ryan
 * 
 * @param <Obj>
 *            type of source and target objects
 * @param <Arrow>
 *            type of the arrow data
 */
public class Arr<Obj, Arrow> {

	@Override
	public String toString() {
		return arr.toString();
	}

	public String toString2() {
		return arr + " : " + src + " -> " + dst;
	}

	public Arrow arr;
	public Obj src;
	public Obj dst;

	public Arr(Arrow arr, Obj src, Obj dst) {
        if (arr == null) {
			throw new RuntimeException();
		}
		this.arr = arr;
		this.src = src;
		this.dst = dst;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((arr == null) ? 0 : arr.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
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
		@SuppressWarnings("rawtypes")
		Arr other = (Arr) obj;
		if (arr == null) {
			if (other.arr != null)
				return false;
		} else if (!arr.equals(other.arr))
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

}

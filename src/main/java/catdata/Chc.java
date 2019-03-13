package catdata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Ryan Wisnesky
 */
@SuppressWarnings("serial")
public class Chc<X, Y> implements Serializable /* , Comparable<Chc<X,Y>> */ {
	public final boolean left;

	public final X l;
	public final Y r;

	public static <X, Y> Iterator<Chc<X, Y>> leftIterator(Iterator<X> it) {
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Chc<X, Y> next() {
				return Chc.inLeft(it.next());
			}
		};

	}

	public static <X, Y> Iterator<Chc<X, Y>> rightIterator(Iterator<Y> it) {
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Chc<X, Y> next() {
				return Chc.inRight(it.next());
			}
		};

	}

	public static <X, Y> Set<X> projIfAllLeft(Set<Chc<X, Y>> set) {
		Set<X> ret = new HashSet<>();

		for (Chc<X, Y> x : set) {
			if (x.left) {
				ret.add(x.l);
			} else {
				throw new RuntimeException("Cannot projLeft " + x);
			}
		}

		return ret;
	}

	public static <X, Y> Set<Y> projIfAllRight(Set<Chc<X, Y>> set) {
		Set<Y> ret = (new HashSet<>(set.size()));

		for (Chc<X, Y> x : set) {
			if (x.left) {
				throw new RuntimeException("Cannot projRight " + x);
			}
			ret.add(x.r);

		}

		return ret;
	}

	public static <X, Y> List<Chc<X, Y>> inLeft(Collection<X> l) {
		List<Chc<X, Y>> ret = (new ArrayList<>(l.size()));

		for (X x : l) {
			ret.add(inLeft(x));
		}

		return ret;
	}

	public static <X, Y> List<Chc<Y, X>> inRight(Collection<X> l) {
		List<Chc<Y, X>> ret = (new ArrayList<>(l.size()));

		for (X x : l) {
			ret.add(inRight(x));
		}

		return ret;
	}

	private Chc(Boolean left, X l, Y r) {
		this.left = left;
		if (left) {
			Util.assertNotNull(l);
		} else {
			Util.assertNotNull(r);
		}
		this.l = l;
		this.r = r;
	}

	
	public synchronized static <X, Y> Chc<X, Y> inLeft(X l) {
		return new Chc<>(true, l, null);
		
	}

	public synchronized static <X, Y> Chc<X, Y> inRight(Y r) {
		return new Chc<>(false, null, r);
	}

	public static <X, Y> Chc<X, Y> inLeftNC(X l) {
		Chc<X, Y> ret = new Chc<>(true, l, null);
		return ret;
	}

	public static <X, Y> Chc<X, Y> inRightNC(Y r) {
		Chc<X, Y> ret = new Chc<>(false, null, r);
		return ret;
	}
	
	@Override
	public String toString() {
		return left ? "inl " + l : "inr " + r;
	}

	public String toStringMash() {
		return left ? l.toString() : r.toString();
	}

	

	public void assertNeitherNull() {
		if (l == null && r == null) {
			throw new RuntimeException("Assertion failed: Chc containing both null");
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Chc<?, ?> other = (Chc<?,?>) obj;
		if (left != other.left)
			return false;
		if (left) {
			return l.equals(other.l);
		}
		return r.equals(other.r);

	}

	private int code = -1;
	@Override
	public synchronized int hashCode() {
		if (code != -1) {
			return code;
		}
		final int prime = 31;
		int result = 1;
		result = prime * result + ((l == null) ? 0 : l.hashCode());
		result = prime * result + (left ? 1231 : 1237);
		result = prime * result + ((r == null) ? 0 : r.hashCode());
		code = result;
		return result;
	}
	

	public static <X, Y> Set<Chc<X, Y>> or(Collection<X> xs, Set<Y> ys) {
		Set<Chc<X, Y>> ret = new HashSet<>(xs.size() + ys.size());
		for (X x : xs) {
			ret.add(Chc.inLeft(x));
		}
		for (Y y : ys) {
			ret.add(Chc.inRight(y));
		}
		return ret;
	}

	public Chc<Y, X> reverse() {
		if (left) {
			return Chc.inRight(l);
		}
		return Chc.inLeft(r);
	}


}
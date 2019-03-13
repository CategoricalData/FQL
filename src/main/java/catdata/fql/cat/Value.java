package catdata.fql.cat;

/**
 * 
 * @author ryan
 * 
 *         A value is either a constant, record, or choice.
 * 
 * @param <Y>
 *            type of tags
 * @param <X>
 *            type of data
 */
public class Value<Y, X> {

	public X x;

	private Value<Y, X>[] tuple;

	private Y tag;
	public Value<Y, X> tagCargo;

	public final VALUETYPE which;

	@Override
	public String toString() {
		String s = "";
		if (which == VALUETYPE.ATOM) {
			s = x.toString();
		} else if (which == VALUETYPE.RECORD) {
			s = "(" + printNicely(tuple) + ")";
		} else if (which == VALUETYPE.CHOICE) {
			s = "<" + tag + "_" + tagCargo + ">";
		}
		return s;
	}

	@SafeVarargs
	private final String printNicely(Value<Y, X>... G) {
		String s = " ";
		for (Value<Y, X> g : G) {
			s += (g + " ");
		}
		return s;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Value)) {
			throw new RuntimeException();
			// return false;
		}
		@SuppressWarnings("rawtypes")
		Value that = (Value) o;
		if (which == VALUETYPE.ATOM && that.which == VALUETYPE.ATOM) {
			return (x.equals(that.x));
		} else if (which == VALUETYPE.RECORD
				&& that.which == VALUETYPE.RECORD) {
			if (tuple.length != that.tuple.length) {
				return false;
			}
			for (int i = 0; i < tuple.length; i++) {
				if (!tuple[i].equals(that.tuple[i])) {
					return false;
				}
			}
			return true;
		} else if (which == VALUETYPE.CHOICE
				&& that.which == VALUETYPE.CHOICE) {
			return (tag.equals(that.tag) && tagCargo
					.equals(that.tagCargo));
		}
		throw new RuntimeException();
	}

	public enum VALUETYPE {
		ATOM, RECORD, CHOICE
	}

	Value(Y tag, Value<Y, X> tagCargo) {
		this.tag = tag;
		this.tagCargo = tagCargo;
        which = VALUETYPE.CHOICE;
	}

	public Value(X x) {
		this.x = x;
        which = VALUETYPE.ATOM;
	}

	@SafeVarargs
	Value(Value<Y, X>... tuple) {
		this.tuple = tuple;
        which = VALUETYPE.RECORD;
	}

}

package catdata.fqlpp.cat;

import java.util.Set;

// turns out this is useless because signatures *always* denote categories 
@SuppressWarnings("serial")
public class Sig extends Category<Signature<?, ?>, Mapping<?, ?, ?, ?>> {

	private Sig() { }
	
	@Override
	public boolean isInfinite() {
		return true;
	}

	@Override
	public String toString() {
		return "Sig";
	}

	@Override
	public boolean isObject(Signature<?,?> o) {
		return true;
	}

	@Override
	public boolean isArrow(Mapping<?,?,?,?> a) {
		return true;
	}

	public static Sig Sig = new Sig();

	//@Override
	//public boolean equals(Object o) {
	//	return (o == this);
	//}

	@Override
	public Set<Signature<?, ?>> objects() {
		throw new RuntimeException("Cannot enumerate objects of Sig.");
	}

	@Override
	public Set<Mapping<?, ?, ?, ?>> arrows() {
		throw new RuntimeException("Cannot enumerate arrows of Sig.");
	}

	@Override
	public Signature<?,?> source(Mapping<?,?,?,?> a) {
		return a.source;
	}

	@Override
	public Signature<?,?> target(Mapping<?,?,?,?> a) {
		return a.target;
	}

	@Override
	public Mapping<?,?,?,?> identity(Signature<?,?> o) {
		return Mapping.identity(o);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Mapping<?,?,?,?> compose(Mapping a1, Mapping a2) {
		return Mapping.compose(a1, a2);
	}

	// /////

	
}

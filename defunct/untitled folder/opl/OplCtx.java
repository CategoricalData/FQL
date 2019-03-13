package catdata.opl;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.opl.OplExp.OplSig;
import catdata.opl.OplParser.DoNotIgnore;

public class OplCtx<S, V> {
	final LinkedHashMap<V, S> vars0;

	public OplCtx(Map<V, S> m) {
		vars0 = new LinkedHashMap<>(m);
	}

	@Override
	public String toString() {
		List<String> l = new LinkedList<>();
		for (V k : vars0.keySet()) {
			if (vars0.get(k) != null) {
				l.add(k + ":" + vars0.get(k));
			} else {
				l.add(k.toString());
			}
		}
		String ret = Util.sep(l, ", ");
		return OplTerm.strip(ret);
	}

	public int indexOf(V var) {
		int i = -1;
		for (V k : vars0.keySet()) {
			i++;
			if (var.equals(k)) {
				break;
			}
		}
		return i;
	}

	public OplCtx(List<Pair<V, S>> vars) {
		vars0 = new LinkedHashMap<>();
		for (Pair<V, S> k : vars) {
			if (vars0.containsKey(k.first)) {
				throw new DoNotIgnore("Duplicate variable " + k.first);
			}
			vars0.put(k.first, k.second);
		}
	}

	public OplCtx() {
		vars0 = new LinkedHashMap<>();
	}

	public S get(V s) {
		S ret = vars0.get(s);
		if (s == null) {
			throw new DoNotIgnore("null var");
		}
		return ret;
	}

	public void validate(OplSig<S, ?, V> oplSig) {
		for (V k : vars0.keySet()) {
			S v = get(k);
			if (v == null) {
				throw new RuntimeException("Cannot infer type for " + k);
			}
			if (!oplSig.sorts.contains(v)) {
				throw new DoNotIgnore("Context has bad sort " + v);
			}
		}
	}

	public List<S> values() {
		List<S> ret = new LinkedList<>();
		for (V k : vars0.keySet()) {
			S v = vars0.get(k);
			ret.add(v);
		}
		return ret;
	}

	public List<Pair<V, S>> values2() {
		List<Pair<V, S>> ret = new LinkedList<>();
		for (V k : vars0.keySet()) {
			S v = vars0.get(k);
			ret.add(new Pair<>(k, v));
		}
		return ret;
	}

	public List<V> names() {
		List<V> ret = new LinkedList<>(vars0.keySet());
		return ret;
	}

	public <SS> OplCtx<SS, V> makeEnv(List<SS> env) {
		List<Pair<V, SS>> in = new LinkedList<>();
		int i = 0;
		for (V k : vars0.keySet()) {
			in.add(new Pair<>(k, env.get(i)));
			i++;
		}
		return new OplCtx<>(in);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((vars0 == null) ? 0 : vars0.hashCode());
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
		OplCtx<?, ?> other = (OplCtx<?, ?>) obj;
		if (vars0 == null) {
			if (other.vars0 != null)
				return false;
		} else if (!vars0.equals(other.vars0))
			return false;
		return true;
	}
	
	public <X> OplCtx<Chc<S,X>, V> inLeft() {
		List<Pair<V, Chc<S, X>>> l = new LinkedList<>();
		for (Pair<V, S> p : values2()) {
			l.add(new Pair<>(p.first, Chc.inLeft(p.second)));
		}
		return new OplCtx<>(l);
	}
	public <X> OplCtx<Chc<X,S>, V> inRight() {
		List<Pair<V, Chc<X, S>>> l = new LinkedList<>();
		for (Pair<V, S> p : values2()) {
			l.add(new Pair<>(p.first, Chc.inRight(p.second)));
		}
		return new OplCtx<>(l);
	}

}
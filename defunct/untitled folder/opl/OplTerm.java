package catdata.opl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.Invocable;
import javax.script.ScriptException;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.ide.DefunctGlobalOptions;
import catdata.opl.OplExp.OplSetInst;
import catdata.opl.OplExp.OplSig;
import catdata.opl.OplParser.DoNotIgnore;

public class OplTerm<C, V> implements Comparable<OplTerm<C, V>> {

	public V var;

	public C head;
	public List<OplTerm<C, V>> args;

	public OplTerm(V a) {
		if (a == null) {
			throw new RuntimeException();
		}
		var = a;
	}

	public boolean isGround(OplCtx<?, V> ctx) {
		if (var != null) {
			return ctx.vars0.containsKey(var);
		}
		for (OplTerm<C, V> t : args) {
			if (!t.isGround(ctx)) {
				return false;
			}
		}
		return true;
	}

	public OplTerm(C h, List<OplTerm<C, V>> a) {
		if (h == null || a == null) {
			throw new RuntimeException();
		}
		head = h;
		args = a;
	}

	JSWrapper eval(Invocable invokable) throws NoSuchMethodException, ScriptException {
		if (var != null) {
			throw new RuntimeException("Can only only evaluate closed terms.");
		}
		List<Object> args0 = new LinkedList<>();
		for (OplTerm<?, ?> t : args) {
			args0.add(t.eval(invokable).o);
		}
		Object ret = invokable.invokeFunction((String) head, args0);
		if (ret == null) {
			throw new RuntimeException("returned null from " + head + " on " + args0);
		}
		return new JSWrapper(ret);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((args == null) ? 0 : args.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		result = prime * result + ((var == null) ? 0 : var.hashCode());
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
		OplTerm<?, ?> other = (OplTerm<?, ?>) obj;
		if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		if (var == null) {
			if (other.var != null)
				return false;
		} else if (!var.equals(other.var))
			return false;
		return true;
	}

	public <S, X> X eval(OplSig<S, C, V> sig, OplSetInst<S, C, X> inst, OplCtx<X, V> env) {
		if (var != null) {
			return env.get(var);
		}
		Pair<List<S>, S> t = sig.getSymbol(head);
		List<X> actual = args.stream().map(x -> x.eval(sig, inst, env))
				.collect(Collectors.toList());
		if (t.first.size() != actual.size()) {
			throw new RuntimeException("Argument length mismatch for " + this + ", expected "
					+ t.first.size() + " but given " + actual.size());
		}
		Map<List<X>, X> m = inst.getSymbol(head);
		X ret = m.get(actual);
		if (ret == null) {
			throw new RuntimeException("Error, model provides no result for " + actual
					+ " on symbol " + head);
		}
		return ret;
	}

	public <S> S type(OplSig<S, C, V> sig, OplCtx<S, V> ctx) {
		if (var != null) {
			return ctx.get(var);
		}
		Pair<List<S>, S> t = sig.getSymbol(head);

		List<S> actual = args.stream().map(x -> x.type(sig, ctx)).collect(Collectors.toList());
		if (t.first.size() != actual.size()) {
			throw new DoNotIgnore("Argument length mismatch for " + this + ", expected "
					+ t.first.size() + " but given " + actual.size()) ;
		}
		for (int i = 0; i < actual.size(); i++) {
			if (!t.first.get(i).equals(actual.get(i))) {
				S z = actual.get(i);
				String y = z == null ? " (possible forgotten ())" : "";
				throw new DoNotIgnore("Argument mismatch for " + this
						+ ", expected term of sort " + t.first.get(i) + " but given "
						+ args.get(i) + " of sort " + z + y);
			}
		}

		return t.second;
	}

	@Override
	public String toString() {
		if (var != null) {
			return var.toString();
		}
		Integer i = termToNat(this);
		if (i != null) {
			return Integer.toString(i);
		}
		List<String> x = args.stream().map(OplTerm::toString).collect(Collectors.toList());
		String ret =  Util.maybeQuote(strip(head.toString())) + "(" + Util.sep(x, ", ") + ")";
		return ret;
	}
	
	public static <V> double termToDouble(OplTerm<?, V> t) {
		Integer i = termToNat(t);
		if (i == null) {
			throw new RuntimeException("Anomaly: please report");
		}
		return i;
	}

	private static <V> Integer termToNat(OplTerm<?, V> t) {
		if (t.var != null) {
			return null;
		}
		if (Util.stripChcs(t.head).second.equals("zero")) {
			return 0;
		} else if (Util.stripChcs(t.head).second.equals("succ")) {
			if (t.args.size() != 1) {
				return null;
			}
			Integer j = termToNat(t.args.get(0));
			if (j == null) {
				return null;
			}
			return 1 + j;
		}
		return null;
	}

	public static <V> OplTerm<String, V> natToTerm(int i) {
		if (i < 0) {
			throw new RuntimeException("Cannot convert negative number to natural");
		}
		if (i == 0) {
			return new OplTerm<>("zero", new LinkedList<>());
		}
		return new OplTerm<>("succ", Collections.singletonList(natToTerm(i - 1)));
	}

	//: dup? In Util?
	public static String strip(String s) {
		if (!DefunctGlobalOptions.debug.opl.opl_pretty_print) {
			return s;
		}
		String ret = s.replace("inl ", "").replace("inr ", "").replace("()", "")
				.replace("forall . ", "");
		if (ret.startsWith("|- ")) {
			ret = ret.substring(3);
		}
		if (ret.startsWith("\"") && ret.endsWith("\"")) {
			ret = ret.substring(1, ret.length()-1);
		}
		return ret;
	}

	public <S> OplTerm<C, V> subst(OplCtx<S, V> G, List<OplTerm<C, V>> L) {
		if (var != null) {
			int i = G.indexOf(var);
			if (i == -1) {
				return this;
			}
			return L.get(i);
		}
		List<OplTerm<C, V>> args0 = args.stream().map(x -> x.subst(G, L))
				.collect(Collectors.toList());
		return new OplTerm<>(head, args0);
	}

	public OplTerm<C, V> replace(OplTerm<C, V> toreplace, OplTerm<C, V> replacee) {
		if (equals(toreplace)) {
			return replacee;
		}
		List<OplTerm<C, V>> l = new LinkedList<>();
		for (OplTerm<C, V> a : args) {
			l.add(a.replace(toreplace, replacee));
		}
		return new OplTerm<>(head, l);
	}
	
	public boolean contains(OplTerm<C, V> lookingfor) {
		if (equals(lookingfor)) {
			return true;
		}
		for (OplTerm<C, V> a : args) {
			if (a.contains(lookingfor)) {
				return true;
			}
		}
		return false;
	}
	
	public OplTerm<C, V> subst(Map<V, OplTerm<C, V>> s) {
		if (var != null) {
			OplTerm<C, V> t = s.get(var);
			if (t != null) {
				return t;
			}
			return this;
		}
		List<OplTerm<C, V>> l = new LinkedList<>();
		for (OplTerm<C, V> a : args) {
			l.add(a.subst(s));
		}
		return new OplTerm<>(head, l);
	}

	public Set<C> symbols() {
		if (var != null) {
			return new HashSet<>();
		}
		Set<C> ret = new HashSet<>();
		ret.add(head);
		for (OplTerm<C, V> arg : args) {
			ret.addAll(arg.symbols());
		}
		return ret;
	}
	

	// needed for machine learning library for some reason
	@Override
	public int compareTo(OplTerm<C, V> o) {
		return o.toString().compareTo(toString());
	}
	
	public <X> OplTerm<Chc<C, X>, V> inLeft() {
		if (var != null) {
			return new OplTerm<>(var);
		}
		List<OplTerm<Chc<C, X>, V>> args0 = new LinkedList<>();
		for (OplTerm<C, V> a : args) {
			args0.add(a.inLeft());
		}
		return new OplTerm<>(Chc.inLeft(head), args0);
	}
	public <X> OplTerm<Chc<X, C>, V> inRight() {
		if (var != null) {
			return new OplTerm<>(var);
		}
		List<OplTerm<Chc<X, C>, V>> args0 = new LinkedList<>();
		for (OplTerm<C, V> a : args) {
			args0.add(a.inRight());
		}
		return new OplTerm<>(Chc.inRight(head), args0);
	}
/*
	private static <C, X, V> OplTerm<C, V> deLeft(OplTerm<Chc<C, X>, V> e) {
		if (e.var != null) {
			return new OplTerm<>(e.var);
		}
		if (!e.head.left) {
			throw new RuntimeException("Cannot de-left " + e);
		}
		List<OplTerm<C, V>> args0 = new LinkedList<>();
		for (OplTerm<Chc<C, X>, V> a : e.args) {
			args0.add(deLeft(a));
		}
		return new OplTerm<>(e.head.l, args0);
	} */

	V getHead() {
		if (var != null) {
			return var;
		}
		if (args.size() != 1) {
			throw new RuntimeException("Report to Ryan " + this);
		}
		return args.get(0).getHead();		
	}

}
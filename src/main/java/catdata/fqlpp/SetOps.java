package catdata.fqlpp;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import catdata.Pair;
import catdata.fqlpp.FnExp.Apply;
import catdata.fqlpp.FnExp.ApplyTrans;
import catdata.fqlpp.FnExp.Case;
import catdata.fqlpp.FnExp.Chr;
import catdata.fqlpp.FnExp.Comp;
import catdata.fqlpp.FnExp.Const;
import catdata.fqlpp.FnExp.Curry;
import catdata.fqlpp.FnExp.Eval;
import catdata.fqlpp.FnExp.FF;
import catdata.fqlpp.FnExp.FnExpVisitor;
import catdata.fqlpp.FnExp.Fst;
import catdata.fqlpp.FnExp.Id;
import catdata.fqlpp.FnExp.Inl;
import catdata.fqlpp.FnExp.Inr;
import catdata.fqlpp.FnExp.Iso;
import catdata.fqlpp.FnExp.Krnl;
import catdata.fqlpp.FnExp.Prod;
import catdata.fqlpp.FnExp.Snd;
import catdata.fqlpp.FnExp.TT;
import catdata.fqlpp.FnExp.Tru;
import catdata.fqlpp.FnExp.Var;
import catdata.fqlpp.SetExp.Cod;
import catdata.fqlpp.SetExp.Dom;
import catdata.fqlpp.SetExp.Exp;
import catdata.fqlpp.SetExp.Intersect;
import catdata.fqlpp.SetExp.Numeral;
import catdata.fqlpp.SetExp.One;
import catdata.fqlpp.SetExp.Plus;
import catdata.fqlpp.SetExp.Prop;
import catdata.fqlpp.SetExp.Range;
import catdata.fqlpp.SetExp.SetExpVisitor;
import catdata.fqlpp.SetExp.Times;
import catdata.fqlpp.SetExp.Union;
import catdata.fqlpp.SetExp.Zero;
import catdata.fqlpp.cat.FinSet;
import catdata.fqlpp.cat.FinSet.Fn;
import catdata.fqlpp.cat.Functor;
import catdata.fqlpp.cat.Transform;

@SuppressWarnings({ "rawtypes", "serial" })
public class SetOps implements SetExpVisitor<Set<?>, FQLPPProgram>, FnExpVisitor<Fn, FQLPPProgram> , Serializable {

	private FQLPPEnvironment ENV;
	public SetOps(FQLPPEnvironment ENV) {
		this.ENV = ENV;
	}
	
	@SuppressWarnings("unused")
	private SetOps() { }
	
	@SuppressWarnings("unchecked")
	@Override
	public Set visit(FQLPPProgram env, Union e) {
		Set s = e.set.accept(env, this);
		Set s1 = e.set1.accept(env, this);
		Set ret = new HashSet<>();
		ret.addAll(s);
		ret.addAll(s1);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set visit(FQLPPProgram env, Intersect e) {
		Set s = e.set.accept(env, this);
		Set s1 = e.set1.accept(env, this);
		Set ret = new HashSet<>();
		for (Object x : s) {
			if (s1.contains(x)) {
				ret.add(x);
			}
		}
		return ret;
	}
	
	@Override
	public Fn visit(FQLPPProgram env, Id e) {
		Set<?> s = e.t.accept(env, this);
		return FinSet.FinSet.identity(s);
	}

	@Override
	public Fn visit(FQLPPProgram env, Comp e) {
		Fn l = e.l.accept(env, this);
		Fn r = e.r.accept(env, this);
		return FinSet.FinSet.compose(l, r);
	}

	@Override
	public Fn visit(FQLPPProgram env, Var e) {
		Fn k = ENV.fns.get(e.v);
		if (k == null) {
			throw new RuntimeException("Missing function: " + e);
		}
		return k;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Const e) {
		return new Fn(e.src.accept(env, this), e.dst.accept(env, this), e.f);
	}

	@Override
	public Fn visit(FQLPPProgram env, TT e) {
		Set<?> s = e.t.accept(env, this);
		return FinSet.terminal(s);
	}

	@Override
	public Fn visit(FQLPPProgram env, FF e) {
		Set<?> s = e.t.accept(env, this);
		return FinSet.initial(s);
	}

	@Override
	public Fn visit(FQLPPProgram env, Fst e) {
		Set<?> s = e.s.accept(env, this);
		Set<?> t = e.t.accept(env, this);
		return FinSet.first(s, t);
	}

	@Override
	public Fn visit(FQLPPProgram env, Snd e) {
		Set<?> s = e.s.accept(env, this);
		Set<?> t = e.t.accept(env, this);
		return FinSet.second(s, t);
	}

	@Override
	public Fn visit(FQLPPProgram env, Inl e) {
		Set<?> s = e.s.accept(env, this);
		Set<?> t = e.t.accept(env, this);
		return FinSet.inleft(s, t);

	}

	@Override
	public Fn visit(FQLPPProgram env, Inr e) {
		Set<?> s = e.s.accept(env, this);
		Set<?> t = e.t.accept(env, this);
		return FinSet.inright(s, t);
	}

	@Override
	public Fn visit(FQLPPProgram env, Eval e) {
		Set<?> s = e.s.accept(env, this);
		Set<?> t = e.t.accept(env, this);
		return FinSet.FinSet.eval(s, t);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Curry e) {
		Fn f = e.f.accept(env, this);
		return FinSet.FinSet.curry(f);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Case e) {
		Fn s = e.l.accept(env, this);
		Fn t = e.r.accept(env, this);
		return FinSet.match(s, t);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Prod e) {
		Fn s = e.l.accept(env, this);
		Fn t = e.r.accept(env, this);
		return FinSet.pair(s, t);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Iso e) {
		Set l = e.l.accept(env, this);
		Set r = e.r.accept(env, this);
		Optional<Pair<Fn, Fn>> k = FinSet.iso(l, r);
		if (!k.isPresent()) {
			throw new RuntimeException("Not isomorphic: " + l + " and " + r);
		}
        return e.lToR ? k.get().first : k.get().second;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Chr e) {
		Fn f = e.f.accept(env, this);
		return FinSet.chr(f);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Krnl e) {
		Fn f = e.f.accept(env, this);
		return FinSet.kernel(f);
	}

	@Override
	public Set<?> visit(FQLPPProgram env, Zero e) {
		return FinSet.initial();
	}

	@Override
	public Set<?> visit(FQLPPProgram env, One e) {
		return FinSet.terminal();
	}

	@Override
	public Set<?> visit(FQLPPProgram env, Prop e) {
		return FinSet.prop();
	}

	@Override
	public Set<?> visit(FQLPPProgram env, Plus e) {
		Set<?> a = e.a.accept(env, this);
		Set<?> b = e.b.accept(env, this);
		return FinSet.coproduct(a, b);
	}

	@Override
	public Set<?> visit(FQLPPProgram env, Times e) {
		Set<?> a = e.a.accept(env, this);
		Set<?> b = e.b.accept(env, this);
		return FinSet.product(a, b);
	}

	@Override
	public Set<?> visit(FQLPPProgram env, Exp e) {
		Set<?> a = e.a.accept(env, this);
		Set<?> b = e.b.accept(env, this);
		return FinSet.FinSet.exp(a, b);
	}

	@Override
	public Set<?> visit(FQLPPProgram env, SetExp.Var e) {
		Set x = ENV.sets.get(e.v);
		if (x == null) {
			throw new RuntimeException("Missing set: " + e);
		}
		return x;
	}

	@Override
	public Set<?> visit(FQLPPProgram env, Dom e) {
		Fn f = e.f.accept(env, this);
		return f.source;
	}
	
	@Override
	public Set<?> visit(FQLPPProgram env, Cod e) {
		Fn f = e.f.accept(env, this);
		return f.target;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<?> visit(FQLPPProgram env, Range e) {
		Fn f = e.f.accept(env, this);
		Set<Object> ret = new HashSet<>();
		for (Object o : f.source) {
			ret.add(f.apply(o));
		}
		return ret;
	}

	@Override
	public Set<?> visit(FQLPPProgram env, SetExp.Const e) {
		return e.s;
	}

	@Override
	public Fn visit(FQLPPProgram env, Tru e) {
        switch (e.str) {
            case "true":
                return FinSet.tru();
            case "false":
                return FinSet.fals();
            case "and":
                return FinSet.and();
            case "or":
                return FinSet.or();
            case "not":
                return FinSet.not();
            case "implies":
                return FinSet.implies();
            default:
                throw new RuntimeException();
        }
	}

	@Override
	public Set<?> visit(FQLPPProgram env, Numeral e) {
		Set<Object> ret = new HashSet<>();
		if (e.i < 0) {
			throw new RuntimeException(e + " is negative.");
		}
		for (int i = 0; i < e.i; i++) {
			ret.add(Integer.toString(i));
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<?> visit(FQLPPProgram env, SetExp.Apply e) {
		FunctorExp k = env.ftrs.get(e.f);
		if (k == null) {
			throw new RuntimeException("Missing functor: " + e.f);
		}
		Set<?> s = e.set.accept(env, this);
		Functor f = k.accept(env, new CatOps(ENV));
		if (!FinSet.FinSet.equals(f.source)) {
			throw new RuntimeException("Domain is not Set in " + e);
		}
		if (!FinSet.FinSet.equals(f.target)) {
			throw new RuntimeException("Codomain is not Set in " + e);
		}
		return (Set<?>) f.applyO(s);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, Apply e) {
		FunctorExp k = env.ftrs.get(e.f);
		if (k == null) {
			throw new RuntimeException("Missing functor: " + e.f);
		}
		Fn s = e.set.accept(env, this);
		Functor f = k.accept(env, new CatOps(ENV));
		if (!FinSet.FinSet.equals(f.source)) {
			throw new RuntimeException("Domain is not Set in " + e);
		}
		if (!FinSet.FinSet.equals(f.target)) {
			throw new RuntimeException("Codomain is not Set in " + e);
		}
		return (Fn) f.applyA(s);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Fn visit(FQLPPProgram env, ApplyTrans e) {
		TransExp k = env.trans.get(e.f);
		if (k == null) {
			throw new RuntimeException("Missing transform: " + e.f);
		}
		Transform f = k.accept(env, new CatOps(ENV));
		if (!FinSet.FinSet.equals(f.source.source)) {
			throw new RuntimeException("Domain is not Set in " + e);
		}
		if (!FinSet.FinSet.equals(f.target.target)) {
			throw new RuntimeException("Codomain is not Set in " + e);
		}
		Set<?> s = e.set.accept(env, this);
		return (Fn) f.apply(s);
	}

}

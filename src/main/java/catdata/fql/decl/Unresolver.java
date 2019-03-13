package catdata.fql.decl;

import java.util.Map;

import catdata.fql.decl.SigExp.Const;
import catdata.fql.decl.SigExp.Exp;
import catdata.fql.decl.SigExp.One;
import catdata.fql.decl.SigExp.Opposite;
import catdata.fql.decl.SigExp.Plus;
import catdata.fql.decl.SigExp.SigExpVisitor;
import catdata.fql.decl.SigExp.Times;
import catdata.fql.decl.SigExp.Union;
import catdata.fql.decl.SigExp.Unknown;
import catdata.fql.decl.SigExp.Var;
import catdata.fql.decl.SigExp.Zero;

public class Unresolver implements SigExpVisitor<SigExp, Map<String, SigExp>> {

	@Override
	public SigExp visit(Map<String, SigExp> env, Zero e) {
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(e)) {
				return new Var(k);
			}
		}
		return e;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, One e) {
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(e)) {
				return new Var(k);
			}
		}
		return e;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, Plus e) {
		SigExp t = new Plus(e.a.accept(env, this), e.b.accept(env, this));
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(t)) {
				return new Var(k);
			}
		}
		return t;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, Times e) {
		SigExp t = new Times(e.a.accept(env, this), e.b.accept(env, this));
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(t)) {
				return new Var(k);
			}
		}
		return t;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, Exp e) {
		SigExp t = new Exp(e.a.accept(env, this), e.b.accept(env, this));
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(t)) {
				return new Var(k);
			}
		}
		return t;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, Var e) {
		return e;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, Const e) {
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(e)) {
				return new Var(k);
			}
		}
		return e;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, Union e) {
		SigExp t = new Union(e.l.accept(env, this), e.r.accept(env, this));
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(t)) {
				return new Var(k);
			}
		}
		return t;
	}

	@Override
	public SigExp visit(Map<String, SigExp> env, Opposite e) {
		SigExp t = new Opposite(e.e.accept(env, this));
		for (String k : env.keySet()) {
			SigExp e0 = env.get(k);
			if (e0.equals(t)) {
				return new Var(k);
			}
		}
		return t;
	}
	
	@Override
	public Const visit(Map<String, SigExp> env, Unknown e) {
		throw new RuntimeException("Encountered unknown type.");
	}

	
}
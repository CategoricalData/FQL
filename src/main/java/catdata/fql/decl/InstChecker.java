package catdata.fql.decl;

import catdata.fql.FQLException;
import catdata.fql.decl.InstExp.Const;
import catdata.fql.decl.InstExp.Delta;
import catdata.fql.decl.InstExp.Eval;
import catdata.fql.decl.InstExp.Exp;
import catdata.fql.decl.InstExp.External;
import catdata.fql.decl.InstExp.FullEval;
import catdata.fql.decl.InstExp.FullSigma;
import catdata.fql.decl.InstExp.InstExpVisitor;
import catdata.fql.decl.InstExp.Kernel;
import catdata.fql.decl.InstExp.One;
import catdata.fql.decl.InstExp.Pi;
import catdata.fql.decl.InstExp.Plus;
import catdata.fql.decl.InstExp.Relationalize;
import catdata.fql.decl.InstExp.Sigma;
import catdata.fql.decl.InstExp.Step;
import catdata.fql.decl.InstExp.Times;
import catdata.fql.decl.InstExp.Two;
import catdata.fql.decl.InstExp.Zero;
import catdata.Pair;
import catdata.Triple;

public class InstChecker implements InstExpVisitor<SigExp, FQLProgram> {

	@Override
	public SigExp visit(FQLProgram env, Zero e) {
		return e.sig.typeOf(env);
	}

	@Override
	public SigExp visit(FQLProgram env, One e) {
		SigExp s = e.sig.typeOf(env);
		SigExp.Const sig = s.toConst(env);
		for (Triple<String, String, String> k : sig.attrs) {
			if (k.third.equals("string") || k.third.equals("int")) {
				throw new RuntimeException("Cannot use unit with string or int (try enums instead).");
			}
		}
		return s;
	}

	@Override
	public SigExp visit(FQLProgram env, Two e) {
		SigExp s = e.sig.typeOf(env);
		SigExp.Const sig = s.toConst(env);
		for (Triple<String, String, String> k : sig.attrs) {
			if (k.third.equals("string") || k.third.equals("int")) {
				throw new RuntimeException("Cannot use prop with string or int (try enums instead).");
			}
		}
		return s;
	}
	
	private SigExp visit2(FQLProgram env, String a, String b) {
		if (!env.insts.containsKey(a)) {
			throw new RuntimeException("Missing instance: " + a);
		}
		if (!env.insts.containsKey(b)) {
			throw new RuntimeException("Missing instance: " + a);
		}
		SigExp lt = env.insts.get(a).accept(env, this);
		SigExp rt = env.insts.get(b).accept(env, this);
		if (!lt.equals(rt)) {
			throw new RuntimeException("Not of same type: " + lt.unresolve(env.sigs)
					+ " and " + rt.unresolve(env.sigs));
		}
		return lt;	
	}

	@Override
	public SigExp visit(FQLProgram env, Plus e) {
		SigExp x = visit2(env, e.a, e.b);
		if (e.a.equals(e.b)) {
			throw new RuntimeException("Cannot sum the same instance");
		}
		return x;
	}

	@Override
	public SigExp visit(FQLProgram env, Times e) {
		return visit2(env, e.a, e.b);
	}

	@Override
	public SigExp visit(FQLProgram env, Exp e) {
		SigExp ret = visit2(env, e.a, e.b);
		SigExp.Const sig = ret.toConst(env);
		for (Triple<String, String, String> k : sig.attrs) {
			if (k.third.equals("string") || k.third.equals("int")) {
				throw new RuntimeException("Cannot use exponentials with string or int (try enums instead).");
			}
		}
		return ret;
	}

	/*
	 * @Override public SigExp visit( Quad<Map<String, SigExp>, Map<String,
	 * MapExp>, Map<String, InstExp>, Map<String, QueryExp>> env, Var e) { if
	 * (seen.contains(e.v)) { throw new RuntimeException("Cyclic definition: " +
	 * e.v); } seen.add(e.v); InstExp i = env.third.get(e.v); if (i == null) {
	 * throw new RuntimeException("Unknown instance " + e); } return
	 * i.accept(env, this); }
	 */

	@SuppressWarnings("unused")
	@Override
	public SigExp visit(FQLProgram env, Const e) {
		SigExp k = e.sig.typeOf(env);
		try {
			new Instance(k.toSig(env), e.data);
			return k;
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	@Override
	public SigExp visit(FQLProgram env, Delta e) {
		InstExp xxx = env.insts.get(e.I);
		if (xxx == null) {
			throw new RuntimeException("Instance not found: " + e.I);
		}
		if (e.equals(xxx)) {
			throw new RuntimeException("Circular: " + e);
		}
		SigExp it = xxx.accept(env, this);
		Pair<SigExp, SigExp> ft = e.F.type(env);
		if (!ft.second.equals(it)) {
			throw new RuntimeException("In " + e + " expected instance to be "
					+ ft.second.unresolve(env.sigs) + " but is " + it.unresolve(env.sigs));
		}
		return ft.first;
	}

	@Override
	//  check disc op fib
	public SigExp visit(FQLProgram env, Sigma e) {
		InstExp xxx = env.insts.get(e.I);
		if (xxx == null) {
			throw new RuntimeException("Instance not found: " + e.I);
		}
		if (e.equals(xxx)) {
			throw new RuntimeException("Circular: " + e);
		}
		SigExp it = xxx.accept(env, this);
		Pair<SigExp, SigExp> ft = e.F.type(env);
		if (!ft.first.equals(it)) {
			throw new RuntimeException("In " + e + " expected instance to be "
					+ ft.first.unresolve(env.sigs) + " but is " + it.unresolve(env.sigs));
		}
		return ft.second;
	}

	@Override
	// check bijection
	public SigExp visit(FQLProgram env, Pi e) {
		InstExp xxx = env.insts.get(e.I);
		if (xxx == null) {
			throw new RuntimeException("Instance not found: " + e.I);
		}
		if (e.equals(xxx)) {
			throw new RuntimeException("Circular: " + e);
		}
		SigExp it = xxx.accept(env, this);
		Pair<SigExp, SigExp> ft = e.F.type(env);
		if (!ft.first.equals(it)) {
			throw new RuntimeException("In " + e + " expected instance to be "
					+ ft.first.unresolve(env.sigs) + " but is " + it.unresolve(env.sigs));
		}
		return ft.second;
	}

	@Override
	public SigExp visit(FQLProgram env, FullSigma e) {
		InstExp xxx = env.insts.get(e.I);
		if (xxx == null) {
			throw new RuntimeException("Instance not found: " + e.I);
		}
		if (e.equals(xxx)) {
			throw new RuntimeException("Circular: " + e);
		}
		SigExp it = xxx.accept(env, this);
		Pair<SigExp, SigExp> ft = e.F.type(env);
		if (!ft.first.equals(it)) {
			throw new RuntimeException("In " + e + " expected instance to be "
					+ ft.first.unresolve(env.sigs) + " but is " + it.unresolve(env.sigs));
		}
		return ft.second;
	}

	@Override
	public SigExp visit(FQLProgram env, Relationalize e) {
		InstExp xxx = env.insts.get(e.I);
		if (xxx == null) {
			throw new RuntimeException("Instance not found: " + e.I);
		}
		if (e.equals(xxx)) {
			throw new RuntimeException("Circular: " + e);
		}
		return xxx.accept(env, this);
	}

	@Override
	public SigExp visit(FQLProgram env, External e) {
		return e.sig.typeOf(env);
	}

	@Override
	public SigExp visit(FQLProgram env, Eval e) {
		Pair<SigExp, SigExp> k = e.q.type(env);
		InstExp xxx = env.insts.get(e.e);
		if (null == env.insts.get(e.e)) {
			throw new RuntimeException("Unknown: " + e.e);
		}
		if (e.equals(xxx)) {
			throw new RuntimeException("Circular: " + e);
		}
		SigExp v = xxx.accept(env, this);
		if (!(k.first.equals(v))) {
			throw new RuntimeException("On " + e + ", expected input to be "
					+ k.first.unresolve(env.sigs) + " but computed " + v.unresolve(env.sigs));
		}
		return k.second;
	}

	@Override
	public SigExp visit(FQLProgram env, FullEval e) {
		Pair<SigExp, SigExp> k = e.q.type(env);
		InstExp xxx = env.insts.get(e.e);
		if (null == env.insts.get(e.e)) {
			throw new RuntimeException("Unknown: " + e.e);
		}
		if (e.equals(xxx)) {
			throw new RuntimeException("Circular: " + e);
		}
		SigExp v = xxx.accept(env, this);
		if (!(k.first.equals(v))) {
			throw new RuntimeException("On " + e + ", expected input to be "
					+ k.first.unresolve(env.sigs) + " but computed " + v.unresolve(env.sigs));
		}
		return k.second;
	}

	@Override
	public SigExp visit(FQLProgram env, Kernel e) {
		TransExp t = env.transforms.get(e.trans);
		if (t == null) {
			throw new RuntimeException("Missing transform: " + e.trans);
		}
		Pair<String, String> u = t.type(env);
		return env.insts.get(u.first).accept(env, this);
	}

	@Override
	public SigExp visit(FQLProgram env, Step e) {
		Pair<SigExp, SigExp> m = e.m.type(env);
		Pair<SigExp, SigExp> n = e.n.type(env);
		if (!m.second.equals(n.first)) {
			throw new RuntimeException("Mappings do not compose in " + e);
		}
		InstExp i = env.insts.get(e.I);
		if (i == null) {
			throw new RuntimeException("Missing instance: " + e.I);
		}
		return i.accept(env, this);
	}

	

}

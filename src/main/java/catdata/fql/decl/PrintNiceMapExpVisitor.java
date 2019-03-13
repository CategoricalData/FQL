package catdata.fql.decl;

import catdata.fql.decl.MapExp.Apply;
import catdata.fql.decl.MapExp.Case;
import catdata.fql.decl.MapExp.Comp;
import catdata.fql.decl.MapExp.Const;
import catdata.fql.decl.MapExp.Curry;
import catdata.fql.decl.MapExp.Dist1;
import catdata.fql.decl.MapExp.Dist2;
import catdata.fql.decl.MapExp.FF;
import catdata.fql.decl.MapExp.Fst;
import catdata.fql.decl.MapExp.Id;
import catdata.fql.decl.MapExp.Inl;
import catdata.fql.decl.MapExp.Inr;
import catdata.fql.decl.MapExp.Iso;
import catdata.fql.decl.MapExp.MapExpVisitor;
import catdata.fql.decl.MapExp.Opposite;
import catdata.fql.decl.MapExp.Prod;
import catdata.fql.decl.MapExp.Snd;
import catdata.fql.decl.MapExp.Sub;
import catdata.fql.decl.MapExp.TT;
import catdata.fql.decl.MapExp.Var;

public class PrintNiceMapExpVisitor implements MapExpVisitor<String, FQLProgram> {

	@Override
	public String visit(FQLProgram env, Id e) {
		return "id " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Comp e) {
		return "(" + e.l.accept(env, this) + " then " + e.r.accept(env, this) + ")";
	}

	@Override
	public String visit(FQLProgram env, Dist1 e) {
		throw new RuntimeException();
	}

	@Override
	public String visit(FQLProgram env, Dist2 e) {
		throw new RuntimeException();
	}

	@Override
	public String visit(FQLProgram env, Var e) {
		return e.v;
	}

	@Override
	public String visit(FQLProgram env, Const e) {
		return e + " : " + e.src.unresolve(env.sigs) + " -> " + e.dst.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, TT e) {
		return "unit " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, FF e) {
		return "void " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Fst e) {
		return "fst " + e.s.unresolve(env.sigs) + " " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Snd e) {
		return "snd " + e.s.unresolve(env.sigs) + " " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Inl e) {
		return "inl " + e.s.unresolve(env.sigs) + " " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Inr e) {
		return "inr " + e.s.unresolve(env.sigs) + " " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Apply e) {
		return "eval " + e.s.unresolve(env.sigs) + " " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Curry e) {
		return "curry " + e.f.accept(env, this);
	}

	@Override
	public String visit(FQLProgram env, Case e) {
		return "(" + e.l.accept(env, this) + " + " + e.r.accept(env, this) + ")";
	}

	@Override
	public String visit(FQLProgram env, Prod e) {
		return "(" + e.l.accept(env, this) + " * " + e.r.accept(env, this) + ")";
	}

	@Override
	public String visit(FQLProgram env, Sub e) {
		return "subschema " + e.s.unresolve(env.sigs) + " " + e.t.unresolve(env.sigs);
	}

	@Override
	public String visit(FQLProgram env, Opposite e) {
		return "opposite " + e.e.accept(env, this);
	}

	@Override
	public String visit(FQLProgram env, Iso e) {
		String x = e.lToR ? "1" : "2";
		return "iso" + x + " " + e.l.unresolve(env.sigs) + " " + e.r.unresolve(env.sigs);
	}

}

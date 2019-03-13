package catdata.fqlpp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.fqlpp.CatExp.CatExpVisitor;
import catdata.fqlpp.CatExp.Cod;
import catdata.fqlpp.CatExp.Colim;
import catdata.fqlpp.CatExp.Const;
import catdata.fqlpp.CatExp.Dom;
import catdata.fqlpp.CatExp.Exp;
import catdata.fqlpp.CatExp.Kleisli;
import catdata.fqlpp.CatExp.Named;
import catdata.fqlpp.CatExp.One;
import catdata.fqlpp.CatExp.Plus;
import catdata.fqlpp.CatExp.Times;
import catdata.fqlpp.CatExp.Union;
import catdata.fqlpp.CatExp.Var;
import catdata.fqlpp.CatExp.Zero;

public class PreProcessor implements CatExpVisitor<CatExp, FQLPPProgram> {

	@Override
	public CatExp visit(FQLPPProgram env, Zero e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, One e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, Plus e) {
		return new Plus(e.a.accept(env, this), e.b.accept(env, this));
	}

	@Override
	public CatExp visit(FQLPPProgram env, Times e) {
		return new Times(e.a.accept(env, this), e.b.accept(env, this));
	}

	@Override
	public CatExp visit(FQLPPProgram env, Exp e) {
		return new Exp(e.a.accept(env, this), e.b.accept(env, this));

	}

	@Override
	public CatExp visit(FQLPPProgram env, Var e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, Const e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, Dom e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, Cod e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, Named e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, Kleisli e) {
		return e;
	}

	@Override
	public CatExp visit(FQLPPProgram env, Union e) {
		CatExp l = e.l.accept(env, this);
		CatExp r = e.r.accept(env, this);
		CatExp lx= CatOps.resolve(env, l);
		CatExp rx= CatOps.resolve(env, r);
		if (!(lx instanceof Const)) {
			throw new RuntimeException("Not a const: " + lx);
		}
		if (!(rx instanceof Const)) {
			throw new RuntimeException("Not a const: " + rx);
		}
		Const ly = (Const) lx;
		Const ry = (Const) rx;
		Set<Pair<Pair<String, List<String>>, Pair<String, List<String>>>> eqs = new HashSet<>();
		Set<Triple<String, String, String>> arrows = new HashSet<>();
		Set<String> nodes = new HashSet<>();
		nodes.addAll(ly.nodes);
		nodes.addAll(ry.nodes);
		arrows.addAll(ly.arrows);
		arrows.addAll(ry.arrows);
		eqs.addAll(ly.eqs);
		eqs.addAll(ry.eqs);
		return new Const(nodes, arrows, eqs);
	}

	@Override
	public CatExp visit(FQLPPProgram env, Colim e) {
		return e;
	}

}

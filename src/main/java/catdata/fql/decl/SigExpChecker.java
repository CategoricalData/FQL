package catdata.fql.decl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import catdata.fql.FQLException;
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
import catdata.Pair;
import catdata.Triple;

public class SigExpChecker implements SigExpVisitor<SigExp, FQLProgram>{

	private List<String> seen = new LinkedList<>();
	
	@Override
	public SigExp visit(FQLProgram env, Zero e) {
		return e;
	}

	@Override
	public SigExp visit(FQLProgram env, One e) {
		return e;
	}

	@Override
	public SigExp visit(FQLProgram env, Plus e) {
		List<String> s = new LinkedList<>(seen);
		SigExp a = e.a.accept(env, this);
		seen = s;
		SigExp b = e.b.accept(env, this);
		seen = s;
		return new Plus(a,b);
	}

	@Override
	public SigExp visit(FQLProgram env, Times e) {
		List<String> s = new LinkedList<>(seen);
		SigExp a = e.a.accept(env, this);
		seen = s;
		SigExp b = e.b.accept(env, this);
		seen = s;
		return new Times(a,b);
	}

	@Override
	public SigExp visit(FQLProgram env, Exp e) {
		List<String> s = new LinkedList<>(seen);
		SigExp a = e.a.accept(env, this);
		seen = s;
		SigExp b = e.b.accept(env, this);
		seen = s;
		return new Exp(a,b);
	}

	@Override
	public SigExp visit(FQLProgram env, Var e) {
		if (seen.contains(e.v)) {
			throw new RuntimeException("Cyclic definition: " + e);
		}
		seen.add(e.v);
		SigExp r = env.sigs.get(e.v);
		if (r == null) {
			throw new RuntimeException("Unknown schema: " + e);
		}
		return r.accept(env, this);
	}

	@SuppressWarnings("unused")
	@Override
	public SigExp visit(FQLProgram env, Const e) {
		try {
			new Signature(env.enums, e.nodes, e.attrs, e.arrows, e.eqs);
		} catch (FQLException ee) {
			ee.printStackTrace();
			throw new RuntimeException(ee.getLocalizedMessage()); // + " in " + e);
		}
		return e;
	}

	@Override
	public SigExp visit(FQLProgram env, Union e) {
		SigExp lt = e.l.typeOf(env);
		SigExp rt = e.r.typeOf(env);
		if (! (lt instanceof Const)) {
			throw new RuntimeException(e.l + " does not have constant schema, has " + lt);
		}
		if (! (rt instanceof Const)) {
			throw new RuntimeException(e.r + " does not have constant schema, has " + lt);
		}
		Const lt0 = (Const) lt;
		Const rt0 = (Const) rt;
		
		
		Set<String> nodes = new HashSet<>(lt0.nodes);
		nodes.addAll(rt0.nodes);
		Set<Triple<String, String, String>> attrs = new HashSet<>(lt0.attrs);
		attrs.addAll(rt0.attrs);
		Set<Triple<String, String, String>> arrows = new HashSet<>(lt0.arrows);
		arrows.addAll(rt0.arrows);
		Set<Pair<List<String>, List<String>>> eqs = new HashSet<>(lt0.eqs);
		eqs.addAll(rt0.eqs);
		
		return new Const(new LinkedList<>(nodes), new LinkedList<>(attrs), new LinkedList<>(arrows), new LinkedList<>(eqs));
	}

	@Override
	public SigExp visit(FQLProgram env, Opposite e) {
		return new Opposite(e.e.accept(env, this));
	}

	@Override
	public SigExp visit(FQLProgram env, Unknown e) {
		throw new RuntimeException("Encountered unknown type");
	}
	
}

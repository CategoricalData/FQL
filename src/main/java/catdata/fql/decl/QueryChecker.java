package catdata.fql.decl;

import java.util.LinkedList;
import java.util.List;

import catdata.Pair;
import catdata.fql.decl.QueryExp.Comp;
import catdata.fql.decl.QueryExp.Const;
import catdata.fql.decl.QueryExp.QueryExpVisitor;
import catdata.fql.decl.QueryExp.Var;

public class QueryChecker implements
		QueryExpVisitor<Pair<SigExp, SigExp>, FQLProgram> {

	private List<String> seen = new LinkedList<>();

	@Override
	public Pair<SigExp, SigExp> visit(FQLProgram env, Const e) {
		Pair<SigExp, SigExp> d = e.delta.type(env);
		Pair<SigExp, SigExp> p = e.pi.type(env);
		Pair<SigExp, SigExp> s = e.sigma.type(env);

		if (!d.first.equals(p.first)) {
			throw new RuntimeException("Mismatch: " + d.first + " and "
					+ p.first);
		}
		if (!p.second.equals(s.first)) {
			throw new RuntimeException("Mismatch: " + p.second + " and "
					+ s.first);
		}

		return new Pair<>(d.second, s.second);
	}

	@Override
	public Pair<SigExp, SigExp> visit(FQLProgram env, Comp e) {
		List<String> x = new LinkedList<>(seen);
		Pair<SigExp, SigExp> lt = e.l.accept(env, this);
		seen = x;
		Pair<SigExp, SigExp> rt = e.r.accept(env, this);
		seen = x;
		if (!lt.second.equals(rt.first)) {
			throw new RuntimeException("Mismatch: " + lt.second + " and "
					+ rt.first);
		}
		return new Pair<>(lt.first, rt.second);
	}

	@Override
	public Pair<SigExp, SigExp> visit(FQLProgram env, Var e) {
		if (seen.contains(e.v)) {
			throw new RuntimeException("Circular: " + e.v);
		}
		seen.add(e.v);
		QueryExp q = env.queries.get(e.v);
		if (q == null) {
			throw new RuntimeException("Unknown query: " + e.v);
		}
		return q.accept(env, this);
	}

	
/*
	private boolean contains(List<Triple<String, String, String>> attrs,
			String s) {
		for (Triple<String, String, String> a : attrs) {
			if (a.first.equals(s)) {
				return true;
			}
		}
		return false;
	} */
}

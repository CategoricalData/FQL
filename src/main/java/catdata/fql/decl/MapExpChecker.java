package catdata.fql.decl;

import java.util.LinkedList;
import java.util.List;

import catdata.Pair;
import catdata.Triple;
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
import catdata.fql.decl.SigExp.Exp;
import catdata.fql.decl.SigExp.One;
import catdata.fql.decl.SigExp.Plus;
import catdata.fql.decl.SigExp.Times;
import catdata.fql.decl.SigExp.Zero;

public class MapExpChecker implements MapExpVisitor<Pair<SigExp, SigExp>, FQLProgram> {

	private List<String> seen = new LinkedList<>();
	
	public MapExpChecker(List<String> seen) {
		this.seen = seen;
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Id e) {
		SigExp x = e.t.typeOf(env);
		return new Pair<>(x, x);
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Comp e) {
		List<String> l = new LinkedList<>(seen);
		Pair<SigExp, SigExp> lt = e.l.accept(env, this);
		seen = l;
		Pair<SigExp, SigExp> rt = e.r.accept(env, this);
		seen = l;
		if (!lt.second.equals(rt.first)) {
			throw new RuntimeException("Composition type mismatch: " + lt.second + " and " + rt.first + " on " + e);
		}
		return new Pair<>(lt.first, rt.second);

	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Dist1 e) {
		SigExp aa = e.a.typeOf(env);
		SigExp bb = e.b.typeOf(env);
		SigExp cc = e.c.typeOf(env);
		return new Pair<>(new Times(aa,
				new Plus(bb, cc)), new Plus(new Times(aa,
				bb), new Times(aa, cc)));
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Dist2 e) {
		SigExp aa = e.a.typeOf(env);
		SigExp bb = e.b.typeOf(env);
		SigExp cc = e.c.typeOf(env);
		return new Pair<>(new Plus(new Times(aa,
				bb), new Times(aa, cc)), new Times(aa,
						new Plus(bb, cc)));
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Var e) {
		if (seen.contains(e.v)) {
			throw new RuntimeException("Cyclic definition: " + e);
		}
		seen.add(e.v);
		MapExp r = env.maps.get(e.v);
		if (r == null) {
			throw new RuntimeException("Unknown mapping " + e.v);
		}
		return r.accept(env, this);
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Const e) {
		SigExp a = e.src.typeOf(env);
		SigExp b = e.dst.typeOf(env);
		return new Pair<>(a, b);
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, TT e) {
		SigExp x = e.t.typeOf(env);
		
		for (String s : e.attrs) {
			if (!(s.equals("string") || s.equals("int") || env.enums.containsKey(s))) {
				throw new RuntimeException("Invalid enum: " + s);
			}
		}
		
		return new Pair<>(x, new One(e.attrs));
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, FF e) {
		SigExp x = e.t.typeOf(env);
		return new Pair<>(new Zero(), x);
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Fst e) {
		SigExp a = e.s.typeOf(env);
		SigExp b = e.t.typeOf(env);
		return new Pair<>(new Times(a, b), a);
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Snd e) {
		SigExp a = e.s.typeOf(env);
		SigExp b = e.t.typeOf(env);
		return new Pair<>(new Times(a, b), b);
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Inl e) {
		SigExp a = e.s.typeOf(env);
		SigExp b = e.t.typeOf(env);
		return new Pair<>(a, new Plus(a, b));
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Inr e) {
		SigExp a = e.s.typeOf(env);
		SigExp b = e.t.typeOf(env);
		return new Pair<>(b, new Plus(a, b));
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Apply e) {
		SigExp s = e.s.typeOf(env);
		SigExp t = e.t.typeOf(env);
		return new Pair<>(new Times(new Exp(s,
				t), t), s);
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Curry e) {
		Pair<SigExp, SigExp> ft = e.f.accept(env, this);
		if (!(ft.first instanceof Times)) {
			throw new RuntimeException("Not a product: " + ft.first + " in " + e);
		}
		Times t = (Times) ft.first;
		return new Pair<>(t.a, new Exp(ft.second, t.b));
	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Case e) {
		List<String> l = new LinkedList<>(seen);
		Pair<SigExp, SigExp> lt = e.l.accept(env, this);
		seen = l;
		Pair<SigExp, SigExp> rt = e.r.accept(env, this);
		seen = l;
		if (!lt.second.equals(rt.second)) {
			throw new RuntimeException("target schema mismatch on " + e + ": " + lt.second + " and " + rt.second);
		}
		return new Pair<>(new Plus(lt.first, rt.first), lt.second);

	}

	@Override
	public Pair<SigExp, SigExp> visit(
			FQLProgram env, Prod e) {
		List<String> l = new LinkedList<>(seen);
		Pair<SigExp, SigExp> lt = e.l.accept(env, this);
		seen = l;
		Pair<SigExp, SigExp> rt = e.r.accept(env, this);
		seen = l;
		if (!lt.first.equals(rt.first)) {
			throw new RuntimeException("source schema mismatch on " + e + ": " + lt.first + " and " + rt.first);
		}
		return new Pair<>(lt.first, new Times(lt.second, rt.second));
	}

	@Override
	public Pair<SigExp, SigExp> visit(FQLProgram env, Sub e) {
		SigExp lt = e.s.typeOf(env);
		SigExp rt = e.t.typeOf(env);
		if (! (lt instanceof SigExp.Const)) {
			throw new RuntimeException(e.s + " does not have constant schema, has " + lt);
		}
		if (! (rt instanceof SigExp.Const)) {
			throw new RuntimeException(e.t + " does not have constant schema, has " + lt);
		}
		SigExp.Const lt0 = (SigExp.Const) lt;
		SigExp.Const rt0 = (SigExp.Const) rt;
		
		for (String n : lt0.nodes) {
			if (!rt0.nodes.contains(n)) {
				throw new RuntimeException("Not subset, missing node " + n);
			}
		}
		for (Triple<String, String, String> n : lt0.arrows) {
			if (!rt0.arrows.contains(n)) {
				throw new RuntimeException("Not subset, missing arrow " + n);
			}
		}
		for (Triple<String, String, String> n : lt0.attrs) {
			if (!rt0.attrs.contains(n)) {
				throw new RuntimeException("Not subset, missing attribute " + n);
			}
		}
		return new Pair<>(lt, rt);
	}

	@Override
	public Pair<SigExp, SigExp> visit(FQLProgram env, Opposite e) {
		Pair<SigExp, SigExp> k = e.e.accept(env, this);
		
		return new Pair<>(new SigExp.Opposite(k.first), new SigExp.Opposite(k.second));
	}

	@Override
	public Pair<SigExp, SigExp> visit(FQLProgram env, Iso e) {
		SigExp l = e.l.typeOf(env);
		SigExp r = e.r.typeOf(env);

        return e.lToR ? new Pair<>(l, r) : new Pair<>(r, l);
	}

}

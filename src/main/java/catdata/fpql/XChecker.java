package catdata.fpql;

import java.util.Map;

import catdata.Pair;
import catdata.fpql.XExp.Apply;
import catdata.fpql.XExp.Compose;
import catdata.fpql.XExp.FLOWER2;
import catdata.fpql.XExp.Flower;
import catdata.fpql.XExp.Id;
import catdata.fpql.XExp.Iter;
import catdata.fpql.XExp.Var;
import catdata.fpql.XExp.XCoApply;
import catdata.fpql.XExp.XConst;
import catdata.fpql.XExp.XCoprod;
import catdata.fpql.XExp.XCounit;
import catdata.fpql.XExp.XDelta;
import catdata.fpql.XExp.XEq;
import catdata.fpql.XExp.XExpVisitor;
import catdata.fpql.XExp.XFF;
import catdata.fpql.XExp.XFn;
import catdata.fpql.XExp.XGrothLabels;
import catdata.fpql.XExp.XIdPoly;
import catdata.fpql.XExp.XInj;
import catdata.fpql.XExp.XInst;
import catdata.fpql.XExp.XLabel;
import catdata.fpql.XExp.XMapConst;
import catdata.fpql.XExp.XMatch;
import catdata.fpql.XExp.XOne;
import catdata.fpql.XExp.XPair;
import catdata.fpql.XExp.XPi;
import catdata.fpql.XExp.XProj;
import catdata.fpql.XExp.XPushout;
import catdata.fpql.XExp.XRel;
import catdata.fpql.XExp.XSOED;
import catdata.fpql.XExp.XSchema;
import catdata.fpql.XExp.XSigma;
import catdata.fpql.XExp.XSuperED;
import catdata.fpql.XExp.XTT;
import catdata.fpql.XExp.XTimes;
import catdata.fpql.XExp.XToQuery;
import catdata.fpql.XExp.XTransConst;
import catdata.fpql.XExp.XTy;
import catdata.fpql.XExp.XUberPi;
import catdata.fpql.XExp.XUnit;
import catdata.fpql.XExp.XVoid;

public class XChecker implements XExpVisitor<Pair<XExp, XExp>, Map<String, XExp>> {

	private static final Var SET = new Var("Set");
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, Var e) {
		if (!env.containsKey(e.v)) {
			throw new RuntimeException("Unbound: " + e.v);
		}
		return env.get(e.v).accept(env, this);
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XMapConst e) {
		return new Pair<>(e.src, e.dst);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XTransConst e) {
		return new Pair<>(e.src, e.dst);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XInst e) {
		return new Pair<>(e.schema, SET);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, Id e) {
		return new Pair<>(e.C, e.C);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, Compose e) {
		Pair<XExp, XExp> a = e.f.accept(env, this);
		Pair<XExp, XExp> b = e.g.accept(env, this);
		if (!a.second.equals(b.first)) {
			throw new RuntimeException("In " + e + ", cod " + a.second + " but dom " + b.first);
		}
		return new Pair<>(a.first, b.second);
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XDelta e) {
		Pair<XExp, XExp> FT = e.F.accept(env, this);
		Pair<XExp, XExp> IT = e.I.accept(env, this);
		if (e.I.kind(env).equals("functor")) {
			if (!IT.first.equals(FT.second)) {
				throw new RuntimeException("In " + e + ", instance type " + IT.first + " but functor cod " + FT.second);
			}
			return new Pair<>(FT.first, SET);
		}
		return new Pair<>(new XDelta(e.F, IT.first), new XDelta(e.F, IT.second));
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XSigma e) {
		Pair<XExp, XExp> FT = e.F.accept(env, this);
		Pair<XExp, XExp> IT = e.I.accept(env, this);
			if (e.I.kind(env).equals("functor")) {
			if (!IT.first.equals(FT.first)) {
				throw new RuntimeException("In " + e + ", instance type " + IT.first + " but functor dom " + FT.first);
			}
			return new Pair<>(FT.second, SET);
		}
		return new Pair<>(new XSigma(e.F, IT.first), new XSigma(e.F, IT.second));
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XPi e) {
		Pair<XExp, XExp> FT = e.F.accept(env, this);
		Pair<XExp, XExp> IT = e.I.accept(env, this);
		if (e.I.kind(env).equals("functor")) {
			if (!IT.first.equals(FT.first)) {
				throw new RuntimeException("In " + e + ", instance type " + IT.first + " but functor dom " + FT.first);
			}
			return new Pair<>(FT.second, SET);
		}
		return new Pair<>(new XPi(e.F, IT.first), new XPi(e.F, IT.second));
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XUnit e) {
		if (e.kind.equals("sigma")) {
			return new Pair<>(e.I, new XDelta(e.F, new XSigma(e.F, e.I)));
		} else if (e.kind.equals("pi")) {
			return new Pair<>(e.I, new XPi(e.F, new XDelta(e.F, e.I)));
		}
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XCounit e) {
		if (e.kind.equals("sigma")) {
			return new Pair<>(new XSigma(e.F, new XDelta(e.F, e.I)), e.I);
		} else if (e.kind.equals("pi")) {
			return new Pair<>(new XDelta(e.F, new XPi(e.F, e.I)), e.I);
		}
		throw new RuntimeException("Anomaly: please report");
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XPushout e) {
		Pair<XExp, XExp> l = e.f.accept(env, this);
		Pair<XExp, XExp> r = e.g.accept(env, this);
		if (!l.first.equals(r.first)) {
			throw new RuntimeException("Pushout from different domains, " + l.first + " and " + r.first);
		}
		return l.first.type(env);
	}	

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XRel e) {
		String k = e.I.kind(env);
		Pair<XExp, XExp> IT = e.I.accept(env, this);
		if (k.equals("functor")) {
			return IT;
		} else if (k.equals("transform")) {
			return new Pair<>(new XRel(IT.first), new XRel(IT.second));
		}
		throw new RuntimeException("Bad kind: " + k + " in " + e);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XCoprod e) {
		Pair<XExp, XExp> l = e.l.accept(env, this);
		Pair<XExp, XExp> r = e.r.accept(env, this);
		if (!l.first.equals(r.first)) {
			throw new RuntimeException("Not of same schema on " + e + ", are " + l.first + " and " + r.first);
		}
		return l;
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XInj e) {
		Pair<XExp, XExp> l = e.l.accept(env, this);
		Pair<XExp, XExp> r = e.r.accept(env, this);
		if (!l.first.equals(r.first)) {
			throw new RuntimeException("Different schemas in " + e + ", are" + l.first + " and " + r.first);
		}
        return e.left ? new Pair<>(e.l, new XCoprod(e.l, e.r)) : new Pair<>(e.r, new XCoprod(e.l, e.r));
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XMatch e) {
		Pair<XExp, XExp> l = e.l.accept(env, this);
		Pair<XExp, XExp> r = e.r.accept(env, this);
		if (!l.second.equals(r.second)) {
			throw new RuntimeException("targets of " + e + " not equal: " + l.second + " and " + r.second);
		}
		return new Pair<>(new XCoprod(l.first, r.first), r.second);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XVoid e) {
		return new Pair<>(e.S, SET);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XFF e) {
		return new Pair<>(new XVoid(e.S.type(env).first), e.S);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XTimes e) {
		Pair<XExp, XExp> l = e.l.accept(env, this);
		Pair<XExp, XExp> r = e.r.accept(env, this);
		if (!l.first.equals(r.first)) {
			throw new RuntimeException("Not of same schema on " + e + ", are " + l.first + " and " + r.first);
		}
		return l;
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XProj e) {
		Pair<XExp, XExp> l = e.l.accept(env, this);
		Pair<XExp, XExp> r = e.r.accept(env, this);
		if (!l.first.equals(r.first)) {
			throw new RuntimeException("Different schemas in " + e + ", are" + l.first + " and " + r.first);
		}
        return e.left ? new Pair<>(new XTimes(e.l, e.r), e.l) : new Pair<>(new XTimes(e.l, e.r), e.r);

	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XPair e) {
		Pair<XExp, XExp> l = e.l.accept(env, this);
		Pair<XExp, XExp> r = e.r.accept(env, this);
		if (!l.first.equals(r.first)) {
			throw new RuntimeException("sources of " + e + " not equal: " + l.second + " and " + r.second);
		}
		return new Pair<>(r.first, new XTimes(l.second, r.second));
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XOne e) {
		return new Pair<>(e.S, SET);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XTT e) {
		return new Pair<>(e.S, new XOne(e.S.type(env).first));
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env,  XPoly e) {
		return new Pair<>(e.src_e, e.dst_e);
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XIdPoly e) {
		return new Pair<>(e.F, e.F);
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, Apply e) {
		String k = e.f.kind(env);
		if (!k.equals("query")) {
			throw new RuntimeException("Can only apply queries in " + e);
		}
		Pair<XExp, XExp> q = e.f.accept(env, this);
		Pair<XExp, XExp> i = e.I.accept(env, this);
		if (e.I.kind(env).equals("functor")) {
			if (!q.first.equals(i.first)) {
				throw new RuntimeException("Query " + e + " expected " + q.first + " but got " + i.first);
			}
			return new Pair<>(q.second, SET);
		} else if (e.I.kind(env).equals("transform")) {
			Object p = i.first.type(env).first;
			if (!p.equals(q.first)) {
				throw new RuntimeException("Schema of transform is " + p + " not " + q.first + " as expected");
			}
			return new Pair<>(new Apply(e.f, i.first), new Apply(e.f, i.second));
		} else {
			throw new RuntimeException();
		}
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XCoApply e) {
		String k = e.f.kind(env);
		if (!k.equals("query")) {
			throw new RuntimeException("Can only co-apply queries in " + e);
		}
		Pair<XExp, XExp> q = e.f.accept(env, this);
		Pair<XExp, XExp> i = e.I.accept(env, this);
		if (e.I.kind(env).equals("functor")) {
		if (!q.second.equals(i.first)) {
			throw new RuntimeException("Query " + e + " expected " + q.first + " but got " + i.first);
		}
		return new Pair<>(q.first, SET);
		}else if (e.I.kind(env).equals("transform")) {
			Object p = i.first.type(env).first;
			if (!p.equals(q.second)) {
				throw new RuntimeException("Schema of transform is " + p + " not " + q.second + " as expected");
			}
			return new Pair<>(new XCoApply(e.f, i.first), new XCoApply(e.f, i.second));
		} else {
			throw new RuntimeException();
		}
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XSOED e) {
		return null; 
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XSuperED e) {
		return null; 
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, Flower e) {
		return null; 
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, FLOWER2 e) {
		return null; 
	}
	
	//////////////////////////////////////////////
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XToQuery e) {
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XUberPi e) {
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XLabel e) {
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XGrothLabels e) {
		throw new RuntimeException("Anomaly: please report");
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XTy e) {
		return null;
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XFn e) {
		return null;
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XConst e) {
		return null;
	}

	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XEq e) {
		return null;
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, XSchema e) {
		return null;
	}
	
	@Override
	public Pair<XExp, XExp> visit(Map<String, XExp> env, Iter e) {
		throw new RuntimeException("Anomaly: please report");
	}

	
	
}

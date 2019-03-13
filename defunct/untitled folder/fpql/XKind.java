package catdata.fpql;

import java.util.Map;

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

public class XKind implements XExpVisitor<String, 	Map<String, XExp>> {

	@Override
	public String visit(Map<String, XExp> env, XPoly e) {
		return "query";
	}
	
	@Override
	public String visit(Map<String, XExp> env, Var e) {
		if (!env.containsKey(e.v)) {
			throw new RuntimeException("Missing: " + e.v);
		}
		return env.get(e.v).accept(env, this);
	}
	
	@Override
	public String visit(Map<String, XExp> env, XSchema e) {
		return "category";
	}

	@Override
	public String visit(Map<String, XExp> env, XMapConst e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, XTransConst e) {
		return "transform";
	}

	@Override
	public String visit(Map<String, XExp> env, XInst e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, Id e) {
		String s = e.C.accept(env, this);
		if (s.equals("category")) {
			return "functor";
		}
		if (s.equals("functor")) {
			return "transform";
		}
		return "Cannot id " + e;
	}

	@Override
	public String visit(Map<String, XExp> env, Compose e) {
		String a = e.f.accept(env, this);
		String b = e.g.accept(env, this);
		if (!a.equals(b)) {
			throw new RuntimeException("In " + e + ", cod " + a + " but dom " + b);
		}
		return a;
	}
	
	@Override
	public String visit(Map<String, XExp> env, XDelta e) {
		return e.I.accept(env, this);
	}

	@Override
	public String visit(Map<String, XExp> env, XSigma e) {
		return e.I.accept(env, this);
	}

	@Override
	public String visit(Map<String, XExp> env, XPi e) {
		return e.I.accept(env, this);
	}

	@Override
	public String visit(Map<String, XExp> env, XUnit e) {
		return "transform";
	}

	@Override
	public String visit(Map<String, XExp> env, XCounit e) {
		return "transform";
	}


	/////////////////////////////////////////////////////////////////////
	
	
	@Override
	public String visit(Map<String, XExp> env, XPushout e) {
		return "functor";
	}	

	@Override
	public String visit(Map<String, XExp> env, XRel e) {
		return e.I.kind(env);
	}

	@Override
	public String visit(Map<String, XExp> env, XCoprod e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, XInj e) {
		return "transfrom";
	}

	@Override
	public String visit(Map<String, XExp> env, XMatch e) {
		return "transform";
	}

	@Override
	public String visit(Map<String, XExp> env, XVoid e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, XFF e) {
		return "transform";
	}

	@Override
	public String visit(Map<String, XExp> env, XTimes e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, XProj e) {
		return "transform";
	}

	@Override
	public String visit(Map<String, XExp> env, XPair e) {
		return "transform";
	}

	@Override
	public String visit(Map<String, XExp> env, XOne e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, XTT e) {
		return "transform";
	}

	@Override
	public String visit(Map<String, XExp> env, Apply e) {
		return e.I.kind(env);
	}
	
	@Override
	public String visit(Map<String, XExp> env, XCoApply e) {
		return e.I.kind(env);
	}

	@Override
	public String visit(Map<String, XExp> env, Flower e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, FLOWER2 e) {
		return "functor";
	}

	@Override
	public String visit(Map<String, XExp> env, XIdPoly e) {
		return "query";
	}
	
	@Override
	public String visit(Map<String, XExp> env, XSOED e) {
		return "query";
	}
	
	@Override
	public String visit(Map<String, XExp> env, XSuperED e) {
		return "functor";
	}
	/////////////////////////////////////////////////////////
	

	@Override
	public String visit(Map<String, XExp> env, Iter e) {
		return null;
	}

	
	@Override
	public String visit(Map<String, XExp> env, XToQuery e) {
		return null;

	}

	@Override
	public String visit(Map<String, XExp> env, XUberPi e) {
		return null;

	}

	@Override
	public String visit(Map<String, XExp> env, XLabel e) {
		return null;

	}

	@Override
	public String visit(Map<String, XExp> env, XGrothLabels e) {
		return null;

	}
	
	@Override
	public String visit(Map<String, XExp> env, XTy e) {
		return null;
	}

	@Override
	public String visit(Map<String, XExp> env, XFn e) {
		return null;
	}

	@Override
	public String visit(Map<String, XExp> env, XConst e) {
		return null;
	}

	@Override
	public String visit(Map<String, XExp> env, XEq e) {
		return null;
	}

	
	
}

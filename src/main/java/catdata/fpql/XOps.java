package catdata.fpql;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
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
import catdata.fpql.XExp.XSOED.FOED;
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

@SuppressWarnings({ "rawtypes", "unchecked" })

public class XOps implements XExpVisitor<XObject, XProgram> {
	
	private final XEnvironment ENV;

	public XOps(XEnvironment env) {
        ENV = env;
	}

	@Override
	public XObject visit(XProgram env, XSchema e) {
		return XCtx.make(ENV.global, e);
	}

	@Override
	public XObject visit(XProgram env, XMapConst e) {
		XObject o = e.src.accept(env, this);
		if (!(o instanceof XCtx<?>)) {
			throw new RuntimeException("Not a schema: " + e.src);
		}
		XObject o2 = e.dst.accept(env, this);
		if (!(o2 instanceof XCtx<?>)) {
			throw new RuntimeException("Not a schema: " + e.dst);
		}
		XCtx<String> ctx = (XCtx<String>) o;
		if (ctx.schema != null || ctx.global == null) {
			throw new RuntimeException("Not a schema: " + e.src);			
		}
		XCtx<String> ctx2 = (XCtx<String>) o2;
		if (ctx2.schema != null || ctx2.global == null) {
			throw new RuntimeException("Not a schema: " + e.dst);			
		}
		return XMapping.make(ENV, ctx, ctx2, e); 
	}

	@Override
	public XObject visit(XProgram env, XSigma e) {
		XObject o = e.F.accept(env, this);
		if (!(o instanceof XMapping<?,?>)) {
			throw new RuntimeException("Not a mapping: " + e.F);
		}
		XMapping<String,String> ctx = (XMapping<String,String>) o;
		XObject o2 = e.I.accept(env, this);
		if (o2 instanceof XCtx<?>) {
			XCtx<String> ctx2 = (XCtx<String>) o2;
			return ctx.apply0(ctx2);
		} else if (o2 instanceof XMapping<?,?>) {
			XMapping<String,String> ctx2 = (XMapping<String,String>) o2;
			return ctx.apply(ctx2);			
		} 
		else {
			throw new RuntimeException("Not an instance or transform: " + e.I);
		} 
	}

	@Override
	public XObject visit(XProgram env, XDelta e) {
		XObject o = e.F.accept(env, this);
		if (!(o instanceof XMapping<?,?>)) {
			throw new RuntimeException("Not a mapping: " + e.F);
		}
		XMapping<String,String> ctx = (XMapping<String,String>) o;
		XObject o2 = e.I.accept(env, this);
		if (o2 instanceof XCtx<?>) {
			XCtx<String> ctx2 = (XCtx<String>) o2;
			return ctx.delta(ctx2);
		} else if (o2 instanceof XMapping<?,?>) {
			XMapping<String,String> ctx2 = (XMapping<String,String>) o2;
			return ctx.deltaT(ctx2);			
		}
		else {
			throw new RuntimeException("Not an instance or transform: " + e.I + " (class " + o2.getClass() + ")");
		} 
//		throw new RuntimeException();
	}
	@Override
	public XObject visit(XProgram env, XInst e) {
		XObject o = e.schema.accept(env, this);
		if (!(o instanceof XCtx<?>)) {
			throw new RuntimeException("Not a schema: " + e.schema);
		}
		XCtx<String> ctx = (XCtx<String>) o;
		if (ctx.schema != null) {
			throw new RuntimeException("Not a schema: " + e.schema);			
		}
		return XCtx.make(ctx, e);		
	}

	@Override
	public XObject visit(XProgram env, Var e) {
		XObject ret = ENV.objs.get(e.v);
		if (ret == null) {
			throw new RuntimeException("Unbound variable: " + e.v);
		}
		return ret;
	}

	@Override
	public XObject visit(XProgram env, XTy e) {
		return new XString("Type", e.javaName);
	}

	@Override
	public XObject visit(XProgram env, XFn e) {
		return new XString("Function", e.javaFn + " : " + e.src + " -> " + e.dst);
	}

	@Override
	public XObject visit(XProgram env, XConst e) {
		return new XString("Constant", e.javaFn + " : " + e.dst);
	}

	@Override
	public XObject visit(XProgram env, XEq e) {
		return new XString("Assumption", Util.sep(e.lhs, ".") + " = " + Util.sep(e.rhs, "."));
	}

	@Override
	public XObject visit(XProgram env, XTransConst e) {
		XObject o = e.src.accept(env, this);
		if (!(o instanceof XCtx<?>)) {
			throw new RuntimeException("Not an instance: " + e.src);
		}
		XObject o2 = e.dst.accept(env, this);
		if (!(o2 instanceof XCtx<?>)) {
			throw new RuntimeException("Not an instance: " + e.dst);
		}
		XCtx<String> ctx = (XCtx<String>) o;
		XCtx<String> ctx2 = (XCtx<String>) o2;
		return XMapping.make(ctx, ctx2, e); 
	}

	@Override
	public XObject visit(XProgram env, XUnit e) {
		XObject o = e.F.accept(env, this);
		if (!(o instanceof XMapping<?, ?>)) {
			throw new RuntimeException("Not a mapping: " + e.F);
		}
		XObject o2 = e.I.accept(env, this);
		if (!(o2 instanceof XCtx<?>)) {
			throw new RuntimeException("Not an instance: " + e.I);
		}
		XMapping<String, String> ctx = (XMapping<String, String>) o;
		XCtx<String> ctx2 = (XCtx<String>) o2;
		if (e.kind.equals("sigma")) {
			return ctx.unit(ctx2); 
		}
		return ctx.pi_unit(ctx2);
	}

	@Override
	public XObject visit(XProgram env, XCounit e) {
		XObject o = e.F.accept(env, this);
		if (!(o instanceof XMapping<?, ?>)) {
			throw new RuntimeException("Not a mapping: " + e.F);
		}
		XObject o2 = e.I.accept(env, this);
		if (!(o2 instanceof XCtx<?>)) {
			throw new RuntimeException("Not an instance: " + e.I);
		}
		XMapping<String, String> ctx = (XMapping<String, String>) o;
		XCtx<String> ctx2 = (XCtx<String>) o2;
		if (e.kind.equals("sigma")) {
			return ctx.counit(ctx2); 
		}
		return ctx.pi_counit(ctx2);
	}

	@Override
	public XObject visit(XProgram env, XPi e) {
		XObject o = e.F.accept(env, this);
		if (!(o instanceof XMapping<?,?>)) {
			throw new RuntimeException("Not a mapping: " + e.F);
		}
		XMapping<String,String> ctx = (XMapping<String,String>) o;
		XObject o2 = e.I.accept(env, this);
		if (o2 instanceof XCtx<?>) {
			XCtx<String> ctx2 = (XCtx<String>) o2;
			return ctx.pi(ctx2);
		} else if (o2 instanceof XMapping<?,?>) {
			XMapping<String,String> ctx2 = (XMapping<String,String>) o2;
			return ctx.piT(ctx2);			
		}
		throw new RuntimeException("Not an instance or transform: " + e.I + " (class " + o2.getClass() + ")"); 
	}

	@Override
	public XObject visit(XProgram env, XRel e) {
		XObject o = e.I.accept(env, this);
		if (o instanceof XCtx<?>) {
			XCtx<?> x = (XCtx<?>) o;
			return x.rel();			
		}
		if (o instanceof XMapping<?,?>) {
			XMapping<?,?> x = (XMapping<?,?>) o;
			return x.rel();
		}
		throw new RuntimeException("Not a instance or homomorphism: " + o);
	}

	@Override
	public XObject visit(XProgram env, XCoprod e) {
		XObject l = e.l.accept(env, this);
		if (!(l instanceof XCtx)) {
			throw new RuntimeException("LHS not an instance in " + e);
		}
		XObject r = e.r.accept(env, this);
		if (!(r instanceof XCtx)) {
			throw new RuntimeException("RHS not an instance in " + e);
		}
		XCtx ll = (XCtx) l;
		XCtx rr = (XCtx) r;
		return XProd.coprod(ll, rr);
	}

	@Override
	public XObject visit(XProgram env, XInj e) {
		XObject l = e.l.accept(env, this);
		if (!(l instanceof XCtx)) {
			throw new RuntimeException("LHS not an instance in " + e);
		}
		XObject r = e.r.accept(env, this);
		if (!(r instanceof XCtx)) {
			throw new RuntimeException("RHS not an instance in " + e);
		}
		XCtx ll = (XCtx) l;
		XCtx rr = (XCtx) r;
        return e.left ? XProd.inl(ll, rr) : XProd.inr(ll, rr);
	}

	@Override
	public XObject visit(XProgram env, XMatch e) {
		XObject l = e.l.accept(env, this);
		if (!(l instanceof XMapping)) {
			throw new RuntimeException("LHS not a homomorphism");
		}
		XObject r = e.r.accept(env, this);
		if (!(r instanceof XMapping)) {
			throw new RuntimeException("RHS not a homomorphism");
		}
		XMapping ll = (XMapping) l;
		XMapping rr = (XMapping) r;
		return XProd.match(ll, rr);
	}

	@Override
	public XObject visit(XProgram env, XVoid e) {
		XObject x = e.S.accept(env, this);
		if (!(x instanceof XCtx)) {
			throw new RuntimeException("Not a schema");
		}
		XCtx c = (XCtx) x;
		return XProd.zero(c);
	}

	@Override
	public XObject visit(XProgram env, XFF e) {
		XObject x = e.S.accept(env, this);
		if (!(x instanceof XCtx)) {
			throw new RuntimeException("Not an instance in " + e);
		}
		XCtx c = (XCtx) x;
		return XProd.ff(c);
	}
	
	@Override
	public XObject visit(XProgram env, XTimes e) {
		XObject l = e.l.accept(env, this);
		if (!(l instanceof XCtx)) {
			throw new RuntimeException("LHS not an instance in " + e);
		}
		XObject r = e.r.accept(env, this);
		if (!(r instanceof XCtx)) {
			throw new RuntimeException("RHS not an instance in " + e);
		}
		XCtx<String> ll = (XCtx<String>) l;
		XCtx<String> rr = (XCtx<String>) r;
		return XProd.prod(ll, rr);

	}

	@Override
	public XObject visit(XProgram env, XProj e) {
		XObject l = e.l.accept(env, this);
		if (!(l instanceof XCtx)) {
			throw new RuntimeException("LHS not an instance in " + e);
		}
		XObject r = e.r.accept(env, this);
		if (!(r instanceof XCtx)) {
			throw new RuntimeException("RHS not an instance in " + e);
		}
		XCtx ll = (XCtx) l;
		XCtx rr = (XCtx) r;
        return e.left ? XProd.fst(ll, rr) : XProd.snd(ll, rr);
	}

	@Override
	public XObject visit(XProgram env, XPair e) {
		XObject l = e.l.accept(env, this);
		if (!(l instanceof XMapping)) {
			throw new RuntimeException("LHS not a homomorphism");
		}
		XObject r = e.r.accept(env, this);
		if (!(r instanceof XMapping)) {
			throw new RuntimeException("RHS not a homomorphism");
		}
		XMapping ll = (XMapping) l;
		XMapping rr = (XMapping) r;
		return XProd.pair(ll, rr);
	}

	@Override
	public XObject visit(XProgram env, XOne e) {
		XObject x = e.S.accept(env, this);
		if (!(x instanceof XCtx)) {
			throw new RuntimeException("Not a schema");
		}
		XCtx c = (XCtx) x;
		return XProd.one(c);
	}

	@Override
	public XObject visit(XProgram env, XTT e) {
		XObject x = e.S.accept(env, this);
		if (!(x instanceof XCtx)) {
			throw new RuntimeException("Not an instance in " + e);
		}
		XCtx c = (XCtx) x;
		return XProd.tt(c);
	}

	@Override
	public XObject visit(XProgram env, Flower e) {
		XObject src0 = e.src.accept(env, this);
		if (!(src0 instanceof XCtx)) {
			throw new RuntimeException("Not an instance in " + e);			
		}
		XCtx src = (XCtx) src0;
		return XProd.flower(e, src);		
	}

	@Override
	public XObject visit(XProgram env, FLOWER2 e) {
		XObject src0 = e.src.accept(env, this);
		if (!(src0 instanceof XCtx)) {
			throw new RuntimeException("Not an instance in " + e);			
		}
		XCtx src = (XCtx) src0;
		return XProd.flower2(e, src);		
	}

/*	@Override
	public XObject visit(XProgram env, XQueryExp e) {
		Object delta0 = e.delta.accept(env, this);
		if (!(delta0 instanceof XMapping)) {
			throw new RuntimeException("Delta not a mapping in " + e);
		}
		Object pi0 = e.pi.accept(env, this);
		if (!(pi0 instanceof XMapping)) {
			throw new RuntimeException("Pi not a mapping in " + e);
		}
		Object sigma0 = e.sigma.accept(env, this);
		if (!(sigma0 instanceof XMapping)) {
			throw new RuntimeException("Sigma not a mapping in " + e);
		}
		return new XQuery((XMapping)pi0, (XMapping)delta0, (XMapping)sigma0);
	} */

	@Override
	public XObject visit(XProgram env, Apply e) {
		XObject f = e.f.accept(env, this);
		if (!(f instanceof XPoly)) {
			throw new RuntimeException("Not a query in " + e);
		}
		XObject i = e.I.accept(env, this);
		if (i instanceof XCtx) {
			return XProd.uberflower((XPoly)f, (XCtx)i);
		}
		return XProd.uberflower((XPoly)f, (XMapping)i);
	}
	//: coapply on transforms, unit,counit for apply/coapply
	@Override
	public XObject visit(XProgram env, XCoApply e) {
		XObject f = e.f.accept(env, this);
		if (!(f instanceof XPoly)) {
			throw new RuntimeException("Not a query in " + e);
		}
		XObject i = e.I.accept(env, this);
		if (!(i instanceof XCtx)) {
			throw new RuntimeException("Not an instance in " + e);
		}
		return ((XPoly)f).coapply((XCtx)i);
	}

	@Override
	public XObject visit(XProgram env, Iter e) {
		throw new RuntimeException("todo: remove iter");
	}
/*		XObject f = e.f.accept(env, this);
		if (!(f instanceof XQuery)) {
			throw new RuntimeException("Not a query in " + e);
		}
		XObject i = e.initial.accept(env, this);
		if (!(f instanceof XCtx)) {
			throw new RuntimeException("Not an instance in " + e);
		}
		XQuery F = (XQuery) f;
		XCtx I = (XCtx) i;
		
		for (int j = 0; j < e.num; j++) {
			I = F.apply(I);
		}
		
		return I;
	} */

	@Override
	public XObject visit(XProgram env, Id e) {
		Object o = e.C.accept(env, this);
		if (!(o instanceof XCtx)) {
			throw new RuntimeException("Not schema/instance in " + e);
		}
		XCtx C = (XCtx) o;
		String str = null;
		if (C.kind().equals("schema")) {
			str = "mapping";
		}
		if (C.kind().equals("instance")) {
			str = "homomorphism";
		}
		if (str == null) {
			throw new RuntimeException();
		}
		XMapping F = new XMapping(C, str);
		//if (e.isQuery) {
		//	return new XQuery(F, F, F);
		//} 
		return F;
	}

	@Override
	public XObject visit(XProgram env, Compose e) {
		Object f0 = e.f.accept(env, this);
		Object g0 = e.g.accept(env, this);
		if (f0 instanceof XMapping && g0 instanceof XMapping) {
			XMapping F = (XMapping) f0;
			XMapping G = (XMapping) g0;
			return new XMapping(F, G);
		} else if (f0 instanceof XPoly && g0 instanceof XPoly) {
			XPoly F = (XPoly) f0;
			XPoly G = (XPoly) g0;
			return XPoly.compose(F, G);
		}
		throw new RuntimeException("Cannot compose in " + e);
	}

	@Override
	public XObject visit(XProgram env, XPoly e) {
		Object a = e.src_e.accept(env, this);
		if (!(a instanceof XCtx)) {
			throw new RuntimeException("Not a schema: " + a);
		}
		Object b = e.dst_e.accept(env, this);
		if (!(b instanceof XCtx)) {
			throw new RuntimeException("Not a schema: " + b);
		}
		e.src = (XCtx) a;
		e.dst = (XCtx) b;
		e.validate();
		return e;
	}

	@Override
	public XObject visit(XProgram env, XToQuery e) {
		Object o = e.inst.accept(env, this);
		if (!(o instanceof XCtx)) {
			throw new RuntimeException("Not instance: " + o);
		}
		XCtx c = (XCtx) o;

		int i = 0;
		//Map m1 = new HashMap();
		Map m2 = new HashMap();
	//	Map mty = new HashMap();
		
		Map from = new HashMap<>();
		for (Object t : c.terms()) {
		//	m1.put("v_v"+i, t);
			m2.put(t, "v_v"+i);
			from.put("v_v"+i, c.type(t).second);
//			from.put("v"+i, c.type(((Pair)t).second);
			i++;
		}
		List where = new LinkedList<>();

		Function f = x -> {
			Object j = m2.get(x);
			if (j == null) {
				return x;
			}
			return j;
		};
		for (Object k : c.eqs) {
			List l = (List) ((Pair)k).first;
			List r = (List) ((Pair)k).second;
			//lookup m2
			where.add(new Pair<>(l.stream().map(f).collect(Collectors.toList()),
					             r.stream().map(f).collect(Collectors.toList())));
		}
		
		Flower iii= new Flower(new HashMap<>(), from, where, e.applyTo);
		return iii.accept(env, this);		
	}

	@Override
	public XObject visit(XProgram env, XUberPi e) {
		XObject eF = e.F.accept(env, this);
		
		if (!(eF instanceof XMapping)) {
			throw new RuntimeException("Not a mapping: " + e.F);
		}
		
		XMapping m = (XMapping) eF;
		return m.uber();
	}

	@Override
	public XObject visit(XProgram env, XLabel e) {
		XObject eF = e.F.accept(env, this);
		
		if (!(eF instanceof XPoly)) {
			throw new RuntimeException("Not a poly: " + e.F);
		}
		
		XPoly m = (XPoly) eF;
		return m.o();
	}

	@Override
	public XObject visit(XProgram env, XIdPoly e) {
		XObject eF = e.F.accept(env, this);
		
		if (!(eF instanceof XCtx)) {
			throw new RuntimeException("Not a schema: " + e.F);
		}
		
		XCtx m = (XCtx) eF;
		return XPoly.id(m);
	}

	@Override
	public XObject visit(XProgram env, XGrothLabels e) {
		XObject eF = e.F.accept(env, this);
		
		if (!(eF instanceof XPoly)) {
			throw new RuntimeException("Not a poly: " + e.F);
		}
		
		XPoly m = (XPoly) eF;
		return m.grotho();

	}

	@Override
	public XObject visit(XProgram env, XPushout e) {
		Object a = e.f.accept(env, this);
		if (!(a instanceof XMapping)) {
			throw new RuntimeException("Not a homomorphism: " + a);
		}
		Object b = e.g.accept(env, this);
		if (!(b instanceof XMapping)) {
			throw new RuntimeException("Not a homomorphism: " + b);
		}

		return XProd.pushout((XMapping)a, (XMapping)b);
	}

	@Override
	public XObject visit(XProgram env, XSOED e) {
		XExp src0 = env.exps.get(e.src);
		if (src0 == null) {
			throw new RuntimeException("Missing: " + e.src);
		}
		if (!(src0 instanceof XSchema)) {
			throw new RuntimeException("Not a schema: " + e.src);
		}
		XSchema src = (XSchema) src0; 
		XCtx src1 = (XCtx) ENV.objs.get(e.src);
		
		XExp dst0 = env.exps.get(e.dst);
		if (dst0 == null) {
			throw new RuntimeException("Missing: " + e.dst);
		}
		if (!(dst0 instanceof XSchema)) {
			throw new RuntimeException("Not a schema: " + e.dst);
		}
		XSchema dst = (XSchema) dst0; 
		XCtx dst1 = (XCtx) ENV.objs.get(e.dst);
		
		XObject I0 = ENV.objs.get(e.I);
		if (I0 == null) {
			throw new RuntimeException("Missing: " + e.I);
		}
		if (!(I0 instanceof XCtx)) {
			throw new RuntimeException("Not an instance: " + e.I);
		}
		XCtx I = (XCtx) I0; 
		if (!src1.equals(I.schema)) {
			throw new RuntimeException("Instance schema does not match source");
		}
		
		List<String> nodes = new LinkedList<>();
		List<Triple<String, String, String>> arrows = new LinkedList<>();
		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
		Map em_s = new HashMap();
		Map em_t = new HashMap();
	
		nodes.addAll(src.nodes);
		nodes.addAll(dst.nodes);
		arrows.addAll(src.arrows);
		arrows.addAll(dst.arrows);
		arrows.addAll(e.es);
		eqs.addAll(src.eqs);
		eqs.addAll(dst.eqs);
		
		for (FOED k : e.as) {
			for (Pair<List<String>, List<String>> v : k.eqs) {
				List<String> l = new LinkedList<>(v.first);
				List<String> r = new LinkedList<>(v.second);
				l.removeAll(Collections.singletonList(k.a));
				r.removeAll(Collections.singletonList(k.a));
				eqs.add(new Pair<>(l, r));
			}
		}
		
		for (String n : src.nodes) {
			em_s.put(n, Collections.singletonList(n));
		}
		for (String n : dst.nodes) {
			em_t.put(n, Collections.singletonList(n));
		}
		for (Triple<String, String, String> n : src.arrows) {
			em_s.put(n.first, Collections.singletonList(n.first));
		}
		for (Triple<String, String, String> n : dst.arrows) {
			em_t.put(n.first, Collections.singletonList(n.first));
		}
		for (Object n : src1.allTerms()) {
			if (em_s.containsKey(n)) {
				continue;
			}
			em_s.put(n, Collections.singletonList(n));
		}
		for (Object n : dst1.allTerms()) {
			if (em_t.containsKey(n)) {
				continue;
			}
			em_t.put(n, Collections.singletonList(n));
		}
		
		XSchema X = new XSchema(nodes, arrows, eqs);
		XCtx Y = (XCtx) X.accept(env, this);
		XMapping F = new XMapping(src1, Y, em_s, "mapping");
		XMapping G = new XMapping(dst1, Y, em_t, "mapping");
		
		XCtx J = F.apply0(I);
		return G.delta(J);
	}

	@Override
	public XObject visit(XProgram env, XSuperED e) {
		XExp src0 = env.exps.get(e.S);
		if (src0 == null) {
			throw new RuntimeException("Missing: " + e.S);
		}
		if (!(src0 instanceof XSchema)) {
			throw new RuntimeException("Not a schema: " + e.S);
		}
		//XSchema src = (XSchema) src0; 
		XCtx src1 = (XCtx) ENV.objs.get(e.S);
		
		XExp dst0 = env.exps.get(e.T);
		if (dst0 == null) {
			throw new RuntimeException("Missing: " + e.T);
		}
		if (!(dst0 instanceof XSchema)) {
			throw new RuntimeException("Not a schema: " + e.T);
		}
		//XSchema dst = (XSchema) dst0; 
		XCtx dst1 = (XCtx) ENV.objs.get(e.T);
		
		XObject I0 = ENV.objs.get(e.I);
		if (I0 == null) {
			throw new RuntimeException("Missing: " + e.I);
		}
		if (!(I0 instanceof XCtx)) {
			throw new RuntimeException("Not an instance: " + e.I);
		}
		XCtx I = (XCtx) I0; 
		if (!src1.equals(I.schema)) {
			throw new RuntimeException("Instance schema does not match source");
		}

		XChaser.validate(e, src1, dst1);
		
		return XChaser.chase(e, src1, dst1, I);
	}
	

	
}

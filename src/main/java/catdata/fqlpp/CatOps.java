package catdata.fqlpp;

import java.io.Serializable;
import java.util.*;

import catdata.Chc;
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
import catdata.fqlpp.CatExp.Zero;
import catdata.fqlpp.FunctorExp.Apply;
import catdata.fqlpp.FunctorExp.Case;
import catdata.fqlpp.FunctorExp.CatConst;
import catdata.fqlpp.FunctorExp.Comp;
import catdata.fqlpp.FunctorExp.Curry;
import catdata.fqlpp.FunctorExp.Eval;
import catdata.fqlpp.FunctorExp.FF;
import catdata.fqlpp.FunctorExp.FinalConst;
import catdata.fqlpp.FunctorExp.Fst;
import catdata.fqlpp.FunctorExp.FunctorExpVisitor;
import catdata.fqlpp.FunctorExp.Id;
import catdata.fqlpp.FunctorExp.Inl;
import catdata.fqlpp.FunctorExp.Inr;
import catdata.fqlpp.FunctorExp.InstConst;
import catdata.fqlpp.FunctorExp.Iso;
import catdata.fqlpp.FunctorExp.MapConst;
import catdata.fqlpp.FunctorExp.Migrate;
import catdata.fqlpp.FunctorExp.Pivot;
import catdata.fqlpp.FunctorExp.Prod;
import catdata.fqlpp.FunctorExp.Prop;
import catdata.fqlpp.FunctorExp.Pushout;
import catdata.fqlpp.FunctorExp.SetSetConst;
import catdata.fqlpp.FunctorExp.Snd;
import catdata.fqlpp.FunctorExp.TT;
import catdata.fqlpp.FunctorExp.Uncurry;
import catdata.fqlpp.FunctorExp.Var;
import catdata.fqlpp.TransExp.Adj;
import catdata.fqlpp.TransExp.AndOrNotImplies;
import catdata.fqlpp.TransExp.ApplyPath;
import catdata.fqlpp.TransExp.ApplyTrans;
import catdata.fqlpp.TransExp.Bool;
import catdata.fqlpp.TransExp.Chr;
import catdata.fqlpp.TransExp.CoProd;
import catdata.fqlpp.TransExp.Inj;
import catdata.fqlpp.TransExp.Ker;
import catdata.fqlpp.TransExp.PeterApply;
import catdata.fqlpp.TransExp.Proj;
import catdata.fqlpp.TransExp.SetSet;
import catdata.fqlpp.TransExp.ToCat;
import catdata.fqlpp.TransExp.ToInst;
import catdata.fqlpp.TransExp.ToMap;
import catdata.fqlpp.TransExp.ToSet;
import catdata.fqlpp.TransExp.TransExpVisitor;
import catdata.fqlpp.TransExp.Whisker;
import catdata.fqlpp.cat.Category;
import catdata.fqlpp.cat.CoMonad;
import catdata.fqlpp.cat.FDM;
import catdata.fqlpp.cat.FinCat;
import catdata.fqlpp.cat.FinSet;
import catdata.fqlpp.cat.FinSet.Fn;
import catdata.fqlpp.cat.FiniteCategory;
import catdata.fqlpp.cat.FunCat;
import catdata.fqlpp.cat.Functor;
import catdata.fqlpp.cat.Groth;
import catdata.fqlpp.cat.Inst;
import catdata.fqlpp.cat.Instance;
import catdata.fqlpp.cat.Mapping;
import catdata.fqlpp.cat.Monad;
import catdata.fqlpp.cat.Signature;
import catdata.fqlpp.cat.Signature.Edge;
import catdata.fqlpp.cat.Signature.Node;
import catdata.fqlpp.cat.Signature.Path;
import catdata.fqlpp.cat.Transform;

@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
public class CatOps implements CatExpVisitor<Category, FQLPPProgram>,
		FunctorExpVisitor<Functor, FQLPPProgram>, TransExpVisitor<Transform, FQLPPProgram> , Serializable {

	private FQLPPEnvironment ENV;
	public CatOps(FQLPPEnvironment ENV) {
		this.ENV = ENV;
	}

	@SuppressWarnings("unused")
	private CatOps() { }
	
	@Override
	public Category visit(FQLPPProgram env, Zero e) {
		return FinCat.initial();
	}

	@Override
	public Category visit(FQLPPProgram env, One e) {
		return FinCat.terminal();
	}

	@Override
	public Category visit(FQLPPProgram env, Plus e) {
		Category<?, ?> a = e.a.accept(env, this);
		Category<?, ?> b = e.b.accept(env, this);
		return FinCat.coproduct(a, b);
	}

	@Override
	public Category visit(FQLPPProgram env, Times e) {
		Category<?, ?> a = e.a.accept(env, this);
		Category<?, ?> b = e.b.accept(env, this);
		return FinCat.product(a, b);
	}

	@Override
	public Category visit(FQLPPProgram env, Exp e) {
		Category<?, ?> a = e.a.accept(env, this);
		Category<?, ?> b = e.b.accept(env, this);
		if (!a.isInfinite() && !b.isInfinite()) {
			return FinCat.exp(a, b);
		} else if (!b.isInfinite() && a.equals(FinSet.FinSet)) {
			return Inst.get(b);
		} else if (!b.isInfinite() && a.equals(FinCat.FinCat)) {
			return FunCat.get(b);
		} else {
			throw new RuntimeException("Cannot compute category " + a + "^" + b);
		}
	}

	@Override
	public Category visit(FQLPPProgram env, CatExp.Var e) {
		Category<?, ?> x = ENV.cats.get(e.v);
		if (x == null) {
			throw new RuntimeException("Missing category: " + e);
		}
		return x;
	}

	@Override
	public Category visit(FQLPPProgram env, Const e) {
		Signature<String, String> s = new Signature<>(e.nodes, e.arrows, e.eqs);
		//		s.KB();
		return s.toCat();
	}

	@Override
	public Category visit(FQLPPProgram env, Dom e) {
		return e.f.accept(env, this).source;
	}

	@Override
	public Category visit(FQLPPProgram env, Cod e) {
		return e.f.accept(env, this).target;
	}

	@Override
	public Functor visit(FQLPPProgram env, Id e) {
		Category c = e.t.accept(env, this);
		return FinCat.FinCat.identity(c);
	}

	@Override
	public Functor visit(FQLPPProgram env, Comp e) {
		Functor f = e.l.accept(env, this);
		Functor g = e.r.accept(env, this);
		return FinCat.FinCat.compose(f, g);
	}

	@Override
	public Functor visit(FQLPPProgram env, Var e) {
		Functor x = ENV.ftrs.get(e.v);
		if (x == null) {
			throw new RuntimeException("Missing functor: " + e);
		}
		return x;
	}

	public static CatExp resolve(FQLPPProgram env, CatExp c) {
		if (c instanceof CatExp.Var) {
			CatExp.Var v = (CatExp.Var) c;
			if (!env.cats.containsKey(v.v)) {
				throw new RuntimeException("Missing catgory: " + v.v);
			}
			return resolve(env, env.cats.get(v.v));
		}
		return c;
	}

	private Pair<Category, Instance<String,String>> toInstance(FQLPPProgram env, InstConst ic) {
		CatExp e = resolve(env, ic.sig);
		if (!(e instanceof Const)) {
			throw new RuntimeException(
					"Can only create instances for finitely-presented categories.");
		}
		Const c = (Const) e;

		Category src = c.accept(env, this);
		Signature<String, String> sig = new Signature<>(c.nodes, c.arrows, c.eqs);

		Map<Node, Set> nm = new HashMap<>();
		for (String n0 : ic.nm.keySet()) {
			Node n = sig.getNode(n0);
			SetExp kkk = ic.nm.get(n.name);
			if (kkk == null) {
				throw new RuntimeException("Missing node mapping from " + n);
			}
			nm.put(n, kkk.accept(env, new SetOps(ENV)));
		}
		Map<Edge, Map> em = new HashMap<>();
		for (String n0 : ic.em.keySet()) {
			Edge n = sig.getEdge(n0);
			Chc<FnExp, SetExp> chc = ic.em.get(n.name);
			if (chc == null) {
				throw new RuntimeException("Missing edge mapping from " + n);
			}
			if (chc.left) {
				FnExp kkk = chc.l;
				em.put(n, kkk.accept(env, new SetOps(ENV)).toMap());
			} else {
				SetExp sss = chc.r;
				Set vvv = sss.accept(env, new SetOps(ENV));
				Map<Object, Object> uuu = new HashMap<>();
				for (Object o : vvv) {
					if (!(o instanceof Pair)) {
						throw new RuntimeException("Not a pair: " + o);
					}
					Pair oo = (Pair) o;
					if (uuu.containsKey(oo.first)) {
						throw new RuntimeException("Duplicate domain entry: " + o + " in " + ic);
					}
					uuu.put(oo.first, oo.second);
				}
				FnExp kkk = new FnExp.Const(uuu::get, ic.nm.get(n.source.name),
						ic.nm.get(n.target.name));
				em.put(n, kkk.accept(env, new SetOps(ENV)).toMap());
			}
		}

		return new Pair<>(src, new Instance(nm, em, sig));
	}
	
	@Override
	public Functor visit(FQLPPProgram env, InstConst ic) {
			Pair<Category, Instance<String,String>> xxx = toInstance(env, ic); //new Instance(nm, em, sig);
		Functor ret = toFunctor(xxx.first, xxx.second);
		ret.instance0 = xxx.second;
		return ret;
	}
	
	private static Functor toFunctor(Category C, Instance<String, String> I) {
		FUNCTION f = p0 -> {
			Path p = (Path) p0;
			return new Fn(I.nm.get(p.source), I.nm.get(p.target), x -> I.evaluate(p).get(x));
		};

		return new Functor(C, FinSet.FinSet, x -> I.nm.get(x), f);
	}
	

	
	private Triple<Category, Category, Mapping<String,String,String,String>> toMapping(FQLPPProgram env, MapConst ic) {
		CatExp src0 = resolve(env, ic.src);
		if (src0 == null) {
			throw new RuntimeException("Missing category: " + ic.src);
		}
		if (!(src0 instanceof Const)) {
			throw new RuntimeException(
					"Can only create mappings for finitely-presented categories.");
		}
		Const src = (Const) src0;

		CatExp dst0 = resolve(env, ic.dst);
		if (!(dst0 instanceof Const)) {
			throw new RuntimeException(
					"Can only create mappings for finitely-presented categories.");
		}
		Const dst = (Const) dst0;

		Category srcX = src.accept(env, this);
		Category dstX = dst.accept(env, this);

		Signature<String, String> srcY = new Signature<>(src.nodes, src.arrows, src.eqs);
		Signature<String, String> dstY = new Signature<>(dst.nodes, dst.arrows, dst.eqs);

		Map<Node, Node> nm = new HashMap<>();
		for (String n0 : ic.nm.keySet()) {
			Signature<String, String>.Node n = srcY.getNode(n0);
			String v = ic.nm.get(n.name);
			if (v == null) {
				throw new RuntimeException("Missing object mapping for " + n.name);
			}
			nm.put(n, dstY.getNode(v));
		}
		Map<Edge, Path> em = new HashMap<>();
		for (String n0 : ic.em.keySet()) {
			Signature<String, String>.Edge n = srcY.getEdge(n0);
			Pair<String, List<String>> k = ic.em.get(n.name);
			if (k == null) {
				throw new RuntimeException("Missing arrow mapping for " + n.name);
			}
			em.put(n, dstY.path(k.first, k.second));
		}

		Mapping<String, String, String, String> I = new Mapping(nm, em, srcY, dstY);
		
		return new Triple<>(srcX, dstX, I);
	}
	
	@Override
	public Functor visit(FQLPPProgram env, MapConst ic) {
		Triple<Category, Category, Mapping<String, String, String, String>> xxx = toMapping(env, ic);
		Mapping<String, String, String, String> I = xxx.third;

		FUNCTION f = p0 -> {
			Path p = (Path) p0;
			return I.apply(p);
		};

		Functor et = new Functor(xxx.first, xxx.second, x -> I.nm.get(x), f);
		et.mapping0 = xxx.third;
		return et;
	}

	@Override
	public Functor visit(FQLPPProgram env, CatConst ic) {
		CatExp e = resolve(env, ic.sig);
		if (!(e instanceof Const)) {
			throw new RuntimeException(
					"Can only create functors to cat from finitely-presented categories.");
		}
		Const c = (Const) e;

		Category cat = c.accept(env, this);
		Signature<String, String> sig = new Signature<>(c.nodes, c.arrows, c.eqs);

		Map<Node, Category> nm = new HashMap<>();
		for (Node n : sig.nodes) {
			CatExp kkk = ic.nm.get(n.name);
			if (kkk == null) {
				throw new RuntimeException("Missing node mapping from " + n);
			}
			nm.put(n, kkk.accept(env, this));
		}
		Map<Edge, Functor> em = new HashMap<>();
		for (Edge n : sig.edges) {
			FunctorExp chc = ic.em.get(n.name);
			if (chc == null) {
				throw new RuntimeException("Missing edge mapping from " + n);
			}
			em.put(n, chc.accept(env, this));
		}

		FUNCTION fff = p0 -> {
			Path p = (Path) p0;
			Functor fn = FinCat.FinCat.identity(nm.get(p.source));
			for (Object nnn : p.path) {
				Edge n = (Edge) nnn;
				fn = FinCat.FinCat.compose(fn, em.get(n));
			}
			return fn;
		};

		return new Functor(cat, FinCat.FinCat, nm::get, fff);
	}

	@Override
	public Functor visit(FQLPPProgram env, FinalConst ic) {
		CatExp e = resolve(env, ic.src);
		if (!(e instanceof Const)) {
			throw new RuntimeException(
					"Can only create functors from finitely-presented categories.");
		}
		Const c = (Const) e;

		Category cat = c.accept(env, this);
		Signature<String, String> sig = new Signature<>(c.nodes, c.arrows, c.eqs);

		Category target = ic.C.accept(env, this);

		Map<Node, Functor> nm = new HashMap<>();
		for (Node n : sig.nodes) {
			FunctorExp kkk = ic.nm.get(n.name);
			if (kkk == null) {
				throw new RuntimeException("Missing node mapping from " + n);
			}
			Functor F = kkk.accept(env, this);
			nm.put(n, F);
		}
		Map<Edge, Transform> em = new HashMap<>();
		for (Edge n : sig.edges) {
			TransExp chc = ic.em.get(n.name);
			if (chc == null) {
				throw new RuntimeException("Missing edge mapping from " + n);
			}
			em.put(n, chc.accept(env, this));
		}

		FUNCTION fff = p0 -> {
			Path p = (Path) p0;
			Object fn = target.identity(nm.get(p.source));
			for (Object nnn : p.path) {
				Edge n = (Edge) nnn;
				fn = target.compose(fn, em.get(n));
			}
			return fn;
		};

		return new Functor(cat, target, nm::get, fff);
	}

	@Override
	public Functor visit(FQLPPProgram env, TT e) {
		Category<?, ?> c = e.t.accept(env, this);
		return FinCat.terminal(c);
	}

	@Override
	public Functor visit(FQLPPProgram env, FF e) {
		Category<?, ?> c = e.t.accept(env, this);
		return FinCat.initial(c);
	}

	@Override
	public Functor visit(FQLPPProgram env, Fst e) {
		Category<?, ?> s = e.s.accept(env, this);
		Category<?, ?> t = e.t.accept(env, this);
		return FinCat.first(s, t);
	}

	@Override
	public Functor visit(FQLPPProgram env, Snd e) {
		Category<?, ?> s = e.s.accept(env, this);
		Category<?, ?> t = e.t.accept(env, this);
		return FinCat.second(s, t);
	}

	@Override
	public Functor visit(FQLPPProgram env, Inl e) {
		Category<?, ?> s = e.s.accept(env, this);
		Category<?, ?> t = e.t.accept(env, this);
		return FinCat.inLeftNC(s, t);
	}

	@Override
	public Functor visit(FQLPPProgram env, Inr e) {
		Category<?, ?> s = e.s.accept(env, this);
		Category<?, ?> t = e.t.accept(env, this);
		return FinCat.inRightNC(s, t);
	}

	@Override
	public Functor visit(FQLPPProgram env, Eval e) {
		Category<?, ?> s = e.s.accept(env, this);
		Category<?, ?> t = e.t.accept(env, this);
		return FinCat.eval(s, t);
	}

	@Override
	public Functor visit(FQLPPProgram env, Curry e) {
		Functor f = e.f.accept(env, this);
		if (!f.source.isInfinite() && f.target.equals(FinSet.FinSet)) {
			return Inst.CURRY(f); 
		}
		return FinCat.curry(f);
	}

	@Override
	public Functor visit(FQLPPProgram env, Case e) {
		Functor l = e.l.accept(env, this);
		Functor r = e.r.accept(env, this);
		if (FinSet.FinSet.equals(l.target) && FinSet.FinSet.equals(r.target)) {
			if (!l.source.equals(r.source)) {
				throw new RuntimeException("Source categories do not match");
			}
			return Inst.get(l.source).coproduct(l, r);
		}
		if (FinCat.FinCat.equals(l.target) && FinCat.FinCat.equals(r.target)) {
			if (!l.source.equals(r.source)) {
				throw new RuntimeException("Source categories do not match");
			}
			return FunCat.get(l.source).coproduct(l, r);
		}

		return FinCat.match(l, r);
	}
 
	@Override
	public Functor visit(FQLPPProgram env, Prod e) {
		Functor l = e.l.accept(env, this);
		Functor r = e.r.accept(env, this);
		if (FinSet.FinSet.equals(l.target) && FinSet.FinSet.equals(r.target)) {
			if (!l.source.equals(r.source)) {
				throw new RuntimeException("Source categories do not match");
			}
			return Inst.get(l.source).product(l, r);
		}
		if (FinCat.FinCat.equals(l.target) && FinCat.FinCat.equals(r.target)) {
			if (!l.source.equals(r.source)) {
				throw new RuntimeException("Source categories do not match");
			}
			return FunCat.get(l.source).product(l, r);
		}
		return FinCat.pair(l, r);
	}
	
	@Override
	public Functor visit(FQLPPProgram env, FunctorExp.Exp e) {
		Functor l = e.l.accept(env, this);
		Functor r = e.r.accept(env, this);
		if (FinSet.FinSet.equals(l.target) && FinSet.FinSet.equals(r.target)) {
			if (!l.source.equals(r.source)) {
				throw new RuntimeException("Source categories do not match");
			}
			return Inst.get(l.source).exp(l, r);
		}
		if (FinCat.FinCat.equals(l.target) && FinCat.FinCat.equals(r.target)) {
			if (!l.source.equals(r.source)) {
				throw new RuntimeException("Source categories do not match");
			}
			throw new RuntimeException("Not implemented yet"); 
//			return FunCat.get(l.source).product(l, r);
		}
		throw new RuntimeException("Cannot exponentiate " + l + " and " + r);
	}

	@Override
	public Functor visit(FQLPPProgram env, Iso e) {
		Category l = e.l.accept(env, this);
		Category r = e.r.accept(env, this);
		Optional<Pair<Functor, Functor>> k = FinCat.iso(l, r);
		if (!k.isPresent()) {
			throw new RuntimeException("Not isomorphic: " + e.l + " and " + e.r);
		}
		return e.lToR ? k.get().first : k.get().second;
	}

	@Override
	public Functor visit(FQLPPProgram env, SetSetConst e) {
		FUNCTION o = x -> {
			Set s = (Set) x;
			FQLPPEnvironment env2 = new FQLPPEnvironment(ENV);
//			FQLProgram env2 = new FQLProgram(env);
	//		SetExp c = new SetExp.Const(s);
			env2.sets.put(e.ob, s);
			return e.set.accept(env, new SetOps(env2));
		};

		FUNCTION a = x -> {
			Fn s = (Fn) x;
			Set src = s.source;
			Set dst = s.target;
		//	SetExp srcE = new SetExp.Const(src);
		//	SetExp dstE = new SetExp.Const(dst);
			FQLPPEnvironment env2 = new FQLPPEnvironment(ENV);
		//	FnExp c = new FnExp.Const(s::apply, srcE, dstE);
			env2.sets.put(e.src, src);
			env2.sets.put(e.dst, dst);
			env2.fns.put(e.f,s);
			return e.fun.accept(env, new SetOps(env2));
		};

		return new Functor(FinSet.FinSet, FinSet.FinSet, o, a);
	}

	@Override
	public Category visit(FQLPPProgram env, Named e) {
		if (e.name.equals("Set")) {
			return FinSet.FinSet;
		} else if (e.name.equals("Cat")) {
			return FinCat.FinCat;
		} else {
			throw new RuntimeException(e.name + " is not a category.");
		}
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Id e) {
		Functor ff = e.t.accept(env, this);
		return Transform.id(ff);
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Comp e) {
		Transform l = e.l.accept(env, this);
		Transform r = e.r.accept(env, this);
		return Transform.compose(l, r);
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Var e) {
		Transform<?, ?, ?, ?> x = ENV.trans.get(e.v);
		if (x == null) {
			throw new RuntimeException("Missing transform: " + e);
		}
		return x;
	}

	@Override
	public Transform visit(FQLPPProgram env, SetSet e) {
		Functor s = e.src.accept(env, this);
		Functor t = e.dst.accept(env, this);
		FUNCTION o = x -> {
			Set set = (Set) x;
			FQLPPEnvironment env2 = new FQLPPEnvironment(ENV);
		//	SetExp c = new SetExp.Const(set);
			env2.sets.put(e.ob, set);
			return e.fun.accept(env, new SetOps(env2));
		};

		return new Transform(s, t, o);
	}

	@Override
	public Transform visit(FQLPPProgram env, ToMap e) {
		Functor s = e.src.accept(env, this);
		Functor t = e.dst.accept(env, this);
		CatExp scat = resolve(env, e.s);
		if (!(scat instanceof Const)) {
			throw new RuntimeException("Source category of " + e + " is not a constant.");
		}
		// CatExp.Const scon = (CatExp.Const) scat;
		CatExp tcat = resolve(env, e.t);
		if (!(tcat instanceof Const)) {
			throw new RuntimeException("Target category of " + e + " is not a constant.");
		}
		Const tcon = (Const) tcat;

		// Signature ssig = new Signature(scon.nodes, scon.arrows, scon.eqs);
		Signature<String, String> tsig = new Signature<>(tcon.nodes, tcon.arrows,
				tcon.eqs);

		FUNCTION o = x -> {
			Node n = (Node) x;
			// Set src = (Set) s.applyO(n);
			// Set dst = (Set) t.applyO(n);
			Pair<String, List<String>> k = e.fun.get(n.name);
			Signature<String, String>.Path fun = tsig.path(k.first, k.second);
			return fun; // new Fn(src, dst, fun);
		};

		return new Transform(s, t, o);
	}

	@Override
	public Transform visit(FQLPPProgram env, ToSet e) {
		Functor s = e.src.accept(env, this);
		Functor t = e.dst.accept(env, this);
		FUNCTION o = x -> {
			Node n = (Node) x;
			Chc<FnExp, SetExp> chc = e.fun.get(n.name);
			if (chc == null) {
				throw new RuntimeException("Missing object mapping for: " + n.name);
			}
			if (chc.left) {
				return chc.l.accept(env, new SetOps(ENV));
			} 
				Set src = (Set) s.applyO(n);
				Set dst = (Set) t.applyO(n);
				Set<Pair> p = (Set<Pair>) chc.r.accept(env, new SetOps(ENV));
				Map<Object, Object> map = new HashMap<>();
				for (Pair h : p) {
					if (map.containsKey(h.first)) {
						throw new RuntimeException("Duplicate arg: " + e);
					}
					map.put(h.first, h.second);
				}
				return new Fn(src, dst, map::get);
			
		};

		return new Transform(s, t, o);
	}

	@Override
	public Transform visit(FQLPPProgram env, ToCat e) {
		Functor s = e.src.accept(env, this);
		Functor t = e.dst.accept(env, this);
		FUNCTION o = x -> {
			Node n = (Node) x;
			// Set src = (Set) s.applyO(n);
			// Set dst = (Set) t.applyO(n);
			Functor fun = e.fun.get(n.name).accept(env, this);
			// if (!src.equals(fun.source)) {
			return fun; // new Fn(src, dst, fun);
		};

		return new Transform(s, t, o);
	}

	@Override
	public Transform visit(FQLPPProgram env, ToInst e) {
		Functor s = e.src.accept(env, this);
		Functor t = e.dst.accept(env, this);
		FUNCTION o = x -> {
			Node n = (Node) x;
			// Set src = (Set) s.applyO(n);
			// Set dst = (Set) t.applyO(n);
			Transform fun = e.fun.get(n.name).accept(env, this);
			// if (!src.equals(fun.source)) {
			return fun; // new Fn(src, dst, fun);
		};

		return new Transform(s, t, o);
	}

	@Override
	public Category visit(FQLPPProgram env, Kleisli e) {
		FunctorExp f0 = env.ftrs.get(e.F);
		if (f0 == null) {
			throw new RuntimeException("Missing functor: " + e.F);
		}
		TransExp ret0 = env.trans.get(e.unit);
		if (ret0 == null) {
			throw new RuntimeException("Missing transform: " + e.unit);
		}
		TransExp join0 = env.trans.get(e.join);
		if (join0 == null) {
			throw new RuntimeException("Missing transform: " + e.join);
		}

		Functor F = f0.accept(env, this);
		Transform ret = ret0.accept(env, this);
		Transform join = join0.accept(env, this);
		if (e.isCo) {
			CoMonad m = new CoMonad(F, ret, join);
			return m.cokleisli();
		} 
			Monad m = new Monad(F, ret, join);
			return m.kleisli();
		
	}

	@Override
	public Transform visit(FQLPPProgram env, Proj e) {
		Functor l = e.l.accept(env, this);
		Functor r = e.r.accept(env, this);
		if (!l.source.equals(r.source)) {
			throw new RuntimeException("Source category does not match");
		}
		if (!l.target.equals(r.target)) {
			throw new RuntimeException("Target category does not match");
		}
		if (l.target.equals(FinSet.FinSet)) {
			return e.proj1 ? Inst.get(l.source).first(l, r) : Inst.get(l.source).second(l, r);
		} else if (l.target.equals(FinCat.FinCat)) {
			return e.proj1 ? FunCat.get(l.source).first(l, r) : FunCat.get(l.source).second(l, r);
		} else {
			throw new RuntimeException("report this error to ryan");
		}
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Prod e) {
		Transform l = e.l.accept(env, this);
		Transform r = e.r.accept(env, this);
		if (!l.source.equals(r.source)) {
			throw new RuntimeException("Source functors do not match");
		}
		if (!l.target.source.equals(r.target.source)) {
			throw new RuntimeException("Categories do not match");
		}
		if (l.source.target.equals(FinSet.FinSet)) {
			return Inst.get(l.target.source).pair(l, r);
		} else if (l.source.target.equals(FinCat.FinCat)) {
			return FunCat.get(l.target.source).pair(l, r);
		} else {
			throw new RuntimeException("Report this error to ryan.");
		}
	}

	@Override
	public Functor visit(FQLPPProgram env, FunctorExp.One e) {
		Category<?, ?> cat = e.cat.accept(env, this);
		Category<?, ?> amb = e.ambient.accept(env, this);
		if (amb.equals(FinSet.FinSet)) {
			return Inst.get(cat).terminal();
		} else if (amb.equals(FinCat.FinCat)) {
			return FunCat.get(cat).terminal();
		} else {
			throw new RuntimeException("Report this error to Ryan. Error: ambient category is "
					+ amb);
		}
	}

	@Override
	public Functor visit(FQLPPProgram env, FunctorExp.Zero e) {
		Category<?, ?> cat = e.cat.accept(env, this);
		Category<?, ?> amb = e.ambient.accept(env, this);
		if (amb.equals(FinSet.FinSet)) {
			return Inst.get(cat).initial();
		} else if (amb.equals(FinCat.FinCat)) {
			return FunCat.get(cat).initial();
		} else {
			throw new RuntimeException("Report this error to Ryan. Error: ambient category is "
					+ amb);
		}
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.One e) {
		Functor F = e.f.accept(env, this);
		if (F.target.equals(FinSet.FinSet)) {
			return Inst.get(F.source).terminal(F);
		} else if (F.target.equals(FinCat.FinCat)) {
			return FunCat.get(F.source).terminal(F);
		} else {
			throw new RuntimeException("Error: Please send your file to Ryan.");
		}
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Zero e) {
		Functor F = e.f.accept(env, this);
		if (F.target.equals(FinSet.FinSet)) {
			return Inst.get(F.source).initial(F);
		} else if (F.target.equals(FinCat.FinCat)) {
			return FunCat.get(F.source).initial(F);
		} else {
			throw new RuntimeException("Error: Please send your file to Ryan.");
		}
	}

	@Override
	public Transform visit(FQLPPProgram env, Inj e) {
		Functor l = e.l.accept(env, this);
		Functor r = e.r.accept(env, this);
		if (!l.source.equals(r.source)) {
			throw new RuntimeException("Source category does not match");
		}
		if (l.target.equals(FinSet.FinSet)) {
			return e.inj1 ? Inst.get(l.source).inleft(l, r) : Inst.get(l.source).inright(l, r);
		} else if (l.target.equals(FinCat.FinCat)) {
			return e.inj1 ? FunCat.get(l.source).inleft(l, r) : FunCat.get(l.source).inright(l, r);
		} else {
			throw new RuntimeException("Cannot inject: " + e + " to get a transform.");
		}
	}

	@Override
	public Transform visit(FQLPPProgram env, CoProd e) {
		Transform l = e.l.accept(env, this);
		Transform r = e.r.accept(env, this);
		if (!l.target.equals(r.target)) {
			throw new RuntimeException("Target functors do not match");
		}
		if (!l.target.source.equals(r.target.source)) {
			throw new RuntimeException("Categories do not match");
		}
		if (l.source.target.equals(FinSet.FinSet)) {
			return Inst.get(l.target.source).match(l, r);
		} else if (l.source.target.equals(FinCat.FinCat)) {
			return FunCat.get(l.target.source).match(l, r);
		} else {
			throw new RuntimeException("Report this error to Ryan");
		}
	}

	@Override
	public Functor visit(FQLPPProgram env, Uncurry e) {
		Functor f = e.F.accept(env, this);
		if (!f.source.isInfinite() && f.target instanceof Inst) {
			return Inst.UNCURRY(f);
		}
		throw new RuntimeException("Cannot uncurry " + f);
	}

	@Override
	public Functor visit(FQLPPProgram env, Migrate e) {
		Functor F = e.F.accept(env, this);
		switch (e.which) {
			case "delta":
				return FDM.deltaF(F);
			case "sigma":
				return FDM.sigmaF(F);
			case "pi":
				return FDM.piF(F);
			default:
				break;
		}
		throw new RuntimeException("Report this error to Ryan.");
	}

	@Override
	public Functor visit(FQLPPProgram env, Apply e) {
		Functor ret1 = null;
		Exception ret1_e = null;
		
		Functor ret2 = null;
		Exception ret2_e = null;

		Functor F = e.F.accept(env, this);
		
		try {
			Functor I = e.I.accept(env, this);
			ret1 = (Functor) F.applyO(I);
		} catch (Exception ex) {
			ret1_e = ex;
		}
		
		try {
			Var I = (Var) e.I;
			Node n = F.source.toSig().new Node(I.v);
			ret2 = (Functor) F.applyO(n);
		} catch (Exception ex) {
			ret2_e = ex;
		}
		
		if (ret1 != null && ret2 != null) {
			throw new RuntimeException("Ambiguous: " + e.I + " is an object in two different categories.");
		}
		
		if (ret1 != null) {
			return ret1;
		}
		if (ret2 != null) {
			return ret2;
		}

		if (ret1_e != null) {
			ret1_e.printStackTrace(); // TODO !!!
		}
		if (ret2_e != null) {
			ret2_e.printStackTrace();
		}
		throw new RuntimeException("Cannot apply:\n\nmost probable cause: " + ret1_e + "\n\nless probable cause: " + ret2_e);
}


	@Override
	public Transform visit(FQLPPProgram env, TransExp.Apply e) {
		Functor F = e.F.accept(env, this);
		Transform I = e.I.accept(env, this);
		return (Transform) F.applyA(I);
	}

	@Override
	public Transform visit(FQLPPProgram env, ApplyPath e) {
		Functor F = e.F.accept(env, this);
		CatExp c = resolve(env, e.cat);
		if (!(c instanceof Const)) {
			throw new RuntimeException("Can only take paths in constant categories.");
		}
		Const C = (Const) c;
		Signature s = new Signature(C.nodes, C.arrows, C.eqs);
		Path n = s.path(e.node, e.edges);
		return (Transform) F.applyA(n);
	}

	@Override
	public Transform visit(FQLPPProgram env, Adj e) {
		Functor F = e.F.accept(env, this);
		if (e.which.equals("return") && e.L.equals("sigma") && e.R.equals("delta")) {
			return FDM.sigmaDelta(F).unit;
		} else if (e.which.equals("coreturn") && e.L.equals("sigma") && e.R.equals("delta")) {
			return FDM.sigmaDelta(F).counit;
		} else if (e.which.equals("return") && e.L.equals("delta") && e.R.equals("pi")) {
			return FDM.deltaPi(F).unit;
		} else if (e.which.equals("coreturn") && e.L.equals("delta") && e.R.equals("pi")) {
			return FDM.deltaPi(F).counit;
		} else {
			throw new RuntimeException("Report this error to Ryan.");
		}
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Iso e) {
		Functor l = e.l.accept(env, this);
		Functor r = e.r.accept(env, this);
		if (!l.source.equals(r.source)) {
			throw new RuntimeException("Source categories do not match: " + l.source + "\nand\n" + r.source);
		}
		if (l.source.isInfinite()) {
			throw new RuntimeException("Source category must be finite.");
		}
		if (!l.target.equals(FinSet.FinSet)) {
			throw new RuntimeException("Target category must be Set.");
		}
		Optional<Pair<Transform, Transform>> k = Inst.get(l.source).iso(l, r);
		if (!k.isPresent()) {
			throw new RuntimeException("Not isomorphic: " + e.l + " and " + e.r);
		}
		return e.lToR ? k.get().first : k.get().second;
	}

	@Override
	public Transform visit(FQLPPProgram env, ApplyTrans e) {
		Transform F = e.F.accept(env, this);
		Functor I = e.I.accept(env, this);
		return (Transform) F.apply(I);
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Curry e) {
		Transform t = e.f.accept(env, this);
		if (t.source.source.isInfinite() || !t.source.target.equals(FinSet.FinSet)) {
			throw new RuntimeException("Cannot curry " + t);
		}
		return e.useInst ? Inst.get(t.source.source).curry(t) : Inst.CURRY(t);
	}

	@Override
	public Transform visit(FQLPPProgram env, TransExp.Eval e) {
		Functor a = e.a.accept(env, this);
		Functor b = e.b.accept(env, this);
		if (!a.source.equals(b.source) || a.source.isInfinite() || !a.target.equals(FinSet.FinSet)) {
			throw new RuntimeException("Cannot eval " + a + " and " + b);
		}
		return Inst.get(a.source).eval(a, b);
	}

	@Override
	public Transform visit(FQLPPProgram env, Whisker e) {
		Functor F  = e.func .accept(env, this);
		Transform T=e.trans.accept(env, this);

		return e.left ? Transform.leftWhisker(F, T) : Transform.rightWhisker(F, T);
	}

	@Override
	public Functor visit(FQLPPProgram env, Prop e) {
		Category c = e.cat.accept(env, this);
		return Inst.get(c).prop();
	}

	@Override
	public Transform visit(FQLPPProgram env, Bool e) {
		Category c = e.cat.accept(env, this);
		return e.b ? Inst.get(c).tru() : Inst.get(c).fals();
	}

	@Override
	public Transform visit(FQLPPProgram env, Chr e) {
		Transform t = e.t.accept(env, this);
		return Inst.get(t.source.source).chr(t);
	}

	@Override
	public Transform visit(FQLPPProgram env, Ker e) {
		Transform t = e.t.accept(env, this);
		return Inst.get(t.source.source).kernel(t);
	}

	@Override
	public Functor visit(FQLPPProgram env, FunctorExp.Dom e) {
		Transform t = new TransExp.Var(e.t).accept(env, this);
		return e.dom ? t.source : t.target;
	}

	@Override
	public Transform visit(FQLPPProgram env, AndOrNotImplies e) {
		Category c = e.cat.accept(env, this);
		return e.which.equals("not") ? Inst.get(c).not() : Inst.get(c).andOrImplies(e.which);
	}

	@Override
	public Transform visit(FQLPPProgram env, PeterApply e) {
		Transform t = e.t.accept(env, this);
		Node n = t.source.source.toSig().new Node(e.node);
		return (Transform) t.apply(n);
	}

	@Override
	public Functor visit(FQLPPProgram env, Pivot e) {
		Functor F = e.F.accept(env, this);
		return e.pivot ? Groth.pivot(F) : Groth.unpivot(F);
	}

	@Override
	public Functor visit(FQLPPProgram env, Pushout e) {
		Transform l = ENV.trans.get(e.l);
		if (l == null) {
			throw new RuntimeException("Missing transform: " + e.l);
		}
		Transform r = ENV.trans.get(e.r);
		if (r == null) {
			throw new RuntimeException("Missing transform: " + e.r);
		}
		
		if (!l.source.equals(r.source)) {
			throw new RuntimeException("Source functors do not match.");
		}
		Category<Object, Object> D = l.source.source;
		
		Set<String> objects = new HashSet<>();
		objects.add("A");
		objects.add("B");
		objects.add("C");
		Set<String> arrows = new HashSet<>();
		arrows.add("f");
		arrows.add("g");
		arrows.add("a");
		arrows.add("b");
		arrows.add("c");
		Map<String, String> sources = new HashMap<>();
		sources.put("f", "A");
		sources.put("g", "A");
		sources.put("a", "A");
		sources.put("b", "B");
		sources.put("c", "C");
		Map<String, String> targets = new HashMap<>();
		targets.put("f", "B");
		targets.put("g", "C");
		targets.put("a", "A");
		targets.put("b", "B");
		targets.put("c", "C");
		Map<Pair<String, String>, String> composition = new HashMap<>();
		composition.put(new Pair<>("a", "a"), "a");
		composition.put(new Pair<>("b", "b"), "b");
		composition.put(new Pair<>("c", "c"), "c");
		composition.put(new Pair<>("a", "f"), "f");
		composition.put(new Pair<>("a", "g"), "g");
		composition.put(new Pair<>("f", "b"), "f");
		composition.put(new Pair<>("g", "c"), "g");
		Map<String, String> identities = new HashMap<>();
		identities.put("A", "a");
		identities.put("B", "b");
		identities.put("C", "c");
		Category<String,String> span = new FiniteCategory<>(objects, arrows, sources, targets, composition, identities);
		
		Category<Pair<Object, String>,Pair<Object,String>> Dspan = FinCat.product(D, span);
		Functor<Pair<Object,String>,Pair<Object,String>,Object,Object> fst = FinCat.first(D, span);
		
		
		FUNCTION<Pair<Object,String>,Set> o = p -> {
			switch (p.second) {
				case "A":
					return (Set) l.source.applyO(p.first);
				case "B":
					return (Set) l.target.applyO(p.first);
				case "C":
					return (Set) r.target.applyO(p.first);
				default:
					throw new RuntimeException();
			}
		};
		FUNCTION<Pair<Object,String>,Fn> x = fp -> {
			Object f = fp.first;
			String p = fp.second;
			Object a = D.source(f);
		//	Object b = D.target(f);
			//String i = span.source(p);
			String j = span.target(p);
		//	Functor I = i == "A" ? l.source : i == "B" ? l.target : r.target;
			Functor J = Objects.equals(j, "A") ? l.source : Objects.equals(j, "B") ? l.target : r.target;
			//Fn If = (Fn) I.applyA(f);
			Fn Jf = (Fn) J.applyA(f);
			Transform P = Objects.equals(p, "f") ? l : Objects.equals(p, "g") ? r : Objects.equals(p, "a") ? Transform.id(l.source) : Objects.equals(p, "b") ? Transform.id(l.target) : Transform.id(r.target);
			//Fn Pb = (Fn) P.apply(b);
			Fn Pa = (Fn) P.apply(a);
			return Fn.compose(Pa, Jf);
		};
		Functor I = new Functor(Dspan, FinSet.FinSet0(), o, x);
		
		return FDM.sigmaF(fst).applyO(I);		
	}

	@Override
	public Category visit(FQLPPProgram env, Union e) {
		throw new RuntimeException("Union should have been eliminated by pre-processing.");
	}

	@Override
	public Category visit(FQLPPProgram env, Colim e) {
		FunctorExp f = ENV.prog.ftrs.get(e.F);
		if (f == null) {
			throw new RuntimeException("Undefined functor: "+ e.F);
		}
		if (!(f instanceof CatConst)) {
			throw new RuntimeException("Not a literal: " + e.F + ", is " + f.getClass());
		}
		CatConst F = (CatConst) f;
		
		Const C = Ben.colim(env, F);
		
		return C.accept(env, this);
	}
}

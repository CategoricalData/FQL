package catdata.opl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.script.Invocable;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.fqlpp.cat.FinSet;
import catdata.ide.DefunctGlobalOptions;
import catdata.opl.OplExp.OplJavaInst;
import catdata.opl.OplExp.OplSig;
import catdata.opl.OplParser.DoNotIgnore;
import catdata.provers.KBExp;
import catdata.provers.KBExpFactoryOldImpl;
import catdata.provers.KBHorn;
import catdata.provers.KBOptions;
import catdata.provers.KBOrders;

public class OplToKB<S,C,V> implements Operad<S, Pair<OplCtx<S,V>, OplTerm<C,V>>> {
	
	@Override
	public Set<S> objects() {
		return sig.sorts;
	}

	@Override
	public Set<Arrow<S, Pair<OplCtx<S, V>, OplTerm<C, V>>>> hom(List<S> src, S dst) {
		Set<Arrow<S, Pair<OplCtx<S, V>, OplTerm<C, V>>>> ret = new HashSet<>();
		for (Pair<OplCtx<S, V>, OplTerm<C, V>> x : hom0(Thread.currentThread(), src, dst)) {
			ret.add(new Arrow<>(src, dst, x));
		}
		return ret;
	}
	
	@Override
	public Arrow<S, Pair<OplCtx<S, V>, OplTerm<C, V>>> id(S o) {
		V v = fr.next();
		OplTerm<C,V> t = new OplTerm<>(v);
		OplCtx<S,V> g = new OplCtx<>(Collections.singletonList(new Pair<>(v, o)));
		return new Arrow<>(Collections.singletonList(o), o, new Pair<>(g, t));
	}

	@Override
	public Arrow<S, Pair<OplCtx<S, V>, OplTerm<C, V>>> comp(
			Arrow<S, Pair<OplCtx<S, V>, OplTerm<C, V>>> F,
			List<Arrow<S, Pair<OplCtx<S, V>, OplTerm<C, V>>>> A) {
		if (F.src.size() != A.size()) {
			throw new RuntimeException("Arity mismatch: " + F + " and " + A);
		}
		List<Pair<V,S>> new_vs = new LinkedList<>();
		List<OplTerm<C, V>> new_args = new LinkedList<>();
		List<S> new_sorts = new LinkedList<>();
		for (Arrow<S, Pair<OplCtx<S, V>, OplTerm<C, V>>> a : A) {
			Map<V, OplTerm<C,V>> mm = new HashMap<>();
			for (V v : a.a.first.names()) {
				V u = fr.next();
				mm.put(v, new OplTerm<>(u));
				S s = a.a.first.get(v);
				new_vs.add(new Pair<>(u, s));
				new_sorts.add(s);
			}
			new_args.add(a.a.second.subst(mm));
		}
		
		return new Arrow<>(new_sorts, F.dst, new Pair<>(new OplCtx<>(new_vs), F.a.second.subst(F.a.first, new_args)));
		
	}
	
	private final OplSig<S, C, V> sig;
	public final OplKB<C, V> KB;
	private final Iterator<V> fr;
//	private OplJavaInst I;
	
	
	public OplToKB(Iterator<V> fr, OplSig<S, C, V> sig) {
		this.sig = sig;
		if (DefunctGlobalOptions.debug.opl.opl_prover_require_const) {
			checkEmpty();
		}
		this.fr = fr;
		OplKB<C, V> KB0 = convert(this.sig);
		KB0.complete(Thread.currentThread());
		KB = KB0;
	}
	
	private void checkEmpty() {
		Set<S> m = new HashSet<>(sig.sorts);
		for (C f : sig.symbols.keySet()) {
			Pair<List<S>, S> a = sig.symbols.get(f);
			if (a.first.isEmpty()) {
				m.remove(a.second);
			}
		}
		
		if (!m.isEmpty()) {
			throw new RuntimeException("Sort " + Util.sep(m, ",") + " has no 0-ary constants");
		}
	}
	
	private final Map<Pair<List<S>, S>, Collection<Pair<OplCtx<S, V>, OplTerm<C, V>>>> hom = new HashMap<>();
	
	@SuppressWarnings("deprecation")
	private static void checkParentDead(Thread cur) {
		if (!cur.isAlive()) {
			Thread.currentThread().stop();
		}
	}
	
	//cur is a 'parent' - if the parent isn't alive, then the current thread shouldn't be either (e.g., cancel)
	public Collection<Pair<OplCtx<S,V>, OplTerm<C,V>>> hom0(Thread cur, List<S> s, S t) {
		Collection<Pair<OplCtx<S,V>, OplTerm<C,V>>> ret = hom.get(new Pair<>(s, t));
		if (!sig.sorts.contains(t)) {
			throw new DoNotIgnore("Bad target sort " + t);
		}
		if (!sig.sorts.containsAll(s)) {
			throw new DoNotIgnore("Bad source sort " + s);		
		}
		
		if (ret == null) {
//			ret = new LinkedList<>(ret)
			List<Pair<V, S>> vars = new LinkedList<>();
			Map<S, Set<V>> vars2 = new HashMap<>();
			//int i = 0;
			for (S z : s) {
				vars.add(new Pair<>(fr.next(), z));
				//i++;
			}
			OplCtx<S,V> ctx = new OplCtx<>(vars);
			for (S sort : sig.sorts) {
				vars2.put(sort, new HashSet<>());
			}
			for (Pair<V, S> k : vars) {
				vars2.get(k.second).add(k.first);
			}
			
		//	int count = 0;
			Map<S, Set<OplTerm<C, V>>> j = arity0(vars2);
            while (true) {
                checkParentDead(cur);
                Map<S, Set<OplTerm<C, V>>> k = inc(j);
                checkParentDead(cur);
                Map<S, Set<OplTerm<C, V>>> k2 = red(k);
                checkParentDead(cur);
                if (j.equals(k2)) {
                    break;
                }
                j = k2;
            }
			ret = j.get(t).stream().map(g -> nice(ctx, g)).collect(Collectors.toList());
			//ret = new LinkedList<>(ret);
			@SuppressWarnings({ "rawtypes", "unchecked" })
			List<Object> rret = (List) ret;
			rret.sort(Util.LengthComparator);
			hom.put(new Pair<>(s,t), ret);
		}
		return ret; 
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
    private Pair<OplCtx<S,V>, OplTerm<C,V>> nice(OplCtx<S, V> G, OplTerm<C, V> e) {
		int i = 0;
		Map m = new HashMap();
		List<Pair> l = new LinkedList<>();
		for (V v : G.names()) {
			l.add(new Pair("v" + i, G.get(v)));
			m.put(v, new OplTerm("v" + i++));
		}
		OplCtx ret = new OplCtx(l);
		return new Pair(ret, e.subst(m));
	}
	
	
	private Map<S, Set<OplTerm<C,V>>> red(Map<S, Set<OplTerm<C,V>>> in) {
		Map<S, Set<OplTerm<C,V>>> ret = new HashMap<>();
		
		for (S k : in.keySet()) {
			Set<OplTerm<C,V>> v = in.get(k);
			Set<OplTerm<C,V>> v2 = new HashSet<>();
			for (OplTerm<C,V> a : v) {
				KBExp<C, V> b = convert(a);
				KBExp<C, V> c = KB.nf(b);
				OplTerm<C, V> d = convert(c);
				v2.add(d);
			}
			ret.put(k, v2);
		}
		
		return ret;
	}

	public KBExp<C,V> nf(KBExp<C,V> r) {
		return KB.nf(r);
	}

	//already caches
	public OplTerm<C,V> nf(OplTerm<C,V> r) {
		return convert(KB.nf(convert(r)));
	} 
	
	private Map<S, Set<OplTerm<C,V>>> arity0(Map<S, Set<V>> vars) {
		Map<S, Set<OplTerm<C,V>>> m = new HashMap<>();
		for (S s : sig.sorts) {
			Set<OplTerm<C,V>> set = new HashSet<>();
			for (V v : vars.get(s)) {
				set.add(new OplTerm<>(v));
			}
			m.put(s, set);
		}
		for (C f : sig.symbols.keySet()) {
			Pair<List<S>, S> a = sig.symbols.get(f);
			if (a.first.isEmpty()) {
				m.get(a.second).add(new OplTerm<>(f, new LinkedList<>()));
			}
		}
		
		return m;
	}
	
	private Map<S, Set<OplTerm<C,V>>> inc(Map<S, Set<OplTerm<C,V>>> in) {
		Map<S, Set<OplTerm<C,V>>> out = new HashMap<>();
		for (S k : in.keySet()) {
			out.put(k, new HashSet<>(in.get(k)));
		}
		
		for (C f : sig.symbols.keySet()) {
			Pair<List<S>, S> a = sig.symbols.get(f);
			if (a.first.isEmpty()) {
				continue;
			}
			Map<Integer, List<OplTerm<C,V>>> arg_ts = new HashMap<>();
			int i = 0;
			for (S t : a.first) {
				arg_ts.put(i++, new LinkedList<>(in.get(t)));
			}
			List<LinkedHashMap<Integer, OplTerm<C,V>>> cands = FinSet.homomorphs(arg_ts);
			for (LinkedHashMap<Integer, OplTerm<C,V>> cand : cands) {
				List<OplTerm<C,V>> actual = new LinkedList<>();
				for (int j = 0; j < i; j++) {
					actual.add(cand.get(j));
				}
				out.get(a.second).add(new OplTerm<>(f, actual));
			}
		}
		
		return out;
	}
	
	
	public String printKB() {
		return KB.printKB();
	}
	
	public static <C,V> KBExp<C, V> convert(OplTerm<C,V> t) {
		if (t.var != null) {
			return KBExpFactoryOldImpl.factory.KBVar(t.var);
		}
		List<KBExp<C, V>> l = t.args.stream().map((Function<OplTerm<C, V>, KBExp<C, V>>) OplToKB::convert).collect(Collectors.toList());
		return KBExpFactoryOldImpl.factory.KBApp(t.head, l);
	}
	
	public static <C,V> OplTerm<C,V> convert(KBExp<C, V> t) {
		if (t.isVar()) {
			return new OplTerm<>(t.getVar());
		}
		List<OplTerm<C,V>> l = new LinkedList<>();
		for (KBExp<C, V> p : t.getArgs()) {
			l.add(convert(p));
		}
		return new OplTerm<>(t.f(), l);
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private  OplKB<C, V> convert(OplSig<S,C,V> s) {
		if (s.prec.keySet().size() != new HashSet<>(s.prec.values()).size()) {
			throw new RuntimeException("Cannot duplicate precedence: " + s.prec);
		}
		//if (!Collections.disjoint(Arrays.asList(KBHorn.reserved), s.symbols.keySet())) {
		//	throw new RuntimeException("Theory contains reserved symbol, one of " + Arrays.toString(KBHorn.reserved));
		//}
		Map<C, Pair<List<S>, S>>  symbols = new HashMap<>(s.symbols);
		List one = new LinkedList(); one.add(new Unit());
		List two = new LinkedList(); two.add(new Unit()); two.add(new Unit());
		symbols.put((C)KBHorn._eq, new Pair(two, new Unit()));
		symbols.put((C)KBHorn._or, new Pair(two, new Unit()));
		symbols.put((C)KBHorn._not, new Pair(one, new Unit()));
		symbols.put((C)KBHorn._true, new Pair(new LinkedList(), new Unit()));
		symbols.put((C)KBHorn._false, new Pair(new LinkedList(), new Unit()));
		
		Function<Pair<C, C>, Boolean> gt = x -> {
			Integer l = s.prec.get(x.first);
			Integer r = s.prec.get(x.second);
			if (l != null && r != null) {
				return l > r;				
			}
			if (l == null && r != null) {
				return false;
			}
			if (l != null) {
				return true;
			}
			String lx = x.first.toString();
			String rx = x.second.toString();
			if (!symbols.containsKey(x.first)) {
				throw new RuntimeException("Missing: " + x.first);
			}
			int la = symbols.get(x.first).first.size();
			int ra = symbols.get(x.second).first.size();
			if (la == ra) {
				if (lx.length() == rx.length()) {
					return lx.compareTo(rx) < 0;
				}
				return lx.length() < rx.length();
			}
			if (la >= 3 && ra >= 3) {
				return la > ra;
			}
			if (la == 0 && ra > 0) {
				return false;
			}
			if (la == 1 && (ra == 0 || ra == 2)) {
				return true;
			}
			if (la == 1 && ra > 2) {
				return false;
			}
			if (la == 2 && ra == 0) {
				return true;
			}
			if (la == 2 && (ra == 1 || ra > 2)) {
				return false;
			}
			if (la >= 3 || ra >= 3) {  //added Aug 3 16
				return la > ra;
			}
			throw new RuntimeException("Bug in precedence, report to Ryan: la=" + la + ", ra=" + ra + ", l=null r=null");
			//function symbols: arity-0 < arity-2 < arity-1 < arity-3 < arity-4
		};

		Set<Pair<KBExp<C, V>, KBExp<C, V>>> eqs = new HashSet<>();
		for (Triple<?, OplTerm<C, V>, OplTerm<C, V>> eq : s.equations) {
			eqs.add(new Pair<>(convert(eq.second), convert(eq.third)));
		}
		
		Set<Pair<KBExp<C, V>, KBExp<C, V>>> rs = new HashSet<>();
		for (Triple<?, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>, List<Pair<OplTerm<C, V>, OplTerm<C, V>>>> impl : s.implications) {
			rs.addAll(convert(impl.second, impl.third));
		}
		
		KBOptions options = new KBOptions(DefunctGlobalOptions.debug.opl.opl_prover_unfailing, 
				DefunctGlobalOptions.debug.opl.opl_prover_sort, DefunctGlobalOptions.debug.opl.opl_allow_horn && !s.implications.isEmpty(), 
				DefunctGlobalOptions.debug.opl.opl_prover_ac, DefunctGlobalOptions.debug.opl.opl_prover_timeout, 
				DefunctGlobalOptions.debug.opl.opl_prover_reduction_limit, DefunctGlobalOptions.debug.opl.opl_prover_filter_subsumed,
				/* NEWDEBUG.debug.opl.simplify, */ DefunctGlobalOptions.debug.opl.opl_prover_compose, false);
		return new OplKB(eqs, KBOrders.lpogt(DefunctGlobalOptions.debug.opl.opl_allow_horn && !s.implications.isEmpty(), gt), fr, rs, options);			
	}
	
	private static <C,V> Set<Pair<KBExp<C, V>, KBExp<C, V>>> convert(List<Pair<OplTerm<C, V>, OplTerm<C, V>>> x, List<Pair<OplTerm<C, V>, OplTerm<C, V>>> y) {
		Set<Pair<KBExp<C, V>, KBExp<C, V>>> rs = new HashSet<>();
		KBExp<C, V> lhs = KBHorn.fals();
		for (Pair<OplTerm<C, V>, OplTerm<C, V>> l : x) {
			KBExp<C, V> e1 = convert(l.first);
			KBExp<C, V> e2 = convert(l.second);
			KBExp<C, V> eq = KBHorn.eq(e1, e2);
			lhs = KBHorn.or(lhs, KBHorn.not(eq));
		}
		for (Pair<OplTerm<C, V>, OplTerm<C, V>> l : y) {
			KBExp<C, V> e1 = convert(l.first);
			KBExp<C, V> e2 = convert(l.second);
			KBExp<C, V> eq = KBHorn.eq(e1, e2);				
			KBExp<C, V> lhs0 = KBHorn.or(lhs, eq);
			rs.add(new Pair<>(lhs0, KBHorn.tru()));
		}
		return rs;
	}

	@SuppressWarnings("rawtypes")
	public static <Z,V> KBExp<Chc<Z,JSWrapper>,V> redBy(OplJavaInst I, KBExp<Chc<Z,JSWrapper>,V> e) {				
		if (I == null) {
			return e;
		}
		try {
			if (e.isVar()) {
				return e;
			}
				
			//KBApp<Chc<Z,JSWrapper>,V> e0 = e.getApp();
				
			List<KBExp<Chc<Z,JSWrapper>,V>> l = new LinkedList<>();
			List<Object> r = new LinkedList<>();
			for (KBExp<Chc<Z, JSWrapper>, V> a : e.getArgs()) {
				KBExp<Chc<Z, JSWrapper>,V> b = redBy(I, a);
				l.add(b);
				if (!b.isVar() && b.getArgs().isEmpty() && !b.f().left) {
					JSWrapper js = b.f().r;
					r.add(js.o);
				}	
			}
			if (l.size() == r.size() && e.f().left) {
				Pair<Function, Object> xxx = Util.stripChcs(e.f().l);
				if (I.defs.containsKey(xxx.second)) {
					if (I.defs.containsKey("_compose") ) {
						List<Object> rr = new LinkedList<>();
						Object ff = I.engine.eval((String)xxx.second);
						rr.add(ff);
						rr.add(r);
						Object o = ((Invocable)I.engine).invokeFunction("_compose", rr);
						return KBExpFactoryOldImpl.factory.KBApp(Chc.inRight(new JSWrapper(o)), new LinkedList<>());
					} 
						Object o = ((Invocable)I.engine).invokeFunction((String)xxx.second, r);
						return KBExpFactoryOldImpl.factory.KBApp(Chc.inRight(new JSWrapper(o)), new LinkedList<>());
					
				}
			} 
			return KBExpFactoryOldImpl.factory.KBApp(e.f(), l);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex.getMessage());
		} 
	} 
		
	@SuppressWarnings("deprecation")
	public Map<S, Set<OplTerm<C, V>>> doHoms() {
		Map<S, Set<OplTerm<C, V>>> sorts = new HashMap<>();
		Thread cur = Thread.currentThread();
		Runnable r = () -> {
                    try {
                        for (S s : sig.sorts) {
                            sorts.put(s, hom0(cur, new LinkedList<>(), s).stream().map(x -> x.second).collect(Collectors.toSet()));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        //		throw new RuntimeException(ex.getMessage());
                    }
                };
		Thread t = new Thread(r);
		t.start();
		try {
			t.join(DefunctGlobalOptions.debug.opl.opl_saturate_timeout);
			t.stop();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex.getMessage());
		}
		if (sorts.keySet().size() == sig.sorts.size()) {
			return sorts;
		}
		throw new RuntimeException("Timeout (" + DefunctGlobalOptions.debug.opl.opl_saturate_timeout + ") exceeded, sorts are " + sorts + ".  Possible cause: infinite instance");
	}

}

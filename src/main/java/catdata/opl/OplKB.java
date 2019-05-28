package catdata.opl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.RuntimeInterruptedException;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.provers.KBExp;
import catdata.provers.KBExpFactoryOldImpl;
import catdata.provers.KBHorn;
import catdata.provers.KBOptions;
import catdata.provers.KBUnifier;

/**
 * 
 * Do not use - replaced by LPOUKB.
 */
@SuppressWarnings("deprecation")
public class OplKB<C, V>  {
	 
	private boolean isComplete = false;
	private boolean isCompleteGround = false;
	
	private final List<Pair<KBExp<C, V>, KBExp<C, V>>> R;
    private List<Pair<KBExp<C, V>, KBExp<C, V>>> E;
    private List<Pair<KBExp<C, V>, KBExp<C, V>>> G; //order matters
	
	private final Iterator<V> fresh;
	
	public final Function<Pair<KBExp<C, V>, KBExp<C, V>>, Boolean> gt;
	private final Set<Pair<Pair<KBExp<C, V>, KBExp<C, V>>, Pair<KBExp<C, V>, KBExp<C, V>>>> seen = new HashSet<>();

	private Map<C, List<Pair<KBExp<C, V>, KBExp<C, V>>>> AC_symbols;
//	protected List<Pair<KBExp<C, V>, KBExp<C, V>>> AC_R, G;
	
	private int count = 0;

	private final KBOptions options;
	
	/**
	 * @param E0 initial equations
	 * @param gt0 ordering
	 * @param fresh fresh variable generator
	 */
	public OplKB(Set<Pair<KBExp<C, V>, KBExp<C, V>>> E0, Function<Pair<KBExp<C, V>, 
			KBExp<C, V>>, Boolean> gt0, Iterator<V> fresh,
			Set<Pair<KBExp<C, V>, KBExp<C, V>>> R0, KBOptions options) {
		this.options = options;
        R = new LinkedList<>();
		for (Pair<KBExp<C, V>, KBExp<C, V>> r : R0) {
			R.add(freshen(fresh, r));
		}
        gt = gt0;
		this.fresh = fresh;
        E = new LinkedList<>();
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : E0) {
			E.add(freshen(fresh, e));
		}
        G = new LinkedList<>();
		try {
			initAC();
		} catch (InterruptedException e1) {
			throw new RuntimeInterruptedException(e1);
//			e1.printStackTrace();
//			throw new RuntimeException("Interrupted " + e1.getMessage());
		}
		initHorn();
		/* if (isProgram()) {
			R.addAll(E);
			E.clear();
			isCompleteGround = true;
			isComplete = true;
		}   //doesnt seem to help*/
	}
	
	
	
	private void initAC() throws InterruptedException {
		if (!options.semantic_ac) {
			return;
		}
		Map<C, Integer> symbols = new HashMap<>();
		AC_symbols = new HashMap<>();
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : E) {
			e.first.symbols(symbols);
			e.second.symbols(symbols);
		}
		outer: for (C f : symbols.keySet()) {
			Integer i = symbols.get(f);
			if (i != 2) {
				continue;
			}
			boolean cand1_found = false;
			boolean cand2_found = false;
			List<Pair<KBExp<C, V>, KBExp<C, V>>> cands = AC_E(f);
			Pair<KBExp<C, V>, KBExp<C, V>> cand1 = cands.get(0);
			Pair<KBExp<C, V>, KBExp<C, V>> cand2 = cands.get(1);
			for (Pair<KBExp<C, V>, KBExp<C, V>> other : E) {
				if (subsumes(fresh, cand1, other) || subsumes(fresh, cand1, other.reverse())) {
					cand1_found = true;
				}
				if (subsumes(fresh, cand2, other) || subsumes(fresh, cand2, other.reverse())) {
					cand2_found = true;
				}
				if (cand1_found && cand2_found) {
					List<Pair<KBExp<C, V>, KBExp<C, V>>> l = new LinkedList<>();
					l.add(AC_E(f).get(1)); //assoc rewrite rule
					l.add(AC_E(f).get(0)); //comm eq
					l.addAll(AC_E0(f)); //perm eqs
					AC_symbols.put(f, l);
					continue outer;
				}
			}
		}
	}
	
	private KBExp<C, V> achelper(C f, V xx, V yy, V zz) {
		KBExp<C,V> x = KBExpFactoryOldImpl.factory.KBVar(xx);
		KBExp<C,V> y = KBExpFactoryOldImpl.factory.KBVar(yy);
		KBExp<C,V> z = KBExpFactoryOldImpl.factory.KBVar(zz);
		List<KBExp<C,V>> yz = new LinkedList<>();
		yz.add(y);
		yz.add(z);
		List<KBExp<C,V>> xfyz = new LinkedList<>();
		xfyz.add(x);
		xfyz.add(KBExpFactoryOldImpl.factory.KBApp(f, yz));
		return KBExpFactoryOldImpl.factory.KBApp(f, xfyz);
	}
	
	private List<Pair<KBExp<C, V>, KBExp<C, V>>> AC_E0(C f) {
		List<Pair<KBExp<C, V>, KBExp<C, V>>> ret = new LinkedList<>(); 
		V x = fresh.next();
		V y = fresh.next();
		V z = fresh.next();
		
		ret.add(freshen(fresh, new Pair<>(achelper(f, x, y, z), achelper(f, y, x, z))));
		ret.add(freshen(fresh, new Pair<>(achelper(f, x, y, z), achelper(f, z, y, x))));
		ret.add(freshen(fresh, new Pair<>(achelper(f, x, y, z), achelper(f, y, z, x))));
		
		return ret;
	}
	
	
	
	private List<Pair<KBExp<C, V>, KBExp<C, V>>> AC_E(C f) {
		List<Pair<KBExp<C, V>, KBExp<C, V>>> ret = new LinkedList<>(); 
		KBExp<C,V> x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		KBExp<C,V> y = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		List<KBExp<C,V>> xy = new LinkedList<>();
		xy.add(x);
		xy.add(y);
		List<KBExp<C,V>> yx = new LinkedList<>();
		yx.add(y);
		yx.add(x);
		ret.add(new Pair<>(KBExpFactoryOldImpl.factory.KBApp(f, xy), KBExpFactoryOldImpl.factory.KBApp(f, yx)));
		
		x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		y = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		KBExp<C,V> z = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		List<KBExp<C,V>> yz = new LinkedList<>();
		yz.add(y);
		yz.add(z);
		xy = new LinkedList<>();
		xy.add(x);
		xy.add(y);
		List<KBExp<C,V>> xfyz = new LinkedList<>();
		xfyz.add(x);
		xfyz.add(KBExpFactoryOldImpl.factory.KBApp(f, yz));
		List<KBExp<C,V>> fxyz = new LinkedList<>();
		fxyz.add(KBExpFactoryOldImpl.factory.KBApp(f, xy));
		fxyz.add(z);
		ret.add(new Pair<>(KBExpFactoryOldImpl.factory.KBApp(f, fxyz), KBExpFactoryOldImpl.factory.KBApp(f, xfyz)));
		
		return ret;
	}

	private void initHorn() {
		if (!options.horn) {
			return;
		}
		R.add(new Pair<>(KBHorn.not(KBHorn.tru()), KBHorn.fals()));
		R.add(new Pair<>(KBHorn.not(KBHorn.fals()), KBHorn.tru()));
		KBExp<C,V> x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		R.add(new Pair<>(KBHorn.or(x, KBHorn.tru()), KBHorn.tru()));
		x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		R.add(new Pair<>(KBHorn.or(x, KBHorn.fals()), x));
		x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		R.add(new Pair<>(KBHorn.or(KBHorn.tru(), x), KBHorn.tru()));
		x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		R.add(new Pair<>(KBHorn.or(KBHorn.fals(), x), x));
		x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
		R.add(new Pair<>(KBHorn.eq(x, x), KBHorn.tru()));
	//	x = KBExpFactoryOldImpl.factory.KBVar(fresh.next());
	//	R.add(new Pair<>(KBHorn.or(x, KBHorn.not(x)), KBHorn.tru()));
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static <C, V> Pair<KBExp<C, V>, KBExp<C, V>> freshen(Iterator<V> fresh, Pair<KBExp<C, V>, KBExp<C, V>> eq) {
		Map<V, KBExp<C, V>> subst = freshenMap(fresh, eq).first;
		return new Pair<>(eq.first.substitute(subst), eq.second.substitute(subst));
	}

	private static <C,V> Pair<Map<V, KBExp<C, V>>, Map<V, KBExp<C, V>>> freshenMap(
            Iterator<V> fresh, Pair<KBExp<C, V>, KBExp<C, V>> eq) {
		Set<V> vars = new HashSet<>();
		KBExp<C, V> lhs = eq.first;
		KBExp<C, V> rhs = eq.second;
		vars.addAll(lhs.getVars());
		vars.addAll(rhs.getVars());
		Map<V, KBExp<C, V>> subst = new HashMap<>();
		Map<V, KBExp<C, V>> subst_inv = new HashMap<>();
		for (V v : vars) {
			V fr = fresh.next();
			subst.put(v, KBExpFactoryOldImpl.factory.KBVar(fr));
			subst_inv.put(fr, KBExpFactoryOldImpl.factory.KBVar(v));
		}
		return new Pair<>(subst, subst_inv);
	}
	

	private static <X> void remove(Collection<X> X, X x) {
		while (X.remove(x));
	}
	
	private static <X> void add(Collection<X> X, X x) {
		if (!X.contains(x)) {
			X.add(x);
		}
	}
	
	private static <X> void addFront(List<X> X, X x) {
		if (!X.contains(x)) {
			X.add(0, x);
		}
	}
	
	private static <X> void addAll(Collection<X> X, Collection<X> x) {
		for (X xx : x) {
			add(X, xx);
		}
	}

	private void sortByStrLen(List<Pair<KBExp<C, V>, KBExp<C, V>>> l) {
		if (options.unfailing) {
			List<Pair<KBExp<C, V>, KBExp<C, V>>> unorientable = new LinkedList<>();
			List<Pair<KBExp<C, V>, KBExp<C, V>>> orientable = new LinkedList<>();
			for (Pair<KBExp<C, V>, KBExp<C, V>> k : l) {
				if (orientable(k)) {
					orientable.add(k);
				} else {
					unorientable.add(k);
				}
			}
			orientable.sort(Util.ToStringComparator);
			l.clear();
			l.addAll(orientable);
			l.addAll(unorientable);
		} else {
			l.sort(Util.ToStringComparator);
		}
	}
	
	private static void checkParentDead(Thread cur) throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
		if (cur != null && !cur.isAlive()) {
			Thread.currentThread().stop();
		} 
		
	}
	
	public void complete() {
		try {
			while (!step(null));
		} catch (InterruptedException ex) {
			throw new RuntimeInterruptedException(ex);
		}
		if (!isCompleteGround) {
			throw new RuntimeException("Not ground complete after iteration timeout.  Last state:\n\n" + toString());
		} 
	}
	
	
	//if the parent dies, the current thread will too
	public void complete(Thread parent) {
		String[] arr = new String[] { null };
		Runnable r = () -> {
                    try {
                        while (!step(parent));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        arr[0] = ex.getMessage();
                    }
                };				
		Thread t = new Thread(r);
		t.start();
		try {
			t.join(options.iterations);
			t.stop();
		} catch (Exception ex) {
			//ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		if (arr[0] != null) {
			throw new RuntimeException(arr[0] + "\n\nLast state:\n\n" + toString());			
		}
		if (!isCompleteGround) {
			//allCpsConfluent(true, true);
			throw new RuntimeException("Not ground complete after iteration timeout.  Last state:\n\n" + toString());
		} 
	}
	
	private static <C, V> boolean subsumes(Iterator<V> fresh, Pair<KBExp<C, V>, KBExp<C, V>> cand,
                                           Pair<KBExp<C, V>, KBExp<C, V>> other) throws InterruptedException {
		return (subsumes0(fresh, cand, other) != null);
	}
	

    private static <C, V> Map<V, KBExp<C, V>> subsumes0(Iterator<V> fresh, Pair<KBExp<C, V>, KBExp<C, V>> cand,
                                                        Pair<KBExp<C, V>, KBExp<C, V>> other) throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
		Pair<KBExp<C, V>, KBExp<C, V>> candX = cand; 
		
		if (!Collections.disjoint(candX.first.getVars(), other.first.getVars()) ||
			!Collections.disjoint(candX.first.getVars(), other.second.getVars()) ||
			!Collections.disjoint(candX.second.getVars(), other.first.getVars())||
			!Collections.disjoint(candX.second.getVars(), other.second.getVars())) {	
			candX = freshen(fresh, cand);
		}
		 
		List<KBExp<C, V>> l = new LinkedList<>(); l.add(candX.first); l.add(candX.second);
		KBExp<C, V> cand0 = KBExpFactoryOldImpl.factory.KBApp( "", l);

		List<KBExp<C, V>> r = new LinkedList<>(); r.add(other.first); r.add(other.second);
		KBExp<C, V> other0 = KBExpFactoryOldImpl.factory.KBApp( "", r);
		
		Map<V, KBExp<C, V>> subst = KBUnifier.findSubst(other0, cand0);
		return subst;
	}
	
	private List<Pair<KBExp<C, V>, KBExp<C, V>>> filterSubsumed(
            Collection<Pair<KBExp<C, V>, KBExp<C, V>>> CPX) throws InterruptedException {
		List<Pair<KBExp<C, V>, KBExp<C, V>>> CP = new LinkedList<>();
		outer: for (Pair<KBExp<C, V>, KBExp<C, V>> cand : CPX) {
			for (Pair<KBExp<C, V>, KBExp<C, V>> e : E) {
				if (subsumes(fresh, cand, e)) {
					continue outer; 
				}
			}
			CP.add(cand);
		}
		return CP;
	}

	private List<Pair<KBExp<C, V>, KBExp<C, V>>> filterSubsumedBySelf(
            Collection<Pair<KBExp<C, V>, KBExp<C, V>>> CPX) throws InterruptedException {
		List<Pair<KBExp<C, V>, KBExp<C, V>>> CP = new LinkedList<>(CPX);
		
		Iterator<Pair<KBExp<C, V>, KBExp<C, V>>> it = CP.iterator();
		while (it.hasNext()) {
			Pair<KBExp<C, V>, KBExp<C, V>> cand = it.next();
			for (Pair<KBExp<C, V>, KBExp<C, V>> e : CP) {
				if (cand.equals(e)) {
					continue;
				}
				if (subsumes(fresh, cand, e)) {
					it.remove();
					break;
				}
				if (subsumes(fresh, cand.reverse(), e)) {
					it.remove();
					break;
				}
				if (subsumes(fresh, cand, e.reverse())) {
					it.remove();
					break;
				}
				//: this one redundant?
				if (subsumes(fresh, cand.reverse(), e.reverse())) {
					it.remove();
					break;
				}
			}
		}
		return CP;
	}
	
	//is also compose2
	//simplify RHS of a rule
    private void compose() throws InterruptedException {
		Pair<KBExp<C, V>, KBExp<C, V>> to_remove;
		Pair<KBExp<C, V>, KBExp<C, V>> to_add;
		do {
			to_remove = null;
			to_add = null;
			for (Pair<KBExp<C, V>, KBExp<C, V>> r : R) {
				Set<Pair<KBExp<C, V>, KBExp<C, V>>> R0 = new HashSet<>(R);
				R0.remove(r);
				KBExp<C, V> new_rhs = red(null, Util.append(E,G), R0, r.second);
				if (!new_rhs.equals(r.second)) {
					to_remove = r;
					to_add = new Pair<>(r.first, new_rhs);
					break;
				}
			}
			if (to_remove != null) {
				R.remove(to_remove);
				R.add(to_add);
			}
		} while (to_remove != null);
	}
	
	//  For this to be a true semi-decision procedure, open terms should first be skolemized
	//@Override
    /*
    private boolean eq(KBExp<C, V> lhs, KBExp<C, V> rhs) {
		KBExp<C, V> lhs0 = nf(lhs);
		KBExp<C, V> rhs0 = nf(rhs);
		if (lhs0.equals(rhs0)) {
			return true;
		}

		if (isComplete) {
			return false;
		}

		try {
			step(Thread.currentThread());
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Interrupted " + e.getMessage());
		}
		return eq(lhs, rhs);
	} */
	
	//@Override
	public KBExp<C, V> nf(KBExp<C, V> e) {
		try {

		if (e.getVars().isEmpty()) {
			if (!isCompleteGround) {
				throw new RuntimeException("Cannot find ground normal form for ground incomplete system.");
			}
			return red(null, Util.append(E,G), R, e);
		}
		if (!isComplete) {
			throw new RuntimeException("Cannot find normal form for incomplete system.\n\n" + this);
		}
		return red(null, Util.append(E,G), R, e);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			throw new RuntimeException("Interrupted: " + e1.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	/*
	  Requires <C> and <V> to be String.  Renames _v345487 to _v0, for example.

	  @return A nicer printout of the rules
	 */
	public String printKB() {
		OplKB<String, String> kb = (OplKB<String, String>) this; //dangerous
		
	//	List<Pair<KBExp<String, String>, KBExp<String, String>>> EE = new LinkedList<>(kb.E);
	//	EE.addAll(kb.G);
		
		List<String> E0 = new LinkedList<>();
		for (Pair<KBExp<String, String>, KBExp<String, String>> r : kb.E) {
			int i = 0;
			Map<String, KBExp<String, String>> m = new HashMap<>();
			for (String v : r.first.getVars()) {
				if (v.startsWith("_v") && !m.containsKey(v)) {
					m.put(v, KBExpFactoryOldImpl.factory.KBVar("v" + i++));
				}
			}
			for (String v : r.second.getVars()) {
				if (v.startsWith("_v") && !m.containsKey(v)) {
					m.put(v, KBExpFactoryOldImpl.factory.KBVar("v" + i++));
				}
			}
			E0.add(stripOuter(r.first.substitute(m).toString()) + " = " + stripOuter(r.second.substitute(m).toString()));
		}
		E0.sort(Comparator.comparingInt(String::length));
		
		List<String> G0 = new LinkedList<>();
		for (Pair<KBExp<String, String>, KBExp<String, String>> r : kb.G) {
			int i = 0;
			Map<String, KBExp<String, String>> m = new HashMap<>();
			for (String v : r.first.getVars()) {
				if (v.startsWith("_v") && !m.containsKey(v)) {
					m.put(v, KBExpFactoryOldImpl.factory.KBVar("v" + i++));
				}
			}
			for (String v : r.second.getVars()) {
				if (v.startsWith("_v") && !m.containsKey(v)) {
					m.put(v, KBExpFactoryOldImpl.factory.KBVar("v" + i++));
				}
			}
			G0.add(stripOuter(r.first.substitute(m).toString()) + " = " + stripOuter(r.second.substitute(m).toString()));
		}
		G0.sort(Comparator.comparingInt(String::length));

		
		List<String> R0 = new LinkedList<>();
		for (Pair<KBExp<String, String>, KBExp<String, String>> r : kb.R) {
			int i = 0;
			Map<String, KBExp<String, String>> m = new HashMap<>();
			for (String v : r.first.getVars()) {
				if (v.startsWith("_v") && !m.containsKey(v)) {
					m.put(v, KBExpFactoryOldImpl.factory.KBVar("v" + i++));
				}
			}
			for (String v : r.second.getVars()) {
				if (v.startsWith("_v") && !m.containsKey(v)) {
					m.put(v, KBExpFactoryOldImpl.factory.KBVar("v" + i++));
				}
			}
			R0.add(stripOuter(r.first.substitute(m).toString()) + " -> " + stripOuter(r.second.substitute(m).toString()));
		}
		R0.sort(Comparator.comparingInt(String::length));
				
		return (Util.sep(R0, "\n\n") + "\n\nE--\n\n" + Util.sep(E0, "\n\n") + "\n\nG--\n\n" + Util.sep(G0, "\n\n")).trim();
	}
	
	private static String stripOuter(String s) {
		if (s.startsWith("(") && s.endsWith(")")) {
			return s.substring(1, s.length() - 1);
		}
		return s;
	}

	private KBExp<C, V> red(Map<KBExp<C, V>, KBExp<C, V>> cache,
                            Collection<Pair<KBExp<C, V>, KBExp<C, V>>> Ex,
                            Collection<Pair<KBExp<C, V>, KBExp<C, V>>> Ry,
                            KBExp<C, V> e) throws InterruptedException {
		int i = 0;
		KBExp<C, V> orig = e;
				
//		Collection<Pair<KBExp<C, V>, KBExp<C, V>>> Ey = new LinkedList<>(Ex);
	//	Ey.addAll(G);
        while (true) {
            i++;

            KBExp<C, V> e0 = step(cache, fresh, Ex, Ry, e);
            if (e.equals(e0)) {
                return e0;
            }
            if (i > options.red_its) {
            	System.out.println(e);
            	System.out.println(e0);
                throw new RuntimeException(
                        "Reduction taking too long (>" + options.red_its + "):" + orig + " goes to " + e0 + " under\n\neqs:" + Util.sep(E, "\n") + "\n\nreds:" + Util.sep(R, "\n"));
            }
            e = e0;
        }
	}
	
	private KBExp<C, V> step(Map<KBExp<C, V>, KBExp<C, V>> cache, Iterator<V> fresh,
                             Collection<Pair<KBExp<C, V>, KBExp<C, V>>> E, Collection<Pair<KBExp<C, V>, KBExp<C, V>>> R, KBExp<C, V> ee) throws InterruptedException {
		if (ee.isVar()) {
			return step1(cache, fresh, E, R, ee); 
		} 
			KBExp<C, V> e = ee; //.getApp();
			List<KBExp<C, V>> args0 = new LinkedList<>();
			for (KBExp<C, V> arg : e.getArgs()) {
				args0.add(step(cache, fresh, E, R, arg)); //needs to be step for correctness
			}
			KBExp<C, V> ret = KBExpFactoryOldImpl.factory.KBApp(e.f(), args0);
			return step1(cache, fresh, E, R, ret);
		
	}
	
	//simplifies equations
	//can also use E U G with extra checking
    private void simplify() throws InterruptedException {
		Map<KBExp<C,V>, KBExp<C,V>> cache = new HashMap<>();  //helped 2x during tests

		List<Pair<KBExp<C, V>, KBExp<C, V>>> newE = new LinkedList<>();
		Set<Pair<KBExp<C, V>, KBExp<C, V>>> newE2 = new HashSet<>(); //also helpful for performance
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : E) {
			KBExp<C, V>	lhs_red = red(cache, new LinkedList<>(), R, e.first);
			KBExp<C, V> rhs_red = red(cache, new LinkedList<>(), R, e.second);
			if (!lhs_red.equals(rhs_red)) {
				Pair<KBExp<C, V>, KBExp<C, V>> p = new Pair<>(lhs_red, rhs_red);
				if (!newE2.contains(p)) {
					newE.add(p);
					newE2.add(p);
				}
//				add(newE, p);
			}
		}
		E = newE;
	}

	//is not collapse2
	//can also use E U G here
    private void collapseBy(Pair<KBExp<C, V>, KBExp<C, V>> ab) throws InterruptedException {
		Set<Pair<KBExp<C, V>, KBExp<C, V>>> AB = Collections.singleton(ab);
		Iterator<Pair<KBExp<C, V>, KBExp<C, V>>> it = R.iterator();
		while (it.hasNext()) {
		Pair<KBExp<C, V>, KBExp<C, V>> r = it.next();
			if (r.equals(ab)) {
				continue;
			}
			KBExp<C, V> lhs = red(null, new LinkedList<>(), AB, r.first);
			if (!r.first.equals(lhs)) {
				addFront(E, new Pair<>(lhs, r.second));	
				it.remove();
			} 
		}
	}

	private Set<Pair<KBExp<C, V>, KBExp<C, V>>> allcps2(
            Set<Pair<Pair<KBExp<C, V>, KBExp<C, V>>, Pair<KBExp<C, V>, KBExp<C, V>>>> seen,
            Pair<KBExp<C, V>, KBExp<C, V>> ab) throws InterruptedException {
		Set<Pair<KBExp<C, V>, KBExp<C, V>>> ret = new HashSet<>();

		Set<Pair<KBExp<C, V>, KBExp<C, V>>> E0 = new HashSet<>(E);
		E0.add(ab);
		E0.add(ab.reverse());
		Pair<KBExp<C, V>, KBExp<C, V>> ba = ab.reverse();
		for (Pair<KBExp<C, V>, KBExp<C, V>> gd : E0) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			Set<Pair<KBExp<C, V>, KBExp<C, V>>> s;
			Pair<KBExp<C, V>, KBExp<C, V>> dg = gd.reverse();

			if (!seen.contains(new Pair<>(ab, gd))) {
				s = cp(ab, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ab, gd));
			}
			if (!seen.contains(new Pair<>(gd, ab))) {
				s = cp(gd, ab);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ab));
			}
			if (!seen.contains(new Pair<>(ab, dg))) {
				s = cp(ab, dg);
				ret.addAll(s);
				seen.add(new Pair<>(ab, dg));
			}
			if (!seen.contains(new Pair<>(dg, ab))) {
				s = cp(dg, ab);
				ret.addAll(s);
				seen.add(new Pair<>(dg, ab));
			}
			////
			if (!seen.contains(new Pair<>(ba, gd))) {
				s = cp(ba, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ba, gd));
			}
			if (!seen.contains(new Pair<>(gd, ba))) {
				s = cp(gd, ba);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ba));
			}
			if (!seen.contains(new Pair<>(ba, dg))) {
				s = cp(ba, dg);
				ret.addAll(s);
				seen.add(new Pair<>(ba, dg));
			}
			if (!seen.contains(new Pair<>(dg, ba))) {
				s = cp(dg, ba);
				ret.addAll(s);
				seen.add(new Pair<>(dg, ba));
			}
		}
		
		for (Pair<KBExp<C, V>, KBExp<C, V>> gd : R) {
			Set<Pair<KBExp<C, V>, KBExp<C, V>>> s;

			if (!seen.contains(new Pair<>(ab, gd))) {
				s = cp(ab, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ab, gd));
			}
			if (!seen.contains(new Pair<>(gd, ab))) {
				s = cp(gd, ab);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ab));
			}
			////
			if (!seen.contains(new Pair<>(ba, gd))) {
				s = cp(ba, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ba, gd));
			}
			if (!seen.contains(new Pair<>(gd, ba))) {
				s = cp(gd, ba);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ba));
			}
		}
		return ret;
	}

	private Set<Pair<KBExp<C, V>, KBExp<C, V>>> allcps(
            Set<Pair<Pair<KBExp<C, V>, KBExp<C, V>>, Pair<KBExp<C, V>, KBExp<C, V>>>> seen,
            Pair<KBExp<C, V>, KBExp<C, V>> ab) throws InterruptedException {
		Set<Pair<KBExp<C, V>, KBExp<C, V>>> ret = new HashSet<>();
		for (Pair<KBExp<C, V>, KBExp<C, V>> gd : R) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			Set<Pair<KBExp<C, V>, KBExp<C, V>>> s;
			if (!seen.contains(new Pair<>(ab, gd))) {
				s = cp(ab, gd);
				ret.addAll(s);
				seen.add(new Pair<>(ab, gd));
			}

			if (!seen.contains(new Pair<>(gd, ab))) {
				s = cp(gd, ab);
				ret.addAll(s);
				seen.add(new Pair<>(gd, ab));
			}
		}
		return ret;
	}

	private Set<Pair<KBExp<C, V>, KBExp<C, V>>> cp(Pair<KBExp<C, V>, KBExp<C, V>> gd0, Pair<KBExp<C, V>, KBExp<C, V>> ab0) throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
		Pair<KBExp<C, V>, KBExp<C, V>> ab = freshen(fresh, ab0);
		Pair<KBExp<C, V>, KBExp<C, V>> gd = freshen(fresh, gd0);
		
		Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V,KBExp<C,V>>>> retX = gd.first.cp(new LinkedList<>(), ab.first,
				ab.second, gd.first, gd.second);

		Set<Pair<KBExp<C, V>, KBExp<C, V>>> ret = new HashSet<>();
		for (Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>> c : retX) {
			//ds !>= gs
			KBExp<C, V> gs = gd.first.substitute(c.third);
			KBExp<C, V> ds = gd.second.substitute(c.third);
			if ((gt.apply(new Pair<>(ds, gs)) || gs.equals(ds))) {
				continue;
			}
			//bs !>= as
			KBExp<C, V> as = ab.first.substitute(c.third);
			KBExp<C, V> bs = ab.second.substitute(c.third);
			if ((gt.apply(new Pair<>(bs, as)) || as.equals(bs))) {
				continue;
			}
			Pair<KBExp<C, V>, KBExp<C, V>> toAdd = new Pair<>(c.first, c.second);
				ret.add(toAdd);
		}
		
		return ret;
	}

	private KBExp<C, V> step1(Map<KBExp<C, V>, KBExp<C, V>> cache, Iterator<V> fresh,
                              Collection<Pair<KBExp<C, V>, KBExp<C, V>>> E, Collection<Pair<KBExp<C, V>, KBExp<C, V>>> R, KBExp<C, V> e0) throws InterruptedException {
		KBExp<C, V> e = e0;
		if (cache != null && cache.containsKey(e)) {
			return cache.get(e);
		}
		//does not improve performance
		//Map<Pair<KBExp<C, V>, KBExp<C, V>>, Map<V, KBExp<C, V>>> findSubstCache = new HashMap<>();
		for (Pair<KBExp<C, V>, KBExp<C, V>> r0 : R) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			Pair<KBExp<C, V>, KBExp<C, V>> r = r0;
			if (!Collections.disjoint(r.first.getVars(), e.getVars()) || !Collections.disjoint(r.second.getVars(), e.getVars())) {
				r = freshen(fresh, r0);
			}
			
			KBExp<C, V> lhs = r.first;
			KBExp<C, V> rhs = r.second;
			Map<V, KBExp<C, V>> s;
		//	if (lhs.equals(e)) { doesn't seem to help
			//	e = rhs;
		//		continue;
		//	} 
		//	if (findSubstCache.containsKey(new Pair<>(lhs, e))) {
				s = KBUnifier.findSubst(lhs, e);
			//	if (s != null) {
		//			findSubstCache.put(new Pair<>(lhs, e), s);
		//		}
		//	} else {
			//	s = KBUnifier.findSubst(lhs, e);				
		//	}
			if (s == null) {
				continue;
			}
			e = rhs.substitute(s);
		}
		e = step1Es(E, e);
		if (cache != null) {
			cache.put(e0, e);
		}
		return e;
	}
	
	
	
	public static class NewConst {
		
		private final Unit u = new Unit();

		

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + u.hashCode();
			return result;
		}



		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NewConst other = (NewConst) obj;
			return u.equals(other.u);
		}



		@Override
		public String toString() {
			return "(FRESH)";
		}
		
		

	}
	
	private KBExp<C, V> step1Es(Collection<Pair<KBExp<C, V>, KBExp<C, V>>> E, KBExp<C, V> e) {
		if (options.unfailing  && e.getVars().isEmpty() ) {
			for (Pair<KBExp<C, V>, KBExp<C, V>> r0 : E) {
				KBExp<C, V> a = step1EsX(r0, e);
				if (a != null) {
					e = a;
				}
				KBExp<C, V> b = step1EsX(new Pair<>(r0.second, r0.first), e);
				if (b != null) {
					e = b;
				}
			}
		}
		return e;
	}

	private KBExp<C, V> step1EsX(Pair<KBExp<C, V>, KBExp<C, V>> r0, KBExp<C, V> e) {
		Pair<KBExp<C, V>, KBExp<C, V>> r = r0;
		if (!Collections.disjoint(r.first.getVars(), e.getVars())
				|| !Collections.disjoint(r.second.getVars(), e.getVars())) {
			r = freshen(fresh, r0);
		}

		KBExp<C, V> lhs = r.first;
		KBExp<C, V> rhs = r.second;
		Map<V, KBExp<C, V>> s0 = KBUnifier.findSubst(lhs, e);
		if (s0 == null) {
			return null;
		}
		Map<V, KBExp<C, V>> s = new HashMap<>(s0);
		

		KBExp<C, V> lhs0 = lhs.substitute(s);
		KBExp<C, V> rhs0 = rhs.substitute(s);
		
		Set<V> newvars = new HashSet<>();
		newvars.addAll(lhs0.getVars());
		newvars.addAll(rhs0.getVars());
		Map<V, KBExp<C, V>> t = new HashMap<>();
 		for (V v : newvars) {
			t.put(v, KBExpFactoryOldImpl.factory.KBApp(new NewConst(), Collections.emptyList())); 
		}
 		lhs0 = lhs0.substitute(t);
 		rhs0 = rhs0.substitute(t);
 	
		if (gt.apply(new Pair<>(lhs0, rhs0))) {
			return rhs0;
		} 
		return null;
	}

	
	private Collection<Pair<KBExp<C, V>, KBExp<C, V>>> reduce(
            Collection<Pair<KBExp<C, V>, KBExp<C, V>>> set) throws InterruptedException {
		Set<Pair<KBExp<C, V>, KBExp<C, V>>> p = new HashSet<>();
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : set) {
			KBExp<C, V> lhs = red(new HashMap<>(), Util.append(E,G), R, e.first);
			KBExp<C, V> rhs = red(new HashMap<>(), Util.append(E,G), R, e.second);
			if (lhs.equals(rhs)) {
				continue;
			}
			p.add(new Pair<>(lhs, rhs));
		}
		return p;
	}
/*
	protected List<Pair<KBExp<C, V>, KBExp<C, V>>> removeOrientable(List<Pair<KBExp<C, V>, KBExp<C, V>>> l) {
		List<Pair<KBExp<C, V>, KBExp<C, V>>> ret = new LinkedList<>();
		Iterator<Pair<KBExp<C, V>, KBExp<C, V>>> it = l.iterator();
		while (it.hasNext()) {
			Pair<KBExp<C, V>, KBExp<C, V>> p = it.next();
			if (orientable(p)) {
				it.remove();
				ret.add(p);
			}
		}
		return ret;
	} */
	
	private boolean strongGroundJoinable(KBExp<C, V> s, KBExp<C, V> t) throws InterruptedException {
	List<Pair<KBExp<C, V>, KBExp<C, V>>> R0 = new LinkedList<>();
		List<Pair<KBExp<C, V>, KBExp<C, V>>> E0 = new LinkedList<>();
		for (C f : AC_symbols.keySet()) {
			List<Pair<KBExp<C, V>, KBExp<C, V>>> lx = AC_symbols.get(f);
			R0.add(lx.get(0));
			E0.addAll(lx.subList(1, 5));
		}
			
		if (!s.equals(red(null, new LinkedList<>(), R0, s))) {
			return false;
		}
		if (!t.equals(red(null, new LinkedList<>(), R0, t))) {
			return false;
		}
		
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : E0) {
			Map<V, KBExp<C,V>> m = subsumes0(fresh, new Pair<>(s,t), e);
			if (m == null) {
				m = subsumes0(fresh, new Pair<>(t,s), e);
			}
			if (m == null) {
				m = subsumes0(fresh, new Pair<>(s,t), e.reverse());
			}
			if (m == null) {
				m = subsumes0(fresh, new Pair<>(s, t), e.reverse());
			}
			if (m == null) {
				continue;
			}
			//return false;
			//if (bijection(m)) {
				//continue;
			//} 
			return false;
		}
			
		KBExp<C, V> s0 = sort(s, AC_symbols.keySet());
		KBExp<C, V> t0 = sort(t, AC_symbols.keySet());
		
		return s0.equals(t0);
		
	}
	

	public KBExp<C, V> sort0(KBExp<C, V> x, Collection<C> acs) {
		List<KBExp<C, V>> args0 = new ArrayList<>(acs.size());
		for (KBExp<C, V> arg : x.getArgs()) {
			args0.add(sort(arg, acs));
		}
		if (!acs.contains(x.f())) {
			return KBExpFactoryOldImpl.factory.KBApp(x.f(), args0);
		}
		KBExp<C, V> a1 = args0.get(0);
		KBExp<C, V> a2 = args0.get(1);
		List<KBExp<C, V>> l = new ArrayList<>(2);
		if (a1.toString().compareTo(a2.toString()) >= 0) {
			// if (Integer.compare(a1.hashCode(), a2.hashCode()) >= 0) {
			l.add(a1);
			l.add(a2);
		} else {
			l.add(a2);
			l.add(a1);
		}
		return KBExpFactoryOldImpl.factory.KBApp(x.f(), l);
	}

	
	public KBExp<C, V> sort1(KBExp<C, V> x, Collection<C> acs) {
		List<KBExp<C, V>> args0 = new ArrayList<>(x.getArgs().size());
		for (KBExp<C, V> arg : x.getArgs()) {
			args0.add(sort1(arg,acs));
		}
		if (!acs.contains(x.f())) {
			return KBExpFactoryOldImpl.factory.KBApp(x.f(), args0);
		}
		KBExp<C, V> a1 = args0.get(0);
		KBExp<C, V> a2x = args0.get(1);
		if (a2x.isVar() || !a2x.f().equals(x.f())) {
			return KBExpFactoryOldImpl.factory.KBApp(x.f(), args0);
		}

		KBExp<C, V> a2 = a2x.getArgs().get(0);
		KBExp<C, V> a3 = a2x.getArgs().get(1);
		List<KBExp<C, V>> l = new ArrayList<>(2);
		List<KBExp<C, V>> r = new ArrayList<>(2);
		if (a1.toString().compareTo(a2.toString()) >= 0) {
			// if (Integer.compare(a1.hashCode(), a2.hashCode()) >= 0) {
			return KBExpFactoryOldImpl.factory.KBApp(x.f(), args0);
		}
		l.add(a2);
		r.add(a1);
		r.add(a3);
		l.add(KBExpFactoryOldImpl.factory.KBApp(x.f(), r));
		return KBExpFactoryOldImpl.factory.KBApp(x.f(), l);

	}

	
	//: when filtering for subsumed, can also take G into account
    private boolean step(Thread parent) throws InterruptedException {
		count++;

		checkParentDead(parent); 
		
		if (options.horn) {
			handleHorn();
		}
		
		if (checkEmpty()) {
			return true;
		}

		if (options.semantic_ac) {
			filterStrongGroundJoinable();
		}
		
		Pair<KBExp<C, V>, KBExp<C, V>> st = pick(E);
		
		KBExp<C, V> s0 = st.first;
		KBExp<C, V> t0 = st.second;
		KBExp<C, V> a, b;
		boolean oriented = false;
		if (gt.apply(new Pair<>(s0, t0))) {
			a = s0; b = t0;
			oriented = true;
		} else if (gt.apply(new Pair<>(t0, s0))) {
			a = t0; b = s0;
			oriented = true;
		} else if (s0.equals(t0)) {
			remove(E, st); return false; //in case x = x coming in
		}  
		else {
			if (options.unfailing) {
				remove(E, st);
				add(E, st); //for sorting, will add to end of list
				a = s0; b = t0; 
			} else {
				throw new RuntimeException("Unorientable: " + st.first + " = " + st.second);
			}
		}
		Pair<KBExp<C, V>, KBExp<C, V>> ab = new Pair<>(a, b);
		if (oriented) {
			R.add(ab);
			List<Pair<KBExp<C, V>, KBExp<C, V>>> CP = filterSubsumed(allcps(seen, ab));
			addAll(E, CP);
			remove(E, st); 
			collapseBy(ab);
		} else {
			List<Pair<KBExp<C, V>, KBExp<C, V>>> CP = filterSubsumed(allcps(seen, ab));
			CP.addAll(filterSubsumed(allcps(seen, ab.reverse())));
			CP.addAll(filterSubsumed(allcps2(seen, ab)));
			CP.addAll(filterSubsumed(allcps2(seen, ab.reverse())));		
			addAll(E, CP);
		}
		
		checkParentDead(parent); 
		
		if (options.compose) {
			compose();
			checkParentDead(parent); 
		}
		
		//: appear to need simplify for correctness.  checked again: definitely need
//		if (options.simplify) {
			simplify(); //definitely needed... cuts down on number of iterations
			//simplify2();	//: add this in for efficiency sometime 
			checkParentDead(parent); 
	//	}
		
		if (options.sort_cps) {
			sortByStrLen(E);
			checkParentDead(parent); 
		}
			
		if (options.filter_subsumed_by_self) {
			E = filterSubsumedBySelf(E);
			checkParentDead(parent); 
		}
		
		return false;	
	}
	
	private void filterStrongGroundJoinable() throws InterruptedException {
		List<Pair<KBExp<C, V>, KBExp<C, V>>> newE = new LinkedList<>(E);
		/*for (Pair<KBExp<C, V>, KBExp<C, V>> st : newE) {
			if (strongGroundJoinable(st.first, st.second)) {
				remove(E, st);
				add(G, st);
			} 
		}*/
		G = filterSubsumedBySelf(G);
	}


	private void handleHorn() {
		Iterator<Pair<KBExp<C, V>, KBExp<C, V>>> it = R.iterator();
		while (it.hasNext()) {
			Pair<KBExp<C, V>, KBExp<C, V>> r = it.next();
			if (!r.second.equals(KBHorn.tru())) {
				continue;
			}
			if (r.first.isVar()) {
				continue;
			}
			KBExp<C,V> app = r.first;
			if (app.f().equals(KBHorn._eq)) {
				if (app.getArgs().get(0).equals(app.getArgs().get(1))) {
					continue;
				}
				E.add(new Pair<>(app.getArgs().get(0), app.getArgs().get(1)));
				it.remove();
			}
		}
	}

	private Pair<KBExp<C, V>, KBExp<C, V>> pick(List<Pair<KBExp<C, V>, KBExp<C, V>>> l) {
        for (Pair<KBExp<C, V>, KBExp<C, V>> x : l) {
            if (orientable(x)) {
                return x;
            }
        }
		return l.get(0);
	}
	
	private boolean orientable(Pair<KBExp<C, V>, KBExp<C, V>> e) {
		if (gt.apply(e)) {
			return true;
		}
        return gt.apply(e.reverse());
    }
	
	//: add ground-completeness check sometime
    private boolean checkEmpty() throws InterruptedException {
		if (E.isEmpty()) {
			isComplete = true;
			isCompleteGround = true;
			return true;
		}
		if (!noEqsBetweenAtoms() || !noNonReflRewrites() || !allUnorientable()) {
			return false;
		}
		if (allCpsConfluent(false, false) || (options.semantic_ac && allCpsConfluent(false, true))) {
			isComplete = false;
			isCompleteGround = true;
			return true;
		}
		
		return false;
	}
	
	private boolean noEqsBetweenAtoms() {
		if (!options.horn) {
			return true;
		}
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : E) {
			if (KBHorn.isAtom(e.first) || KBHorn.isAtom(e.second)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean noNonReflRewrites() {
		if (!options.horn) {
			return true;
		}
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : R) {
			if (!e.first.isVar() && e.second.equals(KBHorn.tru())) {
				KBExp<C,V> a = e.first;
				if (a.f().equals(KBHorn._eq)) {
					if (!a.getArgs().get(0).equals(a.getArgs().get(1))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean allUnorientable() {
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : E) {
			if (orientable(e)) {
				return false;
			}
		}
		return true;
	}

	private boolean allCpsConfluent(boolean print, boolean ground) throws InterruptedException {
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : E) {
			List<Pair<KBExp<C, V>, KBExp<C, V>>> set = filterSubsumed(reduce(allcps2(
					new HashSet<>(), e)));
			if (!allCpsConfluent(print, ground, "equation " + e, set)) {
				return false;
			}
		} 
		for (Pair<KBExp<C, V>, KBExp<C, V>> e : R) {
			List<Pair<KBExp<C, V>, KBExp<C, V>>> set = filterSubsumed(reduce(allcps(new HashSet<>(), e)));
			if (!allCpsConfluent(print, ground, "rule" + e, set)) {
				return false;
			}
		}
		return true;
	}
	
	
	public KBExp<C, V> sort(KBExp<C, V> x, Collection<C> acs) {
		KBExp<C, V> ret = x;
		while (true) {
			KBExp<C, V> next = sort1(sort0(ret, acs), acs);
			if (ret.equals(next)) {
				return ret;
			}
			ret = next;
		}
	}
	
	@SuppressWarnings("unused")
	private boolean allCpsConfluent(boolean print, boolean ground, String s, Collection<Pair<KBExp<C, V>, KBExp<C, V>>> set) throws InterruptedException {
		outer: for (Pair<KBExp<C, V>, KBExp<C, V>> e : set) {
			KBExp<C, V> lhs = red(new HashMap<>(), Util.append(E,G), R, e.first);
			KBExp<C, V> rhs = red(new HashMap<>(), Util.append(E,G), R, e.second);
			if (!lhs.equals(rhs)) {
				if (ground) {
					for (Pair<KBExp<C, V>, KBExp<C, V>> ex : G) {
						if (subsumes(fresh, new Pair<>(lhs, rhs), ex) ||
								subsumes(fresh, new Pair<>(rhs, lhs), ex)) {
							continue outer;
						}
					}
					if (options.semantic_ac) {
						if (!sort(lhs,AC_symbols.keySet()).equals(sort(rhs,AC_symbols.keySet()))) {
							return false;
						}
					}
				} else {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		List<String> a = E.stream().map(x -> x.first + " = " + x.second).collect(Collectors.toList());
		List<String> b = R.stream().map(x -> x.first + " -> " + x.second).collect(Collectors.toList());
		
		return (Util.sep(a, "\n") + "\n" + Util.sep(b, "\n")).trim();
	} 
	
	
	

}

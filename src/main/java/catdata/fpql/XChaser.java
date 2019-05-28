package catdata.fpql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Triple;
import catdata.fpql.XExp.XSuperED;
import catdata.fpql.XExp.XSuperED.SuperFOED;
import catdata.fqlpp.cat.FinSet;

@SuppressWarnings({ "unchecked", "rawtypes" })
class XChaser {

	public static void validate(XSuperED phi, XCtx<String> S, XCtx<String> T) {
		if (!phi.cod.keySet().equals(phi.dom.keySet())) {
			throw new RuntimeException("function symbols cod/dom not equal");
		}
		for (String f : phi.cod.keySet()) {
			List<String> ss = phi.dom.get(f);
			String t = phi.cod.get(f);
			for (String s : ss) {
				if (!S.ids.contains(s)) {
					throw new RuntimeException("Not a source entity: " + s);
				}
			}
			if (!T.allIds().contains(t) && !t.equals("DOM")) {
				throw new RuntimeException("Not a target entity: " + t);
			}
		}
		for (SuperFOED psi : phi.as) {
			for (String v : psi.a.keySet()) {
				String ty = psi.a.get(v);
				if (!S.ids.contains(ty)) {
					throw new RuntimeException("Not a source entity: " + ty);
				}
			}
			for (Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>> eq : psi.lhs) {
				Pair<String, String> lt = check(phi, psi, eq.first, S, T);
				Pair<String, String> rt = check(phi, psi, eq.second, S, T);
				if (!lt.equals(rt)) {
					throw new RuntimeException("Not equal types: " + lt + " and " + rt);
				}
				if (!lt.first.equals("_1")) {
					throw new RuntimeException("Does not start at 1: " + eq.first);
				}
				noEdgesFrom(T.terms(), eq.first);
				noEdgesFrom(T.terms(), eq.second);
			}
			for (Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>> eq : psi.rhs) {
				Pair<String, String> lt = check(phi, psi, eq.first, S, T);
				Pair<String, String> rt = check(phi, psi, eq.second, S, T);
				if (!lt.equals(rt)) {
					throw new RuntimeException("Not equal types: " + lt + " and " + rt);
				}
				if (!lt.first.equals("_1")) {
					throw new RuntimeException("Does not start at 1: " + eq.first);
				}
				if (S.ids.contains(lt.second)) {
					throw new RuntimeException("RHS of -> contains an equality of source entity " + lt);
				}	
			}
		}
	}

	@SuppressWarnings("unlikely-arg-type")
	private static void noEdgesFrom(Set<String> X,
			Triple<String, List<List<String>>, List<String>> t) {
		if (t.third != null) {
			if (X.contains(t)) {
				throw new RuntimeException("Contains edge from target");
			}
		}
		if (t.first != null) {
			for (List<String> Y : t.second) {
				if (X.contains(Y)) {
					throw new RuntimeException("Contains edge from target");
				}	
			}
		}
	}

	private static Pair<String,String> check(XSuperED phi, SuperFOED psi, Triple<String, List<List<String>>, List<String>> bulb, XCtx<String> S, XCtx<String> T) {
		String bulb_dst = null;
		List<String> bulb_src0;
		if (bulb.first != null) {
			bulb_src0 = phi.dom.get(bulb.first);
			if (bulb_src0 == null) {
				throw new RuntimeException("Not a function symbol: " + bulb.first);
			}
			List<Pair<String, String>> bulb_src = new LinkedList<>();
            for (String aBulb_src0 : bulb_src0) {
                bulb_src.add(new Pair<>("_1", aBulb_src0));
            }

			List<Pair<String,String>> arg_ts = bulb.second.stream().map(x -> S.typeWith(x, psi.a)).collect(Collectors.toList());
			if (!bulb_src.equals(arg_ts)) {
				throw new RuntimeException("Arg types are " + arg_ts + " expected " + bulb_src);
			}
			bulb_dst = phi.cod.get(bulb.first);
		}
		if (bulb.third == null) {
			return new Pair<>("_1", bulb_dst);
		} 
			List<String> copy = new LinkedList<>(bulb.third);
			Map<String, String> m = new HashMap<>(psi.a);
			if (bulb_dst != null) {
				copy.add(0, "x_x_x_x");
				m.put("x_x_x_x", bulb_dst);
				return T.typeWith(copy, m);
			}
			return S.typeWith(copy, m);
		

	}
	
	public static <C> XCtx chase(XSuperED ed, XCtx<C> S, XCtx T, XCtx<C> I) {
		Map<String,Set<List<Triple<C,C,List<C>>>>> dom = new HashMap<>();
		Map<Pair<String, List<Triple<C,C,List<C>>>>, Object> gens = new HashMap<>();
		Set eqs = new HashSet<>();
		for (String f : ed.dom.keySet()) {
			dom.put(f, new HashSet<>());
		}
		boolean stable = false;
		for (int i = 0; i < 31; i++) {
			Triple<Map<String,Set<List<Triple<C,C,List<C>>>>>, Map<Pair<String, List<Triple<C,C,List<C>>>>,Object>, Set> next = step(ed, S, T, I, dom, gens, eqs);
			if (dom.equals(next.first) && gens.equals(next.second) && eqs.equals(next.third)) {
				stable = true;
				break;
			}
		}
		if (!stable) {
			throw new RuntimeException("Chase has not reached a fixed point.");
		}
		
		Map typs = new HashMap<>();
		for (String f : dom.keySet()) {
			C t = (C) ed.cod.get(f);
			for (List<Triple<C, C, List<C>>> gen : dom.get(f)) {
				typs.put(new Pair(f, gen), new Pair("_1",t));
			}
		}
		return new XCtx(new HashSet<>(), typs , eqs, T.global, T, "instance");
		
	}
	
	@SuppressWarnings("unused")
    private static <C> Triple<Map<String,Set<List<Triple<C,C,List<C>>>>>,
							 Map<Pair<String,List<Triple<C,C,List<C>>>>, Object>, 
							 Set> 
	step(XSuperED ED, XCtx<C> S, XCtx T, XCtx<C> I, Map<String, Set<List<Triple<C, C, List<C>>>>> dom,
         Map<Pair<String, List<Triple<C, C, List<C>>>>, Object> gens, Set eqs) {
		for (SuperFOED ed : ED.as) {
			List<LinkedHashMap<C, Triple<C,C,List<C>>>> vals = allVals((Map<C,C>)ed.a, I);
			for (LinkedHashMap<C, Triple<C,C,List<C>>> val : vals) {
			//	List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>> 
			//	lhs_substed = substLhs(ed.lhs, val);
				for (Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>> rhs : ed.rhs) {
					Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>
					rhs_substed = new Pair<>(substLhs0(rhs.first, val), substLhs0(rhs.second, val));

                    if (triggers(ed.lhs, rhs, val, I, gens, dom)) {
                        if (rhs.first.first != null) {
                            Set<List<Triple<C, C, List<C>>>> set = dom.get(rhs.first.first);
                            List<List<C>> toadd = (List<List<C>>) ((Object) rhs_substed.first.second);
                            List<Triple<C, C, List<C>>> toadd2 = toadd.stream().map(x -> new Triple<>(I.type(x).first, I.type(x).second, x)).collect(Collectors.toList());
                            set.add(toadd2); //to trigger, the rhs wasn't already there
                            gens.put(new Pair<>(rhs.first.first, toadd2), new Pair<>(rhs.first.first, toadd2));
                        }
                        if (rhs.second.first != null) {
                            Set<List<Triple<C, C, List<C>>>> set = dom.get(rhs.second.first);
                            List<List<C>> toadd = (List<List<C>>) ((Object) rhs_substed.second.second);
                            List<Triple<C, C, List<C>>> toadd2 = toadd.stream().map(x -> new Triple<>(I.type(x).first, I.type(x).second, x)).collect(Collectors.toList());
                            set.add(toadd2); //to trigger, the rhs wasn't already there
                            gens.put(new Pair<>(rhs.second.first, toadd2), new Pair<>(rhs.second.first, toadd2));
                        }
                        eqs.add(new Pair(massage(rhs_substed.first, I), massage(rhs_substed.second, I)));
//							if (rhs.first.first != null && rhs.second.first != null) {

                        //						}
                        //: equate in gens map
                    } else {
                    }
				}
				
			}
		}
		return new Triple<>(dom, gens, eqs);
	}
	
	private static Object massage(Triple<String, List<List<String>>, List<String>> x, XCtx I) {
		if (x.first == null) {
			return x.third;
		}
		List y = x.second.stream().map(z -> new Triple<>(I.type(z).first, I.type(z).second, z)).collect(Collectors.toList());
		List z = new LinkedList<>();
		z.add(new Pair<>(x.first, y));
		if (x.third != null) {
			z.addAll(x.third);
		}
		return z;
	}
	
	static <C>
	List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>>
	 substLhs(List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>> lhs,
			 LinkedHashMap<C, Triple<C,C,List<C>>> val) {
		return lhs.stream().map(x -> new Pair<>(substLhs0(x.first, val), substLhs0(x.second, val))).collect(Collectors.toList());
	}
	
	private static <C> Triple<String, List<List<String>>, List<String>>
	  substLhs0(Triple<String, List<List<String>>, List<String>> x, LinkedHashMap<C, Triple<C, C, List<C>>> val) {
		List<List<String>> p = null;
		if (x.second != null) {
			p = x.second.stream().map(z -> substLhs1(z, val)).collect(Collectors.toList());
		}
		List<String> q = null;
		if (x.third != null) {
			q = substLhs1(x.third, val);
		}
		
		return new Triple<>(x.first, p, q);
	}	
	
	private static <C> List<String> substLhs1(List<String> x, LinkedHashMap<C, Triple<C, C, List<C>>> val) {
		List ret = new LinkedList<>();
		for (String k : x) {
			Triple<C, C, List<C>> v = val.get(k);
			if (v == null) {
				ret.add(k);
			} else {
				ret.add(v.first);
				ret.addAll(v.third);
			}
		}
		return ret;
	}
	
	private static <C> List<LinkedHashMap<C, Triple<C,C,List<C>>>> allVals(Map<C, C> as, XCtx<C> I) {
		Map<C, List<Triple<C, C, List<C>>>> m = new HashMap<>();
		for (C v : as.keySet()) {
			C t = as.get(v);
			List<Triple<C, C, List<C>>> arrs = new LinkedList<>(I.cat().hom((C)"_1", t));
			m.put(v, arrs);
		}
		return FinSet.homomorphs(m);
	}
	
	
	private static <C> boolean triggers(
            List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>> lhs,
            Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>> rhs,
            @SuppressWarnings("unused") LinkedHashMap<C, Triple<C, C, List<C>>> val, XCtx<C> I, Map<Pair<String, List<Triple<C, C, List<C>>>>, Object> gens, Map<String, Set<List<Triple<C, C, List<C>>>>> dom) {
		
		for (Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>> c : lhs) {
			Triple<String, List<List<String>>, List<String>> lhs1 = c.first;
			Triple<String, List<List<String>>, List<String>> lhs2 = c.second;
			Object o1 = eval(lhs1, I, gens); 
			Object o2 = eval(lhs2, I, gens);
			if (o1 == null || o2 == null) {
				return false;
			}
			if (!o1.equals(o2)) {
				return false;
			}
		}
		Triple<String, List<List<String>>, List<String>> rhs1 = rhs.first;
		Triple<String, List<List<String>>, List<String>> rhs2 = rhs.second;
        return !(containedInDom(rhs1, dom, I) || containedInDom(rhs2, dom, I));
    }
	
	
	
	private static <C> Object eval(Triple<String, List<List<String>>, List<String>> x, XCtx I, Map<Pair<String, List<Triple<C, C, List<C>>>>, Object> gens) {
		Triple<C, C, List<C>> t = null;
		if (x.third != null) {
			t = I.find_fast(new Triple<>(I.type(x.third).first, I.type(x.third).second, x.third));
		}
		if (x.first == null) {
			return t;
		}
		Object k = findIn(gens, x.first, x.second, I);
		if (k == null) {
			return null;
		}
		return new Pair(k, t);
	}
	
	private static <C> Object findIn(Map<Pair<String, List<Triple<C, C, List<C>>>>, Object> gens, String f, List<List<String>> args, XCtx I) {
		for (Pair<String, List<Triple<C, C, List<C>>>> k : gens.keySet()) {
			String f0 = k.first;
			if (!f.equals(f0)) {
				continue;
			}
			List<Triple<C, C, List<C>>> args0 = k.second;
			boolean match = true;
			for (int i = 0; i < args0.size(); i++) {
				Triple<C, C, List<C>> cand1 = args0.get(i);
				List<String> cand2 = args.get(i);
				List cand1x = new LinkedList<>(cand1.third);
				cand1x.add(0, cand1.first);
				if (!I.getKB().equiv(cand1x, cand2)) {
					match = false;
					break;
				}
			}
			if (match) {
				return gens.get(k);
			}
		}
		
		return null;
	}
	
	private static <C> boolean containedInDom(Triple<String, List<List<String>>, List<String>> x, Map<String, Set<List<Triple<C, C, List<C>>>>> dom, XCtx<C> I) {
		if (x.first == null) {
			return false;
		}
		Set<List<Triple<C, C, List<C>>>> s = dom.get(x.first);
		for (List<Triple<C, C, List<C>>> cand : s) {
			boolean found = true;
			for (int i = 0; i < cand.size(); i++) {
				Triple<C, C, List<C>> c1 = cand.get(i);
				List<C> c2 = (List<C>) x.second.get(i);
				List<C> z1 = new LinkedList<>();
				z1.add(c1.first);
				z1.addAll(c1.third);
				List<C> z2 = new LinkedList<>();
				z2.add(c1.first);
				z2.addAll(c2);
				found = I.getKB().equiv(z1, z2);
				if (!found) {
					break;
				}
			}
			if (found) {
				return true;
			}
		}		
		return false;
	}
	
	
	
}

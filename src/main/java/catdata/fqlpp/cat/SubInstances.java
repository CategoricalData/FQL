package catdata.fqlpp.cat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.fqlpp.FUNCTION;
import catdata.fqlpp.cat.FinSet.Fn;

class SubInstances {

	private static final List<Boolean> tf = Arrays.asList(true, false);

	private static <X> List<X> toList(Set<Pair<X, X>> set) {
		List<X> ret = new LinkedList<>();
		for (Pair<X, X> s : set) {
			ret.add(s.first);
		}
		return ret;
	}
	
	//: change subinstances_fast to use maps rather than sets
	private static <O,A> Set<Map<Chc<O,A>, Set<Pair<Object, Object>>>> subInstances_fast0(
			Category<O,A> sig, List<O> list,
			Map<Chc<O,A>, Set<Pair<Object, Object>>> inst) {
		Set<Map<Chc<O,A>, Set<Pair<Object, Object>>>> ret = new HashSet<>();
		if (list.isEmpty()) {
			ret.add(inst);
			return ret;
		}
		List<O> rest = new LinkedList<>(list);
		O n = rest.remove(0);
		List<LinkedHashMap<Object, Boolean>> subsets = FinSet.homomorphs(
				toList(inst.get(Chc.inLeftNC(n))), tf);
		for (LinkedHashMap<Object, Boolean> subset : subsets) {
			Map<Chc<O,A>, Set<Pair<Object, Object>>> j = recDel(sig, n, inst,
					subset);
			if (rest.isEmpty()) {
				ret.add(j);
			} else {
				Set<Map<Chc<O,A>, Set<Pair<Object, Object>>>> h = subInstances_fast0(sig, rest, j);
				ret.addAll(h);
			}
		}

		return ret;
	}
	
	private static <O,A> Map<Chc<O,A>, Set<Pair<Object, Object>>> recDel(Category<O,A> sig,
			O init, Map<Chc<O,A>, Set<Pair<Object, Object>>> inst,
			LinkedHashMap<Object, Boolean> del0) {
		Map<Chc<O,A>, Set<Pair<Object, Object>>> ret = copyMap(inst);
		Map<O, Set<Object>> del = new HashMap<>();
		for (O node : sig.objects()) {
			del.put(node, new HashSet<>());
		}

		for (Entry<Object, Boolean> k : del0.entrySet()) {
			if (!k.getValue()) {
				del.get(init).add(k.getKey());
			}
		}

		while (true) {
			Pair<O, Object> toDel = pick(del);
			if (toDel == null) {
				return ret;
			}
			O n = toDel.first;
			Object kill = toDel.second;
			remove(ret.get(Chc.inLeftNC(n)), kill);
			for (A e : sig.arrowsFrom(n)) {
				remove(ret.get(Chc.inRightNC(e)), kill);
			}
			for (A e : sig.arrowsTo(n)) {
				Set<Object> cleared = clearX(ret.get(Chc.inRightNC(e)), kill);
				del.get(sig.source(e)).addAll(cleared);
			}
		}
	}
	
	private static void remove(Set<Pair<Object, Object>> set, Object o) {
        set.removeIf(objectObjectPair -> objectObjectPair.first.equals(o));
	} 

	private static Set<Object> clearX(Set<Pair<Object, Object>> set, Object o) {
		Iterator<Pair<Object, Object>> it = set.iterator();
		Set<Object> ret = new HashSet<>();
		while (it.hasNext()) {
			Pair<Object, Object> kkk = it.next();
			if (kkk.second.equals(o)) {
				it.remove();
				ret.add(kkk.first);
			}
		}
		return ret;
	}
	
	private static <O> Map<O, Set<Pair<Object, Object>>> copyMap(
			Map<O, Set<Pair<Object, Object>>> inst) {
		Map<O, Set<Pair<Object, Object>>> m = new HashMap<>();
		for (O k : inst.keySet()) {
			m.put(k, new HashSet<>(inst.get(k)));
		}
		return m;
	}
	
	private static <O> Pair<O, Object> pick(Map<O, Set<Object>> del) {
		for (Entry<O, Set<Object>> k : del.entrySet()) {
			Iterator<Object> it = k.getValue().iterator();
			while (it.hasNext()) {
				Pair<O, Object> ret = new Pair<>(k.getKey(), it.next());
				it.remove();
				return ret;
			}
		}
		return null;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static <O,A> List<Functor<O,A,Set,Fn>> subInstances(Functor<O,A,Set,Fn> I) {
		List<Functor<O,A,Set,Fn>> ret = new LinkedList<>();
		for (Map<Chc<O,A>, Set<Pair<Object, Object>>> k : subInstances_fast0(
				I.source, I.source.order(), toData(I))) {
			try {
				ret.add(new Functor<>(I.source, FinSet.FinSet, o -> down1(k.get(Chc.inLeftNC(o))), aa -> new Fn<>(down1(k.get(Chc.inLeftNC(I.source.source(aa)))), down1(k.get(Chc.inLeftNC(I.source.target(aa)))), down2(k.get(Chc.inRightNC(aa))))));
			} catch (Exception e) {
			}
		}
		return ret;
	}
	
	@SuppressWarnings({ "rawtypes"  })
	private static <O,A> Map<Chc<O,A>, Set<Pair<Object, Object>>> toData(Functor<O,A,Set,Fn> I) {
		Map<Chc<O, A>, Set<Pair<Object, Object>>> ret = new HashMap<>();
		for (O o : I.source.objects()) {
			ret.put(Chc.inLeftNC(o), up1(I.applyO(o)));
		}
		for (A a : I.source.arrows()) {
			ret.put(Chc.inRightNC(a), up2(I.applyA(a)));
		}
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Set<Pair<Object, Object>> up2(Fn f) {
		Set<Pair<Object, Object>> ret = new HashSet<>();
		for (Object o : f.source) {
			ret.add(new Pair<>(o, f.apply(o)));
		}
		return ret;
	}

	@SuppressWarnings({ "rawtypes"})
	private static Set<Pair<Object, Object>> up1(Set s) {
		Set<Pair<Object, Object>> ret = new HashSet<>();
		for (Object o : s) {
			ret.add(new Pair<>(o,o));
		}
		return ret;
	}

	private static FUNCTION<Object, Object> down2(Set<Pair<Object, Object>> set) {
		return x -> {
			for (Pair<Object, Object> p : set) {
				if (p.first.equals(x)) {
					return p.second;
				}
			}
			throw new RuntimeException("Report this error to Ryan.  Could not find " + x + " in " + set);
		};
	}

	private static Set<Object> down1(Set<Pair<Object, Object>> set) {
		return set.stream().map(x -> x.first).collect(Collectors.toSet());
	}
	
}

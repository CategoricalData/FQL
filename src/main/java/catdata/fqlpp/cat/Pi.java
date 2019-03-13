package catdata.fqlpp.cat;


import java.util.*;

import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.fqlpp.FUNCTION;
import catdata.fqlpp.cat.FinSet.Fn;
import catdata.ide.DefunctGlobalOptions;

@SuppressWarnings({ "rawtypes", "unchecked" })
class Pi {
	
	private static  Set squish(Set<Map> r) {
		Set ret = new HashSet();
		for (Map x : r) {
			ret.add(x.get(0));
		}
		return ret;
	}
	
	
	private static <ObjC, ObjD, ArrowD> Set<Map> subset2(
			Category<ObjD, ArrowD> cat,
			ArrowD e,
			Triple<ObjD, ObjC, ArrowD>[] q2cols,
			Triple<ObjD, ObjC, ArrowD>[] q1cols, Set<Map> raw) {
	
	a: for (int i = 0; i < q2cols.length; i++) {
		for (int j = 0; j < q1cols.length; j++) {
			Triple<ObjD, ObjC, ArrowD> q2c = q2cols[i];
			Triple<ObjD, ObjC, ArrowD> q1c = q1cols[j];
			if (q1c.third.equals(cat.compose(e, q2c.third)) && q2c.second.equals(q1c.second)) {
				raw = select(raw, i + 1, j + 2 + q2cols.length);
				continue a;
			}
		}
		throw new RuntimeException("No col " + q2cols[i] + " in " + Arrays.toString(q1cols));
	}
	return raw;
	}

	public static <O1, A1, O2, A2> Transform<O2,A2,Set,Fn> pi(Functor<O1,A1,O2,A2> F, Transform<O1,A1,Set,Fn> h){
		Category<O2,A2> D = F.target;
		Category<O1,A1> C = F.source;
		
		Map<O2, Map> ret = new HashMap<>();

		Triple<Functor<O2,A2,Set,Fn>,Map<O2,Set<Map>>,Map<O2, Triple<O2,O1,A2>[]>> k1 = pi(F, h.source);
		Triple<Functor<O2,A2,Set,Fn>,Map<O2,Set<Map>>,Map<O2, Triple<O2,O1,A2>[]>> k2 = pi(F, h.target);
		
		for (O2 d0 : D.objects()) {
			Triple<Category<Triple<O2,O1,A2>,Quad<A2,A1,A2,A2>>,Functor<Triple<O2,O1,A2>,Quad<A2,A1,A2,A2>,O2,A2>,Functor<Triple<O2,O1,A2>,Quad<A2,A1,A2,A2>,O1,A1>> B = doComma2(D,C, F, d0);
			Set<Map> src = k1.second.get(d0);
			Set<Map> dst = k2.second.get(d0);

			if (src == null || dst == null) {
				throw new RuntimeException();
			}
			Map<Object, Object> m = new HashMap<>();
			for (Object o : k1.first.applyO(d0)) {
				Map row = lookup(o, src);
				int i = 1;
				Map outX = new HashMap();
				for (Triple<O2, O1, A2> k : B.first.objects()) {
					Object out = h.apply(k.second).apply(row.get(i));
					outX.put(i, out);
					i++;
				}
				Object outY = lookup2(outX, dst);
				m.put(o, outY);
			}
			ret.put(d0, m);
		}
		FUNCTION<O2, Fn> t = d0 -> new Fn<>(k1.first.applyO(d0), k2.first.applyO(d0), ret.get(d0)::get);
		return new Transform<>(k1.first, k2.first, t);
	}
	
	private static Object lookup2(Map x, Set<Map> set) {
		Object ret = null;
		outer: for (Map y : set) {
			for (int i = 1; i < y.size(); i++) {
				if (!x.get(i).equals(y.get(i))) {
					continue outer;
				}
			}
			if (ret != null) {
				throw new RuntimeException(); 
			}
			ret = y.get(0);
		}
		if (ret == null) {
			throw new RuntimeException();
		}
		return ret;
	}
	
	private static Map lookup(Object x, Set<Map> set) {
		for (Map y : set) {
			if (y.get(0).equals(x)) {
				return y;
			}
		}
		throw new RuntimeException("Cannot find " + x + " in " + set);
	}
	
	public static <O1,A1,O2,A2> Triple<Functor<O2,A2,Set,Fn>, Map<O2, Set<Map>>,Map<O2, Triple<O2,O1,A2>[]>> 
	pi(Functor<O1,A1,O2,A2> F, Functor<O1,A1,Set,Fn> inst) {
		Category<O2,A2> D = F.target;
		Category<O1,A1> C = F.source;

		Map<O2, Set> ret1 = new HashMap<>();
		Map<A2, Map> ret2 = new HashMap<>();

		Map<O2, Triple<O2,O1,A2>[]> nodecats = new HashMap<>();
		Map<O2, Set<Map>> nodetables = new HashMap<>();
		for (O2 d0 : D.objects()) {
			Triple<Category<Triple<O2,O1,A2>,Quad<A2,A1,A2,A2>>,Functor<Triple<O2,O1,A2>,Quad<A2,A1,A2,A2>,O2,A2>,Functor<Triple<O2,O1,A2>,Quad<A2,A1,A2,A2>,O1,A1>> B = doComma2(D,C, F, d0);
			Set<Map> r = lim2(Functor.compose(B.third, inst));
			if (r != null) {
				ret1.put(d0, squish(r));
				nodetables.put(d0, r);
			}
			nodecats.put(d0, cnames(B.first));
		}
		for (A2 s : D.arrows()) {
			O2 dA = D.source(s);
			
			Set<Map> q1 = nodetables.get(dA);
			Triple<O2, O1, A2>[] cnames1 = nodecats.get(dA);

			O2 dB = D.target(s);
			Set<Map> q2 = nodetables.get(dB); 
			
			Triple<O2, O1, A2>[] cnames2 = nodecats.get(dB);

			Set<Map> raw = product(q2, q1);
			Set<Map> rax = subset2(D, s, cnames2, cnames1, raw);
			Map<Object, Object> ray = project(rax, cnames2.length + 1, 0);

			ret2.put(s, ray);
		}

		Functor<O2,A2,Set,Fn> ret = new Functor<>(F.target, FinSet.FinSet, ret1::get, a -> new Fn<>(ret1.get(F.target.source(a)), ret1.get(F.target.target(a)), ret2.get(a)::get));
		Triple xxx = new Triple<>(ret, nodetables, nodecats);
		
		return xxx;
	}
	
	private static  Map project(Set<Map> x, int i, int j) {
		Map ret = new HashMap();
		for (Map s : x) {
			ret.put(s.get(i), s.get(j));
		}
		return ret;
	}

	
	private static <OD,AD,OC,AC> 
	Triple<Category<Triple<OD, OC, AD>, Quad<AD, AC, AD, AD>>,
	      Functor<Triple<OD, OC, AD>, Quad<AD, AC, AD, AD>, OD, AD>,
	      Functor<Triple<OD, OC, AD>, Quad<AD, AC, AD, AD>, OC, AC>>
	doComma2(Category<OD, AD> D, Category<OC, AC> C,Functor<OC, AC, OD, AD> F, OD d0) {

		Category<OD, AD> dx = new FiniteCategory<>(d0, D.identity(d0));
		Functor<OD,AD,OD,AD> d = new Functor<>(dx, D, o -> o, x -> x);
		
		return Comma.comma(d.source, C, D, d, F);
	
	}
	
	
	
	private static Set<Map> product(Set<Map> A, Set<Map> B) {
		Set<Map> ret = new HashSet<>();
		for (Map a : A) {
			for (Map b : B) {
				Map c =  new HashMap();
				for (int i = 0; i < a.size(); i++) {
					c.put(i, a.get(i));
				}
				int j = a.size();
				for (int k = 0; k < b.size(); k++, j++) {
					c.put(j, b.get(k));
				}
				ret.add(c);
			}
		}
		return ret;
	}

	

	private static <A,B,C,Y> Triple<A,B,C>[] cnames(Category<Triple<A,B,C>,Y> cat) {
		int m = cat.objects().size();
		Triple<A,B,C>[] cnames =  new Triple[m];
		int i = 0;
		for (Triple<A,B,C> o : cat.objects()) {
			cnames[i++] = o; 
		}

		return cnames;
	}
	
	private static <OA, AA, OB, AB, AC> Set<Map> lim2
	(Functor<Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>, Set, Fn> I) {
	
		Category<Triple<OA, OB, AC>, Quad<AA, AB, AC, AC>> B = I.source;
		
		if (B.objects().isEmpty()) {
			Set<Map> ret = new HashSet<>();
			Map m = new HashMap();
			m.put(0, Unit.unit);
			ret.add(m);
			return ret;
		}
		
		Triple<OA, OB, AC>[] cnames = cnames(I.source);

		int temp = 0;

		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
		Map<String, Object> from = new HashMap<>(); //store objects of B directly
		LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
		Map<Object, Set<Map<Object, Object>>> state = new HashMap<>();

		for (Triple<OA, OB, AC> n : B.objects()) {
			from.put("t" + temp, n);
			select.put("c" + temp, new Pair<>("t" + temp, "c0"));
			temp++;
			
			state.put(n, shred(I.applyO(n)));			
		}

		for (Quad<AA, AB, AC, AC> e : B.arrows()) {
			if (B.isId(e)) {
				continue;
			}
			from.put("t" + temp, e);

			where.add(new Pair<>(new Pair<>("t" + temp, "c0"), new Pair<>("t"
					+ cnamelkp(cnames, B.source(e)), "c0")));
			where.add(new Pair<>(new Pair<>("t" + temp, "c1"), new Pair<>("t"
					+ cnamelkp(cnames, B.target(e)), "c0")));
			temp++;
			
			state.put(e, shred(I.applyA(e)));
		}

		Flower f = new Flower(select, from, where);
		Set<Map<Object, Object>> uuu = f.eval(state);
		Set<Map> x0 = unshred(B.objects().size(), uuu);
		
		x0 = keygen(x0);
		
		return x0;
	}
	
	private static Set<Map> unshred(int size, Set<Map<Object, Object>> result) {
		Set<Map> ret = new HashSet<>();
		for (Map<Object, Object> row : result) {
			Map out = new HashMap();
			for (int i = 0; i < size; i++) {
				out.put(i, row.get("c" + i));
			}			
			ret.add(out);
		}
		return ret;
	}

	private static Set<Map<Object, Object>> shred(Fn f) {
		Set<Map<Object, Object>> ret = new HashSet<>();
		
		for (Object x : f.source) {
			Map<Object, Object> m = new HashMap<>();
			m.put("c0", x);
			m.put("c1", f.apply(x));
			ret.add(m);
		}
		
		return ret;
	}

	private static Set<Map<Object, Object>> shred(Set set) {
		Set<Map<Object, Object>> ret = new HashSet<>();
		
		for (Object x : set) {
			Map<Object, Object> m = new HashMap<>();
			m.put("c0", x);
			ret.add(m);
		}
		
		return ret;
	}

	
	
	private static <Obj> int cnamelkp(Obj[] cnames, Obj s) {
		for (int i = 0; i < cnames.length; i++) {
			if (s.equals(cnames[i])) {
				return i;
			}
		}
		throw new RuntimeException("Cannot lookup position of " + s + " in "
				+ Arrays.toString(cnames));
	}
	
	private static Set<Map> select(Set<Map> i, int m, int n) {
		Set<Map> ret = new HashSet<>();
		for (Map tuple : i) {
			if (tuple.get(m).equals(tuple.get(n))) {
				ret.add(tuple);
			}
		}
		return ret;
	}
	
	
	
	//adds ID column
	private static Set<Map> keygen(Set<Map> x0) {
		int id = 1;
		Set<Map> ret = new HashSet<>();
		for (Map x : x0) {
			Map y =  new HashMap();
			switch (DefunctGlobalOptions.debug.fqlpp.piLineage) {
				case "Fresh IDs":
					y.put(0, id++);
					break;
				case "Lineage as ID":
					y.put(0, x);
					break;
				default:
					y.put(0, Util.printForPi(x));
					break;
			}
			for (int j = 1; j <= x.size(); j++) {
				y.put(j, x.get(j - 1));
			}
			ret.add(y);
		}
		return ret;
	} 
	
}

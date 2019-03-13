package catdata.fqlpp.cat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Quad;
import catdata.Util;
import catdata.fqlpp.cat.FinSet.Fn;

class LeftKanSigma {
	
	
	@SuppressWarnings({ "rawtypes" , "unchecked"})
	public static <O1,A1,O2,A2>
	Quad<Functor<O2,A2,Set,Fn>, Transform<O1,A1,Set,Fn>, Transform<O2,A2,Set,Fn>,
	Map<Object, List<Pair<A2, Object>>>> 
	fullSigma(Functor<O1,A1,O2,A2> F, Functor<O1,A1,Set,Fn> I, Transform<O1,A1,Set,Fn> t, Functor<O2,A2,Set,Fn> JJJ)  {
		Mapping<O1,A1,O2,A2> G = F.toMapping();

	
		FPTransform<O1,A1> m = null;
		if (t != null) {
			m = t.toFPTransform();
		}
		Instance<O2,A2> J = null;
		if (JJJ != null) {
			J = JJJ.toInstance();
		}
		
		Quad<Instance<O2,A2>, 
	     Map<Signature<O1,A1>.Node, Map<Object, Object>>, 
	     Map<Signature<O2,A2>.Node, Map<Object, Object>>, 
	     Map<Object, List<Pair<Signature<O2,A2>.Edge, Object>>>> q = fullSigmaOnPresentation(G, I.toInstance(), m, J, 100);
	
		Functor<Signature<O2, A2>.Node, Signature<O2, A2>.Path, Set, Fn> f2 = q.first.toFunctor();
		
		Functor<O2, A2, Set, Fn> f3 = new Functor<>(F.target, FinSet.FinSet, x -> f2.applyO(G.target.new Node(x)), a -> {
			
		if (F.target.isId(a)) {
			return Fn.id(f2.applyO(G.target.new Node(F.target.source(a))));
		} 
		return f2.applyA(G.target.path(G.target.getEdge(a)));
		} );
	
		//use third
		Transform<O2,A2,Set,Fn> thr = null; 
		if (t != null) { 
			if (JJJ == null) {
				throw new RuntimeException("Left kan sigma anomaly, please report");
			}
			thr = new Transform<>(f3, JJJ, x -> new Fn(f3.applyO(x), JJJ.applyO(x), q.third.get(G.target.new Node(x))::get));
		}
		 
		Transform<O1,A1,Set,Fn> et = new Transform<>(I, Functor.compose(F,f3), x -> new Fn(I.applyO(x), Functor.compose(F,f3).applyO(x), q.second.get(G.source.new Node(x))::get));
		
		Map<Object, List<Pair<A2, Object>>> nq = new HashMap<>();
		
		for (Entry<Object, List<Pair<Signature<O2, A2>.Edge, Object>>> o : q.fourth.entrySet()) {
			List<Pair<A2, Object>> rt = new LinkedList<>();
			for (int j = 0; j < o.getValue().size(); j++) {
				Pair<Signature<O2, A2>.Edge, Object> fst = o.getValue().get(j);
				if (fst.first == null) {
					rt.add(new Pair<>(null, ((Pair)fst.second).second)); //because no guids, have pairs as first
				} else {
					rt.add(new Pair<>(fst.first.name, fst.second )); //
				}
			}
			nq.put(o.getKey(), rt);
		}
		
		return new Quad<>(f3, et, thr, nq);
	}


	private static <O1,A1,O2,A2>
	Quad<Instance<O2,A2>, 
	     Map<Signature<O1,A1>.Node, Map<Object, Object>>, 
	     Map<Signature<O2,A2>.Node, Map<Object, Object>>, 
	     Map<Object, List<Pair<Signature<O2,A2>.Edge, Object>>>> 
	fullSigmaOnPresentation(
            Mapping<O1, A1, O2, A2> F, Instance<O1, A1> I, FPTransform<O1, A1> t, Instance<O2, A2> JJJ, Integer kkk)  {

		LeftKan<O1,A1,O2,A2> D = new LeftKan<>(kkk, F, I, t, JJJ);

		Pair<Instance<O2,A2>, Map<Object, List<Pair<Signature<O2,A2>.Edge, Object>>>> hhh = sigma(D);
		Map<Signature<O1,A1>.Node, Map<Object, Object>> etables = new HashMap<>();
		for (Entry<Signature<O1, A1>.Node, Set<Pair<Object, Object>>> n : D.ua2.entrySet()) {
			etables.put(n.getKey(), Util.convert(n.getValue()));
		}
		
		Instance<O2,A2> j = hhh.first;
		return new Quad<> (j, etables, D.utables2, hhh.second);
	} 

	private static <O1,A1,O2,A2> Pair<Instance<O2,A2>, Map<Object, List<Pair<Signature<O2,A2>.Edge, Object>>>> sigma(
			LeftKan<O1,A1,O2,A2> lk)  {
		
		if (!lk.compute()) {
			throw new RuntimeException("Too many sigma iterations.");
		}

		Map<Signature<O2,A2>.Node, Set<Object>> nm = new HashMap<>();
		Map<Signature<O2,A2>.Edge, Map<Object, Object>> em = new HashMap<>();

		
		for (Signature<O2,A2>.Node e : lk.Pb2.keySet()) {
			Set<Pair<Object, Object>> t = lk.Pb2.get(e);
			nm.put(e, t.stream().map(x -> x.first).collect(Collectors.toSet()));
		}
		for (Signature<O2,A2>.Edge e : lk.Pg2.keySet()) {
			Set<Pair<Object, Object>> t = lk.Pg2.get(e);
			Map<Object, Object> m = (Util.convert(t));
			em.put(e, m);
		}

		Instance<O2,A2> ret = new Instance<>(nm, em, lk.F.target);

		return new Pair<>(ret, lk.lineage2);
	}

}

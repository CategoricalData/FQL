package catdata.opl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.opl.OplExp.OplInst;
import catdata.opl.OplExp.OplPres;
import catdata.opl.OplExp.OplPresTrans;
import catdata.opl.OplExp.OplPushout;
import catdata.opl.OplQuery.Block;

@SuppressWarnings({"unchecked","rawtypes"})
class OplChase {
	
	static OplInst chaseParallel(OplInst I, List EDs, int limit) {

		OplInst ret = I;
		for (int i = 0; i < limit; i++) {
			boolean changed = false;
				OplInst ret2 = (OplInst) stepParallel(ret, EDs).pushout().first;
				if (ret2 != null) {
					ret = ret2;
					changed = true;
				}
			if (!changed) {
				return ret;
			}
		}
		
		throw new RuntimeException("Limit exceeded, last instance:\n\n" + ret);
		
	}

	static OplInst chase(OplInst I, List<OplQuery> EDs, int limit) {
		OplInst ret = I;
		for (int i = 0; i < limit; i++) {
			boolean changed = false;
			for (OplQuery ed : EDs) {
				
				OplPushout ret2 = step(ret, ed);
				
				if (ret2 != null) {
					ret = (OplInst) ret2.pushout().first;
					changed = true;
				}
			}
			if (!changed) {
				return ret;
			}
		}
		
		throw new RuntimeException("Limit exceeded, last instance:\n\n" + ret);
		
	}
	
	private static <S, C, V, X> OplTerm<Chc<C, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>
	inj(OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> term, OplQuery<S, C, V, String, String, V> Q) {
		if (term.var != null) {
			return new OplTerm<>(term.var);
		}
		List<OplTerm<Chc<C, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>> 
		ret = new LinkedList<>();
		for (OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> arg : term.args) {
			ret.add(inj(arg, Q));
		}
		if (term.head.left) {
			return new OplTerm<>(Chc.inLeft(term.head.l), ret);
		}
		Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V> r = term.head.r;
		Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>> t = new Triple<>(Q, r.first.first, r.first.second);
		Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V> x = new Pair<>(t, r.second); 
		return new OplTerm<>(Chc.inRight(x), ret);
	}
	
	private static <S, C, V, X>
	OplPushout<S, C, V, Pair<Triple<OplQuery<S, C, V, String, String, V>, Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, X, Pair<Triple<OplQuery<S, C, V, String, String, V>, Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>  
	stepParallel(OplInst<S, C, V, X> I, List<OplQuery<S, C, V, String, String, V>> Qs) {

		Map<Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, Integer> 
		Asprec = new HashMap<>(), Esprec = new HashMap<>();
		Map<Pair<Triple<OplQuery<S, C, V, String, String, V>, Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, S> 
		Asgens = new HashMap<>(), Esgens = new HashMap<>();
		
		List<Pair<OplTerm<Chc<C, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>, 
		          OplTerm<Chc<C, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>>> 
		Aseqs = new LinkedList<>(), Eseqs = new LinkedList<>();
		
		Map<S, Map<Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, OplTerm<Chc<C, X>, V>>> 
		AsImap = new HashMap<>();
		
		Map<S, Map<Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, 
		           OplTerm<Chc<C, Pair<Triple<OplQuery<S, C, V, String, String, V>, Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>>> 
		AsEsmap = new HashMap<>();
		
		for (S s : I.S.entities) {
			AsImap.put(s, new HashMap<>());
			AsEsmap.put(s, new HashMap<>());
		}
		
		for (OplQuery<S, C, V, String, String, V> Q : Qs) {
			OplPushout<S,C,V,Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, X, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>  p0 = step(I, Q);
			if (p0 == null) {
				continue;
			}
			for (Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V> gen : p0.h1.src.gens.keySet()) {
				Asgens.put(new Pair<>(new Triple<>(Q, gen.first.first, gen.first.second), gen.second), p0.h1.src.gens.get(gen));
				AsImap.get(p0.h1.src.gens.get(gen)).put(new Pair<>(new Triple<>(Q, gen.first.first, gen.first.second), gen.second), p0.h1.map.get(p0.h1.src.gens.get(gen)).get(gen));
				AsEsmap.get(p0.h1.src.gens.get(gen)).put(new Pair<>(new Triple<>(Q, gen.first.first, gen.first.second), gen.second), inj(p0.h2.map.get(p0.h2.src.gens.get(gen)).get(gen), Q));
				
			}
			for (Pair<OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>, OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>> eq : p0.h1.src.equations) {
				Aseqs.add(new Pair<>(inj(eq.first, Q), inj(eq.second, Q)));
			}
			
			for (Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V> gen : p0.h2.dst.gens.keySet()) {
				Esgens.put(new Pair<>(new Triple<>(Q, gen.first.first, gen.first.second), gen.second), p0.h2.dst.gens.get(gen));

			}
			for (Pair<OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>, OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>> eq : p0.h2.dst.equations) {
				Eseqs.add(new Pair<>(inj(eq.first, Q), inj(eq.second, Q)));
			}
		}
		
		OplPres<S, C, V, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>> 
		A0 = new OplPres<>(Asprec, I.S0 , I.S.sig, Asgens, Aseqs), 
		E0 = new OplPres<>(Esprec, I.S0 , I.S.sig, Esgens, Eseqs);
		A0.toSig(); E0.toSig();
		
		OplInst<S, C, V, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>
		As = new OplInst<>(I.S0, "?", "?"), Es = new OplInst<>(I.S0, "?", "?");
		As.validate(I.S, A0, null); 
		Es.validate(I.S, E0, null);			
		
		OplPresTrans<S, C, V, Pair<Triple<OplQuery<S, C, V, String, String, V>, Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, X> AstoI 
		= new OplPresTrans<>(AsImap, "?", "?", As.P, I.P);
		AstoI.validateNotReally(As, I);
		AstoI.validateNotReally(As.P, I.P);
		AstoI.src1 = As; AstoI.dst1 = I;
		//
		
		OplPresTrans<S, C, V, Pair<Triple<OplQuery<S, C, V, String, String, V>, Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>> 
		AstoEs = new OplPresTrans<>(AsEsmap, "?", "?", As.P, Es.P);	
		AstoEs.validateNotReally(As, Es);
		AstoEs.validateNotReally(As.P, Es.P);
		AstoEs.src1 = As; AstoEs.dst1 = Es; //why must do this manually?
		
		OplPushout<S,C,V,Pair<Triple<OplQuery<S, C, V, String, String, V>, Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, X, Pair<Triple<OplQuery<S, C, V, String, String, V>,Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>  
			p = new OplPushout<>("?", "?");
		
		p.validate(AstoI, AstoEs);
			
		return p;
	}
	
	// this will fail because the schema will not have plain strings as entities, will be of the form Chc<S, String>?
	private static <S, C, V, X>
	OplPushout<S,C,V,Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, X, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>  
	step(OplInst<S, C, V, X> I, OplQuery<S, C, V, String, String, V> Q) {
		if (!Q.blocks.containsKey("EXISTS")) {
			throw new RuntimeException("Need a block called EXISTS");
		}
		if (!Q.blocks.get("EXISTS").first.equals("EXISTS")) {
			throw new RuntimeException("EXISTS block must target EXISTS entity");
		}
		Block<S, C, V, String, String, V> EXISTS = Q.blocks.get("EXISTS").second;
		if (!EXISTS.edges.containsKey("THERE")) {
			throw new RuntimeException("EXISTS block must have FK called THERE");
		}
		if (!EXISTS.edges.get("THERE").first.equals("FORALL")) {
			throw new RuntimeException("THERE FK must target FORALL");
		}
		Map<V, OplTerm<C, V>> THERE = EXISTS.edges.get("THERE").second;

		if (!Q.blocks.containsKey("FORALL")) {
			throw new RuntimeException("Need a block called FORALL");
		}
		if (!Q.blocks.get("FORALL").first.equals("FORALL")) {
			throw new RuntimeException("FORALL block must target FORALL entity");
		}
//		Block<S, C, V, String, String, V> FORALL = Q.blocks.get("FORALL").second;

		Pair<OplInst<String,String,V,Chc<OplTerm<Chc<C,X>,V>,Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>>>,Map<Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>,Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>>>
		temp2 = Q.eval(I);
		OplInst<String,String,V,Chc<OplTerm<Chc<C,X>,V>,Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>>> 
		QI0 = temp2.first;
				
		//temp also contains active domains
		Map<String,Set<Chc<OplTerm<Chc<C,X>,V>,Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>>>> temp = Util.revS(QI0.P.gens);
		for (String s : QI0.S.entities) {
			if (!temp.containsKey(s)) {
				temp.put(s, new HashSet<>());
			}
		}
				
		Set<Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>> QIA = Chc.projIfAllRight(temp.get("FORALL"));
		Set<Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>> QIE = Chc.projIfAllRight(temp.get("EXISTS"));

		Map<Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>, Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>> QIm = new HashMap<>();
		for (Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>> back : QIE) {
			Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>> front = temp2.second.get(back);
			if (front == null) {
				throw new RuntimeException("Report to Ryan, no front for back. front=null, back=" + back + " temp2=" + temp2.second);
			}
			QIm.put(back, front);
		}
		
		Set<Pair<Object,Map<V,OplTerm<Chc<C,X>,V>>>> T = new HashSet<>(QIA);
		T.removeAll(QIm.values());
		
		if (T.isEmpty()) {
			return null;
		}		
		
		Map<Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, Integer> 
		Aprec = new HashMap<>(), Eprec = new HashMap<>();
		Map<Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, S> 
		Agens = new HashMap<>(), Egens = new HashMap<>();
		
		List<Pair<OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>, 
		          OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>>> 
		 Aeqs = new LinkedList<>(), Eeqs = new LinkedList<>();
		
		//
		
		Map<S, Map<Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, OplTerm<Chc<C, X>, V>>> 
		AImap = new HashMap<>();
		
		Map<S, Map<Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, 
		           OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>>> 
		AEmap = new HashMap<>();
		
		for (S s : I.S.entities) {
			AImap.put(s, new HashMap<>());
			AEmap.put(s, new HashMap<>());
		}
		
		//are generators at type being lost? if so must change type of Agens, etc
		//should not use QIm here, should use frozen instance for m
		for (Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>> t : T) {
			for (V v : Q.fI.get("FORALL").gens.keySet()) {
				S s = Q.fI.get("FORALL").gens.get(v);
				Agens.put(new Pair<>(t, v), s);
				AImap.get(s).put(new Pair<>(t, v), t.second.get(v));			
			}
			for (Pair<OplTerm<Chc<C, V>, V>, OplTerm<Chc<C, V>, V>> eq : Q.fI.get("FORALL").equations) {
				OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> 
				 lhs = new Fun2<>(t).apply(eq.first);
				OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> 
				 rhs = new Fun2<>(t).apply(eq.second);
				Aeqs.add(new Pair<>(lhs, rhs));
			}
			
			for (V v : Q.fI.get("EXISTS").gens.keySet()) {
				S s = Q.fI.get("EXISTS").gens.get(v);
				Egens.put(new Pair<>(t, v), s);
			}
			
			for (Pair<OplTerm<Chc<C, V>, V>, OplTerm<Chc<C, V>, V>> eq : Q.fI.get("EXISTS").equations) {
				OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> 
				 lhs = new Fun2<>(t).apply(eq.first);
				OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> 
				 rhs = new Fun2<>(t).apply(eq.second);
				Eeqs.add(new Pair<>(lhs, rhs));
			}
			
			//have trigger in A, need to transform into trigger in E
			
			for (V v : Q.fI.get("FORALL").gens.keySet()) {
				S s = Q.fI.get("FORALL").gens.get(v);

				OplTerm<Chc<C, X>, V> inDst = THERE.get(v).inLeft();
								
		        OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> 
		        	toAdd = new Fun<>(t).apply(inDst);
		        
				AEmap.get(s).put(new Pair<>(t, v), toAdd); 				
			}
		}
		
		//
		
		OplPres<S, C, V, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>> 
		A0 = new OplPres<>(Aprec, I.S0 , Q.src.sig, Agens, Aeqs), 
		E0 = new OplPres<>(Eprec, I.S0 , Q.src.sig, Egens, Eeqs);
		A0.toSig(); E0.toSig();
		
		OplInst<S, C, V, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>
		A = new OplInst<>(I.S0, "?", "?"), E = new OplInst<>(I.S0, "?", "?");
		A.validate(Q.src, A0, null); E.validate(Q.src, E0, null);
				
		OplPresTrans<S, C, V, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, X> AtoI 
		= new OplPresTrans<>(AImap, "?", "?", A.P, I.P);
		AtoI.validateNotReally(A, I);
		AtoI.validateNotReally(A.P, I.P);
		AtoI.src1 = A; AtoI.dst1 = I;
		//
		
		OplPresTrans<S, C, V, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>> 
		AtoE = new OplPresTrans<>(AEmap, "?", "?", A.P, E.P);	
		AtoE.validateNotReally(A, E);
		AtoE.validateNotReally(A.P, E.P);
		AtoE.src1 = A; AtoE.dst1 = E; //why must do this manually?
		
		OplPushout<S,C,V,Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>, X, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>> 
		p = new OplPushout<>("?", "?");
		
		p.validate(AtoI, AtoE);
			
		return p;
	}
	
	static class Fun <C,X,V> implements Function<OplTerm<Chc<C, X>, V>,  OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>> {
	
		final Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>> t;
		
		public Fun(Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>> t) {
			this.t = t;
		}
		
		@Override
		public OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> apply(
				OplTerm<Chc<C, X>, V> term) {
			
			if (term.var != null) {
				Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V> p = new Pair<>(t, term.var);
				return new OplTerm<>(Chc.inRight(p), new LinkedList<>());
			}
			
			List<OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>> ret = new LinkedList<>();
			for (OplTerm<Chc<C, X>, V> arg : term.args) {
				ret.add(apply(arg));
			}
			
			if (term.head.left) {
				C c = term.head.l;
				return new OplTerm<>(Chc.inLeft(c), ret);
			} 
				X x = term.head.r;
				throw new RuntimeException("bad " + x + ", report to Ryan");
			
		}
	}


	static class Fun2 <C,X,V> implements Function<OplTerm<Chc<C, V>, V>,  OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>> {
		
		final Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>> t;
		
		public Fun2(Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>> t) {
			this.t = t;
		}
		
		@Override
		public OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V> apply(
				OplTerm<Chc<C, V>, V> term) {
			
			if (term.var != null) {
				throw new RuntimeException("bad2 " + term.var + ", report to Ryan");
			}
			
			List<OplTerm<Chc<C, Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V>>, V>> ret = new LinkedList<>();
			for (OplTerm<Chc<C, V>, V> arg : term.args) {
				ret.add(apply(arg));
			}
			
			if (term.head.left) {
				C c = term.head.l;
				return new OplTerm<>(Chc.inLeft(c), ret);
			} 
				V x = term.head.r;
				Pair<Pair<Object, Map<V, OplTerm<Chc<C, X>, V>>>, V> p = new Pair<>(t, x);
				return new OplTerm<>(Chc.inRight(p), new LinkedList<>());
			}
		
		
	}
}

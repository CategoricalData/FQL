package catdata.fqlpp.cat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fqlpp.FUNCTION;
import catdata.fqlpp.cat.FinSet.Fn;

public class FDM {
	
	@SuppressWarnings("rawtypes")
	private static final Map<Functor, Functor> deltas = new HashMap<>();
	@SuppressWarnings("rawtypes")
    private static final Map<Functor, Functor> sigmas = new HashMap<>();
	@SuppressWarnings("rawtypes")
    private static final Map<Functor, Functor> pis = new HashMap<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <O1, A1, O2, A2> Functor<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> deltaF(
			Functor<O2, A2, O1, A1> F) {
		if (deltas.containsKey(F)) {
			return deltas.get(F);
		}

		Category<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> src = Inst.get(F.target);
		Category<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> dst = Inst.get(F.source);

		FUNCTION<Functor<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>> o = I -> Functor.compose(F, I);
		FUNCTION<Transform<O1, A1, Set, Fn>, Transform<O2, A2, Set, Fn>> a = f -> new Transform<>(
				o.apply(f.source), o.apply(f.target), d -> new Fn<>(o.apply(f.source).applyO(d), o
						.apply(f.target).applyO(d), i -> f.apply(F.applyO(d)).apply(i)));

		deltas.put(F, new Functor<>(src, dst, o, a));
		return deltas.get(F);
	}

	@SuppressWarnings({ "rawtypes" })
	public static <O1, A1, O2, A2> Functor<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> sigmaF(
			Functor<O1, A1, O2, A2> F) {
		if (sigmas.containsKey(F)) {
			return sigmas.get(F);
		}
		
		Map<Functor<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>> cache = new HashMap<>();

		Category<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> src = Inst.get(F.source);
		Category<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> dst = Inst.get(F.target);

		FUNCTION<Functor<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>> o = I -> {
            Functor<O2, A2, Set, Fn> J = cache.computeIfAbsent(I, k -> LeftKanSigma.fullSigma(F, I, null, null).first);
            return J;
		};
			
		FUNCTION<Transform<O1, A1, Set, Fn>, Transform<O2, A2, Set, Fn>> a = t -> LeftKanSigma
				.fullSigma(F, t.source, Transform.compose(t,
						LeftKanSigma.fullSigma(F, t.target, null, null).second), o.apply(t.target)).third;

		sigmas.put(F, new Functor<>(src, dst, o, a));
		return sigmas.get(F);
	}

	// : lineage assumes all IDs in output of sigma are unique
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <O1, A1, O2, A2> Adjunction<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> sigmaDelta(
			Functor<O2, A2, O1, A1> F) {

		Category<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> D = Inst.get(F.source);
		Category<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> C = Inst.get(F.target);
		FUNCTION<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> g0 = I -> {
			Functor<O2, A2, Set, Fn> deltad = deltaF(F).applyO(I);
			Quad<Functor<O1, A1, Set, Fn>, Transform<O2, A2, Set, Fn>, Transform<O1, A1, Set, Fn>, Map<Object, List<Pair<A1, Object>>>> q = LeftKanSigma
					.fullSigma(F, deltad, null, null);
			Functor<O1, A1, Set, Fn> sigmad = q.first;
			Map<Object, List<Pair<A1, Object>>> lineage = q.fourth;
			return new Transform<>(sigmad, I, n -> {
				FUNCTION<Object, Object> h = i -> {
					List<Pair<A1, Object>> l = lineage.get(i);
					Object ret = l.get(0).second;
					for (int e = 1; e < l.size(); e++) {
						Pair<A1, Object> le = l.get(e);
						A1 edge = le.first;
						ret = I.applyA(edge).apply(ret);
					}
					return ret;
				};

				return new Fn<>(sigmad.applyO(n), I.applyO(n), h);
			});
		};
		Transform<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>, Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> f = new Transform<>(
				Functor.compose(deltaF(F), sigmaF(F)), Functor.identity(C), g0);

		FUNCTION<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> f0 = I -> LeftKanSigma
				.fullSigma(F, I, null, null).second;
		Transform<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>, Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> g = new Transform<>(
				Functor.identity(D), Functor.compose(sigmaF(F), deltaF(F)), f0);

		return new Adjunction<>(sigmaF(F), deltaF(F), f, g);
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static <O1, A1, O2, A2> Functor<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> piF(
			Functor<O1, A1, O2, A2> F) {
		if (pis.containsKey(F)) {
			return pis.get(F);
		}

		Category<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> src = Inst.get(F.source);
		Category<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> dst = Inst.get(F.target);

		FUNCTION<Functor<O1, A1, Set, Fn>, Functor<O2, A2, Set, Fn>> o = I -> Pi.pi(F, I).first;

		FUNCTION<Transform<O1, A1, Set, Fn>, Transform<O2, A2, Set, Fn>> a = t -> Pi.pi(F, t);

		pis.put(F, new Functor<>(src, dst, o, a));
		return pis.get(F);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <O1, A1, O2, A2> Adjunction<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>, Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> deltaPi(
			Functor<O2, A2, O1, A1> F) {
		Category<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> D = Inst.get(F.source);
		Category<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> C = Inst.get(F.target);

		
		FUNCTION<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> f = I -> {
			Triple<Functor<O1,A1,Set,Fn>,Map<O1,Set<Map>>,Map<O1, Triple<O1,O2,A1>[]>> xxx = Pi.pi(F, Functor.compose(F, I));
			FUNCTION<O1, Fn> j = n -> new Fn<>(I.applyO(n), xxx.first.applyO(n), i -> {
                outer: for (Map m : xxx.second.get(n)) {
                    for (int p = 1; p < m.size(); p++) {
                        if (xxx.third.get(n)[p-1].third.equals(F.target.identity(n))) {
                            if (!m.get(p).equals(i)) {
                                continue outer;
                            }
                        }
                    }
                    return m.get(0);
                }
            throw new RuntimeException("Cannot find diagonal of " + i + " in " + xxx.second.get(n));
            });
			return new Transform<>(I, xxx.first, j);
		};
		Transform<Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>, Functor<O1, A1, Set, Fn>, Transform<O1, A1, Set, Fn>> unit 
		 = new Transform<>(Functor.identity(C), Functor.compose(deltaF(F), piF(F)), f);

		FUNCTION<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> g =  I -> {
			Triple<Functor<O1,A1,Set,Fn>,Map<O1,Set<Map>>,Map<O1, Triple<O1,O2,A1>[]>> xxx = Pi.pi(F, I);
			Functor<O2,A2,Set,Fn> deltad = Functor.compose(F, xxx.first);
			FUNCTION<O2, Fn> j = m -> {
				O1 n = F.applyO(m);
				Triple<O1,O2,A1>[] col = xxx.third.get(n);
				Triple<O1,O2,A1> tofind = new Triple<>(n, m, F.target.identity(n));
				Set<Map> lim = xxx.second.get(n);
				int[] i = new int[] { 0 };
				for (Triple<O1,O2,A1> cand : col) {
					if (!cand.equals(tofind)) {
						i[0]++;
						continue;
					}
					FUNCTION h = id -> {
						for (Map row : lim) {
							if (row.get(0).equals(id)) {
								return row.get(i[0]+1);
							}
						}
						throw new RuntimeException("Report this error to Ryan.");
					};
					return new Fn<>(deltad.applyO(m), I.applyO(m), h);
				}
				throw new RuntimeException("Report this error to Ryan.");
			};
			return new Transform<>(deltad, I, j);
		};

		Transform<Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>, Functor<O2, A2, Set, Fn>, Transform<O2, A2, Set, Fn>> counit 
		 = new Transform<>(Functor.compose(piF(F), deltaF(F)), Functor.identity(D), g);

		return new Adjunction<>(deltaF(F), sigmaF(F), counit, unit);
	}

	
}

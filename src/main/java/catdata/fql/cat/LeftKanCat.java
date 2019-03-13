package catdata.fql.cat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.fql.FQLException;
import catdata.fql.Fn;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.fql.decl.Signature;
import catdata.ide.DefunctGlobalOptions;

public class LeftKanCat {	

	private static Mapping subset(Signature a, Signature b) throws FQLException {
		List<Pair<String, String>> obm = new LinkedList<>();
		for (Node n : a.nodes) {
			obm.add(new Pair<>(n.string, n.string));
		}
		return new Mapping(true, a, b, obm,
                new LinkedList<>(),
                new LinkedList<>());
	}

	public static Pair<FinCat<Node, Path>, Fn<Path, Arr<Node, Path>>> toCategory(
			Signature sig) throws FQLException {

		Signature A = sig.onlyObjects();
		Instance X = A.terminal(null);
		Mapping F = subset(A, sig);
		LeftKan lk = new LeftKan(0, F, X);
		if (!lk.compute()) {
			throw new FQLException(
					"Category computation has exceeded allowed iterations.");
		}

		return helper(lk, sig);
	}

	private static Pair<FinCat<Node, Path>, Fn<Path, Arr<Node, Path>>> helper(
			LeftKan lk, Signature B) throws FQLException {
		List<Node> objects = B.nodes;

		Set<Arr<Node, Path>> arrows = new HashSet<>();
		Map<Node, Arr<Node, Path>> identities = new HashMap<>();

		Fn<Path, Integer> fn = makeFn(lk);
		List<Path> paths = new LinkedList<>();
		Map<Integer, Path> fn2 = new HashMap<>();

		int numarrs = numarrs(lk);
		for (Node n : B.nodes) {
			paths.add(new Path(B, n));
		}
		outer: for (int iter = 0; iter < DefunctGlobalOptions.debug.fql.MAX_PATH_LENGTH; iter++) {
			for (Path p : paths) {
				Integer i = fn.of(p);
				fn2.putIfAbsent(i, p);
				if (fn2.size() == numarrs) {
					break outer;
				}
			}
			List<Path> paths0 = new LinkedList<>();
			for (Path p : paths) {
				for (Edge e : B.outEdges(p.target)) {
					paths0.add(Path.append(B, p, new Path(B, e)));
				}
			}
			paths = paths0;
		}

		if (fn2.size() < numarrs) {
			String old_str = "Basis path lengths exceed allowed limit ("
					+ DefunctGlobalOptions.debug.fql.MAX_PATH_LENGTH
					+ ").  Only have "
					+ fn2.size()
					+ " basis paths out of required "
					+ numarrs
					+ "."
					+ "  Probable cause: using parallel or hybrid left-kan algorithm (see options).";
			throw new FQLException(old_str);
		}

		for (Integer i : fn2.keySet()) {
			Path p = fn2.get(i);
			arrows.add(new Arr<>(p, p.source, p.target));
		}

		for (Node n : objects) {
			Arr<Node, Path> a = new Arr<>(fn2.get(getOne(lk.ua.get(n)).second), n, n);
			identities.put(n, a);
			for (Pair<Integer, Integer> i : lk.Pb.get(n)) {
				Path p = fn2.get(i.first);
				arrows.add(new Arr<>(p, p.source, p.target));
			}
		}

		Fn<Path, Arr<Node, Path>> r2 = (Path x) -> {
                    if (fn2.get(fn.of(x)) == null) {
                        throw new RuntimeException("Given path " + x
                                + ", transforms to " + fn.of(x)
                                + ", which is not in " + fn2);
                    }
                    return new Arr<>(fn2.get(fn.of(x)), x.source, x.target);
                };

		Map<Pair<Arr<Node, Path>, Arr<Node, Path>>, Arr<Node, Path>> composition = new HashMap<>();
		for (Arr<Node, Path> x : arrows) {
			for (Arr<Node, Path> y : arrows) {
				if (x.dst.equals(y.src)) {
					composition.put(new Pair<>(x, y),
							r2.of(Path.append(B, x.arr, y.arr)));
				}
			}
		}

		FinCat<Node, Path> r1 = new FinCat<>(objects,
				new LinkedList<>(arrows), composition, identities);
		
		return new Pair<>(r1, r2);
	}

	private static int numarrs(LeftKan lk) {
		int ret = 0;
		
		for (Node k : lk.Pb.keySet()) {
			ret += lk.Pb.get(k).size();
		}
		
		return ret;
	}

	private static <X> X getOne(Set<X> set) {
		if (set.size() != 1) {
			throw new RuntimeException("cannot get one from " + set);
		}
		for (X x : set) {
			return x;
		}
		throw new RuntimeException();
	} 
	
	private static Fn<Path, Integer> makeFn(LeftKan lk) {
		return (Path p) -> {
                    Set<Pair<Object, Integer>> set = Instance.compose(lk.ua.get(p.source), lk.eval(p));
                    return getOne(set).second;
                };
	}

}

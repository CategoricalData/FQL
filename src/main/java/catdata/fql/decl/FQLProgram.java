package catdata.fql.decl;

import java.awt.Color;
import java.awt.Paint;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Ref;
import catdata.LineException;
import catdata.Pair;
import catdata.Prog;
import catdata.Unit;
import catdata.fql.decl.InstExp.Const;
import catdata.fql.decl.InstExp.Delta;
import catdata.fql.decl.InstExp.Eval;
import catdata.fql.decl.InstExp.Exp;
import catdata.fql.decl.InstExp.External;
import catdata.fql.decl.InstExp.FullEval;
import catdata.fql.decl.InstExp.FullSigma;
import catdata.fql.decl.InstExp.InstExpVisitor;
import catdata.fql.decl.InstExp.Kernel;
import catdata.fql.decl.InstExp.One;
import catdata.fql.decl.InstExp.Pi;
import catdata.fql.decl.InstExp.Plus;
import catdata.fql.decl.InstExp.Relationalize;
import catdata.fql.decl.InstExp.Sigma;
import catdata.fql.decl.InstExp.Step;
import catdata.fql.decl.InstExp.Times;
import catdata.fql.decl.InstExp.Two;
import catdata.fql.decl.InstExp.Zero;
import catdata.fql.decl.SigExp.Var;
import catdata.fql.decl.Type.Int;
import catdata.fql.decl.Type.Varchar;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

public class FQLProgram implements Prog {
		
	@Override
	public Integer getLine(String s) {
		return lines.get(s);
	}
	
	public final Map<String, Color> nmap = new HashMap<>();
	private final Map<SigExp, Color> smap = new HashMap<>();
	
	public Color smap(SigExp s) {
		if (smap.containsKey(s)) {
			return smap.get(s); 
		}
		Color c = nColor();
		smap.put(s, c);
		return c;
	}
	
	public Graph<String, Object> build;
	public Graph<String, Object> build2;
	public void doColors() {
		for (String k : sigs.keySet()) {
			Color c = nColor();
			nmap.put(k, c);
			smap.put(new Var(k), c);
			smap.put(sigs.get(k), c);
			smap.put(sigs.get(k).toConst(this), c);
		}
		for (String k : insts.keySet()) {
			nmap.put(k, nColor());
		}
		build = build();
		build2 = build2();
	}
	
	private int cindex = 0;
	private static final Color[] colors_arr = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.yellow, Color.CYAN, Color.GRAY, Color.ORANGE, Color.PINK, Color.BLACK, Color.white};
	private Color nColor() {
		if (cindex < colors_arr.length) {
			return colors_arr[cindex++];
		} 
			cindex = 0;
			return nColor();
		
	}
	
	public Map<String, Paint> colorMap2 = new HashMap<>();
	public Map<String, Paint> colorMap3 = new HashMap<>();

	private Graph<String, Object> build2() {
		Graph<String, Object> g2 = new DirectedSparseMultigraph<>();

		for (String k : maps.keySet()) {
			MapExp.Const i = maps.get(k).toConst(this);
			SigExp src = i.src;
			SigExp dst = i.dst;
			String src_k = revLookup(sigs, src);
			String dst_k = revLookup(sigs, dst);
			if (src_k == null || dst_k == null) {
				continue;
			}
			g2.addEdge(k, src_k, dst_k);
		}
		
		return g2;
	}
	
	public static <K,V> K revLookup(Map<K,V> map, V v) {
		for (K k : map.keySet()) {
			V v0 = map.get(k);
			if (v.equals(v0)) {
				return k;
			}
		}
		return null;
	}
	
	private Graph<String, Object> build() {
		// Graph<V, E> where V is the type of the vertices

		Graph<String, Object> g2 = new DirectedSparseMultigraph<>();
		Ref<Integer> guid = new Ref<>(0);

		for (String k : insts.keySet()) {
			InstExp i = insts.get(k);
			i.type(this).toConst(this);
			g2.addVertex(k);

			i.accept(Unit.unit, new InstExpVisitor<Unit, Unit>() {
				@Override
				public Unit visit(Unit env, Zero e) {
					return null;
				}

				@Override
				public Unit visit(Unit env, One e) {
					return null;
				}

				@Override
				public Unit visit(Unit env, Two e) {
					return null;
				}

				@Override
				public Unit visit(Unit env, Plus e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.a, k);
					g2.addEdge(new Pair<>(pp(guid), e), e.b, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, Times e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.a, k);
					g2.addEdge(new Pair<>(pp(guid), e), e.b, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, Exp e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.a, k);
					g2.addEdge(new Pair<>(pp(guid), e), e.b, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, Const e) {
					return null;
				}

				@Override
				public Unit visit(Unit env, Delta e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.I, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, Sigma e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.I, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, Pi e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.I, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, FullSigma e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.I, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, Relationalize e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.I, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, External e) {
					return null;
				}

				@Override
				public Unit visit(Unit env, Eval e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.e, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, FullEval e) {
					g2.addEdge(new Pair<>(pp(guid), e), e.e, k);
					return null;
				}

				@Override
				public Unit visit(Unit env, Kernel e) {
					TransExp t = transforms.get(e.trans);
					Pair<String, String> p = t.type(FQLProgram.this);
					g2.addEdge(new Pair<>(pp(guid), e), p.first, k);
					g2.addEdge(new Pair<>(pp(guid), e), p.second, k);
					return null;
				}

				private Integer pp(Ref<Integer> ref) {
					int ret = ref.x;
					ref.x = ret + 1;
					return ref.x;
				}

				@Override
				public Unit visit(Unit env, Step e) {
					//  (Step) this should add an edge
					return null;
				}
			});
		}

		return g2;
	}

	
	



	public FQLProgram(LinkedHashMap<String, Type> enums,
			LinkedHashMap<String, SigExp> sigs,
			LinkedHashMap<String, MapExp> maps,
			LinkedHashMap<String, InstExp> insts,
			LinkedHashMap<String, FullQueryExp> full_queries,
			LinkedHashMap<String, QueryExp> queries,
			LinkedHashMap<String, TransExp> transforms,
			LinkedHashMap<String, Integer> lines, List<String> drop,
			List<String> order) {
        this.enums = enums;
		this.sigs = sigs;
		this.maps = maps;
		this.insts = insts;
		this.full_queries = full_queries;
		this.queries = queries;
		this.transforms = transforms;
		this.lines = lines;
		this.drop = drop;
		this.order = order;
	}

	public FQLProgram() {
	}

	public static class NewDecl {
		List<String> drop;
		TransExp trans;
		final String name;
		SigExp sig;
		MapExp map;
		InstExp inst;
		final Integer line;
		QueryExp query;
		FullQueryExp full_query;
		List<String> enums;

		// Pair<SigExp, SigExp> map_t;
		// SigExp inst_t;

		public static NewDecl typeDecl(String name, List<String> values,
				Integer line) {
			NewDecl ret = new NewDecl(name, line);
			ret.enums = values;
			return ret;
		}

		public static NewDecl fullQuery(String name, FullQueryExp full_query,
				Integer line) {
			NewDecl ret = new NewDecl(name, line);
			ret.full_query = full_query;
			return ret;
		}

		public static NewDecl transDecl(String name, Integer line,
				TransExp trans) {
			NewDecl ret = new NewDecl(name, line);
			ret.trans = trans;
			return ret;
		}

		public static NewDecl dropDecl(List<String> drop) {
			NewDecl ret = new NewDecl(null, null);
			ret.drop = drop;
			return ret;
		}

		public static NewDecl queryDecl(String name, Integer line,
				QueryExp query) {
			NewDecl ret = new NewDecl(name, line);
			ret.query = query;
			return ret;
		}

		public static NewDecl sigDecl(String name, Integer line, SigExp sig) {
			NewDecl ret = new NewDecl(name, line);
			ret.sig = sig;
			return ret;
		}

		public static NewDecl mapDecl(String name, Integer line, MapExp map
		/* , Pair<SigExp, SigExp> map_t */) {
			NewDecl ret = new NewDecl(name, line);
			ret.map = map;
			// ret.map_t = map_t;
			return ret;
		}

		public static NewDecl instDecl(String name, Integer line, InstExp inst) {
			NewDecl ret = new NewDecl(name, line);
			ret.inst = inst;
			return ret;
		}

		public NewDecl(String name, Integer line) {
			this.name = name;
			this.line = line;
		}
	}

	public LinkedHashMap<String, Type> enums = new LinkedHashMap<>();
	public LinkedHashMap<String, SigExp> sigs = new LinkedHashMap<>();
	public LinkedHashMap<String, MapExp> maps = new LinkedHashMap<>();
	public LinkedHashMap<String, InstExp> insts = new LinkedHashMap<>();
	public LinkedHashMap<String, FullQueryExp> full_queries = new LinkedHashMap<>();
	public LinkedHashMap<String, QueryExp> queries = new LinkedHashMap<>();
	public LinkedHashMap<String, TransExp> transforms = new LinkedHashMap<>();
 
	public List<String> drop = new LinkedList<>();
	public List<String> order = new LinkedList<>();
	public LinkedHashMap<String, Integer> lines = new LinkedHashMap<>();

	public FQLProgram(List<NewDecl> decls) {
		Set<String> seen = new HashSet<>();
		for (NewDecl decl : decls) {
			if (decl.name != null && decl.enums == null) {
				order.add(decl.name); // drops are unnamed, ignore enums
			}
			if (decl.enums != null) {
				checkDup(seen, decl.name, "enum");
				enums.put(decl.name, new Type.Enum(decl.name,
						new HashSet<>(decl.enums)));
				lines.put(decl.name, decl.line);
			} else if (decl.sig != null) {
				checkDup(seen, decl.name, "signature");
				sigs.put(decl.name, decl.sig);
				lines.put(decl.name, decl.line);
			} else if (decl.inst != null) {
				checkDup(seen, decl.name, "instance");
				insts.put(decl.name, decl.inst);
				lines.put(decl.name, decl.line);
			} else if (decl.map != null) {
				checkDup(seen, decl.name, "mapping");
				maps.put(decl.name, decl.map);
				lines.put(decl.name, decl.line);
			} else if (decl.query != null) {
				checkDup(seen, decl.name, "query");
				queries.put(decl.name, decl.query);
				lines.put(decl.name, decl.line);
			} else if (decl.drop != null) {
				drop.addAll(decl.drop);
			} else if (decl.trans != null) {
				checkDup(seen, decl.name, "transform");
				transforms.put(decl.name, decl.trans);
				lines.put(decl.name, decl.line);
			} else if (decl.full_query != null) {
				checkDup(seen, decl.name, "full_query");
				full_queries.put(decl.name, decl.full_query);
				lines.put(decl.name, decl.line);
			}
			if (decl.name != null) {
				seen.add(decl.name.toUpperCase());
			}
			// else {
			// throw new RuntimeException();
			// }
		}
		enums.put("int", new Int());
		enums.put("string", new Varchar());
		enums.put("float", new Type.Float());
	}

	@Override
	public String toString() {
		return "FQLProgram [enums=" + enums + ", sigs=" + sigs + ", maps="
				+ maps + ", insts=" + insts + ", full_queries=" + full_queries
				+ ", queries=" + queries + ", transforms=" + transforms
				+ ", lines=" + lines + ", drop=" + drop + ", order=" + order
				+ "]";
	}


	private static void checkDup(Set<String> seen, String name, String s)
			throws LineException {
		if (seen.contains(name.toUpperCase())) {
			// throw new LineException("Duplicate " + s + " " +, name, s);
			throw new RuntimeException("Duplicate name: " + s + " " + name);
		}

	}

	@Override
	public Collection<String> keySet() {
		return order;
	}

}

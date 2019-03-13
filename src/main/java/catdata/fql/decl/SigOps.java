package catdata.fql.decl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.fql.Fn;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.cat.Arr;
import catdata.fql.cat.FinCat;
import catdata.fql.cat.Inst;
import catdata.fql.decl.MapExp.Apply;
import catdata.fql.decl.MapExp.Case;
import catdata.fql.decl.MapExp.Comp;
import catdata.fql.decl.MapExp.Const;
import catdata.fql.decl.MapExp.Curry;
import catdata.fql.decl.MapExp.Dist1;
import catdata.fql.decl.MapExp.Dist2;
import catdata.fql.decl.MapExp.FF;
import catdata.fql.decl.MapExp.Fst;
import catdata.fql.decl.MapExp.Id;
import catdata.fql.decl.MapExp.Inl;
import catdata.fql.decl.MapExp.Inr;
import catdata.fql.decl.MapExp.Iso;
import catdata.fql.decl.MapExp.MapExpVisitor;
import catdata.fql.decl.MapExp.Prod;
import catdata.fql.decl.MapExp.Snd;
import catdata.fql.decl.MapExp.Sub;
import catdata.fql.decl.MapExp.TT;
import catdata.fql.decl.SigExp.Exp;
import catdata.fql.decl.SigExp.One;
import catdata.fql.decl.SigExp.Opposite;
import catdata.fql.decl.SigExp.Plus;
import catdata.fql.decl.SigExp.SigExpVisitor;
import catdata.fql.decl.SigExp.Times;
import catdata.fql.decl.SigExp.Union;
import catdata.fql.decl.SigExp.Unknown;
import catdata.fql.decl.SigExp.Var;
import catdata.fql.decl.SigExp.Zero;

public class SigOps implements SigExpVisitor<SigExp.Const, FQLProgram>,
		MapExpVisitor<Const, FQLProgram> {

	private static FinCat<Mapping, Map<Node, Path>> exp(@SuppressWarnings("unused") FQLProgram env,
                                                        Signature base, Signature exp) {
		try {
			return Inst.stuff(base, exp);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}

	private static Pair<SigExp.Const, Fn<SigExp.Const, Const>> one(
            Set<String> at) {
		List<String> nodes = new LinkedList<>();
		nodes.add("node0");

		List<Triple<String, String, String>> attrs = new LinkedList<>();

		for (String k : at) {
			attrs.add(new Triple<>(k + "_attr", "node0", k));
		}

		List<Triple<String, String, String>> arrows = new LinkedList<>();

		SigExp.Const sig = new SigExp.Const(nodes, attrs, arrows,
				new LinkedList<>());

		Fn<SigExp.Const, Const> fn = (SigExp.Const src) -> {
                    List<Pair<String, String>> nm = new LinkedList<>();
                    for (String k : src.nodes) {
                        nm.add(new Pair<>(k, "node0"));
                    }
                    
                    List<Pair<String, String>> am = new LinkedList<>();
                    for (Triple<String, String, String> k : src.attrs) {
                        if (!at.contains(k.third)) {
                            throw new RuntimeException("Enum/type not found: "
                                    + k.third);
                        }
                        am.add(new Pair<>(k.first, k.third + "_attr"));
                    }
                    
                    List<Pair<String, List<String>>> em = new LinkedList<>();
                    for (Triple<String, String, String> k : src.arrows) {
                        List<String> l = new LinkedList<>();
                        l.add("node0");
                        em.add(new Pair<>(k.first, l));
                    }
                    return new Const(nm, am, em, src, sig);
                };

		return new Pair<>(sig, fn);
	}

	private static Pair<SigExp.Const, Fn<SigExp.Const, Const>> zero() {
		SigExp.Const sig = new SigExp.Const(new LinkedList<>(),
				new LinkedList<>(),
				new LinkedList<>(),
				new LinkedList<>());

		Fn<SigExp.Const, Const> fn = (SigExp.Const x) -> new Const(new LinkedList<>(),
                        new LinkedList<>(),
                        new LinkedList<>(), sig, x);

		return new Pair<>(sig, fn);
	}

	private static Pair<Quad<SigExp.Const, Const, Const, Fn<Triple<SigExp.Const, Const, Const>, Const>>, Quad<Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>>> prod(
            SigExp.Const a, SigExp.Const b) {
		int node_count = 0;
		Map<Pair<String, String>, String> node_map = new LinkedHashMap<>();
		List<Pair<String, String>> a_objs = new LinkedList<>(); // fst
		List<Pair<String, String>> b_objs = new LinkedList<>(); // snd
		for (String n : a.nodes) {
			for (String m : b.nodes) {
				node_map.put(new Pair<>(n, m), "node" + node_count);
				a_objs.add(new Pair<>("node" + node_count, n));
				b_objs.add(new Pair<>("node" + node_count, m));
				node_count++;
			}
		}
		List<String> nodes = new LinkedList<>();
		nodes.addAll(node_map.values());

		int attr_count = 0;
		Map<Pair<String, String>, String> attr_map = new LinkedHashMap<>();
		List<Pair<String, String>> a_attrs = new LinkedList<>(); // fst
		List<Pair<String, String>> b_attrs = new LinkedList<>(); // snd
		List<Triple<String, String, String>> attrs = new LinkedList<>();
		for (Triple<String, String, String> n : a.attrs) {
			for (Triple<String, String, String> m : b.attrs) {
				if (!n.third.equals(m.third)) {
					continue;
				}
				String k = node_map.get(new Pair<>(n.second, m.second));
				attrs.add(new Triple<>("attr" + attr_count, k, n.third));
				attr_map.put(new Pair<>(n.first, m.first), "attr" + attr_count);
				a_attrs.add(new Pair<>("attr" + attr_count, n.first));
				b_attrs.add(new Pair<>("attr" + attr_count, m.first));
				attr_count++;
			}
		}

		int edge_count = 0;
		Map<Pair<String, String>, String> edge_map_1 = new LinkedHashMap<>();
		Map<Pair<String, String>, String> edge_map_2 = new LinkedHashMap<>();
		List<Pair<String, List<String>>> a_edges = new LinkedList<>(); // fst
		List<Pair<String, List<String>>> b_edges = new LinkedList<>(); // snd
		List<Triple<String, String, String>> edges = new LinkedList<>();
		for (Triple<String, String, String> n : a.arrows) {
			for (String m : b.nodes) {
				String k1 = node_map.get(new Pair<>(n.second, m));
				String k2 = node_map.get(new Pair<>(n.third, m));

				edges.add(new Triple<>("edge" + edge_count, k1, k2));
				edge_map_1.put(new Pair<>(n.first, m), "edge" + edge_count);

				List<String> al = new LinkedList<>();
				al.add(n.second);
				al.add(n.first);
				a_edges.add(new Pair<>("edge" + edge_count, al));

				List<String> bl = new LinkedList<>();
				bl.add(m);
				b_edges.add(new Pair<>("edge" + edge_count, bl));

				edge_count++;
			}
		}
		for (Triple<String, String, String> n : b.arrows) {
			for (String m : a.nodes) {
				String k1 = node_map.get(new Pair<>(m, n.second));
				String k2 = node_map.get(new Pair<>(m, n.third));

				edges.add(new Triple<>("edge" + edge_count, k1, k2));
				edge_map_2.put(new Pair<>(n.first, m), "edge" + edge_count);

				List<String> al = new LinkedList<>();
				al.add(n.second);
				al.add(n.first);
				b_edges.add(new Pair<>("edge" + edge_count, al));

				List<String> bl = new LinkedList<>();
				bl.add(m);
				a_edges.add(new Pair<>("edge" + edge_count, bl));

				edge_count++;
			}
		}

		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
		for (Triple<String, String, String> n : a.arrows) {
			for (Triple<String, String, String> m : b.arrows) {
				List<String> lhs = new LinkedList<>();
				List<String> rhs = new LinkedList<>();

				String src = node_map.get(new Pair<>(n.second, m.second));

				String lhs1 = edge_map_1.get(new Pair<>(n.first, m.second));
				String lhs2 = edge_map_2.get(new Pair<>(m.first, n.third));
				lhs.add(src);
				lhs.add(lhs1);
				lhs.add(lhs2);

				String rhs1 = edge_map_2.get(new Pair<>(m.first, n.second));
				String rhs2 = edge_map_1.get(new Pair<>(n.first, m.third));
				rhs.add(src);
				rhs.add(rhs1);
				rhs.add(rhs2);

				eqs.add(new Pair<>(lhs, rhs));
			}
		}
		for (Pair<List<String>, List<String>> eqA : a.eqs) {
			for (String srcB : b.nodes) {
				List<String> lhsA = new LinkedList<>(eqA.first);
				List<String> rhsA = new LinkedList<>(eqA.second);
				String srcA = lhsA.remove(0);
				rhsA.remove(0);

				String src = node_map.get(new Pair<>(srcA, srcB));

				List<String> lhs = new LinkedList<>();
				List<String> rhs = new LinkedList<>();
				lhs.add(src);
				rhs.add(src);

				for (String k : lhsA) {
					lhs.add(edge_map_1.get(new Pair<>(k, srcB)));
				}
				for (String k : rhsA) {
					rhs.add(edge_map_1.get(new Pair<>(k, srcB)));
				}
				eqs.add(new Pair<>(lhs, rhs));
			}
		}
		for (Pair<List<String>, List<String>> eqA : b.eqs) {
			for (String srcB : a.nodes) {
				List<String> lhsA = new LinkedList<>(eqA.first);
				List<String> rhsA = new LinkedList<>(eqA.second);
				String srcA = lhsA.remove(0);
				rhsA.remove(0);

				String src = node_map.get(new Pair<>(srcB, srcA));

				List<String> lhs = new LinkedList<>();
				List<String> rhs = new LinkedList<>();
				lhs.add(src);
				rhs.add(src);

				for (String k : lhsA) {
					lhs.add(edge_map_2.get(new Pair<>(k, srcB)));
				}
				for (String k : rhsA) {
					rhs.add(edge_map_2.get(new Pair<>(k, srcB)));
				}
				eqs.add(new Pair<>(lhs, rhs));
			}
		}

		SigExp.Const sig = new SigExp.Const(nodes, attrs, edges, eqs);
		Const fst = new Const(a_objs, a_attrs, a_edges, sig, a);
		Const snd = new Const(b_objs, b_attrs, b_edges, sig, b);

		Fn<Triple<SigExp.Const, Const, Const>, Const> pair = x -> {
            SigExp.Const c = x.first;
            Const f = x.second;
            Const g = x.third;

            if (!f.src.equals(g.src)) {
                throw new RuntimeException("Sources don't agree: " + f.src
                        + " and " + g.src);
            }
            if (!f.dst.equals(a)) {
                throw new RuntimeException("Target of " + f + " is not "
                        + a);
            }
            if (!g.dst.equals(b)) {
                throw new RuntimeException("Target of " + g + "is not " + b);
            }

            List<Pair<String, String>> objs = new LinkedList<>();
            for (String obj_c : c.nodes) {
                objs.add(new Pair<>(obj_c, node_map.get(new Pair<>(lookup(
                        obj_c, f.objs), lookup(obj_c, g.objs)))));
            }

            List<Pair<String, String>> attrs1 = new LinkedList<>();
            for (Triple<String, String, String> attr_c : c.attrs) {
                attrs1.add(new Pair<>(attr_c.first, attr_map.get(new Pair<>(
                        lookup(attr_c.first, f.attrs), lookup(attr_c.first,
                                g.attrs)))));
            }

            List<Pair<String, List<String>>> arrows = new LinkedList<>();
            for (Triple<String, String, String> edge_c : c.arrows) {
                List<String> fc = lookup(edge_c.first, f.arrows);
                List<String> gc = lookup(edge_c.first, g.arrows);
                List<String> ret = new LinkedList<>();
                String fcN = fc.get(0);
                String gcN = gc.get(0);
                String node_start = node_map.get(new Pair<>(fcN, gcN));
                ret.add(node_start);
                for (int i = 1; i < fc.size(); i++) {
                    String fcE = fc.get(i);
                    Pair<String, String> p = new Pair<>(fcE, gcN);
                    String v = edge_map_1.get(p);
                    ret.add(v);
                }
                node_start = lookup(edge_c.third, f.objs);

                    for (int i = 1; i < gc.size(); i++) {
                    String gcE = gc.get(i);
                    Pair<String, String> p = new Pair<>(gcE, node_start);
                    String v = edge_map_2.get(p);
                    ret.add(v);
                }
                arrows.add(new Pair<>(edge_c.first, ret));
            }
            Const ret = new Const(objs, attrs1, arrows, c, sig);
            return ret;
        };

		return new Pair<>(new Quad<>(sig, fst, snd, pair), new Quad<>(node_map,
				attr_map, edge_map_1, edge_map_2));
	}

	private static Quad<SigExp.Const, Const, Const, Fn<Triple<SigExp.Const, Const, Const>, Const>> plus(
            SigExp.Const a, SigExp.Const b) {
		int node_count = 0;
		Map<String, String> node_map_1 = new LinkedHashMap<>();
		Map<String, String> node_map_2 = new LinkedHashMap<>();
		List<Pair<String, String>> a_objs = new LinkedList<>();
		List<Pair<String, String>> b_objs = new LinkedList<>();
		for (String n : a.nodes) {
			node_map_1.put(n, "node" + node_count);
			a_objs.add(new Pair<>(n, "node" + node_count));
			node_count++;
		}
		for (String n : b.nodes) {
			node_map_2.put(n, "node" + node_count);
			b_objs.add(new Pair<>(n, "node" + node_count));
			node_count++;
		}
		List<String> nodes = new LinkedList<>();
		nodes.addAll(node_map_1.values());
		nodes.addAll(node_map_2.values());

		int attr_count = 0;
		Map<String, Triple<String, String, String>> attr_map_1 = new LinkedHashMap<>();
		Map<String, Triple<String, String, String>> attr_map_2 = new LinkedHashMap<>();
		List<Pair<String, String>> a_attrs = new LinkedList<>();
		List<Pair<String, String>> b_attrs = new LinkedList<>();
		for (Triple<String, String, String> n : a.attrs) {
			attr_map_1.put(n.first, new Triple<>("attr" + attr_count,
					node_map_1.get(n.second), n.third));
			a_attrs.add(new Pair<>(n.first, "attr" + attr_count));
			attr_count++;
		}
		for (Triple<String, String, String> n : b.attrs) {
			attr_map_2.put(n.first, new Triple<>("attr" + attr_count,
					node_map_2.get(n.second), n.third));
			b_attrs.add(new Pair<>(n.first, "attr" + attr_count));
			attr_count++;
		}
		List<Triple<String, String, String>> attrs = new LinkedList<>();
		attrs.addAll(attr_map_1.values());
		attrs.addAll(attr_map_2.values());

		int edge_count = 0;
		Map<String, Triple<String, String, String>> edge_map_1 = new LinkedHashMap<>();
		Map<String, Triple<String, String, String>> edge_map_2 = new LinkedHashMap<>();
		List<Pair<String, List<String>>> a_arrows = new LinkedList<>();
		List<Pair<String, List<String>>> b_arrows = new LinkedList<>();
		for (Triple<String, String, String> n : a.arrows) {
			edge_map_1.put(n.first, new Triple<>("edge" + edge_count,
					node_map_1.get(n.second), node_map_1.get(n.third)));
			List<String> x = new LinkedList<>();
			x.add(node_map_1.get(n.second));
			x.add("edge" + edge_count);
			a_arrows.add(new Pair<>(n.first, x));
			edge_count++;
		}
		for (Triple<String, String, String> n : b.arrows) {
			edge_map_2.put(n.first, new Triple<>("edge" + edge_count,
					node_map_2.get(n.second), node_map_2.get(n.third)));
			List<String> x = new LinkedList<>();
			x.add(node_map_2.get(n.second));
			x.add("edge" + edge_count);
			b_arrows.add(new Pair<>(n.first, x));
			edge_count++;
		}
		List<Triple<String, String, String>> arrows = new LinkedList<>();
		arrows.addAll(edge_map_1.values());
		arrows.addAll(edge_map_2.values());

		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
		for (Pair<List<String>, List<String>> eq : a.eqs) {
			List<String> lhs = new LinkedList<>();
			lhs.add(node_map_1.get(eq.first.get(0)));
			for (int i = 1; i < eq.first.size(); i++) {
				lhs.add(edge_map_1.get(eq.first.get(i)).first);
			}
			List<String> rhs = new LinkedList<>();
			rhs.add(node_map_1.get(eq.second.get(0)));
			for (int i = 1; i < eq.second.size(); i++) {
				rhs.add(edge_map_1.get(eq.second.get(i)).first);
			}
			eqs.add(new Pair<>(lhs, rhs));
		}
		for (Pair<List<String>, List<String>> eq : b.eqs) {
			List<String> lhs = new LinkedList<>();
			lhs.add(node_map_2.get(eq.first.get(0)));
			for (int i = 1; i < eq.first.size(); i++) {
				lhs.add(edge_map_2.get(eq.first.get(i)).first);
			}
			List<String> rhs = new LinkedList<>();
			rhs.add(node_map_2.get(eq.second.get(0)));
			for (int i = 1; i < eq.second.size(); i++) {
				rhs.add(edge_map_2.get(eq.second.get(i)).first);
			}
			eqs.add(new Pair<>(lhs, rhs));
		}

		SigExp.Const sig = new SigExp.Const(nodes, attrs, arrows, eqs);
		Const inj1 = new Const(a_objs, a_attrs, a_arrows, a, sig);
		Const inj2 = new Const(b_objs, b_attrs, b_arrows, b, sig);

		Fn<Triple<SigExp.Const, Const, Const>, Const> match = x -> {
            SigExp.Const c = x.first;
            Const f = x.second;
            Const g = x.third;

            if (!f.dst.equals(g.dst)) {
                throw new RuntimeException("Targets don't agree: " + f.dst
                        + " and " + g.dst);
            }
            if (!f.src.equals(a)) {
                throw new RuntimeException("Source of " + f + " is not "
                        + a);
            }
            if (!g.src.equals(b)) {
                throw new RuntimeException("Source of " + g + "is not " + b);
            }

            List<Pair<String, String>> objs = new LinkedList<>();
            for (String obj_a : a.nodes) {
                objs.add(new Pair<>(node_map_1.get(obj_a), lookup(obj_a,
                        f.objs)));
            }
            for (String obj_b : b.nodes) {
                objs.add(new Pair<>(node_map_2.get(obj_b), lookup(obj_b,
                        g.objs)));
            }

            List<Pair<String, String>> attrs1 = new LinkedList<>();
            for (Triple<String, String, String> attr_a : a.attrs) {
                attrs1.add(new Pair<>(attr_map_1.get(attr_a.first).first,
                        lookup(attr_a.first, f.attrs)));
            }
            for (Triple<String, String, String> attr_b : b.attrs) {
                attrs1.add(new Pair<>(attr_map_2.get(attr_b.first).first,
                        lookup(attr_b.first, g.attrs)));
            }

            List<Pair<String, List<String>>> arrows1 = new LinkedList<>();
            for (Triple<String, String, String> edge_a : a.arrows) {
                arrows1.add(new Pair<>(edge_map_1.get(edge_a.first).first,
                        lookup(edge_a.first, f.arrows)));
            }
            for (Triple<String, String, String> edge_b : b.arrows) {
                arrows1.add(new Pair<>(edge_map_2.get(edge_b.first).first,
                        lookup(edge_b.first, g.arrows)));
            }

            return new Const(objs, attrs1, arrows1, sig, c);
        };

		return new Quad<>(sig, inj1, inj2, match);
	}

	private static <A, B> B lookup(A a, List<Pair<A, B>> l) {
		for (Pair<A, B> k : l) {
			if (k.first.equals(a)) {
				return k.second;
			}
		}
		throw new RuntimeException();
	}

	// /////////////////////////////////////

	@Override
	public SigExp.Const visit(FQLProgram env, Zero e) {
		return zero().first;
	}

	@Override
	public SigExp.Const visit(FQLProgram env, One e) {
		return one(e.attrs).first;
	}

	@Override
	public SigExp.Const visit(FQLProgram env, Plus e) {
		return plus(e.a.accept(env, this), e.b.accept(env, this)).first;
	}

	@Override
	public SigExp.Const visit(FQLProgram env, Times e) {
		return prod(e.a.accept(env, this), e.b.accept(env, this)).first.first;
	}

	@Override
	public SigExp.Const visit(FQLProgram env, Exp e) {
		try {
			return exp(env, e.a.accept(env, this).toSig(env),
					e.b.accept(env, this).toSig(env)).toSig(env.enums).first
					.toConst();
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}

	@Override
	public SigExp.Const visit(FQLProgram env, Var e) {
		return env.sigs.get(e.v).accept(env, this);
	}

	@Override
	public SigExp.Const visit(FQLProgram env, SigExp.Const e) {
		return e;
	}

	// /////////////////////////////

	@Override
	public Const visit(FQLProgram env, Id e) {
		SigExp.Const s = e.t.toConst(env);

		List<Pair<String, String>> objs = new LinkedList<>();
		for (String x : s.nodes) {
			objs.add(new Pair<>(x, x));
		}

		List<Pair<String, String>> attrs = new LinkedList<>();
		for (Triple<String, String, String> x : s.attrs) {
			attrs.add(new Pair<>(x.first, x.first));
		}

		List<Pair<String, List<String>>> arrows = new LinkedList<>();
		for (Triple<String, String, String> x : s.arrows) {
			List<String> l = new LinkedList<>();
			l.add(x.second);
			l.add(x.first);
			arrows.add(new Pair<>(x.first, l));
		}

		return new Const(objs, attrs, arrows, s, s);
	}

	@Override
	public Const visit(FQLProgram env, Comp e) {
		Const a = e.l.toConst(env);
		Const b = e.r.toConst(env);

		if (!a.dst.equals(b.src)) {
			throw new RuntimeException();
		}

		List<Pair<String, String>> objs = new LinkedList<>();
		for (Pair<String, String> x : a.objs) {
			objs.add(new Pair<>(x.first, lookup(x.second, b.objs)));
		}

		List<Pair<String, String>> attrs = new LinkedList<>();
		for (Pair<String, String> x : a.attrs) {
			attrs.add(new Pair<>(x.first, lookup(x.second, b.attrs)));
		}

		List<Pair<String, List<String>>> arrows = new LinkedList<>();
		for (Pair<String, List<String>> x : a.arrows) {
			String n = lookup(x.second.get(0), b.objs);

			List<String> l = new LinkedList<>();
			l.add(n);
			for (int i = 1; i < x.second.size(); i++) {
				List<String> p = lookup(x.second.get(i), b.arrows);
				l.addAll(p.subList(1, p.size()));
			}
			arrows.add(new Pair<>(x.first, l));
		}

		return new Const(objs, attrs, arrows, a.src, b.dst);
	}

	@Override
	public Const visit(FQLProgram env, Dist1 e) {
		throw new RuntimeException();
	}

	@Override
	public Const visit(FQLProgram env, Dist2 e) {
		throw new RuntimeException();
	}

	@Override
	public Const visit(FQLProgram env, MapExp.Var e) {
		return env.maps.get(e.v).accept(env, this);
	}

	@Override
	public Const visit(FQLProgram env, Const e) {
		Pair<SigExp, SigExp> k = e.type(env); // resolve vars
		return new Const(e.objs, e.attrs, e.arrows, k.first, k.second);
	}

	@Override
	public Const visit(FQLProgram env, TT e) {
		return one(e.attrs).second.of(e.t.accept(env, this));
	}

	@Override
	public Const visit(FQLProgram env, FF e) {
		return zero().second.of(e.t.accept(env, this));
	}

	@Override
	public Const visit(FQLProgram env, Fst e) {
		return prod(e.s.accept(env, this), e.t.accept(env, this)).first.second;
	}

	@Override
	public Const visit(FQLProgram env, Snd e) {
		return prod(e.s.accept(env, this), e.t.accept(env, this)).first.third;
	}

	@Override
	public Const visit(FQLProgram env, Inl e) {
		return plus(e.s.accept(env, this), e.t.accept(env, this)).second;
	}

	@Override
	public Const visit(FQLProgram env, Inr e) {
		return plus(e.s.accept(env, this), e.t.accept(env, this)).third;
	}

	@Override
	public Const visit(FQLProgram env, Apply e) {
		try {
			SigExp.Const A = e.s.accept(env, this);
			SigExp.Const B = e.t.accept(env, this);
			Signature Bsig = B.toSig(env);
			Signature Asig = A.toSig(env);

			FinCat<Mapping, Map<Node, Path>> cat = exp(env, A.toSig(env),
					B.toSig(env));

			Quad<Signature, Pair<Map<Mapping, String>, Map<String, Mapping>>, Pair<Map<Arr<Mapping, Map<Node, Path>>, String>, Map<String, Arr<Mapping, Map<Node, Path>>>>, Pair<Map<Attribute<Mapping>, String>, Map<String, Attribute<Mapping>>>> AeB_stuff = cat
					.toSig(env.enums);

			SigExp.Const AeB = AeB_stuff.first.toConst(); // A^B

			Quad<SigExp.Const, Const, Const, Fn<Triple<SigExp.Const, Const, Const>, Const>> AeBtB_stuff = prod(
					AeB, e.t.accept(env, this)).first;

			SigExp.Const AeBtB = AeBtB_stuff.first; // A^B * B

			if (!AeBtB.attrs.isEmpty()) {
				throw new RuntimeException("found attributes in A^B*B");
			}

			List<Pair<String, String>> nm = new LinkedList<>();

			for (String n : AeBtB.nodes) {
				String n1 = lookup(n, AeBtB_stuff.second.objs); // node in A^B
				String n2 = lookup(n, AeBtB_stuff.third.objs); // node in B

				Mapping f = AeB_stuff.second.second.get(n1);

				Node n0 = f.nm.get(new Node(n2));
				nm.add(new Pair<>(n, n0.string));
			}
		
			List<Pair<String, List<String>>> arrows = new LinkedList<>();

			// Map<String, Path> pm = new HashMap<>();
			for (Triple<String, String, String> n : AeBtB.arrows) {
				List<String> n1 = lookup(n.first, AeBtB_stuff.second.arrows);
				List<String> n2 = lookup(n.first, AeBtB_stuff.third.arrows);

				// the path n1 corresponds to a morphism in the fincat
				Mapping init = AeB_stuff.second.second.get(n1.get(0));
				Arr<Mapping, Map<Node, Path>> nt = cat.identities.get(init);
				for (int i = 1; i < n1.size(); i++) {
					String s = n1.get(i);
					nt = cat.compose(nt, AeB_stuff.third.second.get(s)); // nt
				}

				Path j = nt.dst.appy(Asig, new Path(Bsig, n2));

				Path y = nt.arr.get(new Node(n2.get(0)));

				Path o = Path.append(Asig, y, j);

				arrows.add(new Pair<>(n.first, o.asList()));
			}

			List<Pair<String, String>> attrs = new LinkedList<>();
			// List<Pair<String, String>> objs = new LinkedList<>();
			return new Const(nm, attrs, arrows, AeBtB, A);

		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}

	}

	@Override
	public Const visit(FQLProgram env, Curry e) {
		try {
			Const F = e.f.accept(env, this);

			Pair<SigExp, SigExp> type = e.f.type(env);
			SigExp.Const C = type.second.toConst(env);
			Signature Csig = C.toSig(env);

			if (!(type.first instanceof Times)) {
				throw new RuntimeException();
			}

			Times src = (Times) type.first;
			SigExp.Const A = src.a.toConst(env);
			SigExp.Const B = src.b.toConst(env);
			//Signature Asig = A.toSig(env);
			Signature Bsig = B.toSig(env);
			
			if (!A.attrs.isEmpty()) {
				throw new RuntimeException("Cannot curry when context has attributes.");
			}

			Pair<Quad<SigExp.Const, Const, Const, Fn<Triple<SigExp.Const, Const, Const>, Const>>, Quad<Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>>> AB_stuff = prod(
					A, B);
		//	SigExp.Const AB = AB_stuff.first.first;
			Quad<Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>> maps = AB_stuff.second;

			FinCat<Mapping, Map<Node, Path>> cat = exp(env, C.toSig(env),
					B.toSig(env));
			Quad<Signature, Pair<Map<Mapping, String>, Map<String, Mapping>>, Pair<Map<Arr<Mapping, Map<Node, Path>>, String>, Map<String, Arr<Mapping, Map<Node, Path>>>>, Pair<Map<Attribute<Mapping>, String>, Map<String, Attribute<Mapping>>>> CB_stuff = cat
					.toSig(env.enums);
			Signature CB = CB_stuff.first;

			List<Pair<String, String>> nmret = new LinkedList<>();
			for (String a : A.nodes) {
				Mapping m = curry_helper(F, Csig, B, Bsig, maps, a);
				String target = CB_stuff.second.first.get(m);
				nmret.add(new Pair<>(a,target));
			}
			
			List<Pair<String, List<String>>> amret = new LinkedList<>();
			for (Triple<String, String, String> a : A.arrows) {

				Mapping s = curry_helper(F, Csig, B, Bsig, maps, a.second);
				Mapping t = curry_helper(F, Csig, B, Bsig, maps, a.third);
				
				Map<Node, Path> nt = new HashMap<>();

				for (String b : B.nodes) {
					String p = maps.third.get(new Pair<>(a.first, b)); //edge A*B
					List<String> p0 = lookup(p, F.arrows); // path C
					Path p1 = new Path(Csig, p0);
					nt.put(new Node(b), p1);
				}
								
				List<String> l = new LinkedList<>();
				l.add(CB_stuff.second.first.get(s));
				Arr<Mapping, Map<Node, Path>> arr = new Arr<>(nt, s, t);
				if (null != CB_stuff.third.first.get(arr)) {
					l.add(CB_stuff.third.first.get(arr));
				}
				amret.add(new Pair<>(a.first, l));
			}

			List<Pair<String, String>> attrs = new LinkedList<>();
			// A*B -> C
			// A -> C^B
			
			Const ret = new Const(nmret, attrs, amret, A, CB.toConst());
			ret.toMap(env);
			return ret;
			
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}

	private static Mapping curry_helper(
			Const F,
			Signature Csig,
			SigExp.Const B,
			Signature Bsig,
			Quad<Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>, Map<Pair<String, String>, String>> maps,
			String a) throws FQLException {
		List<Pair<String, String>> nm = new LinkedList<>();
		List<Pair<String, String>> am = new LinkedList<>();
		List<Pair<String, List<String>>> em = new LinkedList<>();

		for (String b : B.nodes) {
			String ab = maps.first.get(new Pair<>(a, b));
			nm.add(new Pair<>(b, lookup(ab, F.objs)));
		}
		for (Triple<String, String, String> b : B.arrows) {
			String i = maps.fourth.get(new Pair<>(b.first, a));
			List<String> j = lookup(i, F.arrows);
			em.add(new Pair<>(b.first, j));
		}
		Mapping m = new Mapping(Bsig, Csig, nm, am, em);
		return m;
	}

	@Override
	public Const visit(FQLProgram env, Case e) {
		Const lx = e.l.accept(env, this);
		Const rx = e.r.accept(env, this);
		SigExp.Const cx = lx.dst.accept(env, this);

		return plus(lx.src.accept(env, this), rx.src.accept(env, this)).fourth
				.of(new Triple<>(cx, lx, rx));

	}

	@Override
	public Const visit(FQLProgram env, Prod e) {
		Const lx = e.l.accept(env, this);
		Const rx = e.r.accept(env, this);
		SigExp.Const cx = lx.src.accept(env, this);
		SigExp.Const dx = rx.src.accept(env, this);
		if (!cx.equals(dx)) {
			throw new RuntimeException(cx + " and " + dx + " and " + lx
					+ " and " + rx);
		}

		return prod(lx.dst.accept(env, this), rx.dst.accept(env, this)).first.fourth
				.of(new Triple<>(cx, lx, rx));
	}

	@Override
	public Const visit(FQLProgram env, Sub e) {
		SigExp lt = e.s.typeOf(env);
		SigExp rt = e.t.typeOf(env);
		if (!(lt instanceof SigExp.Const)) {
			throw new RuntimeException(e.s
					+ " does not have constant schema, has " + lt);
		}
		if (!(rt instanceof SigExp.Const)) {
			throw new RuntimeException(e.t
					+ " does not have constant schema, has " + lt);
		}
		SigExp.Const lt0 = (SigExp.Const) lt;
		SigExp.Const rt0 = (SigExp.Const) rt;

		List<Pair<String, String>> objs = new LinkedList<>();
		List<Pair<String, String>> attrs = new LinkedList<>();
		List<Pair<String, List<String>>> arrows = new LinkedList<>();

		for (String n : lt0.nodes) {
			if (!rt0.nodes.contains(n)) {
				throw new RuntimeException("Not subset, missing node " + n);
			}
			objs.add(new Pair<>(n, n));
		}
		for (Triple<String, String, String> n : lt0.arrows) {
			if (!rt0.arrows.contains(n)) {
				throw new RuntimeException("Not subset, missing arrow " + n);
			}
			List<String> x = new LinkedList<>();
			x.add(n.second);
			x.add(n.first);
			arrows.add(new Pair<>(n.first, x));
		}
		for (Triple<String, String, String> n : lt0.attrs) {
			if (!rt0.attrs.contains(n)) {
				throw new RuntimeException("Not subset, missing attribute " + n);
			}
			attrs.add(new Pair<>(n.first, n.first));
		}

		return new Const(objs, attrs, arrows, lt, rt);
	}

	@Override
	public SigExp.Const visit(FQLProgram env, Union e) {
		return (SigExp.Const) e.typeOf(env);
	}

	@Override
	public SigExp.Const visit(FQLProgram env, Opposite e) {
		SigExp.Const r = e.e.accept(env, this);
		List<Triple<String, String, String>> arrows = new LinkedList<>();

		for (Triple<String, String, String> l : r.arrows) {
			arrows.add(new Triple<>(l.first, l.third, l.second));
		}

		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
		for (Pair<List<String>, List<String>> eq : r.eqs) {
			List<String> lhs = new LinkedList<>(eq.first);
			List<String> rhs = new LinkedList<>(eq.second);
			// Collections.re

			lhs.remove(0);
			rhs.remove(0);
			Collections.reverse(lhs);
			Collections.reverse(rhs);

			if (lhs.size() > 1) {
				String src = lookup(arrows, lhs.get(0));
				rhs.add(0, src);
				lhs.add(0, src);
			} else if (rhs.size() > 1) {
				String src = lookup(arrows, rhs.get(0));
				lhs.add(0, src);
				rhs.add(0, src);
			} else {
				lhs.add(0, eq.first.get(0));
				rhs.add(0, eq.first.get(0));
			}
			eqs.add(new Pair<>(lhs, rhs));
		}

		return new SigExp.Const(new LinkedList<>(r.nodes), new LinkedList<>(
				r.attrs), arrows, eqs);
	}

	private static String lookup(List<Triple<String, String, String>> arrows,
			String string) {
		for (Triple<String, String, String> k : arrows) {
			if (k.first.equals(string)) {
				return k.second;
			}
		}
		throw new RuntimeException();
	}

	@Override
	public Const visit(FQLProgram env, MapExp.Opposite e) {
		Const k = e.e.accept(env, this);
		Pair<SigExp, SigExp> p = k.type(env);

		List<Pair<String, List<String>>> arrows = new LinkedList<>();

		SigExp.Const yyy = p.second.toConst(env);

		for (Pair<String, List<String>> arrow : k.arrows) {
			List<String> xxx = new LinkedList<>(arrow.second);

			xxx.remove(0);
			Collections.reverse(xxx);
			if (xxx.isEmpty()) {
				xxx.add(arrow.second.get(0));
			} else {
				String v = lookup(yyy.arrows, xxx.get(0));
				xxx.add(0, v);
			}

			arrows.add(new Pair<>(arrow.first, xxx));
		}

		return new Const(new LinkedList<>(k.objs), new LinkedList<>(k.attrs),
				arrows, p.first.toConst(env), yyy);
	}

	@Override
	public SigExp.Const visit(FQLProgram env, Unknown e) {
		throw new RuntimeException("Encountered unknown type.");
	}

	@Override
	public Const visit(FQLProgram env, Iso e) {
		try {
		Signature l = e.l.toSig(env);
		Signature r = e.r.toSig(env);
		
		Pair<Mapping, Mapping> s = Inst.iso(l, r);
		if (s == null) {
			throw new RuntimeException("Cannot find isomorphism for " + e);
		}
			return e.lToR ? s.first.toConst() : s.second.toConst();
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}
	}

}

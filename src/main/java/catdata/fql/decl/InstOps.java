package catdata.fql.decl;

import java.util.HashMap;
import java.util.HashSet;
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
import catdata.fql.decl.FullQuery.FullQueryVisitor;
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
import catdata.fql.decl.TransExp.And;
import catdata.fql.decl.TransExp.Bool;
import catdata.fql.decl.TransExp.Case;
import catdata.fql.decl.TransExp.Chi;
import catdata.fql.decl.TransExp.Comp;
import catdata.fql.decl.TransExp.Coreturn;
import catdata.fql.decl.TransExp.FF;
import catdata.fql.decl.TransExp.Fst;
import catdata.fql.decl.TransExp.Id;
import catdata.fql.decl.TransExp.Implies;
import catdata.fql.decl.TransExp.Inl;
import catdata.fql.decl.TransExp.Inr;
import catdata.fql.decl.TransExp.Not;
import catdata.fql.decl.TransExp.Or;
import catdata.fql.decl.TransExp.Prod;
import catdata.fql.decl.TransExp.Return;
import catdata.fql.decl.TransExp.Snd;
import catdata.fql.decl.TransExp.Squash;
import catdata.fql.decl.TransExp.TT;
import catdata.fql.decl.TransExp.TransCurry;
import catdata.fql.decl.TransExp.TransEval;
import catdata.fql.decl.TransExp.TransExpVisitor;
import catdata.fql.decl.TransExp.TransIso;
import catdata.fql.decl.TransExp.UnChi;
import catdata.fql.decl.TransExp.Var;
import catdata.fql.sql.CopyFlower;
import catdata.fql.sql.CreateTable;
import catdata.fql.sql.DropTable;
import catdata.fql.sql.ExpPSM;
import catdata.fql.sql.Flower;
import catdata.fql.sql.FullSigmaCounit;
import catdata.fql.sql.FullSigmaTrans;
import catdata.fql.sql.InsertKeygen;
import catdata.fql.sql.InsertSQL;
import catdata.fql.sql.InsertSQL2;
import catdata.fql.sql.InsertValues;
import catdata.fql.sql.PSM;
import catdata.fql.sql.PSMAnd;
import catdata.fql.sql.PSMBool;
import catdata.fql.sql.PSMChi;
import catdata.fql.sql.PSMCurry;
import catdata.fql.sql.PSMEval;
import catdata.fql.sql.PSMGen;
import catdata.fql.sql.PSMIso;
import catdata.fql.sql.PSMNot;
import catdata.fql.sql.PSMStep;
import catdata.fql.sql.PSMUnChi;
import catdata.fql.sql.PropPSM;
import catdata.fql.sql.Relationalizer;
import catdata.fql.sql.SQL;
import catdata.fql.sql.SimpleCreateTable;
import catdata.fql.sql.Union;

public class InstOps implements
		FullQueryVisitor<Pair<List<PSM>, String>, String>,
		TransExpVisitor<List<PSM>, String>,
		InstExpVisitor<Pair<List<PSM>, Object>, String> {

	private final FQLProgram prog;
	private int count = 0;

	private String next() {
		return "inst_ops_temp" + count++;
	}

	public InstOps(FQLProgram prog) {
		this.prog = prog;
	}

	@Override
	public List<PSM> visit(String dst, Id e) {
		List<PSM> ret = new LinkedList<>();

		for (Node k : prog.insts.get(e.t).type(prog).toSig(prog).nodes) {
			ret.add(new InsertSQL(dst + "_" + k.string, new CopyFlower(e.t
					+ "_" + k.string, "c0", "c1"), "c0", "c1"));
		}

		return ret;
	}

	@Override
	public List<PSM> visit(String dst, Comp e) {
		List<PSM> ret = new LinkedList<>();

		Pair<String, String> ty = e.l.type(prog);
		InstExp inst = prog.insts.get(ty.first);
		Signature inst_type = inst.type(prog).toSig(prog);

		String el = next();
		ret.addAll(PSMGen.makeTables(el, inst_type, false));
		ret.addAll(e.l.accept(el, this));
		String er = next();
		ret.addAll(PSMGen.makeTables(er, inst_type, false));
		ret.addAll(e.r.accept(er, this));

		for (Node k : inst_type.nodes) {
			Map<String, String> from = new HashMap<>();
			from.put("lft", el + "_" + k);
			from.put("rght", er + "_" + k);

			List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
			where.add(new Pair<>(new Pair<>("lft", "c1"), new Pair<>("rght",
					"c0")));

			LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
			select.put("c0", new Pair<>("lft", "c0"));
			select.put("c1", new Pair<>("rght", "c1"));

			Flower f = new Flower(select, from, where);

			ret.add(new InsertSQL(dst + "_" + k, f, "c0", "c1"));
		}

		ret.addAll(PSMGen.dropTables(el, inst_type));
		ret.addAll(PSMGen.dropTables(er, inst_type));

		return ret;
	}

	@Override
	public List<PSM> visit(String dst, Var e) {
		return prog.transforms.get(e.v).accept(dst, this);
	}

	@Override
	public List<PSM> visit(String dst, TransExp.Const e) {
		List<PSM> ret = new LinkedList<>();

		Signature s = prog.insts.get(e.src).type(prog).toConst(prog)
				.toSig(prog);

		List<String> attrs = new LinkedList<>();
		attrs.add("c0");
		attrs.add("c1");

		ret.addAll(PSMGen.makeTables("pre_" + dst, s, false));
		for (Node k : s.nodes) {
			Set<Map<Object, Object>> values = convert(lookup(k.string, e.objs));
			if (!values.isEmpty()) {
				ret.add(new InsertValues("pre_" + dst + "_" + k.string, attrs,
						values));
			}

			SQL f = PSMGen.compose(e.src + "_" + k.string + "_subst_inv", "pre_" + dst + "_" + k.string, e.dst + "_" + k.string + "_subst");
			ret.add(new InsertSQL(dst + "_" + k.string, f, "c0", "c1"));
		}

		ret.addAll(PSMGen.dropTables("pre_" + dst, s));
		return ret;
	}

	private static List<Pair<Object, Object>> lookup(String string,
			List<Pair<String, List<Pair<Object, Object>>>> objs) {
		for (Pair<String, List<Pair<Object, Object>>> k : objs) {
			if (k.first.equals(string)) {
				return k.second;
			}
		}
		throw new RuntimeException(string + " not found in " + objs);
	}

	private static Set<Map<Object, Object>> convert(List<Pair<Object, Object>> list) {
		Set<Map<Object, Object>> ret = new HashSet<>();

		for (Pair<Object, Object> k : list) {
			Map<Object, Object> map = new HashMap<>();
			map.put("c0", k.first);
			map.put("c1", k.second);
			ret.add(map);
		}
		return ret;
	}

	@Override
	public List<PSM> visit(String dst, TT e) {
		try {
			List<PSM> ret = new LinkedList<>();
			Signature s = prog.insts.get(e.obj).type(prog).toSig(prog);

			String temp1 = next();
			ret.addAll(PSMGen.makeTables(temp1, s, false));
			String temp2 = next();
			ret.addAll(PSMGen.makeTables(temp2, s, false));

			Pair<Map<Node, List<Pair<Path, Attribute<Node>>>>, List<PSM>> xxx = Relationalizer
					.observations(s, temp1, e.tgt, false);
			Pair<Map<Node, List<Pair<Path, Attribute<Node>>>>, List<PSM>> yyy = Relationalizer
					.observations(s, temp2, e.obj, false);
			if (!xxx.first.equals(yyy.first)) {
				throw new RuntimeException("not equal: " + xxx + " and " + yyy);
			}
			ret.addAll(xxx.second);
			ret.addAll(yyy.second);

			for (Node n : s.nodes) {
				List<?> cols = xxx.first.get(n);
				Map<String, String> from = new HashMap<>();
				from.put("t1", e.tgt + "_" + n);
				from.put("t1_obs", temp1 + "_" + n + "_" + "observables");
				from.put("t2", e.obj + "_" + n);
				from.put("t2_obs", temp2 + "_" + n + "_" + "observables");
				List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
				where.add(new Pair<>(new Pair<>("t1", "c0"), new Pair<>(
						"t1_obs", "id")));
				where.add(new Pair<>(new Pair<>("t2", "c0"), new Pair<>(
						"t2_obs", "id")));
				for (int i = 0; i < cols.size(); i++) {
					where.add(new Pair<>(new Pair<>("t1_obs", "c" + i),
							new Pair<>("t2_obs", "c" + i)));
				}
				LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
				select.put("c0", new Pair<>("t1", "c0"));
				select.put("c1", new Pair<>("t2", "c0"));
				Flower f = new Flower(select, from, where);
				ret.add(new InsertSQL(dst + "_" + n, f, "c0", "c1"));

			}

			ret.addAll(PSMGen.dropTables(temp1, s));
			ret.addAll(PSMGen.dropTables(temp2, s));

			return ret;

		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	@Override
	public List<PSM> visit(String dst, FF e) {
		return new LinkedList<>();
	}

	@Override
	public List<PSM> visit(String dst, Fst e) {
		List<PSM> ret = new LinkedList<>();

		InstExp k = prog.insts.get(e.obj);
		Signature t = k.type(prog).toSig(prog);

		for (Node n : t.nodes) {
			ret.add(new InsertSQL(dst + "_" + n.string, new CopyFlower(e.obj
					+ "_fst_" + n.string, "c0", "c1"), "c0", "c1"));
		}

		return ret;
	}

	@Override
	public List<PSM> visit(String dst, Snd e) {
		List<PSM> ret = new LinkedList<>();

		InstExp k = prog.insts.get(e.obj);
		Signature t = k.type(prog).toSig(prog);

		for (Node n : t.nodes) {
			ret.add(new InsertSQL(dst + "_" + n.string, new CopyFlower(e.obj
					+ "_snd_" + n.string, "c0", "c1"), "c0", "c1"));
		}

		return ret;
	}

	@Override
	public List<PSM> visit(String dst, Inl e) {
		List<PSM> ret = new LinkedList<>();

		InstExp k = prog.insts.get(e.obj);
		Signature t = k.type(prog).toSig(prog);

		for (Node n : t.nodes) {
			ret.add(new InsertSQL(dst + "_" + n.string, new CopyFlower(e.obj
					+ "_inl_" + n.string, "c0", "c1"), "c0", "c1"));
		}

		return ret;
	}

	@Override
	public List<PSM> visit(String dst, Inr e) {
		List<PSM> ret = new LinkedList<>();

		InstExp k = prog.insts.get(e.obj);
		Signature t = k.type(prog).toSig(prog);

		for (Node n : t.nodes) {
			ret.add(new InsertSQL(dst + "_" + n.string, new CopyFlower(e.obj
					+ "_inr_" + n.string, "c0", "c1"), "c0", "c1"));
		}

		return ret;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PSM> visit(String dst, Case e) {

		Fn<Quad<String, String, String, String>, List<PSM>> fn = (Fn<Quad<String, String, String, String>, List<PSM>>) prog.insts
				.get(e.obj).accept(e.obj, this).second;

		List<PSM> ret = new LinkedList<>();

		Signature inst_type = prog.insts.get(e.obj).type(prog).toSig(prog);

		String el = next();
		ret.addAll(PSMGen.makeTables(el, inst_type, false));
		ret.addAll(e.l.accept(el, this));
		String er = next();
		ret.addAll(PSMGen.makeTables(er, inst_type, false));
		ret.addAll(e.r.accept(er, this));

		ret.addAll(fn.of(new Quad<>(el, er, null,
                dst)));

		ret.addAll(PSMGen.dropTables(el, inst_type));
		ret.addAll(PSMGen.dropTables(er, inst_type));

		return ret;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PSM> visit(String dst, Prod e) {
		Fn<Quad<String, String, String, String>, List<PSM>> fn = (Fn<Quad<String, String, String, String>, List<PSM>>) prog.insts
				.get(e.obj).accept(e.obj, this).second;

		List<PSM> ret = new LinkedList<>();

		Signature inst_type = prog.insts.get(e.obj).type(prog).toSig(prog);

		String el = next();
		ret.addAll(PSMGen.makeTables(el, inst_type, false));
		ret.addAll(e.l.accept(el, this));
		String er = next();
		ret.addAll(PSMGen.makeTables(er, inst_type, false));
		ret.addAll(e.r.accept(er, this));

		ret.addAll(fn.of(new Quad<>(el, er, null,
                dst)));

		ret.addAll(PSMGen.dropTables(el, inst_type));
		ret.addAll(PSMGen.dropTables(er, inst_type));

		return ret;
	}

	@Override
	public List<PSM> visit(String dst, TransExp.Delta e) {
		List<PSM> ret = new LinkedList<>();
		Pair<String, String> ht = e.h.type(prog);
		Signature sig = prog.insts.get(ht.first).type(prog).toSig(prog);

		// Signature sig = prog.insts.get(e.src).type(prog).toSig(prog);

		Mapping F = ((Delta) prog.insts.get(e.src)).F.toMap(prog);

		String next = next();
		ret.addAll(PSMGen.makeTables(next, sig, false));
		ret.addAll(e.h.accept(next, this));

		Signature sig2 = prog.insts.get(e.src).type(prog).toSig(prog);

		for (Node n : sig2.nodes) {
			String fc = F.nm.get(n).string;
			ret.add(new InsertSQL(
					dst + "_" + n.string,
					PSMGen.compose(e.src + "_" + n.string + "_subst_inv", next + "_" + fc, e.dst + "_" + n.string + "_subst"),
					"c0", "c1"));
		}

		ret.addAll(PSMGen.dropTables(next, sig));
		return ret;
	}

	@Override
	public List<PSM> visit(String dst, TransExp.Sigma e) {
		List<PSM> ret = new LinkedList<>();
		Pair<String, String> ht = e.h.type(prog);
		Signature sig = prog.insts.get(ht.first).type(prog).toSig(prog);

		Mapping F = ((Sigma) prog.insts.get(e.src)).F.toMap(prog);

		String next = next();
		ret.addAll(PSMGen.makeTables(next, sig, false));
		ret.addAll(e.h.accept(next, this));

		Signature sig2 = prog.insts.get(e.src).type(prog).toSig(prog);
		String xxx = "sigfunc";
		ret.addAll(PSMGen.makeTables(xxx, sig2, false));

		for (Node n : sig2.nodes) {
			List<Flower> l = new LinkedList<>();
			for (Node m : F.source.nodes) {
				if (F.nm.get(m).equals(n)) {
					l.add(new CopyFlower(next + "_" + m.string, "c0", "c1"));
				}
			}
			String yyy = xxx + "_" + n.string;
			if (l.isEmpty()) {
				ret.add(new InsertSQL(yyy, l.get(0), "c0", "c1"));
			} else {
				ret.add(new InsertSQL(yyy, new Union(l), "c0", "c1"));
			}
			ret.add(new InsertSQL(dst + "_" + n.string, PSMGen
					.compose(e.src + "_" + n.string + "_subst_inv", yyy, e.dst + "_" + n.string + "_subst"), "c0", "c1"));
		}
		ret.addAll(PSMGen.dropTables(xxx, sig2));
		ret.addAll(PSMGen.dropTables(next, sig));
		return ret;

	}

	@Override
	public List<PSM> visit(String dst, TransExp.FullSigma e) {
		List<PSM> ret = new LinkedList<>();

		Mapping F0 = ((FullSigma) prog.insts.get(e.src)).F.toMap(prog);

		Pair<String, String> t = prog.transforms.get(e.h).type(prog);

		// String next = next();
		// ret.addAll(PSMGen.makeTables(next, F0.source, false));
		// ret.addAll(e.h.accept(next, this));
		ret.add(new FullSigmaTrans(F0, t.first, e.src, t.second, e.dst, e.h,
				dst));

		return ret;
	}

	@Override
	public List<PSM> visit(String dst, TransExp.Pi e) {
		try {
			List<PSM> ret = new LinkedList<>();
			Pair<String, String> ht = e.h.type(prog);
			Signature sig = prog.insts.get(ht.first).type(prog).toSig(prog);

			Mapping F = ((Pi) prog.insts.get(e.src)).F.toMap(prog);

			Map<String, Triple<Node, Node, Arr<Node, Path>>[]> colmap1x = PSMGen
					.pi(F, e.h.type(prog).first, e.src).second;

			String next = next();
			ret.addAll(PSMGen.makeTables(next, sig, false));
			ret.addAll(e.h.accept(next, this));

			Signature sig2 = prog.insts.get(e.src).type(prog).toSig(prog);

			for (Node n : sig2.nodes) {
				List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
				Map<String, String> from = new HashMap<>();
				from.put("limit1", e.src + "_" + n.string + "_limit");
				from.put("limit2", e.dst + "_" + n.string + "_limit");
				LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
				int i = 0;
				for (Triple<Node, Node, Arr<Node, Path>> col : colmap1x
						.get(n.string)) {
					from.put("l" + i, next + "_" + col.second.string);
					where.add(new Pair<>(new Pair<>("l" + i, "c0"), new Pair<>(
							"limit1", "c" + i)));
					where.add(new Pair<>(new Pair<>("l" + i, "c1"), new Pair<>(
							"limit2", "c" + i)));
					i++;
				}
				// here a is unused because attributes will be in this order
				//  check
				for (@SuppressWarnings("unused")
				Attribute<Node> a : sig2.attrsFor(n)) {
					where.add(new Pair<>(new Pair<>("limit1", "c" + i),
							new Pair<>("limit2", "c" + i)));
					i++;
				}
				select.put("c0", new Pair<>("limit1", "guid"));
				select.put("c1", new Pair<>("limit2", "guid"));
				Flower f = new Flower(select, from, where);
				ret.add(new InsertSQL(dst + "_" + n.string, f, "c0", "c1"));
			}
			return ret;
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}

	}

	@Override
	public List<PSM> visit(String dst, TransExp.Relationalize e) {
		List<PSM> ret = new LinkedList<>();
		Pair<String, String> ht = e.h.type(prog);
		Signature sig = prog.insts.get(ht.first).type(prog).toSig(prog);

		String next = next();
		ret.addAll(PSMGen.makeTables(next, sig, false));
		ret.addAll(e.h.accept(next, this));

		Map<String, String> attrs = new HashMap<>();
		attrs.put("c0", PSM.VARCHAR());
		attrs.put("c1", PSM.VARCHAR());
		for (Node n : sig.nodes) {
			ret.add(new CreateTable(n.string + "xxx_temp", attrs, false));
			ret.add(new CreateTable(n.string + "yyy_temp", attrs, false));

			LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
			select.put("c0", new Pair<>("l", "c0"));
			select.put("c1", new Pair<>("r", "c0"));
			Map<String, String> from = new HashMap<>();
			from.put("l", e.src + "_" + n.string + "_subst_inv");
			from.put("r", e.src + "_" + n.string + "_squash");
			List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
			where.add(new Pair<>(new Pair<>("l", "c1"), new Pair<>("r", "c1")));
			Flower jk = new Flower(select, from, where);
			ret.add(new InsertSQL(n.string + "yyy_temp", jk, "c0", "c1"));
			ret.add(new InsertSQL(n.string + "xxx_temp", PSMGen
					.compose(next + "_" + n.string, e.dst + "_" + n.string + "_squash", e.dst + "_" + n.string + "_subst"), "c0", "c1"));
			ret.add(new InsertSQL(dst + "_" + n.string, PSMGen
					.compose(n.string + "yyy_temp", n.string + "xxx_temp"), "c0", "c1"));
			ret.add(new DropTable(n.string + "xxx_temp"));
			ret.add(new DropTable(n.string + "yyy_temp"));
		}

		ret.addAll(PSMGen.dropTables(next, sig));
		return ret;
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Zero e) {
		return new Pair<>(new LinkedList<PSM>(), new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, One e) {
		Triple<Const, Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>, Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>>> k = Relationalizer.terminal(prog,
				e.sig.toConst(prog));
		return k.first.accept(dst, this);
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Two e) {
		List<PSM> ret = new LinkedList<>();	
		ret.add(new PropPSM(dst, e.sig.toSig(prog)));	
		return new Pair<>(ret, new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst,
                                         Plus e) {
		SigExp k = e.type(prog);
		Signature s = k.toSig(prog);
		List<PSM> ret = new LinkedList<>();

		for (Node n : s.nodes) {
			List<Flower> l = new LinkedList<>();
			l.add(new CopyFlower(e.a + "_" + n.string, "c0", "c1"));
			l.add(new CopyFlower(e.b + "_" + n.string, "c0", "c1"));
			ret.add(new InsertSQL(dst + "_" + n.string, new Union(l), "c0",
					"c1"));
		}
		for (Attribute<Node> n : s.attrs) {
			List<Flower> l = new LinkedList<>();
			l.add(new CopyFlower(e.a + "_" + n.name, "c0", "c1"));
			l.add(new CopyFlower(e.b + "_" + n.name, "c0", "c1"));
			ret.add(new InsertSQL(dst + "_" + n.name, new Union(l), "c0",
					"c1"));
		}
		for (Edge n : s.edges) {
			List<Flower> l = new LinkedList<>();
			l.add(new CopyFlower(e.a + "_" + n.name, "c0", "c1"));
			l.add(new CopyFlower(e.b + "_" + n.name, "c0", "c1"));
			ret.add(new InsertSQL(dst + "_" + n.name, new Union(l), "c0",
					"c1"));
		}

		ret.addAll(PSMGen.guidify(dst, s, true));

		ret.addAll(PSMGen.makeTables(dst + "_inl", s, false));
		ret.addAll(PSMGen.makeTables(dst + "_inr", s, false));

		for (Node n : s.nodes) {
			SQL f = PSMGen.compose(e.a + "_" + n.string, dst + "_" + n.string + "_subst");
			ret.add(new InsertSQL(dst + "_inl_" + n.string, f, "c0", "c1"));
			SQL f0 = PSMGen.compose(e.b + "_" + n.string, dst + "_" + n.string + "_subst");
			ret.add(new InsertSQL(dst + "_inr_" + n.string, f0, "c0", "c1"));
		}
		// (f+g) : A+B -> C f : A -> C g : B -> C
		Fn<Quad<String, String, String, String>, List<PSM>> fn = x -> {
            String f = x.first; // e.a -> x.third
            String g = x.second; // e.b -> x.third
            // String C = x.third;
            String dst0 = x.fourth;

            // must be a map dst -> x.third

            List<PSM> ret1 = new LinkedList<>();
            for (Node n : s.nodes) {
                Flower sql1 = PSMGen.compose(dst + "_" + n.string + "_subst_inv", f + "_" + n.string);
                Flower sql2 = PSMGen.compose(dst + "_" + n.string + "_subst_inv", g + "_" + n.string);
                List<Flower> flowers = new LinkedList<>();
                flowers.add(sql1);
                flowers.add(sql2);
                ret1.add(new InsertSQL(dst0 + "_" + n.string, new Union(
                        flowers), "c0", "c1"));
            }

            return ret1;
        };
		return new Pair<>(ret, fn);
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst,
                                         Times e) {
		try {
			SigExp k = e.type(prog);
			Signature s = k.toSig(prog);
			List<PSM> ret = new LinkedList<>();
			ret.addAll(PSMGen.makeTables(dst + "_fst", s, false));
			ret.addAll(PSMGen.makeTables(dst + "_snd", s, false));
			
			Pair<Map<Node, List<Pair<Path, Attribute<Node>>>>, List<PSM>> l_obs = Relationalizer.observations(s, dst + "_l_obs", e.a, false);
			Pair<Map<Node, List<Pair<Path, Attribute<Node>>>>, List<PSM>> r_obs = Relationalizer.observations(s, dst + "_r_obs", e.b, false);
			if (!(l_obs.first.equals(r_obs.first))) {
				throw new RuntimeException("Internal error, please report.");
			}
			
			ret.addAll(PSMGen.makeTables(dst + "_l_obs", s, false));
			ret.addAll(l_obs.second);
			ret.addAll(PSMGen.dropTables(dst + "_l_obs", s));
			ret.addAll(PSMGen.makeTables(dst + "_r_obs", s, false));
			ret.addAll(r_obs.second);
			ret.addAll(PSMGen.dropTables(dst + "_r_obs", s));
			
			for (Node n : s.nodes) {
				List<Pair<Path, Attribute<Node>>> lats = l_obs.first.get(n);
			//	List<Pair<Path, Attribute<Node>>> rats = r_obs.first.get(n);
				
				LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
				Map<String, String> from = new HashMap<>();
				List<String> attrs = new LinkedList<>();
				Map<String, String> attrsM = new HashMap<>();
				List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
				from.put("lft", dst + "_l_obs_" + n.string + "_observables");
				from.put("rght", dst + "_r_obs_" + n.string + "_observables");
				attrs.add("lft");
				attrs.add("rght");
				attrsM.put("lft", PSM.VARCHAR());
				attrsM.put("rght", PSM.VARCHAR());
				select.put("lft", new Pair<>("lft", "id"));
				select.put("rght", new Pair<>("rght", "id"));
				int idx = 0;
				for (Pair<Path, Attribute<Node>> aa : lats) {
					Attribute<Node> a = aa.second;
					where.add(new Pair<>(new Pair<>("lft", "c" + idx),
						                 new Pair<>("rght", "c" + idx)));
					select.put("c" + idx,new Pair<>("lft", "c" + idx));
					attrs.add( "c" + idx);
					attrsM.put("c" + idx, a.target.psm());
					idx++;
				}
				Flower f = new Flower(select, from, where);
				ret.add(new CreateTable(dst + "_prod_temp_" + n.string, attrsM,
						false));
				ret.add(new InsertSQL2(dst + "_prod_temp_" + n.string, f, attrs));
				Map<String, String> attrsM0 = new HashMap<>(attrsM);
				attrsM0.put("gguid", PSM.VARCHAR());
				ret.add(new CreateTable(dst + "_prod_guid_" + n.string,
						attrsM0, false));
				ret.add(new InsertKeygen(dst + "_prod_guid_" + n.string,
						"gguid", dst + "_prod_temp_" + n.string, attrs));

				List<Pair<Pair<String, String>, Pair<String, String>>> where0 = new LinkedList<>();

				from = new HashMap<>();
				from.put("t", dst + "_prod_guid_" + n.string);
				select = new LinkedHashMap<>();
				select.put("c0", new Pair<>("t", "gguid"));
				select.put("c1", new Pair<>("t", "lft"));
				f = new Flower(select, from, where0);
				ret.add(new InsertSQL(dst + "_fst_" + n, f, "c0", "c1"));

				from = new HashMap<>();
				from.put("t", dst + "_prod_guid_" + n.string);
				select = new LinkedHashMap<>();
				select.put("c0", new Pair<>("t", "gguid"));
				select.put("c1", new Pair<>("t", "rght"));
				f = new Flower(select, from, where0);
				ret.add(new InsertSQL(dst + "_snd_" + n, f, "c0", "c1"));

				LinkedHashMap<String, Pair<String, String>> select0 = new LinkedHashMap<>();
				select0.put("c0", new Pair<>("t", "gguid"));
				select0.put("c1", new Pair<>("t", "gguid"));
				Map<String, String> from0 = new HashMap<>();
				from0.put("t", dst + "_prod_guid_" + n.string);
				Flower sql = new Flower(select0, from0, where0);
				ret.add(new InsertSQL(dst + "_" + n.string, sql, "c0", "c1"));
				for (Attribute<Node> a : s.attrsFor(n)) {
					select0 = new LinkedHashMap<>();
					select0.put("c0", new Pair<>("t", "gguid"));
					Arr<Node, Path> ppp = s.toCategory2().first.id(n);
					int ppp0 = lats.indexOf(new Pair<>(ppp.arr, a));
					select0.put("c1", new Pair<>("t", "c" + ppp0));
					from0 = new HashMap<>();
					from0.put("t", dst + "_prod_guid_" + n.string);
					sql = new Flower(select0, from0, where0);
					ret.add(new InsertSQL(dst + "_" + a.name, sql, "c0", "c1"));
				}
			//	ret.add(new DropTable(dst + "_prod_temp_" + n)); 

			}

			for (Edge edge : s.edges) {
				Map<String, String> from = new HashMap<>();
				from.put("leftEdge", e.a + "_" + edge.name);
				from.put("rightEdge", e.b + "_" + edge.name);
				from.put("srcGuid", dst + "_prod_guid_" + edge.source);
				from.put("dstGuid", dst + "_prod_guid_" + edge.target);
				List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
				where.add(new Pair<>(new Pair<>("leftEdge", "c0"), new Pair<>(
						"srcGuid", "lft")));
				where.add(new Pair<>(new Pair<>("rightEdge", "c0"), new Pair<>(
						"srcGuid", "rght")));
				where.add(new Pair<>(new Pair<>("leftEdge", "c1"), new Pair<>(
						"dstGuid", "lft")));
				where.add(new Pair<>(new Pair<>("rightEdge", "c1"), new Pair<>(
						"dstGuid", "rght")));
				LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
				select.put("c0", new Pair<>("srcGuid", "gguid"));
				select.put("c1", new Pair<>("dstGuid", "gguid"));
				Flower f = new Flower(select, from, where);
				ret.add(new InsertSQL(dst + "_" + edge.name, f, "c0", "c1"));
			}

			Fn<Quad<String, String, String, String>, List<PSM>> fn = x -> {
                String f = x.first; // x.third -> e.a
                String g = x.second; // x.third -> e.b
                // String C = x.third;

                String dst0 = x.fourth;

                // must be a map x.third -> dst
                List<PSM> ret1 = new LinkedList<>();
                for (Node n : s.nodes) {
                    List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
                    Map<String, String> from = new HashMap<>();
                    from.put("f", f + "_" + n.string);
                    from.put("g", g + "_" + n.string);
                    from.put("lim", dst + "_prod_guid_" + n.string);
                    where.add(new Pair<>(new Pair<>("f", "c0"), new Pair<>(
                            "g", "c0")));
                    where.add(new Pair<>(new Pair<>("lim", "lft"),
                            new Pair<>("f", "c1")));
                    where.add(new Pair<>(new Pair<>("lim", "rght"),
                            new Pair<>("g", "c1")));
                    LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
                    select.put("c0", new Pair<>("f", "c0"));
                    select.put("c1", new Pair<>("lim", "gguid"));
                    Flower flower = new Flower(select, from, where);
                    ret1.add(new InsertSQL(dst0 + "_" + n.string, flower,
                            "c0", "c1"));
                }

                return ret1;
            };
			return new Pair<>(ret, fn);
		} catch (FQLException fe) {
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Exp e) {
		List<PSM> ret = new LinkedList<>();
		
		ret.add(new ExpPSM(dst, e.a, e.b, prog.insts.get(e.a).type(prog).toSig(prog)));
		
		return new Pair<>(ret, new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Const e) {
		List<PSM> ret = new LinkedList<>();
		Signature sig = e.sig.toSig(prog);
		ret.addAll(PSMGen.doConst(dst, sig, e.data));
		ret.addAll(PSMGen.guidify(dst, sig, true));
		return new Pair<>(ret, new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Delta e) {
		// String next = next();
		List<PSM> ret = new LinkedList<>();
		Mapping F0 = e.F.toMap(prog);

		ret.addAll(PSMGen.delta(F0, e.I, dst));
		ret.addAll(PSMGen.guidify(dst, F0.source, true));
		return new Pair<>(ret, new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Sigma e) {
		List<PSM> ret = new LinkedList<>();
		Mapping F0 = e.F.toMap(prog);

		try {
			F0.okForSigma();
			ret.addAll(PSMGen.sigma(F0, dst, e.I)); // yes, sigma is
													// backwards
			ret.addAll(PSMGen.guidify(dst, F0.target, true));
			return new Pair<>(ret, new Object());
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Pi e) {
		List<PSM> ret = new LinkedList<>();
		Mapping F0 = e.F.toMap(prog);

		try {
			F0.okForPi();
			Pair<List<PSM>, Map<String, Triple<Node, Node, Arr<Node, Path>>[]>> xxx = PSMGen
					.pi(F0, e.I, dst);
			ret.addAll(xxx.first);
			return new Pair<>(ret, xxx.second);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}

	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, FullSigma e) {
		List<PSM> ret = new LinkedList<>();
		Mapping F0 = e.F.toMap(prog);

		ret.addAll(PSMGen.SIGMA(F0, dst, e.I)); // yes, backwards
		return new Pair<>(ret, new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Relationalize e) { // out
																		// then
																		// in
		List<PSM> ret = new LinkedList<>();
		Signature sig = prog.insts.get(e.I).type(prog).toSig(prog);

		try {
			// mktable done by relationalizer
			ret.addAll(Relationalizer.compile(sig, dst, e.I).second);
			ret.addAll(PSMGen.guidify(dst, sig, true));
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
		return new Pair<>(ret, new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, External e) {
		List<PSM> ret = new LinkedList<>();
		Signature sig = e.sig.toSig(prog);
		ret.addAll(PSMGen.doExternal(sig, e.name, dst));
		ret.addAll(PSMGen.guidify(dst, sig, true));
		return new Pair<>(ret, new Object());
	}

	@Override
	public Pair<List<PSM>, Object> visit(String dst, Eval e) {
		Query q = Query.toQuery(prog, e.q);

		String next = e.e;
		String next1 = next();
		String next2 = next();

		List<PSM> ret = new LinkedList<>();

		try {
			q.join.okForPi(); //  maybe redundant?
			q.union.okForSigma();

			// ret.addAll(PSMGen.makeTables(next, q.project.target, false));
			ret.addAll(PSMGen.makeTables(next1, q.project.source, false));
			ret.addAll(PSMGen.makeTables(next2, q.join.target, false));

			// ret.addAll(e.e.accept(next, this));

			ret.addAll(PSMGen.delta(q.project, next, next1));
			ret.addAll(PSMGen.guidify(next1, q.project.source));

			ret.addAll(PSMGen.pi(q.join, next1, next2).first);

			ret.addAll(PSMGen.sigma(q.union, dst, next2)); // backwards
			ret.addAll(PSMGen.guidify(dst, q.union.target));

			// ret.addAll(PSMGen.dropTables(next, q.project.target));
			ret.addAll(PSMGen.dropTables(next1, q.project.source));
			ret.addAll(PSMGen.dropTables(next2, q.join.target));

			return new Pair<>(ret, new Object());
		} catch (FQLException fe) {
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	@Override
	public Pair<List<PSM>, Object> visit(String env, FullEval e) {
		throw new RuntimeException();
	}

	// //////////////////////////////////////

	@Override
	public Pair<List<PSM>, String> visit(String dst, FullQuery.Comp e) {
		Pair<List<PSM>, String> n1 = e.l.accept(dst, this);
		Pair<List<PSM>, String> n2 = e.r.accept(n1.second, this);
		List<PSM> ret = new LinkedList<>(n1.first);
		ret.addAll(n2.first);
		return new Pair<>(ret, n2.second);
	}

	@Override
	public Pair<List<PSM>, String> visit(String src, FullQuery.Delta e) {
		String dst = next();
		List<PSM> ret = new LinkedList<>();
		Mapping F0 = e.F;

		ret.addAll(PSMGen.makeTables(dst, F0.source, false));
		ret.addAll(PSMGen.delta(F0, src, dst));
		ret.addAll(PSMGen.guidify(dst, F0.source));
		return new Pair<>(ret, dst);
	}

	@Override
	public Pair<List<PSM>, String> visit(String src, FullQuery.Sigma e) {
		String dst = next();
		List<PSM> ret = new LinkedList<>();
		Mapping F0 = e.F;

		ret.addAll(PSMGen.makeTables(dst, F0.target, false));
		try {
			ret.addAll(PSMGen.sigma(F0, dst, src));
		} catch (Exception ex) {
			// ex.printStackTrace();
			ret.addAll(PSMGen.SIGMA(F0, dst, src));
		}
		// ret.addAll(PSMGen.dropTables(next, F0.target));
		ret.addAll(PSMGen.guidify(dst, F0.target));
		return new Pair<>(ret, dst);
	}

	@Override
	public Pair<List<PSM>, String> visit(String src, FullQuery.Pi e) {
		try {
			String dst = next();
			List<PSM> ret = new LinkedList<>();
			Mapping F0 = e.F;

			ret.addAll(PSMGen.makeTables(dst, F0.target, false));
			ret.addAll(PSMGen.pi(F0, src, dst).first);

			// ret.addAll(PSMGen.dropTables(next, F0.target));
			// ret.addAll(PSMGen.guidify(dst, F0.target));
			return new Pair<>(ret, dst);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	@Override
	public List<PSM> visit(String dst, Squash e) {
		List<PSM> ret = new LinkedList<>();

		InstExp s = prog.insts.get(e.src);
		Signature ty = s.type(prog).toSig(prog);
	
		for (Node n : ty.nodes) {
			ret.add(new InsertSQL(dst + "_" + n.string, PSMGen
					.compose(e.src + "_" + n.string + "_squash", e.src + "_" + n.string + "_subst"), "c0", "c1"));
		}

		return ret;
	}

	// src and dst will be guidified, hence, must apply that subst here
	@Override
	public List<PSM> visit(String env, TransExp.External e) {
		List<PSM> ret = new LinkedList<>();
		Signature sig = prog.insts.get(e.src).type(prog).toSig(prog);
		ret.addAll(PSMGen.makeTables(e.name, sig, false));

		for (Node n : sig.nodes) {
			ret.add(new InsertSQL(e.name + "_" + n.string, PSMGen
					.compose(e.src + "_" + n.string + "_subst_inv", e.name + "_" + n.string, e.dst + "_" + n.string + "_subst"), "c0", "c1"));
		}
		return ret;
	}

	@Override
	public List<PSM> visit(String env, Return e) {
	//	String xxx = "return_temp_xxx";
		List<PSM> ret = new LinkedList<>();
		InstExp i1 = prog.insts.get(e.inst);
		if (i1 instanceof Delta) {
			String middle = ((Delta) i1).I;
			InstExp i2 = prog.insts.get(middle); // can't be null
			Mapping f = ((Delta) i1).F.toMap(prog);
			if (i2 instanceof Sigma) {
				Sigma input0 = ((Sigma) i2);
				String input = input0.I;
				for (Node n : f.source.nodes) {
					ret.add(new InsertSQL(env + "_" + n.string, PSMGen
							.compose(input + "_" + n.string, middle + "_" + f.nm.get(n) + "_subst", e.inst + "_" + n.string + "_subst"),
							"c0", "c1"));
				}
			} else if (i2 instanceof FullSigma) {
				FullSigma input0 = ((FullSigma) i2);
				String input = input0.I;
				for (Node n : f.source.nodes) {
					ret.add(new InsertSQL(env + "_" + n.string, PSMGen
							.compose(input + "_" + n.string, middle + "_" + n.string + "_e", e.inst + "_" + n.string + "_subst"),
							"c0", "c1"));
				}
			} else {
				throw new RuntimeException();
			}
		} else if (i1 instanceof Pi) {
			String middle = ((Pi) i1).I;
			InstExp i2 = prog.insts.get(middle); // can't be null
			Mapping f = ((Pi) i1).F.toMap(prog);
			if (i2 instanceof Delta) {
				Delta input0 = ((Delta) i2);
				String input = input0.I;
				for (Node n : f.target.nodes) {
					try {
						Map<String, Triple<Node, Node, Arr<Node, Path>>[]> colmap = PSMGen
								.pi(f, middle, e.inst).second;

						Triple<Node, Node, Arr<Node, Path>>[] col = colmap
								.get(n.string);
						
						if (col.length == 0) {
							LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
							Map<String, String> from = new HashMap<>();
							List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
							from.put("lim", e.inst + "_" + n.string + "_limit");
							from.put("middle", input + "_" + n.string);
							select.put("c0", new Pair<>("middle", "c0"));
							select.put("c1", new Pair<>("lim", "guid"));
							Flower flower = new Flower(select, from, where);
							ret.add(new InsertSQL(env + "_" + n.string, flower, "c0", "c1"));
							return ret;
						}

						//LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
					//	attrs.put("guid", PSM.VARCHAR());
						LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
						Map<String, String> from = new HashMap<>();
						from.put("lim", e.inst + "_" + n.string + "_limit");
						List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
						int i = 0;
						for (Triple<Node, Node, Arr<Node, Path>> col0 : col) {				
							from.put("c" + i + "_subst_inv", middle + "_"
									+ col0.second.string + "_subst_inv");
							where.add(new Pair<>(new Pair<>("lim", "c" + i),
									new Pair<>("c" + i + "_subst_inv", "c0")));
						//	attrs.put("c" + i, PSM.VARCHAR());
							i++;					
						}

				//		if (col.length > 1) {
							for (int j = 0; j < col.length; j++) {
								if (col[j].third.arr.equals(f.target.toCategory2().second.of(new Path(f.target, n)).arr)) {
								where.add(new Pair<>(new Pair<>("c" + 0
										+ "_subst_inv", "c1"), new Pair<>("c"
										+ j + "_subst_inv", "c1")));
								}
							}
							
							select.put("c" + 0, new Pair<>("c" + 0
									+ "_subst_inv", "c1"));
							select.put("c1", new Pair<>("lim", "guid"));

					//	}

//						ret.add(new CreateTable(xxx, attrs, false));
						Flower flower = new Flower(select, from, where);
						ret.add(new InsertSQL(env + "_" + n.string, flower, "c0", "c1"));
					} catch (FQLException fe) {
						fe.printStackTrace();
						throw new RuntimeException(fe.getMessage());
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			throw new RuntimeException();
		}
		return ret;
	}

	@Override
	public List<PSM> visit(String env, Coreturn e) {
		String xxx = "coreturn_temp_xxx";

		List<PSM> ret = new LinkedList<>();
		InstExp i1 = prog.insts.get(e.inst);
		if (i1 instanceof Sigma) {
			String middle = ((Sigma) i1).I;
			InstExp i2 = prog.insts.get(middle); // can't be null
			Mapping f = ((Sigma) i1).F.toMap(prog);
			if (i2 instanceof Delta) {
			for (Node n : f.target.nodes) {
			List<Flower> u = new LinkedList<>();
					for (Node m : f.source.nodes) {
						if (!f.nm.get(m).equals(n)) {
							continue;
						}
						u.add(new CopyFlower(middle + "_" + m.string
								+ "_subst_inv", "c0", "c1"));
					}
					ret.add(new SimpleCreateTable(xxx, PSM.VARCHAR(), false));
					ret.add(new InsertSQL(xxx, new Union(u), "c0", "c1"));

					ret.add(new InsertSQL(env + "_" + n.string, PSMGen
							.compose(e.inst + "_" + n.string + "_subst_inv", xxx), "c0", "c1"));

					ret.add(new DropTable(xxx));
				}
			}
		} else if (i1 instanceof FullSigma) {
			String middle = ((FullSigma) i1).I;
			InstExp i2 = prog.insts.get(middle); // can't be null
			Mapping f = ((FullSigma) i1).F.toMap(prog);
			if (i2 instanceof Delta) {
				ret.add(new FullSigmaCounit(f, ((Delta) i2).I, middle, e.inst, env));
			} else {
				throw new RuntimeException();
			}
		} else if (i1 instanceof Delta) {
			String middle = ((Delta) i1).I;
			InstExp i2 = prog.insts.get(middle); // can't be null
			Mapping f = ((Delta) i1).F.toMap(prog);
			if (i2 instanceof Pi) {
				Pi input0 = ((Pi) i2);
				String input = input0.I;
				try {
					Map<String, Triple<Node, Node, Arr<Node, Path>>[]> colmap = PSMGen
							.pi(f, input, middle).second;
					for (Node m : f.source.nodes) {
						Node n = f.nm.get(m);
						Triple<Node, Node, Arr<Node, Path>>[] col = colmap
								.get(n.string);
		
						Triple<Node, Node, Arr<Node, Path>> toFind = new Triple<>(
								n, m, new Arr<>(
										new Path(f.target, n), n, n));
						int i = 0;
						boolean found = false;
						for (Triple<Node, Node, Arr<Node, Path>> cand : col) {
							if (cand.equals(toFind)) {
								found = true;
								Map<String, String> from = new HashMap<>();
								from.put("lim", middle + "_" + n + "_limit");
								LinkedHashMap<String, Pair<String, String>> select = new LinkedHashMap<>();
								select.put("c0", new Pair<>("lim", "guid"));
								select.put("c1", new Pair<>("lim", "c" + i));
								List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();
								Flower flower = new Flower(select, from, where);
								ret.add(new SimpleCreateTable(xxx, PSM
										.VARCHAR(), false));
								ret.add(new InsertSQL(xxx, flower, "c0", "c1"));

								ret.add(new InsertSQL(
										env + "_" + m,
										PSMGen.compose(e.inst + "_" + m + "_subst_inv", xxx), "c0", "c1"));
								ret.add(new DropTable(xxx));
								break;
							}
							i++;
						}
						if (!found) {
							throw new RuntimeException();
						}
					}
				} catch (FQLException fe) {
					fe.printStackTrace();
					throw new RuntimeException(fe.getMessage());
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			throw new RuntimeException();
		}

		return ret;
	}

	@Override
	public List<PSM> visit(String env, TransEval e) {
		List<PSM> ret = new LinkedList<>();
		
		InstExp k = prog.insts.get(e.inst);
		Times t = (Times) k;
		InstExp v = prog.insts.get(t.a);
		Exp i = (Exp) v;
		
		ret.add(new PSMEval(env, i.a, i.b, t.a, e.inst, t.type(prog).toSig(prog)));
				
		//e.inst is a^b*b
		//t.a is a^b
		//t.b is b
		//i.a is a
		//i.b is b
				
		return ret;
	}

	@Override
	public List<PSM> visit(String env, TransCurry e) {
		List<PSM> ret = new LinkedList<>();
		
		Signature sig = prog.insts.get(e.inst).type(prog).toSig(prog);
				
		Pair<String, String> k = prog.transforms.get(e.trans).type(prog);
		Times t = (Times) prog.insts.get(k.first);

		ret.add(new PSMCurry(env, t.a, e.inst, e.trans, k.first, k.second, t.b, sig));
		
		return ret;
	}

	@Override
	public List<PSM> visit(String env, TransIso e) {
		List<PSM> ret = new LinkedList<>();
		
		Signature sig = prog.insts.get(e.l).type(prog).toSig(prog);
		
		ret.add(new PSMIso(e.lToR, env, e.l, e.r, sig));
		
		return ret;
	}

	@Override
	public List<PSM> visit(String env, Bool e) {
		List<PSM> ret = new LinkedList<>();

		SigExp.Const sigX = prog.insts.get(e.unit).type(prog).toConst(prog); //.toSig(prog);
		
		Signature sig = sigX.toSig(prog);
		
		Triple<Const, Map<Node, Map<Object, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>>, Map<Node, Map<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Object>>> kkk = Relationalizer.terminal(prog, sigX);
			
		ret.add(new PSMBool(e.bool, e.unit, e.prop, sig, env, kkk.first, kkk.second, kkk.third));
		
		return ret;
	}

	@Override
	public List<PSM> visit(String env, Chi e) {
		List<PSM> ret = new LinkedList<>();

		Signature sig = prog.insts.get(e.prop).type(prog).toSig(prog);

		TransExp t = prog.transforms.get(e.trans);
		Pair<String, String> k = t.type(prog);
		
		ret.add(new PSMChi(sig, env, k.first, k.second, e.prop, e.trans));
		
		return ret;
	}

	@Override
	public List<PSM> visit(String env, UnChi e) {
		List<PSM> ret = new LinkedList<>();

		Signature sig = prog.insts.get(e.a).type(prog).toSig(prog);
		
		for (Node n : sig.nodes) {
			ret.add(new InsertSQL(env + "_" + n.string, new CopyFlower(e.a + "_trans_" + n, "c0", "c1"), "c0", "c1"));
		}
		
		return ret;
	}

	@Override
	public Pair<List<PSM>, Object> visit(String env, Kernel e) {
		List<PSM> ret = new LinkedList<>();

		TransExp t = prog.transforms.get(e.trans);
		Pair<String, String> k = t.type(prog);
	
		Signature sig = prog.insts.get(k.first).type(prog).toSig(prog);

		ret.add(new PSMUnChi(sig, env, k.first, k.second, e.trans));
		
		return new Pair<>(ret, new Object());
	}

	@Override
	public List<PSM> visit(String env, Not e) {
		List<PSM> ret = new LinkedList<>();
		
		InstExp p = prog.insts.get(e.prop);
		Signature sig = p.type(prog).toSig(prog);
		
		ret.add(new PSMNot(sig, env, e.prop));

		return ret;
	}
	
	@Override
	public List<PSM> visit(String env, And e) {
		List<PSM> ret = new LinkedList<>();
		
		Times pr = (Times) prog.insts.get(e.prop);
		Signature sig = pr.type(prog).toSig(prog);
		
		ret.add(new PSMAnd(sig, env, e.prop, pr.a, "and"));

		return ret;
	}


	@Override
	public List<PSM> visit(String env, Or e) {
		List<PSM> ret = new LinkedList<>();
		
		Times pr = (Times) prog.insts.get(e.prop);
		Signature sig = pr.type(prog).toSig(prog);
		
		ret.add(new PSMAnd(sig, env, e.prop, pr.a, "or"));

		return ret;
	}
	
	@Override
	public List<PSM> visit(String env, Implies e) {
		List<PSM> ret = new LinkedList<>();
		
		Times pr = (Times) prog.insts.get(e.prop);
		Signature sig = pr.type(prog).toSig(prog);
		
		ret.add(new PSMAnd(sig, env, e.prop, pr.a, "implies"));

		return ret;
	}

	@Override
	public Pair<List<PSM>, Object> visit(String env, Step e) {
		List<PSM> ret = new LinkedList<>();
		
		ret.add(new PSMStep(env, e.I, e.m.toMap(prog), e.n.toMap(prog)));


		//: drops
		
		return new Pair<>(ret, new Object());		
	}



}

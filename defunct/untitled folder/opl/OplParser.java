package catdata.opl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jparsec.Parser;
import org.jparsec.Parser.Reference;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Terminals.Identifier;
import org.jparsec.Terminals.IntegerLiteral;
import org.jparsec.Terminals.StringLiteral;
import org.jparsec.functors.Tuple3;
import org.jparsec.functors.Tuple4;
import org.jparsec.functors.Tuple5;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.Triple;
import catdata.Util;
import catdata.fpql.XExp.XSchema;
import catdata.ide.DefunctGlobalOptions;
import catdata.opl.OplExp.OplApply;
import catdata.opl.OplExp.OplChaseExp;
import catdata.opl.OplExp.OplColim;
import catdata.opl.OplExp.OplDelta;
import catdata.opl.OplExp.OplDelta0;
import catdata.opl.OplExp.OplDistinct;
import catdata.opl.OplExp.OplEval;
import catdata.opl.OplExp.OplFlower;
import catdata.opl.OplExp.OplGraph;
import catdata.opl.OplExp.OplGround;
import catdata.opl.OplExp.OplId;
import catdata.opl.OplExp.OplInst;
import catdata.opl.OplExp.OplInst0;
import catdata.opl.OplExp.OplJavaInst;
import catdata.opl.OplExp.OplMapping;
import catdata.opl.OplExp.OplPivot;
import catdata.opl.OplExp.OplPragma;
import catdata.opl.OplExp.OplPres;
import catdata.opl.OplExp.OplPresTrans;
import catdata.opl.OplExp.OplPushout;
import catdata.opl.OplExp.OplPushoutBen;
import catdata.opl.OplExp.OplPushoutSch;
import catdata.opl.OplExp.OplSCHEMA0;
import catdata.opl.OplExp.OplSat;
import catdata.opl.OplExp.OplSchema;
import catdata.opl.OplExp.OplSchemaProj;
import catdata.opl.OplExp.OplSetInst;
import catdata.opl.OplExp.OplSetTrans;
import catdata.opl.OplExp.OplSig;
import catdata.opl.OplExp.OplSigma;
import catdata.opl.OplExp.OplUberSat;
import catdata.opl.OplExp.OplUnSat;
import catdata.opl.OplExp.OplUnion;
import catdata.opl.OplExp.OplVar;
import catdata.opl.OplQuery.Agg;
import catdata.opl.OplQuery.Block;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public class OplParser {

	private static final Parser<Integer> NUMBER = IntegerLiteral.PARSER
			.map(Integer::valueOf);

	private static final String[] ops = new String[] { ",", ".", ";", ":", "{", "}", "(",
			")", "=", "->", "+", "*", "^", "|", "?", "@" };

	private static final String[] res = new String[] { "agg", "arith", "distinct", "graph", "nodes", "ed", "tables",
			"chase", "with", "max", "insert", "into", "select", "and", "sql", 
			"ID", "colimit" , "imports", "pragma", "options", "union",
			"pushoutBen", "PUSHOUT", "pivot", "DELTA", "return", "coreturn",
			"pushout", "return", "keys", "INSTANCE", "SCHEMA", "obsEqualities",
			"pathEqualities", "implications", "apply", "id", "query", "edges",
			"for", "entitiesAndAttributes", "instance", "entities",
			"attributes", "types", "schema", "as", "where", "select", "from",
			"flower", "SATURATE", "transpres", "unsaturate", "sigma",
			"saturate", "presentation", "generators", "mapping", "delta",
			"eval", "theory", "model", "sorts", "symbols", "equations",
			"forall", "transform", "javascript" };

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	private static final Parser<Void> IGNORED = Parsers.or(
			Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT,
			Scanners.WHITESPACES).skipMany();

	private static final Parser<?> TOKENIZER = Parsers.or(
			(Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), (Parser<?>) Identifier.TOKENIZER,
			(Parser<?>) IntegerLiteral.TOKENIZER);

	private static Parser<?> term(String... names) {
		return RESERVED.token(names);
	}

	private static Parser<?> ident() {
		return Parsers.or(StringLiteral.PARSER,
				Identifier.PARSER);
	}

	private static final Parser<?> program = program().from(TOKENIZER, IGNORED);
	private static final Parser<?> exp = exp().from(TOKENIZER, IGNORED);

	
	private static Parser<?> program() {
		return Parsers.tuple(decl().source().peek(), decl()).many();
	}

	private static Parser<?> oplTerm() {
		Reference ref = Parser.newReference();
		Parser<?> app = Parsers.tuple(string(), term("("),
				ref.lazy().sepBy(term(",")), term(")"));
		Parser<?> app2 = Parsers.tuple(term("("), ref.lazy(), string(),
				ref.lazy(), term(")"));
		Parser<?> a = Parsers.or(new Parser[] { app, app2,
				string().sepBy1(term(".")) });
		ref.set(a);
		return a;
	}

	private static Parser<?> oplSequent() {
		Parser<?> p1 = Parsers.tuple(ident(), term(":"), ident());
		Parser<?> z = Parsers.longest(p1, ident());
		Parser<?> p = z.sepBy(term(","));
		Parser a = Parsers.tuple(term("forall"), p, term("."));
		Parser retX = Parsers.tuple(a.optional(), oplTerm());
		return retX;
	}

	private static Parser<?> oplEq() {
		Parser<?> p1 = Parsers.tuple(ident(), term(":"), ident());
		Parser<?> z = Parsers.longest(p1, ident());
		Parser<?> p = z.sepBy(term(","));
		Parser<?> q = Parsers.tuple(oplTerm(), term("="), oplTerm());
		Parser a = Parsers.tuple(term("forall"), p, term("."));
		Parser retX = Parsers.tuple(a.optional(), q);
		return retX;
	}
	
	private static Parser<?> sql() {
		Parser p = Parsers.tuple(term("insert"), term("into"), ident(), block2());
		Parser p2 = p.sepBy(term(";"));
		return Parsers.tuple(term("sql"), p2.between(term("{"), term("}")).followedBy(term(":")), ident(), term("->"), ident());
	}

	private static Parser<?> oplImpl() {
		Parser<?> p1 = Parsers.tuple(ident(), term(":"), ident());
		Parser<?> z = Parsers.longest(p1, ident());
		Parser<?> p = z.sepBy(term(","));
		Parser<?> q = Parsers.tuple(oplTerm(), term("="), oplTerm()).sepBy(
				term(","));
		Parser<?> zz = Parsers.tuple(q, term("->"), q);
		Parser a = Parsers.tuple(term("forall"), p, term("."));
		Parser retX = Parsers.tuple(a.optional(), zz);
		return retX;
	}

	private static Parser<?> exp() {
		Reference ref = Parser.newReference();

		Parser<?> theory = theory();
		Parser<?> model = model();
		Parser<?> trans = trans();
		Parser<?> trans_pres = trans_pres();
		Parser<?> eval = Parsers.tuple(term("eval"), ident(), oplTerm());
		Parser<?> java = java();
		Parser<?> mapping = mapping();
		Parser<?> delta = Parsers.tuple(term("delta"), ident(), ident());
		Parser<?> sigma = Parsers.tuple(term("sigma"), ident(), ident());
		Parser<?> presentation = presentation();
		Parser<?> sat = Parsers.tuple(term("saturate"), ident());
		Parser<?> DELTA = Parsers.tuple(term("DELTA"), ident());
		Parser<?> ubersat = Parsers.tuple(term("SATURATE"), ident(), ident());
		Parser<?> unsat = Parsers.tuple(term("unsaturate"), ident());
		Parser<?> flower = flower();
		Parser<?> schema = schema();
		Parser<?> projE = Parsers.tuple(term("entities"), ident());
		Parser<?> projA = Parsers.tuple(term("attributes"), ident());
		Parser<?> projT = Parsers.tuple(term("types"), ident());
		Parser<?> projEA = Parsers
				.tuple(term("entitiesAndAttributes"), ident());
		Parser<?> inst = Parsers.tuple(term("instance"), ident(), ident(),
				ident());
		Parser<?> query = query();
		Parser<?> idQ = Parsers.tuple(term("id"), ident());
		Parser<?> ID = Parsers.tuple(term("ID"), ident());
		Parser<?> apply = Parsers.tuple(term("apply"), ident(), ident());
		Parser<?> SCHEMA = SCHEMA();
		Parser<?> INST = INSTANCE();
		Parser<?> pushout = Parsers.tuple(term("pushout"), ident(), ident());
		Parser<?> pushoutSch = Parsers.tuple(term("PUSHOUT"), ident(), ident());
		Parser<?> pushoutBen = Parsers.tuple(term("pushoutBen"), ident(),
				ident());
		Parser<?> pivot = Parsers.tuple(term("pivot"), ident());
		Parser<?> union = Parsers.tuple(term("union"), ident(), term("{"), ident().many(), term("}"));
		Parser<?> pragma = pragma();
		Parser<?> colim = Parsers.tuple(term("colimit"), ident(), ident());
		Parser<?> sql = sql();
		Parser<?> model2 = model2();
		Parser<?> graph = graph();
		Parser<?> distinct = Parsers.tuple(term("distinct"), ident());
		Parser<?> chase = Parsers.tuple(term("chase"), ident(), Parsers.tuple(term("with"), term("{"), ident().sepBy(term(",")), term("}")), term("max"), IntegerLiteral.PARSER);
		Parser<?> arith = Parsers.tuple(term("arith"), NUMBER);
		
		Parser<?> a = Parsers.or(arith, distinct, graph, model2, sql, chase, ID, colim, pragma, union, pushoutBen,
				pushoutSch, pivot, DELTA, pushout, INST, SCHEMA, apply, idQ,
				query, projEA, inst, schema, projE, projA, projT, flower,
				ubersat, sigma, sat, unsat, presentation, delta, mapping,
				theory, model, eval, trans, trans_pres, java);
		ref.set(a);

		return a;
	}

	private static Parser<?> trans_pres() {
		Parser<?> q = Parsers.tuple(term("("), ident(), term(","), oplTerm(),
				term(")")).sepBy(term(","));
		Parser<?> p = Parsers.tuple(ident(), term("->"), term("{"), q,
				term("}"));
		Parser<?> foo = Parsers.tuple(section("imports", ident()).optional(), section("sorts", p));
		return Parsers.tuple(term("transpres").followedBy(term("{")), foo,
				Parsers.tuple(term("}").followedBy(term(":")), ident(),
						term("->"), ident()));
	}

	private static Parser<?> pragma() {
		Parser<?> p = Parsers.tuple(StringLiteral.PARSER, term("="),
				StringLiteral.PARSER);
		Parser<?> foo = section("options", p);
		return Parsers.tuple(term("pragma"), term("{"), foo, (term("}")));
	}

	private static Parser<?> trans() {
		Parser<?> q = Parsers.tuple(term("("), string(), term(","), string(),
				term(")")).sepBy(term(","));
		Parser<?> p = Parsers.tuple(ident(), term("->"), term("{"), q,
				term("}"));
		Parser<?> foo = section("sorts", p);
		return Parsers.tuple(term("transform").followedBy(term("{")), foo,
				Parsers.tuple(term("}").followedBy(term(":")), ident(),
						term("->"), ident()));
	}

	private static Parser<?> mapping() {
		Parser<?> q = Parsers.tuple(ident(), term("->"), oplSequent());
		Parser<?> p = Parsers.tuple(ident(), term("->"), ident());
		Parser<?> foo = Parsers.tuple(
				section("imports", ident()).optional(),
				section("sorts", p),
				section("symbols", q));
		return Parsers.tuple(term("mapping").followedBy(term("{")), foo,
				Parsers.tuple(term("}").followedBy(term(":")), ident(),
						term("->"), ident()));
	}

	private static Parser<?> theory() {
		Parser<?> q = Parsers.tuple(ident(), Parsers.tuple(term("@"), NUMBER)
				.optional());

		Parser<?> z1 = Parsers.longer(
				Parsers.tuple(ident().sepBy(term(",")), term("->"), ident()),
				ident());

		Parser<?> p = Parsers.tuple(q.sepBy1(term(",")), term(":"), z1);
		Parser<?> foo = Parsers.tuple(
				section("imports", ident()).optional(),
				section("sorts", ident()).optional(),
				section("symbols", p).optional(), 
				section("equations", oplEq()).optional(), 
				section("implications", oplImpl()).optional());
		return Parsers.tuple(Parsers.constant("theory"), Parsers.between(
				term("theory").followedBy(term("{")), foo, term("}")));
	}

	private static Parser<?> SCHEMA() {
		Parser<?> q = Parsers.tuple(ident(), Parsers.tuple(term("@"), NUMBER)
				.optional());

		Parser<?> z1 = Parsers.longer(
				Parsers.tuple(ident().sepBy(term(",")), term("->"), ident()),
				ident());
		Parser<?> p = Parsers.tuple(q.sepBy1(term(",")), term(":"), z1);
		Parser<?> foo = Parsers.tuple(section("entities", ident()),
				section("edges", p), section("attributes", p),
				section("pathEqualities", oplEq()),
				section("obsEqualities", oplEq()));
		
		Parser<?> bar = Parsers.tuple(section("imports", ident()).optional(), foo);
		
		return Parsers.tuple(term("SCHEMA").followedBy(term("{")), bar,
				term("}").followedBy(term(":")), ident());
	}

	private static Parser<?> presentation() {
		Parser<?> q = Parsers.tuple(ident(), Parsers.tuple(term("@"), NUMBER)
				.optional());
		Parser<?> p = Parsers.tuple(q.sepBy1(term(",")), term(":"), ident());
		Parser<?> foo = Parsers.tuple(Parsers.always(),
				section("generators", p),
				section("equations", oplEq()));
		return Parsers.tuple(term("presentation").followedBy(term("{")), foo,
				term("}").followedBy(term(":")), ident());
	}

	private static Parser<?> INSTANCE() {
		Parser<?> q = Parsers.tuple(ident(), Parsers.tuple(term("@"), NUMBER)
				.optional());
		Parser<?> p = Parsers.tuple(q.sepBy1(term(",")), term(":"), ident());
		Parser<?> foo = Parsers.tuple(section("imports", ident()).optional(), 
				section("generators", p),
				section("equations", oplEq()));
		return Parsers.tuple(term("INSTANCE").followedBy(term("{")), foo,
				term("}").followedBy(term(":")), ident());
	}

	private static Parser<?> schema() {
		Parser<?> foo = section("entities", ident());
		return Parsers.tuple(term("schema").followedBy(term("{")), foo,
				term("}").followedBy(term(":")), ident());
	}
	
	private static Parser<?> graph() {
		Parser<?> nodes = section("nodes", ident());
		Parser<?> edges = section("edges", Parsers.tuple(ident(), term(":"), ident(), term("->"), ident()));
		return Parsers.tuple(term("graph"), term("{"), nodes, edges,
				term("}"));
	}

	private static Parser<?> java() {
		Parser<?> q = Parsers.tuple(ident(), term("->"), string());
		Parser<?> foo = section("symbols", q);
		return Parsers.tuple(term("javascript").followedBy(term("{")), foo,
				Parsers.tuple(term("}").followedBy(term(":")), ident()));
	}

	private static Parser<?> model() {
		Parser<?> p = Parsers.tuple(ident(), term("->"), Parsers.between(
				term("{"), string().sepBy(term(",")), term("}")));
		Parser<?> y = Parsers.between(term("("), string().sepBy(term(",")),
				term(")"));
		Parser<?> z = Parsers.tuple(term("("), y, term(","), string(),
				term(")"));
		Parser<?> q = Parsers.tuple(ident(), term("->"),
				Parsers.between(term("{"), z.sepBy(term(",")), term("}")));
		Parser<?> foo = Parsers.tuple(section("sorts", p),
				section("symbols", q));
		return Parsers.tuple(term("model").followedBy(term("{")), foo,
				Parsers.tuple(term("}").followedBy(term(":")), ident()));
	}
	
	private static Parser<?> model2() {
		Parser<?> p = Parsers.tuple(ident(), term("->"), Parsers.between(
				term("{"), ident().sepBy(term(",")), term("}")));
		
		Parser<?> z = Parsers.tuple(term("("), ident(), term(","), ident(),
				term(")"));

		Parser<?> q = Parsers.tuple(ident(), term("->"), Parsers.between(
				term("{"), z.sepBy(term(",")), term("}")));
		
		Parser<?> foo = Parsers.tuple(section("entities", p),
				section("edges", q), section("attributes", q));
		return Parsers.tuple(term("tables").followedBy(term("{")), foo,
				Parsers.tuple(term("}").followedBy(term(":")), ident()));
	}


	public static XSchema toCatConst(Object y) {
		List<String> nodes = new LinkedList<>();
		List<Triple<String, String, String>> arrows = new LinkedList<>();
		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();

		Tuple3 s = (Tuple3) y;

		Tuple3 nodes0 = (Tuple3) s.a;
		Tuple3 arrows0 = (Tuple3) s.b;
		Tuple3 eqs0 = (Tuple3) s.c;

		List nodes1 = (List) nodes0.b;
		List arrows1 = (List) arrows0.b;
		List eqs1 = (List) eqs0.b;

		for (Object o : nodes1) {
			nodes.add((String) o);
		}

		for (Object o : arrows1) {
			Tuple5 x = (Tuple5) o;
			arrows.add(new Triple<>((String) x.a, (String) x.c, (String) x.e));
		}
		for (Object o : eqs1) {
			Tuple3 x = (Tuple3) o;
			List<String> l1 = (List<String>) x.a;
			List<String> l2 = (List<String>) x.c;
			eqs.add(new Pair<>(l1, l2));
		}
		XSchema c = new XSchema(nodes, arrows, eqs);
		return c;
	}

	private static Parser<?> decl() {
		Parser p1 = Parsers.tuple(ident(), term("="), exp());

		return Parsers.or(p1);
	}

	public static Parser<?> section2(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p, term(";"));
	}

	public static OplTerm parse_term(Map m, String s) {
		Object o = oplTerm().from(TOKENIZER, IGNORED).parse(s);
		return toTerm(null, consts(m), o, false);
	}

	private static boolean sugarForNat = false;

	public static OplExp exp(String s) {
		return toExp(exp.parse(s));
	}

		
	public static Program<OplExp> program(String s) {
		List<Triple<String, Integer, OplExp>> ret = new LinkedList<>();
		List decls = (List) program.parse(s);

		sugarForNat = false;
		for (Object d : decls) {
			org.jparsec.functors.Pair pr = (org.jparsec.functors.Pair) d;
			Tuple3 decl = (Tuple3) pr.b;

			toProgHelper(pr.a.toString(), s, ret, decl);
		}
		sugarForNat = false;

		return new Program<>(ret, null);
	}

	private static void toProgHelper(String txt, String s,
			List<Triple<String, Integer, OplExp>> ret,
			 Tuple3 decl) {
		int idx = s.indexOf(txt);
		if (idx < 0) {
			throw new RuntimeException();
		}

		String name = decl.a.toString();
		ret.add(new Triple<>(name, idx, toExp(decl.c)));
	}

	private static OplExp toTheory(Object o) {
		Tuple5 t = (Tuple5) o;

		List<String> imports = t.a == null ? new LinkedList<>() : (List<String>) ((org.jparsec.functors.Pair) t.a).b;
		Tuple3 a = (Tuple3) t.b;
		Tuple3 b = (Tuple3) t.c;
		Tuple3 c = (Tuple3) t.d;
		Tuple3 d = (Tuple3) t.e;

		Set<String> sorts = a == null ? new HashSet<>() : new HashSet<>(
                (Collection<String>) a.b);

		List<Tuple3> symbols0 = b == null ? new LinkedList<>()
				: (List<Tuple3>) b.b;
		List<org.jparsec.functors.Pair> equations0 = c == null ? new LinkedList<>()
				: (List<org.jparsec.functors.Pair>) c.b;
		List<org.jparsec.functors.Pair> implications0 = new LinkedList<>();
		if (d != null) {
			implications0 = (List<org.jparsec.functors.Pair>) d.b;
		}
		Map<String, Pair<List<String>, String>> symbols = new HashMap<>();
		Map<String, Integer> prec = new HashMap<>();
		for (Tuple3 x : symbols0) {
			String dom;
			List<String> args;
			if (x.c instanceof Tuple3) {
				Tuple3 zzz = (Tuple3) x.c;
				args = (List<String>) zzz.a;
				dom = (String) zzz.c;
			} else {
				dom = (String) x.c;
				args = new LinkedList<>();
			}

			List<org.jparsec.functors.Pair> name0s = (List<org.jparsec.functors.Pair>) x.a;
			for (org.jparsec.functors.Pair name0 : name0s) {
				// org.jparsec.functors.Pair name0 =
				// (org.jparsec.functors.Pair) x.a;
				String name = (String) name0.a;

				if (name0.b != null) {
					org.jparsec.functors.Pair zzz = (org.jparsec.functors.Pair) name0.b;
					Integer i = (Integer) zzz.b;
					prec.put(name, i);
				}

				if (symbols.containsKey(name)) {
					throw new DoNotIgnore("Duplicate symbol " + name);
				}
				symbols.put(name, new Pair<>(args, dom));
			}
		}

		if (sorts.contains("Nat") && symbols.keySet().contains("zero")
				&& symbols.keySet().contains("succ") && DefunctGlobalOptions.debug.opl.opl_desugar_nat) {
			sugarForNat = true;
		}

		List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> equations = new LinkedList<>();
		for (org.jparsec.functors.Pair<Tuple3, Tuple3> x : equations0) {
			List<Tuple3> fa = x.a == null ? new LinkedList<>()
					: (List<Tuple3>) x.a.b;
			OplCtx<String, String> ctx = toCtx(fa);
			Tuple3 eq = x.b;
			OplTerm lhs = toTerm(ctx.names(), consts(symbols), eq.a, false);
			OplTerm rhs = toTerm(ctx.names(), consts(symbols), eq.c, false);
			equations.add(new Triple<>(ctx, lhs, rhs));
		}

		List<Triple<OplCtx<String, String>, List<Pair<OplTerm<String, String>, OplTerm<String, String>>>, List<Pair<OplTerm<String, String>, OplTerm<String, String>>>>> implications = new LinkedList<>();
		for (org.jparsec.functors.Pair<Tuple3, Tuple3> x : implications0) {
			List<Tuple3> fa = x.a == null ? new LinkedList<>()
					: (List<Tuple3>) x.a.b;
			OplCtx<String, String> ctx = toCtx(fa);
			Tuple3 eq = x.b;
			List<Tuple3> lhs0 = (List<Tuple3>) eq.a;
			List<Tuple3> rhs0 = (List<Tuple3>) eq.c;
			List<Pair<OplTerm<String, String>, OplTerm<String, String>>> lhs = new LinkedList<>();
			List<Pair<OplTerm<String, String>, OplTerm<String, String>>> rhs = new LinkedList<>();
			for (Tuple3 obj : lhs0) {
				OplTerm lhsX = toTerm(ctx.names(), consts(symbols), obj.a,
						false);
				OplTerm rhsX = toTerm(ctx.names(), consts(symbols), obj.c,
						false);
				lhs.add(new Pair<>(lhsX, rhsX));
			}
			for (Tuple3 obj : rhs0) {
				OplTerm lhsX = toTerm(ctx.names(), consts(symbols), obj.a,
						false);
				OplTerm rhsX = toTerm(ctx.names(), consts(symbols), obj.c,
						false);
				rhs.add(new Pair<>(lhsX, rhsX));
			}
			implications.add(new Triple<>(ctx, lhs, rhs));
		}

		
		OplSig ret = new OplSig<>(new VIt(), prec, sorts, symbols, equations,
				implications);
		ret.imports = new HashSet<>(imports);
		return ret;
	}

	private static OplExp toSCHEMA(Object ox) {		
		Tuple4 oy = (Tuple4) ox;
		String ts = (String) oy.d;

		org.jparsec.functors.Pair newobj = (org.jparsec.functors.Pair) oy.b;

		List<String> imports = newobj.a == null ? new LinkedList<>() : (List<String>) ((org.jparsec.functors.Pair) newobj.a).b;
		
		
		Tuple5 t = (Tuple5) newobj.b;

		Tuple3 a = (Tuple3) t.a;
		Tuple3 b = (Tuple3) t.b;
		Tuple3 c = (Tuple3) t.c;
		Tuple3 d = (Tuple3) t.d;
		Tuple3 e = (Tuple3) t.e;
		
		Set<String> sorts = a == null ? new HashSet<>() : new HashSet<>(
                (Collection<String>) a.b);

		List<Tuple3> symbolsE0 = b == null ? new LinkedList<>()
				: (List<Tuple3>) b.b;
		List<Tuple3> symbolsA0 = c == null ? new LinkedList<>()
				: (List<Tuple3>) c.b;

		List<org.jparsec.functors.Pair> equationsE0 = c == null ? new LinkedList<>()
				: (List<org.jparsec.functors.Pair>) d.b;
		List<org.jparsec.functors.Pair> equationsA0 = c == null ? new LinkedList<>()
				: (List<org.jparsec.functors.Pair>) e.b;

		Map<String, Pair<List<String>, String>> symbolsE = new HashMap<>();
		Map<String, Pair<List<String>, String>> symbolsA = new HashMap<>();
		Map<String, Pair<List<String>, String>> symbolsEA = new HashMap<>();
		Map<String, Integer> prec = new HashMap<>();
		for (Tuple3 x : symbolsE0) {
			String dom;
			List<String> args;
			if (x.c instanceof Tuple3) {
				Tuple3 zzz = (Tuple3) x.c;
				args = (List<String>) zzz.a;
				dom = (String) zzz.c;
			} else {
				dom = (String) x.c;
				args = new LinkedList<>();
			}

			List<org.jparsec.functors.Pair> name0s = (List<org.jparsec.functors.Pair>) x.a;
			for (org.jparsec.functors.Pair name0 : name0s) {
				String name = (String) name0.a;

				if (name0.b != null) {
					org.jparsec.functors.Pair zzz = (org.jparsec.functors.Pair) name0.b;
					Integer i = (Integer) zzz.b;
					prec.put(name, i);
				}

				if (symbolsE.containsKey(name)) {
					throw new DoNotIgnore("Duplicate symbol " + name);
				}
				symbolsE.put(name, new Pair<>(args, dom));
				symbolsEA.put(name, new Pair<>(args, dom));
			}
		}
		for (Tuple3 x : symbolsA0) {
			String dom;
			List<String> args;
			if (x.c instanceof Tuple3) {
				Tuple3 zzz = (Tuple3) x.c;
				args = (List<String>) zzz.a;
				dom = (String) zzz.c;
			} else {
				dom = (String) x.c;
				args = new LinkedList<>();
			}

			List<org.jparsec.functors.Pair> name0s = (List<org.jparsec.functors.Pair>) x.a;
			for (org.jparsec.functors.Pair name0 : name0s) {
				String name = (String) name0.a;

				if (name0.b != null) {
					org.jparsec.functors.Pair zzz = (org.jparsec.functors.Pair) name0.b;
					Integer i = (Integer) zzz.b;
					prec.put(name, i);
				}

				if (symbolsA.containsKey(name)) {
					throw new DoNotIgnore("Duplicate symbol " + name);
				}
				symbolsA.put(name, new Pair<>(args, dom));
				symbolsEA.put(name, new Pair<>(args, dom));
			}
		}

		// /////////////

		List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> equationsE = new LinkedList<>();
		List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> equationsA = new LinkedList<>();
		for (org.jparsec.functors.Pair<Tuple3, Tuple3> x : equationsE0) {
			List<Tuple3> fa = x.a == null ? new LinkedList<>()
					: (List<Tuple3>) x.a.b;
			OplCtx<String, String> ctx = toCtx(fa);
			Tuple3 eq = x.b;
			OplTerm lhs = toTerm(ctx.names(), consts(symbolsEA), eq.a, false);
			OplTerm rhs = toTerm(ctx.names(), consts(symbolsEA), eq.c, false);
			equationsE.add(new Triple<>(ctx, lhs, rhs));
		}
		for (org.jparsec.functors.Pair<Tuple3, Tuple3> x : equationsA0) {
			List<Tuple3> fa = x.a == null ? new LinkedList<>()
					: (List<Tuple3>) x.a.b;
			OplCtx<String, String> ctx = toCtx(fa);
			Tuple3 eq = x.b;
			OplTerm lhs = toTerm(ctx.names(), consts(symbolsEA), eq.a, false);
			OplTerm rhs = toTerm(ctx.names(), consts(symbolsEA), eq.c, false);
			equationsA.add(new Triple<>(ctx, lhs, rhs));
		}

		OplSCHEMA0 ret = new OplSCHEMA0(prec, sorts, symbolsE, symbolsA,
				equationsE, equationsA, ts);
		ret.imports = new HashSet<>(imports);
		return ret;
	}

	private static Collection<String> consts(
			Map<String, Pair<List<String>, String>> symbols) {
		Set<String> ret = new HashSet<>();
		for (String k : symbols.keySet()) {
			Pair<List<String>, String> v = symbols.get(k);
			if (v.first.isEmpty()) {
				ret.add(k);
			}
		}
		return ret;
	}

	private static OplExp toINSTANCE(Object o) {
		if (!o.toString().contains("INSTANCE")) {
			throw new RuntimeException();
		}
		OplPres ex = toPresentation(o);
		OplInst0 ret = new OplInst0(ex);
		Tuple4 t = (Tuple4) o; 
		Tuple3 e = (Tuple3) t.b;
		List<String> imports = e.a == null ? new LinkedList<>() : (List<String>) ((org.jparsec.functors.Pair) e.a).b;
		ret.imports = new HashSet<>(imports);
		return ret;
	}

	private static OplPres toPresentation(Object o) {
		Tuple4 t = (Tuple4) o;

		Tuple3 e = (Tuple3) t.b;
//		List<String> imports = e.a == null ? new LinkedList<>() : (List<String>) ((Tuple3)e.a).b;
		
		String yyy = (String) t.d;

		org.jparsec.functors.Pair b = (org.jparsec.functors.Pair) e.b;
		org.jparsec.functors.Pair c = (org.jparsec.functors.Pair) e.c;

		List<Tuple3> symbols0 = (List<Tuple3>) b.b;
		List<Tuple4> equations0 = (List<Tuple4>) c.b;

		Map<String, String> symbols = new HashMap<>();
		Map<String, Integer> prec = new HashMap<>();
		for (Tuple3 x : symbols0) {
			String dom = (String) x.c;

			List<org.jparsec.functors.Pair> name0s = (List<org.jparsec.functors.Pair>) x.a;
			for (org.jparsec.functors.Pair name0 : name0s) {
				String name = (String) name0.a;

				if (name0.b != null) {
					org.jparsec.functors.Pair zzz = (org.jparsec.functors.Pair) name0.b;
					Integer i = (Integer) zzz.b;
					prec.put(name, i);
				}

				if (symbols.containsKey(name)) {
					throw new DoNotIgnore("Duplicate symbol " + name);
				}
				symbols.put(name, dom);
			}
		} 

		List<Pair<OplTerm<Chc<String, String>, String>, OplTerm<Chc<String, String>, String>>> equations = new LinkedList<>();
		for (org.jparsec.functors.Pair<Tuple3, Tuple3> x : equations0) {
			if (x.a != null) {
				throw new DoNotIgnore(
						"Cannot have universally quantified equations in presentations");
			}
			List<Tuple3> fa = new LinkedList<>();
			OplCtx<String, String> ctx = toCtx(fa);
			Tuple3 eq = x.b;
			OplTerm lhs = toTerm(ctx.names(), symbols.keySet(), eq.a, true);
			OplTerm rhs = toTerm(ctx.names(), symbols.keySet(), eq.c, true);
			equations.add(new Pair<>(lhs, rhs));
		}

		OplPres ret = new OplPres<>(prec, yyy,
				null, symbols, equations);
		return ret;
	}

	private static OplExp toSchema(Object o) {
		Tuple4 t = (Tuple4) o;

		org.jparsec.functors.Pair e = (org.jparsec.functors.Pair) t.b;

		String yyy = (String) t.d;

		List<String> symbols0 = (List<String>) e.b;

		return new OplSchema<String, String, String>(yyy, new HashSet<>(
				symbols0));
	}

	private static OplExp toModel(Object o) {
		if (!o.toString().contains("model")) {
			throw new RuntimeException();
		}
		Tuple3 t = (Tuple3) o;

		org.jparsec.functors.Pair b = (org.jparsec.functors.Pair) t.b;
		org.jparsec.functors.Pair c = (org.jparsec.functors.Pair) t.c;

		Tuple3 y = (Tuple3) b.a;
		List<Tuple3> sorts = (List<Tuple3>) y.b;
		Map<String, Set<String>> sorts0 = new HashMap<>();
		for (Tuple3 x : sorts) {
			String s = x.a.toString();
			List<String> s0 = (List<String>) x.c;
			if (sorts0.containsKey(s)) {
				throw new DoNotIgnore("Duplicate sort: " + s);
			}
			Set<String> s1 = new HashSet<>(s0);
			if (s1.size() != s0.size()) {
				throw new DoNotIgnore("Duplicate member: " + s0);
			}
			sorts0.put(s, s1);
		}

		Map<String, Map<List<String>, String>> symbols0 = new HashMap<>();
		Tuple3 z = (Tuple3) b.b;
		List<Tuple3> q = (List<Tuple3>) z.b;
		for (Tuple3 r : q) {
			List<Tuple5> u = (List<Tuple5>) r.c;
			String fname = (String) r.a;
			if (symbols0.containsKey(fname)) {
				throw new DoNotIgnore("Duplicte symbol " + fname);
			}
			Map<List<String>, String> toadd = new HashMap<>();
			for (Tuple5 e : u) {
				List<String> args = (List<String>) e.b;
				String ret = (String) e.d;
				if (toadd.containsKey(args)) {
					throw new DoNotIgnore("Duplicate argument at " + args);
				}
				toadd.put(args, ret);
			}
			symbols0.put(fname, toadd);
		}
		return new OplSetInst(sorts0, symbols0, c.b.toString());
	}
	
	private static OplExp toModel2(Object o) {
		if (!o.toString().contains("tables")) {
			throw new RuntimeException();
		}
		Tuple3 t = (Tuple3) o;

		Tuple3 b = (Tuple3) t.b;
		org.jparsec.functors.Pair c = (org.jparsec.functors.Pair) t.c;

		Tuple3 y = (Tuple3) b.a;
		List<Tuple3> sorts = (List<Tuple3>) y.b;
		Map<String, List<String>> sorts0 = new HashMap<>();
		for (Tuple3 x : sorts) {
			String s = x.a.toString();
			List<String> s0 = (List<String>) x.c;
			if (sorts0.containsKey(s)) {
				throw new DoNotIgnore("Duplicate sort: " + s);
			}
			sorts0.put(s, s0);
		}

		Map<String, Map<String, String>> symbols0 = new HashMap<>();
		Tuple3 z = (Tuple3) b.b;
		List<Tuple3> q = (List<Tuple3>) z.b;
		
		Tuple3 z0 = (Tuple3) b.c;
		q.addAll((Collection<? extends Tuple3>) z0.b);
		
		for (Tuple3 r : q) {
			List<Tuple5> u = (List<Tuple5>) r.c;
			String fname = (String) r.a;
			if (symbols0.containsKey(fname)) {
				throw new DoNotIgnore("Duplicate symbol " + fname);
			}
			Map<String, String> toadd = new HashMap<>();
			for (Tuple5 e : u) {
				String args = (String) e.b;
				String ret = (String) e.d;
				if (toadd.containsKey(args)) {
					throw new DoNotIgnore("Duplicate argument at " + args);
				}
				toadd.put(args, ret);
			}
			symbols0.put(fname, toadd);
		}
		return new OplGround(sorts0, symbols0, c.b.toString());
	}

	private static OplTerm toTerm(Collection vars, Collection consts, Object a,
			boolean suppressError) {
		if (a instanceof List) {
			List<String> aa = (List<String>) a;
			if (aa.isEmpty()) {
				throw new RuntimeException();
			}
			OplTerm head = toTerm(vars, consts, aa.get(0), suppressError);
			// List<String> bb = new LinkedList<>(aa); Collections.reverse(list)
			for (int j = 1; j < aa.size(); j++) {
				head = new OplTerm(aa.get(j), Collections.singletonList(head));
			}
			return head;
		}
		if (a instanceof String) {
			String a0 = (String) a;
			try {
				int i = Integer.parseInt(a0);
				if (sugarForNat) {
					return OplTerm.natToTerm(i); 
				}
			} catch (Exception e) {
			}

			if (vars != null && vars.contains(a0)) {
				return new OplTerm(a0);
			} else if (vars != null && !vars.contains(a0) || consts != null && consts.contains(a0)) {
				return new OplTerm(a0, new LinkedList<>());
			} else if (consts == null || vars == null) {
				return new OplTerm(a0);
			} else if (suppressError) {
				return new OplTerm(a0, new LinkedList<>());
			}
			throw new DoNotIgnore(a
					+ " is neither a bound variable nor a (0-ary) constant ");
		}
		if (a instanceof Tuple5) {
			Tuple5 t = (Tuple5) a;
			String f = (String) t.c;
			List<OplTerm> l0 = new LinkedList<>();
			l0.add(toTerm(vars, consts, t.b, suppressError));
			l0.add(toTerm(vars, consts, t.d, suppressError));
			return new OplTerm(f, l0);
		}

		Tuple3 t = (Tuple3) a;
		String f = (String) t.a;
		List<Object> l = (List<Object>) t.c;
		List<OplTerm> l0 = l.stream()
				.map(x -> toTerm(vars, consts, x, suppressError))
				.collect(Collectors.toList());
		return new OplTerm(f, l0);

	}

	private static OplCtx<String, String> toCtx(List<Tuple3> fa) {
		List<Pair<String, String>> ret = new LinkedList<>();
		if (fa == null) {
			return new OplCtx<>();
		}
		for (Object tt : fa) {
			if (tt instanceof Tuple3) {
				Tuple3 t = (Tuple3) tt;
				ret.add(new Pair<>(t.a.toString(), t.c.toString()));
			} else {
				ret.add(new Pair<>((String) tt, null));
			}
		}
		return new OplCtx<>(ret);
	}

	private static OplExp toChase(Tuple5 t) {
		//		Parser<?> chase = Parsers.tuple(term("chase"), ident(), Parsers.tuple(term("with"), term("{"), ident().sepBy(term(","), term("}")), term("max"), ident()));
		String I = (String) t.b;
		//t.c is (with { ... })
		Integer i0 = Integer.parseInt((String) t.e);
		
		Tuple4 x = (Tuple4) t.c;
		
		List<String> l = (List<String>) x.c;
		
		return new OplChaseExp(i0, I, new LinkedList<>(l));
	}
	
	private static OplExp toExp(Object c) {
		if (c instanceof String) {
			return new OplVar((String) c);
		}
		
		if (c instanceof Tuple5) {
			Tuple5 p = (Tuple5) c;
			if (p.a.toString().equals("chase")) {
				return toChase(p);
			} else if (p.a.toString().equals("graph")) {
				return toGraph((Tuple3) p.c, (Tuple3)p.d);
			} 
		}


		if (c instanceof Tuple4) { 
			Tuple4 p = (Tuple4) c;
			if (p.a.toString().startsWith("instance")) {
				return new OplInst((String) p.b, (String) p.c, (String) p.d);
			}  else if (p.a.toString().startsWith("pragma")) {
				return toPragma(p.c);
			} else if (p.a.toString().startsWith("union")) {
				return new OplUnion((List<String>) p.d, (String)p.b);
			}
		}

		if (c instanceof Tuple3) {
			Tuple3 p = (Tuple3) c;
			if (p.a.toString().equals("SATURATE")) {
				return new OplUberSat((String) p.b, (String) p.c);
			} else if (p.a.toString().equals("apply")) {
				return new OplApply((String) p.b, (String) p.c);
			} else if (p.a.toString().equals("pushout")) {
				return new OplPushout((String) p.b, (String) p.c);
			} else if (p.a.toString().equals("PUSHOUT")) {
				return new OplPushoutSch((String) p.b, (String) p.c);
			} else if (p.a.toString().equals("pushoutBen")) {
				return new OplPushoutBen((String) p.b, (String) p.c);
			} else if (p.a.toString().equals("colimit")) {
				return new OplColim((String) p.c, (String) p.b);
			} 
		}

	

		if (c instanceof org.jparsec.functors.Pair) {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) c;
			if (p.a.toString().equals("theory")) {
				return toTheory(p.b);
			} else if (p.a.toString().equals("saturate")) {
				return new OplSat((String) p.b);
			} else if (p.a.toString().equals("unsaturate")) {
				return new OplUnSat((String) p.b);
			} else if (p.a.toString().equals("entities")) {
				return new OplSchemaProj((String) p.b, "E");
			} else if (p.a.toString().equals("attributes")) {
				return new OplSchemaProj((String) p.b, "A");
			} else if (p.a.toString().equals("types")) {
				return new OplSchemaProj((String) p.b, "T");
			} else if (p.a.toString().equals("entitiesAndAttributes")) {
				return new OplSchemaProj((String) p.b, "EA");
			} else if (p.a.toString().equals("id")) {
				return new OplId((String) p.b, "query");
			} else if (p.a.toString().equals("DELTA")) {
				return new OplDelta0((String) p.b);
			} else if (p.a.toString().equals("pivot")) {
				return new OplPivot((String) p.b);
			} else if (p.a.toString().equals("ID")) {
				return new OplId((String) p.b, "mapping");
			} else if (p.a.toString().equals("distinct")) {
				return new OplDistinct((String) p.b);
			} else if (p.a.toString().equals("arith")) {
				return arith(Integer.parseInt(p.b.toString()));
			}
 		}

		try {
			return toFlower(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
		}

		try {
			return toModel(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
		}
		
		try {
			return toModel2(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
			//ee.printStackTrace();
		}

		try {
			return toINSTANCE(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
		}

		try {
			return toSCHEMA(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
			 //ee.printStackTrace();
		}

		try {
			return toPresentation(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
		}

		try {
			return toTrans(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
		} catch (Exception ee) {
		}

		try {
			return toTrans_2(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
			// ee.printStackTrace();
		}

		try {
			return toEval(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
		}

		try {
			return toFDM(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
		}

		try {
			return toJava(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
		}

		try {
			return toMapping(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
			// ee.printStackTrace();
		}

		try {
			return toSchema(c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
			// ee.printStackTrace();
		}

		try {
			return toQuery((Tuple4) c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
			//ee.printStackTrace();
		}
		
		try {
			return toSql((Tuple5) c);
		} catch (DoNotIgnore de) {
			de.printStackTrace();
			throw new RuntimeException(de.getMessage());
		} catch (Exception ee) {
			//ee.printStackTrace();
		}

		throw new RuntimeException("Report this error to Ryan.  Details: " + c);
	}

	
	private static OplSig arith(Integer i) {
		Map<String, Pair<List<String>, String>> symbols = new LinkedHashMap<>();
		List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> equations = new LinkedList<>();
		
		List<String> l = new LinkedList<>();
		l.add("Nat");
		l.add("Nat");
		
		for (int a = 0; a < i; a++) {
			symbols.put(Integer.toString(a), new Pair<>(new LinkedList<>(), "Nat"));
			
			for (int b = 0; b < i; b++) {
				int plus = a+b;
				if (plus < i) {
					List<OplTerm<String,String>> args = new LinkedList<>();
					args.add(new OplTerm<>(Integer.toString(a), new LinkedList<>()));
					args.add(new OplTerm<>(Integer.toString(b), new LinkedList<>()));					
					OplTerm<String,String> lhs = new OplTerm<>("+", args);
					OplTerm<String,String> rhs = new OplTerm<>(Integer.toString(plus), new LinkedList<>());
					equations.add(new Triple<>(new OplCtx<>(), lhs, rhs));
				}
				
				int times = a*b;
				if (times < i) {
					List<OplTerm<String,String>> args = new LinkedList<>();
					args.add(new OplTerm<>(Integer.toString(a), new LinkedList<>()));
					args.add(new OplTerm<>(Integer.toString(b), new LinkedList<>()));					
					OplTerm<String,String> lhs = new OplTerm<>("*", args);
					OplTerm<String,String> rhs = new OplTerm<>(Integer.toString(times), new LinkedList<>());
					equations.add(new Triple<>(new OplCtx<>(), lhs, rhs));			
				}
			}
		}
		
		symbols.put("+", new Pair<>(l, "Nat"));
		symbols.put("*", new Pair<>(l, "Nat"));

		return new OplSig<>(new VIt(), new HashMap<>(), Collections.singleton("Nat"), symbols, equations);
	}
	
	private static OplExp toGraph(Tuple3 yyy,  Tuple3 xxx) {
		List<String> c = (List<String>) yyy.b;
		List<Tuple5> d = (List<Tuple5>) xxx.b;
		List<Triple<String, String, String>> l = new LinkedList<>();
		for (Tuple5 t : d) {
			l.add(new Triple<>((String)t.a, (String)t.c, (String)t.e));
		}
		
		return new OplGraph<>(c, l);
	}

	private static OplExp toMapping(Object c) {
		Tuple3 t = (Tuple3) c;

		Tuple3 aa = (Tuple3) t.b;
		Tuple3 a = (Tuple3) aa.b;
		Tuple3 b = (Tuple3) aa.c;
		// Tuple3 b = (Tuple3) t.c;
		
		List<String> imports = aa.a == null ? new LinkedList<>() : (List<String>) ((org.jparsec.functors.Pair) aa.a).b;
		

		List<Tuple3> sorts = (List<Tuple3>) a.b;
		List<Tuple3> symbols = (List<Tuple3>) b.b;

		Map<String, String> sorts0 = new HashMap<>();
		Map<String, Pair<OplCtx<String, String>, OplTerm<String, String>>> symbols0 = new HashMap<>();

		for (Tuple3 z : sorts) {
			String p = (String) z.a;
			if (sorts0.containsKey(p)) {
				throw new DoNotIgnore("Duplicate sort: " + p);
			}
			String q = (String) z.c;
			sorts0.put(p, q);
		}

		for (Tuple3 z : symbols) {
			String p = (String) z.a;
			if (sorts0.containsKey(p)) {
				throw new DoNotIgnore("Duplicate symbol: " + p);
			}
			org.jparsec.functors.Pair ppp = (org.jparsec.functors.Pair) z.c;
			Tuple3 q = (Tuple3) ppp.a;
			List<Tuple3> ctx = q == null ? new LinkedList<>()
					: (List<Tuple3>) q.b;
			List<Pair<String, String>> ctx0 = new LinkedList<>();
			Set<String> seen = new HashSet<>();
			for (Object uu : ctx) {
				String name;
				String type = null;
				if (uu instanceof Tuple3) {
					Tuple3 u = (Tuple3) uu;
					name = (String) u.a;
					type = (String) u.c;
				} else {
					name = (String) uu;
				}
				if (seen.contains(name)) {
					throw new DoNotIgnore("Duplicate var: " + name);
				}
				seen.add(name);
				ctx0.add(new Pair<>(name, type));
			}
			OplCtx ccc = new OplCtx<>(ctx0);
			symbols0.put(p,
					new Pair<>(ccc, toTerm(ccc.names(), null, ppp.b, false)));
		}

		Tuple4 x = (Tuple4) t.c;
		String src0 = (String) x.b;
		String dst0 = (String) x.d;
		OplMapping ret = new OplMapping(sorts0, symbols0, src0, dst0);
		ret.imports = new HashSet<>(imports);
		return ret;
	}

	//: pull this up into util
	public static class VIt implements Iterator<String> {

		VIt() {
		}

		public static final VIt vit = new VIt();

		static int i = 0;

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public String next() {
			return "_v" + (i++);
		}

	}

	public static class DoNotIgnore extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public DoNotIgnore(String string) {
			super(string);
		}

	}

	private static OplExp toJava(Object x) {
		Tuple3 t = (Tuple3) x;

		Tuple3 b = (Tuple3) t.b;
		List<Tuple3> l = (List<Tuple3>) b.b;
		Map<String, String> defs = new HashMap<>();
		for (Tuple3 k : l) {
			String f = (String) k.a;
			String body = (String) k.c;
			if (defs.containsKey(f)) {
				throw new DoNotIgnore("Duplicate symbol: " + f);
			}
			defs.put(f, body);
		}

		org.jparsec.functors.Pair c = (org.jparsec.functors.Pair) t.c;
		return new OplJavaInst(defs, (String) c.b);
	}

	private static OplExp toTrans(Object c) {
		Tuple3 t = (Tuple3) c;
		if (!t.a.toString().equals("transform")) {
			throw new RuntimeException();
		}

		Map<String, Map<String, String>> map = new HashMap<>();
		Tuple3 tb = (Tuple3) t.b;
		List<Tuple5> l = (List<Tuple5>) tb.b;

		for (Tuple5 x : l) {
			String name = (String) x.a;
			List<Tuple5> y = (List<Tuple5>) x.d;
			Map<String, String> m = new HashMap<>();
			for (Tuple5 z : y) {
				String xx = (String) z.b;
				String yy = (String) z.d;
				if (m.containsKey(xx)) {
					throw new DoNotIgnore("Duplicate argument: " + xx);
				}
				m.put(xx, yy);
			}
			if (map.containsKey(name)) {
				throw new DoNotIgnore("Duplicate sort: " + name);
			}
			map.put(name, m);
		}

		Tuple4 tc = (Tuple4) t.c;
		return new OplSetTrans(map, (String) tc.b, (String) tc.d);
	}

	private static OplExp toTrans_2(Object c) {
		Tuple3 t = (Tuple3) c;
		if (!t.a.toString().equals("transpres")) {
			throw new RuntimeException();
		}

		Map<String, Map<String, OplTerm>> map = new HashMap<>();
		
		org.jparsec.functors.Pair q = (org.jparsec.functors.Pair) t.b;
		List<String> imports = q.a == null ? new LinkedList<>() : (List<String>) ((org.jparsec.functors.Pair) q.a).b;
		
		
		Tuple3 tb = (Tuple3) q.b;
		List<Tuple5> l = (List<Tuple5>) tb.b;

		for (Tuple5 x : l) {
			String name = (String) x.a;
			List<Tuple5> y = (List<Tuple5>) x.d;
			Map<String, OplTerm> m = new HashMap<>();
			for (Tuple5 z : y) {
				String xx = (String) z.b;
				// OplTerm yy = toTermNoVars(z.d);
				OplTerm yy = toTerm(new HashSet<>(), new HashSet<>(), z.d,
						false);
				if (m.containsKey(xx)) {
					throw new DoNotIgnore("Duplicate argument: " + xx);
				}
				m.put(xx, yy);
			}
			if (map.containsKey(name)) {
				throw new DoNotIgnore("Duplicate sort: " + name);
			}
			map.put(name, m);
		}

		Tuple4 tc = (Tuple4) t.c;
		OplPresTrans ret = new OplPresTrans(map, (String) tc.b, (String) tc.d);
		ret.imports = new HashSet<>(imports);
		return ret;
	}

	private static OplExp toPragma(Object t) {
		Tuple3 tb = (Tuple3) t;
		List<Tuple3> l = (List<Tuple3>) tb.b;

		Map<String, String> map = new HashMap<>();
		for (Tuple3 x : l) {
			String k = (String) x.a;
			String v = (String) x.c;

			if (map.containsKey(k)) {
				throw new DoNotIgnore("Duplicate key: " + k);
			}
			map.put(k, v);
		}

		return new OplPragma(map);
	}

	private static OplExp toEval(Object c) {
		Tuple3 t = (Tuple3) c;
		if (!t.a.toString().equals("eval")) {
			throw new RuntimeException();
		}
		String i = (String) t.b;
		OplTerm r = toTerm(null, null, t.c, false);
		return new OplEval(i, r);
	}

	private static OplExp toFDM(Object c) {
		Tuple3 t = (Tuple3) c;
		String i = (String) t.b;
		String r = (String) t.c;

		if (t.a.toString().equals("delta")) {
			return new OplDelta(i, r);
		} else if (t.a.toString().equals("sigma")) {
			return new OplSigma(i, r);
		}
		throw new RuntimeException();

	}

	private static Parser<?> section(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p.sepBy(term(",")), term(";"));
	}

	private static Parser<?> string() {
		return Parsers.or(StringLiteral.PARSER,
				IntegerLiteral.PARSER, Identifier.PARSER);
	}

	private static Parser<?> flower() {
		Parser<?> from0 = Parsers.tuple(ident(), term("as"), ident()).sepBy(
				term(","));
		Parser<?> from = Parsers.tuple(term("from"), from0, term(";"));

		Parser<?> where0 = Parsers.tuple(oplTerm(), term("="), oplTerm())
				.sepBy(term(","));
		Parser<?> where = Parsers.tuple(term("where"), where0, term(";"));

		Parser<?> select0 = Parsers.tuple(oplTerm(), term("as"), ident())
				.sepBy(term(","));
		Parser<?> select = Parsers.tuple(term("select"), select0, term(";"));

		Parser p = Parsers.tuple(select, from, where);
		Parser ret = Parsers.tuple(term("flower"),
				p.between(term("{"), term("}")), ident());

		return ret;
	}

	private static OplFlower toFlower(Object c) {
		Tuple3 p = (Tuple3) c;
		// if (p.a.toString().equals("flower")) {
		String I = (String) p.c;
		Tuple3 q = (Tuple3) p.b;

		List s = (List) ((org.jparsec.functors.Pair) q.a).b; // list of tuple3 of (path, string)
		List f = (List) ((org.jparsec.functors.Pair) q.b).b; // list of tuple3 of (string, string)
		List w = (List) ((org.jparsec.functors.Pair) q.c).b; // list of tuple3 of (path, path)

		Map<String, OplTerm<String, String>> select = new HashMap<>();
		Map<String, String> from = new HashMap<>();
		List<Pair<OplTerm<String, String>, OplTerm<String, String>>> where = new LinkedList<>();

		Set<String> seen = new HashSet<>();

		for (Object o : f) {
			Tuple3 t = (Tuple3) o;
			String lhs = t.a.toString();
			String rhs = t.c.toString();
			if (seen.contains(rhs)) {
				throw new DoNotIgnore(
						"Duplicate AS name: "
								+ rhs
								+ " (note: AS names can't be used in the schema either)");
			}
			seen.add(rhs);
			from.put(rhs, lhs);
		}
		for (Object o : w) {
			Tuple3 t = (Tuple3) o;
			OplTerm lhs = toTerm(from.keySet(), null, t.a, false);
			OplTerm rhs = toTerm(from.keySet(), null, t.c, false);
			where.add(new Pair<>(rhs, lhs));
		}
		for (Object o : s) {
			Tuple3 t = (Tuple3) o;
			OplTerm lhs = toTerm(from.keySet(), null, t.a, false);
			String rhs = t.c.toString();
			if (seen.contains(rhs)) {
				throw new DoNotIgnore(
						"Duplicate AS name: "
								+ rhs
								+ " (note: AS names can't be used in the schema either)");
			}
			seen.add(rhs);
			select.put(rhs, lhs);
		}
		return new OplFlower<>(select, from, where, I);

	}
	
	private static Block<String, String, String, String, String, String> fromBlock2(
            Object o) {
		Tuple3<List, List, List> t = (Tuple3<List, List, List>) o;

		LinkedHashMap<String, String> from = new LinkedHashMap<>();
		Set<Pair<OplTerm<String, String>, OplTerm<String, String>>> where = new HashSet<>();
		Map<String, Chc<Agg<String, String, String, String, String, String>, OplTerm<String, String>>> attrs = new HashMap<>();
		Map<String, Pair<Object, Map<String, OplTerm<String, String>>>> edges = new HashMap<>();

		//from
		for (Object x : (Iterable) ((org.jparsec.functors.Pair) t.b).b) {
			org.jparsec.functors.Pair l = (org.jparsec.functors.Pair) x;
			String gen;
			String ty;
			if (l.b == null) {
				gen = (String) l.a;
				ty = (String) l.a;
			} else {
				org.jparsec.functors.Pair g = (org.jparsec.functors.Pair) l.b;
				gen = (String) g.b;
				ty = (String) l.a;
			}
			if (from.containsKey(gen)) {
				throw new DoNotIgnore("In from clause, duplicate for: " + gen);
			}
			from.put(gen, ty);				
		}

		//where
		//Object z = ((org.jparsec.functors.Pair)t.c).b;
		if (t.c != null) {
			for (Object x : (Iterable) ((org.jparsec.functors.Pair) t.c).b) {
				Tuple3 l = (Tuple3) x;
				where.add(new Pair(toTerm(from.keySet(), null, l.a, true), toTerm(
						from.keySet(), null, l.c, true)));
			}
		}
		
		//return
		for (Object x : (Iterable) ((org.jparsec.functors.Pair) t.a).b){
			Tuple3 l = (Tuple3) x;
			String dst = (String) l.c;
			if (attrs.containsKey(dst) || edges.containsKey(dst)) {
				throw new DoNotIgnore("In select clause, duplicate for: " + dst);
			}
			if (l.a instanceof Tuple3 && ((org.jparsec.functors.Pair) l.a).a.toString().equals("(")) {
				edges.put(dst,
						new Pair(null,
								fromBlockHelper2(from.keySet(), l.a)));				

			} else {
				attrs.put(dst, Chc.inRight(toTerm(from.keySet(), null, l.a, true)));				
			}
		}


		Block bl = new Block<>(from, where, attrs, edges);
		return bl;
	}

	@SuppressWarnings("unused")
	private static Agg<String, String, String, String, String, String> fromAgg(Collection vars, Collection consts, Object o,
			boolean suppressError) {
		
		Tuple5<Tuple4<?,String,String,?>, List<Tuple3<String,?,String>>, List<Tuple3<?,?,?>>, org.jparsec.functors.Pair<?,?>,?> t = (Tuple5<Tuple4<?, String, String, ?>, List<Tuple3<String, ?, String>>, List<Tuple3<?, ?, ?>>, org.jparsec.functors.Pair<?, ?>, ?>) o;

		LinkedHashMap<String, String> from = new LinkedHashMap<>();
		Set<Pair<OplTerm<String, String>, OplTerm<String, String>>> where = new HashSet<>();

		for (Object x : t.b) {
			Tuple3 l = (Tuple3) x;
			if (from.containsKey(l.a.toString())) {
				throw new RuntimeException("Duplicate for: " + l.a);
			}
			from.put(l.a.toString(), l.c.toString());
		}
		Set<String> allVars = new HashSet<>();
		allVars.addAll(vars);
		allVars.addAll(from.keySet());
		
		for (Object x : t.c) {
			Tuple3 l = (Tuple3) x;
			where.add(new Pair(toTerm(allVars, null, l.a, true), toTerm(
					allVars, null, l.c, true)));
		}
		
		OplTerm<String, String> att = toTerm(allVars, null, t.d.b, true); 
			
		return new Agg<>(t.a.b, t.a.c, from, where, att);
	}
	
	private static Block<String, String, String, String, String, String> fromBlock(
            Object o) {
		Tuple4<List, List, List, List> t = (Tuple4<List, List, List, List>) o;

		LinkedHashMap<String, String> from = new LinkedHashMap<>();
		Set<Pair<OplTerm<String, String>, OplTerm<String, String>>> where = new HashSet<>();
		Map<String, Chc<Agg<String, String, String, String, String, String>,OplTerm<String, String>>> attrs = new HashMap<>();
		Map<String, Pair<Object, Map<String, OplTerm<String, String>>>> edges = new HashMap<>();

		for (Object x : t.a) {
			Tuple3 l = (Tuple3) x;
			if (from.containsKey(l.a.toString())) {
				throw new RuntimeException("Duplicate for: " + l.a);
			}
			from.put(l.a.toString(), l.c.toString());
		}

		for (Object x : t.b) {
			Tuple3 l = (Tuple3) x;
			where.add(new Pair(toTerm(from.keySet(), null, l.a, true), toTerm(
					from.keySet(), null, l.c, true)));
		}

		for (Object x : t.c) {
			Tuple3 l = (Tuple3) x;
			if (attrs.containsKey(l.a.toString())) { 
				throw new RuntimeException("Duplicate for: " + l.a);
			}
			if (l.c.toString().contains("agg") && l.c.toString().contains("{") && l.c.toString().contains("return") && l.c.toString().contains("}")) {
				attrs.put(l.a.toString(), Chc.inLeft(fromAgg(from.keySet(), null, l.c, true))); 				
			} else {
				attrs.put(l.a.toString(), Chc.inRight(toTerm(from.keySet(), null, l.c, true))); 
			}
		}

		for (Object x : t.d) {
			Tuple5 l = (Tuple5) x;
			if (from.containsKey(l.a.toString())) {
				throw new RuntimeException("Duplicate for: " + l.a);
			}
			edges.put(l.a.toString(),
					new Pair(l.e.toString(),
							fromBlockHelper(from.keySet(), l.c)));
		}

		Block bl = new Block<>(from, where, attrs, edges);
		return bl;
	}

	// {b2=a1.f, b3=a1.f}
	private static Map<String, OplTerm<String, String>> fromBlockHelper(
            Set<String> vars, Object o) {
		List<Tuple3> l = (List<Tuple3>) o;
		Map<String, OplTerm<String, String>> ret = new HashMap<>();
		for (Tuple3 t : l) {
			if (ret.containsKey(t.a.toString())) {
				throw new DoNotIgnore("Duplicate column: " + t.a + "\n in " + o);
			}
			ret.put(t.a.toString(), toTerm(vars, null, t.c, true));
		}
		return ret;
	}

	private static Map<String, OplTerm<String, String>> fromBlockHelper2(
            Set<String> vars, Object o) {
		Tuple3 tx = (Tuple3) o;
		List<Tuple3> l = (List<Tuple3>) tx.b; //is token ( ?
		Map<String, OplTerm<String, String>> ret = new HashMap<>();
		for (Tuple3 t : l) {
			if (ret.containsKey(t.c.toString())) {
				throw new DoNotIgnore("Duplicate column: " + t.c + "\n in " + o);
			}
			ret.put(t.c.toString(), toTerm(vars, null, t.a, true));
		}
		return ret;
	}

	
	private static Map<Object, Pair<String, Block<String, String, String, String, String, String>>> fromBlocks(
            List l) {
		Map<Object, Pair<String, Block<String, String, String, String, String, String>>> ret = new HashMap<>();
		for (Object o : l) {
			Tuple5 t = (Tuple5) o;
			Block<String, String, String, String, String, String> b = fromBlock(t.c);
			ret.put(t.a.toString(), new Pair<>(t.e.toString(), b));
		}
		return ret;
	}
	
	private static Map<Object, Pair<String, Block<String, String, String, String, String, String>>> fromBlocks2(
            List l) {
		Map<Object, Pair<String, Block<String, String, String, String, String, String>>> ret = new HashMap<>();
		for (Object o : l) {
			Tuple4 t = (Tuple4) o;
			Block<String, String, String, String, String, String> b = fromBlock2(t.d);
			ret.put(t.c.toString(), new Pair<>(t.c.toString(), b));
		}
		return ret;
	}

	private static OplQuery<String, String, String, String, String, String> toQuery(
            Tuple4 o) {
		Map<Object, Pair<String, Block<String, String, String, String, String, String>>> blocks = fromBlocks((List) o.a);
		return new OplQuery<>(
				(String) o.b, (String) o.d, blocks);
	}
	
	private static OplQuery<String, String, String, String, String, String> toSql(
            Tuple5 o) {
		if (!o.a.toString().equals("sql")) {
			throw new RuntimeException();
		}
		
		Map<Object, Pair<String, Block<String, String, String, String, String, String>>> blocks = fromBlocks2((List) o.b);
		return new OplQuery<>(
				(String) o.c, (String) o.e, blocks);
	}
	private static Parser<?> block2() {
		Parser<?> fromAs = Parsers.tuple(ident(), Parsers.tuple(term("as"), ident()).optional());
		
		Parser<?> from = Parsers.tuple(term("from"), fromAs.sepBy(term(",")));
		
		Parser<?> where = Parsers.tuple(term("where"), Parsers.tuple(oplTerm(), term("="), oplTerm()).sepBy(term("and")));
		
		Parser<?> zzz = Parsers.tuple(term("("), Parsers.tuple(oplTerm(), term("as"), ident()).sepBy(term(",")), term(")"));
		
		Parser<?> retAs = Parsers.tuple(Parsers.or(zzz, oplTerm()), term("as"), ident());
		
		Parser<?> ret = Parsers.tuple(term("select"), retAs.sepBy(term(",")));
		
		Parser p = Parsers.tuple(ret, from, where.optional());
		return p;
	}
	
	
	private static Parser<?> agg() {
		Parser p1 = Parsers.tuple(ident(), term(":"), ident()).sepBy(term(","))
				.between(term("for"), term(";"));
		Parser p2 = Parsers.tuple(oplTerm(), term("="), oplTerm())
				.sepBy(term(",")).between(term("where"), term(";"));
		Parser p3 = Parsers.tuple(term("return"),oplTerm());
	
		Parser p0 = Parsers.tuple(term("agg"), ident(), ident(), term("{"));
		
		Parser p = Parsers.tuple(p0, p1, p2, p3, term("}"));
		return p;		
	}
	
	private static Parser<?> block() {
		Parser eee = Parsers.or(term("="), Parsers.tuple(term(":"), term("=")));
		Parser p1 = Parsers.tuple(ident(), term(":"), ident()).sepBy(term(","))
				.between(term("for"), term(";"));
		Parser p2 = Parsers.tuple(oplTerm(), term("="), oplTerm())
				.sepBy(term(",")).between(term("where"), term(";"));
		Parser p3 = Parsers.tuple(ident(), eee, Parsers.or(agg(), oplTerm()))
				.sepBy(term(",")).between(term("return"), term(";"));

		Parser q = Parsers.tuple(ident(), eee, oplTerm())
				.sepBy(term(",")).between(term("{"), term("}"));
		Parser a = Parsers.tuple(ident(), eee, q, term(":"), ident());
		Parser p4 = a.sepBy(term(",")).between(term("keys"), term(";"));

		Parser p = Parsers.tuple(p1, p2, p3, p4);
		return p.between(term("{"), term("}"));
	}

	private static Parser<?> query() {
		Parser eee = Parsers.or(term("="), Parsers.tuple(term(":"), term("=")));
		Parser p = Parsers.tuple(ident(), eee, block(), term(":"),
				ident());
		Parser p2 = p.sepBy(term(",")).between(term("{"), term("}"))
				.between(term("query"), term(":"));
		return Parsers.tuple(p2, ident(), term("->"), ident());
	}

}

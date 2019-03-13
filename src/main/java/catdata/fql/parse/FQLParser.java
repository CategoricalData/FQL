package catdata.fql.parse;

import java.util.*;

import catdata.fql.decl.FullQueryExp.Comp;
import catdata.fql.decl.FullQueryExp.Delta;
import catdata.fql.decl.FullQueryExp.Match;
import catdata.fql.decl.FullQueryExp.Pi;
import catdata.fql.decl.FullQueryExp.Sigma;
import catdata.fql.decl.FullQueryExp.Var;
import catdata.fql.decl.InstExp.Eval;
import catdata.fql.decl.InstExp.Exp;
import catdata.fql.decl.InstExp.External;
import catdata.fql.decl.InstExp.FullEval;
import catdata.fql.decl.InstExp.FullSigma;
import catdata.fql.decl.InstExp.Kernel;
import catdata.fql.decl.InstExp.One;
import catdata.fql.decl.InstExp.Plus;
import catdata.fql.decl.InstExp.Relationalize;
import catdata.fql.decl.InstExp.Step;
import catdata.fql.decl.InstExp.Times;
import catdata.fql.decl.InstExp.Two;
import catdata.fql.decl.InstExp.Zero;
import catdata.fql.decl.MapExp.Apply;
import catdata.fql.decl.MapExp.Case;
import catdata.fql.decl.MapExp.Curry;
import catdata.fql.decl.MapExp.FF;
import catdata.fql.decl.MapExp.Fst;
import catdata.fql.decl.MapExp.Id;
import catdata.fql.decl.MapExp.Inl;
import catdata.fql.decl.MapExp.Inr;
import catdata.fql.decl.MapExp.Iso;
import catdata.fql.decl.MapExp.Opposite;
import catdata.fql.decl.MapExp.Prod;
import catdata.fql.decl.MapExp.Snd;
import catdata.fql.decl.MapExp.Sub;
import catdata.fql.decl.MapExp.TT;
import catdata.fql.decl.SigExp.Union;
import catdata.fql.decl.SigExp.Unknown;
import catdata.fql.decl.TransExp.And;
import catdata.fql.decl.TransExp.Bool;
import catdata.fql.decl.TransExp.Chi;
import catdata.fql.decl.TransExp.Coreturn;
import catdata.fql.decl.TransExp.Implies;
import catdata.fql.decl.TransExp.Not;
import catdata.fql.decl.TransExp.Or;
import catdata.fql.decl.TransExp.Return;
import catdata.fql.decl.TransExp.Squash;
import catdata.fql.decl.TransExp.TransCurry;
import catdata.fql.decl.TransExp.TransEval;
import catdata.fql.decl.TransExp.TransIso;
import catdata.fql.decl.TransExp.UnChi;
import org.jparsec.Parser;
import org.jparsec.Parser.Reference;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Terminals.DecimalLiteral;
import org.jparsec.Terminals.Identifier;
import org.jparsec.Terminals.IntegerLiteral;
import org.jparsec.Terminals.StringLiteral;
import org.jparsec.Token;
import org.jparsec.functors.Tuple3;
import org.jparsec.functors.Tuple4;
import org.jparsec.functors.Tuple5;

import catdata.Pair;
import catdata.Triple;
import catdata.fql.decl.FQLProgram;
import catdata.fql.decl.FullQueryExp;
import catdata.fql.decl.InstExp;
import catdata.fql.decl.MapExp;
import catdata.fql.decl.QueryExp;
import catdata.fql.decl.SigExp;
import catdata.fql.decl.TransExp;
import catdata.fql.decl.FQLProgram.NewDecl;
import catdata.fql.decl.SigExp.Const;

/**
 * 
 * @author ryan
 * 
 *         Parser for FQL programs.
 */
@SuppressWarnings("deprecation")
public class FQLParser {

	
/*	static final Parser<Integer> NUMBER = Terminals.IntegerLiteral.PARSER
			.map(new Map<String, Integer>() {
				public Integer map(String s) {
					return Integer.valueOf(s);
				}
			}); */

	private static final String[] ops = new String[] { ",", ".", ";", ":", "{", "}", "(",
			")", "=", "->", "+", "*", "^", "|", "?" };

	private static final String[] res = new String[] { "not", "and", "or", "implies", "return", "coreturn", "opposite", "EVAL", "QUERY", "union",
			"subschema", "match", "drop", "nodes", "attributes", "enum",
			"ASWRITTEN", "schema", "transform", "dist1", "dist2", "arrows",
			"equations", "id", "delta", "sigma", "pi", "SIGMA", "eval", /* "eq" , */
			"relationalize", "external", "then", "query", "instance", "fst",
			"snd", "inl", "inr", "curry", "mapping", "eval", "void", "unit",
			"prop", "iso1", "iso2", "true", "false", "char", "kernel", "step" };

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT,
			Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	private static final Parser<?> TOKENIZER = Parsers.or(
			(Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), (Parser<?>) Identifier.TOKENIZER,
			(Parser<?>) DecimalLiteral.TOKENIZER,
			(Parser<?>) IntegerLiteral.TOKENIZER);
	
	private static Parser<?> term(String... names) {
		return RESERVED.token(names);
	}

	private static Parser<?> ident() {
		return Identifier.PARSER;
	}

	private static final Parser<?> program = program().from(TOKENIZER, IGNORED);

	private static Parser<?> program() {
		return Parsers
				.or(Parsers.tuple(schemaDecl().source().peek(), schemaDecl()),
						Parsers.tuple(instanceDecl().source().peek(),
								instanceDecl()),
						Parsers.tuple(mappingDecl().source().peek(),
								mappingDecl()),
						Parsers.tuple(enumDecl().source().peek(), enumDecl()),
						Parsers.tuple(fullQueryDecl().source().peek(),
								fullQueryDecl()),
						Parsers.tuple(queryDecl().source().peek(), queryDecl()),
						Parsers.tuple(transDecl().source().peek(), transDecl()),
						Parsers.tuple(dropDecl().source().peek(), dropDecl()))
				.many();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    private static Parser<?> schema() {
		Reference ref = Parser.newReference();

		Parser<?> plusTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("+"), ref.lazy()), term(")"));
		Parser<?> prodTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("*"), ref.lazy()), term(")"));
		Parser<?> expTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("^"), ref.lazy()), term(")"));
		Parser<?> unionTy = Parsers
				.between(term("("),
						Parsers.tuple(ref.lazy(), term("union"), ref.lazy()),
						term(")"));

		Parser<?> xxx = ident().sepBy(term(",")).between(term("{"), term("}"));

		Parser<?> op = Parsers.tuple(term("opposite"), ref.lazy());

		Parser<?> a = Parsers.or(term("void"),
				Parsers.tuple(term("unit"), xxx), plusTy, prodTy, expTy,
				unionTy, ident(), schemaConst(), op, term("?"));

		ref.set(a);

		return a;
	}

	private static Parser<?> enumDecl() {
		return Parsers.tuple(term("enum"), ident(), term("="), Parsers.between(
				term("{"), string().sepBy(term(",")), term("}")));
	}

	private static Parser<?> queryDecl() {
		return Parsers.tuple(term("query"), ident(), term("="), query());
	}

	private static Parser<?> fullQueryDecl() {
		return Parsers.tuple(term("QUERY"), ident(), term("="), fullQuery());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static Parser<?> query() {
		Reference ref = Parser.newReference();

		Parser p1 = Parsers.tuple(term("delta"), mapping());
		Parser p2 = Parsers.tuple(term("pi"), mapping());
		Parser p3 = Parsers.tuple(term("sigma"), mapping());
		Parser comp = Parsers.tuple(term("("), ref.lazy(), term("then"),
				ref.lazy(), term(")"));
	//	Parser zzz = Parsers.tuple(ident(), term(","), ident());
		//Parser yyy = Parsers.between(term("("), zzz, term(")"));
		//Parser xxx = Parsers
			//	.between(term("{"), yyy.sepBy(term(",")), term("}"));
	//	Parser mtch = Parsers.tuple(term("match"), xxx, schema(), schema(),
	//			Terminals.StringLiteral.PARSER);
		Parser ret = Parsers.or(Parsers.tuple(p1, p2, p3), comp, ident() /*, mtch */);

		ref.set(ret);

		return ret;
	}

	// add identity query

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static Parser<?> fullQuery() {
		Reference ref = Parser.newReference();

		Parser p1 = Parsers.tuple(term("delta"), mapping());
		Parser p2 = Parsers.tuple(term("pi"), mapping());
		Parser p3 = Parsers.tuple(term("SIGMA"), mapping());
		Parser comp = Parsers.tuple(term("("), ref.lazy(), term("then"),
				ref.lazy(), term(")"));
		Parser zzz = Parsers.tuple(ident(), term(","), ident());
		Parser yyy = Parsers.between(term("("), zzz, term(")"));
		Parser xxx = Parsers
				.between(term("{"), yyy.sepBy(term(",")), term("}"));
		Parser mtch = Parsers.tuple(term("match"), xxx, schema(), schema(),
				StringLiteral.PARSER);
		Parser ret = Parsers.or(p1, p2, p3, comp, ident(), mtch);

		ref.set(ret);

		return ret;
	}

	private static Parser<?> schemaDecl() {
		return Parsers.tuple(term("schema"), ident(), term("="), schema());
	}

	private static Parser<?> schemaConst() {
		Parser<?> p1 = ident();
		Parser<?> p2 = Parsers.tuple(ident(), term(":"), ident(), term("->"),
				ident());
		Parser<?> pX = Parsers.tuple(ident(), term(":"), ident(), term("->"),
				ident());
		Parser<?> p3 = Parsers.tuple(path(), term("="), path());
		Parser<?> foo = Parsers.tuple(section("nodes", p1), Parsers.or(
				section("attributes", p2),
				Parsers.tuple(term("attributes"),
						term("ASWRITTEN"), term(";"))), section("arrows", pX),
				section("equations", p3));
		return Parsers.between(term("{"), foo, term("}"));
	}

	private static int unknown_idx = 0;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static SigExp toSchema(Object o) {
		try {
			Tuple3<?, ?, ?> t = (Tuple3<?, ?, ?>) o;
			Token z = (Token) t.b;
			String y = z.toString();
            switch (y) {
                case "+":
                    return new SigExp.Plus(toSchema(t.a), toSchema(t.c));
                case "*":
                    return new SigExp.Times(toSchema(t.a), toSchema(t.c));
                case "^":
                    return new SigExp.Exp(toSchema(t.a), toSchema(t.c));
                case "union":
                    return new Union(toSchema(t.a), toSchema(t.c));
			default:
				break;
            }
		} catch (RuntimeException cce) {
		}

		try {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			if (p.a.toString().equals("unit")) {
				return new SigExp.One(new HashSet<>((Collection<String>) p.b));
			} else if (p.a.toString().equals("opposite")) {
				return new SigExp.Opposite(toSchema(p.b));
			}
		} catch (RuntimeException cce) {
		}

		try {
			if (o.toString().equals("void")) {
				return new SigExp.Zero();
			} else if (o.toString().equals("?")) {
				return new Unknown("?" + unknown_idx++);
			}
			
			throw new RuntimeException();
		} catch (RuntimeException cce) {
		}

		try {
			return toSchemaConst(o);
		} catch (RuntimeException cce) {
		}

		return new SigExp.Var(o.toString());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static SigExp toSchemaConst(Object y) {
		List<String> nodes = new LinkedList<>();
		List<Triple<String, String, String>> attrs = new LinkedList<>();
		List<Triple<String, String, String>> arrows = new LinkedList<>();
		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();

		Tuple4 s = (Tuple4) y;

		Tuple3 nodes0 = (Tuple3) s.a;
		Tuple3 attrs0 = (Tuple3) s.b;
		Tuple3 arrows0 = (Tuple3) s.c;
		Tuple3 eqs0 = (Tuple3) s.d;

		List nodes1 = (List) nodes0.b;
		List arrows1 = (List) arrows0.b;
		List eqs1 = (List) eqs0.b;

		for (Object o : nodes1) {
			nodes.add((String) o);
		}

		if (attrs0.b.toString().equals("ASWRITTEN")) {
			for (String k : nodes) {
				attrs.add(new Triple<>(k + "_att", k, "string"));
			}
		} else {
			List attrs1 = (List) attrs0.b;
			for (Object o : attrs1) {
				Tuple5 x = (Tuple5) o;
				attrs.add(new Triple<>((String) x.a, (String) x.c, (String) x.e));
			}
		}
		for (Object o : arrows1) {
			Tuple5 x = (Tuple5) o;
			arrows.add(new Triple<>((String) x.a, (String) x.c, (String) x.e));
		}
		for (Object o : eqs1) {
			Tuple3 x = (Tuple3) o;
			eqs.add(new Pair<>((List<String>) x.a, (List<String>) x.c));
		}
		Const c = new Const(nodes, attrs, arrows, eqs);
		return c;
	}

	private static Parser<?> transDecl() {
		return Parsers
				.tuple(term("transform"), ident(), term("="), transform());
	}

	private static Parser<?> dropDecl() {
		return Parsers.tuple(term("drop"), ident().many());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static Parser<?> transform() {
		Reference ref = Parser.newReference();

		Parser plusTy = Parsers.tuple(ident(), term("."), Parsers.between(
				term("("), Parsers.tuple(ref.lazy(), term("+"), ref.lazy()),
				term(")")));
		Parser prodTy = Parsers.tuple(ident(), term("."), Parsers.between(
				term("("), Parsers.tuple(ref.lazy(), term("*"), ref.lazy()),
				term(")")));
		Parser compTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("then"), ref.lazy()), term(")"));

		Parser a = Parsers.or(Parsers.tuple(term("external"), ident(), ident(), ident()),
				Parsers.tuple(ident(), term("."), term("char"), ident()),
				Parsers.tuple(ident(), term("."), term("kernel")),
				Parsers.tuple(ident(), term("."), term("unit"), ident()),
				Parsers.tuple(ident(), term("."), term("void"), ident()),
				Parsers.tuple(ident(), term("."), term("curry"), ident()),
				Parsers.tuple(ident(), term("."), term("fst")),
				Parsers.tuple(ident(), term("."), term("not")),
				Parsers.tuple(ident(), term("."), term("and")),
				Parsers.tuple(ident(), term("."), term("or")),
				Parsers.tuple(ident(), term("."), term("implies")),
				Parsers.tuple(ident(), term("."), term("return")),
				Parsers.tuple(ident(), term("."), term("coreturn")),
				Parsers.tuple(ident(), term("."), term("snd")),
				Parsers.tuple(ident(), term("."), term("eval")),
				Parsers.tuple(ident(), term("."), term("true"), ident()),
				Parsers.tuple(ident(), term("."), term("false"), ident()),
				Parsers.tuple(ident(), term("."), term("inl")),
				Parsers.tuple(ident(), term("."), term("inr")),
				Parsers.tuple(term("iso1"), ident(), ident()),
				Parsers.tuple(term("iso2"), ident(), ident()),
				Parsers.tuple(ident(), term("."), term("relationalize")),
				Parsers.tuple(term("delta"), ident(), ident(), ref.lazy()),
				Parsers.tuple(term("sigma"), ident(), ident(), ref.lazy()),
				Parsers.tuple(term("SIGMA"), ident(), ident(), ident()),
				Parsers.tuple(term("pi"), ident(), ident(), ref.lazy()),
				Parsers.tuple(term("relationalize"), ident(), ident(),
						ref.lazy()),
				// Parsers.tuple(term("apply"), sig(), sig()),
				// Parsers.tuple(term("curry"), ref.lazy()),
				// Parsers.tuple(term("eq"), sig()),
				Parsers.tuple(term("id"), ident()),
				// Parsers.tuple(term("dist1"), sig(), sig(), sig()),
				// Parsers.tuple(term("dist2"), sig(), sig(), sig()), compTy,
				compTy, plusTy, prodTy, ident(), transConst());

		ref.set(a);
		return a;
	}

	private static Parser<?> transConst() {
		Parser<?> p = Parsers.tuple(term("("), string(), term(","), string(),
				term(")")).sepBy(term(","));

		Parser<?> node = Parsers.tuple(ident(), term("->"),
				p.between(term("{"), term("}")));

		Parser<?> xxx = section("nodes", node);

		Parser<?> p1 = Parsers.tuple(
				Parsers.between(term("{"), xxx, term("}")), term(":"), ident(),
				term("->"), ident());

		return p1;
	}

	@SuppressWarnings({ "rawtypes" })
    private static TransExp toTransConst(Object decl, String t1, String t2) {

		List<Pair<String, List<Pair<Object, Object>>>> objs = new LinkedList<>();

		Tuple3 a0 = (Tuple3) decl;
		List b0 = (List) a0.b;
		for (Object o : b0) {
			Tuple3 z = (Tuple3) o;
			String p = (String) z.a;

			List<?> q = (List<?>) z.c;
			List<Pair<Object, Object>> l = new LinkedList<>();
			for (Object q0 : q) {
				Tuple5 q1 = (Tuple5) q0;
				l.add(new Pair<>(q1.b, q1.d));
			}

			objs.add(new Pair<>(p, l));
		}

		return new TransExp.Const(objs, t1, t2);

	}

	@SuppressWarnings("rawtypes")
    private static TransExp toTrans(Object o) {

		try {
			Tuple4 p = (Tuple4) o;
			String src = p.b.toString();
			String dst = p.c.toString();
			TransExp h = toTrans(p.d);
			String kind = p.a.toString();
            switch (kind) {
                case "delta":
                    return new TransExp.Delta(h, src, dst);
                case "pi":
                    return new TransExp.Pi(h, src, dst);
                case "sigma":
                    return new TransExp.Sigma(h, src, dst);
                case "relationalize":
                    return new TransExp.Relationalize(h, src, dst);
                default:
                    throw new RuntimeException(o.toString());
            }
		} catch (RuntimeException ex) {

		}
		
		try {
			Tuple4 p = (Tuple4) o;
			String src = p.b.toString();
			String dst = p.c.toString();
			String name = p.d.toString();
			String kind = p.a.toString();
            switch (kind) {
                case "external":
                    return new TransExp.External(src, dst, name);
                case "SIGMA":
                    return new TransExp.FullSigma(name, src, dst);
                default:
                    throw new RuntimeException(o.toString());
            }
		} catch (RuntimeException ex) {

		}

		try {
			Tuple4 p = (Tuple4) o;

			String obj = p.a.toString();
			String dst = p.d.toString();
			if (p.c.toString().equals("void")) {
				return new TransExp.FF(obj, dst);
			} else if (p.c.toString().equals("unit")) {
				return new TransExp.TT(obj, dst);
			} else if (p.c.toString().equals("curry")) {
				return new TransCurry(obj, dst);
			} else if (p.c.toString().equals("true")) {
				return new Bool(true, dst, obj);
			} else if (p.c.toString().equals("false")) {
				return new Bool(false, dst, obj);
			} else if (p.c.toString().equals("char")) {
				return new Chi(obj, dst);
			} 

		} catch (RuntimeException re) {

		}

		try {
			Tuple3 p = (Tuple3) o;

			Object p2 = p.b;
			Object p3 = p.c;
			Object o1 = p.a;
			String p1 = p.a.toString();

			if (p1.equals("iso1")) {
				return new TransIso(true, p2.toString(), p3.toString());
			} else if (p1.equals("iso2")) {
				return new TransIso(false, p2.toString(), p3.toString());
			} else if (p3.toString().equals("fst")) {
				return new TransExp.Fst(p1);
			} else if (p3.toString().equals("not")) {
				return new Not(p1);
		    } else if (p3.toString().equals("and")) {
				return new And(p1);
		    } else if (p3.toString().equals("or")) {
				return new Or(p1);
		    } else if (p3.toString().equals("implies")) {
				return new Implies(p1);
		    } else if (p3.toString().equals("eval")) {
				return new TransEval(p1);
			} else if (p3.toString().equals("relationalize")) {
				return new Squash(p1);
			} else if (p3.toString().equals("snd")) {
				return new TransExp.Snd(p1);
			} else if (p3.toString().equals("return")) {
				return new Return(p1);
			} else if (p3.toString().equals("coreturn")) {
				return new Coreturn(p1);
			} else if (p3.toString().equals("inl")) {
				return new TransExp.Inl(p1);
			} else if (p3.toString().equals("kernel")) {
				return new UnChi(p1);
			} else if (p3.toString().equals("inr")) {
				return new TransExp.Inr(p1);
			} else if (p2.toString().equals("then")) {
				return new TransExp.Comp(toTrans(o1), toTrans(p3));
			} else if (p3 instanceof Tuple3) {
				Tuple3 y = (Tuple3) p3;
				String x = y.b.toString();
                switch (x) {
                    case "+":
                        return new TransExp.Case(p1, toTrans(y.a), toTrans(y.c));
                    case "*":
                        return new TransExp.Prod(p1, toTrans(y.a), toTrans(y.c));
                    // } else if (x.equals("^")) {
                    // return new TransExp.(p1, toTrans(y.a), toTrans(y.c));
                    default:
                        throw new RuntimeException("foo");
                }
			}

		} catch (RuntimeException re) {
		}

		try {
			Tuple5 p = (Tuple5) o;

			Object p2 = p.c;
			Object p3 = p.e;
			Object o1 = p.a;
			return toTransConst(o1, p2.toString(), p3.toString());
		} catch (RuntimeException re) {
		}

		try {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			String p1 = p.a.toString();
			Object p2 = p.b;
			if (p1.equals("id")) {
				return new TransExp.Id(p2.toString());
			}
		} catch (RuntimeException re) {

		}

		if (o instanceof String) {
			return new TransExp.Var(o.toString());
		}

		throw new RuntimeException();
	}

	private static Parser<?> mappingDecl() {
		return Parsers.tuple(term("mapping"), ident(), term("="), mapping());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static Parser<?> mapping() {
		Reference ref = Parser.newReference();

		Parser plusTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("+"), ref.lazy()), term(")"));
		Parser prodTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("*"), ref.lazy()), term(")"));
		Parser compTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("then"), ref.lazy()), term(")"));

		Parser<?> xxx = ident().sepBy(term(",")).between(term("{"), term("}"));

		Parser a = Parsers.or(Parsers.tuple(term("unit"), xxx, schema()),
				Parsers.tuple(term("void"), schema()),
				Parsers.tuple(term("iso1"), schema(), schema()),
				Parsers.tuple(term("iso2"), schema(), schema()),
				Parsers.tuple(term("fst"), schema(), schema()),
				Parsers.tuple(term("snd"), schema(), schema()),
				Parsers.tuple(term("inl"), schema(), schema()),
				Parsers.tuple(term("inr"), schema(), schema()),
				Parsers.tuple(term("eval"), schema(), schema()),
				Parsers.tuple(term("opposite"), ref.lazy()),
				Parsers.tuple(term("curry"), ref.lazy()),
				//	Parsers.tuple(term("eq"), schema()),
				Parsers.tuple(term("id"), schema()),
				Parsers.tuple(term("subschema"), schema(), schema()),
				//	Parsers.tuple(term("dist1"), schema(), schema(), schema()),
				//	Parsers.tuple(term("dist2"), schema(), schema(), schema()),
				compTy, plusTy, prodTy, ident(), mappingConst());

		ref.set(a);
		return a;
	}

	private static Parser<?> mappingConst() {
		Parser<?> node = Parsers.tuple(ident(), term("->"), ident());
		Parser<?> arrow = Parsers.tuple(ident(), term("->"), path());


		Parser<?> xxx = Parsers.tuple(section("nodes", node),
				Parsers.or(section("attributes", node), Parsers.tuple(term("attributes"),
						term("ASWRITTEN"), term(";"))), section("arrows", arrow));

		
		Parser<?> p1 = Parsers.between(term("{"), xxx, term("}"));

		return Parsers.tuple(p1, term(":"), schema(), term("->"), schema());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static MapExp toMapConst(Object decl, SigExp t1, SigExp t2) {
		Tuple3 x = (Tuple3) decl;

		List<Pair<String, String>> objs = new LinkedList<>();
		List<Pair<String, String>> attrs = new LinkedList<>();
		List<Pair<String, List<String>>> arrows = new LinkedList<>();

		Tuple3 a = (Tuple3) x.a;
		Tuple3 b = (Tuple3) x.b;
		Tuple3 c = (Tuple3) x.c;
		List a0 = (List) a.b;
		for (Object o : a0) {
			Tuple3 z = (Tuple3) o;
			String p = (String) z.a;
			String q = (String) z.c;
			objs.add(new Pair<>(p, q));
		}

		
		if (b.b.toString().equals("ASWRITTEN")) {
			for (Pair<String, String> k : objs) {
				attrs.add(new Pair<>(k.first + "_att", k.second + "_att"));
			}
		} else {
		List b0 = (List) b.b;
		for (Object o : b0) {
			Tuple3 z = (Tuple3) o;
			String p = (String) z.a;
			String q = (String) z.c;
			attrs.add(new Pair<>(p, q));
		}
		}
		List c0 = (List) c.b;
		for (Object o : c0) {
			Tuple3 z = (Tuple3) o;
			String p = (String) z.a;
			List<String> q = (List<String>) z.c;
			arrows.add(new Pair<>(p, q));
		}

		try {
		MapExp.Const ret = new MapExp.Const(objs, attrs, arrows, t1, t2);
		return ret;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static MapExp toMapping(Object o) {
		try {
			Tuple3 p = (Tuple3) o;

			Object p2 = p.b;
			Object p3 = p.c;
			Object o1 = p.a;
			String p1 = p.a.toString();

			if (p1.equals("fst")) {
				return new Fst(toSchema(p2), toSchema(p3));
			} else if (p1.equals("snd")) {
				return new Snd(toSchema(p2), toSchema(p3));
			} else if (p1.equals("unit")) {
				return new TT(toSchema(p3), new HashSet<>(
                        (Collection<String>) p2));
			} else if (p1.equals("subschema")) {
				return new Sub(toSchema(p2), toSchema(p3));
			} else if (p1.equals("inl")) {
				return new Inl(toSchema(p2), toSchema(p3));
			} else if (p1.equals("inr")) {
				return new Inr(toSchema(p2), toSchema(p3));
			} else if (p1.equals("iso1")) {
				return new Iso(true, toSchema(p2), toSchema(p3));
			} else if (p1.equals("iso2")) {
				return new Iso(false, toSchema(p2), toSchema(p3));
			} else if (p1.equals("eval")) {
				return new Apply(toSchema(p2), toSchema(p3));
			} else if (p2.toString().equals("then")) {
				return new MapExp.Comp(toMapping(o1), toMapping(p3));
			} else if (p2.toString().equals("*")) {
				return new Prod(toMapping(o1), toMapping(p3));
			} else if (p2.toString().equals("+")) {
				return new Case(toMapping(o1), toMapping(p3));
			}

		} catch (RuntimeException re) {
		}

		if (o instanceof Tuple5) {
			Tuple5 p = (Tuple5) o;

			Object p2 = p.c;
			Object p3 = p.e;
			Object o1 = p.a;
			return toMapConst(o1, toSchema(p2), toSchema(p3));
		}
	
		try {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			String p1 = p.a.toString();
			Object p2 = p.b;
            switch (p1) {
                case "id":
                    return new Id(toSchema(p2));
                case "curry":
                    return new Curry(toMapping(p2));
                case "void":
                    return new FF(toSchema(p2));
                case "opposite":
                    return new Opposite(toMapping(p2));
			default:
				break;
            }
		} catch (RuntimeException re) {

		}

		if (o instanceof String) {
			return new MapExp.Var(o.toString());
		}

		throw new RuntimeException("Cannot parse " + o);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private static Parser<?> instance() {
		Reference ref = Parser.newReference();

		Parser plusTy = Parsers.between(term("("),
				Parsers.tuple(ident(), term("+"), ident()), term(")"));
		Parser prodTy = Parsers.between(term("("),
				Parsers.tuple(ident(), term("*"), ident()), term(")"));
		Parser expTy = Parsers.between(term("("),
				Parsers.tuple(ident(), term("^"), ident()), term(")"));

		Parser<?> external = Parsers.tuple(term("external"), ident(), schema());
		Parser<?> delta = Parsers.tuple(term("delta"), mapping(), ident());
		Parser<?> sigma = Parsers.tuple(term("sigma"), mapping(), ident());
		Parser<?> pi = Parsers.tuple(term("pi"), mapping(), ident());
		Parser<?> SIGMA = Parsers.tuple(term("SIGMA"), mapping(), ident());
		Parser<?> relationalize = Parsers.tuple(term("relationalize"), ident());
		Parser<?> eval = Parsers.tuple(term("eval"), query(), ident());
		Parser<?> fullEval = Parsers.tuple(term("EVAL"), fullQuery(),
				ident());
		Parser<?> step = Parsers.tuple(term("step"), mapping(), mapping(),
				ident());

		Parser a = Parsers.or(Parsers.tuple(term("kernel"), ident()),
				Parsers.tuple(term("prop"), schema()),
				Parsers.tuple(term("void"), schema()),
				Parsers.tuple(term("unit"), schema()), plusTy, prodTy, expTy,
				/* ident(), */ instanceConst(), delta, sigma, pi, SIGMA, external,
				relationalize, eval, fullEval, step);

		ref.set(a);

		return a;
	}

	private static Parser<?> instanceDecl() {
		return Parsers.tuple(term("instance"), ident(), term("="), instance());
	}

	private static Parser<?> instanceConst() {
		Parser<?> node = Parsers.tuple(ident(), term("->"), Parsers.between(
				term("{"), string().sepBy(term(",")), term("}")));
		Parser<?> arrow = Parsers.tuple(
				ident(),
				term("->"),
				Parsers.between(
						term("{"),
						Parsers.between(term("("),
								Parsers.tuple(string(), term(","), string()),
								term(")")).sepBy(term(",")), term("}")));

		Parser<?> xxx = Parsers.tuple(section("nodes", node), Parsers.or(
				section("attributes", arrow),
				Parsers.tuple(term("attributes"),
						term("ASWRITTEN"), term(";"))),
				section("arrows", arrow));
		Parser<?> constant = Parsers
				.tuple(Parsers.between(term("{"), xxx, term("}")), term(":"),
						schema());
		return constant;
	}

	@SuppressWarnings("rawtypes")
    private static InstExp toInstConst(Object decl) {
		Tuple3 y = (Tuple3) decl;
		Tuple3 x = (Tuple3) y.a;

		// List<Pair<String, List<Pair<Object, Object>>>> data = new
		// LinkedList<>();

		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.c;
		Tuple3 attrs = (Tuple3) x.b;

		List nodes0 = (List) nodes.b;
		List arrows0 = (List) arrows.b;

		// List<Object> seen = new LinkedList<>();

		List<Pair<String, List<Pair<Object, Object>>>> nodesX = new LinkedList<>();
		for (Object o : nodes0) {
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			List m = (List) u.c;
			List<Pair<Object, Object>> l = new LinkedList<>();
			for (Object h : m) {
				l.add(new Pair<>(h, h));
			}
			// if (seen.contains(n)) {
			// throw new RuntimeException("duplicate field: " + o);
			// }
			// seen.add(n);
			nodesX.add(new Pair<>(n, l));
		}

		// RuntimeException toThrow = null;

		List<Pair<String, List<Pair<Object, Object>>>> attrsX = new LinkedList<>();
		if (attrs.b.toString().equals("ASWRITTEN")) {
			for (Pair<String, List<Pair<Object, Object>>> k : nodesX) {
				attrsX.add(new Pair<>(k.first + "_att", k.second));
			}
		} else {
			List attrs0 = (List) attrs.b;

			for (Object o : attrs0) {

				Tuple3 u = (Tuple3) o;
				String n = (String) u.a;
				List m = (List) u.c;
				List<Pair<Object, Object>> l = new LinkedList<>();
				for (Object h : m) {
					Tuple3 k = (Tuple3) h;
					l.add(new Pair<>(k.a, k.c));
				}
				// if (seen.contains(n)) {
				// toThrow = new RuntimeException("duplicate field: " + n );
				// throw toThrow;
				// }
				// seen.add(n);
				attrsX.add(new Pair<>(n, l));
			}
		}
		List<Pair<String, List<Pair<Object, Object>>>> arrowsX = new LinkedList<>();
		for (Object o : arrows0) {
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			List m = (List) u.c;
			List<Pair<Object, Object>> l = new LinkedList<>();
			for (Object h : m) {
				Tuple3 k = (Tuple3) h;
				l.add(new Pair<>(k.a, k.c));
			}
			// if (seen.contains(n)) {
			// throw new RuntimeException("duplicate field: " + o);
			// }
			// seen.add(n);
			arrowsX.add(new Pair<>(n, l));
		}
		InstExp.Const ret = new InstExp.Const(nodesX, attrsX, arrowsX,
				toSchema(y.c));
		return ret;
	}

	@SuppressWarnings("rawtypes")
    private static InstExp toInst(Object o) {
		try {
			Tuple4 t = (Tuple4) o;
			Token z = (Token) t.a;
			String y = z.toString();
			if (y.equals("step")) {
				return new Step(t.d.toString(), toMapping(t.b), toMapping(t.c));
			}
		} catch (RuntimeException cce) {
		}
		
		try {
			Tuple3 t = (Tuple3) o;
			Token z = (Token) t.a;
			String y = z.toString();
            switch (y) {
                case "delta":
                    return new InstExp.Delta(toMapping(t.b), t.c.toString());
                case "sigma":
                    return new InstExp.Sigma(toMapping(t.b), t.c.toString());
                case "SIGMA":
                    return new FullSigma(toMapping(t.b), t.c.toString());
                case "pi":
                    return new InstExp.Pi(toMapping(t.b), t.c.toString());
                case "external":
                    return new External(toSchema(t.b), t.c.toString());
                case "eval":
                    return new Eval(toQuery(t.b), t.c.toString());
                case "EVAL":
                    return new FullEval(toFullQuery(t.b), t.c.toString());
			default:
				break;
            }
		} catch (RuntimeException cce) {
		}

		try {
			Tuple3 t = (Tuple3) o;
			Token z = (Token) t.b;
			String y = z.toString();
			if (y.equals("+")) {
				return new Plus(t.a.toString(), t.c.toString());
			} else if (y.equals("*")) {
				return new Times(t.a.toString(), t.c.toString());
			}
			if (y.equals("^")) {
				return new Exp(t.a.toString(), (t.c).toString());
			}
		} catch (RuntimeException cce) {
		}

		try {
			org.jparsec.functors.Pair pr = (org.jparsec.functors.Pair) o;

			if (pr.a.toString().equals("unit")) {
				return new One(toSchema(pr.b));
			} else if (pr.a.toString().equals("void")) {
				return new Zero(toSchema(pr.b));
			} else if (pr.a.toString().equals("prop")) {
				return new Two(toSchema(pr.b));
			} else if (pr.a.toString().equals("relationalize")) {
				return new Relationalize(pr.b.toString());
			} else if (pr.a.toString().equals("kernel")) {
				return new Kernel(pr.b.toString());
			}
			throw new RuntimeException();
		} catch (RuntimeException cce) {
		}

		return toInstConst(o);
	}

	@SuppressWarnings("rawtypes")
    private static QueryExp toQuery(Object o) {
		if (o instanceof Tuple5) {
			Tuple5 t = (Tuple5) o;
			return new QueryExp.Comp(toQuery(t.b), toQuery(t.d));

		} else if (o instanceof Tuple3) {
			Tuple3 x = (Tuple3) o;
			org.jparsec.functors.Pair p1 = (org.jparsec.functors.Pair) x.a;
			org.jparsec.functors.Pair p2 = (org.jparsec.functors.Pair) x.b;
			org.jparsec.functors.Pair p3 = (org.jparsec.functors.Pair) x.c;
			return new QueryExp.Const(toMapping(p1.b), toMapping(p2.b),
					toMapping(p3.b));
		} else {
			return new QueryExp.Var(o.toString());
		}
	}

	@SuppressWarnings("rawtypes")
    private static FullQueryExp toFullQuery(Object o) {
		if (o instanceof Tuple5) {
			Tuple5 t = (Tuple5) o;
			if (t.a.toString().equals("match")) {
				return new Match(toMatch(t.b), toSchema(t.c),
						toSchema(t.d), t.e.toString());
			}
			return new Comp(toFullQuery(t.b), toFullQuery(t.d));
		} else if (o instanceof org.jparsec.functors.Pair) {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			if (p.a.toString().equals("delta")) {
				return new Delta(toMapping(p.b));
			} else if (p.a.toString().equals("SIGMA")) {
				return new Sigma(toMapping(p.b));
			} else if (p.a.toString().equals("pi")) {
				return new Pi(toMapping(p.b));
			}
		} else {
			return new Var(o.toString());
		}
		throw new RuntimeException();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Set<Pair<String, String>> toMatch(Object b) {
		List<Tuple3> l = (List<Tuple3>) b;
		Set<Pair<String, String>> ret = new HashSet<>();
		for (Tuple3 k : l) {
			ret.add(new Pair<>(k.a.toString(), k.c.toString()));
		}
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static FQLProgram program(String s) {
		List<NewDecl> ret = new LinkedList<>();
		List decls = (List) program.parse(s);

		for (Object d : decls) {
			org.jparsec.functors.Pair pr = (org.jparsec.functors.Pair) d;
			Object decl = pr.b;
			String txt = pr.a.toString();
			int idx = s.indexOf(txt);
			if (idx < 0) {
				throw new RuntimeException();
			}

			if (decl instanceof org.jparsec.functors.Pair) {
				org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) decl;
				if (p.a.toString().equals("drop")) {
					ret.add(NewDecl.dropDecl((List<String>) p.b));
					continue;
				}
			}
			Tuple3 t = (Tuple3) decl;
			String kind = t.a.toString();
			switch (kind) {
			case "enum":
				Tuple4 tte = (Tuple4) decl;
				String name = (String) tte.b;

				List<String> values = (List<String>) tte.d;

				ret.add(NewDecl.typeDecl(name, values, idx));

				break;
			case "query":
				Tuple4 tta = (Tuple4) decl;
				name = (String) tta.b;

				ret.add(NewDecl.queryDecl(name, idx, toQuery(tta.d)));
				break;
			case "QUERY":
				tta = (Tuple4) decl;
				name = (String) tta.b;

				ret.add(NewDecl.fullQuery(name, toFullQuery(tta.d), idx));
				break;
			case "schema":
				Tuple4 tt = (Tuple4) decl;
				name = (String) tt.b;

				ret.add(NewDecl.sigDecl(name, idx, toSchema(tt.d)));

				break;
			case "instance":
				Tuple4 tt0 = (Tuple4) decl;
				name = (String) t.b;

				NewDecl toAdd = NewDecl.instDecl(name, idx,
						toInst(tt0.d));
				ret.add(toAdd);

				break;
			case "mapping":
				Tuple4 t0 = (Tuple4) decl;
				name = (String) t.b;

				ret.add(NewDecl.mapDecl(name, idx, toMapping(t0.d)));
				break;
			case "transform":
				Tuple4 tx = (Tuple4) decl;
				name = (String) tx.b;

				ret.add(NewDecl.transDecl(name, idx, toTrans(tx.d)));
				break;

			default:
				throw new RuntimeException("Unknown decl: " + kind);
			}
		}
 
		return new FQLProgram(ret);
	}

	private static Parser<List<String>> path() {
		return Identifier.PARSER.sepBy1(term("."));
	}
 
	private static Parser<?> section(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p.sepBy(term(",")), term(";"));
	}

	private static Parser<?> string() {
		return Parsers.or(StringLiteral.PARSER, DecimalLiteral.PARSER,
				IntegerLiteral.PARSER, Identifier.PARSER);
	}

}
package catdata.fqlpp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fqlpp.CatExp.Cod;
import catdata.fqlpp.CatExp.Colim;
import catdata.fqlpp.CatExp.Kleisli;
import catdata.fqlpp.CatExp.Named;
import catdata.fqlpp.CatExp.Plus;
import catdata.fqlpp.CatExp.Times;
import catdata.fqlpp.CatExp.Union;
import catdata.fqlpp.FnExp.Const;
import catdata.fqlpp.FnExp.Krnl;
import catdata.fqlpp.FnExp.Tru;
import catdata.fqlpp.FunctorExp.Apply;
import catdata.fqlpp.FunctorExp.Case;
import catdata.fqlpp.FunctorExp.Comp;
import catdata.fqlpp.FunctorExp.Curry;
import catdata.fqlpp.FunctorExp.Dom;
import catdata.fqlpp.FunctorExp.Eval;
import catdata.fqlpp.FunctorExp.Exp;
import catdata.fqlpp.FunctorExp.FF;
import catdata.fqlpp.FunctorExp.Fst;
import catdata.fqlpp.FunctorExp.Id;
import catdata.fqlpp.FunctorExp.Inl;
import catdata.fqlpp.FunctorExp.Inr;
import catdata.fqlpp.FunctorExp.Iso;
import catdata.fqlpp.FunctorExp.Migrate;
import catdata.fqlpp.FunctorExp.One;
import catdata.fqlpp.FunctorExp.Pivot;
import catdata.fqlpp.FunctorExp.Prod;
import catdata.fqlpp.FunctorExp.Prop;
import catdata.fqlpp.FunctorExp.Pushout;
import catdata.fqlpp.FunctorExp.Snd;
import catdata.fqlpp.FunctorExp.TT;
import catdata.fqlpp.FunctorExp.Uncurry;
import catdata.fqlpp.FunctorExp.Var;
import catdata.fqlpp.FunctorExp.Zero;
import catdata.fqlpp.SetExp.Intersect;
import catdata.fqlpp.SetExp.Numeral;
import catdata.fqlpp.SetExp.Range;
import catdata.fqlpp.TransExp.Adj;
import catdata.fqlpp.TransExp.AndOrNotImplies;
import catdata.fqlpp.TransExp.ApplyPath;
import catdata.fqlpp.TransExp.ApplyTrans;
import catdata.fqlpp.TransExp.Bool;
import catdata.fqlpp.TransExp.Chr;
import catdata.fqlpp.TransExp.CoProd;
import catdata.fqlpp.TransExp.Inj;
import catdata.fqlpp.TransExp.Ker;
import catdata.fqlpp.TransExp.PeterApply;
import catdata.fqlpp.TransExp.Proj;
import catdata.fqlpp.TransExp.SetSet;
import catdata.fqlpp.TransExp.ToCat;
import catdata.fqlpp.TransExp.ToInst;
import catdata.fqlpp.TransExp.ToMap;
import catdata.fqlpp.TransExp.ToSet;
import catdata.fqlpp.TransExp.Whisker;
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
import catdata.Triple;
import catdata.Unit;
import catdata.fqlpp.FQLPPProgram.NewDecl;
import catdata.fqlpp.FunctorExp.CatConst;
import catdata.fqlpp.FunctorExp.FinalConst;
import catdata.fqlpp.FunctorExp.InstConst;
import catdata.fqlpp.FunctorExp.MapConst;
import catdata.fqlpp.FunctorExp.SetSetConst;
import catdata.fqlpp.cat.FinSet.Fn;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
class PPParser {

	static final Parser<Integer> NUMBER = IntegerLiteral.PARSER
			.map(Integer::valueOf);

	private static final String[] ops = new String[] { ",", ".", ";", ":", "{", "}", "(",
			")", "=", "->", "+", "*", "^", "|", "?" };

	private static final String[] res = new String[] {  "set", "function", "category", "functor", "range",
		    "colim", "not", "and", "or", "implies", "return", "coreturn", "uncurry", "pushout",
			 "match",  "objects", "cod", "dom", "apply", "on", "object", "arrow", "left", "right", "whisker",
			 "transform",  "arrows", "Set", "Cat", "kleisli", "cokleisli",
			"equations", "id", "delta", "sigma", "pi", "eval", "in", "path", "union",
			"relationalize",  "fst", "forall", "exists", "tt", "ff", "APPLY",
			"snd", "inl", "inr", "curry",  "void", "unit", "CURRY", "pivot", "unpivot",
			"prop", "iso1", "iso2", "true", "false", "char", "kernel", "union", "intersect" };

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	public static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT,
			Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	public static final Parser<?> TOKENIZER = Parsers.or(
			(Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), (Parser<?>) Identifier.TOKENIZER,
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
				.or(    Parsers.tuple(setDecl().source().peek(), setDecl()),
						Parsers.tuple(fnDecl().source().peek(), fnDecl()),
						Parsers.tuple(catDecl().source().peek(), catDecl()),
						Parsers.tuple(ftrDecl().source().peek(), ftrDecl()),
						Parsers.tuple(transDecl().source().peek(), transDecl())
						).many();

	}
	
	private static Object toValue(Object o) {
		if (o.toString().equals("true")) {
			return true;
		}
		if (o.toString().equals("false")) {
			return false;
		}
		if (o instanceof Tuple5) {
			Tuple5 t = (Tuple5) o;
			if (t.a.toString().equals("(")) {
				return new Pair(toValue(t.b), toValue(t.d));
			}
			List l = (List) t.b;
			Map s = new HashMap();
			for (Object y : l) {
				Pair yy = (Pair) toValue(y);
				if (s.containsKey(yy.first)) {
					throw new RuntimeException("Duplicate domain entry in " + o);
				}
				s.put(yy.first, yy.second); 
			}
			Tuple3 tt= (Tuple3) t.e;
			Set ui = (Set) toValue(tt.a);
			Set uj = (Set) toValue(tt.c);
			return new Fn(ui, uj, s::get);
		}
		if (o instanceof Tuple3) {
			Tuple3 p = (Tuple3) o;
			List l = (List) p.b;
			Set s = new HashSet();
			for (Object y : l) {
				s.add(toValue(y));
			}
			return s;
		}
		if (o instanceof org.jparsec.functors.Pair) {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			if (p.a.toString().equals("inl")) {
				return Chc.inLeftNC(toValue(p.b));
			} else if (p.a.toString().equals("inr")) {
				return Chc.inRightNC(toValue(p.b));
			} else {
				return Unit.unit;
			}
		}
		return o.toString();
	}
	
	private static Parser<?> value() {
		Reference ref = Parser.newReference();

		Parser<?> p1 = Parsers.tuple(term("inl"), ref.lazy());
		Parser<?> p2 = Parsers.tuple(term("inr"), ref.lazy());
		Parser<?> p3 = Parsers.tuple(term("("), term(")"));
		Parser<?> p4 = Parsers.tuple(term("("), ref.lazy(), term(","), ref.lazy(), term(")"));
		Parser<?> p5 = term("true");
		Parser<?> p6 = term("false");
		Parser<?> p7 = Parsers.tuple(term("{"), ref.lazy().sepBy(term(",")), term("}"));
		Parser<?> xxx = Parsers.tuple(p7, term("->"), p7);
		Parser<?> p8 = Parsers.tuple(term("{"), p4.sepBy(term(",")), term("}"), term(":"), xxx);
		
//		Parser<?> op = Parsers.tuple(term("opposite"), ref.lazy());

		Parser<?> a = Parsers.or(new Parser[] { string(), p1, p2, p3, p4, p5, p6, p8, p7 });

		ref.set(a);

		return a;
	}

	private static Parser<?> set() {
		Reference ref = Parser.newReference();

		Parser<?> app = Parsers.tuple(term("apply"), ident(), term("on"), term("object"), ref.lazy());
		
		Parser<?> union = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("union"), ref.lazy()), term(")"));
		Parser<?> isect = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("intersect"), ref.lazy()), term(")"));
		
		Parser<?> plusTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("+"), ref.lazy()), term(")"));
		Parser<?> prodTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("*"), ref.lazy()), term(")"));
		Parser<?> expTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("^"), ref.lazy()), term(")"));
		Parser<?> k = Parsers.tuple(term("cod"), ident());
		Parser<?> v = Parsers.tuple(term("dom"), ident());
		Parser<?> u = Parsers.tuple(term("range"), ident());
		Parser<?> a = Parsers.or(union, isect, term("void"), term("unit"), term("prop"),
				plusTy, prodTy, expTy, k, v, u,
				ident(), setConst(), IntegerLiteral.PARSER, app);

		ref.set(a);

		return a;
	}
	
	private static Parser<?> cat() {
		Reference ref = Parser.newReference();

		Parser<?> plusTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("+"), ref.lazy()), term(")"));
		Parser<?> prodTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("*"), ref.lazy()), term(")"));
		Parser<?> expTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("^"), ref.lazy()), term(")"));
		
		Parser<?> k = Parsers.tuple(term("cod"), ident());
		Parser<?> v = Parsers.tuple(term("dom"), ident());
		Parser<?> colim = Parsers.tuple(term("colim"), ident());
		
		Parser<?> kleisli  = Parsers.tuple(term("kleisli"), ident(), ident(), ident());
		Parser<?> cokleisli= Parsers.tuple(term("cokleisli"), ident(), ident(), ident());
		
		Parser<?> union = Parsers.tuple(term("union"), ref.lazy(), ref.lazy());
		
		Parser<?> a = Parsers.or(colim, term("void"), term("unit"),
				plusTy, prodTy, expTy, k, v,
				ident(), catConst(), term("Cat"), term("Set"), kleisli, cokleisli, union);

		ref.set(a);

		return a;
	}
	
	public static Parser<?> catConst() {
		Parser<?> p1 = ident();
		Parser<?> pX = Parsers.tuple(ident(), term(":"), ident(), term("->"),
				ident());
		Parser<?> p3 = Parsers.tuple(path(), term("="), path());
		Parser<?> foo = Parsers.tuple(section("objects", p1), 
				section("arrows", pX),
				section("equations", p3));
		return Parsers.between(term("{"), foo, term("}"));
	}

	private static Parser<?> setDecl() {
		return Parsers.tuple(term("set"), ident(), term("="), set());
	}

	private static Parser<?> setConst() {
		return Parsers.tuple(term("{"), value().sepBy(term(",")), term("}"));
	}


	private static SetExp toSet(Object o) {
		try {
			Tuple5 t = (Tuple5) o;
			String f = t.b.toString();
			SetExp e = toSet(t.e);
			return new SetExp.Apply(f, e);
		} catch (Exception e) { }
		
		try {
			int i = Integer.parseInt(o.toString());
			return new Numeral(i);
		} catch (Exception e) { }
		
		try {
			Tuple3<?, ?, ?> t = (Tuple3<?, ?, ?>) o;
			String y = t.b.toString();
			if (y.equals("+")) {
				return new SetExp.Plus(toSet(t.a), toSet(t.c));
			} else if (y.equals("*")) {
				return new SetExp.Times(toSet(t.a), toSet(t.c));
			} else if (y.equals("^")) {
				return new SetExp.Exp(toSet(t.a), toSet(t.c));
			} else if (y.equals("union")) {
				return new SetExp.Union(toSet(t.a), toSet(t.c));
			} else if (y.equals("intersect")) {
				return new Intersect(toSet(t.a), toSet(t.c));
			} 
			
			else if (t.a.toString().equals("{")) {
				List tb = (List) t.b;
				Set x = new HashSet();
				for (Object uu : tb) {
					x.add(toValue(uu));
				}
				return new SetExp.Const(x);
			}
		} catch (RuntimeException cce) {
		}

		try {
			org.jparsec.functors.Pair<?, ?> p = (org.jparsec.functors.Pair<?, ?>) o;
			if (p.a.toString().equals("dom")) {
				return new SetExp.Dom(toFn(p.b));
			} else if (p.a.toString().equals("cod")) {
				return new SetExp.Cod(toFn(p.b));
			} else if (p.a.toString().equals("range")) {
				return new Range(toFn(p.b));
			}
		} catch (RuntimeException cce) {
		}
		
		try {
			if (o.toString().equals("void")) {
				return new SetExp.Zero();
			} else if (o.toString().equals("unit")) {
				return new SetExp.One();
			} else if (o.toString().equals("prop")) {
				return new SetExp.Prop();
			}
			
			throw new RuntimeException();
		} catch (RuntimeException cce) {
		}

		return new SetExp.Var(o.toString()); 
	}

	public static CatExp.Const toCatConst(Object y) {
		Set<String> nodes = new HashSet<>();
		Set<Triple<String, String, String>> arrows = new HashSet<>();
		Set<Pair<Pair<String, List<String>>, Pair<String, List<String>>>> eqs = new HashSet<>();

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
			String xxx = l1.remove(0);
			String yyy = l2.remove(0);
			eqs.add(new Pair<>(new Pair<>(xxx, l1), new Pair<>(yyy, l2)));
		}
		CatExp.Const c = new CatExp.Const(nodes, arrows, eqs);
		return c;
	}

	private static CatExp toCat(Object o) {
		 if (o.toString().equals("Set")) {
			return new Named("Set");
		} 
		if (o.toString().equals("Cat")) {
			return new Named("Cat");
		} 
		
		if (o instanceof Tuple4) {
			Tuple4 t = (Tuple4) o;
			return new Kleisli(t.b.toString(), t.c.toString(), t.d.toString(), t.a.toString().equals("cokleisli"));
		}
		
		try {
			Tuple3<?, ?, ?> t = (Tuple3<?, ?, ?>) o;
			String y = t.b.toString();
			if (y.equals("+")) {
				return new Plus(toCat(t.a), toCat(t.c));
			} else if (y.equals("*")) {
				return new Times(toCat(t.a), toCat(t.c));
			} else if (y.equals("^")) {
				return new CatExp.Exp(toCat(t.a), toCat(t.c));
			} else if (t.a.toString().equals("union")) {
				return new Union(toCat(t.b), toCat(t.c));
			}
			else {
				return toCatConst(o);
			}
		} catch (RuntimeException cce) {
		}
		
		try {
			org.jparsec.functors.Pair<?, ?> p = (org.jparsec.functors.Pair<?, ?>) o;
			if (p.a.toString().equals("dom")) {
				return new CatExp.Dom(toFtr(p.b));
			} else if (p.a.toString().equals("cod")) {
				return new Cod(toFtr(p.b));
			} else if (p.a.toString().equals("colim")) {
				return new Colim((String)p.b);
			}
		} catch (RuntimeException cce) {
		}

		try {
			if (o.toString().equals("void")) {
				return new CatExp.Zero();
			} else if (o.toString().equals("unit")) {
				return new CatExp.One();
			} 		
		} catch (RuntimeException cce) {
		}

		return new CatExp.Var(o.toString()); 
	}
	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static SetExp toSetConst(Object y) {
		return new SetExp.Const((Set)toValue(y));
	}


	private static Parser<?> transDecl() {
		return Parsers.tuple(term("transform"), ident(), term("="), trans());
	}
	
	private static Parser<?> fnDecl() {
		return Parsers.tuple(term("function"), ident(), term("="), fn());
	}
	
	private static Parser<?> ftrDecl() {
		return Parsers.tuple(term("functor"), ident(), term("="), ftr());
	}
	
	private static Parser<?> catDecl() {
		return Parsers.tuple(term("category"), ident(), term("="), cat());
	}
	
	private static Parser<?> trans() {
		Reference ref = Parser.newReference();

		Parser compTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term(";"), ref.lazy()), term(")"));
		Parser prodTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("*"), ref.lazy()), term(")"));
		Parser sumTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("+"), ref.lazy()), term(")"));
		
		//////
		Parser<?> node = Parsers.tuple(ident(), term("->"), fn());
		Parser s2 = Parsers.tuple(ftr(), term(":"), term("Set"), term("->"), term("Set"));
		Parser s1 = Parsers.between(term("("), s2, term(")"));
		Parser<?> setset = Parsers
				.tuple(Parsers.between(term("{"), section2("objects", node), term("}")), term(":"),
						s1, term("->"), s1);
		/////
		Parser<?> node2 = Parsers.tuple(ident(), term("->"), fn().or(setOfPairs()));

		Parser x2 = Parsers.tuple(ftr(), term(":"), cat(), term("->"), term("Set"));
		Parser x1 = Parsers.between(term("("), x2, term(")"));
		Parser<?> toset = Parsers
				.tuple(Parsers.between(term("{"), section("objects", node2), term("}")), term(":"),
						x1, term("->"), x1);
		/////
		Parser nd =  Parsers.tuple(ident(), term("->"), ftr());
		Parser t2 = Parsers.tuple(ftr(), term(":"), cat(), term("->"), term("Cat"));
		Parser t1 = Parsers.between(term("("), t2, term(")"));
		Parser<?> tocat = Parsers
				.tuple(Parsers.between(term("{"), section("objects", nd), term("}")), term(":"),
						t1, term("->"), t1);
		/////
		
		Parser edX =  Parsers.tuple(ident(), term("->"), ref.lazy());
		Parser uip = term("Cat");
		Parser piu = term("Set");
		Parser u2X = Parsers.tuple(ftr(), term(":"), cat(), term("->"), Parsers.between(term("("), Parsers.tuple(uip.or(piu), term("^"), cat()), term(")")));
		Parser u1X = Parsers.between(term("("), u2X, term(")"));
		Parser<?> tofinal = Parsers
				.tuple(Parsers.between(term("{"), section("objects", edX), term("}")), term(":"),
						u1X, term("->"), u1X); 
		/////
		Parser ed =  Parsers.tuple(ident(), term("->"), path());
		Parser u2 = Parsers.tuple(ftr(), term(":"), cat(), term("->"), cat());
		Parser u1 = Parsers.between(term("("), u2, term(")"));
		Parser<?> tomap = Parsers
				.tuple(Parsers.between(term("{"), section("objects", ed), term("}")), term(":"),
						u1, term("->"), u1);
		

		
		Parser a = Parsers.or(ident(),
				Parsers.tuple(term("id"), ftr()), tofinal,
				compTy, setset, toset, tocat, tomap,
				Parsers.tuple(term("tt"), ref.lazy()),
				Parsers.tuple(term("not"), cat()),
				Parsers.tuple(term("and"), cat()),
				Parsers.tuple(term("or"), cat()),
				Parsers.tuple(term("implies"), cat()),
				Parsers.tuple(term("char"), ref.lazy()),
				Parsers.tuple(term("kernel"), ref.lazy()),
				Parsers.tuple(term("fst"), ftr(), ftr()),
				Parsers.tuple(term("snd"), ftr(), ftr()),
				Parsers.tuple(term("ff"), ref.lazy()),
				Parsers.tuple(term("curry"), ref.lazy()),
				Parsers.tuple(term("CURRY"), ref.lazy()),
				Parsers.tuple(term("return"), term("sigma"), term("delta"), ftr()),
				Parsers.tuple(term("coreturn"), term("sigma"), term("delta"), ftr()),
				Parsers.tuple(term("return"), term("delta"), term("pi"), ftr()),
				Parsers.tuple(term("coreturn"), term("delta"), term("pi"), ftr()),
				Parsers.tuple(term("apply"), ftr(), term("on"), term("arrow"), ref.lazy()),
				Parsers.tuple(term("apply"), ftr(), term("on"), term("path"), Parsers.tuple(path(), term("in"), cat())),
				Parsers.tuple(term("APPLY"), ref.lazy(), term("on"), ident()),
				Parsers.tuple(term("apply"), ref.lazy(), term("on"), ftr()),
				Parsers.tuple(term("left"), term("whisker"), ftr(), ref.lazy()),
				Parsers.tuple(term("right"), term("whisker"), ftr(), ref.lazy()),
				Parsers.tuple(term("inl"), ftr(), ftr()),
				Parsers.tuple(term("inr"), ftr(), ftr()),
				Parsers.tuple(term("eval"), ftr(), ftr()),
				Parsers.tuple(term("iso1"), ftr(), ftr()),
				Parsers.tuple(term("iso2"), ftr(), ftr()),
				Parsers.tuple(term("true"), cat()),
				Parsers.tuple(term("false"), cat()),
				prodTy, sumTy);
		
		ref.set(a);
		return a;
	}
	

	private static Parser<?> fn() {
		Reference ref = Parser.newReference();

		Parser plusTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("+"), ref.lazy()), term(")"));
		Parser prodTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("*"), ref.lazy()), term(")"));
		Parser compTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term(";"), ref.lazy()), term(")"));

		Parser<?> app2 = Parsers.tuple(term("apply"), ident(), set());
		Parser<?> app = Parsers.tuple(term("apply"), ident(), term("on"), term("arrow"), ref.lazy());
		
		Parser a = Parsers.or(term("true"),
				term("false"),
				term("and"),
				term("or"),
				term("not"),
				term("implies"),
				Parsers.tuple(term("tt"), set()),
				Parsers.tuple(term("ff"), set()),
				Parsers.tuple(term("iso1"), set(), set()),
				Parsers.tuple(term("iso2"), set(), set()),
				Parsers.tuple(term("fst"), set(), set()),
				Parsers.tuple(term("snd"), set(), set()),
				Parsers.tuple(term("inl"), set(), set()),
				Parsers.tuple(term("inr"), set(), set()),
				Parsers.tuple(term("eval"), set(), set()),
				Parsers.tuple(term("curry"), ref.lazy()),
				Parsers.tuple(term("char"), ref.lazy()),
				Parsers.tuple(term("kernel"), ref.lazy()),
				Parsers.tuple(term("id"), set()),
				compTy, plusTy, prodTy, ident(), fnConst(), app, app2);

		ref.set(a);
		return a;
	}
	
	private static Parser<?> ftr() {
		Reference ref = Parser.newReference();

		Parser plusTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("+"), ref.lazy()), term(")"));
		Parser expTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("^"), ref.lazy()), term(")"));
		Parser prodTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term("*"), ref.lazy()), term(")"));
		Parser compTy = Parsers.between(term("("),
				Parsers.tuple(ref.lazy(), term(";"), ref.lazy()), term(")"));
		
		////
		Parser<?> node = Parsers.tuple(ident(), term("->"), cat());
		Parser<?> arrow = Parsers.tuple(
				ident(),
				term("->"),
				ref.lazy());
		Parser<?> xxx = Parsers.tuple(section("objects", node), 
				section("arrows", arrow));
		Parser<?> constant = Parsers
				.tuple(Parsers.between(term("{"), xxx, term("}")), term(":"),
						cat(), term("->"), term("Cat"));
		////
		
		////
				Parser<?> node2 = Parsers.tuple(ident(), term("->"), ref.lazy());
				Parser<?> arrow2 = Parsers.tuple(
						ident(),
						term("->"),
						ident());
				Parser<?> xxx2 = Parsers.tuple(section("objects", node2), 
						section("arrows", arrow2));
				Parser t1 = Parsers.between(term("("),
						Parsers.tuple(term("Set"), term("^"), cat()), term(")"));
				Parser t2 = Parsers.between(term("("),
						Parsers.tuple(term("Cat"), term("^"), cat()), term(")"));
				Parser<?> constant2 = Parsers
						.tuple(Parsers.between(term("{"), xxx2, term("}")), term(":"),
								cat(), term("->"), t1.or(t2));
		////
		Parser catTerm = term("Cat");
		Parser setTerm = term("Set");
		Parser a = Parsers.or(Parsers.tuple(term("unit"), cat(), catTerm.or(setTerm)),
				Parsers.tuple(term("void"), cat(), catTerm.or(setTerm)),
				Parsers.tuple(term("tt"), cat()),
				Parsers.tuple(term("ff"), cat()),
				Parsers.tuple(term("dom"), ident()),
				Parsers.tuple(term("cod"), ident()),
				Parsers.tuple(term("iso1"), cat(), cat()),
				Parsers.tuple(term("iso2"), cat(), cat()),
				Parsers.tuple(term("fst"), cat(), cat()),
				Parsers.tuple(term("snd"), cat(), cat()),
				Parsers.tuple(term("inl"), cat(), cat()),
				Parsers.tuple(term("inr"), cat(), cat()),
				Parsers.tuple(term("eval"), cat(), cat()),
				Parsers.tuple(term("curry"), ref.lazy()),
				Parsers.tuple(term("uncurry"), ref.lazy()),
				Parsers.tuple(term("delta"), ref.lazy()),
				Parsers.tuple(term("sigma"), ref.lazy()),
				Parsers.tuple(term("pi"), ref.lazy()),
				Parsers.tuple(term("pivot"), ref.lazy()),
				Parsers.tuple(term("unpivot"), ref.lazy()),
				Parsers.tuple(term("pushout"), ident(), ident()),
				Parsers.tuple(term("apply"), ref.lazy(), term("on"), term("object"), ref.lazy()),
				Parsers.tuple(term("id"), cat()),
				Parsers.tuple(term("prop"), cat()),
				compTy, plusTy, prodTy, expTy, ident(),
				instanceConst(), constant, setsetConst(), constant2,
				mappingConst());

		ref.set(a);
		return a;
	}
	
	private static Parser<?> instanceConst() {
		Parser<?> node = Parsers.tuple(ident(), term("->"), set());
		Parser<?> arrow = Parsers.tuple(
				ident(),
				term("->"),
				fn().or(setOfPairs()));

		Parser<?> xxx = Parsers.tuple(section("objects", node), 
				section("arrows", arrow));
		Parser<?> constant = Parsers
				.tuple(Parsers.between(term("{"), xxx, term("}")), term(":"),
						cat(), term("->"), term("Set"));
		return constant;
	}
	
	private static Parser<?> mappingConst() {
		Parser<?> node = Parsers.tuple(ident(), term("->"), ident());
		Parser<?> arrow = Parsers.tuple(
				ident(),
				term("->"),
				path());

		Parser<?> xxx = Parsers.tuple(section("objects", node), 
				section("arrows", arrow));
		Parser<?> constant = Parsers
				.tuple(Parsers.between(term("{"), xxx, term("}")), term(":"),
						cat(), term("->"), cat());
		return constant;
	}
	
	private static Parser<?> section2(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p, term(";"));
	}
	
	private static Parser<?> setsetConst() {
		Parser<?> node = Parsers.tuple(ident(), term("->"), set());
		
		Parser<?> arrow = Parsers.tuple(
				Parsers.tuple(ident(), term(":"), ident(), term("->"), ident()),
				term("->"),
				fn());

		Parser<?> xxx = Parsers.tuple(section2("objects", node), 
				                      section2("arrows", arrow));
		
		Parser<?> constant = Parsers
				.tuple(Parsers.between(term("{"), xxx, term("}")), term(":"),
						term("Set"), term("->"), term("Set"));
		return constant;
	}
	
	
	private static Parser setOfPairs() {
		Parser<?> q = Parsers.tuple(term("("), value(), term(","), value(), term(")"));
		return Parsers.tuple(term("{"), q.sepBy(term(",")), term("}"));		
	}

	private static Parser<?> fnConst() {
		Parser<?> p = Parsers.tuple(set(), term("->"), set());
		Parser<?> q = Parsers.tuple(term("("), value(), term(","), value(), term(")"));
		return Parsers.tuple(term("{"), q.sepBy(term(",")), term("}"), term(":"), p);
	}
	
	private static FnExp toFn(Object o) {
		try {
			Tuple5 t5 = (Tuple5) o;
			
			if (t5.a.toString().equals("apply")) {
				Tuple5 t = (Tuple5) o;
				String f = t.b.toString();
				FnExp e = toFn(t.e);
				return new FnExp.Apply(f, e);
			} 
			
			Tuple3 t3 = (Tuple3) t5.e;
			Map s = new HashMap<>();
			List l = (List) t5.b;
			for (Object y : l) {
				Pair yy = (Pair) toValue(y);
				if (s.containsKey(yy.first)) {
					throw new RuntimeException("Duplicate domain entry in " + o);
				}
				s.put(yy.first, yy.second); 
			}
			SetExp t3a = toSet(t3.a);
			SetExp t3c = toSet(t3.c);
			return new Const(s::get, t3a, t3c);
		} catch (Exception e) {
			
		}
		try {
			Tuple3 p = (Tuple3) o;

			Object p2 = p.b;
			Object p3 = p.c;
			Object o1 = p.a;
			String p1 = p.a.toString();

			if (p1.equals("fst")) {
				return new FnExp.Fst(toSet(p2), toSet(p3));
			} else if (p1.equals("snd")) {
				return new FnExp.Snd(toSet(p2), toSet(p3));
			}  else if (p1.equals("inl")) {
				return new FnExp.Inl(toSet(p2), toSet(p3));
			} else if (p1.equals("inr")) {
				return new FnExp.Inr(toSet(p2), toSet(p3));
			} else if (p1.equals("iso1")) {
				return new FnExp.Iso(true, toSet(p2), toSet(p3));
			} else if (p1.equals("iso2")) {
				return new FnExp.Iso(false, toSet(p2), toSet(p3));
			} else if (p1.equals("eval")) {
				return new FnExp.Eval(toSet(p2), toSet(p3));
			} else if (p2.toString().equals(";")) {
				return new FnExp.Comp(toFn(o1), toFn(p3));
			} else if (p2.toString().equals("*")) {
				return new FnExp.Prod(toFn(o1), toFn(p3));
			} else if (p2.toString().equals("+")) {
				return new FnExp.Case(toFn(o1), toFn(p3));
			} if (p1.equals("apply")) {
				return new FnExp.ApplyTrans(p2.toString(), toSet(p3));
			}

		} catch (RuntimeException re) {
		}


		try {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			String p1 = p.a.toString();
			Object p2 = p.b;
			switch (p1) {
				case "id":
					return new FnExp.Id(toSet(p2));
				case "curry":
					return new FnExp.Curry(toFn(p2));
				case "ff":
					return new FnExp.FF(toSet(p2));
				case "tt":
					return new FnExp.TT(toSet(p2));
				case "char":
					return new FnExp.Chr(toFn(p2));
				case "kernel":
					return new Krnl(toFn(p2));
			default:
				break;
			}
		} catch (RuntimeException re) {

		}

		if (o instanceof String) {
			return new FnExp.Var(o.toString());
		}
		if (o.toString().equals("true")) {
			return new Tru("true");
		} else if (o.toString().equals("false")) {
			return new Tru("false");
		} else if (o.toString().equals("and")) {
			return new Tru("and");
		} else if (o.toString().equals("or")) {
			return new Tru("or");
		} else if (o.toString().equals("not")) {
			return new Tru("not");
		} else if (o.toString().equals("implies")) {
			return new Tru("implies");
		} 

		throw new RuntimeException(); 
	}
	
	//: ** notation
	
	private static TransExp toTrans(Object o) {
		try {
			Tuple5 t = (Tuple5) o;
			
			if (t.a.toString().equals("apply")) {
				FunctorExp f = toFtr(t.b);
				if (t.d.toString().equals("arrow")) {
					TransExp e = toTrans(t.e);
					return new TransExp.Apply(f, e);
				} 
					Tuple3 t3 = (Tuple3) t.e;
					List<String> l = (List<String>) t3.a;
					return new ApplyPath(f, l.remove(0), l, toCat(t3.c));
				
			}
			

			Tuple5 a = (Tuple5) t.c;
			Tuple5 b = (Tuple5) t.e;

			if (a.c.toString().equals("Set") && a.e.toString().equals("Set") &&
				b.c.toString().equals("Set") && b.e.toString().equals("Set")) {
				Tuple3 z = (Tuple3) ((org.jparsec.functors.Pair) t.a).b;
				FunctorExp src = toFtr(a.a);
				FunctorExp dst = toFtr(b.a);
				String ob = z.a.toString();
				FnExp fun = toFn(z.c);
				return new SetSet(ob, fun, src, dst);
			} else if (a.e.toString().equals("Set") && b.e.toString().equals("Set")) {
				FunctorExp src = toFtr(a.a);
				FunctorExp dst = toFtr(b.a);
				List<Object> ob = (List<Object>) ((org.jparsec.functors.Pair) t.a).b;
				Map<String, Chc<FnExp, SetExp>> fun = new HashMap<>();
		
				for (Object ttt : ob) {
					if (fun.containsKey(o)) {
						throw new RuntimeException("Duplicate arrow: " + ttt + " in " + o);
					}						
					Tuple3 u = (Tuple3) ttt;
					String n = (String) u.a;
					try {
						fun.put(n, Chc.inLeftNC(toFn(u.c)));
					} catch (Exception yyy) {
						fun.put(n, Chc.inRightNC(toSet(u.c)));						
					}
				}
				return new ToSet(fun , src, dst);
			} else if (a.e.toString().equals("Cat") && b.e.toString().equals("Cat")) {
				FunctorExp src = toFtr(a.a);
				FunctorExp dst = toFtr(b.a);
				List<Object> ob = (List<Object>) ((org.jparsec.functors.Pair) t.a).b;
				Map<String, FunctorExp> fun = new HashMap<>();
				
				for (Object ttt : ob) {
					if (fun.containsKey(o)) {
						throw new RuntimeException("Duplicate arrow: " + ttt + " in " + o);
					}
					Tuple3 u = (Tuple3) ttt;
					String n = (String) u.a;
					fun.put(n, toFtr(u.c));
				}
				return new ToCat(fun , src, dst);
			} else {
				try {
					FunctorExp src = toFtr(a.a);
					FunctorExp dst = toFtr(b.a);
					List<Object> ob = (List<Object>) ((org.jparsec.functors.Pair) t.a).b;
					Map<String, TransExp> fun = new HashMap<>();
					
					for (Object ttt : ob) {
						if (fun.containsKey(o)) {
							throw new RuntimeException("Duplicate arrow: " + ttt + " in " + o);
						}
						Tuple3 u = (Tuple3) ttt;
						String n = (String) u.a;
						fun.put(n, toTrans(u.c));
					}
					return new ToInst(fun , src, dst);
				}catch(Exception ex) { }
				
				FunctorExp src = toFtr(a.a);
				FunctorExp dst = toFtr(b.a);
				List<Object> ob = (List<Object>) ((org.jparsec.functors.Pair) t.a).b;
				Map<String, Pair<String, List<String>>> fun = new HashMap<>();		
				for (Object ttt : ob) {
					if (fun.containsKey(o)) {
						throw new RuntimeException("Duplicate arrow: " + ttt + " in " + o);
					}
					Tuple3 u = (Tuple3) ttt;
					String n = (String) u.a;
					List<String> l = (List<String>) u.c;
					String ll = l.remove(0);
					fun.put(n, new Pair<>(ll, l));
				}
				return new ToMap(fun , src, dst, toCat(a.c), toCat(a.e));
			}
		} catch (Exception re) { }
		
		try {
			Tuple4 t = (Tuple4) o;
			if (t.a.toString().equals("return") || t.a.toString().equals("coreturn")) {
				return new Adj(t.a.toString(), t.b.toString(), t.c.toString(), toFtr(t.d));
			} else if (t.a.toString().equals("left") ) {
				return new Whisker(true, toFtr(t.c), toTrans(t.d));
			} else if (t.a.toString().equals("right") ) {
				return new Whisker(false, toFtr(t.c), toTrans(t.d));
			} else if (t.a.toString().equals("APPLY")) {
				return new PeterApply(t.d.toString(),toTrans(t.b));
			} else {
				return new ApplyTrans(toTrans(t.b), toFtr(t.d));
			}
		} catch (Exception re) { }

		try {
			Tuple3 p = (Tuple3) o;

			Object p2 = p.b;
			Object p3 = p.c;
			Object o1 = p.a;
			//String p1 = p.a.toString();

			if (p2.toString().equals(";")) {
				return new TransExp.Comp(toTrans(o1), toTrans(p3));
			} else if (o1.toString().equals("fst")) {
				return new Proj(toFtr(p2), toFtr(p3), true);
			} else if (o1.toString().equals("snd")) {
				return new Proj(toFtr(p2), toFtr(p3), false);
			} else if (o1.toString().equals("inl")) {
				return new Inj(toFtr(p2), toFtr(p3), true);
			} else if (o1.toString().equals("inr")) {
				return new Inj(toFtr(p2), toFtr(p3), false);
			}  else if (o1.toString().equals("eval")) {
				return new TransExp.Eval(toFtr(p2), toFtr(p3));
			} else if (p2.toString().equals("*")) {
				return new TransExp.Prod(toTrans(o1), toTrans(p3));
			} else if (p2.toString().equals("+")) {
				return new CoProd(toTrans(o1), toTrans(p3));
			} else if (o1.toString().equals("iso1")) {
				return new TransExp.Iso(true, toFtr(p2), toFtr(p3));
			} else if (o1.toString().equals("iso2")) {
				return new TransExp.Iso(false, toFtr(p2), toFtr(p3));
			}

		} catch (RuntimeException re) { }

		try {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			String p1 = p.a.toString();
			Object p2 = p.b;
			switch (p1) {
				case "id":
					return new TransExp.Id(toFtr(p2));
				case "tt":
					return new TransExp.One(toFtr(p2));
				case "ff":
					return new TransExp.Zero(toFtr(p2));
				case "curry":
					return new TransExp.Curry(toTrans(p2), true);
				case "CURRY":
					return new TransExp.Curry(toTrans(p2), false);
				case "true":
					return new Bool(true, toCat(p2));
				case "false":
					return new Bool(false, toCat(p2));
				case "char":
					return new Chr(toTrans(p2));
				case "kernel":
					return new Ker(toTrans(p2));
				case "not":
				case "and":
				case "implies":
				case "or":
					return new AndOrNotImplies(p1, toCat(p2));
			default:
				break;
			}
		} catch (RuntimeException re) { }

		if (o instanceof String) {
			return new TransExp.Var(o.toString());
		}
		
		throw new RuntimeException("Could not create transform from " + o); 
	}
	
	private static FunctorExp toSetSet(Object decl) {
		Tuple3 y = (Tuple3) decl;
		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.a;
		
		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.b;
		
		Tuple3 nodes0  = (Tuple3) nodes.b;
		Tuple3 arrows0 = (Tuple3) arrows.b;

		Tuple5 arrows1 = (Tuple5) arrows0.a;
		
		SetSetConst ret = new SetSetConst(nodes0.a.toString(), toSet(nodes0.c), arrows1.a.toString(), arrows1.c.toString(), arrows1.e.toString(), toFn(arrows0.c));
		return ret;
	}
	
//	@SuppressWarnings("rawtypes")
	private static FunctorExp toInstConst(Object decl) {
		Tuple3 y = (Tuple3) decl;
		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.a;
		
		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.b;
		
		List nodes0 = (List) nodes.b;
		List arrows0 = (List) arrows.b;


		Map<String, SetExp> nodesX = new HashMap<>();
		for (Object o : nodes0) {
			if (nodesX.containsKey(o)) {
				throw new RuntimeException("Duplicate object: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			SetExp l = toSet(u.c);
			nodesX.put(n, l);
		}
		
		Map<String, Chc<FnExp,SetExp>> arrowsX = new HashMap<>();
		for (Object o : arrows0) {
			if (arrowsX.containsKey(o)) {
				throw new RuntimeException("Duplicate arrow: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			try {
				FnExp l = toFn(u.c);
				arrowsX.put(n, Chc.inLeftNC(l));
			} catch (Exception eee) {
				SetExp l = toSet(u.c);
				arrowsX.put(n, Chc.inRightNC(l));				
			}
		}
		InstConst ret = new InstConst(toCat(y.c), nodesX, arrowsX);
		return ret;
	}
	
	private static FunctorExp toCatFtrConst(Object decl) {
		Tuple5 y = (Tuple5) decl;
		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.a;
		
		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.b;
		
		List nodes0 = (List) nodes.b;
		List arrows0 = (List) arrows.b;

		Map<String, CatExp> nodesX = new HashMap<>();
		for (Object o : nodes0) {
			if (nodesX.containsKey(o)) {
				throw new RuntimeException("Duplicate object: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			CatExp l = toCat(u.c);
			nodesX.put(n, l);
		}

		Map<String, FunctorExp> arrowsX = new HashMap<>();
		for (Object o : arrows0) {
			if (arrowsX.containsKey(o)) {
				throw new RuntimeException("Duplicate arrow: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			FunctorExp l = toFtr(u.c);
			arrowsX.put(n, l);
		}
		CatConst ret = new CatConst(toCat(y.c), nodesX, arrowsX);
		return ret;
	}
	
	private static FunctorExp toFinalConst(Object decl) {
		Tuple5 y = (Tuple5) decl;
		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.a;
		
		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.b;
		
		List nodes0 = (List) nodes.b;
		List arrows0 = (List) arrows.b;

		Map<String, FunctorExp> nodesX = new HashMap<>();
		for (Object o : nodes0) {
			if (nodesX.containsKey(o)) {
				throw new RuntimeException("Duplicate object: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			FunctorExp l = toFtr(u.c);
			nodesX.put(n, l);
		}

		Map<String, TransExp> arrowsX = new HashMap<>();
		for (Object o : arrows0) {
			if (arrowsX.containsKey(o)) {
				throw new RuntimeException("Duplicate arrow: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			TransExp l = toTrans(u.c);
			arrowsX.put(n, l);
		}
		FinalConst ret = new FinalConst(toCat(y.c), toCat(y.e), nodesX, arrowsX);
		return ret;
	}

	private static FunctorExp toMapConst(Object decl) {
		Tuple5 y = (Tuple5) decl;
		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.a;
		
		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.b;
		
		List nodes0 = (List) nodes.b;
		List arrows0 = (List) arrows.b;


		Map<String, String> nodesX = new HashMap<>();
		for (Object o : nodes0) {
			if (nodesX.containsKey(o)) {
				throw new RuntimeException("Duplicate object: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			String l = u.c.toString();
			nodesX.put(n, l);
		}
		
		Map<String, Pair<String, List<String>>> arrowsX = new HashMap<>();
		for (Object o : arrows0) {
			if (arrowsX.containsKey(o)) {
				throw new RuntimeException("Duplicate arrow: " + o + " in " + decl);
			}
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			List<String> l = (List<String>) u.c;
			String ll = l.remove(0);
			arrowsX.put(n, new Pair<>(ll, l));
		}
		MapConst ret = new MapConst(toCat(y.c), toCat(y.e), nodesX, arrowsX);
		return ret;
	}

	
	private static FunctorExp toFtr(Object o) {
		try {
			
			Tuple5 p = (Tuple5) o;
			
			if (p.a.toString().equals("apply")) {
				FunctorExp f = toFtr(p.b);
				FunctorExp e = toFtr(p.e);
				return new Apply(f, e);
			} 
			
			if (p.e.toString().equals("Set")) {
                return p.c.toString().equals("Set") ? toSetSet(o) : toInstConst(o);
			} else if (p.e.toString().equals("Cat")) {
				return toCatFtrConst(o);
			} else {
				if (p.e instanceof Tuple3) {
					Tuple3 t = (Tuple3) p.e;
					if ((t.a.toString().equals("Cat") || t.a.toString().equals("Set")) && t.b.toString().equals("^")) {
						return toFinalConst(o);
					} 
						throw new RuntimeException();
					
				}
				return toMapConst(o);
			}
		} catch (Exception e) { }
		
		try {
			Tuple3 p = (Tuple3) o;

			Object p2 = p.b;
			Object p3 = p.c;
			Object o1 = p.a;
			String p1 = p.a.toString();

			if (p1.equals("fst")) {
				return new Fst(toCat(p2), toCat(p3));
			} else if (p1.equals("snd")) {
				return new Snd(toCat(p2), toCat(p3));
			}  else if (p1.equals("inl")) {
				return new Inl(toCat(p2), toCat(p3));
			} else if (p1.equals("inr")) {
				return new Inr(toCat(p2), toCat(p3));
			} else if (p1.equals("iso1")) {
				return new Iso(true, toCat(p2), toCat(p3));
			} else if (p1.equals("iso2")) {
				return new Iso(false, toCat(p2), toCat(p3));
			} else if (p1.equals("eval")) {
				return new Eval(toCat(p2), toCat(p3));
			} else if (p2.toString().equals(";")) {
				return new Comp(toFtr(o1), toFtr(p3));
			} else if (p2.toString().equals("*")) {
				return new Prod(toFtr(o1), toFtr(p3));
			} else if (p2.toString().equals("+")) {
				return new Case(toFtr(o1), toFtr(p3));
			} else if (p2.toString().equals("^")) {
				return new Exp(toFtr(o1), toFtr(p3));
			} else if (p1.equals("unit")) {
				return new One(toCat(p.b), toCat(p.c));
			} else if (p1.equals("void")) {
				return new Zero(toCat(p.b), toCat(p.c));
			} else if (p1.equals("pushout")) {
				return new Pushout(p.b.toString(), p.c.toString());
			}

		} catch (RuntimeException re) {
		}


		try {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			String p1 = p.a.toString();
			Object p2 = p.b;
			switch (p1) {
				case "id":
					return new Id(toCat(p2));
				case "prop":
					return new Prop(toCat(p2));
				case "curry":
					return new Curry(toFtr(p2));
				case "uncurry":
					return new Uncurry(toFtr(p2));
				case "delta":
				case "sigma":
				case "pi":
					return new Migrate(toFtr(p2), p1);
				case "ff":
					return new FF(toCat(p2));
				case "tt":
					return new TT(toCat(p2));
				case "dom":
					return new Dom(p2.toString(), true);
				case "cod":
					return new Dom(p2.toString(), false);
				case "pivot":
					return new Pivot(toFtr(p2), true);
				case "unpivot":
					return new Pivot(toFtr(p2), false);
			default:
				break;
			} 
		} catch (RuntimeException re) {

		}

		if (o instanceof String) {
			return new Var(o.toString());
		}

		throw new RuntimeException("Bad: " + o); 
	}

	public static FQLPPProgram program(String s) {
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

			Tuple3 t = (Tuple3) decl;
			String kind = t.a.toString();
			String name = t.b.toString();
			switch (kind) {
			case "set":
				Tuple4 tt = (Tuple4) decl;
		//		name = (String) t.b;
				ret.add(NewDecl.setDecl(name, idx, toSet(tt.d)));
				break;
			case "function":
				Tuple4 t0 = (Tuple4) decl;
			//	name = (String) t.b;
				ret.add(NewDecl.fnDecl(name, idx, toFn(t0.d)));
				break;
			case "functor":
				Tuple4 ti = (Tuple4) decl;
			//	name = (String) t.b;
				ret.add(NewDecl.ftrDecl(name, idx, toFtr(ti.d)));
				break;
			case "category":
				Tuple4 tx = (Tuple4) decl;
			//	name = (String) t.b;
				ret.add(NewDecl.catDecl(name, idx, toCat(tx.d)));
				break;
			case "transform":
				Tuple4 te = (Tuple4) decl;
			//	name = (String) t.b;
				ret.add(NewDecl.transDecl(name, idx, toTrans(te.d)));
				break;
			
			default:
				throw new RuntimeException("Unknown decl: " + kind);
			}
		}

		return new FQLPPProgram(ret); 
	}

		private static Parser<List<String>> path() {
			return Identifier.PARSER.sepBy1(term("."));
		}

	private static Parser<?> section(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p.sepBy(term(",")), term(";"));
	}

	private static Parser<?> string() {
		return Parsers.or(StringLiteral.PARSER,
				IntegerLiteral.PARSER, Identifier.PARSER);
	}

}
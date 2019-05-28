package catdata.fpql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.fpql.XExp.Apply;
import catdata.fpql.XExp.Compose;
import catdata.fpql.XExp.Id;
import catdata.fpql.XExp.Var;
import catdata.fpql.XExp.XBool;
import catdata.fpql.XExp.XCoApply;
import catdata.fpql.XExp.XConst;
import catdata.fpql.XExp.XCoprod;
import catdata.fpql.XExp.XCounit;
import catdata.fpql.XExp.XDelta;
import catdata.fpql.XExp.XEq;
import catdata.fpql.XExp.XFF;
import catdata.fpql.XExp.XFn;
import catdata.fpql.XExp.XGrothLabels;
import catdata.fpql.XExp.XIdPoly;
import catdata.fpql.XExp.XInj;
import catdata.fpql.XExp.XLabel;
import catdata.fpql.XExp.XMapConst;
import catdata.fpql.XExp.XMatch;
import catdata.fpql.XExp.XOne;
import catdata.fpql.XExp.XPair;
import catdata.fpql.XExp.XPi;
import catdata.fpql.XExp.XProj;
import catdata.fpql.XExp.XPushout;
import catdata.fpql.XExp.XRel;
import catdata.fpql.XExp.XSchema;
import catdata.fpql.XExp.XSigma;
import catdata.fpql.XExp.XTT;
import catdata.fpql.XExp.XTimes;
import catdata.fpql.XExp.XToQuery;
import catdata.fpql.XExp.XTransConst;
import catdata.fpql.XExp.XTy;
import catdata.fpql.XExp.XUberPi;
import catdata.fpql.XExp.XUnit;
import catdata.fpql.XExp.XVoid;
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

import catdata.Pair;
import catdata.Triple;
import catdata.fpql.XExp.FLOWER2;
import catdata.fpql.XExp.Flower;
import catdata.fpql.XExp.XInst;
import catdata.fpql.XExp.XSOED;
import catdata.fpql.XExp.XSuperED;
import catdata.fpql.XExp.XSOED.FOED;
import catdata.fpql.XExp.XSuperED.SuperFOED;
import catdata.fpql.XPoly.Block;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
class XParser {

	static final Parser<Integer> NUMBER = IntegerLiteral.PARSER
			.map(Integer::valueOf);

	private static final String[] ops = new String[] { ",", ".", ";", ":", "{", "}", "(",
			")", "=", "->", "+", "*", "^", "|", "?", "@", };
	

	private static final String[] res = new String[] { "forall", "exists", "supersoed", "soed", "on", "pushout", "coapply", "grothlabels", "idpoly", "labels", "uberpi", "hom", "for", "polynomial", "attributes", "not", "id", "ID", "apply", "iterate", "true", "false", "FLOWER", "and", "or", "INSTANCE", "as", "flower", "select", "from", "where", "unit", "tt", "pair", "fst", "snd", "void", "ff", "inl", "inr", "case", "relationalize", "return", "coreturn", "variables", "type", "constant", "fn", "assume", "nodes", "edges", "equations", "schema", "mapping", "instance", "homomorphism", "delta", "sigma", "pi" };

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT,
			Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	private static final Parser<?> TOKENIZER = Parsers.or(
			(Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), (Parser<?>) Identifier.TOKENIZER,
			(Parser<?>) IntegerLiteral.TOKENIZER);

	private static Parser<?> term(String... names) {
		return RESERVED.token(names);
	}

	private static Parser<?> ident() {
		return string(); //Terminals.Identifier.PARSER;
	}

	private static final Parser<?> program = program().from(TOKENIZER, IGNORED);

	private static Parser<?> program() {
		return Parsers.tuple(decl().source().peek(), decl()).many();
	}
	
	private static Parser<?> exp() {
		Reference ref = Parser.newReference();

		Parser<?> sigma = Parsers.tuple(term("sigma"), ref.lazy(), ref.lazy());
		Parser<?> delta = Parsers.tuple(term("delta"), ref.lazy(), ref.lazy());
		Parser<?> pi = Parsers.tuple(term("pi"), ref.lazy(), ref.lazy());
		
		Parser<?> rel = Parsers.tuple(term("relationalize"), ref.lazy());

		Parser<?> coprod = Parsers.tuple(term("("), ref.lazy(), term("+"), ref.lazy(), term(")"));
		Parser<?> inl = Parsers.tuple(term("inl"), ref.lazy(), ref.lazy());
		Parser<?> inr = Parsers.tuple(term("inr"), ref.lazy(), ref.lazy());
		Parser<?> match = Parsers.tuple(term("case"), ref.lazy(), ref.lazy());
		Parser<?> zero = Parsers.tuple(term("void"), ref.lazy());
		Parser<?> ff = Parsers.tuple(term("ff"), ref.lazy());
		
		Parser<?> prod = Parsers.tuple(term("("), ref.lazy(), term("*"), ref.lazy(), term(")"));
		Parser<?> fst = Parsers.tuple(term("fst"), ref.lazy(), ref.lazy());
		Parser<?> snd = Parsers.tuple(term("snd"), ref.lazy(), ref.lazy());
		Parser<?> pair = Parsers.tuple(term("pair"), ref.lazy(), ref.lazy());
		Parser<?> unit = Parsers.tuple(term("unit"), ref.lazy());
		Parser<?> tt = Parsers.tuple(term("tt"), ref.lazy());
		
		Parser<?> ret = Parsers.tuple(term("return"), term("sigma"), term("delta"), ref.lazy(), ref.lazy());
		Parser<?> counit = Parsers.tuple(term("coreturn"), term("sigma"), term("delta"), ref.lazy(), ref.lazy());
		Parser<?> unit1 = Parsers.tuple(term("return"), term("delta"), term("pi"), ref.lazy(), ref.lazy());
		Parser<?> counit1 = Parsers.tuple(term("coreturn"), term("delta"), term("pi"), ref.lazy(), ref.lazy());
		
		Parser<?> flower = flower(ref);
		Parser<?> FLOWER = FLOWER(ref);
		Parser id1 = Parsers.tuple(term("id"), ref.lazy());
		Parser id2 = Parsers.tuple(term("ID"), ref.lazy());
		Parser<?> comp = Parsers.tuple(term("("), ref.lazy(), term(";"), ref.lazy(), term(")"));
		
//		Parser<?> query = query(ref);
		Parser<?> apply = Parsers.tuple(term("apply"), ref.lazy(), ref.lazy());
		Parser<?> iter = Parsers.tuple(term("iterate"), IntegerLiteral.PARSER, ref.lazy(), ref.lazy());
		
		Parser<?> hom = Parsers.tuple(term("hom"), ref.lazy(), ref.lazy());
		Parser<?> uberpi = Parsers.tuple(term("uberpi"), ref.lazy());
		Parser<?> labels = Parsers.tuple(term("labels"), ref.lazy());
		Parser<?> glabels = Parsers.tuple(term("grothlabels"), ref.lazy());
		Parser<?> idpoly = Parsers.tuple(term("idpoly"), ref.lazy());
		Parser<?> coapply = Parsers.tuple(term("coapply"), ref.lazy(), ref.lazy());
		Parser<?> pushout = Parsers.tuple(term("pushout"), ref.lazy(), ref.lazy());

		Parser<?> soed = soed();
		Parser<?> supersoed = superSoed();
		
		Parser<?> a = Parsers.or(supersoed, soed, pushout, coapply, glabels, idpoly, labels, uberpi, hom, poly(ref), id1, id2, comp, apply, iter, FLOWER, flower, prod, fst, snd, pair, unit, tt, zero, ff, coprod, inl, inr, match, rel, pi, ret, counit, unit1, counit1, ident(), schema(), mapping(ref), instance(ref), transform(ref), sigma, delta);

		ref.set(a);

		return a;
	}

	public static Parser<?> type() {
		return Parsers.tuple(term("type"), Parsers.always());
	}
	
	public static Parser<?> fn() {
		return Parsers.tuple(term("fn"), ident(), term("->"), ident(), Parsers.always());
	}
	
	public static Parser<?> constx() {
		return Parsers.tuple(term("constant"), ident(), Parsers.always());
	}
	
	public static Parser<?> assume() {
		return Parsers.tuple(term("assume"), path(), term("="), path());
	}
	
	private static Parser<?> superSoed() {
		Parser<?> es = Parsers.tuple(ident(), term(":"), ident().sepBy(term(",")), term("->"),
				ident());
		
		//bulb
		//bulb . path
		//path
		Parser bulb = Parsers.tuple(term("@"), ident(), Parsers.tuple(term("("), path().sepBy(term(",")), term(")")));
		Parser bulbpath = Parsers.tuple(bulb, term("."), path());
		
		Parser superPath = Parsers.or(new Parser[] { bulbpath, bulb, path()});
		
		Parser<?> x = Parsers.tuple(superPath, term("="), superPath);
		Parser<?> y = Parsers.tuple(x.sepBy(term(",")), term("->"), x.sepBy(term(",")));
		Parser<?> a = Parsers.tuple(term("forall"), Parsers.tuple(ident(),term(":"), ident()).sepBy(term(",")), term(","), y);
		Parser<?> foo = Parsers.tuple(term("exists"), es.sepBy(term(",")), term(";"), a.followedBy(term(";")).many());
		
		Parser<?> p = Parsers.between(term("supersoed").followedBy(term("{")), foo, term("}"));
		Parser<?> q = Parsers.tuple(term(":"), ident().followedBy(term("->")), ident().followedBy(term("on")), ident());
		return Parsers.tuple(p, q);
	}
	
	private static Parser<?> soed() {
		Parser<?> es = Parsers.tuple(ident(), term(":"), ident(), term("->"),
				ident());
		Parser<?> x = Parsers.tuple(path(), term("="), path());
		Parser<?> a = Parsers.tuple(term("forall"), ident().followedBy(term(":")), ident().followedBy(term(",")), x.sepBy(term(",")));
		Parser<?> foo = Parsers.tuple(term("exists"), es.sepBy(term(",")), term(";"), a.followedBy(term(";")).many());
		
		Parser<?> p = Parsers.between(term("soed").followedBy(term("{")), foo, term("}"));
		Parser<?> q = Parsers.tuple(term(":"), ident().followedBy(term("->")), ident().followedBy(term("on")), ident());
		return Parsers.tuple(p, q);
	}
	
	private static Parser<?> schema() {
		Parser<?> p1 = ident();
		Parser<?> pX = Parsers.tuple(ident(), term(":"), ident(), term("->"),
				ident());
		Parser<?> p3 = Parsers.tuple(path(), term("="), path());
		Parser<?> foo = Parsers.tuple(section("nodes", p1), 
				section("edges", pX),
				section("equations", p3));
		return Parsers.between(term("schema").followedBy(term("{")), foo, term("}"));
	}
	
	
	private static XSchema toCatConst(Object y) {
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
		//Parser e = Parsers.or(new Parser[] { exp(), type(), fn(), constx(), assume() });
		
		Parser p0 = Parsers.tuple(Parsers.tuple(ident(), term(":"), ident()).between(term("("), term(")")), term("="), exp());
		Parser p1 = Parsers.tuple(ident(), term("="), exp());
		Parser p3 = Parsers.tuple(ident().many1(), term(":"), Parsers.tuple(ident(), term("->"), ident()));
		Parser p4 = Parsers.tuple(ident().many1(), term(":"), term("type"));
		Parser p5 = Parsers.tuple(ident(), term(":"), Parsers.tuple(path(), term("="), path()));
		Parser p2 = Parsers.tuple(ident().many1(), term(":"), ident());
		
		return Parsers.or(new Parser[] {p0, p1, p3, p4, p5, p2});
		
//		return Parsers.tuple(ident(), Parsers.or(term("="), term(":")), e);
	}
	
/*	public static final Parser<?> query(Reference ref) {
		Parser<?> xxx = ref.lazy().between(term("pi"), term(";"));
		Parser<?> yyy = ref.lazy().between(term("delta"), term(";"));
		Parser<?> zzz = ref.lazy().between(term("sigma"), term(";"));
		Parser p = Parsers.tuple(xxx, yyy, zzz);
		
		Parser<?> ret = Parsers.tuple(term("query"), p.between(term("{"), term("}")));
		return ret;
	} */
		
	private static Parser<?> instance(Reference ref) {
		Parser<?> node = Parsers.tuple(ident().many1(), term(":"), ident());
		Parser<?> p3 = Parsers.tuple(path(), term("="), path());
		Parser<?> xxx = Parsers.tuple(section("variables", node), 
				section("equations", p3));
		Parser kkk = ((Parser)term("INSTANCE")).or(term("instance"));
		Parser<?> constant = Parsers
				.tuple(kkk, xxx.between(term("{"), term("}")), term(":"),
						ref.lazy());
		return constant;
	} 
	
	private static Parser<?> mapping(Reference ref) {
		Parser<?> node = Parsers.tuple(ident(), term("->"), ident());
		Parser<?> arrow = Parsers.tuple(
				ident(),
				term("->"),
				path());

		Parser<?> xxx = Parsers.tuple(section("nodes", node), 
				section("edges", arrow));
		Parser<?> constant = Parsers
				.tuple(Parsers.between(term("mapping").followedBy(term("{")), xxx, term("}")), term(":"),
						ref.lazy(), term("->"), ref.lazy());
		return constant;
	} 
	
	private static Parser<?> transform(Reference ref) {
		Parser p = Parsers.tuple(ident(), term(":"), ident());
		Parser<?> node = Parsers.tuple(p.or(ident()), term("->"), path());
		Parser<?> xxx =section("variables", node);
		Parser<?> constant = Parsers
				.tuple(Parsers.between(term("homomorphism").followedBy(term("{")), xxx, term("}")), term(":"),
						ref.lazy(), term("->"), ref.lazy());
		return constant;
	} 
	
	public static Parser<?> section2(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p, term(";"));
	}
		
	
/*
	
//	@SuppressWarnings("rawtypes")
	public static FunctorExp toInstConst(Object decl) {
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
				arrowsX.put(n, Chc.inLeft(l));
			} catch (Exception eee) {
				SetExp l = toSet(u.c);
				arrowsX.put(n, Chc.inRight(l));				
			}
		}
		InstConst ret = new InstConst(toCat(y.c), nodesX, arrowsX);
		return ret;
	}
	*/
	/*
	public static FunctorExp toCatFtrConst(Object decl) {
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
	*/

	/*public static FunctorExp toMapConst(Object decl) {
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
	} */

	public static List path(String s) {
		Parser p = Parsers.or(StringLiteral.PARSER,
				              IntegerLiteral.PARSER,
				              Identifier.PARSER);
		Parser e = Parsers.tuple(p, term(","), p);
		Parser q = Parsers.between(term("("), e, term(")"));
		Parser a = Parsers.or(q, p).sepBy1(term("."));

		List l = (List) a.from(TOKENIZER, IGNORED).parse(s);
		List ret = new LinkedList();
		
		for (Object o : l) {
			if (o instanceof Tuple3) {
				Tuple3 z = (Tuple3) o;
				ret.add(new Pair(z.a, z.c));
			} else {
				ret.add(o);
			}
		}
		
		return ret;
	}
	
	public static XProgram program(String s) {
		List<Triple<String, Integer, XExp>> ret = new LinkedList<>();
		List decls = (List) program.parse(s);

		for (Object d : decls) {
			org.jparsec.functors.Pair pr = (org.jparsec.functors.Pair) d;
			Tuple3 decl = (Tuple3) pr.b;
			
			if (decl.a instanceof List) {
				List l = (List) decl.a;
				for (Object o : l) {
					toProgHelper(pr.a.toString(), s, ret, new Tuple3(o, decl.b, decl.c));
				}
			} else {
				toProgHelper(pr.a.toString(), s, ret, decl);
			}
		}

		return new XProgram(ret); 
	}

private static void toProgHelper(String z, String s, List<Triple<String, Integer, XExp>> ret, Tuple3 decl) {
	String txt = z;
	int idx = s.indexOf(txt);
	if (idx < 0) {
		throw new RuntimeException();
	}

	if (decl.a instanceof Tuple3) {
		Tuple3 t = (Tuple3) decl.a;
		Object ooo = toExp(decl.c);
		if (ooo instanceof Flower) {
			Flower f = (Flower) toExp(decl.c);
			f.ty = t.c.toString();
			ret.add(new Triple<>(t.a.toString(), idx, f));				
		} else if (ooo instanceof FLOWER2) {
			FLOWER2 f = (FLOWER2) toExp(decl.c);
			f.ty = t.c.toString();
			ret.add(new Triple<>(t.a.toString(), idx, f));				
		} else {
			throw new RuntimeException("Can only use (v:T) for flowers");
		}
	} else { 
		String name = decl.a.toString();
		if (decl.b.toString().equals(":")) {
			ret.add(new Triple<>(name, idx, newToExp(decl.c)));				
		} else {
			ret.add(new Triple<>(name, idx, toExp(decl.c)));
		}
	}
}
	

	private static XExp newToExp(Object c) {
		if (c.toString().equals("type")) {
			return new XTy("");
		}
		if (c instanceof String) {
			return new XConst((String)c, "");
		}
		Tuple3 t = (Tuple3) c;
		if (t.b.toString().equals("->")) {
			return new XFn((String)t.a, (String)t.c, "");
		}
		return new XEq((List<String>) t.a, (List<String>) t.c);

	}
	/*
J = soed {
	exists f:A->B, g:C->D;
	forall a:A, a.f = p.q, a.g = p.f;
	forall b:B, p = q; 
} : X -> Y on I
	 */
	private static XSOED fromSoed(Object ooo) {
		org.jparsec.functors.Pair ooo1 = (org.jparsec.functors.Pair) ooo;
		
		
		Tuple4 a = (Tuple4) ooo1.a;
		List<Triple<String, String, String>> es = new LinkedList<>();
		List<FOED> as = new LinkedList<>();
		
		List<Tuple5> es0 = (List<Tuple5>) a.b;
		for (Tuple5 t : es0) {
			es.add(new Triple(t.a, t.c, t.e));
		}
		
		List<Tuple4> as0 = (List<Tuple4>) a.d;
		for (Tuple4 t : as0) {
			List<Tuple3> eqs = (List<Tuple3>) t.d;
			List<Pair<List<String>, List<String>>> eqs0 = new LinkedList<>();
			for (Tuple3 x : eqs) {
				eqs0.add(new Pair(x.a, x.c));
			}
			as.add(new FOED((String)t.b, (String)t.c, eqs0));
		}
				
		Tuple4 b = (Tuple4) ooo1.b;
		String src = (String) b.b;
		String dst = (String) b.c;
		String i = (String) b.d;		
		XSOED ret = new XSOED(es, as, src, dst, i);
		return ret;
	}
/*
	J = soed {
		exists f:A->B, g:C->D;
		forall a:A, a.f = p.q, a.g = p.f;
		forall b:B, p = q; 
	} : X -> Y on I
		 */
		private static XSuperED fromSuperSoed(Object ooo) {
			org.jparsec.functors.Pair ooo1 = (org.jparsec.functors.Pair) ooo;
			
			Tuple4 a = (Tuple4) ooo1.a;
		//	List<Triple<String, List<String>, String>> es = new LinkedList<>();
			List<SuperFOED> as = new LinkedList<>();
			Map<String, List<String>> dom = new HashMap<>();
			Map<String, String> cod = new HashMap<>();
			
			List<Tuple5> es0 = (List<Tuple5>) a.b;
			for (Tuple5 t : es0) {
				if (dom.keySet().contains(t.a)) {
					throw new RuntimeException("Duplicate function name " + t.a);
				}
				dom.put((String) t.a, (List<String>) t.c);
				cod.put((String) t.a, (String) t.e);
			}
			
			List<Tuple4> as0 = (List<Tuple4>) a.d;
			for (Tuple4 t : as0) {
				List<Tuple3> aas = (List<Tuple3>) t.b;
				Map<String, String> aa = new HashMap<>();
				for (Tuple3 xxx : aas) {
					if (aa.containsKey(xxx.a)) {
						throw new RuntimeException("Duplicate var " + xxx.a);
					}
					aa.put((String) xxx.a, (String) xxx.c);
				}
				
				Tuple3 td = (Tuple3) t.d;
				List<Tuple3> lhss = (List<Tuple3>) td.a;
				List<Tuple3> rhss = (List<Tuple3>) td.c;
				
				List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>> cc = new LinkedList<>();
				List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>> bb = new LinkedList<>();

				for (Tuple3 o : lhss) {
					bb.add(new Pair<>(fromBulb(o.a), fromBulb(o.c)));
				}
				for (Tuple3 o : rhss) {
					cc.add(new Pair<>(fromBulb(o.a), fromBulb(o.c)));
				}
				
				as.add(new SuperFOED(aa , bb, cc));
			}
					
			Tuple4 b = (Tuple4) ooo1.b;
			String src = (String) b.b;
			String dst = (String) b.c;
			String i = (String) b.d;		
			XSuperED ret = new XSuperED(dom, cod, as, src, dst, i); //es, as, src, dst, i);
			return ret;
		}
	

		private static Triple<String, List<List<String>>, List<String>> fromBulb(Object o) {
			try {
				Tuple3 t = (Tuple3) o;
				Tuple3 a = (Tuple3) t.a;
				Tuple3 b = (Tuple3) a.c;
				return new Triple<>((String)a.b, (List<List<String>>) b.b, (List<String>)t.c);
			} catch (Exception ee) { 
			}
			try {
				Tuple3 a = (Tuple3) o;
				Tuple3 b = (Tuple3) a.c;
				return new Triple<>((String)a.b, (List<List<String>>) b.b, null);
			} catch (Exception ee) { 
			}
			return new Triple<>(null, null, (List<String>) o);
		}

	private static XExp toExp(Object c) {
		if (c instanceof String) {
			return new Var((String) c);
		}
		
		try {
			return fromPoly((Tuple4)c);
		} catch (Exception e) { }
		
		
		try {
			return fromSoed(c);
		} catch (Exception e) {
		}
		
		
		
		try {
			return toCatConst(c);
		} catch (Exception e) { }
		
		try {
			if (c.toString().contains("variables")) {
				return toInstConst(c);
			}
		} catch (Exception e) { }
		
		try {
			return toMapping(c);
		} catch (Exception e) { }
		
		try {
			return toTrans(c);
		} catch (Exception e) { }
		
		if (c instanceof Tuple5) {
			Tuple5 p = (Tuple5) c; 
			if (p.c.toString().equals("+")) {
				return new XCoprod(toExp(p.b), toExp(p.d));
			}
			if (p.c.toString().equals("*")) {
				return new XTimes(toExp(p.b), toExp(p.d));
			}
			if (p.c.toString().equals(";")) {
				return new Compose(toExp(p.b), toExp(p.d));
			}
			if (p.a.toString().equals("return") && p.b.toString().equals("sigma")) {
				return new XUnit("sigma", toExp(p.d), toExp(p.e));
			}
			if (p.a.toString().equals("coreturn") && p.b.toString().equals("sigma")) {
				return new XCounit("sigma", toExp(p.d), toExp(p.e));
			}
			if (p.a.toString().equals("return") && p.b.toString().equals("delta")) {
				return new XUnit("pi", toExp(p.d), toExp(p.e));
			}
			if (p.a.toString().equals("coreturn") && p.b.toString().equals("delta")) {
				return new XCounit("pi", toExp(p.d), toExp(p.e));
			}

			return new XFn((String) p.b, (String) p.d, (String) p.e);
		}
		if (c instanceof Tuple4) {
			Tuple4 p = (Tuple4) c;
			return new XEq((List<String>) p.b, (List<String>) p.d);
		} 
		if (c instanceof Tuple3) {
			Tuple3 p = (Tuple3) c;
			if (p.a.toString().equals("flower")) {
				XExp I = toExp(p.c);
				Tuple3 q = (Tuple3) p.b;
				
				List s = (List) ((org.jparsec.functors.Pair) q.a).b; //list of tuple3 of (path, string)
				List f = (List) ((org.jparsec.functors.Pair) q.b).b; //list of tuple3 of (string, string)
				List w = (List) ((org.jparsec.functors.Pair) q.c).b; //list of tuple3 of (path, path)
				
				Map<Object, List<Object>> select = new HashMap<>();
				Map<Object, Object> from = new HashMap<>();
				List<Pair<List<Object>, List<Object>>> where = new LinkedList<>();
				
				Set<String> seen = new HashSet<>();
				for (Object o : w) {
					Tuple3 t = (Tuple3) o;
					List lhs = (List) t.a;
					List rhs = (List) t.c;
					where.add(new Pair<>(rhs, lhs));
				}
				for (Object o : s) {
					Tuple3 t = (Tuple3) o;
					List lhs = (List) t.a;
					String rhs = t.c.toString();
					if (seen.contains(rhs)) {
						throw new RuntimeException("Duplicate AS name: " + rhs + " (note: AS names can't be used in the schema either)");
					}
					seen.add(rhs);
					select.put(rhs, lhs);
				}
				for (Object o : f) {
					Tuple3 t = (Tuple3) o;
					String lhs = t.a.toString();
					String rhs = t.c.toString();
					if (seen.contains(rhs)) {
						throw new RuntimeException("Duplicate AS name: " + rhs + " (note: AS names can't be used in the schema either)");
					}
					seen.add(rhs);
					from.put(rhs, lhs);
				}
				
				return new Flower(select, from, where, I);
			}
			if (p.a.toString().equals("FLOWER")) {
				XExp I = toExp(p.c);
				Tuple3 q = (Tuple3) p.b;
				
				List s = (List) ((org.jparsec.functors.Pair) q.a).b; //list of tuple3 of (path, string)
				List f = (List) ((org.jparsec.functors.Pair) q.b).b; //list of tuple3 of (string, string)
				Object w =  ((org.jparsec.functors.Pair) q.c).b; //list of tuple3 of (path, path)
				
				Map<Object, List<Object>> select = new HashMap<>();
				Map<Object, Object> from = new HashMap<>();
			//	List<Pair<List<String>, List<String>>> where = new LinkedList<>();
				
				Set<String> seen = new HashSet<>();
				for (Object o : s) {
					Tuple3 t = (Tuple3) o;
					List lhs = (List) t.a;
					String rhs = t.c.toString();
					if (seen.contains(rhs)) {
						throw new RuntimeException("Duplicate AS name: " + rhs + " (note: AS names can't be used in the schema either)");
					}
					seen.add(rhs);
					select.put(rhs, lhs);
				}
				for (Object o : f) {
					Tuple3 t = (Tuple3) o;
					String lhs = t.a.toString();
					String rhs = t.c.toString();
					if (seen.contains(rhs)) {
						throw new RuntimeException("Duplicate AS name: " + rhs + " (note: AS names can't be used in the schema either)");
					}
					seen.add(rhs);
					from.put(rhs, lhs);
				}
				
				return new FLOWER2(select, from, toWhere(w), I);
			}
			
			if (p.a.toString().equals("pushout")) {
				return new XPushout(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("hom")) {
				return new XToQuery(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("sigma")) {
				return new XSigma(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("delta")) {
				return new XDelta(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("pi")) {
				return new XPi(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("inl")) {
				return new XInj(toExp(p.b), toExp(p.c), true);
			}
			if (p.a.toString().equals("inr")) {
				return new XInj(toExp(p.b), toExp(p.c), false);
			}
			if (p.a.toString().equals("case")) {
				return new XMatch(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("fst")) {
				return new XProj(toExp(p.b), toExp(p.c), true);
			}
			if (p.a.toString().equals("snd")) {
				return new XProj(toExp(p.b), toExp(p.c), false);
			}
			if (p.a.toString().equals("pair")) {
				return new XPair(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("apply")) {
				return new Apply(toExp(p.b), toExp(p.c));
			}
			if (p.a.toString().equals("coapply")) {
				return new XCoApply(toExp(p.b), toExp(p.c));
			}
			return new XConst((String) p.b, (String) p.c);
		}
		if (c instanceof org.jparsec.functors.Pair) {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) c;
			if (p.a.toString().equals("idpoly")) {
				return new XIdPoly(toExp(p.b));
			}
			if (p.a.toString().equals("uberpi")) {
				return new XUberPi(toExp(p.b));
			}
			if (p.a.toString().equals("labels")) {
				return new XLabel(toExp(p.b));
			}
			if (p.a.toString().equals("grothlabels")) {
				return new XGrothLabels(toExp(p.b));
			}
			if (p.a.toString().equals("relationalize")) {
				return new XRel(toExp(p.b));
			} 
			if (p.a.toString().equals("void")) {
				return new XVoid(toExp(p.b));
			}
			if (p.a.toString().equals("ff")) {
				return new XFF(toExp(p.b));
			}
			if (p.a.toString().equals("unit")) {
				return new XOne(toExp(p.b));
			}
			if (p.a.toString().equals("tt")) {
				return new XTT(toExp(p.b));
			}
			if (p.a.toString().equals("id")) {
				return new Id(false, toExp(p.b));
			}
			if (p.a.toString().equals("ID")) {
				return new Id(true, toExp(p.b));
			}
//			if (p.a.toString().equals("query")) {
	//			Tuple3 t = (Tuple3) p.b;
		//		return new XExp.XQueryExp(toExp(t.a), toExp(t.b), toExp(t.c));
			//}
			
			
				
				try {
					return fromSuperSoed(c);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				return new XTy((String)p.b);
			
		}
		
	
		
		throw new RuntimeException("x: " + c.getClass() + " " + c);
	}
	
	/* public static final Parser<?> instance(Reference ref) {
		Parser<?> node = Parsers.tuple(ident(), term(":"), ident());
		Parser<?> p3 = Parsers.tuple(path(), term("="), path());
		Parser<?> xxx = Parsers.tuple(section("variables", node), 
				section("equations", p3));
		Parser kkk = ((Parser)term("INSTANCE")).or((Parser) term("instance"));
		Parser<?> constant = Parsers
				.tuple(kkk, xxx.between(term("{"), term("}")), term(":"),
						ref.lazy());
		return constant;  */
	private static XInst toInstConst(Object decl) {
		Tuple4 y = (Tuple4) decl;
		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.b;
		
		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.b;
		
		List nodes0 = (List) nodes.b;
		List arrows0 = (List) arrows.b;

		List<Pair<String, String>> nodesX = new LinkedList<>();
		for (Object o : nodes0) {
			Tuple3 u = (Tuple3) o;
			List<String> n2 = (List) u.a;
			String l = (String) u.c;
			
			for (String n : n2) {
		//		String n = (String) u.a;
				nodesX.add(new Pair<>(n, l));
			}
		} 
		
		List<Pair<List<String>, List<String>>> eqsX = new LinkedList<>();
		 for (Object o : arrows0) {
			Tuple3 u = (Tuple3) o;
			List<String> n = (List<String>) u.a;
			List<String> m = (List<String>) u.c;
			eqsX.add(new Pair<>(n, m));
		 }
		XInst ret = new XInst(toExp(y.d), nodesX, eqsX);
		if (y.a.toString().equals("INSTANCE")) {
			ret.saturated = true;
		}
		return ret;
	}
	
	private static XMapConst toMapping(Object decl) {
		Tuple5 y = (Tuple5) decl;
		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.a;
		
		Tuple3 nodes = (Tuple3) x.a;
		Tuple3 arrows = (Tuple3) x.b;
		
		List nodes0 = (List) nodes.b;
		List arrows0 = (List) arrows.b;

		List<Pair<String, String>> nodesX = new LinkedList<>();
		for (Object o : nodes0) {
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			String l = (String) u.c;
			nodesX.add(new Pair<>(n, l));
		} 
		
		List<Pair<String, List<String>>> eqsX = new LinkedList<>();
		 for (Object o : arrows0) {
			Tuple3 u = (Tuple3) o;
			String n = (String) u.a;
			List<String> m = (List<String>) u.c;
			eqsX.add(new Pair<>(n, m));
		 }
		XMapConst ret = new XMapConst(toExp(y.c), toExp(y.e), nodesX, eqsX);
		return ret;
	}
	
	private static XTransConst toTrans(Object decl) {
		Tuple5 y = (Tuple5) decl;
//		org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) y.a;
		
		Tuple3 nodes = (Tuple3) y.a;
//		Tuple3 arrows = (Tuple3) x.b;
		
		List nodes0 = (List) nodes.b;
//		List arrows0 = (List) arrows.b;

		List<Pair<Pair<String, String>, List<String>>> eqsX = new LinkedList<>();
		 for (Object o : nodes0) {
			Tuple3 u = (Tuple3) o;
			List<String> m = (List<String>) u.c;

			if (u.a instanceof Tuple3) {
				Tuple3 n = (Tuple3) u.a;
				eqsX.add(new Pair<>(new Pair<>(n.a.toString(), n.c.toString()), m));
			} else {
				String n = (String) u.a;
				eqsX.add(new Pair<>(new Pair<>(n, null), m));
			}

		 }
		XTransConst ret = new XTransConst(toExp(y.c), toExp(y.e), eqsX);
		return ret;
	}

	private static Parser path() {
		return  Parsers.or(ident()).sepBy1(term("."));
	}
	
	private static XBool toWhere(Object o) {
		if (o instanceof Tuple5) {
			Tuple5 o2 = (Tuple5) o;
			boolean isAnd = o2.c.toString().equals("and");
			return new XBool(toWhere(o2.b), toWhere(o2.d), isAnd);
		}
		if (o instanceof Tuple3) {
			Tuple3 o2 = (Tuple3) o;
			return new XBool((List<Object>)o2.a, (List<Object>)o2.c);
		}
		if (o instanceof org.jparsec.functors.Pair) {
			org.jparsec.functors.Pair x = (org.jparsec.functors.Pair) o;
			return new XBool(toWhere(x.b));

		}
		if (o.toString().equals("true")) {
			return new XBool(true);
		}
		if (o.toString().equals("false")) {
			return new XBool(false);
		}
		throw new RuntimeException();
	}
	
	private static Parser where() {
		Reference ref = Parser.newReference();	
		
		Parser p1 = Parsers.tuple(term("("), ref.lazy(), term("and"), ref.lazy(), term(")"));
		Parser p2 = Parsers.tuple(term("("), ref.lazy(), term("or"), ref.lazy(), term(")"));
		Parser p3 = Parsers.tuple(path(), term("="), path());
		Parser p4 = Parsers.tuple(term("not"), ref.lazy());
		
		Parser p = Parsers.or(p1, p2, p3, p4, term("true"), term("false"));
		
		ref.set(p);
		
		return p;
	}

	private static Parser<?> section(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p.sepBy(term(",")), term(";"));
	}

	 private static Parser<?> string() {
		return Parsers.or(StringLiteral.PARSER,
				IntegerLiteral.PARSER, Identifier.PARSER);
	} 
	 
	private static Parser<?> flower(Reference self) {
		Parser<?> from0 = Parsers.tuple(ident(), term("as"), ident()).sepBy(term(","));
		Parser<?> from = Parsers.tuple(term("from"), from0, term(";"));

		Parser<?> where0 = Parsers.tuple(path(), term("="), path()).sepBy(term(","));
		Parser<?> where = Parsers.tuple(term("where"), where0, term(";")); 

		Parser<?> select0 = Parsers.tuple(path(), term("as"), ident()).sepBy(term(","));
		Parser<?> select = Parsers.tuple(term("select"), select0, term(";"));

		Parser p = Parsers.tuple(select, from, where);
		Parser ret = Parsers.tuple(term("flower"), p.between(term("{"), term("}")), self.lazy());
		
		return ret;
	}
	
	private static Parser<?> FLOWER(Reference self) {
		Parser<?> from0 = Parsers.tuple(ident(), term("as"), ident()).sepBy(term(","));
		Parser<?> from = Parsers.tuple(term("from"), from0, term(";"));

		Parser<?> where = Parsers.tuple(term("where"), where(), term(";")); 

		Parser<?> select0 = Parsers.tuple(path(), term("as"), ident()).sepBy(term(","));
		Parser<?> select = Parsers.tuple(term("select"), select0, term(";"));

		Parser p = Parsers.tuple(select, from, where);
		Parser ret = Parsers.tuple(term("FLOWER"), p.between(term("{"), term("}")), self.lazy());
		
		return ret;
	}
/*{ from a1:A, a2:A;
           where a1.att2=a2.f.att1;
           attributes att3 = a2.att2,
                      att4 = a2.att2;
           edges e1 = {b2=a1.f, b3=a1.f} : q2,
                 e2 = { ... } : q3; 
           } */
	private static Parser<?> block() {
		Parser p1 = Parsers.tuple(ident(), term(":"), ident()).sepBy(term(",")).between(term("for"), term(";"));
		Parser p2 = Parsers.tuple(ident().sepBy1(term(".")), term("="), ident().sepBy1(term("."))).sepBy(term(",")).between(term("where"), term(";"));
		Parser p3 = Parsers.tuple(ident(), term("="), ident().sepBy1(term("."))).sepBy(term(",")).between(term("attributes"), term(";"));
		
		Parser q = Parsers.tuple(ident(), term("="), ident().sepBy1(term("."))).sepBy(term(",")).between(term("{"), term("}"));
		Parser a = Parsers.tuple(ident(), term("="), q, term(":"), ident());
		Parser p4= a.sepBy(term(",")).between(term("edges"), term(";"));
		
		Parser p = Parsers.tuple(p1, p2, p3, p4);
		return p.between(term("{"), term("}"));
	}
	
	private static Block<String, String> fromBlock(Object o) {
		Tuple4<List, List, List, List> t = (Tuple4<List, List, List, List>) o;
		Map<Object, String> from = new HashMap<>();
		Set<Pair<List<Object>, List<Object>>> where = new HashSet<>();
		Map<String, List<Object>> attrs = new HashMap<>();
		Map<String, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
		
		for (Object x : t.a) {
			Tuple3 l = (Tuple3) x;
			if (from.containsKey(l.a.toString())) {
				throw new RuntimeException("Duplicate for: " + l.a);
			}
			from.put(l.a.toString(), l.c.toString());
		}
		
		for (Object x : t.b) {
			Tuple3 l = (Tuple3) x;
			where.add(new Pair(l.a, l.c));
		}
		
		for (Object x : t.c) {
			Tuple3 l = (Tuple3) x;
			if (attrs.containsKey(l.a.toString())) {
				throw new RuntimeException("Duplicate for: " + l.a);
			}
			attrs.put(l.a.toString(), (List<Object>)l.c);
		}
		
		for (Object x : t.d) {
			Tuple5 l = (Tuple5) x;
			if (from.containsKey(l.a.toString())) {
				throw new RuntimeException("Duplicate for: " + l.a);
			}
			edges.put(l.a.toString(), new Pair(l.e.toString(), fromBlockHelper(l.c)));
		}

		return new Block<>(from, where, attrs, edges);
	}
	
	//{b2=a1.f, b3=a1.f}
	private static Map<String, List<String>> fromBlockHelper(Object o) {
		List<Tuple3> l = (List<Tuple3>) o;
		Map<String, List<String>> ret = new HashMap<>();
		for (Tuple3 t : l) {
			if (ret.containsKey(t.a.toString())) {
				throw new RuntimeException("Duplicate column: " + t.a);
			}
			ret.put(t.a.toString(), (List<String>)t.c);
		}
		return ret;
	}
	
	private static Map<Object, Pair<String, Block<String, String>>> fromBlocks(List l) {
		Map<Object, Pair<String, Block<String, String>>> ret = new HashMap<>();
		for (Object o : l) {
			Tuple5 t = (Tuple5) o;
			Block<String, String> b = fromBlock(t.c);
			ret.put(t.a.toString(), new Pair<>(t.e.toString(), b));
		}
		return ret;
	}
	private static XPoly<String, String> fromPoly(Tuple4 o) {
		Map<Object, Pair<String, Block<String, String>>> blocks = fromBlocks((List)o.a);
		return new XPoly<>(toExp(o.b), toExp(o.d), blocks);
	}
	private static Parser<?> poly(Reference ref) {
		Parser p = Parsers.tuple(ident(), term("="), block(), term(":"), ident());
		Parser p2 = p.sepBy(term(",")).between(term("{"), term("}")).between(term("polynomial"), term(":"));
		return Parsers.tuple(p2, ref.lazy(), term("->"), ref.lazy());
	}
}
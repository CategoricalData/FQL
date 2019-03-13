package catdata.fql.parse;

import java.util.ArrayList;
import java.util.List;

import catdata.Pair;

/**
 * 
 * @author ryan
 * 
 *         Combine parsers to make new parsers
 */
public class ParserUtils {

	public static <T> RyanParser<List<T>> many(RyanParser<T> p) {
		return (Tokens s) -> {
                    List<T> ret = new ArrayList<>();
                    try {
                        while (true) {
                            Partial<? extends T> x = p.parse(s);
                            s = x.tokens;
                            ret.add(x.value);
                        }
                    } catch (BadSyntax e) {
                    }
                    return new Partial<>(s, ret);
                };
	}

	/**
	 * 
	 * @param <T>
	 * @param p1
	 * @param p2
	 * @return a parser that matches and ignores p1, followed by p2
	 */
	public static <T> RyanParser<T> seq(RyanParser<?> p1,
                                        RyanParser<T> p2) {
		return (Tokens s) -> {
                    Partial<?> x = p1.parse(s);
                    Partial<T> y = p2.parse(x.tokens);
                    return new Partial<>(y.tokens, y.value);
                };
	}

	public static <T> RyanParser<List<T>> manySep(RyanParser<T> p,
                                                  RyanParser<?> sep) {
		return (Tokens s) -> {
                    try {
                        Partial<T> x = p.parse(s);
                        
                        RyanParser<T> pair_p = seq(sep, p);
                        RyanParser<List<T>> pr = many(pair_p);
                        Partial<List<T>> y = pr.parse(x.tokens);
                        
                        y.value.add(0, x.value);
                        return new Partial<>(y.tokens, y.value);
                    } catch (BadSyntax e) {
                        return new Partial<>(s, new ArrayList<>());
                    }
                };
	}

	/**
	 * @param <T>
	 * @param <U>
	 * @param l
	 * @param u
	 * @param r
	 * @return a parser that matches l u r and returns (l,r)
	 */
	public static <T, U> RyanParser<Pair<T, U>> inside(RyanParser<T> l,
                                                       RyanParser<?> u, RyanParser<U> r) {
		return (Tokens s) -> {
                    Partial<? extends T> l0 = l.parse(s);
                    Partial<?> u0 = u.parse(l0.tokens);
                    Partial<? extends U> r0 = r.parse(u0.tokens);
                    return new Partial<>(r0.tokens, new Pair<>(
                            l0.value, r0.value));
                };
	}

	/**
	 * @param <T>
	 * @param l
	 * @param u
	 * @param r
	 * @return a parser that matches l u r and returns u
	 */
	public static <T> RyanParser<T> outside(RyanParser<?> l,
                                            RyanParser<T> u, RyanParser<?> r) {
		return (Tokens s) -> {
                    Partial<?> l0 = l.parse(s);
                    Partial<? extends T> u0 = u.parse(l0.tokens);
                    Partial<?> r0 = r.parse(u0.tokens);
                    return new Partial<>(r0.tokens, u0.value);
                };
	}

	@SuppressWarnings({"rawtypes"})
	public static RyanParser<Object> or(RyanParser p, RyanParser q) {

		return (Tokens s) -> {
                    try {
                        return p.parse(s);
                    } catch (BadSyntax e) {
                    }
                    return q.parse(s);
                };

	}

}

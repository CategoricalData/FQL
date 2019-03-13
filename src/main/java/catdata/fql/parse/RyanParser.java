package catdata.fql.parse;

/**
 * 
 * @author ryan
 * 
 * @param <T>
 *            the type of thing to parse
 * 
 *            interface for parser combinators
 */
@FunctionalInterface
public interface RyanParser<T> {

	Partial<T> parse(Tokens s) throws BadSyntax;

}

package catdata.fql.parse;

/**
 * 
 * @author ryan
 * 
 *         Parser for strings.
 */
public class StringParser implements RyanParser<String> {

	@Override
	public Partial<String> parse(Tokens s) throws BadSyntax {
		String k = s.peek(0);
		if (!k.equals(";") && !k.equals("}")&& !k.equals(",")&& !k.equals("{")&& !k.equals(")") && !k.equals("=") && !k.equals("/") && !k.equals("\\")) {
			return new Partial<>(s.pop(), s.head());
		}
		throw new BadSyntax(s, "Cannot parse string from " + s);
	}

}

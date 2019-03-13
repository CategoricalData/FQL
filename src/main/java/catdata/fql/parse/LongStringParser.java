package catdata.fql.parse;


/**
 * 
 * @author ryan
 *
 * parses a potentially quoted string
 */
public class LongStringParser implements RyanParser<String> {

	@Override
	public Partial<String> parse(Tokens s) throws BadSyntax {
		try {
			return new QuotedParser().parse(s);
		} catch (Exception e) { }
		return new StringParser().parse(s);
	}

}

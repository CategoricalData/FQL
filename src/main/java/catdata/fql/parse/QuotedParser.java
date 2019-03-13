package catdata.fql.parse;

/**
 * 
 * @author ryan
 *
 * Parser for quoted strings.
 */
public class QuotedParser implements RyanParser<String> {

	@Override
	public Partial<String> parse(Tokens s) throws BadSyntax {
		return ParserUtils.outside(new KeywordParser("\""), new StringParser(), new KeywordParser("\"")).parse(s);
	}

}

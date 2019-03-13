package catdata.fql.parse;

import catdata.Unit;

/**
 * 
 * @author ryan
 * 
 *         parses a keyword
 */
public class KeywordParser implements RyanParser<Unit> {

	private final String word;

	public KeywordParser(String keyword) {
		word = keyword;
	}

	@Override
	public Partial<Unit> parse(Tokens s) throws BadSyntax {
		if (s.head().equals(word)) {
			return new Partial<>(s.pop(), Unit.unit);
		}
		throw new BadSyntax(s, "Keyword " + word + " expected at " + s.head()
				+ " " + s.peek(1) + " " + s.peek(2));
	}

}

package catdata.fql.parse;

import java.util.List;

/**
 * 
 * @author ryan
 * 
 *         Parser for paths.
 */
public class PathParser implements RyanParser<List<String>> {

	@Override
	public Partial<List<String>> parse(Tokens s) throws BadSyntax {
		Partial<List<String>> ret = ParserUtils.manySep(new StringParser(),
				new KeywordParser(".")).parse(s);
		if (ret.value.isEmpty()) {
			throw new BadSyntax(s, "Error - empty path at " + s);
		}

		return ret;
	}

}

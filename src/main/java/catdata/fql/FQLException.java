package catdata.fql;

@SuppressWarnings("serial")
/*

  @author ryan
 *
 * Recoverable FQL exceptions (type errors, syntax errors, etc)
 */
public class FQLException extends Exception {

	public FQLException(String string) {
		super(string);
	}

}

package catdata.fql.parse;

import java.util.List;

/**
 * 
 * @author ryan
 * 
 *         Interface for tokenizers.
 */
public interface Tokens {

	String toString2();

	/**
	 * @return a new Tokens without the head
	 * @throws BadSyntax
	 *             is there is no head
	 */
    Tokens pop() throws BadSyntax;

	/**
	 * Look ahead without popping
	 * 
	 * @param n
	 *            how far ahead to look
	 * @return the nth token, or null if there is none
	 */
    String peek(int n);

	/**
	 * @return the current token
	 * @throws BadSyntax
	 *             if there is none
	 */
    String head() throws BadSyntax;

	List<String> words();

}

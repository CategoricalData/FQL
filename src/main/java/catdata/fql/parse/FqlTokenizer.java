package catdata.fql.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import catdata.Pair;

/**
 * 
 * @author ryan
 * 
 *         Tokenizer for FQL.
 */
public class FqlTokenizer implements Tokens {

	private final List<String> words;
	//private List<String> lines;
	
	private final String[] symbols = new String[] { ",", ":", ";", "->", ".", "{",
			"}", "(", ")", "=", "[", "]", "+", "*" };

	private final String comment_start = "/*";
	private final String comment_end = "*/";

	private final String quote = "\"";

	private final String space = " ";
	private final String tab = "\t";
	private final String linefeed = "\n";
	private final String carriagereturn = "\r";

	private FqlTokenizer(List<String> s, @SuppressWarnings("unused") List<String> t) {
		words = s; //lines = t;
	}

	public FqlTokenizer(String s) throws BadSyntax {
		BufferedReader br = new BufferedReader(new StringReader(s));
		StringBuilder sb = new StringBuilder();
		String l;
		try {
			while ((l = br.readLine()) != null) {
				int i = l.indexOf("//");
				if (i == -1) {
					sb.append(l);
					sb.append("\n");
				} else {
					String k = l.substring(0, i);
					sb.append(k);
					sb.append("\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		words = tokenize(sb.toString()).first;
	}

	private enum State {
		IN_QUOTE, IN_COMMENT, NORMAL
	}

	private Pair<List<String>, List<String>> tokenize(String input) throws BadSyntax {
		List<String> ret = new LinkedList<>();
		List<String> ret2 = new LinkedList<>();
		
		State state = State.NORMAL;
		String quote_state = "";
		String comment_state = "";
		String token_state = "";
		while (true) {
			if (input.isEmpty()) {
				switch (state) {
					case NORMAL:
						if (!token_state.isEmpty()) {
							ret.add(token_state);
							//ret2.add()
						}
						return new Pair<>(ret, ret2);
					case IN_QUOTE:
						throw new BadSyntax(this, "Unfinished quote: "
								+ quote_state);
					case IN_COMMENT:
						throw new BadSyntax(this, "Unfinished comment: "
								+ comment_state);
					default:
						break;
				}
			}
			if (input.startsWith(comment_start)) {
				switch (state) {
					case NORMAL:
						if (!token_state.isEmpty()) {
							ret.add(token_state);
							token_state = "";
						}
						state = State.IN_COMMENT;
						break;
					case IN_QUOTE:
						quote_state += comment_start;
						break;
					case IN_COMMENT:
						comment_state += comment_start;
						break;
					default:
						break;
				}
				input = input.substring(comment_start.length());
				continue;
			}
			if (input.startsWith(comment_end)) {
				switch (state) {
					case NORMAL:
						throw new BadSyntax(this, "No comment to end: "
								+ token_state);
					case IN_QUOTE:
						quote_state += comment_end;
						break;
					case IN_COMMENT:
						comment_state = "";
						state = State.NORMAL;
						break;
					default:
						break;
				}
				input = input.substring(comment_end.length());
				continue;
			}
			if (input.startsWith(quote)) {
				switch (state) {
					case NORMAL:
						if (!token_state.isEmpty()) {
							ret.add(token_state);
							token_state = "";
						}
						state = State.IN_QUOTE;
						break;
					case IN_QUOTE:
						ret.add(quote_state);
						state = State.NORMAL;
						quote_state = "";
						break;
					case IN_COMMENT:
						comment_state += quote;
						break;
					default:
						break;
				}
				ret.add(quote);
				input = input.substring(1);
				continue;
			}
			if (input.startsWith(space) || input.startsWith(tab)
					|| input.startsWith(linefeed)
					|| input.startsWith(carriagereturn)) {
				switch (state) {
					case NORMAL:
						if (!token_state.isEmpty()) {
							ret.add(token_state);
							token_state = "";
						}
						break;
					case IN_COMMENT:
						comment_state += input.substring(0, 1);
						break;
					case IN_QUOTE:
						quote_state += input.substring(0, 1);
						break;
					default:
						break;
				}
				input = input.substring(1);
				continue;
			}

			String matched = matchSymbol(input);
			if (matched == null) {
				switch (state) {
					case NORMAL:
						token_state += input.substring(0, 1);
						break;
					case IN_COMMENT:
						comment_state += input.substring(0, 1);
						break;
					case IN_QUOTE:
						quote_state += input.substring(0, 1);
						break;
					default:
						break;
				}
				input = input.substring(1);
				continue;
			}

			switch (state) {
				case NORMAL:
					if (!token_state.isEmpty()) {
						ret.add(token_state);
						token_state = "";
					}
					ret.add(matched);
					break;
				case IN_COMMENT:
					comment_state += matched;
					break;
				case IN_QUOTE:
					quote_state += matched;
					break;
				default:
					break;
			}
			input = input.substring(matched.length());

		}
	}

	private String matchSymbol(String input) {
		for (String symbol : symbols) {
			if (input.startsWith(symbol)) {
				return symbol;
			}
		}
		return null;
	}


	@Override
	public String head() throws BadSyntax {
		try {
			return words.get(0);
		} catch (IndexOutOfBoundsException e) {
			throw new BadSyntax(this, "Premature end of input");
		}
	}

	@Override
	public String peek(int n) {
		try {
			return words.get(n);
		} catch (IndexOutOfBoundsException e) {
			return "";
		}
	}

	@Override
	public Tokens pop() throws BadSyntax {
		List<String> ret = new LinkedList<>(words);
		List<String> ret2 = new LinkedList<>();
		try {
			ret.remove(0);
		//	ret2.remove(0);
		} catch (IndexOutOfBoundsException e) {
			throw new BadSyntax(this, "Premature end of input");
		}
		return new FqlTokenizer(ret, ret2);
	}

	@Override
	public String toString() {
		// int i = 0;
		String s = "";
		for (String w : words) {
			s = s + " " + w + " ";
		}
		return (s + "\n");
	}

	@Override
	public String toString2() {
		int i = 0;
		String s = "";
		for (String w : words) {
			s = s + " " + w + " ";
			i++;
			if (i == 10) {
				s += " ... ";
				break;
			}
		}
		return (s + "\n");
	}

	@Override
	public List<String> words() {
		return words;
	}


}

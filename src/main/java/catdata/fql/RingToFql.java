package catdata.fql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import catdata.Pair;
import catdata.fql.parse.FqlTokenizer;
import catdata.fql.parse.KeywordParser;
import catdata.fql.parse.ParserUtils;
import catdata.fql.parse.Partial;
import catdata.fql.parse.PrettyPrinter;
import catdata.fql.parse.RyanParser;
import catdata.fql.parse.StringParser;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;

/**
 * 
 * @author ryan
 * 
 *         Translates SQL (in categorical normal form) to FQL.
 */
public class RingToFql {

	private final Example[] examples = { new PeopleExample() };

	private final String help = "Polynomials written in fully explicit form (i.e., containing only additions of (\"1\" or multiplications of variables), can be treated as FQL queries.";
	

	static class PeopleExample extends Example {
		@Override
		public String getName() {
			return "Example 1";
		}

		@Override
		public String getText() {
			return extext1;
		}
	
	}

	private String translate(String in) {
		try {
			List<Pair<String, List<List<String>>>> list = program(in);
			return translate2(list) + "/* output will have\n" + in.trim() + "\n*/";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public RingToFql() {
		CodeTextPanel input = new CodeTextPanel("Input Polynomials", "");
		CodeTextPanel output = new CodeTextPanel("FQL Output", "");

		JButton transButton = new JButton("Translate");
		JButton helpButton = new JButton("Help");
	
		JComboBox<Example> box = new JComboBox<>(examples);
		box.setSelectedIndex(-1);
		box.addActionListener((ActionEvent e) -> input.setText(((Example) box.getSelectedItem()).getText()));

		transButton.addActionListener((ActionEvent e) -> {
                    try {
                        output.setText(translate(input.getText()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        output.setText(ex.getLocalizedMessage());
                    }
                });

		helpButton.addActionListener((ActionEvent e) -> {
                    JTextArea jta = new JTextArea(help);
                    jta.setWrapStyleWord(true);
                    // jta.setEditable(false);
                    jta.setLineWrap(true);
                    JScrollPane p = new JScrollPane(jta,
                            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    p.setPreferredSize(new Dimension(300, 200));
                    
                    JOptionPane pane = new JOptionPane(p);
                    // Configure via set methods
                    JDialog dialog = pane.createDialog(null,
                            "Help on Polynomials to FQL");
                    dialog.setModal(false);
                    dialog.setVisible(true);
                    dialog.setResizable(true);
                });

		JPanel p = new JPanel(new BorderLayout());

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setDividerSize(4);
		jsp.setResizeWeight(0.5d);
		jsp.add(input);
		jsp.add(output);

		JPanel tp = new JPanel(new GridLayout(1, 5));

		tp.add(transButton);
		tp.add(helpButton);
		tp.add(new JLabel());
		tp.add(new JLabel("Load Example", SwingConstants.RIGHT));
		tp.add(box);

		p.add(jsp, BorderLayout.CENTER);
		p.add(tp, BorderLayout.NORTH);
		JFrame f = new JFrame("Polynomials to FQL");
		f.setContentPane(p);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	private static final String extext1 = "p = x*x + y*y*y + y*y*y \nq = x + x*y + x*y + x*y\nr = 1 + 1 + 1 + y\n";

	
	private static String translate2(List<Pair<String, List<List<String>>>> in) {
		int fresh = 0;
		
		List<String> vars = new LinkedList<>();
		List<String> polynomials = new LinkedList<>();
		List<String> monomials = new LinkedList<>();
		List<String> occurences = new LinkedList<>();
		
		List<Pair<String, String>> delta = new LinkedList<>();
		List<Pair<String, String>> sigma = new LinkedList<>();
		List<Pair<String, String>> pi = new LinkedList<>();

		for (Pair<String, List<List<String>>> k : in) {
			String p = k.first;
			if (polynomials.contains(p)) {
				throw new RuntimeException("Duplicate polynomial: " + p);
			}
			polynomials.add(p);
			for (List<String> l : k.second) {
				String m = "m" + fresh++;
				monomials.add(m);
				sigma.add(new Pair<>(m, p));
				for (String r : l) {
					if (r.equals("1")) {
						continue;
					}
					assertVar(r);
					String o = "o" + fresh++;
					occurences.add(o);
					pi.add(new Pair<>(o, m));
					delta.add(new Pair<>(o, r));
					if (!vars.contains(r)) {
						vars.add(r);
					}
				}
			}
		}
		
		String occ = "schema occurances = {nodes "
				+ PrettyPrinter.sep0(",", occurences)
				+ "; attributes; arrows; equations;}\n";
		
		String mon = "schema monomials = {nodes "
				+ PrettyPrinter.sep0(",", monomials)
				+ "; attributes; arrows; equations;}\n";
		
		String src = "schema variables = {nodes "
				+ PrettyPrinter.sep0(",", vars)
				+ "; attributes; arrows; equations;}\n";

		String dst = "schema polynomials = {nodes " + PrettyPrinter.sep0(",", polynomials)
				+ "; attributes; arrows; equations;}\n";

		String d = "mapping load = {nodes "
				+ sepEdge(delta)
				+ "; attributes; arrows;} : occurances -> variables \n";
		
		String p = "mapping multiply = {nodes "
				+ sepEdge(pi)
				+ "; attributes; arrows;} : occurances -> monomials \n";
		
		String s = "mapping add = {nodes "
				+ sepEdge(sigma)
				+ "; attributes; arrows;} : monomials -> polynomials \n";

		String q = "query q = delta load pi multiply sigma add\n";
		
		String i0 = "//to set variable v := n, put n IDs into node v\n";
		String i = "instance input = {nodes " + sepSpecial(vars) + "; attributes; arrows; } : variables\n"; 
		
		String r = "instance output = eval q input\n"; 
		
		
		return src + "\n" + occ + "\n" + mon + "\n" + dst + "\n" + d + "\n" + p + "\n" + s + "\n" + q + "\n" + i0 + i + "\n" + r;

	}

	private static String sepSpecial(List<String> l ) {
		String ret = "";
		boolean first = true;
		for (String k : l) {
			if (!first) {
				ret += ", ";
			}
			first = false;

			ret += k + " -> { }";
		}
		
		return ret;
	}

	private static String sepEdge(List<Pair<String, String>> delta) {
		String ret = "";
		boolean first = true;
		for (Pair<String, String> k : delta) {
			if (!first) {
				ret += ", ";
			}
			first = false;

			ret += k.first + " -> " + k.second;
		}
		
		return ret;
	}

	private static void assertVar(String o) {
		try {
			int z = Integer.parseInt(o);
			throw new RuntimeException("Encountered non-1 numeral: " + z);
		} catch (NumberFormatException e) {
		}
	}
	
	private final RyanParser<List<List<String>>> tparser = ParserUtils.manySep(
			ParserUtils.manySep(new StringParser(), new KeywordParser("*")),
			new KeywordParser("+"));
	private final RyanParser<List<Pair<String, List<List<String>>>>> parser = ParserUtils
			.many(ParserUtils.inside(new StringParser(),
					new KeywordParser("="), tparser));

	private List<Pair<String, List<List<String>>>> program(String s)
			throws Exception {
		Partial<List<Pair<String, List<List<String>>>>> k = parser
				.parse(new FqlTokenizer(s));

		if (!k.tokens.toString().trim().isEmpty()) {
			throw new RuntimeException("Unconsumed input: " + k.tokens);
		}

		return k.value;
	}

}

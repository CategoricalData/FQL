package catdata.opl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import catdata.opl.OplExp.OplSig;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Terminals.Identifier;
import org.jparsec.Terminals.IntegerLiteral;
import org.jparsec.Terminals.StringLiteral;
import org.jparsec.functors.Tuple3;

import catdata.Pair;
import catdata.Util;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;


@SuppressWarnings("deprecation")
public class CfgToOpl {
	
	static class STLCExample extends Example {
		
		@Override
		public String getName() {
			return "STLC";
		}

		@Override
		public String getText() {
			return s;
		}
		
		final String s = "t ::= 1 | t \"*\" t | t \"^\" t"
				+ "\n\n, //separate productions by ,\n"
				+ "\ne ::= id t | e \";\" e | \"!\" t | \"(\" e \",\" e \")\" | fst t t | snd t t | curry e | eval t t";
		
	}

	private final Example[] examples = { new STLCExample() } ;
	
	private final String help = "";
	
	private static String kind() {
		return "CFG";
	}
	
	
	private static String translate(String in) {
		return program(in).toString();
	}

	public CfgToOpl() {
		CodeTextPanel input = new CodeTextPanel(BorderFactory.createEtchedBorder(), kind() + " Input", "");
		CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(), "OPL Output", "");

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
                    //jta.setEditable(false);
                    jta.setLineWrap(true);
                    JScrollPane p = new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    p.setPreferredSize(new Dimension(300,200));
                    
                    JOptionPane pane = new JOptionPane(p);
                    // Configure via set methods
                    JDialog dialog = pane.createDialog(null, "Help on CFG to OPL");
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

		// JPanel bp = new JPanel(new GridLayout(1, 5));
		JPanel tp = new JPanel(new GridLayout(1, 4));

		// bp.add(field);

		tp.add(transButton);
		tp.add(helpButton);
		// tp.add(jdbcButton);
		// tp.add(helpButton);
		tp.add(new JLabel("Load Example", SwingConstants.RIGHT));
		tp.add(box);

		// bp.add(runButton);
		// bp.add(runButton2);
		// bp.add(lbl);
		// bp.add(field);
		// bp.add(jdbcBox);

		// p.add(bp, BorderLayout.SOUTH);
		p.add(jsp, BorderLayout.CENTER);
		p.add(tp, BorderLayout.NORTH);
		JFrame f = new JFrame(kind() + " to OPL");
		f.setContentPane(p);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	
//	public static Object transSQLSchema(String in) {
	//	return in;
	//}
	
	
	private static final String[] ops = new String[] { "|" , "::=", "," };

	private static final String[] res = new String[] { };

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT,
			Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	private static final Parser<?> TOKENIZER = Parsers.or(
			(Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), (Parser<?>) Identifier.TOKENIZER,
			(Parser<?>) IntegerLiteral.TOKENIZER);

	private static Parser<?> term(String... names) {
		return RESERVED.token(names);
	}

	private static Parser<?> ident() {
		return Identifier.PARSER;
	}

	private static Object program(String s) {
		return toCfg( program.parse(s) );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    private static OplExp toCfg(Object o) {
		Map<String, List<List<String>>> ret = new HashMap<>();
		
		List<Tuple3> l = (List<Tuple3>) o;
		for (Tuple3 p : l) {
			String x = (String) p.a;
			if (ret.containsKey(x)) {
				throw new RuntimeException("Duplicate production name: " + x);
			}
			ret.put(x, (List<List<String>>) p.c);
		}
		
		Map<String, Pair<List<String>, String>> symbols = new HashMap<>();
		int i = 0;
		for (String k : ret.keySet()) {
			List<List<String>> v = ret.get(k);
			for (List<String> u : v) {
				List<String> pre = new LinkedList<>();
				List<String> tys = new LinkedList<>();
				for (String z : u) {
					if (ret.keySet().contains(z)) {
						tys.add(z);
					} else {
						pre.add(z);
					}
				}
				String name0 = Util.sep(pre, "_");
				String xxx = symbols.keySet().contains(name0) ? "_" + (i++) : ""; 
				String name = "\"" + name0 +  xxx + "\"";
				symbols.put(name, new Pair<>(tys, k));
				i++;
			}
		}
		return new OplSig(null, new HashMap<>(), ret.keySet(), symbols, new LinkedList<>());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
    private static Parser program() {
		Parser q = string().many().sepBy(term("|"));
		Parser p = Parsers.tuple(ident(), term("::="), q).sepBy(term(","));
		return p;
	}
	

	@SuppressWarnings("unchecked")
    private static final Parser<?> program = program().from(TOKENIZER, IGNORED);

	
	private static Parser<?> string() {
		return Parsers.or(StringLiteral.PARSER,
				IntegerLiteral.PARSER, Identifier.PARSER);
	}
	

}


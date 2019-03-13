package catdata.fpql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Collectors;

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

import catdata.fpql.XExp.Var;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Terminals.Identifier;
import org.jparsec.Terminals.IntegerLiteral;
import org.jparsec.Terminals.StringLiteral;
import org.jparsec.functors.Tuple3;
import org.jparsec.functors.Tuple4;
import org.jparsec.functors.Tuple5;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.fpql.XExp.XInst;
import catdata.fpql.XExp.XMapConst;
import catdata.fpql.XExp.XSchema;
import catdata.fpql.XExp.XTransConst;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.Language;

@SuppressWarnings("deprecation")
public class XRaToFpql {

	private static XMapConst doMapping(List<Pair<String, String>> nm,
			/* List<Pair<String, String>> am, */ List<Pair<String, List<String>>> em, XExp src, XExp dst) {
		List<Pair<String, List<String>>> em2 = new LinkedList<>(em);
		return new XMapConst(src, dst, nm, em2);
	}

	public static XSchema doSchema(List<String> nodes, /* List<Triple<String, String, String>> attrs, */
			List<Triple<String, String, String>> arrows, List<Pair<List<String>, List<String>>> eqs) {		
		return new XSchema(new LinkedList<>(nodes), new LinkedList<>(arrows), new LinkedList<>(eqs));
	}

	public static XInst doInst(List<Pair<String, List<Pair<Object, Object>>>> nodes,
		//	List<Pair<String, List<Pair<Object, Object>>>> attrs,
			List<Pair<String, List<Pair<Object, Object>>>> arrows, XExp sch) {
		List<Pair<String, String>> vars = new LinkedList<>();
		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
		
		for (Pair<String, List<Pair<Object, Object>>> k : nodes) {
			for (Pair<Object, Object> v : k.second) {
				vars.add(new Pair<>(v.first.toString(), k.first));
			}
		}
		/*for (Pair<String, List<Pair<Object, Object>>> k : attrs) {
			for (Pair<Object, Object> v : k.second) {
				List<String> lhs = new LinkedList<>();
				List<String> rhs = new LinkedList<>();
				lhs.add("v" + v.first.toString());
				lhs.add(k.first);
				rhs.add(v.second.toString());
				eqs.add(new Pair<>(lhs, rhs));
			}
		} */
		for (Pair<String, List<Pair<Object, Object>>> k : arrows) {
			for (Pair<Object, Object> v : k.second) {
				List<String> lhs = new LinkedList<>();
				List<String> rhs = new LinkedList<>();
				lhs.add(v.first.toString());
				lhs.add(k.first);
				rhs.add(v.second.toString());
				eqs.add(new Pair<>(lhs, rhs));
			}
		}
		
		return new XInst(sch, vars, eqs);
	}

	/*
	 * public static String doAdom(SigExp.Const A, String a) { String k = a;
	 * List<Pair<List<String>, List<String>>> eeqs = new LinkedList<>();
	 * List<Triple<String, String, String>> attrs = new LinkedList<>();
	 * attrs.add(new Triple<>("att", "adom", "adom")); List<Triple<String,
	 * String, String>> dd_attrs = new LinkedList<>(); dd_attrs.add(new
	 * Triple<>("att", "x", "adom"));
	 * 
	 * List<Triple<String, String, String>> e_attrs = new LinkedList<>();
	 * 
	 * List<String> bn = new LinkedList<>(); bn.add("r"); bn.add("d");
	 * List<Triple<String, String, String>> barrs = new LinkedList<>();
	 * List<Pair<String, String>> abn = new LinkedList<>(); abn.add(new
	 * Pair<>("r", "guid")); abn.add(new Pair<>("d", "adom")); List<Pair<String,
	 * String>> abatt = new LinkedList<>(); abatt.add(new Pair<>("att", "att"));
	 * List<Pair<String, String>> e_abatt = new LinkedList<>();
	 * 
	 * List<Pair<String, List<String>>> abarr = new LinkedList<>();
	 * 
	 * List<String> cn = new LinkedList<>(); cn.add("r"); cn.add("d");
	 * cn.add("m");
	 * 
	 * List<Triple<String, String, String>> carrs = new LinkedList<>();
	 * carrs.add(new Triple<>("f", "m", "d"));
	 * 
	 * List<Pair<String, String>> bcn = new LinkedList<>(); bcn.add(new
	 * Pair<>("r", "r")); bcn.add(new Pair<>("d", "d"));
	 * 
	 * List<Pair<String, List<String>>> bcarr = new LinkedList<>();
	 * 
	 * List<Triple<String, String, String>> bbarrs = new LinkedList<>();
	 * bbarrs.add(new Triple<>("f", "a", "b")); bbarrs.add(new Triple<>("g",
	 * "a", "c")); bbarrs.add(new Triple<>("h", "b", "d")); bbarrs.add(new
	 * Triple<>("i", "c", "d")); List<String> bbn = new LinkedList<>();
	 * bbn.add("r"); bbn.add("a"); bbn.add("b"); bbn.add("c"); bbn.add("d");
	 * 
	 * List<Triple<String, String, String>> ccarrs = new LinkedList<>();
	 * List<String> ccn = new LinkedList<>(); List<Pair<List<String>,
	 * List<String>>> cceqs = new LinkedList<>(); ccn.add("r"); ccn.add("a");
	 * ccn.add("b"); ccn.add("c"); ccn.add("d"); ccn.add("e"); ccarrs.add(new
	 * Triple<>("f", "a", "b")); ccarrs.add(new Triple<>("g", "a", "c"));
	 * ccarrs.add(new Triple<>("h", "b", "d")); ccarrs.add(new Triple<>("i",
	 * "c", "d")); ccarrs.add(new Triple<>("ff", "e", "b")); ccarrs.add(new
	 * Triple<>("gg", "e", "c")); List<String> l1 = new LinkedList<>();
	 * l1.add("e"); l1.add("ff"); l1.add("h"); List<String> l2 = new
	 * LinkedList<>(); l2.add("e"); l2.add("gg"); l2.add("i"); cceqs.add(new
	 * Pair<>(l1, l2));
	 * 
	 * List<Triple<String, String, String>> ddarrs = new LinkedList<>();
	 * List<Pair<List<String>, List<String>>> ddeqs = new LinkedList<>();
	 * List<String> ddn = new LinkedList<>(); ddn.add("r"); ddn.add("v");
	 * ddn.add("w"); ddn.add("x"); ddn.add("y"); ddarrs.add(new Triple<>("f",
	 * "v", "w")); ddarrs.add(new Triple<>("g", "w", "x")); ddarrs.add(new
	 * Triple<>("h", "x", "y")); ddarrs.add(new Triple<>("ff", "v", "w")); l1 =
	 * new LinkedList<>(); l1.add("v"); l1.add("f"); l1.add("g"); l2 = new
	 * LinkedList<>(); l2.add("v"); l2.add("ff"); l2.add("g"); ddeqs.add(new
	 * Pair<>(l1, l2));
	 * 
	 * List<Pair<String, String>> ffn = new LinkedList<>(); List<Pair<String,
	 * List<String>>> ffarr = new LinkedList<>(); ffn.add(new Pair<>("r", "r"));
	 * ffn.add(new Pair<>("a", "m")); ffn.add(new Pair<>("b", "m")); ffn.add(new
	 * Pair<>("c", "m")); ffn.add(new Pair<>("d", "d")); l1 = new
	 * LinkedList<>(); l1.add("m"); ffarr.add(new Pair<>("f", l1)); l1 = new
	 * LinkedList<>(); l1.add("m"); ffarr.add(new Pair<>("g", l1)); l1 = new
	 * LinkedList<>(); l1.add("m"); l1.add("f"); ffarr.add(new Pair<>("h", l1));
	 * l1 = new LinkedList<>(); l1.add("m"); l1.add("f"); ffarr.add(new
	 * Pair<>("i", l1));
	 * 
	 * List<Pair<String, String>> ggn = new LinkedList<>(); ggn.add(new
	 * Pair<>("r", "r")); ggn.add(new Pair<>("a", "a")); ggn.add(new Pair<>("b",
	 * "b")); ggn.add(new Pair<>("c", "c")); ggn.add(new Pair<>("d", "d"));
	 * List<Pair<String, List<String>>> ggarr = new LinkedList<>(); l1 = new
	 * LinkedList<>(); l1.add("a"); l1.add("f"); ggarr.add(new Pair<>("f", l1));
	 * l1 = new LinkedList<>(); l1.add("a"); l1.add("g"); ggarr.add(new
	 * Pair<>("g", l1)); l1 = new LinkedList<>(); l1.add("b"); l1.add("h");
	 * ggarr.add(new Pair<>("h", l1)); l1 = new LinkedList<>(); l1.add("c");
	 * l1.add("i"); ggarr.add(new Pair<>("i", l1));
	 * 
	 * List<Pair<String, String>> hhn = new LinkedList<>(); hhn.add(new
	 * Pair<>("r", "r")); hhn.add(new Pair<>("e", "v")); hhn.add(new Pair<>("a",
	 * "w")); hhn.add(new Pair<>("b", "w")); hhn.add(new Pair<>("c", "w"));
	 * hhn.add(new Pair<>("d", "y")); List<Pair<String, List<String>>> hharr =
	 * new LinkedList<>(); l1 = new LinkedList<>(); l1.add("w"); hharr.add(new
	 * Pair<>("f", l1)); l1 = new LinkedList<>(); l1.add("w"); hharr.add(new
	 * Pair<>("g", l1)); l1 = new LinkedList<>(); l1.add("w"); l1.add("g");
	 * l1.add("h"); hharr.add(new Pair<>("h", l1)); l1 = new LinkedList<>();
	 * l1.add("w"); l1.add("g"); l1.add("h"); hharr.add(new Pair<>("i", l1)); l1
	 * = new LinkedList<>(); l1.add("v"); l1.add("f"); hharr.add(new
	 * Pair<>("ff", l1)); l1 = new LinkedList<>(); l1.add("v"); l1.add("ff");
	 * hharr.add(new Pair<>("gg", l1));
	 * 
	 * List<Pair<String, String>> iin = new LinkedList<>(); iin.add(new
	 * Pair<>("guid", "r")); iin.add(new Pair<>("adom", "x")); List<Pair<String,
	 * List<String>>> iiarr = new LinkedList<>();
	 * 
	 * int i = 0; for (Triple<String, String, String> m0 : A.arrows) { String m
	 * = m0.first; bn.add("m" + i); barrs.add(new Triple<>("i" + i, "r", "m" +
	 * i)); barrs.add(new Triple<>("f" + i, "m" + i, "d"));
	 * 
	 * abn.add(new Pair<>("m" + i, "guid")); List<String> l = new
	 * LinkedList<>(); l.add("guid"); abarr.add(new Pair<>("i" + i, l)); l = new
	 * LinkedList<>(); l.add("guid"); l.add(m); abarr.add(new Pair<>("f" + i,
	 * l));
	 * 
	 * carrs.add(new Triple<>("i" + i, "r", "m"));
	 * 
	 * bcn.add(new Pair<>("m" + i, "m"));
	 * 
	 * l = new LinkedList<>(); l.add("r"); l.add("i" + i); bcarr.add(new
	 * Pair<>("i" + i, l));
	 * 
	 * l = new LinkedList<>(); l.add("m"); l.add("f"); bcarr.add(new Pair<>("f"
	 * + i, l));
	 * 
	 * bbarrs.add(new Triple<>("i" + i, "r", "a")); ccarrs.add(new Triple<>("i"
	 * + i, "r", "a")); ddarrs.add(new Triple<>("i" + i, "r", "w"));
	 * 
	 * l = new LinkedList<>(); l.add("r"); l.add("i" + i); ffarr.add(new
	 * Pair<>("i" + i, l));
	 * 
	 * l = new LinkedList<>(); l.add("r"); l.add("i" + i); ggarr.add(new
	 * Pair<>("i" + i, l));
	 * 
	 * l = new LinkedList<>(); l.add("r"); l.add("i" + i); hharr.add(new
	 * Pair<>("i" + i, l));
	 * 
	 * l = new LinkedList<>(); l.add("r"); l.add("i" + i); l.add("g");
	 * iiarr.add(new Pair<>(m, l));
	 * 
	 * i++; }
	 * 
	 * SigExp.Const b = new SigExp.Const(bn, e_attrs, barrs, eeqs); MapExp.Const
	 * ab = new MapExp.Const(abn, e_abatt, abarr, b, A); // F
	 * 
	 * SigExp.Const c = new SigExp.Const(cn, e_attrs, carrs, eeqs); MapExp.Const
	 * bc = new MapExp.Const(bcn, e_abatt, bcarr, b, c); // G
	 * 
	 * SigExp.Const bb = new SigExp.Const(bbn, e_attrs, bbarrs, eeqs);
	 * SigExp.Const cc = new SigExp.Const(ccn, e_attrs, ccarrs, cceqs);
	 * SigExp.Const dd = new SigExp.Const(ddn, dd_attrs, ddarrs, ddeqs);
	 * 
	 * MapExp.Const ff = new MapExp.Const(ffn, e_abatt, ffarr, bb, c);
	 * MapExp.Const gg = new MapExp.Const(ggn, e_abatt, ggarr, bb, cc);
	 * MapExp.Const hh = new MapExp.Const(hhn, e_abatt, hharr, c, dd);
	 * MapExp.Const ii = new MapExp.Const(iin, abatt, iiarr, A, dd);
	 * 
	 * String ret = "///////////////\n"; ret += "schema " + k + "_B = " + b +
	 * "\n\n"; ret += "mapping " + k + "_F = " + ab + " : " + k + "_B -> " + a +
	 * "\n\n"; ret += "schema " + k + "_C = " + c + "\n\n"; ret += "mapping " +
	 * k + "_G = " + bc + " : " + k + "_B -> " + k + "_C\n\n";
	 * 
	 * ret += "schema " + k + "_BB = " + bb + "\n\n"; ret += "schema " + k +
	 * "_CC = " + cc + "\n\n"; ret += "schema " + k + "_DD = " + dd + "\n\n";
	 * 
	 * ret += "mapping " + k + "_FF = " + ff + " : " + k + "_BB -> " + k +
	 * "_C\n\n"; ret += "mapping " + k + "_GG = " + gg + " : " + k + "_BB -> " +
	 * k + "_CC\n\n"; ret += "mapping " + k + "_HH = " + hh + " : " + k +
	 * "_CC -> " + k + "_DD\n\n"; ret += "mapping " + k + "_II = " + ii + " : "
	 * + a + " -> " + k + "_DD\n\n";
	 * 
	 * ret += "//////////////\n"; return ret; // emit as "as_rel" }
	 */
	/*
	private static String doAdom2(String k, String in, String out) {
		String ret = "";
		ret += "\n\ninstance " + out + "_y = delta " + k + "_F " + in;
		ret += "\n\ninstance " + out + "_z = sigma " + k + "_G " + out + "_y";
		ret += "\n\ninstance " + out + "_w1= delta " + k + "_FF " + out + "_z";
		ret += "\n\ninstance " + out + "_w2= pi " + k + "_GG " + out + "_w1";
		ret += "\n\ninstance " + out + "_w3= SIGMA " + k + "_HH " + out + "_w2";
		ret += "\n\ninstance " + out + "_rel= delta " + k + "_II " + out + "_w3";
		/*
		 * instance Y3=delta F3 X3 instance Z3=sigma G3 Y3
		 * 
		 * instance W1=delta F Z3 instance W2=pi G W1 instance W3=SIGMA H W2
		 * instance RelationImage=delta I W3
		 */
//		return ret;
//	}


	private final Example[] examples = { new PeopleExample() /* , new NegExample() */, new EDExample(), new SelectExample() };

	private final String help = "Bags of tuples can be represented in FQL using an explicit active domain construction.  See the People example.  Unions of conjunctive queries *of base relations* are supported, using DISTINCT and ALL for set semantics.  (The translated FQL will not compile if not translating unions of conjunctive queries of base relations).  Primary and foreign keys are not supported by this encoding.  WHERE clauses must have equalities between variables, not constants.  SQL keywords MUST be capitalized.  The observables viewer pane is useful for visualizing instances.";

	private static String kind() {
		return "SPCU";
	}
	
	static class SelectExample extends Example {
		
		@Override
		public Language lang() {
			throw new RuntimeException();
		}

		@Override
		public String getName() {
			return "Select const";
		}

		@Override
		public String getText() {
			return s;
		}
		
		
		final String s =
		"CREATE TABLE R ("
				+ "\n c1 VARCHAR(255), "
				+ "\n c2 VARCHAR(255)"
				+ "\n);"
				+ "\n"
				+ "\nINSERT INTO R VALUES (\"one\", \"one\"), (\"one\", \"one\"), (\"one\", \"two\"), (\"two\", \"three\") ;"
				+ "\n"
				+ "\nq1 = SELECT r.c1 AS col1, r.c2 AS col2"
				+ "\n     FROM R AS r "
				+ "\n     WHERE r.c1 = \"one\" "
				+ "\n"
				+ "\nq2 = SELECT DISTINCT r.c1 AS col1, r.c2 AS col2"
				+ "\n     FROM R AS r "
				+ "\n     WHERE r.c1 = \"one\" "
				+ "\n"
				+ "\nq3 = SELECT r.c1 AS col1, r.c2 AS col2"
				+ "\n     FROM R AS r "
				+ "\n     WHERE r.c1 = \"four\" "
				+ "\n"
				+ "\nq4 = SELECT r.c1 AS col1, r.c2 AS col2"
				+ "\n     FROM R AS r "
				+ "\n     WHERE r.c1 = \"one\" AND r.c1 = \"two\""
				+ "\n";

	}

	static class EDExample extends Example {
		
		@Override
		public Language lang() {
			throw new RuntimeException();
		}

		@Override
		public String getName() {
			return "ED";
		}

		@Override
		public String getText() {
			return "CREATE TABLE P (" + "\n f VARCHAR(255)" + "\n);  " + "\n"
					+ "\nCREATE TABLE Q (" + "\n g VARCHAR(255), " + "\n h VARCHAR(255)" + "\n);"
					+ "\n" + "\nINSERT INTO P VALUES (\"a\"),(\"b\"),(\"c\");  "
					+ "\nINSERT INTO Q VALUES (\"a\", \"b\"),(\"x\",\"x\");" + "\n"
					+ "\nc1 = FORALL Q AS Q1 " + "\n     WHERE Q1.g = Q1.h"
					+ "\n     EXISTS P AS P1, P AS P2" + "\n     WHERE P1.f = a " + "\n";
		}

	}

	static class PeopleExample extends Example {
		
		@Override
		public Language lang() {
			return null;
		}
		
		@Override
		public String getName() {
			return "People";
		}

		@Override
		public String getText() {
			return extext1;
		}
	}

	static class NegExample extends Example {
		@Override
		public Language lang() {
			throw new RuntimeException();
		}
		@Override
		public String getName() {
			return "Negation";
		}

		@Override
		public String getText() {
			return negText;
		}
	}

	private static String translate(String in) {
		List<Pair<String, EExternal>> list = program(in);
		return transSQLSchema(list);
	}

	public XRaToFpql() {
		CodeTextPanel input = new CodeTextPanel(BorderFactory.createEtchedBorder(), kind() + " Input", "");
		CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(), "FPQL Output", "");

		JButton transButton = new JButton("Translate");
		JButton helpButton = new JButton("Help");

		JComboBox<Example> box = new JComboBox<>(examples);
		box.setSelectedIndex(-1);
		box.addActionListener((ActionEvent e) -> {
                    if (box.getSelectedItem() != null) {
                        input.setText(((Example) box.getSelectedItem()).getText());
                    }
                });

		//  shred and unshred queries

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
                    JScrollPane p = new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    p.setPreferredSize(new Dimension(300, 200));
                    
                    JOptionPane pane = new JOptionPane(p);
                    // Configure via set methods
                    JDialog dialog = pane.createDialog(null, "Help on SPCU to FPQL");
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
		JPanel tp = new JPanel(new GridLayout(1, 5));

		// bp.add(field);

		tp.add(transButton);
		tp.add(helpButton);
		// tp.add(jdbcButton);
		// tp.add(helpButton);
		tp.add(new JLabel());
		// tp.add(lbl);
		// tp.add(field);
		tp.add(new JLabel("Load Example", SwingConstants.RIGHT));
		tp.add(box);

		// p.add(bp, BorderLayout.SOUTH);
		p.add(jsp, BorderLayout.CENTER);
		p.add(tp, BorderLayout.NORTH);
		JFrame f = new JFrame(kind() + " to FPQL");
		f.setContentPane(p);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	private static final String extext1 = "CREATE TABLE Place ("
			+ "\n description VARCHAR(255)"
			+ "\n);  "
			+ "\n"
			+ "\nCREATE TABLE Person ("
			+ "\n name VARCHAR(255), "
			+ "\n home VARCHAR(255)"
			+ "\n);"
			+ "\n"
			+ "\nINSERT INTO Place VALUES (\"NewYork\"),(\"Chicago\"),(\"NewYork\"); //bag semantics "
			+ "\nINSERT INTO Person VALUES (\"Alice\", \"Chicago\");" + "\n"
			+ "\nq1 = SELECT DISTINCT x.description AS col0, z.name AS col1"
			+ "\n     FROM Place AS x, Place AS y, Person AS z "
			+ "\n     WHERE x.description = y.description AND x.description = z.home" + "\n"
			+ "\nq2 = SELECT x.description AS col0, z.name AS col1"
			+ "\n     FROM Place AS x, Place AS y, Person AS z "
			+ "\n     WHERE x.description = y.description AND x.description = z.home" + "\n"
			+ "\nq3 = q1 UNION q2" + "\n" + "\nq4 = q1 UNION ALL q2" + "\n";

	private static final String negText = "//our encoding of negation doesn't work correctly yet\n"
			+ "\nCREATE TABLE A (" + "\n a VARCHAR(255)" + "\n);  " + "\n"
			+ "\nINSERT INTO A VALUES (\"a\"),(\"a\"),(\"a\"); " + "\n" + "\nCREATE TABLE B ("
			+ "\n b VARCHAR(255)" + "\n);" + "\n" + "\nINSERT INTO B VALUES (\"b\"),(\"b\");"
			+ "\n" + "\na1 = SELECT DISTINCT x.a AS c FROM A AS x " + "\n"
			+ "\na3 = SELECT x.a AS c FROM A AS x" + "\n" + "\nb2 = SELECT x.b AS c FROM B AS x"
			+ "\n" + "\na3b2 = a3 UNION ALL b2" + "\n" + "\na2b2 = a3b2 EXCEPT a1" + "\n"
			+ "\na1b1 = a3 UNION b2" + "\n" + "\nb1 = a1b1 EXCEPT a1\n"
			+ "\n\n///////// the active domain has an effect on difference: "
			+ "\n/*enum str = {a,b}" + "\n" + "\nschema X = {" + "\n nodes" + "\n  adom,"
			+ "\n  guid;" + "\n attributes" + "\n  att: adom -> str;" + "\n arrows"
			+ "\n  c: guid -> adom;" + "\n equations;" + "\n}" + "\n" + "\ninstance F1 = {"
			+ "\n nodes" + "\n  adom -> {123, 124}, " + "\n  guid -> {125, 126};" + "\n attributes"
			+ "\n  att -> {(124, b), (123, a)};" + "\n arrows"
			+ "\n  c -> {(126, 123), (125, 124)};" + "\n} : X" + "\n" + "\n//encodes {a}"
			+ "\ninstance F2 = {" + "\n nodes" + "\n  adom -> {27}, " + "\n  guid -> {28};"
			+ "\n attributes" + "\n  att -> {(27, a)};" + "\n arrows" + "\n  c -> {(28, 27)};"
			+ "\n} : X" + "\n" + "\n//encodes {a}, but includes b in active domain"
			+ "\n//having b in active domain will kill b in the output of the difference" + "\n"
			+ "\n//instance F2 = {" + "\n// nodes" + "\n//  adom -> {26, 27}, "
			+ "\n//  guid -> {28};" + "\n// attributes" + "\n//  att -> {(26, b), (27, a)};"
			+ "\n// arrows" + "\n//  c -> {(28, 27)};" + "\n//} : X" + "\n"
			+ "\ninstance tprp = prop X" + "\ninstance tone = unit X" + "\n"
			+ "\ninstance prpprp = (tprp * tprp)" + "\n" + "\ntransform F1t = tone.unit F1"
			+ "\ntransform F2t = tone.unit F2" + "\n" + "\ntransform chiF1t = tprp.char F1t"
			+ "\ntransform chiF2t = tprp.char F2t"
			+ "\ntransform negchiF2t = (tprp.char F2t then tprp.not)" + "\n"
			+ "\ntransform t0 = prpprp.(chiF1t * negchiF2t)"
			+ "\ntransform t1 = (t0 then prpprp.and)" + "\n"
			+ "\ninstance F1minusF2 = kernel t1\n*/";

	private static String transSQLSchema(List<Pair<String, EExternal>> in) {
		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
		List<Triple<String, String, String>> arrows = new LinkedList<>();
		//List<Triple<String, String, String>> attrs = new LinkedList<>();
		List<String> nodes = new LinkedList<>();

		List<Pair<String, List<Pair<Object, Object>>>> inodes = new LinkedList<>();
		//List<Pair<String, List<Pair<Object, Object>>>> iattrs = new LinkedList<>();
		List<Pair<String, List<Pair<Object, Object>>>> iarrows = new LinkedList<>();
		//String adom = "adom";
		//nodes.add(adom);
	//	List<Pair<Object, Object>> adomT = new LinkedList<>();
		//LinkedList<Pair<Object, Object>> attT = new LinkedList<>();
		//inodes.add(new Pair<String, List<Pair<Object, Object>>>(adom, adomT));
	//	iattrs.add(new Pair<String, List<Pair<Object, Object>>>("att", attT));
	//	attrs.add(new Triple<>("att", adom, "adom"));
		Set<Object> enums = new HashSet<>();

		Map<String, Object> dom1 = new HashMap<>();

		List<Pair<String, EExternal>> queries = new LinkedList<>();

		int count = 0;
		Set<String> seen = new HashSet<>();
		Map<String, List<String>> cols = new HashMap<>();
		for (Pair<String, EExternal> kk0 : in) {
			EExternal k0 = kk0.second;
			// String key = kk0.first;
			if (k0 instanceof ECreateTable) {
				ECreateTable k = (ECreateTable) k0;
				if (seen.contains(k.name)) {
					throw new RuntimeException("Duplicate name: " + k.name);
				}
				if (k.name.equals("adom") || k.name.equals("att")) {
					throw new RuntimeException("The names adom and att cannot be used.");
				}
				seen.add(k.name);
				nodes.add(k.name);
				inodes.add(new Pair<>(k.name,
                        new LinkedList<>()));
				List<String> lcols = new LinkedList<>();
				for (Pair<String, String> col : k.types) {
					lcols.add(col.first);
					if (seen.contains(col.first)) {
						throw new RuntimeException("Duplicate name: " + col.first);
					}
					seen.add(col.first);
					arrows.add(new Triple<>(k.name + "_" + col.first, k.name, "adom"));
					iarrows.add(new Pair<>(k.name + "_"
                            + col.first, new LinkedList<>()));
				}
				cols.put(k.name, lcols);
			}
			if (k0 instanceof EInsertValues) {
				EInsertValues k = (EInsertValues) k0;
				List<String> lcols = cols.get(k.target);
				if (lcols == null) {
					throw new RuntimeException("Missing: " + k.target);
				}
				for (List<String> tuple : k.values) {
					if (lcols.size() != tuple.size()) {
						throw new RuntimeException("Column size mismatch " + tuple + " in "
								+ k.target);
					}
					List<Pair<Object, Object>> node = lookup2(k.target, inodes);
					if (node == null) {
						throw new RuntimeException("Missing table " + k.target);
					}

					String id = "v" + count++;
					node.add(new Pair<>(id, id));

					for (int colNum = 0; colNum < tuple.size(); colNum++) {
						Object xxx = dom1.get(tuple.get(colNum));
						if (xxx == null) {
							dom1.put(tuple.get(colNum), tuple.get(colNum)); //was 2nd=count
							enums.add(tuple.get(colNum));
						//	adomT.add(new Pair<Object, Object>(count, count));
					//		adomT.add(new Pair<Object, Object>(count, tuple.get(colNum)
						//			));
							xxx = dom1.get(tuple.get(colNum));
	//						count++;
						}

						List<Pair<Object, Object>> yyy = lookup2(
								k.target + "_" + lcols.get(colNum), iarrows);
						if (yyy == null) {
							throw new RuntimeException("Anomaly: please report");
						}
						yyy.add(new Pair<>(id, xxx));
					}
				}
			}
			if (k0 instanceof EFlower || k0 instanceof EUnion || k0 instanceof EDiff
					|| k0 instanceof EED) {
				queries.add(kk0);
			}
		}

		XSchema exp = doSchema(nodes, /* attrs, */ arrows, eqs);
		XInst inst = doInst(inodes, /* iattrs, */ iarrows, new Var("S"));

		// int ctx = 0;
		String xxx = "\n\n";
		Map<String, String> schemas = new HashMap<>();
		Map<String, XSchema> schemas0 = new HashMap<>();
	//	Map<String, Boolean> done = new HashMap<>();
		for (Pair<String, EExternal> gh0 : queries) {
			String k = gh0.first;
			EExternal gh = gh0.second;
			if (gh instanceof EFlower) {
				EFlower fl = (EFlower) gh;
				Pair<String, XSchema> yyy = trans(exp, fl, k, enums);
				xxx += yyy.first + "\n\n";
				schemas.put(k, k + "Schema");
				schemas0.put(k, yyy.second);

			} else if (gh instanceof EUnion) {
				EUnion g = (EUnion) gh;
				String s1 = schemas.get(g.l);
				schemas.put(k, s1);
				schemas0.put(k, schemas0.get(g.l));

				xxx += longSlash + "\n/* Translation of " + k + "  */\n" + longSlash;

				if (g.distinct) {
					xxx += "\n\n" + k + "_temp = (" + g.l + " + " + g.r + ")";
					xxx += "\n\n" + k + " = relationalize " + k + "_temp";
				} else {
					xxx += "\n\n" + k + " = (" + g.l + " + " + g.r + ")";
				}
				xxx += "\n\n";

			} 
			else if (gh instanceof EED) {
				EED c = (EED) gh;
				XInst f = doED(/*cols, */ c.from1, c.where1, exp);
		
				c.from2.putAll(c.from1);
				c.where2.addAll(c.where1);
				XInst g = doED(/* cols, */ c.from2, c.where2, exp);
		
				List<Pair<Pair<String, String>, List<String>>> vm = new LinkedList<>();
				for (Pair<String, String> x : f.nodes) {
					List<String> l = new LinkedList<>();
					l.add(x.first);
					vm.add(new Pair<>(new Pair<>(x.first, null), l));
				}
				
				XTransConst i = new XTransConst(f, g, vm);
			
				xxx += longSlash + "\n/* Translation of " + k + " */\n" + longSlash;
				xxx += "\n\n" + k + "A = " + f + " : S";
				xxx += "\n\n" + k + "E = " + g + " : S";
				xxx += "\n\n" + k + "I = " + i + " : " + k + "A -> " + k + "E";
				xxx += "\n\n";
			} else {
				throw new RuntimeException();
			}
		}

		String comment = "//schema S and instance I represent the entire input database.\n\n";
		String preS = "adom : type\n"; 
		String senums0 = preS + Util.sep(enums.stream().map(x -> x + " : adom").collect(Collectors.toList()), "\n");
		return comment + senums0 + "\n\nS = " + exp + "\n\nI = " + inst + " : S" + xxx;
	}

	private static XInst doED(/* HashMap<String, List<String>> cols, */Map<String, String> from,
			List<Pair<Pair<String, String>, Pair<String, String>>> where, XSchema S) {
		List<Pair<String, String>> nodes = new LinkedList<>();
		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();

		for (String v : from.keySet()) {
			String t = from.get(v);
			nodes.add(new Pair<>(v, t));
		}

		//: no constants for now
		for (Pair<Pair<String, String>, Pair<String, String>> eq : where) {
			String v1 = eq.first.first;
			String t1 = from.get(v1);
			List<String> lhs = new LinkedList<>();
			lhs.add(v1);
			lhs.add(t1 + "_" + eq.first.second);
			List<String> rhs = new LinkedList<>();

			if (eq.second.second != null) {
				String v2 = eq.second.first;
				String t2 = from.get(v2);
				rhs.add(v2);
				rhs.add(t2 + "_" + eq.second.second);
			} else {
				rhs.add(eq.second.first);
			}

			eqs.add(new Pair<>(lhs, rhs));

		}
		
		return new XInst(S, nodes, eqs);
	}

	private static Pair<String, XSchema> trans(XSchema src, EFlower fl, String pre, Set<Object> enums) {
		// SigExp src0 = new SigExp.Var("S");

//		LinkedList<Pair<List<String>, List<String>>> eqs = 

		List<String> nodes1 = new LinkedList<>();
		List<String> nodes2 = new LinkedList<>();
		List<String> nodes3 = new LinkedList<>();
	//	nodes1.add("adom");
	//	nodes2.add("adom");
		nodes2.add("guid");
	//	nodes3.add("adom");

	//	List<Triple<String, String, String>> attrs = new LinkedList<>();
	//	attrs.add(new Triple<>("att", "adom", "adom"));

		List<Triple<String, String, String>> edges1 = new LinkedList<>();
		List<Triple<String, String, String>> edges2 = new LinkedList<>();
		List<Triple<String, String, String>> edges3 = new LinkedList<>();

		List<Pair<String, String>> inodes1 = new LinkedList<>();
		List<Pair<String, String>> inodes2 = new LinkedList<>();
		List<Pair<String, String>> inodes3 = new LinkedList<>();
	//	inodes1.add(new Pair<>("adom", "adom"));
	//	inodes2.add(new Pair<>("adom", "adom"));
	//	inodes3.add(new Pair<>("adom", "adom"));

	//	List<Pair<String, String>> iattrs = new LinkedList<>();
	//	iattrs.add(new Pair<>("att", "att"));

		List<Pair<String, List<String>>> iedges1 = new LinkedList<>();
		List<Pair<String, List<String>>> iedges2 = new LinkedList<>();
		List<Pair<String, List<String>>> iedges3 = new LinkedList<>();

		for (String k : fl.from.keySet()) {
			String v = fl.from.get(k);
			inodes1.add(new Pair<>(k, v));
			nodes1.add(k);
			inodes2.add(new Pair<>(k, "guid"));
			for (Triple<String, String, String> arr : src.arrows) {
				if (arr.second.equals(v)) {
					List<String> l = new LinkedList<>();
					l.add(v);
					l.add(arr.first);
					edges1.add(new Triple<>(k + "_" + arr.first, k, "adom"));
					iedges1.add(new Pair<>(k + "_" + arr.first, l));
					edges2.add(new Triple<>(k + "_" + arr.first, "guid", "adom"));

					List<String> l0 = new LinkedList<>();
					l0.add("guid");
					l0.add(k + "_" + arr.first);
					iedges2.add(new Pair<>(k + "_" + arr.first, l0));
				}
			}
		}

		List<List<Triple<String, String, String>>> eqcs = merge(edges2, fl);
		
		//for each p.q = 3, add (eqc_for(p.q).get(0) = 3) to some list

		Iterator<Triple<String, String, String>> it = edges2.iterator();
		while (it.hasNext()) {
			Triple<String, String, String> k = it.next();
			for (List<Triple<String, String, String>> v : eqcs) {
				if (v.contains(k) && !v.get(0).equals(k)) {
					it.remove();
				}
			}
		}
	
		for (Pair<String, List<String>> kk : iedges2) {
			Triple<String, String, String> k = new Triple<>(kk.second.get(1), "guid", "adom");
			for (List<Triple<String, String, String>> v : eqcs) {
				if (v.contains(k) && !v.get(0).equals(k)) {
					List<String> xxx = new LinkedList<>();
					xxx.add("guid");
					xxx.add(v.get(0).first);
					kk.setSecond(xxx);
					break;
				}
			}
		}

		nodes3.add("guid");
		inodes3.add(new Pair<>("guid", "guid"));
		// List<String> ll = new LinkedList<>();
		// ll.add("guid");
		// iedges3.add(new Pair<>("adom", ll));

		for (String k : fl.select.keySet()) {
			Pair<String, String> v = fl.select.get(k);
			edges3.add(new Triple<>(k, "guid", "adom"));
			Triple<String, String, String> t = new Triple<>(v.first + "_" + fl.from.get(v.first)
					+ "_" + v.second, "guid", "adom");
			if (fl.from.get(v.first) == null) {
				throw new RuntimeException(v.first + " is not selectable in " + fl);
			}
			for (List<Triple<String, String, String>> eqc : eqcs) {
				if (eqc.contains(t)) {
					List<String> li = new LinkedList<>();
					li.add("guid");
					li.add(eqc.get(0).first);
					iedges3.add(new Pair<>(k, li));
				} 
			}
		}

		XSchema sig1 = doSchema(nodes1, /* attrs, */ edges1, new LinkedList<>());
		XSchema sig2 = doSchema(nodes2, /* attrs, */ edges2, new LinkedList<>());
		XSchema sig3 = doSchema(nodes3, /* attrs, */ edges3, new LinkedList<>());
		
		for (Pair<Pair<String, String>, Pair<String, String>> x : fl.where) {
			if (x.second.second != null) {
				continue;
			}
			String c = x.second.first; //: add to global consts
			enums.add(c);
			Triple<String, String, String> found = null;
			Triple<String, String, String> tofind = new Triple<>(x.first.first + "_" + fl.from.get(x.first.first) + "_" + x.first.second, "guid", "adom");
			for (List<Triple<String, String, String>> eqc : eqcs) {
				if (eqc.contains(tofind)) {
					found = eqc.get(0);
					break;
				}
			}
			if (found == null) {
				throw new RuntimeException("Bad flower: " + fl);
			}
			List<String> lhs = new LinkedList<>();
			lhs.add(found.first);
			//lhs.add("att");
			List<String> rhs = new LinkedList<>();
			rhs.add("\"!_guid\"");
			rhs.add(c);
			Pair<List<String>, List<String>> eq = new Pair<>(lhs, rhs);
			sig2.eqs.add(eq);
		}

		XMapConst map1 = doMapping(inodes1, /* iattrs, */ iedges1, sig1, new Var("S"));
		XMapConst map2 = doMapping(inodes2, /* iattrs, */ iedges2, src, sig2);
		XMapConst map3 = doMapping(inodes3, /* iattrs, */ iedges3, sig3, sig2);

		String xxx = "";
		xxx += "\n\n" + pre + "fromSchema = " + sig1;
		xxx += "\n\n" + pre + "fromMapping = " + map1 + " : " + pre
				+ "fromSchema -> S";
		xxx += "\n\n" + pre + "fromInstance = delta " + pre + "fromMapping I";

		xxx += "\n\n" + pre + "whereSchema = " + sig2;
		xxx += "\n\n" + pre + "whereMapping = " + map2 + " : " + pre
				+ "fromSchema -> " + pre + "whereSchema";
		xxx += "\n\n" + pre + "whereInstance = pi " + pre + "whereMapping " + pre
				+ "fromInstance";

		xxx += "\n\n" + pre + "Schema = " + sig3;
		xxx += "\n\n" + pre + "selectMapping = " + map3 + " : " + pre
				+ "Schema -> " + pre + "whereSchema";

		if (fl.distinct) {
			xxx += "\n\n" + pre + "selectInstance = delta " + pre + "selectMapping " + pre
					+ "whereInstance";
			xxx += "\n\n" + pre + " = relationalize " + pre + "selectInstance";
		} else {
			xxx += "\n\n" + pre + " = delta " + pre + "selectMapping " + pre
					+ "whereInstance";
		}
		String comment = longSlash + "\n/* " + "Translation of " + pre + "  */\n" + longSlash;
		return new Pair<>(comment + xxx, sig3);
	}

	private static final String longSlash = "////////////////////////////////////////////////////////////////////////////////";

	private static List<List<Triple<String, String, String>>> merge(
			List<Triple<String, String, String>> edges2,
			// List<Pair<String, List<String>>> iedges2,
			EFlower ef) {

		List<List<Triple<String, String, String>>> eqcs = new LinkedList<>();
		for (Triple<String, String, String> k : edges2) {
			List<Triple<String, String, String>> l = new LinkedList<>();
			l.add(k);
			eqcs.add(l);
		}
		for (Pair<Pair<String, String>, Pair<String, String>> k : ef.where) {
			if (k.second.second == null) {
				continue;
			}
			mergeEqc(eqcs, k.first, k.second, ef.from);
		}

		return eqcs;
	}

	private static void mergeEqc(List<List<Triple<String, String, String>>> eqcs,
			Pair<String, String> l, Pair<String, String> r, Map<String, String> from) {
		Triple<String, String, String> l0 = new Triple<>(l.first + "_" + from.get(l.first) + "_"
				+ l.second, "guid", "adom");
		Triple<String, String, String> r0 = new Triple<>(r.first + "_" + from.get(r.first) + "_"
				+ r.second, "guid", "adom");
	
		List<Triple<String, String, String>> lx = null, rx = null;
		lbl: for (List<Triple<String, String, String>> k : eqcs) {
			if (!k.contains(l0)) {
				continue;
			}
			for (List<Triple<String, String, String>> v : eqcs) {
				if (k.equals(v)) {
					continue;
				}
				if (v.contains(r0)) {
					lx = k;
					rx = v;
					break lbl;
				}
			}
		}
		if (Objects.equals(lx, rx)) {
			return;
		}
		if (rx == null || lx == null) {
			throw new RuntimeException("Anomaly: please report");
		}
		eqcs.remove(rx);
		lx.addAll(rx);
	}

	public static Object maybeQuote(Object o) {
		if (o instanceof String) {
			String x = (String) o;
			if (x.startsWith("\"") && x.endsWith("\"")) {
				return o;
			}
			try {
				return Integer.parseInt(x);
			} catch (Exception ex) {
			}
			return "\"" + o + "\"";
		}
		return o;
	}

	private static List<Pair<Object, Object>> lookup2(String target,
			List<Pair<String, List<Pair<Object, Object>>>> inodes) {
		for (Pair<String, List<Pair<Object, Object>>> k : inodes) {
			if (k.first.equals(target)) {
				return k.second;
			}
		}
		return null;
		// throw new RuntimeException("Not found: " + target + " in " + inodes);
	}

	public static class EInsertValues extends EExternal {
		final String target;
		final List<List<String>> values;

		public EInsertValues(String target, List<List<String>> values) {
			this.target = target;
			this.values = values;
		}

	}

	public abstract static class EExternal {
	}

	public static class EED extends EExternal {

		public static <T> Set<T> diff(Set<? extends T> s1, Set<? extends T> s2) {
			Set<T> symmetricDiff = new HashSet<>(s1);
			symmetricDiff.addAll(s2);
			Set<T> tmp = new HashSet<>(s1);
			tmp.retainAll(s2);
			symmetricDiff.removeAll(tmp);
			return symmetricDiff;
		}

		public EED(Map<String, String> from1, Map<String, String> from2,
				List<Pair<Pair<String, String>, Pair<String, String>>> where1,
				List<Pair<Pair<String, String>, Pair<String, String>>> where2) {
			this.from1 = from1;
			this.from2 = from2;
			this.where1 = where1;
			this.where2 = where2;
			if (diff(from1.keySet(), from2.keySet()).isEmpty()) {
				throw new RuntimeException("Non-disjoint AS clauses in " + this);
			}
		}

		final Map<String, String> from1;
		final Map<String, String> from2;
		final List<Pair<Pair<String, String>, Pair<String, String>>> where1;
		final List<Pair<Pair<String, String>, Pair<String, String>>> where2;

		@Override
		public String toString() {
			String x = "FORALL ";
			boolean b = false;
			for (String k : from1.keySet()) {
				if (b) {
					x += ", ";
				}
				b = true;
				String p = from1.get(k);
				x += p + " AS " + k;
			}
			// if (where1.size() > 0) {
			x += "\nWHERE ";
			// }

			b = false;
			for (Pair<Pair<String, String>, Pair<String, String>> k : where1) {
				if (b) {
					x += " AND ";
				}
				b = true;
				x += k.first.first + "." + k.first.second + " = " + k.second.first + "."
						+ k.second.second;
			}

			b = false;
			x += "\nEXISTS ";
			for (String k : from2.keySet()) {
				if (b) {
					x += ", ";
				}
				b = true;
				String p = from2.get(k);
				x += p + " AS " + k;
			}
			// if (where1.size() > 0) {
			x += "\nWHERE ";
			// }

			b = false;
			for (Pair<Pair<String, String>, Pair<String, String>> k : where2) {
				if (b) {
					x += " AND ";
				}
				b = true;
				x += k.first.first + "." + k.first.second + " = " + k.second.first + "."
						+ k.second.second;
			}

			return x;
		}
	}

	public static class EFlower extends EExternal {
		final Map<String, Pair<String, String>> select;
		final Map<String, String> from;
		final List<Pair<Pair<String, String>, Pair<String, String>>> where;
		final boolean distinct;

		public EFlower(Map<String, Pair<String, String>> select, Map<String, String> from,
				List<Pair<Pair<String, String>, Pair<String, String>>> where, boolean distinct) {
			this.select = select;
			this.from = from;
			this.where = where;
			this.distinct = distinct;
		}

		@Override
		public String toString() {
			String x = "SELECT ";
			if (distinct) {
				x += "DISTINCT ";
			}
			boolean b = false;
			for (String k : select.keySet()) {
				if (b) {
					x += ", ";
				}
				b = true;
				Pair<String, String> p = select.get(k);
				x += p.first + "." + p.second + " AS " + k;
			}
			x += "\nFROM ";

			b = false;
			for (String k : from.keySet()) {
				if (b) {
					x += ", ";
				}
				b = true;
				String p = from.get(k);
				x += p + " AS " + k;
			}
			if (!where.isEmpty()) {
				x += "\nWHERE ";
			}

			b = false;
			for (Pair<Pair<String, String>, Pair<String, String>> k : where) {
				if (b) {
					x += " AND ";
				}
				b = true;
				x += k.first.first + "." + k.first.second + " = " + k.second.first + "."
						+ k.second.second;
			}

			return x;
		}
	}

	public static class EUnion extends EExternal {
		final boolean distinct;
		final String l;
		final String r;

		public EUnion(boolean distinct, String l, String r) {
			this.distinct = distinct;
			this.l = l;
			this.r = r;
		}

		@Override
		public String toString() {
			String x = "";
			if (!distinct) {
				x += " ALL";
			}
			return l + "\n" + "UNION" + x + "\n" + r;
		}
	}

	public static class EDiff extends EExternal {
		final boolean distinct;
		final String l;
		final String r;

		public EDiff(boolean distinct, String l, String r) {
			this.distinct = distinct;
			this.l = l;
			this.r = r;
		}

		@Override
		public String toString() {
			String x = "";
			if (!distinct) {
				x += " ALL";
			}
			return l + "\n" + "EXCEPT" + x + "\n" + r;
		}
	}

	public static class ECreateTable extends EExternal {
		final String name;
		final List<Pair<String, String>> types;

		public ECreateTable(String name, List<Pair<String, String>> types) {
			this.name = name;
			this.types = types;
		}
	}

	static final Parser<Integer> NUMBER = IntegerLiteral.PARSER
			.map(Integer::valueOf);

	private static final String[] ops = new String[] { ",", ".", ";", ":", "{", "}", "(", ")", "=", "->", "+",
			"*", "^", "|" };

	private static final String[] res = new String[] { "VARCHAR", "INT", "SELECT", "FROM", "WHERE", "DISTINCT",
			"UNION", "EXCEPT", "ALL", "CREATE", "TABLE", "AS", "AND", "OR", "NOT", "INSERT",
			"INTO", "VALUES", "FORALL", "EXISTS" };

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT,
			Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	private static final Parser<?> TOKENIZER = Parsers.or(
			(Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER, RESERVED.tokenizer(),
			(Parser<?>) Identifier.TOKENIZER,
			(Parser<?>) IntegerLiteral.TOKENIZER);

	private static Parser<?> term(String... names) {
		return RESERVED.token(names);
	}

	private static Parser<?> ident() {
		return Identifier.PARSER;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List<Pair<String, EExternal>> program(String s) {
		List<Pair<String, EExternal>> ret = new LinkedList<>();
		List<Tuple3> decls = (List<Tuple3>) program.parse(s);

		for (Tuple3 decl : decls) {
			if (decl.a.toString().equals("CREATE")) {
				ret.add(new Pair<>(null, toECreateTable(decl)));
			} else if (decl.toString().contains("INSERT")) {
				ret.add(new Pair<>(null, toEInsertValues(decl)));
			} else if (decl.toString().contains("SELECT")) {
				ret.add(new Pair<>(decl.a.toString(), toFlower(decl.c)));
			} else if (decl.toString().contains("UNION")) {
				ret.add(new Pair<>(decl.a.toString(), toUnion(decl.c)));
			} else if (decl.toString().contains("EXCEPT")) {
				ret.add(new Pair<>(decl.a.toString(), toDiff(decl.c)));
			} else if (decl.toString().contains("FORALL")) {
				ret.add(new Pair<>(decl.a.toString(), toEd(decl.c)));
			} else {
				throw new RuntimeException();
			}
		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	private static ECreateTable toECreateTable(Object decl) {
		Tuple4 t = (Tuple4) decl;

		String name = t.c.toString();
		Tuple3 t0 = (Tuple3) t.d;
		List t1 = (List) t0.b;

		List<Pair<String, String>> types = new LinkedList<>();

		for (Object o : t1) {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			types.add(new Pair<>(p.a.toString(), p.b.toString()));
		}
		return new ECreateTable(name, types);
	}

	private static final Parser<?> program = program().from(TOKENIZER, IGNORED);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static EInsertValues toEInsertValues(Object decl) {
		Tuple5 t = (Tuple5) decl;
		String target = t.b.toString();
		List<Tuple3> x = (List<Tuple3>) t.d;
		List<List<String>> values = new LinkedList<>();
		for (Tuple3 y : x) {
			List<String> l = (List<String>) y.b;
			values.add(l);
		}
		return new EInsertValues(target, values);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Parser<?> ed() {
		Parser tuple = Parsers.tuple(ident(), term("."), ident());

		Parser<?> from0 = Parsers.tuple(ident(), term("AS"), ident()).sepBy1(term(","));
		Parser<?> from1 = Parsers.tuple(term("FORALL"), from0);
		Parser<?> from2 = Parsers.tuple(term("EXISTS"), from0);

		Parser<?> where0 = Parsers.tuple(tuple, term("="), tuple.or(string())).sepBy(term("AND"));

//		Parser<?> where0 = Parsers.tuple(tuple, term("="), tuple).sepBy(term("AND"));
		Parser<?> where = Parsers.tuple(term("WHERE"), where0).optional();

		return Parsers.tuple(from1, where, from2, where);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Parser<?> flower() {
		Parser tuple = Parsers.tuple(ident(), term("."), ident());

		Parser<?> from0 = Parsers.tuple(ident(), term("AS"), ident()).sepBy(term(","));
		Parser<?> from = Parsers.tuple(term("FROM"), from0);

		Parser<?> where0 = Parsers.tuple(tuple, term("="), tuple.or(string())).sepBy(term("AND"));
		Parser<?> where = Parsers.tuple(term("WHERE"), where0); 

		Parser<?> select0 = Parsers.tuple(tuple, term("AS"), ident()).sepBy1(term(","));
		Parser<?> select = Parsers.tuple(term("SELECT"), term("DISTINCT").optional(), select0);

		return Parsers.tuple(select, from, where);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static EED toEd(Object decl) {
		Map<String, String> from1 = new LinkedHashMap<>();
		Map<String, String> from2 = new LinkedHashMap<>();
		List<Pair<Pair<String, String>, Pair<String, String>>> where1 = new LinkedList<>();
		List<Pair<Pair<String, String>, Pair<String, String>>> where2 = new LinkedList<>();

		Tuple4 o = (Tuple4) decl;

		org.jparsec.functors.Pair from10 = (org.jparsec.functors.Pair) o.a;
		org.jparsec.functors.Pair where10 = (org.jparsec.functors.Pair) o.b;
		org.jparsec.functors.Pair from11 = (org.jparsec.functors.Pair) o.c;
		org.jparsec.functors.Pair where11 = (org.jparsec.functors.Pair) o.d;

		List<Tuple3> from10x = (List<Tuple3>) from10.b;
		for (Tuple3 k : from10x) {
			from1.put(k.c.toString(), k.a.toString());
		}

		// if (where0 == null) {
		//
		// } else {
		List<Tuple3> where10x = (List<Tuple3>) where10.b;
		for (Tuple3 k : where10x) {
			Tuple3 l = (Tuple3) k.a;
			if (k.c instanceof Tuple3) {
			Tuple3 r = (Tuple3) k.c;
			where1.add(new Pair<>(new Pair<>(l.a.toString(), l.c.toString()), new Pair<>(r.a
					.toString(), r.c.toString())));
			} else {
				String r = (String) k.c;
				where1.add(new Pair<>(new Pair<>(l.a.toString(), l.c.toString()), new Pair<>(r, null)));				
			}
		}
		// }

		List<Tuple3> from11x = (List<Tuple3>) from11.b;
		for (Tuple3 k : from11x) {
			from2.put(k.c.toString(), k.a.toString());
		}

		// if (where0 == null) {
		//
		// } else {
		List<Tuple3> where11x = (List<Tuple3>) where11.b;
		for (Tuple3 k : where11x) {
			Tuple3 l = (Tuple3) k.a;
			if (k.c instanceof Tuple3) {
				Tuple3 r = (Tuple3) k.c;
				where2.add(new Pair<>(new Pair<>(l.a.toString(), l.c.toString()), new Pair<>(r.a
						.toString(), r.c.toString())));
				} else {
					String r = (String) k.c;
					where2.add(new Pair<>(new Pair<>(l.a.toString(), l.c.toString()), new Pair<>(r, null)));				
				}
		}
		// }

		return new EED(from1, from2, where1, where2);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static EFlower toFlower(Object decl) {
		Map<String, Pair<String, String>> select = new LinkedHashMap<>();
		Map<String, String> from = new LinkedHashMap<>();
		List<Pair<Pair<String, String>, Pair<String, String>>> where = new LinkedList<>();

		Tuple3 o = (Tuple3) decl;

		Tuple3 select0 = (Tuple3) o.a;
		org.jparsec.functors.Pair from0 = (org.jparsec.functors.Pair) o.b;
		org.jparsec.functors.Pair where0 = (org.jparsec.functors.Pair) o.c;

		boolean distinct;
        distinct = select0.b != null;

		List<Tuple3> select1 = (List<Tuple3>) select0.c;
		for (Tuple3 k : select1) {
			Tuple3 a = (Tuple3) k.a;
			String b = k.c.toString();
			select.put(b, new Pair<>(a.a.toString(), a.c.toString()));
		}

		List<Tuple3> from1 = (List<Tuple3>) from0.b;
		for (Tuple3 k : from1) {
			from.put(k.c.toString(), k.a.toString());
		}

		if (where0 != null) {
			List<Tuple3> where1 = (List<Tuple3>) where0.b;
			for (Tuple3 k : where1) {
				Tuple3 l = (Tuple3) k.a;
				if (k.c instanceof Tuple3) {
				Tuple3 r = (Tuple3) k.c;
				where.add(new Pair<>(new Pair<>(l.a.toString(), l.c.toString()), new Pair<>(r.a
						.toString(), r.c.toString())));
				} else {
					String r = (String) k.c;
					where.add(new Pair<>(new Pair<>(l.a.toString(), l.c.toString()), new Pair<>(r, null)));
				}
			}
		}

		return new EFlower(select, from, where, distinct);
	}

	@SuppressWarnings("rawtypes")
	private static EUnion toUnion(Object o) {
		Tuple4 t = (Tuple4) o;
		return new EUnion(t.c == null, t.a.toString(), t.d.toString());
	}

	@SuppressWarnings("rawtypes")
	private static EDiff toDiff(Object o) {
		Tuple4 t = (Tuple4) o;
		return new EDiff(t.c == null, t.a.toString(), t.d.toString());
	}

	private static Parser<?> union() {
		// Parser<?> p = flower().between(term("("), term(")"));
		return Parsers.tuple(ident(), term("UNION"), term("ALL").optional(), ident());
	}

	private static Parser<?> diff() {
		// Parser<?> p = flower().between(term("("), term(")"));
		return Parsers.tuple(ident(), term("EXCEPT"), term("ALL").optional(), ident());
	}

	private static Parser<?> insertValues() {
		Parser<?> p = string().sepBy(term(","));
		return Parsers.tuple(Parsers.tuple(term("INSERT"), term("INTO")), ident(), term("VALUES"),
				Parsers.tuple(term("("), p, term(")")).sepBy(term(",")), term(";"));
	}

	private static Parser<?> createTable() {
		Parser<?> q2 = Parsers.tuple(ident(), term("INT"));
		Parser<?> q3 = Parsers.tuple(ident(), term("VARCHAR"),
				IntegerLiteral.PARSER.between(term("("), term(")")));
		Parser<?> p = Parsers.or(q2, q3).sepBy1(term(","));

		return Parsers.tuple(term("CREATE"), term("TABLE"), ident(),
				Parsers.tuple(term("("), p, term(")")), term(";"));
	}

	private static Parser<?> program() {
		Parser<?> p1 = Parsers.tuple(ident(), term("="), flower());
		Parser<?> p2 = Parsers.tuple(ident(), term("="), union());
		Parser<?> p3 = Parsers.tuple(ident(), term("="), diff());
		Parser<?> p4 = Parsers.tuple(ident(), term("="), ed());

		return Parsers.or(createTable(), insertValues(), p1, p2, p3, p4).many();
	}

	private static Parser<?> string() {
		return Parsers.or(StringLiteral.PARSER, IntegerLiteral.PARSER,
				Identifier.PARSER);
	}
}

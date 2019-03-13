package catdata.fpql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
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
import catdata.Triple;
import catdata.Util;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.Language;

/**
 * 
 * @author ryan
 *
 */
public class XJsonToFQL {

	private final Example[] examples = { new EmpEx() };

	static class EmpEx extends Example {
		
		@Override
		public Language lang() {
			return null;
		}

		@Override
		public String getName() {
			return "Employees";
		}

		@Override
		public String getText() {
			return emp_str;
		}
		
	}
	
	private final String help = "Translates Idea Flow JSON into an FPQL schema";

	private static String kind() {
		return "JSON";
	}

	private static String translate(String in) {
		try {
			JsonReader rdr = Json.createReader(new StringReader(in));
			JsonObject obj = rdr.readObject();
			JsonObject graph = obj.getJsonObject("graph");
			JsonArray nodes = graph.getJsonArray("nodes");
			JsonArray edges = graph.getJsonArray("edges");
			JsonArray eqs = graph.getJsonArray("equations");
			Set<String> types = new HashSet<>();
			Set<String> entities = new HashSet<>();
			Set<Triple<String, String, String>> consts = new HashSet<>();
			Set<Triple<String, String, String>> fks = new HashSet<>();
			Set<Pair<List<String>, List<String>>> tfns = new HashSet<>();
			Set<Pair<List<String>, List<String>>> efns = new HashSet<>();
			Set<String> all_e = new HashSet<>();
			 for (JsonObject o : nodes.getValuesAs(JsonObject.class)) {
				 String id = o.getJsonString("id").toString();
				 String ty = o.getJsonString("type").toString();
				 if (ty.equals("\"type\"")) {
					 types.add(id);
				 } else {
					 entities.add(id);
					 all_e.add(id);
				 }
			 }
			 for (JsonObject o : edges.getValuesAs(JsonObject.class)) {
				 String id = o.getJsonString("id").toString();
				 String src = o.getJsonString("source").toString();
				 String dst = o.getJsonString("target").toString();
				 if (!types.contains(src) && !types.contains(dst)) {
					 all_e.add(id);
				 }
				 if (dst.equals("\"_1\"")) {
					 continue;
				 }
				 if (types.contains(src)) {
					 consts.add(new Triple<>(id, src, dst));
				 } else {
					 fks.add(new Triple<>(id, src, dst));
				 }
			 }
			 outer: for (JsonObject o : eqs.getValuesAs(JsonObject.class)) {
					JsonArray lhs = o.getJsonArray("lhs");
					JsonArray rhs = o.getJsonArray("rhs");
					List<String> l = new LinkedList<>();
					List<String> r = new LinkedList<>();
					boolean isty = false; 
					for (JsonString x : lhs.getValuesAs(JsonString.class)) {
						 String s = x.toString();
						 if (s.contains("!_")) {
							 continue outer;
						 }
						 if (!all_e.contains(s)) {
							 isty = true;
						 }
						 l.add(s);
					}
					for (JsonString x : rhs.getValuesAs(JsonString.class)) {
						 String s = x.toString();
						 if (s.contains("!_")) {
							 continue outer;
						 }
						 if (!all_e.contains(s)) {
							 isty = true;
						 }
						 r.add(s);						
					}
					if (isty) {
						tfns.add(new Pair<>(l, r));
					} else {
						efns.add(new Pair<>(l, r));
					}
			 }
			 
			 String ret = "";
			 for (String ty : types) {
				 if (ty.equals("\"_1\"")) {
					 continue;
				 }
				 ret += ty + " : type\n"; 
			 }
			 ret += "\n";
			 for (Triple<String, String, String> c : consts) {
				 if (c.third.equals("\"_1\"")) {
					 continue;
				 }
                 ret += c.second.equals("\"_1\"") ? c.first + " : " + c.third + "\n" : c.first + " : " + c.second + " -> " + c.third + "\n";
			 }
			 ret += "\n";
			 Set<String> temp1 = new HashSet<>();
			 for (Pair<List<String>, List<String>> x : tfns) {
				 temp1.add( Util.sep(x.first, ".") + " = " + Util.sep(x.second, "."));
			 }
			 ret += Util.sep(temp1, ",\n");
			 
			 ret += "\n";
			 ret += "S = schema {\n";
			 ret += "nodes\n";
			 ret += Util.sep(entities, ",\n");
			 
			 ret += ";\n";
			 ret += "edges\n";
			 ret += Util.sep(fks.stream().map(x -> x.first + " : " + x.second + " -> " + x.third).collect(Collectors.toList()), ",\n");
			 
			 ret += ";\n";
			 ret += "equations\n";
			 Set<String> temp2 = new HashSet<>();
			 for (Pair<List<String>, List<String>> x : efns) {
				 temp2.add( Util.sep(x.first, ".") + " = " + Util.sep(x.second, ".") );
			 }
			 ret += Util.sep(temp2, ",\n");
			 
			 ret += ";\n";
			 ret += "}\n";
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public XJsonToFQL() {
		CodeTextPanel input = new CodeTextPanel(BorderFactory.createEtchedBorder(), kind()
				+ " Input", "");
		CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(),
				"FPQL Output", "");

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
                    JScrollPane p = new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    p.setPreferredSize(new Dimension(300, 200));
                    
                    JOptionPane pane = new JOptionPane(p);
                    // Configure via set methods
                    JDialog dialog = pane.createDialog(null, "Help on JSON to FPQL");
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
		JFrame f = new JFrame(kind() + " to FPQL");
		f.setContentPane(p);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
	
	private static final String emp_str = "{\"graph\": { \"directed\":true,"
			+ "\n\"nodes\":["
			+ "\n{\"id\": \"dom\", \"type\":\"type\", \"label\":\"dom\"},"
			+ "\n{\"id\": \"Employee\", \"type\":\"entity\", \"label\":\"Employee\"},"
			+ "\n{\"id\": \"_1\", \"type\":\"type\", \"label\":\"_1\"},"
			+ "\n{\"id\": \"Department\", \"type\":\"entity\", \"label\":\"Department\"}], "
			+ "\n\"edges\":["
			+ "\n{\"id\": \"Carl\", \"directed\":\"true\", \"label\":\"Carl\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"last\", \"directed\":\"true\", \"label\":\"last\", \"source\":\"Employee\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"Bob\", \"directed\":\"true\", \"label\":\"Bob\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"manager\", \"directed\":\"true\", \"label\":\"manager\", \"source\":\"Employee\", \"target\":\"Employee\"},"
			+ "\n{\"id\": \"!_Employee\", \"directed\":\"true\", \"label\":\"!_Employee\", \"source\":\"Employee\", \"target\":\"_1\"},"
			+ "\n{\"id\": \"!_dom\", \"directed\":\"true\", \"label\":\"!_dom\", \"source\":\"dom\", \"target\":\"_1\"},"
			+ "\n{\"id\": \"worksIn\", \"directed\":\"true\", \"label\":\"worksIn\", \"source\":\"Employee\", \"target\":\"Department\"},"
			+ "\n{\"id\": \"Bo\", \"directed\":\"true\", \"label\":\"Bo\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"name\", \"directed\":\"true\", \"label\":\"name\", \"source\":\"Department\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"first\", \"directed\":\"true\", \"label\":\"first\", \"source\":\"Employee\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"Al\", \"directed\":\"true\", \"label\":\"Al\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"secretary\", \"directed\":\"true\", \"label\":\"secretary\", \"source\":\"Department\", \"target\":\"Employee\"},"
			+ "\n{\"id\": \"Akin\", \"directed\":\"true\", \"label\":\"Akin\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"Cs\", \"directed\":\"true\", \"label\":\"Cs\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"Cork\", \"directed\":\"true\", \"label\":\"Cork\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"Math\", \"directed\":\"true\", \"label\":\"Math\", \"source\":\"_1\", \"target\":\"dom\"},"
			+ "\n{\"id\": \"!_Department\", \"directed\":\"true\", \"label\":\"!_Department\", \"source\":\"Department\", \"target\":\"_1\"},"
			+ "\n{\"id\": \"!__1\", \"directed\":\"true\", \"label\":\"!__1\", \"source\":\"_1\", \"target\":\"_1\"}"
			+ "\n],"
			+ "\n\"equations\":["
			+ "\n{\"lhs\": [\"Cs\", \"!_dom\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"!__1\", \"!__1\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"Akin\", \"!_dom\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"first\", \"!_dom\"], \"rhs\": [\"!_Employee\"]},"
			+ "\n{\"lhs\": [\"last\", \"!_dom\"], \"rhs\": [\"!_Employee\"]},"
			+ "\n{\"lhs\": [\"_1\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"manager\", \"!_Employee\"], \"rhs\": [\"!_Employee\"]},"
			+ "\n{\"lhs\": [\"secretary\", \"!_Employee\"], \"rhs\": [\"!_Department\"]},"
			+ "\n{\"lhs\": [\"Carl\", \"!_dom\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"Employee\", \"manager\", \"worksIn\"], \"rhs\": [\"Employee\", \"worksIn\"]},"
			+ "\n{\"lhs\": [\"dom\", \"!_dom\"], \"rhs\": [\"!_dom\"]},"
			+ "\n{\"lhs\": [\"!_Employee\", \"!__1\"], \"rhs\": [\"!_Employee\"]},"
			+ "\n{\"lhs\": [\"!_dom\", \"!__1\"], \"rhs\": [\"!_dom\"]},"
			+ "\n{\"lhs\": [\"Bo\", \"!_dom\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"name\", \"!_dom\"], \"rhs\": [\"!_Department\"]},"
			+ "\n{\"lhs\": [\"Department\", \"secretary\", \"worksIn\"], \"rhs\": [\"Department\"]},"
			+ "\n{\"lhs\": [\"Employee\", \"!_Employee\"], \"rhs\": [\"!_Employee\"]},"
			+ "\n{\"lhs\": [\"Bob\", \"!_dom\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"worksIn\", \"!_Department\"], \"rhs\": [\"!_Employee\"]},"
			+ "\n{\"lhs\": [\"Cork\", \"!_dom\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"Department\", \"!_Department\"], \"rhs\": [\"!_Department\"]},"
			+ "\n{\"lhs\": [\"_1\", \"!__1\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"Al\", \"!_dom\"], \"rhs\": [\"!__1\"]},"
			+ "\n{\"lhs\": [\"Employee\", \"manager\", \"manager\"], \"rhs\": [\"Employee\", \"manager\"]},"
			+ "\n{\"lhs\": [\"!_Department\", \"!__1\"], \"rhs\": [\"!_Department\"]},"
			+ "\n{\"lhs\": [\"Math\", \"!_dom\"], \"rhs\": [\"!__1\"]}"
			+ "\n]}}"
			+ "\n";




}

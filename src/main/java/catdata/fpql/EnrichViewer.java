package catdata.fpql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import catdata.fpql.XExp.Var;
import org.jparsec.error.ParserException;

import catdata.LineException;
import catdata.Pair;
import catdata.Triple;
import catdata.fpql.XExp.XSchema;
import catdata.fpql.XPoly.Block;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.Language;

public class EnrichViewer {

	abstract static class Example2 extends Example {
		public abstract String att();		
		public abstract String left();		
		public abstract String isa_inst();
		public abstract String toenrich_inst();		
	}
	
	static class NistExample extends Example2 {

		@Override
		public Language lang() {
			return null;
		}

		
		@Override
		public String left() {
			return "l";
		}

		@Override
		public String att() {
			return "material_Material_Name";
		}
		
		@Override
		public String isa_inst() {
			return "enrich_with";
		}

		@Override
		public String toenrich_inst() {
			return "I";
		}

		@Override
		public String getName() {
			return "NIST";
		}

		@Override
		public String getText() {
			return s;
		}
		
		final String s = "adom : type"
				+ "\n"
				+ "\nS = schema {"
				+ "\n nodes"
				+ "\n  unitcode,"
				+ "\n  productorservicecategory,"
				+ "\n  equipmenttype,"
				+ "\n  industry,"
				+ "\n  material,"
				+ "\n  moldtypes,"
				+ "\n  process,"
				+ "\n  supplier,"
				+ "\n  capability,"
				+ "\n  capabilitycategories,"
				+ "\n  capabilityequipment,"
				+ "\n  capabilityindustry,"
				+ "\n  capabilitymaterials,"
				+ "\n  capabilityprocesses,"
				+ "\n  capabilitytypesofmolds,"
				+ "\n  suppliercapabilities;"
				+ "\n edges"
				+ "\n  productorservicecategory_Parent_id: productorservicecategory -> productorservicecategory,"
				+ "\n  capability_Parent_id: capability -> capability,"
				+ "\n  capability_Production_Volume_Min_Unit: capability -> unitcode,"
				+ "\n  capability_Production_Volume_Max_Unit: capability -> unitcode,"
				+ "\n  capability_Max_Length_Unit: capability -> unitcode,"
				+ "\n  capability_Tolerance_Unit: capability -> unitcode,"
				+ "\n  capabilitycategories_Capability_id: capabilitycategories -> capability,"
				+ "\n  capabilitycategories_ProductOrServiceCategory_id: capabilitycategories -> productorservicecategory,"
				+ "\n  capabilityequipment_Capability_id: capabilityequipment -> capability,"
				+ "\n  capabilityequipment_EquipmentType_id: capabilityequipment -> equipmenttype,"
				+ "\n  capabilityindustry_Capability_id: capabilityindustry -> capability,"
				+ "\n  capabilityindustry_Industry_id: capabilityindustry -> industry,"
				+ "\n  capabilitymaterials_Capability_id: capabilitymaterials -> capability,"
				+ "\n  capabilitymaterials_Material_id: capabilitymaterials -> material,"
				+ "\n  capabilityprocesses_Capability_id: capabilityprocesses -> capability,"
				+ "\n  capabilityprocesses_Process_id: capabilityprocesses -> process,"
				+ "\n  capabilitytypesofmolds_Capability_id: capabilitytypesofmolds -> capability,"
				+ "\n  capabilitytypesofmolds_MoldTypes_id: capabilitytypesofmolds -> moldtypes,"
				+ "\n  suppliercapabilities_Supplier_id: suppliercapabilities -> supplier,"
				+ "\n  suppliercapabilities_Capability_id: suppliercapabilities -> capability,"
				+ "\n  unitcode_Code: unitcode -> adom,"
				+ "\n  unitcode_Description: unitcode -> adom,"
				+ "\n  productorservicecategory_Category_Name: productorservicecategory -> adom,"
				+ "\n  productorservicecategory_isConcrete: productorservicecategory -> adom,"
				+ "\n  equipmenttype_EquipmentType_Name: equipmenttype -> adom,"
				+ "\n  industry_Industry_Name: industry -> adom,"
				+ "\n  material_Material_Name: material -> adom,"
				+ "\n  moldtypes_MoldTypes_Name: moldtypes -> adom,"
				+ "\n  process_Process_Name: process -> adom,"
				+ "\n  supplier_Source: supplier -> adom,"
				+ "\n  supplier_Note: supplier -> adom,"
				+ "\n  capability_Capability_Name: capability -> adom,"
				+ "\n  capability_Production_Volume_Min: capability -> adom,"
				+ "\n  capability_Production_Volume_Max: capability -> adom,"
				+ "\n  capability_Max_Length: capability -> adom,"
				+ "\n  capability_Tolerance: capability -> adom;"
				+ "\n equations"
				+ "\n  productorservicecategory.productorservicecategory_Parent_id.productorservicecategory_Parent_id.productorservicecategory_Parent_id = productorservicecategory.productorservicecategory_Parent_id.productorservicecategory_Parent_id.productorservicecategory_Parent_id.productorservicecategory_Parent_id,"
				+ "\n  capability.capability_Parent_id.capability_Parent_id.capability_Parent_id = capability.capability_Parent_id.capability_Parent_id.capability_Parent_id.capability_Parent_id;"
				+ "\n}"
				+ "\n"
				+ "\nisa_schema_new = schema {"
				+ "\n	nodes A, B;"
				+ "\n	edges l : A -> B, r : A -> B, name : B -> adom;"
				+ "\n	equations;"
				+ "\n}"
				+ "\n";

	
	}
	
	private final Example2[] examples = {new PeopleExample(), new NistExample()};

	private final String help = "Schema, then Is_a"; // "SQL schemas and instances in categorical normal form (CNF) can be treated as FQL instances directly.  To be in CNF, every table must have a primary key column called id.  This column will be treated as a meaningless ID.  Every column in a table must either be a string, an integer, or a foreign key to another table.  Inserted values must be quoted.  See the People example for details.";

	protected static String kind() {
		return "Enrich";
	}

	
	private final CodeTextPanel topArea = new CodeTextPanel(BorderFactory.createEtchedBorder(),
			"Input FQL", "//Schema, then isa");

	private final JTextField name = new JTextField("");
	private final JTextField kid= new JTextField("");
	private final JTextField instField= new JTextField("");
	private final JTextField isaField= new JTextField("");
	
	public EnrichViewer() {
		CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(),
				"Output FQL", "");

		// JButton jdbcButton = new JButton("Load using JDBC");
		// JButton runButton = new JButton("Run " + kind());
		JButton transButton = new JButton("Enrich");
		JButton helpButton = new JButton("Help");
		// JButton runButton2 = new JButton("Run FQL");
		// JCheckBox jdbcBox = new JCheckBox("Run using JDBC");
		// JLabel lbl = new JLabel("Suffix (optional):", JLabel.RIGHT);
		// lbl.setToolTipText("FQL will translate table T to T_suffix, and generate SQL to load T into T_suffix");
		// final JTextField field = new JTextField(8);
		// field.setText("fql");

		JComboBox<Example> box = new JComboBox<>(examples);
		box.setSelectedIndex(-1);
		box.addActionListener(e -> {
				topArea.setText(((Example) box.getSelectedItem()).getText());
				name.setText(((Example2) box.getSelectedItem()).att());
				kid.setText(((Example2) box.getSelectedItem()).left());
				instField.setText(((Example2) box.getSelectedItem()).toenrich_inst());
				isaField.setText(((Example2) box.getSelectedItem()).isa_inst());
			
		});

		transButton.addActionListener(e -> {
				try {
					String p = translate(topArea.getText());
					output.setText(p);
				} catch (Exception ex) {
					ex.printStackTrace();
					output.setText(ex.getLocalizedMessage());
				}
			
		});

		helpButton.addActionListener(e -> {
				JTextArea jta = new JTextArea(help);
				jta.setWrapStyleWord(true);
				// jta.setEditable(false);
				jta.setLineWrap(true);
				JScrollPane p = new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				p.setPreferredSize(new Dimension(300, 200));

				JOptionPane pane = new JOptionPane(p);
				// Configure via set methods
				JDialog dialog = pane.createDialog(null, "Help on Knuth-Bendix Completion");
				dialog.setModal(false);
				dialog.setVisible(true);
				dialog.setResizable(true);

			
		});

		JPanel p = new JPanel(new BorderLayout());

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setDividerSize(4);
		jsp.setResizeWeight(0.5d);
		jsp.add(topArea);
		jsp.add(output);

		// JPanel bp = new JPanel(new GridLayout(1, 5));
		JPanel tp = new JPanel(new GridLayout(2, 8));

		// bp.add(field);

		tp.add(transButton);
		tp.add(helpButton);
		// tp.add(jdbcButton);
		// tp.add(helpButton);
		tp.add(new JLabel());		
		tp.add(new JLabel());		
		tp.add(new JLabel());		
		tp.add(new JLabel());		
		tp.add(new JLabel("Load Example", SwingConstants.RIGHT));
		tp.add(box);
		
		tp.add(new JLabel("Attr:", SwingConstants.RIGHT));
		tp.add(name);
		tp.add(new JLabel("Kid:", SwingConstants.RIGHT));
		tp.add(kid);
		tp.add(new JLabel("Inst:", SwingConstants.RIGHT));
		tp.add(instField);
		tp.add(new JLabel("Isa:", SwingConstants.RIGHT));
		tp.add(isaField);


		// bp.add(runButton);
		// bp.add(runButton2);
		// bp.add(lbl);
		// bp.add(field);
		// bp.add(jdbcBox);

		// p.add(bp, BorderLayout.SOUTH);
		p.add(jsp, BorderLayout.CENTER);
		p.add(tp, BorderLayout.NORTH);
		JFrame f = new JFrame("Enrich");
		f.setContentPane(p);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
	
	private String translate(String program) {
		XProgram init;
		try {
			init = XParser.program(program);
	} catch (ParserException e) {
		int col = e.getLocation().column;
		int line = e.getLocation().line;
		topArea.requestFocusInWindow();
		topArea.area.setCaretPosition(topArea.area.getDocument()
				.getDefaultRootElement().getElement(line - 1)
				.getStartOffset()
				+ (col - 1));
		//String s = e.getMessage();
		//String t = s.substring(s.indexOf(" "));
		//t.split("\\s+");
		e.printStackTrace();
		return "Syntax error: " + e.getLocalizedMessage();
	} catch (Throwable e) {
		e.printStackTrace();
		return "Error: " + e.getLocalizedMessage();
	}
		if (init == null) {
			return "";
		}

		String isaX = null, matX = null;
		XSchema isa = null, mat = null;
		for (String line : init.order) {
			XExp exp = init.exps.get(line);
			if (exp instanceof XSchema) {
				if (isaX == null) {
					isaX = line;
					isa = (XSchema) exp;
					continue;
				}
				if (matX == null) {
					matX = line;
					mat = (XSchema) exp;
					continue;
				}
				throw new RuntimeException("More than two schemas");
			}
		}
		if (isaX == null || matX == null) {
			throw new RuntimeException("Fewer than two schemas");
		}
		
/*		if (isa.arrows.size() != 2) {
			String temp = isaX;
			XExp.XSchema temp2 = isa;
			isaX = matX;
			isa = mat;
			matX = temp;
			mat = temp2;
		} */

		XEnvironment env; 
		try {
			env = XDriver.makeEnv(program, init);
		} catch (LineException e) {
			String toDisplay = "Error in " + e.kind + " " + e.decl + ": "
					+ e.getLocalizedMessage();
			e.printStackTrace();
			topArea.requestFocusInWindow();
			Integer theLine = init.getLine(e.decl);
			topArea.area.setCaretPosition(theLine);
			return toDisplay;
		} catch (Throwable re) {
			return "Error: " + re.getLocalizedMessage();
		}
		
		@SuppressWarnings("unchecked")
		XCtx<String> isa0 = (XCtx<String>) env.objs.get(isaX);
		@SuppressWarnings("unchecked")
		XCtx<String> mat0 = (XCtx<String>) env.objs.get(matX);
		
		return go(isa, mat, isaX, matX, isa0, mat0, name.getText(), kid.getText(), instField.getText(), isaField.getText());
		
	}

	private static String go(XSchema mat, XSchema isa, String schemaName, String isaSchemaName, XCtx<String> mat0,
			@SuppressWarnings("unused") XCtx<String> isa0, String att, String kid, String inst, String isa_inst) {
		String node = nodeForAtt(mat, att);
		String r = otherEdge(isa, kid);
		String n = onlyAtt(isa);
		String a = src(isa, kid);
		
		Triple<XSchema, XPoly<String, String>, XPoly<String, String>> merged = merge(mat, isa, schemaName, isaSchemaName, "merged");
		
		String ret = "";
		
		ret += "merged = " + merged.first + "\n\n";
		ret += "inc1_q = " + merged.third + "\n\n";
		ret += "inc2_q = " + merged.second + "\n\n";
		ret += "inst_inc = apply inc1_q " + isa_inst + "\n\n";
		ret += "isa_inc = apply inc2_q " + inst + "\n\n";
		ret += "merged_inst = (isa_inc + inst_inc)\n\n";		
		ret += "enrich = " + enrich(mat0, mat, "merged", schemaName, node, att, a, kid, r, n) + "\n\n";
		ret += "enriched = apply enrich merged_inst\n\n";
		
		return ret;
	}



	private static XPoly<String, String> enrich(XCtx<String> S0, XSchema S, String s, String T, String mat, String name, 
			 String a, /*String b,*/ String l, String r, String n) {
		Map<Object, Pair<String, Block<String, String>>> blocks = new HashMap<>();
		//Map<String, Map<String, Triple<String, String, List<String>>>> vars1 = new HashMap<>();
		Map<String, Map<Triple<String, String, List<String>>, String>> vars2 = new HashMap<>();
		int i = 0;
		for (String X : S.nodes) {
			//Map<String, Triple<String, String, List<String>>> m1 = new HashMap<>();
			Map<Triple<String, String, List<String>>, String> m2 = new HashMap<>();
		//	vars1.put(X, m1);
			vars2.put(X, m2);
			for (Triple<String, String, List<String>> rp : S0.cat().hom(X, mat)) {
				//m1.put("v" + i, rp);
				m2.put(rp, "v" + i);
				i++;
			}
		}
		
		for (String X : S.nodes) {
			Map<Object, String> from = new HashMap<>();
			from.put("x", X);
			Set<Pair<List<Object>, List<Object>>> where = new HashSet<>();
			Map<String, List<Object>> attrs = new HashMap<>();
			Map<String, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
			
		//	Map<String, Triple<String, String, List<String>>> m1 = vars1.get(X); //new HashMap<>();
			Map<Triple<String, String, List<String>>, String> m2 = vars2.get(X); //new HashMap<>();
			for (Triple<String, String, List<String>> p : S0.cat().hom(X, mat)) {
				String rp = m2.get(p);
				from.put(rp, a);
				List<Object> lhs = new LinkedList<>();
				List<Object> rhs = new LinkedList<>();
				lhs.add("x");
				lhs.addAll(p.third);
				lhs.add(name);
				rhs.add(rp);
				rhs.add(l);
				rhs.add(n);
				where.add(new Pair<>(lhs, rhs));
			}
			for (Triple<String, String, String> e : S.arrows) {
				Map<Object, List<Object>> map = new HashMap<>();
				if (!e.second.equals(X)) {
					continue;
				}
				if (S.nodes.contains(e.third)) {
					List<Object> xxx = new LinkedList<>();
					xxx.add("x");
					xxx.add(e.first);
					map.put("x", xxx);
					for (Triple<String, String, List<String>> p0 : S0.cat().hom(e.third, mat)) {
						Object rep0 = vars2.get(e.third).get(p0);
						List<String> list = new LinkedList<>();
						list.add(e.first);
						list.addAll(p0.third);
						Triple<String, String, List<String>> ep0 = S0.find_fast(new Triple<>(X, p0.second, list));
						if (ep0 == null) {
							throw new RuntimeException("Cannot find " + new Triple<>(X, e.third, list) + " in " + S0);
						}
						String tgt = m2.get(ep0);
						if (tgt == null) {
							throw new RuntimeException("Cannot find " + ep0 + " in " + m2.keySet());
						}
						List<Object> yyh = Collections.singletonList(tgt);
						map.put(rep0, yyh);
					}
					edges.put(e.first, new Pair<>("q" + e.third, map));
				 //edge	
				} else {
					List<Object> list = new LinkedList<>();
					if (e.first.equals(name) && X.equals(mat)) {
						Triple<String, String, List<String>> id = S0.find_fast(new Triple<>(mat, mat, new LinkedList<>()));
						list.add(m2.get(id));
						list.add(r);
						list.add(n);
					} else {
						list.add("x");
						list.add(e.first);
					}
					attrs.put(e.first, list);
				 //att	
				}
			}
			Block<String, String> block = new Block<>(from, where, attrs, edges);
			blocks.put("q" + X, new Pair<>(X, block));
		}

		XPoly<String, String> poly = new XPoly<>(new Var(s), new Var(T), blocks);
		return poly;
	}

	private static String src(XSchema isa, String e) {
		for (Triple<String, String, String> k : isa.arrows) {
			if (k.first.equals(e)) {
				return k.second;
			}
		}
		throw new RuntimeException("Bad edge: " + e);
	}

	private static String onlyAtt(XSchema isa) {
		for (Triple<String, String, String> k : isa.arrows) {
			if (isa.nodes.contains(k.third)) {
				continue;
			}
			return k.first;
		}
		throw new RuntimeException("Bad att");
	}

	private static String otherEdge(XSchema isa, String e) {
		for (Triple<String, String, String> k : isa.arrows) {
			if (k.first.equals(e)) {
				continue;
			}
			if (isa.nodes.contains(k.third)) {
				return k.first;
			}
		}
		throw new RuntimeException();
	}

	private static String nodeForAtt(XSchema mat, String att) {
		for (Triple<String, String, String> k : mat.arrows) {
			if (k.first.equals(att)) {
				return k.second;
			}
		}
		throw new RuntimeException("Bad attribute: " + att + " not in " + mat);
	}
	private static Triple<XSchema, XPoly<String, String>, XPoly<String, String>> merge(XSchema isa, XSchema mat, String isa0, String mat0, String merged) {
		Set<String> nodes = new HashSet<>();
		Set<Triple<String, String, String>> arrows = new HashSet<>();
		Set<Pair<List<String>, List<String>>> eqs = new HashSet<>();
		
		nodes.addAll(isa.nodes);
		nodes.addAll(mat.nodes);
		arrows.addAll(isa.arrows);
		arrows.addAll(mat.arrows);
		eqs.addAll(isa.eqs);
		eqs.addAll(mat.eqs);
		
		XSchema ret1 = new XSchema(new LinkedList<>(nodes), new LinkedList<>(arrows), new LinkedList<>(eqs));
	
		return new Triple<>(ret1, idPoly(isa, isa0, merged), idPoly(mat, mat0, merged));
	}

	private static XPoly<String, String> idPoly(XSchema isa, String isa0, String merged) {
		Map<Object, Pair<String, Block<String, String>>> blocks = new HashMap<>();
		for (String node : isa.nodes) {
			Map<Object, String> from = new HashMap<>();
			from.put("v", node);
			Set<Pair<List<Object>, List<Object>>> where = new HashSet<>();
			Map<String, List<Object>> attrs = new HashMap<>();
			Map<String, Pair<Object, Map<Object, List<Object>>>> edges = new HashMap<>();
			for (Triple<String, String, String> arrow : isa.arrows) {
				if (!arrow.second.equals(node)) {
					continue;
				}
				if (isa.nodes.contains(arrow.third)) {
					Map<String, List<String>> map = new HashMap<>();
					List<String> l = new LinkedList<>();
					l.add("v");
					l.add(arrow.first);
					map.put("v", l);
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Pair<Object,Map<Object,List<Object>>> ppp = new Pair("q_" + arrow.third, map);
					edges.put(arrow.first, ppp);
				} else {
					List<Object> l = new LinkedList<>();
					l.add("v");
					l.add(arrow.first);
					attrs.put(arrow.first, l);
				}
			}
			
			Block<String, String> block = new Block<>(from, where, attrs, edges);
			blocks.put("q_" + node, new Pair<>(node, block));
		}
		return new XPoly<>(new Var(isa0), new Var(merged), blocks);
	}
	
	/* @SuppressWarnings({ "unchecked", "rawtypes" })
	public static final Signature<String, String> program(String s) {
		//Object o = program.parse(s);
		//return toProg(o);
		// return to(o);
		return null;
	} */

	static class PeopleExample extends Example2 {

		@Override
		public Language lang() {
			return null;
		}

		@Override
		public String att() {
			return "schoolName";
		}

		@Override
		public String left() {
			return "left";
		}

		@Override
		public String isa_inst() {
			return "isa_inst";
		}

		@Override
		public String toenrich_inst() {
			return "I";
		}

		@Override
		public String getName() {
			return "People";
		}

		@Override
		public String getText() {
			return s;
		}
		
		final String s = "adom : type"
				+ "\n"
				+ "\nWisnesky Spivak Chlipala Morrisett Malecha Gross Harvard MIT Math CS Stanford : adom"
				+ "\n"
				+ "\nS = schema {"
				+ "\n	nodes Person, School, Dept;"
				+ "\n	edges advisor : Person -> Person, "
				+ "\n	      instituteOf : Person -> School, "
				+ "\n	      deptOf : Person -> Dept,"
				+ "\n	      biggestDept : School -> Dept,"
				+ "\n	      lastName : Person -> adom,"
				+ "\n	      schoolName : School -> adom,"
				+ "\n	      deptName : Dept -> adom;"
				+ "\n	equations advisor . advisor = advisor;"
				+ "\n}"
				+ "\n"
			//	+ "\n"
				+ "\nisa_schema = schema {"
				+ "\n	nodes nodeA, nodeB;"
				+ "\n	edges left : nodeA -> nodeB, right : nodeA -> nodeB, theatt : nodeB -> adom;"
				+ "\n	equations;"
				+ "\n}"
				+ "\n"
				+ "\nisa_inst = instance {"
				+ "\n	variables a0 a1 : nodeA, b0 b1 b2 : nodeB;"
				+ "\n	equations b0.theatt = MIT, b1.theatt = Harvard, b2.theatt = Stanford,"
				+ "\n	          a0.left = b0, a0.right = b0,"
				+ "\n	          a1.left = b1, a1.right = b2"
				+ "\n	;"
				+ "\n} : isa_schema"
				+ "\n"				+ "\nI = instance {"
				+ "\n	variables ryan david adam greg gregory jason : Person,"
				+ "\n	          harvard mit : School, math cs : Dept;"
				+ "\n	equations ryan.lastName = Wisnesky, ryan.advisor = david, ryan.instituteOf = harvard, ryan.deptOf = math,"
				+ "\n	          gregory.lastName = Malecha, gregory.advisor = greg,  gregory.instituteOf = harvard, gregory.deptOf = cs,"
				+ "\n	          jason.lastName = Gross, jason.advisor = adam, jason.instituteOf = mit, jason.deptOf = math,"
				+ "\n	          adam.lastName = Chlipala, adam.instituteOf = mit, adam.deptOf = cs,"
				+ "\n	          greg.lastName = Morrisett, greg.instituteOf = harvard, greg.deptOf = cs,"
				+ "\n	          david.lastName = Spivak, david.instituteOf = mit, david.deptOf = math,"
				+ "\n	          mit.schoolName = MIT, harvard.schoolName = Harvard,"
				+ "\n	          math.deptName = Math, cs.deptName = CS,"
				+ "\n	          harvard.biggestDept = math, mit.biggestDept = cs;"
				+ "\n} : S " ;



		
	}
	
}

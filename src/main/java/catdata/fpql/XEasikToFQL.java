package catdata.fpql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import catdata.Pair;
import catdata.Triple;
import catdata.fpql.XExp.XSchema;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;

public class XEasikToFQL {

	private final Example[] examples = { new ContraintsEx(), new DemoEx() };

	static class DemoEx extends Example {
			
		@Override
		public String getName() {
			return "Demo";
		}
		
		@Override
		public String getText() {
			return demo_example;
		}
	}
	
	static class ContraintsEx extends Example {
	
		@Override
		public String getName() {
			return "Constraints";
		}

		@Override
		public String getText() {
			return constraints_example;
		}

	}

	private final String help = "Translates EASIK XML into FPQL.  Ignore constraints besides path equalities.";

	private static String kind() {
		return "EASIK";
	}

	private static String translate1(Node sketch) {
		List<String> ns = new LinkedList<>();
		List<Triple<String, String, String>> es = new LinkedList<>();
		List<Pair<List<String>, List<String>>> eqs = new LinkedList<>();
		
		NodeList l = sketch.getChildNodes();
        for (int temp = 0; temp < l.getLength(); temp++) {
        	Node n = l.item(temp);
        	NodeList j = n.getChildNodes();
    		for (int temp2 = 0; temp2 < j.getLength(); temp2++) {
    			Node m = j.item(temp2);

    			if (m.getNodeName().equals("entity")) {
    				String nodeName = m.getAttributes().getNamedItem("name").getTextContent();
    				ns.add(nodeName);
    				NodeList k = m.getChildNodes();
    				for (int temp3 = 0; temp3 < k.getLength(); temp3++) {
    					Node w = k.item(temp3);
    					if (w.getNodeName().equals("attribute")) {
    	    				String attName = w.getAttributes().getNamedItem("name").getTextContent();
    						es.add(new Triple<>(nodeName + "_" + attName.replace(" ", "_") , nodeName, "dom"));
    					}
    				}
    			} else if (m.getNodeName().equals("edge")) {
    				es.add(new Triple<>(m.getAttributes().getNamedItem("id").getTextContent(),
							m.getAttributes().getNamedItem("source").getTextContent(), m.getAttributes().getNamedItem("target").getTextContent()));
    			} else if (m.getNodeName().equals("commutativediagram")) {
       				NodeList k = m.getChildNodes();
       				Node w1 = null;
       				Node w2 = null;
       				for (int temp4 = 0; temp4 < k.getLength(); temp4++) {
       					Node wX = k.item(temp4);
       					if (wX.getNodeName().equals("path") && w1 == null) {
       						w1 = wX;
       					} else if (wX.getNodeName().equals("path") && w2 == null) {
       						w2 = wX;
       					}
       				}
       				if (w1 == null || w2 == null) {
       					throw new RuntimeException("Easik to FQL internal error");
       				}
       				String cod1 = w1.getAttributes().getNamedItem("domain").getTextContent();
       				String cod2 = w2.getAttributes().getNamedItem("domain").getTextContent();
       				List<String> lhs = new LinkedList<>();
       				List<String> rhs = new LinkedList<>();
       				lhs.add(cod1);
       				rhs.add(cod2);
       				
       				NodeList lhsX = w1.getChildNodes();
       				for (int temp3 = 0; temp3 < lhsX.getLength(); temp3++) {
       					if (!lhsX.item(temp3).getNodeName().equals("edgeref")) {
       						continue;
       					}
       					String toAdd = lhsX.item(temp3).getAttributes().getNamedItem("id").getTextContent();
       					lhs.add(toAdd);
    				}
       				NodeList rhsX = w2.getChildNodes();
       				for (int temp3 = 0; temp3 < rhsX.getLength(); temp3++) {
       					if (!rhsX.item(temp3).getNodeName().equals("edgeref")) {
       						continue;
       					}
       					String toAdd = rhsX.item(temp3).getAttributes().getNamedItem("id").getTextContent();
       					rhs.add(toAdd);
    				}
       				eqs.add(new Pair<>(lhs, rhs));
    			}
    		}
        }
        
		XSchema sch = new XSchema(ns, es, eqs);
		return sketch.getAttributes().getNamedItem("name").getTextContent().replace(" ", "_") + " = " + sch;
	}
	
	private static String translate(String in) {
		String ret = "dom : type\n\n";
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputStream stream = new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8));
			Document doc = dBuilder.parse(stream);
			doc.getDocumentElement().normalize();
			NodeList sketchesNodes = doc.getElementsByTagName("sketches");
			if (sketchesNodes.getLength() != 1) {
				throw new RuntimeException("multiple sketches tags");
			}
			Node sketchesNode = sketchesNodes.item(0);
			NodeList nList = sketchesNode.getChildNodes();
	        for (int temp = 0; temp < nList.getLength(); temp++) {
	        	Node nNode = nList.item(temp);
	        	if (!nNode.getNodeName().equals("easketch")) {
	        		continue;
	        	}
	        	ret += translate1(nNode) + "\n\n";
	        }	        
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public XEasikToFQL() {
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

		helpButton.addActionListener(new ActionListenerImpl());

		JPanel p = new JPanel(new BorderLayout());

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setDividerSize(4);
		jsp.setResizeWeight(0.5d);
		jsp.add(input);
		jsp.add(output);

		JPanel tp = new JPanel(new GridLayout(1, 4));

		tp.add(transButton);
		tp.add(helpButton);
		tp.add(new JLabel("Load Example", SwingConstants.RIGHT));
		tp.add(box);

		p.add(jsp, BorderLayout.CENTER);
		p.add(tp, BorderLayout.NORTH);
		JFrame f = new JFrame(kind() + " to FPQL");
		f.setContentPane(p);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	private static final String constraints_example = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
			+ "\n<easketch_overview>"
			+ "\n<header>"
			+ "\n<title>Constraints</title>"
			+ "\n<author>Andrew Wood</author>"
			+ "\n<description>Example of each constraint type</description>"
			+ "\n<creationDate>2009-05-27T09:58:50</creationDate>"
			+ "\n<lastModificationDate>2009-05-28T12:17:21</lastModificationDate>"
			+ "\n</header>"
			+ "\n<sketches>"
			+ "\n<easketch cascade=\"cascade\" name=\"Equalizer Constraint\" partial-cascade=\"set_null\" x=\"243\" y=\"30\">"
			+ "\n<header>"
			+ "\n<title>Equalizer Constraint</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2009-05-27T10:00:18</creationDate>"
			+ "\n<lastModificationDate>2009-05-28T12:07:36</lastModificationDate>"
			+ "\n</header>"
			+ "\n<entities>"
			+ "\n<entity name=\"Equalizer\" x=\"207\" y=\"248\"/>"
			+ "\n<entity name=\"A\" x=\"96\" y=\"249\"/>"
			+ "\n<entity name=\"B\" x=\"8\" y=\"197\"/>"
			+ "\n<entity name=\"C\" x=\"9\" y=\"98\"/>"
			+ "\n<entity name=\"Codomain\" x=\"77\" y=\"19\"/>"
			+ "\n<entity name=\"D\" x=\"175\" y=\"143\"/>"
			+ "\n</entities>"
			+ "\n<edges>"
			+ "\n<edge cascade=\"cascade\" id=\"isA_1\" source=\"Equalizer\" target=\"A\" type=\"injective\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f1\" source=\"A\" target=\"D\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f2\" source=\"D\" target=\"Codomain\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f3\" source=\"A\" target=\"B\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f4\" source=\"B\" target=\"C\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f5\" source=\"C\" target=\"Codomain\" type=\"normal\"/>"
			+ "\n</edges>"
			+ "\n<keys/>"
			+ "\n<constraints>"
			+ "\n<equalizerconstraint isVisible=\"true\" x=\"91\" y=\"145\">"
			+ "\n<path codomain=\"A\" domain=\"Equalizer\">"
			+ "\n<edgeref id=\"isA_1\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Codomain\" domain=\"A\">"
			+ "\n<edgeref id=\"f3\"/>"
			+ "\n<edgeref id=\"f4\"/>"
			+ "\n<edgeref id=\"f5\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Codomain\" domain=\"A\">"
			+ "\n<edgeref id=\"f1\"/>"
			+ "\n<edgeref id=\"f2\"/>"
			+ "\n</path>"
			+ "\n</equalizerconstraint>"
			+ "\n</constraints>"
			+ "\n</easketch>"
			+ "\n<easketch cascade=\"cascade\" name=\"Product Constraint\" partial-cascade=\"set_null\" x=\"192\" y=\"162\">"
			+ "\n<header>"
			+ "\n<title>Product Constraint</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2009-05-27T10:05:13</creationDate>"
			+ "\n<lastModificationDate>2009-05-28T12:10:08</lastModificationDate>"
			+ "\n</header>"
			+ "\n<entities>"
			+ "\n<entity name=\"Product\" x=\"137\" y=\"196\"/>"
			+ "\n<entity name=\"P1\" x=\"85\" y=\"120\"/>"
			+ "\n<entity name=\"P2\" x=\"210\" y=\"118\"/>"
			+ "\n</entities>"
			+ "\n<edges>"
			+ "\n<edge cascade=\"cascade\" id=\"f1\" source=\"Product\" target=\"P1\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f2\" source=\"Product\" target=\"P2\" type=\"normal\"/>"
			+ "\n</edges>"
			+ "\n<keys/>"
			+ "\n<constraints>"
			+ "\n<productconstraint isVisible=\"true\" x=\"153\" y=\"139\">"
			+ "\n<path codomain=\"P1\" domain=\"Product\">"
			+ "\n<edgeref id=\"f1\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"P2\" domain=\"Product\">"
			+ "\n<edgeref id=\"f2\"/>"
			+ "\n</path>"
			+ "\n</productconstraint>"
			+ "\n</constraints>"
			+ "\n</easketch>"
			+ "\n<easketch cascade=\"cascade\" name=\"Commutative Diagram\" partial-cascade=\"set_null\" x=\"44\" y=\"38\">"
			+ "\n<header>"
			+ "\n<title>Commutative Diagram</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2009-05-27T09:59:02</creationDate>"
			+ "\n<lastModificationDate>2009-05-28T12:16:08</lastModificationDate>"
			+ "\n</header>"
			+ "\n<entities>"
			+ "\n<entity name=\"Domain\" x=\"167\" y=\"203\"/>"
			+ "\n<entity name=\"Codomain\" x=\"165\" y=\"8\"/>"
			+ "\n<entity name=\"A\" x=\"82\" y=\"110\"/>"
			+ "\n<entity name=\"B\" x=\"279\" y=\"117\"/>"
			+ "\n</entities>"
			+ "\n<edges>"
			+ "\n<edge cascade=\"cascade\" id=\"f1\" source=\"Domain\" target=\"A\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f2\" source=\"A\" target=\"Codomain\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f3\" source=\"Domain\" target=\"B\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f4\" source=\"B\" target=\"Codomain\" type=\"normal\"/>"
			+ "\n</edges>"
			+ "\n<keys/>"
			+ "\n<constraints>"
			+ "\n<commutativediagram isVisible=\"true\" x=\"179\" y=\"113\">"
			+ "\n<path codomain=\"Codomain\" domain=\"Domain\">"
			+ "\n<edgeref id=\"f1\"/>"
			+ "\n<edgeref id=\"f2\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Codomain\" domain=\"Domain\">"
			+ "\n<edgeref id=\"f3\"/>"
			+ "\n<edgeref id=\"f4\"/>"
			+ "\n</path>"
			+ "\n</commutativediagram>"
			+ "\n</constraints>"
			+ "\n</easketch>"
			+ "\n<easketch cascade=\"cascade\" name=\"Sum Constraint\" partial-cascade=\"set_null\" x=\"367\" y=\"161\">"
			+ "\n<header>"
			+ "\n<title>Sum Constraint</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2009-05-27T09:59:13</creationDate>"
			+ "\n<lastModificationDate>2009-05-28T12:16:37</lastModificationDate>"
			+ "\n</header>"
			+ "\n<entities>"
			+ "\n<entity name=\"Sum\" x=\"149\" y=\"184\"/>"
			+ "\n<entity name=\"Summand3\" x=\"220\" y=\"34\"/>"
			+ "\n<entity name=\"Summand2\" x=\"128\" y=\"34\"/>"
			+ "\n<entity name=\"Summand1\" x=\"32\" y=\"34\"/>"
			+ "\n</entities>"
			+ "\n<edges>"
			+ "\n<edge cascade=\"cascade\" id=\"isA_1\" source=\"Summand1\" target=\"Sum\" type=\"injective\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"isA_2\" source=\"Summand2\" target=\"Sum\" type=\"injective\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"isA_3\" source=\"Summand3\" target=\"Sum\" type=\"injective\"/>"
			+ "\n</edges>"
			+ "\n<keys/>"
			+ "\n<constraints>"
			+ "\n<sumconstraint isVisible=\"true\" x=\"55\" y=\"102\">"
			+ "\n<path codomain=\"Sum\" domain=\"Summand1\">"
			+ "\n<edgeref id=\"isA_1\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Sum\" domain=\"Summand2\">"
			+ "\n<edgeref id=\"isA_2\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Sum\" domain=\"Summand3\">"
			+ "\n<edgeref id=\"isA_3\"/>"
			+ "\n</path>"
			+ "\n</sumconstraint>"
			+ "\n</constraints>"
			+ "\n</easketch>"
			+ "\n<easketch cascade=\"cascade\" name=\"Pullback Constraint\" partial-cascade=\"set_null\" x=\"15\" y=\"158\">"
			+ "\n<header>"
			+ "\n<title>Pullback Constraint</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2009-05-27T09:59:06</creationDate>"
			+ "\n<lastModificationDate>2009-05-28T12:12:55</lastModificationDate>"
			+ "\n</header>"
			+ "\n<entities>"
			+ "\n<entity name=\"Pullback\" x=\"147\" y=\"203\"/>"
			+ "\n<entity name=\"A\" x=\"162\" y=\"20\"/>"
			+ "\n<entity name=\"B\" x=\"69\" y=\"112\"/>"
			+ "\n<entity name=\"C\" x=\"255\" y=\"112\"/>"
			+ "\n</entities>"
			+ "\n<edges>"
			+ "\n<edge cascade=\"cascade\" id=\"f1\" source=\"B\" target=\"A\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f2\" source=\"Pullback\" target=\"C\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"isA_1\" source=\"C\" target=\"A\" type=\"injective\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"isA_2\" source=\"Pullback\" target=\"B\" type=\"injective\"/>"
			+ "\n</edges>"
			+ "\n<keys/>"
			+ "\n<constraints>"
			+ "\n<pullbackconstraint isVisible=\"true\" x=\"161\" y=\"113\">"
			+ "\n<path codomain=\"B\" domain=\"Pullback\">"
			+ "\n<edgeref id=\"isA_2\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"A\" domain=\"B\">"
			+ "\n<edgeref id=\"f1\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"C\" domain=\"Pullback\">"
			+ "\n<edgeref id=\"f2\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"A\" domain=\"C\">"
			+ "\n<edgeref id=\"isA_1\"/>"
			+ "\n</path>"
			+ "\n</pullbackconstraint>"
			+ "\n</constraints>"
			+ "\n</easketch>"
			+ "\n</sketches>"
			+ "\n<views/>"
			+ "\n</easketch_overview>" + "\n";
	
	private static final String demo_example = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
			+ "\n<easketch_overview>"
			+ "\n<header>"
			+ "\n<title/>"
			+ "\n<description/>"
			+ "\n<creationDate>2014-09-25T22:17:15</creationDate>"
			+ "\n<lastModificationDate>2014-09-26T14:09:04</lastModificationDate>"
			+ "\n</header>"
			+ "\n<sketches>"
			+ "\n<easketch cascade=\"cascade\" name=\"Hotel\" partial-cascade=\"set_null\" x=\"13\" y=\"9\">"
			+ "\n<header>"
			+ "\n<title>Hotel</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2014-09-25T22:17:21</creationDate>"
			+ "\n<lastModificationDate>2014-09-25T22:20:06</lastModificationDate>"
			+ "\n<connectionParam name=\"pkFormat\" value=\"id\"/>"
			+ "\n<connectionParam name=\"hostname\" value=\"localhost\"/>"
			+ "\n<connectionParam name=\"database\" value=\"Hotel\"/>"
			+ "\n<connectionParam name=\"port\" value=\"3306\"/>"
			+ "\n<connectionParam name=\"fkFormat\" value=\"&lt;edge&gt;\"/>"
			+ "\n<connectionParam name=\"type\" value=\"MySQL\"/>"
			+ "\n<connectionParam name=\"quoteIdentifiers\" value=\"false\"/>"
			+ "\n<connectionParam name=\"username\" value=\"root\"/>"
			+ "\n</header>"
			+ "\n<entities>"
			+ "\n<entity name=\"Guest\" x=\"343\" y=\"217\">"
			+ "\n<attribute attributeTypeClass=\"easik.database.types.Varchar\" name=\"Name\" size=\"255\"/>"
			+ "\n</entity>"
			+ "\n<entity name=\"Room\" x=\"424\" y=\"359\">"
			+ "\n<attribute attributeTypeClass=\"easik.database.types.Varchar\" name=\"Window View\" size=\"255\"/>"
			+ "\n</entity>"
			+ "\n<entity name=\"Cot\" x=\"568\" y=\"222\"/>"
			+ "\n<entity name=\"GuestWithCot\" x=\"426\" y=\"121\"/>"
			+ "\n</entities>"
			+ "\n<edges>"
			+ "\n<edge cascade=\"cascade\" id=\"f1\" source=\"GuestWithCot\" target=\"Guest\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f2\" source=\"Guest\" target=\"Room\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f3\" source=\"Cot\" target=\"Room\" type=\"normal\"/>"
			+ "\n<edge cascade=\"cascade\" id=\"f4\" source=\"GuestWithCot\" target=\"Cot\" type=\"normal\"/>"
			+ "\n</edges>"
			+ "\n<keys/>"
			+ "\n<constraints>"
			+ "\n<pullbackconstraint isVisible=\"true\" x=\"465\" y=\"237\">"
			+ "\n<path codomain=\"Guest\" domain=\"GuestWithCot\">"
			+ "\n<edgeref id=\"f1\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Room\" domain=\"Guest\">"
			+ "\n<edgeref id=\"f2\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Cot\" domain=\"GuestWithCot\">"
			+ "\n<edgeref id=\"f4\"/>"
			+ "\n</path>"
			+ "\n<path codomain=\"Room\" domain=\"Cot\">"
			+ "\n<edgeref id=\"f3\"/>"
			+ "\n</path>"
			+ "\n</pullbackconstraint>"
			+ "\n</constraints>"
			+ "\n</easketch>"
			+ "\n<easketch cascade=\"cascade\" name=\"Simple\" partial-cascade=\"set_null\" x=\"23\" y=\"160\">"
			+ "\n<header>"
			+ "\n<title>Simple</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2014-09-26T13:43:32</creationDate>"
			+ "\n<lastModificationDate>2014-09-26T13:43:52</lastModificationDate>"
			+ "\n</header>"
			+ "\n<entities>"
			+ "\n<entity name=\"A\" x=\"66\" y=\"74\"/>"
			+ "\n<entity name=\"B\" x=\"222\" y=\"77\"/>"
			+ "\n</entities>"
			+ "\n<edges>"
			+ "\n<edge cascade=\"cascade\" id=\"f1\" source=\"A\" target=\"B\" type=\"normal\"/>"
			+ "\n</edges>"
			+ "\n<keys/>"
			+ "\n<constraints/>"
			+ "\n</easketch>"
			+ "\n</sketches>"
			+ "\n<views>"
			+ "\n<view name=\"V0\" on_sketch=\"Simple\" viewDefinitionEdge=\"ve_1\" x=\"185\" y=\"162\">"
			+ "\n<header>"
			+ "\n<title>V0</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2014-09-26T13:44:14</creationDate>"
			+ "\n<lastModificationDate>2014-09-26T13:45:04</lastModificationDate>"
			+ "\n</header>"
			+ "\n<queryNodes>"
			+ "\n<queryNode name=\"V_B\" query=\"Select * From B \" x=\"199\" y=\"41\"/>"
			+ "\n<queryNode name=\"V_A\" query=\"Select * From A \" x=\"50\" y=\"40\"/>"
			+ "\n</queryNodes>"
			+ "\n<ViewEdges>"
			+ "\n<ViewEdge cascade=\"restrict\" id=\"f1\" source=\"V_A\" target=\"V_B\" type=\"normal\"/>"
			+ "\n</ViewEdges>"
			+ "\n</view>"
			+ "\n<view name=\"RoomCotView\" on_sketch=\"Hotel\" viewDefinitionEdge=\"ve_0\" x=\"176\" y=\"40\">"
			+ "\n<header>"
			+ "\n<title>RoomCotView</title>"
			+ "\n<description/>"
			+ "\n<creationDate>2014-09-26T13:34:49</creationDate>"
			+ "\n<lastModificationDate>2014-09-26T13:35:36</lastModificationDate>"
			+ "\n</header>"
			+ "\n<queryNodes>"
			+ "\n<queryNode name=\"V_GuestWithCot\" query=\"Select * From GuestWithCot \" x=\"67\" y=\"59\"/>"
			+ "\n</queryNodes>"
			+ "\n<ViewEdges/>"
			+ "\n</view>"
			+ "\n</views>"
			+ "\n</easketch_overview>"
			+ "\n";

    private class ActionListenerImpl implements ActionListener {

        public ActionListenerImpl() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea jta = new JTextArea(help);
            jta.setWrapStyleWord(true);
            // jta.setEditable(false);
            jta.setLineWrap(true);
            JScrollPane p = new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            p.setPreferredSize(new Dimension(300, 200));
            
            JOptionPane pane = new JOptionPane(p);
            // Configure via set methods
            JDialog dialog = pane.createDialog(null, "Help on EASIK to FPQL");
            dialog.setModal(false);
            dialog.setVisible(true);
            dialog.setResizable(true);
        }
    }



}

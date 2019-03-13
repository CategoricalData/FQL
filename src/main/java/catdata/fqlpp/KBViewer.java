package catdata.fqlpp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
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

import catdata.Pair;
import catdata.fqlpp.CatExp.Const;
import catdata.fqlpp.cat.Signature;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;

public class KBViewer {

	private final Example[] examples = { new Cat() };

	private final String help = ""; 
	protected static String kind() {
		return "Knuth Bendix";
	}

	private static final class Cat extends Example {
		
		@Override
		public String getName() {
			return "Category";
		}

		@Override
		public String getText() {
			return catstr;
		}
	} 


	
	public KBViewer() {
		CodeTextPanel input = new CodeTextPanel(BorderFactory.createEtchedBorder(),
				"Input Category", "");
		CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(),
				"Output Re-writes", "");

		JButton transButton = new JButton("Complete");
		JButton helpButton = new JButton("Help");
	
		JComboBox<Example> box = new JComboBox<>(examples);
		box.setSelectedIndex(-1);
		box.addActionListener((ActionEvent e) -> input.setText(((Example) box.getSelectedItem()).getText()));

		transButton.addActionListener((ActionEvent e) -> {
                    try {
                        String p = translate(input.getText());
                        output.setText(p);
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
		JFrame f = new JFrame("Knuth-Bendix Completion");
		f.setContentPane(p);
		f.pack();
		f.setSize(new Dimension(700, 600));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
	
	private static String translate(String s) {
		Object o = PPParser.catConst().from(PPParser.TOKENIZER, PPParser.IGNORED).parse(s);
		Const c = PPParser.toCatConst(o);
		Signature<String, String> sig = new Signature<>(c.nodes, c.arrows, c.eqs);
		
		List<Pair<List<String>, List<String>>> rules = new LinkedList<>();
		for (Signature<String,String>.Eq eq : sig.eqs) {
			rules.add(new Pair<>(eq.lhs.path.stream().map(x -> x.name).collect(Collectors.toList()), 
					             eq.rhs.path.stream().map(x -> x.name).collect(Collectors.toList())));
		}
		sig.edges.stream().map(x -> x.name).collect(Collectors.toList());
		ThueSlow<String> kb = new ThueSlow<>(rules);
		kb.complete();
		return kb.toString();
	}
	

	private static final String catstr = "{"
+ "\n	objects "
+ "\n		Ob, "
+ "\n		Hom, "
+ "\n		Comp;"
+ "\n	arrows"
+ "\n		Dom:Hom->Ob,"
+ "\n		Cod:Hom->Ob,"
+ "\n		Id:Ob->Hom,"
+ "\n		LeftId:Hom->Comp, //f:a-->b  mapped to id_a ; f"
+ "\n		RightId:Hom->Comp, //f:a-->b  mapped to f ; id_b"
+ "\n		First:Comp->Hom,"
+ "\n		Second:Comp->Hom,"
+ "\n		Compose:Comp->Hom;"
+ "\n	equations"
+ "\n		Ob.Id.Dom=Ob,"
+ "\n		Ob.Id.Cod=Ob,"
+ "\n		Hom.LeftId.Second=Hom,"
+ "\n		Hom.RightId.First=Hom,"
+ "\n		Hom.LeftId.Compose=Hom,"
+ "\n		Hom.RightId.Compose=Hom,"
+ "\n		Hom.LeftId.First=Hom.Dom.Id,"
+ "\n		Hom.RightId.Second=Hom.Cod.Id,"
+ "\n		Comp.First.Cod=Comp.Second.Dom,"
+ "\n		Comp.First.Dom=Comp.Compose.Dom,"
+ "\n		Comp.Second.Cod=Comp.Compose.Cod;"
+ "\n}"
+ "\n";

}

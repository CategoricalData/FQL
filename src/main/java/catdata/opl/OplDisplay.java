package catdata.opl;

import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import catdata.Environment;
import catdata.Pair;
import catdata.Program;
import catdata.ide.CodeTextPanel;
import catdata.ide.Disp;
import catdata.ide.DefunctGlobalOptions;
import catdata.ide.ProgressMonitorWrapper;
import catdata.opl.OplExp.OplGraph;
import catdata.opl.OplExp.OplInst;
import catdata.opl.OplExp.OplJavaInst;
import catdata.opl.OplExp.OplMapping;
import catdata.opl.OplExp.OplPivot;
import catdata.opl.OplExp.OplPragma;
import catdata.opl.OplExp.OplPres;
import catdata.opl.OplExp.OplPresTrans;
import catdata.opl.OplExp.OplPushout;
import catdata.opl.OplExp.OplSchema;
import catdata.opl.OplExp.OplSetInst;
import catdata.opl.OplExp.OplSetTrans;
import catdata.opl.OplExp.OplSig;
import catdata.opl.OplExp.OplTyMapping;


public class OplDisplay implements Disp {

	
	@Override
	public void close() {
	}
	
	@SuppressWarnings("rawtypes")
	private static String doLookup(String c, OplObject o) {
		if (o instanceof OplSig) {
			return "theory " + c;
		}
		if (o instanceof OplSetInst) {
			return "model " + c + " : " + ((OplSetInst)o).sig;
		}
		if (o instanceof OplJavaInst) {
			return "javascript " + c + " : " + ((OplJavaInst)o).sig;
		}
		if (o instanceof OplSetTrans) {
			OplSetTrans x = (OplSetTrans) o;
			return "transform " + c + " : " + x.src + " -> " + x.dst;
		}
		if (o instanceof OplMapping) {
			OplMapping x = (OplMapping) o;
			return "mapping " + c + " : " + x.src0 + " -> " + x.dst0;
 		}
		if (o instanceof OplTyMapping) {
			OplTyMapping x = (OplTyMapping) o;
			return "mapping " + c + " : " + x.src0 + " -> " + x.dst0;
		}
		if (o instanceof OplPres) {
			return "presentation " + c + " : " + ((OplPres)o).S;
 		}
		if (o instanceof OplSchema) {
			return "schema " + c + " : " + ((OplSchema)o).sig0;
 		}
		if (o instanceof OplQuery) {
			OplQuery q = (OplQuery) o;
			return "query " + c + " : " + q.src_e + " -> " + q.dst_e;
 		}
		if (o instanceof OplPresTrans) {
			OplPresTrans x = (OplPresTrans) o;
			return "transpres " + c + " : " + x.src0 + " -> " + x.dst0;
		}
		if (o instanceof OplInst) {
			OplInst oo = (OplInst) o;
			return "instance " + c + /* "=" + oo.P0 + */ " : " + oo.S0 /* + "," + oo.J0 */ ;
 		}
		if (o instanceof OplPushout) {
			OplPushout oo = (OplPushout) o;
			return "pushout " + c + " of " + oo.s1 + " , " + oo.s2;
 		}
		if (o instanceof OplPivot) {
			OplPivot oo = (OplPivot) o;
			return "pivot " + c + " of " + oo.I0;
 		}
		if (o instanceof OplGraph) {
			//OplGraph oo = (OplGraph) o;
			return "graph " + c;
			
		}
		return c;
	}
	
	@SuppressWarnings("unused")
	private static JComponent wrapDisplay(String name, OplObject obj) {
		if (!DefunctGlobalOptions.debug.opl.opl_lazy_gui) {
			return obj.display();
		}
		JPanel ret = new JPanel(new GridLayout(1,1));
		JPanel lazyPanel = new JPanel();
		JButton button = new JButton("Show");

		lazyPanel.add(button); 
		button.addActionListener(z -> {
			JComponent[] comp = new JComponent[1];
			new ProgressMonitorWrapper( "Making GUI for " + name, () -> {
				comp[0] = obj.display();
				ret.remove(lazyPanel);
				ret.add(comp[0]);
				ret.validate();
			});
		});
		ret.add(lazyPanel);
		return ret;
	}


	public OplDisplay(String title, Program<OplExp> p, Environment<OplObject> env, long start, long middle) {
		//Map<Object, String> map = new HashMap<>();
		for (String c : p.order) {
			OplObject obj = env.get(c);
			if (obj instanceof OplPragma) {
				continue;
			}
		//	map.put(obj, c);
			try {
				frames.add(new Pair<>(doLookup(c, obj).replace(": ?", ""), wrapDisplay(c, obj)));
			} catch (Exception ex) {
				ex.printStackTrace();
				frames.add(new Pair<>(doLookup(c, obj), new CodeTextPanel(BorderFactory.createEtchedBorder(), "Exception", ex.getMessage())));
			}
		}
		long end = System.currentTimeMillis();
		int c1 = (int) ((middle - start) / (1000f));
		int c2 = (int) ((end - middle) / (1000f));
		display(title + " | (exec: " + c1 + "s)(gui: " + c2 + "s)", p.order);
	}
	
	private JFrame frame = null;
	//private String name;
	private final List<Pair<String, JComponent>> frames = new LinkedList<>();

	private final CardLayout cl = new CardLayout();
	private final JPanel x = new JPanel(cl);
	private final JList<String> yyy = new JList<>();
	//private final Map<String, String> indices = new HashMap<>();

	private void display(String s, @SuppressWarnings("unused") List<String> order) {
		frame = new JFrame();
	//	this.name = s;

		Vector<String> ooo = new Vector<>();
		//int index = 0;
		for (Pair<String, JComponent> p : frames) {
			x.add(p.second, p.first);
			ooo.add(p.first);
		//	indices.put(order.get(index++), p.first);
		}
		x.add(new JPanel(), "blank");
		cl.show(x, "blank");

		yyy.setListData(ooo);
		JPanel temp1 = new JPanel(new GridLayout(1, 1));
		temp1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
				"Select:"));
		JScrollPane yyy1 = new JScrollPane(yyy);
		temp1.add(yyy1);
	//	temp1.setMinimumSize(new Dimension(200, 600));
	//	yyy.setPreferredSize(new Dimension(200, 600));
		yyy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		yyy.addListSelectionListener((ListSelectionEvent e) -> {
                    int i = yyy.getSelectedIndex();
                    if (i == -1) {
                        cl.show(x, "blank");
                    } else {
                        cl.show(x, ooo.get(i));
                    }
                });

		JPanel north = new JPanel(new GridLayout(1, 1));
	//	JButton saveButton = new JButton("Save GUI");
	//	north.add(saveButton);
	//	saveButton.setMinimumSize(new Dimension(10,10));
	//	saveButton.addActionListener(x -> GUI.save2(env));
		JSplitPane px = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		//px.setResizeWeight(.8);
		px.setDividerLocation(200);
//		FQLSplit px = new FQLSplit(.5, JSplitPane.HORIZONTAL_SPLIT);
		px.setDividerSize(4);
		frame = new JFrame(/* "Viewer for " + */s);

		JSplitPane temp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		temp2.setResizeWeight(1);
		temp2.setDividerSize(0);
		temp2.setBorder(BorderFactory.createEmptyBorder());
		temp2.add(temp1);
		temp2.add(north);

		// px.add(temp1);
		px.add(temp2);

		px.add(x);

		// JPanel bd = new JPanel(new BorderLayout());
		// bd.add(px, BorderLayout.CENTER);
		// bd.add(north, BorderLayout.NORTH);

		// frame.setContentPane(bd);
		frame.setContentPane(px);
		frame.setSize(900, 600);

		ActionListener escListener = (ActionEvent e) -> frame.dispose();

		frame.getRootPane().registerKeyboardAction(escListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
		KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
		frame.getRootPane().registerKeyboardAction(escListener, ctrlW,
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		frame.getRootPane().registerKeyboardAction(escListener, commandW,
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		

	}

	


}

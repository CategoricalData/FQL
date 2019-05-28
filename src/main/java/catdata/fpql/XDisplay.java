package catdata.fpql;

import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import catdata.Pair;
import catdata.ide.Disp;

public class XDisplay implements Disp {
	
	@SuppressWarnings({ "rawtypes" })
	private static String doLookup(String c, XObject obj, Map<Object, String> map, XProgram prog) {
		String front = obj.kind() + " " + c;
		if (obj instanceof XCtx) {
			XCtx ctx = (XCtx) obj;
			if (ctx.global == null) {
				throw new RuntimeException();
			} else if (ctx.schema == null) {
				return front;
			} else {
				String s = map.get(ctx.schema);
				if (s == null) {
					s = prog.getType(c).first.toString();
				}
				return front + " : " + s;
			}
		} else if (obj instanceof XMapping) {
			XMapping m = (XMapping) obj;
			String s = null;
			String t = null;
			if (map.containsKey(m.src)) {
				s = map.get(m.src);
			}
			if (map.containsKey(m.dst)) {
				t = map.get(m.dst);
			} 
			if (s == null) {
				s = prog.getType(c).first.toString();
			}
			if (t == null) {
				t = prog.getType(c).second.toString();
			}
			return front + " : " + s + " -> " + t;
		} else if (obj instanceof XPoly) {
			XPoly m = (XPoly) obj;
			String s = null;
			String t = null;
			if (map.containsKey(m.src)) {
				s = map.get(m.src);
			}
			if (map.containsKey(m.dst)) {
				t = map.get(m.dst);
			}
			if (s == null) {
				s = prog.getType(c).first.toString();
			}
			if (t == null) {
				t = prog.getType(c).second.toString();
			}
			return front + " : " + s + " -> " + t;
		}
		
		return front; 
		
	}

	public XDisplay(String title, XProgram p, XEnvironment env, long start, long middle) {
		Map<Object, String> map = new HashMap<>();
		for (String c : p.order) {
			XObject obj = env.objs.get(c);
			map.put(obj, c);
			if (obj instanceof XString) {
				continue;
			}
			if (obj instanceof XPoly) {
//				((XPoly)obj).validate();
				continue;
			}
			frames.add(new Pair<>(doLookup(c, obj, map, p), obj.display()));
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
      //  name = s;

		Vector<String> ooo = new Vector<>();
	//	int index = 0;
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

	@Override
	public void close() {
	}

			
}

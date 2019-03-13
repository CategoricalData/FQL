package catdata.fql.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Paint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

import com.google.common.base.Function;

import catdata.Pair;
import catdata.fql.FQLException;
import catdata.fql.cat.Arr;
import catdata.fql.cat.FinCat;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Node;
import catdata.fql.decl.Path;
import catdata.ide.DefunctGlobalOptions;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;

/**
 *
 * @author ryan
 *
 *         Displays the category of elements of an instance, the Grothendieck
 *         construction.
 */
public class CategoryOfElements {

	private static Pair<Graph<Pair<Node, Object>, Pair<Path, Integer>>, HashMap<Pair<Node, Object>, Map<Attribute<Node>, Object>>> build(
			Instance i) throws FQLException {
		FinCat<Node, Path> c = i.thesig.toCategory2().first;
		HashMap<Pair<Node, Object>, Map<Attribute<Node>, Object>> map = new HashMap<>();

		Graph<Pair<Node, Object>, Pair<Path, Integer>> g2 = new DirectedSparseMultigraph<>();
		for (Node n : c.objects) {
			for (Pair<Object, Object> o : i.data.get(n.string)) {
				Pair<Node, Object> xx = new Pair<>(n, o.first);
				g2.addVertex(xx);

				List<Attribute<Node>> attrs = i.thesig.attrsFor(n);
				Map<Attribute<Node>, Object> m = new HashMap<>();
				for (Attribute<Node> attr : attrs) {
					Object a = lookup(i.data.get(attr.name), o.first);
					m.put(attr, a);
				}
				map.put(xx, m);
			}
		}

		int j = 0;
		for (Pair<Node, Object> x : g2.getVertices()) {
			for (Pair<Node, Object> y : g2.getVertices()) {
				Set<Arr<Node, Path>> h = c.hom(x.first, y.first);
				for (Arr<Node, Path> arr : h) {
					if (c.isId(arr)) {
						continue;
					}
					if (!DefunctGlobalOptions.debug.fql.ALL_GR_PATHS && arr.arr.path.size() != 1) {
						continue;
					}
					if (doLookup(i, arr.arr, x.second, y.second)) {
						g2.addEdge(new Pair<>(arr.arr, j++), x, y);
					}
				}
			}
		}

		return new Pair<>(g2, map);
	}

	private static Object lookup(Set<Pair<Object, Object>> set, Object first) {
		for (Pair<Object, Object> p : set) {
			if (p.first.equals(first)) {
				return p.second;
			}
		}
		throw new RuntimeException();
	}

	private static boolean doLookup(Instance i, Path arr, Object x1, Object x2) {
		for (Pair<Object, Object> y : i.evaluate(arr)) {
			if (y.first.equals(x1) && y.second.equals(x2)) {
				return true;
			}
		}
		return false;
	}

	private static JPanel dot(String name, @SuppressWarnings("unused") Instance inst,
                              Graph<Pair<Node, Object>, Pair<Path, Integer>> sgv,
                              Map<Pair<Node, Object>, Map<Attribute<Node>, Object>> map0) {

		String str = "";
		int i = 0;
		Map<Pair<Node, Object>, Integer> map = new HashMap<>();
		for (Pair<Node, Object> p : sgv.getVertices()) {
			String s = p.toString() + map0.get(p);
			s.replace("\"", "\\\"");
			map.put(p, i); // a [label="Foo"];
			str += i + " [label=\"" + s + "\"];\n";
			i++;
		}

		for (Pair<Path, Integer> p : sgv.getEdges()) {
			Pair<Node, Object> src = sgv.getSource(p);
			Pair<Node, Object> dst = sgv.getDest(p);
			int src_id = map.get(src);
			int dst_id = map.get(dst);
			str += src_id + " -> " + dst_id + " [label=\"" + p.first + "\"];\n";
		}

		str = "digraph " + name + " {\n" + str.trim() + "\n}";
		JPanel p = new JPanel(new GridLayout(1, 1));
		JTextArea area = new JTextArea(str);
		JScrollPane jsp = new JScrollPane(area);
		p.add(jsp);
		return p;
	}

	private static JPanel doView(Color clr, @SuppressWarnings("unused") Instance inst,
                                 Graph<Pair<Node, Object>, Pair<Path, Integer>> sgv,
                                 Map<Pair<Node, Object>, Map<Attribute<Node>, Object>> map0) {
		JPanel cards = new JPanel(new CardLayout());

		Layout<Pair<Node, Object>, Pair<Path, Integer>> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Pair<Node, Object>, Pair<Path, Integer>> vv = new VisualizationViewer<>(
				layout);
		Function<Pair<Node, Object>, Paint> vertexPaint = (Pair<Node, Object> i) -> clr;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexLabelRenderer(new MyVertexT(cards));
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		vv.getRenderContext().setEdgeLabelTransformer(
			(Pair<Path, Integer> t) ->  t.first.toString()
			);

		vv.getRenderContext()
			.setVertexLabelTransformer((Pair<Node, Object> t) -> t.second.toString() );

		JPanel ret = new JPanel(new GridLayout(1, 1));
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		for (Pair<Node, Object> n : sgv.getVertices()) {
			Map<Attribute<Node>, Object> s = map0.get(n);
			Object[] columnNames = new Object[s.keySet().size()];
			Object[][] rowData = new Object[1][s.keySet().size()];

			int i = 0;
			for (Attribute<Node> a : s.keySet()) {
				columnNames[i] = a.name;
				rowData[0][i] = s.get(a);
				i++;
			}
			JPanel p = new JPanel(new GridLayout(1, 1));
			JTable table = new JTable(rowData, columnNames);
			JScrollPane jsp = new JScrollPane(table);
			p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
					"Attributes for " + n.second));

			p.add(jsp);
			cards.add(p, n.second.toString());
		}
		cards.add(new JPanel(), "blank");
		CardLayout cl = (CardLayout) (cards.getLayout());
		cl.show(cards, "blank");

		pane.add(new GraphZoomScrollPane(vv));
		pane.add(cards);
		pane.setResizeWeight(.8d);
		ret.add(pane);

		return ret;
	}

	public static Pair<JPanel, JPanel> makePanel(String name, Instance i, Color c) {
		try {
			JPanel ret;
			JPanel ret2;
			Pair<Graph<Pair<Node, Object>, Pair<Path, Integer>>, HashMap<Pair<Node, Object>, Map<Attribute<Node>, Object>>> g = build(i);
			ret = g.first.getVertexCount() == 0 ? new JPanel() : doView(c, i, g.first, g.second);
			ret2 = dot(name, i, g.first, g.second);

			return new Pair<>(ret, ret2);

		} catch (FQLException e) {
			JPanel p = new JPanel(new GridLayout(1, 1));
			JTextArea a = new JTextArea(e.getMessage());
			p.add(new JScrollPane(a));
			return new Pair<>(p, p);
		}

	}

	private static class MyVertexT implements VertexLabelRenderer {

		final JPanel cards;

		public MyVertexT(JPanel cards) {
			this.cards = cards;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Component getVertexLabelRendererComponent(JComponent arg0, Object arg1,
				Font arg2, boolean arg3, T arg4) {
			Pair<Node, Object> p = (Pair<Node, Object>) arg4;
			if (arg3) {
				CardLayout c = (CardLayout) cards.getLayout();
				c.show(cards, p.second.toString());
			}

			return new JLabel(p.second.toString());
		}
	}

}

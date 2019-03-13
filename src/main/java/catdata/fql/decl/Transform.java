package catdata.fql.decl;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableRowSorter;

import com.google.common.base.Function;

import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.FQLException;
import catdata.fql.FqlOptions;
import catdata.fql.FqlUtil;
import catdata.fql.cat.Arr;
import catdata.fql.cat.FinCat;
import catdata.fql.parse.PrettyPrinter;
import catdata.ide.DefunctGlobalOptions;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;

public class Transform {

	public final Instance src;
    private final Instance dst;
	public final Map<String, Set<Pair<Object, Object>>> data;

	public List<Pair<String, List<Pair<Object, Object>>>> data() {
		List<Pair<String, List<Pair<Object, Object>>>> ret = new LinkedList<>();

		for (String k : data.keySet()) {
			ret.add(new Pair<>(k,
                    new LinkedList<>(data.get(k))));
		}

		return ret;
	}

	public Transform(Instance src, Instance dst,
			List<Pair<String, List<Pair<Object, Object>>>> b) {
		this.src = src;
		this.dst = dst;

		data = new HashMap<>();
		for (Pair<String, List<Pair<Object, Object>>> k : b) {
			if (src.thesig.isNode(k.first)) {
				data.put(k.first, new HashSet<>(k.second));
			}
		}

		validate();
	}

	private void validate() {

		for (Node n : src.thesig.nodes) {
			Set<Pair<Object, Object>> v = data.get(n.string);
			if (v == null) {
				throw new RuntimeException("Missing node: " + n);
			}
			for (Pair<Object, Object> k : src.data.get(n.string)) {
				try {
					lookup(data.get(n.string), k.first);
				} catch (RuntimeException re) {
					throw new RuntimeException("Not total: " + n.string
							+ "\n\n" + this + "\n\nsrc " + src + "\n\ndst "
							+ dst);
				}
			}
			for (Pair<Object, Object> k : v) {
				if (!src.data.get(n.string).contains(
						new Pair<>(k.first, k.first))) {
					throw new RuntimeException("Non-domain value in " + n
							+ "\n\n" + this + "\n\nsrc " + src + "\n\ndst "
							+ dst);
				}
				if (!dst.data.get(n.string).contains(
						new Pair<>(k.second, k.second))) {
					throw new RuntimeException("Non-range value in " + n
							+ "\n\n" + this + "\n\nsrc " + src + "\n\ndst "
							+ dst);
				}
			}
		}

		for (Edge f : src.thesig.edges) {
			Set<Pair<Object, Object>> lhs = compose(data.get(f.source.string),
					dst.data.get(f.name));
			Set<Pair<Object, Object>> rhs = compose(src.data.get(f.name),
					data.get(f.target.string));

			if (!lhs.equals(rhs)) {
				throw new RuntimeException("Not respected on " + f + " in "
						+ this);
			}
		}

		for (Node n : src.thesig.nodes) {
			Set<Pair<Object, Object>> N = src.data.get(n.string);
			for (Pair<Object, Object> id : N) {
				for (Attribute<Node> attr : src.thesig.attrsFor(n)) {
					Set<Pair<Object, Object>> a = src.data.get(attr.name);
					Object valSrc = lookup(a, id.first);

					Object trans_id = lookup(data.get(n.string), id.first);
					a = dst.data.get(attr.name);
					Object valDst = lookup(a, trans_id);

					if (!valSrc.equals(valDst)) {
						String xxx = "cannot pair (" + id.first + ", "
								+ trans_id + "), not equal: att(" + id.first
								+ ") = " + valSrc + " and att(" + trans_id
								+ ") = " + valDst;
						throw new RuntimeException(xxx);
					}
				}
			}
		}

	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transform other = (Transform) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}

	public static Transform composeX(Transform l, Transform r) {
		if (!(l.dst.equals(r.src))) {
			throw new RuntimeException();
		}
		List<Pair<String, List<Pair<Object, Object>>>> xxx = new LinkedList<>();

		for (String k : l.data.keySet()) {
			Set<Pair<Object, Object>> v = l.data.get(k);
			Set<Pair<Object, Object>> v0 = r.data.get(k);
			xxx.add(new Pair<>(k,
                    new LinkedList<>(compose(v, v0))));
		}

		return new Transform(l.src, r.dst, xxx);
	}

	private static Set<Pair<Object, Object>> compose(
			Set<Pair<Object, Object>> l, Set<Pair<Object, Object>> r) {
		Set<Pair<Object, Object>> ret = new HashSet<>();
		for (Pair<Object, Object> k : l) {
			ret.add(new Pair<>(k.first, lookup(r, k.second)));
		}
		return ret;
	}

	public JPanel text() {
		JPanel ret = new JPanel(new GridLayout(1, 1));
		JTextArea area = new JTextArea(toString());
		ret.add(new JScrollPane(area));
		return ret;
	}

	public JPanel graphical(Color scolor, Color tcolor, String srcZ, String dstZ) {
		return makePanel(scolor, tcolor, srcZ, dstZ);
	}

	@SuppressWarnings("unchecked")
	public JPanel view(String src_n, String dst_n) {
		List<JPanel> panels = new LinkedList<>();
		LinkedList<String> sorted = new LinkedList<>(data.keySet());
		sorted.sort(String::compareTo);
		for (String k : sorted) {
			Set<Pair<Object, Object>> xxx = data.get(k);
			List<Pair<Object, Object>> table = new LinkedList<>(xxx);

			Object[][] arr = new Object[table.size()][2];
			int i = 0;
			for (Pair<Object, Object> p : table) {
				arr[i][0] = p.first;
				arr[i][1] = p.second;
				i++;
			}
			@SuppressWarnings("serial")
			JTable t = new JTable(arr, new Object[] { src_n, dst_n })  {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension d = getPreferredSize();
					return new Dimension(d.width, d.height);
				}
			};
			// //t.setRowSelectionAllowed(false);
			// t.setColumnSelectionAllowed(false);
			// MouseListener[] listeners = t.getMouseListeners();
			// for (MouseListener l : listeners) {
			// t.removeMouseListener(l);
			// }
			TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());

			t.setRowSorter(sorter);
			sorter.allRowsChanged();
			sorter.toggleSortOrder(0);
			t.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			JPanel p = new JPanel(new GridLayout(1, 1));
			p.add(new JScrollPane(t));
			p.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEmptyBorder(2, 2, 2, 2), k + "  ("
							+ xxx.size() + " rows)"));
			panels.add(p);
		//	p.setPreferredSize(new Dimension(60, 60));
		}

		/*int x = (int) Math.ceil(Math.sqrt(panels.size()));
		if (x == 0) {
			return new JPanel();
		}
		JPanel panel = new JPanel(new GridLayout(x, x));
		for (JPanel p : panels) {
			panel.add(p);
		}
		panel.setBorder(BorderFactory.createEtchedBorder()); */
		return FqlUtil.makeGrid(((List<JComponent>)((Object)panels)));
	}

	@Override
	public String toString() {
		String nm = "\n nodes\n";
		boolean b = false;
		for (Entry<String, Set<Pair<Object, Object>>> k : data.entrySet()) {
			if (b) {
				nm += ", \n";
			}
			b = true;

			boolean c = false;
			nm += "  " + k.getKey() + " -> " + "{";

			for (Pair<Object, Object> k0 : k.getValue()) {
				if (c) {
					nm += ", ";
				}
				c = true;
				nm += "(" + PrettyPrinter.q(k0.first) + ", "
						+ PrettyPrinter.q(k0.second) + ")";
			}
			nm += "}";
		}
		nm = nm.trim();
		nm += ";\n";

		return "{\n " + nm + "}";
	}

	private Pair<Graph<Quad<Node, Object, String, Boolean>, Pair<Path, Integer>>, HashMap<Quad<Node, Object, String, Boolean>, Map<Attribute<Node>, Object>>> build(
			String src_n, String dst_n) throws FQLException {
		FinCat<Node, Path> c = src.thesig.toCategory2().first;
		HashMap<Quad<Node, Object, String, Boolean>, Map<Attribute<Node>, Object>> map = new HashMap<>();

		Graph<Quad<Node, Object, String, Boolean>, Pair<Path, Integer>> g2 = new DirectedSparseMultigraph<>();
		for (Node n : c.objects) {
			for (Pair<Object, Object> o : src.data.get(n.string)) {
				Quad<Node, Object, String, Boolean> xx = new Quad<>(n, o.first,
						src_n, true);
				g2.addVertex(xx);

				List<Attribute<Node>> attrs = src.thesig.attrsFor(n);
				Map<Attribute<Node>, Object> m = new HashMap<>();
				for (Attribute<Node> attr : attrs) {
					Object a = lookup(src.data.get(attr.name), o.first);
					m.put(attr, a);
				}
				map.put(xx, m);
			}
		}

		int j = 0;
		for (Quad<Node, Object, String, Boolean> x : g2.getVertices()) {
			for (Quad<Node, Object, String, Boolean> y : g2.getVertices()) {
				if (!x.third.equals(y.third)) {
					continue;
				}
				Set<Arr<Node, Path>> h = c.hom(x.first, y.first);
				for (Arr<Node, Path> arr : h) {
					if (c.isId(arr)) {
						continue;
					}
					if (!DefunctGlobalOptions.debug.fql.ALL_GR_PATHS && arr.arr.path.size() != 1) {
						continue;
					}
					if (doLookup(src, arr.arr, x.second, y.second)) {
						g2.addEdge(new Pair<>(arr.arr, j++), x, y);
					}
				}
			}
		}

		for (Node n : c.objects) {
			for (Pair<Object, Object> o : dst.data.get(n.string)) {
				Quad<Node, Object, String, Boolean> xx = new Quad<>(n, o.first,
						dst_n, false);
				g2.addVertex(xx);

				List<Attribute<Node>> attrs = dst.thesig.attrsFor(n);
				Map<Attribute<Node>, Object> m = new HashMap<>();
				for (Attribute<Node> attr : attrs) {
					Object a = lookup(dst.data.get(attr.name), o.first);
					m.put(attr, a);
				}
				map.put(xx, m);
			}
		}

		for (Quad<Node, Object, String, Boolean> x : g2.getVertices()) {
			for (Quad<Node, Object, String, Boolean> y : g2.getVertices()) {
				Set<Arr<Node, Path>> h = c.hom(x.first, y.first);
				for (Arr<Node, Path> arr : h) {
					if (c.isId(arr)) {
						continue;
					}
					if (!DefunctGlobalOptions.debug.fql.ALL_GR_PATHS && arr.arr.path.size() != 1) {
						continue;
					}
					if (doLookup(dst, arr.arr, x.second, y.second)) {
						g2.addEdge(new Pair<>(arr.arr, j++), x, y);
					}
				}
			}
		}

		for (String k : data.keySet()) {
			Set<Pair<Object, Object>> v = data.get(k);
			for (Pair<Object, Object> i : v) {
				Node n = src.thesig.getNode(k);
				g2.addEdge(new Pair<>(null, j++), new Quad<>(n,
						i.first, src_n, true), new Quad<>(n, i.second, dst_n,
						false));
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
		throw new RuntimeException("not found: " + first + " in " + set);
	}

	private static boolean doLookup(Instance i, Path arr, Object x1, Object x2) {
		for (Pair<Object, Object> y : i.evaluate(arr)) {
			if (y.first.equals(x1) && y.second.equals(x2)) {
				return true;
			}
		}
		return false;
	}

	// public JComponent lowerComp() throws FQLException {
	// JComponent c = src.thesig.pretty();
	// c.setMaximumSize(new Dimension(400,100));
	// return c;
	// }
	/*
	 * public JComponent lowerComp(String s, String d) { int size =
	 * src.thesig.nodes.size();
	 *
	 * // JPanel pan = new JPanel(new GridLayout(1, size + 1)); // for (Node n :
	 * src.thesig.nodes) {  // JLabel l = new JLabel(n.string); //
	 * l.setOpaque(true); // l.setHorizontalAlignment(SwingConstants.CENTER); //
	 * l.setBackground(sColor); // pan.add(l); // }
	 *
	 * //JPanel xxx = new JPanel(); // xxx.add(new JLabel(" ")); //JPanel yu =
	 * new MyLabel2(); // yu.setSize(20, 12); //xxx.add(new MyLabel2()); //
	 * xxx.add(new JLabel(s + " (source)")); // xxx.add(new JLabel("    ")); //
	 * JPanel uy = new MyLabel(); // uy.setSize(20,20); // xxx.add(uy); //
	 * xxx.add(new JLabel(d + " (target)")); // pan.add(xxx); // pan.set
	 * JScrollPane p = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER,
	 * JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	 *
	 * pan.setBorder(BorderFactory.createTitledBorder(
	 * BorderFactory.createEmptyBorder(2, 2, 2, 2), "Legend"));
	 * p.setViewportView(pan); return p; }
	 */

	@SuppressWarnings("unchecked")
    private static JPanel doView(Color scolor, Color tcolor,
								 @SuppressWarnings("unused") String src_n,
								 @SuppressWarnings("unused") String dst_n,
								 Graph<Quad<Node, Object, String, Boolean>, Pair<Path, Integer>> first,
								 Map<Quad<Node, Object, String, Boolean>, Map<Attribute<Node>, Object>> second) {

		// HashMap<Pair<Node, Object>,String> map = new HashMap<>();
		JPanel cards = new JPanel(new CardLayout());

		try {
			Class<?> c = Class.forName(FqlOptions.layout_prefix
					+ DefunctGlobalOptions.debug.fql.trans_graph);
			Constructor<?> x = c.getConstructor(Graph.class);
			Layout<Quad<Node, Object, String, Boolean>, Pair<Path, Integer>> layout = (Layout<Quad<Node, Object, String, Boolean>, Pair<Path, Integer>>) x
					.newInstance(first);

			layout.setSize(new Dimension(600, 350));
			VisualizationViewer<Quad<Node, Object, String, Boolean>, Pair<Path, Integer>> vv = new VisualizationViewer<>(
					layout);

			Function<Quad<Node, Object, String, Boolean>, Paint> vertexPaint =
			  (Quad<Node, Object, String, Boolean> i) -> i.fourth ? scolor : tcolor;

			DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
			gm.setMode(Mode.TRANSFORMING);
			vv.setGraphMouse(gm);
			gm.setMode(Mode.PICKING);
			// Set up a new stroke Transformer for the edges
			float dash[] = { 1.0f };
			Stroke edgeStroke = new BasicStroke(0.5f,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash,
					10.0f);
			// Function<String, Stroke> edgeStrokeTransformer = new
			// Function<String, Stroke>() {
			// public Stroke transform(String s) {
			// return edgeStroke;
			// }
			// };
			vv.getRenderContext().setVertexLabelRenderer(new MyVertexT(cards));
			Stroke bs = new BasicStroke();
			Function<Pair<Path, Integer>, Stroke> edgeStrokeTransformer =
				(Pair<Path, Integer> s) -> (s.first == null) ? edgeStroke : bs;

			vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
			vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);
			// vv.getRenderContext().setVertexLabelTransformer(
			// new ToStringLabeller());
			vv.getRenderContext().setEdgeLabelTransformer(
					(Pair<Path, Integer> t) -> (t.first == null) ? "" : t.first.toString()
					);
			// new ToStringLabeller());
			// vv.getRenderer().getVertexRenderer().
			// vv.getRenderContext().setLabelOffset(20);
			// vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);

			vv.getRenderContext()
					.setVertexLabelTransformer(
							(Quad<Node, Object, String, Boolean> t) ->
							 	t.third + "." + t.first + "." + t.second
							);
			// vv.getRenderer().setVertexRenderer(new MyRenderer());

			JPanel ret = new JPanel(new BorderLayout());
			JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

			for (Quad<Node, Object, String, Boolean> n : first.getVertices()) {
				Map<Attribute<Node>, Object> s = second.get(n);

				Object[] columnNames = new Object[s.keySet().size()];
				Object[][] rowData = new Object[1][s.keySet().size()];

				int i = 0;
				// for (Pair<Node, Object> k : map0.keySet()) {
				// Map<Attribute<Node>, Object> v = ma;
				for (Attribute<Node> a : s.keySet()) {
					columnNames[i] = a.name;
					rowData[0][i] = s.get(a);
					i++;
				}

				// }
				JPanel p = new JPanel(new GridLayout(1, 1));
				@SuppressWarnings("serial")
				JTable table = new JTable(rowData, columnNames)  {
					@Override
					public Dimension getPreferredScrollableViewportSize() {
						Dimension d = getPreferredSize();
						return new Dimension(d.width, d.height);
					}
				};
				JScrollPane jsp = new JScrollPane(table);
				p.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createEmptyBorder(), "Attributes for "
								+ n.second));

				p.add(jsp);
				cards.add(p, n.second.toString());
			}
			cards.add(new JPanel(), "blank");
			CardLayout cl = (CardLayout) (cards.getLayout());
			cl.show(cards, "blank");

			pane.add(new GraphZoomScrollPane(vv));
			pane.setResizeWeight(1.0d);
			pane.add(cards);

			cards.setPreferredSize(new Dimension(400, 100));

			ret.add(pane, BorderLayout.CENTER);
			// JComponent iii = lowerComp(src_n, dst_n);
			// iii.setPreferredSize(new Dimension(1, 60));
			// ret.add(iii, BorderLayout.NORTH);
			ret.setBorder(BorderFactory.createEtchedBorder());
			return ret;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException();
		}
	}

	private JPanel makePanel(Color scolor, Color tcolor, String src_n, String dst_n) {
		try {
			Pair<Graph<Quad<Node, Object, String, Boolean>, Pair<Path, Integer>>, HashMap<Quad<Node, Object, String, Boolean>, Map<Attribute<Node>, Object>>> g = build(
					src_n, dst_n);
			if (g.first.getVertexCount() == 0) {
				return new JPanel();
			}
			return doView(scolor, tcolor, src_n, dst_n, g.first, g.second);
		} catch (FQLException e) {
			JPanel p = new JPanel(new GridLayout(1, 1));
			JTextArea a = new JTextArea(e.getMessage());
			p.add(new JScrollPane(a));
			return p;
		}

	}

	private static class MyVertexT implements VertexLabelRenderer {

		final JPanel cards;

		public MyVertexT(JPanel cards) {
			this.cards = cards;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Component getVertexLabelRendererComponent(JComponent arg0,
				Object arg1, Font arg2, boolean arg3, T arg4) {
			Quad<Node, Object, String, Boolean> p = (Quad<Node, Object, String, Boolean>) arg4;
			if (arg3) {
				CardLayout c = (CardLayout) cards.getLayout();
				c.show(cards, p.second.toString());
			}

			return new JLabel(p.second.toString());

		}
	}


	public static Transform prod(
			@SuppressWarnings("unused") Instance I,
			Triple<Instance, Map<Object, Pair<Object, Object>>, Map<Pair<Object, Object>, Object>> IHc,
			Triple<Instance, Map<Object, Pair<Object, Object>>, Map<Pair<Object, Object>, Object>> IHd,
			Transform h0) {
		List<Pair<String, List<Pair<Object, Object>>>> d = new LinkedList<>();

		for (Node n : IHc.first.thesig.nodes) {
			Set<Pair<Object, Object>> v = IHc.first.data.get(n.string);
			List<Pair<Object, Object>> l = new LinkedList<>();
			for (Pair<Object, Object> p : v) {
				Pair<Object, Object> u = IHc.second.get(p.first); // I, Hc
				Object j = lookup(h0.data.get(n.string), u.second); // Hd
				Object k = IHd.third.get(new Pair<>(u.first, j));
				l.add(new Pair<>(p.first, k));
			}
			d.add(new Pair<>(n.string, l));
		}

		return new Transform(IHc.first, IHd.first, d);
	}

	public Instance preimage(Instance a) throws FQLException {
		Map<String, Set<Pair<Object, Object>>> map = new HashMap<>();

		for (Node n : src.thesig.nodes) {
			Set<Pair<Object, Object>> kx = new HashSet<>();
			for (Pair<Object, Object> i : src.data.get(n.string)) {
				Object v = lookup(data.get(n.string), i.first);
				if (a.data.get(n.string).contains(new Pair<>(v, v))) {
					kx.add(i);
				}
			}
			map.put(n.string, kx);
		}

		for (Edge n : src.thesig.edges) {
			Set<Pair<Object, Object>> kx = new HashSet<>();
			for (Pair<Object, Object> i : src.data.get(n.name)) {
				if (map.get(n.source.string).contains(
						new Pair<>(i.first, i.first))) {
					kx.add(i);
				}
			}
			map.put(n.name, kx);
		}

		for (Attribute<Node> n : src.thesig.attrs) {
			Set<Pair<Object, Object>> kx = new HashSet<>();
			for (Pair<Object, Object> i : src.data.get(n.name)) {
				if (map.get(n.source.string).contains(
						new Pair<>(i.first, i.first))) {
					kx.add(i);
				}
			}
			map.put(n.name, kx);
		}

		Instance ret = new Instance(src.thesig, map);
		return ret;
	}



	public Instance apply() throws FQLException { //TODO !!!
		Map<String, Set<Pair<Object, Object>>> map = new HashMap<>();

		for (Node n : src.thesig.nodes) {
			Set<Pair<Object, Object>> kx = new HashSet<>();
			for (Pair<Object, Object> i : src.data.get(n.string)) {
				Object v = lookup(data.get(n.string), i.first);
				kx.add(new Pair<>(v, v));
			}
			map.put(n.string, kx);
		}

		for (Edge n : src.thesig.edges) {
			Set<Pair<Object, Object>> kx = new HashSet<>();
			for (Pair<Object, Object> i : src.data.get(n.name)) {
				Object v1 = lookup(data.get(n.source.string), i.first);
				Object v2 = lookup(data.get(n.target.string), i.second);
				kx.add(new Pair<>(v1, v2));
			}
			map.put(n.name, kx);
		}

		for (Attribute<Node> n : src.thesig.attrs) {
			Set<Pair<Object, Object>> kx = new HashSet<>();
			for (Pair<Object, Object> i : src.data.get(n.name)) {
				Object v1 = lookup(data.get(n.source.string), i.first);
				kx.add(new Pair<>(v1, i.second));
			}
			map.put(n.name, kx);
		}

		return new Instance(src.thesig, map);
	}

}

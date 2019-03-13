package catdata.fqlpp;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import com.google.common.base.Function;

import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.fqlpp.CatExp.Const;
import catdata.fqlpp.cat.Category;
import catdata.fqlpp.cat.FinCat;
import catdata.fqlpp.cat.FinSet;
import catdata.fqlpp.cat.FinSet.Fn;
import catdata.fqlpp.cat.FunCat;
import catdata.fqlpp.cat.Functor;
import catdata.fqlpp.cat.Inst;
import catdata.fqlpp.cat.Signature;
import catdata.fqlpp.cat.Signature.Edge;
import catdata.fqlpp.cat.Signature.Node;
import catdata.fqlpp.cat.Transform;
import catdata.ide.CodeTextPanel;
import catdata.ide.DefunctGlobalOptions;
import catdata.ide.Disp;
import catdata.ide.GUI;
import catdata.ide.GuiUtil;
//import com.google.common.base.Function;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;

/**
 *
 * @author ryan
 *
 */

@SuppressWarnings({"rawtypes"})
public class FqlppDisplay implements Disp {

	private final Map<Object, Color> colors = new HashMap<>();

	private Color getColor(Object o) {
		if (FinSet.FinSet.equals(o) || FinCat.FinCat.equals(o)) {
			return null;
		}
		Color c = colors.get(o);
		if (c == null) {
			colors.put(o, nColor());
		}
		return colors.get(o);
	}

	private int cindex = 0;
	private static final Color[] colors_arr = new Color[] { Color.RED, Color.GREEN, Color.BLUE,
			Color.MAGENTA, Color.yellow, Color.CYAN, Color.GRAY, Color.ORANGE, Color.PINK,
			Color.BLACK, Color.white };

	private Color nColor() {
		if (cindex < colors_arr.length) {
			return colors_arr[cindex++];
		} 
			cindex = 0;
			return nColor();
		
	}

	private final List<Pair<String, JComponent>> frames = new LinkedList<>();

	private static JPanel showSet(Set<?> view, Color c) {
		JTabbedPane px = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fqlpp.set_graph) {
			JComponent gp = makeCatViewer(Category.fromSet(view), c);
			px.add("Graph", gp);
		}

		if (DefunctGlobalOptions.debug.fqlpp.set_tabular) {
			Object[][] rowData = new Object[view.size()][1];
			int i = 0;
			for (Object o : view) {
				rowData[i++][0] = Util.nice(o.toString());
			}
			Object[] colNames = new Object[] { "Element" };
			JPanel gp = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), view.size()
					+ " elements", rowData, colNames);
			px.add("Table", gp);
		}

		if (DefunctGlobalOptions.debug.fqlpp.set_textual) {
			CodeTextPanel gp = new CodeTextPanel(BorderFactory.createEtchedBorder(), view.size()
					+ " elements", Util.nice(view.toString()));
			px.add("Text", gp);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	private JPanel showCat(Category<?, ?> view, Color c) {
		JTabbedPane px = new JTabbedPane();

		Signature<String, String> sig = null;
		String key = unr(env.cats, view, null);
		if (key != null) {
			CatExp r = CatOps.resolve(prog, prog.cats.get(key));
			if (r instanceof Const) {
				Const sig0 = (Const) r;
				sig = new Signature<>(sig0.nodes, sig0.arrows, sig0.eqs);
			}
		}
		if (sig != null && !view.isInfinite()) {
			if (DefunctGlobalOptions.debug.fqlpp.cat_schema) {
				Graph g = buildFromSig(sig);
				if (g.getVertexCount() == 0) {
					px.add("Schema", new JPanel());
				} else if (g.getVertexCount() > DefunctGlobalOptions.debug.fqlpp.MAX_NODES) {
					CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getVertexCount() + " nodes, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_NODES);
					px.add("Schema", xxx);
				} else if (g.getEdgeCount() > DefunctGlobalOptions.debug.fqlpp.MAX_EDGES) {
					CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getEdgeCount() + " edges, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_EDGES);
					px.add("Schema", xxx);
				} else {
					px.add("Schema", doSchemaView(c, g));
				}
			}
		}

		if (DefunctGlobalOptions.debug.fqlpp.cat_graph && !view.isInfinite()) {
			JComponent gp = makeCatViewer(view, c);
			px.add("Graph", gp);
		}

		if (DefunctGlobalOptions.debug.fqlpp.cat_tabular && !view.isInfinite()) {
			JPanel gp = catTable(view);
			px.add("Table", gp);
		}

		if (DefunctGlobalOptions.debug.fqlpp.cat_textual) {
			CodeTextPanel gp = new CodeTextPanel(BorderFactory.createEtchedBorder(),null,
					Util.nice(view.toString()));
			px.add("Text", gp);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	@SuppressWarnings({ "unchecked" })
	private static JPanel catTable(Category view) {
		List<JComponent> gp = new LinkedList<>();

		Object[][] rowData1 = new Object[view.objects().size()][1];
		int i = 0;
		for (Object o : view.objects()) {
			rowData1[i++][0] = Util.nice(o.toString());
		}
		Object[] colNames1 = new Object[] { "Object" };
		JPanel gp1 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "Objects ("
				+ view.objects().size() + ")", rowData1, colNames1);

		Object[][] rowData2 = new Object[view.arrows().size()][3];
		i = 0;
		for (Object o : view.arrows()) {
			rowData2[i][0] = Util.nice(o.toString());
			rowData2[i][1] = Util.nice(view.source(o).toString());
			rowData2[i][2] = Util.nice(view.target(o).toString());
			i++;
		}
		Object[] colNames2 = new Object[] { "Arrow", "Source", "Target" };
		JPanel gp2 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "Arrows ("
				+ view.arrows().size() + ")", rowData2, colNames2);

		Object[][] rowData3 = new Object[view.objects().size()][2];
		i = 0;
		for (Object o : view.objects()) {
			rowData3[i][0] = Util.nice(o.toString());
			rowData3[i][1] = Util.nice(view.identity(o).toString());
			i++;
		}
		Object[] colNames3 = new Object[] { "Object", "Arrow" };
		JPanel gp3 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "s ("
				+ view.objects().size() + ")", rowData3, colNames3);

		Object[][] rowData4 = new Object[view.arrows().size()][2];
		i = 0;
		for (Object o : view.arrows()) {
			rowData4[i][0] = Util.nice(o.toString());
			rowData4[i][1] = Util.nice(view.source(o).toString());
			i++;
		}
		Object[] colNames4 = new Object[] { "Arrow", "Object" };
		JPanel gp4 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "Sources ("
				+ view.arrows().size() + ")", rowData4, colNames4);

		Object[][] rowData5 = new Object[view.arrows().size()][2];
		i = 0;
		for (Object o : view.arrows()) {
			rowData5[i][0] = Util.nice(o.toString());
			rowData5[i][1] = Util.nice(view.target(o).toString());
			i++;
		}
		Object[] colNames5 = new Object[] { "Arrow", "Object" };
		JPanel gp5 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "Targets ("
				+ view.arrows().size() + ")", rowData5, colNames5);

		Object[][] rowData6 = new Object[view.compositionSize()][3];
		i = 0;
		for (Object o1 : view.arrows()) {
			for (Object o2 : view.arrows()) {
				if (!view.target(o1).equals(view.source(o2))) {
					continue;
				}
				rowData6[i][0] = Util.nice(o1.toString());
				rowData6[i][1] = Util.nice(o2.toString());
				rowData6[i][2] = Util.nice(view.compose(o1, o2).toString());
				i++;
			}
		}
		Object[] colNames6 = new Object[] { "A1", "A2", "A1 ; A2" };
		JPanel gp6 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "Composition (" + i + ")",
				rowData6, colNames6);

		gp.add(gp1);
		gp.add(gp4);
		gp.add(gp3);
		gp.add(gp2);
		gp.add(gp5);
		gp.add(gp6);
		return GuiUtil.makeGrid2(gp);
	}

	@SuppressWarnings("unchecked")
    private JPanel showFn(Fn view, Color src, Color dst) {
		JTabbedPane px = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fqlpp.fn_graph) {
			JComponent gp = makeFnViewer(view, src, dst);
			px.add("Graph", gp);
		}

		if (DefunctGlobalOptions.debug.fqlpp.fn_tabular) {
			Object[][] rowData = new Object[view.source.size()][2];
			int i = 0;
			for (Object o : view.source) {
				rowData[i][0] = Util.nice(o.toString());
				rowData[i][1] = Util.nice(view.apply(o).toString());
				i++;
			}
			Object[] colNames = new Object[] { unr(env.sets, view.source, "..."),
					unr(env.sets, view.target, "...") };
			JPanel gp = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), view.source.size()
					+ " elements in domain, " + view.target.size() + " elements in codomain",
					rowData, colNames);
			px.add("Table", gp);
		}

		if (DefunctGlobalOptions.debug.fqlpp.fn_textual) {
			CodeTextPanel gp = new CodeTextPanel(BorderFactory.createEtchedBorder(),
					view.source.size() + " elements in domain, " + view.target.size()
							+ " elements in codomain", Util.nice(view.toStringLong()));
			px.add("Text", gp);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	@SuppressWarnings("unchecked")
    private JPanel showFtr(Functor view, Color c, @SuppressWarnings("unused") FunctorExp e) {
		JTabbedPane px = new JTabbedPane();

		if (view.source.isInfinite()) {
			CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), null,
					"Cannot display functors from " + view.source);
			px.add("Text", p);
			JPanel top = new JPanel(new GridLayout(1, 1));
			top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			top.add(px);
			return top;
		}

		Signature<String, String> src_sig = null;
		Signature<String, String> dst_sig = null;
		String src_key = unr(env.cats, view.source, null);
		if (src_key != null) {
			CatExp r = CatOps.resolve(prog, prog.cats.get(src_key));
			if (r instanceof Const) {
				Const sig0 = (Const) r;
				src_sig = new Signature<>(sig0.nodes, sig0.arrows, sig0.eqs);
			}
		}
		String dst_key = unr(env.cats, view.target, null);
		if (dst_key != null) {
			CatExp r = CatOps.resolve(prog, prog.cats.get(dst_key));
			if (r instanceof Const) {
				Const sig0 = (Const) r;
				dst_sig = new Signature<>(sig0.nodes, sig0.arrows, sig0.eqs);
			}
		}
		if (src_sig != null && FinSet.FinSet.equals(view.target)) {
			if (DefunctGlobalOptions.debug.fqlpp.ftr_instance) {
			JPanel vwr = new JPanel(new GridLayout(1, 1));
			if (view.source.objects().isEmpty()) {
				px.add("Instance", vwr);
			} else {
				JComponent zzz = doFNView2(view, vwr, c, buildFromSig(src_sig), src_sig);
				JSplitPane newthing = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
				newthing.setResizeWeight(.5d);
				newthing.add(zzz);
				newthing.add(vwr);
				JPanel xxx = new JPanel(new GridLayout(1, 1));
				xxx.add(newthing);
				px.add("Instance", xxx);
			}
			}

			if (DefunctGlobalOptions.debug.fqlpp.ftr_joined) {
			px.add("Joined", (Component) makeJoined(src_sig, view).first); //cast needed for javac for some reason
			}

			if (DefunctGlobalOptions.debug.fqlpp.ftr_elements) {
			Graph g = buildElements(src_sig, view);
			if (g.getVertexCount() == 0) {
				px.add("Elements", new JPanel());
			} else if (g.getVertexCount() > DefunctGlobalOptions.debug.fqlpp.MAX_NODES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getVertexCount() + " nodes, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_NODES);
				px.add("Elements", xxx);
			} else if (g.getEdgeCount() > DefunctGlobalOptions.debug.fqlpp.MAX_EDGES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getEdgeCount() + " edges, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_EDGES);
				px.add("Elements", xxx);
			} else {
				px.add("Elements", doElementsView(c, g));
			}
			}
		}
		if (src_sig != null && dst_sig != null && !view.source.isInfinite() && !view.target.isInfinite()) {
			//JPanel vwr = new JPanel(new GridLayout(1, 1));
			if (DefunctGlobalOptions.debug.fqlpp.ftr_mapping) {
			Graph g = buildMapping(src_sig, dst_sig, view);
			if (g.getVertexCount() == 0) {
				px.add("Mapping", new JPanel());
			} else if (g.getVertexCount() > DefunctGlobalOptions.debug.fqlpp.MAX_NODES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getVertexCount() + " nodes, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_NODES);
				px.add("Mapping", xxx);
			} else if (g.getEdgeCount() > DefunctGlobalOptions.debug.fqlpp.MAX_EDGES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getEdgeCount() + " edges, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_EDGES);
				px.add("Mapping", xxx);
			}else {
				JComponent zzz = doMappingView(c, getColor(view.target), g);
				JPanel xxx = new JPanel(new GridLayout(1, 1));
				xxx.add(zzz);
				px.add("Mapping", xxx);
			}
			}
		}

		if (DefunctGlobalOptions.debug.fqlpp.ftr_graph) {
			JPanel vwr = new JPanel(new GridLayout(1, 1));
			Graph g = buildFromCat(view.source);
			if (view.source.objects().isEmpty()) {
				px.add("Graph", vwr);
			} else if (g.getVertexCount() > DefunctGlobalOptions.debug.fqlpp.MAX_NODES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getVertexCount() + " nodes, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_NODES);
				px.add("Graph", xxx);
			} else if (g.getEdgeCount() > DefunctGlobalOptions.debug.fqlpp.MAX_EDGES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getEdgeCount() + " edges, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_EDGES);
				px.add("Graph", xxx);
			} else {
				JComponent zzz = doFNView(view, vwr, c, g);
				JSplitPane newthing = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
				newthing.setResizeWeight(.5d);
				newthing.add(zzz);
				newthing.add(vwr);
				JPanel xxx = new JPanel(new GridLayout(1, 1));
				xxx.add(newthing);
				px.add("Graph", xxx);
			}
		}

		if (DefunctGlobalOptions.debug.fqlpp.ftr_tabular) {
			List<JComponent> gp = new LinkedList<>();
			//JPanel gp = new JPanel(new GridLayout(2, 1));

			Object[][] rowData = new Object[view.source.objects().size()][2];
			int i = 0;
			for (Object o : view.source.objects()) {
				rowData[i][0] = Util.nice(o.toString());
				rowData[i][1] = Util.nice(view.applyO(o).toString());
				i++;
			}
			Object[] colNames = new Object[] { "Input", "Output" };
			JPanel gp1 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "On Objects ("
					+ view.source.objects().size() + ")", rowData, colNames);

			Object[][] rowData2 = new Object[view.source.arrows().size()][6];
			i = 0;
			for (Object o : view.source.arrows()) {
				rowData2[i][0] = Util.nice(o.toString());
				rowData2[i][1] = Util.nice(view.source.source(o).toString());
				rowData2[i][2] = Util.nice(view.source.target(o).toString());
				rowData2[i][3] = Util.nice(view.applyA(o).toString());
				rowData2[i][4] = Util.nice(view.target.source(view.applyA(o)).toString());
				rowData2[i][5] = Util.nice(view.target.target(view.applyA(o)).toString());
				i++;
			}
			Object[] colNames2 = new Object[] { "Input", "Source", "Target", "Output", "Source",
					"Target" };
			JPanel gp2 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "On Arrows ("
					+ view.source.arrows().size() + ")", rowData2, colNames2);

			gp.add(gp1);
			gp.add(gp2);

			px.add("Table", GuiUtil.makeGrid(gp));
		}

		if (DefunctGlobalOptions.debug.fqlpp.ftr_textual) {
			CodeTextPanel gp = new CodeTextPanel(BorderFactory.createEtchedBorder(), "",
					Util.nice(view.toString()));
			px.add("Text", gp);
		}


		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	@SuppressWarnings("unchecked")
    private JPanel showTrans(Transform view, Color c) {
		JTabbedPane px = new JTabbedPane();

		if (view.source.source.isInfinite()) {
			CodeTextPanel p = new CodeTextPanel(BorderFactory.createEtchedBorder(), null,
					"Cannot display transforms from " + view.source.source);
			px.add("Text", p);
			JPanel top = new JPanel(new GridLayout(1, 1));
			top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			top.add(px);
			return top;
		}

		Signature<Object, Object> src_sig = null;
	//	Signature<Object, Object> dst_sig = null;
		String src_key = unr(env.cats, view.source.source, null);
		if (src_key == null) {
			src_key = unr(env.cats, view.target.source, null);
		}
		if (src_key != null) {
			CatExp r = CatOps.resolve(prog, prog.cats.get(src_key));
			if (r instanceof Const) {
				Const sig0 = (Const) r;
				src_sig = new Signature(sig0.nodes, sig0.arrows, sig0.eqs);
			}
		}


		if (src_sig != null && FinSet.FinSet.equals(view.target.target)) {
		//	JPanel vwr = new JPanel(new GridLayout(1, 1));
			if (DefunctGlobalOptions.debug.fqlpp.trans_elements) {
			Graph g = build2Elements(src_sig, view);
			if (g.getVertexCount() == 0) {
				px.add("Elements", new JPanel());
			} else if (g.getVertexCount() > DefunctGlobalOptions.debug.fqlpp.MAX_NODES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getVertexCount() + " nodes, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_NODES);
				px.add("Elements", xxx);
			} else if (g.getEdgeCount() > DefunctGlobalOptions.debug.fqlpp.MAX_EDGES) {
				CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getEdgeCount() + " edges, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_EDGES);
				px.add("Elements", xxx);
			}else {
				JComponent zzz = doElements2View(c, g);
				JPanel xxx = new JPanel(new GridLayout(1, 1));
				xxx.add(zzz);
				px.add("Elements", xxx);
			}
			}
		}

		if (DefunctGlobalOptions.debug.fqlpp.trans_graph) {
			JPanel vwr = new JPanel(new GridLayout(1, 1));
			if (view.source.source.objects().isEmpty()) {
				px.add("Graph", vwr);

			} else {
				JComponent zzz = doNTView(view, vwr, c, buildFromCat(view.source.source));
				JSplitPane newthing = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
				newthing.setResizeWeight(.5d);
				newthing.add(zzz);
				newthing.add(vwr);
				JPanel xxx = new JPanel(new GridLayout(1, 1));
				xxx.add(newthing);
				px.add("Graph", xxx);
			}
		}

		if (DefunctGlobalOptions.debug.fqlpp.trans_tabular) {
			JPanel gp = new JPanel(new GridLayout(1, 1));

			Object[][] rowData = new Object[view.source.source.objects().size()][2];
			int i = 0;
			for (Object o : view.source.source.objects()) {
				rowData[i][0] = Util.nice(o.toString());
				rowData[i][1] = Util.nice(view.apply(o).toString());
				i++;
			}
			Object[] colNames = new Object[] { "Input", "Output" };
			JPanel gp1 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), "On Objects ("
					+ view.source.source.objects().size() + ")", rowData, colNames);

			gp.add(gp1);

			px.add("Table", gp);
		}

		if (DefunctGlobalOptions.debug.fqlpp.trans_textual) {
			CodeTextPanel gp = new CodeTextPanel(BorderFactory.createEtchedBorder(), "",
					Util.nice(view.toString()));
			px.add("Text", gp);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}



	private final FQLPPProgram prog;
	private final FQLPPEnvironment env;
	GUI gui;

	// private Map<String, Color> cmap = new HashMap<>();
	public FqlppDisplay(String title, FQLPPProgram p, FQLPPEnvironment env) {
        prog = p;
		this.env = env;
	//	this.gui = gui;

		for (String c : p.order) {
			if (env.sets.containsKey(c)) {
				Set<?> exp = env.sets.get(c);
				frames.add(new Pair<>("set " + c, showSet(exp, getColor(exp))));
			} else if (env.fns.containsKey(c)) {
				Fn o = env.fns.get(c);
				frames.add(new Pair<>("function " + c + " : " + unr(env.sets, o.source, "...")
						+ " -> " + unr(env.sets, o.target, "..."), showFn(o, getColor(o.source),
						getColor(o.target))));
			} else if (env.cats.containsKey(c)) {
				Category<?, ?> cat = env.cats.get(c);
				frames.add(new Pair<>("category " + c, showCat(cat, getColor(cat))));
			} else if (env.ftrs.containsKey(c)) {
				Functor o = env.ftrs.get(c);
				String ddd = "...";
				if (FinSet.FinSet.equals(o.target)) {
					ddd = "Set";
				} else if (FinCat.FinCat.equals(o.target)) {
					ddd = "Cat";
				} else if (o.target instanceof Inst) {
					ddd = "Set^" + unr(env.cats, ((Inst)o.target).cat, ddd);
				} else if (o.target instanceof FunCat) {
					ddd = "Cat^" + unr(env.cats, ((FunCat)o.target).cat, ddd);
				} else {
					ddd = unr(env.cats, o.target, ddd);
				}
				String eee = "...";
				if (FinSet.FinSet.equals(o.source)) {
					eee = "Set";
				} else if (FinCat.FinCat.equals(o.source)) {
					eee = "Cat";
				} else if (o.source instanceof Inst) {
					eee = "Set^" + unr(env.cats, ((Inst)o.source).cat, ddd);
				} else if (o.source
						instanceof FunCat) {
					eee = "Cat^" + unr(env.cats, ((FunCat)o.source).cat, ddd);
				} else {
					eee = unr(env.cats, o.source, eee);
				}
				frames.add(new Pair<>("functor " + c + " : " + eee + " -> " + ddd, showFtr(o,
						getColor(o.source), p.ftrs.get(c))));
			} else if (env.trans.containsKey(c)) {
				Transform o = env.trans.get(c);
				String ddd = unr(env.ftrs, o.target, "...");
				String eee = unr(env.ftrs, o.source, "...");
				frames.add(new Pair<>("transform " + c + " : " + eee + " -> " + ddd, showTrans(o,
						getColor(o.source.source))));
			} else {
				throw new RuntimeException();
			}
		}

		display(title, prog.order);
	}

	private static <X> String unr(Map<String, X> set, X s, String xxx) {
		for (Entry<String, X> k : set.entrySet()) {
			if (k.getValue().equals(s)) {
				return k.getKey();
			}
		}
		return xxx;
	}

	private JFrame frame = null;
	//private String name;

	private final CardLayout cl = new CardLayout();
	private final JPanel x = new JPanel(cl);
	private final JList<String> yyy = new JList<>();
	//private final Map<String, String> indices = new HashMap<>();

	private void display(String s, @SuppressWarnings("unused") List<String> order) {
		frame = new JFrame();
       // name = s;

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
			JSplitPane px = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		//px.setResizeWeight(.8);
		px.setDividerLocation(200);
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
		if (frame == null) {
			return;
		}
		frame.setVisible(false);
		frame.dispose();
		frame = null;
	}

	private static <X, Y> Graph<Pair<X, Object>, Triple<Y, Pair<X, Object>, Pair<X, Object>>> buildElements(
            Signature<X, Y> c,
            Functor<Signature<X, Y>.Node, Signature<X, Y>.Path, Set<Object>, Fn<Object, Object>> I) {
		Graph<Pair<X, Object>, Triple<Y, Pair<X, Object>, Pair<X, Object>>> ret = new DirectedSparseMultigraph<>();

		for (Signature<X, Y>.Node n : c.nodes) {
			for (Object o : I.applyO(n)) {
				ret.addVertex(new Pair<>(n.name, o));
			}
		}
		for (Signature<X, Y>.Edge e : c.edges) {
			for (Object o : I.applyO(e.source)) {
				Object fo = I.applyA(c.path(e)).apply(o);
				Pair<X, Object> s = new Pair<>(e.source.name, o);
				Pair<X, Object> t = new Pair<>(e.target.name, fo);
				ret.addEdge(new Triple<>(e.name, s, t), s, t);
			}
		}

		return ret;
	}

	@SuppressWarnings("unchecked")
    private static Graph buildMapping(Signature<String, String> src, Signature<String, String> dst, Functor F) {

		Graph<Object, Object> ret = new DirectedSparseMultigraph<>();

		for (Node n : src.nodes) {
			ret.addVertex(new Pair<>(n.name, "src"));
		}
		for (Edge e : src.edges) {
			Pair s = new Pair<>(e.source.name, "src");
			Pair t = new Pair<>(e.target.name, "src");
			ret.addEdge(new Quad<>(e.name, s, t, "src"), s, t);
		}

		for (Node n : dst.nodes) {
			ret.addVertex(new Pair<>(n.name, "dst"));
		}
		for (Edge e : dst.edges) {
			Pair s = new Pair<>(e.source.name, "dst");
			Pair t = new Pair<>(e.target.name, "dst");
			ret.addEdge(new Quad<>(e.name, s, t, "dst"), s, t);
		}

		int i = 0;
		for (Node n : src.nodes) {
			Node fo = (Node) F.applyO(n);
			Pair s = new Pair<>(n.name, "src");
			Pair t = new Pair<>(fo.name, "dst");
			ret.addEdge(new Quad<>("", s, t, i++), s, t);
		}

		return ret;
	}

	@SuppressWarnings("unchecked")
    private static Graph build2Elements(Signature<Object, Object> sig,
                                        Transform<Object, Object, Set, Fn> trans) {
		Functor<Object, Object, Set, Fn> I = trans.source;
		Functor<Object, Object, Set, Fn> J = trans.target;

		Graph<Object, Object> ret = new DirectedSparseMultigraph<>();

		for (Node n : sig.nodes) {
			for (Object o : I.applyO(n)) {
				ret.addVertex(new Triple<>(o, n.name, "src"));
			}
		}
		for (Edge e : sig.edges) {
			for (Object o : I.applyO(e.source)) {
				Object fo = I.applyA(sig.path(e)).apply(o);
				Triple s = new Triple<>(o, e.source.name, "src");
				Triple t = new Triple<>(fo, e.target.name, "dst");
				ret.addEdge(new Quad<>(e.name, s, t, "src"), s, t);
			}
		}

		for (Node n : sig.nodes) {
			for (Object o : J.applyO(n)) {
				ret.addVertex(new Triple<>(o, n.name, "dst"));
			}
		}
		for (Edge e : sig.edges) {
			for (Object o : J.applyO(e.source)) {
				Object fo = J.applyA(sig.path(e)).apply(o);
				Triple s = new Triple<>(o, e.source.name, "dst");
				Triple t = new Triple<>(fo, e.target.name, "dst");
				ret.addEdge(new Quad<>(e.name, s, t, "dst"), s, t);
			}
		}

		int i = 0;
		for (Node n : sig.nodes) {
			for (Object o : I.applyO(n)) {
				Object fo = trans.apply(n).apply(o);
				Triple s = new Triple<>(o, n.name, "src");
				Triple t = new Triple<>(fo, n.name, "dst");
				ret.addEdge(new Quad<>("", s, t, i++), s, t);
			}
		}

		return ret;
	}

	private static Graph<Signature<String, String>.Node, String> buildFromSig(
            Signature<String, String> c) {
		Graph<Signature<String, String>.Node, String> g2 = new DirectedSparseMultigraph<>();
		for (Signature<String, String>.Node n : c.nodes) {
			g2.addVertex(n);
		}
		for (Signature<String, String>.Edge e : c.edges) {
			g2.addEdge(e.name, e.source, e.target);
		}
		return g2;
	}

	private static <X, Y> Graph<X, Y> buildFromCat(Category<X, Y> c) {
		Graph<X, Y> g2 = new DirectedSparseMultigraph<>();
		for (X n : c.objects()) {
			g2.addVertex(n);
		}
		for (Y e : c.arrows()) {
			if (c.identity(c.source(e)).equals(e)) {
				continue;
			}
			g2.addEdge(e, c.source(e), c.target(e));
		}
		return g2;
	}

	@SuppressWarnings("unchecked")
    private static Graph<Pair<String, Color>, Integer> buildFromFn(Fn f, Color src, Color dst) {

		Graph<Pair<String, Color>, Integer> g2 = new DirectedSparseMultigraph<>();
		for (Object n : f.source) {
			g2.addVertex(new Pair<>(Util.nice(n.toString()), src));
		}
		for (Object n : f.target) {
			g2.addVertex(new Pair<>(Util.nice(n.toString()), dst));
		}
		int i = 0;
		for (Object n : f.source) {
			Pair<String, Color> p1 = new Pair<>(Util.nice(n.toString()), src);
			Pair<String, Color> p2 = new Pair<>(Util.nice(f.apply(n).toString()), dst);

			g2.addEdge(i++, p1, p2);
		}

		return g2;
	}

	private static <X, Y> JComponent makeCatViewer(Category<X, Y> cat, Color clr) {
		Graph<X, Y> g = buildFromCat(cat);
		if (g.getVertexCount() == 0) {
			return new JPanel();
		} else if (g.getVertexCount() > DefunctGlobalOptions.debug.fqlpp.MAX_NODES) {
			CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getVertexCount() + " nodes, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_NODES);
			JPanel ret = new JPanel(new GridLayout(1,1));
			ret.add(xxx);
			return ret;
		} else if (g.getEdgeCount() > DefunctGlobalOptions.debug.fqlpp.MAX_EDGES) {
			CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getEdgeCount() + " edges, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_EDGES);
			JPanel ret = new JPanel(new GridLayout(1,1));
			ret.add(xxx);
			return ret;
		}
		return doCatView(clr, g);
	}

	private static JComponent makeFnViewer(Fn cat, Color src, Color dst) {
		Graph<Pair<String, Color>, Integer> g = buildFromFn(cat, src, dst);
		if (g.getVertexCount() == 0) {
			return new JPanel();
		} else if (g.getVertexCount() > DefunctGlobalOptions.debug.fqlpp.MAX_NODES) {
			CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getVertexCount() + " nodes, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_NODES);
			JPanel ret = new JPanel(new GridLayout(1,1));
			ret.add(xxx);
			return ret;
		}else if (g.getEdgeCount() > DefunctGlobalOptions.debug.fqlpp.MAX_EDGES) {
			CodeTextPanel xxx = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "Graph has " + g.getEdgeCount() + " edges, which exceeds limit of " + DefunctGlobalOptions.debug.fqlpp.MAX_EDGES);
			JPanel ret = new JPanel(new GridLayout(1,1));
			ret.add(xxx);
			return ret;
		}
		return doFnView(g);
	}

	private static JComponent doFnView(Graph<Pair<String, Color>, Integer> sgv) {
		try {
			Layout<Pair<String, Color>, Integer> layout = new FRLayout<>(sgv);
			// layout.setSize(new Dimension(600, 200));
			VisualizationViewer<Pair<String, Color>, Integer> vv = new VisualizationViewer<>(layout);
			Function<Pair<String, Color>, Paint> vertexPaint = x -> x.second;
			DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
			gm.setMode(Mode.TRANSFORMING);
			vv.setGraphMouse(gm);
			gm.setMode(Mode.PICKING);
			vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

			vv.getRenderContext().setVertexLabelTransformer(x -> x.first);
			vv.getRenderContext().setEdgeLabelTransformer(x -> "");

			GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
			JPanel ret = new JPanel(new GridLayout(1, 1));
			ret.add(zzz);
			ret.setBorder(BorderFactory.createEtchedBorder());
			return ret;
		} catch (Throwable cnf) {
			cnf.printStackTrace();
			throw new RuntimeException();
		}
	}

	@SuppressWarnings("unchecked")
    private static <X, Y> JComponent doCatView(Color clr, Graph<X, Y> sgv) {
		Layout<X, Y> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<X, Y> vv = new VisualizationViewer<>(layout);
		Function<X, Paint> vertexPaint = x -> clr;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function ttt = arg0 -> Util.nice(arg0.toString());
		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(ttt);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	@SuppressWarnings("unchecked")
    private <X, Y> JComponent doFNView(Functor fn, JPanel p, Color clr, Graph<X, Y> sgv) {
		Layout<X, Y> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<X, Y> vv = new VisualizationViewer<>(layout);
		Function<X, Paint> vertexPaint = z -> clr;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function fff = arg0 -> Util.nice(arg0.toString());
		vv.getRenderContext().setVertexLabelTransformer(fff);
		vv.getRenderContext().setEdgeLabelTransformer(fff);

		vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        return;
                    }
                    vv.getPickedEdgeState().clear();
                    X str = ((X) e.getItem());
                    Object y = fn.applyO(str);
                    p.removeAll();
                    if (y instanceof Category) {
                        Category ttt = (Category) y;
                        JPanel sss = showCat(ttt, getColor(ttt));
                        p.add(sss);
                    } else if (y instanceof Set) {
                        Set ttt = (Set) y;
                        JPanel sss = showSet(ttt, getColor(ttt));
                        p.add(sss);
                    } else if (y instanceof Functor) {
                        Functor ttt = (Functor) y;
                        JPanel sss = showFtr(ttt, getColor(ttt), null);
                        p.add(sss);
                    } else {
                        String sss = Util.nice(y.toString());
                        p.add(new CodeTextPanel(BorderFactory.createEtchedBorder(), null, sss));
                    }
                    p.revalidate();
                });

		vv.getPickedEdgeState().addItemListener((ItemEvent e) -> {
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        return;
                    }
                    vv.getPickedVertexState().clear();
                    X str = ((X) e.getItem());
                    Object y = fn.applyA(str);
                    p.removeAll();
                    if (y instanceof Functor) {
                        Functor ttt = (Functor) y;
                        JPanel sss = showFtr(ttt, getColor(ttt.source), null);
                        p.add(sss);
                    } else if (y instanceof Fn) {
                        Fn ttt = (Fn) y;
                        JPanel sss = showFn(ttt, getColor(ttt.source), getColor(ttt.target));
                        p.add(sss);
                    } else if (y instanceof Transform) {
                        Transform ttt = (Transform) y;
                        JPanel sss = showTrans(ttt, getColor(ttt.source));
                        p.add(sss);
                    } else {
                        String sss = Util.nice(y.toString());
                        p.add(new CodeTextPanel(BorderFactory.createEtchedBorder(), null, sss));
                    }
                    p.revalidate();
                });

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	@SuppressWarnings("unchecked")
  private static <X, Y> JComponent doFNView2(Functor fn, JPanel p, Color clr, Graph<X, Y> sgv,
                                               Signature sig) {
		Layout<X, Y> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<X, Y> vv = new VisualizationViewer<>(layout);
		Function<X, Paint> vertexPaint = z -> clr;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function ttt = arg0 -> Util.nice(arg0.toString());
		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(ttt);

		Map<Object, JPanel> map = (Map<Object, JPanel>) makeJoined(sig, fn).second; //javac again
		vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        return;
                    }
                    vv.getPickedEdgeState().clear();
                    X str = ((X) e.getItem());
                    // Object y = fn.applyO(str);
                    p.removeAll();
                    p.add(map.get(str));
                    p.revalidate();
                });

		vv.getPickedEdgeState().addItemListener((ItemEvent e) -> {
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        return;
                    }
                    vv.getPickedVertexState().clear();
                    X str = ((X) e.getItem());
                    // Object y = fn.applyA(str);
                    p.removeAll();
                    p.add(map.get(str));
                    p.revalidate();
                });

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	@SuppressWarnings("unchecked")
    private static  JComponent doSchemaView(Color clr, Graph sgv) {
		Layout layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer vv = new VisualizationViewer<>(layout);
		Function vertexPaint = x -> clr;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function ttt = arg0 -> Util.nice(arg0.toString());
		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(ttt);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}


	private static JComponent doElementsView(Color clr, Graph<Pair, Triple> sgv) {
		Layout<Pair, Triple> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Pair, Triple> vv = new VisualizationViewer<>(layout);
		Function<Pair, Paint> vertexPaint = z -> clr;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function<Pair, String> ttt1 = arg0 -> Util.nice(arg0.second.toString());
		vv.getRenderContext().setVertexLabelTransformer(ttt1);

		Function<Triple, String> ttt2 = arg0 -> Util.nice(arg0.first.toString());
		vv.getRenderContext().setEdgeLabelTransformer(ttt2);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	private static JComponent doMappingView(Color clr1, Color clr2, Graph<Pair, Quad> sgv) {
		Layout<Pair, Quad> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Pair, Quad> vv = new VisualizationViewer<>(layout);
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);

		Function<Pair, Paint> vertexPaint = x -> x.second.equals("src") ? clr1 : clr2;
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function<Pair, String> ttt1 = arg0 -> Util.nice(arg0.first.toString());
		vv.getRenderContext().setVertexLabelTransformer(ttt1);

		Function<Quad, String> ttt2 = arg0 -> Util.nice(arg0.first.toString());
		vv.getRenderContext().setEdgeLabelTransformer(ttt2);

		float dash[] = { 1.0f };
		Stroke edgeStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
				10.0f, dash, 10.0f);
		Stroke bs = new BasicStroke();
		Function<Quad, Stroke> edgeStrokeTransformer = x -> x.fourth instanceof Integer ? edgeStroke
				: bs;
		vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	private static JComponent doElements2View(Color clr, Graph<Triple, Quad> sgv) {
		Layout<Triple, Quad> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Triple, Quad> vv = new VisualizationViewer<>(layout);
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);

		Color clr1 = clr.brighter().brighter();
		Color clr2 = clr.darker().darker();

		Function<Triple, Paint> vertexPaint = x -> x.third.equals("src") ? clr1 : clr2;
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function<Triple, String> ttt1 = arg0 -> Util.nice(arg0.first.toString());
		vv.getRenderContext().setVertexLabelTransformer(ttt1);

		Function<Quad, String> ttt2 = arg0 -> Util.nice(arg0.first.toString());
		vv.getRenderContext().setEdgeLabelTransformer(ttt2);

		float dash[] = { 1.0f };
		Stroke edgeStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
				10.0f, dash, 10.0f);
		Stroke bs = new BasicStroke();
		Function<Quad, Stroke> edgeStrokeTransformer = x -> x.fourth instanceof Integer ? edgeStroke
				: bs;
		vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	@SuppressWarnings("unchecked")
    private <X, Y> JComponent doNTView(Transform fn, JPanel p, Color clr, Graph<X, Y> sgv) {
		//Layout<X, Y> layout = new FRLayout<>(sgv);
		Layout<X, Y> layout = new FRLayout(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<X, Y> vv = new VisualizationViewer<>(layout);
		Function<X, Paint> vertexPaint = z -> clr;
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		Function www = arg0 -> Util.nice(arg0.toString());
		vv.getRenderContext().setVertexLabelTransformer(www);
		vv.getRenderContext().setEdgeLabelTransformer(www);

		vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        return;
                    }
                    vv.getPickedEdgeState().clear();
                    X str = ((X) e.getItem());
                    Object y = fn.apply(str);
                    p.removeAll();
                    if (y instanceof Functor) {
                        Functor ttt = (Functor) y;
                        JPanel sss = showFtr(ttt, getColor(ttt.source), null);
                        p.add(sss);
                    } else if (y instanceof Fn) {
                        Fn ttt = (Fn) y;
                        JPanel sss = showFn(ttt, getColor(ttt.source), getColor(ttt.target));
                        p.add(sss);
                    } else if (y instanceof Transform) {
                        Transform ttt = (Transform) y;
                        JPanel sss = showTrans(ttt, getColor(ttt.source));
                        p.add(sss);
                    } else {
                        String sss = Util.nice(y.toString());
                        p.add(new CodeTextPanel(BorderFactory.createEtchedBorder(), null, sss));
                    }
                    p.revalidate();
                });

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());
		return ret;
	}

	@SuppressWarnings("unchecked")
    private static
	Pair<JPanel, Map<Object, JPanel>> makeJoined(Signature<String, String> sig,
                                                 Functor<Object, Object, Set, Fn<Object, Object>> F) {
		Map<Node, List<Signature<String, String>.Edge>> map = new HashMap<>();
		Map<Object, JPanel> mapX = new HashMap<>();
		for (Node n : sig.nodes) {
			map.put(n, new LinkedList<>());
		}
		for (Edge t : sig.edges) {
			map.get(t.source).add(t);
		}

//		int x = (int) Math.ceil(Math.sqrt(sig.nodes.size()));
		List<JComponent> ret = new LinkedList<>();

		for (Node n : sig.nodes) {
			List<Signature<String, String>.Edge> cols = map.get(n);
			Object[] colNames = new Object[cols.size() + 1];
			colNames[0] = "ID";
			Set set = F.applyO(n);
			Object[][] rowData = new Object[set.size()][cols.size() + 1];
			int j = 0;
			for (Object o : set) {
				rowData[j][0] = o;
				j++;
			}
			int i = 1;
			for (Signature<String, String>.Edge t : cols) {
				colNames[i] = t.name;
				Fn<Object, Object> fn = F.applyA(sig.path(t));
				j = 0;
				for (Object o : set) {
					rowData[j][i] = fn.apply(o);
					j++;
				}
				i++;
			}
			JPanel p = GuiUtil.makeTable(BorderFactory.createEtchedBorder(), n + " (" + set.size()
					+ " rows)", rowData, colNames);
			ret.add(p);
			mapX.put(n, p);
		}

		for (Signature<String, String>.Edge t : sig.edges) {
			Object[] colNames = new Object[2];
			colNames[0] = t.source;
			colNames[1] = t.target;
			Set set = F.applyO(t.source);
			Object[][] rowData = new Object[set.size()][2];
			Fn<Object, Object> fn = F.applyA(sig.path(t));
			int j = 0;
			for (Object o : set) {
				rowData[j][0] = o;
				rowData[j][1] = fn.apply(o);
				j++;
			}

			JPanel p = GuiUtil.makeTable(BorderFactory.createEtchedBorder(),
					t.name + " (" + set.size() + " rows)", rowData, colNames);
			mapX.put(t.name, p);
		}

		return new Pair<>(GuiUtil.makeGrid2(ret), mapX);
	}



}

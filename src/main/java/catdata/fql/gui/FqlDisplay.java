package catdata.fql.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import catdata.fql.decl.SigExp.Var;
import com.google.common.base.Function;

import catdata.Pair;
import catdata.Unit;
import catdata.fql.FQLException;
import catdata.fql.FqlOptions;
import catdata.fql.decl.FQLProgram;
import catdata.fql.decl.FqlEnvironment;
import catdata.fql.decl.FullQuery;
import catdata.fql.decl.FullQueryExp;
import catdata.fql.decl.InstExp;
import catdata.fql.decl.InstExp.Const;
import catdata.fql.decl.InstExp.Delta;
import catdata.fql.decl.InstExp.Eval;
import catdata.fql.decl.InstExp.Exp;
import catdata.fql.decl.InstExp.External;
import catdata.fql.decl.InstExp.FullEval;
import catdata.fql.decl.InstExp.FullSigma;
import catdata.fql.decl.InstExp.InstExpVisitor;
import catdata.fql.decl.InstExp.Kernel;
import catdata.fql.decl.InstExp.One;
import catdata.fql.decl.InstExp.Pi;
import catdata.fql.decl.InstExp.Plus;
import catdata.fql.decl.InstExp.Relationalize;
import catdata.fql.decl.InstExp.Sigma;
import catdata.fql.decl.InstExp.Step;
import catdata.fql.decl.InstExp.Times;
import catdata.fql.decl.InstExp.Two;
import catdata.fql.decl.InstExp.Zero;
import catdata.fql.decl.Instance;
import catdata.fql.decl.MapExp;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Query;
import catdata.fql.decl.QueryExp;
import catdata.fql.decl.SigExp;
import catdata.fql.decl.Signature;
import catdata.fql.decl.Transform;
import catdata.fql.decl.Unresolver;
import catdata.ide.Disp;
import catdata.ide.DefunctGlobalOptions;
import catdata.ide.Split;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

/**
 *
 * @author ryan
 *
 *         Class for showing all the viewers.
 */
public class FqlDisplay implements Disp {

	private final List<Pair<String, JComponent>> frames = new LinkedList<>();

	private static JPanel showInst(String c, Color clr, Instance view) throws FQLException {
		JTabbedPane px = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fql.inst_graphical) {
			JPanel gp = view.pretty(clr);
			px.add("Graphical", gp);
		}

		if (DefunctGlobalOptions.debug.fql.inst_textual) {
			JPanel ta = view.text();
			px.add("Textual", ta);
		}

		if (DefunctGlobalOptions.debug.fql.inst_tabular) {
			JPanel tp = view.view();
			px.add("Tabular", tp);
		}

		if (DefunctGlobalOptions.debug.fql.inst_joined) {
			JPanel joined = view.join();
			px.add("Joined", joined);
		}

		if (DefunctGlobalOptions.debug.fql.inst_gr || DefunctGlobalOptions.debug.fql.inst_dot) {
			Pair<JPanel, JPanel> groth = view.groth(c, clr);
			if (DefunctGlobalOptions.debug.fql.inst_gr) {
				px.add("Elements", groth.first);
			}
			if (DefunctGlobalOptions.debug.fql.inst_dot) {
				px.add("Dot", groth.second);
			}
		}

		if (DefunctGlobalOptions.debug.fql.inst_obs) {
			JPanel rel = view.observables2();
			px.add("Observables", rel);
		}

		if (DefunctGlobalOptions.debug.fql.inst_rdf) {
			JPanel rel = view.rdf(c);
			px.add("RDF", rel);
		}

		if (DefunctGlobalOptions.debug.fql.inst_adom) {
			JPanel rel = view.adom();
			px.add("Adom", rel);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);

		return top;

	}

	private static JPanel showMapping(FqlEnvironment environment, Color scolor, Color tcolor, Mapping view) {

		JTabbedPane px = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fql.mapping_graphical) {
			JPanel gp = view.pretty(scolor, tcolor, environment);
			px.add("Graphical", gp);
		}

		if (DefunctGlobalOptions.debug.fql.mapping_textual) {
			JPanel ta = view.text();
			px.add("Textual", ta);
		}

		if (DefunctGlobalOptions.debug.fql.mapping_tabular) {
			JPanel tp = view.view();
			px.add("Tabular", tp);
		}

		if (DefunctGlobalOptions.debug.fql.mapping_ed) {
			JPanel map = view.constraint();
			px.add("ED", map);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	private static JPanel showTransform(Color scolor, Color tcolor, @SuppressWarnings("unused") FqlEnvironment environment,
                                        String src_n, String dst_n, Transform view)  {
		JTabbedPane px = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fql.transform_graphical) {
			JPanel gp = view.graphical(scolor, tcolor, src_n, dst_n);
			px.add("Graphical", gp);
		}

		if (DefunctGlobalOptions.debug.fql.transform_textual) {
			JPanel ta = view.text();
			px.add("Textual", ta);
		}

		if (DefunctGlobalOptions.debug.fql.transform_tabular) {
			JPanel tp = view.view(src_n, dst_n);
			px.add("Tabular", tp);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	private static JPanel showSchema(String name, @SuppressWarnings("unused") FqlEnvironment environment, Color clr, Signature view)
			 {
		JTabbedPane px = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fql.schema_graphical) {
			JComponent gp = view.pretty(clr);
			px.add("Graphical", gp);
		}

		if (DefunctGlobalOptions.debug.fql.schema_textual) {
			JPanel ta = view.text();
			px.add("Textual", ta);
		}

		if (DefunctGlobalOptions.debug.fql.schema_tabular) {
			JPanel tp = view.view();
			px.add("Tabular", tp);
		}

		if (DefunctGlobalOptions.debug.fql.schema_ed) {
			JPanel map = view.constraint();
			px.add("ED", map);
		}

		if (DefunctGlobalOptions.debug.fql.schema_denotation) {
			JPanel den = view.denotation();
			px.add("Denotation", den);
		}

		if (DefunctGlobalOptions.debug.fql.schema_rdf) {
			JPanel rel = view.rdf();
			px.add("OWL", rel);
		}

		if (DefunctGlobalOptions.debug.fql.schema_dot) {
			JPanel dot = view.dot(name);
			px.addTab("Dot", dot);
		}

		if (DefunctGlobalOptions.debug.fql.schema_check) {
			JPanel chk = view.chk();
			px.addTab("Check", chk);
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	private static JPanel showFullQuery(FQLProgram p, @SuppressWarnings("unused") FqlEnvironment env, FullQuery view, FullQueryExp x) {
		JTabbedPane px = new JTabbedPane();

		JTextArea area = new JTextArea(x.printNicely(p));

		if (DefunctGlobalOptions.debug.fql.query_graphical) {
			px.add("Graphical", view.pretty());
		}
		if (DefunctGlobalOptions.debug.fql.query_textual) {
			px.add("Text", new JScrollPane(area));
		}

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	private static JPanel showQuery(FQLProgram prog, FqlEnvironment environment, Query view)
		 {
		JTabbedPane px = new JTabbedPane();

		Mapping d = view.project;
		Mapping p = view.join;
		Mapping u = view.union;
		Signature s = d.target;
		Signature i1 = d.source;
		Signature i2 = p.target;
		Signature t = u.target;

		px.add("Source", showSchema("Source", environment, prog.smap(s.toConst()), s));
		px.add("Delta",
				showMapping(environment, prog.smap(i1.toConst()), prog.smap(s.toConst()), d));
		px.add("Intermediate 1", showSchema("Int1", environment, prog.smap(i1.toConst()), i1));
		px.add("Pi", showMapping(environment, prog.smap(i1.toConst()), prog.smap(i2.toConst()), p));
		px.add("Intermediate 2", showSchema("Int2", environment, prog.smap(i2.toConst()), i2));
		px.add("Sigma",
				showMapping(environment, prog.smap(i2.toConst()), prog.smap(t.toConst()), u));
		px.add("Target", showSchema("Target", environment, prog.smap(t.toConst()), t));

		JPanel top = new JPanel(new GridLayout(1, 1));
		top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		top.add(px);
		return top;
	}

	private final FQLProgram prog;
	private final FqlEnvironment env;

	public FqlDisplay(String title, FQLProgram p, FqlEnvironment environment, long start,
			long middle) {
		long end = System.currentTimeMillis();
		int c1 = (int) ((middle - start) / (1000f));
		int c2 = (int) ((end - middle) / (1000f));

        prog = p;
        env = environment;
		p.doColors();

		try {
			for (String c : p.order) {
				if (environment.signatures.get(c) != null) {
					frames.add(new Pair<>("schema " + c, showSchema(c,
							environment, p.nmap.get(c), environment.getSchema(c))));
				} else if (environment.mappings.get(c) != null) {
					Pair<SigExp, SigExp> xxx = p.maps.get(c).type(p);
					String a = xxx.first.accept(p.sigs, new Unresolver()).toString();
					String b = xxx.second.accept(p.sigs, new Unresolver()).toString();
					frames.add(new Pair<>(
							"mapping " + c + " : " + a + " -> " + b, showMapping(environment,
									prog.smap(xxx.first), prog.smap(xxx.second),
									environment.getMapping(c))));
				} else if (environment.instances.get(c) != null) {
					String xxx = p.insts.get(c).type(p).accept(p.sigs, new Unresolver()).toString();
					frames.add(new Pair<>("instance " + c + " : " + xxx,
							showInst(c, p.nmap.get(c), environment.instances.get(c))));
				} else if (environment.queries.get(c) != null) {
					Pair<SigExp, SigExp> xxx = p.queries.get(c).type(p);
					String a = xxx.first.accept(p.sigs, new Unresolver()).toString();
					String b = xxx.second.accept(p.sigs, new Unresolver()).toString();
					frames.add(new Pair<>("query " + c + " : " + a + " -> " + b,
							showQuery(prog, environment, environment.queries.get(c))));
				} else if (environment.transforms.get(c) != null) {
					Pair<String, String> xxx = p.transforms.get(c).type(p);
					frames.add(new Pair<>("transform " + c + " : " + xxx.first
							+ " -> " + xxx.second, showTransform(prog.nmap.get(xxx.first),
							prog.nmap.get(xxx.second), environment, xxx.first, xxx.second,
							environment.transforms.get(c))));
				}  else if (p.full_queries.get(c) != null) {
					Pair<SigExp, SigExp> xxx = p.full_queries.get(c).type(p);
					String a = xxx.first.accept(p.sigs, new Unresolver()).toString();
					String b = xxx.second.accept(p.sigs, new Unresolver()).toString();
					FullQuery view = env.full_queries.get(c);
					FullQueryExp x = p.full_queries.get(c);

					frames.add(new Pair<>("QUERY " + c + " : " + a + " -> " + b,
							showFullQuery(p, environment, view, x)));
				} else {
					if (!DefunctGlobalOptions.debug.fql.continue_on_error) {
						throw new RuntimeException("Not found: " + c);
					}
				}
			}
		} catch (FQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex.getMessage());
		}
		display(title + " | (exec: " + c1 + "s)(gui: " + c2 + "s)", p.order);
	}

	private JFrame frame = null;
	private String name;

	private final CardLayout cl = new CardLayout();
	private final JPanel x = new JPanel(cl);
	private final JList<String> yyy = new JList<>();
	private final Map<String, String> indices = new HashMap<>();

	private void display(String s, List<String> order) {
		frame = new JFrame();
        name = s;

		Vector<String> ooo = new Vector<>();
		int index = 0;
		for (Pair<String, JComponent> p : frames) {
			x.add(p.second, p.first);
			ooo.add(p.first);
			indices.put(order.get(index++), p.first);
		}
		x.add(new JPanel(), "blank");
		cl.show(x, "blank");

		yyy.setListData(ooo);
		JPanel temp1 = new JPanel(new GridLayout(1, 1));
		temp1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
				"Select:"));
		JScrollPane yyy1 = new JScrollPane(yyy);
		temp1.add(yyy1);
		temp1.setMinimumSize(new Dimension(10, 10));
		yyy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		yyy.addListSelectionListener((ListSelectionEvent e) -> {
                    int i = yyy.getSelectedIndex();
                    if (i == -1) {
                        cl.show(x, "blank");
                    } else {
                        cl.show(x, ooo.get(i));
                    }
                });

		JPanel north = new JPanel(new GridLayout(2, 1));
		JButton instanceFlowButton = new JButton("Instance Dependence Graph");
		JButton schemaFlowButton = new JButton("Schema Mapping Graph");
		instanceFlowButton.setMinimumSize(new Dimension(10, 10));
		schemaFlowButton.setMinimumSize(new Dimension(10, 10));

		north.add(instanceFlowButton);
		instanceFlowButton.addActionListener((ActionEvent e) -> showInstanceFlow(prog));
		north.add(schemaFlowButton);
		schemaFlowButton.addActionListener((ActionEvent e) -> showSchemaFlow());
		Split px = new Split(.5, JSplitPane.HORIZONTAL_SPLIT);
		px.setDividerSize(6);
		px.setDividerLocation(220);
		frame = new JFrame(/* "Viewer for " + */s);

		JSplitPane temp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		temp2.setResizeWeight(1);
		temp2.setDividerSize(0);
		temp2.setBorder(BorderFactory.createEmptyBorder());
		temp2.add(temp1);
		temp2.add(north);

		px.add(temp2);

		px.add(x);

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

	private void showInstanceFlow(FQLProgram prog) {
		JFrame f = new JFrame();

		ActionListener escListener = (ActionEvent e) -> f.dispose();
		f.getRootPane().registerKeyboardAction(escListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
		KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
		f.getRootPane().registerKeyboardAction(escListener, ctrlW,
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		f.getRootPane().registerKeyboardAction(escListener, commandW,
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		Graph<String, Object> g = prog.build;
		if (g.getVertexCount() == 0) {
			f.add(new JPanel());
		} else {
			f.add(doView(g));
		}
		f.setSize(600, 540);
		f.setTitle("Instance Dependence Graph for " + name);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	private void showSchemaFlow() {
		JFrame f = new JFrame();

		ActionListener escListener = (ActionEvent e) -> f.dispose();
		f.getRootPane().registerKeyboardAction(escListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
		KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
		f.getRootPane().registerKeyboardAction(escListener, ctrlW,
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		f.getRootPane().registerKeyboardAction(escListener, commandW,
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		Graph<String, Object> g = prog.build2;
		if (g.getVertexCount() == 0) {
			f.add(new JPanel());
		} else {
			f.add(doView2(g));
		}
		f.setSize(600, 540);
		f.setTitle("Schema Mapping Graph for " + name);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}



	@SuppressWarnings({ "unchecked" })
    private JComponent doView(Graph<String, Object> sgv) {

		try {
			Class<?> c = Class
					.forName(FqlOptions.layout_prefix + DefunctGlobalOptions.debug.fql.instFlow_graph);
			Constructor<?> x = c.getConstructor(Graph.class);
			Layout<String, Object> layout = (Layout<String, Object>) x.newInstance(sgv);

			layout.setSize(new Dimension(600, 540));
			VisualizationViewer<String, Object> vv = new VisualizationViewer<>(layout);
			Function<String, Paint> vertexPaint = prog.nmap::get;
			DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
			gm.setMode(Mode.TRANSFORMING);
			vv.setGraphMouse(gm);
			gm.setMode(Mode.PICKING);
			vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

			vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
			vv.getRenderContext().setEdgeLabelTransformer((Object arg0) -> ((Pair<?, ?>) arg0).second.toString());

			vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
                            if (e.getStateChange() != ItemEvent.SELECTED) {
                                return;
                            }
                            vv.getPickedEdgeState().clear();
                            String str = ((String) e.getItem());
                            yyy.setSelectedValue(indices.get(str), true);
                        });

			vv.getPickedEdgeState().addItemListener((ItemEvent e) -> {
                            if (e.getStateChange() != ItemEvent.SELECTED) {
                                return;
                            }
                            vv.getPickedVertexState().clear();
                            Object o = ((Pair<?, ?>) e.getItem()).second;
                            handleInstanceFlowEdge(o);
                        });

			vv.getRenderContext().setLabelOffset(20);
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

	private final Set<String> extraInsts = new HashSet<>();
	private void handleInstanceFlowEdge(Object o) {
		InstExp i = (InstExp) o;
		Object f = i.accept(Unit.unit,
		 new InstExpVisitor<Object, Unit>() {
			@Override
			public MapExp visit(Unit env, Zero e) {
				return null;
			}

			@Override
			public MapExp visit(Unit env, One e) {
				return null;
			}

			@Override
			public MapExp visit(Unit env, Two e) {
				throw new RuntimeException();
			}

			@Override
			public MapExp visit(Unit env, Plus e) {
				return null;
			}

			@Override
			public MapExp visit(Unit env, Times e) {
				return null;
			}

			@Override
			public MapExp visit(Unit env, Exp e) {
				throw new RuntimeException();
			}

			@Override
			public MapExp visit(Unit env, Const e) {
				return null;
			}

			@Override
			public MapExp visit(Unit env, Delta e) {
				return e.F;
			}

			@Override
			public MapExp visit(Unit env, Sigma e) {
				return e.F;
			}

			@Override
			public MapExp visit(Unit env, Pi e) {
				return e.F;
			}

			@Override
			public MapExp visit(Unit env, FullSigma e) {
				return e.F;
			}

			@Override
			public Unit visit(Unit env, Relationalize e) {
				return null;
			}

			@Override
			public Unit visit(Unit env, External e) {
				return null;
			}

			@Override
			public Object visit(Unit env, Eval e) {
				return e.q;
			}

			@Override
			public Object visit(Unit env, FullEval e) {
				return e.q;
			}

			@Override
			public Object visit(Unit env, Kernel e) {
				return null;
			}

			@Override
			public Object visit(Unit env, Step e) {
				return null; // (Step) this should return a pair
			}

		});
		if (f == null) {
			return;
		}
		if (f instanceof QueryExp) {
			QueryExp q = (QueryExp) f;
			if (q instanceof QueryExp.Var) {
				QueryExp.Var qq = (QueryExp.Var) q;
				yyy.setSelectedValue(indices.get(qq.v), true);
				return;
			}

			String k = FQLProgram.revLookup(prog.queries, q);
			if (k != null) {
				yyy.setSelectedValue(indices.get(k), true);
				return;
			}
			String str = q.toString();
			if (!extraInsts.contains(str)) {
				Query view = q.toQuery(prog);
				JPanel p = showQuery(prog, env, view);
				x.add(p, str);
				extraInsts.add(str);
			}
			yyy.clearSelection();
			cl.show(x, str);
		} else if (f instanceof FullQueryExp) {
			FullQueryExp q = (FullQueryExp) f;
			if (q instanceof FullQueryExp.Var) {
				FullQueryExp.Var qq = (FullQueryExp.Var) q;
				yyy.setSelectedValue(indices.get(qq.v), true);
				return;
			}

			String k = FQLProgram.revLookup(prog.full_queries, q);
			if (k != null) {
				yyy.setSelectedValue(indices.get(k), true);
				return;
			}
			String str = q.toString();
			if (!extraInsts.contains(str)) {
				FullQuery view = q.toFullQuery(prog);
				JPanel p = showFullQuery(prog, env, view, q);
				x.add(p, str);
				extraInsts.add(str);
			}
			yyy.clearSelection();
			cl.show(x, str);
		} else if (f instanceof MapExp) {
			MapExp q = (MapExp) f;
			if (q instanceof MapExp.Var) {
				MapExp.Var qq = (MapExp.Var) q;
				yyy.setSelectedValue(indices.get(qq.v), true);
				return;
			}

			String k = FQLProgram.revLookup(prog.maps, q);
			if (k != null) {
				yyy.setSelectedValue(indices.get(k), true);
				return;
			}
			String str = q.toString();
			if (!extraInsts.contains(str)) {
				Mapping view = q.toMap(prog);
					JPanel p = showMapping(env, prog.smap(view.source.toConst()),
					prog.smap(view.target.toConst()), view);
					x.add(p, str);
					extraInsts.add(str);
			}
			yyy.clearSelection();
			cl.show(x, str);
		} else {
			throw new RuntimeException();
		}

	}

	@SuppressWarnings("unchecked")
    private JComponent doView2(Graph<String, Object> sgv) {
		try {
			Class<?> c = Class.forName(FqlOptions.layout_prefix + DefunctGlobalOptions.debug.fql.schFlow_graph);
			Constructor<?> x = c.getConstructor(Graph.class);
			Layout<String, Object> layout = (Layout<String, Object>) x.newInstance(sgv);
			layout.setSize(new Dimension(600, 540));
			VisualizationViewer<String, Object> vv = new VisualizationViewer<>(layout);
			Function<String, Paint> vertexPaint = (String i) -> prog.smap(new Var(i));
			DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
			gm.setMode(Mode.TRANSFORMING);
			vv.setGraphMouse(gm);
			gm.setMode(Mode.PICKING);

			vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
			vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
			vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());

			vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
                            if (e.getStateChange() != ItemEvent.SELECTED) {
                                return;
                            }
                            vv.getPickedEdgeState().clear();
                            String str = ((String) e.getItem());
                            yyy.setSelectedValue(indices.get(str), true);
                        });

			vv.getPickedEdgeState().addItemListener((ItemEvent e) -> {
                            if (e.getStateChange() != ItemEvent.SELECTED) {
                                return;
                            }
                            vv.getPickedVertexState().clear();
                            String str = ((String) e.getItem());
                            yyy.setSelectedValue(indices.get(str), true);
                        });

			vv.getRenderContext().setLabelOffset(20);

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

	@Override
	public void close() {
		if (frame == null) {
			return;
		}
		frame.setVisible(false);
		frame.dispose();
		frame = null;
	}


}

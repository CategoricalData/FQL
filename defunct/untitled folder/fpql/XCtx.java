package catdata.fpql;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.fpql.XExp.XInst;
import catdata.fpql.XExp.XSchema;
import catdata.fqlpp.cat.Category;
import catdata.ide.CodeTextPanel;
import catdata.ide.DefunctGlobalOptions;
import catdata.ide.GuiUtil;
import catdata.provers.ThueSlow;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;


public class XCtx<C> implements XObject {

	String toStringX() {
		String rec1 = schema == null ? "null" : schema.toStringX();
		String rec2 = global == null ? "null" : global.toStringX();
		return "[[[" + ids + " ||| " + types + " ||| " + eqs + " ||| " + rec1 + " ||| " + rec2 + "]]]";
	}

	private Set<C> hom(C src, C dst, Set<C> set) {
		Set<C> ret = new HashSet<>();
		for (C c : set) {
			Pair<C, C> t = type(c);
			if (t.first.equals(src) && t.second.equals(dst)) {
				ret.add(c);
			}
		}
		return ret;
	}

	public Set<C> localhom(C src, C dst) {
		return hom(src, dst, terms());
	}
	public Set<C> allHom(C src, C dst) {
		return hom(src, dst, allTerms());
	}

	public boolean saturated = false;

	private ThueSlow<C, Unit> kb;
	final Set<C> ids;
	final XCtx<C> global;
	final XCtx<C> schema;
	final Map<C, Pair<C, C>> types;
	Set<Pair<List<C>, List<C>>> eqs;
	private boolean initialized = false;

	private boolean shouldAbbreviate = false;

	private String abbrPrint(List<?> l) {
		if (!shouldAbbreviate) {
			return Util.sep(l, ".");
		}
		List<Object> r = l.stream().map(x -> {
			if (x instanceof Pair) {
				return ((Pair<?,?>)x).first;
			}
			return x;
		}).collect(Collectors.toList());
		return Util.sep(r, ".");
	}

	private String kind = "TODO";

	@Override
	public String kind() {
		return kind;
	}

	public Set<C> terms() {
		return types.keySet();
	}

	public Set<C> allIds() {
		Set<C> ret = new HashSet<>(ids);
		if (schema != null) {
			ret.addAll(schema.ids);
		}
		if (global != null) {
			ret.addAll(global.ids);
		}
		return ret;
	}

	public Set<C> allTerms() {
		Set<C> ret = new HashSet<>(terms());
		if (schema != null) {
			ret.addAll(schema.terms());
		}
		if (global != null) {
			ret.addAll(global.terms());
		}
		return ret;
	}

	public Set<Pair<List<C>, List<C>>> allEqs() {
		Set<Pair<List<C>, List<C>>> ret = new HashSet<>(eqs);
		if (schema != null) {
			ret.addAll(schema.eqs);
		}
		if (global != null) {
			ret.addAll(global.eqs);
		}
		return ret;
	}

	private Pair<C, C> typeWith0(C c, Map<C, C> m) {
		C x = m.get(c);
		if (x != null) {
			if (!ids.contains(x) && !x.equals("DOM")) {
				throw new RuntimeException("Bad entity: " + x);
			}
			@SuppressWarnings("unchecked")
			C ccc = (C) "_1";
			return new Pair<>(ccc, x);
		}
		Pair<C, C> ret = types.get(c);
		if (ret != null) {
			return ret;
		}
		if (schema != null) {
			return schema.type(c);
		}
		if (global != null) {
			return global.type(c);
		}

		throw new RuntimeException("Cannot type " + c); // don't toString here,
		// since used by
		// expand()
	}

	public Pair<C, C> type(C c) {
		Pair<C, C> ret = types.get(c);
		if (ret != null) {
			return ret;
		}
		if (schema != null) {
			return schema.type(c);
		}
		if (global != null) {
			return global.type(c);
		}
		throw new RuntimeException("Cannot type " + c); // don't toString here,
														// since used by
														// expand()
	}

	// public XCtx<C> copy() {
	// return new XCtx<>(new HashSet<>(ids), new HashSet<>(consts), new
	// HashMap<>(typeOf),
	// new HashSet<>(eqs), new HashSet<>(local));
	// throw new RuntimeException();
	// }

	public static <C> XCtx<C> empty_global() {
		Set<C> i = new HashSet<>();
		Map<C, Pair<C, C>> t = new HashMap<>();

		@SuppressWarnings("unchecked")
		C ccc = (C) "_1";
		i.add(ccc);
		t.put(ccc, new Pair<>(ccc, ccc));

		return new XCtx<>(i, t, new HashSet<>(), null, null, "schema");
	}

	public static <C> XCtx<C> empty_schema() {
		return new XCtx<>(new HashSet<>(), new HashMap<>(), new HashSet<>(), empty_global(), null, "schema");

	}

	public XCtx(Set<C> ids, Map<C, Pair<C, C>> types, Set<Pair<List<C>, List<C>>> eqs,
			XCtx<C> global, XCtx<C> schema, String kind) {
		this.types = new HashMap<>(types);
		this.eqs = new HashSet<>(eqs);
		this.ids = new HashSet<>(ids);
		this.global = global;
		this.schema = schema;
		if (schema != null) {
			for (Object o : types.keySet()) {
				if (schema.types.keySet().contains(o)) {
					throw new RuntimeException("Attempt to shadow name " + o);
				}
			}
		}
		if (global != null) {
			for (Object o : types.keySet()) {
				if (global.types.keySet().contains(o)) {
					throw new RuntimeException("Attempt to shadow name " + o);
				}
			}
		}
	//	sane();
		init();
	//	sane();
		this.kind = kind;
	}

	public ThueSlow<C, Unit> getKB() {
		// init();
		return kb;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void init() {
	//	sane();
		if (initialized) {
			return;
		}
		validate(true);
	//	sane();
		for (C c : ids) {
			types.put((C) ("!_" + c), new Pair<>(c, (C) "_1"));
		}
		List lhs = new LinkedList<>();
		lhs.add("_1");
		List rhs = new LinkedList<>();
		rhs.add("!_" + "_1");
		eqs.add(new Pair<>(lhs, rhs));
	//	sane();
		for (C c : terms()) {
			Pair<C, C> t = types.get(c);
			lhs = new LinkedList<>();
			lhs.add(c);
			lhs.add("!_" + t.second);
			rhs = new LinkedList<>();
			rhs.add("!_" + t.first);
			eqs.add(new Pair<>(lhs, rhs));
		}
	//	sane();
		validate(false);
	//	sane();
		kb();
	//	sane();
		initialized = true;
	}

	//this will destroy terms, so must copy
	private void kb() {
		List<Pair<List<C>, List<C>>> rules = new LinkedList<>();
		for (Pair<List<C>, List<C>> eq : allEqs()) {
			rules.add(new Pair<>(new LinkedList<>(eq.first), new LinkedList<>(eq.second)));
		}
		for (C id : allIds()) {
			List<C> l = new LinkedList<>();
			l.add(id);
			rules.add(new Pair<>(l, new LinkedList<>()));
		}
		kb = new ThueSlow<>(rules);
	}

	private void validate(boolean initial) {
		//sane();
		if (!types.keySet().containsAll(ids)) {
			throw new RuntimeException("ids not contained in const");
		}
		Set<C> values = types.values().stream().flatMap(x -> {
			Set<C> ret = new HashSet<>();
			ret.add(x.first);
			ret.add(x.second);
			return ret.stream();
		}).collect(Collectors.toSet());
		if (!allIds().containsAll(values)) {
			values.removeAll(allIds());
			throw new RuntimeException("typeof returns non-ids: " + values + " in " + this);
		}
		for (C c : ids) {
			Pair<C, C> t = types.get(c);
			if (!t.first.equals(t.second)) {
				throw new RuntimeException("Not identity " + c);
			}
		}

		for (Pair<List<C>, List<C>> eq : eqs) {
			if (!allTerms().containsAll(eq.first)) {
				if (!initial || !eq.first.toString().contains("!")) {
					throw new RuntimeException("unknown const in: " + Util.sep(eq.first, ".") + " in " + this
							+ " (first)");
				}
			}
			if (!allTerms().containsAll(eq.second)) {
				if (!initial || !eq.second.toString().contains("!")) {
					throw new RuntimeException("unknown const in: " + Util.sep(eq.second, ".") + " in " + this
							+ " (second)");
				}
			}
			if (!initial
					|| (!eq.second.toString().contains("!") && !eq.first.toString().contains("!"))) {
				if (!type(eq.first).equals(type(eq.second))) {
					throw new RuntimeException("Type mismatch on equation " + Util.sep(eq.first,".") + " : " +  type(eq.first).first + " -> " + type(eq.first).second
							+ " = " +
							Util.sep(eq.second, ".") + " : " + type(eq.second).first + " -> " + type(eq.second).second);
				}
			}
		}

	//	sane();
	}




	public Pair<C, C> type(List<C> first) {
		if (first.isEmpty()) {
			throw new RuntimeException("Empty");
		}
		Iterator<C> it = first.iterator();
		Pair<C, C> ret = type(it.next());
		while (it.hasNext()) {
			Pair<C, C> next = type(it.next());
			if (!ret.second.equals(next.first)) {
				throw new RuntimeException("Ill-typed: " + Util.sep(first, ".") + " in " + this);
			}
			ret = new Pair<>(ret.first, next.second);
		}
		return ret;
	}

	public Pair<C,C> typeWith(List<C> first, Map<C, C> ctx) {
		if (first.isEmpty()) {
			throw new RuntimeException("Empty");
		}
		Iterator<C> it = first.iterator();
		Pair<C, C> ret = typeWith0(it.next(), ctx);
		while (it.hasNext()) {
			Pair<C, C> next = typeWith0(it.next(), ctx);
			if (!ret.second.equals(next.first)) {
				throw new RuntimeException("Ill-typed: " + Util.sep(first, ".") + " in " + this);
			}
			ret = new Pair<>(ret.first, next.second);
		}
		return ret;
	}



	private String toString = null;

	@Override
	public String toString() {
		if (toString != null) {
			return toString;
		}
		String kb_text = "types:\n  " + Util.sep(allIds(), ",\n  ");
		List<String> tt = allTerms().stream()
				.map(x -> x + " : " + type(x).first + " -> " + type(x).second)
				.collect(Collectors.toList());
		kb_text = kb_text.trim();
		kb_text += "\n\nterms:\n  " + Util.sep(tt, ",\n  ");
		List<String> xx = allEqs().stream()
				.map(x -> Util.sep(x.first, ".") + " = " + Util.sep(x.second, "."))
				.collect(Collectors.toList());
		kb_text = kb_text.trim();
		kb_text += "\n\nequations:\n  " + Util.sep(xx, ",\n  ");
		kb_text = kb_text.trim();
		kb_text += "\n\nlocal: " + terms();

		toString = kb_text;
		return kb_text;
	}

	@Override
	public JComponent display() {
		init();

		JTabbedPane ret = new JTabbedPane();

		if (DefunctGlobalOptions.debug.fpql.x_text) {
			String kb_text = "types:\n  " + Util.sep(allIds(), ",\n  ");
			List<String> tms = allTerms().stream()
					.map(x -> x + " : " + type(x).first + " -> " + type(x).second)
					.collect(Collectors.toList());
			kb_text = kb_text.trim();
			kb_text += "\n\nterms:\n  " + Util.sep(tms, ",\n  ");
			List<String> xx = allEqs().stream()
					.map(x -> Util.sep(x.first, ".") + " = " + Util.sep(x.second, "."))
					.collect(Collectors.toList());
			kb_text = kb_text.trim();
			kb_text += "\n\nequations:\n  " + Util.sep(xx, ",\n  ");
			kb_text = kb_text.trim();

		//	try {
			//	kb.complete();
			//	kb_text += "\n\nKnuth-Bendix Completion:\n" + kb;
		//	} catch (Exception e) {
			//	e.printStackTrace();
			//	kb_text = "\n\nERROR in Knuth-Bendix\n\n" + e.getMessage();
			//}
			JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			//JPanel bot = new JPanel(new GridLayout(1,3));
		//	JTextField fff = new JTextField();
		//	JButton but = new JButton("Reduce");
			//JTextField ggg = new JTextField();
			//bot.add(fff);
			//bot.add(but);
			//bot.add(ggg);
		//	pane.setResizeWeight(1);
			// but.addActionListener(x -> {
			//	String s = fff.getText();
			//	List<C> l = XParser.path(s);
			//	try {
			//		ggg.setText(Util.sep(getKB().normalize("", l), "."));
			//	} catch (Exception ex) {
			//		ex.printStackTrace();
			//		ggg.setText(ex.getMessage());
			//	}
			//}); 
			JComponent kbc = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", kb_text);
			pane.add(kbc);
			//pane.add(bot);
			ret.addTab("Text", pane);
		}

		String cat = null;
		if (DefunctGlobalOptions.debug.fpql.x_cat) {
			try {
				cat = cat().toString();
			} catch (Exception e) {
				e.printStackTrace();
				cat = "ERROR\n\n" + e.getMessage();
			}
			JComponent ctp = new CodeTextPanel(BorderFactory.createEtchedBorder(), "", cat);
			ret.addTab("Category", ctp);
		}

		if (schema != null) {
			if (DefunctGlobalOptions.debug.fpql.x_tables) {
				// if category tab blew up, so should this
				JComponent tables = cat != null && cat.startsWith("ERROR") ? new CodeTextPanel(BorderFactory.createEtchedBorder(), "", cat) : makeTables(x -> cat().arrows(), new HashSet<>());
				ret.addTab("Full Tables", tables);
			}
			if (DefunctGlobalOptions.debug.fpql.x_adom) {
				ret.addTab("Adom Tables", makeTables(z -> foo(), global.ids));
			}
		}

		if (DefunctGlobalOptions.debug.fpql.x_graph) {
			ret.addTab("Graph", makeGraph(schema != null));
		}

		if (DefunctGlobalOptions.debug.fpql.x_graph && (schema != null)) {
			ret.addTab("Elements", elements());
		}

		if (DefunctGlobalOptions.debug.fpql.x_json) {
			String tj = toJSON();
			if (tj != null) {
				ret.addTab("JSON", new CodeTextPanel(BorderFactory.createEtchedBorder(), "", tj));
			}
		}

		return ret;
	}

	private JComponent makeGraph(boolean isInstance) {
		if (allTerms().size() > 128) {
			return new JTextArea("Too large to display");
		}
		Graph<C, C> sgv = buildFromSig();

		Layout<C, C> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<C, C> vv = new VisualizationViewer<>(layout);
		vv.getRenderContext().setLabelOffset(20);
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.PICKING); //was TRANSFORMING
		vv.setGraphMouse(gm);

		vv.getRenderContext().setVertexFillPaintTransformer(
			(C x) -> global.terms().contains(x) ? Color.RED : Color.GREEN );

		com.google.common.base.Function<C, String> ttt =
			(C arg0) -> {
				String ret = arg0.toString();
				return (ret.length() > 40)
					? ret.substring(0, 39) + "..."
					: ret;
			};

		vv.getRenderContext().setVertexLabelTransformer(ttt);
//		vv.getRenderer().setVertexRenderer(new MyRenderer());
		vv.getRenderContext().setEdgeLabelTransformer(ttt);

		vv.getPickedVertexState().addItemListener(e -> {
				if (e.getStateChange() != ItemEvent.SELECTED) {
					return;
				}
				vv.getPickedEdgeState().clear();
				//Object str = e.getItem();
				if (cl == null) {
                }
//				cl.show(clx, xgrid.get(str));

		});

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());

		if (isInstance && DefunctGlobalOptions.debug.fpql.x_tables && xcat != null) {
			JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			jsp.setResizeWeight(.8d); // setDividerLocation(.9d);
			jsp.setDividerSize(2);
			jsp.add(ret);
			jsp.add(clx);
			return jsp;
		} 
			return ret;
		

	}

	private Graph<C, C> buildFromSig() {
		Graph<C, C> g2 = new DirectedSparseMultigraph<>();
		for (C n : allIds()) {
			if (n.equals("_1")) {
				continue;
			}
			g2.addVertex(n);
		}
		for (C e : allTerms()) {
			if (allIds().contains(e)) {
				continue;
			}
			if (e.toString().startsWith("!")) {
				continue;
			}
			Pair<C,C> t = type(e);
			if (t.first.equals("_1") || t.second.equals("_1")) {
				continue;
			}
			g2.addEdge(e, type(e).first, type(e).second);
		}
		return g2;
	}

	// private Set<List<C>> hom(C src, C dst, int n);

	private final Map<Integer, Set<List<C>>> pathsUpTo = new HashMap<>();

	private Set<List<C>> pathsUpTo(int n) {
		if (pathsUpTo.containsKey(n)) {
			return pathsUpTo.get(n);
		}
		if (n == 0) {
			Set<List<C>> ret = new HashSet<>();
			for (C c : allIds()) {
				List<C> l = new LinkedList<>();
				l.add(c);
				ret.add(l);
			}
			pathsUpTo.put(0, ret);
			return ret;
		}
		Set<List<C>> set = new HashSet<>(pathsUpTo(n - 1));
		Set<List<C>> toAdd = new HashSet<>();
		for (List<C> l : set) {
			for (C e : outEdges(typeAsMap(), type(l).second)) {
				List<C> r = new LinkedList<>(l);
				r.add(e);
				toAdd.add(r);
			}
		}
		set.addAll(toAdd);
		pathsUpTo.put(n, set);
		return set;
	}

	private Map<C, Pair<C, C>> typeAsMap() {
		Map<C, Pair<C, C>> ret = new HashMap<>(types);
		if (global != null) {
			ret.putAll(global.types);
		}
		if (schema != null) {
			ret.putAll(schema.types);
		}
		return ret;
	}

	public Set<List<C>> pathsUpTo(int n, C src, C dst) {
		Set<List<C>> ret = new HashSet<>();
		for (List<C> l : pathsUpTo(n)) {
			Pair<C, C> t = type(l);
			if (t.first.equals(src) && t.second.equals(dst)) {
				ret.add(l);
			}
		}
		return ret;
	}

	private List<Triple<C, C, List<C>>> foo() {
		// try {

		List<Triple<C, C, List<C>>> paths = new LinkedList<>();
		for (C c : allIds()) {
			paths.add(new Triple<>(c, c, new LinkedList<>()));
		}

		List<Triple<C, C, List<C>>> consts = new LinkedList<>();
		for (C c : global.terms()) {
			if (!global.ids.contains(c)) {
				if (global.type(c).first.equals("_1")) {
					List<C> l = new LinkedList<>();
					l.add(c);
					consts.add(new Triple<>(global.type(c).first, global.type(c).second, l));
				}
			}
		}

		int iter = 0;
		for (; iter < DefunctGlobalOptions.debug.fpql.MAX_PATH_LENGTH; iter++) {
			Set<Triple<C, C, List<C>>> newPaths1 = extend2(paths, global.types, consts);
			paths.addAll(newPaths1);
			Set<Triple<C, C, List<C>>> newPaths2 = extend2(paths, schema.types, consts);
			paths.addAll(newPaths2);
			Set<Triple<C, C, List<C>>> newPaths3 = extend2(paths, types, consts);
			if (paths.containsAll(newPaths3)) {
				// need one more iteration for all attributes
				newPaths1 = extend2(paths, global.types, consts);
				paths.addAll(newPaths1);
				newPaths2 = extend2(paths, schema.types, consts);
				paths.addAll(newPaths2);
				break;
			}
			paths.addAll(newPaths3);
		}
		if (iter == DefunctGlobalOptions.debug.fpql.MAX_PATH_LENGTH) {
			throw new RuntimeException("Exceeded maximum path length");
		}

		return paths; // new FQLTextPanel(BorderFactory.createEtchedBorder(),
						// "", paths.toString());
		// } catch (Exception e) {
		// e.printStackTrace();
		// return new FQLTextPanel(BorderFactory.createEtchedBorder(), "",
		// "ERROR: " + e.getMessage());
		// }
	}

	private Set<Triple<C, C, List<C>>> extend2(List<Triple<C, C, List<C>>> paths,
			Map<C, Pair<C, C>> t, List<Triple<C, C, List<C>>> consts) {
		Set<Triple<C, C, List<C>>> newPaths = new HashSet<>();
		for (Triple<C, C, List<C>> p : paths) {
			for (C e : outEdges(t, p.second)) {
				List<C> p0 = new LinkedList<>(p.third);
				p0.add(e);
				Triple<C, C, List<C>> toAdd = new Triple<>(p.first, t.get(e).second, p0);
				Triple<C, C, List<C>> found = find_old(kb, toAdd, paths);
				if (found == null) {
					found = find_old(kb, toAdd, newPaths);
					if (found == null) {
						find_old(kb, toAdd, consts);
						newPaths.add(toAdd);
					}
				}
			}
		}
		return newPaths;
	}

/*	private JComponent bar() {
		try {
			String text = "";
			// List<Triple<C, C, List<C>>> consts = new LinkedList<>();

			Map<C, Set<List<C>>> arrs = new HashMap<>();
			Map<C, Set<List<C>>> consts = new HashMap<>();
			for (C c : schema.ids) {
				Set<List<C>> set = new HashSet<>();
				int i = 1;
				for (; i < DEBUG.debug.MAX_PATH_LENGTH; i++) {
					@SuppressWarnings("unchecked")
					C ccc = (C) "_1";
					Set<List<C>> cands = pathsUpTo(i, ccc, c);
					Set<List<C>> toAdd = new HashSet<>();
					for (List<C> cand : cands) {
						List<C> reduced = find2(getKB(), cand, set);
						if (reduced == null) {
							reduced = find2(getKB(), cand, toAdd);
							if (reduced == null) {
								toAdd.add(cand);
							}
						}
					}
					if (toAdd.isEmpty()) {
						break;
					}
					set.addAll(toAdd);
				}
				if (i == DEBUG.debug.MAX_PATH_LENGTH) {
					throw new RuntimeException("Exceeded max path length");
				}
				arrs.put(c, set);
			}

			for (C c : global.ids) {
				consts.put(c, new HashSet<>());
			}

			for (C c : global.terms()) {
				if (global.ids.contains(c)) {
					continue;
				}
				if (!type(c).first.equals("_1")) {
					continue;
				}
				Set<List<C>> set = consts.get(type(c).second);
				List<C> l = new LinkedList<>();
				l.add(c);
				set.add(l);
			}

			List<JComponent> grid = new LinkedList<>();

			// Map<C, Set<List<C>>> entities = new HashMap<>();
			Map<C, Set<C>> m = new HashMap<>();
			for (C c : allIds()) {
				// entities.put(c, new HashSet<>());
				m.put(c, new HashSet<>());
			}

			// for (Triple<C, C, List<C>> k : cat.arrows()) {
			// if (k.first.equals("1")) { // uncomment causes exception
			// Set<List<C>> set = entities.get(k.second);
			// set.add(k.third);
			// }
			// }

			// does column names
			for (C c : allTerms()) {
				Pair<C, C> t = type(c);
				Set<C> set = m.get(t.first);
				set.add(c);
			}

			List<C> keys = new LinkedList<>(m.keySet());
			keys.sort(new Comparator() {
				@Override
				public int compare(Object o1, Object o2) {
					return ((Comparable) o1).compareTo(o2);
				}
			});

			for (C c : keys) {
				if (global.ids.contains(c)) {
					continue;
				}
				Pair<C, C> t = type(c);
				Set<List<C>> src = arrs.get(t.first);
				if (src == null) {
					throw new RuntimeException("Nothing for " + t.first + " in " + arrs.keySet());
				}
				List<C> cols = new LinkedList<>(m.get(c));
				cols = cols.stream().filter(x -> !x.toString().startsWith("!"))
						.collect(Collectors.toList());

				Object[][] rowData = new Object[src.size()][cols.size()];
				int idx = cols.indexOf(c);
				if (idx != 0) {
					C old = cols.get(0);
					cols.set(0, c);
					cols.set(idx, old);
				}
				List<String> colNames3 = cols
						.stream()
						.map(x -> type(x).second.equals(x) ? x.toString() : x + " ("
								+ type(x).second + ")").collect(Collectors.toList());
				Object[] colNames = colNames3.toArray();

				int row = 0;
				for (List<C> l : src) {
					rowData[row][0] = l;
					int cl = 0;
					for (C col : cols) {
						List<C> r = new LinkedList<>(l);
						r.add(col);
						if (arrs.containsKey(type(col).second)) {
							for (List<C> cand : arrs.get(type(col).second)) {
								if (kb.equiv(cand, r)) {
									rowData[row][cl] = abbrPrint(cand);
									break;
								}
							}
						} else {
							boolean found = false;
							for (List<C> cand : consts.get(type(col).second)) {
								if (kb.equiv(cand, r)) {
									rowData[row][cl] = abbrPrint(cand);
									break;
								}
							}
							if (!found) {
								consts.get(type(col).second).add(r);
								rowData[row][cl] = abbrPrint(r);
							}
						}
						cl++;
					}
					row++;
				}
				JPanel table = GuiUtil.makeTable(BorderFactory.createEtchedBorder(),
						c + " (" + src.size() + ") rows", rowData, colNames);
				grid.add(table);
			}

			return GuiUtil.makeGrid(grid);

			// return new FQLTextPanel(BorderFactory.createEtchedBorder(), "",
			// text);
		} catch (Exception e) {
			e.printStackTrace();
			return new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "ERROR: "
					+ e.getMessage());
		}
	}
*/
/*	private List<C> find2(KB_ThueSlow<C> kb2, List<C> cand, Set<List<C>> set) {
		for (List<C> l : set) {
			if (kb2.equiv(cand, l)) {
				return l;
			}
		}
		return null;
	}*/

	private JComponent clx;
	private CardLayout cl;
//	private Map<C, String> xgrid;
//	public JComponent getGrid(C c) {
//		if (xgrid != null) {
//			return xgrid.get(c);
//		}
//		if (DEBUG.debug.x_tables) {
////			makeTables(x -> cat().arrows(), new HashSet<>());
//			return getGrid(c);
//		}
//		return new JPanel();
//	}


	//  have this suppress pair IDs when possible
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JComponent makeTables(Function<Unit, Collection<Triple<C, C, List<C>>>> fn,
			Set<C> ignore) {
		cl = new CardLayout();
	//	xgrid = new HashMap<>();
		clx = new JPanel();
		clx.setLayout(cl);
		clx.add(new JPanel(), "0");
		cl.show(clx, "0");
	//	xgrid.put((C)"_1", "0");

		int www = 1;
		try {
			// Category<C, Triple<C, C, List<C>>> cat = cat();
			List<JComponent> grid = new LinkedList<>();
			Collection<Triple<C, C, List<C>>> cat = fn.apply(new Unit());

			// Map<C, Set<List<C>>> entities = new HashMap<>();
			Map entities = new HashMap<>();
			Map<C, Set<C>> m = new HashMap<>();
			for (C c : allIds()) {
				entities.put(c, new HashSet<>());
				m.put(c, new HashSet<>());
			}

			for (Triple<C, C, List<C>> k : cat) {
				if (k.first.equals("_1")) { // uncomment causes exception
					Set set = (Set<List<C>>) entities.get(k.second);
					// set.add(k);
					set.add(k.third);
				}
			}

			for (C c : allTerms()) {
				Pair<C, C> t = type(c);
				Set<C> set = m.get(t.first);
				set.add(c);
			}

			List<C> keys = new LinkedList<>(m.keySet());
			keys.sort((Object o1, Object o2) ->
					 ((Comparable) o1).compareTo(o2));

			for (C c : keys) {
				if (c.equals("_1")) {
					continue;
				}
				if (ignore.contains(c)) {
					continue;
				}
				Pair<C, C> t = type(c);
				Set<List<C>> src = (Set<List<C>>) entities.get(t.first);
				List<C> cols = new LinkedList<>(m.get(c));
				cols = cols.stream().filter(x -> !x.toString().startsWith("!"))
						.collect(Collectors.toList());

				Object[][] rowData = new Object[src.size()][cols.size()];
				int idx = cols.indexOf(c);
				if (idx != -1) {
					C old = cols.get(0);
					cols.set(0, c);
					cols.set(idx, old);
					if (cols.size() > 1) {
						List<C> colsX = new LinkedList<>(cols.subList(1, cols.size()));
						colsX.sort(null);
						colsX.add(0, c);
						cols = colsX;
					}
				}

				List<String> colNames3 = cols
						.stream()
						.map(x -> type(x).second.equals(x) ? x.toString() : x + " ("
								+ type(x).second + ")").collect(Collectors.toList());
				Object[] colNames = colNames3.toArray();

				int row = 0;
				for (List<C> l : src) {
					rowData[row][0] = l;
					int cl = 0;
					for (C col : cols) {
						List<C> r = new LinkedList<>(l);
						r.add(col);
						for (Triple<C, C, List<C>> cand : cat) {
							if (!cand.first.equals("_1")) {
								continue;
							}
							if (!cand.second.equals(type(col).second)) {
								continue;
							}
							if (kb.equiv(cand.third, r)) {
								rowData[row][cl] = abbrPrint(cand.third);
								break;
							}
						}
						cl++;
					}
					row++;
				}
				JPanel table = GuiUtil.makeTable(BorderFactory.createEtchedBorder(),
						c + " (" + src.size() + ") rows", rowData, colNames);
				JPanel table2 = GuiUtil.makeTable(BorderFactory.createEtchedBorder(),
						c + " (" + src.size() + ") rows", rowData, colNames);
			//	xgrid.put(c, Integer.toString(www));
				clx.add(new JScrollPane(table2), Integer.toString(www));
				www++;
				grid.add(table);
			}

			return GuiUtil.makeGrid(grid);
		} catch (Exception e) {
			e.printStackTrace();
			return new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "ERROR\n\n"
					+ e.getMessage());
		}
	}

	private Category<C, Triple<C, C, List<C>>> xcat;

//	private Category<C, Triple<C, C, List<C>>> small_cat;

	private static <C> Set<C> outEdges(Map<C, Pair<C, C>> t, C p) {
		Set<C> ret = new HashSet<>();
		for (C c : t.keySet()) {
			if (c.equals(p)) {
				continue;
			}
			if (t.get(c).first.equals(p)) {
				ret.add(c);
			}
		}
		return ret;
	}

	private final Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> find_cache = new HashMap<>();

	public Triple<C, C, List<C>> find_fast(Triple<C, C, List<C>> tofind) {
		if (find_cache.containsKey(tofind)) {
			return find_cache.get(tofind);
		}
		Triple<C, C, List<C>> found = find_old(getKB(), tofind,
				cat().hom(tofind.first, tofind.second));
		find_cache.put(tofind, found);
		return found;
	}

	// test for inconsistency here?
	private static <D> Triple<D, D, List<D>> find_old(ThueSlow<D, Unit> kb, Triple<D, D, List<D>> tofind,
                                                      Collection<Triple<D, D, List<D>>> cat) {
		Set<Triple<D, D, List<D>>> ret = new HashSet<>();
		for (Triple<D, D, List<D>> arr : cat) {
			if (arr.first.equals(tofind.first) && arr.second.equals(tofind.second)) {
				if (kb.equiv(arr.third, tofind.third)) {
					ret.add(arr);
				}
			}
		}
		if (ret.isEmpty()) {
			return null;
		}
		if (ret.size() == 1) {
			for (Triple<D, D, List<D>> k : ret) {
				return k;
			}
		}

		ret.add(tofind);
		List<String> xxx = ret.stream().map(x -> {
			List<D> h = new LinkedList<>(x.third);
			h.add(0, x.first);
			return Util.sep(h, ".");
		}).collect(Collectors.toList());

		throw new RuntimeException("Inconsistent: " + Util.sep(xxx, "\n=\n"));

	}

	public Category<C, Triple<C, C, List<C>>> cat() {
		if (xcat != null) {
			return xcat;
		}

		if (saturated && DefunctGlobalOptions.debug.fpql.fast_amalgams) {
			xcat = satcat();
			return xcat;
		}

		List<Triple<C, C, List<C>>> paths = new LinkedList<>();
		LinkedHashMap<C, Pair<C, C>> t = new LinkedHashMap<>();

		List<Triple<C, C, List<C>>> consts = new LinkedList<>();

		if (global != null) {
			for (C c : global.ids) {
				paths.add(new Triple<>(c, c, new LinkedList<>()));
			}
			for (C c : global.terms()) {
				if (!global.ids.contains(c)) {
					if (global.type(c).first.equals("_1")) {
						List<C> l = new LinkedList<>();
						l.add(c);
						consts.add(new Triple<>(global.type(c).first, global.type(c).second, l));
					}
				}
			}
			t.putAll(global.types);
			extend(global.getKB(), paths, t, consts);
		}

		if (schema != null) {
			for (C c : schema.ids) {
				paths.add(new Triple<>(c, c, new LinkedList<>()));
			}
			t.putAll(schema.types);
			extend(schema.getKB(), paths, t, consts);
		}

		for (C c : ids) {
			paths.add(new Triple<>(c, c, new LinkedList<>()));
		}
		t.putAll(types);
		extend(getKB(), paths, t, consts);

		Set<Triple<C, C, List<C>>> arrows = new HashSet<>(paths);

		@SuppressWarnings("serial")
		Category<C, Triple<C, C, List<C>>> xcat2 = new Category<>() {

			@Override
			public Set<C> objects() {
				return allIds();
			}

			@Override
			public Set<Triple<C, C, List<C>>> arrows() {
				return arrows;
			}

			@Override
			public C source(Triple<C, C, List<C>> a) {
				return a.first;
			}

			@Override
			public C target(Triple<C, C, List<C>> a) {
				return a.second;
			}

			@Override
			public Triple<C, C, List<C>> identity(C o) {
				return new Triple<>(o, o, new LinkedList<>());
			}

			@Override
			public Triple<C, C, List<C>> compose(Triple<C, C, List<C>> a1, Triple<C, C, List<C>> a2) {
				Triple<C, C, List<C>> r = cache.get(new Pair<>(a1, a2));
				if (r != null) {
					return r;
				}
				List<C> ret = new LinkedList<>(a1.third);
				ret.addAll(a2.third);
				Triple<C, C, List<C>> xxx = new Triple<>(a1.first, a2.second, ret);
				Triple<C, C, List<C>> yyy = find_old(getKB(), xxx, hom(a1.first, a2.second));
				if (yyy == null) {
					throw new RuntimeException("Found nothing equivalent to " + ret + " in "
							+ arrows());
				}
				cache.put(new Pair<>(a1, a2), yyy);
				return yyy;
			}

			final Map<Pair<Triple<C, C, List<C>>, Triple<C, C, List<C>>>, Triple<C, C, List<C>>> cache = new HashMap<>();
		};

		if (DefunctGlobalOptions.debug.fpql.validate_amalgams) {
			xcat2.validate();
		}
		xcat = xcat2;

		return xcat;
	}

	private Map<Pair<C, C>, List<C>> eqm;

	private Category<C, Triple<C, C, List<C>>> satcat() {
		Category<C, Triple<C, C, List<C>>> sch = schema.cat();
		eqm = new HashMap<>();

		Set<Triple<C, C, List<C>>> new_arrs = new HashSet<>();
		for (C a : schema.allIds()) {
			for (C v : types.keySet()) {
				Pair<C, C> t = type(v);
				C b = t.second;
				if (b.equals("_1")) {
					continue;
				}
				List<C> l = new LinkedList<>();
				@SuppressWarnings("unchecked")
				C ccc = (C) ("!_" + a);
				l.add(ccc);
				l.add(v);
				Triple<C, C, List<C>> arr = new Triple<>(a, b, l);
				new_arrs.add(arr);
			}
		}

		Set<Triple<C, C, List<C>>> arrs = new HashSet<>();
		arrs.addAll(sch.arrows());
		arrs.addAll(new_arrs);

		Map<Pair<Triple<C, C, List<C>>, Triple<C, C, List<C>>>, Triple<C, C, List<C>>> comp_cache = new HashMap<>();

		@SuppressWarnings("serial")
		Category<C, Triple<C, C, List<C>>> ret = new Category<>() {
			@Override
			public Set<C> objects() {
				return sch.objects();
			}

			@Override
			public Set<Triple<C, C, List<C>>> arrows() {
				return arrs;
			}

			@Override
			public C source(Triple<C, C, List<C>> a) {
				return a.first;
			}

			@Override
			public C target(Triple<C, C, List<C>> a) {
				return a.second;
			}

			@Override
			public Triple<C, C, List<C>> identity(C o) {
				return sch.identity(o);
			}

			@Override
			public Triple<C, C, List<C>> compose(Triple<C, C, List<C>> f, Triple<C, C, List<C>> g) {
				Pair<Triple<C, C, List<C>>, Triple<C, C, List<C>>> p = new Pair<>(f, g);
				Triple<C, C, List<C>> ret = comp_cache.get(p);
				if (ret != null) {
					return ret;
				}
				ret = local_compose(f, g);
				comp_cache.put(p, ret);
				return ret;
			}

			@SuppressWarnings({"rawtypes", "unchecked",})
			private Triple<C, C, List<C>> local_compose(Triple<C, C, List<C>> f,
					Triple<C, C, List<C>> g) {
				if (!arrows().contains(f)) {
					throw new RuntimeException(f.toString());
				}
				if (!arrows().contains(g)) {
					throw new RuntimeException(g.toString());
				}
				if (!f.second.equals(g.first)) {
					throw new RuntimeException("cannot compose " + f + " and " + g);
				}
				if (sch.hom(f.first, f.second).contains(f)
						&& sch.hom(g.first, g.second).contains(g)) {
					return sch.compose(f, g);
				}
				if (new_arrs.contains(f) && new_arrs.contains(g)) {
					Pair<C, C> ft = new Pair<>(f.first, f.second);
					Pair<C, C> gt = new Pair<>(g.first, g.second);
					C a = ft.first;
					C b = gt.first;
			//		C v = f.third.get(1);
					C v0 = g.third.get(1);
					if (schema.allIds().contains(a) && !b.equals("_1")) {
						List<C> l = new LinkedList<>();
						C ccc = (C) ("!_" + a);
						l.add(ccc);
						l.add(v0);
						Triple<C, C, List<C>> ret = new Triple<>(a, type(v0).second, l);
						if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
							throw new RuntimeException();
						}
						if (!arrows().contains(ret)) {
							throw new RuntimeException(ret.toString());
						}
						return ret;
					}
				}
				if (new_arrs.contains(f) && sch.arrows().contains(g)) {
					if (g.third.isEmpty()) {
						if (!f.first.equals(g.first) || !f.second.equals(g.second)) {
							throw new RuntimeException();
						}
						if (!arrows().contains(f)) {
							throw new RuntimeException(f.toString());
						}
						return f;
					}
					//C b = g.first;
					C b0 = g.second;
					C a = f.first;
					C v = f.third.get(1);
					if (b0.equals("_1") && a.equals("_1")) {
						Triple ret = new Triple("_1", "_1", new LinkedList());
						if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
							throw new RuntimeException();
						}
						if (!arrows().contains(ret)) {
							throw new RuntimeException(ret.toString());
						}
						return ret;
					}
					if (b0.equals("_1") && !a.equals("_1")) {
						List l = new LinkedList();
						l.add("!_" + a);
						Triple ret = new Triple(a, "_1", l);
						if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
							throw new RuntimeException();
						}
						if (!arrows().contains(ret)) {
							throw new RuntimeException(ret.toString());
						}
						return ret;
					}
					if (g.third.get(0).toString().startsWith("!") && !a.equals("_1")) {
						List<C> l = new LinkedList();
						l.add((C) ("!_" + a));
						l.addAll(g.third.subList(1, g.third.size()));
						Triple<C, C, List<C>> ret = new Triple<>(a, g.second, l);
						ret = find_old(getKB(), ret, hom(ret.first, ret.second));
						if (ret == null) {
							throw new RuntimeException("Anomaly: please report");
						}
						if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
							throw new RuntimeException();
						}
						if (!arrows().contains(ret)) {
							throw new RuntimeException(ret.toString());
						}
						return ret;
					}
					if (g.third.get(0).toString().startsWith("!") && a.equals("_1")) {
						List<C> l = new LinkedList();
						l.addAll(g.third.subList(1, g.third.size()));
						Triple<C, C, List<C>> ret = new Triple<>(f.first, g.second, l);
						if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
							throw new RuntimeException();
						}
						// must find equivalent - see CTDB example
						ret = find_old(getKB(), ret, hom(ret.first, ret.second));
						if (!arrows().contains(ret)) {
							throw new RuntimeException("Anomaly: please report: " + ret);
						}
						return ret;
					}

					List<C> vl = new LinkedList<>();
					vl.add(v);
					Triple<C, C, List<C>> sofar = new Triple<>(type(v).first, type(v).second, vl);

					List gnX = new LinkedList<>(g.third);
					for (C gn : g.third) {
						gnX.remove(0);
						sofar = findEq(sofar, gn);
						if (sch.arrows().contains(sofar)) {
							List hhh = new LinkedList();
							hhh.add("!_" + a);
							hhh.addAll(sofar.third);
							hhh.addAll(gnX);

							Triple<C, C, List<C>> ret0 = new Triple<>(a, g.second, hhh);
							Triple ret = find_old(schema.getKB(), ret0,
									sch.hom(ret0.first, ret0.second));
							if (!arrows().contains(ret)) {
								throw new RuntimeException("f " + f + " and " + g + "\n\nbad: "
										+ ret + " not found inn\n\n"
										+ Util.sep(arrows(), "\n"));
							}
							if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
								throw new RuntimeException();
							}
							return ret;
						}
					}

					List<C> retl = new LinkedList<>();
					retl.add((C) ("!_" + a));
					retl.addAll(sofar.third);
					Triple<C, C, List<C>> ret = new Triple<>(f.first, g.second, retl);

					if (a.equals("_1") && global.allIds().contains(sofar.second)
							&& global.cat().hom((C) "_1", sofar.second).contains(sofar)) {
						if (!arrows().contains(sofar)) {
							throw new RuntimeException(sofar.toString());
						}
						if (!sofar.first.equals(f.first) || !sofar.second.equals(g.second)) {
							throw new RuntimeException();
						}
						return sofar;
					}
					if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
						throw new RuntimeException(ret + " not " + f + " and " + g);
					}
					// another one where have to use KB
					ret = find_old(getKB(), ret, hom(ret.first, ret.second));
					if (!arrows().contains(ret)) {
						throw new RuntimeException("f " + f + " and " + g + "\n\nbad: "
								+ ret + " not found inn\n\n" + Util.sep(arrows(), "\n"));
					}
					return ret;
				}
				if (sch.arrows().contains(f) && new_arrs.contains(g)) {
					C a0 = f.first;
					//C a = f.second;
					C v = g.third.get(1);
					List<C> l = new LinkedList<>();
					l.add((C) ("!_" + a0));
					l.add(v);
					Triple<C, C, List<C>> ret = new Triple<>(a0, g.second, l);
					if (!ret.first.equals(f.first) || !ret.second.equals(g.second)) {
						throw new RuntimeException();
					}
					if (!arrows().contains(ret)) {
						throw new RuntimeException(ret.toString());
					}
					return ret;
				}

				throw new RuntimeException("bottomed out: " + f + " and " + g + "\n"
						+ sch.hom(f.first, f.second) + "\n" + sch.hom(g.first, g.second));
			}

			@SuppressWarnings({ "unchecked" })
			private Triple<C, C, List<C>> findEq(Triple<C, C, List<C>> sofar, C gn) {
				if (sofar.third.size() != 1) {
					throw new RuntimeException("sofar third not length 1 is " + sofar);
				}
				C v = sofar.third.get(0);
				List<C> tofind = new LinkedList<>();
				tofind.add(v);
				tofind.add(gn);
				List<C> found = eqm.get(new Pair<>(v, gn));
				// Pair<List<C>, List<C>> xxx = null;
				for (Pair<List<C>, List<C>> eq : eqs) {
					if (found != null) {
						break;
					}
					if (eq.first.equals(tofind)) {
						found = eq.second;
						// xxx = eq;
						break;
					}
					if (eq.second.equals(tofind)) {
						found = eq.first;
						// xxx = eq;
						break;
					}
				}
				eqm.put(new Pair<>(v, gn), found);
				if (found == null) {
					throw new RuntimeException("sofar " + sofar + " gn " + gn + "\n\n" + allEqs());
				}
				@SuppressWarnings("rawtypes")
				List l = new LinkedList<>();

				l.addAll(found);
				Triple<C, C, List<C>> ret = new Triple<>(type(found).first, type(found).second, l);
				return ret;
			}

		};
		// cache the composition table
		if (DefunctGlobalOptions.debug.fpql.validate_amalgams) {
			ret.validate();
		}
		return ret;
	}

	// : cache these?
	public Category<C, Triple<C, C, List<C>>> small_cat() {
		Set<C> localIds = new HashSet<>(ids);
		List<Triple<C, C, List<C>>> paths = new LinkedList<>();
		LinkedHashMap<C, Pair<C, C>> t = new LinkedHashMap<>();

		List<Triple<C, C, List<C>>> consts = new LinkedList<>();

		if (global != null) {
			for (C c : global.ids) {
				if (c.equals("_1")) {
					continue;
				}
				paths.add(new Triple<>(c, c, new LinkedList<>()));
				localIds.add(c);
			}
			for (Entry<C, Pair<C, C>> k : global.types.entrySet()) {
				if (k.getValue().first.equals("_1") || k.getValue().second.equals("_1")
						|| k.getKey().equals("_1")) {
					continue;
				}
				t.put(k.getKey(), k.getValue());
			}
			extend(global.getKB(), paths, t, consts);
		}

		if (schema != null) {
			throw new RuntimeException();
		}

		for (C c : ids) {
			paths.add(new Triple<>(c, c, new LinkedList<>()));
		}
		for (Entry<C, Pair<C, C>> k : types.entrySet()) {
			if (k.getValue().first.equals("_1") || k.getValue().second.equals("_1")
					|| k.getKey().equals("_1")) {
				continue;
			}
			t.put(k.getKey(), k.getValue());
		}

		extend(getKB(), paths, t, consts);

		Set<Triple<C, C, List<C>>> arrows = new HashSet<>(paths);

		@SuppressWarnings("serial")
		Category<C, Triple<C, C, List<C>>> xcat2 = new Category<>() {

			@Override
			public Set<C> objects() {
				return localIds;
			}

			@Override
			public Set<Triple<C, C, List<C>>> arrows() {
				return arrows;
			}

			@Override
			public C source(Triple<C, C, List<C>> a) {
				return a.first;
			}

			@Override
			public C target(Triple<C, C, List<C>> a) {
				return a.second;
			}

			@Override
			public Triple<C, C, List<C>> identity(C o) {
				return new Triple<>(o, o, new LinkedList<>());
			}

			@Override
			public Triple<C, C, List<C>> compose(Triple<C, C, List<C>> a1, Triple<C, C, List<C>> a2) {
				List<C> ret = new LinkedList<>(a1.third);
				ret.addAll(a2.third);
				Triple<C, C, List<C>> xxx = new Triple<>(a1.first, a2.second, ret);
				Triple<C, C, List<C>> yyy = find_old(getKB(), xxx, hom(a1.first, a2.second));
				if (yyy == null) {
					throw new RuntimeException("Found nothing equivalent to " + ret + " in "
							+ arrows());
				}
				return yyy;
			}
		};

		xcat2.validate();

		// xcat = xcat2;

		return xcat2;
	}

	// mutate paths in place
	private static <C> void extend(ThueSlow<C, Unit> kb, Collection<Triple<C, C, List<C>>> paths,
                                   Map<C, Pair<C, C>> t, Collection<Triple<C, C, List<C>>> consts) {
		int iter = 0;
		for (; iter < DefunctGlobalOptions.debug.fpql.MAX_PATH_LENGTH; iter++) {
			Set<Triple<C, C, List<C>>> newPaths = new HashSet<>();
			for (Triple<C, C, List<C>> p : paths) {
				for (C e : outEdges(t, p.second)) {
					List<C> p0 = new LinkedList<>(p.third);
					p0.add(e);

					Triple<C, C, List<C>> toAdd = new Triple<>(p.first, t.get(e).second, p0);
					Triple<C, C, List<C>> found = find_old(kb, toAdd, paths);

					if (found == null) {
						found = find_old(kb, toAdd, newPaths);
						if (found == null) {
							find_old(kb, toAdd, consts);
							newPaths.add(toAdd);
						}
					}
				}
			}
			if (paths.containsAll(newPaths)) {
				break;
			}
			paths.addAll(newPaths);
		}
		if (iter == DefunctGlobalOptions.debug.fpql.MAX_PATH_LENGTH) {
			throw new RuntimeException("Exceeded maximum path length");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static XCtx<String> make(XCtx<String> S, XInst I) {
		// Set<String> seen = new HashSet<>();
		Map t = new HashMap<>();
		Set e = new HashSet<>();

		for (Pair<String, String> k : I.nodes) {
			if (k.second.equals("_1")) {
				throw new RuntimeException("Cannot create unit variable");
			}
			if (!S.types.containsKey(k.second) && !S.global.types.containsKey(k.second)) {
				throw new RuntimeException("Unknown node/type: " + k.second);
			}
			if (t.containsKey(k.first)) {
				if (t.get(k.first).equals(new Pair<>("_1", k.second))) {
					throw new RuntimeException("Duplicate name: " + k.first);
				}
			}
			if (S.types.containsKey(k.first)) {
				throw new RuntimeException("Name of variable is also in schema " + k);
			}
			if (S.global.types.containsKey(k.first)) {
				throw new RuntimeException("Name of variable is also global " + k);
			}
			t.put(k, new Pair<>("_1", k.second));
		}

		XCtx tmp = new XCtx(new HashSet<>(), t, e, S.global, S, "instance");

		for (Pair<List<String>, List<String>> k : I.eqs) {
			// : must expand paths
			// : supress variable check for now

			Set s = new HashSet<>();
			s.add(new LinkedList<>());
			List<List> lhs = new LinkedList<>(expand(s, k.first, S, tmp));
			List<List> rhs = new LinkedList<>(expand(s, k.second, S, tmp));
			if (lhs.size() == 1 && rhs.size() > 1) {
				List rhsX = new LinkedList<>();
				List x = new LinkedList();
				x.add(new Pair<>(((Pair) rhs.get(0).get(0)).first, tmp.type(lhs.get(0)).second));
				rhsX.add(x);
				rhs = rhsX;
			} else if (rhs.size() == 1 && lhs.size() > 1) {
				List lhsX = new LinkedList<>();
				List x = new LinkedList();
				x.add(new Pair<>(((Pair) lhs.get(0).get(0)).first, tmp.type(rhs.get(0)).second));
				lhsX.add(x);
				lhs = lhsX;
			}
			if (rhs.isEmpty()) {
				throw new RuntimeException("In equation "
			+ Util.sep(k.first, ".") + " = " + Util.sep(k.second, ".") + ", the right hand side refers to non-existent terms.  You should probably add terms at the global or instance level.");
			}
			if (lhs.isEmpty()) {
				throw new RuntimeException("In equation "
			+ Util.sep(k.first, ".") + " = " + Util.sep(k.second, ".") + ", the left hand side refers to non-existent terms.  You should probably add terms at the global or instance level.");
			}
			if (rhs.size() > 1) {
				throw new RuntimeException("In equation "
			+ Util.sep(k.first, ".") + " = " + Util.sep(k.second, ".") + ", the right hand is ambiguous, and could mean any of {"  +
						printDirty(rhs) + "}");

			}
			if (lhs.size() > 1) {
				throw new RuntimeException("In equation "
			+ Util.sep(k.first, ".") + " = " + Util.sep(k.second, ".") + ", the left hand is ambiguous, and could mean any of {"  +
						printDirty(lhs) + "}");

			}

			e.add(new Pair<>(new LinkedList<>(lhs).get(0), new LinkedList<>(rhs).get(0)));
		}

		// s.parent = s_old;
		// s.validate();
		// s.init();
		XCtx<String> ret = new XCtx<>(new HashSet<>(), t, e, S.global, S, "instance");
		ret.saturated = I.saturated;
		ret.shouldAbbreviate = true;
		return ret;
	}

	@SuppressWarnings("rawtypes")
	private static String printDirty(List<List> xxx) {
		List yyy = xxx.stream().map(x -> Util.sep(x, ".")).collect(Collectors.toList());
		return Util.sep(yyy, ",");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Set<List> expand(Set<List> sofar, List rest, XCtx s, XCtx I) {
		if (rest.isEmpty()) {
			return sofar;
		}

		Object o = rest.get(0);
		List rest2 = rest.subList(1, rest.size());

		if (s.allTerms().contains(o)) {
			Set<List> sofar2 = new HashSet<>();
			for (List l : sofar) {
				List r = new LinkedList<>(l);
				r.add(o);
				try {
					I.type(r);
				} catch (Exception e) {
					continue;
				}
				sofar2.add(r);
			}
			return expand(sofar2, rest2, s, I);
		}

		// Set<Pair> poss = new HashSet<>();
		Set<List> ret = new HashSet<>();
		for (Object v : I.terms()) {
			Pair p = (Pair) v;
			if (p.first.equals(o)) {
				Set<List> sofar2 = new HashSet<>();
				for (List l : sofar) {
					List r = new LinkedList<>(l);
					r.add(p);
					try {
						I.type(r);
					} catch (Exception e) {
						continue;
					}
					sofar2.add(r);
				}
				ret.addAll(expand(sofar2, rest2, s, I));
			}
		}

		return ret;
	}

	public static XCtx<String> make(XCtx<String> env, XSchema s) {
		Set<String> i = new HashSet<>();
		Map<String, Pair<String, String>> t = new HashMap<>();

		for (String k : s.nodes) {
			if (env.types.containsKey(k)) {
				throw new RuntimeException("Name of node is also global " + k);
			}
			if (t.containsKey(k)) {
				throw new RuntimeException("Duplicate node: " + k);
			}
			i.add(k);
			t.put(k, new Pair<>(k, k));
		}

		for (Triple<String, String, String> k : s.arrows) {
			if (env.types.containsKey(k.first)) {
				throw new RuntimeException("Name of edge is also global " + k.first);
			}
			if (i.contains(k.first)) {
				throw new RuntimeException("Name of edge is also node " + k.first);
			}
			if (t.containsKey(k.first)) {
				throw new RuntimeException("Duplicate edge: " + k);
			}

			String edge1 = null;
			if (i.contains(k.second)) {
				edge1 = "true";
			}
			if (env.types.containsKey(k.second)) {
				edge1 = "false";
			}
			if (edge1 == null) {
				throw new RuntimeException("Error in " + k + ": " + k.second
						+ " is not node or type");
			}

			String edge2 = null;
			if (i.contains(k.third)) {
				edge2 = "true";
			}
			if (env.types.containsKey(k.third)) {
				edge2 = "false";
			}
			if (edge2 == null) {
				throw new RuntimeException("Error in " + k + ": " + k.third
						+ " is not node or type");
			}

			if (Objects.equals(edge1, "false") && Objects.equals(edge2, "false")) {
				throw new RuntimeException("Error in " + k
						+ ": functions should be declared at top-level");
			}

			if (Objects.equals(edge1, "false") && Objects.equals(edge2, "true")) {
				throw new RuntimeException("Error in " + k + ": cannot have functions from types");
			}

			t.put(k.first, new Pair<>(k.second, k.third));
		}

		Set<Pair<List<String>, List<String>>> e = new HashSet<>(s.eqs);

		return new XCtx<>(i, t, e, env, null, "schema");
	}


	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
		result = prime * result + ((global == null) ? 0 : global.hashCode());
		result = prime * result + ((ids == null) ? 0 : ids.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
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
		XCtx<?> other = (XCtx<?>) obj;
		if (eqs == null) {
			if (other.eqs != null)
				return false;
		} else if (!eqs.equals(other.eqs))
			return false;
		if (global == null) {
			if (other.global != null)
				return false;
		} else if (!global.equals(other.global))
			return false;
		if (ids == null) {
			if (other.ids != null)
				return false;
		} else if (!ids.equals(other.ids))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public void simp() {
		if (!initialized) {
			throw new RuntimeException();
		}
		xcat = null;
		eqm = null;
		kb = null;

		subst((C) "!__1", (C) "_1");

		kb();
	}

/*
	private boolean match() {
		Set<Pair<C, C>> substs = new HashSet<>();
		for (Pair<List<C>, List<C>> eq1 : allEqs()) {
			for (Pair<List<C>, List<C>> eq2 : allEqs()) {
				if (eq1.first.size() != 2) {
					throw new RuntimeException();
				}
				if (eq2.first.size() != 2) {
					throw new RuntimeException();
				}
				if (eq1.second.size() != 1) {
					throw new RuntimeException();
				}
				if (eq2.second.size() != 1) {
					throw new RuntimeException();
				}
				if (eq1.first.equals(eq2.first)) {
					C r1 = eq1.second.get(0);
					C r2 = eq2.second.get(0);
					substs.add(new Pair<>(r1, r2));
				}
			}
		}

		if (substs.isEmpty()) {
			return false;
		}

		for (Pair<C, C> p : substs) {
			subst(p.first, p.second);
		}

		return true;
	}
*/
	private List<C> subst(C s, C t, List<C> l) {
		return l.stream().map(x -> x.equals(s) ? t : x).collect(Collectors.toList());
	}

	private void subst(C s, C t) {
		if (!types.containsKey(s)) {
			throw new RuntimeException();
		}
		types.remove(s);
		Set<Pair<List<C>, List<C>>> new_eqs = new HashSet<>();
		for (Pair<List<C>, List<C>> eq : eqs) {
			Pair<List<C>, List<C>> new_eq = new Pair<>(subst(s, t, eq.first),
					subst(s, t, eq.second));
			new_eqs.add(new_eq);
		}
		eqs = new_eqs;
	}

	public Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> obs(Triple<C, C, List<C>> i) {
		if (!i.first.equals("_1")) {
			throw new RuntimeException();
		}
		if (global.allIds().contains(i.second)) {
			throw new RuntimeException();
		}
		Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> ret = new HashMap<>();
		for (C t : global.allIds()) {
			for (Triple<C, C, List<C>> arr : cat().hom(i.second, t)) {
				Triple<C, C, List<C>> val = cat().compose(i, arr);
				ret.put(arr, val);
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public List<Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>>> obs(List<C> i) {
		Function<C, Object> f = c -> {
			if (schema.allTerms().contains(c)) {
				return c;
			}
			List<C> l = new LinkedList<>();
			l.add(c);
			Triple<C, C, List<C>> t = new Triple<>(type(c).first, type(c).second, l);
			Triple<C, C, List<C>> u = find_fast(t);
			return obs(u);
		};
		Object ret = i.stream().map(f).collect(Collectors.toList());
		return (List<Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>>>) ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public XCtx<C> rel() {
		Set<Pair<List<C>, List<C>>> new_eqs = new HashSet<>();
		if (schema == null) {
			throw new RuntimeException("Problem with relationalize");
		}
		Map new_types = new HashMap<>();

		//Set<Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>>> gens = new HashSet<>();
		Map<Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>>, Triple<C, C, List<C>>> genMap = new HashMap<>();
		Map<Triple<C, C, List<C>>, Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>>> genMap2 = new HashMap<>();

		for (C c : schema.ids) {
			for (Triple<C, C, List<C>> i : cat().hom((C) "_1", c)) {
				Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> o = obs(i);
				new_types.put(o, new Pair("_1", c));
				genMap.put(o, i);
				genMap2.put(i, o);
			}
		}
		for (C c : schema.global.ids) {
			for (Triple<C, C, List<C>> i : cat().hom((C) "_1", c)) {
				if (schema.global.cat().hom((C) "_1", c).contains(i)) {
					continue;
				}
				Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> o = new HashMap<>();
				o.put(new Triple<>(c, c, new LinkedList<>()), i);
				new_types.put(o, new Pair("_1", c));
				genMap.put(o, i);
				genMap2.put(i, o);
			}
		}

		for (Object iX : new_types.keySet()) {
			Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> o = (Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>>) iX;
			Triple<C, C, List<C>> i = genMap.get(o);
			for (C e : allTerms()) {
				if (!type(e).first.equals(i.second)) {
					continue;
				}
				List<C> lhs = new LinkedList<>();
				lhs.add((C) o);
				lhs.add(e);

				List<C> tofind = new LinkedList<>();
				tofind.addAll(i.third);
				tofind.add(e);
				Triple<C, C, List<C>> rhs0 = new Triple<>(i.first, type(e).second, tofind);
				Triple<C, C, List<C>> rhsX = find_fast(rhs0);
				if (rhsX == null) {
					throw new RuntimeException();
				}

				List<C> rhs = new LinkedList<>();

				Map<Triple<C, C, List<C>>, Triple<C, C, List<C>>> o2 = genMap2.get(rhsX);
				if (o2 == null) {
					if (rhsX.third.isEmpty()) {
						rhs.add(rhsX.first);
					} else {
						rhs.addAll(rhsX.third);
					}
				} else {
					rhs.add((C) o2);
				}

				new_eqs.add(new Pair<>(lhs, rhs));
			}
		}

		XCtx<C> ret = new XCtx<>(new HashSet<>(), new_types, new_eqs, global, schema, kind);
		ret.saturated = true;
		return ret;
	}

	private final Map<Pair<C,C>, XCtx<C>> y_cache = new HashMap<>();

	@SuppressWarnings("unchecked")
	public XCtx<C> y(C name, C type) {
		Pair<C,C> p = new Pair<>(name, type);
		XCtx<C> ret = y_cache.get(p);
		if (ret != null) {
			return ret;
		}

		Map<C, Pair<C, C>> types0 = new HashMap<>();

		types0.put(name, new Pair<>((C)"_1", type));

		ret = new XCtx<>(new HashSet<>(), types0, new HashSet<>(), global, this, "instance");
		y_cache.put(p, ret);
		return ret;
	}

	public XCtx<C> hat() {
		Map<C, Pair<C, C>> new_types = new HashMap<>();
		for (C k : types.keySet()) {
			Pair<C, C> v = types.get(k);
			if (!global.ids.contains(v.first) && !global.ids.contains(v.second)) {
				new_types.put(k,v);
			}
		}

		Set<Pair<List<C>, List<C>>> new_eqs = new HashSet<>();
		for (Pair<List<C>, List<C>> p : eqs) {
			if (containsType(p.first) || containsType(p.second)) {
				continue;
			}
			new_eqs.add(p);
		}

		return new XCtx<>(ids, new_types, new_eqs, empty_global(), null, "schema");
	}

	private boolean containsType(List<C> l) {
		for (C k : l) {
			Pair<C, C> v = type(k);
			if (global.ids.contains(v.first) || global.ids.contains(v.second)) {
				return true;
			}
		}
		return false;
	}

	private Graph<Triple<C,C,List<C>>, Pair<Integer, C>> elemGraph() {
		Graph<Triple<C,C,List<C>>, Pair<Integer, C>> g = new DirectedSparseMultigraph<>();
		@SuppressWarnings("unchecked")
		C ccc = (C) "_1";
		for (Triple<C, C, List<C>> arr : cat().arrowsFrom(ccc)) {
			if (global.ids.contains(arr.second)) {
				continue;
			}
			if (arr.second.equals("_1")) {
				continue;
			}
			g.addVertex(arr);
		}
		int i = 0;
		for (Triple<C, C, List<C>> arr : cat().arrowsFrom(ccc)) {
			if (global.ids.contains(arr.second)) {
				continue;
			}
			if (cat().isId(arr)) {
				continue;
			}
			if (arr.second.equals("_1")) {
				continue;
			}
			for (C c : schema.terms()) {
				Pair<C, C> t = schema.type(c);
				if (!t.first.equals(arr.second)) {
					continue;
				}
				if (global.ids.contains(t.second)) {
					continue;
				}
				if (t.second.equals("_1")) {
					continue;
				}
				if (schema.ids.contains(c)) {
					continue;
				}
				List<C> l = new LinkedList<>(arr.third);
				l.add(c);
				Triple<C, C, List<C>> tofind = new Triple<>(arr.first, t.second, l);
				Triple<C, C, List<C>> found = find_fast(tofind);
				g.addEdge(new Pair<>(i++, c), arr, found);
			}
		}
		return g;
	}

	private JPanel elements() {
		if (schema == null || global == null) {
			throw new RuntimeException();
		}
		JPanel ret = new JPanel(new GridLayout(1,1));
		try {
			ret.add(makeElems());
		} catch (Exception e) {
			ret.add(new CodeTextPanel(BorderFactory.createEtchedBorder(), "", "ERROR\n\n" +  e.getMessage()));
		}
		return ret;
	}

	private static final Color[] colors = { Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW, Color.CYAN, Color.WHITE, Color.GRAY, Color.BLACK, Color.PINK, Color.ORANGE };
	private final Map<C, Color> colorMap = new HashMap<>();
	private void initColors() {
		int i = 0;
		for (C c : schema.ids) {
			colorMap.put(c, colors[i]);
			i++;
			if (i == colors.length) {
				i = 0;
			}
		}
	}

	private final Map<Triple<C, C, List<C>>, JPanel> attPanels = new HashMap<>();
	private JPanel attsFor(Triple<C, C, List<C>> arr) {
		Map<C, String> tys = new HashMap<>();
		Map<C, String> vals = new HashMap<>();
		for (C c : schema.terms()) {
			Pair<C, C> t = schema.type(c);
			if (!t.first.equals(arr.second)) {
				continue;
			}
			if (schema.ids.contains(t.second)) {
				continue;
			}
			if (t.second.equals("_1")) {
				continue;
			}
			tys.put(c, t.second.toString());
			List<C> l = new LinkedList<>(arr.third);
			l.add(c);
			Triple<C, C, List<C>> tofind = new Triple<>(arr.first, t.second, l);
			Triple<C, C, List<C>> found = find_fast(tofind);
			vals.put(c, abbrPrint(found.third));
		}

		Object[][] rowData = new Object[tys.keySet().size()][3];
		Object[] colNames = {"Attribute", "Type", "Value" };

		int i = 0;
		for (C c : tys.keySet()) {
			rowData[i][0] = c;
			rowData[i][1] = tys.get(c);
			rowData[i][2] = vals.get(c);
			i++;
		}

		String str = "Attributes for " + abbrPrint(arr.third) + " (" + rowData.length + ")";
		JPanel ret = new JPanel(new GridLayout(1,1));
		ret.add(GuiUtil.makeTable(BorderFactory.createEmptyBorder(), str, rowData, colNames));
		return ret;
	}

	private JComponent makeElems() {
		Graph<Triple<C,C,List<C>>, Pair<Integer, C>> sgv = elemGraph();
		if (sgv.getVertexCount() > 64) {
			return new JTextArea("Too large to display");
		}
		initColors();
		Layout<Triple<C,C,List<C>>, Pair<Integer, C>> layout = new FRLayout<>(sgv);
		layout.setSize(new Dimension(600, 400));
		VisualizationViewer<Triple<C,C,List<C>>, Pair<Integer, C>> vv = new VisualizationViewer<>(layout);
		vv.getRenderContext().setLabelOffset(20);
		DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
		gm.setMode(Mode.TRANSFORMING);
		vv.setGraphMouse(gm);
		gm.setMode(Mode.PICKING);

		JPanel botPanel = new JPanel(new GridLayout(1,1));

		com.google.common.base.Function<Triple<C,C,List<C>>, Paint> vertexPaint =
			(Triple<C,C,List<C>> x) -> colorMap.get(x.second);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        return;
                    }
                    vv.getPickedEdgeState().clear();
                    @SuppressWarnings("unchecked")
                            Triple<C, C, List<C>> arr = (Triple<C, C, List<C>>)e.getItem();

                    //		cl.show(clx, xgrid.get(str));
            JPanel foo = attPanels.computeIfAbsent(arr, k -> attsFor(arr));
            botPanel.removeAll();
                    botPanel.add(foo);
                    botPanel.revalidate();
                });

		com.google.common.base.Function<Triple<C,C,List<C>>, String> ttt =
			(Triple<C,C,List<C>> arg0) -> {
				String ret = abbrPrint(arg0.third); //.toString();
				return (ret.length() > 32)
					? ret.substring(0, 31) + "..."
					: ret;
			};
		com.google.common.base.Function<Pair<Integer, C>, String> ttt2 =
			(Pair<Integer, C> arg0) -> arg0.second.toString();

		vv.getRenderContext().setVertexLabelTransformer(ttt);
		vv.getRenderContext().setEdgeLabelTransformer(ttt2);

		GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(zzz);
		ret.setBorder(BorderFactory.createEtchedBorder());

			JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			jsp.setResizeWeight(.8d); // setDividerLocation(.9d);
			jsp.setDividerSize(4);
			jsp.add(ret);
			jsp.add(botPanel);
			return jsp;

	}


	 /*re-implement the render functionality to work with internal frames(JInternalFrame)*/
   /*  private class MyRenderer extends JPanel implements Vertex<C, C>
    {
        static final long serialVersionUID = 420000L;
        @Override
        public void paintVertex(RenderContext<C, C> rc,
                                Layout<C, C> layout, C vertex) {
              GraphicsDecorator graphicsContext = rc.getGraphicsContext();
                Point2D center = layout.transform(vertex);
                Dimension size = new Dimension(100, 80);

                  JPanel sv = new JPanel(new GridLayout(1,1));
                sv.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                JTextArea area = new JTextArea(vertex.toString());
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                sv.add(new JScrollPane(area));
                //OK
                graphicsContext.draw(sv, rc.getRendererPane(), (int)center.getX(),
                                     (int)center.getY(), (int)size.getWidth(), (int)size.getHeight(), true);

        }
    } */

     private String toJSON() {
    	 if (global == null) {
    		  throw new RuntimeException("Attempt to JSON the type side");
    	 } else if (schema == null) {
    		 return toJSONSchema();
    	 } else {
    		 return null;
    	 }
     }

     private String toJSONSchema() {
    	// String ns = "";
    	// String es = "";

    	 Set<String> ns0 = new HashSet<>();
    	 Set<String> es0 = new HashSet<>();
    	 Set<String> eq0 = new HashSet<>();
    		 for (C k : global.ids) {
    			 String s = "{\"id\": \"" + k + "\", \"type\":\"type\", \"label\":\"" + k + "\"}";
        		 ns0.add(s);
    		 }
    		 for (C k : global.terms()) {
    			 if (global.ids.contains(k)) {
    				 continue;
    			 }
    			 String s = "{\"id\": \"" + k + "\", \"directed\":\"true\", \"label\":\"" + k + "\", \"source\":\""
    			 + global.type(k).first + "\", \"target\":\"" + global.type(k).second + "\"}";
        		 es0.add(s);
    		 }
    		 for (Pair<List<C>, List<C>> k : global.eqs) {
    			 List<String> lhs = k.first.stream().map(x -> "\"" + x + "\"").collect(Collectors.toList());
    			 List<String> rhs = k.second.stream().map(x -> "\"" + x + "\"").collect(Collectors.toList());
    			 String s = "{\"lhs\": [" + Util.sep(lhs, ", ") + "], \"rhs\": [" + Util.sep(rhs, ", ") + "]}";
    			 eq0.add(s);
    		 }
    		 for (C k : ids) {
    			 String s = "{\"id\": \"" + k + "\", \"type\":\"entity\", \"label\":\"" + k + "\"}";
        		 ns0.add(s);
    		 }
    		 for (C k : terms()) {
    			 if (ids.contains(k)) {
    				 continue;
    			 }
    			 String s = "{\"id\": \"" + k + "\", \"directed\":\"true\", \"label\":\"" + k + "\", \"source\":\""
    					 + type(k).first + "\", \"target\":\"" + type(k).second + "\"}";
    			 es0.add(s);
    		 }
    		 for (Pair<List<C>, List<C>> k : eqs) {
    			 List<String> lhs = k.first.stream().map(x -> "\"" + x + "\"").collect(Collectors.toList());
    			 List<String> rhs = k.second.stream().map(x -> "\"" + x + "\"").collect(Collectors.toList());
    			 String s = "{\"lhs\": [" + Util.sep(lhs, ", ") + "], \"rhs\": [" + Util.sep(rhs, ", ") + "]}";
    			 eq0.add(s);
    		 }

    	 return "{\"graph\": { \"directed\":true,\n\"nodes\":[\n" + Util.sep(ns0, ",\n") +
    			 	"],\n\"edges\":[\n" + Util.sep(es0, ",\n") + "\n]," +
    			 	"\n\"equations\":[\n" + Util.sep(eq0, ",\n") + "\n]}}";
     }


//     public String toJSONInstance() {
//    	 String ns = "";
//    	 String es = "";
//
//    	 Set<String> ns0 = new HashSet<>();
//    	 Set<String> es0 = new HashSet<>();
//    		 for (C k : global.ids) {
//    			 String s = "{\"id\": \"" + k + "\", \"type\":\"type\", \"label\":\"" + k + "\"}";
//        		 ns0.add(s);
//    		 }
//    		 for (C k : global.terms()) {
//    			 if (global.ids.contains(k)) {
//    				 continue;
//    			 }
//    			 String s = "{\"id\": \"" + k + "\", \"directed\":\"true\", \"label\":\"" + k + "\", \"source\":\""
//    			 + global.type(k).first + "\", \"target\":\"" + global.type(k).second + "\"}";
//        		 es0.add(s);
//    		 }
//    		 for (C k : schema.ids) {
//    			 String s = "{\"id\": \"" + k + "\", \"type\":\"entity\", \"label\":\"" + k + "\"}";
//        		 ns0.add(s);
//    		 }
//    		 for (C k : schema.terms()) {
//    			 if (schema.ids.contains(k)) {
//    				 continue;
//    			 }
//    			 String s = "{\"id\": \"" + k + "\", \"directed\":\"true\", \"label\":\"" + k + "\", \"source\":\""
//    					 + schema.type(k).first + "\", \"target\":\"" + schema.type(k).second + "\"}";
//    			 es0.add(s);
//    		 }
//    		 for (C k : terms()) {
//    			 String s = "{\"id\": \"" + k + "\", \"directed\":\"true\", \"label\":\"" + k + "\", \"source\":\""
//    					 + type(k).first + "\", \"target\":\"" + type(k).second + "\"}";
//    			 es0.add(s);
//    		 }
//
//    	 return "{\"graph\": { \"directed\":true,\n\"nodes\":[\n" + Util.sep(ns0, ",\n") + "],\n\"edges\":[\n" + Util.sep(es0, ",\n") + "\n]}}";
//     }



}

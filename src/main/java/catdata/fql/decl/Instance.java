package catdata.fql.decl;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.TableRowSorter;

import com.google.common.base.Function;

import catdata.IntRef;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.fql.FQLException;
import catdata.fql.Fn;
import catdata.fql.FqlOptions;
import catdata.fql.FqlUtil;
import catdata.fql.cat.Arr;
import catdata.fql.cat.FDM;
import catdata.fql.cat.FinCat;
import catdata.fql.cat.Inst;
import catdata.fql.cat.Value;
import catdata.fql.gui.CategoryOfElements;
import catdata.fql.parse.PrettyPrinter;
import catdata.fql.sql.PropPSM;
import catdata.ide.CodeTextPanel;
import catdata.ide.DefunctGlobalOptions;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class Instance {

	public Set<Object> getNode(Node n) {
		Set<Object> ret = new HashSet<>();
		for (Pair<Object, Object> k : data.get(n.string)) {
			ret.add(k.first);
		}
		return ret;
	}

	public Map<Object, Object> getNode2(Node n) {
		Map<Object, Object> ret = new HashMap<>();
		for (Pair<Object, Object> k : data.get(n.string)) {
			ret.put(k.first, k.second);
		}
		return ret;
	}

	//  better drop handling, by kind, visitor

	private void conformsTo(Signature s) throws FQLException {

		for (Node n : s.nodes) {
			Set<Pair<Object, Object>> i = data.get(n.string);
			if (i == null) {
				throw new FQLException("Missing node table " + n.string
						);
			}
			for (Pair<Object, Object> p : i) {
				if (p.first == null || p.second == null) {
					throw new FQLException("Null data in " + this);
				}
				if (!p.first.equals(p.second)) {
					throw new FQLException("Not reflexive: " + s + " and "
							+ this);
				}
				if (!(p.first instanceof String && p.second instanceof String)) {
					throw new RuntimeException("Non string IDs in " + this);
				}
			}
		}
		for (Attribute<Node> a : s.attrs) {
			Set<Pair<Object, Object>> i = data.get(a.name);
			if (i == null) {
				throw new FQLException("Missing Attribute<Node> table "
						+ a.name);
			}

			HashSet<Object> x = new HashSet<>();
			for (Pair<Object, Object> p : i) {
				x.add(p.first);
				if (!(p.first instanceof String)) {
					throw new RuntimeException("Not string ID in attr " + this);
				}
				if (!a.target.in(p.second)) {
					throw new RuntimeException("Bad attr domain: " + p.second
							+ " not in " + a.target);
				}
			}
			if (data.get(a.source.string).size() != x.size()) {
				throw new RuntimeException("does not map exactly the domain values in " + a.name
						+ "\n\ndata size " + data.get(a.source.string).size()
						+ " expected " + x.size());
			}

			for (Pair<Object, Object> p1 : i) {
				for (Pair<Object, Object> p2 : i) {
					if (p1.first.equals(p2.first)) {
						if (!p1.second.equals(p2.second)) {
							throw new FQLException("not functional: " + " in " + s);
						}
					}
				}
				// functional

				if (!contained(p1.first, data.get(a.source.string))) {
					throw new FQLException("Domain has non foreign key: " + p1.first
							+ " not contained in " + a.source.string);
				}
				if (!a.target.in(p1.second)) {
					throw new FQLException("Not a " + a.target + ": "
							+ p1.second);
				}
			}
		}
		for (Edge e : s.edges) {
			Set<Pair<Object, Object>> i = data.get(e.name);
			if (i == null) {
				throw new FQLException("Missing edge table " + e.name );
			}

			HashSet<Object> x = new HashSet<>();
			for (Pair<Object, Object> p : i) {
				x.add(p.first);
			}
			if (data.get(e.source.string).size() != x.size()) {
				Set<Pair<Object,Object>> s1 = data.get(e.source.string);
				Set<Object> y = new HashSet<>();
				for (Pair<Object, Object> p : data.get(e.source.string)) {
					y.add(p.first);
				}
//				Set sx = new HashSet<>(s1);
				y.removeAll(x);
				throw new FQLException(
						"Instance does not map all domain values in " + e.name
								+ "\n\n" + s1.size() + " vs " + x.size() + "\n\nmissing " + y );
			}

			for (Pair<Object, Object> p1 : i) {
				for (Pair<Object, Object> p2 : i) {
					if (p1.first.equals(p2.first)) {
						if (!p1.second.equals(p2.second)) {
							throw new FQLException("Not functional: " + " in "
									+ s);
						}
					}
				}
				// functional

				if (!contained(p1.first, data.get(e.source.string))) {
					throw new FQLException("Domain has non foreign key: "
							+ p1.first + " in " + e.source.string);
				}
				if (!contained(p1.second, data.get(e.target.string))) {
					throw new FQLException("Range has non foreign key: "
							+ p1.second + " in " + e.name);
				}
			}
		}
		for (Eq eq : s.eqs) {
			Set<Pair<Object, Object>> lhs = evaluate(eq.lhs);
			Set<Pair<Object, Object>> rhs = evaluate(eq.rhs);
			if (!lhs.equals(rhs)) {
				throw new FQLException("Violates constraints: " + s
						+ "\n\n eq is " + eq + "\nlhs is " + lhs
						+ "\n\nrhs is " + rhs);
			}
		}
		/*
		 * if (DEBUG.VALIDATE_WITH_EDS) { validateUsingEDs(); }
		 */
		// toFunctor();
	}

	public Map<String, Set<Pair<Object, Object>>> shred(String pre) {
		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>();

		for (String k : data.keySet()) {
			ret.put(pre + "_" + k, data.get(k));
		}

		return ret;
	}

	private Object follow(Path p, Object id) {
		for (Edge e : p.path) {
			id = PropPSM.lookup(data.get(e.name), id);
		}
		return id;
	}

	private final Map<Path, Set<Pair<Object, Object>>> cache = new HashMap<>();
	public Set<Pair<Object, Object>> evaluate(Path p) {
		Set<Pair<Object, Object>> x = data.get(p.source.string);
		if (x == null) {
			throw new RuntimeException("Couldnt find " + p.source.string);
		}
		if (cache.containsKey(p)) {
			return cache.get(p);
		}
		for (Edge e : p.path) {
			if (data.get(e.name) == null) {
				throw new RuntimeException("Couldnt find " + e.name);
			}

			x = compose4(x, data.get(e.name));
		}
		cache.put(p, x);
		return x;
	}

	public static <X, Y, Z> Set<Pair<X, Z>> compose(Set<Pair<X, Y>> x,
			Set<Pair<Y, Z>> y) {
		Set<Pair<X, Z>> ret = new HashSet<>();

		for (Pair<X, Y> p1 : x) {
			for (Pair<Y, Z> p2 : y) {
				if (p1.second.equals(p2.first)) {
					Pair<X, Z> p = new Pair<>(p1.first, p2.second);
					ret.add(p);
				}
			}
		}
		return ret;
	}
	//TODO aql why 4 copies of compose?
	private static <X, Y, Z> Set<Pair<X, Z>> compose2(Set<Pair<X, Y>> x,
													  Set<Pair<Y, Z>> y) {
		Set<Pair<X, Z>> ret = new HashSet<>();

		for (Pair<X, Y> p1 : x) {
			for (Pair<Y, Z> p2 : y) {
				if (p1.second.equals(p2.first)) {
					Pair<X, Z> p = new Pair<>(p1.first, p2.second);
					ret.add(p);
				}
			}
		}
		return ret;
	}
	public static <X, Y, Z> Set<Pair<X, Z>> compose3(Set<Pair<X, Y>> x,
			Set<Pair<Y, Z>> y) {
		Set<Pair<X, Z>> ret = new HashSet<>();

		for (Pair<X, Y> p1 : x) {
			for (Pair<Y, Z> p2 : y) {
				if (p1.second.equals(p2.first)) {
					Pair<X, Z> p = new Pair<>(p1.first, p2.second);
					ret.add(p);
				}
			}
		}
		return ret;
	}

	private static <X, Y, Z> Set<Pair<X, Z>> compose4(Set<Pair<X, Y>> x,
													  Set<Pair<Y, Z>> y) {
		Set<Pair<X, Z>> ret = new HashSet<>();

		for (Pair<X, Y> p1 : x) {
			for (Pair<Y, Z> p2 : y) {
				if (p1.second.equals(p2.first)) {
					Pair<X, Z> p = new Pair<>(p1.first, p2.second);
					ret.add(p);
				}
			}
		}
		return ret;
	}

	private static boolean contained(Object second, Set<Pair<Object, Object>> set) {
		for (Pair<Object, Object> p : set) {
			if (p.first.equals(second) && p.second.equals(second)) {
				return true;
			}
		}
		return false;
	}

	public final Map<String, Set<Pair<Object, Object>>> data;

	public final Signature thesig;

	public Instance(Signature thesig,
			Map<String, Set<Pair<Object, Object>>> data) throws FQLException {
		this(thesig, degraph(data));
	}

	private static List<Pair<String, List<Pair<Object, Object>>>> degraph(
			Map<String, Set<Pair<Object, Object>>> data2) {
		List<Pair<String, List<Pair<Object, Object>>>> ret = new LinkedList<>();
		for (Entry<String, Set<Pair<Object, Object>>> e : data2.entrySet()) {
			ret.add(new Pair<>(e.getKey(),
                    new LinkedList<>(e.getValue())));
		}
		return ret;
	}

	private boolean external = false;

	public boolean isExternal() {
		return external;
	}

	public Instance(Signature thesig) throws FQLException {
		// this.name = n;
        data = new HashMap<>();
        external = true;
		for (Node node : thesig.nodes) {
            data.put(node.string, new HashSet<>());
		}
		for (Edge e : thesig.edges) {
            data.put(e.name, new HashSet<>());
		}
		for (Attribute<Node> a : thesig.attrs) {
            data.put(a.name, new HashSet<>());
		}
		this.thesig = thesig;
		if (!typeCheck(thesig)) {
			throw new FQLException("Type-checking failure " + this);
		}
		conformsTo(thesig);
	}

	public Instance(Signature thesig,
			List<Pair<String, List<Pair<Object, Object>>>> data)
			throws FQLException {
		// this.name = n;
		this.thesig = thesig;
		this.data = new HashMap<>();

		for (Pair<String, List<Pair<Object, Object>>> k : data) {
			if (!thesig.contains(k.first)) {
				throw new FQLException("Extraneous table: " + k.first);
			}
		}

		List<String> seen = new LinkedList<>();
		for (Node node : thesig.nodes) {
			if (seen.contains(node.string)) {
				throw new FQLException("Duplicate table: " + node.string);
			}
			seen.add(node.string);
			this.data.put(node.string, makeFirst(node.string, data));
			// this.data.put(data(node.string), lookup(node.string, data));
		}
		for (Edge e : thesig.edges) {
			if (seen.contains(e.name)) {
				throw new FQLException("Duplicate table: " + e.name);
			}
			seen.add(e.name);

			this.data.put(e.name, lookup(e.name, data));
		}
		for (Attribute<Node> a : thesig.attrs) {
			if (seen.contains(a.name)) {
				throw new FQLException("Duplicate table: " + a.name);
			}
			seen.add(a.name);

			this.data.put(a.name, lookup(a.name, data));
		}
		if (!typeCheck(thesig)) {
			throw new FQLException("Type-checking failure " + this);
		}
		conformsTo(thesig);

	}

	private static Set<Pair<Object, Object>> makeFirst(String string,
			List<Pair<String, List<Pair<Object, Object>>>> data2) {
		for (Pair<String, List<Pair<Object, Object>>> p : data2) {
			if (string.equals(p.first)) {
				return secol(p.second);
			}
		}
		throw new RuntimeException("conformsTo failure: cannot find table "
				+ string);
		// + " in " + data2);
	}

	private static Set<Pair<Object, Object>> secol(List<Pair<Object, Object>> second) {
		Set<Pair<Object, Object>> ret = new HashSet<>();
		for (Pair<Object, Object> p : second) {
			ret.add(new Pair<>(p.first, p.first));
		}
		return ret;
	}

	private static Set<Pair<Object, Object>> lookup(String n,
			List<Pair<String, List<Pair<Object, Object>>>> data2)
			throws FQLException {
		for (Pair<String, List<Pair<Object, Object>>> p : data2) {
			if (n.equals(p.first)) {
				return new HashSet<>(p.second);
			}
		}
		throw new FQLException("cannot find " + n + " in " + data2);
	}


	private boolean typeCheck(Signature thesig2) {
		for (String s : data.keySet()) {
			if (!thesig2.contains(s) && !s.contains(" ")) {
				return false;
			}
		}
		for (String s : thesig2.all()) {
			if (null == data.get(s)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
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
		Instance other = (Instance) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}

	public String quickPrint() {
		return data.toString();
	}

	@Override
	public String toString() {
		String x = "\n nodes\n";
		boolean b = false;

		for (Node k0 : thesig.nodes) {
			Set<Pair<Object, Object>> k = data.get(k0.string);
			if (b) {
				x += ", \n";
			}
			b = true;
			x += "  " + k0.string + " -> {";
			boolean d = false;
			for (Pair<Object, Object> v : k) {
				if (d) {
					x += ", ";
				}
				d = true;
				x += PrettyPrinter.q(v.first);
			}
			x += "}";
		}
		x = x.trim();
		x += ";\n";
		x += " attributes\n";
		b = false;
		for (Attribute<Node> k0 : thesig.attrs) {
			Set<Pair<Object, Object>> k = data.get(k0.name);
			if (b) {
				x += ", \n";
			}
			b = true;
			x += "  " + k0.name + " -> {";
			boolean d = false;
			for (Pair<Object, Object> v : k) {
				if (d) {
					x += ", ";
				}
				d = true;
				x += "(" + PrettyPrinter.q(v.first) + ", "
						+ PrettyPrinter.q(v.second) + ")";
			}
			x += "}";
		}
		x = x.trim();
		x += ";\n";
		x += " arrows\n";
		b = false;
		for (Edge k0 : thesig.edges) {
			Set<Pair<Object, Object>> k = data.get(k0.name);
			if (b) {
				x += ", \n";
			}
			b = true;
			x += "  " + k0.name + " -> {";
			boolean d = false;
			for (Pair<Object, Object> v : k) {
				if (d) {
					x += ", ";
				}
				d = true;
				x += "(" + PrettyPrinter.q(v.first) + ", "
						+ PrettyPrinter.q(v.second) + ")";
			}
			x += "}";
		}
		x = x.trim();

		return "{\n " + x + ";\n}";
	}


	@SuppressWarnings("unchecked")
	public JPanel view() throws FQLException {
		List<JPanel> panels = new LinkedList<>();
		// Map<String, Set<Pair<String,String>>> data;
		List<String> sorted = new LinkedList<>(data.keySet());
		sorted.sort(Comparator.comparing(String::toString));
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
			Pair<String, String> cns = thesig.getColumnNames(k);
			@SuppressWarnings("serial")
			JTable t = new JTable(arr, new Object[] { cns.first, cns.second })  {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension d = getPreferredSize();
					return new Dimension(d.width, d.height);
				}
			};
			TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());

			t.setRowSorter(sorter);
			sorter.allRowsChanged();
			sorter.toggleSortOrder(0);
			t.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			JPanel p = new JPanel(new GridLayout(1, 1));
			p.add(new JScrollPane(t));
			p.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEmptyBorder(2, 2, 2, 2), k + "   ("
							+ xxx.size() + " rows)"));
			panels.add(p);
		}

		return FqlUtil.makeGrid((List<JComponent>)((Object)panels));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JPanel join() {
		prejoin();
		List pans = makePanels();
		return FqlUtil.makeGrid(pans);
	}

	private List<JPanel> makePanels() {
		List<JPanel> ret = new LinkedList<>();

		Comparator<String> strcmp = String::compareTo;

		List<String> xxx = new LinkedList<>(joined.keySet());
		xxx.sort(strcmp);

		for (String name : xxx) {
			JTable t = joined.get(name);
			JPanel p = new JPanel(new GridLayout(1, 1));
			p.add(new JScrollPane(t));
			p.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEmptyBorder(), name + "   ("
							+ data.get(name).size() + " rows)"));
			ret.add(p);
		}

		return ret;
	}

	@SuppressWarnings("serial")
	private void prejoin() {
		if (joined != null) {
			return;
		}
		vwr.setLayout(cards);
		vwr.add(new JPanel(), "");
		cards.show(vwr, "");
		Map<String, Map<String, Set<Pair<Object, Object>>>> jnd = new HashMap<>();
		Map<String, Set<Pair<Object, Object>>> nd = new HashMap<>();

		List<String> names = new LinkedList<>();

		for (Node n : thesig.nodes) {
			nd.put(n.string, data.get(n.string));
			jnd.put(n.string, new HashMap<>());
			names.add(n.string);
		}

		for (Edge e : thesig.edges) {
			jnd.get(e.source.string).put(e.name, data.get(e.name));
			// names.add(e.name);
		}

		for (Attribute<Node> a : thesig.attrs) {
			jnd.get(a.source.string).put(a.name, data.get(a.name));
			// names.add(a.name);
		}

		Comparator<String> strcmp = String::compareTo;
		names.sort(strcmp);
		joined = makejoined(jnd, nd, names);

		for (Edge e : thesig.edges) {
			String name = e.name;
			Object[][] rowData = new Object[data.get(name).size()][2];
			int i = 0;
			for (Pair<Object, Object> k : data.get(name)) {
				rowData[i][0] = k.first;
				rowData[i][1] = k.second;
				i++;
			}
			Object[] colNames = new Object[] { e.source.string, e.target.string };
			JTable t = new JTable(rowData, colNames) {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension d = getPreferredSize();
					return new Dimension(d.width, d.height);
				}
			};
			JPanel p = new JPanel(new GridLayout(1, 1));
			TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());
			sorter.toggleSortOrder(0);
			t.setRowSorter(sorter);
			sorter.allRowsChanged();
			p.add(new JScrollPane(t));

			p.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEmptyBorder(),
					name + " (" + data.get(name).size() + " rows)"));
			vwr.add(p, name);
		}

		for (Attribute<Node> e : thesig.attrs) {
			String name = e.name;
			Object[][] rowData = new Object[data.get(name).size()][2];
			int i = 0;
			for (Pair<Object, Object> k : data.get(name)) {
				rowData[i][0] = k.first;
				rowData[i][1] = k.second;
				i++;
			}
			Object[] colNames = new Object[] { e.source.string,
					e.target.toString() };
			JTable t = new JTable(rowData, colNames) {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension d = getPreferredSize();
					return new Dimension(d.width, d.height);
				}
			};
			JPanel p = new JPanel(new GridLayout(1, 1));
			TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());
			sorter.toggleSortOrder(0);
			t.setRowSorter(sorter);
			sorter.allRowsChanged();
			p.add(new JScrollPane(t));

			p.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEmptyBorder(),
					name + " (" + data.get(name).size() + " rows)"));
			vwr.add(p, name);
		}

		for (Attribute<Node> e : thesig.attrs) {
			String name = e.name;
			Object[][] rowData = new Object[data.get(name).size()][1];
			int i = 0;
			for (Pair<Object, Object> k : data.get(name)) {
				rowData[i][0] = k.second;
				i++;
			}
			Object[] colNames = new Object[] { e.target.toString() };
			JTable t = new JTable(rowData, colNames) {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension d = getPreferredSize();
					return new Dimension(d.width, d.height);
				}
			};
			JPanel p = new JPanel(new GridLayout(1, 1));
			TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());
			sorter.toggleSortOrder(0);
			t.setRowSorter(sorter);
			sorter.allRowsChanged();
			p.add(new JScrollPane(t));

			p.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEmptyBorder(), "domain of " + name
							+ " (" + data.get(name).size() + " rows)"));
			vwr.add(p, "domain of " + name);
		}

	}

	@SuppressWarnings("serial")
	private Map<String, JTable> makejoined(
			Map<String, Map<String, Set<Pair<Object, Object>>>> joined,
			Map<String, Set<Pair<Object, Object>>> nd, List<String> names) {
		Comparator<String> strcmp = String::compareTo;
		Map<String, JTable> ret = new HashMap<>();
		for (String name : names) {
			Map<String, Set<Pair<Object, Object>>> m = joined.get(name);
			Set<Pair<Object, Object>> ids = nd.get(name);
			Object[][] arr = new Object[ids.size()][m.size() + 1];
			Set<String> cols = m.keySet();
			List<String> cols2 = new LinkedList<>(cols);
			cols2.sort(strcmp);
			cols2.add(0, "ID");
			Object[] cols3 = cols2.toArray();

			int i = 0;
			for (Pair<Object, Object> id : ids) {
				arr[i][0] = id.first;

				int j = 1;
				for (String col : cols2) {
					if (col.equals("ID")) {
						continue;
					}
					Set<Pair<Object, Object>> coldata = m.get(col);
					for (Pair<Object, Object> p : coldata) {
						if (p.first.equals(id.first)) {
							arr[i][j] = p.second;
							break;
						}
					}
					j++;
				}
				i++;
			}

			JTable t = new JTable(arr, cols3) {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension d = getPreferredSize();
					return new Dimension(d.width, d.height);
				}
			};

			// foo and t are for the graph and tabular pane, resp
			JTable foo = new JTable(t.getModel()) {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension d = getPreferredSize();
					return new Dimension(d.width, d.height);
				}
			};
			JPanel p = new JPanel(new GridLayout(1, 1));
			// p.add(t);
			TableRowSorter<?> sorter = new MyTableRowSorter(t.getModel());

			sorter.toggleSortOrder(0);
			t.setRowSorter(sorter);
			sorter.allRowsChanged();
			TableRowSorter<?> sorter2 = new MyTableRowSorter(foo.getModel());

			sorter2.toggleSortOrder(0);
			foo.setRowSorter(sorter2);
			sorter2.allRowsChanged();
			p.add(new JScrollPane(foo));

			p.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEmptyBorder(), name + " (" + ids.size()
							+ " rows)"));
			vwr.add(p, name);

			ret.put(name, t);
		}

		return ret;
	}

	public JPanel text() {
		JTextArea ta = new JTextArea(toString());
		JPanel tap = new JPanel(new GridLayout(1, 1));
		ta.setBorder(BorderFactory.createEmptyBorder());
		tap.setBorder(BorderFactory.createEmptyBorder());
		ta.setWrapStyleWord(true);
		ta.setLineWrap(true);
		JScrollPane xxx = new JScrollPane(ta);
		tap.add(xxx);
		return tap;
	}

	private String rdfX(@SuppressWarnings("unused") String name) {
		String xxx = "";
		String prefix = "fql://entity/"; // + name + "/";

		for (Node n : thesig.nodes) {
			Set<Pair<Object, Object>> ids = data.get(n.string);
			for (Pair<Object, Object> idX : ids) {
				Object id = idX.first;
				xxx += "<rdf:Description rdf:about=\"" + prefix + id + "\">\n";
				xxx += "    <rdf:type rdf:resource=\"fql://node/" + n.string
						+ "\"/>\n"; // +
				for (Attribute<Node> a : thesig.attrsFor(n)) {
					xxx += "    <attribute:" + a.name + ">"
							+ lookupX(data.get(a.name), id) + "</attribute:"
							+ a.name + ">\n";
				}
				for (Edge a : thesig.edges) {
					if (!a.source.equals(n)) {
						continue;
					}

					xxx += "    <arrow:" + a.name + " rdf:resource=\"" + prefix
							+ lookupX(data.get(a.name), id) + "\"/>\n";
				}
				xxx += "</rdf:Description>\n\n";
			}
		}

		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "\n<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
				+ "\n    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\""
				+ "\n    xmlns:node=\"fql://node/\""
				+ "\n    xmlns:arrow=\"fql://arrow/\""
				+ "\n    xmlns:attribute=\"fql://attribute/\">\n\n" + xxx
				+ "</rdf:RDF>";
		return ret;
	}

	private static Object lookupX(Set<Pair<Object, Object>> set, Object id) {
		for (Pair<Object, Object> k : set) {
			if (k.first.equals(id)) {
				return k.second;
			}
		}
		throw new RuntimeException("Not found: " + id + " in " + set);
	}

	public JPanel rdf(String name) {
		JTextArea ta = new JTextArea(rdfX(name));
		JPanel tap = new JPanel(new GridLayout(1, 1));
		ta.setBorder(BorderFactory.createEmptyBorder());
		tap.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane xxx = new JScrollPane(ta);
		tap.add(xxx);
		return tap;
	}

	@SuppressWarnings("unchecked")
	public static boolean iso(Instance i1, Instance i2) {
		sameNodes(i1, i2);
		sameEdges(i1, i2);

		Signature sig = i1.thesig;

		Map<String, List<Map<Object, Object>>> subs1 = new HashMap<>();
		Map<String, List<Map<Object, Object>>> subs2 = new HashMap<>();
		for (Node n : sig.nodes) {
			String k = n.string;

			Object i1i2X = Inst.bijections(dedupl(i1.data.get(k)),
					dedupl(i2.data.get(k)));
			Object i2i1X = Inst.bijections(dedupl(i2.data.get(k)),
					dedupl(i1.data.get(k)));

			List<Map<Object, Object>> i1i2 = (List<Map<Object, Object>>) i1i2X;
			List<Map<Object, Object>> i2i1 = (List<Map<Object, Object>>) i2i1X;

			subs1.put(k, i1i2);
			subs2.put(k, i2i1);
		}

		Subs subs1X = new Subs(subs1);
		Subs subs2X = new Subs(subs2);
		Map<String, Map<Object, Object>> sub;

		boolean flag = false;
		while ((sub = subs1X.next()) != null) {
			try {
				Instance iX = i1.apply(sub);
				if (iX.equals(i2)) {
					flag = true;
					break;
				}
			} catch (Exception e) {
			}
		}
		if (!flag) {
			return false;
		}

		flag = false;
		while ((sub = subs2X.next()) != null) {
			try {
				Instance iX = i2.apply(sub);
				if (iX.equals(i1)) {
					flag = true;
					break;
				}
			} catch (Exception e) {
			}
		}
		return flag;
	}

	static class Subs {
		private final Map<String, List<Map<Object, Object>>> sub;
		private final LinkedList<String> keys;
		private final int[] counters;
		private final int[] sizes;

		public Subs(Map<String, List<Map<Object, Object>>> subs1) {
            sub = subs1;
            keys = new LinkedList<>(sub.keySet());

            counters = makeCounters(keys.size() + 1);
            sizes = makeSizes(keys, sub);
		}

		public Map<String, Map<Object, Object>> next() {
			if (counters[keys.size()] == 1) {
				return null;
			}

			Map<String, Map<Object, Object>> s = new HashMap<>();
			for (String k : keys) {
				s.put(k, sub.get(k).get(counters[keys.indexOf(k)]));
			}

			inc5(counters, sizes);

			return s;
		}
	}

	private static int[] makeSizes(List<String> keys,
			Map<String, List<Map<Object, Object>>> sub) {
		int[] ret = new int[keys.size()];
		int i = 0;
		for (String k : keys) {
			ret[i++] = sub.get(k).size();
		}
		return ret;
	}

	private static void inc5(int[] counters, int... sizes) {
		counters[0]++;
		for (int i = 0; i < counters.length - 1; i++) {
			if (counters[i] == sizes[i]) {
				counters[i] = 0;
				counters[i + 1]++;
			}
		}
	}

	private static int[] makeCounters(int size) {
		int[] ret = new int[size];
		for (int i = 0; i < size; i++) {
			ret[i] = 0;
		}
		return ret;
	}

	private Instance apply(Map<String, Map<Object, Object>> sub)
			throws FQLException {
		List<Pair<String, List<Pair<Object, Object>>>> ret = new LinkedList<>();

		for (Node n : thesig.nodes) {
			ret.add(new Pair<>(n.string, apply(data.get(n.string),
					sub.get(n.string), sub.get(n.string))));
		}
		for (Edge e : thesig.edges) {
			ret.add(new Pair<>(e.name, apply(data.get(e.name),
					sub.get(e.source.string), sub.get(e.target.string))));
		}

		return new Instance(thesig, ret);
	}

	private static List<Pair<Object, Object>> apply(
			Set<Pair<Object, Object>> set, Map<Object, Object> s1,
			Map<Object, Object> s2) {
		List<Pair<Object, Object>> ret = new LinkedList<>();

		for (Pair<Object, Object> p : set) {
			ret.add(new Pair<>(s1.get(p.first), s2.get(p.second)));
		}

		return ret;
	}

	private static List<Object> dedupl(Set<Pair<Object, Object>> set) {
		List<Object> ret = new LinkedList<>();
		for (Pair<Object, Object> p : set) {
			ret.add(p.first);
		}
		return ret;
	}

	private static void sameEdges(Instance i1, Instance i2) {
		for (Edge e1 : i1.thesig.edges) {
			if (!i2.thesig.edges.contains(e1)) {
				throw new RuntimeException("Missing " + e1 + " in " + i2 + ")");
			}
		}
		for (Edge e2 : i2.thesig.edges) {
			if (!i1.thesig.edges.contains(e2)) {
				throw new RuntimeException("Missing " + e2 + " in " + i1 + ")");
			}
		}
	}

	private static void sameNodes(Instance i1, Instance i2) {
		for (Node n1 : i1.thesig.nodes) {
			if (!i2.thesig.nodes.contains(n1)) {
				throw new RuntimeException("Missing " + n1 + " in " + i2 + ")");
			}
		}
		for (Node n2 : i2.thesig.nodes) {
			if (!i1.thesig.nodes.contains(n2)) {
				throw new RuntimeException("Missing " + n2 + " in " + i1 + ")");
			}
		}
	}

	public JPanel pretty(Color c) {
		return makeViewer(c);
	}

	private Graph<String, String> build() {
		// Graph<V, E> where V is the type of the vertices

		Graph<String, String> g2 = new DirectedSparseMultigraph<>();
		for (Node n : thesig.nodes) {
			g2.addVertex(n.string);
		}

		for (Edge e : thesig.edges) {
			g2.addEdge(e.name, e.source.string, e.target.string);
		}

		for (Attribute<Node> a : thesig.attrs) {
			g2.addVertex(a.name);
			g2.addEdge(a.name, a.source.string, a.name);
		}

		return g2;
	}

	private JPanel makeViewer(Color c) {
		Graph<String, String> g = build();
		if (g.getVertexCount() == 0) {
			return new JPanel();
		}
		return doView(c, g);
	}

	@SuppressWarnings({ "unchecked" })
	private JPanel doView(
			Color clr, Graph<String, String> sgv) {
		try {
			Class<?> c = Class.forName(FqlOptions.layout_prefix
					+ DefunctGlobalOptions.debug.fql.inst_graph);
			Constructor<?> x = c.getConstructor(Graph.class);
			Layout<String, String> layout = (Layout<String, String>) x
					.newInstance(sgv);

			VisualizationViewer<String, String> vv = new VisualizationViewer<>(
					layout);
			Function<String, Paint> vertexPaint = (String i) -> thesig.isAttribute(i) ? UIManager.getColor("Panel.background") : clr;
			DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
			vv.setGraphMouse(gm);
			gm.setMode(Mode.PICKING);
			vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
			vv.getRenderContext().setVertexLabelTransformer((String str) -> {

                            if (thesig.isAttribute(str)) {
                                str = thesig.getTypeLabel(str);
                            }
                            return str;
                        });

			vv.getPickedVertexState().addItemListener((ItemEvent e) -> {
                            if (e.getStateChange() != ItemEvent.SELECTED) {
                                return;
                            }
                            vv.getPickedEdgeState().clear();
                            String str = ((String) e.getItem());
                            prejoin();

                if (thesig.isAttribute(str)) {
                    cards.show(vwr, "domain of " + str);

                } else {
                    cards.show(vwr, str);
                }
                        });
			vv.getPickedEdgeState().addItemListener((ItemEvent e) -> {
                            if (e.getStateChange() != ItemEvent.SELECTED) {
                                return;
                            }
                            vv.getPickedVertexState().clear();
                            String str = ((String) e.getItem());
                            prejoin();
                            cards.show(vwr, str);
                        });
			vv.getRenderContext().setLabelOffset(20);
			vv.getRenderContext().setEdgeLabelTransformer((String s) -> {
                            if (thesig.isAttribute(s)) {
                                return "";
                            }
                            return s;
                        });

			float dash[] = { 1.0f };
			Stroke edgeStroke = new BasicStroke(0.5f,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash,
					10.0f);
			Stroke bs = new BasicStroke();
			Function<String, Stroke> edgeStrokeTransformer = s -> {
				if (thesig.isAttribute(s)) {
					return edgeStroke;
				}
				return bs;
			};

			vv.getRenderContext().setEdgeStrokeTransformer(
					edgeStrokeTransformer);
			vv.getRenderContext().setVertexLabelTransformer(
					new ToStringLabeller());

			GraphZoomScrollPane zzz = new GraphZoomScrollPane(vv);

			JSplitPane newthing = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			newthing.setResizeWeight(.8d);
			newthing.add(zzz);
			newthing.add(vwr);
			JPanel xxx = new JPanel(new GridLayout(1, 1));
			xxx.add(newthing);
		    layout.setSize(new Dimension(400,400));
			return xxx;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException();
		}
	}


	public static Instance terminal(Signature s, String init)
			throws FQLException {
		List<Pair<String, List<Pair<Object, Object>>>> ret = new LinkedList<>();

		int i = 0;
		String g = init;
		Map<Node, String> map = new HashMap<>();
		for (Node node : s.nodes) {
			List<Pair<Object, Object>> tuples = new LinkedList<>();

			if (init == null) {
				g = Integer.toString(i);
			}

			tuples.add(new Pair<>(g, g));
			ret.add(new Pair<>(node.string, tuples));
			map.put(node, g);
			i++;
		}

		for (Edge e : s.edges) {
			List<Pair<Object, Object>> tuples = new LinkedList<>();
			tuples.add(new Pair<>(map.get(new Node(e.source.string)), map
                    .get(new Node(e.target.string))));
			ret.add(new Pair<>(e.name, tuples));
		}

		return new Instance(s, ret);
	}

	public Inst<Node, Path, Object, Object> toFunctor2() throws FQLException {
		FinCat<Node, Path> cat = thesig.toCategory2().first;

		Map<Node, Set<Value<Object, Object>>> objM = new HashMap<>();
		for (Node obj : cat.objects) {
			if (data.get(obj.string) == null) {
				throw new RuntimeException("No data for " + obj + " in " + data);
			}
			objM.put(obj, conv(data.get(obj.string)));
		}

		Map<Arr<Node, Path>, Map<Value<Object, Object>, Value<Object, Object>>> arrM = new HashMap<>();
		for (Arr<Node, Path> arr : cat.arrows) {
			List<String> es = arr.arr.asList();

			String h = es.get(0);
			Set<Pair<Object, Object>> h0 = data.get(h);
			for (int i = 1; i < es.size(); i++) {
				h0 = compose2(h0, data.get(es.get(i)));
			}
			Map<Value<Object, Object>, Value<Object, Object>> xxx = FDM
					.degraph(h0);
			arrM.put(arr, xxx);
		}

		return new Inst<>(objM, arrM, cat);
	}

	private static Set<Value<Object, Object>> conv(Set<Pair<Object, Object>> set) {
		Set<Value<Object, Object>> ret = new HashSet<>();
		for (Pair<Object, Object> p : set) {
			ret.add(new Value<>(p.first));
		}
		return ret;
	}

	private final JPanel vwr = new JPanel();
	private final CardLayout cards = new CardLayout();
	private Map<String, JTable> joined;

	public Pair<JPanel, JPanel> groth(String name, Color c) {
		return CategoryOfElements.makePanel(name, this, c);
	}

	private static JPanel makePanel2(Pair<Object[], Object[][]> res) {
		Object[] colnames = res.first;
		Object[][] rows = res.second;

		JPanel ret = new JPanel(new GridLayout(1, 1));

		@SuppressWarnings("serial")
		JTable table = new JTable(rows, colnames)  {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				Dimension d = getPreferredSize();
				return new Dimension(d.width, d.height);
			}
		};
		TableRowSorter<?> sorter = new MyTableRowSorter(table.getModel());

		table.setRowSorter(sorter);
		sorter.allRowsChanged();
		sorter.toggleSortOrder(0);

		ret.add(new JScrollPane(table));

		String str = rows.length + " IDs, " + proj1(rows)
				+ " unique attribute combinations";
		ret.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEmptyBorder(), str));

		return ret;
	}

	private static int proj1(Object[][] in) {
		Set<List<Object>> ret = new HashSet<>();
		for (Object[] k : in) {
			List<Object> xxx = new LinkedList<>();
			xxx.addAll(Arrays.asList(k).subList(1, k.length));
			ret.add(xxx);
		}
		return ret.size();
	}

	public JPanel observables2() {
		try {
			Map<Node, Pair<Object[], Object[][]>> xxx = computeObservables();

			JTabbedPane t = new JTabbedPane();

			for (Node n : thesig.nodes) {
				t.addTab(n.string, makePanel2(xxx.get(n)));
			}
			JPanel ret = new JPanel(new GridLayout(1, 1));
			ret.add(t);
			ret.setBorder(BorderFactory.createEtchedBorder());
			return ret;

		} catch (Throwable e) {
			e.printStackTrace();
			JPanel ret = new JPanel(new GridLayout(1, 1));
			JTextArea a = new JTextArea(e.getMessage());
			ret.add(new JScrollPane(a));
			return ret;
		}
	}

	private final Comparator<Pair<Path, Attribute<Node>>> comparator = (Pair<Path, Attribute<Node>> o1, Pair<Path, Attribute<Node>> o2) -> {
            List<String> x1 = o1.first.asList();
            x1.add(o1.second.name);
            List<String> x2 = o2.first.asList();
            x2.add(o2.second.name);

            Iterator<String> i1 = x1.iterator();
            Iterator<String> i2 = x2.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                int c = i1.next().compareTo(i2.next());
                if (c != 0) {
                    return c;
                }
            }
            if (i1.hasNext()) {
                return 1;
            } else if (i2.hasNext()) {
                return -1;
            } else {
                return 0;
            }
        };

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Node, Pair<Object[], Object[][]>> computeObservables()
			throws FQLException {

		Map<Pair<Path, Attribute<Node>>, Set<Pair<Object, Object>>> rem = new HashMap<>();

		FinCat<Node, Path> cat = thesig.toCategory2().first;
		for (Arr<Node, Path> arr : cat.arrows) {
			Path p = arr.arr;
			Set<Pair<Object, Object>> v = evaluate(p);
			for (Attribute<Node> a : thesig.attrsFor(p.target)) {
				Set<Pair<Object, Object>> vv = compose(v, data.get(a.name));
				rem.put(new Pair<>(p, a), vv);
			}
		}

		Map<Node, List<Pair<Path, Attribute<Node>>>> m = new HashMap<>();
		for (Node n : thesig.nodes) {
			m.put(n, new LinkedList<>());
		}
		for (Pair<Path, Attribute<Node>> k : rem.keySet()) {
			m.get(k.first.source).add(k);
		}
		Map<Node, Pair<Object[], Object[][]>> ret = new HashMap<>();
		for (Node n : thesig.nodes) {
			(m.get(n)).sort(comparator);
			Object[] ar = new Object[m.get(n).size() + 1];
			Map<Object, Object[]> rows = new HashMap<>();
			for (Pair<Object, Object> o : data.get(n.string)) {
				Object[] arr = new Object[m.get(n).size() + 1];
				arr[0] = o.first;
				rows.put(o.first, arr);
			}
			ar[0] = "ID";
			int i = 1;
			for (Pair<Path, Attribute<Node>> k : m.get(n)) {
				List<String> print = k.first.asList();
				print.remove(0);
				print.add(k.second.name);
				ar[i] = PrettyPrinter.sep0(".", print);
				Set<Pair<Object, Object>> v = rem.get(k);
				for (Pair<Object, Object> p : v) {
					rows.get(p.first)[i] = p.second;
				}
				i++;
			}
			ret.put(n, new Pair(ar, rows.values().toArray(new Object[][] {})));
		}

		return ret;
	}


	/**
	 * Quickly compares two instances by checking the counts of tuples in all
	 * the rows.
	 */
	public static boolean quickCompare(Instance i, Instance j) {
		//List<String> l = new LinkedList<>();
		if (!i.data.keySet().equals(j.data.keySet())) {
			throw new RuntimeException(i.data.keySet() + "\n\n"
					+ j.data.keySet());
		}
		for (String k : i.data.keySet()) {
			Set<Pair<Object, Object>> v = i.data.get(k);
			Set<Pair<Object, Object>> v0 = j.data.get(k);
			if (v.size() != v0.size()) {
		//		l.add(k);
				return false;
			}
		}
		return true;
	}

	private static Triple<Instance, Map<Object, Pair<Object, Object>>, Map<Pair<Object, Object>, Object>> prod(
			IntRef idx, Instance I, Instance J) throws FQLException {
		if (!I.thesig.equals(J.thesig)) {
			throw new RuntimeException();
		}
		Map<String, Set<Pair<Object, Object>>> d = new HashMap<>();
		Map<Object, Pair<Object, Object>> m1 = new HashMap<>();
		Map<Pair<Object, Object>, Object> m2 = new HashMap<>();

		for (Node n : I.thesig.nodes) {
			Set<Pair<Object, Object>> s = new HashSet<>();
			for (Pair<Object, Object> id1 : I.data.get(n.string)) {
				for (Pair<Object, Object> id2 : J.data.get(n.string)) {
					Pair<Object, Object> p = new Pair<>(id1.first, id2.second);
					String str = Integer.toString(++idx.i);
					m1.put(str, p);
					m2.put(p, str);
					s.add(new Pair<>(str, str));
				}
			}
			d.put(n.string, s);
		}
		for (Edge e : I.thesig.edges) {
			Set<Pair<Object, Object>> s = new HashSet<>();
			for (Pair<Object, Object> k : d.get(e.source.string)) {
				Pair<Object, Object> y = m1.get(k.first);
				Object p = m2.get(new Pair<>(lookupX(I.data.get(e.name),
						y.first), lookupX(J.data.get(e.name), y.second)));
				s.add(new Pair<>(k.first, p));
			}
			d.put(e.name, s);
		}
		Instance K = new Instance(I.thesig, d);
		return new Triple<>(K, m1, m2);
	}

	public static Quad<Instance, Map<Pair<Node, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>>>, Map<Node, Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>>>, Map<Node, Map<Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>, Object>>> exp2(
			IntRef idx, Instance J, Instance I) throws FQLException {
		if (!J.thesig.equals(I.thesig)) {
			throw new RuntimeException();
		}

		Map<Node, List<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>> obsbar = I.thesig.obsbar();
		Map<Node, List<Pair<Arr<Node, Path>, Attribute<Node>>>> obs = I.thesig.obs();
		Fn<Path, Arr<Node, Path>> fn = I.thesig.toCategory2().second;
		FinCat<Node, Path> cat = I.thesig.toCategory2().first;

		Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();
		Map<Node, Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>>> map1 = new HashMap<>();
		Map<Node, Map<Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>, Object>> map2 = new HashMap<>();
		Map<Pair<Node, LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>>, Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>>> instances = new HashMap<>();

		for (Node n : I.thesig.nodes) {
			Set<Pair<Object, Object>> d = new HashSet<>();
			Map<Object, Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>> m1 = new HashMap<>();
			Map<Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform>, Object> m2 = new HashMap<>();
			for (LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> w : obsbar.get(n)) {
				Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>> Iw = I.omega(n, w, idx);
				instances.put(new Pair<>(n, w),  Iw);
				for (Transform t : Inst.hom(Iw.first, J)) {
					Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform> p = new Pair<>(w, t);
					Object str = Integer.toString(++idx.i);
					m1.put(str, p);
					m2.put(p, str);
					d.add(new Pair<>(str, str));
				}
			}
			data.put(n.string, d);
			map1.put(n, m1);
			map2.put(n, m2);
		}
		for (Attribute<Node> a : I.thesig.attrs) {
			Set<Pair<Object, Object>> d = new HashSet<>();
			for (Pair<Object, Object> k : data.get(a.source.string)) {
				Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform> k0 = map1.get(a.source).get(k.first);
				Object v = k0.first.get(new Pair<>(cat.id(a.source), a));
				d.add(new Pair<>(k.first, v));
			}
			data.put(a.name, d);
		}
		for (Edge e : I.thesig.edges) {
			Set<Pair<Object, Object>> d = new HashSet<>();
			for (Pair<Object, Object> k : data.get(e.source.string)) {
				Pair<LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object>, Transform> k0 = map1.get(e.source).get(k.first);
				LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> w0 = PropPSM.truncate2(I.thesig, k0.first, fn.of(new Path(I.thesig, e)), obs.get(e.target));

				Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>> Iw  = instances.get(new Pair<>(e.source, k0.first));
				Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>> Iw0 = instances.get(new Pair<>(e.target, w0));

				List<Pair<String, List<Pair<Object, Object>>>> tbd = new LinkedList<>();
				for (Node node : I.thesig.nodes) {
					 List<Pair<Object, Object>> set = new LinkedList<>();
					 for (Pair<Object, Object> id0 : Iw0.first.data.get(node.string)) {
						 Pair<Arr<Node, Path>, Object> p0 = Iw0.second.get(node).get(id0.first);
						 Pair<Arr<Node, Path>, Object> p = new Pair<>(cat.compose(fn.of(new Path(I.thesig, e)), p0.first), p0.second);
						 Object id = Iw.third.get(node).get(p);
						 set.add(new Pair<>(id0.first, id));
					 }
					 tbd.add(new Pair<>(node.string, set));
				}
				Transform f = new Transform(Iw0.first, Iw.first, tbd);

				Transform t = Transform.composeX(f, k0.second);
				Object u = map2.get(e.target).get(new Pair<>(w0, t));
				d.add(new Pair<>(k.first, u));
			}
			data.put(e.name, d);
		}
		Instance ret = new Instance(I.thesig, data);
		return new Quad<>(ret, instances, map1, map2);
	}

	// untyped
	public static Quad<Instance, Map<Node, Map<Object, Transform>>, Map<Node, Triple<Instance, Map<Object, Pair<Object, Object>>, Map<Pair<Object, Object>, Object>>>, Pair<Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>>> exp(
			IntRef idx, Instance J, Instance I) throws FQLException {
		if (!J.thesig.equals(I.thesig)) {
			throw new RuntimeException();
		}

		Pair<Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>>, Map<Edge, Transform>> xxx = I.thesig
				.repX(idx);
		Map<Node, Triple<Instance, Map<Object, Path>, Map<Path, Object>>> nm = xxx.first;
		Map<Edge, Transform> em = xxx.second;

		Map<Node, Map<Object, Transform>> map1 = new HashMap<>();
		Map<Node, Map<Transform, Object>> map2 = new HashMap<>();
		Map<Node, Triple<Instance, Map<Object, Pair<Object, Object>>, Map<Pair<Object, Object>, Object>>> map4 = new HashMap<>();
		Map<String, Set<Pair<Object, Object>>> data = new HashMap<>();

		for (Node n : I.thesig.nodes) {

			Map<Object, Transform> m1 = new HashMap<>();
			Map<Transform, Object> m2 = new HashMap<>();
			Set<Pair<Object, Object>> d = new HashSet<>();
			Triple<Instance, Map<Object, Pair<Object, Object>>, Map<Pair<Object, Object>, Object>> yyy = prod(
					idx, I, nm.get(n).first);
			map4.put(n, yyy);
			for (Transform t : Inst.hom(yyy.first, J)) {
				Object str = Integer.toString(++idx.i);
				m1.put(str, t);
				m2.put(t, str);
				d.add(new Pair<>(str, str));
			}
			map1.put(n, m1);
			map2.put(n, m2);
			data.put(n.string, d);
		}
		for (Edge e : I.thesig.edges) {
			Set<Pair<Object, Object>> d = new HashSet<>();
			for (Entry<Object, Transform> k : map1.get(e.source).entrySet()) {
				Transform h0 = em.get(e);
				Transform h = Transform.prod(I, map4.get(e.target),
						map4.get(e.source), h0);
				Transform t = Transform.composeX(h, k.getValue());
				Object o = map2.get(e.target).get(t);
				d.add(new Pair<>(k.getKey(), o));
			}
			data.put(e.name, d);
		}

		Instance IJ = new Instance(I.thesig, data);

		return new Quad<>(IJ, map1, map4, xxx);
	}

	private List<Pair<Object, Object>> ids() {
		List<Pair<Object, Object>> ret = new LinkedList<>();

		for (Node n : thesig.nodes) {
			ret.addAll(data.get(n.string));
		}

		return ret;
	}

	public List<Instance> subInstances_slow() {
		List<Instance> ret = new LinkedList<>();

		List<Pair<Object, Object>> ids = ids();

		List<LinkedHashMap<Pair<Object, Object>, Boolean>> subsets = Inst
				.homomorphs(ids, tf);
		for (LinkedHashMap<Pair<Object, Object>, Boolean> subset : subsets) {
			try {
				ret.add(filter(subset));
			} catch (FQLException fe) {
			}
		}
		return ret;
	}

	private static final List<Boolean> tf = Arrays.asList(true, false);

	private static Set<Map<String, Set<Pair<Object, Object>>>> subInstances_fast0(
			Signature sig, List<Node> list,
			Map<String, Set<Pair<Object, Object>>> inst) {
		Set<Map<String, Set<Pair<Object, Object>>>> ret = new HashSet<>();
		if (list.isEmpty()) {
			ret.add(inst);
			return ret;
		}
		List<Node> rest = new LinkedList<>(list);
		Node n = rest.remove(0);
		List<LinkedHashMap<Object, Boolean>> subsets = Inst.homomorphs(
				toList(inst.get(n.string)), tf);
		for (LinkedHashMap<Object, Boolean> subset : subsets) {
			Map<String, Set<Pair<Object, Object>>> j = recDel(sig, n, inst,
					subset);
			if (rest.isEmpty()) {
				ret.add(j);
			} else {
				Set<Map<String, Set<Pair<Object, Object>>>> h = subInstances_fast0(
						sig, rest, j);
				ret.addAll(h);
			}
		}

		return ret;
	}

	private static Map<String, Set<Pair<Object, Object>>> copyMap(
			Map<String, Set<Pair<Object, Object>>> inst) {
		Map<String, Set<Pair<Object, Object>>> m = new HashMap<>();
		for (String k : inst.keySet()) {
			m.put(k, new HashSet<>(inst.get(k)));
		}
		return m;
	}

	public List<Instance> subInstances() {
		List<Instance> ret = new LinkedList<>();
		for (Map<String, Set<Pair<Object, Object>>> k : subInstances_fast0(
				thesig, thesig.order(), data)) {
			try {
				ret.add(new Instance(thesig, k));
			} catch (Exception e) {
			}
		}
		return ret;
	}

	private static List<Object> toList(Set<Pair<Object, Object>> set) {
		List<Object> ret = new LinkedList<>();
		for (Pair<Object, Object> s : set) {
			ret.add(s.first);
		}
		return ret;
	}

	private static void remove(Set<Pair<Object, Object>> set, Object o) {
		set.removeIf(objectObjectPair -> objectObjectPair.first.equals(o));
	}

	private static Set<Object> clearX(Set<Pair<Object, Object>> set, Object o) {
		Iterator<Pair<Object, Object>> it = set.iterator();
		Set<Object> ret = new HashSet<>();
		while (it.hasNext()) {
			Pair<Object, Object> kkk = it.next();
			if (kkk.second.equals(o)) {
				it.remove();
				ret.add(kkk.first);
			}
		}
		return ret;
	}

	private static Map<String, Set<Pair<Object, Object>>> recDel(Signature sig,
			Node init, Map<String, Set<Pair<Object, Object>>> inst,
			LinkedHashMap<Object, Boolean> del0) {
		Map<String, Set<Pair<Object, Object>>> ret = copyMap(inst);
		Map<Node, Set<Object>> del = new HashMap<>();
		for (Node node : sig.nodes) {
			del.put(node, new HashSet<>());
		}

		for (Entry<Object, Boolean> k : del0.entrySet()) {
			if (!k.getValue()) {
				del.get(init).add(k.getKey());
			}
		}

        while (true) {
            Pair<Node, Object> toDel = pick(del); // removes in place
            if (toDel == null) {
                return ret;
            }
            Node n = toDel.first;
            Object kill = toDel.second;
            // delete from n, and clear attrs
            remove(ret.get(n.string), kill);
            for (Attribute<Node> a : sig.attrsFor(n)) {
                remove(ret.get(a.name), kill);
            }
            for (Edge e : sig.edgesFrom(n)) {
                remove(ret.get(e.name), kill);
            }
            for (Edge e : sig.edgesTo(n)) {
                Set<Object> cleared = clearX(ret.get(e.name), kill);
                del.get(e.source).addAll(cleared);
            }
        }
	}

	private static Pair<Node, Object> pick(Map<Node, Set<Object>> del) {
		for (Entry<Node, Set<Object>> k : del.entrySet()) {
			Iterator<Object> it = k.getValue().iterator();
			while (it.hasNext()) {
				Pair<Node, Object> ret = new Pair<>(k.getKey(), it.next());
				it.remove();
				return ret;
			}
		}
		return null;
	}

	private Instance filter(LinkedHashMap<Pair<Object, Object>, Boolean> subset)
			throws FQLException {
		Map<String, Set<Pair<Object, Object>>> d = new HashMap<>();

		for (Node n : thesig.nodes) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			for (Pair<Object, Object> k : data.get(n.string)) {
				Boolean b = subset.get(k);
				if (b) {
					set.add(k);
				}
			}
			d.put(n.string, set);
		}
		for (Edge n : thesig.edges) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			for (Pair<Object, Object> k : data.get(n.name)) {
				Boolean b = subset.get(new Pair<>(k.first, k.first));
				Boolean bx = subset.get(new Pair<>(k.second, k.second));
				if (b && bx) {
					set.add(k);
				}
			}
			d.put(n.name, set);
		}
		for (Attribute<Node> n : thesig.attrs) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			for (Pair<Object, Object> k : data.get(n.name)) {
				Boolean b = subset.get(new Pair<>(k.first, k.first));
				if (b) {
					set.add(k);
				}
			}
			d.put(n.name, set);
		}

		return new Instance(thesig, d);
	}

	public LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> flag(
			Node c, Object id) throws FQLException {
		LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> ret = new LinkedHashMap<>();
		Pair<FinCat<Node, Path>, Fn<Path, Arr<Node, Path>>> xxx = thesig
				.toCategory2();
		for (Node d : thesig.nodes) {
			for (Arr<Node, Path> p : xxx.first.hom(c, d)) {
				Object new_id = follow(p.arr, id);
				for (Attribute<Node> a : thesig.attrsFor(d)) {
					ret.put(new Pair<>(p, a),
							PropPSM.lookup(data.get(a.name), new_id));
				}
			}
		}
		return ret;
	}

	// must also return map, take in intref
	private Triple<Instance, Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>>, Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>>> omega(Node c,
																																				 LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> w,
																																				 IntRef ref) throws FQLException {
		Map<String, Set<Pair<Object, Object>>> map = new HashMap<>();

		FinCat<Node, Path> cat = thesig.toCategory2().first;
		Fn<Path, Arr<Node, Path>> fn = thesig.toCategory2().second;
		Map<Node, List<Pair<Arr<Node, Path>, Attribute<Node>>>> obs = thesig
				.obs();

		Map<Node, Map<Object, Pair<Arr<Node, Path>, Object>>> map1 = new HashMap<>();
		Map<Node, Map<Pair<Arr<Node, Path>, Object>, Object>> map2 = new HashMap<>();

		for (Node d : thesig.nodes) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			Map<Object, Pair<Arr<Node, Path>, Object>> m1 = new HashMap<>();
			Map<Pair<Arr<Node, Path>, Object>, Object> m2 = new HashMap<>();
			for (Arr<Node, Path> p : cat.hom(c, d)) {
				for (Pair<Object, Object> y : data.get(d.string)) {
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> rhs = flag(
							d, y.first);
					LinkedHashMap<Pair<Arr<Node, Path>, Attribute<Node>>, Object> lhs = PropPSM
							.truncate2(thesig, w, p, obs.get(d));
					if (lhs.equals(rhs)) {
						Object newid = Integer.toString(++ref.i);
						Pair<Arr<Node, Path>, Object> add = new Pair<>(p,
								y.first);
						set.add(new Pair<>(newid, newid));
						m1.put(newid, add);
						m2.put(add, newid);
					}
				}
			}
			map.put(d.string, set);
			map1.put(d, m1);
			map2.put(d, m2);
		}
		for (Attribute<Node> a : thesig.attrs) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			for (Pair<Object, Object> k : map.get(a.source.string)) {
				Pair<Arr<Node, Path>, Object> v = map1.get(a.source).get(
						k.first); // v.second is ID in I(d)
				set.add(new Pair<>(k.first, lookupX(data.get(a.name), v.second)));
			}
			map.put(a.name, set);
		}
		for (Edge e : thesig.edges) {
			Set<Pair<Object, Object>> set = new HashSet<>();
			for (Pair<Object, Object> k : map.get(e.source.string)) {
				Pair<Arr<Node, Path>, Object> v = map1.get(e.source).get(
						k.first); // v.second is ID in I(d1)
				Object u = lookupX(data.get(e.name), v.second); // is ID in
																// I(d2)
				Arr<Node, Path> j = cat.compose(v.first,
						fn.of(new Path(thesig, e)));
				Pair<Arr<Node, Path>, Object> g = new Pair<>(j, u);
				Object t = map2.get(e.target).get(g);
				set.add(new Pair<>(k.first, t));
			}
			map.put(e.name, set);
		}

		Instance ret = new Instance(thesig, map);
		return new Triple<>(ret, map1, map2);
	}

	public JPanel adom() {
		String str2 = "";
		Set<String> set = new HashSet<>();

		for (Attribute<Node> k : thesig.attrs) {
			Set<String> setX = new HashSet<>();

			for (Pair<Object, Object> v : data.get(k.name)) {
				set.add(v.second.toString());
				setX.add(v.second.toString());
			}
			str2 += k.name + ":\n";

			boolean first = true;
			for (String s : setX) {
				if (!first) {
					str2 += ",\n";
				}
				first = false;
				str2 += ("\"" + s + "\"");
			}

			str2 += "\n\n";
		}

		String str = "";
		boolean first = true;
		for (String s : set) {
			if (!first) {
				str += ",\n";
			}
			first = false;
			str += ("\"" + s + "\"");
		}

		str += "\n\n\n\n ------------------------------- \n\n\n\n";
		str += str2;

		return new CodeTextPanel("Active Domain", str );
	}

}

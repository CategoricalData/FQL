package catdata.opl;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.ide.CodeTextPanel;
import catdata.ide.GuiUtil;
import catdata.opl.OplExp.OplInst;
import catdata.opl.OplExp.OplPres;
import catdata.opl.OplExp.OplSchema;
import catdata.opl.OplExp.OplSig;
import catdata.opl.OplParser.VIt;
import catdata.sql.SqlColumn;
import catdata.sql.SqlForeignKey;
import catdata.sql.SqlInstance;
import catdata.sql.SqlLoader;
import catdata.sql.SqlSchema;
import catdata.sql.SqlTable;
import catdata.sql.SqlType;

public class SqlToOpl extends JPanel {

	public static void showPanel() {
		GuiUtil.show(new SqlToOpl(), 700, 600, "SQL to OPL");
	}

	private final CodeTextPanel output = new CodeTextPanel(BorderFactory.createEtchedBorder(), "OPL Output", "");

	private final SqlLoader input = new SqlLoader(output, "SQL Input");

	private void doRun(boolean cnf) {
		if (input.schema == null) {
			output.setText("Please Run or Load first");
			return;
		}
		String ret = "";
		Pair<OplSchema<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, OplInst<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String, String>> x;
		x = cnf ? convertCnf(input.schema, input.instance, "S0", "I0") : convert(input.schema, input.instance, "S0", "I0");

		ret += "S0 = " + x.first.sig;
		ret += "\n\nS = " + x.first;
		ret += "\n\nI0 = " + x.second.P;
		ret += "\n\nI = instance S I0 none";

		output.setText(ret);
	}

	private SqlToOpl() {
		super(new BorderLayout());

		JButton transButton = new JButton("Translate");
		JButton cnfButton = new JButton("CNF Trans");

		transButton.addActionListener(x -> doRun(false));
		cnfButton.addActionListener(x -> doRun(true));

		JPanel tp = new JPanel(new GridLayout(1, 4));

		tp.add(transButton);
		tp.add(cnfButton);
		tp.add(new JLabel());
		tp.add(new JLabel());

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setDividerSize(4);
		jsp.setResizeWeight(0.5d);
		jsp.add(input);
		jsp.add(output);
		JPanel ret = new JPanel(new GridLayout(1, 1));
		ret.add(jsp);

		add(ret, BorderLayout.CENTER);
		add(tp, BorderLayout.NORTH);

		setBorder(BorderFactory.createEtchedBorder());

	}

	private static final long serialVersionUID = 1L;

	//: happy medium between CNF and non-CNF? ignore as many columns as possible?
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Pair<OplSchema<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, OplInst<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String, String>> convert(SqlSchema info, SqlInstance inst, String S0, String I0) {
		Set<Chc<SqlType, SqlTable>> sorts = new HashSet<>();
		Set<Chc<SqlType, SqlTable>> entities = new HashSet<>();
		Map<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, Pair<List<Chc<SqlType, SqlTable>>, Chc<SqlType, SqlTable>>> symbols = new HashMap<>();
		List<Triple<OplCtx<Chc<SqlType, SqlTable>, String>, OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>>> equations = new LinkedList<>();

		for (SqlType type : info.types) {
			sorts.add(Chc.inLeft(type));
		}

		for (SqlTable table : info.tables) {
			sorts.add(Chc.inRight(table));
			entities.add(Chc.inRight(table));
			for (SqlColumn col : table.columns) {
				symbols.put(Chc.inRight(Chc.inLeft(col)), new Pair<>(Collections.singletonList(Chc.inRight(table)), Chc.inLeft(col.type)));
			}
		}

		for (SqlForeignKey fk : info.fks) {
			symbols.put(Chc.inRight(Chc.inRight(fk)), new Pair<>(Collections.singletonList(Chc.inRight(fk.source)), Chc.inRight(fk.target)));

			OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> head = new OplTerm<>("x");
			OplCtx<Chc<SqlType, SqlTable>, String> ctx = new OplCtx<>(Collections.singletonList(new Pair<>("x", Chc.inRight(fk.source))));

			OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> rhs0 = new OplTerm<>(Chc.inRight(Chc.inRight(fk)), Collections.singletonList(head));

			for (SqlColumn tcol : fk.map.keySet()) {
				SqlColumn scol = fk.map.get(tcol);
				Chc<SqlColumn, SqlForeignKey> l = Chc.inLeft(scol);
				Chc<SqlColumn, SqlForeignKey> r = Chc.inLeft(tcol);
				OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> lhs = new OplTerm<>(Chc.inRight(l), Collections.singletonList(head));
				OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> rhs = new OplTerm<>(Chc.inRight(r), Collections.singletonList(rhs0));
				equations.add(new Triple<>(ctx, lhs, rhs));
			}
		}

		OplSchema<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> sch = new OplSchema<>(S0, entities);

		OplSig<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> sig = new OplSig<>(VIt.vit, new HashMap<>(), sorts, symbols, equations);
		sch.validate(sig);

		OplInst<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String, String> I = new OplInst<>(S0, I0, "none");

		Map<String, Chc<SqlType, SqlTable>> gens = new HashMap<>();
		List<Pair<OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String>, OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String>>> eqs = new LinkedList<>();

		int fr = 0;
		if (inst != null) {
			Map<SqlTable, Map<Map<SqlColumn, Optional<Object>>, String>> iso1 = new HashMap<>();
			//Map<SqlTable, Map<String, Map<SqlColumn, Optional<Object>>>> iso2 = new HashMap<>();

			for (SqlTable table : info.tables) {
				Set<Map<SqlColumn, Optional<Object>>> tuples = inst.get(table);

				Map<Map<SqlColumn, Optional<Object>>, String> i1 = new HashMap<>();
			//	Map<String, Map<SqlColumn, Optional<Object>>> i2 = new HashMap<>();
				for (Map<SqlColumn, Optional<Object>> tuple : tuples) {
					String i = "v" + (fr++);
					i1.put(tuple, i);
				//	i2.put(i, tuple);
					gens.put(i, Chc.inRight(table));
					for (SqlColumn col : table.columns) {
						SqlType ty = col.type;
						Optional<Object> val = tuple.get(col);
						if (!val.isPresent()) {
							continue;
						}
						symbols.put(Chc.inLeft(val.get()), new Pair<>(new LinkedList<>(), Chc.inLeft(ty)));
						OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> rhs = new OplTerm<>(Chc.inLeft(Chc.inLeft(val.get())), new LinkedList<>());
						OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> lhs = new OplTerm<>(Chc.inLeft(Chc.inRight(Chc.inLeft(col))), Collections.singletonList(new OplTerm<>(Chc.inRight(i), new LinkedList<>())));
						eqs.add(new Pair<>(lhs, rhs));
					}
				}
				iso1.put(table, i1);
			//	iso2.put(table, i2);
			}

			for (SqlForeignKey fk : info.fks) {
				for (Map<SqlColumn, Optional<Object>> in : inst.get(fk.source)) {
					Map<SqlColumn, Optional<Object>> out = inst.follow(in, fk);
					String tgen = iso1.get(fk.target).get(out);
					String sgen = iso1.get(fk.source).get(in);
					OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> rhs = new OplTerm<>(Chc.inRight(tgen), new LinkedList<>());
					OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> lhs = new OplTerm<>(Chc.inLeft(Chc.inRight(Chc.inRight(fk))), Collections.singletonList(new OplTerm<>(Chc.inRight(sgen), new LinkedList<>())));

					eqs.add(new Pair<>(lhs, rhs));
				}

			}
		}

		OplPres<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String, String> P = new OplPres<>(new HashMap<>(), S0, sig, gens, eqs);

		P.toSig();
		I.validate(sch, P, null);

		return new Pair<>(sch, I);

	}

	// : code formatter should not wrap lines ever
	private static Pair<OplSchema<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, OplInst<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String, String>> convertCnf(SqlSchema info, SqlInstance inst, String S0, String I0) {
		if (!info.isCnf()) {
			throw new RuntimeException("Schema not in categorical normal form");
		}

		Set<Chc<SqlType, SqlTable>> sorts = new HashSet<>();
		Set<Chc<SqlType, SqlTable>> entities = new HashSet<>();
		Map<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, Pair<List<Chc<SqlType, SqlTable>>, Chc<SqlType, SqlTable>>> symbols = new HashMap<>();
		List<Triple<OplCtx<Chc<SqlType, SqlTable>, String>, OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, OplTerm<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>>> equations = new LinkedList<>();

		for (SqlType type : info.types) {
			sorts.add(Chc.inLeft(type));
		}

		for (SqlTable table : info.tables) {
			sorts.add(Chc.inRight(table));
			entities.add(Chc.inRight(table));
			for (SqlColumn col : table.columns) {
				if (col.equals(table.getCnfId())) {
					continue;
				}
				if (isFk(info, table, col)) {
					continue;
				}
				symbols.put(Chc.inRight(Chc.inLeft(col)), new Pair<>(Collections.singletonList(Chc.inRight(table)), Chc.inLeft(col.type)));
			}
		}

		for (SqlForeignKey fk : info.fks) {
			symbols.put(Chc.inRight(Chc.inRight(fk)), new Pair<>(Collections.singletonList(Chc.inRight(fk.source)), Chc.inRight(fk.target)));
		}

		OplSchema<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> sch = new OplSchema<>(S0, entities);

		OplSig<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String> sig = new OplSig<>(VIt.vit, new HashMap<>(), sorts, symbols, equations);
		sch.validate(sig);

		OplInst<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String, String> I = new OplInst<>(S0, I0, "none");

		Map<String, Chc<SqlType, SqlTable>> gens = new HashMap<>();
		List<Pair<OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String>, OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String>>> eqs = new LinkedList<>();

		int fr = 0;
		if (inst != null) {
			Map<SqlTable, Map<Object, String>> iso1 = new HashMap<>();
			//Map<SqlTable, Map<String, Object>> iso2 = new HashMap<>();

			for (SqlTable table : info.tables) {
				Set<Map<SqlColumn, Optional<Object>>> tuples = inst.get(table);

				Map<Object, String> i1 = new HashMap<>();
				//Map<String, Object> i2 = new HashMap<>();
				for (Map<SqlColumn, Optional<Object>> tuple : tuples) {
					String i = "v" + (fr++);
					if (!tuple.get(table.getCnfId()).isPresent()) {
						throw new RuntimeException("Anomly: please report");
					}
					i1.put(tuple.get(table.getCnfId()).get(), i);
					//i2.put(i, tuple.get(table.getCnfId()).get());
					gens.put(i, Chc.inRight(table));
					for (SqlColumn col : table.columns) {
						if (col.equals(table.getCnfId())) {
							continue;
						}
						if (isFk(info, table, col)) {
							continue;
						}
						SqlType ty = col.type;
						Optional<Object> val = tuple.get(col);
						if (!val.isPresent()) {
							continue;
						}
						symbols.put(Chc.inLeft(val.get()), new Pair<>(new LinkedList<>(), Chc.inLeft(ty)));
						OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> rhs = new OplTerm<>(Chc.inLeft(Chc.inLeft(val.get())), new LinkedList<>());
						OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> lhs = new OplTerm<>(Chc.inLeft(Chc.inRight(Chc.inLeft(col))), Collections.singletonList(new OplTerm<>(Chc.inRight(i), new LinkedList<>())));
						eqs.add(new Pair<>(lhs, rhs));
					}
				}
				iso1.put(table, i1);
				//iso2.put(table, i2);
			}

			for (SqlForeignKey fk : info.fks) {
				for (Map<SqlColumn, Optional<Object>> in : inst.get(fk.source)) {
					Map<SqlColumn, Optional<Object>> out = inst.follow(in, fk);
					if (!out.get(fk.target.getCnfId()).isPresent() || !in.get(fk.source.getCnfId()).isPresent()) {
						throw new RuntimeException("Anomaly: please report");
					}
					String tgen = iso1.get(fk.target).get(out.get(fk.target.getCnfId()).get());
					String sgen = iso1.get(fk.source).get(in.get(fk.source.getCnfId()).get());
					OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> rhs = new OplTerm<>(Chc.inRight(tgen), new LinkedList<>());
					OplTerm<Chc<Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String>, String> lhs = new OplTerm<>(Chc.inLeft(Chc.inRight(Chc.inRight(fk))), Collections.singletonList(new OplTerm<>(Chc.inRight(sgen), new LinkedList<>())));

					eqs.add(new Pair<>(lhs, rhs));
				}
			}

		}

		OplPres<Chc<SqlType, SqlTable>, Chc<Object, Chc<SqlColumn, SqlForeignKey>>, String, String> P = new OplPres<>(new HashMap<>(), S0, sig, gens, eqs);

		P.toSig();
		I.validate(sch, P, null);

		return new Pair<>(sch, I);

	}

	private static boolean isFk(SqlSchema info, SqlTable src, SqlColumn col) {
		for (SqlForeignKey fk : info.fks) {
			if (!fk.source.equals(src)) {
				continue;
			}
			for (SqlColumn tcol : fk.map.keySet()) {
				if (fk.map.get(tcol).equals(col)) {
					return true;
				}
			}
		}
		return false;
	}

}

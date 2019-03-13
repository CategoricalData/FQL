package catdata.fql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.LineException;
import catdata.Pair;
import catdata.Triple;
import catdata.fql.FqlOptions.SQLKIND;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Driver;
import catdata.fql.decl.Edge;
import catdata.fql.decl.FQLProgram;
import catdata.fql.decl.InstExp;
import catdata.fql.decl.InstExp.Delta;
import catdata.fql.decl.InstExp.Exp;
import catdata.fql.decl.InstExp.External;
import catdata.fql.decl.InstExp.Kernel;
import catdata.fql.decl.InstExp.One;
import catdata.fql.decl.InstExp.Times;
import catdata.fql.decl.InstExp.Two;
import catdata.fql.decl.InstOps;
import catdata.fql.decl.Node;
import catdata.fql.decl.SigExp.Const;
import catdata.fql.decl.Signature;
import catdata.fql.decl.TransExp;
import catdata.fql.sql.CreateTable;
import catdata.fql.sql.ExpPSM;
import catdata.fql.sql.FullSigma;
import catdata.fql.sql.InsertValues;
import catdata.fql.sql.PSM;
import catdata.fql.sql.PSMGen;
import catdata.fql.sql.PSMInterp;
import catdata.fql.sql.PSMUnChi;
import catdata.fql.sql.PropPSM;
import catdata.fql.sql.SimpleCreateTable;
import catdata.ide.DefunctGlobalOptions;

//todo always execute postlude

/**
 * 
 * @author ryan
 * 
 *         Class for communicating with external sql engines over jdbc.
 */
public class JDBCBridge {

	private static List<PSM> maybeExecTransform(InstOps ops, FQLProgram prog,
			Statement Stmt, String k, TransExp v, PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> ret) throws SQLException {
		List<PSM> psm = new LinkedList<>();

		Pair<String, String> val = prog.transforms.get(k).type(prog);
		InstExp i = prog.insts.get(val.first);
		Const ss = i.type(prog).toConst(prog);
		Signature s = ss.toSig(prog);
		psm.addAll(PSMGen.makeTables(k, s, false));
		switch (DefunctGlobalOptions.debug.fql.sqlKind) {
		case NATIVE:
			psm.addAll(v.accept(k, ops));
			interp.interpX(psm, ret);
			break;
		case H2:
		case JDBC:
		default:
			if (v instanceof TransExp.External
					&& DefunctGlobalOptions.debug.fql.sqlKind == SQLKIND.H2) {

			} else {
				psm.addAll(v.accept(k, ops));
			}
			maybeExec(psm, Stmt, ret, interp, s);
			if (v.gather()) {
				gatherTransform(prog, ret, Stmt, k, ss);
				//  have non SQL transform output into temps, so can gather them like any other
			}
			break;
		}
		return psm;
	}
	
	private static List<PSM> maybeExecInstance(InstOps ops, FQLProgram prog,
			Statement Stmt, String k, InstExp v, PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> ret) throws SQLException {

		List<PSM> psm = new LinkedList<>();
		psm.addAll(PSMGen.makeTables(k, v.type(prog).toSig(prog), false));
		switch (DefunctGlobalOptions.debug.fql.sqlKind) {
		case NATIVE:
			psm.addAll(v.accept(k, ops).first);
			interp.interpX(psm, ret);
			break;
		case H2:
		case JDBC:
		default:
			if (v instanceof InstExp.FullSigma) {
				List<PSM> xxx = v.accept(k, ops).first;
				if (xxx.size() != 1) {
					throw new RuntimeException();
				}
				FullSigma yyy = (FullSigma) xxx.get(0);
				int theguid = getGuid(Stmt);
				interp.guid = theguid;
				yyy.exec(interp, ret);
				Stmt.execute("SET @guid = " + interp.guid);
				psm.addAll(makeInserts(k, ret, v.type(prog).toSig(prog),
						((InstExp.FullSigma) v).F.toMap(prog).source));
			} else if (v instanceof Exp) {
				List<PSM> xxx = v.accept(k, ops).first;
				if (xxx.size() != 1) {
					throw new RuntimeException();
				}
				ExpPSM yyy = (ExpPSM) xxx.get(0);
				int theguid = getGuid(Stmt);
				interp.guid = theguid;
				yyy.exec(interp, ret);
				Stmt.execute("SET @guid = " + interp.guid);
				psm.addAll(makeInserts(k, ret, v.type(prog).toSig(prog), null));
			} else if (v instanceof Two) {
				List<PSM> xxx = v.accept(k, ops).first;
				if (xxx.size() != 1) {
					throw new RuntimeException();
				}
				PropPSM yyy = (PropPSM) xxx.get(0);
				int theguid = getGuid(Stmt);
				interp.guid = theguid;
				yyy.exec(interp, ret);
				Stmt.execute("SET @guid = " + interp.guid);
				
				psm.addAll(makeInserts(k, ret, v.type(prog).toSig(prog), null));
			} else if (v instanceof Kernel) {
				List<PSM> xxx = v.accept(k, ops).first;
				if (xxx.size() != 1) {
					throw new RuntimeException();
				}
				PSMUnChi yyy = (PSMUnChi) xxx.get(0);
				int theguid = getGuid(Stmt);
				interp.guid = theguid;
				yyy.exec(interp, ret);
				Stmt.execute("SET @guid = " + interp.guid);
				psm.addAll(makeInserts(k, ret, v.type(prog).toSig(prog), null));
				Signature ooo = v.type(prog).toSig(prog);
				for (Node n : ooo.nodes) {
					psm.add(new SimpleCreateTable(k + "_trans_" + n.string, PSM.VARCHAR(), false));
				}
				for (Edge n : ooo.edges) {
					psm.add(new SimpleCreateTable(k + "_trans_" + n.name, PSM.VARCHAR(), false));
				}
				for (Attribute<Node> n : ooo.attrs) {
					psm.add(new SimpleCreateTable(k + "_trans_" + n.name, n.target.psm(), false));
				}
				psm.addAll(makeInserts(k + "_trans", ret, v.type(prog).toSig(prog), null));
			}

			else if (v instanceof External
					&& DefunctGlobalOptions.debug.fql.sqlKind == SQLKIND.H2) {
			} else {
				psm.addAll(v.accept(k, ops).first);
			}
			for (PSM sql : psm) {
				Stmt.execute(sql.toPSM());
			}
			if (!(v instanceof InstExp.FullSigma)
					&& !(v instanceof Exp)
					&& !(v instanceof Two) && !(v instanceof Kernel)) {
				gatherInstance(prog, ret, Stmt, k, v);
			}
			if (v instanceof Delta) {
				gatherSubstInv(prog, ret, Stmt, k, v);
			} else if (v instanceof Times) {
				gatherTransform(prog, ret, Stmt, k + "_fst",
						v.type(prog).toConst(prog));
				gatherTransform(prog, ret, Stmt, k + "_snd",
						v.type(prog).toConst(prog));
			} else if (v instanceof One) {
				gatherSubstInv2(prog, ret, Stmt, k, v);
			}
							
			break;
		}
		return psm;
	}

	public static Triple<Map<String, Set<Map<Object, Object>>>, String, List<Throwable>> run(
			FQLProgram prog) {
		Map<String, Set<Map<Object, Object>>> ret = new HashMap<>();
		List<Throwable> exns = new LinkedList<>();
		InstOps ops = new InstOps(prog);
		PSMInterp interp = new PSMInterp();
		Connection Conn;
		Statement Stmt = null;
		List<PSM> sqls = new LinkedList<>();
		try {
			switch (DefunctGlobalOptions.debug.fql.sqlKind) {
			case H2:
				Class.forName("org.h2.Driver");
				Conn = DriverManager.getConnection("jdbc:h2:mem:");
				Stmt = Conn.createStatement();
				Stmt.execute("SET @GUID = 0");
				break;
			case JDBC:
				Class.forName(DefunctGlobalOptions.debug.fql.jdbcClass);
				Conn = DriverManager.getConnection(DefunctGlobalOptions.debug.fql.jdbcUrl);
				Stmt = Conn.createStatement();
				String[] prel = DefunctGlobalOptions.debug.fql.prelude.split(";");
				for (String s : prel) {
					Stmt.execute(s);
				}
				break;
			case NATIVE:
				break;
			default:
				throw new RuntimeException();
			}

			for (String k : prog.order) {
				InstExp v1 = prog.insts.get(k);
				TransExp v2 = prog.transforms.get(k);

				try {
					if (v1 != null) {
						sqls.addAll(maybeExecInstance(ops, prog, Stmt, k, v1,
								interp, ret));
					} else if (v2 != null) {
						sqls.addAll(maybeExecTransform(ops, prog, Stmt, k, v2,
								interp, ret));
					}
				} catch (Throwable re) {
					String thing = (v1 == null) ? "transform" : "instance";
					re.printStackTrace();
					LineException exn = new LineException(
							re.getLocalizedMessage(), k, thing);
					if (DefunctGlobalOptions.debug.fql.continue_on_error) {
						exns.add(exn);
					} else {
						if (DefunctGlobalOptions.debug.fql.sqlKind == SQLKIND.JDBC) {
							String[] prel0 = DefunctGlobalOptions.debug.fql.afterlude.split(";");
							for (String s : prel0) {
								if (!s.trim().isEmpty()) {
									if (Stmt == null) {
										throw new RuntimeException("Anomaly: please report");
									}
									Stmt.execute(s);
								}
							}
						}
						throw exn;
					}
				}
			}

			List<PSM> drops = Driver.computeDrops(prog);

			if (DefunctGlobalOptions.debug.fql.sqlKind == SQLKIND.JDBC) {
				for (PSM dr : drops) {
					if (Stmt == null) {
						throw new RuntimeException("Anomaly: please report");
					}
					Stmt.execute(dr.toPSM());
				}

				String[] prel0 = DefunctGlobalOptions.debug.fql.afterlude.split(";");
				for (String s : prel0) {
					if (!s.trim().isEmpty()) {
						if (Stmt == null) {
							throw new RuntimeException("Anomaly: please report");
						}
						Stmt.execute(s);
					}
				}
			}

			String str;
			try {
				str = DefunctGlobalOptions.debug.fql.prelude
						+ "\n\n"
						+ PSMGen.prettyPrint(sqls)
						+ "\n\n"
						+ (drops.isEmpty() ? "" : PSMGen.prettyPrint(drops)
								+ "\n\n") + DefunctGlobalOptions.debug.fql.afterlude + "\n\n";
			} catch (RuntimeException re) {
				str = re.getLocalizedMessage();
			}

			return new Triple<>(ret, str, exns);
		} catch (Exception exception) {
			if (exception instanceof LineException) {
				throw ((LineException) exception);
			}
			exception.printStackTrace();
			throw new RuntimeException(exception.getLocalizedMessage());
		}
	}

	private static void maybeExec(List<PSM> sqls, Statement stmt,
			Map<String, Set<Map<Object, Object>>> state, PSMInterp interp,
			Signature s) throws SQLException {
		for (PSM sql : sqls) {
			String k = sql.isSql();
			if (k == null) {
				stmt.execute(sql.toPSM());
			} else {
				sql.exec(interp, state);
				List<PSM> yyy = makeInserts(k, state, s, null);
				for (PSM y : yyy) {
					stmt.execute(y.toPSM());
				}
			}
		}
	}

	private static int getGuid(Statement stmt) throws SQLException {
		stmt.execute("CREATE TABLE GUID_TEMP_TABLE_XXX_YYY(C0 INTEGER)");
		stmt.execute("INSERT INTO GUID_TEMP_TABLE_XXX_YYY VALUES (@GUID)");
		ResultSet RS = stmt
				.executeQuery("SELECT * FROM GUID_TEMP_TABLE_XXX_YYY");
		Integer i = null;
		while (RS.next()) {
			i = (Integer) RS.getObject("C0");
			break;
		}
		RS.close();
		stmt.execute("DROP TABLE GUID_TEMP_TABLE_XXX_YYY");
		if (i == null) {
			throw new RuntimeException("Cannot get GUID from database");
		}
		return i;
	}

	private static List<PSM> makeInserts(String k,
			Map<String, Set<Map<Object, Object>>> state, Signature sig,
			Signature src_sig) {
		List<PSM> ret = new LinkedList<>();

		List<String> attrs = new LinkedList<>();
		attrs.add("c0");
		attrs.add("c1");

		if (src_sig != null) {
			for (Node n : src_sig.nodes) {
				Set<Map<Object, Object>> v = state.get(k + "_" + n.string
						+ "_e");
				ret.add(new SimpleCreateTable(k + "_" + n.string + "_e", PSM
						.VARCHAR(), false));
				if (v.isEmpty()) {
					continue;
				}
				ret.add(new InsertValues(k + "_" + n.string + "_e", attrs, v));
			}
			Set<Map<Object, Object>> v = state.get(k + "_lineage");
			Map<String, String> at = new LinkedHashMap<>();
			at.put("c0", PSM.VARCHAR());
			at.put("c1", PSM.VARCHAR());
			at.put("c2", PSM.VARCHAR());
			at.put("c3", PSM.VARCHAR());
			ret.add(new CreateTable(k + "_lineage", at, false));
			if (!v.isEmpty()) {
				ret.add(new InsertValues(k + "_lineage", new LinkedList<>(at
						.keySet()), v));
			}
		}

		for (Node n : sig.nodes) {
			Set<Map<Object, Object>> v = state.get(k + "_" + n.string);
			if (v.isEmpty()) {
				continue;
			}
			ret.add(new InsertValues(k + "_" + n.string, attrs, v));
		}
		for (Edge e : sig.edges) {
			Set<Map<Object, Object>> v = state.get(k + "_" + e.name);
			if (v.isEmpty()) {
				continue;
			}
			ret.add(new InsertValues(k + "_" + e.name, attrs, v));
		}
		for (Attribute<Node> a : sig.attrs) {
			Set<Map<Object, Object>> v = state.get(k + "_" + a.name);
			if (v.isEmpty()) {
				continue;
			}
			ret.add(new InsertValues(k + "_" + a.name, attrs, v));
		}

		return ret;
	}

	private static void gatherTransform(@SuppressWarnings("unused") FQLProgram prog,
			Map<String, Set<Map<Object, Object>>> ret, Statement Stmt,
			String k, Const t) throws SQLException {

		for (String n : t.nodes) {
			ResultSet RS = Stmt
					.executeQuery("SELECT c0,c1 FROM " + k + "_" + n);
			Set<Map<Object, Object>> ms = new HashSet<>();
			while (RS.next()) {
				Map<Object, Object> m = new HashMap<>();
				m.put("c0", RS.getObject("c0"));
				m.put("c1", RS.getObject("c1"));
				ms.add(m);
			}
			RS.close();
			ret.put(k + "_" + n, ms);
		}
		for (Triple<String, String, String> n : t.arrows) {
			ret.put(k + "_" + n.first, new HashSet<>());
		}
		for (Triple<String, String, String> n : t.attrs) {
			ret.put(k + "_" + n.first, new HashSet<>());
		}
	}

	private static void gatherSubstInv2(FQLProgram prog,
			Map<String, Set<Map<Object, Object>>> ret, Statement Stmt,
			String k, InstExp v) throws SQLException {

		Const t = v.type(prog).toConst(prog);

		for (String n : t.nodes) {
			ResultSet RS = Stmt.executeQuery("SELECT c0,c1 FROM " + k + "_" + n
					+ "_subst_inv");
			Set<Map<Object, Object>> ms = new HashSet<>();
			while (RS.next()) {
				Map<Object, Object> m = new HashMap<>();
				m.put("c0", RS.getObject("c0").toString());
				m.put("c1", RS.getObject("c1").toString());
				ms.add(m);
			}
			RS.close();
			ret.put(k + "_" + n + "_subst_inv", ms);
		}
	} 
	
	private static void gatherSubstInv(FQLProgram prog,
			Map<String, Set<Map<Object, Object>>> ret, Statement Stmt,
			String k, InstExp v) throws SQLException {

		Const t = v.type(prog).toConst(prog);

		for (String n : t.nodes) {
			ResultSet RS = Stmt.executeQuery("SELECT c0,c1 FROM " + k + "_" + n
					+ "_subst_inv");
			Set<Map<Object, Object>> ms = new HashSet<>();
			while (RS.next()) {
				Map<Object, Object> m = new HashMap<>();
				m.put("c0", Integer.parseInt(RS.getObject("c0").toString()));
				m.put("c1", Integer.parseInt(RS.getObject("c1").toString()));
				ms.add(m);
			}
			RS.close();
			ret.put(k + "_" + n + "_subst_inv", ms);
		}
	}

	private static void gatherInstance(FQLProgram prog,
			Map<String, Set<Map<Object, Object>>> ret, Statement Stmt,
			String k, InstExp v) throws SQLException {
		Const t = v.type(prog).toConst(prog);

		for (String n : t.nodes) {
			ResultSet RS = Stmt
					.executeQuery("SELECT c0,c1 FROM " + k + "_" + n);
			Set<Map<Object, Object>> ms = new HashSet<>();
			while (RS.next()) {
				Map<Object, Object> m = new HashMap<>();
				m.put("c0", RS.getObject("c0"));
				m.put("c1", RS.getObject("c1"));
				ms.add(m);
			}
			RS.close();
			ret.put(k + "_" + n, ms);
		}
		for (Triple<String, String, String> n : t.attrs) {
			ResultSet RS = Stmt.executeQuery("SELECT c0,c1 FROM " + k + "_"
					+ n.first);
			Set<Map<Object, Object>> ms = new HashSet<>();
			while (RS.next()) {
				Map<Object, Object> m = new HashMap<>();
				m.put("c0", RS.getObject("c0"));
				m.put("c1", RS.getObject("c1"));
				ms.add(m);
			}
			RS.close();
			ret.put(k + "_" + n.first, ms);
		}
		for (Triple<String, String, String> n : t.arrows) {
			ResultSet RS = Stmt.executeQuery("SELECT c0,c1 FROM " + k + "_"
					+ n.first);
			Set<Map<Object, Object>> ms = new HashSet<>();
			while (RS.next()) {
				Map<Object, Object> m = new HashMap<>();
				m.put("c0", RS.getObject("c0"));
				m.put("c1", RS.getObject("c1"));
				ms.add(m);
			}
			RS.close();
			ret.put(k + "_" + n.first, ms);
		}
	}

}

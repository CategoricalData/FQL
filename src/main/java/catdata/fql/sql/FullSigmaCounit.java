package catdata.fql.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Node;

public class FullSigmaCounit extends PSM {
	
	private final Mapping F;
	private final String i1;
    private final String i2;
    private final String i3;
	private final String trans;
	
	
	public FullSigmaCounit(Mapping F, String i1, String i2, String i3, String trans) {
		this.F = F;
		this.i1 = i1;
		this.i2 = i2;
		this.i3 = i3;
		this.trans = trans;
	}

	@Override
	public void exec(PSMInterp interp,
			Map<String, Set<Map<Object, Object>>> state) {
		Set<Map<Object, Object>> lineage = state.get(i3 + "_lineage");
		
		for (Node n : F.target.nodes) {
			Set<Map<Object, Object>> i3i = state.get(i3 + "_" + n);
			Set<Map<Object, Object>> m = new HashSet<>();
			for (Map<Object, Object> row : i3i) {
				Object id = row.get("c0").toString();
				for (Map<Object, Object> v : lineage) {
					Object id0 = v.get("c0").toString();
					if (id.equals(id0)) {
						String node = v.get("c1").toString();
						String idX = v.get("c2").toString();
						String[] cols = v.get("c3").toString().split("\\s+");
						
						Set<Map<Object, Object>> subst_inv = state.get(i2 + "_" + node + "_subst_inv");
						for (Map<Object, Object> y : subst_inv) {
							if (y.get("c0").toString().equals(idX)) {
								String ret = y.get("c1").toString();
								for (String col : cols) {
									if (col.trim().isEmpty()) {
										continue;
									}
									Set<Map<Object, Object>> u = state.get(i1 + "_" + col);
									for (Map<Object, Object> e : u) {
										if (e.get("c0").toString().equals(ret)) {
											ret = e.get("c1").toString();
										}
									}
								}
								Map<Object, Object> rowX = new HashMap<>();
								rowX.put("c0", id);
								rowX.put("c1", ret);
								m.add(rowX);
							}
						}
					}
				}
			}
			state.put(trans + "_" + n, m);			
		}
		
		for (Attribute<Node> n : F.target.attrs) {
			state.put(trans + "_" + n.name, new HashSet<>());
		}
		for (Edge n : F.target.edges) {
			state.put(trans + "_" + n.name, new HashSet<>());
		}
	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for full sigma counit");
	}
	
	@Override
	public String isSql() {
		return trans;
	}


}

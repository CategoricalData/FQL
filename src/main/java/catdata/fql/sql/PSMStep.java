package catdata.fql.sql;

import java.util.Map;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Signature;

public class PSMStep extends PSM {
	
	private final String pre;
    //private final String I;
	private final Mapping m;
    private final Mapping n;

	@Override
	public String isSql() {
		return pre;
	}
	
	public PSMStep(String pre, @SuppressWarnings("unused") String i, Mapping m, Mapping n) {
        this.pre = pre;
	//	I = i;
		this.m = m;
		this.n = n;
	}

	@Override
	public void exec(PSMInterp interp, Map<String, Set<Map<Object, Object>>> state) {
		try {
		//Signature w = m.source;
		Signature r = n.source;
		if (!m.target.equals(r)) {
			throw new RuntimeException("Cannot compose " + m + " and " + n);
		}
	
		/*Instance star_w = Instance.terminal(w, "0"); 
		Instance star_r = Instance.terminal(r, "0"); 
		
		Quad<Instance, Map<Node, Map<Object, Integer>>, Map<Node, Map<Integer, Object>>, Map<Object, List<Pair<String, Object>>>> sigma_m_star_w_0 = LeftKanSigma.fullSigmaWithAttrs(interp, m, star_w, null, null, null);
		Instance sigma_m_star_w = sigma_m_star_w_0.first;
		List<Pair<String, List<Pair<Object, Object>>>> l = new LinkedList<>();
		for (Node o : sigma_m_star_w.thesig.nodes) {
			List<Pair<Object, Object>> x = new LinkedList<>();
			for (Pair<Object, Object> k : sigma_m_star_w.data.get(o.string)) {
				x.add(new Pair<Object, Object>(k.first, "0"));
			}
			l.add(new Pair<>(o.string, x));
		}
		Transform h = new Transform(sigma_m_star_w, star_r, l );
		*/
		
		
		
		
		
		/*Use the terminal map 
		 * 
Sigma_m(*_W)-->*_R.
Then apply Sigma_n to both sides. */
		
		//  Auto-generated method stub
		throw new FQLException("sd");
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getMessage());
		}

	}

	@Override
	public String toPSM() {
		throw new RuntimeException("Cannot generate SQL for step.");
	}
	
	


}

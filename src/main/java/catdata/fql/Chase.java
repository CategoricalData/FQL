package catdata.fql;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import catdata.Pair;
import catdata.Triple;
import catdata.fql.decl.Attribute;
import catdata.fql.decl.Edge;
import catdata.fql.decl.Instance;
import catdata.fql.decl.Mapping;
import catdata.fql.decl.Node;
import catdata.fql.decl.Signature;
import catdata.fql.parse.FqlTokenizer;
import catdata.fql.parse.KeywordParser;
import catdata.fql.parse.ParserUtils;
import catdata.fql.parse.Partial;
import catdata.fql.parse.RyanParser;
import catdata.fql.parse.StringParser;
import catdata.fql.parse.Tokens;
import catdata.fql.sql.ED;
import catdata.fql.sql.EmbeddedDependency;
import catdata.fql.sql.Flower;
import catdata.ide.CodeTextPanel;

/**
 * 
 * @author ryan
 * 
 *         Class for running the chase.
 */
public class Chase {

	private static final JButton run = new JButton("Run");

	public static void dostuff() {
		JFrame f = new JFrame("Chaser");

		run.addActionListener((ActionEvent e) -> {
                    try {
                        Pair<String, String> x = run(eds.getText(), inst.getText(),
                                (KIND) box.getSelectedItem());
                        res.setText(x.second);
                        eds0.setText(x.first);
                    } catch (Throwable ee) {
                        ee.printStackTrace();
                        res.setText(ee.toString());
                    }
                });

		JPanel ret = new JPanel(new BorderLayout());

		JPanel inner = new JPanel(new GridLayout(3, 1));

		inner.add(eds);
		inner.add(inst);
		inner.add(res);

		JPanel bar = new JPanel();
		bar.add(run);
		bar.add(box);

		ret.add(inner, BorderLayout.CENTER);
		ret.add(bar, BorderLayout.NORTH);

		f.setContentPane(ret);
		f.setSize(800, 600);
		f.setVisible(true);

	}

	private static final JComboBox<KIND> box = new JComboBox<>(new KIND[] { KIND.PARALLEL, KIND.STANDARD,
			KIND.CORE, KIND.HYBRID });
	private static final CodeTextPanel eds = new CodeTextPanel("EDs", "forall x, S(x,x) -> exists y, T(y,y)");
	private static final CodeTextPanel eds0 = new CodeTextPanel("Simplified EDs", "");
	//static CodeTextPanel eds1 = new CodeTextPanel("Tuple EDs", "");
	private static final CodeTextPanel inst = new CodeTextPanel("Instance", "S -> {(a,a),(b,b)}, T -> {}");
	private static final CodeTextPanel res = new CodeTextPanel("Result", "");

	private static final RyanParser<EmbeddedDependency> ed_p = make_ed_p();
	private static final RyanParser<Pair<String, List<Pair<String, String>>>> inst_p0 = make_inst_p0();

	private static final RyanParser<List<EmbeddedDependency>> eds_p = ParserUtils.many(ed_p);
	private static final RyanParser<List<Pair<String, List<Pair<String, String>>>>> inst_p = ParserUtils.manySep(
			inst_p0, new KeywordParser(","));

	private static Pair<String, String> run(String eds, String inst, KIND kind) throws Exception {
		Partial<List<EmbeddedDependency>> xxx = eds_p.parse(new FqlTokenizer(eds));
		List<EmbeddedDependency> eds0 = xxx.value;

		if (!xxx.tokens.toString().trim().isEmpty()) {
			throw new FQLException("Unconsumed input: " + xxx.tokens);
		}

		Partial<List<Pair<String, List<Pair<String, String>>>>> yyy = inst_p
				.parse(new FqlTokenizer(inst));
		Map<String, Set<Pair<Object, Object>>> inst0 = conv(yyy.value);

		if (!yyy.tokens.toString().trim().isEmpty()) {
			throw new FQLException("Unconsumed input: " + yyy.tokens);
		}

		Pair<List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>>, List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>>> zzz = split(eds0);

		if (inst0.isEmpty()) {
			return new Pair<>(printNicely3(zzz), "");
		}

		Map<String, Set<Pair<Object, Object>>> res = chase(new HashSet<>(), zzz, inst0, kind);

		return new Pair<>(printNicely3(zzz), printNicely(res));
	}

	private static String printNicely3(
			Pair<List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>>, List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>>> zzz) {
		String ret = "";
		for (Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>> ed : zzz.first) {
			ret += ed + "\n\n";
		}
		for (Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>> ed : zzz.second) {
			ret += ed + "\n\n";
		}
		return ret.trim();
	}

	private static RyanParser<Pair<String, List<Pair<String, String>>>> make_inst_p0() {
		return ParserUtils.inside(new StringParser(), new KeywordParser("->"), ParserUtils.outside(
				new KeywordParser("{"), ParserUtils.manySep(ParserUtils.outside(new KeywordParser(
						"("), ParserUtils.inside(new StringParser(), new KeywordParser(","),
						new StringParser()), new KeywordParser(")")), new KeywordParser(",")),
				new KeywordParser("}")));
	}

	private static RyanParser<EmbeddedDependency> make_ed_p() {
		return (Tokens s) -> {
                    RyanParser<List<String>> strings = ParserUtils.many(new StringParser());
                    RyanParser<List<Triple<String, String, String>>> where1 = ParserUtils.manySep(
                            facts_p(), new KeywordParser("/\\"));
                    
                    RyanParser<Pair<List<Triple<String, String, String>>, List<Pair<String, String>>>> where2 = facts_p2();
                    
                    RyanParser<Pair<List<String>, List<Triple<String, String, String>>>> p = ParserUtils
                            .seq(new KeywordParser("forall"),
                                    ParserUtils.inside(strings, new KeywordParser(","), where1));
                    
                    RyanParser<Pair<List<String>, Pair<List<Triple<String, String, String>>, List<Pair<String, String>>>>> q = ParserUtils
                            .seq(new KeywordParser("exists"),
                                    ParserUtils.inside(strings, new KeywordParser(","), where2));
                    
                    RyanParser<Pair<Pair<List<String>, List<Triple<String, String, String>>>, Pair<List<String>, Pair<List<Triple<String, String, String>>, List<Pair<String, String>>>>>> xxx = ParserUtils
                            .inside(p, new KeywordParser("->"), q);
                    
                    Partial<Pair<Pair<List<String>, List<Triple<String, String, String>>>, Pair<List<String>, Pair<List<Triple<String, String, String>>, List<Pair<String, String>>>>>> yyy = xxx
                            .parse(s);
                    s = yyy.tokens;
                    Pair<Pair<List<String>, List<Triple<String, String, String>>>, Pair<List<String>, Pair<List<Triple<String, String, String>>, List<Pair<String, String>>>>> t = yyy.value;
                    
                    return new Partial<>(s, new EmbeddedDependency(t.first.first,
                            t.second.first, t.first.second, t.second.second.first,
                            t.second.second.second));
                };
	}

        @SuppressWarnings("unchecked")
		private static RyanParser<Pair<List<Triple<String, String, String>>, List<Pair<String, String>>>> facts_p2() {
		return (Tokens s) -> {
                    RyanParser<List<Object>> objs0 = ParserUtils.manySep(
                            ParserUtils.or(facts_p(), eq_p()), new KeywordParser("/\\"));
                    Partial<List<Object>> par = objs0.parse(s);
                    List<Object> objs = par.value;
                    s = par.tokens;
                    
                    List<Triple<String, String, String>> l1 = new LinkedList<>();
                    List<Pair<String, String>> l2 = new LinkedList<>();
                    
                    for (Object o : objs) {
                        if (o instanceof Triple) {
                            l1.add((Triple<String, String, String>) o);
                        } else {
                            l2.add((Pair<String, String>) o);
                        }
                    }
                    
                    return new Partial<>(s, new Pair<>(l1, l2));
                };
	}

	private static RyanParser<Pair<String, String>> eq_p() {
		return ParserUtils.inside(new StringParser(), new KeywordParser("="), new StringParser());
	}

	private static RyanParser<Triple<String, String, String>> facts_p() {
		return (Tokens s) -> {
                    StringParser p = new StringParser();
                    
                    String b;
                    String a;
                    String r;
                    
                    Partial<String> x = p.parse(s);
                    s = x.tokens;
                    r = x.value;
                    
                    RyanParser<Pair<String, String>> h = ParserUtils.outside(new KeywordParser("("),
                            ParserUtils.inside(new StringParser(), new KeywordParser(","),
                                    new StringParser()), new KeywordParser(")"));
                    Partial<Pair<String, String>> y = h.parse(s);
                    s = y.tokens;
                    a = y.value.first;
                    b = y.value.second;
                    
                    return new Partial<>(s,
                            new Triple<>(r, a, b));
                };
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map<String, Set<Pair<Object, Object>>> conv(
			List<Pair<String, List<Pair<String, String>>>> value) {
		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>();

		for (Pair<String, List<Pair<String, String>>> k : value) {
			ret.put(k.first, new HashSet(k.second));
		}
		return ret;
	}

	private static String printNicely(Map<String, Set<Pair<Object, Object>>> m) {
		String ret = "";
		for (String k : m.keySet()) {
			ret += k + " = {" + printNicely2(m.get(k)) + "}";
			ret += "\n";
		}
		return ret;
	}

	private static String printNicely2(Set<Pair<Object, Object>> set) {
		String ret = "";
		for (Pair<Object, Object> p : set) {
			ret += p.toString();
		}
		return ret;
	}

	private static Pair<List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>>, List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>>> split(
			List<EmbeddedDependency> l) {
		List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>> ret1 = new LinkedList<>();
		List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>> ret2 = new LinkedList<>();
		for (EmbeddedDependency e : l) {
			Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>> s = new Triple<>(
					e.forall, e.where, e.tgd);
			Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>> t = new Triple<>(
					e.forall, e.where, e.egd);
			if (!s.third.isEmpty()) {
				ret1.add(s);
			}
			if (!t.third.isEmpty()) {
				ret2.add(t);
			}
		}
		return new Pair<>(ret1, ret2);
	}

	private static int ruleNo = -1;

	public enum KIND {
		PARALLEL, STANDARD, CORE, HYBRID
	}

	private static Map<String, Set<Pair<Object, Object>>> chase(
			Set<String> keys,
			Pair<List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>>, List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>>> zzz,
			Map<String, Set<Pair<Object, Object>>> inst0, KIND kind) {

		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>(inst0);

		List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>> tgds = zzz.first;
		List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>> egds = zzz.second;

		int i = 0;
		ruleNo = 0;
		while (true) {
			Map<String, Set<Pair<Object, Object>>> ret_old = new HashMap<>(ret);
			switch (kind) {
				case CORE:
					boolean fired = false;
					for (Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>> tgd : tgds) {
						ret = union(ret, chaseTgd(ret_old, tgd.first, tgd.second, tgd.third));
						fired = !ret.equals(ret_old);
					}
					for (Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>> egd : egds) {
						ret = apply(ret, chaseEgd(ret_old, egd.first, egd.second, egd.third));
						fired = !ret.equals(ret_old);
					}
					ret = minimize(keys, ret, tgds, egds);
					if (!fired) {
						return ret;
					}
					break;
				case PARALLEL:
					for (Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>> tgd : tgds) {
						ret = union(ret, chaseTgd(ret_old, tgd.first, tgd.second, tgd.third));
					}
					for (Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>> egd : egds) {
						ret = apply(ret, chaseEgd(ret_old, egd.first, egd.second, egd.third));
					}
					if (ret.equals(ret_old)) {
						return ret;
					}
					break;
				case STANDARD:
					fired = false;
					for (int x = 0; x < tgds.size() + egds.size(); x++) {
						int pos = (ruleNo + x) % (tgds.size() + egds.size());
						if (pos < egds.size()) {
							Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>> egd = egds
									.get(pos);
							ret = apply(ret, chaseEgd(ret_old, egd.first, egd.second, egd.third));
						} else if (pos < tgds.size() + egds.size()) {
							Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>> tgd = tgds
									.get(pos - egds.size());
							ret = union(ret, chaseTgd(ret_old, tgd.first, tgd.second, tgd.third));
						}
						if (!ret.equals(ret_old)) {
							ruleNo++;
							if (ruleNo == tgds.size() + egds.size()) {
								ruleNo = 0;
							}
							fired = true;
							break;
						}
					}
					if (!fired) {
						return ret;
					}
					break;
				case HYBRID:
					fired = false;

					for (Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>> egd : egds) {
						ret = apply(ret, chaseEgd(ret_old, egd.first, egd.second, egd.third));
						if (!ret.equals(ret_old)) {
							fired = true;
							break;
						}
					}
					if (fired) {
						break;
					}

					for (int x = 0; x < tgds.size(); x++) {
						int pos = (ruleNo + x) % (tgds.size());
						Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>> tgd = tgds
								.get(pos);
						ret = union(ret, chaseTgd(ret_old, tgd.first, tgd.second, tgd.third));

						if (!ret.equals(ret_old)) {
							ruleNo++;
							if (ruleNo == tgds.size()) {
								ruleNo = 0;
							}
							fired = true;
							break;
						}
					}

					if (!fired) {
						return ret;
					}
				default:
					break;
			}

			if (i++ > Chase.chase_limit) {
				throw new RuntimeException("Chase exceeds " + Chase.chase_limit + " iterations");
			}
		}

	}

	private static final int chase_limit = 64*64;

	private static Map<String, Set<Pair<Object, Object>>> minimize(
			Set<String> keys,
			Map<String, Set<Pair<Object, Object>>> I,
			List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>> tgds,
			List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>> egds) {

		Map<String, Set<Pair<Object, Object>>> sol = I;
		for (Map<String, Set<Pair<Object, Object>>> I0 : smaller(keys, I, tgds, egds)) {
			if (hasHomo(keys, I0, I) && hasHomo(keys, I, I0)) {
				if (size(I0) < size(sol)) {
					sol = I0;
				}
			}
		}

		return sol;
	}

	private static List<Map<String, Set<Pair<Object, Object>>>> smaller(
			Set<String> keys,
			Map<String, Set<Pair<Object, Object>>> i,
			List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>> tgds,
			List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>> egds) {
		Set<Object> ids = ids(keys, i);
		Collection<Set<Object>> subsets = pow(ids);
		List<Map<String, Set<Pair<Object, Object>>>> ret = new LinkedList<>();

		for (Set<Object> subset : subsets) {
			Map<String, Set<Pair<Object, Object>>> i0 = remove(i, subset);
			if (obeys(i0, tgds, egds)) {
				ret.add(i0);
			}
		}

		return ret;
	}

	private static boolean obeys(
			Map<String, Set<Pair<Object, Object>>> i0,
			List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>> tgds,
			List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>> egds) {

		for (Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>> egd : egds) {
			EmbeddedDependency xxx0 = conv(egd.first, egd.second, egd.third);
			ED xxx = ED.from(xxx0);

			Flower front = xxx.front();
			Flower back = xxx.back();
			Set<Map<Object, Object>> frontX = front.eval(ED.conv(i0));
			Set<Map<Object, Object>> backX = back.eval(ED.conv(i0));
			if (!frontX.equals(backX)) {
				return false;
			}
		}
		for (Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>> tgd : tgds) {
			EmbeddedDependency xxx0 = conv2(tgd.first, tgd.second, tgd.third);
			ED xxx = ED.from(xxx0);

			Flower front = xxx.front();
			Flower back = xxx.back();
			Set<Map<Object, Object>> frontX = front.eval(ED.conv(i0));
			Set<Map<Object, Object>> backX = back.eval(ED.conv(i0));
			if (!frontX.equals(backX)) {
				return false;
			}
		}

		return true;
	}

	private static Map<String, Set<Pair<Object, Object>>> remove(
			Map<String, Set<Pair<Object, Object>>> i, Set<Object> subset) {

		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>();

		for (String k : i.keySet()) {
			ret.put(k, remove(i.get(k), subset));
		}

		return ret;
	}

	private static Set<Pair<Object, Object>> remove(Set<Pair<Object, Object>> set,
			Set<Object> subset) {
		Set<Pair<Object, Object>> ret = new HashSet<>();

		for (Pair<Object, Object> p : set) {
			if (subset.contains(p.first) || subset.contains(p.second)) {
				continue;
			}
			ret.add(p);
		}

		return ret;
	}

	private static boolean hasHomo(Set<String> keys, Map<String, Set<Pair<Object, Object>>> i,
			Map<String, Set<Pair<Object, Object>>> i0) {

		List<List<Pair<Object, Object>>> homs = homomorphs(ids(keys, i), ids(keys, i0));
		for (List<Pair<Object, Object>> hom : homs) {
			Map<String, Set<Pair<Object, Object>>> applied = apply(i, hom);
			if (subset(applied, i0)) {
				return true;
			}
		}
		return false;
	}

	private static boolean subset(Map<String, Set<Pair<Object, Object>>> l,
			Map<String, Set<Pair<Object, Object>>> r) {
		for (String k : l.keySet()) {
			for (Pair<Object, Object> p : l.get(k)) {
				if (!r.get(k).contains(p)) {
					return false;
				}
			}
		}
		return true;
	}

	private static Set<Object> ids(@SuppressWarnings("unused") Set<String> keys, Map<String, Set<Pair<Object, Object>>> i) {
		Set<Object> ret = new HashSet<>();
		// ignore keys
		for (String k : i.keySet()) {
			for (Pair<Object, Object> p : i.get(k)) {
				ret.add(p.first);
				ret.add(p.second);
			}
		}

		return ret;
	}

	private static <X> List<List<Pair<X, X>>> homomorphs(Collection<X> A, Collection<X> B) {
		List<List<Pair<X, X>>> ret = new LinkedList<>();

		if (A.isEmpty()) {
			return ret;
		}

		if (B.isEmpty()) {
			throw new RuntimeException();
		}

		int[] counters = new int[A.size() + 1];

		// int i = 0;
		while (true) {

			if (counters[A.size()] == 1) {
				break;
			}
			ret.add(make2(counters, new LinkedList<>(A), new LinkedList<>(B)));
			inc(counters, B.size());
		}

		return ret;
	}

	private static <X> List<Pair<X, X>> make2(int[] counters, List<X> A, List<X> B) {
		List<Pair<X, X>> ret = new LinkedList<>();
		int i = 0;
		for (X x : A) {
			ret.add(new Pair<>(x, B.get(counters[i++])));
		}
		return ret;
	}

	private static void inc(int[] counters, int size) {
		counters[0]++;
		for (int i = 0; i < counters.length - 1; i++) {
			if (counters[i] == size) {
				counters[i] = 0;
				counters[i + 1]++;
			}
		}
	}

	private static int size(Map<String, Set<Pair<Object, Object>>> sol) {
		int i = 0;

		for (String k : sol.keySet()) {
			i += sol.get(k).size();
		}

		return i;
	}

	private static Map<String, Set<Pair<Object, Object>>> union(
			Map<String, Set<Pair<Object, Object>>> a, Map<String, Set<Pair<Object, Object>>> b) {
		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>();

		for (String k : a.keySet()) {
			Set<Pair<Object, Object>> x = new HashSet<>();
			x.addAll(a.get(k));
			x.addAll(b.get(k));
			ret.put(k, x);
		}

		return ret;
	}

	private static Map<String, Set<Pair<Object, Object>>> apply(
			Map<String, Set<Pair<Object, Object>>> I, List<Pair<Object, Object>> subst) {
		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>(I);

		List<Pair<Object, Object>> subst0 = new LinkedList<>(subst);

		while (true) {
			if (subst0.isEmpty()) {
				break;
			}
			Pair<Object, Object> phi = subst0.remove(0);
			ret = apply0(ret, phi);
			subst0 = apply2(subst0, phi);
		}

		return ret;
	}

	private static List<Pair<Object, Object>> apply2(List<Pair<Object, Object>> l,
			Pair<Object, Object> phi) {

		List<Pair<Object, Object>> ret = new LinkedList<>();

		for (Pair<Object, Object> p : l) {
			ret.add(new Pair<>(p.first.equals(phi.first) ? phi.second : p.first, p.second
					.equals(phi.first) ? phi.second : p.second));
		}

		return ret;

	}

	private static Map<String, Set<Pair<Object, Object>>> apply0(
			Map<String, Set<Pair<Object, Object>>> I, Pair<Object, Object> s) {

		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>();

		for (String k : I.keySet()) {
			ret.put(k, apply1(I.get(k), s));
		}

		return ret;
	}

	private static Set<Pair<Object, Object>> apply1(Set<Pair<Object, Object>> set,
			Pair<Object, Object> s) {

		Set<Pair<Object, Object>> ret = new HashSet<>();

		for (Pair<Object, Object> p : set) {
			ret.add(new Pair<>(p.first.equals(s.first) ? s.second : p.first, p.second
					.equals(s.first) ? s.second : p.second));
		}

		return ret;
	}

	private static int fresh = 0;

	private static Map<String, Set<Pair<Object, Object>>> chaseTgd(
			Map<String, Set<Pair<Object, Object>>> i, List<String> forall,
			List<Triple<String, String, String>> where, List<Triple<String, String, String>> t) {

		Map<String, Set<Pair<Object, Object>>> ret = new HashMap<>();
		for (String k : i.keySet()) {
			ret.put(k, new HashSet<>());
		}

		EmbeddedDependency xxx0 = conv2(forall, where, t);
		ED xxx = ED.from(xxx0);

		Flower front = xxx.front();

		Flower back = xxx.back();

		Set<Map<Object, Object>> frontX = front.eval(ED.conv(i));

		Set<Map<Object, Object>> backX = back.eval(ED.conv(i));

		if (frontX.equals(backX)) {
			return ret;
		}

		for (Map<Object, Object> eq : frontX) {

			Map<String, String> map = new HashMap<>();
			for (String v : xxx0.exists) {
				String v0 = "_" + (fresh++);
				map.put(v, v0);
			}

			for (Triple<String, String, String> fact : xxx0.tgd) {
				Object a;
				try {
					a = eq.get("c" + getColNo(xxx0.forall, xxx0.where, fact.second));
				} catch (Exception ee) {
					a = map.get(fact.second);
				}
				Object b;
				try {
					b = eq.get("c" + getColNo(xxx0.forall, xxx0.where, fact.third));
				} catch (Exception ee) {
					b = map.get(fact.third);
				}
				ret.get(fact.first).add(new Pair<>(a, b));
			}

		}

		return ret;
	}

	private static List<Pair<Object, Object>> chaseEgd(Map<String, Set<Pair<Object, Object>>> i,
			List<String> forall, List<Triple<String, String, String>> where,
			List<Pair<String, String>> t) {

		List<Pair<Object, Object>> ret = new LinkedList<>();

		EmbeddedDependency xxx0 = conv(forall, where, t);
		ED xxx = ED.from(xxx0);

		Flower front = xxx.front();

		Flower back = xxx.back();

		Set<Map<Object, Object>> frontX = front.eval(ED.conv(i));

		Set<Map<Object, Object>> backX = back.eval(ED.conv(i));

		if (frontX.equals(backX)) {
			return ret;
		}

		for (Pair<String, String> eq : t) {
			int a = getColNo(xxx0.forall, xxx0.where, eq.first);
			int b = getColNo(xxx0.forall, xxx0.where, eq.second);
			for (Map<Object, Object> row : frontX) {
				if (row.get("c" + a).toString().startsWith("_")) {
					ret.add(new Pair<>(row.get("c" + a), row.get("c" + b)));
				} else {
					ret.add(new Pair<>(row.get("c" + b), row.get("c" + a)));
				}
			}
		}

		return ret;
	}

	private static int getColNo(@SuppressWarnings("unused") List<String> f, List<Triple<String, String, String>> s, String str) {
		int i = 0;
		for (Triple<String, String, String> k : s) {
			if (str.equals(k.second)) {
				return i;
			}
			if (str.equals(k.third)) {
				return i + 1;
			}
			i += 2;
		}
		throw new RuntimeException("Canont find " + str + " in " + s);
	}

	private static EmbeddedDependency conv(List<String> f, List<Triple<String, String, String>> s,
			List<Pair<String, String>> t) {
		Set<String> all = new HashSet<>(f);
		for (Pair<String, String> p : t) {
			all.add(p.first);
			all.add(p.second);
		}

		List<String> exists = new LinkedList<>(all);
		exists.removeAll(f);

		List<Triple<String, String, String>> tgd = new LinkedList<>();

		return new EmbeddedDependency(f, exists, s, tgd, t);

	}

	private static EmbeddedDependency conv2(List<String> f, List<Triple<String, String, String>> s,
			List<Triple<String, String, String>> t) {

		Set<String> all = new HashSet<>(f);
		for (Triple<String, String, String> p : t) {
			all.add(p.second);
			all.add(p.third);
		}
		for (Triple<String, String, String> p : t) {
			all.add(p.second);
			all.add(p.third);
		}

		List<String> exists = new LinkedList<>(all);
		exists.removeAll(f);

		List<Pair<String, String>> egd = new LinkedList<>();

		return new EmbeddedDependency(f, exists, s, t, egd);

	}

	public static Instance sigma(Mapping m, Instance i) throws FQLException {

		Triple<Pair<Signature, List<Pair<Attribute<Node>, Pair<Edge, Attribute<Node>>>>>, Pair<Signature, List<Pair<Attribute<Node>, Pair<Edge, Attribute<Node>>>>>, Pair<Signature, List<Pair<Attribute<Node>, Pair<Edge, Attribute<Node>>>>>> kkk = m
				.toEDs();

		Signature cd = kkk.second.first;

		Map<String, Set<Pair<Object, Object>>> I = new HashMap<>();
		for (Node n : cd.nodes) {
			I.put(n.string, new HashSet<>());
		}
		for (Edge n : cd.edges) {
			I.put(n.name, new HashSet<>());
		}
		for (Attribute<Node> n : cd.attrs) {
			I.put(n.name, new HashSet<>());
		}
		for (String k : i.data.keySet()) {
			I.put("src_" + k, i.data.get(k));
		}

		List<EmbeddedDependency> eds0 = Signature.toED("", kkk.second);
	
		Pair<List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>>, List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>>> zzz = split(eds0);

		Set<String> keys = new HashSet<>();
		for (Node n : m.target.nodes) {
			keys.add("dst_" + n.string);
		}
		for (Edge n : m.target.edges) {
			keys.add("dst_" + n.name);
		}
		for (Attribute<Node> n : m.target.attrs) {
			keys.add("dst_" + n.name);
		}
		Map<String, Set<Pair<Object, Object>>> res = chase(keys, zzz, I, KIND.PARALLEL);
		Map<String, Set<Pair<Object, Object>>> res0 = new HashMap<>();
		for (Node n : m.target.nodes) {
			res0.put(n.string, res.get("dst_" + n.string));
		}
		for (Edge n : m.target.edges) {
			res0.put(n.name, res.get("dst_" + n.name));
		}
		for (Attribute<Node> n : m.target.attrs) {
			res0.put(n.name, res.get("dst_" + n.name));
		}

		Instance ret = new Instance(m.target, res0);

		return ret;

	}

	public static Instance delta(Mapping m, Instance i) throws FQLException {

		Triple<Pair<Signature, List<Pair<Attribute<Node>, Pair<Edge, Attribute<Node>>>>>, Pair<Signature, List<Pair<Attribute<Node>, Pair<Edge, Attribute<Node>>>>>, Pair<Signature, List<Pair<Attribute<Node>, Pair<Edge, Attribute<Node>>>>>> kkk = m
				.toEDs();

		Signature cd = m.toEDs().first.first;

		Map<String, Set<Pair<Object, Object>>> I = new HashMap<>();
		for (Node n : cd.nodes) {
			I.put(n.string, new HashSet<>());
		}
		for (Edge n : cd.edges) {
			I.put(n.name, new HashSet<>());
		}
		for (Attribute<Node> n : cd.attrs) {
			I.put(n.name, new HashSet<>());
		}
		for (String k : i.data.keySet()) {
			I.put("dst_" + k, i.data.get(k));
		}

		List<EmbeddedDependency> eds0 = Signature.toED("", kkk.first);

	

		Pair<List<Triple<List<String>, List<Triple<String, String, String>>, List<Triple<String, String, String>>>>, List<Triple<List<String>, List<Triple<String, String, String>>, List<Pair<String, String>>>>> zzz = split(eds0);

		Set<String> keys = new HashSet<>();
		for (Node n : m.target.nodes) {
			keys.add("src_" + n.string);
		}
		for (Edge n : m.target.edges) {
			keys.add("src_" + n.name);
		}
		for (Attribute<Node> n : m.target.attrs) {
			keys.add("src_" + n.name);
		}
	
		Map<String, Set<Pair<Object, Object>>> res = chase(keys, zzz, I, KIND.HYBRID); // changed
		Map<String, Set<Pair<Object, Object>>> res0 = new HashMap<>();
		for (Node n : m.source.nodes) {
			res0.put(n.string, res.get("src_" + n.string));
		}
		for (Edge n : m.source.edges) {
			res0.put(n.name, res.get("src_" + n.name));
		}
		for (Attribute<Node> n : m.source.attrs) {
			res0.put(n.name, res.get("src_" + n.name));
		}

		Instance ret = new Instance(m.source, res0);

		return ret;

	}

	private static <T> Set<Set<T>> pow(Set<T> originalSet) {
		Set<Set<T>> sets = new HashSet<>();
		if (originalSet.isEmpty()) {
			sets.add(new HashSet<>());
			return sets;
		}
		List<T> list = new ArrayList<>(originalSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<>(list.subList(1, list.size()));
		for (Set<T> set : pow(rest)) {
			Set<T> newSet = new HashSet<>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}
}

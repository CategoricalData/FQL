package catdata.opl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Environment;
import catdata.Pair;
import catdata.Program;
import catdata.Triple;
import catdata.Util;
import catdata.ide.DefunctGlobalOptions;
import catdata.opl.OplExp.OplApply;
import catdata.opl.OplExp.OplChaseExp;
import catdata.opl.OplExp.OplColim;
import catdata.opl.OplExp.OplDelta;
import catdata.opl.OplExp.OplDelta0;
import catdata.opl.OplExp.OplDistinct;
import catdata.opl.OplExp.OplEval;
import catdata.opl.OplExp.OplExpVisitor;
import catdata.opl.OplExp.OplFlower;
import catdata.opl.OplExp.OplGraph;
import catdata.opl.OplExp.OplGround;
import catdata.opl.OplExp.OplId;
import catdata.opl.OplExp.OplInst;
import catdata.opl.OplExp.OplInst0;
import catdata.opl.OplExp.OplJavaInst;
import catdata.opl.OplExp.OplMapping;
import catdata.opl.OplExp.OplPivot;
import catdata.opl.OplExp.OplPragma;
import catdata.opl.OplExp.OplPres;
import catdata.opl.OplExp.OplPresTrans;
import catdata.opl.OplExp.OplPushout;
import catdata.opl.OplExp.OplPushoutBen;
import catdata.opl.OplExp.OplPushoutSch;
import catdata.opl.OplExp.OplSCHEMA0;
import catdata.opl.OplExp.OplSat;
import catdata.opl.OplExp.OplSchema;
import catdata.opl.OplExp.OplSchemaProj;
import catdata.opl.OplExp.OplSetInst;
import catdata.opl.OplExp.OplSetTrans;
import catdata.opl.OplExp.OplSig;
import catdata.opl.OplExp.OplSigma;
import catdata.opl.OplExp.OplTyMapping;
import catdata.opl.OplExp.OplUberSat;
import catdata.opl.OplExp.OplUnSat;
import catdata.opl.OplExp.OplUnion;
import catdata.opl.OplExp.OplVar;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class OplOps implements OplExpVisitor<OplObject, Program<OplExp>> {

	private final Environment<OplObject> ENV;

	public OplOps(Environment<OplObject> env) {
        ENV = env;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplPivot e) {
		OplObject o = ENV.get(e.I0);
		if (o instanceof OplInst) {
			OplInst I = (OplInst) o;
			e.validate(I);
			return e;
		}
		throw new RuntimeException("Not an instnce: " + e.I0);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSig e) {
		for (Object k2 : e.imports) {
			String k = (String) k2;
			OplObject o = ENV.get(k);
			if (!(o instanceof OplSig)) {
				throw new RuntimeException("Not a theory: " + k);
			}
			OplSig a = (OplSig) o;
			e.sorts.addAll(a.sorts);
			Util.putAllSafely(e.symbols, a.symbols);
			e.equations.addAll(a.equations);
			e.implications.addAll(a.implications);
			e.prec.putAll(a.prec);
			// Util.putAllSafely(e.prec, a.prec);
		}
		e.validate();
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> Env, OplDelta0 e) {
		OplObject o = ENV.get(e.F0);
		if (o instanceof OplTyMapping) {
			OplTyMapping F = (OplTyMapping) o;
			e.validate(F);
			return e.toQuery();
		}
		throw new RuntimeException("Not a typed mapping: " + e.F0);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSCHEMA0 e) {
		OplObject t0 = ENV.get(e.typeSide);
		if (!(t0 instanceof OplSig)) {
			throw new RuntimeException("Not a theory: " + e.typeSide);
		}
		OplSig t = (OplSig) t0;
		if (!t.implications.isEmpty()) {
			throw new RuntimeException("Can't use implications with SCHEMA");
		}

		OplSchema ret = new OplSchema("?", e.entities);
		ret.forSchema0 = e.typeSide;
		Map prec = new HashMap();
		prec.putAll(t.prec);
		prec.putAll(e.prec);

		Set sorts = new HashSet();
		sorts.addAll(t.sorts);
		sorts.addAll(e.entities);
		if (!Collections.disjoint(t.sorts, e.entities)) {
			throw new RuntimeException("Schema has an entity that is also a type side sort");
		}


		Map symbols = new HashMap();
		symbols.putAll(t.symbols);
		symbols.putAll(e.attrs);
		symbols.putAll(e.edges);
		if (!Collections.disjoint(t.symbols.keySet(), e.attrs.keySet())) {
			throw new RuntimeException("Schema has an attribute that is also a type side symbol");
		}
		if (!Collections.disjoint(t.symbols.keySet(), e.edges.keySet())) {
			throw new RuntimeException("Schema has an attribute that is also a type side symbol");
		}
		

		List equations = new LinkedList();
		equations.addAll(t.equations);
		equations.addAll(e.pathEqs);
		equations.addAll(e.obsEqs);

		for (Object k2 : e.imports) {
			String k = (String) k2;
			OplObject o = ENV.get(k);
			if (!(o instanceof OplSchema)) {
				throw new RuntimeException("Not a SCHEMA: " + k);
			}
			OplSchema a = (OplSchema) o;
			sorts.addAll(a.sig.sorts);
			Util.putAllSafely(symbols, a.sig.symbols);
			equations.addAll(a.sig.equations);
			e.entities.addAll(a.entities);
			prec.putAll(a.sig.prec);
			// Util.putAllSafely(prec, a.sig.prec);
		}

		OplSig sig = new OplSig(t.fr, prec, sorts, symbols, equations);
		sig.validate();
		ret.validate(sig);
		e.validate(sig);
		return ret;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSetInst e) {
		OplObject o = ENV.get(e.sig);
		if (!(o instanceof OplSig)) {
			throw new RuntimeException("Not a theory: " + o + " . is " + o.getClass());
		}
		OplSig s = (OplSig) o;
		e.validate(s);
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplEval e) {
		OplObject i = ENV.get(e.I);
		if (i instanceof OplSetInst) {
			OplSetInst i0 = (OplSetInst) i;
			OplObject s = ENV.get(i0.sig);
			OplSig s0 = (OplSig) s;
			e.e.type(s0, new OplCtx<String, String>());
			return new OplString(e.e.eval(s0, i0, new OplCtx<String, String>()).toString());
		}

		if (i instanceof OplJavaInst) {
			OplJavaInst i0 = (OplJavaInst) i;
			OplObject s = ENV.get(i0.sig);
			OplSig s0 = (OplSig) s;
			e.e.type(s0, new OplCtx<String, String>());
			try {
				return new OplString(OplTerm.strip(OplToKB.redBy(i0, OplToKB.convert(e.e.inLeft())).toString()));
				// return new
				// OplString(e.e.eval((Invocable)i0.engine).toString());
			} catch (Exception ee) {
				ee.printStackTrace();
				throw new RuntimeException(ee.getMessage());
			}
		}
		throw new RuntimeException("Not a set/js model: " + e.I);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplVar e) {
		return ENV.get(e.name);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSetTrans e) {
		OplObject src = ENV.get(e.src);
		OplObject dst = ENV.get(e.dst);
		if (!(src instanceof OplSetInst)) {
			throw new RuntimeException("Source is not a model in " + e);
		}
		if (!(dst instanceof OplSetInst)) {
			throw new RuntimeException("Target is not a model in " + e);
		}
		OplSetInst src0 = (OplSetInst) src;
		OplSetInst dst0 = (OplSetInst) dst;
		if (!src0.sig.equals(dst0.sig)) {
			throw new RuntimeException("Theories of source and target do not match in " + e);
		}
		OplSig sig = (OplSig) ENV.get(src0.sig);
		e.validate(sig, src0, dst0);
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplJavaInst e) {
		OplObject sig = ENV.get(e.sig);
		if (!(sig instanceof OplSig)) {
			throw new RuntimeException("Not a signature: " + e.sig);
		}
		OplSig sig0 = (OplSig) sig;
		e.validate(sig0, ENV);
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplMapping e) {
		OplObject src = ENV.get(e.src0);
		OplObject dst = ENV.get(e.dst0);
		if (src instanceof OplSig && dst instanceof OplSig) {
			OplSig src0 = (OplSig) src;
			OplSig dst0 = (OplSig) dst;
			for (Object k2 : e.imports) {
				String k = (String) k2;
				OplExp o = env.exps.get(k);
				if (!(o instanceof OplMapping)) {
					throw new RuntimeException("Not a mapping: " + k);
				}
				OplMapping a = (OplMapping) o;
				Util.putAllSafely(e.sorts, a.sorts);
				Util.putAllSafely(e.symbols, a.symbols);
			}
			e.validate(src0, dst0);
			return e;
		} else if (src instanceof OplSchema && dst instanceof OplSchema) {
			OplSchema src0 = (OplSchema) src;
			OplSchema dst0 = (OplSchema) dst;
			for (Object k2 : e.imports) {
				String k = (String) k2;
				OplExp o = (OplExp) ENV.get(k);
				if (!(o instanceof OplTyMapping)) {
					throw new RuntimeException("Not a typed mapping: " + k + o.getClass());
				}
				OplTyMapping a = (OplTyMapping) o;
				Util.putAllSafely(e.sorts, a.m.sorts);
				Util.putAllSafely(e.symbols, a.m.symbols);
			}
			// e.validate(src0.projEA(), dst0.projEA());
			OplTyMapping ret = new OplTyMapping<>(e.src0, e.dst0, src0, dst0, e);
			return ret;
		}
		throw new RuntimeException("Source or Target is not a theory/schema in " + e);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplDelta e) {
		OplObject F = ENV.get(e.F);
		if (!(F instanceof OplMapping)) {
			throw new RuntimeException("Not a mapping: " + e.F);
		}
		OplMapping F0 = (OplMapping) F;

		OplObject I = ENV.get(e.I);
		if (I instanceof OplSetInst) {
			OplSetInst I0 = (OplSetInst) I;
			return F0.delta(I0);
		}
		if (I instanceof OplSetTrans) {
			OplSetTrans h = (OplSetTrans) I;
			return F0.delta(h);
		}

		throw new RuntimeException("Not a model or transform: " + e.I);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplPres e) {
		OplObject i = ENV.get(e.S);
		if (i instanceof OplSig) {
			OplSig S = (OplSig) i;
			OplPres ret = OplPres.OplPres0(e.prec, e.S, S, e.gens, e.equations);
			ret.toSig();
			return ret;
		} else if (i instanceof OplSchema) {
			OplSchema S = (OplSchema) i;
			OplPres ret = OplPres.OplPres0(e.prec, e.S, S.sig, e.gens, e.equations);
			ret.toSig();
			return ret;
		} else {
			throw new RuntimeException("Not a presentation or schema: " + e.S);
		}
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplPushout e) {
		OplObject s1 = ENV.get(e.s1);
		OplObject s2 = ENV.get(e.s2);
		if (!(s1 instanceof OplPresTrans)) {
			throw new RuntimeException(e.s1 + " is not a transform");
		}
		if (!(s2 instanceof OplPresTrans)) {
			throw new RuntimeException(e.s2 + " is not a transform");
		}
		e.validate((OplPresTrans) s1, (OplPresTrans) s2);
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplPushoutSch e) {
		OplObject s1 = ENV.get(e.s1);
		OplObject s2 = ENV.get(e.s2);
		if (!(s1 instanceof OplTyMapping)) {
			throw new RuntimeException(e.s1 + " is not a ty mapping");
		}
		if (!(s2 instanceof OplTyMapping)) {
			throw new RuntimeException(e.s2 + " is not a ty mapping");
		}
		e.validate((OplTyMapping) s1, (OplTyMapping) s2);
		return e.pushout();
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplPushoutBen e) {
		OplObject s1 = ENV.get(e.s1);
		OplObject s2 = ENV.get(e.s2);
		if (!(s1 instanceof OplMapping)) {
			throw new RuntimeException(e.s1 + " is not a mapping");
		}
		if (!(s2 instanceof OplMapping)) {
			throw new RuntimeException(e.s2 + " is not a mapping");
		}
		e.validate((OplMapping) s1, (OplMapping) s2);
		return e.pushout();
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSat e) {
		OplObject i = ENV.get(e.I);
		if (i instanceof OplPres) {
			OplPres S = (OplPres) i;
			OplObject ob = OplSat.saturate(S);
			return ob;
		}
		if (i instanceof OplSig) {
			OplSig S = (OplSig) i;
			OplObject ob = S.saturate(e.I);
			return ob;
		}
		throw new RuntimeException("Not a presentation or theory");
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplUberSat e) {
		OplObject p = ENV.get(e.P);
		if (!(p instanceof OplPres)) {
			throw new RuntimeException("Not a presentation: " + e.P);
		}
		OplPres S = (OplPres) p;

		OplObject i = ENV.get(e.I);
		if (!(i instanceof OplJavaInst)) {
			throw new RuntimeException("Not a javascript model: " + e.I);
		}
		OplJavaInst I = (OplJavaInst) i;

		OplObject ob = OplUberSat.saturate(I, S);
		return ob;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplUnSat e) {
		OplObject i = ENV.get(e.I);
		if (!(i instanceof OplSetInst)) {
			throw new RuntimeException("Not a model: " + e.I);
		}
		OplSetInst S = (OplSetInst) i;
		return OplUnSat.desaturate(S.sig, S);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSigma e) {

		OplObject F = ENV.get(e.F);
		OplObject I = ENV.get(e.I);

		if (F instanceof OplMapping) {
			OplMapping F0 = (OplMapping) F;
			if (I instanceof OplPres) {
				OplPres I0 = (OplPres) I;
				return F0.sigma(I0);
			} else if (I instanceof OplPresTrans) {
				OplPresTrans h = (OplPresTrans) I;
				return F0.sigma(h);
			}
			throw new RuntimeException("Not a presentation of an instance or transform: " + e.I);
		}
		if (F instanceof OplTyMapping) {
			OplTyMapping F0 = (OplTyMapping) F;
			if (I instanceof OplInst) {
				OplInst I0 = (OplInst) I;
				OplInst ret = new OplInst<>(F0.dst0, "?", I0.J0);
				ret.validate(F0.dst, F0.extend().sigma(I0.P), I0.J);
				return ret;
			} else if (I instanceof OplPresTrans) {
				OplPresTrans h = (OplPresTrans) I;
				OplPresTrans z = F0.extend().sigma(h);
				z.src1 = new OplInst<>("?", "?", h.src1.J0);
				z.dst1 = new OplInst<>("?", "?", h.src1.J0);
				z.src1.validate(F0.dst, z.src, h.src1.J);
				z.dst1.validate(F0.dst, z.dst, h.src1.J);
				return z;
			}
			throw new RuntimeException("Not an instance: " + e.I + "\n\n " + I.getClass());

		}
		throw new RuntimeException("Not a mapping: " + e.F);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplPresTrans e) {
		OplObject src = ENV.get(e.src0);
		OplObject dst = ENV.get(e.dst0);
		if (src instanceof OplPres && dst instanceof OplPres) {
			OplPres src0 = (OplPres) src;
			OplPres dst0 = (OplPres) dst;

			for (Object k2 : e.imports) {
				String k = (String) k2;
				OplExp o = env.exps.get(k);
				if (!(o instanceof OplPresTrans)) {
					throw new RuntimeException("Not a typed mapping: " + k);
				}
				OplPresTrans a = (OplPresTrans) o;
				for (Object z : a.pre_map.keySet()) {
					if (!e.pre_map.containsKey(z)) {
						e.pre_map.put(z, new HashMap());
					}
					Map u = (Map) a.pre_map.get(z);
					Map v = (Map) e.pre_map.get(z);
					Util.putAllSafely(v, u);
				}
			}

			e.validateNotReally(src0, dst0); // ?

			// e.toMapping(); redundant
			return e;
		} else if (src instanceof OplInst && dst instanceof OplInst) {
			OplInst src0 = (OplInst) src;
			OplInst dst0 = (OplInst) dst;

			for (Object k2 : e.imports) {
				String k = (String) k2;
				OplExp o = env.exps.get(k);
				if (!(o instanceof OplPresTrans)) {
					throw new RuntimeException("Not a typed mapping: " + k);
				}
				OplPresTrans a = (OplPresTrans) o;
				for (Object z : a.pre_map.keySet()) {
					if (!e.pre_map.containsKey(z)) {
						e.pre_map.put(z, new HashMap());
					}
					Map u = (Map) a.pre_map.get(z);
					Map v = (Map) e.pre_map.get(z);
					Util.putAllSafely(v, u);
				}
			}
			e.validateNotReally(src0, dst0); // ?
			// e.toMapping(); redundant
			return e;
		}
		throw new RuntimeException("Source or target is not a presentation or instance in " + e);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplFlower e) {
		OplObject I0 = ENV.get(e.I0);
		if (I0 instanceof OplSetInst) {
			OplSetInst I = (OplSetInst) I0;
			return (OplObject) e.eval(I).second;
		}
		if (I0 instanceof OplSetTrans) {
			OplSetTrans h = (OplSetTrans) I0;
			return e.eval(h);
		}
		throw new RuntimeException("Not a set model or transform: " + e.I0);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSchema e) {
		OplObject I0 = ENV.get(e.sig0);
		if (I0 instanceof OplSig) {
			OplSig I = (OplSig) I0;
			e.validate(I);
			return e;
		}
		throw new RuntimeException("Not a theory: " + e.sig0);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplSchemaProj e) {
		OplObject I0 = ENV.get(e.sch0);
		if (I0 instanceof OplSchema) {
			OplSchema I = (OplSchema) I0;
			return e.proj(I);
		}
		throw new RuntimeException("Not a schema: " + e.sch0);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplInst e) {
		OplSchema S;
		OplPres P;
		OplJavaInst J = null;

		OplObject S0 = ENV.get(e.S0);
		if (S0 instanceof OplSchema) {
			S = (OplSchema) S0;
		} else {
			throw new RuntimeException("Not a schema: " + e.S0);
		}

		OplObject P0 = ENV.get(e.P0);
		if (P0 instanceof OplPres) {
			P = (OplPres) P0;
		} else {
			throw new RuntimeException("Not a presentation: " + e.P0);
		}

		if (!e.J0.equals("none")) {
			OplObject J0 = ENV.get(e.J0);
			if (J0 instanceof OplJavaInst) {
				J = (OplJavaInst) J0;
			} else {
				throw new RuntimeException("Not a JS model: " + e.J0);
			}
		}

		e.validate(S, P, J);
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplQuery e) {
		OplSchema I, J;

		OplObject I0 = ENV.get(e.src_e);
		if (I0 instanceof OplSchema) {
			I = (OplSchema) I0;
		} else {
			throw new RuntimeException("Not a schema: " + e.src_e);
		}

		OplObject J0 = ENV.get(e.dst_e);
		if (J0 instanceof OplSchema) {
			J = (OplSchema) J0;
		} else {
			throw new RuntimeException("Not a schema: " + e.dst_e);
		}

		e.validate(I, J);
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplId e) {
        switch (e.kind) {
            case "query": {
                OplObject I0 = ENV.get(e.s);
                if (I0 instanceof OplSchema) {
                    return OplQuery.id(e.s, (OplSchema) I0);
                }
                throw new RuntimeException("Not a schema: " + e.s);
            }
            case "mapping":
				OplObject I0 = ENV.get(e.s);
				if (I0 instanceof OplSchema) {
                    return OplTyMapping.id(e.s, (OplSchema) I0);
                }
				throw new RuntimeException("Not a schema: " + e.s);
			default:
                throw new RuntimeException("Report to Ryan");
        }
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplApply e) {
		OplObject Q0 = ENV.get(e.Q0);
		if (!(Q0 instanceof OplQuery)) {
			throw new RuntimeException("Not a query: " + e.Q0);
		}
		OplQuery Q = (OplQuery) Q0;
		OplObject I0 = ENV.get(e.I0);
		if (I0 instanceof OplInst) {
			return (OplObject) Q.eval((OplInst) I0).first;
		}
		if (I0 instanceof OplPresTrans) {
			return Q.eval((OplPresTrans) I0);
		}
		throw new RuntimeException("Not an instance or transform: " + e.I0);

	}

	@Override
	public OplObject visit(Program<OplExp> env, OplTyMapping e) {
		throw new RuntimeException("Report to Ryan");
		// return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplInst0 e) {
		OplObject zzz = ENV.get(e.P.S);
		if (!(zzz instanceof OplSchema)) {
			throw new RuntimeException("Not a SCHEMA: " + e.P.S);
		}

		for (Object k2 : e.imports) {
			String k = (String) k2;
			OplExp o = env.exps.get(k);
			OplInst0 a;
			if (o instanceof OplInst0) {
				a = (OplInst0) o;
			} else if (o instanceof OplGround) {
				a = ((OplGround)o).validate(((OplInst)ENV.get(k)).S);
			} else {
				throw new RuntimeException("Not an instance: " + k);
			}
			Util.putAllSafely(e.P.gens, a.P.gens);
			e.P.equations.addAll(a.P.equations);
			e.P.prec.putAll(a.P.prec);
			// Util.putAllSafely(e.P.prec, a.P.prec);
		}

		OplPres P = (OplPres) visit(env, e.P);
		OplInst ret = new OplInst(e.P.S, "?", "none");
		OplObject S0 = ENV.get(e.P.S);
		if (!(S0 instanceof OplSchema)) {
			throw new RuntimeException("Not a schema: " + e.P.S);
		}
		OplSchema S = (OplSchema) S0;

		ret.validate(S, P, null);
		return ret;
	}

	//private static int temp = 0;

	@Override
	public OplObject visit(Program<OplExp> env, OplUnion e) {
		String typeSide;
		OplSchema schema = null;
		Map<String, Integer> prec = new HashMap<>();
		
		OplObject o = ENV.get(e.base);
		if (o instanceof OplSchema) {
			schema = (OplSchema) o;
			typeSide = schema.forSchema0;
			if (typeSide == null) {
				throw new RuntimeException(e.base + " is not a SCHEMA literal");
			}
		} else if (o instanceof OplSig) {
			typeSide = e.base;
		} else {
			throw new RuntimeException("Report this program to Ryan");
		}
		
		if (schema == null) {
			Set<String> entities = new HashSet<>();
			Map<String, Pair<List<String>, String>> edges = new HashMap<>();
			Map<String, Pair<List<String>, String>> attrs = new HashMap<>();
			List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> pathEqs = new LinkedList<>();
			List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> obsEqs = new LinkedList<>();
			
			for (String s : e.names) {
				OplExp exp = env.exps.get(s);
				if (exp == null) {
					throw new RuntimeException("Missing expression: " + s);
				}
				if (!(exp instanceof OplSCHEMA0)) {
					throw new RuntimeException("Not a schema: " + s);
				}
				OplSCHEMA0<String, String, String> toAdd = (OplSCHEMA0<String, String, String>) exp;
				if (!toAdd.typeSide.equals(typeSide)) {
					throw new RuntimeException("not all equal typesides in " + e);
				}

				for (Object entity : toAdd.entities) {
					String proposed = s + "_" + entity;
					if (entities.contains(proposed)) {
						throw new RuntimeException("name clash: " + entity);
					}
					entities.add(proposed);
				}
				
				for (Object edge : toAdd.edges.keySet()) {
					String proposed = s + "_" + edge;
					if (edges.containsKey(proposed)) {
						throw new RuntimeException("name clash: " + edge);
					}
					edges.put(proposed, new Pair<>(Collections.singletonList(s + "_" + toAdd.edges.get(edge).first.get(0)), s + "_" + toAdd.edges.get(edge).second));
					if (toAdd.prec.containsKey(edge)) {
						prec.put(proposed, toAdd.prec.get(edge));
					}
				}
				for (Object att : toAdd.attrs.keySet()) {
					String proposed = s + "_" + att;
					if (attrs.containsKey(proposed)) {
						throw new RuntimeException("name clash: " + att);
					}
					attrs.put(proposed, new Pair<>(Collections.singletonList(s + "_" + toAdd.attrs.get(att).first.get(0)), toAdd.attrs.get(att).second));
					if (toAdd.prec.containsKey(att)) {
						prec.put(proposed, toAdd.prec.get(att));
					}
				}
				for (Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>> tr : toAdd.pathEqs) {
					String v = tr.first.names().get(0);
					String t = s + "_" + tr.first.values().get(0);
					OplCtx<String, String> ctx = new OplCtx<>(Collections.singletonList(new Pair<>(v, t)));
					OplTerm<String, String> lhs1 = prepend(s, tr.second);
					OplTerm<String, String> rhs1 = prepend(s, tr.third);
					pathEqs.add(new Triple<>(ctx, lhs1, rhs1));
				}
				for (Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>> tr : toAdd.obsEqs) {
					String v = tr.first.names().get(0);
					String t = s + "_" + tr.first.values().get(0);
					OplCtx<String, String> ctx = new OplCtx<>(Collections.singletonList(new Pair<>(v, t)));
					OplTerm<String, String> lhs1 = prepend(s, tr.second);
					OplTerm<String, String> rhs1 = prepend(s, tr.third);
					obsEqs.add(new Triple<>(ctx, lhs1, rhs1));
				}

			} 
			
			return new OplSCHEMA0<>(prec, entities, edges, attrs, pathEqs, obsEqs, typeSide).accept(env, this);
			
		} 
			
			for (String s : e.names) {
				OplObject exp = ENV.get(s);
				if (!(exp instanceof OplInst)) {
					throw new RuntimeException("Not an instance: " + s);
				}
				OplInst<String, String, String, String> toAdd = (OplInst<String, String, String, String>) exp;
				if (!toAdd.S.equals(schema)) {
					throw new RuntimeException(s + " is not on schema " + schema);
				}
			}
			


			OplSig<String, String, String> sig = schema.sig;
			Map<String, String> gens = new HashMap<>();
			List<Pair<OplTerm<Object, String>, OplTerm<Object, String>>> equations1 = new LinkedList<>();
//			List<Pair<OplTerm<Chc<String,String>, String>, OplTerm<Chc<String,String>, String>>> equations2 = new LinkedList<>();
			for (String s : e.names) {
				OplExp exp = env.exps.get(s);
				OplInst0<String, String, String, String> toAdd = (OplInst0<String, String, String, String>) exp;
				for (Object gen : toAdd.P.gens.keySet()) {
					Object ty = toAdd.P.gens.get(gen);
					gens.put(s + "_" + gen, /* toAdd.P.S + "_" + */ ty.toString());
				}
				for (Object gen : toAdd.P.prec.keySet()) {
					if (toAdd.P.gens.keySet().contains(gen)) {
						if (toAdd.P.prec.containsKey(gen)) {
							prec.put(s + "_" + gen, toAdd.P.prec.get(gen));
						}
					} else {
						if (toAdd.P.prec.containsKey(gen)) {
							prec.put(toAdd.P.S + "_" + gen, toAdd.P.prec.get(gen));
						}
					}
				}
				for (Pair<OplTerm<Chc<String, String>, String>, OplTerm<Chc<String, String>, String>> tr : toAdd.P.equations) {
					OplTerm<Object, String> lhs1 = prepend2((OplTerm) tr.first,  s, toAdd.P.gens.keySet());
					OplTerm<Object, String> rhs1 = prepend2((OplTerm) tr.second, s, toAdd.P.gens.keySet());
					equations1.add(new Pair<>(lhs1, rhs1));
				}
			}
			
			//OplPres<String, String, String, String> pres = new OplPres(prec, "_temp" + temp, sig, gens, equations2);
			
			OplPres<String, String, String, String> pres = new OplPres(prec, e.base, sig, gens, equations1);
			if (DefunctGlobalOptions.debug.opl.opl_prover_simplify_instances) {
				pres = pres.simplify();
			}
			//temp++;
			OplInst0 ret = new OplInst0<>(pres);
			OplObject x = (OplObject) ret.accept(env, this);
			return x;
		
		
	}

	private OplTerm<Object, String> prepend2(OplTerm<Object, String> first, String i, Set<String> gens) {
		if (first.var != null) {
			return first;
		}
		List<OplTerm<Object, String>> args = new LinkedList<>();
		for (OplTerm<Object, String> arg : first.args) {
			args.add(prepend2(arg, i, gens));
		}
        return gens.contains(first.head) ? new OplTerm<>(i + "_" + first.head, args) : new OplTerm<>(first.head, args);

	}

	
	public static OplTerm<String, String> prepend(String s, OplTerm<String, String> e) {
		if (e.var != null) {
			return e;
		}
		List<OplTerm<String, String>> args = new LinkedList<>();
		for (OplTerm<String, String> arg : e.args) {
			args.add(prepend(s, arg));
		}

		return new OplTerm<>(s + "_" + e.head, args);
	}

	private OplTerm<Chc<String, String>, String> prepend3(String s, OplTerm<Chc<String, String>, String> e) {
		if (e.var != null) {
			return e;
		}
		List<OplTerm<Chc<String, String>, String>> args = new LinkedList<>();
		for (OplTerm<Chc<String, String>, String> arg : e.args) {
			args.add(prepend3(s, arg));
		}
        return e.head.l != null ? new OplTerm<>(e.head, args) : new OplTerm<>(Chc.inRight(s + "_" + e.head.r), args);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplPragma e) {
		for (String k : e.map.keySet()) {
			String v = e.map.get(k);
			DefunctGlobalOptions.debug.opl.set(k, v);
		}
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplColim e) {
		OplObject o = ENV.get(e.name);
		if (!(o instanceof OplGraph)) {
			throw new RuntimeException("Not a graph: " + e.name);
		}
		OplGraph<String, String> shape = (OplGraph<String, String>) o;
	
		OplObject base0 = ENV.get(e.base);
		String typeSide;
		if (base0 instanceof OplSig) {
			typeSide = e.base;
		
			OplUnion u0 = new OplUnion(new LinkedList<>(shape.nodes), typeSide);
			OplObject u1 = u0.accept(env, this);

			OplSchema<String, String, String> u = (OplSchema<String, String, String>) u1;

			Map<String, Set<String>> equivs = new HashMap<>();
			Map<String, String> equivs0 = new HashMap<>();

			for (String schname : shape.nodes) {
				OplSchema<String, String, String> sch = (OplSchema<String, String, String>) ENV.get(schname);
				for (String ename : sch.entities) {
					Set<String> set = new HashSet<>();
					set.add(schname + "_" + ename);
					equivs.put(schname + "_" + ename, set);
				}
			}

			// : type check colimit
			for (String mname : shape.edges.keySet()) {
				Pair<String, String> mt = shape.edges.get(mname);
				String s = mt.first;
				String t = mt.second;

				OplSchema<String, String, String> s0 = (OplSchema<String, String, String>) ENV.get(s);
				OplTyMapping<String, String, String, String, String> m0 = (OplTyMapping<String, String, String, String, String>) ENV
						.get(mname);

				if (!m0.src0.equals(s)) {
					throw new RuntimeException("Source of " + m0 + " is " + m0.src + " and not " + s + "as expected");
				}
				if (!m0.dst0.equals(t)) {
					throw new RuntimeException("Target of " + m0 + " is " + m0.dst + " and not " + t + "as expected");
				}

				for (String ob : s0.entities) {
					String ob0 = m0.m.sorts.get(ob);
					Set<String> set1 = equivs.get(s + "_" + ob);
					Set<String> set2 = equivs.get(t + "_" + ob0);
					set1.addAll(set2);
					equivs.put(s + "_" + ob, set1);
					equivs.put(t + "_" + ob0, set1);
				}
			}

			for (String k : equivs.keySet()) {
				List<String> v = new LinkedList<>(equivs.get(k));
				v.sort(String.CASE_INSENSITIVE_ORDER);
				equivs0.put(k, Util.sep(v, "__"));
			}

			Set<String> entities = new HashSet<>(equivs0.values());
			Map<String, Pair<List<String>, String>> edges = new HashMap<>();
			Map<String, Pair<List<String>, String>> attrs = new HashMap<>();
			List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> pathEqs = new LinkedList<>();
			List<Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>>> obsEqs = new LinkedList<>();

			Function<String, String> fun = x -> {
				if (equivs0.containsKey(x)) {
					return equivs0.get(x);
				}
				return x;
			};

			for (String edge : u.toSchema0().edges.keySet()) {
				Pair<List<String>, String> ty = u.toSchema0().edges.get(edge);
				edges.put(edge,
						new Pair<>(ty.first.stream().map(fun).collect(Collectors.toList()), fun.apply(ty.second)));
			}
			for (String attr : u.toSchema0().attrs.keySet()) {
				Pair<List<String>, String> ty = u.toSchema0().attrs.get(attr);
				attrs.put(attr,
						new Pair<>(ty.first.stream().map(fun).collect(Collectors.toList()), fun.apply(ty.second)));
			}
			for (Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>> eq : u
					.toSchema0().pathEqs) {
				OplCtx<String, String> ctx = new OplCtx<>(eq.first.values2().stream().map(x -> new Pair<>(x.first, fun.apply(x.second))).collect(Collectors.toList()));
				pathEqs.add(new Triple<>(ctx, fun2(equivs0, eq.second), fun2(equivs0, eq.third)));
			}
			for (Triple<OplCtx<String, String>, OplTerm<String, String>, OplTerm<String, String>> eq : u
					.toSchema0().obsEqs) {
				OplCtx<String, String> ctx = new OplCtx<>(eq.first.values2().stream().map(x -> new Pair<>(x.first, fun.apply(x.second))).collect(Collectors.toList()));
				obsEqs.add(new Triple<>(ctx, fun2(equivs0, eq.second), fun2(equivs0, eq.third)));
			}

			for (String mname : shape.edges.keySet()) {
				Pair<String, String> mt = shape.edges.get(mname);
				String s = mt.first;
				String t = mt.second;

				OplSchema<String, String, String> s0 = (OplSchema<String, String, String>) ENV.get(s);
				// OplSchema<String, String, String> t0 = (OplSchema<String,
				// String, String>) ENV.get(t);
				OplTyMapping<String, String, String, String, String> m0 = (OplTyMapping<String, String, String, String, String>) ENV
						.get(mname);

				for (String edge : s0.projE().symbols.keySet()) {
					Pair<OplCtx<String, String>, OplTerm<String, String>> edge2 = m0.m.symbols.get(edge);
					List<OplTerm<String, String>> args = edge2.first.vars0.keySet().stream().map((Function<String, OplTerm<String, String>>) OplTerm::new
					).collect(Collectors.toList());
					OplTerm<String, String> lhs = fun2(equivs0, new OplTerm<>(s + "_" + edge, args));
					OplCtx<String, String> ctx = new OplCtx<>(edge2.first.values2().stream().map(x -> 
						 new Pair<>(x.first, fun.apply(s + "_" + x.second))
					).collect(Collectors.toList()));
					OplTerm<String, String> rhs = fun2(equivs0, prepend(t, edge2.second));

					pathEqs.add(new Triple<>(ctx, lhs, rhs));
				}
				for (String edge : s0.projA().symbols.keySet()) {
					Pair<OplCtx<String, String>, OplTerm<String, String>> edge2 = m0.m.symbols.get(edge);
					List<OplTerm<String, String>> args = edge2.first.vars0.keySet().stream().map((Function<String, OplTerm<String, String>>) OplTerm::new
					).collect(Collectors.toList());
					OplTerm<String, String> lhs = fun2(equivs0, new OplTerm<>(s + "_" + edge, args));
					OplCtx<String, String> ctx = new OplCtx<>(edge2.first.values2().stream().map(x -> new Pair<>(x.first, fun.apply(s + "_" + x.second))).collect(Collectors.toList()));
					OplTerm<String, String> rhs = fun2(equivs0, prepend(t, edge2.second));

					obsEqs.add(new Triple<>(ctx, lhs, rhs));
				}
			}

			OplSCHEMA0<String, String, String> ret = new OplSCHEMA0<>(new HashMap<>(), entities, edges, attrs, pathEqs,
					obsEqs, typeSide);
			OplSchema retsch = (OplSchema) ret.accept(env, this);
			e.compiled.put("Colimit", ret);

			for (String schname : shape.nodes) {
				OplSchema<String, String, String> sch = (OplSchema<String, String, String>) ENV.get(schname);

				Map<String, String> inj_sorts = new HashMap<>();
				Map<String, Pair<OplCtx<String, String>, OplTerm<String, String>>> inj_symbols = new HashMap<>();

				for (String ename : sch.entities) {
					inj_sorts.put(ename, fun.apply(schname + "_" + ename));
				}
				for (String c1 : sch.projEA().symbols.keySet()) {
					Pair<List<String>, String> t = sch.projEA().symbols.get(c1);
					List<Pair<String, String>> l = new LinkedList<>();
					List<OplTerm<String, String>> vs = new LinkedList<>();
					for (String s1 : t.first) {
						String v = (String) retsch.sig.fr.next();
						vs.add(new OplTerm<>(v));
						l.add(new Pair<>(v, fun.apply(schname + "_" + s1)));
					}
					OplCtx<String, String> ctx = new OplCtx<>(l);
					OplTerm<String, String> value = fun2(equivs0, new OplTerm<>(schname + "_" + c1, vs));
					inj_symbols.put(c1, new Pair<>(ctx, value));
				}

				OplMapping<String, String, String, String, String> mapping = new OplMapping<>(
						inj_sorts, inj_symbols, schname, "Colimit");

				// : name of colimit
				OplTyMapping<String, String, String, String, String> tm = new OplTyMapping<>(
                        schname, "Colimit", sch, retsch, mapping);
				tm.extend().validate(sch.sig, retsch.sig);
				e.compiled.put(schname + "To" + "Colimit", mapping);
			}

			return e;
		} else if (base0 instanceof OplSchema) {
			OplSchema sch2 = (OplSchema) base0;
			//OplSCHEMA0 sch = sch2.toSchema0();

			List<Pair<OplTerm<Chc<String, String>, String>, OplTerm<Chc<String, String>, String>>> equations = new LinkedList<>();
			//List<Pair<OplTerm<Object, String>, OplTerm<Object, String>>> equations1 = new LinkedList<>();

			Map<String, String> gens = new HashMap<>();
			Map<String, Integer> prec = new HashMap<>();

			for (String s : shape.nodes) {
				OplInst<String, String, String, String> toAdd = (OplInst<String, String, String, String>) ENV.get(s);

				for (Object gen : toAdd.P.gens.keySet()) {
					Object ty = toAdd.P.gens.get(gen);
					gens.put(s + "_" + gen, ty.toString());
				}

				for (Object gen : toAdd.P.prec.keySet()) {
					if (toAdd.P.gens.keySet().contains(gen)) {
						if (toAdd.P.prec.containsKey(gen)) {
							prec.put(s + "_" + gen, toAdd.P.prec.get(gen));
						}
					} else {
						if (toAdd.P.prec.containsKey(gen)) {
							prec.put(toAdd.P.S + "_" + gen, toAdd.P.prec.get(gen));
						}
					}
				}

				for (Pair<OplTerm<Chc<String, String>, String>, OplTerm<Chc<String, String>, String>> tr : toAdd.P.equations) {
					OplTerm lhs1 = prepend3(s, tr.first);
					OplTerm rhs1 = prepend3(s, tr.second);
					equations.add(new Pair<>(lhs1, rhs1));
				}

				
			}

			for (String mname : shape.edges.keySet()) {
				Pair<String, String> mt = shape.edges.get(mname);
				String s = mt.first;
				String t = mt.second;

				OplInst<String, String, String, String> s0 = (OplInst<String, String, String, String>) ENV.get(s);
				// OplSchema<String, String, String> t0 = (OplSchema<String,
				// String, String>) ENV.get(t);
				OplPresTrans<String, String, String, String, String> m0 = (OplPresTrans<String, String, String, String, String>) ENV
						.get(mname);

				for (String edge : s0.projE().gens.keySet()) {
					Pair<OplCtx<String, String>, OplTerm<Chc<String, String>, String>> edge2 = m0.mapping.symbols
							.get(Chc.inRight(edge));
					OplTerm<Chc<String, String>, String> lhs = new OplTerm<>(
							Chc.inRight(s + "_" + edge), new LinkedList<>());
					equations.add(new Pair<>(lhs, prepend3(t, edge2.second)));
				}
			}
			OplPres<String, String, String, String> pres = new OplPres<>(prec, e.base, sch2.sig, gens, equations);
			OplInst<String, String, String, String> colimInst = new OplInst<>(e.base, "?", null);
			colimInst.validate(sch2, pres, null);
			e.compiled.put("ColimitInstance", colimInst);

			for (String s : shape.nodes) {
				OplInst<String, String, String, String> toAdd = (OplInst<String, String, String, String>) ENV.get(s);

				Map<String, Map<String, OplTerm<Chc<String, String>, String>>> inj_map = new HashMap<>();
				for (String entity : toAdd.S.entities) {
					inj_map.put(entity, new HashMap<>());
				}

				for (Object gen : toAdd.P.gens.keySet()) {
					Object ty = toAdd.P.gens.get(gen);
					gens.put(s + "_" + gen, ty.toString());
					Map<String, OplTerm<Chc<String, String>, String>> m = inj_map.get(ty);
					OplTerm<Chc<String, String>, String> term = new OplTerm<>(Chc.inRight(s + "_" + gen),
							new LinkedList<>());
					m.put((String) gen, term);
				}
				// OplPresTrans
				OplPresTrans<String, String, String, String, String> inj = new OplPresTrans<>(inj_map, s,
						"ColimitInstance", toAdd.P, pres);

				e.compiled.put(s + "ToColimitInstance", inj);
			}

			return colimInst;
		}

		throw new RuntimeException("Report your program to Ryan");

	}

	@SuppressWarnings("unlikely-arg-type")
	static OplTerm<String, String> fun2(Map<String, String> equivs, OplTerm<String, String> x) {
		if (x.var != null) {
			return x;
		}
		List<OplTerm<String, String>> ret = new LinkedList<>();
		for (OplTerm<String, String> arg : x.args) {
			ret.add(fun2(equivs, arg));
		}
		String repl = x.head;
		if (equivs.containsKey(x)) {
			repl = equivs.get(x);
		}
		return new OplTerm<>(repl, ret);
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplChaseExp e) {
		OplObject I0 = ENV.get(e.I);
		List<OplQuery> EDs = new LinkedList<>();
		if (!(I0 instanceof OplInst)) {
			throw new RuntimeException("Not an instance: " + e.I);
		}
		OplInst I = (OplInst) I0;

		for (String ed : e.EDs) {
			OplObject ed0 = ENV.get(ed);
			if (!(ed0 instanceof OplQuery)) {
				throw new RuntimeException("Not a query: " + ed0);
			}
			EDs.add((OplQuery) ed0);
		}

		return OplChase.chase(I, EDs, e.limit);
	
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplGround e) {
		OplObject o = ENV.get(e.sch);
		if (!(o instanceof OplSchema)) {
			throw new RuntimeException("Not a schema: " + e.sch);
		}
		OplSchema sch = (OplSchema) o;
		return (OplObject) e.validate(sch).accept(env, this);
	}
	
	@Override
	public OplObject visit(Program<OplExp> env, OplGraph e) {
		return e;
	}

	@Override
	public OplObject visit(Program<OplExp> env, OplDistinct e) {
		OplObject o = ENV.get(e.str);
		if (!(o instanceof OplInst)) {
			throw new RuntimeException("Not an instance: " + e.str);
		}
		OplInst ret = (OplInst) o;
		return e.validate(ret);
	}

}

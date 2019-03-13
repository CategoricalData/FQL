package catdata.fql.decl;

import java.util.List;

import catdata.Pair;
import catdata.fql.parse.PrettyPrinter;

public abstract class TransExp {
	
	public abstract boolean gather();
	
	public Pair<String, String> type(FQLProgram p) {
		return accept(p, new TransChecker());
	}
	
	public static class Chi extends TransExp {
		
		public final String prop;
        public final String trans;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((prop == null) ? 0 : prop.hashCode());
			result = prime * result + ((trans == null) ? 0 : trans.hashCode());
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
			Chi other = (Chi) obj;
			if (prop == null) {
				if (other.prop != null)
					return false;
			} else if (!prop.equals(other.prop))
				return false;
			if (trans == null) {
				if (other.trans != null)
					return false;
			} else if (!trans.equals(other.trans))
				return false;
			return true;
		}

		public Chi(String prop, String trans) {
			this.prop = prop;
			this.trans = trans;
		}

		@Override
		public boolean gather() {
			return false;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
			return prop + ".char " + trans;
		}
	}
	
public static class UnChi extends TransExp {
		
		public final String a;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
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
			UnChi other = (UnChi) obj;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			return true;
		}

		public UnChi(String a) {
			this.a = a;
		}

		@Override
		public boolean gather() {
			return true;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
			return a + ".kernel";
		}
	}

	public static class Not extends TransExp {
		public final String prop;

		public Not(String prop) {
			this.prop = prop;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((prop == null) ? 0 : prop.hashCode());
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
			Not other = (Not) obj;
			if (prop == null) {
				if (other.prop != null)
					return false;
			} else if (!prop.equals(other.prop))
				return false;
			return true;
		}
		@Override
		public boolean gather() {
			return false;
		}
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
			return prop + ".not";
		}
	}
	
	public static class And extends TransExp {
		public final String prop;

		public And(String prop) {
			this.prop = prop;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((prop == null) ? 0 : prop.hashCode());
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
			And other = (And) obj;
			if (prop == null) {
				if (other.prop != null)
					return false;
			} else if (!prop.equals(other.prop))
				return false;
			return true;
		}
		@Override
		public boolean gather() {
			return false;
		}
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
			return prop + ".and";
		}
	}
	
	public static class Or extends TransExp {
		public final String prop;

		public Or(String prop) {
			this.prop = prop;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((prop == null) ? 0 : prop.hashCode());
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
			Or other = (Or) obj;
			if (prop == null) {
				if (other.prop != null)
					return false;
			} else if (!prop.equals(other.prop))
				return false;
			return true;
		}
		@Override
		public boolean gather() {
			return false;
		}
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
			return prop + ".or";
		}
	}
	
	public static class Implies extends TransExp {
		public final String prop;

		public Implies(String prop) {
			this.prop = prop;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((prop == null) ? 0 : prop.hashCode());
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
			Implies other = (Implies) obj;
			if (prop == null) {
				if (other.prop != null)
					return false;
			} else if (!prop.equals(other.prop))
				return false;
			return true;
		}
		@Override
		public boolean gather() {
			return false;
		}
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
			return prop + ".implies";
		}
	}
	
	public static class Bool extends TransExp {
		
		public final boolean bool;
		public final String unit;
        public final String prop;
		
		public Bool(boolean bool, String unit, String prop) {
			this.bool = bool;
			this.unit = unit;
			this.prop = prop;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + (bool ? 1231 : 1237);
			result = prime * result + ((prop == null) ? 0 : prop.hashCode());
			result = prime * result + ((unit == null) ? 0 : unit.hashCode());
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
			Bool other = (Bool) obj;
			if (bool != other.bool)
				return false;
			if (prop == null) {
				if (other.prop != null)
					return false;
			} else if (!prop.equals(other.prop))
				return false;
			if (unit == null) {
				if (other.unit != null)
					return false;
			} else if (!unit.equals(other.unit))
				return false;
			return true;
		}
		@Override
		public boolean gather() {
			return false;
		}
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
            return bool ? prop + ".true " + unit : prop + ".false " + unit;
		}
		
	}
	
	public static class TransIso extends TransExp {
		public final boolean lToR;
		public final String l;
        public final String r;
		
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + (lToR ? 1231 : 1237);
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			TransIso other = (TransIso) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (lToR != other.lToR)
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		public TransIso(boolean lToR, String l, String r) {
			this.lToR = lToR;
			this.l = l;
			this.r = r;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {
            return lToR ? "iso1 " + l + " " + r : "iso2 " + l + " " + r;
		}

		@Override
		public boolean gather() {
			return false;
		}
	}
	
	public static class TransCurry extends TransExp {
		
		public final String inst;
		public final String trans;
		
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((inst == null) ? 0 : inst.hashCode());
			result = prime * result + ((trans == null) ? 0 : trans.hashCode());
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
			TransCurry other = (TransCurry) obj;
			if (inst == null) {
				if (other.inst != null)
					return false;
			} else if (!inst.equals(other.inst))
				return false;
			if (trans == null) {
				if (other.trans != null)
					return false;
			} else if (!trans.equals(other.trans))
				return false;
			return true;
		}
		public TransCurry(String inst, String trans) {
			this.inst = inst;
			this.trans = trans;
		}
		@Override
		public String toString() {
			return inst + ".curry " + trans;
		}
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public boolean gather() {
			return false;
		}

	}	
	
	public static class TransEval extends TransExp {
		
		public final String inst;
		
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public TransEval(String inst) {
			this.inst = inst;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((inst == null) ? 0 : inst.hashCode());
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
			TransEval other = (TransEval) obj;
			if (inst == null) {
				if (other.inst != null)
					return false;
			} else if (!inst.equals(other.inst))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return inst + ".eval";
		}
		@Override
		public boolean gather() {
			return false;
		}

		
	}
	
	public static class Return extends TransExp {
		
		public final String inst;
		
		public Return(String inst) {
			this.inst = inst;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Return other = (Return) obj;
			if (inst == null) {
				if (other.inst != null)
					return false;
			} else if (!inst.equals(other.inst))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((inst == null) ? 0 : inst.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return inst + ".return";
		}
		
		@Override
		public boolean gather() {
			return true;
		}

	}
	
	public static class Coreturn extends TransExp {
		
		@Override
		public boolean gather() {
			return !isFull;
		}

		public final String inst;
		public boolean isFull = false; //should be set by checker
		
		
		public Coreturn(String inst) {
			this.inst = inst;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Coreturn other = (Coreturn) obj;
			if (inst == null) {
				if (other.inst != null)
					return false;
			} else if (!inst.equals(other.inst))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((inst == null) ? 0 : inst.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return inst + ".coreturn";
		}
		
	}
	
	public static class External extends TransExp {
		public final String src;
        public final String dst;
        public final String name;
		
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			External other = (External) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}
		public External(String src, String dst, String name) {
			this.src = src;
			this.dst = dst;
			this.name = name;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			return result;
		}
		@Override
		public String toString() {
			return "external " + src + " " + dst + " " + name;
		}
		@Override
		public boolean gather() {
			return true;
		}

	}
	
	public static class Squash extends TransExp {

		public final String src;
		
		@Override
		public String toString() {
			return src + ".relationalize";
		}
		
		public Squash(String src) {
			this.src = src;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Squash other = (Squash) obj;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			return result;
		}
		@Override
		public boolean gather() {
			return true;
		}

	}
	
	public static class Delta extends TransExp {
		public final TransExp h;
		public Delta(TransExp h, String src, String dst) {
			this.h = h;
			this.src = src;
			this.dst = dst;
		}
		public final String src;
        public final String dst;
		
		@Override
		public String toString() {
			return "delta " + src + " " + dst + " " + h;
		}
		
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Delta other = (Delta) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			return result;
		}
		@Override
		public boolean gather() {
			return true;
		}

	}
	
	public static class Sigma extends TransExp {
		public final TransExp h;
		public final String src;
        public final String dst;
		
		public Sigma(TransExp h, String src, String dst) {
			this.h = h;
			this.src = src;
			this.dst = dst;
		}

		@Override
		public String toString() {
			return "sigma " + src + " " + dst + " " + h;
		}
		
		
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
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
			Sigma other = (Sigma) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public boolean gather() {
			return true;
		}

	}
	
	public static class FullSigma extends TransExp {
		public final String h;
		public final String src;
        public final String dst;
		
		@Override
		public String toString() {
			return "SIGMA " + src + " " + dst + " " + h;
		}
		
		public FullSigma(String h, String src, String dst) {
			this.h = h;
			this.src = src;
			this.dst = dst;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
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
			FullSigma other = (FullSigma) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		@Override
		public boolean gather() {
			return false;
		}


	}
	
	public static class Pi extends TransExp {
		public final TransExp h;
		public final String src;
        public final String dst;
		
		@Override
		public String toString() {
			return "pi " + src + " " + dst + " " + h;
		}
		

		public Pi(TransExp h, String src, String dst) {
			this.h = h;
			this.src = src;
			this.dst = dst;
		}


		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
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
			Pi other = (Pi) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}
		
		@Override
		public boolean gather() {
			return true;
		}

	}
	
	public static class Relationalize extends TransExp {
		public final TransExp h;
		public final String src;
        public final String dst;

		@Override
		public String toString() {
			return "relationalize " + src + " " + dst + " " + h;
		}
		

		public Relationalize(TransExp h, String src, String dst) {
			this.h = h;
			this.src = src;
			this.dst = dst;
		}


		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
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
			Relationalize other = (Relationalize) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}
	

	
/*
	public Const toConst(Map<String, SigExp> env, Map<String, TransExp> ctx) {
		return accept(new Pair<>(env, ctx), new SigOps());
	}
	*/
	/*
	public Transform toMap(Map<String, SigExp> env, Map<String, TransExp> ctx) {
		Const e = toConst(env, ctx);
		try {
			return new Mapping(e.src.toSig(env), e.dst.toSig(env), e.objs, e.attrs,
					e.arrows);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}
	*/

	public abstract <R, E> R accept(E env, TransExpVisitor<R, E> v);

	@Override
	public abstract boolean equals(Object o);
/*
	public final Pair<SigExp, SigExp> type(Map<String, SigExp> env,
			Map<String, TransExp> ctx) {
		return accept(new Pair<>(env, ctx), new TransExpChecker(
				new LinkedList<String>()));
	}
	*/

	public static class Id extends TransExp {
		public final String t;

		public Id(String t) {
			this.t = t;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((t == null) ? 0 : t.hashCode());
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
			Id other = (Id) obj;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return "id " + t;
		}

		@Override
		public boolean gather() {
			return true;
		}

	}
/*
	public static class Dist2 extends TransExp {
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public SigExp a, b, c;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
			result = prime * result + ((c == null) ? 0 : c.hashCode());
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
			Dist2 other = (Dist2) obj;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (b == null) {
				if (other.b != null)
					return false;
			} else if (!b.equals(other.b))
				return false;
			if (c == null) {
				if (other.c != null)
					return false;
			} else if (!c.equals(other.c))
				return false;
			return true;
		}

		public Dist2(SigExp a, SigExp b, SigExp c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}

		@Override
		public String toString() {
			return "dist2";
		}

	}

	public static class Dist1 extends TransExp {
		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public SigExp a, b, c;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
			result = prime * result + ((c == null) ? 0 : c.hashCode());
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
			Dist1 other = (Dist1) obj;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (b == null) {
				if (other.b != null)
					return false;
			} else if (!b.equals(other.b))
				return false;
			if (c == null) {
				if (other.c != null)
					return false;
			} else if (!c.equals(other.c))
				return false;
			return true;
		}

		public Dist1(SigExp a, SigExp b, SigExp c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}

		@Override
		public String toString() {
			return "dist1";
		}

	}
*/
	public static class Const extends TransExp {
		public final List<Pair<String, List<Pair<Object, Object>>>> objs;
		public final String src;
    public final String dst;

		public Const(List<Pair<String, List<Pair<Object, Object>>>> objs,
				 String src, String dst) {
			this.objs = objs;
			this.src = src;
			this.dst = dst;
			//Collections.sort(this.objs);
		}

	
		@Override
		public String toString() {
			
			String nm = "\n nodes\n";
			boolean b = false;
			for (Pair<String, List<Pair<Object, Object>>> k : objs) {
				if (b) {
					nm += ", \n";
				}
				b = true;
				
				boolean c = false;
				nm += "  " + k.first + " -> " + "{";

				for (Pair<Object, Object> k0 : k.second) {
					if (c) {
						nm += ", ";
					}
					c = true;
					nm += "(" + PrettyPrinter.q(k0.first) + ", " + PrettyPrinter.q(k0.second) + ")";
				}
				nm += "}";
			}
			nm = nm.trim();
			nm += ";\n";

			return "{\n " + nm + "}";
		}
		

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((objs == null) ? 0 : objs.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
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
			Const other = (Const) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (objs == null) {
				if (other.objs != null)
					return false;
			} else if (!objs.equals(other.objs))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public boolean gather() {
			return true;
		}


	}

	public static class Var extends TransExp {
		public final String v;

		public Var(String v) {
			this.v = v;
			if (v.contains(" ")) {
				throw new RuntimeException();
			}
		}

		@Override
		public String toString() {
			return v;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((v == null) ? 0 : v.hashCode());
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
			Var other = (Var) obj;
			if (v == null) {
				if (other.v != null)
					return false;
			} else if (!v.equals(other.v))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}

	public static class TT extends TransExp {
		public final String obj;
		public final String tgt;

		public TT(String obj, String tgt) {
			this.tgt = tgt;
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
			result = prime * result + ((tgt == null) ? 0 : tgt.hashCode());
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
			TT other = (TT) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			if (tgt == null) {
				if (other.tgt != null)
					return false;
			} else if (!tgt.equals(other.tgt))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".unit " + tgt;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}

	public static class FF extends TransExp {
		public final String tgt;
        public final String obj;

		public FF(String obj, String tgt) {
			this.tgt = tgt;
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
			result = prime * result + ((tgt == null) ? 0 : tgt.hashCode());
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
			FF other = (FF) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			if (tgt == null) {
				if (other.tgt != null)
					return false;
			} else if (!tgt.equals(other.tgt))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".void " + tgt;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}

	public static class Fst extends TransExp {
		public final String obj;

		public Fst(String obj) {
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
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
			Fst other = (Fst) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".fst";
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}

	public static class Snd extends TransExp {
		public final String obj;

		public Snd(String obj) {
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
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
			Snd other = (Snd) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".snd";
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}

	public static class Inl extends TransExp {
		public final String obj;

		public Inl(String obj) {
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
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
			Inl other = (Inl) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".inl ";
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}

	public static class Inr extends TransExp {
		public final String obj;

		public Inr(String obj) {
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
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
			Inr other = (Inr) obj;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".inr";
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public boolean gather() {
			return true;
		}

	}
/*
	public static class Apply extends TransExp {
		SigExp s, t;

		public Apply(SigExp s, SigExp t) {
			this.s = s;
			this.t = t;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((s == null) ? 0 : s.hashCode());
			result = prime * result + ((t == null) ? 0 : t.hashCode());
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
			Apply other = (Apply) obj;
			if (s == null) {
				if (other.s != null)
					return false;
			} else if (!s.equals(other.s))
				return false;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "apply " + s + " " + t;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Curry extends TransExp {
		TransExp f;

		public Curry(TransExp f) {
			this.f = f;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
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
			Curry other = (Curry) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		public String toString() {
			return "curry " + f;
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}
*/
	public static class Prod extends TransExp {
		public final String obj;
		public final TransExp l;
    public final TransExp r;

		public Prod(String obj, TransExp l, TransExp r) {
			this.l = l;
			this.r = r;
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			Prod other = (Prod) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".(" + l + " * " + r + ")";
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public boolean gather() {
			return true;
		}


	}

	public static class Comp extends TransExp {
		public final TransExp l;
        public final TransExp r;

		public Comp(TransExp TransExp, TransExp r) {
            l = TransExp;
			this.r = r;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			Comp other = (Comp) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "(" + l + " then " + r + ")";
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public boolean gather() {
			return true;
		}


	}

	public static class Case extends TransExp {
		public final String obj;
		public final TransExp l;
        public final TransExp r;

		public Case(String obj, TransExp l, TransExp r) {
			this.l = l;
			this.r = r;
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			Case other = (Case) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (this.obj == null) {
				if (other.obj != null)
					return false;
			} else if (!this.obj.equals(other.obj))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return obj + ".(" + l + " + " + r + ")";
		}

		@Override
		public <R, E> R accept(E env, TransExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public boolean gather() {
			return true;
		}


	}

	@Override
	public abstract int hashCode();

	public interface TransExpVisitor<R, E> {
		R visit(E env, Or e);
		R visit(E env, Implies e);
		R visit(E env, And e);
		R visit(E env, Not e);
		R visit(E env, Id e);
		R visit(E env, Comp e);
		R visit(E env, Var e);
		R visit(E env, Const e);
		R visit(E env, TT e);
		R visit(E env, FF e);
		R visit(E env, Fst e);
		R visit(E env, Snd e);
		R visit(E env, Inl e);
		R visit(E env, Inr e);
		R visit(E env, Delta e);
		R visit(E env, Sigma e);
		R visit(E env, FullSigma e);
		R visit(E env, Pi e);
		R visit(E env, Relationalize e);
		R visit(E env, Squash e);
		R visit(E env, TransCurry e);
		R visit(E env, TransEval e);
		R visit(E env, Case e);
		R visit(E env, Prod e);
		R visit(E env, External e);
		R visit(E env, Return e);
		R visit(E env, Coreturn e);
		R visit(E env, TransIso e);
		R visit(E env, Bool e);
		R visit(E env, Chi e);
		R visit(E env, UnChi e);
	}


}

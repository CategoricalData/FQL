package catdata.fql.decl;

import java.util.LinkedList;
import java.util.List;

import catdata.Pair;
import catdata.fql.parse.PrettyPrinter;

public abstract class InstExp {

	public final SigExp type(FQLProgram prog) {
		return accept(prog, new InstChecker());
	}
	
	public static class Step extends InstExp {
		public final String I;
		public final MapExp m;
        public final MapExp n;
		
		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {
			return "step " + m + " " + n + " " + I;
		}
		
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((m == null) ? 0 : m.hashCode());
			result = prime * result + ((n == null) ? 0 : n.hashCode());
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
			Step other = (Step) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (m == null) {
				if (other.m != null)
					return false;
			} else if (!m.equals(other.m))
				return false;
			if (n == null) {
				if (other.n != null)
					return false;
			} else if (!n.equals(other.n))
				return false;
			return true;
		}
		public Step(String i, MapExp m, MapExp n) {
            I = i;
			this.m = m;
			this.n = n;
		}
		
	}
	
	public static class Kernel extends InstExp {
		public final String trans;

		public Kernel(String trans) {
            this.trans = trans;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Kernel other = (Kernel) obj;
			if (trans == null) {
				if (other.trans != null)
					return false;
			} else if (!trans.equals(other.trans))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((trans == null) ? 0 : trans.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return "kernel " + trans;
		}
	}
	
/*	public static class Prop extends InstExp {
		public Prop(SigExp sig) {
			super();
			this.sig = sig;
		}
		
		@Override 
		public String toString() {
			return "prop " + sig;
		}

		public SigExp sig;

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Prop other = (Prop) obj;
			if (sig == null) {
				if (other.sig != null)
					return false;
			} else if (!sig.equals(other.sig))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sig == null) ? 0 : sig.hashCode());
			return result;
		}
	} */
	
	public static class FullEval extends InstExp {
		public final FullQueryExp q;
		public final String e;

		public FullEval(FullQueryExp q, String e) {
            this.q = q;
			this.e = e;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((e == null) ? 0 : e.hashCode());
			result = prime * result + ((q == null) ? 0 : q.hashCode());
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
			FullEval other = (FullEval) obj;
			if (e == null) {
				if (other.e != null)
					return false;
			} else if (!e.equals(other.e))
				return false;
			if (q == null) {
				if (other.q != null)
					return false;
			} else if (!q.equals(other.q))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "EVAL " + q + " " + e;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}
	
	public static class Eval extends InstExp {
		public final QueryExp q;
		public final String e;

		public Eval(QueryExp q, String e) {
            this.q = q;
			this.e = e;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((e == null) ? 0 : e.hashCode());
			result = prime * result + ((q == null) ? 0 : q.hashCode());
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
			Eval other = (Eval) obj;
			if (e == null) {
				if (other.e != null)
					return false;
			} else if (!e.equals(other.e))
				return false;
			if (q == null) {
				if (other.q != null)
					return false;
			} else if (!q.equals(other.q))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "eval " + q + " " + e;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	//  Const equality for instances
	public static class Const extends InstExp {
		// pubic List
		// public List<Pair<String, List<Pair<Object, Object>>>> data;
		public final SigExp sig;
		public final List<Pair<String, List<Pair<Object, Object>>>> nodes;
        public final List<Pair<String, List<Pair<Object, Object>>>> attrs;
        public final List<Pair<String, List<Pair<Object, Object>>>> arrows;
        public final List<Pair<String, List<Pair<Object, Object>>>> data;

		public Const(List<Pair<String, List<Pair<Object, Object>>>> nodes,
				List<Pair<String, List<Pair<Object, Object>>>> attrs,
				List<Pair<String, List<Pair<Object, Object>>>> arrows,
				SigExp sig) {
			this.nodes = nodes;
			this.attrs = attrs;
			this.arrows = arrows;
			this.sig = sig;
			data = new LinkedList<>();
			List<String> seen = new LinkedList<>();
			for (Pair<String, List<Pair<Object, Object>>> k : nodes) {
				if (seen.contains(k.first)) {
					throw new RuntimeException("Duplicate table: " + k.first);
				}
				seen.add(k.first);
			}
			for (Pair<String, List<Pair<Object, Object>>> k : attrs) {
				if (seen.contains(k.first)) {
					throw new RuntimeException("Duplicate table: " + k.first);
				}
				seen.add(k.first);
			}
			for (Pair<String, List<Pair<Object, Object>>> k : arrows) {
				if (seen.contains(k.first)) {
					throw new RuntimeException("Duplicate table: " + k.first);
				}
				seen.add(k.first);
			}
			
			data.addAll(nodes);
			data.addAll(attrs);
			data.addAll(arrows);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((data == null) ? 0 : data.hashCode());
			result = prime * result + ((sig == null) ? 0 : sig.hashCode());
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
			if (data == null) {
				if (other.data != null)
					return false;
			} else if (!data.equals(other.data))
				return false;
			if (sig == null) {
				if (other.sig != null)
					return false;
			} else if (!sig.equals(other.sig))
				return false;
			return true;
		}

		
		
		@Override
		public String toString() {
			String x = "\n nodes\n";
			boolean b = false;
			for (Pair<String, List<Pair<Object, Object>>> k : nodes) {
				if (b) {
					x += ", \n";
				}
				b = true;
				x += "  " + k.first + " -> {";
				boolean d = false;
				for (Pair<Object, Object> v : k.second) {
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
			for (Pair<String, List<Pair<Object, Object>>> k : attrs) {
				if (b) {
					x += ", \n";
				}
				b = true;
				x += "  " + k.first + " -> {";
				boolean d = false;
				for (Pair<Object, Object> v : k.second) {
					if (d) {
						x += ", ";
					}
					d = true;
					x += "(" + PrettyPrinter.q(v.first) + ", " + PrettyPrinter.q(v.second) + ")";
				}
				x += "}";
			}
			x = x.trim();
			x += ";\n";
			x += " arrows\n";
			b = false;
			for (Pair<String, List<Pair<Object, Object>>> k : arrows) {
				if (b) {
					x += ", \n";
				}
				b = true;
				x += "  " + k.first + " -> {";
				boolean d = false;
				for (Pair<Object, Object> v : k.second) {
					if (d) {
						x += ", ";
					}
					d = true;
					x += "(" + PrettyPrinter.q(v.first) + ", " + PrettyPrinter.q(v.second) + ")";
				}
				x += "}";
			}
			x = x.trim();
			return "{\n " + x + ";\n}";
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		

	}

	

	public static class Zero extends InstExp {

		public final SigExp sig;

		public Zero(SigExp sig) {
            this.sig = sig;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((sig == null) ? 0 : sig.hashCode());
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
			Zero other = (Zero) obj;
			if (sig == null) {
				if (other.sig != null)
					return false;
			} else if (!sig.equals(other.sig))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "void " + sig;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class One extends InstExp {

		public final SigExp sig;

		public One(SigExp sig) {
            this.sig = sig;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((sig == null) ? 0 : sig.hashCode());
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
			One other = (One) obj;
			if (sig == null) {
				if (other.sig != null)
					return false;
			} else if (!sig.equals(other.sig))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "unit " + sig;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Two extends InstExp {

		public final SigExp sig;

		public Two(SigExp sig) {
            this.sig = sig;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((sig == null) ? 0 : sig.hashCode());
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
			Two other = (Two) obj;
			if (sig == null) {
				if (other.sig != null)
					return false;
			} else if (!sig.equals(other.sig))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "prop " + sig;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Plus extends InstExp {
		public final String a;
        public final String b;

		public Plus(String a, String b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
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
			Plus other = (Plus) obj;
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
			return true;
		}

		@Override
		public String toString() {
			return "(" + a + " + " + b + ")";
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Times extends InstExp {
		public final String a;
        public final String b;

		public Times(String a, String b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
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
			Times other = (Times) obj;
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
			return true;
		}

		@Override
		public String toString() {
			return "(" + a + " * " + b + ")";
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	
	public static class Exp extends InstExp {
		public final String a;
        public final String b;

		public Exp(String a, String b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
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
			Exp other = (Exp) obj;
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
			return true;
		}

		@Override
		public String toString() {
			return "(" + a + " ^ " + b + ")";
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Delta extends InstExp {

		public final MapExp F;
		public final String I;

		public Delta(MapExp f, String i) {
            F = f;
			I = i;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			Delta other = (Delta) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "delta " + F + " " + I;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Sigma extends InstExp {

		public final MapExp F;
		public final String I;

		public Sigma(MapExp f, String i) {
            F = f;
			I = i;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "sigma " + F + " " + I;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Pi extends InstExp {

		public final MapExp F;
		public final String I;

		public Pi(MapExp f, String i) {
            F = f;
			I = i;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "pi " + F + " " + I;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class FullSigma extends InstExp {

		public final MapExp F;
		public final String I;

		public FullSigma(MapExp f, String i) {
            F = f;
			I = i;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "SIGMA " + F + " " + I;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Relationalize extends InstExp {
		public final String I;

		public Relationalize(String i) {
            I = i;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "relationalize " + I;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class External extends InstExp {

		public final SigExp sig;
		public final String name;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((sig == null) ? 0 : sig.hashCode());
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
			External other = (External) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (sig == null) {
				if (other.sig != null)
					return false;
			} else if (!sig.equals(other.sig))
				return false;
			return true;
		}

		public External(SigExp sig, String name) {
            this.sig = sig;
			this.name = name;
		}

		@Override
		public String toString() {
			return "external " + sig + " " + name;
		}

		@Override
		public <R, E> R accept(E env, InstExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	@Override
	public abstract boolean equals(Object o);

	public abstract <R, E> R accept(E env, InstExpVisitor<R, E> v);

	@Override
	public abstract int hashCode();

	public interface InstExpVisitor<R, E> {
		R visit(E env, Zero e);
		R visit(E env, One e);
		R visit(E env, Two e);
		R visit(E env, Plus e);
		R visit(E env, Times e);
		R visit(E env, Exp e);
		// public R visit (E env, Var e);
        R visit(E env, Const e);
		R visit(E env, Delta e);
		R visit(E env, Sigma e);
		R visit(E env, Pi e);
		R visit(E env, FullSigma e);
		R visit(E env, Relationalize e);
		R visit(E env, External e);
		R visit(E env, Eval e);
		R visit(E env, FullEval e);
		R visit(E env, Kernel e);
		R visit(E env, Step e);
	}

}

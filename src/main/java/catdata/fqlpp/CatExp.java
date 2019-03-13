package catdata.fqlpp;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;


@SuppressWarnings("serial")
public abstract class CatExp implements Serializable {
	
	public static class Colim extends CatExp {
		final String F;
		
		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
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
			Colim other = (Colim) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			return true;
		}

		public Colim(String f) {
			F = f;
		}
		
		@Override
		public String toString() {
			return "colim " + F;
		}
	}
	
	
	public static class Union extends CatExp {
		final CatExp l;
        final CatExp r;
		
		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
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
			Union other = (Union) obj;
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

		public Union(CatExp l, CatExp r) {
            this.l = l;
			this.r = r;
		}
		
		
		
	}
	
	
	
	public static class Kleisli extends CatExp {
		final String F;
        final String unit;
        final String join;
		
		final Boolean isCo;
		
		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((isCo == null) ? 0 : isCo.hashCode());
			result = prime * result + ((join == null) ? 0 : join.hashCode());
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
			Kleisli other = (Kleisli) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			if (isCo == null) {
				if (other.isCo != null)
					return false;
			} else if (!isCo.equals(other.isCo))
				return false;
			if (join == null) {
				if (other.join != null)
					return false;
			} else if (!join.equals(other.join))
				return false;
			if (unit == null) {
				if (other.unit != null)
					return false;
			} else if (!unit.equals(other.unit))
				return false;
			return true;
		}

		public Kleisli(String f, String unit, String join, Boolean isCo) {
            F = f;
			this.unit = unit;
			this.join = join;
			this.isCo = isCo;
		}
		
	}
	
	public static class Named extends CatExp {
		final Object name;
		
		public Named(Object name) {
			this.name = name;
		}
		
		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			Named other = (Named) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		
	}
		
	public static class Const extends CatExp {
		
		public final Set<String> nodes;
		public final Set<Triple<String, String, String>> arrows;
		public final Set<Pair<Pair<String, List<String>>, Pair<String, List<String>>>> eqs;

		public Const(
				Set<String> nodes,
				Set<Triple<String, String, String>> arrows,
				Set<Pair<Pair<String, List<String>>, Pair<String, List<String>>>> eqs) {
			this.nodes = nodes;
			this.arrows = arrows;
			this.eqs = eqs;
		}

		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result
					+ ((arrows == null) ? 0 : arrows.hashCode());
			result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
			result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
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
			if (arrows == null) {
				if (other.arrows != null)
					return false;
			} else if (!arrows.equals(other.arrows))
				return false;
			if (eqs == null) {
				if (other.eqs != null)
					return false;
			} else if (!eqs.equals(other.eqs))
				return false;
			if (nodes == null) {
				if (other.nodes != null)
					return false;
			} else if (!nodes.equals(other.nodes))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			String x = "\n objects\n";
			boolean b = false;
			for (String n : nodes) {
				if (b) {
					x += ",\n";
				}
				x += "  " + n;
				b = true;
			}
			
			x = x.trim();
			x += ";\n";
			x += " arrows\n";

			b = false;
			for (Triple<String, String, String> a : arrows) {
				if (b) {
					x += ",\n";
				}
				x += "  " + a.first + ": " + a.second + " -> " + a.third;
				b = true;
			}

			x = x.trim();
			x += ";\n";
			x += " equations\n";

			b = false;
			for (Pair<Pair<String, List<String>>, Pair<String, List<String>>> a : eqs) {
				if (b) {
					x += ",\n";
				}
				x += "  " + printOneEq(a.first) + " = " + printOneEq(a.second);
				b = true;
			}
			x = x.trim();
			return "{\n " + x + ";\n}";

		}
		
		private static String printOneEq(Pair<String, List<String>> l) {
			String ret = l.first;
			for (String a : l.second) {
				ret += ".";
				ret += a;
			}
			return ret;
		}
		
	}

	public static class Var extends CatExp {
		public String v;

		public Var(String v) {
			if (v.contains(" ") || v.equals("void") || v.equals("unit")) {
				throw new RuntimeException("Cannot var " + v);
			}
			this.v = v;
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
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Zero extends CatExp {

		public Zero() {
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof Zero);
		}

		@Override
		public String toString() {
			return "void";
		}

		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	
	public static class One extends CatExp {

		public One() {
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof One);
		}

		@Override
		public String toString() {
			return "unit";
		}

		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}
	
	
	public static class Plus extends CatExp {
		final CatExp a;
        final CatExp b;

		public Plus(CatExp a, CatExp b) {
			this.a = a;
			this.b = b;
			if (a == null || b == null) {
				throw new RuntimeException();
			}
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + a.hashCode();
			result = prime * result + b.hashCode();
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
			return a.equals(other.a) && b.equals(other.b);
		}

		@Override
		public String toString() {
			return "(" + a + " + " + b + ")";
		}

		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Times extends CatExp {
		public final CatExp a;
        public final CatExp b;

		public Times(CatExp a, CatExp b) {
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
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Exp extends CatExp {
		public final CatExp a;
        public final CatExp b;

		public Exp(CatExp a, CatExp b) {
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
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}
	
	public static class Dom extends CatExp {

		public Dom(FunctorExp f) {
			this.f = f;
		}

		@Override
		public int hashCode() {
			int prime = 31;
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
			Dom other = (Dom) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		public final FunctorExp f;
		
		@Override
		public String toString() {
			return "dom " + f;
		}

		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}
	
	public static class Cod extends CatExp {

		public Cod(FunctorExp f) {
			this.f = f;
		}

		@Override
		public int hashCode() {
			int prime = 31;
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
			Cod other = (Cod) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		public final FunctorExp f;
		
		@Override
		public String toString() {
			return "cod " + f;
		}

		@Override
		public <R, E> R accept(E env, CatExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}


	@Override
	public abstract boolean equals(Object o);

	public abstract <R, E> R accept(E env, CatExpVisitor<R, E> v);

	@Override
	public abstract int hashCode();
	
	public interface CatExpVisitor<R, E> {
		R visit(E env, Zero e);
		R visit(E env, One e);
		R visit(E env, Plus e);
		R visit(E env, Times e);
		R visit(E env, Exp e);
		R visit(E env, Var e);
		R visit(E env, Const e);
		R visit(E env, Dom e);
		R visit(E env, Cod e);
		R visit(E env, Named e);
		R visit(E env, Kleisli e);
		R visit(E env, Union e);
		R visit(E env, Colim e);
	}
	
}

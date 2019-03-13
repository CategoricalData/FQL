package catdata.fqlpp;

import java.io.Serializable;
import java.util.Set;

import catdata.Util;

@SuppressWarnings("serial")
public abstract class SetExp implements Serializable {
	
	public static class Intersect extends SetExp {
		public final SetExp set1;
        public final SetExp set;

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		public Intersect(SetExp set1, SetExp set) {
            this.set1 = set1;
			this.set = set;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((set == null) ? 0 : set.hashCode());
			result = prime * result + ((set1 == null) ? 0 : set1.hashCode());
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
			Intersect other = (Intersect) obj;
			if (set == null) {
				if (other.set != null)
					return false;
			} else if (!set.equals(other.set))
				return false;
			if (set1 == null) {
				if (other.set1 != null)
					return false;
			} else if (!set1.equals(other.set1))
				return false;
			return true;
		}
		
	}
	
	public static class Union extends SetExp {
		public final SetExp set1;
        public final SetExp set;
		
		public Union(SetExp set1, SetExp set) {
            this.set1 = set1;
			this.set = set;
		}

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((set == null) ? 0 : set.hashCode());
			result = prime * result + ((set1 == null) ? 0 : set1.hashCode());
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
			if (set == null) {
				if (other.set != null)
					return false;
			} else if (!set.equals(other.set))
				return false;
			if (set1 == null) {
				if (other.set1 != null)
					return false;
			} else if (!set1.equals(other.set1))
				return false;
			return true;
		}
		
	}

	public static class Apply extends SetExp {
		public final String f;
		public final SetExp set;

		public Apply(String f, SetExp set) {
			this.f = f;
			this.set = set;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
			result = prime * result + ((set == null) ? 0 : set.hashCode());
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
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			if (set == null) {
				if (other.set != null)
					return false;
			} else if (!set.equals(other.set))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Numeral extends SetExp {
		public final int i;

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public Numeral(int i) {
			this.i = i;
		}

		@Override
		public String toString() {
			return Integer.toString(i);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + i;
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
			Numeral other = (Numeral) obj;
            return i == other.i;
        }

	}

	public static class Const extends SetExp {

		@Override
		public String toString() {
			return Util.nice(s.toString());
		}

		final Set<?> s;

		public Const(Set<?> s) {
            this.s = s;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((s == null) ? 0 : s.hashCode());
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
			if (s == null) {
				if (other.s != null)
					return false;
			} else if (!s.equals(other.s))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Var extends SetExp {
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
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Zero extends SetExp {

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
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Dom extends SetExp {

		public Dom(FnExp f) {
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

		public final FnExp f;

		@Override
		public String toString() {
			return "dom " + f;
		}

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Cod extends SetExp {

		public Cod(FnExp f) {
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

		public final FnExp f;

		@Override
		public String toString() {
			return "cod " + f;
		}

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Range extends SetExp {

		public Range(FnExp f) {
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
			Range other = (Range) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		public final FnExp f;

		@Override
		public String toString() {
			return "range " + f;
		}

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class One extends SetExp {

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
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Prop extends SetExp {

		public Prop() {
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
			return "prop";
		}

		@Override
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Plus extends SetExp {
		final SetExp a;
        final SetExp b;

		public Plus(SetExp a, SetExp b) {
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
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Times extends SetExp {
		public final SetExp a;
        public final SetExp b;

		public Times(SetExp a, SetExp b) {
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
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Exp extends SetExp {
		public final SetExp a;
        public final SetExp b;

		public Exp(SetExp a, SetExp b) {
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
		public <R, E> R accept(E env, SetExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	@Override
	public abstract boolean equals(Object o);

	public abstract <R, E> R accept(E env, SetExpVisitor<R, E> v);

	@Override
	public abstract int hashCode();

	public interface SetExpVisitor<R, E> {
		R visit(E env, Zero e);

		R visit(E env, One e);

		R visit(E env, Prop e);

		R visit(E env, Plus e);

		R visit(E env, Times e);

		R visit(E env, Exp e);

		R visit(E env, Var e);

		R visit(E env, Dom e);

		R visit(E env, Cod e);

		R visit(E env, Range e);

		R visit(E env, Const e);

		R visit(E env, Numeral e);

		R visit(E env, Apply e);
		
		R visit(E env, Union e);
		
		R visit(E env, Intersect e);
		
	}

}

package catdata.fql.decl;

import java.util.*;

import catdata.fql.FQLException;
import catdata.Pair;
import catdata.Triple;
import catdata.fql.parse.PrettyPrinter;

public abstract class SigExp {
	
	

	public SigExp unresolve(Map<String, SigExp> env) {
		return accept(env, new Unresolver());
	}
	
	public Const toConst(FQLProgram env) {
		return accept(env, new SigOps());
	}
	
	public Signature toSig(FQLProgram env) {
		Const e = toConst(env);
		try {
			return new Signature(env.enums, e.nodes, e.attrs, e.arrows, e.eqs);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}
	
	public static class Unknown extends SigExp {

		public Unknown(String name) {
			this.name = name;
		}

		public final String name;
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Unknown other = (Unknown) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
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
		public String toString() {
			return name;
		}
		
	}
		
	public static class Opposite extends SigExp {
		final SigExp e;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((e == null) ? 0 : e.hashCode());
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
			Opposite other = (Opposite) obj;
			if (e == null) {
				if (other.e != null)
					return false;
			} else if (!e.equals(other.e))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "opposite " + e;
		}

		public Opposite(SigExp e) {
            this.e = e;
		}
		
		@Override
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Union extends SigExp {
		final SigExp l;
        final SigExp r;

		public Union(SigExp l, SigExp r) {
            this.l = l;
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

		@Override
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {
			return "(" + l + " union " + r + ")";
		}
	}
	
	public static class Const extends SigExp {
		public final List<String> nodes;
		public final List<Triple<String, String, String>> attrs;
		public final List<Triple<String, String, String>> arrows;
		public final List<Pair<List<String>, List<String>>> eqs;

		public Const(List<String> nodes,
				List<Triple<String, String, String>> attrs,
				List<Triple<String, String, String>> arrows,
				List<Pair<List<String>, List<String>>> eqs) {
            this.nodes = nodes;
			this.attrs = attrs;
			this.arrows = arrows;
			this.eqs = eqs;
			Collections.sort(this.nodes);
			Collections.sort(this.attrs);
			Collections.sort(this.arrows);
			(this.eqs).sort(comp);
		}
		
		static final Comparator<Pair<List<String>, List<String>>> comp = new Comparator<>() {

			@Override
			public int compare(Pair<List<String>, List<String>> o1,
					Pair<List<String>, List<String>> o2) {
				int c = compareTo(o1.first, o2.first);
                return c == 0 ? compareTo(o1.second, o2.second) : c;

			}

			private int compareTo(List<String> l, List<String> r) {
				List<String> small, large;
				if (l.size() < r.size()) {
					small = l;
					large = r;
				} else {
					small = r;
					large = l;
				}
				for (int i = 0; i < small.size(); i++) {
					int c = small.get(i).compareTo(large.get(i));
					if (c == 0) {
					}
				}
				if (l.size() == r.size()) {
					return 0;
				} else if (Objects.equals(small, l)) {
					return -1;
				}
				return 1;


			}
			
		};


		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result
					+ ((arrows == null) ? 0 : arrows.hashCode());
			result = prime * result + ((attrs == null) ? 0 : attrs.hashCode());
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
			if (attrs == null) {
				if (other.attrs != null)
					return false;
			} else if (!attrs.equals(other.attrs))
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
			String x = "\n nodes\n";
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
			x += " attributes\n";
			b = false;
			for (Triple<String, String, String> a : attrs) {
				if (b) {
					x += ",\n";
				}
				x += "  " + a.first + ": " + a.second + " -> " + a.third;
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
			for (Pair<List<String>, List<String>> a : eqs) {
				if (b) {
					x += ",\n";
				}
				x += "  " + printOneEq(a.first) + " = " + printOneEq(a.second);
				b = true;
			}
			x = x.trim();
			return "{\n " + x + ";\n}";

		}
		
		private static String printOneEq(List<String> l) {
			String ret = "";
			boolean b = false;
			for (String a : l) {
				if (b) {
					ret += ".";
				}
				ret += a;
				b = true;
			}
			return ret;
		}

		@Override
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Var extends SigExp {
		public String v;

		public Var(String v) {
			if (v.contains(" ") || v.equals("void") || v.equals("unit")) {
				throw new RuntimeException();
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
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Zero extends SigExp {

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
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class One extends SigExp {
		
		final Set<String> attrs;
		
		public One(Set<String> attrs) {
			this.attrs = attrs;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((attrs == null) ? 0 : attrs.hashCode());
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
			if (attrs == null) {
				if (other.attrs != null)
					return false;
			} else if (!attrs.equals(other.attrs))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "unit {" + PrettyPrinter.sep0(", ", new LinkedList<>(attrs)) + "}";
		}

		@Override
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Plus extends SigExp {
		final SigExp a;
        final SigExp b;

		public Plus(SigExp a, SigExp b) {
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
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Times extends SigExp {
		public final SigExp a;
        public final SigExp b;

		public Times(SigExp a, SigExp b) {
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
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class Exp extends SigExp {
		public final SigExp a;
        public final SigExp b;

		public Exp(SigExp a, SigExp b) {
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
		public <R, E> R accept(E env, SigExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	@Override
	public abstract boolean equals(Object o);

	public abstract <R, E> R accept(E env, SigExpVisitor<R, E> v);

	@Override
	public abstract int hashCode();
	
	public SigExp typeOf(FQLProgram env) {
		return accept(env, new SigExpChecker());
	}
	
	
	public interface SigExpVisitor<R, E> {
		R visit(E env, Zero e);
		R visit(E env, One e);
		R visit(E env, Plus e);
		R visit(E env, Times e);
		R visit(E env, Exp e);
		R visit(E env, Var e);
		R visit(E env, Const e);
		R visit(E env, Union e);
		R visit(E env, Opposite e);
		R visit(E env, Unknown e);

	}
	
}

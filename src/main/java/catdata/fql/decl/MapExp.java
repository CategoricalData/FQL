package catdata.fql.decl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import catdata.fql.FQLException;
import catdata.Pair;
import catdata.fql.parse.PrettyPrinter;

public abstract class MapExp {

	public Const toConst(FQLProgram env) {
		return accept(env, new SigOps());
	}
	
	public Mapping toMap(FQLProgram env) {
		Const e = toConst(env);
		try {
			return new Mapping(e.src.toSig(env), e.dst.toSig(env), e.objs, e.attrs,
					e.arrows);
		} catch (FQLException fe) {
			fe.printStackTrace();
			throw new RuntimeException(fe.getLocalizedMessage());
		}
	}

	public abstract <R, E> R accept(E env, MapExpVisitor<R, E> v);

	@Override
	public abstract boolean equals(Object o);

	public final Pair<SigExp, SigExp> type(FQLProgram env) {
		return accept(env, new MapExpChecker(
                new LinkedList<>()));
	}
	
	public static class Iso extends MapExp {
		
		public Iso(boolean lToR, SigExp l, SigExp r) {
			this.lToR = lToR;
			this.l = l;
			this.r = r;
		}
		@Override
		public String toString() {
			if (lToR) {
				return "iso1 " + l + " " + r;
			} 
			return "iso2 " + l + " " + r;
		}
		
		final boolean lToR;
		final SigExp l;
        final SigExp r;
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
			Iso other = (Iso) obj;
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
		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
	}
	
	public static class Sub extends MapExp {
		final SigExp s;
        final SigExp t;

		@Override
		public int hashCode() {
			int prime = 31;
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
			Sub other = (Sub) obj;
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

		public Sub(SigExp s, SigExp t) {
			this.s = s;
			this.t = t;
		}
		
		@Override
		public String toString() {
			return "subschema " + s + " " + t;
		}
		
		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}
	
	public static class Id extends MapExp {
		public final SigExp t;

		public Id(SigExp t) {
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
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return "id " + t;
		}

	}

	public static class Dist2 extends MapExp {
		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public final SigExp a;
        public final SigExp b;
        public final SigExp c;

		@Override
		public int hashCode() {
			int prime = 31;
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

	public static class Dist1 extends MapExp {
		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public final SigExp a;
        public final SigExp b;
        public final SigExp c;

		@Override
		public int hashCode() {
			int prime = 31;
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

	public static class Const extends MapExp {
		public final List<Pair<String, List<String>>> arrows;
		public final List<Pair<String, String>> attrs;
		public final List<Pair<String, String>> objs;
		public final SigExp src;
		public final SigExp dst;

		public Const(List<Pair<String, String>> objs,
				List<Pair<String, String>> attrs,
				List<Pair<String, List<String>>> arrows, SigExp src, SigExp dst) {
			this.objs = objs;
			this.attrs = attrs;
			this.arrows = arrows;
			this.src = src;
			this.dst = dst;
			if (src == null || dst == null) {
				throw new RuntimeException(toString());
			}
			Set<String> seen1 = new HashSet<>();
			Set<String> seen2 = new HashSet<>();
			Set<String> seen3 = new HashSet<>();
			for (Pair<String, String> k : objs) {
				if (seen1.contains(k.first)) {
					throw new RuntimeException("Duplicate object mapping: " + k.first + " in " + this);
				}
				seen1.add(k.first);
				if (k.first == null || k.second == null) {
					throw new RuntimeException(toString());
				}
			}
			for (Pair<String, String> k : attrs) {
				if (seen2.contains(k.first)) {
					throw new RuntimeException("Duplicate attribute mapping: " + k.first+ " in " + this);
				}
				seen2.add(k.first);

				if (k.first == null || k.second == null) {
					throw new RuntimeException(toString());
				}
			}
			for (Pair<String, List<String>> k : arrows) {
				if (seen3.contains(k.first)) {
					throw new RuntimeException("Duplicate arrow mapping: " + k.first+ " in " + this);
				}
				seen3.add(k.first);

				if (k.first == null || k.second == null) {
					throw new RuntimeException(toString());
				} 
				for (String v : k.second) {
					if (v == null) {
						throw new RuntimeException(toString());
					}
				}
			}
 			
			//Collections.sort(this.objs);
			//Collections.sort(this.attrs);
			//Collections.sort(this.arrows);
		}


		@Override
		public String toString() {	
			String nm = "\n nodes\n";
			boolean b = false;
			for (Pair<String, String> k : objs) {
				if (b) {
					nm += ",\n";
				}
				b = true;
				nm += "  " + k.first + " -> " + k.second;
			}
			nm = nm.trim();
			nm += ";\n";

			nm += " attributes\n";
			b = false;
			for (Pair<String, String> k : attrs) {
				if (b) {
					nm += ",\n";
				}
				b = true;
				nm += "  " + k.first + " -> " + k.second;
			}
			nm = nm.trim();
			nm += ";\n";
			
			nm += " arrows\n";
			b = false;
			for (Pair<String, List<String>> k : arrows) {
				if (b) {
					nm += ",\n";
				}
				b = true;
				nm += "  " + k.first + " -> " + PrettyPrinter.sep0(".", k.second);
			}
			nm = nm.trim();
			nm += ";\n";

			return "{\n " + nm + "}"; // : " + src + " -> " + dst;
		}
		

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result
					+ ((arrows == null) ? 0 : arrows.hashCode());
			result = prime * result + ((attrs == null) ? 0 : attrs.hashCode());
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
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Var extends MapExp {
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
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class TT extends MapExp {
		final SigExp t;
		final Set<String> attrs;

		public TT(SigExp t, Set<String> attrs) {
			this.t = t;
			this.attrs = attrs;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((attrs == null) ? 0 : attrs.hashCode());
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
			TT other = (TT) obj;
			if (attrs == null) {
				if (other.attrs != null)
					return false;
			} else if (!attrs.equals(other.attrs))
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
			return "unit {" + PrettyPrinter.sep0(",", new LinkedList<>(attrs)) + "} " + t;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class FF extends MapExp {
		final SigExp t;

		public FF(SigExp t) {
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
			FF other = (FF) obj;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "void " + t;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Fst extends MapExp {
		final SigExp s;
        final SigExp t;

		public Fst(SigExp s, SigExp t) {
			this.s = s;
			this.t = t;
		}

		@Override
		public int hashCode() {
			int prime = 31;
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
			Fst other = (Fst) obj;
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
			return "fst " + s + " " + t;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Snd extends MapExp {
		final SigExp s;
        final SigExp t;

		public Snd(SigExp s, SigExp t) {
			this.s = s;
			this.t = t;
		}

		@Override
		public int hashCode() {
			int prime = 31;
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
			Snd other = (Snd) obj;
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
			return "snd " + s + " " + t;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Inl extends MapExp {
		final SigExp s;
        final SigExp t;

		public Inl(SigExp s, SigExp t) {
			this.s = s;
			this.t = t;
		}

		@Override
		public int hashCode() {
			int prime = 31;
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
			Inl other = (Inl) obj;
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
			return "inl " + s + " " + t;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Inr extends MapExp {
		final SigExp s;
        final SigExp t;

		public Inr(SigExp s, SigExp t) {
			this.s = s;
			this.t = t;
		}

		@Override
		public int hashCode() {
			int prime = 31;
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
			Inr other = (Inr) obj;
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
			return "inr " + s + " " + t;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Apply extends MapExp {
		final SigExp s;
        final SigExp t;

		public Apply(SigExp s, SigExp t) {
			this.s = s;
			this.t = t;
		}

		@Override
		public int hashCode() {
			int prime = 31;
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
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Curry extends MapExp {
		final MapExp f;

		public Curry(MapExp f) {
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
			Curry other = (Curry) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "curry " + f;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Prod extends MapExp {
		final MapExp l;
        final MapExp r;

		public Prod(MapExp l, MapExp r) {
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
			Prod other = (Prod) obj;
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
			return "(" + l + " * " + r + ")";
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Comp extends MapExp {
		final MapExp l;
        final MapExp r;

		public Comp(MapExp MapExp, MapExp r) {
            l = MapExp;
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
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	public static class Case extends MapExp {
		final MapExp l;
        final MapExp r;

		public Case(MapExp l, MapExp r) {
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
			Prod other = (Prod) obj;
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
			return "(" + l + " + " + r + ")";
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}
	
	public static class Opposite extends MapExp {
		final MapExp e;

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

		public Opposite(MapExp e) {
			this.e = e;
		}

		@Override
		public <R, E> R accept(E env, MapExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}

	@Override
	public abstract int hashCode();

	public interface MapExpVisitor<R, E> {
		R visit(E env, Id e);
		R visit(E env, Comp e);
		R visit(E env, Dist1 e);
		R visit(E env, Dist2 e);
		R visit(E env, Var e);
		R visit(E env, Const e);
		R visit(E env, TT e);
		R visit(E env, FF e);
		R visit(E env, Fst e);
		R visit(E env, Snd e);
		R visit(E env, Inl e);
		R visit(E env, Inr e);
		R visit(E env, Apply e);
		R visit(E env, Curry e);
		R visit(E env, Case e);
		R visit(E env, Prod e);
		R visit(E env, Sub e);
		R visit(E env, Opposite e);
		R visit(E env, Iso e);
	}

	public String printNicely(FQLProgram p) {
		return accept(p, new PrintNiceMapExpVisitor());
	}

}

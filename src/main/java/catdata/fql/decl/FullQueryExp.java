package catdata.fql.decl;

import java.util.Set;

import catdata.Pair;

public abstract class FullQueryExp {
	
	public FullQuery toFullQuery(FQLProgram env) {
		return accept(env, new ToFullQueryExp()).accept(env, new ToFullQueryVisitor());
	}

	public static class Delta extends FullQueryExp {

		@Override
		public String toString() {
			return "delta " + f;
		}

		public final MapExp f;

		public Delta(MapExp f) {
			this.f = f;
		}

		@Override
		public <R, E> R accept(E env, FullQueryExpVisitor<R, E> v) {
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
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
			return result;
		}

		@Override
		public String printNicely(FQLProgram p) {
			return "delta " + f.printNicely(p);
		}

	}

	public static class Sigma extends FullQueryExp {

		@Override
		public String toString() {
			return "SIGMA " + f;
		}
		
		@Override
		public String printNicely(FQLProgram p) {
			return "SIGMA " + f.printNicely(p);
		}

		public final MapExp f;

		public Sigma(MapExp f) {
			this.f = f;
		}

		@Override
		public <R, E> R accept(E env, FullQueryExpVisitor<R, E> v) {
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
			Sigma other = (Sigma) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
			return result;
		}

	}

	public static class Pi extends FullQueryExp {

		@Override
		public String toString() {
			return "pi " + f;
		}
		
		@Override
		public String printNicely(FQLProgram p) {
			return "pi " + f.printNicely(p);
		}

		public final MapExp f;

		@Override
		public <R, E> R accept(E env, FullQueryExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public Pi(MapExp f) {
			this.f = f;
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
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
			return result;
		}

	}

	public abstract <R, E> R accept(E env, FullQueryExpVisitor<R, E> v);

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

	public static class Var extends FullQueryExp {

		public String v;

		public Var(String v) {
			if (v.contains(" ")) {
				throw new RuntimeException(v);
			}
			this.v = v;
		}

		@Override
		public <R, E> R accept(E env, FullQueryExpVisitor<R, E> v) {
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
			Var other = (Var) obj;
			if (v == null) {
				if (other.v != null)
					return false;
			} else if (!v.equals(other.v))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((v == null) ? 0 : v.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return v;
		}

		@Override
		public String printNicely(FQLProgram p) {
			return v;
		}

	}

	public static class Match extends FullQueryExp {

		public final Set<Pair<String, String>> rel;
		public final SigExp src;
        public final SigExp dst;
		public final String kind;

		public Match(Set<Pair<String, String>> rel, SigExp src, SigExp dst,
				String kind) {
			this.rel = rel;
			this.src = src;
			this.dst = dst;
			this.kind = kind;
		}

		@Override
		public <R, E> R accept(E env, FullQueryExpVisitor<R, E> v) {
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
			Match other = (Match) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (kind == null) {
				if (other.kind != null)
					return false;
			} else if (!kind.equals(other.kind))
				return false;
			if (rel == null) {
				if (other.rel != null)
					return false;
			} else if (!rel.equals(other.rel))
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
			result = prime * result + ((kind == null) ? 0 : kind.hashCode());
			result = prime * result + ((rel == null) ? 0 : rel.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "match {" + print(rel) + "} " + src + " " + dst + " " + "\""
					+ kind + "\"";
		}

		private static String print(Set<Pair<String, String>> rel2) {
			String ret = "";
			boolean b = false;
			for (Pair<String, String> k : rel2) {
				if (b) {
					ret += ",";
				}
				b = true;
				ret += "(" + k.first + ", " + k.second + ")";
			}
			return ret;
		}

		@Override
		public String printNicely(FQLProgram p) {
			return toFullQueryExp(p).printNicely(p);
		}


	}

	public FullQueryExp toFullQueryExp(FQLProgram p) {
		return accept(p, new ToFullQueryExp());
	}

	public final Pair<SigExp, SigExp> type(FQLProgram env) {
		return accept(env, new FullQueryExpChecker());
	}

	public static class Comp extends FullQueryExp {
		final FullQueryExp l;
        final FullQueryExp r;

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

		public Comp(FullQueryExp l, FullQueryExp r) {
			this.l = l;
			this.r = r;
		}

		@Override
		public String toString() {
			return "(" + l + " then " + r + ")";
		}

		@Override
		public <R, E> R accept(E env, FullQueryExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String printNicely(FQLProgram p) {
			return "(" + l.printNicely(p) + "\nthen\n" + r.printNicely(p) + ")";
		}

	}

	public interface FullQueryExpVisitor<R, E> {
		// public R visit (E env, Const e);
        R visit(E env, Comp e);

		R visit(E env, Var e);

		R visit(E env, Match e);

		R visit(E env, Delta e);

		R visit(E env, Sigma e);

		R visit(E env, Pi e);
	}

	public abstract String printNicely(FQLProgram p);

	
	

}

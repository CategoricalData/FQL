package catdata.fql.decl;

import catdata.Pair;
import catdata.fql.decl.Query.ToQueryVisitor;



public abstract class QueryExp {
	
	public Query toQuery(FQLProgram env) {
		return accept(env, new ToQueryVisitor());
	}
	
	public static class Var extends QueryExp {

		public String v;
		
		public Var(String v) {
			if (v.contains(" ")) {
				throw new RuntimeException();
			}
			this.v = v;
		}

		@Override
		public <R, E> R accept(E env, QueryExpVisitor<R, E> v) {
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
		
	}
	
	public static class Comp extends QueryExp {
		final QueryExp l;
        final QueryExp r;

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

		public Comp(QueryExp l, QueryExp r) {
            this.l = l;
			this.r = r;
		}
		
		@Override
		public String toString() {
			return "(" + l + " then " + r + ")";
		}

		@Override
		public <R, E> R accept(E env, QueryExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

				
	}
	
	public interface QueryExpVisitor<R,E> {
		R visit(E env, Const e);
		R visit(E env, Comp e);
		R visit(E env, Var e);
	}
	
	public abstract <R, E> R accept(E env, QueryExpVisitor<R, E> v);
	
	@Override
	public abstract boolean equals(Object o);
	
	@Override
	public abstract int hashCode();
	
	
	
	public static class Const extends QueryExp {
		final MapExp delta;
        final MapExp sigma;
        final MapExp pi;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((delta == null) ? 0 : delta.hashCode());
			result = prime * result + ((pi == null) ? 0 : pi.hashCode());
			result = prime * result + ((sigma == null) ? 0 : sigma.hashCode());
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
			if (delta == null) {
				if (other.delta != null)
					return false;
			} else if (!delta.equals(other.delta))
				return false;
			if (pi == null) {
				if (other.pi != null)
					return false;
			} else if (!pi.equals(other.pi))
				return false;
			if (sigma == null) {
				if (other.sigma != null)
					return false;
			} else if (!sigma.equals(other.sigma))
				return false;
			return true;
		}

		public Const(MapExp delta, MapExp pi, MapExp sigma) {
            this.delta = delta;
			this.sigma = sigma;
			this.pi = pi;
		}
		
		@Override
		public String toString() {
			return "delta " + delta + " pi " + pi + " sigma " + sigma;
		}
		
		@Override
		public <R, E> R accept(E env, QueryExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

	}


	
	public final Pair<SigExp, SigExp> type(FQLProgram env) {
		return accept(env, new QueryChecker());
	}
	
	
}

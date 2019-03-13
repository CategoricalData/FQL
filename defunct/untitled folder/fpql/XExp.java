package catdata.fpql;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.ide.DefunctGlobalOptions;

public abstract class XExp {
	
	public String kind(Map<String, XExp> env) {
		return accept(env, new XKind());
	}
	
	public Pair<XExp, XExp> type(Map<String, XExp> env) {
		return accept(env, new XChecker());
	}
	
	
	public static class XSuperED extends XExp {

		final Map<String, List<String>> dom;
		final Map<String, String> cod;
		final List<SuperFOED> as;
		final String S;
		final String T;
		final String I;
		
		



		@Override
		public String toString() {
			return "XSuperED [dom=" + dom + ", cod=" + cod + ", as=" + as + ", S=" + S + ", T=" + T
					+ ", I=" + I + "]";
		}



		public XSuperED(Map<String, List<String>> dom, Map<String, String> cod, List<SuperFOED> as,
				String s, String t, String i) {
			this.dom = dom;
			this.cod = cod;
			this.as = as;
			S = s;
			T = t;
			I = i;
		}



		//f(x.p.q,y,z).a.b = 
		static class SuperFOED {
			final Map<String, String> a;
			final List<Pair< Triple<String,List<List<String>>,List<String>>  ,
				       Triple<String,List<List<String>>,List<String>>  >> lhs;
            final List<Pair< Triple<String,List<List<String>>,List<String>>  ,
                       Triple<String,List<List<String>>,List<String>>  >> rhs;
			public SuperFOED(
					Map<String, String> a,
					List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>> lhs,
					List<Pair<Triple<String, List<List<String>>, List<String>>, Triple<String, List<List<String>>, List<String>>>> rhs) {
				this.a = a;
				this.lhs = lhs;
				this.rhs = rhs;
			}
			@Override
			public int hashCode() {
				int prime = 31;
				int result = 1;
				result = prime * result + ((a == null) ? 0 : a.hashCode());
				result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
				result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
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
				SuperFOED other = (SuperFOED) obj;
				if (a == null) {
					if (other.a != null)
						return false;
				} else if (!a.equals(other.a))
					return false;
				if (lhs == null) {
					if (other.lhs != null)
						return false;
				} else if (!lhs.equals(other.lhs))
					return false;
				if (rhs == null) {
					if (other.rhs != null)
						return false;
				} else if (!rhs.equals(other.rhs))
					return false;
				return true;
			}
			
			
			
		}

		

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((S == null) ? 0 : S.hashCode());
			result = prime * result + ((T == null) ? 0 : T.hashCode());
			result = prime * result + ((as == null) ? 0 : as.hashCode());
			result = prime * result + ((cod == null) ? 0 : cod.hashCode());
			result = prime * result + ((dom == null) ? 0 : dom.hashCode());
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
			XSuperED other = (XSuperED) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (S == null) {
				if (other.S != null)
					return false;
			} else if (!S.equals(other.S))
				return false;
			if (T == null) {
				if (other.T != null)
					return false;
			} else if (!T.equals(other.T))
				return false;
			if (as == null) {
				if (other.as != null)
					return false;
			} else if (!as.equals(other.as))
				return false;
			if (cod == null) {
				if (other.cod != null)
					return false;
			} else if (!cod.equals(other.cod))
				return false;
			if (dom == null) {
				if (other.dom != null)
					return false;
			} else if (!dom.equals(other.dom))
				return false;
			return true;
		}



		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
	}
	
	public static class XSOED extends XExp {
				
		final List<Triple<String, String, String>> es;
		final List<FOED> as;
		
		final String src;
        final String dst;
        final String I;
		
		public XSOED(List<Triple<String, String, String>> es, List<FOED> as, String src,
				String dst, String i) {
			this.es = es;
			this.as = as;
			this.src = src;
			this.dst = dst;
            I = i;
		}

		static class FOED {
			final String a;
			final String t;
			final List<Pair<List<String>, List<String>>> eqs;
			
			public FOED(String a, String t, List<Pair<List<String>, List<String>>> eqs) {
				this.a = a;
				this.t = t;
				this.eqs = eqs;
			}

			@Override
			public String toString() {
				return "FOED [a=" + a + ", t=" + t + ", eqs=" + eqs + "]";
			}

			@Override
			public int hashCode() {
				int prime = 31;
				int result = 1;
				result = prime * result + ((a == null) ? 0 : a.hashCode());
				result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
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
				FOED other = (FOED) obj;
				if (a == null) {
					if (other.a != null)
						return false;
				} else if (!a.equals(other.a))
					return false;
				if (eqs == null) {
					if (other.eqs != null)
						return false;
				} else if (!eqs.equals(other.eqs))
					return false;
				if (t == null) {
					if (other.t != null)
						return false;
				} else if (!t.equals(other.t))
					return false;
				return true;
			}
			
		}
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((as == null) ? 0 : as.hashCode());
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((es == null) ? 0 : es.hashCode());
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
			XSOED other = (XSOED) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (as == null) {
				if (other.as != null)
					return false;
			} else if (!as.equals(other.as))
				return false;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (es == null) {
				if (other.es != null)
					return false;
			} else if (!es.equals(other.es))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "XSOED [es=" + es + ", as=" + as + ", src=" + src + ", dst=" + dst + ", I=" + I
					+ "]";
		}
		
		
	}
	
	public static class XPushout extends XExp {
		final XExp f;
        final XExp g;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
			result = prime * result + ((g == null) ? 0 : g.hashCode());
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
			XPushout other = (XPushout) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			if (g == null) {
				if (other.g != null)
					return false;
			} else if (!g.equals(other.g))
				return false;
			return true;
		}

		public XPushout(XExp f, XExp g) {
			this.f = f;
			this.g = g;
		}
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}
	
	public static class XCoApply extends XExp {
		final XExp f;
        final XExp I;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			XCoApply other = (XCoApply) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		public XCoApply(XExp f, XExp i) {
			this.f = f;
			I = i;
		}

		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {
			return "coapply " + f + " " + I;
		}
	}
	
	public static class XGrothLabels extends XExp {
		public final XExp F;

		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
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
			XGrothLabels other = (XGrothLabels) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			return true;
		}

		public XGrothLabels(XExp f) {
			F = f;
		}

		
	}
	
	public static class XIdPoly extends XExp {
		public final XExp F;

		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
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
			XIdPoly other = (XIdPoly) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			return true;
		}

		public XIdPoly(XExp f) {
			F = f;
		}
		
	}
	
	public static class XLabel extends XExp {
		public final XExp F;

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
			XLabel other = (XLabel) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			return true;
		}

		public XLabel(XExp f) {
			F = f;
		}
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}
	
	public static class XUberPi extends XExp {
		public final XExp F;

		public XUberPi(XExp f) {
			F = f;
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
			XUberPi other = (XUberPi) obj;
			if (F == null) {
				if (other.F != null)
					return false;
			} else if (!F.equals(other.F))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}
	
	
	public static class XToQuery extends XExp {
		
		public final XExp inst;
		public final XExp applyTo;
		
		public XToQuery(XExp inst, XExp applyTo) {
			this.inst = inst;
			this.applyTo = applyTo;
		}


		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}


		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((applyTo == null) ? 0 : applyTo.hashCode());
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
			XToQuery other = (XToQuery) obj;
			if (applyTo == null) {
				if (other.applyTo != null)
					return false;
			} else if (!applyTo.equals(other.applyTo))
				return false;
			if (inst == null) {
				if (other.inst != null)
					return false;
			} else if (!inst.equals(other.inst))
				return false;
			return true;
		}

	}
	
	
	public static class Id extends XExp {
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		final boolean isQuery;
		public final XExp C;
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((C == null) ? 0 : C.hashCode());
			result = prime * result + (isQuery ? 1231 : 1237);
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
			if (C == null) {
				if (other.C != null)
					return false;
			} else if (!C.equals(other.C))
				return false;
            return isQuery == other.isQuery;
        }
		public Id(boolean isQuery, XExp c) {
			this.isQuery = isQuery;
			C = c;
		}
		
	}
	
	public static class Compose extends XExp {
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		final XExp f;
        final XExp g;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
			result = prime * result + ((g == null) ? 0 : g.hashCode());
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
			Compose other = (Compose) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			if (g == null) {
				if (other.g != null)
					return false;
			} else if (!g.equals(other.g))
				return false;
			return true;
		}

		public Compose(XExp f, XExp g) {
			this.f = f;
			this.g = g;
		}
		
		
		@Override
		public String toString() {
			return "(" + f + " ; " + g + ")";
		}
	}
	
	public static class Apply extends XExp {
		final XExp f;
        final XExp I;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			Apply other = (Apply) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			return true;
		}

		public Apply(XExp f, XExp i) {
			this.f = f;
			I = i;
		}
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {
			return "coapply " + f + " " + I;
		}

	}
	
	public static class Iter extends XExp {
		final XExp f;
        final XExp initial;
		final int num;
		
		public Iter(XExp f, XExp initial, int num) {
			this.f = f;
			this.initial = initial;
			this.num = num;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((f == null) ? 0 : f.hashCode());
			result = prime * result + ((initial == null) ? 0 : initial.hashCode());
			result = prime * result + num;
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
			Iter other = (Iter) obj;
			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;
			if (initial == null) {
				if (other.initial != null)
					return false;
			} else if (!initial.equals(other.initial))
				return false;
            return num == other.num;
        }
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
	}
	
/*	public static class XQueryExp extends XExp {
		XExp pi, delta, sigma;
		 
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
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
			XQueryExp other = (XQueryExp) obj;
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

		public XQueryExp(XExp pi, XExp delta, XExp sigma) {
			super();
			this.pi = pi;
			this.delta = delta;
			this.sigma = sigma;
		}
	} */
	
	public static class XBool {
		
		@Override
		public String toString() {
			if (lhs != null && rhs != null) {
				return Util.sep(lhs, ".") + " = " + Util.sep(rhs, ".");
			}
			if (l != null && r != null) {
				String s = isAnd ? " and " : " or ";
				return "(" + l + s + r + ")";
			}			
			if (isTrue != null) {
				return "true";
			}
			if (isFalse != null) {
				return "false";
			}
			if (not != null) {
				return "not " + not;
			}
			throw new RuntimeException();
		}
		/*
		public void normalize() {
			if (l == null && r == null) {
				return;
			}
			
			l.normalize();
			r.normalize();
			
			//a + b --> a' + b'

			//OR
			if (!isAnd) {
				//true \/ p  p \/ true ---> true
				if (l.equals(new XBool(true)) || r.equals(new XBool(true))) {
					isTrue = new Unit();
					l = null;
					r = null;
				} else
				//false \/ p --> p
				if (l.equals(new XBool(false))) {
					become(r);
				} else
				//p \/ false --> p 
				if (r.equals(new XBool(false))) {
					become(l);
				}
				return;
			}
			
			//false /\ p  p /\ false ---> false
			if (l.equals(new XBool(false)) || r.equals(new XBool(false))) {
				isFalse = new Unit();
				l = null;
				r = null;
			} else
			//true /\ p --> p
			if (l.equals(new XBool(true))) {
				become(r);
			} else
			//p /\ true --> p 
			if (r.equals(new XBool(false))) {
				become(l);
			} else

			//(a+b)*c --> a*b + b*c 
			if (l.l != null && l.r != null) {
				XBool l2 = new XBool(l.l, r, true);
				XBool r2 = new XBool(l.r, r, true);				
				isAnd = false;
				l = l2;
				r = r2;
				l.normalize();
				r.normalize();
				return; 
			} else
			//a*(b+c) --> a*b + a*c
			if (r.l != null && r.r != null) {
				XBool l2 = new XBool(l, r.l, true);
				XBool r2 = new XBool(l, r.r, true);				
				isAnd = false;
				l = l2;
				r = r2;
				l.normalize();
				r.normalize();
				return;
			}
			
		}
		
		public void become(XBool x) {
			this.isAnd = x.isAnd;
			this.isFalse = x.isFalse;
			this.isTrue = x.isTrue;
			this.l = x.l;
			this.r = x.r;
			this.lhs = x.lhs;
			this.rhs = x.rhs;
		}
		*/
		public XBool(XBool not) {
			this.not = not;
		}
		public XBool(List<Object> lhs, List<Object> rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public XBool(XBool l, XBool r, boolean isAnd) {
			this.l = l;
			this.r = r;
			this.isAnd = isAnd;
		}
		
		public XBool(boolean b) {
			if (b) {
				isTrue = new Unit();
			} else {
				isFalse = new Unit();
			}
		}
		
		public List<Object> lhs, rhs;
		public XBool l,r;
		public boolean isAnd;
		public XBool not;
		
		public Object isTrue, isFalse;
		
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + (isAnd ? 1231 : 1237);
			result = prime * result + ((isFalse == null) ? 0 : isFalse.hashCode());
			result = prime * result + ((isTrue == null) ? 0 : isTrue.hashCode());
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
			result = prime * result + ((not == null) ? 0 : not.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
			result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
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
			XBool other = (XBool) obj;
			if (isAnd != other.isAnd)
				return false;
			if (isFalse == null) {
				if (other.isFalse != null)
					return false;
			} else if (!isFalse.equals(other.isFalse))
				return false;
			if (isTrue == null) {
				if (other.isTrue != null)
					return false;
			} else if (!isTrue.equals(other.isTrue))
				return false;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (lhs == null) {
				if (other.lhs != null)
					return false;
			} else if (!lhs.equals(other.lhs))
				return false;
			if (not == null) {
				if (other.not != null)
					return false;
			} else if (!not.equals(other.not))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			if (rhs == null) {
				if (other.rhs != null)
					return false;
			} else if (!rhs.equals(other.rhs))
				return false;
			return true;
		}
	}
	
	public static class FLOWER2 extends XExp {
		final Map<Object, List<Object>> select;
		Map<Object, Object> from;
		final XBool where;
		final XExp src;
		String ty;
		
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((select == null) ? 0 : select.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			result = prime * result + ((ty == null) ? 0 : ty.hashCode());
			result = prime * result + ((where == null) ? 0 : where.hashCode());
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
			FLOWER2 other = (FLOWER2) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (select == null) {
				if (other.select != null)
					return false;
			} else if (!select.equals(other.select))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			if (ty == null) {
				if (other.ty != null)
					return false;
			} else if (!ty.equals(other.ty))
				return false;
			if (where == null) {
				if (other.where != null)
					return false;
			} else if (!where.equals(other.where))
				return false;
			return true;
		}

		@SuppressWarnings("unchecked")
		public FLOWER2(Map<Object, List<Object>> select, Map<Object, Object> from,
				XBool where, XExp src) {
			this.select = select;
			this.from = from;
			this.where = where;
			this.src = src;
            this.from = DefunctGlobalOptions.debug.fpql.reorder_joins ? sort(from) : from;
		}
		
		private void count(XBool b, Map<String, Integer> counts) {
			if (b.isTrue != null || b.isFalse != null) {
				return;
			}
			if (b.not != null) {
				count(b.not, counts);
				return;
			}
			if (b.l != null && b.r != null) {
				count(b.l, counts);
				count(b.r, counts);
				return;
			}
			countX(b.lhs, counts);
			countX(b.rhs, counts);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static void countX(List l, Map counts) {
			for (Object s : l) {
				Integer i = (Integer) counts.get(s);
				if (i == null) {
					continue;
				}
				counts.put(s, i+1);
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Map sort(Map m) {
			Map count = new HashMap<>();
			for (Object s : m.keySet()) {
				count.put(s, 0);
			}
			count(where, count);
			List l = new LinkedList<>(m.keySet());
			l.sort((Object o1, Object o2) -> ((Integer)count.get(o2)) - ((Integer)count.get(o1)));
			Map ret = new LinkedHashMap<>();
			for (Object s : l) {
				ret.put(s, m.get(s));
			}
			return ret;
		}

		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
	}
	
	
	public static class Flower extends XExp {
		final Map<Object, List<Object>> select;
		final Map<Object, Object> from;
		final List<Pair<List<Object>, List<Object>>> where;
		final XExp src;
		String ty;
		 
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((select == null) ? 0 : select.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			result = prime * result + ((ty == null) ? 0 : ty.hashCode());
			result = prime * result + ((where == null) ? 0 : where.hashCode());
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
			Flower other = (Flower) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (select == null) {
				if (other.select != null)
					return false;
			} else if (!select.equals(other.select))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			if (ty == null) {
				if (other.ty != null)
					return false;
			} else if (!ty.equals(other.ty))
				return false;
			if (where == null) {
				if (other.where != null)
					return false;
			} else if (!where.equals(other.where))
				return false;
			return true;
		}

		@SuppressWarnings("unchecked")
		public Flower(Map<Object, List<Object>> select, Map<Object, Object> from,
				List<Pair<List<Object>, List<Object>>> where, XExp src) {
			this.select = select;
			this.where = where;
			this.src = src;
            this.from = DefunctGlobalOptions.debug.fpql.reorder_joins ? sort(from) : from;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static void count(List<Object> l, Map counts) {
			for (Object s : l) {
				Integer i = (Integer) counts.get(s);
				if (i == null) {
					continue;
				}
				counts.put(s, i+1);
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Map sort(Map<Object, Object> m) {
			Map count = new HashMap<>();
			for (Object s : m.keySet()) {
				count.put(s, 0);
			}
			for (Pair<List<Object>, List<Object>> k : where) {
				count(k.first, count);
				count(k.first, count);
			}
			List l = new LinkedList(m.keySet());
			l.sort((Object o1, Object o2) -> ((Integer)count.get(o2)) - ((Integer)count.get(o1)));
			Map ret = new LinkedHashMap<>();
			for (Object s : l) {
				ret.put(s, m.get(s));
			}
			return ret;
		}

		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return "Flower [select=" + select + ", from=" + from + ", where=" + where + ", src="
					+ src + ", ty=" + ty + "]";
		}
		
	}
	
	
	public static class XTimes extends XExp {
		final XExp l;
        final XExp r;
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		public XTimes(XExp l, XExp r) {
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
			XTimes other = (XTimes) obj;
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
	}
	
	public static class XProj extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		final XExp l;
        final XExp r;
		final boolean left;
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + (left ? 1231 : 1237);
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
			XProj other = (XProj) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (left != other.left)
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}
		public XProj(XExp l, XExp r, boolean left) {
			this.l = l;
			this.r = r;
			this.left = left;
		}
	}
	
	public static class XPair extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		final XExp l;
        final XExp r;

		public XPair(XExp l, XExp r) {
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
			XPair other = (XPair) obj;
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
			return "pair " + l + " " + r;
		}

	}
	
	public static class XTT extends XExp {
		final XExp S;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((S == null) ? 0 : S.hashCode());
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
			XTT other = (XTT) obj;
			if (S == null) {
				if (other.S != null)
					return false;
			} else if (!S.equals(other.S))
				return false;
			return true;
		}

		public XTT(XExp s) {
			S = s;
		}
	
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return "tt " + S;
		}

	}
	
	public static class XOne extends XExp {
		final XExp S;

		public XOne(XExp s) {
			S = s;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((S == null) ? 0 : S.hashCode());
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
			XOne other = (XOne) obj;
			if (S == null) {
				if (other.S != null)
					return false;
			} else if (!S.equals(other.S))
				return false;
			return true;
		}
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		@Override
		public String toString() {
			return "unit " + S;
		}

	}


	public static class XFF extends XExp {
		final XExp S;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((S == null) ? 0 : S.hashCode());
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
			XFF other = (XFF) obj;
			if (S == null) {
				if (other.S != null)
					return false;
			} else if (!S.equals(other.S))
				return false;
			return true;
		}

		public XFF(XExp s) {
			S = s;
		}
	
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return "ff " + S;
		}

	}
	
	public static class XVoid extends XExp {
		final XExp S;

		public XVoid(XExp s) {
			S = s;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((S == null) ? 0 : S.hashCode());
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
			XVoid other = (XVoid) obj;
			if (S == null) {
				if (other.S != null)
					return false;
			} else if (!S.equals(other.S))
				return false;
			return true;
		}
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public String toString() {
			return "void " + S;
		}

	}
	
	public static class XCoprod extends XExp {
		final XExp l;
        final XExp r;
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		public XCoprod(XExp l, XExp r) {
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
			XCoprod other = (XCoprod) obj;
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
	}
	
	public static class XInj extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		final XExp l;
        final XExp r;
		final boolean left;
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + (left ? 1231 : 1237);
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
			XInj other = (XInj) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (left != other.left)
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}
		public XInj(XExp l, XExp r, boolean left) {
			this.l = l;
			this.r = r;
			this.left = left;
		}
		
		@Override
		public String toString() {
			String tag = left ? "inl " : "inr ";
			return tag + l + " " + r;		
		}

	}
	
	public static class XMatch extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		final XExp l;
        final XExp r;

		public XMatch(XExp l, XExp r) {
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
			XMatch other = (XMatch) obj;
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
			return "case " + l + " " + r;
		}

	}
	
	
	public static class XRel extends XExp {
		final XExp I;
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		public XRel(XExp i) {
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
			XRel other = (XRel) obj;
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

	}
	
	public static class XUnit extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		final String kind;
		final XExp F;
        final XExp I;
		public XUnit(String kind, XExp f, XExp i) {
			this.kind = kind;
			F = f;
			I = i;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((kind == null) ? 0 : kind.hashCode());
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
			XUnit other = (XUnit) obj;
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
			if (kind == null) {
				if (other.kind != null)
					return false;
			} else if (!kind.equals(other.kind))
				return false;
			return true;
		}
		
		@Override public String toString() {
			if (kind.equals("sigma")) {
				return "return sigma delta " + F + " " + I;
			} else if (kind.equals("pi")) {
				return "return delta pi " + F + " " + I;
			}
			throw new RuntimeException();
		}
	}
	
	public static class XCounit extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		final String kind;
		final XExp F;
        final XExp I;
		public XCounit(String kind, XExp f, XExp i) {
			this.kind = kind;
			F = f;
			I = i;
		}
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((F == null) ? 0 : F.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((kind == null) ? 0 : kind.hashCode());
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
			XCounit other = (XCounit) obj;
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
			if (kind == null) {
				if (other.kind != null)
					return false;
			} else if (!kind.equals(other.kind))
				return false;
			return true;
		}		
		
		@Override public String toString() {
			if (kind.equals("sigma")) {
				return "coreturn sigma delta " + F + " " + I;
			} else if (kind.equals("pi")) {
				return "coreturn delta pi " + F + " " + I;
			}
			throw new RuntimeException();
		}
		
	}
	
	
	@Override
	public abstract boolean equals(Object o);

	public abstract <R, E> R accept(E env, XExpVisitor<R, E> v);

	@Override
	public abstract int hashCode();
	
	
	
	public static class XTy extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((javaName == null) ? 0 : javaName.hashCode());
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
			XTy other = (XTy) obj;
			if (javaName == null) {
				if (other.javaName != null)
					return false;
			} else if (!javaName.equals(other.javaName))
				return false;
			return true;
		}

		public XTy(String javaName) {
			this.javaName = javaName;
		}

		final String javaName;
	}
	
	public static class XFn extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((javaFn == null) ? 0 : javaFn.hashCode());
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
			XFn other = (XFn) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (javaFn == null) {
				if (other.javaFn != null)
					return false;
			} else if (!javaFn.equals(other.javaFn))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		public XFn(String src, String dst, String javaFn) {
			this.src = src;
			this.dst = dst;
			this.javaFn = javaFn;
		}

		final String src;
        final String dst;
        final String javaFn;
	}
	
	public static class XConst extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		final String dst;
        final String javaFn;

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((javaFn == null) ? 0 : javaFn.hashCode());
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
			XConst other = (XConst) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (javaFn == null) {
				if (other.javaFn != null)
					return false;
			} else if (!javaFn.equals(other.javaFn))
				return false;
			return true;
		}

		public XConst(String dst, String javaFn) {
			this.dst = dst;
			this.javaFn = javaFn;
		}
	}
	
	public static class XEq extends XExp {
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
			result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
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
			XEq other = (XEq) obj;
			if (lhs == null) {
				if (other.lhs != null)
					return false;
			} else if (!lhs.equals(other.lhs))
				return false;
			if (rhs == null) {
				if (other.rhs != null)
					return false;
			} else if (!rhs.equals(other.rhs))
				return false;
			return true;
		}

		public XEq(List<String> lhs, List<String> rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}

		final List<String> lhs;
        final List<String> rhs;
	}
	
	public static class XInst extends XExp {
		public boolean saturated = false;
		public final XExp schema;
		public XInst(XExp schema, List<Pair<String, String>> nodes,
				List<Pair<List<String>, List<String>>> eqs) {
			this.schema = schema;
			this.nodes = nodes;
			this.eqs = eqs;
		}


		public final List<Pair<String, String>> nodes;
		public final List<Pair<List<String>, List<String>>> eqs;
	
		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
			result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
			result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
			XInst other = (XInst) obj;
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
			if (schema == null) {
				if (other.schema != null)
					return false;
			} else if (!schema.equals(other.schema))
				return false;
			return true;
		}

		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {
			String x = "\n variables\n";
			//boolean b = false;
			
			List<String> nodes2 = nodes.stream().map(z -> "  " + z.first + ": " + z.second).collect(Collectors.toList());
			x += Util.sep(nodes2, ",\n");
			x = x.trim();
			x += ";\n";
			x += " equations\n";
			List<String> eqs2 = eqs.stream().map(z -> "  " + Util.sep(z.first, ".") + " = " + Util.sep(z.second, ".")).collect(Collectors.toList());
			x += Util.sep(eqs2, ",\n");
			x = x.trim();
			return "instance {\n " + x + ";\n}";
		}
	}
	
	public static class XSchema extends XExp {
		public final List<String> nodes;
		public final List<Triple<String, String, String>> arrows;
		public final List<Pair<List<String>, List<String>>> eqs;

		public XSchema(
				List<String> nodes,
				List<Triple<String, String, String>> arrows,
				List<Pair<List<String>, List<String>>> eqs) {
			this.nodes = nodes;
			this.arrows = arrows;
			this.eqs = eqs;
		}

		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
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
			XSchema other = (XSchema) obj;
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
			x += " edges\n";

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
			return "schema {\n " + x + ";\n}";
		}
		
		private static String printOneEq(List<String> l) {
			return Util.sep(l, ".");
		}
		
	}
	
	public static class Var extends XExp {
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
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
	}

	public static class XTransConst extends XExp {
		final XExp src;
        final XExp dst;
		public final List<Pair<Pair<String, String>, List<String>>> vm;
		

		public XTransConst(XExp src, XExp dst, List<Pair<Pair<String, String>, List<String>>> vm) {
			this.src = src;
			this.dst = dst;
			this.vm = vm;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			result = prime * result + ((vm == null) ? 0 : vm.hashCode());
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
			XTransConst other = (XTransConst) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			if (vm == null) {
				if (other.vm != null)
					return false;
			} else if (!vm.equals(other.vm))
				return false;
			return true;
		}



		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {	
			String nm0 = "\n variables\n";
			boolean b = false;
			for (Pair<Pair<String, String>, List<String>> k : vm) {
				if (b) {
					nm0 += ",\n";
				}
				b = true;
                nm0 += k.first.second != null ? "  " + k.first.first + ":" + k.first.second + " -> " + Util.sep(k.second, ".") : "  " + k.first.first + " -> " + Util.sep(k.second, ".");
			}
			nm0 = nm0.trim();
			nm0 += ";\n";

			return "homomorphism {\n " + nm0 + "}"; // : " + src + " -> " + dst;
		}

	
	}
	
	public static class XMapConst extends XExp {
		
		public final XExp src;
        public final XExp dst;
		
		public final List<Pair<String, String>> nm;
		public final List<Pair<String, List<String>>> em;
		

		public XMapConst(XExp src, XExp dst, List<Pair<String, String>> nm,
				List<Pair<String, List<String>>> em) {
			this.src = src;
			this.dst = dst;
			this.nm = nm;
			this.em = em;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			XMapConst other = (XMapConst) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (em == null) {
				if (other.em != null)
					return false;
			} else if (!em.equals(other.em))
				return false;
			if (nm == null) {
				if (other.nm != null)
					return false;
			} else if (!nm.equals(other.nm))
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
			result = prime * result + ((em == null) ? 0 : em.hashCode());
			result = prime * result + ((nm == null) ? 0 : nm.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			return result;
		}
		
		@Override
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public String toString() {	
			String nm0 = "\n nodes\n";
			boolean b = false;
			for (Pair<String, String> k : nm) {
				if (b) {
					nm0 += ",\n";
				}
				b = true;
				nm0 += "  " + k.first + " -> " + k.second;
			}
			nm0 = nm0.trim();
			nm0 += ";\n";

			nm0 += " edges\n";
			b = false;
			for (Pair<String, List<String>> k : em) {
				if (b) {
					nm0 += ",\n";
				}
				b = true;
				nm0 += "  " + k.first + " -> " + Util.sep(k.second, ".");
			}
			nm0 = nm0.trim();
			nm0 += ";\n";

			return "mapping {\n " + nm0 + "}"; // : " + src + " -> " + dst;
		}

	}
	
	
	
	public static class XSigma extends XExp {
		final XExp F;
        final XExp I;
		
		public XSigma(XExp f, XExp i) {
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
			XSigma other = (XSigma) obj;
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
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override public String toString() {
			return "sigma " + F + " " + I;
		}

		
	}
	
	public static class XPi extends XExp {
		final XExp F;
        final XExp I;
		

		public XPi(XExp f, XExp i) {
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
			XPi other = (XPi) obj;
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
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override public String toString() {
			return "pi " + F + " " + I;
		}

	}

	
	public static class XDelta extends XExp {
		final XExp F;
        final XExp I;
		
		public XDelta(XExp f, XExp i) {
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
			XDelta other = (XDelta) obj;
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
		public <R, E> R accept(E env, XExpVisitor<R, E> v) {
			return v.visit(env, this);
		}
		
		@Override public String toString() {
			return "delta " + F + " " + I;
		}
	}
	
	public interface XExpVisitor<R, E> {
		R visit(E env, XSchema e);
		R visit(E env, XMapConst e);
		R visit(E env, XTransConst e);
		R visit(E env, XSigma e);
		R visit(E env, XDelta e);
		R visit(E env, XInst e);
		R visit(E env, Var e);
		R visit(E env, XTy e);
		R visit(E env, XFn e);
		R visit(E env, XConst e);
		R visit(E env, XEq e);
		R visit(E env, XUnit e);
		R visit(E env, XCounit e);
		R visit(E env, XPi e);
		R visit(E env, XRel e);
		R visit(E env, XCoprod e);
		R visit(E env, XInj e);
		R visit(E env, XMatch e);
		R visit(E env, XVoid e);
		R visit(E env, XFF e);
		R visit(E env, XTimes e);
		R visit(E env, XProj e);
		R visit(E env, XPair e);
		R visit(E env, XOne e);
		R visit(E env, XTT e);
		R visit(E env, Flower e);
		R visit(E env, FLOWER2 e);
//		public R visit (E env, XQueryExp e);
R visit(E env, Apply e);
		R visit(E env, Iter e);
		R visit(E env, Id e);
		R visit(E env, Compose e);
		R visit(E env, @SuppressWarnings("rawtypes") XPoly e);
		R visit(E env, XToQuery e);
		R visit(E env, XUberPi e);
		R visit(E env, XLabel e);
		R visit(E env, XIdPoly e);
		R visit(E env, XGrothLabels e);
		R visit(E env, XCoApply e);
		R visit(E env, XPushout e);
		R visit(E env, XSOED e);
		R visit(E env, XSuperED e);
	}

}

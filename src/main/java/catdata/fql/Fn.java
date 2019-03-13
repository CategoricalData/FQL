package catdata.fql;

@FunctionalInterface
public interface Fn<X,Y> {

	Y of(X x) ;
	
}

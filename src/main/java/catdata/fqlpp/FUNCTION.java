package catdata.fqlpp;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface FUNCTION<X,Y> extends Function<X,Y>, Serializable {

	default <Z> FUNCTION<X,Z> andThen(FUNCTION<Y, Z> f) {
		return x -> f.apply(apply(x));
	}
	
}

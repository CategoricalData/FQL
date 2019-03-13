package catdata.fqlpp.cat;

public interface Subobjects<O,A> {

	O prop();
	
	A tru();
	
	A fals();
	
	A chr(A a);
	
	A kernel(A a);
	
}

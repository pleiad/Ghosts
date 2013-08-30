package cl.pleiad.ghosts.tests;

public class OperatorsTest {
	static public void main(String[] args) {
		String c = "hola";
		String d = "ho";
		char e = 'e';
		char f = 'f';
		String g = c + d; //String supports +
		char h = e++; //Postfix work on chars
		char i = ++e; //Unary works on chars
		int j = e + f; //char works as int for operations
		int k = e - f; //char works as int for operations
	}
}

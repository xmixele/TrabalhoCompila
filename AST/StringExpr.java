package AST;

public class StringExpr extends ExprLiteral {

	public StringExpr( String value ) {
		this.value = value;
	}

	public void genC( PW pw ) {
		pw.print("\"" + value + "\"");
	}

	public String getValue() {
		return value;
	}

	public Type getType() {
		return Type.stringType;
	}

	private String value;
}
package Lexer;

public enum Symbol {
	EOF("eof"),
	IDENT("Ident"),
	NUMBER("Number"),
	PLUS("+"),
	MINUS("-"),
	MULT("*"),
	DIV("/"),
	LT("<"),
	LE("<="),
	GT(">"),
	GE(">="),
	NEQ("!="),
	EQ("=="),
	ASSIGN("="),
	LEFTPAR("("),
	RIGHTPAR(")"),
	SEMICOLON(";"),
	VAR("var"),
	BEGIN("begin"),
	END("end"),
	IF("if"),
	THEN("then"),
	ELSE("else"),
	ENDIF("endif"),
	COMMA(","),
	READ("read"),
	WRITE("write"),
	COLON(":"),
	INTEGER("integer"),
	BOOLEAN("boolean"),
	CHAR("char"),
	CHARACTER("character"),
	TRUE("true"),
	FALSE("false"),
	OR("||"),
	AND ("&&"),
	REMAINDER("%"),
	NOT("!"),
	FUNCTION("function");

	Symbol(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	private String name;
}

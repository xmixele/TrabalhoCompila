/* ==========================================================================
 * Universidade Federal de São Carlos - Campus Sorocaba
 * Disciplina: Compiladores
 * Prof. Leticia Berto
 *
 * Trabalho - Análise Léxica e Sintática (Fase 1)
 *
 * Aluno: Bruno Rizzi       RA: 743515
 * Aluna: Giulia Fazzi      RA: 743542
 * Aluno: Leonardo Nozaki   RA: 743561
 * Aluna: Michele Carvalho  RA: 726573
 * ========================================================================== */

import AST.*;
import java.util.*;
import java.lang.Character;
import Error.*;
import Lexer.*;
import java.io.*;
import AuxComp.SymbolTable;

public class Compiler {
  // compile must receive an input with an character less than
  // p_input.lenght
  public Program compile( char []input, PrintWriter outError, String nomeArq ) {
    symbolTable = new SymbolTable();
    error = new CompilerError( outError, nomeArq);
    lexer = new Lexer(input, error);
    error.setLexer(lexer);
    lexer.nextToken();
    return program();
  }

  private Program program() {
    /* Program ::= Func {Func} */

    ArrayList<Function> funcList = new ArrayList<Function>();
    funcList.add(func());

    while (lexer.token == Symbol.FUNCTION) {
      funcList.add(func());
    }

    //Se nao houver mais funcoes entao deve ser o fim do arquivo (EOF)
    Program program = new Program(funcList);
    if ( lexer.token != Symbol.EOF ) {
      error.signal("EOF expected, anything is wrong");
    }

    //Analise semantica, verificar se existe uma funcao chamada main no codigo
    if(symbolTable.getInGlobal("main") == null){
      error.signal("Source code must have a Function called main");
    }
    return program;
  }

  private Function func() {
    /* Func ::= "function" Id [ "(" ParamList ")" ] ["->" Type ] StatList */

    ArrayList<VarDecStat> arrayParamList = null;
    Type type = null;
    ArrayList<Statement> arrayStatList = null;
    String id = "";
    Function s = null;
    int flag = 0;

    if (lexer.token != Symbol.FUNCTION) {
      if(lexer.token == Symbol.LITERALINT){
        error.signal("'function' expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("'function' expected before " + lexer.getStringValue());
      }
      else{
        error.signal("'function' expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }


    if (lexer.token != Symbol.IDENT) {
      if(lexer.token == Symbol.LITERALINT){
        error.signal("Function Id expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("Function Id expected before " + lexer.getStringValue());
      }
      else{
        error.signal("Function Id expected before " + lexer.token);
      }
    }
    else{
      //Analise semantica, funcao nao deve ter sido declarada anteriormente
      id = (String) lexer.getStringValue();
      s = (Function) symbolTable.getInGlobal(id);
      if(s != null){
        error.signal("Function " + id + " has already been declared");
        flag = 1; //Flag de controle, valor 1 indica que funcao ja existe com esse id
      }
      lexer.nextToken();
      //Analise sematica, main nao deve ter parametros nem retorno
      if ( id.compareTo("main") == 0 && lexer.token != Symbol.LEFTBRACE){
        error.signal("function main must be a parameterless and returnless function");
      }
    }

    if (lexer.token == Symbol.LEFTBRACE){
      //Se o token atual é {, entao nao tem parametros nem retorno
      arrayStatList = statList();
      for (Statement x: arrayStatList){
        if (x instanceof ReturnStat) {
          error.signal("function " + id + " must have no return");
        }
      }

      s = new Function( id, arrayParamList, type, arrayStatList );
      if(flag == 0){ //Se a funcao nao existe, coloque na hash
        symbolTable.putInGlobal( id, s );
      }
      symbolTable.removeLocalIdent();
      return s;
    }
    else if (lexer.token == Symbol.LEFTPAR) {
      //Verifica os parametros
      lexer.nextToken();
      arrayParamList = paramList();

      if(lexer.token == Symbol.IDENT){
        error.signal(", expected before " + lexer.getStringValue());
      }
      else if (lexer.token != Symbol.RIGHTPAR){
        if(lexer.token == Symbol.LITERALINT){
          error.signal(") expected before " + lexer.getNumberValue());
        }
        else if(lexer.token == Symbol.LITERALSTRING){
          error.signal(") expected before " + lexer.getStringValue());
        }
        else{
          error.signal(") expected before " + lexer.token);
        }
      }
      else{
        lexer.nextToken();
      }
    }

    if (lexer.token == Symbol.LEFTBRACE){ // void
      //Se o token atual é {, entao nao tem retorno
      arrayStatList = statList();
      for(Statement x: arrayStatList){
        if(x instanceof ReturnStat){
          error.signal("function " + id + " must have no return");
        }
      }

      s =  new Function( id, arrayParamList, type, arrayStatList );
      if(flag == 0){ //Se a funcao nao existe, coloque na hash
        symbolTable.putInGlobal( id, s );
      }
      symbolTable.removeLocalIdent();
      return s;
    }
    else if (lexer.token == Symbol.ARROW) {
      //verifica o retorno
      lexer.nextToken();
      type = type();
    }

    if (lexer.token == Symbol.LEFTBRACE){
      //Se o token atual é {, entao na possui erros
      boolean hasReturn = false;
      arrayStatList = statList();
      for(Statement x: arrayStatList){
        if(x instanceof ReturnStat){
          hasReturn = true;
          String typeReturn = ( (ReturnStat) x).getExpr().getTypeStringValue();
          if(typeReturn != null && type.equals(typeReturn)){
            error.signal("function " + id + " has different return type");
          }
        }
      }

      if (!hasReturn){
        error.signal("function " + id + " must have return");
      }

      s = new Function( id, arrayParamList, type, arrayStatList );
      if(flag == 0){ //Se a funcao nao existe, coloque na hash
        symbolTable.putInGlobal( id, s );
      }
      symbolTable.removeLocalIdent();
      return s;
    }
    else{
      //Esta faltando algo no cabecalho da funcao
      if(lexer.token == Symbol.LITERALINT){
        error.signal("( or -> expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("( or -> expected before " + lexer.getStringValue());
      }
      else{
        error.signal("( or ->  expected before " + lexer.token);
      }
    }

    // don't need nextToken(), has in end of type()

    arrayStatList = statList();
    boolean hasReturn = false;
    for(Statement x: arrayStatList){
      if(x instanceof ReturnStat){
        hasReturn = true;
        String typeReturn = ( (ReturnStat) x).getExpr().getTypeStringValue();
        if(typeReturn != null && type.equals(typeReturn)){
            error.signal("function " + id + " has different return type");
        }
      }
    }

    if (!hasReturn){
      error.signal("function " + id + " must have return");
    }

    s = new Function( id, arrayParamList, type, arrayStatList );
    if(flag == 0){ //Se a funcao nao existe, coloque na hash
      symbolTable.putInGlobal( id, s );
    }
    symbolTable.removeLocalIdent();
    return s;
  }

  private ArrayList<VarDecStat> paramList() {
    /* ParamList ::= ParamDec {"," ParamDec} */

    symbolTable.add();//Acrescente um nivel na pilha de hash das variaveis locais

    ArrayList<VarDecStat> paramList = new ArrayList<VarDecStat>();
    paramDec(paramList);

    while (lexer.token == Symbol.COMMA) {
      lexer.nextToken();
      paramDec(paramList);
    }
    return paramList;
  }

  private void paramDec(ArrayList<VarDecStat> paramList) {
    /* ParamDec ::= Id ":" Type */
    String id = "";
    int flag = 0;

    if (lexer.token != Symbol.IDENT) {
      if(lexer.token == Symbol.LITERALINT){
        error.signal("Id expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("Id expected before " + lexer.getStringValue());
      }
      else{
        error.signal("Id expected before " + lexer.token);
      }
      flag = 1; //variavel sem id
    }
    else {
      id = lexer.getStringValue();
      lexer.nextToken();
    }

    if (lexer.token != Symbol.COLON) {
      if(lexer.token == Symbol.LITERALINT){
        error.signal(": expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal(": expected before " + lexer.getStringValue());
      }
      else{
        error.signal(": expected before " + lexer.token);
      }
    }
    else {
      lexer.nextToken();
    }

    // get the type of the variable
    Type typeVar = type();  // end of type has nextToken()

    VarDecStat v = new VarDecStat(id, typeVar, true);

    paramList.add(v);  // inserts in the parameter list the variable object

    //Analise semantica
    //Verifica se a variavel tem id e tipo valido
    if(flag == 0 && typeVar != null){
      //Verifica se a variavel ja foi declarada ou nao
      if(symbolTable.getInLocal(id) != null){
        error.signal("var " + id + " has already been declared");
      }
      else{
        symbolTable.putInLocal(id, v);
      }
    }
  }

  private Type type() {
    /* Type ::= "Int" | "Boolean" | "String" */

    Type result;

    switch ( lexer.token ) {
      case INT :
        result = Type.integerType;
        break;
      case BOOLEAN :
        result = Type.booleanType;
        break;
      case STRING :
        result = Type.stringType;
        break;
      default :  // only supports the above types
        error.signal("Type expected: Int, Boolean or String");
        result = null;
    }
    lexer.nextToken();
    return result;
  }

  private ArrayList<Statement> statList() {
    /* StatList ::= "{” {Stat} ”}" */
    /* Stat ::= AssignExprStat | ReturnStat | VarDecStat | IfStat | WhileStat */

    ArrayList<Statement> stmt = new ArrayList<Statement>(); // Statement is abstract class

    if (lexer.token != Symbol.LEFTBRACE){
      if(lexer.token == Symbol.LITERALINT){
        error.signal("{ expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("{ expected before " + lexer.getStringValue());
      }
      else{
        error.signal("{ expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }
    symbolTable.add(); //Acrescente um nivel na pilha de hash das variaveis locais

    while (lexer.token == Symbol.IDENT || lexer.token == Symbol.LITERALINT || lexer.token == Symbol.TRUE ||
            lexer.token == Symbol.FALSE || lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.RETURN ||
            lexer.token == Symbol.VAR || lexer.token == Symbol.IF || lexer.token == Symbol.WHILE ||
	    lexer.token == Symbol.WRITE || lexer.token == Symbol.WRITELN) {
      stmt.add(stat());
    }
    symbolTable.sub(); //Remove um nivel na pilha de hash das variaveis locais

    if (lexer.token != Symbol.RIGHTBRACE){
      if(lexer.token == Symbol.LITERALINT){
        error.signal("} expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("} expected before " + lexer.getStringValue());
      }
      else{
        error.signal("} expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }

    return stmt;
  }

  private Statement stat() {
    /* Stat ::= AssignExprStat | ReturnStat | VarDecStat | IfStat | WhileStat */

    switch (lexer.token) {
      case IDENT:      // all are terminals of the assign
      case LITERALINT:
      case TRUE:
      case FALSE:
      case LITERALSTRING:
      case WRITE:
      case WRITELN:
        return assignExprStat();
      case RETURN:
        return returnStat();
      case VAR:
        return varDecStat();
      case IF:
        return ifStat();
      case WHILE:
        return whileStat();
      default :
        error.signal("Statement expected");
    }

    return null;
  }

  private AssignExprStat assignExprStat() {
    /* AssignExprStat ::= Expr [ "=" Expr ] ";" */
    Expr left = expr();
    Expr right = null;

    if (lexer.token == Symbol.ASSIGN) {
      if(left.isId() == false){ //Lado esquerdo nao é variavel
        error.signal("Id expected before " + lexer.token);
      }
      //Se lado esquerdo for variavel, ja é feito a verificacao de declaração la em baixo
      lexer.nextToken();
      right = expr();
      if (lexer.token != Symbol.SEMICOLON){
        if(lexer.token == Symbol.LITERALINT){
          error.signal("; expected before " + lexer.getNumberValue());
        }
        else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
          error.signal("; expected before " + lexer.getStringValue());
        }
        else{
          error.signal("; expected before " + lexer.token);
        }
      }
      else{
        lexer.nextToken();
      }

    } else if(lexer.token != Symbol.SEMICOLON){
      if(lexer.token == Symbol.LITERALINT){
        error.signal("; or = expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("; or = expected before " + lexer.getStringValue());
      }
      else{
        error.signal("; or = expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }

    if ( ! checkAssignment( left.getType(), right.getType() ) )
      error.signal("Type error in assignment");

    return new AssignExprStat( left, right );
  }

  private boolean checkAssignment( Type varType, Type exprType ) {
    if ( varType == Type.undefinedType || exprType == Type.undefinedType )
      return true;
    else
      return varType == exprType;
  }

  private ReturnStat returnStat() {
    /* ReturnStat ::= "return" Expr ";" */
    if (lexer.token != Symbol.RETURN){
      error.signal("'return' expected");
    }
    else{
      lexer.nextToken();
    }

    Expr e = expr();

    if (lexer.token != Symbol.SEMICOLON){
      if(lexer.token == Symbol.LITERALINT){
        error.signal("; expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("; expected before " + lexer.getStringValue());
      }
      else{
        error.signal("; expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }
    return new ReturnStat(e);
  }

  private IfStat ifStat() {
    // IfStat ::= "if" Expr StatList [ "else" StatList ]
    ArrayList<Statement> slLeft = new ArrayList<Statement>();
    ArrayList<Statement> slRight = new ArrayList<Statement>();

    if (lexer.token != Symbol.IF){
      error.signal("'if' expected");
    }
    else{
      lexer.nextToken();
    }

    Expr e = expr();
    if ( e.getType() != Type.booleanType )
      error.signal("Boolean type expected in if expression");

    slLeft = statList();

    if ( lexer.token == Symbol.ELSE ) {
      lexer.nextToken();
      slRight = statList();
    }

    return new IfStat( e, slLeft, slRight );
  }

  private VarDecStat varDecStat() {
    // VarDecStat ::= "var" Id ":" Type ";"

    if (lexer.token != Symbol.VAR){
      error.signal("'var' expected");
    }
    else{
      lexer.nextToken();
    }

    if (lexer.token != Symbol.IDENT){
      if(lexer.token == Symbol.LITERALINT){
        error.signal("Id expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("Id expected before " + lexer.getStringValue());
      }
      else{
        error.signal("Id expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }
    // name of the identifier
    String id = lexer.getStringValue();
    if(symbolTable.getInLocal(id) != null){
      error.signal("var " + id + " has already been declared");
    }

    if (lexer.token != Symbol.COLON){
      if(lexer.token == Symbol.LITERALINT){
        error.signal(": expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal(": expected before " + lexer.getStringValue());
      }
      else{
        error.signal(": expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }
    // get the type
    Type typeVar = type();

    if (lexer.token != Symbol.SEMICOLON){
      if(lexer.token == Symbol.LITERALINT){
        error.signal("; expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("; expected before " + lexer.getStringValue());
      }
      else{
        error.signal("; expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }

    VarDecStat v = new VarDecStat( id, typeVar, false );

    symbolTable.putInLocal(id, v);

    return v;
  }


  private WhileStat whileStat() {
    // WhileStat ::= "while" Expr StatList
    ArrayList<Statement> sl = new ArrayList<Statement>();

    if (lexer.token != Symbol.WHILE){
      error.signal("'while' expected");
    }
    else{
      lexer.nextToken();
    }
    Expr e = expr();
    if ( ! checkWhileExpr(e.getType()) )
      error.signal("Boolean expression expected");

    sl = statList();

    return new WhileStat( e, sl );
  }

  private boolean checkWhileExpr( Type exprType ) {
    if ( exprType == Type.undefinedType || exprType == Type.booleanType )
      return true;
    else
      return false;
  }


  private Expr expr() {
    /* Expr ::= ExprAnd {”or” ExprAnd} */
    ArrayList<ExprAnd> expr = new ArrayList<ExprAnd>();
    Type type;
    ExprAnd right = null;

    ExprAnd left = exprAnd();
    type = left.getType();
    expr.add(left);

    while (lexer.token == Symbol.OR) {
      lexer.nextToken();
      expr.add(right = exprAnd());
      // analise semantica
      if ( ! checkBooleanExpr( type, right.getType() ) )
        error.signal("Expression of boolean type expected");
    }

    return new Expr( expr, Symbol.OR, type );
  }

  private boolean checkBooleanExpr( Type left, Type right ) {
    if ( left == Type.undefinedType || right == Type.undefinedType )
      return true;
    else
      return left == Type.booleanType && right == Type.booleanType;
  }

  private ExprAnd exprAnd() {
    /* ExprAnd ::= ExprRel {”and” ExprRel} */
    ArrayList<ExprRel> expr = new ArrayList<ExprRel>();
    Type type;
    ExprRel right = null;

    ExprRel left = exprRel();
    type = left.getType();
    expr.add(left);

    while (lexer.token == Symbol.AND) {
      lexer.nextToken();
      expr.add(right = exprRel());
      // analise semantica
      if ( ! checkBooleanExpr( type, right.getType() ) )
        error.signal("Expression of boolean type expected");
    }

    return new ExprAnd( expr, Symbol.AND, type );
  }

  private ExprRel exprRel() {
    /* ExprRel ::= ExprAdd [ RelOp ExprAdd ] */

    ExprAdd left = exprAdd();
    Type type = left.getType();
    ExprAdd right = null;
    Symbol op = null;

    /* RelOp ::= "<" | "<=" | ">" | ">=" | "==" | "!=" */
    if (lexer.token == Symbol.LT || lexer.token == Symbol.LE || lexer.token == Symbol.GT ||
        lexer.token == Symbol.GE || lexer.token == Symbol.EQ || lexer.token == Symbol.NEQ){
      op = lexer.token;
      lexer.nextToken();
      right = exprAdd();

      if ( ! checkRelExpr(type, right.getType() ) )
        error.signal("Type error in expression");
    }

    return new ExprRel( left, right, op, type );
  }

  private boolean checkRelExpr( Type left, Type right ) {
    if ( left == Type.undefinedType || right == Type.undefinedType )
      return true;
    else if ( left == Type.stringType || right == Type.stringType )
      return false;
    else
      return left == right;
  }

  private ExprAdd exprAdd() {
    /* ExprAdd ::= ExprMult { (” + ” | ” − ”) ExprMult} */

    ArrayList<ExprMult> expr = new ArrayList<ExprMult>();
    ArrayList<Symbol> op = new ArrayList<Symbol>();
    Type type;
    ExprMult right = null;

    ExprMult left = exprMult();
    type = left.getType();
    expr.add(left);

    while (lexer.token == Symbol.PLUS || lexer.token == Symbol.MINUS){
      op.add(lexer.token);
      lexer.nextToken();
      expr.add(right = exprMult());
      if ( ! checkMathExpr( type, right.getType() ) )
        error.signal("Expression of type integer expected");
    }

    return new ExprAdd( expr, op, type );
  }

  private boolean checkMathExpr( Type left, Type right ) {
    boolean orLeft = left == Type.integerType || left == Type.undefinedType;
    boolean orRight = right == Type.integerType || right == Type.undefinedType;

    if ( left == Type.stringType || right == Type.stringType )
      return false;
    return orLeft && orRight;
  }

  private ExprMult exprMult() {
    /* ExprMult ::= ExprUnary {(” ∗ ” | ”/”) ExprUnary} */

    ArrayList<ExprUnary> expr = new ArrayList<ExprUnary>();
    ArrayList<Symbol> op = new ArrayList<Symbol>();
    Type type;
    ExprUnary right = null;

    ExprUnary left = exprUnary();
    type = left.getType();
    expr.add(left);

    while (lexer.token == Symbol.PLUS || lexer.token == Symbol.MINUS){
      op.add(lexer.token);
      lexer.nextToken();
      expr.add(right = exprUnary());
      if ( ! checkMathExpr( type, right.getType() ) )
        error.signal("Expression of type integer expected");
    }

    return new ExprMult( expr, op, type );
  }

  private ExprUnary exprUnary() {
    /* ExprUnary ::= [ ( "+" | "-" ) ] ExprPrimary */

    Symbol op=null;
    if (lexer.token == Symbol.PLUS || lexer.token == Symbol.MINUS){
      op = lexer.token;
      lexer.nextToken();
    }

    ExprPrimary exprPrimary = exprPrimary();
    Type type = exprPrimary.getType();

    // se teve operação então só pode ser int
    if (op != null){
      if ( type != Type.integerType )
        error.signal("Expression of type integer expected");
    }

    return new ExprUnary(exprPrimary, op, type);
  }

  private ExprPrimary exprPrimary() {
    /* ExprPrimary ::= Id | FuncCall | ExprLiteral */

    switch (lexer.token) {
      case IDENT:
      case WRITE:
      case WRITELN:
        String id = lexer.getStringValue();
        lexer.nextToken();
        if (lexer.token == Symbol.LEFTPAR){
          return funcCall(id);
        }
        else{
          VarDecStat v = (VarDecStat) symbolTable.getInLocal( id );
          if(v == null){
            error.signal("Variable " + id + " was not declared");
            VarDecStat v1 = new VarDecStat(id);
            return new VariableExpr(v1);
          }
          return new VariableExpr(v);
        }
      default:
        return exprLiteral();
    }
  }

  private ExprLiteral exprLiteral() {
    /* ExprLiteral ::= LiteralInt | LiteralBoolean | LiteralString */
    switch (lexer.token) {
      case LITERALINT:
        int number = lexer.getNumberValue();
	      String n = lexer.getStringValue();
        lexer.nextToken();
        return new NumberExpr(number);
     case TRUE :
        lexer.nextToken();
        return BooleanExpr.True;
      case FALSE :
        lexer.nextToken();
        return BooleanExpr.False;
      case LITERALSTRING:
        String s = lexer.getStringValue();
        lexer.nextToken();
        return new StringExpr(s);
      default :
        error.signal("ExprLiteral expected");
        return null;
    }
  }

  private FuncCall funcCall(String id) {
    /* FuncCall ::= Id "(" [ Expr {”, ”Expr} ] ")" */
    ArrayList<Expr> expr = new ArrayList<Expr>();

    // when funcCall is called token already is (
    if (lexer.token != Symbol.LEFTPAR){
      if(lexer.token == Symbol.LITERALINT){
        error.signal("( expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal("( expected before " + lexer.getStringValue());
      }
      else{
        error.signal("( expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }

    if (lexer.token == Symbol.IDENT || lexer.token == Symbol.LITERALINT || lexer.token == Symbol.FALSE ||
        lexer.token == Symbol.TRUE || lexer.token == Symbol.LITERALSTRING) {
      expr.add(expr());
      while (lexer.token == Symbol.COMMA){
        lexer.nextToken();
        expr.add(expr());
      }
    }

    if (lexer.token != Symbol.RIGHTPAR){
      if(lexer.token == Symbol.LITERALINT){
        error.signal(") expected before " + lexer.getNumberValue());
      }
      else if(lexer.token == Symbol.LITERALSTRING || lexer.token == Symbol.IDENT){
        error.signal(") expected before " + lexer.getStringValue());
      }
      else{
        error.signal(") expected before " + lexer.token);
      }
    }
    else{
      lexer.nextToken();
    }

    if (id.equals(Symbol.WRITE.toString()) || id.equals(Symbol.WRITELN.toString())){
      return new Write( expr, id );
    }

    return new FuncCall( expr, id);
  }

  public boolean getC(){
    return error.getFlagC();
  }

  private SymbolTable symbolTable;
  private Lexer lexer;
  private CompilerError error;
}


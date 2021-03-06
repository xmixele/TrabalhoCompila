/* ==========================================================================
 * Universidade Federal de São Carlos - Campus Sorocaba
 * Disciplina: Compiladores
 * Prof. Leticia Berto
 *
 * Trabalho - Análise Semântica (Fase 2)
 *
 * Aluno: Bruno Rizzi       RA: 743515
 * Aluna: Giulia Fazzi      RA: 743542
 * Aluno: Leonardo Nozaki   RA: 743561
 * Aluna: Michele Carvalho  RA: 726573
 * ========================================================================== */

package AST;

import java.util.*;

public class IfStat extends Statement {
	public IfStat( Expr expr, ArrayList<Statement> leftPart, ArrayList<Statement> rightPart ) {
		this.expr = expr;
		this.leftPart = leftPart;
		this.rightPart = rightPart;
	}

	public void genC( PW pw ) {
		int i = 0;
		int tam = this.leftPart.size();
		pw.print("if (");
		this.expr.genC(pw);
		pw.out.println("){");
		pw.add();
		while(i < tam){
			this.leftPart.get(i).genC(pw);
			i++;
		}
		pw.sub();
		pw.println("}");

		if(!this.rightPart.isEmpty()){
			i = 0;
			tam = this.rightPart.size();
			pw.println("else{");
			pw.add();
			while(i < tam){
				this.rightPart.get(i).genC(pw);
				i++;
			}
			pw.sub();
			pw.println("}");
		}
	}

	private Expr expr;
	private ArrayList<Statement> leftPart = new ArrayList<Statement>();
  private ArrayList<Statement> rightPart = new ArrayList<Statement>();
}

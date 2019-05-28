package AST;
import java.util.*;

public class Program {
	public Program( ArrayList<Function> funcList ) {
		this.funcList = funcList;
	}

	public void genC( PW pw ) {
		int i = 1;
		int tam = this.funcList.size();
		this.funcList.get(0).genC(pw);
		pw.print("\n");
		while(i < tam){
			this.funcList.get(i).genC(pw);
			pw.print("\n");
			i++;
		}
	}

	private ArrayList<Function> funcList;
}

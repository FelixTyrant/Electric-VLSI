package trick;
import Jama.*;
import java.io.*;
import java.text.DecimalFormat;

public class IntervalExp {

   private static void printM(String msg, Matrix m) {
       DecimalFormat fmt = new DecimalFormat("0.00000000000000E00");

       PrintWriter FILE = new PrintWriter(System.out, true);
       FILE.print(msg);
       m.print(FILE,fmt,15);
   }

    static Matrix abs(Matrix S) {
	int m = S.getRowDimension();
	int n = S.getColumnDimension();
	Matrix A = new Matrix(m,n);
	for (int i = 0; i < m; i++)
	    for (int j = 0; j < n; j++)
		A.set(i, j, Math.abs(S.get(i,j)));
	return A;
    }

    

   public static void main (String argv[])
       throws FileNotFoundException
    {
	int pow = 10;
	int n = 1<<pow;

	int numPoints = 500;
	int numVars = 1+4;
	Matrix raw = new Matrix(numPoints + 1, numVars);
	String varName[] = new String[numVars];
	String varType[] = new String[numVars];
	varName[0] = "omega";
	varType[0] = "frequency";
	varName[1] = "stupid";
	varType[1] = "voltage";
	varName[2] = "best";
	varType[2] = "voltage";
	varName[3] = "fast1";
	varType[3] = "voltage";
	varName[4] = "fast2";
	varType[4] = "voltage";

	for (int ip = 0; ip <= numPoints; ip++) {
	    double omega = 50.0/numPoints*ip;
	    System.out.println("omega="+omega);
	    double r = Math.exp(Math.log(0.9)/n);
	    //double eps = 2*Math.PI*nperiod/n;
	    double eps = omega/n;
	    double rc = r*Math.cos(eps);
	    double rs = r*Math.sin(eps);
	    Matrix A = new Matrix(2, 2);
	    A.set(0,0,rc); A.set(0,1,-rs);
	    A.set(1,0,rs); A.set(1,1,rc);
	    Matrix Ad = new Matrix(2, 2, 0.01/n);
	    IntervalMatrix Ai = new IntervalMatrix(A, Ad);
	    Matrix S = Matrix.identity(2,2);
	    IntervalMatrix Si = new IntervalMatrix(S);
	    IntervalMatrix Si2 = null;;
	    Matrix[] Sa = new Matrix[n+1];
	    Matrix[] Sm = new Matrix[n+1];
	    Sa[0] = abs(S);
	    Sm[0] = Si.mag();
	    for (int i = 0; i < n; i++) {
		S = A.times(S);
		Sa[i+1] = abs(S);
		Si = Ai.times(Si);

		Matrix Sum = new Matrix(2,2);
		for (int j = 0; j < i; j++) {
		    Sum = Sum.plus(Sm[j].times(Ai.delta).times(Sa[i-j]));
		}
		Si2 = new IntervalMatrix(S, Sum);
		Sm[i+1] = Si2.mag();
	    }
	    Matrix P = A;
	    IntervalMatrix Pi = Ai;
	    IntervalMatrix Mi = Ai;
	    for (int i = 0; i < pow; i++) {
		P = P.times(P);
		Pi = Pi.times(Pi);

		Matrix Mc = Mi.center;
		Matrix Md = Mi.delta;
		Matrix Ma = abs(Mc);
		Mi = new IntervalMatrix(Mc.times(Mc), Md.times(Mi.mag()).plus(Ma.times(Md)));
	    }

	    raw.set(ip, 0, omega);
	    raw.set(ip, 1, Si.delta.norm2());
	    raw.set(ip, 2, Si2.delta.norm2());
	    raw.set(ip, 3, Pi.delta.norm2());
	    raw.set(ip, 4, Mi.delta.norm2());

    /*    
    	    printM("A=",A);
	    printM("A.c=",Ai.center);
	    printM("A.d=",Ai.delta);
	    printM("A**"+n+"=",S);
	    printM("A**"+n+".c=",Si.center);
	    printM("A**"+n+".d=",Si.delta);
	    printM("A**"+n+"_.c=",Si2.center);
	    printM("A**"+n+"_.d=",Si2.delta);
	    printM("A**(2**"+pow+")=",P);
	    printM("A**(2**"+pow+").c=",Pi.center);
	    printM("A**(2**"+pow+").d=",Pi.delta);
	    printM("A**(2**"+pow+")_.c=",Mi.center);
	    printM("A**(2**"+pow+")_.d=",Mi.delta);
    */
	}
	RawWriter.write("omega.raw", varName, varType, raw);
    }
}

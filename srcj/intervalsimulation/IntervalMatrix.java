package trick;
import Jama.*;

class IntervalMatrix {
    Matrix center;
    Matrix delta;
    IntervalMatrix(Matrix center, Matrix delta) {
	if (center.getRowDimension() != delta.getRowDimension() ||
	    center.getColumnDimension() != delta.getRowDimension()) {
	    throw new IllegalArgumentException("Interval Matrix inner dimensions must agree.");
	}
	this.center = center;
	this.delta = delta;
    }
    IntervalMatrix(Matrix center) {
	this.center = center;
	this.delta = new Matrix(center.getRowDimension(), center.getColumnDimension());
    }
    int getRowDimension() { return center.getRowDimension(); }
    int getColumnDimension() { return center.getColumnDimension(); }
    Matrix mag() {
	int m = getRowDimension();
	int n = getColumnDimension();
	double[][] c = center.getArray();
	double[][] d = delta.getArray();
	Matrix x = new Matrix(m,n);
	double[][] mg = x.getArray();
	for (int i = 0; i < m; i++) {
	    double[] cr = c[i];
	    double[] dr = d[i];
	    double[] xr = mg[i];
	    for (int j = 0; j < n; j++)
		xr[j] = Math.abs(cr[j]) + dr[j];
	}
	return x;
    }
    IntervalMatrix plus(IntervalMatrix B) {
	return new IntervalMatrix(center.plus(B.center), delta.plus(B.delta));
    }
    IntervalMatrix plus(Matrix B) {
	return new IntervalMatrix(center.plus(B), delta);
    }
    IntervalMatrix times(IntervalMatrix B) {
	if (getColumnDimension() != B.getRowDimension()) {
	    throw new IllegalArgumentException("Matrix inner dimensions must agree.");
	}
	int m = getRowDimension();
	int mn = getColumnDimension();
	int n = B.getColumnDimension();
	double[][] ac = center.getArray();
	double[][] ad = delta.getArray();
	double[][] bc = B.center.getArray();
	double[][] bd = B.delta.getArray();
	Matrix xc = new Matrix(m,n);
	Matrix xd = new Matrix(m,n);
	double[][] cc = xc.getArray();
	double[][] cd = xd.getArray();
	double[] bccolj = new double[mn];
	double[] bdcolj = new double[mn];
	for (int j = 0; j < n; j++) {
	    for (int k = 0; k < mn; k++) {
		bccolj[k] = bc[k][j];
		bdcolj[k] = bd[k][j];
	    }
	    for (int i = 0; i < m; i++) {
		double[] acrowi = ac[i];
		double[] adrowi = ad[i];
		double sc = 0.0, sd = 0.0;
		for (int k = 0; k < mn; k++) {
		    double a_c = acrowi[k];
		    double a_d = adrowi[k];
		    double b_c = bccolj[k];
		    double b_d = bdcolj[k];
		    double p1 = (a_c - a_d)*(b_c - b_d);
		    double p2 = (a_c - a_d)*(b_c + b_d);
		    double p3 = (a_c + a_d)*(b_c - b_d);
		    double p4 = (a_c + a_d)*(b_c + b_d);
		    double pl = Math.min(Math.min(p1,p2),Math.min(p3,p4));
		    double ph = Math.max(Math.max(p1,p2),Math.max(p3,p4));
		    sc += 0.5*(pl+ph);
		    sd += 0.5*(ph-pl);
		}
		cc[i][j] = sc;
		cd[i][j] = sd;
	    }
	}
	return new IntervalMatrix(xc,xd);
    }

    IntervalMatrix times(double s) {
	return new IntervalMatrix(center.times(s), delta.times(Math.abs(s)));
    }
}

package r.builtins;

import r.data.*;

/**
 * "colMeans"
 * 
 * <pre>
 * x -- an array of two or more dimensions, containing numeric, complex, integer or logical values, or a numeric data frame.
 * na.rm -- logical. Should missing values (including NaN) be omitted from the calculations?
 * dims -- integer: Which dimensions are regarded as rows or columns to sum over. For row*, the sum or mean is over 
 *         dimensions dims+1, ...; for col* it is over dimensions 1:dims.
 * </pre>
 */
final class ColMeans extends ColRowBase {

    static final CallFactory _ = new ColMeans("colMeans");

    @Override double[] stat(RComplex x, int m, int n, boolean naRM) {
        return colSumsMeans(x, m, n, true, naRM);
    }

    @Override double[] stat(RDouble x, int m, int n, boolean naRM) {
        return colSumsMeans(x, m, n, true, naRM);
    }

    @Override double[] stat(RInt x, int m, int n, boolean naRM) {
        return colSumsMeans(x, m, n, true, naRM);
    }

    @Override int[] getResultDimension(int[] sourceDim) {
        int[] result = new int[sourceDim.length - 1];
        System.arraycopy(sourceDim, 1, result, 0, result.length);
        return result;
    }

    private ColMeans(String name) {
        super(name);
    }

}

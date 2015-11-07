/**
 * Created by Silun Wang on 15/9/26.
 */
public class RetrievalModelBM25 extends RetrievalModel {
    static double k1;
    static double b;
    static double k3;

    public static void setK1(double k1) {
        RetrievalModelBM25.k1 = k1;
    }

    public static void setB(double b) {
        RetrievalModelBM25.b = b;
    }

    public static void setK3(double k3) {
        RetrievalModelBM25.k3 = k3;
    }

    public static double getB() {
        return b;
    }

    public static double getK1() {
        return k1;
    }

    public static double getK3() {
        return k3;
    }

    @Override
    public String defaultQrySopName() {
        return new String("#sum");
    }
}

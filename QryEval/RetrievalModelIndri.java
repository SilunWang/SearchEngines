/**
 * Created by Silun Wang on 15/9/27.
 */
public class RetrievalModelIndri extends RetrievalModel {
    static double lambda;
    static double mu;

    public static void setLambda(double lambda) {
        RetrievalModelIndri.lambda = lambda;
    }

    public static void setMu(double mu) {
        RetrievalModelIndri.mu = mu;
    }

    public static double getLambda() {
        return lambda;
    }

    public static double getMu() {
        return mu;
    }

    @Override
    public String defaultQrySopName() {
        return new String("#and");
    }
}

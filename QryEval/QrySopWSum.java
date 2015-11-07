import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by Silun Wang on 15/10/11.
 */

public class QrySopWSum extends QrySop {

    Stack<Double> weights = new Stack<Double>();
    ArrayList<Double> weightsArr = new ArrayList<Double>();

    double weightSum = 0.0;

    public Stack<Double> getWeights() {
        return this.weights;
    }

    public void calculateWeightSum() {
        while (!weights.isEmpty()) {
            double tmp = weights.pop();
            this.weightSum += tmp;
            // add to first
            weightsArr.add(0, tmp);
        }
    }

    /**
     * Indicates whether the query has a match.
     *
     * @param r The retrieval model that determines what is a match
     * @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        if (r instanceof RetrievalModelIndri)
            return this.docIteratorHasMatchMin(r);
        else
            return this.docIteratorHasMatchAll(r);
    }

    /**
     * Get a score for the document that docIteratorHasMatch matched.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws java.io.IOException Error accessing the Lucene index
     */
    public double getScore(RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelIndri) {
            calculateWeightSum();
            return this.getScoreIndri(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the AND operator.");
        }
    }

    @Override
    public double getDefaultScore(RetrievalModel r, long doc_id) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            //please do not forget to calculate sum
            //or the weightsArr will be empty
            calculateWeightSum();
            double default_score = 0.0;
            for (int i = 0; i < this.args.size(); i++) {
                Qry q = this.args.get(i);
                default_score += ((QrySop) q).getDefaultScore(r, doc_id) * this.weightsArr.get(i) / this.weightSum;
            }
            return default_score;
        }
        else {
            return 0.0;
        }
    }



    private double getScoreIndri(RetrievalModel r) throws IOException {

        if (!this.docIteratorHasMatchCache()) {
            return 0.0;

        } else {

            double score = 0.0;
            int doc_id = this.docIteratorGetMatch();

            for (int i = 0; i < this.args.size(); i++) {

                Qry q = this.args.get(i);

                // this document contain a query term
                if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == doc_id) {

                    // operand: QryIop
                    if (q instanceof QryIop) {
                        double lambda = RetrievalModelIndri.getLambda();
                        double mu = RetrievalModelIndri.getMu();
                        // term frequency
                        int tf = ((QryIop) q).docIteratorGetMatchPosting().tf;
                        // length(d)
                        int doc_len = Idx.getFieldLength(((QryIop) q).getField(), doc_id);
                        // term frequency in the entire collection
                        int ctf = ((QryIop) q).invertedList.ctf;
                        // total number of word occurrences in collection
                        long sum_len = Idx.getSumOfFieldLengths(((QryIop) q).getField());
                        // maximum likelihood estimate
                        double p_MLE = ((double) ctf) / sum_len;
                        double p = (1 - lambda) * (tf + mu * p_MLE) / (doc_len + mu) + mu * p_MLE;
                        score += p * this.weightsArr.get(i) / this.weightSum;
                    }

                    // operand QrySopScore
                    else if (q instanceof QrySopScore) {
                        double lambda = RetrievalModelIndri.getLambda();
                        double mu = RetrievalModelIndri.getMu();
                        // term frequency
                        int tf = q.getArg(0).docIteratorGetMatchPosting().tf;
                        // length(d)
                        int doc_len = Idx.getFieldLength(q.getArg(0).getField(), doc_id);
                        // term frequency in the entire collection
                        int ctf = q.getArg(0).invertedList.ctf;
                        // total doc len
                        long sum_len = Idx.getSumOfFieldLengths(q.getArg(0).getField());
                        // maximum likelihood estimate
                        double p_MLE = ((double) ctf) / sum_len;
                        double p = (1 - lambda) * (tf + mu * p_MLE) / (doc_len + mu) + lambda * p_MLE;
                        score += p * this.weightsArr.get(i) / this.weightSum;
                    }

                    // other QrySop
                    else if (q instanceof QrySop) {
                        score += ((QrySop) q).getScore(r) * this.weightsArr.get(i) / this.weightSum;
                    }

                } else {
                    score += ((QrySop) q).getDefaultScore(r, doc_id) * this.weightsArr.get(i) / this.weightSum;
                }
            }
            return score;
        }
    }
}

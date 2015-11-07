/**
 * Created by Silun Wang on 15/9/19.
 * For operator AND
 */

import java.io.IOException;


public class QrySopAnd extends QrySop {

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

        if (r instanceof RetrievalModelUnrankedBoolean) {
            return this.getScoreUnrankedBoolean(r);
        } else if (r instanceof RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean(r);
        } else if (r instanceof RetrievalModelIndri) {
          return this.getScoreIndri(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the AND operator.");
        }
    }

    @Override
    public double getDefaultScore(RetrievalModel r, long doc_id) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            double default_score = 1.0;
            for (Qry q : this.args) {
                default_score *= Math.pow(((QrySop) q).getDefaultScore(r, doc_id), 1.0 / this.args.size());
            }
            return default_score;
        }
        else {
            return 0.0;
        }
    }

    /**
     * getScore for the UnrankedBoolean retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws java.io.IOException Error accessing the Lucene index
     */
    private double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }
    }

    /**
     * getScore for the RankedBoolean retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws java.io.IOException Error accessing the Lucene index
     */
    private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            double score = Integer.MAX_VALUE;
            for (Qry q : this.args) {
                score = Math.min(score, ((QrySop) q).getScore(r));
            }
            return score;
        }
    }

    private double getScoreIndri(RetrievalModel r) throws IOException {

        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {

            double score = 1.0;
            int doc_id = this.docIteratorGetMatch();

            for (Qry q : this.args) {
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
                        score *= Math.pow(p, 1.0 / this.args.size());
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
                        score *= Math.pow(p, 1.0 / this.args.size());
                    }
                    // other QrySop
                    else if (q instanceof QrySop) {
                        score *= Math.pow(((QrySop) q).getScore(r), 1.0 / this.args.size());
                    }

                } else {
                    score *= Math.pow(((QrySop) q).getDefaultScore(r, doc_id), 1.0 / this.args.size());
                }
            }
            return score;
        }
    }
}

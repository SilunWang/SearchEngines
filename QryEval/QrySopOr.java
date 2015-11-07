/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

/**
 * The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

    /**
     * Indicates whether the query has a match.
     *
     * @param r The retrieval model that determines what is a match
     * @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
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
                    (r.getClass().getName() + " doesn't support the OR operator.");
        }
    }

    @Override
    public double getDefaultScore(RetrievalModel r, long doc_id) throws IOException {
        return 0;
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
            double score = 0.0;
            int doc_id = this.docIteratorGetMatch();
            for (Qry q : this.args) {
                // if matches the same term
                if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == doc_id)
                    score = Math.max(score, ((QrySop) q).getScore(r));
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
                        double p = (1 - lambda) * (tf + mu * p_MLE) / (doc_len + mu) + lambda * p_MLE;
                        score *= 1 - p;
                    } else if (q instanceof QrySop) {
                        score *= 1 - ((QrySop) q).getScore(r);
                    }
                }
            }
            return 1 - score;
        }
    }

}

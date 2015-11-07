/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

/**
 * The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

    /**
     *  Document-independent values that should be determined just once.
     *  Some retrieval models have these, some don't.
     */

    /**
     * Indicates whether the query has a match.
     *
     * @param r The retrieval model that determines what is a match
     * @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchFirst(r);
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
        } else if (r instanceof  RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean(r);
        } else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SCORE operator.");
        }
    }

    /**
     * getScore for the Unranked retrieval model.
     *
     * @param r The retrieval model that determines how scores are calculated.
     * @return The document score.
     * @throws java.io.IOException Error accessing the Lucene index
     */
    public double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }
    }

    public double getScoreRankedBoolean(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            double score;
            Qry q = this.args.get(0);
            score = ((QryIop) q).docIteratorGetMatchPosting().tf;
            return score;
        }
    }

    /**
     * Initialize the query operator (and its arguments), including any
     * internal iterators.  If the query operator is of type QryIop, it
     * is fully evaluated, and the results are stored in an internal
     * inverted list that may be accessed via the internal iterator.
     *
     * @param r A retrieval model that guides initialization
     * @throws java.io.IOException Error accessing the Lucene index.
     */
    public void initialize(RetrievalModel r) throws IOException {

        Qry q = this.args.get(0);
        q.initialize(r);
    }

    @Override
    public double getDefaultScore(RetrievalModel r, long doc_id) throws IOException {

        if (r instanceof RetrievalModelIndri) {
            QryIop q = (QryIop) this.args.get(0);
            double lambda = RetrievalModelIndri.getLambda();
            double mu = RetrievalModelIndri.getMu();
            // length(d)
            int doc_len = Idx.getFieldLength(q.getField(), (int)doc_id);
            // term frequency in the entire collection
            int ctf = q.invertedList.ctf;
            // total number of word occurrences in collection
            long sum_len = Idx.getSumOfFieldLengths(q.getField());
            // maximum likelihood estimate
            double p_MLE = ((double) ctf) / sum_len;
            double p = (1 - lambda) * (mu * p_MLE) / (doc_len + mu) + lambda * p_MLE;
            return p;
        } else {
            return 0.0;
        }
    }

}

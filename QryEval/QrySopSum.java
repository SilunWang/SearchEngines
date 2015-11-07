/**
 * Created by Silun Wang on 15/9/26.
 */

import java.io.IOException;
import java.util.Hashtable;


public class QrySopSum extends QrySop {


    @Override
    public double getScore(RetrievalModel r) throws IOException {

        int doc_id = this.docIteratorGetMatch();
        double sum = 0.0;
        double k1 = RetrievalModelBM25.getK1();
        double b = RetrievalModelBM25.getB();
        double k3 = RetrievalModelBM25.getK3();
        Hashtable<String, Integer> queryTable = new Hashtable<String, Integer>();

        // query term stat
        for (int i = 0; i < this.args.size(); i++) {
            int cnt = 0;
            if (this.args.get(i) instanceof QryIop && queryTable.containsKey(this.args.get(i).toString())) {
                cnt = (Integer) queryTable.get(this.args.get(i).toString());
            }
            queryTable.put(this.args.get(i).toString(), cnt + 1);
        }

        // Score list: calculate
        for (Qry q : this.args) {
            if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == doc_id && q instanceof QrySop)
                sum += ((QrySop) q).getScore(r);
        }

        if (r instanceof RetrievalModelBM25) {

            // Inverted list: use BM25 formula to calculate sum
            for (Qry q : this.args) {
                // QrySop
                if (q instanceof QrySop)
                    continue;
                // QryIop
                double score;
                if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == doc_id) {
                    String field = ((QryIop) q).getField();
                    // document frequency
                    double df = ((QryIop) q).getDf();
                    // term frequency
                    double tf = ((QryIop) q).docIteratorGetMatchPosting().tf;
                    // document length in specific field
                    double doc_len = Idx.getFieldLength(field, doc_id);
                    // query term frequency
                    double qtf = queryTable.get(q.toString());
                    if (qtf == 0)   // already calculated
                        continue;
                    else            // set as already calculated
                        queryTable.put(q.toString(), 0);
                    // average length: use Idx.getDocCount instead of getNumDocs
                    double avg_len = ((double) Idx.getSumOfFieldLengths(field)) / Idx.getDocCount(field);
                    // N = Idx.getNumDocs
                    double RSJ_weight = Math.max(0, Math.log((Idx.getNumDocs() - df + 0.5) / (df + 0.5)));
                    double tf_weight = tf / (tf + k1 * (1 - b + b * (doc_len / avg_len)));
                    double user_weight = ((k3 + 1) * qtf) / (k3 + qtf);
                    score = RSJ_weight * tf_weight * user_weight;
                    sum += score;
                }
            }

        }

        else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
        return sum;
    }

    @Override
    public double getDefaultScore(RetrievalModel r, long doc_id) throws IOException {
        return 0;
    }

    @Override
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
    }
}

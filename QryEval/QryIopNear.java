import java.io.IOException;
import java.util.*;

/**
 * For operator NEAR
 * Created by Silun Wang on 15/9/19.
 */
public class QryIopNear extends QryIop {
    // near distance
    int dist;

    public QryIopNear(int n) {
        this.dist = n;
    }

    @Override
    protected void evaluate() throws IOException {

        this.invertedList = new InvList(this.getField());
        // no argument
        if (args.size() == 0)
            return;
        if (args.size() == 1) {
            this.invertedList = ((QryIop) this.args.get(0)).invertedList;
            return;
        }

        // iterate each doc, greedy algorithms
        while (true) {
            //  init doc_id
            int doc_id = Qry.INVALID_DOCID;

            if (this.docIteratorHasMatchAll(null))
                doc_id = this.args.get(0).docIteratorGetMatch();
            // fail to find a match all
            if (doc_id == Qry.INVALID_DOCID)
                break;
            // the last argument's position in doc
            List<Integer> positions = new ArrayList<Integer>();

            // iterate each location
            while (true) {
                boolean success = false;
                int position = 0;

                // index from 1 to n-1
                for (int i = 1; i < this.args.size(); i++) {
                    // get previous pos
                    if (!((QryIop) this.args.get(i-1)).locIteratorHasMatch())
                        break;
                    int position1 = ((QryIop) this.args.get(i - 1)).locIteratorGetMatch();

                    // go beyond pre pos
                    ((QryIop) this.args.get(i)).locIteratorAdvancePast(position1);
                    if (!((QryIop) this.args.get(i)).locIteratorHasMatch())
                        break;
                    int position2 = ((QryIop) this.args.get(i)).locIteratorGetMatch();

                    // near condition satisfied?
                    if (position2 > position1 && position2 - position1 <= this.dist) {
                        position = position2;
                        if (i == this.args.size() - 1)
                            success = true;
                    } else {
                        success = false;
                        break;
                    }
                }

                // find a legal position
                if (success) {
                    positions.add(position);
                    // move each loc pointer
                    for (Qry q_i : this.args) {
                        ((QryIop) q_i).locIteratorAdvance();
                    }
                } else {
                    // move first arg
                    ((QryIop) this.args.get(0)).locIteratorAdvance();
                    // if no more match position for the first arg
                    if (!((QryIop) this.args.get(0)).locIteratorHasMatch())
                        break;
                    // reset other pointers to beginning
                    for (int i = 1; i < this.args.size(); i++) {
                        ((QryIop) this.args.get(i)).locIteratorReset();
                        int preposition = ((QryIop) this.args.get(0)).locIteratorGetMatch();
                        ((QryIop) this.args.get(i)).locIteratorAdvancePast(preposition);
                    }

                }

            }

            // move doc pointer
            for (Qry q_i : this.args)
                q_i.docIteratorAdvancePast(doc_id);
            // success
            if (positions.size() > 0)
                this.invertedList.appendPosting(doc_id, positions);
        }
    }
}

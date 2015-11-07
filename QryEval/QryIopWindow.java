import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Silun Wang on 15/10/11.
 */
public class QryIopWindow extends QryIop {

    int windowSize;

    public QryIopWindow(int n) {
        this.windowSize = n;
    }

    @Override
    protected void evaluate() throws IOException {

        this.invertedList = new InvList(this.getField());
        // no argument
        if (args.size() == 0)
            return;
        // one argument
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

            // window records the last argument's position
            ArrayList<Integer> positions = new ArrayList<Integer>();

            while (true) {
                boolean success = false;
                int leftPos = Integer.MAX_VALUE;
                int leftIdx = -1;
                int rightPos = Integer.MIN_VALUE;
                int cnt = 0;

                // iterate each location
                for (int i = 0; i < this.args.size(); i++) {
                    if (!((QryIop) this.args.get(i)).locIteratorHasMatch())
                        break;
                    int position = ((QryIop) this.args.get(i)).locIteratorGetMatch();
                    if (position < leftPos) {
                        leftPos = position;
                        leftIdx = i;
                    }
                    if (position > rightPos) {
                        rightPos = position;
                    }
                    cnt++;
                }

                if (rightPos - leftPos < windowSize && rightPos > leftPos && cnt == this.args.size()) {
                    success = true;
                }

                if (success) {
                    // find a successful match
                    positions.add(rightPos);
                    // move every loc pointer
                    for (Qry q_i : this.args) {
                        ((QryIop) q_i).locIteratorAdvance();
                    }
                } else if (leftIdx != -1){
                    // no successful match, move leftmost pointer
                    ((QryIop) this.args.get(leftIdx)).locIteratorAdvance();
                    if (!((QryIop) this.args.get(leftIdx)).locIteratorHasMatch())
                        break;
                } else {
                    // no leftmost pointer
                    break;
                }

            }


            // move doc pointer
            for (Qry q_i : this.args)
                q_i.docIteratorAdvancePast(doc_id);
            // has window match
            if (positions.size() > 0)
                this.invertedList.appendPosting(doc_id, positions);
        }
    }

}

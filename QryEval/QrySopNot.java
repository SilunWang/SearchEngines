import java.io.IOException;

/**
 * Created by Silun Wang on 15/9/27.
 */
public class QrySopNot extends QrySop {
    @Override
    public double getScore(RetrievalModel r) throws IOException {
        return 0;
    }

    @Override
    public double getDefaultScore(RetrievalModel r, long doc_id) throws IOException {
        return 0;
    }

    @Override
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return !this.docIteratorHasMatchMin(r);
    }
}

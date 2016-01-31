/*
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.1.
 */

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.*;

/**
 * QryEval is a simple application that reads queries from a file,
 * evaluates them against an index, and writes the results to an
 * output file.  This class contains the main method, a method for
 * reading parameter and query files, initialization methods, a simple
 * query parser, a simple query processor, and methods for reporting
 * results.
 * <p/>
 * This software illustrates the architecture for the portion of a
 * search engine that evaluates queries.  It is a guide for class
 * homework assignments, so it emphasizes simplicity over efficiency.
 * Everything could be done more efficiently and elegantly.
 * <p/>
 * The {@link Qry} hierarchy implements query evaluation using a
 * 'document at a time' (DaaT) methodology.  Initially it contains an
 * #OR operator for the unranked Boolean retrieval model and a #SYN
 * (synonym) operator for any retrieval model.  It is easily extended
 * to support additional query operators and retrieval models.  See
 * the {@link Qry} class for details.
 * <p/>
 * The {@link RetrievalModel} hierarchy stores parameters and
 * information required by different retrieval models.  Retrieval
 * models that need these parameters (e.g., BM25 and Indri) use them
 * very frequently, so the RetrievalModel class emphasizes fast access.
 * <p/>
 * The {@link Idx} hierarchy provides access to information in the
 * Lucene index.  It is intended to be simpler than accessing the
 * Lucene index directly.
 * <p/>
 * As the search engine becomes more complex, it becomes useful to
 * have a standard approach to representing documents and scores.
 * The {@link ScoreList} class provides this capability.
 */
public class QryEval {

    //  --------------- Constants and variables ---------------------

    private static final String USAGE =
            "Usage:  java QryEval paramFile\n\n";

    private static final EnglishAnalyzerConfigurable ANALYZER =
            new EnglishAnalyzerConfigurable(Version.LUCENE_43);
    private static final String[] TEXT_FIELDS =
            {"body", "title", "url", "inlink"};
    static Map<String, String> parameters;

    //  --------------- Methods ---------------------------------------

    /**
     * @param args The only argument is the parameter file name.
     * @throws Exception Error accessing the Lucene index.
     */
    public static void main(String[] args) throws Exception {

        //  This is a timer that you may find useful.  It is used here to
        //  time how long the entire program takes, but you can move it
        //  around to time specific parts of your code.

        Timer timer = new Timer();
        timer.start();

        //  Check that a parameter file is included, and that the required
        //  parameters are present.  Just store the parameters.  They get
        //  processed later during initialization of different system
        //  components.

        if (args.length < 1) {
            throw new IllegalArgumentException(USAGE);
        }

        parameters = readParameterFile(args[0]);

        //  Configure query lexical processing to match index lexical
        //  processing.  Initialize the index and retrieval model.

        ANALYZER.setLowercase(true);
        ANALYZER.setStopwordRemoval(true);
        ANALYZER.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);

        Idx.initialize(parameters.get("indexPath"));
        RetrievalModel model = initializeRetrievalModel(parameters);
        FileIO.setFilename(parameters.get("trecEvalOutputPath"));
        FileIO.deleteFile(parameters.get("trecEvalOutputPath"));
        if (parameters.containsKey("BM25:b"))
            RetrievalModelBM25.setB(Double.parseDouble(parameters.get("BM25:b")));
        if (parameters.containsKey("BM25:k_1"))
            RetrievalModelBM25.setK1(Double.parseDouble(parameters.get("BM25:k_1")));
        if (parameters.containsKey("BM25:k_3"))
            RetrievalModelBM25.setK3(Double.parseDouble(parameters.get("BM25:k_3")));
        if (parameters.containsKey("Indri:lambda"))
            RetrievalModelIndri.setLambda(Double.parseDouble(parameters.get("Indri:lambda")));
        if (parameters.containsKey("Indri:mu"))
            RetrievalModelIndri.setMu(Double.parseDouble(parameters.get("Indri:mu")));

        //  Perform experiments.
        processQueryFile(parameters.get("queryFilePath"), model);

        //  Clean up.
        timer.stop();
        System.out.println("Time:  " + timer);
    }

    /**
     * Allocate the retrieval model and initialize it using parameters
     * from the parameter file.
     *
     * @return The initialized retrieval model
     * @throws java.io.IOException Error accessing the Lucene index.
     */
    private static RetrievalModel initializeRetrievalModel(Map<String, String> parameters)
            throws IOException {

        RetrievalModel model = null;
        String modelString = parameters.get("retrievalAlgorithm").toLowerCase();

        if (modelString.equals("unrankedboolean")) {
            model = new RetrievalModelUnrankedBoolean();
        } else if (modelString.equals("rankedboolean")) {
            model = new RetrievalModelRankedBoolean();
        } else if (modelString.equals("bm25")) {
            model = new RetrievalModelBM25();
        } else if (modelString.equals("indri")) {
            model = new RetrievalModelIndri();
        } else {
            throw new IllegalArgumentException
                    ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
        }

        return model;
    }

    /**
     * Optimize the query by removing degenerate nodes produced during
     * query parsing, for example '#NEAR/1 (of the)' which turns into
     * '#NEAR/1 ()' after stopwords are removed; and unnecessary nodes
     * or subtrees, such as #AND (#AND (a)), which can be replaced by 'a'.
     */
    static Qry optimizeQuery(Qry q) {

        //  Term operators don't benefit from optimization.

        if (q instanceof QryIopTerm) {
            return q;
        }

        //  Optimization is a depth-first task, so recurse on query
        //  arguments.  This is done in reverse to simplify deleting
        //  query arguments that become null.

        for (int i = q.args.size() - 1; i >= 0; i--) {

            Qry q_i_before = q.args.get(i);
            Qry q_i_after = optimizeQuery(q_i_before);

            if (q_i_after == null) {
                q.removeArg(i);            // optimization deleted the argument
            } else {
                if (q_i_before != q_i_after) {
                    q.args.set(i, q_i_after);    // optimization changed the argument
                }
            }
        }

        //  If the operator now has no arguments, it is deleted.
        if (q.args.size() == 0) {
            return null;
        }

        /*
        //  Only SCORE operators can have a single argument.  Other
        //  query operators that have just one argument are deleted.

        if ((q.args.size() == 1) && (!(q instanceof QrySopSum)) &&
                (!(q instanceof QrySopScore))) {
            q = q.args.get(0);
        }
        */
        return q;

    }

    /**
     * Return a query tree that corresponds to the query.
     *
     * @param qString A string containing a query.
     * @throws java.io.IOException Error accessing the Lucene index.
     */
    static Qry parseQuery(String qString, RetrievalModel model) throws IOException {

        //  Add a default query operator to every query. This is a tiny
        //  bit of inefficiency, but it allows other code to assume
        //  that the query will return document ids and scores.

        String defaultOp = model.defaultQrySopName();
        qString = defaultOp + "(" + qString + ")";

        //  Simple query tokenization.  Terms like "near-death" are handled later.

        StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
        String token = null;

        //  This is a simple, stack-based parser.  These variables record
        //  the parser's state.

        Qry currentOp = null;
        Stack<Qry> opStack = new Stack<Qry>();
        boolean weightExpected = false;
        Stack<Stack<Double>> weightStack = new Stack<Stack<Double>>();

        //  Each pass of the loop processes one token. The query operator
        //  on the top of the opStack is also stored in currentOp to
        //  make the code more readable.

        while (tokens.hasMoreTokens()) {

            token = tokens.nextToken();

            if (token.matches("[ ,(\t\n\r]")) {
                continue;
            } else if (token.equals(")")) {    // Finish current query op.

                // If the current query operator is not an argument to another
                // query operator (i.e., the opStack is empty when the current
                // query operator is removed), we're done (assuming correct
                // syntax - see below).
                if (opStack.peek() instanceof QrySopWSum || opStack.peek() instanceof QrySopWAnd)
                    weightStack.pop();

                opStack.pop();

                if (opStack.empty())
                    break;

                // Not done yet.  Add the current operator as an argument to
                // the higher-level operator, and shift processing back to the
                // higher-level operator.

                Qry arg = currentOp;
                currentOp = opStack.peek();
                currentOp.appendArg(arg);
                if (currentOp instanceof QrySopWAnd || currentOp instanceof QrySopWSum)
                    weightExpected = true;

            } else if (token.equalsIgnoreCase("#or")) {
                currentOp = new QrySopOr();
                currentOp.setDisplayName(token);
                opStack.push(currentOp);

            } else if (token.equalsIgnoreCase("#and")) {
                currentOp = new QrySopAnd();
                currentOp.setDisplayName(token);
                opStack.push(currentOp);

            } else if (token.equalsIgnoreCase("#syn")) {
                currentOp = new QryIopSyn();
                currentOp.setDisplayName(token);
                opStack.push(currentOp);

            } else if (token.toLowerCase().indexOf("#near") == 0) {
                int len = token.length();
                int n = Integer.parseInt(token.substring(6, len));
                currentOp = new QryIopNear(n);
                currentOp.setDisplayName(token);
                opStack.push(currentOp);

            } else if (token.toLowerCase().indexOf("#window") == 0) {
                int len = token.length();
                int n = Integer.parseInt(token.substring(8, len));
                currentOp = new QryIopWindow(n);
                currentOp.setDisplayName(token);
                opStack.push(currentOp);

            } else if (token.equalsIgnoreCase("#sum")) {
                currentOp = new QrySopSum();
                currentOp.setDisplayName(token);
                opStack.push(currentOp);

            } else if (token.equalsIgnoreCase("#wand")) {

                currentOp = new QrySopWAnd();
                currentOp.setDisplayName(token);
                weightStack.push(((QrySopWAnd) currentOp).getWeights());
                weightExpected = true;
                opStack.push(currentOp);

            } else if (token.equalsIgnoreCase("#wsum")) {
                currentOp = new QrySopWSum();
                currentOp.setDisplayName(token);
                weightStack.push(((QrySopWSum) currentOp).getWeights());
                weightExpected = true;
                opStack.push(currentOp);

            } else if ((currentOp instanceof QrySopWAnd || currentOp instanceof QrySopWSum)
                    && weightExpected) {
                weightStack.peek().push(Double.parseDouble(token));
                weightExpected = false;

            } else {

                //  Split the token into a term and a field.
                int delimiter = token.indexOf('.');
                String field = null;
                String term = null;

                if (delimiter < 0) {
                    field = "body";
                    term = token;
                } else {
                    field = token.substring(delimiter + 1).toLowerCase();
                    term = token.substring(0, delimiter);
                }

                if ((field.compareTo("url") != 0) &&
                        (field.compareTo("keywords") != 0) &&
                        (field.compareTo("title") != 0) &&
                        (field.compareTo("body") != 0) &&
                        (field.compareTo("inlink") != 0)) {
                    throw new IllegalArgumentException("Error: Unknown field " + token);
                }

                //  Lexical processing, stopwords, stemming.  A loop is used
                //  just in case a term (e.g., "near-death") gets tokenized into
                //  multiple terms (e.g., "near" and "death").
                // first pop out their weight
                double tmpWeight = 0;
                // next op should be weight
                if (currentOp instanceof QrySopWAnd || currentOp instanceof QrySopWSum) {
                    tmpWeight = weightStack.peek().pop();
                    weightExpected = true;
                }

                String t[] = tokenizeQuery(term);

                for (int j = 0; j < t.length; j++) {

                    Qry termOp = new QryIopTerm(t[j], field);
                    currentOp.appendArg(termOp);
                    // push weight
                    if (currentOp instanceof QrySopWAnd || currentOp instanceof QrySopWSum) {
                        weightStack.peek().push(tmpWeight);
                    }
                }
                // next input should be weight

            }

        }


        //  A broken structured query can leave unprocessed tokens on the opStack,

        if (tokens.hasMoreTokens()) {
            throw new IllegalArgumentException
                    ("Error:  Query syntax is incorrect.  " + qString);
        }

        return currentOp;
    }

    /**
     * Print a message indicating the amount of memory used. The caller
     * can indicate whether garbage collection should be performed,
     * which slows the program but reduces memory usage.
     *
     * @param gc If true, run the garbage collector before reporting.
     */
    public static void printMemoryUsage(boolean gc) {

        Runtime runtime = Runtime.getRuntime();

        if (gc)
            runtime.gc();

        System.out.println("Memory used:  "
                + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
    }

    /**
     * Process one query.
     *
     * @param qString A string that contains a query.
     * @param model   The retrieval model determines how matching and scoring is done.
     * @return Search results
     * @throws java.io.IOException Error accessing the index
     */
    static ScoreList processQuery(String qString, RetrievalModel model) throws IOException {

        Qry q = parseQuery(qString, model);
        q = optimizeQuery(q);

        // Show the query that is evaluated
        System.out.println("    --> " + q);

        if (q != null) {

            ScoreList r = new ScoreList();

            if (q.args.size() > 0) {        // Ignore empty queries

                q.initialize(model);

                while (q.docIteratorHasMatch(model)) {
                    int docid = q.docIteratorGetMatch();
                    double score = ((QrySop) q).getScore(model);
                    r.add(docid, score);
                    q.docIteratorAdvancePast(docid);
                }
            }

            return r;
        } else
            return null;
    }

    static ScoreList processLetorQuery(String query) {
        ScoreList r = null;
        try {
            r = processQuery(query, new RetrievalModelBM25());
        } catch (Exception e) {

        }

        return r;
    }

    /**
     * Process the query file.
     *
     * @param queryFilePath
     * @param model
     * @throws java.io.IOException Error accessing the Lucene index.
     */
    static void processQueryFile(String queryFilePath, RetrievalModel model) throws IOException {

        BufferedReader input = null;

        try {
            String qLine;
            input = new BufferedReader(new FileReader(queryFilePath));

            //  Each pass of the loop processes one query.
            while ((qLine = input.readLine()) != null) {

                int d = qLine.indexOf(':');
                if (d < 0) {
                    throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
                }

                printMemoryUsage(false);

                String qid = qLine.substring(0, d);
                String query = qLine.substring(d + 1);

                System.out.println("Original Query " + qLine);

                /* Process query or expand query */
                ScoreList r;
                String newQuery;
                // missing or no query expansion
                if (!parameters.containsKey("fb") || parameters.get("fb").equalsIgnoreCase("false")) {
                    r = processQuery(query, model);
                } else {
                    /* get parameters */
                    // the number of documents to use for query expansion
                    int fbDocs = Integer.parseInt(parameters.get("fbDocs"));
                    // the number of terms that are added to the query
                    int fbTerms = Integer.parseInt(parameters.get("fbTerms"));
                    // amount of smoothing used to calculate p(t|d)
                    int fbMu = Integer.parseInt(parameters.get("fbMu"));
                    // the weight on the original query
                    double fbOrigWeight = Double.parseDouble(parameters.get("fbOrigWeight"));
                    newQuery = "#WAND(" + String.valueOf(fbOrigWeight) + " #AND(" + query + ") ";

                    // if has rank file, read from file
                    if (parameters.containsKey("fbInitialRankingFile")) {
                        // construct new score list from file
                        File file = new File(parameters.get("fbInitialRankingFile"));
                        FileInputStream fis = new FileInputStream(file);
                        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                        String line;
                        r = new ScoreList();
                        while ((line = br.readLine()) != null) {
                            String[] arr = line.split(" +");
                            if (arr[0].equals(qid))
                                r.add(Idx.getInternalDocid(arr[2]), Double.parseDouble(arr[4]));
                        }
                        br.close();
                    }
                    // no rank file
                    else {
                        // get score list from original query
                        r = processQuery(query, model);
                    }
                    r.sort();
                    // a queue to store top weighted terms
                    List<Pair> queue = new LinkedList<Pair>();
                    // recording score of every term
                    HashMap<String, Double> ScoreMap = new HashMap<String, Double>();
                    // recording ctf of every term
                    HashMap<String, Long> CTFMap = new HashMap<String, Long>();
                    for (int i = 0; i < fbDocs; i++) {
                        // for every doc-i, get term vector
                        TermVector vector = new TermVector(r.getDocid(i), "body");
                        for (int j = 1; j < vector.stemsLength(); j++) {
                            CTFMap.put(vector.stemString(j), vector.totalStemFreq(j));
                        }
                    }

                    for (int i = 0; i < fbDocs; i++) {
                        // for every doc-i, get term vector
                        TermVector vector = new TermVector(r.getDocid(i), "body");
                        // for every term in the map, sum up scores for every doc
                        for (Map.Entry<String, Long> entry : CTFMap.entrySet()) {
                            String term = entry.getKey();
                            long ctf = entry.getValue();
                            double p_term = 0.0;
                            if (ScoreMap.containsKey(term))
                                p_term = ScoreMap.get(term);
                            // default tf
                            int tf = 0;
                            if (vector.indexOfStem(term) != -1)
                                tf = vector.stemFreq(vector.indexOfStem(term));
                            // length(d)
                            int doc_len = vector.positionsLength();
                            // length(C)
                            long sum_len = Idx.getSumOfFieldLengths("body");
                            // p_MLE(t|C)
                            double p_MLE = ((double) ctf) / sum_len;
                            // p(t|d)
                            double tmp = (tf + fbMu * p_MLE + 0.0) / (doc_len + fbMu);
                            tmp *= r.getDocidScore(i) * Math.log(((double) sum_len) / ctf);
                            p_term += tmp;
                            ScoreMap.put(term, p_term);
                        }
                    }

                    for (Map.Entry<String, Double> entry : ScoreMap.entrySet()) {
                        // ignore "app.com" "app,com"
                        if (entry.getKey().contains(".") || entry.getKey().contains(","))
                            continue;
                        // add the element to queue
                        queue.add(new Pair(entry.getKey(), entry.getValue()));
                        Collections.sort(queue, new Comparator<Pair>() {
                            @Override
                            public int compare(Pair o1, Pair o2) {
                                return o1.score - o2.score > 0 ? 1 : -1;
                            }
                        });
                        // queue is full
                        while (queue.size() > fbTerms)
                            queue.remove(0);
                    }

                    newQuery += String.valueOf(1 - fbOrigWeight) + " ";

                    String expandQuery = "";
                    // construct new query
                    while (!queue.isEmpty()) {
                        Pair pair = queue.get(0);
                        queue.remove(0);
                        pair.score = Math.round(pair.score * 10000.0) / 10000.0;
                        String queryLine = String.valueOf(pair.score) + " " + pair.term;
                        expandQuery = queryLine + " " + expandQuery;
                    }
                    expandQuery = "#WAND(" + expandQuery + ")";
                    newQuery += expandQuery + ")";
                    System.out.println("New Query " + newQuery);
                    // if need to write to expansion query file
                    if (parameters.containsKey("fbExpansionQueryFile")) {
                        FileWriter writer = new FileWriter(parameters.get("fbExpansionQueryFile"), false);
                        BufferedWriter out = new BufferedWriter(writer);
                        out.write(qid + ": ");
                        out.write(expandQuery + "\n");
                        out.close();
                    }

                    // process new query, get new result
                    r = processQuery(newQuery, model);
                }

                if (r != null) {
                    r.sort();
                    writeResults(qid, r);
                    printResults(qid, r);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    static class Pair {
        String term;
        double score;
        public Pair(String t, double s) {
            term = t;
            score = s;
        }
    }

    /**
     * Print the query results.
     * <p/>
     * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
     * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
     * <p/>
     * QueryID Q0 DocID Rank Score RunID
     *
     * @param queryName Original query.
     * @param result    A list of document ids and scores
     * @throws java.io.IOException Error accessing the Lucene index.
     */
    static void printResults(String queryName, ScoreList result) throws IOException {
        String Q0 = "Q0";
        String output = "";
        if (result.size() >= 1) {
            // best 100 docs
            for (int i = 0; i < result.size() && i < 100; i++) {
                String DocID = Idx.getExternalDocid(result.getDocid(i));
                String rank = String.valueOf(i + 1);
                String score = String.valueOf(result.getDocidScore(i));
                output += queryName + "\t" + Q0 + "\t" + DocID + "\t" + rank + "\t" + score + "\trun-1\n";
            }
        }
        else {
            output += queryName + "\t" + Q0 + "\tdummy\t1\t0\trun-1\n";
        }
        System.out.print(output);
    }

    static void writeResults(String queryName, ScoreList result) throws IOException {
        String Q0 = "Q0";
        String output = "";
        if (result.size() >= 1) {
            // best 100 docs
            for (int i = 0; i < result.size() && i < 100; i++) {
                String DocID = Idx.getExternalDocid(result.getDocid(i));
                String rank = String.valueOf(i + 1);
                String score = String.valueOf(result.getDocidScore(i));
                output += queryName + "\t" + Q0 + "\t" + DocID + "\t" + rank + "\t" + score + "\trun-1\n";
            }
        }
        else {
            output += queryName + "\t" + Q0 + "\tdummy\t1\t0\trun-1\n";
        }
        FileIO.write2File(output);
    }

    /**
     * Read the specified parameter file, and confirm that the required
     * parameters are present.  The parameters are returned in a
     * HashMap.  The caller (or its minions) are responsible for
     * processing them.
     *
     * @return The parameters, in <key, value> format.
     */
    private static Map<String, String> readParameterFile(String parameterFileName)
            throws IOException {

        Map<String, String> parameters = new HashMap<String, String>();

        File parameterFile = new File(parameterFileName);

        if (!parameterFile.canRead()) {
            throw new IllegalArgumentException
                    ("Can't read " + parameterFileName);
        }

        Scanner scan = new Scanner(parameterFile);
        String line = null;
        do {
            line = scan.nextLine();
            String[] pair = line.split("=");
            parameters.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());

        scan.close();

        if (!(parameters.containsKey("indexPath") &&
                parameters.containsKey("queryFilePath") &&
                parameters.containsKey("trecEvalOutputPath") &&
                parameters.containsKey("retrievalAlgorithm"))) {
            throw new IllegalArgumentException
                    ("Required parameters were missing from the parameter file.");
        }

        return parameters;
    }

    /**
     * Given a query string, returns the terms one at a time with stopwords
     * removed and the terms stemmed using the Krovetz stemmer.
     * <p/>
     * Use this method to process raw query terms.
     *
     * @param query String containing query
     * @return Array of query tokens
     * @throws java.io.IOException Error accessing the Lucene index.
     */
    static String[] tokenizeQuery(String query) throws IOException {

        TokenStreamComponents comp =
                ANALYZER.createComponents("dummy", new StringReader(query));
        TokenStream tokenStream = comp.getTokenStream();

        CharTermAttribute charTermAttribute =
                tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        List<String> tokens = new ArrayList<String>();

        while (tokenStream.incrementToken()) {
            String term = charTermAttribute.toString();
            tokens.add(term);
        }

        return tokens.toArray(new String[tokens.size()]);
    }

}

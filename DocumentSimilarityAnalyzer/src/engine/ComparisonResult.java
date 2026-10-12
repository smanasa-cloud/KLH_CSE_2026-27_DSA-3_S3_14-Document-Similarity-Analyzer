package engine;

/*
 * ComparisonResult.java
 *
 * A simple data holder that stores everything we learn from comparing
 * ONE pair of documents. This is built by DocumentAnalyzer.compare()
 * and later displayed on the website as a "Pairwise Comparison Card".
 */
public class ComparisonResult {

    public String document1Name;
    public String document2Name;

    public String longestCommonSubstring;
    public int longestCommonSubstringLength;

    public double suffixSimilarity;
    public int editDistance;
    public double editSimilarity;

    public double finalSimilarity;
    public String classification;
    public boolean duplicateFlag;

    public ComparisonResult(String document1Name, String document2Name) {
        this.document1Name = document1Name;
        this.document2Name = document2Name;
    }
}

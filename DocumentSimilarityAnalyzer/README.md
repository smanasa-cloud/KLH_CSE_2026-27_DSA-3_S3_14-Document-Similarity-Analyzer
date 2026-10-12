# Document Similarity Analyzer

A small DSA-3 project built with Java 17 and plain HTML, CSS, and JavaScript. It compares 2–10 TXT/DOCX documents or pasted text, shows pairwise scores, a matrix, rankings, possible duplicates, and related groups.

## Problem and solution

Comparing many documents by eye is slow. This prototype normalizes each document, finds its longest exact shared substring with a suffix array and Kasai LCP, computes Levenshtein edit distance, and combines the two scores. It compares each unique pair once, then presents a matrix, ranked matches, and connected groups above a chosen threshold. It is an educational similarity metric, not a plagiarism verdict.

## Run locally

Install **JDK 17 or newer** and open a terminal in this folder. On Windows PowerShell or on macOS/Linux:

```sh
javac -d out src/model/*.java src/engine/*.java src/*.java
java -cp out Main
```

On **Windows PowerShell**, wildcard expansion for `javac` may differ. Use this instead:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem .\src -Recurse -Filter *.java | ForEach-Object FullName
javac -d out $files
java -cp out Main
```

Open **http://localhost:8080**. Stop with Ctrl+C. If port 8080 is taken, change `8080` in `src/Main.java`, recompile, and use the new URL. Use **Load sample set**, or upload TXT/DOCX files or paste text. Enter 2–10 nonempty documents with unique names. Each TXT or pasted document is limited to 2,500 characters; each DOCX file is limited to 1 MB and 2,500 extracted characters. Set a threshold and optionally tick parallel mode. Press **Analyze documents**. Ten documents create 45 unique pair comparisons, so a full analysis can take longer.

The browser reads TXT files as text and sends DOCX file data to the Java server. The server extracts the main Word document text from `word/document.xml` using Java's built-in ZIP and XML readers, then runs the same analysis pipeline and returns JSON. Old `.doc` files and PDF files are not supported. Images of text, Word headers and footers, and some specialized Word content are not extracted. `samples/` contains four small TXT examples and `document1_word.docx`. Upload `document1.txt` alongside `document1_word.docx` to check that mixed formats match at 100%. No database, package manager, or third-party Java library is required. The page attempts to load an optional Google Font; it falls back to Arial offline.

## Folder structure

- `src/engine/`: the DSA algorithm classes.
- `src/model/Document.java`: document data and normalization.
- `src/Main.java`: Java built-in HTTP server, input checks, result JSON, and static files.
- `src/WordTextExtractor.java`: reads text from DOCX outside the DSA engine.
- `web/`: HTML, CSS, and JavaScript dashboard.
- `samples/`: four demonstration documents.
- `out/`: compiled class files, created when you compile.
- `VIVA.md`: short viva answers.

## Pipeline and modules

1. **Module 1:** separate the simple web/API adapter from the DSA engine; normalize input and choose suitable algorithms.
2. **Module 2:** build a suffix array using prefix doubling and merge sort; construct an LCP array using Kasai's algorithm; scan neighboring suffixes from different documents for the longest common substring.
3. **Module 3:** calculate Levenshtein distance with two rows of dynamic programming.
4. **Module 6:** optionally split independent pair comparisons across at most four Java threads. Sequential mode is the default.

Modules 4 and 5 are not used. The engine uses arrays and loops rather than `java.util` collections; HTTP code uses standard Java utilities.

A **suffix** is text from one position to the end: `banana` has suffixes `banana`, `anana`, `nana`, `ana`, `na`, `a`. A suffix array sorts their start positions: `[5,3,1,0,4,2]`. **LCP** is the length of the common prefix of neighboring sorted suffixes. **Kasai's algorithm** builds all these LCP lengths in linear time by reusing the previous match length. After joining two texts with `#`, the longest LCP between suffixes from different texts gives the longest shared substring. **Edit distance** counts single-character insertions, deletions, and substitutions needed to change one cleaned text into the other.

## Score and output

```text
suffixScore = 100 × 2 × longestSharedLength / (lengthA + lengthB)
editScore   = 100 × (1 - editDistance / max(lengthA, lengthB))
finalScore  = 0.70 × suffixScore + 0.30 × editScore
```

The 70/30 weights are project choices, not optimized scientific weights. The score uses the **single longest** shared passage; separate short matching passages do not add up. For identical nonempty texts the score is 100%. The threshold determines the `duplicate` flag and edges between documents in the grouping graph. Categories use fixed bands: 90+ possible duplicate, 75+ highly similar, 50+ moderate, 25+ slight, below 25 low similarity. Thus the category label and adjustable duplicate threshold can differ. A group is a connected component: A–B and B–C can group A, B, C even if A–C is below the threshold. Single documents appear in their own group.

Example: upload identical copies of `document1.txt`; their matrix entries will be 100%, their edit distance 0, and they will be flagged when the threshold is 100% or less. With the four samples, the first two should rank above the unrelated football document; exact percentages depend on cleaned text.

## Complexity and limits

For two texts of lengths a and b, suffix-array construction is approximately O((a+b) log²(a+b)) with this educational merge sort, Kasai LCP and scanning are O(a+b), and edit distance takes O(a×b) time and O(min(a,b)) additional DP memory. For d documents, there are d(d−1)/2 pairs. The matrix takes O(d²) memory. Parallel mode may help for larger input, but thread overhead can make small samples slower. This demo has a strict 10 document / 2,500 extracted character per document limit because edit distance across many large pairs is costly. It has not been benchmarked for 10,000 documents. It normalizes to ASCII letters, digits, and spaces, so non-Latin letters are not handled well. Adding PDF/old DOC parsing, Unicode-aware cleaning, candidate filtering, and fuller matching coverage are future improvements.

## Checks

The build can be checked with `java -cp out engine.SuffixArray`, `java -cp out engine.KasaiLCP`, `java -cp out engine.EditDistance`, and `java -cp out engine.ParallelAnalyzer`. Open the website, load the sample set, analyze it in both modes, switch ranking target, adjust threshold, and check empty/duplicate-name errors.

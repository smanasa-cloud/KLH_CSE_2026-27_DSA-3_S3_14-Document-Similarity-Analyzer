import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import engine.*;
import model.Document;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Small HTTP adapter. All comparisons remain in the engine package. */
public class Main {
    private static final int MAX_BYTES = 16_000_000;
    private static final int MAX_DOCS = 10;
    private static final int MAX_TEXT_LENGTH = 2500;
    private static final Path WEB = Path.of("web");

    public static void main(String[] args) throws IOException {
        if (!Files.isDirectory(WEB)) {
            System.err.println("Run this from the DocumentSimilarityAnalyzer folder.");
            return;
        }
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.createContext("/", Main::handle);
        server.start();
        System.out.println("Open http://localhost:8080 (Ctrl+C to stop)");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/analyze")) {
                if (!exchange.getRequestMethod().equals("POST")) {
                    send(exchange, 405, "application/json", "{\"error\":\"Use POST for analysis.\"}");
                    return;
                }
                analyze(exchange);
                return;
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, "text/plain", "Method not allowed");
                return;
            }
            String file = path.equals("/") ? "index.html" : path.substring(1);
            if (!file.equals("index.html") && !file.equals("style.css") && !file.equals("app.js")) {
                send(exchange, 404, "text/plain", "Not found");
                return;
            }
            String type = file.endsWith(".css") ? "text/css" : file.endsWith(".js") ? "text/javascript" : "text/html";
            send(exchange, 200, type, Files.readString(WEB.resolve(file), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, "application/json", "{\"error\":" + quote(e.getMessage()) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "application/json", "{\"error\":\"Analysis failed. Check the Java terminal.\"}");
        } finally {
            exchange.close();
        }
    }

    private static void analyze(HttpExchange exchange) throws IOException {
        byte[] body;
        try (InputStream input = exchange.getRequestBody()) {
            body = input.readNBytes(MAX_BYTES + 1);
        }
        if (body.length > MAX_BYTES) throw new IllegalArgumentException("Request is too large.");
        Map<String, String> form = parseForm(new String(body, StandardCharsets.UTF_8));
        int count = integer(form.get("count"), "document count");
        if (count < 2 || count > MAX_DOCS) throw new IllegalArgumentException("Add 2 to 10 documents.");
        double threshold;
        try { threshold = Double.parseDouble(form.getOrDefault("threshold", "80")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Threshold must be a number."); }
        if (!Double.isFinite(threshold) || threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100.");
        }
        Document[] docs = new Document[count];
        for (int i = 0; i < count; i++) {
            String name = form.getOrDefault("name" + i, "").trim();
            String format = form.getOrDefault("format" + i, "text");
            String text;
            if (format.equals("docx")) {
                text = WordTextExtractor.extract(form.get("docx" + i));
            } else if (format.equals("text")) {
                text = form.getOrDefault("text" + i, "");
            } else {
                throw new IllegalArgumentException("Only TXT and DOCX files are supported.");
            }
            if (name.isEmpty() || text.trim().isEmpty()) throw new IllegalArgumentException("Each document needs a name and text.");
            if (name.length() > 80 || text.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException("Each document must have at most 2,500 characters and a short name.");
            // Distinct display names are needed by the existing ranking and matrix classes.
            for (int j = 0; j < i; j++) {
                if (docs[j].fileName.equalsIgnoreCase(name)) throw new IllegalArgumentException("Give each document a unique name.");
            }
            docs[i] = new Document(i, name, text);
            if (docs[i].normalizedText.isEmpty()) throw new IllegalArgumentException("Document " + name + " has no letters or digits after cleaning.");
        }
        boolean parallel = "true".equals(form.get("parallel"));
        ComparisonResult[] results = parallel ? ParallelAnalyzer.compareAllParallel(docs, threshold)
                                             : DocumentAnalyzer.compareAll(docs, threshold);
        double[][] matrix = SimilarityMatrix.build(docs, results);
        String[][] groups = Clustering.buildClusters(docs, matrix, threshold);
        send(exchange, 200, "application/json", resultJson(docs, results, matrix, groups, threshold, parallel));
    }

    private static int integer(String value, String label) {
        try { return Integer.parseInt(value); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid " + label + "."); }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            int equals = pair.indexOf('=');
            if (equals < 0) continue;
            String key = URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
            form.put(key, value);
        }
        return form;
    }

    private static String resultJson(Document[] docs, ComparisonResult[] results, double[][] matrix,
                                     String[][] groups, double threshold, boolean parallel) {
        StringBuilder json = new StringBuilder("{\"names\":[");
        for (int i = 0; i < docs.length; i++) {
            if (i > 0) json.append(',');
            json.append(quote(docs[i].fileName));
        }
        json.append("],\"threshold\":").append(threshold).append(",\"parallel\":").append(parallel);
        json.append(",\"matrix\":[");
        for (int i = 0; i < docs.length; i++) {
            if (i > 0) json.append(',');
            json.append('[');
            for (int j = 0; j < docs.length; j++) {
                if (j > 0) json.append(',');
                json.append(matrix[i][j]);
            }
            json.append(']');
        }
        json.append("],\"pairs\":[");
        for (int i = 0; i < results.length; i++) {
            if (i > 0) json.append(',');
            ComparisonResult r = results[i];
            json.append("{\"a\":").append(quote(r.document1Name))
                .append(",\"b\":").append(quote(r.document2Name))
                .append(",\"final\":").append(r.finalSimilarity)
                .append(",\"suffix\":").append(r.suffixSimilarity)
                .append(",\"edit\":").append(r.editSimilarity)
                .append(",\"distance\":").append(r.editDistance)
                .append(",\"longest\":").append(quote(r.longestCommonSubstring))
                .append(",\"longestLength\":").append(r.longestCommonSubstringLength)
                .append(",\"category\":").append(quote(r.classification))
                .append(",\"duplicate\":").append(r.duplicateFlag).append('}');
        }
        json.append("],\"groups\":[");
        for (int i = 0; i < groups.length; i++) {
            if (i > 0) json.append(',');
            json.append('[');
            for (int j = 0; j < groups[i].length; j++) {
                if (j > 0) json.append(',');
                json.append(quote(groups[i][j]));
            }
            json.append(']');
        }
        json.append("],\"rankings\":[");
        for (int i = 0; i < docs.length; i++) {
            if (i > 0) json.append(',');
            json.append('[');
            Ranking.RankedEntry[] ranked = Ranking.rankSimilarTo(docs[i], results);
            for (int j = 0; j < ranked.length; j++) {
                if (j > 0) json.append(',');
                json.append("{\"name\":").append(quote(ranked[j].documentName))
                    .append(",\"score\":").append(ranked[j].similarity).append('}');
            }
            json.append(']');
        }
        json.append("]}");
        return json.toString();
    }

    private static String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') result.append('\\').append(c);
            else if (c == '\n') result.append("\\n");
            else if (c == '\r') result.append("\\r");
            else if (c == '\t') result.append("\\t");
            else if (c < 32) result.append(' ');
            else result.append(c);
        }
        return result.append('"').toString();
    }

    private static void send(HttpExchange exchange, int status, String type, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}

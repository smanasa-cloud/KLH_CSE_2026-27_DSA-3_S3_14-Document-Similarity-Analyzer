import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Reads the main text of a DOCX file. This file belongs to the web/input layer. */
public class WordTextExtractor {
    public static final int MAX_FILE_BYTES = 1_000_000;
    private static final int MAX_XML_BYTES = 2_000_000;
    private static final int MAX_TEXT_LENGTH = 2500;
    private static final String WORD_NAMESPACE =
        "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    public static String extract(String base64) {
        // Base64 is about one third larger than the original file.
        if (base64 == null || base64.length() > 1_400_000) {
            throw new IllegalArgumentException("Word file must be at most 1 MB.");
        }
        byte[] file;
        try {
            file = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Word file data.");
        }
        if (file.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Word file must be at most 1 MB.");
        }

        // DOCX is a ZIP containing word/document.xml. No external library is needed.
        byte[] documentXml = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = zip.read(buffer)) != -1) {
                        if (output.size() + count > MAX_XML_BYTES) {
                            throw new IllegalArgumentException("Word document is too large to read.");
                        }
                        output.write(buffer, 0, count);
                    }
                    documentXml = output.toByteArray();
                    break;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the Word file. Upload a valid .docx file.");
        }
        if (documentXml == null) {
            throw new IllegalArgumentException("Could not find text in the Word file. Upload a valid .docx file.");
        }

        StringBuilder text = new StringBuilder();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.newSAXParser().parse(new ByteArrayInputStream(documentXml), new DefaultHandler() {
                private boolean insideText = false;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                    if (!WORD_NAMESPACE.equals(uri)) return;
                    if (localName.equals("t")) insideText = true;
                    else if (localName.equals("tab")) text.append(' ');
                    else if (localName.equals("br") || localName.equals("cr")) text.append('\n');
                }

                @Override
                public void characters(char[] characters, int start, int length) throws SAXException {
                    if (insideText) {
                        text.append(characters, start, length);
                        if (text.length() > MAX_TEXT_LENGTH + 100) {
                            throw new SAXException("Word text exceeds 2,500 characters.");
                        }
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (!WORD_NAMESPACE.equals(uri)) return;
                    if (localName.equals("t")) insideText = false;
                    else if (localName.equals("p")) text.append('\n');
                }
            });
        } catch (SAXException e) {
            if (e.getMessage() != null && e.getMessage().contains("exceeds 2,500")) {
                throw new IllegalArgumentException("Word text exceeds 2,500 characters.");
            }
            throw new IllegalArgumentException("Could not read text from this Word file.");
        } catch (IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException("Could not read text from this Word file.");
        }
        String extracted = text.toString().trim();
        if (extracted.isEmpty()) throw new IllegalArgumentException("Word document has no readable text.");
        if (extracted.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Word text exceeds 2,500 characters.");
        }
        return extracted;
    }
}

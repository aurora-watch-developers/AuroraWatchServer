package uk.ac.lancs.aurorawatch.server.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import uk.ac.lancs.aurorawatch.server.Status;

import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

@Service
public class FileDownloaderService {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloaderService.class);
    private static final String USER_AGENT = "User-Agent";
    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");
    private static final long THROTTLE = 60 * 1000;
    

    @Value("${file.download.from}")
    private String urlString;

    @Value("${file.download.userAgent}")
    private String userAgent;

    private DocumentBuilder builder;
    private long lastRun = 0;

    public FileDownloaderService() throws ParserConfigurationException {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    private boolean throttle() {
        long timeNow = System.currentTimeMillis();
        if (lastRun + THROTTLE > timeNow) {
            LOG.warn("throttle() called too frequently: LAST_RUN " + lastRun
                    + ", THROTTLE " + THROTTLE + ", timeNow " + timeNow);
            return true;
        }
        lastRun = timeNow;
        return false;
    }

    private Status parseStatus(Document document) {
        LOG.debug("Parsing document: " + document);

        NodeList nodeList = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {

            Node node = nodeList.item(i);
            LOG.trace(node.getNodeName());
            if (node.getNodeName().equalsIgnoreCase("current")) {

                NodeList childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {

                    Node cNode = childNodes.item(j);
                    LOG.trace("child node " + cNode.getNodeName());
                    if (cNode.getNodeName().equalsIgnoreCase("state")) {

                        String name = cNode.getAttributes().getNamedItem("name").getNodeValue();
                        Status status = Status.fromString(name);
                        if (status == null) {
                            LOG.warn("Unknown status: " + name);
                        }
                        return status;
                    }

                }
            }
        }
        return null;

    }

    private String extractContentType(String contentTypeString) {
        if (contentTypeString == null) {
            return null;
        }
        try {
            Matcher m = CHARSET_PATTERN.matcher(contentTypeString);
            if (m.find()) {
                return m.group(1).trim().toUpperCase();
            }
        } catch (Exception e) {
            LOG.warn(e.toString());
        }
        return null;
    }

    public Status getStatus() {
        // Check that someone isn't repeatedly hitting /scheduledFileDownload
        // outside of cron.xml
        if (throttle()) { 
            return null; 
        }

        HttpURLConnection conn = null;
        InputStream in = null;

        try {

            URL url = new URL(urlString);
            FetchOptions options = FetchOptions.Builder.withDeadline(30000);
            HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST, options);
            request.addHeader(new HTTPHeader(USER_AGENT, userAgent));
            URLFetchService service = URLFetchServiceFactory.getURLFetchService();
            HTTPResponse response = service.fetch(request);
            
            LOG.info("Fetching " + url.toString());

            if (!(response.getResponseCode() == HttpStatus.OK.value() || 
                    response.getResponseCode() == HttpStatus.NOT_MODIFIED.value())) {
                LOG.error("Invalid status code returned: " + response.getResponseCode());
                return null;
            }

            String characterEncoding = null;
            List<HTTPHeader> headers = response.getHeaders();
            for (HTTPHeader header : headers) {
                LOG.trace(header.getName() + ": " + header.getValue());
                if (header.getName().equals("Content-Type")) {
                    String contentType = header.getValue();
                    characterEncoding = extractContentType(contentType);
                    LOG.debug("Response set a Content-Type: " + contentType + " with characterEncoding " + characterEncoding);
                }
            }
            
            if (characterEncoding == null) {
                characterEncoding = "utf-8";
                LOG.debug("No character encoding set: defaulting to " + characterEncoding);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(response.getContent());
            Document document = builder.parse(bais);
            Status status = parseStatus(document);
            LOG.info("Done fetching " + urlString);
            return status;

        } catch (IOException e) {
            LOG.error("Could not fetch " + urlString, e);
            return null;
            
        } catch (SAXException e) {
            LOG.error("Could not parse " + urlString, e);
            return null;

        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignore) {

                }
            }
            IOUtils.closeQuietly(in);
        }
    }
}

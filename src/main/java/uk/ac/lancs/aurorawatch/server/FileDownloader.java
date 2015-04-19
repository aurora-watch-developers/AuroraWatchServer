package uk.ac.lancs.aurorawatch.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Service
public class FileDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloader.class);
    private static final String USER_AGENT = "User-Agent";
    private static final long LAST_RUN = System.currentTimeMillis();
    private static final long THROTTLE = 60 * 1000;

    @Value("${file.download.from}")
    private String urlString;

    @Value("${file.download.userAgent}")
    private String userAgent;

    private DocumentBuilder builder;

    public FileDownloader() throws ParserConfigurationException {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }
    
    private boolean throttle() {
        long timeNow = System.currentTimeMillis();
        if (LAST_RUN + THROTTLE > timeNow) {
            LOG.warn("throttle() called too frequently: LAST_RUN " + LAST_RUN + ", THROTTLE " + THROTTLE + ", timeNow " + timeNow);
            return true;
        }
        return false;
    }
    
    private Status parseStatus(Document document) {
        LOG.debug("Parsing document: " + document);

        NodeList nodeList = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {

            Node node = nodeList.item(i);
            if (node.getNodeName().equalsIgnoreCase("current")) {

                NodeList childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {

                    Node cNode = childNodes.item(j);
                    if (cNode.getNodeName().equalsIgnoreCase("state")) {

                        String name = cNode.getAttributes().getNamedItem("name").getNodeValue();
                        return Status.fromString(name);
                    }

                }
            }
        }
        return null;

    }

    public Status getStatus() {
        // Check that someone isn't repeatedly hitting /scheduledFileDownload
        // outside of cron.xml
        /*if (throttle()) {
            return null;
        }*/
        
        HttpURLConnection conn = null;
        InputStream in = null;

        try {

            URL url = new URL(urlString);

            LOG.info("Fetching " + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(USER_AGENT, userAgent);
            conn.setRequestMethod("GET");

            in = new BufferedInputStream(conn.getInputStream());
            

            Document document = builder.parse(in);
            Status status = parseStatus(document);
            LOG.info("Fetched " + urlString);
            return status;

        } catch (IOException | SAXException e) {
            LOG.error("Could not fetch " + urlString, e);
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

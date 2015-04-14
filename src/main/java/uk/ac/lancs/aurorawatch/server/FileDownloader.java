package uk.ac.lancs.aurorawatch.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class FileDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloader.class);
    private static final String USER_AGENT = "User-Agent";

    @Value("${file.download.from}")
    private String urlString;

    @Value("${file.download.to}")
    private String path;

    @Value("${file.download.userAgent}")
    private String userAgent;

    private DocumentBuilder builder;
    private Status status;

    public FileDownloader() throws ParserConfigurationException {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
    }
    
    @PostConstruct
    public void afterPropertiesSet() {
        status = parseStatus();
    }

    private boolean downloadFile() {
        HttpURLConnection conn = null;
        InputStream in = null;
        OutputStream out = null;

        File file = new File(path);
        try {

            URL url = new URL(urlString);

            LOG.info("Fetching " + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(USER_AGENT, userAgent);
            conn.setRequestMethod("POST");

            in = new BufferedInputStream(conn.getInputStream());
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Couldn't create dir: " + parent);
            }
            if (!file.exists() && !file.createNewFile()) {
                throw new IOException("Create " + path + " returned false");
            }
            out = new FileOutputStream(file);

            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data)) != -1) {
                out.write(data, 0, count);
            }

            out.flush();
            out.close();
            in.close();

            LOG.info("Fetched " + urlString);
            return true;

        } catch (IOException e) {
            LOG.error("Could not fetch " + urlString, e);
            if (file.exists() && file.length() == 0 && !file.delete()) {
                LOG.warn("Could not tidy up file " + file);
            }
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignore) {

                }
            }
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    private Status parseStatus() {
        LOG.debug("Parsing status file: " + path);
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        Document document;
        try {
            document = builder.parse(file);
        } catch (IOException | SAXException e) {
            LOG.warn("Could not parse " + file, e);
            return null;
        }

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

    public boolean statusChanged() {
        Status prevStatus = status;
        LOG.debug("previous status: " + prevStatus);
        status = null;
        if (downloadFile()) {
            status = parseStatus();
            LOG.info("current status: " + prevStatus);
        }
        return status != null && (prevStatus == null || prevStatus != status);
    }
    
    public Status getStatus() {
        return status;
    }
}

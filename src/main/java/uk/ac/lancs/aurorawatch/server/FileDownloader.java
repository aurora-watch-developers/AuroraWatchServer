package uk.ac.lancs.aurorawatch.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    public boolean downloadFile() {
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
}

package org.aurorawatchdevs.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.appengine.api.datastore.Entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Service
public class GCMNotifierService {

    private static final Logger LOG = LoggerFactory.getLogger(GCMNotifierService.class);

    @Value("${gcm.apiKey}")
    private String apiKey;

    @Value("${gcm.projectNumber}")
    private String projectNumber;

    public void notifyGCM(Iterable<Entity> clients) {
        LOG.debug("notifyGCM: start");

        StringBuilder registrationIds = new StringBuilder();
        registrationIds.append("{ \"registration_ids\": [");
        String splitter = "";
        for (Entity entity : clients) {
            LOG.info("notifying " + entity.getProperty("email"));
            registrationIds.append(splitter);
            splitter = ", ";
            registrationIds.append("\"" + entity.getProperty("registrationId") + "\"");
        }
        registrationIds.append("] }");

        postNotifications(registrationIds.toString());

    }

    private void postNotifications(String message) {
        try {
            URL url = new URL("https://android.googleapis.com/gcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");

            apiKey = "AIzaSyCrNYbWH_gNHMN6z2giB4Oe_dUgnTn94d8"; //Probably need to store this somewhere better!

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "key=" + apiKey);

            LOG.debug("notifyGCM: apiKey= " + apiKey);
            LOG.debug("notifyGCM: message= " + message);

            conn.setDoOutput(true);

            OutputStreamWriter streamWriter = new OutputStreamWriter(conn.getOutputStream());
            streamWriter.write(message);
            streamWriter.flush();
            streamWriter.close();

            int responseCode = conn.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

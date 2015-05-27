package org.aurorawatchdevs.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.appengine.api.datastore.Entity;

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
        registrationIds.append("{ \"regstration_ids\": [");

        String splitter = "";
        for (Entity entity : clients) {
            LOG.info("notifying " + entity.getProperty("email"));
            registrationIds.append(splitter);
            splitter = ", ";
            registrationIds.append("\"" + entity.getProperty("registrationId") + "\"");
        }

        registrationIds.append("] }");
    }
}

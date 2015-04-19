package uk.ac.lancs.aurorawatch.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GCMNotifierService {

    private static final Logger LOG = LoggerFactory.getLogger(GCMNotifierService.class);

    @Value("${gcm.apiKey}")
    private String apiKey;

    @Value("${gcm.projectNumber}")
    private String projectNumber;
    
    public void notifyGCM() {
        LOG.debug("notifyGCM: start");
    }
}

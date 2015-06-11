package org.aurorawatchdevs.server.web;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import org.aurorawatchdevs.server.Status;
import org.aurorawatchdevs.server.dao.AlertLevelDAO;
import org.aurorawatchdevs.server.dao.StatusDAO;
import org.aurorawatchdevs.server.service.FileDownloaderService;
import org.aurorawatchdevs.server.service.GCMNotifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

@Controller
public class FileDownloadController {
    
    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadController.class);
    
    @Autowired
    private FileDownloaderService downloader;
  
    @Autowired  
    private GCMNotifierService notifier;
    
    @Autowired
    private StatusDAO statusDAO;
    
    @Autowired
    private AlertLevelDAO alertDAO;
    
    Status checkStatus() {
        Status status = downloader.getStatus();
        if (status == null) {
            LOG.warn("Status could not be retrieved");
            return null;
        }
        boolean changed = statusDAO.save(status);
        if (changed) {
            LOG.warn("Status changed: " + status);
            Iterable<Entity> clients = alertDAO.getWithStatusGreaterThanOrEqualTo(status);
            notifier.notifyGCM(clients);
            
        } else {
            LOG.debug("No status change: " + status);
        }
        return status;
    }

    @RequestMapping(value="/scheduledFileDownload", method = RequestMethod.GET)
    public void scheduledFileDownload(Writer responseWriter) throws IOException {
        LOG.debug("scheduledFileDownload: start");
        Status status = checkStatus();
        String statusString = status == null ? "unknown" : status.name();
        responseWriter.write(statusString);
    }
    
    @RequestMapping(value="/testGCM", method = RequestMethod.GET) 
    public void testGCM(@RequestParam String email, Writer responseWriter) throws IOException {
        
        Entity entity = alertDAO.getWithEmail(email);
        if (entity == null) {
            responseWriter.write("User not found with email " + email);
            return;
        }
        
        notifier.notifyGCM(Arrays.asList(entity));
        responseWriter.write("OK");
    }
}

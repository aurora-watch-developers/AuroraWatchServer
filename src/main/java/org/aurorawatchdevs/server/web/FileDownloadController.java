package org.aurorawatchdevs.server.web;

import java.io.IOException;
import java.io.Writer;

import org.aurorawatchdevs.server.Status;
import org.aurorawatchdevs.server.dao.StatusDAO;
import org.aurorawatchdevs.server.service.FileDownloaderService;
import org.aurorawatchdevs.server.service.GCMNotifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class FileDownloadController {
    
    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadController.class);
    
    @Autowired
    private FileDownloaderService downloader;
  
    @Autowired  
    private GCMNotifierService notifier;
    
    @Autowired
    private StatusDAO dao;
    
    Status checkStatus() {
        Status status = downloader.getStatus();
        if (status == null) {
            LOG.warn("Status could not be retrieved");
            return null;
        }
        boolean changed = dao.save(status);
        if (changed) {
            LOG.warn("Status changed: " + status);
            notifier.notifyGCM();
            
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
}

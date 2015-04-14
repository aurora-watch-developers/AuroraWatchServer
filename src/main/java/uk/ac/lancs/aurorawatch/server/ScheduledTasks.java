package uk.ac.lancs.aurorawatch.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasks.class);
    
    @Autowired
    private FileDownloader downloader;
    
    @Autowired
    private GCMNotifier notifier;
    
    void checkStatus() {
        boolean statusChanged = downloader.statusChanged();
        if (statusChanged) {
            LOG.warn("Status changed: " + downloader.getStatus());
            notifier.notifyGCM();
            
        } else {
            LOG.debug("No status change: " + downloader.getStatus());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void timedTask() {
        checkStatus();
    }
}

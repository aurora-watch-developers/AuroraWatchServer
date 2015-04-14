package uk.ac.lancs.aurorawatch.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    
    @Autowired
    private FileDownloader downloader;

    @Scheduled(fixedRate = 60000)
    public void reportCurrentTime() {
        downloader.downloadFile();
    }
}

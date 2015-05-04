package org.aurorawatchdevs.server.dao;

import java.util.Date;

import org.aurorawatchdevs.server.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

@Service
public class StatusDAO {
    
    static final String ENTITY_NAME = Status.class.getSimpleName();
    static final String STATUS = "status";
    
    private static final Logger LOG = LoggerFactory.getLogger(StatusDAO.class);
    
    public boolean save(Status status) {
        if (status == null) {
            LOG.warn("Status is required");
            return false;
        }
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Date now = new Date();
        
        Query query = new Query(ENTITY_NAME);
        PreparedQuery pq = datastore.prepare(query);
        Entity statusEntity = pq.asSingleEntity();
        
        if (statusEntity == null) {
            statusEntity = new Entity(ENTITY_NAME);
        }
        
        // Has the status changed?
        boolean changed = statusEntity.getProperty(STATUS) == null || !statusEntity.getProperty(STATUS).equals(status.name());

        if (changed) {
            statusEntity.setProperty("since", now);
            statusEntity.setProperty(STATUS, status.name());
        }
        statusEntity.setProperty("updated", now);
        datastore.put(statusEntity);
        return changed;
    }

}

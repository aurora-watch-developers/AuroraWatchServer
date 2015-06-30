package org.aurorawatchdevs.server.dao;

import java.util.Date;
import java.util.Iterator;

import org.aurorawatchdevs.server.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

@Service
public class AlertLevelDAO {
    
    static final String ENTITY_NAME = "AlertClient";
    static final String STATUS = "status";
    static final String REGISTRATION_ID = "registration_id";
    static final String EMAIL = "email";
    
    private static final Logger LOG = LoggerFactory.getLogger(AlertLevelDAO.class);
    
    public boolean save(String registrationId, String email, Status status) {
        if (StringUtils.isEmpty(registrationId) || StringUtils.isEmpty(email) || status == null) {
            LOG.warn("Registration ID \"" + registrationId + "\", email \"" + email + "\" and status \"" + status + "\" are all required");
            return false;
        }
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Date now = new Date();
        
        Filter filter = new FilterPredicate(REGISTRATION_ID, FilterOperator.EQUAL, registrationId);
        Query query = new Query(ENTITY_NAME).setFilter(filter);
        PreparedQuery pq = datastore.prepare(query);
        Entity alertLevelEntity = pq.asSingleEntity();
        
        boolean create = false;
        if (alertLevelEntity == null) {
            Key alertLevelKey = KeyFactory.createKey(ENTITY_NAME, REGISTRATION_ID);
            alertLevelEntity = new Entity(ENTITY_NAME, alertLevelKey);
            alertLevelEntity.setProperty("created", now);
            create = true;
        }
        alertLevelEntity.setProperty(REGISTRATION_ID, registrationId);
        alertLevelEntity.setProperty(EMAIL, email);
        alertLevelEntity.setProperty(STATUS, status.id());
        alertLevelEntity.setProperty("updated", now);
        datastore.put(alertLevelEntity);
        return create;
    }
    
    public Iterable<Entity> getWithStatusGreaterThanOrEqualTo(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("status must be provided");
        }
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        
        Filter statusFilter = new FilterPredicate("status",
                FilterOperator.GREATER_THAN_OR_EQUAL, status.id());
        Query query = new Query(ENTITY_NAME).setFilter(statusFilter);
        PreparedQuery pq = datastore.prepare(query);
        
        Iterable<Entity> results = pq.asIterable();
        return results;
    }
    
    public Entity getWithEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email must be provided");
        }
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Filter emailFilter = new FilterPredicate("email",
                FilterOperator.EQUAL, email);
        Query query = new Query(ENTITY_NAME).setFilter(emailFilter);
        PreparedQuery pq = datastore.prepare(query);
        
        Iterator<Entity> results = pq.asIterator();

        LOG.info("getWithEmail found result? " + results.hasNext());

        return results.hasNext() ? results.next() : null;
    }

}

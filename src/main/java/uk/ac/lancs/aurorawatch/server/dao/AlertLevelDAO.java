package uk.ac.lancs.aurorawatch.server.dao;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import uk.ac.lancs.aurorawatch.server.Status;

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
    
    static final String ENTITY_NAME = "AlertLevel";
    static final String STATUS = "status";
    static final String TOKEN = "token";
    
    private static final Logger LOG = LoggerFactory.getLogger(AlertLevelDAO.class);
    
    public boolean save(String token, Status status) {
        if (StringUtils.isEmpty(token) || status == null) {
            LOG.warn("Token \"" + token + "\" and level \"" + "\" are both required");
            return false;
        }
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Date now = new Date();
        
        Filter filter = new FilterPredicate(TOKEN, FilterOperator.EQUAL, token);
        Query query = new Query(ENTITY_NAME).setFilter(filter);
        PreparedQuery pq = datastore.prepare(query);
        Entity alertLevelEntity = pq.asSingleEntity();
        
        boolean create = false;
        if (alertLevelEntity == null) {
            Key alertLevelKey = KeyFactory.createKey(ENTITY_NAME, token);
            alertLevelEntity = new Entity(ENTITY_NAME, alertLevelKey);
            alertLevelEntity.setProperty("created", now);
            create = true;
        }
        
        alertLevelEntity.setProperty(STATUS, status.name());
        alertLevelEntity.setProperty(TOKEN, token);
        alertLevelEntity.setProperty("updated", now);
        datastore.put(alertLevelEntity);
        return create;
    }
    
    public Iterable<Entity> getAll() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        
        Query query = new Query(ENTITY_NAME);
        PreparedQuery pq = datastore.prepare(query);
        Iterable<Entity> results = pq.asIterable();
        return results;
    }

}

package org.aurorawatchdevs.server.web;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.aurorawatchdevs.server.Status;
import org.aurorawatchdevs.server.dao.AlertLevelDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.appengine.api.datastore.Entity;

@Controller
public class UserPreferencesController {
    
    private static final Logger LOG = LoggerFactory.getLogger(UserPreferencesController.class);
    
    @Autowired
    private AlertLevelDAO dao;
    
    private void logAndWrite(Writer writer, String msg) throws IOException {
        LOG.error(msg);
        writer.write(msg);
        return;
    }

    @RequestMapping(value="/saveAlertLevel", method = RequestMethod.GET)
    public void setAlertLevel(@RequestParam String token, @RequestParam String level, Writer responseWriter) 
            throws IOException {
        
        LOG.debug("setAlertLevel: token " + token + ", level " + level);
        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(level)) {
            logAndWrite(responseWriter, "Token \"" + token + "\" and level \"" + level + "\" are both required");
            return;
        }
        
        Status status = Status.fromString(level);
        if (status == null) {
            logAndWrite(responseWriter, "Alert level \"" + level + "\" is invalid");
            return;
        }
        
        dao.save(token, status);
    }
    
    @RequestMapping(value="/listAlertLevels", method = RequestMethod.GET)
    public void listAlertLevels(Writer responseWriter) 
            throws IOException {
        
        LOG.debug("listAlertLevels");
        Iterable<Entity> alertLevels = dao.getAll();
        Iterator<Entity> it = alertLevels.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();
            responseWriter.write("<p>");
            responseWriter.write(entity.getProperty("created").toString());
            responseWriter.write(" ");
            responseWriter.write(entity.getProperty("status").toString());
            responseWriter.write(" ");
            responseWriter.write(entity.getProperty("token").toString());
            responseWriter.write(" ");
            responseWriter.write("</p>");
        }
    }
}

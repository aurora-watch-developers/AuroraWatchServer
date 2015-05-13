package org.aurorawatchdevs.server.web;

import java.io.IOException;
import java.io.Writer;

import org.aurorawatchdevs.server.Status;
import org.aurorawatchdevs.server.dao.AlertLevelDAO;
import org.aurorawatchdevs.server.service.TokenCheckerService;
import org.aurorawatchdevs.server.service.TokenCheckerService.TokenValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

@Controller
public class UserPreferencesController {
    
    private static final Logger LOG = LoggerFactory.getLogger(UserPreferencesController.class);
    
    @Autowired
    private AlertLevelDAO dao;
    
    @Autowired
    private TokenCheckerService tokenChecker;
    
    private void logAndWrite(Writer writer, String msg) throws IOException {
        LOG.error(msg);
        writer.write(msg);
        return;
    }
    
    @RequestMapping(value="/saveAlertLevel", method = RequestMethod.GET)
    public void setAlertLevelDebug(@RequestParam String token, @RequestParam String registrationId, @RequestParam String level, Writer responseWriter) 
            throws IOException {
        
        setAlertLevel(token, registrationId, level, responseWriter);
    }

    @RequestMapping(value="/saveAlertLevel", method = RequestMethod.POST)
    public void setAlertLevel(@RequestParam String token, @RequestParam String registrationId, @RequestParam String level, Writer responseWriter) 
            throws IOException {
        
        LOG.debug("setAlertLevel: token " + token + ", level " + level);
        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(level)) {
            logAndWrite(responseWriter, "Token \"" + token + "\" and level \"" + level + "\" are both required");
            return;
        }
        
        GoogleIdToken.Payload payload;
        try {
            payload = tokenChecker.validate(token);
        } catch (TokenValidationException e) {
            logAndWrite(responseWriter, "Token \"" + token + "\" is invalid: " + e.toString());
            return;
        }
        
        Status status = Status.fromString(level);
        if (status == null) {
            logAndWrite(responseWriter, "Alert level \"" + level + "\" is invalid");
            return;
        }
        
        dao.save(registrationId, payload.getEmail(), status);
    }
}

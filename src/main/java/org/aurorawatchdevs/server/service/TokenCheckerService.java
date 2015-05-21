package org.aurorawatchdevs.server.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class TokenCheckerService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenCheckerService.class);

    @Value("${tokenChecker.aud}")
    private String aud;

    @Value("${tokenChecker.azp}")
    private String[] azp;
    
    private JsonFactory jsonFactory = new GsonFactory();
    private GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier(new NetHttpTransport(), jsonFactory);
    
    public GoogleIdToken.Payload validate(String tokenString) throws IOException, TokenValidationException {
        
        
        try {
            GoogleIdToken token = GoogleIdToken.parse(jsonFactory, tokenString);
            if (!verifier.verify(token)) {
                throw new TokenValidationException("Verification failed. (Time-out?)");
            }
                
            GoogleIdToken.Payload payload = token.getPayload();
            if (!payload.getAudience().equals(aud)) {
                throw new TokenValidationException("Payload audience " + payload.getAudience() + " doesn't match configured value aud");
            }
            boolean match = false;
            for (int i = 0; !match && i < azp.length; i++) {
                if(payload.getAuthorizedParty().equals(azp[i])) {
                    match = true;
                }
            }
            if (!match) {
                throw new TokenValidationException("Payload authorized party " + payload.getAuthorizedParty() + " doesn't match configured value azp");
            }
            return payload;
            
        } catch (IllegalArgumentException e) {
            LOG.warn(e.toString(), e);
            throw new TokenValidationException("Error parsing token: " + e);

        } catch (GeneralSecurityException e) {
            LOG.warn(e.toString(), e);
            throw new TokenValidationException("Security issue: " + e);
        }
    }
    
    public static class TokenValidationException extends Exception {

        private static final long serialVersionUID = 5208926627831212292L;

        public TokenValidationException(String message) {
            super(message);
        }
    }

}

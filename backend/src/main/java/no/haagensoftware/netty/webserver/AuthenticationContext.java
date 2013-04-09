package no.haagensoftware.netty.webserver;

import java.util.Hashtable;
import java.util.UUID;

import org.apache.log4j.Logger;

import no.haagensoftware.auth.MozillaPersonaCredentials;
import no.haagensoftware.auth.NewUser;
import no.haagensoftware.perst.PerstDBEnv;
import no.haagensoftware.perst.dao.PerstUserDao;
import no.haagensoftware.perst.datatypes.PerstUser;

public class AuthenticationContext {
	private Logger logger = Logger.getLogger(AuthenticationContext.class.getName());
	
	private PerstDBEnv dbEnv;
	private Hashtable<String, MozillaPersonaCredentials> authenticatedUsers;
	private PerstUserDao userDao;
	
	public AuthenticationContext(PerstDBEnv dbEnv) {
		this.dbEnv = dbEnv;
		authenticatedUsers = new Hashtable<>();
		this.userDao = new PerstUserDao();
	}
	
	public AuthenticationResult verifyUUidToken(String uuidToken) {
		AuthenticationResult authResult = new AuthenticationResult();
		MozillaPersonaCredentials credentials = authenticatedUsers.get(uuidToken);
		if (credentials != null && credentials.getStatus().equalsIgnoreCase("okay")) {
			authResult.setUuidToken(uuidToken);
			authResult.setUuidValidated(true);
		} else {
			authResult.setUuidToken(null);
			authResult.setUuidValidated(false);
			authResult.setStatusMessage("User not already logged in");
		}
		
		return authResult;
	}
	
	public MozillaPersonaCredentials getAuthenticatedUser(String uuidToken) {
		return authenticatedUsers.get(uuidToken);
	}
	
	public AuthenticationResult verifyAndGetUser(MozillaPersonaCredentials credentials) {
		AuthenticationResult authResult = new AuthenticationResult();
		if (credentials != null && credentials.getStatus().equalsIgnoreCase("okay")) {
			PerstUser user = getUser(credentials.getEmail());
			String uuid = UUID.randomUUID().toString();
			if (user != null) {
	    		authenticatedUsers.put(uuid, credentials);
	    		authResult.setUuidToken(uuid);
	    		authResult.setUuidValidated(true);
			} else {
				authResult.setUuidToken(uuid);
	    		authResult.setUuidValidated(false);
	    		authResult.setStatusMessage("User not registered");
			}
		} else {
			authResult.setUuidValidated(false);
			authResult.setUuidToken(null);
			authResult.setStatusMessage("User not authenticated");
		}
		
		return authResult;
	}
	
	public boolean logUserOut(String uuidToken) {
		boolean loggedOut = false;
		
		for (String uuid : authenticatedUsers.keySet()) {
			logger.info("UUID: " + uuid);
		}
		logger.info("logging out user: " + uuidToken + " :: " + authenticatedUsers.contains(uuidToken));
		authenticatedUsers.remove(uuidToken);
		loggedOut = true;
		
		return loggedOut;
	}
	
	public boolean userIsNew(String email) {
		return getUser(email) == null;
	}
	
	public boolean registerNewUser(NewUser user) {
		PerstUser newUser = new PerstUser();
		newUser.setUserId(user.getEmail());
		newUser.setFirstName(user.getFirstName());
		newUser.setLastName(user.getLastName());
		newUser.setUserLevel("user");
		newUser.setHomeCountry(user.getHomeCountry());
		
		this.userDao.persistUser(dbEnv, newUser);
		
		return true;
	}
	
	public PerstUser getUser(String email) {
		return this.userDao.getUser(dbEnv, email);
	}
}
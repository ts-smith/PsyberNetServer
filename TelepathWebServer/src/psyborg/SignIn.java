package psyborg;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class SignIn extends HttpServlet{

	public void doPost(HttpServletRequest req, HttpServletResponse resp){

		String requestedAccount = req.getParameter("accountName");
		String password = req.getParameter("password");

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Key accountKey = KeyFactory.createKey("AccountKey", requestedAccount);
		Query accountQuery = new Query("Account", accountKey);

		Entity accountEntity = datastore.prepare(accountQuery).asSingleEntity();


		if (accountEntity!=null){
			String hashedPassword = (String) accountEntity.getProperty("passwordHash");



			if(password.equals(hashedPassword)){		

				MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
				syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

				String passwordCacheKey = requestedAccount+"-PW";
				String compareValue = hashedPassword;
				Expiration expires = Expiration.byDeltaSeconds(86400);

				syncCache.put(passwordCacheKey, compareValue, expires);

				try {
					resp.getWriter().print("success");
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			else{
				try {
					resp.getWriter().print("No Match     "+ password+" , "+hashedPassword);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else{
			try {
				resp.getWriter().print("No Matching Account, remove this and line above when launching for real");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

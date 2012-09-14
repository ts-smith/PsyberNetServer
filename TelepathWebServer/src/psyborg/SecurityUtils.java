package psyborg;

import java.math.BigInteger;
import java.util.regex.Pattern;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;

public class SecurityUtils {
	static String bin2hex(byte[] data) {
		  return String.format("%0" + (data.length * 2) + 'x', new BigInteger(1, data));
		}
	
	static Pattern p = Pattern.compile("[a-zA-Z0-9 _-]+");
	
	static Pattern anonCode = Pattern.compile("[a-zA-Z0-9]+");
	
	public static boolean checkAndCachePassword(String requester, String passwordHash, MemcacheService syncCache, DatastoreService datastore){
		if (passwordHash != null){

			String storedPasswordHash = "nope";

			String cachedPassword = ((String) syncCache.get(requester+"-PW"));

			boolean notCached = false;
			if(cachedPassword!=null){
				storedPasswordHash = cachedPassword;
			}
			else{
				notCached = true;

				Key accountKey = KeyFactory.createKey("AccountKey", requester);
				Query accountQuery = new Query("Account", accountKey);

				Entity accountEntity = datastore.prepare(accountQuery).asSingleEntity();

				if(accountEntity != null){
					storedPasswordHash = (String) accountEntity.getProperty("passwordHash");	
				}
			}
			if (passwordHash.equals(storedPasswordHash)){

				if (notCached){
					Expiration expiration = Expiration.byDeltaSeconds(86400);
					syncCache.put(requester+"-PW", storedPasswordHash, expiration);
				}

				return true;
			}
		}
		return false;
	}
}

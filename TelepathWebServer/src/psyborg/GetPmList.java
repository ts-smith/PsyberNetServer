package psyborg;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.gson.Gson;

/*
 * gets a list of all the people the user has sent pms to for dispatch
 */

public class GetPmList extends HttpServlet{

	Gson gson = new Gson();

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp){

		String user = req.getParameter("user");
		String passwordHash = req.getParameter("password");

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();

		boolean valid = SecurityUtils.checkAndCachePassword(user, passwordHash, syncCache, datastore);

		if (valid){
			Key pmKey = KeyFactory.createKey("pmList", user);

			Entity pmList;
			try {
				pmList = datastore.get(pmKey);
			} catch (EntityNotFoundException e) {
				pmList = null;
			}

			if (pmList != null){
				HashSet<String> set = new HashSet<String>(pmList.getProperties().keySet());

				set.remove("!lastUpdate");

				//will create standard collection
				String jsonPmList = gson.toJson(set);

				try {
					resp.getWriter().write(jsonPmList);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			else{
				try {
					resp.getWriter().write("[]");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
}
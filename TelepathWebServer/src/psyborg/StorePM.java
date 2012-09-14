package psyborg;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;

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
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class StorePM extends HttpServlet {
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp){

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();

		String sender = req.getParameter("sender");
		String receiver = req.getParameter("receiver");

		String content = req.getParameter("message");

		if(content.length()<500){



			//first validate password

			String passwordHash = req.getParameter("password");
			
			boolean valid = SecurityUtils.checkAndCachePassword(sender, passwordHash, syncCache, datastore);

			if (valid){

				//update sender pmList with potentially new contact and date //? why date, for checking updates?

				Key pmListKey = KeyFactory.createKey("pmList", sender);

				Entity pmList;

				try {
					pmList = datastore.get(pmListKey);
				} catch (EntityNotFoundException e) {
					pmList = new Entity("pmList", sender);
				}

				Date sent = new Date();
				
				pmList.setProperty(receiver, sent);

				datastore.put(pmList);

				//---

				//make receiver pmList have fresh global-update and contact properties.
				Key receiverPmListKey = KeyFactory.createKey("pmList", receiver);

				Entity receiverList;
				try {
					receiverList = datastore.get(receiverPmListKey);
				} catch (EntityNotFoundException e) {
					receiverList = new Entity("pmList", receiver);
				}

				receiverList.setProperty(sender, sent);
				//using character(!) that is not allowed in names for property
				receiverList.setProperty("!lastUpdate", sent);

				datastore.put(receiverList);

				//---

				Entity message = new Entity("pmConversation");
				message.setProperty("sender", sender);
				message.setProperty("message", content);
				message.setProperty("date", sent);
				
				String pmKey = makePMKey(sender, receiver);
				message.setProperty("pmKey", pmKey);

				datastore.put(message);
				
				
				try {
					resp.getWriter().write("success");
				} catch (IOException e1) {
					e1.printStackTrace();
				}

	



			}
		}
	}

	private String makePMKey(String sender, String receiver){

		int compared = sender.compareTo(receiver);

		String key;
		if (compared < 0){
			key = sender+":"+receiver;
		}
		else{
			key = receiver+":"+sender;
		}
		return key;

	}
}

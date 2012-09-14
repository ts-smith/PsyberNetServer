package psyborg;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.gson.Gson;

public class WriteConversations extends HttpServlet{
	SimpleDateFormat compareFormat = new SimpleDateFormat("yyDDDHHmmss");
	Gson gson = new Gson();
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String desiredConversation = req.getParameter("region");
		long clientLastUpdate = Long.parseLong(req.getParameter("lastUpdate"));



		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		long lastUpdate;
		//check if update time is cached
		MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

		String updateCacheKey = desiredConversation+"-ConversationsUpdate"; 
		String cachedUpdateTime = (String) syncCache.get(updateCacheKey);
		
		if (cachedUpdateTime != null) {
			lastUpdate = Long.parseLong(cachedUpdateTime);
		}
		else {
			Key dateKey = KeyFactory.createKey("ConversationsUpdateKey", desiredConversation);
			Query dateQuery = new Query("ConversationsUpdate", dateKey);

			Entity conversationDate = datastore.prepare(dateQuery).asSingleEntity();
			String lastUpdateString = (String) conversationDate.getProperty("lastUpdate");

			lastUpdate = Long.parseLong(lastUpdateString);

		}

		if (clientLastUpdate<lastUpdate){
			String cachedConversationKey = desiredConversation+"-Conversations";
			Text cachedConversation = (Text) syncCache.get(cachedConversationKey);
			
			Text jsonText;
			
			if (cachedConversation != null){
				jsonText = cachedConversation;
				
			}
			else{
				
				Key conversationKey = KeyFactory.createKey("ConversationsKey", desiredConversation);
				Query conversationQuery = new Query("Conversations", conversationKey);

				Entity conversations = datastore.prepare(conversationQuery).asSingleEntity();

				//jsonContent should be an arrayList
				jsonText = (Text) conversations.getProperty("content");
				
			}
			
			

			String jsonString = jsonText.getValue();
			resp.getWriter().print(jsonString);

		}
	}
}

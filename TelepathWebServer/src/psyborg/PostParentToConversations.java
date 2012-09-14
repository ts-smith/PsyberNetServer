package psyborg;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.regex.Matcher;

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
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.apphosting.utils.config.AppYaml.Handler.Security;
import com.google.gson.Gson;

/*
 * Puts parent node at end of list
 */

public class PostParentToConversations extends HttpServlet{

	Gson gson = new Gson();
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//is this for web logs or debugging or both?
		syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

		String parent = req.getParameter("poster");



		boolean softValidity = false;

		if(parent.startsWith("Anon-")){
			String anonCode = parent.split("-")[1];

			if(anonCode.length()==5){
				Matcher anonMatch = SecurityUtils.anonCode.matcher(anonCode);
				if(anonMatch.matches()){
					softValidity = true;
				}
			}
		}
		else {
			String passwordHash = req.getParameter("password");

			softValidity = SecurityUtils.checkAndCachePassword(parent, passwordHash, syncCache, datastore);

		}

		if (softValidity){

			String regionReq = req.getParameter("region");

			Key conversationKey = KeyFactory.createKey("ConversationsKey", regionReq);
			Query conversationQuery = new Query("Conversations", conversationKey);

			Entity conversationsEntity = datastore.prepare(conversationQuery).asSingleEntity();

			Key idKey = KeyFactory.createKey("ConversationsIdKey", regionReq);
			Query idQuery = new Query("ConversationsId", idKey);

			Entity idEntity = datastore.prepare(idQuery).asSingleEntity();

			String currentIdString = (String) idEntity.getProperty("conversationId");

			int currentId = Integer.parseInt(currentIdString);
			currentId++;
			if(currentId>3000){
				currentId=0;
			}
			
			idEntity.setProperty("conversationId", String.valueOf(currentId));
			datastore.put(idEntity);


			String time = req.getParameter("date");
			String topic = req.getParameter("topic");
			String content = req.getParameter("content");
			String idNum = currentIdString;
			ArrayList replies = new ArrayList();

			ArrayList<Object> newParent = new ArrayList<Object>(6);
			//order is important
			newParent.add(parent);
			newParent.add(time);
			newParent.add(topic);
			newParent.add(content);
			newParent.add(idNum);
			newParent.add(replies);


			//this should not be null, but who knows
			if(conversationsEntity != null){

				Text conversationsText = (Text) conversationsEntity.getProperty("content");
				String jsonConversations = conversationsText.getValue();
				ArrayList<Object> conversations = gson.fromJson(jsonConversations, ArrayList.class);

				if (conversations != null){
					conversations.add(newParent);
				}
				else{
					conversations=new ArrayList<Object>();
					conversations.add(newParent);
				}

				String newJsonConversations = gson.toJson(conversations);

				Text contentText = new Text(newJsonConversations);

				conversationsEntity.setProperty("content", contentText);
				datastore.put(conversationsEntity);

				//put conversation in memcached
				String conversationsCacheKey = regionReq+"-Conversations";
				syncCache.put(conversationsCacheKey, contentText);

				//---

				Key updateKey = KeyFactory.createKey("ConversationsUpdateKey", regionReq);
				Query updateQuery = new Query("ConversationsUpdate", updateKey);

				Entity updateEntity = datastore.prepare(updateQuery).asSingleEntity();

				SimpleDateFormat compareFormat = new SimpleDateFormat("yyDDDHHmmss");
				String updated = compareFormat.format(new Date());

				updateEntity.setProperty("lastUpdate", updated);

				datastore.put(updateEntity);

				//may want some sort of response

				// Memcached
				String updateCacheKey = regionReq + "-ConversationsUpdate";

				syncCache.put(updateCacheKey, updated);
			}		
		}
		else if (!parent.startsWith("Anon-")){
			//should only be called in a hacking attempt
			resp.getWriter().print("gooby pls");
		}
	}
}



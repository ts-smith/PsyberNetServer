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
import com.google.gson.Gson;

public class PostReplyToConversations extends HttpServlet{

	Gson gson = new Gson();
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();



	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

		String poster = req.getParameter("poster");

		boolean softValidity = false;

		if(poster.startsWith("Anon-")){
			String anonCode = poster.split("-")[1];

			if(anonCode.length()==5){
				Matcher anonMatch = SecurityUtils.anonCode.matcher(anonCode);
				if(anonMatch.matches()){
					softValidity = true;
				}
			}
		}
		else {
			String passwordHash = req.getParameter("password");
			
			softValidity = SecurityUtils.checkAndCachePassword(poster, passwordHash, syncCache, datastore);
		}




		if (softValidity){


			String regionReq = req.getParameter("region");



			Key conversationKey = KeyFactory.createKey("ConversationsKey", regionReq);
			Query conversationQuery = new Query("Conversations", conversationKey);

			Entity conversationsEntity = datastore.prepare(conversationQuery).asSingleEntity();



			String idNumString = req.getParameter("idNum");
			String content = req.getParameter("content");
			String date = req.getParameter("date");



			if(conversationsEntity != null){
				
				Text contentText = (Text) conversationsEntity.getProperty("content");
				String conversationsContent = contentText.getValue();

				if (conversationsContent != null){

					ArrayList<ArrayList> conversations = gson.fromJson(conversationsContent, ArrayList.class);

					int idNum = Integer.parseInt(idNumString);

					for (int i = 0; i < conversations.size(); i++) {
						ArrayList<Object> conversation = conversations.get(i);

						//if it is the right conversation
						if (idNum == Integer.parseInt((String)conversation.get(4))){
							//get the reply array
							ArrayList replies = (ArrayList)conversation.get(5);

							ArrayList reply = new ArrayList(3);
							reply.add(poster);
							reply.add(content);
							reply.add(date);

							replies.add(reply);

							Text newText = new Text(gson.toJson(conversations));
							conversationsEntity.setProperty("content", newText);
							datastore.put(conversationsEntity);

							String conversationsCacheKey = regionReq+"-Conversations"; 
							syncCache.put(conversationsCacheKey, newText);

						}
					}
				}


				Key dateKey = KeyFactory.createKey("ConversationsUpdateKey", regionReq);
				Query dateQuery = new Query("ConversationsUpdate", dateKey);

				Entity conversationDate = datastore.prepare(dateQuery).asSingleEntity();

				SimpleDateFormat compareFormat = new SimpleDateFormat("yyDDDHHmmss");
				String updated = compareFormat.format(new Date());

				conversationDate.setProperty("lastUpdate", updated);

				datastore.put(conversationDate);

				
				String updateCacheKey = regionReq + "-ConversationsUpdate";

				syncCache.put(updateCacheKey, updated);
			}		
		}
		else if(!poster.startsWith("Anon-")){
			//should only be called in a hacking attempt
			resp.getWriter().print("gooby pls");
		}
	}
}

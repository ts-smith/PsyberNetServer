package psyborg;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import com.google.gson.Gson;

/* allows manual editing of region content, useful despite google app engine tool for keeping caches up to date
 * 
 */

public class ContentEditor extends HttpServlet{
	SimpleDateFormat compareFormat = new SimpleDateFormat("yyDDDHHmmss");
	Gson gson = new Gson();
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//not sure if spaces replaced with underscores at this point
		String desiredConversation = req.getParameter("region");
		//Definitely want to do caching somewhere around here;

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		//this would be one of the places to use a caching system when possible;   around here at least
		Key dateKey = KeyFactory.createKey("ConversationsUpdateKey", desiredConversation);
		Query dateQuery = new Query("ConversationsUpdate", dateKey);

		Entity conversationDate = datastore.prepare(dateQuery).asSingleEntity();
		String lastUpdateString = (String) conversationDate.getProperty("lastUpdate");

		long lastUpdate = Long.parseLong(lastUpdateString);



		Key conversationKey = KeyFactory.createKey("ConversationsKey", desiredConversation);
		Query conversationQuery = new Query("Conversations", conversationKey);

		Entity conversations = datastore.prepare(conversationQuery).asSingleEntity();

		//jsonContent should be an arrayList
		Text jsonText = (Text) conversations.getProperty("content");
		String jsonContent = jsonText.getValue();

		String html = " <html> " +
				"<body>" +
				"<form method=\"post\">" +
				"<TEXTAREA NAME=\"content\" ROWS=\"10\" COLS=\"60\">"+

				jsonContent +
			    
			    "</TEXTAREA>" +
			    
                 "<input type=\"hidden\" name=\"region\" value=\" "+ desiredConversation + "\"/> "+    // <-- "region"
				
				
				"<div><input type=\"submit\" value=\"Return\" /></div>" +
				"</form>" +
				"</body>" +
				"</html>";
		 
		resp.getWriter().print(html);

	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		String regionReq = req.getParameter("region");

		Key conversationKey = KeyFactory.createKey("ConversationsKey", regionReq);
		Query conversationQuery = new Query("Conversations", conversationKey);

		Entity conversationsEntity = datastore.prepare(conversationQuery).asSingleEntity();
		
		
		//don't know about how data types are move around datastore

		
		String content = req.getParameter("content");
		Text contentText = new Text(content);
		//this should not be null, but who knows
		if(conversationsEntity != null){
			
			
			conversationsEntity.setProperty("content", contentText);
			datastore.put(conversationsEntity);
			
			Key updateKey = KeyFactory.createKey("ConversationsUpdateKey", regionReq);
			Query updateQuery = new Query("ConversationsUpdate", updateKey);
			
			Entity updateEntity = datastore.prepare(updateQuery).asSingleEntity();
			
			SimpleDateFormat compareFormat = new SimpleDateFormat("yyDDDHHmmss");
			String updated = compareFormat.format(new Date());
			
			updateEntity.setProperty("lastUpdate", updated);
			
			datastore.put(updateEntity);
			
			//may want some sort of response
		}
	}
}

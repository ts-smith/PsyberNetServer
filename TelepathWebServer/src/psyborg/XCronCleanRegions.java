package psyborg;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

public class XCronCleanRegions extends HttpServlet{
	//private static final Logger log = Logger.getLogger(FindCandidateRegions.class.getName());

	Gson gson = new Gson();
	ImmutableMap<String, Integer> days = ImmutableMap.<String,Integer>builder()
			.put("Sun", 1).put("Mon", 2)
			.put("Tue", 3).put("Wed", 4)
			.put("Thu", 5).put("Fri", 6)
			.put("Sat", 7).build();
	SimpleDateFormat compareDay = new SimpleDateFormat("E");



	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		compareDay.setTimeZone(TimeZone.getTimeZone("UTC"));
		int currentDayNum = days.get(compareDay.format(new Date()));		

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

		Key regionKeyKey = KeyFactory.createKey("RegionKeysKey", "Key");
		Query keyQuery = new Query("RegionKeys", regionKeyKey);


		List<Entity> regionKeys =  datastore.prepare(keyQuery).asList(FetchOptions.Builder.withDefaults());

		for (Entity regionKey : regionKeys){
			String key = (String) regionKey.getProperty("Key");

			Key conversationsKey = KeyFactory.createKey("ConversationsKey", key);
			Query conversationsQuery = new Query("Conversations", conversationsKey);

			Entity conversations = datastore.prepare(conversationsQuery).asSingleEntity();


			String jsonConversations = ((Text) conversations.getProperty("content")).getValue();

			ArrayList<ArrayList> conversationsArray = gson.fromJson(jsonConversations, ArrayList.class);


			//not sure if good idea, but practicing using linked lists
			LinkedList<ArrayList> linkedConversation = new LinkedList<ArrayList>(conversationsArray);

			StopIteration:
				for (Iterator iterator = linkedConversation.iterator(); iterator.hasNext();) {
					ArrayList parentRow = (ArrayList) iterator.next();

					String conversationStartDay = ((String)parentRow.get(1)).substring(0, 3);

					int startNum= days.get(conversationStartDay);

					int tempCurrentDay = currentDayNum;
					
					if (tempCurrentDay<startNum){
						tempCurrentDay+=7;
					}
					int difference = tempCurrentDay - startNum;

					if (difference > 3 ){
						iterator.remove();
						resp.getWriter().write("removed"+difference+":"+tempCurrentDay+":"+startNum);
					}
					else{
						//since things are ordered sequentially, any failure will mean it is done
						break StopIteration;
					}
				}

			//this works because toJson will strip  any list implementation details away,
			//and the resulting json will be convertible directly into an arrayList
			Text cleanedJsonText = new Text(gson.toJson(linkedConversation)); 

			conversations.setProperty("content", cleanedJsonText);
			datastore.put(conversations);

			syncCache.put(key+"-Conversations", cleanedJsonText);

			//---

			SimpleDateFormat standardCompareFormat = new SimpleDateFormat("yyDDDHHmmss");
			String updated = standardCompareFormat.format(new Date());

			Key updateKey = KeyFactory.createKey("ConversationsUpdateKey", key);
			Query updateQuery = new Query("ConversationsUpdate", updateKey);

			Entity updateEntity = datastore.prepare(updateQuery).asSingleEntity();

			updateEntity.setProperty("lastUpdate", updated);

			datastore.put(updateEntity);

			//---

			String updateCacheKey = key + "-ConversationsUpdate";

			syncCache.put(updateCacheKey, updated);

			resp.getWriter().write("completed");
		}
	}
}

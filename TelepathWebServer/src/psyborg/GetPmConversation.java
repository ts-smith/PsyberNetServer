package psyborg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.gson.Gson;
/*
 * gets 10 pmConversation items sorted by date
 */

public class GetPmConversation extends HttpServlet{

	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
	Gson gson = new Gson();

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp){

		String requester = req.getParameter("requester");
		String target = req.getParameter("target");

		String passwordHash = req.getParameter("password");

		boolean valid = SecurityUtils.checkAndCachePassword(requester, passwordHash, syncCache, datastore);

		if (valid){
			String pmKey = makePMKey(requester, target);


			Query query = new Query("pmConversation");
			query.setFilter(new FilterPredicate("pmKey", Query.FilterOperator.EQUAL, pmKey)).addSort("date", SortDirection.DESCENDING);

			String offsetString = req.getParameter("offset");
			int offset;
			if (offsetString == null){
				offset = 0;
			}
			else{
				offset = 10 * Integer.parseInt(offsetString);
			}
			
			
			List<Entity> pmMessageEntities = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10).offset(offset));
			
			
			
			
			if (pmMessageEntities.size()==0){
				try {
					resp.getWriter().write("inValid");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			ArrayList<ArrayList> jsonBuilder = new ArrayList(pmMessageEntities.size());
			
			for (Entity pmEntity : pmMessageEntities){
				ArrayList<String> jsonBuilderItem = new ArrayList(3);
				
				jsonBuilderItem.add((String) pmEntity.getProperty("sender"));
				jsonBuilderItem.add((String) pmEntity.getProperty("message"));
				jsonBuilderItem.add(pmEntity.getProperty("date").toString());
				
				jsonBuilder.add(jsonBuilderItem);
			}
			
			String jsonConversation = gson.toJson(jsonBuilder);
			
			try {
				resp.getWriter().print(jsonConversation);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		else{
			try {
				resp.getWriter().write("iHere");
			} catch (IOException e) {
				e.printStackTrace();
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

package psyborg;

import java.io.IOException;
import java.util.ArrayList;

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

public class WriteOverview extends HttpServlet{
	//private static final Logger log = Logger.getLogger(FindCandidateRegions.class.getName());

	Gson gson = new Gson();

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String regionReq = req.getParameter("region");

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Key conversationKey = KeyFactory.createKey("ConversationsKey", regionReq);
		Query conversationQuery = new Query("Conversations", conversationKey);//why isn't this sorted by descending? may never know

		Key bulletinKey = KeyFactory.createKey("BulletinsKey", regionReq);
		Query bulletinQuery = new Query("Bulletins", bulletinKey);//.addSort("date", descending); was taken off

		Entity conversations = datastore.prepare(conversationQuery).asSingleEntity();
		Entity bulletins = datastore.prepare(conversationQuery).asSingleEntity();

		String jsonOverView = getOverviewString(conversations, bulletins);

		resp.getWriter().print(jsonOverView);
	}
	String getOverviewString(Entity conversations, Entity bulletins){
		Overview overview = new Overview();

		if(conversations != null){
			Text conversationsText = (Text) conversations.getProperty("content");
			String conversationsContent = conversationsText.getValue();

			if (conversationsContent != null){
				ArrayList fromJson = gson.fromJson(conversationsContent, ArrayList.class);

				ArrayList recentFive = new ArrayList(5);

				if(fromJson.size()>5){
					for (int i = fromJson.size()-5;i<fromJson.size();i++){
						recentFive.add(fromJson.get(i));
					}
				}
				else{
					recentFive=fromJson;
				}
				overview.conversationsContent=recentFive;

			}
		}
		else{
			overview.conversationsContent=null;
		}

		if (bulletins != null){
			Text bulletinsText = (Text) bulletins.getProperty("content");
			String bulletinsContent = bulletinsText.getValue();

			if(bulletinsContent != null){
				ArrayList fromJson = gson.fromJson(bulletinsContent, ArrayList.class);

				ArrayList recentFive = new ArrayList(5);

				if(fromJson.size()>5){
					for (int i = fromJson.size()-5;i<fromJson.size();i++){
						recentFive.add(fromJson.get(i));
					}
				}
				else{
					recentFive=fromJson;
				}
				overview.bulletinsContent=recentFive;
			}
		}
		else{
			overview.bulletinsContent=null;
		}
		return gson.toJson(overview, Overview.class);
	}
}

package psyborg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

/*
 * tool for creating new regions, used in conjunction with *sigh* createRegionGoats.jsp
 */

public class RegionCreator extends HttpServlet {
	Gson gson = new Gson();
	
	private static final Logger log = Logger.getLogger(RegionCreator.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException{

		String regionName = req.getParameter("regionName");
		String xString = req.getParameter("xString");
		String yString = req.getParameter("yString");
		
		List<String> hashKeys = getHashKeys(xString, yString);
		
		Region region = new Region(regionName, stringToFloatArray(xString), stringToFloatArray(yString));
		String jsonRegion = gson.toJson(region);
		
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		
		String noSpaceRegion = regionName.replace(" ", "_");
		Text emptyContent = new Text("[]");
		
		Key conversationKey = KeyFactory.createKey("ConversationsKey", noSpaceRegion);
		Entity generalConversation = new Entity("Conversations", conversationKey);
		
		generalConversation.setProperty("content", emptyContent);
		
		Key bulletinKey = KeyFactory.createKey("BulletinsKey", noSpaceRegion);
		Entity bulletin = new Entity("Bulletins", bulletinKey);
		bulletin.setProperty("content", emptyContent);
		
		Key conversationUpdateKey = KeyFactory.createKey("ConversationsUpdateKey", noSpaceRegion);
		Entity conversationUpdate = new Entity("ConversationsUpdate", conversationUpdateKey);
		conversationUpdate.setProperty("lastUpdate", "0");
		
		Key conversationIdKey = KeyFactory.createKey("ConversationsIdKey", noSpaceRegion);
		Entity conversationId = new Entity("ConversationsId", conversationIdKey);
		conversationId.setProperty("conversationId", "0");
		
		//not sure if necessary
		Key bulletinUpdateKey = KeyFactory.createKey("BulletinsUpdateKey", noSpaceRegion);
		Entity bulletinUpdate = new Entity("BulletinsUpdate", bulletinUpdateKey);
		bulletinUpdate.setProperty("lastUpdate", "0");
		
		Key bulletinIdKey = KeyFactory.createKey("BulletinsIdKey", noSpaceRegion);
		Entity bulletinId = new Entity("BulletinsId", bulletinIdKey);
		bulletinId.setProperty("bulletinId", "0");
		
		
		datastore.put(generalConversation);
		datastore.put(conversationUpdate);
		datastore.put(conversationId);
		
		datastore.put(bulletin);
		datastore.put(bulletinUpdate);
		datastore.put(bulletinId);
		
		Key regionKeysKey = KeyFactory.createKey("RegionKeysKey", "Key");
		Entity regionKeyEntity = new Entity("RegionKeys", regionKeysKey);
		regionKeyEntity.setProperty("Key", noSpaceRegion);
		datastore.put(regionKeyEntity);
		
		
		for(String keyString:hashKeys){
			Key hashKey = KeyFactory.createKey("HashCellKey", keyString);
			Query query = new Query("HashCell", hashKey);
			Entity hashCell = datastore.prepare(query).asSingleEntity();
			
			
			
			if (hashCell == null){
				hashCell = new Entity("HashCell", hashKey);
				hashCell.setProperty("readableKey", keyString);
				
				
				
			}	
			
			hashCell.setProperty(regionName, jsonRegion);
			datastore.put(hashCell);
		}
		resp.getWriter().print(regionName+ "  |  "+jsonRegion);
	}
	ArrayList getHashKeys(String xString, String yString){
		
		String[] xRay = xString.split(",");
		int[] xInts = hashesForDimension(xRay);
		
		String[] yRay = yString.split(",");
		int[] yInts = hashesForDimension(yRay);
		
		ArrayList hashKeys = new ArrayList(yInts.length*xInts.length);
		for(Integer x:xInts){
			for(Integer y:yInts){
				hashKeys.add(x+":"+y);
			}
		}
		return hashKeys;
	}
	int[] hashesForDimension(String[] floats){
		ArrayList<Integer> holder = new ArrayList(floats.length);
		for (String floatString: floats){
			float floatVersion = Float.parseFloat(floatString);
			int intHash = (int) floatVersion/2;
			if (!holder.contains(intHash)){
				holder.add(intHash);
			}
		}
		holder = createRange(holder);
		int[] returnValue = new int[holder.size()];
		for(int i = 0;i<holder.size();i++){
			Integer converter =(Integer)holder.get(i);
			returnValue[i] = converter;
		}
		return returnValue;
	}
	ArrayList createRange(ArrayList<Integer> unorderedArray){
		ArrayList toReturn = new ArrayList();
		Integer bottom = 4000; //impossible values
		Integer top = -4000;
		for (Integer item: unorderedArray){
			if(item<bottom){
				bottom=item;
			}
			if (item>top){
				top=item;
			}
		}
		int usedRange = top - bottom;
		toReturn.add(bottom);
		for (int i = bottom+1;i<top;i++){
			toReturn.add(i);
		}
		toReturn.add(top);
		return toReturn;
		
	}
	float[] stringToFloatArray(String floats){
		String[] stringArray = floats.split(",");
		float[] floatArray = new float[stringArray.length];
		
		for (int i = 0;i<floatArray.length;i++){
			floatArray[i]=Float.valueOf(stringArray[i]);
		}
		return floatArray;
	}
}

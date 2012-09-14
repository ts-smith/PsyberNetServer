package psyborg;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

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
import com.google.appengine.tools.util.Logging;
import com.google.gson.Gson;

/*
 * find regions available for position
 */
public class FindCandidateRegions extends HttpServlet{
	//private static final Logger log = Logger.getLogger(FindCandidateRegions.class.getName());	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Gson gson = new Gson();
		String xCoord = req.getParameter("xCoord");
		String yCoord = req.getParameter("yCoord");
		
		if (xCoord != null && yCoord != null){
			float[] coords = {Float.valueOf(xCoord), Float.valueOf(yCoord)};
			Entity hashCell = findHashCell(coords);
			if (hashCell != null){
				RegionList regionList = new RegionList();
				
				
				//why the hell did I not just do get property readableKey?
				for (Entry<String, Object> entrySet: hashCell.getProperties().entrySet()){
					if(!(entrySet.getKey().equals("readableKey"))){
						String jsonRegion = (String) entrySet.getValue();
						Region regionObject = gson.fromJson((String) jsonRegion, Region.class);
						if (pointIsContained(coords, regionObject)){
							regionList.validRegions.add((String) jsonRegion);
						}
					}
				}
				String jsonRegionList = gson.toJson(regionList);
				resp.getWriter().println(jsonRegionList);
			}
		}
	}
	
	Entity findHashCell(float[] coords){
		String hashCellKeyCode = findHashCellKey(coords);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key hashCellKey = KeyFactory.createKey("HashCellKey", hashCellKeyCode);

		Query query = new Query("HashCell", hashCellKey);
		Entity hashCell = datastore.prepare(query).asSingleEntity();
		return hashCell;
	}
	String findHashCellKey(float[] coords){
		
		int x = (int) (coords[0] / 2); //2 is less arbitrary number than before, but still arbitrary
		int y = (int) (coords[1] / 2); //ditto ^
		String keyString = x+":"+y;
		return keyString;
	}
	static boolean pointIsContained(float[] point, Region region){
		//algorithm: http://alienryderflex.com/polygon/
		int polySides = region.xVertices.length;
		float[] polyX=region.xVertices;
		float[] polyY=region.yVertices;
		float x = point[0];
		float y = point[1];

		int i, j=polySides-1;
		boolean  oddNodes=false;

		for (i=0; i<polySides; i++) {
			if ((polyY[i]< y && polyY[j]>=y
					||   polyY[j]< y && polyY[i]>=y)
					&&  (polyX[i]<=x || polyX[j]<=x)
					&&  (polyX[i]<=x || polyX[j]<=x)){
				oddNodes^=(x > polyX[i]+(y-polyY[i])/(polyY[j]-polyY[i])*(polyX[j]-polyX[i])); 
				}
			j=i; 
			}
		return oddNodes; 
	}
}

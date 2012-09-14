package psyborg;

//don't exactly remember what this was for, was part of json within json structure or something. Madness
public class Region {
	String regionName;
	
	float[] xVertices;
	float[] yVertices;
	public Region(String regionName, float[] xVertices, float[] yVertices){
		this.regionName = regionName;
		this.xVertices = xVertices;
		this.yVertices = yVertices;
	}
	public Region(){
		
	}
}

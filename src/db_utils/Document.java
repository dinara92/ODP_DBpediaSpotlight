package db_utils;

public class Document {

	public Document(int id, int categoryId2, String url2, String title2, String description2) {
		// TODO Auto-generated constructor stub
		urlid = id;
		categoryId = categoryId2;
		url = url2;
		title = title2;
		description = description2;
		
	}
	

	
	public Document(int id, String title2, String description2) {
		// TODO Auto-generated constructor stub
		urlid = id;
		description = description2;
		title = title2;
	}
	
	int urlid;
	int categoryId;
	String url;
	String title;
	String description;

}

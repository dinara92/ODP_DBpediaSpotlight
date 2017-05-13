package external_clients;



import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db_utils.ODPDatabase;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple web service-based annotation client for DBpedia Spotlight.
 *
 * @author dinara
 */

public class DBpediaSpotlightClientDB extends AnnotationClientDB {


    private final static String API_URL = "http://172.17.0.2:80/";

	
    //private final static String API_URL = "http://spotlight.dbpedia.org/";

	private static final double CONFIDENCE = 0.4;
	private static final int SUPPORT = 0;

	@Override
	public HashMap<String, DBpediaResource> extract(Text text) throws AnnotationException {

        LOG.info("Querying API.");
		String spotlightResponse;
		try {
			GetMethod getMethod = new GetMethod(API_URL + "rest/annotate/?" +
					"confidence=" + CONFIDENCE
					+ "&support=" + SUPPORT
					+ "&text=" + URLEncoder.encode(text.text(), "utf-8"));
			getMethod.addRequestHeader(new Header("Accept", "application/json"));

			spotlightResponse = request(getMethod);
		} catch (UnsupportedEncodingException e) {
			throw new AnnotationException("Could not encode text.", e);
		}

		assert spotlightResponse != null;

		JSONObject resultJSON = null;
		JSONArray entities = null;

		try {
			resultJSON = new JSONObject(spotlightResponse);
			//System.out.println("All JSON " + resultJSON);
			
			entities = resultJSON.getJSONArray("Resources");
			//System.out.println("All entities " + resultJSON.getJSONArray("Resources"));


		} catch (JSONException e) {
			throw new AnnotationException("Received invalid response from DBpedia Spotlight API.");
		}

		//LinkedList<DBpediaResource> resources = new LinkedList<DBpediaResource>();
		HashMap<String, DBpediaResource> resourcesMap = new HashMap<String, DBpediaResource>();

		for(int i = 0; i < entities.length(); i++) {
			try {
				JSONObject entity = entities.getJSONObject(i);
				//System.out.println("Surface form " + entity.getString("@surfaceForm"));
				//System.out.println(entity.getString("@URI"));

				//resources.add(
						//new DBpediaResource(entity.getString("@URI"),Integer.parseInt(entity.getString("@support"))));
				resourcesMap.put(entity.getString("@surfaceForm"), new DBpediaResource(entity.getString("@URI"), Integer.parseInt(entity.getString("@support"))));
				
				

			} catch (JSONException e) {
                LOG.error("JSON exception "+e);
            }

		}


		return resourcesMap;
	}
	

	
        public static void main(String[] args) throws Exception {

        DBpediaSpotlightClientDB c = new DBpediaSpotlightClientDB ();


//        File input = new File("/home/dinara/java-projects/dbpedia_spotlight/dbpedia_spotlight_test_files/test_1.txt");
//        File output = new File("/home/dinara/java-projects/dbpedia_spotlight/dbpedia_spotlight_test_files/kkk.txt");

        
        c.evaluate();


//        SpotlightClient c = new SpotlightClient(api_key);
//        List<DBpediaResource> response = c.extract(new Text(text));
//        PrintWriter out = new PrintWriter(manualEvalDir+"AnnotationText-Spotlight.txt.set");
//        System.out.println(response);

    }



}
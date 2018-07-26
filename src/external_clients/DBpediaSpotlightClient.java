package external_clients;


/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Scanner;


/**
 * Simple web service-based annotation client for DBpedia Spotlight.
 *
 * @author pablomendes, Joachim Daiber
 */

public class DBpediaSpotlightClient extends AnnotationClient {

	//private final static String API_URL = "http://jodaiber.dyndns.org:2222/";
    //private final static String API_URL = "http://spotlight.sztaki.hu:2222/";
    //private final static String API_URL = "http://spotlight.sztaki.hu:2228/";
    private final static String API_URL = "http://172.17.0.2:80/";

	
    //private final static String API_URL = "http://spotlight.dbpedia.org/";

	//private static final double CONFIDENCE = 0.5; //0001 with this conf, rest - with 0.6
	private static final double CONFIDENCE = 0.6;

	private static final int SUPPORT = 0;

	public HashMap<String, DBpediaResource> extract(Text text) throws AnnotationException {

//        LOG.info("Querying API.");
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


        DBpediaSpotlightClient c = new DBpediaSpotlightClient ();
           
//        c.evaluate();
        
        
        Scanner sc = new Scanner(System.in);
        int start_doc = sc.nextInt();
        int end_doc = sc.nextInt();
        c.evaluate(start_doc, end_doc);

//        SpotlightClient c = new SpotlightClient(api_key);
//        List<DBpediaResource> response = c.extract(new Text(text));
//        PrintWriter out = new PrintWriter(manualEvalDir+"AnnotationText-Spotlight.txt.set");
//        System.out.println(response);

    }



}
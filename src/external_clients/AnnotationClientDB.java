package external_clients;


import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;

import db_utils.ODPDatabase;

import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * This class has been translate to scala. Please use the AnnotationClientScala.scala for new External Clients!
 * (AnnotationClientScala.scala is at eval/src/main/scala/org/dbpedia/spotlight/evaluation/external/)
 *
 * @author dinara
 */

public abstract class AnnotationClientDB {

    public Logger LOG = Logger.getLogger(this.getClass());
	static ODPDatabase ODP_DB = null;

    // Create an instance of HttpClient.
    private static HttpClient client = new HttpClient();


    public String request(HttpMethod method) throws AnnotationException {

        String response = null;

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        try {
            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                LOG.error("Method failed: " + method.getStatusLine());
            }

            // Read the response body.
            byte[] responseBody = method.getResponseBody(); //TODO Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            response = new String(responseBody);

        } catch (HttpException e) {
            LOG.error("Fatal protocol violation: " + e.getMessage());
            throw new AnnotationException("Protocol error executing HTTP request.",e);
        } catch (IOException e) {
            LOG.error("Fatal transport error: " + e.getMessage());
            LOG.error(method.getQueryString());
            throw new AnnotationException("Transport error executing HTTP request.",e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
        return response;

    }

    protected static String readFileAsString(String filePath) throws java.io.IOException{
        return readFileAsString(new File(filePath));
    }
    
    protected static String readFileAsString(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = new BufferedInputStream(new FileInputStream(file));
        f.read(buffer);
        return new String(buffer);
    }

    static abstract class LineParser {

        public abstract String parse(String s) throws ParseException;

        static class ManualDatasetLineParser extends LineParser {
            public String parse(String s) throws ParseException {
                return s.trim();
            }
        }

        static class OccTSVLineParser extends LineParser {
            public String parse(String s) throws ParseException {
                String result = s;
                try {
                    result = s.trim().split("\t")[3];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new ParseException(e.getMessage(), 3);
                }
                return result; 
            }
        }
    }

    public void saveExtractedEntitiesSet(HashSet<Integer> cid_hash, LineParser parser, int restartFrom) throws Exception {

        int i=0;
        int correct =0 ;
        int error = 0;
        int sum = 0;

        for (int j = 418868; j<3624445; j++){
        	if(!cid_hash.contains(ODP_DB.getCateg(j))){
            
        	String s = parser.parse(ODP_DB.getDoc(j));
            if (s!= null && !s.equals("")) {
                i++;

                if (i<restartFrom) continue;

                HashMap<String, DBpediaResource> entities = new HashMap<String, DBpediaResource>();

                try {
                    final long startTime = System.nanoTime();
                    entities = extract(new Text(ODP_DB.getDoc(j).replaceAll("\\s+"," ")));
                    
                    
                    final long endTime = System.nanoTime();
                    sum += endTime - startTime;
                    LOG.info(String.format("(%s) Extraction ran in %s ns.", i, endTime - startTime));
                    correct++;
                } catch (AnnotationException e) {
                    error++;
                    LOG.error(e);
                    e.printStackTrace();
                }

                
                System.out.println("\n");
                String db_doc = "";

                for(String snippet_word: ODP_DB.getDoc(j).split(" ")){
                	String snippet_word_upd = URLEncoder.encode(snippet_word.replaceAll("[^a-zA-Z]", ""), "UTF-8");
                	if(entities.containsKey(snippet_word_upd)){
                			//System.out.print(entities.get(snippet_word_upd).uri() + " ");
                		if(entities.get(snippet_word_upd).uri().contains("'") || entities.get(snippet_word_upd).uri().contains("\"")){
                			db_doc += snippet_word_upd + " " + URLEncoder.encode(entities.get(snippet_word_upd).uri(), "UTF-8") + " ";
                		}
                		else{
            			db_doc += snippet_word_upd + " " + entities.get(snippet_word_upd).uri() + "_ ";
                		}
                		//System.out.println(db_doc);

                	}
                	else{
                			//System.out.print(snippet_word_upd + " ");
                			db_doc += snippet_word_upd + " ";
                			//System.out.println(db_doc);
                	}
                }
                //System.out.println(j + " - " + db_doc);
                ODP_DB.putDoc(j, db_doc);
            }
        	}
        	//else{
        	//	System.out.println(j + " IS IN CATEGORY IN TOP/WORLD!!!");
        	//}
        }
        LOG.info(String.format("Extracted entities from %s text items, with %s successes and %s errors.", i, correct, error));
        double avg = (new Double(sum) / i);
        LOG.info(String.format("Average extraction time: %s ms", avg * 1000000));
    }

	public static HashSet<Integer> init() throws SQLException {
		if (ODP_DB == null) {
			ODP_DB = new ODPDatabase();
			ODP_DB.connectDB(ODP_DB.url);
			System.out.println("Connected to db..");
		}
		HashSet<Integer> cid_hash = ODP_DB.getNonWorldCateg();
		return cid_hash;
	
	}
    public void evaluate() throws Exception {
    	HashSet<Integer> cid_hash = init();
        evaluateManual(cid_hash, 0);
    }

    public void evaluateManual(HashSet<Integer> cid_hash, int restartFrom) throws Exception {
         saveExtractedEntitiesSet(cid_hash, new LineParser.ManualDatasetLineParser(), restartFrom);
    }


    /**
     * Entity extraction code.
     * @param text
     * @return
     */
    //public abstract List<DBpediaResource> extract(Text text) throws AnnotationException; //- was different structure 
    public abstract HashMap<String, DBpediaResource> extract(Text text) throws AnnotationException;

}
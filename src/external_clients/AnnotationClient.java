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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;


import java.io.*;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.HashMap;

/**
 * This class has been translate to scala. Please use the AnnotationClientScala.scala for new External Clients!
 * (AnnotationClientScala.scala is at eval/src/main/scala/org/dbpedia/spotlight/evaluation/external/)
 *
 * @author pablomendes
 */

public abstract class AnnotationClient {

    public Logger LOG = Logger.getLogger(this.getClass());
	public static final String	CORPUS_NAME						=	"news.en-000";	
	public static final String	CORPUS_NAME2					=	"-of-00100.txt";
	
    // Create an instance of HttpClient.
    private static HttpClient client = new HttpClient();


    public String requestOriginal(HttpMethod method) throws AnnotationException {

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
//            byte[] responseBody = method.getResponseBody(); //TODO Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.

            InputStream inputStream = method.getResponseBodyAsStream();  
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));  
            StringBuffer stringBuffer = new StringBuffer();  
            String str= "";  
            while((str = br.readLine()) != null){  
            stringBuffer .append(str );  
            }  
            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            response = new String(stringBuffer);

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

    public void saveExtractedEntitiesSet(File inputFile, File outputFile, LineParser parser, int restartFrom) throws Exception {
        PrintWriter out = new PrintWriter(outputFile);
        LOG.info("Opening input file "+inputFile.getAbsolutePath());
        String text = readFileAsString(inputFile);
        int i=0;
        int correct =0 ;
        int error = 0;
        int sum = 0;
        for (String snippet: text.split("\n")) {
        	//out.print("\tSnippet " + snippet);

            String s = parser.parse(snippet);
            if (s!= null && !s.equals("")) {
                i++;

                if (i<restartFrom) continue;

                //List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
                HashMap<String, DBpediaResource> entities = new HashMap<String, DBpediaResource>();

                try {
                    final long startTime = System.nanoTime();
                    entities = extract(new Text(snippet.replaceAll("\\s+"," ")));
                    
                    
                    final long endTime = System.nanoTime();
                    sum += endTime - startTime;
                    LOG.info(String.format("(%s) Extraction ran in %s ns.", i, endTime - startTime));
                    correct++;
                } catch (AnnotationException e) {
                    error++;
                    LOG.error(e);
                    e.printStackTrace();
                }

                /*for (DBpediaResource e: entities) {
                    out.print("db_" + e.uri() + " ");
                }*/
                
                //out.println("Entity called out of map: " + entities.get("britain"));
                
                /*for(String entity_key: entities.keySet()){
                	
                	out.print("key : " + entity_key + " entity: " + entities.get(entity_key) + " ");
                }*/
                
                System.out.println("\n");
                for(String snippet_word: snippet.split(" ")){
                	//System.out.println("Snippet word is : " + snippet_word.replaceAll("[^a-zA-Z]", " "));
                	if(entities.containsKey(snippet_word.replaceAll("[^a-zA-Z]", ""))){
                			out.print(snippet_word.replaceAll("[^a-zA-Z]", "") + "-" + " db_" + entities.get(snippet_word.replaceAll("[^a-zA-Z]", "")).uri() + " ");
                	}
                	else{
                			out.print(snippet_word.replaceAll("[^a-zA-Z]", "") + " ");
                	}
                }
                out.println();
                out.flush();
            }
        }
        out.close();
        LOG.info(String.format("Extracted entities from %s text items, with %s successes and %s errors.", i, correct, error));
        LOG.info("Results saved to: "+outputFile.getAbsolutePath());
        double avg = (new Double(sum) / i);
        LOG.info(String.format("Average extraction time: %s ms", avg * 1000000));
    }
    
    public void saveExtractedEntities1BilCorpus(LineParser parser, int restartFrom) throws Exception {
//        PrintWriter out = new PrintWriter(outputFile);
//        LOG.info("Opening input file "+inputFile.getAbsolutePath());
//        String text = readFileAsString(inputFile);
        int i=0;
        int correct =0 ;
        int error = 0;
        int sum = 0;
        //BufferedReader reader = null;
		BufferedWriter writer = null;
		
		for(int docNum=32;docNum<100;docNum++)
		{
			String current_directory = System.getProperty("user.dir");
			System.out.println(current_directory);
//			reader = new BufferedReader(new FileReader(CORPUS_NAME+Integer.toString(docNum)+CORPUS_NAME2));
//			String text = readFileAsString("//home//dinara//java-projects//FusionODPWord2VecNew//clean1BilCorpus//" + CORPUS_NAME+Integer.toString(docNum)+CORPUS_NAME2);
			String text = readFileAsString("./clean1BilCorpus/" + CORPUS_NAME+Integer.toString(docNum)+CORPUS_NAME2);
			writer = new BufferedWriter(new FileWriter("dbp-" + CORPUS_NAME+Integer.toString(docNum)+CORPUS_NAME2));
//			String line = reader.readLine();
	        for (String snippet: text.split("\n")) {
	        	//out.print("\tSnippet " + snippet);
	
	            String s = parser.parse(snippet);
	            if (s!= null && !s.equals("")) {
	                i++;
	
	                if (i<restartFrom) continue;
	
	                HashMap<String, DBpediaResource> entities = new HashMap<String, DBpediaResource>();
	
	                try {
	                    final long startTime = System.nanoTime();
	                    entities = extract(new Text(snippet.replaceAll("\\s+"," ")));
	                    
	                    
	                    final long endTime = System.nanoTime();
	                    sum += endTime - startTime;
	                    //LOG.info(String.format("(%s) Extraction ran in %s ns.", i, endTime - startTime));
	                    correct++;
	                } catch (AnnotationException e) {//no-print
	                    //error++;
	                    //LOG.error(e);
	                    //e.printStackTrace();
	                }
	
	                System.out.println("\n");
	                String doc = "";
	                for(String snippet_word: snippet.split(" ")){
	                	String snippet_word_upd = URLEncoder.encode(snippet_word.replaceAll("[^a-zA-Z]", ""), "UTF-8");
	                	//System.out.println("Snippet word is : " + snippet_word.replaceAll("[^a-zA-Z]", " "));
	                	if(entities.containsKey(snippet_word.replaceAll("[^a-zA-Z]", ""))){
	            				doc += snippet_word_upd + " " + entities.get(snippet_word_upd).uri() + "_ ";
	                	}
	                	
	                	else{
	            				doc += snippet_word + " ";
	                	}
	                }
	                writer.write(doc + "\n");
	                writer.flush();

	            }
	        }
        //reader.close();
        writer.close();
//        LOG.info(String.format("Extracted entities from %s text items, with %s successes and %s errors.", i, correct, error));
//        LOG.info("Results saved to: "+outputFile.getAbsolutePath());
 //       double avg = (new Double(sum) / i);
//        LOG.info(String.format("Average extraction time: %s ms", avg * 1000000));
    }
    }

    public void jarEntities1BilCorpus(LineParser parser, int restartFrom, int docNum1, int docNum2) throws Exception {
      int i=0;
      int correct =0 ;
      int error = 0;
      int sum = 0;
		BufferedWriter writer = null;
		
		for(int docNum=docNum1;docNum<docNum2;docNum++)
		{
			String current_directory = System.getProperty("user.dir");
			System.out.println(current_directory);
			String text = readFileAsString("//home//dinara//java-projects//FusionODPWord2VecNew//clean1BilCorpus//" + CORPUS_NAME+Integer.toString(docNum)+CORPUS_NAME2);
			writer = new BufferedWriter(new FileWriter("dbp-" + CORPUS_NAME+Integer.toString(docNum)+CORPUS_NAME2));
	        for (String snippet: text.split("\n")) {
	
	            String s = parser.parse(snippet);
	            if (s!= null && !s.equals("")) {
	                i++;
	
	                if (i<restartFrom) continue;
	
	                HashMap<String, DBpediaResource> entities = new HashMap<String, DBpediaResource>();
	
	                try {
	                    final long startTime = System.nanoTime();
	                    entities = extract(new Text(snippet.replaceAll("\\s+"," ")));
	                    
	                    
	                    final long endTime = System.nanoTime();
	                    sum += endTime - startTime;
	                    correct++;
	                } catch (AnnotationException e) {
	                    error++;

	                }
	
	                System.out.println("\n");
	                String doc = "";
	                for(String snippet_word: snippet.split(" ")){
	                	String snippet_word_upd = URLEncoder.encode(snippet_word.replaceAll("[^a-zA-Z]", ""), "UTF-8");
	                	if(entities.containsKey(snippet_word.replaceAll("[^a-zA-Z]", ""))){
	            				doc += snippet_word_upd + " " + entities.get(snippet_word_upd).uri() + "_ ";
	                	}
	                	
	                	else{
	            				doc += snippet_word + " ";
	                	}
	                }
	                writer.write(doc + "\n");
	                writer.flush();

	            }
	        }
      writer.close();
      LOG.info(String.format("Extracted entities from %s text items, with %s successes and %s errors.", i, correct, error));
      double avg = (new Double(sum) / i);
      LOG.info(String.format("Average extraction time: %s ms", avg * 1000000));
  }
  }

    
    public void saveExtractedEntitiesLP50AUC(LineParser parser, int restartFrom) throws Exception {

      int i=0;
      int correct =0 ;
      int error = 0;
      int sum = 0;
      BufferedReader reader = null;
	BufferedWriter writer = null;
				
			String current_directory = System.getProperty("user.dir");
			System.out.println(current_directory);

			String text = readFileAsString("//home//dinara//java-projects//docsimtest//lee.cor");
			writer = new BufferedWriter(new FileWriter("dbpdia04conf_lee.cor"));
			
//			String line = reader.readLine();
	        for (String snippet: text.split("\n")) {
	        	System.out.println("\tSnippet " + snippet);
	
	            String s = parser.parse(snippet);
	            if (s!= null && !s.equals("")) {
	                i++;
	
	                if (i<restartFrom) continue;
	
	                HashMap<String, DBpediaResource> entities = new HashMap<String, DBpediaResource>();
	
	                try {
	                    final long startTime = System.nanoTime();
	                    entities = extract(new Text(snippet.replaceAll("\\s+"," ")));
	                    //System.out.println("entities = " + entities);
	                    
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
	                String doc = "";
	                for(String snippet_word: snippet.split(" ")){
	                	String snippet_word_upd = URLEncoder.encode(snippet_word.replaceAll("[^a-zA-Z]", ""), "UTF-8");
	                	//System.out.println("Snippet word is : " + snippet_word.replaceAll("[^a-zA-Z]", " "));
	                	if(entities.containsKey(snippet_word.replaceAll("[^a-zA-Z]", ""))){
	            				doc += snippet_word_upd + " " + entities.get(snippet_word_upd).uri() + "_ ";
	                	}
	                	
	                	else{
	            				doc += snippet_word + " ";
	                	}
	                }
	                writer.write(doc + "\n");
	                writer.flush();

	            }
	        }
      reader.close();
      writer.close();
      LOG.info(String.format("Extracted entities from %s text items, with %s successes and %s errors.", i, correct, error));
//      LOG.info("Results saved to: "+outputFile.getAbsolutePath());
      double avg = (new Double(sum) / i);
      LOG.info(String.format("Average extraction time: %s ms", avg * 1000000));
  
  }
    
    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                System.out.println(fileEntry.getName());
            }
        }
    }



    
    public void saveExtractedEntitiesNYT(LineParser parser, int restartFrom) throws Exception {
//      PrintWriter out = new PrintWriter(outputFile);
//      LOG.info("Opening input file "+inputFile.getAbsolutePath());
//      String text = readFileAsString(inputFile);
      int i=0;
      int correct =0 ;
      int error = 0;
      int sum = 0;
      //BufferedReader reader = null;
      BufferedWriter writer = null;
      File directory = new File("//home//dinara//java-projects//FusionODPWord2VecNew//NYT_dbpedia_new//");
      

      for (final File fileEntry : directory.listFiles()) {
          /*if (fileEntry.isDirectory()) {
              listFilesForFolder(fileEntry);
          } else {*/
              //System.out.println(fileEntry.getName());
         
          
		//for(int docNum=1;docNum<120;docNum++)
		//{
			String current_directory = System.getProperty("user.dir");
			System.out.println(current_directory);

			String text = readFileAsString("//home//dinara//java-projects//FusionODPWord2VecNew//NYT_dbpedia_new//" + fileEntry.getName());
			writer = new BufferedWriter(new FileWriter("//home//dinara//java-projects//FusionODPWord2VecNew//NYT_dbpedia_new//"  + fileEntry.getName()));
//			String line = reader.readLine();
	        for (String snippet: text.split("\n")) {
	        	//out.print("\tSnippet " + snippet);
	
	            String s = parser.parse(snippet);
	            if (s!= null && !s.equals("")) {
	                i++;
	
	                if (i<restartFrom) continue;
	
	                HashMap<String, DBpediaResource> entities = new HashMap<String, DBpediaResource>();
	
	                try {
	                    final long startTime = System.nanoTime();
	                    entities = extract(new Text(snippet.replaceAll("\\s+"," ")));
	                    
	                    
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
	                String doc = "";
	                for(String snippet_word: snippet.split(" ")){
	                	String snippet_word_upd = URLEncoder.encode(snippet_word.replaceAll("[^a-zA-Z]", ""), "UTF-8");
	                	//System.out.println("Snippet word is : " + snippet_word.replaceAll("[^a-zA-Z]", " "));
	                	if(entities.containsKey(snippet_word.replaceAll("[^a-zA-Z]", ""))){
	            				doc += snippet_word_upd + " " + entities.get(snippet_word_upd).uri() + "_ ";
	                	}
	                	
	                	else{
	            				doc += snippet_word + " ";
	                	}
	                }
	                writer.write(doc + "\n");
	                writer.flush();

	            }
	        }
      //reader.close();
      writer.close();
      LOG.info(String.format("Extracted entities from %s text items, with %s successes and %s errors.", i, correct, error));
//      LOG.info("Results saved to: "+outputFile.getAbsolutePath());
      double avg = (new Double(sum) / i);
      LOG.info(String.format("Average extraction time: %s ms", avg * 1000000));
  //}
  }
      }
    
    
    public void evaluate(File inputFile, File outputFile) throws Exception {
        evaluateManual(inputFile,outputFile,0);
    }
    public void evaluate() throws Exception {
        evaluateManual(0);
    }
    public void evaluate(int docNum1, int docNum2) throws Exception {
        evaluateManual(0, docNum1, docNum2);

    }
    public void evaluateManual(File inputFile, File outputFile, int restartFrom) throws Exception {
         saveExtractedEntitiesSet(inputFile, outputFile, new LineParser.ManualDatasetLineParser(), restartFrom);
    }
    public void evaluateManual(int restartFrom) throws Exception {
        saveExtractedEntities1BilCorpus(new LineParser.ManualDatasetLineParser(), restartFrom);
        //saveExtractedEntitiesLP50AUC(new LineParser.ManualDatasetLineParser(), restartFrom);
        //saveExtractedEntitiesNYT(new LineParser.ManualDatasetLineParser(), restartFrom);
   }
    
    public void evaluateManual(int restartFrom, int docNum1, int docNum2) throws Exception {
        //saveExtractedEntities1BilCorpus(new LineParser.ManualDatasetLineParser(), restartFrom);
        //saveExtractedEntitiesLP50AUC(new LineParser.ManualDatasetLineParser(), restartFrom);
        //saveExtractedEntitiesNYT(new LineParser.ManualDatasetLineParser(), restartFrom);
        jarEntities1BilCorpus(new LineParser.ManualDatasetLineParser(), restartFrom, docNum1, docNum2);
   }
//    public void evaluateCurcerzan(File inputFile, File outputFile) throws Exception {
//         saveExtractedEntitiesSet(inputFile, outputFile, new LineParser.OccTSVLineParser());
//    }

    /**
     * Entity extraction code.
     * @param text
     * @return
     */
    //public abstract List<DBpediaResource> extract(Text text) throws AnnotationException; //- was different structure 
    public abstract HashMap<String, DBpediaResource> extract(Text text) throws AnnotationException;

}
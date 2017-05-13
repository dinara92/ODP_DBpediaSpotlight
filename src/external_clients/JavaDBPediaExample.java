package external_clients;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

public class JavaDBPediaExample {

	public static void main(String[] args) {
        ParameterizedSparqlString qs = new ParameterizedSparqlString(""
                + "prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX dbo:     <http://dbpedia.org/ontology/>"
                + "PREFIX dct:     <http://purl.org/dc/terms/>"
                + "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                + "\n"
                + "select  distinct ?resource ?birthPlace ?subject ?type where {\n"
                + "  ?resource rdfs:label 'Barack Obama'@en.\n"
                + "  ?resource dbo:birthPlace ?birthPlace.\n"
                + "  ?resource dct:subject ?subject.\n"
                + "  ?resource rdf:type ?type.\n"
                + "}");


        		
        QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", qs.asQuery());

        ResultSet results = exec.execSelect();

        while (results.hasNext()) {

//            System.out.println(results.next().get("birthPlace").toString());
//            System.out.println(results.next().get("subject").toString());
            System.out.println(results.next().get("type").toString());

        }

        ResultSetFormatter.out(results);
    }

}

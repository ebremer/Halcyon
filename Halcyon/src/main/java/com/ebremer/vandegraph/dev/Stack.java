package com.ebremer.vandegraph.dev;

import java.io.IOException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author erich
 */
public class Stack {
    public static String SHACLPATH = "stack.jsonld";
    private final Model stack = ModelFactory.createDefaultModel();
    final private static Logger logger = LoggerFactory.getLogger(Stack.class);
    
    public Stack() {
        ClassPathResource cpr = new ClassPathResource(SHACLPATH);
        String baseURI = "https://wildwildwest.com/";
        try {
            RDFParser.create()
                    .source(cpr.getInputStream())
                    .base(baseURI)
                    //.lang(Lang.TURTLE)
                    .lang(Lang.JSONLD11)
                    //errorHandler(ErrorHandlerFactory.errorHandlerStrict()) // Strict parsing
                    .parse(stack);
        } catch (IOException ex) {
            System.getLogger(Stack.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        stack.write(System.out, "TTL");
    }
    
    public Model getModel() {
        return stack;
    }
}

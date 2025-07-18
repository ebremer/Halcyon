package com.ebremer.vandegraph.dev;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author erich
 */
public class Stack {
    public static String SHACLPATH = "stack.ttl";
    private final Model stack = ModelFactory.createDefaultModel();
    final private static Logger logger = LoggerFactory.getLogger(Stack.class);
    
    public Stack() {
        ClassPathResource cpr = new ClassPathResource(SHACLPATH);
        try {
            RDFDataMgr.read(stack, cpr.getInputStream(), Lang.TURTLE);
        } catch (FileNotFoundException ex) {
            logger.error(ex.getMessage());
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Stack.class.getName()).log(Level.SEVERE, null, ex);
        }
        stack.write(System.out, "TTL");
    }
    
    public Model getModel() {
        return stack;
    }
}

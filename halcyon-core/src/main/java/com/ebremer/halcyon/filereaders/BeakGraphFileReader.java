package com.ebremer.halcyon.filereaders;

import com.ebremer.halcyon.lib.URITools;
import com.ebremer.rocrate4j.ROCrateReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

/**
 *
 * @author erich
 */
public class BeakGraphFileReader extends AbstractFileReader {
    
    public BeakGraphFileReader(URI uri) {
        super(uri);
    }

    @Override
    public Model getMeta(URI xuri) {
        File file = new File(xuri);
        String fixb = URITools.fix(file);
        String fixe = URITools.fix(file)+"/";
        try (ROCrateReader roc = new ROCrateReader(uri)) {
            if (roc.hasManifest()) {
                Model z = roc.getManifest();
                UpdateRequest update = UpdateFactory.create();
                ParameterizedSparqlString pss = new ParameterizedSparqlString("""
                    delete {?s ?p ?o}
                    insert {?neo ?p ?o}
                    where {
                        ?s ?p ?o
                    }
                """);
                pss.setIri("s", fixe);
                pss.setIri("neo", fixb);
                update.add(pss.toString());
                pss = new ParameterizedSparqlString("""
                    delete {?s ?p ?neo}
                    insert {?s ?p ?o}
                    where {
                        ?s ?p ?o
                    }
                """);
                pss.setIri("o", fixe);
                pss.setIri("neo", fixb);
                update.add(pss.toString());
                UpdateAction.execute(update, z);
                return z;
            }
        } catch (IOException ex) {
            Logger.getLogger(BeakGraphFileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    @Override
    public Model getMeta() {                
        return getMeta(uri);
    }

    @Override
    public String getFormat() {
        return "zip";
    }

    @Override
    public Set<String> getSupportedFormats() {
        Set<String> set = new HashSet<>();
        set.add("zip");
        return set;
    }

    @Override
    public void close() {}
    
}

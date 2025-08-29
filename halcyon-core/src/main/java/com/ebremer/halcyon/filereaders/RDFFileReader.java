package com.ebremer.halcyon.filereaders;

import com.ebremer.halcyon.server.utils.PathMapper;
import com.ebremer.ns.LDP;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.RDF;

/**
 *
 * @author Erich Bremer
 */
public class RDFFileReader extends AbstractFileReader {
    private Model m;    
    private static final Map<String, Lang> EXT_TO_LANG = new HashMap<>();

    static {
        EXT_TO_LANG.put("ttl", Lang.TURTLE);
        EXT_TO_LANG.put("nt", Lang.NT);
        EXT_TO_LANG.put("jsonld", Lang.JSONLD11);
        EXT_TO_LANG.put("rdf", Lang.RDFXML);
    }
    
    private static Lang getLangFromUri(URI uri) {
        String path = uri.getPath();
        if (path == null) return null;
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == path.length() - 1) {
            return null;
        }
        String ext = path.substring(dotIndex + 1).toLowerCase();
        return EXT_TO_LANG.get(ext);
    }
    
    //public RDFFileReader(URI uri, File file) {
       
    //}

    public RDFFileReader(URI uri) {
        super(uri);
        m = ModelFactory.createDefaultModel();
        String baseURI = uri.toString();        
        m.createResource(baseURI)
                .addProperty(RDF.type, LDP.RDFSource);
        Lang lang = getLangFromUri(uri);
        Optional<URI> x = PathMapper.getPathMapper().http2file(uri);
        if (x.isPresent()) {
            System.out.println(x.get());
        }
        File file = new File("D:\\HalcyonStorage\\utah\\HnE\\Stack2\\stack.jsonld");
        try (FileInputStream fis = new FileInputStream(file)) {            
            RDFParser.create()
                    .source(fis)
                    .base(baseURI)
                    .lang(lang)
                    //errorHandler(ErrorHandlerFactory.errorHandlerStrict()) // Strict parsing
                    .parse(m);
        } catch (FileNotFoundException ex) {
            System.getLogger(RDFFileReader.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } catch (IOException ex) {
            System.getLogger(RDFFileReader.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    @Override
    public Model getMeta() {
        return m;
    }

    @Override
    public Model getMeta(URI uri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getSupportedFormats() {
        Set<String> set = new HashSet<>();
        set.add("nt");
        set.add("ttl");
        set.add("jsonld");
        return set;
    }

    @Override
    public void close() {}
    
    public static void main(String[] args) {
        File file = new File("D:\\HalcyonStorage\\utah\\HnE\\Stack2\\stack.jsonld");
        URI uri = URI.create("https://localhost:8888/utah/HnE/Stack2/stack.jsonld");
        //URI uri = file.toURI();
        RDFFileReader r = new RDFFileReader(uri);
        RDFDataMgr.write(System.out, r.getMeta(), Lang.TURTLE);
    }
    
}

package com.ebremer.halcyon.filereaders;

import com.ebremer.halcyon.server.utils.HalcyonSettings;
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
    private Optional<PathMapper> pathMapper;

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

    public RDFFileReader(URI uri) {
        this(uri, null);        
    }
        
    public RDFFileReader(URI uri, PathMapper pm) {
        super(uri);
        if (pm==null) {
            pathMapper = Optional.empty();
        } else {
            this.pathMapper = Optional.of(pm);
        }
        m = ModelFactory.createDefaultModel();
        String baseURI = uri.toString();     
        m.createResource(baseURI)
                .addProperty(RDF.type, LDP.RDFSource);
        Lang lang = getLangFromUri(uri);
        URI src;
        if (pathMapper.isEmpty()) {
            src = uri;
        } else {
            PathMapper pmx = pathMapper.get();
            Optional<URI> x = pmx.http2file(uri);
            if (x.isPresent()) {
                src = x.get();
            } else {
                throw new Error("file does not exist : "+uri);
            }
        }
        try (FileInputStream fis = new FileInputStream(new File(src))) {
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
        URI uri = URI.create("https://localhost:8888/ldp/utah/HnE/Stack2/stack.jsonld");
        File settingsFile = new File("D:\\projects\\Halcyon\\Halcyon\\settings.ttl");
        HalcyonSettings settings = HalcyonSettings.getSettings(settingsFile);
        PathMapper pathMapper = PathMapper.getPathMapper(settings);
        RDFFileReader r = new RDFFileReader(uri,pathMapper);
        RDFDataMgr.write(System.out, r.getMeta(), Lang.TURTLE);      
    }
    
}

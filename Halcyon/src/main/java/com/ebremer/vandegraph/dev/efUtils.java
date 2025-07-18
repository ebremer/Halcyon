package com.ebremer.vandegraph.dev;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.processor.FromRdfProcessor;
import com.apicatalog.rdf.RdfDataset;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import static org.apache.jena.riot.lang.LangJSONLD11.JSONLD_OPTIONS;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;


import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.jena.riot.system.JenaTitanium;
import org.apache.jena.sparql.core.DatasetGraph;

/**
 *
 * @author Erich Bremer
 */
public class efUtils {
    
    public static final String NS = "https://halcyon.is/zephyr/ns/";
    
    public static String toJSONLD(Model model) throws JsonLdError {           
        Dataset dsx = DatasetFactory.create(model);
        DatasetGraph dsg = dsx.asDatasetGraph();
        RdfDataset ds = JenaTitanium.convert(dsg);
        Document doc = RdfDocument.of(ds);
        JsonLdOptions options = new JsonLdOptions();
        options.setOrdered(false);
        options.setUseNativeTypes(true);
        options.setOmitGraph(true);  
        JsonArray array = FromRdfProcessor.fromRdf(doc, options);
        JsonObjectBuilder cxt = Json.createObjectBuilder();
        dsg.prefixes().stream().forEach(p->cxt.add(p.getPrefix(), p.getUri()));
        cxt
            .add("src",
                Json.createObjectBuilder()
                    .add(Keywords.ID, "zep:src")
                    .add(Keywords.TYPE, Keywords.ID)
            )
            .add("scalex", "zep:scalex")
            .add("scaley", "zep:scaley")
            .add("zorder", "zep:zorder")
            .add("pixelsizeX", "zep:pixelsizeX")
            .add("pixelsizeY", "zep:pixelsizeY")
            .add("offsetx", "zep:offsetx")
            .add("offsety", "zep:offsety")
            .add("x", "zep:x")
            .add("y", "zep:y")
            .add("FeatureLayer", "zep:FeatureLayer")
            .add("Stack", "zep:Stack")
        ;
        JsonObject frame = Json.createObjectBuilder()
            .add(Keywords.CONTEXT, cxt)
            .add(Keywords.EMBED, Keywords.ALWAYS)
            .add(Keywords.OMIT_DEFAULT, true)
            .add(Keywords.REQUIRE_ALL, false)
            //.add("classification", Json.createObjectBuilder())
            .build();
        FramingApi api = JsonLd.frame(JsonDocument.of(array), JsonDocument.of(frame));
        JsonStructure x = api.get();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriterFactory writerFactory = Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        JsonWriter out = writerFactory.createWriter(baos);
        out.write(x);       
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }     
    
    public static void main(String[] args) throws JsonLdError {
        Stack stack = new Stack();
        Model m = stack.getModel();
        System.out.println("========== JSON-LD ==========================================");
        System.out.println(toJSONLD(m));
    }
    
}

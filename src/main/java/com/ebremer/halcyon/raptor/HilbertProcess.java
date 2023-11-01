package com.ebremer.halcyon.raptor;

import com.ebremer.halcyon.raptor.spatial.scale;
import com.ebremer.beakgraph.ng.AbstractProcess;
import com.ebremer.beakgraph.ng.BeakWriter;
import com.ebremer.halcyon.raptor.Objects.Scale;
import com.ebremer.halcyon.raptor.spatial.Area;
import com.ebremer.halcyon.raptor.spatial.Perimeter;
import com.ebremer.ns.EXIF;
import com.ebremer.ns.GEO;
import com.ebremer.ns.HAL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.jena.vocabulary.XSD;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 *
 * @author erich
 */
public class HilbertProcess implements AbstractProcess {
    private final ConcurrentHashMap<Resource,Polygon> buffer;
    private final int width;
    private final int height;
    private final int tileSizeX;
    private final int tileSizeY;
    private final HashSet<Integer> scaleset;
    private final int numscales;
    private final String uuid = "urn:uuid"+UUID.randomUUID().toString();
    private final String annotations = "urn:uuid"+UUID.randomUUID().toString();
    private final ArrayList<Scale> scales = new ArrayList<>();
    
    public HilbertProcess(int width, int height, int tileSizeX, int tileSizeY) {
        scaleset = new HashSet<>();
        FunctionRegistry.get().put(HAL.NS+"eStarts", eStarts.class);
        FunctionRegistry.get().put(HAL.NS+"Intersects", Intersects.class);
        FunctionRegistry.get().put(HAL.NS+"scale", scale.class);
        FunctionRegistry.get().put(HAL.NS+"area", Area.class);
        FunctionRegistry.get().put(HAL.NS+"perimeter", Perimeter.class);
        this.width = width;
        this.height = height;
        this.tileSizeX = tileSizeX;
        this.tileSizeY = tileSizeY;
        buffer = new ConcurrentHashMap<>();
        int max = Math.max(width, height);
        max = ((int) Math.ceil(Math.log(max)/Math.log(2))) - ((int) Math.ceil(Math.log(tileSizeX)/Math.log(2)))+1;
        System.out.println("# of levels --> "+max);
        int w = width;
        int h = height;
        for (int i=0; i<max; i++) {
            int numTilesX = (int) Math.ceil((double)w/(double)tileSizeX);
            int numTilesY = (int) Math.ceil((double)h/(double)tileSizeY);
            scales.add(new Scale(i, w, h, numTilesX, numTilesY));
            w = (int) Math.round((double)w/2d);
            h = (int) Math.round((double)h/2d);
        }
        numscales = max;
    }
    
    public void FindClassifications(Dataset ds) {
        ParameterizedSparqlString pssx = new ParameterizedSparqlString(
            """
            insert {
                ?featureCollection hal:hasClassification ?class
            }
            where {
                {
                    ?featureCollection a geo:FeatureCollection; rdfs:member ?feature .
                    ?feature a geo:Feature; hal:classification ?class
                    filter (!bnode(?class))
                } union {
                    ?featureCollection a geo:FeatureCollection; rdfs:member ?feature .
                    ?feature a geo:Feature; hal:classification ?bnode .
                    ?bnode a ?class .
                }
            }
            """
        );
        pssx.setNsPrefix("rdfs", RDFS.uri);
        pssx.setNsPrefix("rdf", RDF.uri);
        pssx.setNsPrefix("hal", HAL.NS);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setIri("annotations", annotations);
        UpdateAction.parseExecute(pssx.toString(), ds);
    }
    
    public void SeparateAnnotations(Dataset ds) {
        ParameterizedSparqlString pssx = new ParameterizedSparqlString(
            """
            delete {
                ?feature
                    geo:hasGeometry ?geometry .
                    ?geometry ?gp ?go
            }
            insert {
                graph ?annotations {
                    ?feature
                        geo:hasGeometry ?geometry .
                        ?geometry ?gp ?go
                }
            }
            where {
                ?feature
                    geo:hasGeometry ?geometry .
                    ?geometry ?gp ?go
            }
            """
        );
        pssx.setNsPrefix("rdf", RDF.uri);
        pssx.setNsPrefix("hal", HAL.NS);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setIri("annotations", annotations);
        UpdateAction.parseExecute(pssx.toString(), ds);     
    }
        
    public List<RDFNode> getNGs(Polygon swkt, int scale, int w, int h) {
        List<RDFNode> list = new LinkedList<>();
        Geometry geometry = null;
        try {
            Envelope bb = swkt.getEnvelopeInternal();
            int minx = (int) (bb.getMinX()/w);
            int miny = (int) (bb.getMinY()/h);
            int maxx = (int) Math.ceil(bb.getMaxX()/w);
            int maxy = (int) Math.ceil(bb.getMaxY()/h);
            for (int a=minx; a<maxx;a++) {
                for (int b=miny; b<maxy;b++) {
                    list.add(ResourceFactory.createResource(HAL.Grid.toString().toLowerCase()+"/"+scale+"/"+a+"/"+b));
                }                
            }
        } catch (IllegalArgumentException ex) {
            if (geometry!=null) {
                System.out.println("ARGH --> "+geometry.getCoordinates().length);
            } else {
                System.out.println("ARGH --> "+swkt);
            }
            Logger.getLogger(HilbertProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;
    }
    
    public HashSet<RDFNode> CreateNamedGraphs(Dataset ds, int scale) {
        buffer.clear();
        final HashSet<RDFNode> ngs = new HashSet<>();
        ParameterizedSparqlString pss = new ParameterizedSparqlString(
            """
            select ?a ?wkt
            where {
                graph ?annotations { ?a geo:hasGeometry/geo:asWKT ?wkt }
            }
            """
        );
        pss.setNsPrefix("geo", GEO.NS);
        pss.setNsPrefix("rdf", RDF.uri);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setIri("annotations", annotations);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(),ds);
        ResultSet rs = qe.execSelect();
        Model xx = ds.getNamedModel(uuid);
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            Polygon hp = Tools.WKT2Polygon(qs.get("wkt").asLiteral().getString());
            if (hp!=null) {
                getNGs(hp,scale,tileSizeX,tileSizeY).forEach(ng->{
                    ngs.add(ng);
                    Resource anno = qs.get("a").asResource();
                    xx.add(anno, SchemaDO.containedIn, ng);
                    buffer.put(anno, hp);
                });
            }
        }
        return ngs;
    }
        
    public void BinTheAnnotations(Dataset ds) {
        ParameterizedSparqlString pssx = new ParameterizedSparqlString(
            """
            delete {
                graph ?grid { ?feature so:containedIn ?ng }
            }
            insert {
                graph ?ng {
                   ?feature
                        geo:hasGeometry ?geometry .
                        ?geometry ?gp ?go
                }
            }
            where {
                graph ?grid { ?feature so:containedIn ?ng }
                graph ?annotations {
                   ?feature
                        geo:hasGeometry ?geometry .
                        ?geometry ?gp ?go
                }
            }
            """
        );
        pssx.setNsPrefix("rdf", RDF.uri);
        pssx.setNsPrefix("hal", HAL.NS);
        pssx.setNsPrefix("so", SchemaDO.NS);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setIri("grid", uuid);
        pssx.setIri("annotations", annotations);
        UpdateAction.parseExecute(pssx.toString(), ds);
        System.out.println("Missing polygons...");
        System.out.println("Annotations binned...");
        System.out.println("POLYGONS --> "+buffer.size());
    }
    
    public void ScalePolygons(Dataset ds) {
        ParameterizedSparqlString pssx = new ParameterizedSparqlString(
            """
            delete { ?geometry geo:asWKT ?wkt }
            insert { ?geometry geo:asWKT ?scaledPolygon }
            where {
                ?geometry geo:asWKT ?wkt
                bind (hal:scale(?wkt,0.5) as ?scaledPolygon)
            }
            """
        );
        pssx.setNsPrefix("rdf", RDF.uri);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setNsPrefix("hal", HAL.NS);
        UpdateAction.parseExecute(pssx.toString(), ds.getNamedModel(annotations));
    }
    
    public void RemoveRuntPolygons(Dataset ds) {
        ParameterizedSparqlString pssx = new ParameterizedSparqlString(
            """
            delete {
                graph ?annotations {
                    ?feature
                        geo:hasGeometry ?geometry .
                        ?geometry ?gp ?go; geo:asWKT ?wkt .                    
                    }
            }
            where {
                graph ?annotations {
                    ?feature
                        geo:hasGeometry ?geometry .
                        ?geometry ?gp ?go; geo:asWKT ?wkt .                    
                }
                filter (hal:Area(?wkt)<2)
            }
            """
        );
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setNsPrefix("rdf", RDF.uri);
        pssx.setNsPrefix("hal", HAL.NS);
        pssx.setNsPrefix("xsd", XSD.NS);
        UpdateAction.parseExecute(pssx.toString(), ds);
    }
    
    public void RemoveCrud(Model m) {
        ParameterizedSparqlString pssx = new ParameterizedSparqlString(
            """
            delete {
                ?featureCollection rdfs:member ?feature
            }
            where {
                ?featureCollection a geo:FeatureCollection; rdfs:member ?feature
            }
            """
        );
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setNsPrefix("rdfs", RDFS.uri);
        pssx.setNsPrefix("rdf", RDF.uri);
        pssx.setNsPrefix("hal", HAL.NS);
        UpdateAction.parseExecute(pssx.toString(), m);
    }

    @Override
    public void Process(BeakWriter bw, Dataset ds) {        
        Model xxx = ds.getDefaultModel();
        System.out.println("Calculate Areas...");
        FeatureGeneration.AddAreas(xxx);
        System.out.println("Calculate Perimeters...");
        FeatureGeneration.AddPerimeters(xxx);
        //FeatureGeneration.Display(xxx);
        //RDFDataMgr.write(System.out, ds.getDefaultModel(), RDFFormat.TURTLE_PRETTY);
        Resource root = xxx.createResource();
        root.addProperty(RDF.type, HAL.Segmentation);
        Literal tx = xxx.createTypedLiteral(String.valueOf(512), XSDDatatype.XSDint);
        Literal ty = xxx.createTypedLiteral(String.valueOf(512), XSDDatatype.XSDint);
        Literal fw = xxx.createTypedLiteral(String.valueOf(width), XSDDatatype.XSDint);
        Literal fh = xxx.createTypedLiteral(String.valueOf(height), XSDDatatype.XSDint);
        Resource spatialGrid = xxx.createResource()
                .addProperty(RDF.type, HAL.Grid)
                .addProperty(EXIF.width, fw)
                .addProperty(EXIF.height, fh)
                .addProperty(HAL.tileSizeX, tx)
                .addProperty(HAL.tileSizeY, ty);
        scales.forEach(s->{
            Literal w = xxx.createTypedLiteral(String.valueOf(s.width()), XSDDatatype.XSDint);
            Literal h = xxx.createTypedLiteral(String.valueOf(s.height()), XSDDatatype.XSDint);
            Literal si = xxx.createTypedLiteral(String.valueOf(s.scale()), XSDDatatype.XSDint);
            Resource scale = xxx.createResource()
                .addProperty(HAL.scaleIndex, si)
                .addProperty(EXIF.width, w)
                .addProperty(EXIF.height, h);   
            spatialGrid.addProperty(HAL.scale, scale);
        });
        ds.addNamedModel(uuid, ModelFactory.createDefaultModel());
        ds.addNamedModel(annotations, ModelFactory.createDefaultModel());        
        System.out.println("Find Classifications...");
        FindClassifications(ds);
        System.out.println("Separate Annotations...");
        SeparateAnnotations(ds);
        //RDFDataMgr.write(System.out, ds.getDefaultModel(), RDFFormat.TURTLE_PRETTY);
        System.out.println("Remove Crud...");
        RemoveCrud(ds.getDefaultModel());        
        //RDFDataMgr.write(System.out, ds.getDefaultModel(), RDFFormat.TURTLE_PRETTY);
        System.out.println("Analyze Dataset...");
        bw.Analyze(ds);
        System.out.println("Write Default Graph...");
        Resource dg = ResourceFactory.createResource("urn:halcyon:defaultgraph");
        System.out.println("Created Resource for Default Graph...");
        bw.RegisterNamedGraph(dg);
        System.out.println("Registered Default Graph...");
        bw.Add(dg, ds.getDefaultModel());
        System.out.println("Added Default Graph...");
        for (int ss=0; ss<scales.size();ss++) {
            scaleset.add(ss);
            HashSet<RDFNode> ngs = CreateNamedGraphs(ds,ss);
            System.out.println("# of triples in annotations --> "+ds.getNamedModel(annotations).size());
            System.out.println("Create Named Graphs..."+ss);
            System.out.println("Bin Annotations..."+ss);
            BinTheAnnotations(ds);
            int c = ngs.size();
            for (RDFNode node : ngs) {
                Resource ng = node.asResource();
                long begin = System.nanoTime();
                bw.getbyPredicate().forEach((k,paw)->{
                    paw.sumCounts();
                    paw.resetCounts();
                    paw.resetVectors();
                });
                bw.RegisterNamedGraph(ng);
                bw.Add(ng,ds.getNamedModel(ng));
                double end = System.nanoTime() - begin;
                end = end / 1000000d;
                c--;
                System.out.println(ng+" "+c+"  "+end);
            }            
            System.out.println("RemoveChunks..."+ss);
            ngs.forEach(node->{
                Resource ng = node.asResource();
                ds.removeNamedModel(ng);
            });
            System.out.println("Scaling Polygons..."+ss);
            ScalePolygons(ds);     
            System.out.println("Remove Runt/Empty Polygons..."+ss);
            RemoveRuntPolygons(ds);  
            ngs.clear();
        }
        Model last = ModelFactory.createDefaultModel();
        ParameterizedSparqlString pssx = new ParameterizedSparqlString(
            """
            construct {
                ?roc so:name ?title .
                ?roc so:description ?description .
                ?roc so:datePublished ?date .
                ?roc so:contributor ?contributor .
                ?roc so:ScholarlyArticle ?references .
                ?roc so:publisher ?publisher .
            }
            where {
                ?featureCollection a geo:FeatureCollection;
                optional { ?featureCollection dcterms:contributor ?contributor }
                optional { ?featureCollection dcterms:description ?description }
                optional { ?featureCollection dcterms:references ?references }
                optional { ?featureCollection dcterms:title ?title }
                optional { ?featureCollection dcterms:date ?date }
                optional { ?featureCollection dcterms:publisher ?publisher }
            }
            """
        );
        pssx.setNsPrefix("so", SchemaDO.NS);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setIri("roc", bw.getROC().getRDE().getURI());
        pssx.setNsPrefix("dcterms", DCTerms.NS);
        QueryExecution qe = QueryExecutionFactory.create(pssx.toString(), ds.getDefaultModel());
        last.add(qe.execConstruct());
        pssx = new ParameterizedSparqlString(
            """
            construct {
                _:CreateAction a so:CreateAction;
                    so:name ?title;
                    so:instrument ?creator;
                    so:object ?source
            }
            where {
                ?featureCollection a geo:FeatureCollection;
                    dcterms:title ?title;
                    dcterms:source ?source;
                    dcterms:creator ?creator .
            }
            """
        );
        pssx.setNsPrefix("so", SchemaDO.NS);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setNsPrefix("dcterms", DCTerms.NS);
        qe = QueryExecutionFactory.create(pssx.toString(), ds.getDefaultModel());
        last.add(qe.execConstruct());
        pssx = new ParameterizedSparqlString(
            """
            construct {
                ?source ?sp ?so .
            }
            where {
                ?featureCollection a geo:FeatureCollection;
                    dcterms:source ?source .
                ?source ?sp ?so .
            }
            """
        );
        pssx.setNsPrefix("so", SchemaDO.NS);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setNsPrefix("dcterms", DCTerms.NS);
        qe = QueryExecutionFactory.create(pssx.toString(), ds.getDefaultModel());
        last.add(qe.execConstruct());
        pssx = new ParameterizedSparqlString(
            """
            construct {
                ?roc so:hasPart ?featureCollection .
                ?featureCollection a geo:FeatureCollection; ?p ?o
            }
            where {
                ?featureCollection a geo:FeatureCollection; ?p ?o
            }
            """
        );
        pssx.setNsPrefix("so", SchemaDO.NS);
        pssx.setNsPrefix("geo", GEO.NS);
        pssx.setIri("roc", bw.getROC().getRDE().getURI());
        pssx.setNsPrefix("dcterms", DCTerms.NS);
        qe = QueryExecutionFactory.create(pssx.toString(), ds.getDefaultModel());
        last.add(qe.execConstruct());
        Resource CA = last.listResourcesWithProperty(RDF.type, SchemaDO.CreateAction).next();
        Resource FC = last.listResourcesWithProperty(RDF.type, GEO.FeatureCollection).next();
        
        CA.addProperty(SchemaDO.result, bw.getTarget());
        FC.addProperty(RDFS.member, bw.getTarget());
        
        bw.getROC().getManifest().getManifestModel().add(last);
    }
}

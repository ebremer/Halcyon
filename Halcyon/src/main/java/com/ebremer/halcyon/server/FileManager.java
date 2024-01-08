package com.ebremer.halcyon.server;

import com.ebremer.halcyon.server.utils.HalcyonSettings;
import com.ebremer.halcyon.data.DataCore;
import com.ebremer.halcyon.filesystem.DirectoryProcessor;
import com.ebremer.halcyon.services.Service;
import com.ebremer.halcyon.server.utils.StorageLocation;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public final class FileManager implements Service {
    private static HalcyonSettings hs = null;
    private Timer timer;
    private TimerTask task;
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
    
    public FileManager() {
        logger.info("Starting FileManager...");
        hs = HalcyonSettings.getSettings();
        resume();
    }
    
    public void pause() {
        this.timer.cancel();
    }
    
    public void resume() {
        this.timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                //System.out.println("Performing storage scan...");
                pause();
                Dataset ds = DataCore.getInstance().getDataset();
                DirectoryProcessor dp = new DirectoryProcessor(ds);                
                ArrayList<StorageLocation> list = hs.getStorageLocations();
                Iterator<StorageLocation> i = list.iterator();
                while (i.hasNext()) {                    
                    Path p = i.next().path;
                   // System.out.println("Scanning --> "+p);
                    dp.Traverse(p);
                }
                ValidateData();
                resume();
            }
        };
        long delay = 1000L * 10L;
        this.timer.schedule(task, delay);
    }
    
    public void ValidateData() {
        Dataset ds = DataCore.getInstance().getDataset();
        ds.begin(ReadWrite.READ);
        QueryExecution qe = QueryExecutionFactory.create("select distinct ?g where {graph ?g {?g ?p ?o}}", ds);
        ResultSet results = qe.execSelect().materialise();
        ds.end();
        results.forEachRemaining(qs->{
            String r = qs.get("g").toString();
            if (r.startsWith("file:/")) {
                try {
                    URI uri = new URI(r);
                    Path path = Path.of(uri);
                    if (!path.toFile().exists()) {
                        UpdateRequest request = UpdateFactory.create();
                        ParameterizedSparqlString pss = new ParameterizedSparqlString(
                            """
                                delete where {graph ?kill {?s ?p ?o}};
                                delete where {graph ?g {?kill ?p ?o}};
                                delete where {graph ?g {?s ?p ?kill}};
                            """);
                        pss.setIri("kill", r);
                        request.add(pss.toString());
                        ds.begin(ReadWrite.WRITE);
                        UpdateAction.execute(request, ds);
                        ds.commit();
                        ds.end();
                        System.out.println("DELETED : "+r);
                    }
                } catch (URISyntaxException ex) {
                    System.out.println("NG --> "+r);
                    logger.error(ex.getMessage());
                }
            }
        });                
    }

    @Override
    public String getName() {
        return "FileManager";
    }
}
package com.ebremer.halcyon.filereaders;

import com.ebremer.halcyon.lib.ImageMeta;
import com.ebremer.halcyon.lib.ImageRegion;
import com.ebremer.halcyon.lib.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.apache.jena.rdf.model.Model;

/**
 *
 * @author Erich Bremer
 */
public class RDFImageReader extends AbstractImageReader {
    
    public RDFImageReader(URI uri, URI base) throws IOException {
        
    }

    @Override
    public BufferedImage readTile(ImageRegion region, Rectangle preferredsize) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Model readTileMeta(ImageRegion region, Rectangle preferredsize) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ImageMeta getImageMeta() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getMetaVersion() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Model getMeta() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Model getMeta(URI uri) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Set<String> getSupportedFormats() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void close() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}

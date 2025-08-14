package com.ebremer.halcyon.filereaders;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Erich Bremer
 */
public class RDFFileReaderFactory implements FileReaderFactory {

    @Override
    public FileReader create(URI uri, URI base) {
        return new RDFFileReader(uri);    
    }
    
    @Override
    public FileReader create(SeekableByteChannel src, URI base) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getSupportedFormats() {
        Set<String> set = new HashSet<>();
        set.add("jsonld");
        set.add("nt");
        set.add("ttl");
        return set;
    }
    
}

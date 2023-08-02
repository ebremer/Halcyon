package com.ebremer.halcyon.converters;

import com.beust.jcommander.Parameter;
import java.io.File;

/**
 *
 * @author erich
 */
public class HoverNet2OAParameters {
    @Parameter(names = "-help", converter = BooleanConverter.class, help = true)
    private boolean help;
    
    public boolean isHelp() {
        return help;
    }
    
    @Parameter(names = "-src", description = "Source Folder or File", required = true)
    public File src;

    @Parameter(names = "-dest", description = "Destination Folder or File", required = true)
    public File dest;
    
    @Parameter(names = "-threads", description = "# of Threads")
    public int threads = 1;
    
    @Parameter(names = {"-version","-v"}, converter = BooleanConverter.class)
    public boolean version = false;
}

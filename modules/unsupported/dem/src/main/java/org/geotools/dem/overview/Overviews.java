package org.geotools.dem.overview;

import java.io.File;
import java.io.IOException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;


public final class Overviews {
        
    private static final String COMMAND_OVERVIEWS = "gdaladdo";
    
    private Overviews() {}
    
    protected static File getExecutableFromPath(String name) throws IOException {
        if (File.pathSeparatorChar == '\\') { //windows file system
            name = name + ".exe";
        }
        String systemPath = System.getenv("PATH");
        if (systemPath == null) {
            systemPath = System.getenv("path");
        }
        if (systemPath == null) {
            throw new IOException("Path is not set, cannot locate " + name);
        }
        String[] paths = systemPath.split(File.pathSeparator);

        for (String pathDir : paths) {
            File file = new File(pathDir, name);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return file;
            }
        }
        throw new IOException("Could not locate executable (or could locate, but does not have execution rights): "
                        + name);
    }
    
    public static void add(File file, int... levels) throws IOException {        
        CommandLine cmd = new CommandLine(getExecutableFromPath(COMMAND_OVERVIEWS));        
        cmd.addArgument(file.getAbsolutePath());
        for (int level : levels) {
            cmd.addArgument("" + level);
        }
        new DefaultExecutor().execute(cmd);
    }
    
    public static void clean(File file) throws IOException {   
        CommandLine cmd = new CommandLine(getExecutableFromPath(COMMAND_OVERVIEWS));        
        cmd.addArgument(file.getAbsolutePath());
        cmd.addArgument("-clean");        
        new DefaultExecutor().execute(cmd);
    }
    

}

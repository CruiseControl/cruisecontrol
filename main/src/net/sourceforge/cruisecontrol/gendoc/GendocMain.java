package net.sourceforge.cruisecontrol.gendoc;

import java.io.FileWriter;
import java.io.IOException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.gendoc.html.ConfigHtmlGenerator;

/**
 * Provides a main() method that can be invoked to generate the configuration XML
 * documentation page.
 * @author pollens@msoe.edu
 */
public final class GendocMain {

    /** Root plugin name for use during gendoc parsing. */
    private static final String ROOT_PLUGIN = "cruisecontrol";
    
    private GendocMain() {
        // Do not allow instantiation.
    }
    
    /**
     * Generates configxml.html.
     * @param args Array of command-line arguments. Arg[0] should be the file path
     *        where the HTML content is to be written.
     * @throws Exception If the HTML could not be written out.
     */
    public static void main(String[] args) throws Exception {
        // Get destination path from command-line.
        String destination = args[0];
        
        // Just generate documentation for the plugins in the root registry.
        PluginRegistry registry = PluginRegistry.createRegistry();
        PluginInfoParser parser = new PluginInfoParser(registry, ROOT_PLUGIN);
        
        String html = new ConfigHtmlGenerator().generate(parser);
        
        // Write the documentation out to the specified file.
        FileWriter writer = new FileWriter(destination);
        
        try {
            writer.write(html);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                // Do nothing. At least we tried to close the stream.
            }
        }
        
        System.out.println("Plugin documentation generated successfully");
    }
    
}

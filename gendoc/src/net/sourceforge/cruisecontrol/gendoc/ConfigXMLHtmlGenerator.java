package net.sourceforge.cruisecontrol.gendoc;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

/**
 * Velocity based configxml.html generator.
 * @see #generate(net.sourceforge.cruisecontrol.gendoc.Gendoc.PluginModelTree)
 * @author jerome@coffeebreaks.org
 */
public class ConfigXMLHtmlGenerator {

	private Template template;
	private Context context;

    public ConfigXMLHtmlGenerator()
	{
    }

    /**
     * Initialize templating mecanism.
     * Note: paths til velocity.properties and configxml_html.vm are hardcoded. The caller must have
     * cruisecontrol/main or cruisecontrol/gendoc as its "user.dir"
     * @throws Exception
     */
    public void init()
		throws Exception
	{
        Properties properties = new Properties();
        InputStream aStream = new FileInputStream(new File("../gendoc/src/velocity.properties"));
        properties.load(aStream);

        String userDirDebug = System.getProperty("user.dir");
        System.out.println("DEBUG: user.dir: " + userDirDebug);
        
        String fileResourceLoaderPath = "..";
        properties.setProperty( "file.resource.loader.path", fileResourceLoaderPath);
        Velocity.init(properties);

        String templateRelativePath = "gendoc" + File.separator + "src" + File.separator + "configxml_html.vm";
        template = Velocity.getTemplate(templateRelativePath);
        if (template == null) {
            throw new IllegalArgumentException("Configuration error: template not found in " + templateRelativePath
            + " with fileResourceLoaderPath " + fileResourceLoaderPath + " and user.dir: " + userDirDebug);
        }
        context = new VelocityContext();
	}

    public void generate(Gendoc.PluginModelTree tree) throws Exception {
        File outputDirectory = new File("target");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        BufferedWriter bw;
        bw = new BufferedWriter(new FileWriter(outputDirectory.getAbsolutePath() + "/configxml.html"));
        context.put("tree", tree);

        try {
            this.template.merge(context, bw);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            bw.close();
        }
    }
}

/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, 2006, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.gendoc.html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Properties;

import net.sourceforge.cruisecontrol.gendoc.PluginInfoParser;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
//import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

/**
 * This Class provides the Mechanism for generator the Configxml.html
 * 
 * @author Anthony Love (lovea@msoe.edu)
 * @version 1.0
 */
public class ConfigHtmlGenerator {

    private static VelocityEngine engine;

    private final Template template;
    private final Context context;

    /**
     * Creates a new HTML generator for documenting plugins.
     * @throws Exception if anything goes wrong.
     */
    public ConfigHtmlGenerator() throws Exception {
        final VelocityEngine engine = getVelocityEngine();

        final String myTemplateBody = getTemplateFromJar();

        final StringResourceRepository repository = StringResourceLoader.getRepository();
        repository.putStringResource("myTemplate", myTemplateBody);

        template = engine.getTemplate("myTemplate");

        /*  below appears to duplicate above, and below fails during JMX calls...
        final Template templateTmp = engine.getTemplate("myTemplate");

        String userDirDebug = System.getProperty("user.dir");
        System.out.println("DEBUG: user.dir: " + userDirDebug);

        String templateRelativePath = "\\main\\src\\net\\sourceforge\\cruisecontrol\\gendoc\\html\\"
            + "configxml_html.vm";
        template = Velocity.getTemplate(templateRelativePath);
        //*/

        if (template == null) {
            throw new IllegalArgumentException("Configuration error: template not found.");
        }

        context = new VelocityContext();
    }

    private String getTemplateFromJar() throws IOException {
        // Reading the file contents from the JAR
        InputStream inStream = ConfigHtmlGenerator.class.getResourceAsStream("configxml_html.vm");
        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader streamReader = new InputStreamReader(inStream);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    /**
     * Creates a VelocityEngine singleton
     * 
     * @return The initiated VelocityEngine
     * @throws Exception In Case of Initialize Error
     */
    private static VelocityEngine getVelocityEngine() throws Exception {

        //@todo Dan Rollo changed this to a singleton because multiple calls during unit test resulted
        // in "engine already inited" errors from Velocity. We may want to revert the singleton if keeping it around is
        // a memory hog, or if the singleton has other bad side effects....
        if (engine == null) {
            Properties p = new Properties();
            p.setProperty("resource.loader", "string");
            p.setProperty("string.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
            p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
            p.setProperty("runtime.log.logsystem.log4j.category", "org.apache.velocity");

            engine = new VelocityEngine();
            engine.init(p);
        }

        return (engine);
    }

    /**
     * Generates the HTML file from the given PluginTree
     * 
     * @param parser The PluginParser to obtained the Plugin tree
     * @return The generated HTML file
     * @throws Exception In Case of failure
     */
    public String generate(PluginInfoParser parser) throws Exception {
        StringWriter sw = new StringWriter();
        
        context.put("generalErrors", parser.getParsingErrors());
        context.put("allPlugins", parser.getAllPlugins());
        context.put("rootPlugin", parser.getRootPlugin());
        
        context.put("utils", new HtmlUtils());

        try {
            this.template.merge(context, sw);
            return sw.getBuffer().toString();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }

}

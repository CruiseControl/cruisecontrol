/********************************************************************************
 * Plugin to publish jars and update a jnlp file
 *
 *
 * @author      Nuno Ferro  (mail@nunoferro.com)    2009/05/07
 * @cc-plugin
 ********************************************************************************/
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JnlpPublisher implements Publisher {

    // @todo Needs unit tests, documentation in configxml.html, and a new entry in default-plugins.properties

    private static final long serialVersionUID = -5939441135781954954L;

    private static final Logger LOG = Logger.getLogger(JnlpPublisher.class);

    private String strTarget;
    private String strJnlp;
    private String strSource;
    private boolean bDeleteOldJars;

    /**
     * @param target Specifies where the jar file should be published.
     */
    @Required
    public void setTarget(final String target) {
        this.strTarget = target;
    }

    /**
     * @param jnlp specifies the jnlp file to be updated.
     */
    @Required
    public void setJnlp(final String jnlp) {
        this.strJnlp = jnlp;
    }

    /**
     * @param source specifies where to get the jar.
     */
    @Default("")
    @Description("tries to find default from ant (don't think it's working though)")
    public void setSource(final String source) {
        this.strSource = source;
    }

    /**
     * @param bDel if true deletes old jars with the same name.
     */
    @Default("false")
    public void setDeleteOldJars(final boolean bDel) {
        this.bDeleteOldJars = bDel;
    }

    public void publish(final Element cruisecontrolLog) throws CruiseControlException {
        final XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        if (helper.isBuildSuccessful()) {
            final Project project = new Project();

            // if the source is not specified we set the default:
            // ProjectPath/dist
            if (strSource == null) {
                strSource = project.getBaseDir().getPath() + File.separator + "dist";
            }

            // Validate target
            final File dirTarget = new File(this.strTarget);
            if (!dirTarget.exists()) {
                throw new CruiseControlException("target directory " + dirTarget.getAbsolutePath()
                    + " does not exist");
            }
            if (!dirTarget.isDirectory()) {
                throw new CruiseControlException("target directory " + dirTarget.getAbsolutePath()
                    + " is not a directory");
            }

            // Validate source
            final File dirSource = new File(this.strSource);
            if (!dirSource.exists()) {
                throw new CruiseControlException("source directory " + dirSource.getAbsolutePath()
                    + " does not exist");
            }
            if (!dirSource.isDirectory()) {
                throw new CruiseControlException("source directory " + dirSource.getAbsolutePath()
                    + " is not a directory");
            }

            // Validate JNLP file
            final File fileJnlp = new File(this.strJnlp);
            if (!fileJnlp.exists()) {
                throw new CruiseControlException("jnlp file " + fileJnlp.getAbsolutePath()
                    + " does not exist");
            }

            String strEntry = null;
            String strNewFile = null;
            String strVersion = null;

            for (final File file : dirSource.listFiles()) {
                final String strFilename  = file.getName();
                final int idxVersion      = strFilename.indexOf("__V");
                final int idxJar          = strFilename.lastIndexOf(".jar");

                // if the filename is valid (it has __V and ends with .jar
                if (idxVersion > 0 && idxVersion < idxJar) {
                    // Grab file version
                    final String strTmp = strFilename.substring(idxVersion + 3, idxJar);

                    // Compare version, if it's bigger override
                    if (strVersion == null || strVersion.compareTo(strTmp) > 0) {
                        strEntry   = strFilename.substring(0, idxVersion);
                        strNewFile = strFilename;
                        strVersion = strTmp;
                    }
                }
            }

            if (strNewFile != null && strVersion != null) {
                // Copy file
                final FileUtils utils = FileUtils.getFileUtils();
                try {
                    // first delete old jars if needed
                    if (this.bDeleteOldJars) {
                        for (final File file : dirTarget.listFiles()) {
                            if (file.getName().indexOf(strEntry + "__V") > -1) {
                                if (!file.delete()) {
                                    LOG.warn("Failed to delete old jnlp jar file: " + file.getAbsolutePath());
                                }
                            }
                        }
                    }
                    // copy the file
                    utils.copyFile(new File(dirSource, strNewFile), new File(dirTarget, strNewFile));
                } catch (IOException e) {
                    throw new CruiseControlException(e);
                }

                try {
                    // Update JNLP file
                    updateJnlpFile(this.strJnlp, strEntry, strVersion);
                } catch (Exception ex) {
                    throw new CruiseControlException(ex);
                }
            }
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(strTarget, "target", this.getClass());
        ValidationHelper.assertIsSet(strJnlp, "jnlp", this.getClass());
    }

    static void updateJnlpFile(final String fileName, final String jarEntry, final String newVersion)
            throws Exception {
        final Document jnlpDocument = readJnlpDocument(fileName);
        replaceVersion(jnlpDocument, jarEntry, newVersion);
        writeDocument(jnlpDocument, fileName);
    }

    static Document readJnlpDocument(final String fileName) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(fileName));
    }

    static void replaceVersion(final Document jnlpDocument, final String jarEntry, final String newVersion) {
        final NodeList jarNodes = jnlpDocument.getElementsByTagName("jar");
        for (int i = 0; i < jarNodes.getLength(); i++) {
            final Node node = jarNodes.item(i);
            final NamedNodeMap map = node.getAttributes();
            final Node hrefNode = map.getNamedItem("href");
            if (hrefNode != null && hrefNode.getNodeValue().indexOf(jarEntry + ".jar") > -1) {
                final Attr versionAttribute = jnlpDocument.createAttribute("version");
                versionAttribute.setValue(newVersion);
                map.setNamedItem(versionAttribute);
                break;
            }
        }
    }

    static void writeDocument(final Document jnlpDocument, final String fileName) throws Exception {
        final TransformerFactory xformFactory = TransformerFactory.newInstance();
        final Transformer idTransform = xformFactory.newTransformer();
        final Source input = new DOMSource(jnlpDocument);
        final Result output = new StreamResult(new FileOutputStream(fileName));
        idTransform.transform(input, output);
    }
}

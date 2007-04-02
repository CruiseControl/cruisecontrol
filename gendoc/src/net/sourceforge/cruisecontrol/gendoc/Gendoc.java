package net.sourceforge.cruisecontrol.gendoc;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.*;

import java.io.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Generates plugin XML models and configxml.html documentation from default-plugin.properties and classdoc information.
 *
 * <h2>Plugin reference generator documentation</h2>
 * <p>
 * Plugins are identified recursively from the root one, whose class is fixed.
 * FIXME further document how tags/comments are translated into documentation.
 * </p>
 *
 * <h3><a name="abstractPlugins"/>Abstract plugins (or interfaces)</h3>
 * FIXME
 *
 * @todo improvement we could identify plugins just because they inherit from an interface used as add parameter. no need to tag them with @cc-config
 * just start parsing the root file and identify plugins because of their types
 * @todo may be the need to override some automatically inferred information ? (e.g. names...)
 * @todo plugin name doesn't really need to be specified in the plugin ? the couple (add field type, parent)
 * uniquely identifies the add method, thus the required name  
 * @todo child element description could be retrieved from first line of the matching type
 * @todo fail fast if anything's missing
 * @todo look into what problems inheritance can cause (e.g. add/set methods not redefined in sub-class, etc...) 
 * @author jerome@coffeebreaks.org
 */
public class Gendoc {
    /**
     * not all plugin models are generated from the source today
     * so we complete the information by adding them manually to those automatically generated.
     * this String identifies this particular plugin types as special care is needed to handle them.
     * once all the CC config reference is generated from source, this will go away.
     */
    private static final String NO_TYPE = "N/A";

    // FIXME refactor with main module
    /**
     * A simple plugin RootRegistry. Only used to store the contents of the Default plugin registry.
     */
    public static class RootRegistry {
        Map plugins = new HashMap();

        public void register(String name, String type) {
            plugins.put(name, type);
        }

        public String getPluginType(String name) {
            return (String) plugins.get(name);
        }

        public String getPluginName(String type) {
            Iterator iterator = plugins.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (entry.getValue().equals(type)) {
                    return (String) entry.getKey();
                }
            }
            return null;
        }
    }

    /**
     * Encapsulates the tree (represented by the node that allows to navigate it) in a class
     * to add functionality, mostly focused on navigation/display purposes.
     */
    public static class PluginModelTree {
        private static String LINE_SEPARATOR = System.getProperty("line.separator");

        private TreeNode root;

        public PluginModelTree(TreeNode root) {
            this.root = root;
        }

        public TreeNode getRoot() {
            return root;
        }

        public String printTreeToPreTag() throws IOException {
            StringWriter w = new StringWriter();
            writeTreeNode(root, w, 0, "  ");
            return w.getBuffer().toString();
        }

        public int countLinesInTree(String tree) {
            int count = 0;
            if (tree != null) {
                int idx = -1;
                do {
                    idx = tree.indexOf('\n', idx + 1);
                    if (idx >= 0)
                    {
                        count++;
                    }
                } while (idx >= 0);
            }
            return count;
        }
        /**
         * @param leafName the nodeName of the leaf that this all tree-path in this tree lead to. 
         * @return the hirearchy representation for the particular leafName (a tree showing only the path(s)
         *  leading to the specified leaf identified by its visible 'node' name)
         * @throws IOException
         * FIXME this doesn't work when leafname is an interface. How do we distinguish between them ?
         */
        public String printHierarchyToPreTag(String leafName) throws IOException {
            if (leafName == null)
            {
                throw new NullPointerException("null leafName argument");
            }
            TreeNode restrictedTree = getHierarchy(leafName);
            StringWriter w = new StringWriter();
            writeTreeHierarchy(restrictedTree, leafName, w, 0, "  ");
            return w.getBuffer().toString();
        }

        /**
         * Write the text intended to appear in the hierarchy. Takes care of the
         * fact that a plugin might appear as more than one leafes.
         * Some properties of the results:
         * <ul>
         * <li>Tree is indented</li>
         * <li>No <a href="#abstractPlugins">abstract-plugins</a> are shown in a hierarchy tree</li>
         * <li>All elements have links pointing to the other plugins, except the leafes themselves (would not be useful)</li>
         * </ul>
         * @param node
         * @param leafName
         * @param w
         * @param depth
         * @param indentationStr
         * @throws IOException
         * @throws IllegalStateException if a non implementable plugin appears in the tree
         */
        private void writeTreeHierarchy(TreeNode node, String leafName, StringWriter w, int depth, String indentationStr) throws IOException {
            String nodeName = node.getNodeName();
            // indentation
            indent(w, depth, indentationStr);
            // link
            boolean linkNode = !leafName.equals(nodeName);
            if (linkNode) {
                w.append("<a href=\"#");
                w.append(nodeName);
                w.append("\">");
            }
            
            boolean isInterface = ((PluginModel) node.getMe()).isInterface();
            if (isInterface) {
                throw new IllegalStateException("no interface expected in hierarchy tree but got one on node: " + node);
            }
            w.append("&lt;").append(nodeName).append("&gt;");
            if (linkNode) {
                w.append("</a>");
            }
            w.append(LINE_SEPARATOR);

            // children
            for (int i = 0; i < node.getChildren().size(); i++) {
                TreeNode child = (TreeNode) node.getChildren().get(i);
                writeTreeHierarchy(child, leafName, w, depth + 1, indentationStr);
            }

            // alternatives
            for (int i = 0; i < node.getAlternatives().size(); i++) {
                TreeNode alternative = (TreeNode) node.getAlternatives().get(i);
                writeTreeHierarchy(alternative, leafName, w, depth, indentationStr);
            }
        }

        /**
         * @return a List of TreeNode represented the prefix navigation of the tree (parent appended before its children)
         */
        public List getFlattenedPluginTree() {
            List flattenedPluginTree = new ArrayList();
            collectPluginTreeNodes(root, 0, flattenedPluginTree);
            return flattenedPluginTree;
        }

        private void collectPluginTreeNodes(TreeNode node, int depth, List collect) {
            collect.add(node);
            for (int i = 0; i < node.getChildren().size(); i++) {
                TreeNode childNode = (TreeNode) node.getChildren().get(i);
                collectPluginTreeNodes(childNode, depth + 1, collect);
            }
            for (int i = 0; i < node.getAlternatives().size(); i++) {
                TreeNode childNode = (TreeNode) node.getAlternatives().get(i);
                collectPluginTreeNodes(childNode, depth, collect);
            }
        }

        private void writeTreeNode(TreeNode node, Writer w, int depth, String indentationStr) throws IOException {
            String nodeName = node.getNodeName();
            boolean isInterface = ((PluginModel) node.getMe()).isInterface();

            // indentation
            indent(w, depth, indentationStr);
            // link
            w.append("<a href=\"#");
            if (isInterface) {
                w.append("if_");
            }
            w.append(nodeName).append("\">");
            if (isInterface) {
                w.append("- ").append(nodeName).append(" -");
            } else {
                w.append("&lt;").append(nodeName);
                if (node.getChildren().size() == 0) {
                    w.append("/");
                }
                w.append("&gt;");
            }
            w.append("</a>");
            w.append(LINE_SEPARATOR);

            // children
            for (int i = 0; i < node.getChildren().size(); i++) {
                TreeNode child = (TreeNode) node.getChildren().get(i);
                writeTreeNode(child, w, depth + 1, indentationStr);
            }

            // alternatives
            // FIXME when there's a cardinatlity of 0..1 and a default plugin, we shouldn't display all
            // QUestion: if we remove them, they don't get to appear at all in the hierarchy tree.
            for (int i = 0; i < node.getAlternatives().size(); i++) {
                TreeNode alternative = (TreeNode) node.getAlternatives().get(i);
                writeTreeNode(alternative, w, depth, indentationStr);
            }

            // closing tag if necessary
            if (node.getChildren().size() > 0 && ! isInterface) {
                indent(w, depth, indentationStr);
                w.append("&lt;").append("/").append(nodeName).append("&gt;").append(LINE_SEPARATOR);
            }
        }

        private void indent(Writer w, int depth, String indentationChar) throws IOException {
            for (int i = 0; i < depth; i++) {
                 w.append(indentationChar);
            }
        }

        public TreeNode getHierarchy(String name) {
            List leafNodes = this.getNodesByName(name);
            Set nodesInHierarchy = new HashSet();
            for (int i = 0; i < leafNodes.size(); i++) {
                TreeNode node = (TreeNode) leafNodes.get(i);
                do {
                    nodesInHierarchy.add(node);
                    node = node.getParent();
                } while (node != null);
            }
            // construct a tree that only contains the elements that lead to one of the leaves
            return generateTreeNode(nodesInHierarchy, root, null);
        }

        private PluginTreeNodeImpl generateTreeNode(Set nodesInHierarchy, TreeNode treeNode, PluginTreeNodeImpl parent) {
            PluginTreeNodeImpl parentNode = new PluginTreeNodeImpl(parent, (PluginModel) treeNode.getMe());
            for (int i = 0; i < treeNode.getChildren().size(); i++) {
                PluginTreeNodeImpl childNode = (PluginTreeNodeImpl) treeNode.getChildren().get(i);
                if (nodesInHierarchy.contains(childNode)) {
                    parentNode.addChild(generateTreeNode(nodesInHierarchy, childNode, parentNode));
                }
            }
            return parentNode;
        }

        private List getNodesByName(String name) {
            return getNodesByName(root, name);
        }

        private List getNodesByName(TreeNode node, String name) {
            List list = new ArrayList();
            if (((PluginModel)node.getMe()).isInterface()) {
                for (int i = 0; i < node.getAlternatives().size(); i++) {
                    list.addAll(getNodesByName((TreeNode) node.getAlternatives().get(i), name));
                }
            } else {
                if (name.equals(node.getNodeName())) {
                    list.add(node);
                }
                else {
                    for (int i = 0; i < node.getChildren().size(); i++) {
                        list.addAll(getNodesByName((TreeNode) node.getChildren().get(i), name));
                    }
                }
            }
            return list;
        }

        /**
         * @return the plugins listed alphabetically
         */
        public List getAlphabeticalPluginIndex() {
            List l = getFlattenedPluginTree();
            Collections.sort(l, new Comparator() {
                public int compare(Object o1, Object o2) {
                    TreeNode tn1 = (TreeNode) o1;
                    TreeNode tn2 = (TreeNode) o2;
                    return tn1.getNodeName().compareTo(tn2.getNodeName());
                }
            });
            return l;
        }
    }

    static RootRegistry rootRegistry = new RootRegistry();
    static URLClassLoader fullClasspathClassLoader;

    public static void main(String[] args) throws IOException {
        JavaDocBuilder builder = new JavaDocBuilder();
        
        builder.addSourceTree(new File("src"));

        ClassLibrary lib = builder.getClassLibrary();

        URL[] urls1 = new URL[] { new File("target", "classes").toURL() };
        URLClassLoader classLoader = new URLClassLoader(urls1);
        rootRegistry = loadRootRegistry(classLoader, "net/sourceforge/cruisecontrol/default-plugins.properties");

        URL[] urls = getDependenciesURLs("lib");
        ClassLoader jarsClassLoader = new URLClassLoader(urls);
        System.out.println("Found " + urls.length + " jar(s) under lib.");
        lib.addClassLoader(jarsClassLoader);

        URL[] classpath = new URL[urls.length + urls1.length];
        System.arraycopy(urls1, 0, classpath, 0, urls1.length);
        System.arraycopy(urls, 0, classpath, urls1.length, urls.length);
        fullClasspathClassLoader = new URLClassLoader(classpath);

        JavaSource[] sources = builder.getSources();

        Map models = new HashMap();
        // FIXME it would be great if we could make CC generic enough for this to not have to be there.
        // add things that cannot be generated today
        addMissingPluginModels(models);
        /*
        for (int i = 0; i < sources.length; i++) {
            handleClasses(models, sources[i].getClasses());
        }
        */
        // map all identified classes from the sources to their parent class and interface
        // that way one can find all implementations/sub-class of a particular type without
        // reparsing every class
        TypeToJavaClassMapping typesToClasses = new TypeToJavaClassMapping();
        for (int i = 0; i < sources.length; i++) {
            JavaSource source = sources[i];
            JavaClass[] classes = source.getClasses();
            for (int j = 0; j < classes.length; j++) {
                JavaClass aClass = classes[j];
                List types = findTypes(aClass);
                for (int k = 0; k < types.size(); k++) {
                    JavaClass type = (JavaClass) types.get(k);
                    typesToClasses.mapClassToType(type.getFullyQualifiedName(), aClass);
                }
            }
        }
        List typesToTreat = new ArrayList();
        typesToTreat.add("cruisecontrol");
        recursivelyHandleTypes(new HashSet(), typesToTreat, typesToClasses, models);

        printPluginMapping(models, System.out);

        PluginTreeNodeImpl treeRoot = generateTreeNode(models, rootRegistry);
        PluginModelTree tree = new PluginModelTree(treeRoot);

        // debugging
        printPluginTree(tree.getFlattenedPluginTree(), System.out, "  ");

        generateXMLModels(models);
        generateConfigXMLHtml(tree);
    }

    private static void addMissingPluginModels(Map models) {
        addPluginToModels(models, generateRootPluginModel());
        addPluginToModels(models, generatePluginPluginModel());
        addPluginToModels(models, generatePropertyPluginModel());
        addPluginToModels(models, generateSystemPluginModel());
        addPluginToModels(models, generateConfigurationPluginModel());
        addPluginToModels(models, generateThreadsPluginModel());
        addPluginToModels(models, generateIncludeProjectsPluginModel());
    }

    // fix peculiar things...
    private static PluginModel fixMissingPluginModelIfNecessary(PluginModel model) {
        if (model.type.equals("net.sourceforge.cruisecontrol.ProjectConfig")) {
            PluginModel.Child child = generatePluginPluginAsChild();
            model.children.children.add(0, child);
            child = generatePropertyPluginAsChild();
            model.children.children.add(0, child);
        }
        return model;
    }

    private static void fixMissingPluginModels(Map models) {
        // fix peculiar things...
        String projectPluginType = rootRegistry.getPluginType("project");
        PluginModel projectPluginModel = (PluginModel) models.get(projectPluginType);
        // System.out.println("Project plugin type for <project>: " + projectPluginType);
        // System.out.println("Project plugin model for <project>: " + projectPluginModel);

        PluginModel.Child child = generatePluginPluginAsChild();
        projectPluginModel.children.children.add(0, child);
        child = generatePropertyPluginAsChild();
        projectPluginModel.children.children.add(0, child);
    }

    private static void generateConfigXMLHtml(PluginModelTree tree) {
        try {
            ConfigXMLHtmlGenerator configXMLHtmlGenerator = new ConfigXMLHtmlGenerator();
            configXMLHtmlGenerator.init();
            configXMLHtmlGenerator.generate(tree);
        } catch (Throwable t) {
            System.err.println("Failed to generate HTML documentation " + t.getMessage());
            System.err.println("Classpath: " + System.getProperty("java.class.path"));
            t.printStackTrace(System.err);
        }
    }

    private static void generateXMLModels(Map models) throws IOException {
        File outputDir = new File("target" + File.separatorChar + "classes" + File.separatorChar + "META-INF");

        if (! outputDir.exists() && ! outputDir.mkdirs())
        {
            System.err.println("Couldn't create output directory to store the XML models: " + outputDir);
            System.err.println("Exiting.");
            System.exit(-1);
        }
        Iterator iterator = models.values().iterator();
        while (iterator.hasNext()) {
            PluginModel pluginModel = (PluginModel) iterator.next();
            String fileName;
            String type = pluginModel.type;
            if (pluginModel.type.equals(NO_TYPE)) {
                type = "pseudo-" + pluginModel.name;
            }
            fileName = type + ".xml";
            File file = new File(outputDir, fileName);
            writeOut(pluginModel, file);
        }

        System.out.println("Wrote " + models.size() + " plugin description(s) under " + outputDir);
    }

    /**
     * Little class to avoid recomputing the depth...
     */
    public static class PluginTreeEntryView {
        private int depth;
        private TreeNode node;

        public PluginTreeEntryView(int depth, TreeNode node) {
            this.depth = depth;
            this.node = node;
        }

        public int getDepth() {
            return depth;
        }

        public TreeNode getNode() {
            return node;
        }

        public PluginModel getModel() {
            return (PluginModel) node.getMe();
        }
    }

    private static void printPluginTree(List list, PrintStream out, String indentation) {
        out.println("********** Plugin Tree **********");
        for (int i = 0; i < list.size(); i++) {
            TreeNode node = (TreeNode) list.get(i);
            StringBuffer buf = new StringBuffer();
            TreeNode parent = node.getParent();
            while (parent != null) {
                buf.append(indentation);
                parent = parent.getParent();
            }
            buf.append(node.getNodeName());
            buf.append( ": ");
            PluginModel model = (PluginModel) node.getMe();
            buf.append(model.type);
            if (model.isInterface()) {
                buf.append(" +");
            }
            out.println(buf.toString());
        }
        out.println("*********************************");
    }

    private static PluginTreeNodeImpl generateTreeNode(Map models, RootRegistry rootRegistry) {
        PluginModel model = (PluginModel) models.get("cruisecontrol");
        return generateTree(model, null, models, rootRegistry);
    }

    private static PluginTreeNodeImpl generateTree(final PluginModel nodeModel, PluginTreeNodeImpl parent, Map models, RootRegistry rootRegistry) {
        final PluginTreeNodeImpl currentNode = new PluginTreeNodeImpl(parent, nodeModel);

        for (int i = 0; i < nodeModel.children.children.size(); i++) {
            PluginModel.Child child = (PluginModel.Child) nodeModel.children.children.get(i);

            PluginModel childModel = findPluginModel(child, models, rootRegistry);
            if (childModel != null) {
                currentNode.addChild(generateTree(childModel, currentNode, models, rootRegistry));
            } else {
                System.err.println("Couldn't find PluginModel for child " + child + " of " + nodeModel);
            }
        }

        // find all implementations and add them....
        Iterator iterator = models.values().iterator();
        while (iterator.hasNext()) {
            PluginModel pluginModel = (PluginModel) iterator.next();
            // identify sub-classes
            if (isPluginTypeCompatible(pluginModel, nodeModel.type) && nodeModel.isInterface()) {
                currentNode.addAlternative(generateTree(pluginModel, (PluginTreeNodeImpl) currentNode.getParent(), models, rootRegistry));
            }
        }
        // sort them. We sort them afterwards as we need them to be registered to their parent for getNodeName()
        // to be effective
        // we prioritize the default, i.e. the one with the same name as the interface
        // the rest is always alphabetical
        Comparator alternativeSorter = new Comparator() {
            public int compare(Object o1, Object o2) {
                PluginTreeNodeImpl t1 = (PluginTreeNodeImpl) o1;
                PluginTreeNodeImpl t2 = (PluginTreeNodeImpl) o2;
                if (nodeModel.isInterface()) {
                    if (t1.getNodeName().equals(currentNode.getNodeName())) {
                        return -1;
                    }
                    if (t2.getNodeName().equals(currentNode.getNodeName())) {
                        return +1;
                    }
                }
                return t1.getNodeName().compareTo(t2.getNodeName());
            }
        };
        Collections.sort(currentNode.alternatives, alternativeSorter);
        return currentNode;
    }

    private static boolean isPluginTypeCompatible(PluginModel pluginModel, String type) {
        if (type.equals(pluginModel.type)) {
            return false;
        }
        // FIXME we may want to resolve and store this info in the model instead.
        // This requires to have a correct classpath
        if (NO_TYPE.equals(type) || NO_TYPE.equals(pluginModel.type)) {
            return false;
        }
        try {
            Class interfaceClass = Class.forName(type, false, fullClasspathClassLoader);
            Class pluginClass = Class.forName(pluginModel.type, false, fullClasspathClassLoader);
            return interfaceClass.isAssignableFrom(pluginClass);
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: " + e.getMessage());
            return false;
        }
    }

    private static PluginModel findPluginModel(PluginModel.Child child, Map models, RootRegistry rootRegistry) {
        // if key specified, use the key, otherwise try with the name (for those plugins that do not have a
        // class that allows to autogenerate the model from)
        String key = child.type;
        if ((key == null || key.equals(NO_TYPE)) && child.name != null) {
            key = rootRegistry.getPluginType(child.name);
        }
        if (key != null) {
            PluginModel model = (PluginModel) models.get(key);
            if (model != null) {
                return model;
            }
        }
        if (child.name != null) {
            PluginModel model = (PluginModel) models.get(child.name);
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    private static void addPluginToModels(Map models, PluginModel model) {
        String nodeOrClassName = getNodeOrClasName(model);
        models.put(nodeOrClassName, model);

    }

    private static String getNodeOrClasName(PluginModel model) {
        if (model.type.equals(NO_TYPE)) {
            return model.name;
        } else {
            return model.type;
        }
    }

    private static RootRegistry loadRootRegistry(ClassLoader loader, String name) {
        Properties pluginDefinitions = new Properties();
        try {
            pluginDefinitions.load(loader.getResourceAsStream(name));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load plugin-definitions from default-plugins.properties: " + e);
        }
        for (Iterator iter = pluginDefinitions.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            rootRegistry.register((String) entry.getKey(), (String) entry.getValue());
        }
        return rootRegistry;
    }

    private static PluginModel generateRootPluginModel() {
        PluginModel pluginModel = new PluginModel();
        // pluginModel.parent = "/";
        pluginModel.name = "cruisecontrol";
        pluginModel.type = NO_TYPE; // net.sourceforge.cruisecontrol.CruiseControlConfig
        pluginModel.description = "<p>The <code>&lt;cruisecontrol&gt;</code> element is the root element " +
                "of the configuration, and acts as a container to the rest of the " +
                "configuration elements.</p>";


        PluginModel.Child child;

        child = generatePropertyPluginAsChild();
        pluginModel.children.children.add(child);

        child = generatePluginPluginAsChild();
        pluginModel.children.children.add(child);

        child = new PluginModel.Child();
        child.name = "include.projects";
        child.type = NO_TYPE;
        // child.cardinality = "0..*";
        child.description = "Add projects defined in other configuration files.";
        pluginModel.children.children.add(child);

        child = new PluginModel.Child();
        child.name = "system";
        child.type = NO_TYPE;
        // child.cardinality = "0..1";
        child.description = "Currently just a placeholder for the &lt;configuration&gt; element, " +
                "which in its turn is just a placeholder for the <a href=\"#threads\">&lt;threads&gt;</a> element.<br/> " +
                "We expect that in the future, more system-level features can be configured under this element.";
        pluginModel.children.children.add(child);

        child = new PluginModel.Child();
        child.name = "project";
        child.type = "net.sourceforge.cruisecontrol.ProjectConfig";
        // child.cardinality = "0..1";
        child.description = "Defines a basic unit of work.";
        pluginModel.children.children.add(child);

        return pluginModel;
    }

    private static PluginModel generatePluginPluginModel() {
        PluginModel pluginModel = new PluginModel();
        // pluginModel.parent = ;
        pluginModel.type = NO_TYPE;
        pluginModel.name = "plugin";
        pluginModel.description = "<p>A <code>&lt;plugin&gt;</code> element registers a classname with an " +
                "alias for use within the configuration file.</p>" +
                "<p>Plugins can also be <a href=\"plugins.html#preconfiguration\">pre-configured</a> " +
                "at registration time. This can greatly reduce the configuration file size.</p>" +
                "<p>The <a href=\"plugins.html\">plugins</a> page contains a discussion of " +
                "the plugin architecture used with CruiseControl.</p>";

        PluginModel.Attribute attribute;
        attribute = new PluginModel.Attribute();
        attribute.name = "name";
        attribute.required = "true";
        attribute.type = "java.lang.String";
        // child.cardinality = "0..1";
        attribute.description = "The alias used to refer to the plugin elsewhere in the configuration file.";
        pluginModel.attributes.attributes.add(attribute);

        attribute = new PluginModel.Attribute();
        attribute.name = "classname";
        attribute.required = "true";
        attribute.type = "java.lang.String";
        // child.cardinality = "0..1";
        attribute.description = "The class that implements the plugin.";
        pluginModel.attributes.attributes.add(attribute);

        return pluginModel;
    }

    private static PluginModel generatePropertyPluginModel() {
        PluginModel.Child child = generatePropertyPluginAsChild();

        PluginModel pluginModel = new PluginModel();
        // pluginModel.parent = ;
        pluginModel.type = NO_TYPE;
        pluginModel.name = child.name;
        pluginModel.description = "        <p>The <code>&lt;property&gt;</code> element is used to set a property (or set of properties)\n" +
                "        within the CruiseControl configuration file. Properties may be set at the global level\n" +
                "        and/or within the scope of a project. There are three ways to set properties within CruiseControl:</p>\n" +
                "\n" +
                "        <ol>\n" +
                "            <li>By supplying both the name and value attributes.</li>\n" +
                "            <li>By setting the file attribute with the filename of the property file to load.\n" +
                "                This property file must follow the format defined by the class java.util.Properties,\n" +
                "                with the same rules about how non-ISO8859-1 characters must be escaped.</li>\n" +
                "            <li>By setting the environment attribute with a prefix to use. Properties will be defined\n" +
                "                for every environment variable by prefixing the supplied name and a period to the name\n" +
                "                of the variable.</li>\n" +
                "\n" +
                "        </ol>\n" +
                "\n" +
                "        <p>Properties in CruiseControl are <i>not entirely</i> immutable: whoever sets a property <i>last</i>\n" +
                "        will freeze it's value <i>within the scope in which the property was set</i>. In other words,\n" +
                "        you may define a property at the global level, then eclipse this value within the scope of a single\n" +
                "        project by redefining the property within that project. You may not, however, set a property more\n" +
                "        than once within the same scope. If you do so, only the last assignment will be used.</p>\n" +
                "\n" +
                "        <p>Just as in Ant, the value part of a property being set may contain references to other properties.\n" +
                "        These references are resolved at the time these properties are set. This also holds for properties\n" +
                "        loaded from a property file, or from the environment.</p>\n" +
                "\n" +
                "        <p>Also note that the property <code>${project.name}</code> is set for you automatically and will always resolve\n" +
                "        to the name of the project currently being serviced - even outside the scope of the project\n" +
                "        definition.</p>\n" +
                "\n" +
                "        <p>Finally, note that properties bring their best when combined with\n" +
                "        <a href=\"plugins.html#preconfiguration\">plugin preconfigurations</a>.\n" +
                "        </p>";


        String required = "Exactly one of name, environment, or file.";
        PluginModel.Attribute attribute;
        attribute = new PluginModel.Attribute();
        attribute.name = "name";
        attribute.required = required;
        attribute.type = "java.lang.String";
        attribute.description = "The name of the property to set.";
        pluginModel.attributes.attributes.add(attribute);

        attribute = new PluginModel.Attribute();
        attribute.name = "environment";
        attribute.required = required;
        attribute.type = "java.lang.String";
        attribute.description = "The prefix to use when retrieving environment variables. Thus if you specify environment=\"myenv\" you will be able to access OS-specific environment variables via property names such as \"myenv.PATH\" or \"myenv.MAVEN_HOME\".";
        pluginModel.attributes.attributes.add(attribute);

        attribute = new PluginModel.Attribute();
        attribute.name = "environment";
        attribute.required = required;
        attribute.type = "java.lang.String";
        attribute.description = "The prefix to use when retrieving environment variables. Thus if you specify environment=\"myenv\" you will be able to access OS-specific environment variables via property names such as \"myenv.PATH\" or \"myenv.MAVEN_HOME\".";
        pluginModel.attributes.attributes.add(attribute);

        attribute = new PluginModel.Attribute();
        attribute.name = "file";
        attribute.required = required;
        attribute.type = "java.lang.String";
        attribute.description = "The filename of the property file to load.";
        pluginModel.attributes.attributes.add(attribute);

        attribute = new PluginModel.Attribute();
        attribute.name = "value";
        attribute.required = "Yes, if name was set.";
        attribute.type = "java.lang.String";
        attribute.description = "The value of the property. This may contain any previously defined properties.";
        pluginModel.attributes.attributes.add(attribute);

        attribute = new PluginModel.Attribute();
        attribute.name = "topper";
        attribute.type = "java.lang.String";
        attribute.description = "Used in conjunction with <code>environment</code>. If set to <code>true</code>, all environment variable names will be converted to upper case.";
        pluginModel.attributes.attributes.add(attribute);

        return pluginModel;
    }

    private static PluginModel generateSystemPluginModel() {
        PluginModel pluginModel = new PluginModel();
        // pluginModel.parent = ;
        pluginModel.type = NO_TYPE;
        pluginModel.name = "system";
        pluginModel.description = "<p>FIXME</p>";

        PluginModel.Child attribute;
        attribute = new PluginModel.Child();
        attribute.name = "configuration";
        // child.cardinality = "0..1";
        attribute.type = NO_TYPE;
        attribute.description = "FIXME.";
        pluginModel.children.children.add(attribute);

        return pluginModel;
    }

    private static PluginModel generateConfigurationPluginModel() {
        PluginModel pluginModel = new PluginModel();
        // pluginModel.parent = ;
        pluginModel.type = NO_TYPE;
        pluginModel.name = "configuration";
        pluginModel.description = "<p>FIXME.</p>";

        PluginModel.Child child;
        child = new PluginModel.Child();
        child.name = "threads";
        // child.cardinality = "0..1";
        child.type = NO_TYPE;
        child.description = "FIXME.";
        pluginModel.children.children.add(child);

        return pluginModel;
    }

    private static PluginModel generateThreadsPluginModel() {
        PluginModel pluginModel = new PluginModel();
        // pluginModel.parent = ;
        pluginModel.type = NO_TYPE;
        pluginModel.name = "threads";
        pluginModel.description = "<p>The <code>&lt;threads&gt;</code> element can be used to configure the number of threads that CruiseControl can use " +
                "simultaneously to build projects. This is done through the <code>count</code> attribute. " +
                "If this element (or one of its parent elements) is not specified, this defaults to 1. " +
                "This means that only one project will be built at a time. Raise this number if your server has enough resources to " +
                "build multiple projects simultaneously (especially useful on multi-processor systems). If more projects than the maximum " +
                "number of threads are scheduled to run at a given moment, the extra projects will be queued.</p>";

        PluginModel.Attribute attribute;
        attribute = new PluginModel.Attribute();
        attribute.name = "count";
        attribute.required = "true";
        attribute.type = "integer";
        attribute.description = "Maximum number of threads to be in use simultaneously to build projects.";
        pluginModel.attributes.attributes.add(attribute);

        return pluginModel;
    }

    private static PluginModel generateIncludeProjectsPluginModel() {
        PluginModel pluginModel = new PluginModel();
        // pluginModel.parent = ;
        pluginModel.type = NO_TYPE;
        pluginModel.name = "include.projects";
        pluginModel.description = "<p>The &lt;include.projects&gt; tag is used to consolidate several configuration " +
                "files into a single configuration. One advantage over using XML includes are that " +
                "the target files are valid configuration files in their own right and not just XML " +
                "fragments. Also, including projects using the tag is less fragile as an error in one " +
                "file will not keep the rest of the projects for building.</p> " +
                "<p>Configuration files included this way are processed with the properties and " +
                "plugins defined in the main configuration file, which easily allows per instance " +
                "configuration. Properties and plugins defined in the processed files are not made " +
                "available outside the scope of that file.</p> " +
                "<p>Project names must still remain unique. The first project with a given name will " +
                "be loaded and any subsequent projects attempting to use the same name will be skipped.</p>";

        PluginModel.Attribute attribute;
        attribute = new PluginModel.Attribute();
        attribute.name = "file";
        attribute.required = "true";
        attribute.type = "java.lang.String";
        attribute.description = "Relative path from current configuration file to the configuration file to process.";
        pluginModel.attributes.attributes.add(attribute);

        return pluginModel;
    }

    private static PluginModel.Child generatePluginPluginAsChild() {
        PluginModel.Child child = new PluginModel.Child();
        child.name = "plugin";
        child.type = NO_TYPE;
        // child.cardinality = "0..*";
        child.description = "Registers a classname with an alias.";
        return child;
    }

    private static PluginModel.Child generatePropertyPluginAsChild() {
        PluginModel.Child child = new PluginModel.Child();
        child.name = "property";
        child.type = NO_TYPE;
        // child.cardinality = "0..*";
        child.description = "Defines a name/value pair used in configuration.";
        return child;
    }

    private static void printPluginMapping(Map models, PrintStream out) {
        out.println("********** Plugin Mapping **********");
        Iterator iterator2 = models.values().iterator();
        while (iterator2.hasNext()) {
            PluginModel pluginModel = (PluginModel) iterator2.next();
            out.println("Plugin: " + pluginModel.type);
        }
        out.println("*************************************");
    }

    private static void writeOut(PluginModel pluginModel, File file) throws IOException {
        FileWriter writer = new FileWriter(file);

        try {
            XmlOutputerImpl outputer = new XmlOutputerImpl();
            pluginModel.toXml(outputer);
            writer.write(outputer.toString());
            System.out.println("Wrote " + file);
        } finally {
            writer.close();
        }
    }

    private static void handleClasses(Map models, JavaClass[] classes) {
        for (int i = 0; i < classes.length; i++) {
            JavaClass class1 = classes[i];
            handleClass(models, class1);
        }
    }

    /**
     * @param aClass
     * @return a List of JavaClass parent or interface of this class
     */
    private static List findTypes(JavaClass aClass) {
        List types = new ArrayList();
        while (aClass != null && ! aClass.getFullyQualifiedName().equals("java.lang.Object")) {
            JavaClass[] interfaces = aClass.getImplementedInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                JavaClass anInterface = interfaces[i];
                types.add(anInterface);
            }
            types.add(aClass);
            aClass = aClass.getSuperJavaClass();
        }
        return types;
    }

    static class TypeToJavaClassMapping {
        Map mapping = new HashMap();

        public void mapClassToType(String type, JavaClass aClass) {
            getClassesForType(type).add(aClass);
        }

        public List getClassesForType(String type) {
            List result = (List) mapping.get(type);
            if (result == null) {
                result = new ArrayList();
                mapping.put(type, result);
            }
            return result;
        }
    }

    private static void recursivelyHandleTypes(Set handledTypes, List typesToTreat, TypeToJavaClassMapping mappings, Map models) {
        if (typesToTreat.size() == 0) {
            return;
        }
        String nodeOrClasName = (String) typesToTreat.remove(0);
        if (! handledTypes.contains(nodeOrClasName)) {
            handledTypes.add(nodeOrClasName);

            // if it's a node, that means the plugin is not yet parseable
            PluginModel model = (PluginModel) models.get(nodeOrClasName);
            List pluginModelsToTreat = new ArrayList();
            if (model != null) {
                pluginModelsToTreat.add(model);
            } else {
                List classesForType = mappings.getClassesForType(nodeOrClasName);
                /*
                if (classesForType.size() == 0) {
                    classesForType = mappings.getClassesForType(rootRegistry.getPluginType(nodeOrClasName));
                }*/
                for (int i = 0; i < classesForType.size(); i++) {
                    JavaClass javaClass = (JavaClass) classesForType.get(i);
                    PluginModel pluginModel = (PluginModel) models.get(javaClass.getFullyQualifiedName());
                    if (pluginModel == null) {
                        pluginModel = (PluginModel) models.get(rootRegistry.getPluginName(javaClass.getFullyQualifiedName()));
                    }
                    // can we find extract it from the source ?
                    if (pluginModel == null) {
                        pluginModel = extractPluginModel2(javaClass);
                        addPluginToModels(models, pluginModel);

                        pluginModelsToTreat.add(pluginModel);
                    }
                }
            }
            for (int i = 0; i < pluginModelsToTreat.size(); i++) {
                PluginModel pluginModel = (PluginModel) pluginModelsToTreat.get(i);

                for (int j = 0; j < pluginModel.getChildren().children.size(); j++) {
                    PluginModel.Child child = (PluginModel.Child) pluginModel.getChildren().children.get(j);
                    String nodeOrClasNameToTreat = getNodeOrClasNameToTreat(child);
                    typesToTreat.add(nodeOrClasNameToTreat);
                }
            }
        }
        recursivelyHandleTypes(handledTypes, typesToTreat, mappings, models);
    }

    private static String getNodeOrClasNameToTreat(PluginModel.Child child) {
        if (child.getType().equals(NO_TYPE)) {
            return child.getName();
        } else {
            return child.getType();
        }
    }

    private static void handleClass(Map models, JavaClass class1) {
        PluginModel pluginModel = extractPluginModelFromSource(class1);
        if (pluginModel != null) {
            addPluginToModels(models, pluginModel);
        }
        handleClasses(models, class1.getNestedClasses());
    }

    private static PluginModel extractPluginModel2(JavaClass class1) {
        PluginModel realModel = extractPluginModelFromSource(class1);

        return fixMissingPluginModelIfNecessary(realModel);
    }

    /**
     * FIXME document
     * Should only be called for known plugin
     * @param class1
     * @return
     */
    private static PluginModel extractPluginModelFromSource(JavaClass class1) {
        PluginModel model;
        model = new PluginModel();
        model.type = class1.getFullyQualifiedName();
        model.registryName = rootRegistry.getPluginName(model.type);
        // model.name = tag.getNamedParameter("name");
        model.description = class1.getComment();
        model.isInterface = class1.isInterface();


        JavaMethod[] methods = class1.getMethods();
        for (int i = 0; i < methods.length; i++) {
            JavaMethod method = methods[i];

            if (method.getTagByName("cc-config-skip") != null) {
                continue;
            }
            if (method.isPublic() && method.getName().startsWith("set")) {
                PluginModel.Attribute attribute = new PluginModel.Attribute();
                String fieldName = getSetterFieldName(method);
                attribute.name = fieldName;
                attribute.type = method.getParameters()[0].getType().toString();
                attribute.required = method.getTagByName("required") == null ? "false" : "true";
                attribute.description = method.getComment();
                JavaField field = getClassFieldByNameIgnoreCase(class1, fieldName);
                if (method.getTagByName("defaultValue") != null) {
                        String value = method.getTagByName("defaultValue").getValue();
                        if (value.trim().equals("")) {
                            throw new IllegalArgumentException("defaultValue for field " + field
                                    + " in class " + class1.getName() + " is empty.");
                        }
                        attribute.defaultValue = value;
                } else if (field != null) {
                    String expression = field.getInitializationExpression().replace('\r', ' ').trim();
                    if (! expression.equals("")) {
                        attribute.defaultValue = expression;
                    } else {
                        attribute.defaultValue = getDefaultValue(field.getType().toString());
                    }
                }
                model.attributes.attributes.add(attribute);
            } else if (method.isPublic() && method.getName().startsWith("add")) {
                PluginModel.Child child = new PluginModel.Child();
                JavaParameter param = method.getParameters()[0];
                child.name = param.getName().toLowerCase();
                child.type = param.getType().toString();
                /*
                DocletTag cardinality = method.getTagByName("cardinality");
                if (cardinality != null) {
                    child.cardinality = cardinality.getValue();
                }
                */
                child.description = method.getComment();
                model.children.children.add(child);
            }
        }
        return model;
    }

    private static String getSetterFieldName(JavaMethod method)
    {
        String fieldName = method.getName().substring(3);
        fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        // fieldName = fieldName.toLowerCase();
        return fieldName;
    }

    private static JavaField getClassFieldByNameIgnoreCase(JavaClass class1, String fieldName) {
        for (int i = 0; i < class1.getFields().length; i++) {
            JavaField field = class1.getFields()[i];
            if (field.getName().equalsIgnoreCase(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private static String getDefaultValue(String type) {
        if (type.equals("boolean")) {
            return "false";
        } else if (type.equals("int") || type.equals("long") || type.equals("short")) {
            return "0";
        } else if (type.equals("float") || type.equals("double")) {
            return "0.0";
        } else if (type.equals("char")) {
            return null;
        }
        return null;

    }

    private static URL[] getDependenciesURLs(String dirPath) {
        File dir = new File(dirPath);
        List urls = new ArrayList();
        recursivelyCollectJarsFromDir(dir, urls);
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    private static void recursivelyCollectJarsFromDir(File dir, List urls) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    recursivelyCollectJarsFromDir(file, urls);
                } else {
                    if (file.getName().endsWith(".jar")) {
                        try {
                            urls.add(file.toURL());
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}

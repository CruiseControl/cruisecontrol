/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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
package net.sourceforge.cruisecontrol.config;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.Schedule;
import net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher;
import net.sourceforge.cruisecontrol.util.Util;
import org.jdom.Element;
import org.jdom.Attribute;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.io.File;
import java.util.List;
import java.util.Iterator;

/**
 * GUI editor for CruiseControl configuration files.
 *
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class ConfigurationEditor {

    private JFrame mainFrame;
    private final ConfigurationContext context = new ConfigurationContext();
    private JSplitPane splitPane;

    /**
     * Constructs a new Frame (window) used to edit CC config files and
     * shows the window.
     */
    public ConfigurationEditor() throws IntrospectionException {
        //context.setConfigurationFile(new File("C:/pdj/ccsandbox/config.xml"));

        mainFrame = new JFrame("CruiseControl Configuration Editor");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 500);
        mainFrame.setResizable(true);

        JMenuBar menuBar = new JMenuBar();
        mainFrame.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem open = new JMenuItem("Open Configuration File...");
        fileMenu.add(open);

        open.addActionListener(
                new OpenConfigurationFileListener(mainFrame, context, this));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        refreshSplitPane();

        mainFrame.getContentPane().add(splitPane);

        mainFrame.setVisible(true);
    }

    private void refreshSplitPane() throws IntrospectionException {
        JPanel editorPanel = new PluginEditorPane(null);
        editorPanel.setBackground(Color.lightGray);

        JScrollPane treePanel = new JScrollPane(createTree(splitPane));
        treePanel.setBackground(Color.gray);

        splitPane.setLeftComponent(treePanel);
        splitPane.setRightComponent(editorPanel);
        splitPane.setDividerLocation(200);
    }

    public JTree createTree(final JSplitPane splitPane) throws IntrospectionException {
        DefaultMutableTreeNode top =  null;
        if (context.getConfigurationFile() == null) {
            top = new DefaultMutableTreeNode("Open a CruiseControl Configuration File using File/Open...");
            final JTree tree = new JTree(top);
            tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
            return tree;
        }
        top = new DefaultMutableTreeNode("CruiseControl");


        try {
            Element rootElem = Util.loadConfigFile(context.getConfigurationFile());
            System.out.println("First element: " + rootElem.getName());

            addChildrenToTree(rootElem, top);
        } catch (CruiseControlException e) {
            e.printStackTrace();
        }


        final JTree tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        //Listen for when the user selects a different node in the tree.
        //  When they do, show them the proper editor in the right pane.
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        tree.getLastSelectedPathComponent();

                if (node == null) {
                    return;
                }

                Object nodeInfo = node.getUserObject();
                if (nodeInfo instanceof PluginTreeNodeData) {
                    splitPane.setRightComponent(((PluginTreeNodeData) nodeInfo).getEditorPane());
                } else {
                    //Only know how to show PluginTreeNodeData instances, so show a blank panel.
                    splitPane.setRightComponent(new JPanel());
                }
            }
        });

        return tree;
    }

    private void addChildrenToTree(Element rootElem, DefaultMutableTreeNode top)
            throws IntrospectionException, CruiseControlException {

        //Traverse the elements and build tree nodes
        List children = rootElem.getChildren();
        PluginRegistry registry = PluginRegistry.createRegistry();
        for (int i = 0; i < children.size(); i++) {
            Element nextElem = (Element) children.get(i);

            final String elemName = nextElem.getName();
            final PluginEditorPane editorPane = new PluginEditorPane(registry.getPluginClass(elemName));
            DefaultMutableTreeNode nextNode =
                    new DefaultMutableTreeNode(
                            new PluginTreeNodeData(editorPane, elemName));
            top.add(nextNode);
            List attributes = nextElem.getAttributes();
            for (Iterator iter = attributes.iterator(); iter.hasNext();) {
                Attribute nextAttr = (Attribute) iter.next();
                final String fieldName = nextAttr.getName();
                final String fieldValue = nextAttr.getValue();
                System.out.println("Setting next field [" + fieldName
                        + "] to [" + fieldValue + "].");
                editorPane.setFieldValue(fieldName, fieldValue);
            }

            addChildrenToTree(nextElem, nextNode);
        }
    }

    /**
     * Creates a JTree used during implementation of the ConfigurationEditor.
     * This method can be thrown away.
     */
    public static JTree createTestingTree(final JSplitPane splitPane) throws IntrospectionException {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("CruiseControl");

        DefaultMutableTreeNode firstProject = new DefaultMutableTreeNode(
                new PluginTreeNodeData(new PluginEditorPane(Project.class),
                        "Project 1"));
        top.add(firstProject);

        firstProject.add(new DefaultMutableTreeNode(
                new PluginTreeNodeData(new PluginEditorPane(AntBuilder.class), "Builder")));
        firstProject.add(new DefaultMutableTreeNode(
                new PluginTreeNodeData(new PluginEditorPane(CVSBootstrapper.class), "Bootstrapper")));
        firstProject.add(new DefaultMutableTreeNode(
                new PluginTreeNodeData(new PluginEditorPane(Schedule.class), "Schedule")));
        firstProject.add(new DefaultMutableTreeNode(
                new PluginTreeNodeData(new PluginEditorPane(LinkEmailPublisher.class), "Publisher")));

        top.add(new DefaultMutableTreeNode(
                new PluginTreeNodeData(new PluginEditorPane(Project.class), "Project 2"), true));

        final JTree tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        //Listen for when the user selects a different node in the tree.
        //  When they do, show them the proper editor in the right pane.
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        tree.getLastSelectedPathComponent();

                if (node == null) {
                    return;
                }

                Object nodeInfo = node.getUserObject();
                if (nodeInfo instanceof PluginTreeNodeData) {
                    splitPane.setRightComponent(((PluginTreeNodeData) nodeInfo).getEditorPane());
                } else {
                    //Only know how to show PluginTreeNodeData instances, so show a blank panel.
                    splitPane.setRightComponent(new JPanel());
                }
            }
        });

        return tree;
    }

    public static class PluginTreeNodeData {
        private PluginEditorPane editorPane;
        private String name;

        public PluginTreeNodeData(PluginEditorPane editorPane, String name) {
            this.editorPane = editorPane;
            this.name = name;
        }

        public PluginEditorPane getEditorPane() {
            return editorPane;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }
    }

    /**
     * Should be registered as the listener on the File/Open file... menu
     * option.
     */
    private static class OpenConfigurationFileListener
            implements ActionListener {

        private JFrame mainFrame;
        private ConfigurationContext context;
        private ConfigurationEditor editor;

        public OpenConfigurationFileListener(JFrame mainFrame,
                                             ConfigurationContext context,
                                             ConfigurationEditor editor) {
            this.mainFrame = mainFrame;
            this.context = context;
            this.editor = editor;
        }

        /**
         * Called when the user selects the File/Open file... menu
         * option. Lets the user select a CruiseControl configuration
         * file. If the user selects a new file, then the ConfigurationContext
         * will be updated. If the user cancels, then nothing changes.
         */
        public void actionPerformed(ActionEvent event) {
            JFileChooser chooser = new JFileChooser();
            int returnValue = chooser.showOpenDialog(mainFrame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                System.out.println("User selected a file.");
                File chosenFile = chooser.getSelectedFile();
                System.out.println("The file was: " + chosenFile.getAbsolutePath());
                context.setConfigurationFile(chosenFile);
                try {
                    editor.refreshSplitPane();
                } catch (IntrospectionException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("User cancelled...");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new ConfigurationEditor();
    }
}
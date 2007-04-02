package net.sourceforge.cruisecontrol.gendoc;

import java.util.List;
import java.util.ArrayList;

/**
 * A TreeNode for our plugin tree. The {@link #getMe()} returns a {@link PluginModel} instance.
 * @author jerome@coffeebreaks.org
 */
public class PluginTreeNodeImpl implements TreeNode {
    PluginTreeNodeImpl parent;
    PluginModel me;
    List children = new ArrayList();
    List alternatives = new ArrayList();

    public PluginTreeNodeImpl(PluginTreeNodeImpl parent, PluginModel me) {
        this.parent = parent;
        this.me = me;
    }

    public void addChild(PluginTreeNodeImpl child) {
        if (child.parent != this) {
            throw new IllegalStateException("parent differ: parent: " + child.parent + " - this: " + this);
        }
        children.add(child);
    }

    public Object getMe() {
        return me;
    }

    public String getNodeName() {
        if (me.name != null) {
            return me.name;
        }
        if (me.registryName != null) {
            return me.registryName;
        }

        // System.err.println("NodeName not found. Probably not a real plugin... " + model);
        // search for a name...
        if (this.getParent() != null) {
            PluginModel parentModel = (PluginModel) ((TreeNode) this.getParent()).getMe();
            for (int i = 0; i < parentModel.children.children.size(); i++) {
                PluginModel.Child child = (PluginModel.Child) parentModel.children.children.get(i);
                if (child.type.equals(me.type)) {
                    return child.name;
                }
            }
        }    
        return "FIXME-" + this.me.type.substring(this.me.type.lastIndexOf('.') + 1);
    }

    public PluginModel getMeAsPlugin() {
        return me;
    }

    public TreeNode getParent() {
        return parent;
    }

    public List getChildren() {
        return children;
    }

    public List getAlternatives() {
        return alternatives;
    }

    /**
     * Used to add an alternative (usually an implementation of the current node that is an interface)
     * @param alternative
     */
    public void addAlternative(PluginTreeNodeImpl alternative) {
        if (! this.me.isInterface()) {
            throw new IllegalArgumentException("Alternative can only be added to 'interface' nodes");
        }
        if (alternative.parent != this.getParent()) {
            throw new IllegalStateException("parent differ: parent: " + alternative.parent + " - this: " + this.getParent());
        }
        alternatives.add(alternative);
    }

    public String toString() {
        return "Node for - " + me;
    }
}

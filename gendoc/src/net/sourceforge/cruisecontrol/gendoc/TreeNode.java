package net.sourceforge.cruisecontrol.gendoc;

import java.util.List;

/**
 * Generic interface for tree nodes.
 *
 * The only specificity if the {@link #getAlternatives()} method.
 * @author jerome@coffeebreaks.org
 */
public interface TreeNode {
    /**
     * @return the parent node
     */
    TreeNode getParent();

    /**
     * @return the object stored in that node
     */
    Object getMe();

    /**
     * @return a name for the node
     */
    String getNodeName();

    /**
     * @return the children
     */
    List getChildren();

    /**
     * @return the alternative nodes
     */
    List getAlternatives();
}

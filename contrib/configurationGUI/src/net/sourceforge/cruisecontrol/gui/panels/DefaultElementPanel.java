package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sourceforge.cruisecontrol.gui.ProjectBrowser;

import org.arch4j.ui.components.PropertiesPanel;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * A pane for editing the details of a CruiseControl plugin -- an AntBuilder,
 * ModificationSet, etc.
 */
public class DefaultElementPanel extends PropertiesPanel implements EditorPanel {

    /**
     * Fields on this pane with the key being, i.e. Component.getName(). Every
     * element in this map must be at least a component. The key is always
     * lower case.
     */
    private final Map fieldMap = new HashMap();

    private Element element;
    
    public DefaultElementPanel(Class builderClass) throws IntrospectionException {

        if (builderClass != null) {
            addFields(builderClass);
        }
    }
    
    public void setElement( Element anElement ) {
    
    	element = anElement;
    	
        List attributes = anElement.getAttributes();
        for (Iterator iter = attributes.iterator(); iter.hasNext();) {
            Attribute nextAttr = (Attribute) iter.next();
            String fieldName = nextAttr.getName();
            String fieldValue = nextAttr.getValue();

            setFieldValue(fieldName, fieldValue);
        }
    }
    
    public void setProjectBrowser( ProjectBrowser aBrowser ) {
    	
    }
    
    private void setFieldValue(String fieldName, String value) {
        Component component = (Component) fieldMap.get(fieldName.toLowerCase());

        if (component instanceof JTextField) {
            ((JTextField) component).setText(value);
        }
        else {
        	boolean theValue = Boolean.valueOf( value ).booleanValue();
        	((JCheckBox) component).setSelected( theValue );
        }
    }

    private void addFields( Class builderClass )
            throws IntrospectionException {

    	setFieldOffset( 150 );
    	
        BeanInfo info = Introspector.getBeanInfo(builderClass);
        PropertyDescriptor[] properties = info.getPropertyDescriptors();

        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor nextProperty = properties[i];

            //Ignore the "class" property
            if ("class".equals(nextProperty.getName())) {
                continue;
            }
            if (nextProperty.getWriteMethod() == null) {
                continue;
            }

            String name = nextProperty.getName();

            Component nextField = null;
            if (nextProperty.getPropertyType() == boolean.class) {
                nextField = addCheckBox( new JLabel( name ) );
            } else {
                nextField = addTextField( new JLabel(name), null );
                
                // set the value on a focus lost event
                nextField.addFocusListener(
                		new FocusAdapter() {
                			
    						public void focusLost(FocusEvent e) {
    							
    							JTextField theField = (JTextField) e.getComponent();
    							String fieldName = theField.getName();
    							
    							element.setAttribute( fieldName, theField.getText() );
    						}
                		});
            }
            
            nextField.setName(name);
            fieldMap.put(name.toLowerCase(), nextField);
        }
    }

    public String getSimpleClassname(Class theClass) {
        String fullName = theClass.getName();
        int lastDotIndex = fullName.lastIndexOf(".");

        return fullName.substring(lastDotIndex + 1);
    }
    

    /**
     * Get the size to use when determining scrolling.
     */
	public Dimension getPreferredSize() {


		return new Dimension( y, 400 );
	}
}

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

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Component;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A pane for editing the details of a CruiseControl plugin -- an AntBuilder,
 * ModificationSet, etc.
 *
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class PluginEditorPane extends JPanel {

    /**
     * Fields on this pane with the key being, i.e. Component.getName(). Every
     * element in this map must be at least a component. The key is always
     * lower case.
     */
    private final Map fieldMap = new HashMap();

    public PluginEditorPane(Class builderClass) throws IntrospectionException {
        this.setLayout(new BorderLayout());
        GridBagLayout gridBag = new GridBagLayout();
        JPanel fieldPane = new JPanel(gridBag);
        fieldPane.setBorder(BorderFactory.createEmptyBorder(20, 35,
                                                            20, 35));
        this.add(new JScrollPane(fieldPane));

        if (builderClass != null) {
            addFields(builderClass, gridBag, fieldPane);
        }
    }

    public void setFieldValue(String fieldName, String value) {
        Component component = (Component) fieldMap.get(fieldName.toLowerCase());

        if (component instanceof JTextField) {
            ((JTextField) component).setText(value);
        }
    }

    private void addFields(Class builderClass, GridBagLayout gridBag,
                           JPanel fieldPane)
            throws IntrospectionException {


        String editorName = getSimpleClassname(builderClass) + " Editor";
        this.add(new JLabel(editorName), BorderLayout.NORTH);
        BeanInfo info = Introspector.getBeanInfo(builderClass);
        PropertyDescriptor[] properties = info.getPropertyDescriptors();

        List labels = new ArrayList();
        List fields = new ArrayList();

        final String[] booleanOptions = new String[]{"", "true", "false"};
        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor nextProperty = properties[i];
            System.out.println("Next property name [" + nextProperty.getName() + "]");
            //Ignore the "class" property
            if ("class".equals(nextProperty.getName())) {
                continue;
            }
            if (nextProperty.getWriteMethod() == null) {
                continue;
            }

            String name = nextProperty.getName();
            labels.add(new JLabel(name));

            Component nextField = null;
            if (nextProperty.getPropertyType() == boolean.class) {
                JComboBox booleanCombo = new JComboBox(booleanOptions);
                booleanCombo.setBackground(Color.white);
                nextField = booleanCombo;
            } else {
                nextField = new JTextField(25);
            }
            nextField.setName(name);
            fields.add(nextField);
            fieldMap.put(name.toLowerCase(), nextField);
        }

        addLabelTextRows((JLabel[]) labels.toArray(new JLabel[0]),
                         (JComponent[]) fields.toArray(new JComponent[0]),
                         gridBag,
                         fieldPane
        );
    }

    private static void addLabelTextRows(JLabel[] labels,
                                         JComponent[] textFields,
                                         GridBagLayout gridbag,
                                         Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(5, 5, 5, 5);
        int numLabels = labels.length;

        for (int i = 0; i < numLabels; i++) {
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default
            gridbag.setConstraints(labels[i], c);
            container.add(labels[i]);

            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridbag.setConstraints(textFields[i], c);
            container.add(textFields[i]);
        }
    }

    public String getSimpleClassname(Class theClass) {
        String fullName = theClass.getName();
        int lastDotIndex = fullName.lastIndexOf(".");

        return fullName.substring(lastDotIndex + 1);
    }
}

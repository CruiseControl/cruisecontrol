package net.sourceforge.cruisecontrol.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.arch4j.ui.components.PropertiesPanel;

/**
 * @author Allan Wick
 */
public class SelectionPanel extends PropertiesPanel {

	private ArrayList selections;
	private String selectedType;

	private boolean wasCancelled = true;
	
	public SelectionPanel( ArrayList aSelectionList ) {
		
		selections = aSelectionList;
		
		build();
	}
	
	public void addComponents() {
		// do nothing to remove header area
	}
	
	public void build() {
		
		ButtonGroup theGroup = new ButtonGroup();
		
		for( Iterator iter=selections.iterator();iter.hasNext(); ) {
			
			String theText = (String) iter.next();
			JRadioButton theOption = new JRadioButton( theText );
			theOption.addActionListener( new ActionListener() {
				
				public void actionPerformed( ActionEvent anEvent ) {
					
					JRadioButton theSource = (JRadioButton) anEvent.getSource();
					
					selectedType = theSource.getText();
				}
			
			});
			
			setLeftMargin(5);
			
			addComponentRelativeToRight( theOption, 0 );
			positionToNextLine();
			
			theGroup.add( theOption );
		}
		
		positionToNextLine();
		
		addOkCancelButtons();
	}

	public int getPanelHeight() {
		
		int size = selections.size() * yIncrement;
		
		// add some room for buttons
		size += 100;
		
		return size < 300?300:size;
	}
	
	public Dimension getPreferredSize() {
		
		return new Dimension( 400, getPanelHeight() );
	}
	
	public String getSelectedType() {
		
		return selectedType;
	}
	
	public void okPressed() {
		
		wasCancelled = false;
		
		super.okPressed();
	}
	
	public boolean wasCancelled() {
		
		return wasCancelled;
	}
}

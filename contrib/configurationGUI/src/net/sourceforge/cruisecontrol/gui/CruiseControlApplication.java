/*
 * Created on Jul 2, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sourceforge.cruisecontrol.gui;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

import org.arch4j.ui.AbstractSplashScreen;
import org.arch4j.ui.JApplication;
import org.arch4j.ui.components.DialogManager;

/**
 * @author bonarheim
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class CruiseControlApplication extends JApplication {
	
	protected JFrame frame;
	protected boolean inLongOperation = false;
	
	public CruiseControlApplication() {
		super();
	}
	
	public CruiseControlApplication(AbstractSplashScreen aSplashScreen) {
		super(aSplashScreen);
	}

	/*
	 * If the screen resolution is 1024x768 make the window size
	 * the screen size minus the size of the windows toolbar. If
	 * the screen resolution is greater than 1024x768 just make 
	 * it 1024x768.
	 */
	protected static void setWindowSize(JFrame theFrame) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		
		if (screenSize.height > 768) {
			theFrame.setBounds(0, 0, 1024, 768);
			centerWindow( theFrame );
		} else {
			//	subtract the height of the task bar from the height of the screen
			theFrame.setBounds(0, 0, screenSize.width, screenSize.height - 34);
		}
	}

	public void setWaitCursor() {
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
	}

	public void setDefaultCursor() {
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	}

	/**
	* Returns the frame.
	* @return JFrame
	*/
	public JFrame getFrame() {
		return frame;
	}

	/**
	* Sets the frame.
	* @param frame The frame to set
	*/
	public void setFrame(JFrame frame) {
		this.frame = frame;
	}

	protected void displayError(String anError, String aTitle) {
		DialogManager.getManager().displayError(this, anError, aTitle);
	}

	protected void displayInformation(String aMessage, String aTitle) {
		
		DialogManager.getManager().displayInformation( this, aMessage, aTitle );
	}
	
	/**
	 * Create a new instance of <code>WaitDialog</code>, set it's explanation text to the given String,
	 * and return it.  The dialog is <b>not</b> shown.
	 *
	 * @param explainText The explanatory text.
	 * @return The new WaitDialog.
	*/
//	protected WaitDialog createWaitDialog(String explainText) {
//		WaitDialog waitDialog = new WaitDialog( this );
//		waitDialog.setExplainText( explainText );
//		return waitDialog;
//	}

	/**
	     * Gets the receiver's frame Title.
	     * @return {String}
	     */
	public String getFrameTitle() {
	    return getFrame().getTitle();
	}

	/**
	 * Check to see if we are in the middle of a long operation.
	 * Only one can occur at a time.
	 *
	 * @return <code>true</code>, if we got the lock and can continue the long operation;
	 * <code>false</code> if we could not get the lock.
	*/
	protected synchronized boolean checkForAndStartLongOperation() {
		
		if (inLongOperation) {
			return false;
		}
		else {
			inLongOperation = true;
			return true;
		}
	}

	/**
	 * Unlock the long operation flag.
	*/
	protected synchronized void endLongOperation() {
		inLongOperation = false;
	}
}

package net.sourceforge.cruisecontrol.distributed;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.rmi.RemoteException;
import java.util.prefs.Preferences;

import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.core.CCDistVersion;
import net.sourceforge.cruisecontrol.distributed.core.PreferencesHelper;

/**
 * @author Dan Rollo
 * Date: Aug 25, 2005
 * Time: 3:38:53 PM
 */
// @todo Use JDesktop stuff for tray icon??
final class BuildAgentUI extends JFrame implements BuildAgent.AgentStatusListener, BuildAgent.LUSCountListener,
        PreferencesHelper.UIPreferences {

    private static final Logger LOG = Logger.getLogger(BuildAgentUI.class);

    private static final int CONSOLE_LINE_BUFFER_SIZE = 1000;

    private final BuildAgent buildAgent;
    private final JTextArea txaAgentInfo;
    private final Action atnStop;
    private final Action atnEditEntries;
    private final String origTitle;

    BuildAgentUI(final BuildAgent parentbuildAgent) {
        super("CruiseControl - Build Agent " + CCDistVersion.getVersion());

        origTitle = getTitle();

        buildAgent = parentbuildAgent;

        buildAgent.addLUSCountListener(this);
        buildAgent.addAgentStatusListener(this);

        atnStop = new AbstractAction("Stop") {
            public void actionPerformed(final ActionEvent e) {
                doExit();
            }
        };
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent evt) {
                doExit();
            }
        });

        final JTextArea txaConsole = new JTextArea();
        txaConsole.setFont(new java.awt.Font("Courier New", 0, 12));

        final JScrollPane scrConsole = new JScrollPane();
        scrConsole.setViewportView(txaConsole);
        scrConsole.setPreferredSize(new Dimension(550, 300));

        txaConsole.setEditable(false);
        // need to register with BuildAgent Logger (not UI Logger).
        BuildAgent.LOG.addAppender(new Log4JJTextAreaAppender(txaConsole));

        final JPanel pnlN = new JPanel(new BorderLayout());
        txaAgentInfo = new JTextArea("Registering with Lookup Services...");
        txaAgentInfo.setEditable(false);
        pnlN.add(txaAgentInfo, BorderLayout.CENTER);

        final JButton btnStop = new JButton(atnStop);
        btnStop.setToolTipText("Terminates this Build Agent. (If agent is busy, the build may not stop.)");
        btnStop.setPreferredSize(new Dimension(62, -1));

        final JPanel pnlButtons = new JPanel(new GridLayout(0, 1));
        pnlButtons.add(btnStop);

        atnEditEntries = new AbstractAction("Entries") {
            public void actionPerformed(ActionEvent e) {
                doEditEntries();
            }
        };
        final JButton btnEntries = new JButton(atnEditEntries);
        btnEntries.setToolTipText("Edit Entry Overrides. System provided entries are not editable.");
        pnlButtons.add(btnEntries);
        
        pnlN.add(pnlButtons, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(pnlN, BorderLayout.NORTH);
        getContentPane().add(scrConsole, BorderLayout.CENTER);

        final Image imgIcon = PreferencesHelper.getCCImageIcon();
        if (imgIcon != null) {
            setIconImage(imgIcon);
        }

        pack();

        // apply screen info from last run
        PreferencesHelper.applyWindowInfo(this);

        setVisible(true);
    }

    private void doEditEntries() {
        try {
            final BuildAgentService agentService = buildAgent.getService();
            final BuildAgentEntryOverrideUI ui
                    = new BuildAgentEntryOverrideUI(this, agentService,
                            agentService.getMachineName() + ": " + buildAgent.getServiceID());

            ui.addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent evt) {
                    atnEditEntries.setEnabled(true);
                }
            });

            atnEditEntries.setEnabled(false);
        } catch (RemoteException e1) {
            final String msg = "An error occurred while editing entry overrides: ";
            LOG.error(msg, e1);
            JOptionPane.showMessageDialog(BuildAgentUI.this, msg + e1.getMessage(),
                    "Error Editing Entry Overrides", JOptionPane.ERROR_MESSAGE);
        }
    }


    public void dispose() {
        // ensure we do cleanup even when closing due to webstart resart

        // save screen info
        PreferencesHelper.saveWindowInfo(this);

        super.dispose();
    }

    private void doExit() {
        atnStop.setEnabled(false);

        final BuildAgentUI theThis = this;
        new Thread("Build Agent doExit Thread") {
            public void run() {
                BuildAgent.kill();
                // on some JVM's the kill call above doesn't return, so sys exit is done by main()
                LOG.info("BuildAgent.kill() completed");
                buildAgent.removeAgentStatusListener(theThis);
                LOG.info("AgentStatusListener removed");
                buildAgent.removeLUSCountListener(theThis);
                LOG.info("LUSCountListener removed");
                System.exit(0);
            }
        } .start();
    }

    public void statusChanged(BuildAgentService buildAgentService) {
        updateAgentInfoUI(buildAgentService);
    }

    void updateAgentInfoUI(final BuildAgentService buildAgentService) {
        // collect data to update on separate thread
        // it's also good to wait a bit for the agent info to be updated...
        new Thread("AgentUI updateAgentInfoUI-data") {
            public void run() {
                doUpdateAgentInfoUI(buildAgentService);
            }
        } .start();
    }

    private void doUpdateAgentInfoUI(final BuildAgentService buildAgentService) {
        String agentInfo = "";
        if (buildAgentService != null) {
            try {
                agentInfo = buildAgentService.asString();
            } catch (RemoteException e) {
                agentInfo = e.getMessage();
            }
        }
        final String agentText = "Build Agent: " + buildAgent.getServiceID() + "\n"
                + agentInfo
                + MulticastDiscovery.toStringEntries(buildAgent.getEntries());

        // only update UI on event dispatch thread,
        //"AgentUI updateAgentInfoUI Thread"
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    txaAgentInfo.setText(agentText);
                }
        });
    }


    public void lusCountChanged(final int newLUSCount) {
        //"Agent lusCountChanged Thread"
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setTitle(origTitle + ", LUS's: " + newLUSCount);                                
            }
        });
    }

    public Preferences getPrefsBase() {
        return buildAgent.getPrefsRoot();
    }

    public Window getWindow() {
        return this;
    }

    /**
     * Log4J appender to duplicate log output to a JTextArea
     */
    public class Log4JJTextAreaAppender extends AppenderSkeleton {

        private final JTextArea txaL4JConsole;

        public Log4JJTextAreaAppender(final JTextArea txaConsole) {
            this.txaL4JConsole = txaConsole;
        }

        protected void append(final LoggingEvent event) {
            final String msg = event.getRenderedMessage();
            //"Agent Log4JJTextAreaAppender Thread"
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    txaL4JConsole.append(msg + "\n");
                    if (txaL4JConsole.getLineCount() > CONSOLE_LINE_BUFFER_SIZE) {
                        // remove old lines
                        try {
                            txaL4JConsole.replaceRange("", 0,
                                    txaL4JConsole.getLineEndOffset(
                                            txaL4JConsole.getLineCount() - CONSOLE_LINE_BUFFER_SIZE
                                    ));
                        } catch (BadLocationException e) {
                            //ignore
                        }
                    }
                    // Make sure the last line is always visible
                    txaL4JConsole.setCaretPosition(txaL4JConsole.getDocument().getLength());
                }
            });

        }

        public boolean requiresLayout() {
            return false;
        }

        public void close() {
        }
    }
}

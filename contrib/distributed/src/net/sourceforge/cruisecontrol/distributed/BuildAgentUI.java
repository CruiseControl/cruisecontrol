package net.sourceforge.cruisecontrol.distributed;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;
import java.rmi.RemoteException;

import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.core.CCDistVersion;

/**
 * @author: Dan Rollo
 * Date: Aug 25, 2005
 * Time: 3:38:53 PM
 */
// @todo Use JDesktop stuff for tray icon??
final class BuildAgentUI extends JFrame implements BuildAgent.AgentStatusListener, BuildAgent.LUSCountListener {

    private static final Logger LOG = Logger.getLogger(BuildAgentUI.class);

    private static final int CONSOLE_LINE_BUFFER_SIZE = 1000;

    private final BuildAgent buildAgent;
    private final JTextArea txaAgentInfo;
    private final JButton btnStop = new JButton("Stop");
    private final JTextArea txaConsole = new JTextArea();
    private final JScrollPane scrConsole = new JScrollPane();
    private final String origTitle;

    BuildAgentUI(final BuildAgent parentbuildAgent) {
        super("CruiseControl - Build Agent " + CCDistVersion.getVersion());

        origTitle = getTitle();

        buildAgent = parentbuildAgent;

        buildAgent.addLUSCountListener(this);
        buildAgent.addAgentStatusListener(this);

        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doExit();
            }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent evt) {
                doExit();
            }
        });

        txaConsole.setFont(new java.awt.Font("Courier New", 0, 12));

        scrConsole.setViewportView(txaConsole);
        scrConsole.setPreferredSize(new Dimension(500, 300));

        txaConsole.setEditable(false);
        // need to register with BuildAgent Logger (not UI Logger).
        BuildAgent.LOG.addAppender(new Log4JJTextAreaAppender(txaConsole));

        final JPanel pnlN = new JPanel(new BorderLayout());
        txaAgentInfo = new JTextArea("Registering with Lookup Services...");
        txaAgentInfo.setEditable(false);
        pnlN.add(txaAgentInfo, BorderLayout.CENTER);
        pnlN.add(btnStop, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(pnlN, BorderLayout.NORTH);
        getContentPane().add(scrConsole, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

    private void doExit() {
        btnStop.setEnabled(false);
        final BuildAgentUI theThis = this;
        new Thread("Build Agent doExit Thread") {
            public void run() {
                BuildAgent.kill();
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
        // only update UI on event dispatch thread,
        // it's also good to wait a bit for the agent info to be updated...
        SwingUtilities.invokeLater(new Thread("AgentUI updateAgentInfoUI Thread") {
                public void run() {
                    String agentInfo = "";
                    if (buildAgentService != null) {
                        try {
                            agentInfo = buildAgentService.asString();
                        } catch (RemoteException e) {
                            agentInfo = e.getMessage();
                        }
                    }

                    txaAgentInfo.setText("Build Agent: " + buildAgent.getServiceID() + "\n"
                            + agentInfo
                            + MulticastDiscovery.toStringEntries(buildAgent.getEntries())
                    );
                }
        });
    }


    public void lusCountChanged(final int newLUSCount) {
        SwingUtilities.invokeLater(new Thread("Agent lusCountChanged Thread") {
            public void run() {
                setTitle(origTitle + ", LUS's: " + newLUSCount);                                
            }
        });
    }

    /**
     * Log4J appender to duplicate log output to a JTextArea
     */
    public class Log4JJTextAreaAppender extends AppenderSkeleton {

        private final JTextArea txaConsole;

        public Log4JJTextAreaAppender(final JTextArea txaConsole) {
            this.txaConsole = txaConsole;
        }

        protected void append(final LoggingEvent event) {
            final String msg = event.getRenderedMessage();
            SwingUtilities.invokeLater(new Thread("Agent Log4JJTextAreaAppender Thread") {
                public void run() {
                    txaConsole.append(msg + "\n");
                    if (txaConsole.getLineCount() > CONSOLE_LINE_BUFFER_SIZE) {
                        // remove old lines
                        try {
                            txaConsole.replaceRange("", 0,
                                    txaConsole.getLineEndOffset(
                                            txaConsole.getLineCount() - CONSOLE_LINE_BUFFER_SIZE
                                    ));
                        } catch (BadLocationException e) {
                            //ignore
                        }
                    }
                    // Make sure the last line is always visible
                    txaConsole.setCaretPosition(txaConsole.getDocument().getLength());
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

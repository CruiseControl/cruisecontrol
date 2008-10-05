package net.sourceforge.cruisecontrol.distributed.util;

import org.apache.log4j.Logger;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceID;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.BuildAgentEntryOverrideUI;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.core.CCDistVersion;
import net.sourceforge.cruisecontrol.distributed.core.PreferencesHelper;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import java.rmi.RemoteException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.Preferences;

/**
 * @author Dan Rollo
 * Date: Aug 1, 2005
 * Time: 4:00:38 PM
 */
public final class BuildAgentUtility {
    private static final Logger LOG = Logger.getLogger(BuildAgentUtility.class);

    // helps make UI class testable in a headless environment
    static interface UISetInfo {
        void setInfo(final String infoText);
    }

    // @todo make BuidAgentService implement/extend jini ServiceUI?
    static class UI extends JFrame implements PreferencesHelper.UIPreferences, UISetInfo {

        private static final int CONSOLE_LINE_BUFFER_SIZE = 1000;

        private final String origTitle;
        private final BuildAgentUtility buildAgentUtility;
        private final JButton btnRefresh = new JButton("Refresh");
        private final JComboBox cmbAgents = new JComboBox();
        private final Action atnInvoke;
        private final Action atnEditEntries;
        private static final String METH_RESTART = "restart";
        private static final String METH_KILL = "kill";
        private final JComboBox cmbRestartOrKill = new JComboBox(new String[] {METH_RESTART, METH_KILL});
        private final JCheckBox chkAfterBuildFinished = new JCheckBox("Wait for build to finish.", true);
        private final JButton btnInvokeOnAll = new JButton("Invoke on All");
        private final JPanel pnlEdit = new JPanel(new BorderLayout());
        private final JButton btnClose = new JButton("Close");
        private final JTextArea txaConsole = new JTextArea();
        private final JScrollPane scrConsole = new JScrollPane();

        private static final Preferences PREFS_BASE = Preferences.userNodeForPackage(UI.class);
        static Preferences getPrefsRoot() { return PREFS_BASE; }

        /**
         * No-arg constructor for use in unit tests, to be overridden by MockUI.
         */
        UI() {
            origTitle = null;
            buildAgentUtility = null;
            atnInvoke = null;
            atnEditEntries = null;
        }

        private UI(final BuildAgentUtility buildAgentUtil) {
            super("CruiseControl - Agent Utility " + CCDistVersion.getVersion());

            origTitle = getTitle();

            buildAgentUtility = buildAgentUtil;

            btnClose.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    exitForm();
                }
            });
            addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent evt) {
                    exitForm();
                }
            });

            btnRefresh.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    refreshAgentList();
                }
            });

            cmbAgents.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    atnInvoke.setEnabled(true);
                    atnEditEntries.setEnabled(true);
                }
            });

            atnInvoke = new AbstractAction("Invoke") {
                public void actionPerformed(final ActionEvent e) {
                    try {
                        invokeOnAgent(
                                ((ComboItemWrapper) cmbAgents.getSelectedItem()).getAgent()
                        );
                    } catch (RemoteException e1) {
                        checkAndShowRestartRequiresWebStart(e1);
                        LOG.info(e1.getMessage());
                        appendInfo(e1.getMessage());
                        throw new RuntimeException(e1);
                    }
                }
            };
            atnInvoke.setEnabled(false);

            atnEditEntries = new AbstractAction("Entries") {
                public void actionPerformed(final ActionEvent e) {
                    try {
                        final ComboItemWrapper agentWrapper = ((ComboItemWrapper) cmbAgents.getSelectedItem());
                        final BuildAgentService agentService = agentWrapper.getAgent();

                        new BuildAgentEntryOverrideUI(BuildAgentUtility.UI.this, agentService,
                                agentService.getMachineName() + ": " + agentWrapper.getServiceID());

                    } catch (RemoteException e1) {
                        final String msg = "An error occurred while editing entry overrides: ";
                        LOG.error(msg, e1);
                        appendInfo(msg + e1.getMessage());
                        JOptionPane.showMessageDialog(BuildAgentUtility.UI.this, msg + "\n\n" + e1.getMessage(),
                                "Error Editing Entry Overrides", JOptionPane.ERROR_MESSAGE);
                        throw new RuntimeException(e1);
                    }
                }
            };
            atnEditEntries.setEnabled(false);

            btnInvokeOnAll.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    for (int i = 0; i < cmbAgents.getItemCount(); i++) {
                        try {
                            invokeOnAgent(((ComboItemWrapper) cmbAgents.getItemAt(i)).getAgent());
                        } catch (RemoteException e1) {
                            checkAndShowRestartRequiresWebStart(e1);
                            LOG.info(e1.getMessage());
                            appendInfo(e1.getMessage());
                            //throw new RuntimeException(e1); // allow remaining items to be invoked
                        }
                    }
                }
            });

            txaConsole.setFont(new Font("Courier New", 0, 12));

            scrConsole.setViewportView(txaConsole);
            scrConsole.setPreferredSize(new Dimension(626, 300));


            getContentPane().setLayout(new BorderLayout());
            final JPanel pnlNN = new JPanel(new BorderLayout());
            pnlNN.add(btnRefresh, BorderLayout.WEST);

            pnlNN.add(cmbAgents, BorderLayout.CENTER);

            final JPanel pnlButtonsTop = new JPanel(new GridLayout(1, 0));
            final JButton btnInvoke = new JButton(atnInvoke);
            btnInvoke.setPreferredSize(new Dimension(76, -1));
            pnlButtonsTop.add(btnInvoke);
            pnlButtonsTop.add(new JButton(atnEditEntries));
            pnlNN.add(pnlButtonsTop, BorderLayout.EAST);

            final JPanel pnlNS = new JPanel(new BorderLayout());
            pnlNS.add(btnClose, BorderLayout.EAST);

            pnlEdit.add(cmbRestartOrKill, BorderLayout.WEST);
            pnlEdit.add(chkAfterBuildFinished, BorderLayout.CENTER);
            pnlEdit.add(btnInvokeOnAll, BorderLayout.EAST);

            final JPanel northPanel = new JPanel(new BorderLayout());
            northPanel.add(pnlNN, BorderLayout.NORTH);
            northPanel.add(pnlEdit, BorderLayout.CENTER);
            northPanel.add(pnlNS, BorderLayout.SOUTH);
            getContentPane().add(northPanel, BorderLayout.NORTH);
            getContentPane().add(scrConsole, BorderLayout.CENTER);
            pack();

            // set screen info from last run
            PreferencesHelper.applyWindowInfo(this);

            setVisible(true);
        }

        private void checkAndShowRestartRequiresWebStart(RemoteException e1) {
            final String msg = checkRestartRequiresWebStart(e1);
            if (msg != null) {
                LOG.info(msg);
                appendInfo(msg);
            }
        }

        private void invokeOnAgent(final BuildAgentService agent) throws RemoteException {
            if (METH_RESTART.equals(cmbRestartOrKill.getSelectedItem())) {
                agent.restart(chkAfterBuildFinished.isSelected());
            } else {
                agent.kill(chkAfterBuildFinished.isSelected());
            }
        }

        // begin implementation of UIPreferences
        public Preferences getPrefsBase() {
            return UI.getPrefsRoot();
        }

        public Window getWindow() {
            return this;
        }
        // end implementation of UIPreferences

        private static final class ComboItemWrapper {
            private static ComboItemWrapper[] wrapArray(final ServiceItem[] serviceItems) {
                final ComboItemWrapper[] result = new ComboItemWrapper[serviceItems.length];
                for (int i = 0; i < serviceItems.length; i++) {
                    result[i] = new ComboItemWrapper(serviceItems[i]);
                }
                return result;
            }

            private final ServiceItem serviceItem;
            private ComboItemWrapper(final ServiceItem serviceItemToWrap) {
                this.serviceItem = serviceItemToWrap;
            }

            public BuildAgentService getAgent() {
                return (BuildAgentService) serviceItem.service;
            }

            public ServiceID getServiceID() {
                return serviceItem.serviceID;
            }


            private String errToString;

            public String toString() {
                // don't make remote calls if agent has errored out already, otherwise may appear to hang ui
                if (errToString != null) {
                    return errToString;
                }

                try {
                    return getAgent().getMachineName() + ": " + serviceItem.serviceID;
                } catch (RemoteException e) {
                    errToString = "Remote Error: " + e.getMessage();
                } catch (Exception e) {
                    errToString = "Error: " + e.getMessage();
                }
                return errToString;
            }
        }

        private void refreshAgentList() {
            SwingUtilities.invokeLater(new Thread("BuildAgentUtility btn.disable Thread") {
                public void run() {
                    btnRefresh.setEnabled(false);
                    atnInvoke.setEnabled(false);
                    atnEditEntries.setEnabled(false);
                    btnInvokeOnAll.setEnabled(false);
                    cmbAgents.setEnabled(false);
                }
            });
            new Thread("BuildAgentUtility refreshAgentList Thread") {
                public void run() {
                    doRefreshAgentList();
                }
            } .start();
        }

        private void doRefreshAgentList() {
            try {
                final List<ServiceItem> tmpList = new ArrayList<ServiceItem>();
                final String agentInfoAll = buildAgentUtility.getAgentInfoAll(tmpList);
                final ServiceItem[] serviceItems = tmpList.toArray(new ServiceItem[tmpList.size()]);
                final ComboBoxModel comboBoxModel = new DefaultComboBoxModel(
                        ComboItemWrapper.wrapArray(serviceItems));
                SwingUtilities.invokeLater(new Thread("BuildAgentUtility setcomboBoxModel Thread") {
                    public void run() {
                        UI.this.setTitle(origTitle + ", LUS's: " + buildAgentUtility.lastLUSCount);
                        cmbAgents.setModel(comboBoxModel);
                    }
                });
                setInfo(agentInfoAll);
            } finally {
                SwingUtilities.invokeLater(new Thread("BuildAgentUtility btn.enable Thread") {
                    public void run() {
                        btnRefresh.setEnabled(true);
                        btnInvokeOnAll.setEnabled(true);
                        cmbAgents.setEnabled(true);
                    }
                });
            }
        }

        private void exitForm() {
            // save screen info
            PreferencesHelper.saveWindowInfo(this);

            System.exit(0);
        }

        // Only public to allow testing of UI in headless environs
        public void setInfo(final String infoText) {
            LOG.debug(infoText);
            SwingUtilities.invokeLater(new Thread("BuildAgentUtility txaConsole.setInfo Thread") {
                public void run() {
                    txaConsole.setText(infoText);
                }
            });
        }

        private void appendInfo(final String infoText) {
            SwingUtilities.invokeLater(new Thread("BuildAgentUtility txaConsole.appendInfo Thread") {
                public void run() {
                    txaConsole.append(infoText + "\n");
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
    }

    private final UISetInfo ui;

    private int lastLUSCount;

    private boolean isFailFast;
    private boolean isInited;

    public static BuildAgentUtility createForJMX() {
        return new BuildAgentUtility(true);
    }

    static final String SYS_PROP_IS_FAIL_FAST = "agentUtilFailFast";

    private BuildAgentUtility(final boolean isHeadlessUtil) {
        if (!isHeadlessUtil) {
            throw new IllegalStateException("this contructor must be passed a param value of true");
        }
        ui = new UISetInfo() {
            public void setInfo(String infoText) {
                LOG.info(infoText);
            }
        };

        isFailFast = Boolean.getBoolean(SYS_PROP_IS_FAIL_FAST);
    }
    private BuildAgentUtility() {
        this(null);
    }
    BuildAgentUtility(final UISetInfo mockUI) {

        CCDistVersion.printCCDistVersion();

        if (mockUI == null) {
            ui = new UI(this);
            ((UI) ui).btnRefresh.doClick();
        } else {
            ui = mockUI;
            isFailFast = true;
        }
    }


    public int getLastLUSCount() { return lastLUSCount; }

    /** Number of seconds to wait for Lookup Services to report in. */
    public static final int LUS_WAIT_SECONDS = 5;

    public String getAgentInfoAll(final List<ServiceItem> lstServiceItems) {

        final StringBuffer result = new StringBuffer();
        try {

            if (!isInited && !isFailFast) {
                try {
                    MulticastDiscovery.begin();
                    final String msgWaitLUS = "Waiting " + LUS_WAIT_SECONDS + " seconds for registrars to report in...";
                    ui.setInfo(msgWaitLUS);
                    LOG.info(msgWaitLUS);

                    Thread.sleep(LUS_WAIT_SECONDS * 1000);
                    isInited = true;
                } catch (InterruptedException e1) {
                    LOG.warn("Sleep interrupted", e1);
                }
            }

            final String waitMessage = "Waiting for Build Agents to report in...";
            ui.setInfo(waitMessage);
            LOG.info(waitMessage);
            final ServiceItem[] serviceItems = MulticastDiscovery.findBuildAgentServices(null,
                    (isFailFast ? 0 : MulticastDiscovery.DEFAULT_FIND_WAIT_DUR_MILLIS));

            // update LUS count
            lastLUSCount = MulticastDiscovery.getLUSCount();

            // clear and rebuild list
            lstServiceItems.clear();
            lstServiceItems.addAll(Arrays.asList(serviceItems));

            LOG.info("Agents found: " + serviceItems.length);
            result.append("Found: ").append(serviceItems.length).append(" agent")
                    .append(serviceItems.length != 1 ? "s" : "")
                    .append(".\n");

            BuildAgentService agent;
            String agentInfo;
            for (ServiceItem serviceItem : serviceItems) {
                agent = (BuildAgentService) serviceItem.service;
                agentInfo = "Build Agent: " + serviceItem.serviceID + "\n"
                        + agent.asString()
                        + MulticastDiscovery.toStringEntries(serviceItem.attributeSets)
                        + "\n";
                LOG.debug(agentInfo);
                result.append(agentInfo);
            }
        } catch (RemoteException e) {
            final String message = "Search failed due to an unexpected error";
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        }

        return result.toString();
    }


    public static String checkRestartRequiresWebStart(final RemoteException e) {
        if (e.getCause() != null && e.getCause() instanceof ClassNotFoundException
                && "javax.jnlp.UnavailableServiceException".equals(e.getCause().getMessage())) {

            return "\nNOTE: Restart feature is only available on Agents launched via WebStart.\n"
                    + e.getCause().getMessage();
        }
        return null;
    }

    public static void main(final String[] args) {
        new BuildAgentUtility();
    }
}

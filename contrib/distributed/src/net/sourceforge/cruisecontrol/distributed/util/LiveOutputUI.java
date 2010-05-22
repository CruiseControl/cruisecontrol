package net.sourceforge.cruisecontrol.distributed.util;

import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import org.apache.log4j.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

/**
 * Shows Live Output from the selected agent.
 *
 * @author Dan Rollo
 *         Date: May 21, 2010
 *         Time: 1:11:16 PM
 */
class LiveOutputUI extends JDialog {

    private static final Logger LOG = Logger.getLogger(LiveOutputUI.class);

    private static final class LiveOutputInfo {
        private final String id;
        private final int start;
        private final String[] lines;

        private LiveOutputInfo(final String outputID, final int startLine, final String[] outputLines) {
            id = outputID;
            start = startLine;
            lines = outputLines;
        }

        public String toString() {
            return "LiveOutput: outputID: " + id + ", start: " + start
                    + ", lines - count: " + (lines == null ? 0 : lines.length);
        }
    }

    private static final class LUSInfoUI extends JPanel {
        private static final Font DISPLAY_FONT = new Font("Courier New", 0, 12);

        private LUSInfoUI(final LiveOutputUI parent, final LiveOutputInfo liveOutputInfo) {
            super();
            setLayout(new BorderLayout());

            final JLabel lblInfo = new JLabel("Start: " + liveOutputInfo.start
                    + "; Output ID: " + liveOutputInfo.id
                    + "; Line Count: " + (liveOutputInfo.lines == null ? 0 : liveOutputInfo.lines.length)
            );
            lblInfo.setFont(DISPLAY_FONT);

            if (liveOutputInfo.lines != null) {
                for (final String line : liveOutputInfo.lines) {
                    parent.txaLines.append(line + "\n");
                }
            }

            final JPanel pnlNorth = new JPanel(new BorderLayout(20, 2));
            pnlNorth.add(lblInfo, BorderLayout.NORTH);

            add(pnlNorth, BorderLayout.NORTH);

            add(parent.txaLines, BorderLayout.CENTER);
        }
    }

    private void rebuild() {

        if (pnlAllLUS != null) {
            pnlMain.remove(pnlAllLUS);
        }

        final JPanel pnlView = new JPanel(new GridLayout(0, 1));

        final String outputID;
        try {
            outputID = agent.getIDRemote();
        } catch (RemoteException e) {
            final String msg = "An error occurred while reading remoteOutputID: ";
            LOG.error(msg, e);
            throw new RuntimeException(e);
        }

        final int start = Integer.parseInt(txtStart.getText());

        final String[] outputLines;
        try {
            outputLines = agent.retrieveLinesRemote(start);
        } catch (RemoteException e) {
            final String msg = "An error occurred while reading retrieveLinesRemote: ";
            LOG.error(msg, e);
            throw new RuntimeException(e);
        }

        if (chkUpdateStart.isSelected()) {
            if (!outputID.equals(priorOutputID)) {
                txtStart.setText("0");
            } else {
                txtStart.setText(start + outputLines.length + "");
            }
        }

        if (chkClearPriorText.isSelected()) {
            txaLines.setText("");
        }
        priorOutputID = outputID;

        final LiveOutputInfo liveOutputInfo = new LiveOutputInfo(outputID, start, outputLines);
        pnlView.add(new LUSInfoUI(this, liveOutputInfo));

        final JScrollPane scrPane = new JScrollPane(pnlView);
        scrPane.setPreferredSize(new Dimension(525, 54));

        pnlAllLUS = new JPanel(new BorderLayout());
        pnlAllLUS.add(scrPane, BorderLayout.CENTER);

        pnlMain.add(pnlAllLUS);
        pnlMain.validate();
    }


    private final BuildAgentService agent;
    private final Action atnLiveOutput;
    private final JPanel pnlMain;
    private JPanel pnlAllLUS;

    private final JTextField txtStart;
    private final JCheckBox chkUpdateStart;
    private final JCheckBox chkClearPriorText;

    private final JTextArea txaLines;

    private String priorOutputID;
    
    LiveOutputUI(final BuildAgentUtility.UI owner,
                    final BuildAgentService agentService, final String agentInfo,
                    final Action atnLiveOutput) {

        super(owner, "Live Output - " + agentInfo);
        setLocationRelativeTo(owner);

        this.agent = agentService;
        this.atnLiveOutput = atnLiveOutput;

        pnlMain = new JPanel(new BorderLayout(5, 5));
        getContentPane().add(pnlMain);

        final JLabel lblStart = new JLabel("Start: ");
        txtStart = new JTextField(0 + "", 5);
        lblStart.setLabelFor(txtStart);
        final JPanel pnlTop = new JPanel(new FlowLayout());
        pnlTop.add(lblStart);
        pnlTop.add(txtStart);

        chkUpdateStart = new JCheckBox("Update Start to Output Line Count + 1, or 0 if Output ID changed.", true);
        pnlTop.add(chkUpdateStart);

        chkClearPriorText = new JCheckBox("Clear Prior Output.", false);
        pnlTop.add(chkClearPriorText);

        pnlMain.add(pnlTop, BorderLayout.NORTH);

        txaLines = new JTextArea();
        txaLines.setEditable(false);

        final Action atnRefresh = new AbstractAction("Refresh") {
            public void actionPerformed(final ActionEvent e) {
                LOG.debug("Rebuilding Live Output...");
                rebuild();
            }
        };
        final JPanel pnlSouth = new JPanel(new GridLayout());
        pnlSouth.add(new JButton(atnRefresh));

        final Action atnClose = new AbstractAction("Close") {
            public void actionPerformed(final ActionEvent e) {
                doExit();
            }
        };
        pnlSouth.add(new JButton(atnClose));

        pnlMain.add(pnlSouth, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent evt) {
                doExit();
            }
        });

        rebuild();

        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private void doExit() {
        LiveOutputUI.this.dispose();
        atnLiveOutput.setEnabled(true);
    }
}


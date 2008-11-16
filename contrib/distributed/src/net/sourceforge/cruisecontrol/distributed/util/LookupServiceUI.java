package net.sourceforge.cruisecontrol.distributed.util;

import net.jini.core.lookup.ServiceRegistrar;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

/**
 * Shows discovered Lookup Services and allows a way to destory them.
 *
 * @author Dan Rollo
 *         Date: Nov 15, 2008
 *         Time: 6:50:03 PM
 */
class LookupServiceUI extends JDialog {

    private static final Logger LOG = Logger.getLogger(LookupServiceUI.class);

    private static final class LUSInfo {
        final ServiceRegistrar lus;
        final String host;
        final String id;

        private LUSInfo(final ServiceRegistrar lus) {
            this.lus = lus;

            String tmpHost;
            try {
                tmpHost = lus.getLocator().getHost();
            } catch (RemoteException e) {
                tmpHost = e.getMessage();
            }
            host = tmpHost;

            id = lus.getServiceID().toString();
        }

        public String toString() {
            return "LookupService: Host: " + host + ", ID: " + id;
        }
    }

    private static final class LUSInfoUI extends JPanel {
        private static final Font DISPLAY_FONT = new Font("Courier New", 0, 12);

        private LUSInfoUI(final LookupServiceUI parent, final LUSInfo lusInfo) {
            super();
            setLayout(new BorderLayout());

            final JLabel lblID = new JLabel("ID: " + lusInfo.id);
            lblID.setFont(DISPLAY_FONT);

            final JLabel lblHost = new JLabel("Host: " + lusInfo.host);
            lblHost.setFont(DISPLAY_FONT);
            
            final Action atnDestroy = new AbstractAction("Destory") {
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug("Destroying LUS: " + lusInfo);
                    try {
                        MulticastDiscovery.destroyLookupService(lusInfo.lus, 5000);
                    } catch (RemoteException e1) {
                        JOptionPane.showMessageDialog(parent, e1.getMessage(),
                                "Error Destorying LUS", JOptionPane.ERROR_MESSAGE);
                    }
                    parent.rebuild();
                }
            };

            final JPanel pnlNorth = new JPanel(new BorderLayout(20, 2));
            pnlNorth.add(lblID, BorderLayout.WEST);
            pnlNorth.add(lblHost, BorderLayout.CENTER);
            pnlNorth.add(new JButton(atnDestroy), BorderLayout.EAST);

            add(pnlNorth, BorderLayout.NORTH);
        }
    }

    private void rebuild() {

        if (pnlAllLUS != null) {
            pnlMain.remove(pnlAllLUS);
        }

        final JPanel pnlView = new JPanel(new GridLayout(0, 1));

        final ServiceRegistrar[] validRegistrars = buildAgentUtil.getValidRegistrars();
        for (final ServiceRegistrar lus : validRegistrars) {
            final LUSInfo lusInfo = new LUSInfo(lus);
            pnlView.add(new LUSInfoUI(this, lusInfo));
        }

        final JScrollPane scrPane = new JScrollPane(pnlView);
        scrPane.setPreferredSize(new Dimension(525, 54));

        pnlAllLUS = new JPanel(new BorderLayout());
        pnlAllLUS.add(scrPane, BorderLayout.CENTER);
        
        pnlMain.add(pnlAllLUS);
        pnlMain.validate();

        // update parent UI to show correct LUS counts
        owner.updateLUSCountUI(validRegistrars.length);
    }


    private final BuildAgentUtility.UI owner;
    private final BuildAgentUtility buildAgentUtil;
    private final Action atnListLookupServices;
    private final JPanel pnlMain;
    private JPanel pnlAllLUS;

    LookupServiceUI(final BuildAgentUtility.UI owner,
                    final BuildAgentUtility buildAgentUtil,
                    final Action atnListLookupServices) {

        super(owner, "Lookup Services");
        setLocationRelativeTo(owner);

        this.owner = owner;
        this.buildAgentUtil = buildAgentUtil;
        this.atnListLookupServices = atnListLookupServices;

        pnlMain = new JPanel(new BorderLayout(5, 5));
        getContentPane().add(pnlMain);

        final Action atnRefresh = new AbstractAction("Refresh") {
            public void actionPerformed(final ActionEvent e) {
                LOG.debug("Rebuilding LUS list...");
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
        LookupServiceUI.this.dispose();
        atnListLookupServices.setEnabled(true);
    }
}

package net.sourceforge.cruisecontrol.distributed;

import org.apache.log4j.Logger;

import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Allow editing of entry overrides for a Build Agent
 * @author Dan Rollo
 * Date: Apr 3, 2007
 * Time: 1:48:11 PM
 */
// @todo Change this class into a Jini ServiceUI component
public class BuildAgentEntryOverrideUI extends JDialog {

    private static final Logger LOG = Logger.getLogger(BuildAgentEntryOverrideUI.class);

    private static final class PropRow {
        private String name;
        private String value;
        private PropRow(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        public String toString() { return "Name: " + name + "; Value: " + value + ";"; }
    }

    private static final class PropertyTableModel extends AbstractTableModel {

        private final ArrayList rows = new ArrayList();
        private final int colCount = 2;

        private void addRow(final PropRow propRow) {
            rows.add(propRow);
            fireTableDataChanged();
        }

        private void clearAll() {
            rows.clear();
            fireTableDataChanged();
        }

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return colCount;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final PropRow row = getPropRow(rowIndex);
            if (columnIndex == COL_NAME) {
                row.name = (String) aValue;
            } else {
                row.value = (String) aValue;
            }

            fireTableDataChanged();
        }
        
        public Object getValueAt(int rowIndex, int columnIndex) {
            final PropRow row = getPropRow(rowIndex);
            return (columnIndex == COL_NAME ? row.name : row.value);
        }

        private PropRow getPropRow(int rowIndex) {
            return (PropRow) rows.get(rowIndex);
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) { return true; }        
    }

    static final int COL_NAME = 0;
    static final int COL_VALUE = 1;

    private final JTable tblEntries;
    // For unit testing.
    void setValueAt(Object aValue, int row, int column) {
        tblEntries.setValueAt(aValue, row, column);
    }
    
    private final Action atnSave;
    // For unit testing.
    void doSave() { atnSave.actionPerformed(null); }
    boolean isSaveEnabled() { return atnSave.isEnabled(); }

    private final Action atnNewRow;
    // For unit testing.
    void doNewRow() { atnNewRow.actionPerformed(null); }

    private final Action atnClearAll;
    // For unit testing.
    void doClearAll() { atnClearAll.actionPerformed(null); }

    /**
     * Constructor
     * @param owner parent of this dialog
     * @param agent the BuildAgentService who's entries we are editing
     * @param agentInfo text identifying the agent who's entries we are editing
     * @throws RemoteException if an error occurs getting entry overrides from agent
     */
    public BuildAgentEntryOverrideUI(final Frame owner, final BuildAgentService agent, final String agentInfo)
            throws RemoteException {

        super(owner, "Edit Entry Overrides - " + agentInfo);

        // populate table with existing entry overrides

        final PropertyEntry[] entryOverrides = agent.getEntryOverrides();
        final PropertyTableModel mdlTable = new PropertyTableModel();
        for (int i = 0; i < entryOverrides.length; i++) {
            mdlTable.addRow(new PropRow(entryOverrides[i].name, entryOverrides[i].value));
        }
        mdlTable.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                atnSave.setEnabled(true);
            }
        });
        tblEntries = new JTable(mdlTable);
        tblEntries.getTableHeader().getColumnModel().getColumn(COL_NAME).setHeaderValue("Name");
        tblEntries.getTableHeader().getColumnModel().getColumn(COL_VALUE).setHeaderValue("Value");
        tblEntries.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        final JScrollPane scrTable = new JScrollPane(tblEntries);
        scrTable.setPreferredSize(new Dimension(550, 200));
        final JPanel pnlMain = new JPanel();
        pnlMain.add(scrTable);

        atnSave = new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
                final Properties props = new Properties();
                String name;
                String value;
                for (int i = 0; i < mdlTable.getRowCount(); i++) {
                    name = (String) mdlTable.getValueAt(i, COL_NAME);
                    value = (String) mdlTable.getValueAt(i, COL_VALUE);
                    if (name != null && value != null) {
                        props.put(name, value);
                    }
                }
                try {
                    agent.setEntryOverrides(SearchablePropertyEntries.getPropertiesAsEntryArray(props));
                } catch (RemoteException e1) {
                    final String msg = "Error saving entry overrides.";
                    LOG.error(msg, e1);
                    JOptionPane.showMessageDialog(BuildAgentEntryOverrideUI.this, msg + "\n\n" + e1.getMessage(),
                            "Error Saving Changes", JOptionPane.ERROR_MESSAGE);
                    throw new RuntimeException(msg, e1);
                }
                atnSave.setEnabled(false);
            }
        };
        atnSave.setEnabled(false);

        final JPanel pnlButtons = new JPanel(new GridLayout(0, 1));
        pnlButtons.add(new JButton(atnSave));

        atnNewRow = new AbstractAction("New Row") {
            public void actionPerformed(ActionEvent e) {
                mdlTable.addRow(new PropRow(null, null));
                tblEntries.requestFocusInWindow();
            }
        };
        pnlButtons.add(new JButton(atnNewRow));

        atnClearAll = new AbstractAction("Clear All") {
            public void actionPerformed(ActionEvent e) {
                mdlTable.clearAll();
            }
        };
        pnlButtons.add(new JButton(atnClearAll));

        pnlMain.add(pnlButtons);
        
        getContentPane().add(pnlMain);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent evt) {
                doExit();
            }
        });

        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private void doExit() {
        // Prompt if unsaved settings
        if (atnSave.isEnabled()) {
            final int repsonse = JOptionPane.showConfirmDialog(this,
                    "You have unsaved changes. Save these changes now?",
                    "Save Changes?", JOptionPane.YES_NO_OPTION);
            if (JOptionPane.YES_NO_OPTION == repsonse) {
                atnSave.actionPerformed(null);
            }
        }

        dispose();
    }

}

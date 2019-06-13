package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.awt.GridBagConstraints.*;
import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;
import static ru.byprogminer.Lab8_Programming.gui.GuiUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class PermissionsDialog extends JDialog {

    public interface Listener {

        void addButtonClicked(Event event);
        void removeButtonClicked(Event event);

        void okButtonClicked(Event event);
    }

    public static final class Event {

        public final PermissionsDialog dialog;
        public final String permission;

        private Event(PermissionsDialog dialog, String permission) {
            this.dialog = Objects.requireNonNull(dialog);
            this.permission = permission;
        }
    }

    private static final String ESCAPE_ACTION = "escape";
    private static final int MARGIN = 5;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final DefaultTableModel permissionsTableModel = new DefaultTableModel(arrayOf("Permission"), 0);
    private final JTable permissionsTable = new JTable(permissionsTableModel) {

        @Override
        public boolean isCellEditable(int i, int i1) {
            return false;
        }
    };
    private final JScrollPane permissionsScrollPane = new JScrollPane(permissionsTable);
    private final JButton addButton = new JButton("Add");
    private final JButton removeButton = new JButton("Remove");
    private final JButton okButton = new JButton("Ok");

    private final Set<Listener> listeners = new HashSet<>();
    private String selectedPermission = null;

    public PermissionsDialog(Window parentWindow, String name) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL);
    }

    public PermissionsDialog(Window parentWindow, String name, ModalityType modalityType) {
        super(parentWindow, name, modalityType);

        int row = 0;
        configureDefaultJTable(permissionsTable, permissionsScrollPane);

        permissionsTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> permissionsSelectionModelValueChanged());
        permissionsTable.doLayout();
        contentPane.add(permissionsScrollPane, new GridBagConstraints(0, row, 2, 1, 1, 1, CENTER, BOTH, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        addButton.setFont(DEFAULT_BUTTON_FONT);
        addButton.setMargin(DEFAULT_BUTTON_MARGIN);
        addButton.addActionListener(actionEvent -> sendEvent(Listener::addButtonClicked));
        contentPane.add(addButton, new GridBagConstraints(0, row, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, MARGIN), 0, 0));

        removeButton.setEnabled(false);
        removeButton.setFont(DEFAULT_BUTTON_FONT);
        removeButton.setMargin(DEFAULT_BUTTON_MARGIN);
        removeButton.addActionListener(actionEvent -> sendEvent(Listener::removeButtonClicked));
        contentPane.add(removeButton, new GridBagConstraints(1, row, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        okButton.setFont(DEFAULT_BUTTON_FONT);
        okButton.setMargin(DEFAULT_BUTTON_MARGIN);
        okButton.addActionListener(actionEvent -> sendEvent(Listener::okButtonClicked, new Event(this, null)));
        contentPane.add(okButton, new GridBagConstraints(0, row, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        ++row;

        contentPane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));

        final InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION);

        final ActionMap actionMap = contentPane.getActionMap();
        actionMap.put(ESCAPE_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                sendEvent(Listener::okButtonClicked, new Event(PermissionsDialog.this, null));
            }
        });
        setContentPane(contentPane);
        pack();

        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        deselectPermission();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setPermissions(Set<String> permissions) {
        deselectPermission();

        permissionsTableModel.setRowCount(0);
        permissions.forEach(this::addRow);
    }

    private void permissionsSelectionModelValueChanged() {
        final int selection = permissionsTable.getSelectionModel().getMinSelectionIndex();

        if (selection != -1) {
            selectPermission(selection);
        }
    }

    private void selectPermission(int index) {
        permissionsTable.getSelectionModel().setSelectionInterval(index, index);
        selectedPermission = (String) permissionsTable.getValueAt(index, 0);

        removeButton.setEnabled(true);
    }

    private void deselectPermission() {
        permissionsTable.getSelectionModel().clearSelection();
        selectedPermission = null;

        removeButton.setEnabled(false);
    }

    private void addRow(String permission) {
        permissionsTableModel.addRow(arrayOf(permission));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        sendEvent(handler, new Event(this, selectedPermission));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}

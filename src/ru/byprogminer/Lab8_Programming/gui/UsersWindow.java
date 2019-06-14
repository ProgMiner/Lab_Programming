package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
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
public class UsersWindow extends JFrame {

    public interface Listener {

        void changeUsernameButtonClicked(Event event);
        void changePasswordButtonClicked(Event event);
        void permissionsButtonClicked(Event event);

        void registerButtonClicked(Event event);
        void removeButtonClicked(Event event);

        void okButtonClicked(Event event);
    }

    public static abstract class Adapter implements Listener {

        @Override
        public void changeUsernameButtonClicked(Event event) {}

        @Override
        public void changePasswordButtonClicked(Event event) {}

        @Override
        public void permissionsButtonClicked(Event event) {}

        @Override
        public void registerButtonClicked(Event event) {}

        @Override
        public void removeButtonClicked(Event event) {}

        @Override
        public void okButtonClicked(Event event) {}
    }

    public static final class Event {

        public final UsersWindow window;
        public final String selectedUser;

        private Event(UsersWindow window, String selectedUser) {
            this.window = Objects.requireNonNull(window);
            this.selectedUser = selectedUser;
        }
    }

    private static final String ESCAPE_ACTION = "escape";
    private static final int MARGIN = 5;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final DefaultTableModel usersTableModel = new DefaultTableModel(arrayOf("Username"), 0);
    private final JTable usersTable = new JTable(usersTableModel) {

        @Override
        public boolean isCellEditable(int i, int i1) {
            return false;
        }
    };
    private final JScrollPane usersScrollPane = new JScrollPane(usersTable);
    private final String selectedUserText = "Selected user: ";
    private final JLabel selectedUserLabel = new JLabel(selectedUserText);
    private final JButton changeUsernameButton = new JButton("Change username");
    private final JButton changePasswordButton = new JButton("Change password");
    private final JButton permissionsButton = new JButton("Permissions...");
    private final JButton registerButton = new JButton("Register user");
    private final JButton removeButton = new JButton("Remove user");
    private final JButton okButton = new JButton("Ok");

    private final Set<Listener> listeners = new HashSet<>();
    private String selectedUser = null;

    public UsersWindow(String name) {
        super(name);

        build();
        buildListeners();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setUsers(Set<String> users) {
        deselectUser();

        usersTableModel.setRowCount(0);
        users.forEach(this::addRow);
    }

    public void rebuild() {
        contentPane.removeAll();
        build();

        revalidate();
        repaint();
    }

    private void usersSelectionModelValueChanged() {
        final int selection = usersTable.getSelectionModel().getMinSelectionIndex();

        if (selection != -1) {
            selectUser(selection);
        }
    }

    private void addRow(String username) {
        usersTableModel.addRow(arrayOf(username));
    }

    private void selectUser(int index) {
        usersTable.getSelectionModel().setSelectionInterval(index, index);
        selectedUser = (String) usersTableModel.getValueAt(index, 0);

        selectedUserLabel.setText(selectedUserText + selectedUser);
        changeUsernameButton.setEnabled(true);
        changePasswordButton.setEnabled(true);
        permissionsButton.setEnabled(true);
        removeButton.setEnabled(true);
    }

    private void deselectUser() {
        usersTable.getSelectionModel().clearSelection();
        selectedUser = null;

        selectedUserLabel.setText(selectedUserText);
        changeUsernameButton.setEnabled(false);
        changePasswordButton.setEnabled(false);
        permissionsButton.setEnabled(false);
        removeButton.setEnabled(false);
    }

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        sendEvent(handler, new Event(this, selectedUser));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }

    private void build() {
        configureDefaultJTable(usersTable, usersScrollPane);

        final TableColumnModel usersTableColumnModel = usersTable.getColumnModel();
        usersTableColumnModel.getColumn(0).setMinWidth(100);

        usersTable.doLayout();
        contentPane.add(usersScrollPane, new GridBagConstraints(0, 0, 1, 10, 1, 1, CENTER, BOTH, new Insets(0, 0, MARGIN, MARGIN), 0, 0));

        int row = 0;
        selectedUserLabel.setFont(DEFAULT_FONT);
        selectedUserLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(selectedUserLabel, new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        changeUsernameButton.setFont(DEFAULT_BUTTON_FONT);
        changeUsernameButton.setMargin(DEFAULT_BUTTON_MARGIN);
        contentPane.add(changeUsernameButton, new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        changePasswordButton.setFont(DEFAULT_BUTTON_FONT);
        changePasswordButton.setMargin(DEFAULT_BUTTON_MARGIN);
        contentPane.add(changePasswordButton, new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        permissionsButton.setFont(DEFAULT_BUTTON_FONT);
        permissionsButton.setMargin(DEFAULT_BUTTON_MARGIN);
        contentPane.add(permissionsButton, new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        contentPane.add(new JSeparator(SwingConstants.HORIZONTAL), new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        registerButton.setFont(DEFAULT_BUTTON_FONT);
        registerButton.setMargin(DEFAULT_BUTTON_MARGIN);
        contentPane.add(registerButton, new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        removeButton.setEnabled(false);
        removeButton.setFont(DEFAULT_BUTTON_FONT);
        removeButton.setMargin(DEFAULT_BUTTON_MARGIN);
        contentPane.add(removeButton, new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        contentPane.add(new JSeparator(SwingConstants.HORIZONTAL), new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        okButton.setFont(DEFAULT_BUTTON_FONT);
        okButton.setMargin(DEFAULT_BUTTON_MARGIN);
        contentPane.add(okButton, new GridBagConstraints(1, row, 1, 1, 0, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        contentPane.add(Box.createVerticalGlue(), new GridBagConstraints(1, row, 2, 1, 0, 0, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));
        ++row;

        contentPane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        setContentPane(contentPane);
        pack();

        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        deselectUser();
    }

    private void buildListeners() {
        usersTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> usersSelectionModelValueChanged());
        changeUsernameButton.addActionListener(actionEvent -> sendEvent(Listener::changeUsernameButtonClicked));
        changePasswordButton.addActionListener(actionEvent -> sendEvent(Listener::changePasswordButtonClicked));
        permissionsButton.addActionListener(actionEvent -> sendEvent(Listener::permissionsButtonClicked));
        registerButton.addActionListener(actionEvent -> sendEvent(Listener::registerButtonClicked));
        removeButton.addActionListener(actionEvent -> sendEvent(Listener::removeButtonClicked));
        okButton.addActionListener(actionEvent -> sendEvent(Listener::okButtonClicked));

        final InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION);

        final ActionMap actionMap = contentPane.getActionMap();
        actionMap.put(ESCAPE_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                sendEvent(Listener::okButtonClicked, new Event(UsersWindow.this, null));
            }
        });
    }
}

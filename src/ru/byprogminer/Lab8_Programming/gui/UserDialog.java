package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.awt.GridBagConstraints.*;
import static ru.byprogminer.Lab5_Programming.LabUtils.containsRef;
import static ru.byprogminer.Lab8_Programming.gui.GuiUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class UserDialog extends JDialog {

    public interface Listener {

        void okButtonClicked(Event event);
        void cancelButtonClicked(Event event);
    }

    public static abstract class Adapter implements Listener {

        @Override
        public void okButtonClicked(Event event) {}

        @Override
        public void cancelButtonClicked(Event event) {}
    }

    public static final class Event {

        public final UserDialog dialog;
        public final String username;
        public final String email;
        public final char[] password;

        private Event(UserDialog dialog, String username, String email, char[] password) {
            this.dialog = Objects.requireNonNull(dialog);
            this.username = username;
            this.email = email;
            this.password = password;
        }
    }

    public static class Kind {

        public enum Field {

            USERNAME, EMAIL, PASSWORD
        }

        public static final Kind LOGIN = new Kind("Login", "Cancel");
        public static final Kind REGISTER = new Kind("Register", "Cancel", Field.USERNAME, Field.EMAIL);

        public static final Kind USERNAME = new Kind("Ok", "Cancel", Field.USERNAME);
        public static final Kind PASSWORD = new Kind("Ok", "Cancel", Field.PASSWORD);

        public final String okText;
        public final String cancelText;
        public final Field[] fields;

        public Kind(String okText, String cancelText, Field... fields) {
            this.okText = okText;
            this.cancelText = cancelText;
            this.fields = Objects.requireNonNull(fields);
        }

        public Kind(String okText, String cancelText) {
            this(okText, cancelText, Field.USERNAME, Field.PASSWORD);
        }
    }

    private static final String ENTER_ACTION = "enter";
    private static final String ESCAPE_ACTION = "escape";
    private static final int MARGIN = 5;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final JLabel usernameLabel = new JLabel("Username:", JLabel.RIGHT);
    private final JTextField usernameTextField = new JTextField(DEFAULT_TEXT_FIELD_COLUMNS);
    private final JLabel emailLabel = new JLabel("E-mail:", JLabel.RIGHT);
    private final JTextField emailTextField = new JTextField(DEFAULT_TEXT_FIELD_COLUMNS);
    private final JLabel passwordLabel = new JLabel("Password:", JLabel.RIGHT);
    private final JPasswordField passwordPasswordField = new JPasswordField(DEFAULT_TEXT_FIELD_COLUMNS);
    private final JButton okButton = new JButton();
    private final JButton cancelButton = new JButton();

    private final Set<Listener> listeners = new HashSet<>();

    public UserDialog(Window parentWindow, String name, Kind kind, String initialUsername) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL, kind, initialUsername);
    }

    public UserDialog(Window parentWindow, String name, ModalityType modalityType, Kind kind, String initialUsername) {
        super(parentWindow, name, modalityType);

        int row = 0;
        if (containsRef(kind.fields, Kind.Field.USERNAME)) {
            usernameLabel.setFont(DEFAULT_FONT);
            usernameLabel.setBorder(DEFAULT_MARGIN_BORDER);
            contentPane.add(usernameLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

            usernameTextField.setFont(DEFAULT_FONT);
            usernameTextField.setText(initialUsername);
            usernameTextField.setMargin(DEFAULT_MARGIN);
            contentPane.add(usernameTextField, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

            ++row;
        }

        if (containsRef(kind.fields, Kind.Field.EMAIL)) {
            emailLabel.setFont(DEFAULT_FONT);
            emailLabel.setBorder(DEFAULT_MARGIN_BORDER);
            contentPane.add(emailLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

            emailTextField.setFont(DEFAULT_FONT);
            emailTextField.setMargin(DEFAULT_MARGIN);
            contentPane.add(emailTextField, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

            ++row;
        }

        if (containsRef(kind.fields, Kind.Field.PASSWORD)) {
            passwordLabel.setFont(DEFAULT_FONT);
            passwordLabel.setBorder(DEFAULT_MARGIN_BORDER);
            contentPane.add(passwordLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

            passwordPasswordField.setFont(DEFAULT_FONT);
            passwordPasswordField.setMargin(DEFAULT_MARGIN);
            contentPane.add(passwordPasswordField, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

            ++row;
        }
        contentPane.add(Box.createVerticalGlue(), new GridBagConstraints(0, row, 2, 1, 1, 1, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));
        ++row;

        if (kind.okText != null) {
            okButton.setText(kind.okText);
            okButton.setFont(DEFAULT_BUTTON_FONT);
            okButton.setMargin(DEFAULT_BUTTON_MARGIN);
            okButton.addActionListener(actionEvent -> sendEvent(Listener::okButtonClicked));
            contentPane.add(okButton, new GridBagConstraints(0, row, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
            ++row;
        }

        if (kind.cancelText != null) {
            cancelButton.setText(kind.cancelText);
            cancelButton.setFont(DEFAULT_BUTTON_FONT);
            cancelButton.setMargin(DEFAULT_BUTTON_MARGIN);
            cancelButton.addActionListener(actionEvent -> sendEvent(Listener::cancelButtonClicked, new Event(this, null, null, null)));
            contentPane.add(cancelButton, new GridBagConstraints(0, row, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            ++row;
        }

        contentPane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));

        final InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ENTER_ACTION);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION);

        final ActionMap actionMap = contentPane.getActionMap();
        actionMap.put(ENTER_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                sendEvent(Listener::okButtonClicked);
            }
        });
        actionMap.put(ESCAPE_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                sendEvent(Listener::cancelButtonClicked, new Event(UserDialog.this, null, null, null));
            }
        });
        setContentPane(contentPane);
        pack();

        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        sendEvent(handler, new Event(this, usernameTextField.getText(),
                emailTextField.getText(), passwordPasswordField.getPassword()));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}

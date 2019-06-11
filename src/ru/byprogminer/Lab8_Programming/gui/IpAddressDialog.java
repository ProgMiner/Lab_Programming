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
import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;
import static ru.byprogminer.Lab5_Programming.LabUtils.validatePort;
import static ru.byprogminer.Lab8_Programming.gui.GuiUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class IpAddressDialog extends JDialog {

    public interface Listener {

        void okButtonClicked(Event event);
        void cancelButtonClicked(Event event);
    }

    public static final class Event {

        public final IpAddressDialog dialog;
        public final String address;
        public final int port;

        private Event(IpAddressDialog dialog, String address, int port) {
            this.dialog = Objects.requireNonNull(dialog);
            this.address = address;
            this.port = port;
        }
    }

    public static class Kind {

        public static final Kind SERVER_STARTING = new Kind("Start server", "Skip server starting");
        public static final Kind CONNECT = new Kind("Connect", null);

        public final String okText;
        public final String cancelText;

        public Kind(String okText, String cancelText) {
            this.okText = okText;
            this.cancelText = cancelText;
        }
    }

    private static final String ENTER_ACTION = "enter";
    private static final String ESCAPE_ACTION = "escape";
    private static final int MARGIN = 5;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final JLabel addressLabel = new JLabel("Address:", JLabel.RIGHT);
    private final JTextField addressTextField = new JTextField(DEFAULT_TEXT_FIELD_COLUMNS);
    private final JLabel portLabel = new JLabel("Port:", JLabel.RIGHT);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JButton okButton = new JButton();
    private final JButton cancelButton = new JButton();

    private final Set<Listener> listeners = new HashSet<>();

    public IpAddressDialog(Window parentWindow, String name, Kind kind, String initialAddress, int initialPort) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL, kind, initialAddress, initialPort);
    }

    public IpAddressDialog(
            Window parentWindow,
            String name,
            ModalityType modalityType,
            Kind kind,
            String initialAddress,
            int initialPort
    ) {
        super(parentWindow, name, modalityType);

        int row = 0;
        addressLabel.setFont(DEFAULT_FONT);
        addressLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(addressLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

        addressTextField.setFont(DEFAULT_FONT);
        addressTextField.setText(initialAddress);
        addressTextField.setMargin(DEFAULT_MARGIN);
        contentPane.add(addressTextField, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        portLabel.setFont(DEFAULT_FONT);
        portLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(portLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

        final JSpinner.DefaultEditor portSpinnerEditor = (JSpinner.DefaultEditor) portSpinner.getEditor();
        portSpinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        portSpinnerEditor.getTextField().setBorder(DEFAULT_MARGIN_BORDER);

        portSpinner.setFont(DEFAULT_FONT);
        portSpinner.getModel().setValue(initialPort);
        contentPane.add(portSpinner, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

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
            cancelButton.addActionListener(actionEvent -> sendEvent(Listener::cancelButtonClicked, new Event(this, null, -1)));
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
                sendEvent(Listener::cancelButtonClicked, new Event(IpAddressDialog.this, null, -1));
            }
        });
        setContentPane(contentPane);
        setLocationRelativeTo(parentWindow);
        pack();

        addressTextField.requestFocus();
        setMinimumSize(getSize());
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        final int port;

        try {
            port = validatePort(portSpinner.getModel().getValue().toString());
        } catch (Throwable e) {
            JOptionPane.showMessageDialog(this, arrayOf("Bad port provided", e.getLocalizedMessage()));
            return;
        }

        sendEvent(handler, new Event(this, addressTextField.getText(), port));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}

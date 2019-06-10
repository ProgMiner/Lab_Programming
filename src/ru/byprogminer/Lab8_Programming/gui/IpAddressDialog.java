package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static java.awt.GridBagConstraints.*;
import static ru.byprogminer.Lab5_Programming.LabUtils.validatePort;
import static ru.byprogminer.Lab8_Programming.gui.GuiUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class IpAddressDialog extends ActionRotationDialog<IpAddressDialog.Action> {

    public static abstract class Action {

        public static final class Ok extends Action {

            public final String address;
            public final int port;

            private Ok(String address, int port) {
                this.address = address;
                this.port = port;
            }
        }

        public static final class Cancel extends Action {}

        public static final class Close extends Action {}

        private Action() {}
    }

    public enum Kind {

        NONE("Ok", "Cancel"),
        SERVER_STARTING("Start server", "Skip server starting"),
        CONNECT("Connect", null);

        public final String okText;
        public final String cancelText;

        Kind(String okText, String cancelText) {
            this.okText = okText;
            this.cancelText = cancelText;
        }
    }

    private final int margin = 5;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final JLabel addressLabel = new JLabel("Address:", JLabel.RIGHT);
    private final JTextField addressTextField = new JTextField("0.0.0.0");
    private final JLabel portLabel = new JLabel("Port:", JLabel.RIGHT);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JButton okButton = new JButton();
    private final JButton cancelButton = new JButton();

    public IpAddressDialog(Window parentWindow, String name, Kind kind, int initialPort) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL, kind, initialPort);
    }

    public IpAddressDialog(Window parentWindow, String name, ModalityType modalityType, Kind kind, int initialPort) {
        super(parentWindow, name, modalityType);

        addressLabel.setFont(DEFAULT_FONT);
        addressLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(addressLabel, new GridBagConstraints(0, 0, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, margin), 0, 0));

        addressTextField.setFont(DEFAULT_FONT);
        addressTextField.setMargin(DEFAULT_MARGIN);
        contentPane.add(addressTextField, new GridBagConstraints(1, 0, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        portLabel.setFont(DEFAULT_FONT);
        portLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(portLabel, new GridBagConstraints(0, 1, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, margin), 0, 0));

        portSpinner.setFont(DEFAULT_FONT);
        portSpinner.getModel().setValue(initialPort);
        ((JSpinner.DefaultEditor) portSpinner.getEditor()).getTextField().setBorder(DEFAULT_MARGIN_BORDER);
        ((JSpinner.DefaultEditor) portSpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        contentPane.add(portSpinner, new GridBagConstraints(1, 1, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));
        contentPane.add(Box.createVerticalGlue(), new GridBagConstraints(0, 2, 2, 1, 1, 1, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));

        if (kind.okText != null) {
            okButton.setText(kind.okText);
            okButton.setMargin(DEFAULT_MARGIN);
            okButton.setFont(DEFAULT_FONT.deriveFont(Font.BOLD));
            okButton.addActionListener(actionEvent -> {
                if (getAction() != null) {
                    return;
                }

                setAllEnabled(false);
                final int port;

                try {
                    port = validatePort(portSpinner.getModel().getValue().toString());
                } catch (Throwable e) {
                    JOptionPane.showMessageDialog(this, "Bad port provided: " + e.getLocalizedMessage());
                    return;
                }

                setAction(new Action.Ok(addressTextField.getText(), port));
            });
            contentPane.add(okButton, new GridBagConstraints(0, 3, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));
        }

        if (kind.cancelText != null) {
            cancelButton.setText(kind.cancelText);
            cancelButton.setMargin(DEFAULT_MARGIN);
            cancelButton.setFont(DEFAULT_FONT.deriveFont(Font.BOLD));
            cancelButton.addActionListener(actionEvent -> {
                if (getAction() == null) {
                    setAllEnabled(false);
                    setAction(new Action.Cancel());
                }
            });
            contentPane.add(cancelButton, new GridBagConstraints(0, 4, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        }

        contentPane.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (getAction() == null) {
                    setAllEnabled(false);
                    setAction(new Action.Close());
                }
            }
        });
        setContentPane(contentPane);
        pack();

        setMinimumSize(getSize());
    }

    @Override
    protected void revertState() {
        setAllEnabled(true);
    }

    private void setAllEnabled(boolean enabled) {
        addressTextField.setEnabled(enabled);
        portSpinner.setEnabled(enabled);
        okButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }
}

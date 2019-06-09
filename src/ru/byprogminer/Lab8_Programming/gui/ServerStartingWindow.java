package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.awt.GridBagConstraints.*;
import static ru.byprogminer.Lab5_Programming.LabUtils.validatePort;
import static ru.byprogminer.Lab8_Programming.gui.GuiUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class ServerStartingWindow extends JFrame {

    private final int margin = 5;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final JLabel addressLabel = new JLabel("Address:", JLabel.RIGHT);
    private final JTextField addressTextField = new JTextField("0.0.0.0");
    private final JLabel portLabel = new JLabel("Port:", JLabel.RIGHT);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JButton startButton = new JButton("Start server");
    private final JButton skipButton = new JButton("Skip server starting");

    private volatile BiFunction<String, Integer, Boolean> startListener = null;
    private volatile Supplier<Boolean> skipListener = null;

    public ServerStartingWindow(int initialPort) {
        super(BASE_APP_NAME);

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

        startButton.setMargin(DEFAULT_MARGIN);
        startButton.setFont(DEFAULT_FONT.deriveFont(Font.BOLD));
        startButton.addActionListener(actionEvent -> {
            final BiFunction<String, Integer, Boolean> tmpStartListener = startListener;

            if (tmpStartListener != null) {
                setAllEnabled(false);

                final Integer port;

                try {
                    port = validatePort(portSpinner.getModel().getValue().toString());
                } catch (Throwable e) {
                    JOptionPane.showMessageDialog(this, "Bad port provided: " + e.getLocalizedMessage());
                    return;
                }

                new Thread(() -> {
                    if (tmpStartListener.apply(addressTextField.getText(), port)) {
                        setVisible(false);
                    } else {
                        setAllEnabled(true);
                    }
                }).start();
            }
        });
        contentPane.add(startButton, new GridBagConstraints(0, 3, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        skipButton.setMargin(DEFAULT_MARGIN);
        skipButton.setFont(DEFAULT_FONT.deriveFont(Font.BOLD));
        skipButton.addActionListener(actionEvent -> {
            final Supplier<Boolean> tmpSkipListener = skipListener;

            if (tmpSkipListener != null) {
                setAllEnabled(false);

                new Thread(() -> {
                    if (tmpSkipListener.get()) {
                        setVisible(false);
                    } else {
                        setAllEnabled(true);
                    }
                }).start();
            }
        });
        contentPane.add(skipButton, new GridBagConstraints(0, 4, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPane.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        setContentPane(contentPane);
        pack();

        setMinimumSize(getSize());
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);

        ((JSpinner.DefaultEditor) portSpinner.getEditor()).getTextField().setMargin(DEFAULT_MARGIN);
    }

    public void setStartListener(BiFunction<String, Integer, Boolean> listener) {
        startListener = listener;
    }

    public void setSkipListener(Supplier<Boolean> listener) {
        skipListener = listener;
    }

    private void setAllEnabled(boolean enabled) {
        addressTextField.setEnabled(enabled);
        portSpinner.setEnabled(enabled);
        startButton.setEnabled(enabled);
        skipButton.setEnabled(enabled);
    }
}

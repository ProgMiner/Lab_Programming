package ru.byprogminer.Lab8_Programming.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
public class AboutDialog extends JDialog {

    private static final String BACKGROUND = "/resources/about.png";
    private static final String ESCAPE_ACTION = "escape";
    private static final float TITLE_FONT_SIZE = 38;
    private static final int BACKGROUND_WIDTH = 862;
    private static final int BACKGROUND_HEIGHT = 600;
    private static final int MARGIN = 5;

    private final JLabel titleLabel = new JLabel();
    private final JPanel contentPanel = new JPanel(new GridLayout(1, 1));
    private final JScrollPane contentScrollPane = new JScrollPane(contentPanel);
    private final JPanel contentPane = GuiUtils.makeBackgroundPanel(loadBackground());

    public AboutDialog(Window parentWindow, String name) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL);
    }

    public AboutDialog(Window parentWindow, String name, ModalityType modalityType) {
        super(parentWindow, name, modalityType);
        contentPane.setLayout(null);

        titleLabel.setOpaque(true);
        titleLabel.setBorder(null);
        titleLabel.setVerticalAlignment(JLabel.BOTTOM);
        titleLabel.setBackground(GuiUtils.TRANSPARENT_COLOR);
        titleLabel.setFont(GuiUtils.DEFAULT_FONT.deriveFont(TITLE_FONT_SIZE));
        titleLabel.setBounds(137, 75, 525, 107);
        contentPane.add(titleLabel);

        contentPanel.setBorder(null);
        contentPanel.setOpaque(false);
        contentPanel.setBackground(GuiUtils.TRANSPARENT_COLOR);
        contentScrollPane.getViewport().setBackground(GuiUtils.TRANSPARENT_COLOR);
        contentScrollPane.getViewport().setOpaque(false);
        contentScrollPane.setBorder(null);
        contentScrollPane.setOpaque(false);
        contentScrollPane.setBackground(GuiUtils.TRANSPARENT_COLOR);
        contentScrollPane.setBounds(137, 200, 525, 325);
        contentPane.add(contentScrollPane);
        contentPane.setOpaque(false);
        contentPane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        contentPane.setPreferredSize(new Dimension(BACKGROUND_WIDTH, BACKGROUND_HEIGHT));
        contentPane.setMinimumSize(new Dimension(BACKGROUND_WIDTH, BACKGROUND_HEIGHT));
        contentPane.setMaximumSize(new Dimension(BACKGROUND_WIDTH, BACKGROUND_HEIGHT));

        final InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION);

        final ActionMap actionMap = contentPane.getActionMap();
        actionMap.put(ESCAPE_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispatchEvent(new WindowEvent(AboutDialog.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        setContentPane(contentPane);
        setUndecorated(true);
        setBackground(GuiUtils.TRANSPARENT_COLOR);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowDeactivated(WindowEvent mouseEvent) {
                dispatchEvent(new WindowEvent(AboutDialog.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        pack();

        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    public void setTitle(String title) {
        titleLabel.setText(Objects.requireNonNull(title));
        repaint();
    }

    public void setContent(Component content) {
        contentPanel.removeAll();

        contentPanel.add(content);
        revalidate();
        repaint();
    }

    public void setContent(String content) {
        final JLabel contentLabel = new JLabel(content);

        contentLabel.setText(content);
        contentLabel.setVerticalAlignment(JLabel.TOP);
        contentLabel.setFont(GuiUtils.DEFAULT_FONT);
        setContent(contentLabel);
    }

    private static BufferedImage loadBackground() {
        try {
            return ImageIO.read(Objects.requireNonNull(AboutDialog.class.getResourceAsStream(BACKGROUND)));
        } catch (IOException | NullPointerException e) {
            return null;
        }
    }
}

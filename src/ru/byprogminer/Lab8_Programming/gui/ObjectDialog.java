package ru.byprogminer.Lab8_Programming.gui;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.awt.GridBagConstraints.*;
import static ru.byprogminer.Lab5_Programming.LabUtils.*;
import static ru.byprogminer.Lab8_Programming.gui.GuiUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class ObjectDialog<T extends Object> extends JDialog {

    public interface Listener<T extends Object> {

        void okButtonClicked(Event<T> event);
        void cancelButtonClicked(Event<T> event);
    }

    public static final class Event<T extends Object> {

        public final ObjectDialog dialog;
        public final T object;

        private Event(ObjectDialog dialog, T object) {
            this.dialog = Objects.requireNonNull(dialog);
            this.object = object;
        }
    }

    public static class Kind {

        public enum Fields {

            OBJECT, LIVING_OBJECT
        }

        public static final Kind OBJECT = new Kind("Continue", "Cancel", Fields.OBJECT);
        public static final Kind LIVING_OBJECT = new Kind("Continue", "Cancel", Fields.LIVING_OBJECT);

        public final String okText;
        public final String cancelText;
        public final Fields fields;

        public Kind(String okText, String cancelText, Fields fields) {
            this.okText = okText;
            this.cancelText = cancelText;
            this.fields = Objects.requireNonNull(fields);
        }
    }

    private static final String ENTER_ACTION = "enter";
    private static final String ESCAPE_ACTION = "escape";
    private static final int MARGIN = 5;

    private static final int MAX_ICON_WIDTH = 50;
    private static final int MAX_ICON_HEIGHT = 20;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final JLabel nameLabel = new JLabel("Name:", JLabel.RIGHT);
    private final JTextField nameTextField = new JTextField(DEFAULT_TEXT_FIELD_COLUMNS);
    private final JLabel volumeLabel = new JLabel("Volume:", JLabel.RIGHT);
    private final SpinnerNumberModel volumeSpinnerModel = new SpinnerNumberModel(0.0, null, null, 0.1);
    private final JSpinner volumeSpinner = new JSpinner(volumeSpinnerModel);
    private final JLabel creatingTimeLabel = new JLabel("Creating time:", JLabel.RIGHT);
    private final JTextField creatingTimeTextField = new JTextField(16);
    private final JLabel positionLabel = new JLabel("Position:", JLabel.RIGHT);
    private final JPanel positionPanel = new JPanel(new GridLayout(1, 3, MARGIN, MARGIN));
    private final SpinnerNumberModel positionXSpinnerModel = new SpinnerNumberModel(0.0, null, null, 0.1);
    private final JSpinner positionXSpinner = new JSpinner(positionXSpinnerModel);
    private final SpinnerNumberModel positionYSpinnerModel = new SpinnerNumberModel(0.0, null, null, 0.1);
    private final JSpinner positionYSpinner = new JSpinner(positionYSpinnerModel);
    private final SpinnerNumberModel positionZSpinnerModel = new SpinnerNumberModel(0.0, null, null, 0.1);
    private final JSpinner positionZSpinner = new JSpinner(positionZSpinnerModel);
    private final JLabel livesLabel = new JLabel("Lives:", JLabel.RIGHT);
    private final JCheckBox livesCheckBox = new JCheckBox();
    private final JLabel imageLabel = new JLabel("Image:", JLabel.RIGHT);
    private final JButton imageButton = new JButton("Select file...");
    private final JButton itemsButton = new JButton("Items...");
    private final JButton okButton = new JButton();
    private final JButton cancelButton = new JButton();

    private final Kind kind;
    private BufferedImage image = null;
    private final Set<Object> items = new HashSet<>();

    private final Set<Listener<T>> listeners = new HashSet<>();

    public ObjectDialog(Window parentWindow, String name, Kind kind, Object initialObject) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL, kind, initialObject);
    }

    public ObjectDialog(Window parentWindow, String name, ModalityType modalityType, Kind kind, Object initialObject) {
        super(parentWindow, name, modalityType);

        this.kind = kind;
        final LivingObject initialLivingObject;
        if (kind.fields == Kind.Fields.LIVING_OBJECT) {
            initialLivingObject = (LivingObject) initialObject;
        } else {
            initialLivingObject = null;
        }

        int row = 0;
        nameLabel.setFont(DEFAULT_FONT);
        nameLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(nameLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

        nameTextField.setFont(DEFAULT_FONT);
        nameTextField.setMargin(DEFAULT_MARGIN);
        if (initialObject != null) {
            nameTextField.setText(initialObject.getName());
        }
        contentPane.add(nameTextField, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        volumeLabel.setFont(DEFAULT_FONT);
        volumeLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(volumeLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

        final JSpinner.DefaultEditor volumeSpinnerEditor = (JSpinner.DefaultEditor) volumeSpinner.getEditor();
        volumeSpinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        volumeSpinnerEditor.getTextField().setBorder(DEFAULT_MARGIN_BORDER);

        volumeSpinner.setFont(DEFAULT_FONT);
        if (initialObject != null) {
            volumeSpinner.getModel().setValue(initialObject.getVolume());
        }
        contentPane.add(volumeSpinner, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        creatingTimeLabel.setFont(DEFAULT_FONT);
        creatingTimeLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(creatingTimeLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

        creatingTimeTextField.setFont(DEFAULT_FONT);
        creatingTimeTextField.setMargin(DEFAULT_MARGIN);
        if (initialObject != null) {
            creatingTimeTextField.setText(initialObject.getCreatingTime().format(Object.DATE_TIME_FORMATTER));
        } else {
            creatingTimeTextField.setText(LocalDateTime.now().format(Object.DATE_TIME_FORMATTER));
        }
        contentPane.add(creatingTimeTextField, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        positionLabel.setFont(DEFAULT_FONT);
        positionLabel.setBorder(DEFAULT_MARGIN_BORDER);
        contentPane.add(positionLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

        final JSpinner.DefaultEditor positionXSpinnerEditor = (JSpinner.DefaultEditor) positionXSpinner.getEditor();
        positionXSpinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        positionXSpinnerEditor.getTextField().setBorder(DEFAULT_MARGIN_BORDER);

        positionXSpinner.setFont(DEFAULT_FONT);
        if (initialObject != null) {
            positionXSpinner.getModel().setValue(initialObject.getX());
        }
        positionPanel.add(positionXSpinner);

        final JSpinner.DefaultEditor positionYSpinnerEditor = (JSpinner.DefaultEditor) positionYSpinner.getEditor();
        positionYSpinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        positionYSpinnerEditor.getTextField().setBorder(DEFAULT_MARGIN_BORDER);

        positionYSpinner.setFont(DEFAULT_FONT);
        if (initialObject != null) {
            positionYSpinner.getModel().setValue(initialObject.getY());
        }
        positionPanel.add(positionYSpinner);

        final JSpinner.DefaultEditor positionZSpinnerEditor = (JSpinner.DefaultEditor) positionZSpinner.getEditor();
        positionZSpinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        positionZSpinnerEditor.getTextField().setBorder(DEFAULT_MARGIN_BORDER);

        positionZSpinner.setFont(DEFAULT_FONT);
        if (initialObject != null) {
            positionZSpinner.getModel().setValue(initialObject.getZ());
        }
        positionPanel.add(positionZSpinner);
        contentPane.add(positionPanel, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        if (kind.fields == Kind.Fields.LIVING_OBJECT) {
            livesLabel.setFont(DEFAULT_FONT);
            livesLabel.setBorder(DEFAULT_MARGIN_BORDER);
            contentPane.add(livesLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

            livesCheckBox.setFont(DEFAULT_FONT);
            livesCheckBox.setMargin(DEFAULT_MARGIN);
            if (initialLivingObject != null) {
                livesCheckBox.setSelected(initialLivingObject.isLives());
            } else {
                livesCheckBox.setSelected(true);
            }
            contentPane.add(livesCheckBox, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
            ++row;

            imageLabel.setFont(DEFAULT_FONT);
            imageLabel.setBorder(DEFAULT_MARGIN_BORDER);
            contentPane.add(imageLabel, new GridBagConstraints(0, row, 1, 1, 1, 0, ABOVE_BASELINE, HORIZONTAL, new Insets(0, 0, 0, MARGIN), 0, 0));

            if (initialLivingObject != null) {
                imageButton.setIcon(imageToIcon(image = initialLivingObject.getImage()));
            }

            imageButton.setFont(DEFAULT_BUTTON_FONT);
            imageButton.setMargin(DEFAULT_BUTTON_MARGIN);
            imageButton.addActionListener(actionEvent -> imageButtonClicked());
            contentPane.add(imageButton, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
            ++row;

            if (initialLivingObject != null) {
                items.addAll(initialLivingObject.getItems());
            }

            itemsButton.setFont(DEFAULT_BUTTON_FONT);
            itemsButton.setMargin(DEFAULT_BUTTON_MARGIN);
            itemsButton.addActionListener(actionEvent -> itemsButtonClicked());
            contentPane.add(itemsButton, new GridBagConstraints(1, row, 1, 1, 3, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
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
            cancelButton.addActionListener(actionEvent -> sendEvent(Listener::cancelButtonClicked, new Event<>(this, null)));
            contentPane.add(cancelButton, new GridBagConstraints(0, row, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            ++row;
        }

        contentPane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        setContentPane(contentPane);

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
                sendEvent(Listener::cancelButtonClicked, new Event<>(ObjectDialog.this, null));
            }
        });
        setLocationRelativeTo(parentWindow);
        pack();

        setMinimumSize(getSize());
    }

    public void addListener(Listener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener<T> listener) {
        listeners.remove(listener);
    }

    private void imageButtonClicked() {
        final JFileChooser fileChooser = new JFileChooser("Select image file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        final int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();

            try {
                image = ImageIO.read(selectedFile);

                if (image == null) {
                    throw new IllegalArgumentException("File is not image");
                }

                imageButton.setIcon(imageToIcon(image));
                imageButton.setText(selectedFile.getAbsolutePath());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, arrayOf("Bad file selected", e.getLocalizedMessage()));
            }
        }
    }

    private void itemsButtonClicked() {
        // TODO items dialog
    }

    private Icon imageToIcon(BufferedImage image) {
        final int oldWidth = image.getWidth();
        final int oldHeight = image.getHeight();

        final double coef = Math.min(
                (double) Math.min(MAX_ICON_WIDTH, oldWidth) / oldWidth,
                (double) Math.min(MAX_ICON_HEIGHT, oldHeight) / oldHeight
        );

        final BufferedImage preparedImage = new BufferedImage((int) (oldWidth * coef),
                (int) (oldHeight * coef), BufferedImage.TYPE_INT_ARGB);

        preparedImage.getGraphics().drawImage(image, 0, 0, preparedImage.getWidth(), preparedImage.getHeight(), null);
        return new ImageIcon(preparedImage);
    }

    @SuppressWarnings("unchecked")
    private void sendEvent(BiConsumer<Listener<T>, Event<T>> handler) {
        try {
            if (kind.fields == Kind.Fields.LIVING_OBJECT) {
                final LivingObject livingObject = new LivingObject(nameTextField.getText(),
                        volumeSpinnerModel.getNumber().doubleValue(),
                        parseLocalDateTime(creatingTimeTextField.getText()),
                        positionXSpinnerModel.getNumber().doubleValue(),
                        positionYSpinnerModel.getNumber().doubleValue(),
                        positionZSpinnerModel.getNumber().doubleValue());

                setLivingObjectLives(livingObject, livesCheckBox.isSelected());
                livingObject.getItems().addAll(items);

                if (image != null) {
                    livingObject.setImage(image);
                }

                sendEvent(handler, new Event<>(this, (T) livingObject));
            } else {
                final Object object = new Object(nameTextField.getText(),
                        volumeSpinnerModel.getNumber().doubleValue(),
                        parseLocalDateTime(creatingTimeTextField.getText()),
                        positionXSpinnerModel.getNumber().doubleValue(),
                        positionYSpinnerModel.getNumber().doubleValue(),
                        positionZSpinnerModel.getNumber().doubleValue());

                sendEvent(handler, new Event<>(this, (T) object));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, arrayOf("Bad object format", e.getLocalizedMessage()));
        }
    }

    private void sendEvent(BiConsumer<Listener<T>, Event<T>> handler, Event<T> event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}

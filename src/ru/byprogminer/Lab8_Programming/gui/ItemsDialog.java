package ru.byprogminer.Lab8_Programming.gui;

import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.awt.GridBagConstraints.*;
import static ru.byprogminer.Lab5_Programming.LabUtils.*;
import static ru.byprogminer.Lab8_Programming.gui.GuiUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class ItemsDialog extends JDialog {

    public interface Listener {

        void saveButtonClicked(Event event);
        void cancelButtonClicked(Event event);
    }

    public static final class Event {

        public final ItemsDialog dialog;
        public final Set<Object> items;

        private Event(ItemsDialog dialog, Set<Object> items) {
            this.dialog = Objects.requireNonNull(dialog);
            this.items = items == null ? null : Collections.unmodifiableSet(items);
        }
    }

    private static final String ESCAPE_ACTION = "escape";
    private static final int MARGIN = 5;

    private final JPanel contentPane = new JPanel(new GridBagLayout());
    private final DefaultTableModel itemsTableModel = new DefaultTableModel(arrayOf("Name", "Volume", "Creating time", "X", "Y", "Z"), 0);
    private final JTable itemsTable = new JTable(itemsTableModel);
    private final JScrollPane itemsScrollPane = new JScrollPane(itemsTable);
    private final JButton addButton = new JButton("Add item");
    private final JButton removeButton = new JButton("Remove item");
    private final JButton saveButton = new JButton("Save");
    private final JButton cancelButton = new JButton("Cancel");

    private final Set<Listener> listeners = new HashSet<>();
    private final Logger log = Loggers.getObjectLogger(this);

    public ItemsDialog(Window parentWindow, String name) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL);
    }

    public ItemsDialog(Window parentWindow, String name, ModalityType modalityType) {
        super(parentWindow, name, modalityType);

        int row = 0;
        configureDefaultJTable(itemsTable, itemsScrollPane);

        final TableColumnModel itemsTableColumnModel = itemsTable.getColumnModel();
        itemsTableColumnModel.getColumn(0).setMinWidth(100);
        itemsTableColumnModel.getColumn(0).setPreferredWidth(120);
        itemsTableColumnModel.getColumn(1).setMinWidth(60);
        itemsTableColumnModel.getColumn(2).setMinWidth(145);
        itemsTableColumnModel.getColumn(3).setMinWidth(25);
        itemsTableColumnModel.getColumn(4).setMinWidth(25);
        itemsTableColumnModel.getColumn(5).setMinWidth(25);

        itemsTable.getSelectionModel().addListSelectionListener(listSelectionEvent ->
                removeButton.setEnabled(!itemsTable.getSelectionModel().isSelectionEmpty()));
        itemsTable.doLayout();
        contentPane.add(itemsScrollPane, new GridBagConstraints(0, row, 2, 1, 1, 1, CENTER, BOTH, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        addButton.setFont(DEFAULT_BUTTON_FONT);
        addButton.setMargin(DEFAULT_BUTTON_MARGIN);
        addButton.addActionListener(actionEvent -> addButtonClicked());
        contentPane.add(addButton, new GridBagConstraints(0, row, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, MARGIN), 0, 0));

        removeButton.setEnabled(false);
        removeButton.setFont(DEFAULT_BUTTON_FONT);
        removeButton.setMargin(DEFAULT_BUTTON_MARGIN);
        removeButton.addActionListener(actionEvent -> removeButtonClicked());
        contentPane.add(removeButton, new GridBagConstraints(1, row, 1, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        saveButton.setFont(DEFAULT_BUTTON_FONT);
        saveButton.setMargin(DEFAULT_BUTTON_MARGIN);
        saveButton.addActionListener(actionEvent -> sendEvent(Listener::saveButtonClicked));
        contentPane.add(saveButton, new GridBagConstraints(0, row, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        ++row;

        cancelButton.setFont(DEFAULT_BUTTON_FONT);
        cancelButton.setMargin(DEFAULT_BUTTON_MARGIN);
        cancelButton.addActionListener(actionEvent -> sendEvent(Listener::cancelButtonClicked, new Event(this, null)));
        contentPane.add(cancelButton, new GridBagConstraints(0, row, 2, 1, 1, 0, CENTER, HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        ++row;

        contentPane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));

        final InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION);

        final ActionMap actionMap = contentPane.getActionMap();
        actionMap.put(ESCAPE_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                sendEvent(Listener::cancelButtonClicked, new Event(ItemsDialog.this, null));
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

    public void setItems(Set<Object> items) {
        itemsTableModel.setRowCount(0);
        items.forEach(this::addRow);
    }

    private void addRow(Object item) {
        itemsTableModel.addRow(arrayOf(item.getName(), item.getVolume(), item.getCreatingTime()
                .format(Object.DATE_TIME_FORMATTER), item.getX(), item.getY(), item.getZ()));
    }

    private void addButtonClicked() {
        final ObjectDialog<Object> dialog = new ObjectDialog<>(this, "Add item", ObjectDialog.Kind.OBJECT, null);

        dialog.addListener(new ObjectDialog.Listener<Object>() {

            @Override
            public void okButtonClicked(ObjectDialog.Event<Object> event) {
                addRow(event.object);

                cancelButtonClicked(event);
            }

            @Override
            public void cancelButtonClicked(ObjectDialog.Event<Object> event) {
                event.dialog.setVisible(false);
                event.dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    private void removeButtonClicked() {
        final ListSelectionModel selectionModel = itemsTable.getSelectionModel();
        final int selection = selectionModel.getMinSelectionIndex();

        if (selection != -1) {
            itemsTableModel.removeRow(selection);

            if (selection < itemsTableModel.getRowCount()) {
                selectionModel.setSelectionInterval(selection, selection);
            } else if (itemsTableModel.getRowCount() > 0) {
                final int newSelection = itemsTableModel.getRowCount() - 1;

                selectionModel.setSelectionInterval(newSelection, newSelection);
            }
        }

        removeButton.setEnabled(!selectionModel.isSelectionEmpty());
    }

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        final Set<Object> items = new HashSet<>();

        final Vector dataVector = itemsTableModel.getDataVector();
        for (int i = 0; i < dataVector.size(); ++i) {
            final java.lang.Object rowObject = dataVector.get(i);

            if (rowObject instanceof Vector) {
                final Vector row = (Vector) rowObject;

                try {
                    final Object item = new Object(Objects.toString(row.get(0), ""));

                    callIfNotNull(row.get(1), object -> item.setVolume(Double.parseDouble(object.toString())));
                    throwing().unwrap(Exception.class, () -> callIfNotNull(row.get(2), throwing()
                            .consumer(object -> item.setCreatingTime(parseLocalDateTime(object.toString())))));

                    callIfNotNull(row.get(3), object -> item.setX(Double.parseDouble(object.toString())));
                    callIfNotNull(row.get(4), object -> item.setY(Double.parseDouble(object.toString())));
                    callIfNotNull(row.get(5), object -> item.setZ(Double.parseDouble(object.toString())));

                    items.add(item);
                    log.info("Added item: " + item);
                } catch (Throwable e) {
                    JOptionPane.showMessageDialog(this,  arrayOf("Bad object format", e.getLocalizedMessage()));
                    log.log(Level.SEVERE, "bad object format", e);
                    return;
                }
            } else {
                log.log(Level.WARNING, "table row is not a Vector");
            }
        }

        sendEvent(handler, new Event(this, items));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}

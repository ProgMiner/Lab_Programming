package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.Map;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

@SuppressWarnings("FieldCanBeLocal")
public class CollectionInfoDialog extends JDialog {

    private static final String ESCAPE_ACTION = "escape";
    private static final int MARGIN = 5;

    private final JPanel contentPane = new JPanel(new GridLayout(1, 1));
    private final DefaultTableModel metadataTableModel = new DefaultTableModel() {

        @Override
        public boolean isCellEditable(int i, int i1) {
            return false;
        }
    };
    private final JTable metadataTable = new JTable(metadataTableModel);
    private final JScrollPane listScrollPane = new JScrollPane(metadataTable);

    public CollectionInfoDialog(Window parentWindow, String name, Map<String, String> metadata) {
        this(parentWindow, name, ModalityType.DOCUMENT_MODAL, metadata);
    }

    public CollectionInfoDialog(Window parentWindow, String name, ModalityType modalityType, Map<String, String> metadata) {
        super(parentWindow, name, modalityType);

        metadataTableModel.addColumn("Key");
        metadataTableModel.addColumn("Value");
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataTableModel.addRow(arrayOf(entry.getKey(), entry.getValue()));
        }
        metadataTable.setFont(GuiUtils.DEFAULT_FONT);
        contentPane.add(listScrollPane);
        contentPane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));

        final InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION);

        final ActionMap actionMap = contentPane.getActionMap();
        actionMap.put(ESCAPE_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispatchEvent(new WindowEvent(CollectionInfoDialog.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        setContentPane(contentPane);
        setLocationRelativeTo(parentWindow);
        pack();

        setMinimumSize(getSize());
    }
}
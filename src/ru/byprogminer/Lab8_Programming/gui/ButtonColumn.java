package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

/**
 *  The ButtonColumn class provides a renderer and an editor that looks like a
 *  JButton. The renderer and editor will then be used for a specified column
 *  in the table. The TableModel will contain the String to be displayed on
 *  the button.
 *
 *  The button can be invoked by a mouse click or by pressing the space bar
 *  when the cell has focus. Optionally a mnemonic can be set to invoke the
 *  button. When the button is invoked the provided Action is invoked. The
 *  source of the Action will be the table. The action command will contain
 *  the model row number of the button that was clicked.
 *
 * http://www.camick.com/java/source/ButtonColumn.java
 */
public class ButtonColumn extends AbstractCellEditor implements
        TableCellRenderer, TableCellEditor, ActionListener, MouseListener
{

    private final JTable table;

    private final JButton editorButton;
    private final JButton rendererButton;
    private boolean isButtonColumnEditor = false;
    private Object editorValue;

    private final Set<ActionListener> actionListeners = new HashSet<>();

    /**
     *  Create the ButtonColumn to be used as a renderer and editor. The
     *  renderer and editor will automatically be installed on the TableColumn
     *  of the specified column.
     *
     *  @param table the table containing the button renderer/editor
     */
    public ButtonColumn(JTable table) {
        this.table = table;

        rendererButton = new JButton();
        editorButton = new JButton();
        editorButton.setFocusPainted(false);
        editorButton.addActionListener(this);

        table.addMouseListener(this);
    }

    public void setup(int column) {
        final TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
    }

    public void addActionListener(ActionListener actionListener) {
        actionListeners.add(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionListeners.remove(actionListener);
    }

    public JButton getEditorButton() {
        return editorButton;
    }

    public JButton getRendererButton() {
        return rendererButton;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value == null) {
            editorButton.setText("");
            editorButton.setIcon(null);
        } else if (value instanceof Icon) {
            editorButton.setText("");
            editorButton.setIcon((Icon) value);
        } else {
            editorButton.setText(value.toString());
            editorButton.setIcon(null);
        }

        this.editorValue = value;
        return editorButton;
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        if (isSelected) {
            rendererButton.setForeground(table.getSelectionForeground());
            rendererButton.setBackground(table.getSelectionBackground());
        } else {
            rendererButton.setForeground(table.getForeground());
            rendererButton.setBackground(UIManager.getColor("Button.background"));
        }

        if (value == null) {
            rendererButton.setText("");
            rendererButton.setIcon(null);
        } else if (value instanceof Icon) {
            rendererButton.setText("");
            rendererButton.setIcon((Icon) value);
        } else {
            rendererButton.setText(value.toString());
            rendererButton.setIcon(null);
        }

        return rendererButton;
    }

    /**
     *	The button has been pressed. Stop editing and invoke the custom Action
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        int row = table.convertRowIndexToModel(table.getEditingRow());

        //  Invoke the Action
        ActionEvent event = new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "" + row);
        actionListeners.forEach(actionListener -> actionListener.actionPerformed(event));
        fireEditingCanceled();
    }


    /**
     *  When the mouse is pressed the editor is invoked. If you then then drag
     *  the mouse to another cell before releasing it, the editor is still
     *  active. Make sure editing is stopped when the mouse is released.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (table.isEditing() && table.getCellEditor() == this) {
            isButtonColumnEditor = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isButtonColumnEditor && table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }

        isButtonColumnEditor = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}

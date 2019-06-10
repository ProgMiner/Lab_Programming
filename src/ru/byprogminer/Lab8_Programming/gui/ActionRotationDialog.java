package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Objects;

/**
 * JDialog that provides a flexible linear mechanism for handle actions
 *
 * The ActionRotationDialog objects are working in two threads: Swing thread and thread of user of the dialog.
 *
 * Algorithm is:
 *  1) Thread 1 starts the dialog (by calling the start method)
 *  2) Thread 1 enters into the action handling loop (hasMoreElements/nextElement)
 *  3) User do the action in the started dialog
 *  4) The dialog blocks all elements and sets action by the setAction method
 *  5) Thread 1 catch the action from nextElement action and processes it
 *  6) Thread 1 calls the completeAction method for unblock the dialog
 *  7) Go to the 3 step
 *
 * When user do the terminate action that completed successful Thread 1 calls the stop method.
 *
 * @param <A> type of action object
 */
abstract class ActionRotationDialog<A> extends JDialog implements Enumeration<A> {

    private volatile A action = null;

    private final Object nextActionLock = new Object();

    public ActionRotationDialog(Window parentWindow, String name, ModalityType modalityType) {
        super(parentWindow, name, modalityType);
    }

    public void start() {
        SwingUtilities.invokeLater(() -> setVisible(true));

        while (!isVisible()) {
            Thread.yield();
        }
    }

    public void stop() {
        SwingUtilities.invokeLater(() -> setVisible(false));

        while (isVisible()) {
            Thread.yield();
        }
    }

    public void completeAction() {
        action = null;
        revertState();
    }

    @Override
    public boolean hasMoreElements() {
        return isVisible();
    }

    @Override
    public A nextElement() {
        synchronized (nextActionLock) {
            if (!hasMoreElements()) {
                return null;
            }

            while (action == null) {
                Thread.yield();
            }

            return this.action;
        }
    }

    protected void setAction(A action) {
        if (this.action == null) {
            this.action = Objects.requireNonNull(action);
        }
    }

    protected A getAction() {
        return action;
    }

    protected abstract void revertState();
}

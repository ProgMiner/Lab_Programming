package ru.byprogminer.Lab7_Programming.renderers;

import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.views.*;
import ru.byprogminer.Lab8_Programming.gui.CollectionInfoDialog;
import ru.byprogminer.Lab8_Programming.gui.MainWindow;

import javax.swing.*;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

public class GuiRenderer implements Renderer {

    private final MainWindow mainWindow;

    public GuiRenderer(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    @Override
    public void render(View view) {
        if (view.error != null) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                    view.error + ". Please try again or consult a specialist."));
        }

        if (view instanceof NotLoggedView) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                    "You are not logged in."));
            return;
        }

        if (view instanceof WrongCredentialsView) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                    "Wrong password or user is not exists."));
            return;
        }

        if (view instanceof NotPermittedView) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                    "You don't have permission."));
            return;
        }

        if (view instanceof InfoView) {
            final InfoView infoView = (InfoView) view;

            SwingUtilities.invokeLater(() -> {
                final CollectionInfoDialog dialog = new CollectionInfoDialog(mainWindow, "Collection info", infoView.metadata);
                dialog.setVisible(true);
            });

            return;
        }

        if (view instanceof ShowView) {
            final ShowView showView = (ShowView) view;

            // TODO

            return;
        }

        if (view instanceof AddView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "No one elements added."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "One element added."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        modifyView.affectedRows + " elements added."));
            }

            return;
        }

        if (view instanceof RemoveView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "No one elements removed."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "One element removed."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        modifyView.affectedRows + " elements removed."));
            }

            return;
        }

        if (view instanceof ImportView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "No one elements imported."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "One element imported."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        modifyView.affectedRows + " elements imported."));
            }

            return;
        }

        if (view instanceof ModifyView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "No one elements affected."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "One element affected."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        modifyView.affectedRows + " elements affected."));
            }

            return;
        }

        if (view.error == null) {
            if (view instanceof LoadView) {
                final LoadView loadView = (LoadView) view;

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "Loaded from " + loadView.filename + "."));
                return;
            }

            if (view instanceof SaveView) {
                final SaveView saveView = (SaveView) view;

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "Saved to " + saveView.filename + "."));
                return;
            }
        }

        if (view instanceof ChangePasswordView) {
            final ChangePasswordView changePasswordView = (ChangePasswordView) view;

            if (changePasswordView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow, arrayOf(
                        "Password changed successfully.",
                        "You may need reset current user if you change it's password."
                )));
            }

            return;
        }

        if (view instanceof RegisterView) {
            final RegisterView registerView = (RegisterView) view;

            if (view.error == null && !registerView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "User " + registerView.username +  " wasn't registered."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "User " + registerView.username +  " was registered successfully."));

                if (registerView.password == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                            "On address " + registerView.email +  " has sent a mail with password."));
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow, arrayOf(
                            "An error occurred while mail sending. Password of user " + registerView.username + ":",
                            new JTextField(registerView.password)
                    )));
                }
            }
        }
    }
}

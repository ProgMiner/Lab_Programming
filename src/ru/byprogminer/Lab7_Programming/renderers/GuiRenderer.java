package ru.byprogminer.Lab7_Programming.renderers;

import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.views.*;
import ru.byprogminer.Lab8_Programming.gui.CollectionInfoDialog;
import ru.byprogminer.Lab8_Programming.gui.GuiDisabler;
import ru.byprogminer.Lab8_Programming.gui.MainWindow;
import ru.byprogminer.Lab8_Programming.gui.UsersWindow;

import javax.swing.*;
import java.util.Collections;
import java.util.HashSet;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

public class GuiRenderer implements Renderer {

    private final MainWindow mainWindow;
    private final UsersWindow usersWindow;

    public GuiRenderer(MainWindow mainWindow, UsersWindow usersWindow) {
        this.mainWindow = mainWindow;
        this.usersWindow = usersWindow;
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
                final CollectionInfoDialog dialog = new CollectionInfoDialog(mainWindow, "Collection info");
                dialog.setMetadata(infoView.metadata);
                dialog.setVisible(true);
            });

            return;
        }

        if (view instanceof ShowView) {
            final ShowView showView = (ShowView) view;

            SwingUtilities.invokeLater(() -> {
                final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(mainWindow);
                mainWindow.setElements(Collections.unmodifiableSet(new HashSet<>(showView.elements)));

                disabler.revert();
            });

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

        if (view instanceof ReplaceView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "No one elements changed."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        "One element changed."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                        modifyView.affectedRows + " elements changed."));
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

        // Users views

        if (view instanceof UsersView) {
            final UsersView usersView = (UsersView) view;

            SwingUtilities.invokeLater(() -> usersWindow.setUsers(usersView.users));
        }

        if (view instanceof ChangeUsernameView) {
            final ChangeUsernameView changeUsernameView = (ChangeUsernameView) view;

            if (changeUsernameView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow, arrayOf(
                        "Username changed successfully.",
                        "You need to logout-login if you changed username of the current user."
                )));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow,
                        "Unable to change username."));
            }

            return;
        }

        if (view instanceof ChangePasswordView) {
            final ChangePasswordView changePasswordView = (ChangePasswordView) view;

            if (changePasswordView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow, arrayOf(
                        "Password changed successfully.",
                        "You need to logout-login if you changed password of the current user."
                )));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow,
                        "Unable to change password."));
            }

            return;
        }

        if (view instanceof RegisterView) {
            final RegisterView registerView = (RegisterView) view;

            if (view.error == null && !registerView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow,
                        "User " + registerView.username +  " wasn't registered."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow,
                        "User " + registerView.username +  " was registered successfully."));

                if (registerView.password == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow,
                            "On address " + registerView.email +  " has sent a mail with password."));
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow, arrayOf(
                            "An error occurred while mail sending. Password of user " + registerView.username + ":",
                            new JTextField(registerView.password)
                    )));
                }
            }

            return;
        }

        if (view instanceof RemoveUserView) {
            final RemoveUserView removeUserView = (RemoveUserView) view;

            if (removeUserView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow, arrayOf("User removed successfully.")));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(usersWindow, "Unable to remove user."));
            }
        }
    }
}

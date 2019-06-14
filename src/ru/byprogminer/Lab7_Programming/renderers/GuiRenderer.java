package ru.byprogminer.Lab7_Programming.renderers;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.views.*;
import ru.byprogminer.Lab8_Programming.gui.*;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static ru.byprogminer.Lab5_Programming.LabUtils.arrayOf;

public class GuiRenderer extends AbstractRenderer {

    private final MainWindow mainWindow;
    private final UsersWindow usersWindow;
    private Window currentDialogWindow;

    public GuiRenderer(MainWindow mainWindow, UsersWindow usersWindow) {
        currentDialogWindow = this.mainWindow = Objects.requireNonNull(mainWindow);
        this.usersWindow = Objects.requireNonNull(usersWindow);
    }

    public void setCurrentDialogWindow(Window currentDialogWindow) {
        this.currentDialogWindow = currentDialogWindow;
    }

    public static LivingObject requestElement(Window parentWindow, String title, LivingObject selectedElement) {
        final AtomicReference<LivingObject> elementReference = new AtomicReference<>();

        final AtomicReference<ObjectDialog<LivingObject>> dialogReference = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            final ObjectDialog<LivingObject> dialog = new ObjectDialog<>(parentWindow, title, ObjectDialog.Kind.LIVING_OBJECT, selectedElement);
            dialog.addListener(new ObjectDialog.Listener<LivingObject>() {

                @Override
                public void okButtonClicked(ObjectDialog.Event<LivingObject> event) {
                    elementReference.set(event.object);
                    cancelButtonClicked(event);
                }

                @Override
                public void cancelButtonClicked(ObjectDialog.Event<LivingObject> event) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });

            dialog.setVisible(true);
            dialogReference.set(dialog);
        });

        ObjectDialog<LivingObject> dialog;
        while ((dialog = dialogReference.get()) == null) {
            Thread.yield();
        }

        while (dialog.isVisible()) {
            Thread.yield();
        }

        return elementReference.get();
    }

    @Override
    protected void doRender(View view) {
        if (view.error != null) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                    view.error + ". Please try again or consult a specialist."));
        }

        if (view instanceof NotLoggedView) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                    "You are not logged in."));
            return;
        }

        if (view instanceof WrongCredentialsView) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                    "Wrong password or user is not exists."));
            return;
        }

        if (view instanceof NotPermittedView) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                    "You don't have permission."));
            return;
        }

        if (view instanceof InfoView) {
            final InfoView infoView = (InfoView) view;

            SwingUtilities.invokeLater(() -> {
                final CollectionInfoDialog dialog = new CollectionInfoDialog(currentDialogWindow, "Collection info");
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
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "No one elements added."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "One element added."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        modifyView.affectedRows + " elements added."));
            }

            return;
        }

        if (view instanceof RemoveView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "No one elements removed."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "One element removed."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        modifyView.affectedRows + " elements removed."));
            }

            return;
        }

        if (view instanceof ReplaceView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "No one elements changed."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "One element changed."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        modifyView.affectedRows + " elements changed."));
            }

            return;
        }

        if (view instanceof ImportView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "No one elements imported."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "One element imported."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        modifyView.affectedRows + " elements imported."));
            }

            return;
        }

        if (view instanceof ModifyView) {
            final ModifyView modifyView = (ModifyView) view;

            if (view.error == null && modifyView.affectedRows == 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "No one elements affected."));
            } else if (modifyView.affectedRows == 1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "One element affected."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        modifyView.affectedRows + " elements affected."));
            }

            return;
        }

        if (view.error == null) {
            if (view instanceof LoadView) {
                final LoadView loadView = (LoadView) view;

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "Loaded from " + loadView.filename + "."));
                return;
            }

            if (view instanceof SaveView) {
                final SaveView saveView = (SaveView) view;

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "Saved to " + saveView.filename + "."));
                return;
            }
        }

        // Users views

        if (view instanceof UsersView) {
            final UsersView usersView = (UsersView) view;

            SwingUtilities.invokeLater(() -> {
                final GuiDisabler<UsersWindow> disabler = GuiDisabler.disable(usersWindow);

                usersWindow.setUsers(usersView.users);
                disabler.revert();
            });
        }

        if (view instanceof ChangeUsernameView) {
            final ChangeUsernameView changeUsernameView = (ChangeUsernameView) view;

            if (changeUsernameView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow, arrayOf(
                        "Username changed successfully.",
                        "You need to logout-login if you changed username of the current user."
                )));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "Unable to change username."));
            }

            return;
        }

        if (view instanceof ChangePasswordView) {
            final ChangePasswordView changePasswordView = (ChangePasswordView) view;

            if (changePasswordView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow, arrayOf(
                        "Password changed successfully.",
                        "You need to logout-login if you changed password of the current user."
                )));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "Unable to change password."));
            }

            return;
        }

        if (view instanceof RegisterView) {
            final RegisterView registerView = (RegisterView) view;

            if (view.error == null && !registerView.ok) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "User " + registerView.username +  " wasn't registered."));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                        "User " + registerView.username +  " was registered successfully."));

                if (registerView.password == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow,
                            "On address " + registerView.email +  " has sent a mail with password."));
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow, arrayOf(
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
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow, arrayOf("User removed successfully.")));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(currentDialogWindow, "Unable to remove user."));
            }
        }
    }
}

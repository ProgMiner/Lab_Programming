package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.csv.CsvReader;
import ru.byprogminer.Lab5_Programming.csv.CsvReaderWithHeader;
import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.*;
import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.controllers.UsersController;
import ru.byprogminer.Lab7_Programming.csv.CsvLivingObjectReader;
import ru.byprogminer.Lab7_Programming.renderers.GuiRenderer;
import ru.byprogminer.Lab7_Programming.views.CheckPasswordView;
import ru.byprogminer.Lab8_Programming.gui.*;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class GuiFrontend implements Frontend, MainWindow.Listener, UsersWindow.Listener {

    private final MainWindow mainWindow;
    private final UsersWindow usersWindow;
    private final CollectionController collectionController;
    private final UsersController usersController;
    private final Renderer renderer;

    private final CurrentUser currentUser = new CurrentUser();
    private volatile String previousUser = "";

    public GuiFrontend(MainWindow mainWindow, CollectionController collectionController, UsersController usersController) {
        this.mainWindow = mainWindow;
        this.usersWindow = new UsersWindow("Users");
        this.collectionController = collectionController;
        this.usersController = usersController;
        this.renderer = new GuiRenderer(mainWindow, usersWindow);

        mainWindow.addListener(this);
        usersWindow.addListener(this);
    }

    private void refreshElements() {
        renderer.render(collectionController.show());
    }

    private void refreshUsers() {
        renderer.render(usersController.get());
    }

    @Override
    public void exec() throws IllegalStateException {
        SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));
        refreshElements();

        while (!mainWindow.isVisible()) {
            Thread.yield();
        }

        while (mainWindow.isVisible()) {
            Thread.yield();
        }

        mainWindow.dispose();
    }

    @Override
    public void stop() {
        SwingUtilities.invokeLater(() -> mainWindow.setVisible(false));
    }

    @Override
    public void mainFileLoadMenuItemClicked(MainWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final JFileChooser fileChooser = new JFileChooser("Load from file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (fileChooser.showOpenDialog(event.window) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(mainWindow);
            new Thread(() -> {
                renderer.render(collectionController.load(fileChooser.getSelectedFile().getAbsolutePath(), currentUser.get()));

                refreshElements();
                disabler.revert();
            }).start();
        });
    }

    @Override
    public void mainFileSaveMenuItemClicked(MainWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final JFileChooser fileChooser = new JFileChooser("Save to file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (fileChooser.showSaveDialog(event.window) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(mainWindow);
            new Thread(() -> {
                renderer.render(collectionController.save(fileChooser.getSelectedFile().getAbsolutePath(), currentUser.get()));

                disabler.revert();
                refreshElements();
            }).start();
        });
    }

    @Override
    public void mainFileImportMenuItemClicked(MainWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final JFileChooser fileChooser = new JFileChooser("Import from file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (fileChooser.showOpenDialog(event.window) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final Scanner scanner;

            try {
                scanner = new Scanner(fileChooser.getSelectedFile());
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("file not found", e);
            }

            final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(mainWindow);
            new Thread(() -> {
                renderer.render(collectionController.importObjects(new CsvLivingObjectReader(new CsvReaderWithHeader(
                        new CsvReader(scanner))).getObjects(), currentUser.get()));

                disabler.revert();
                refreshElements();
            });
        });
    }

    @Override
    public void mainFileUsersMenuItemClicked(MainWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            usersWindow.rebuild();

            usersWindow.setVisible(true);
        });

        refreshUsers();
    }

    @Override
    public void mainFileExitMenuItemClicked(MainWindow.Event event) {
        stop();
    }

    @Override
    public void mainLanguageMenuItemClicked(MainWindow.Event event) {
        // TODO
    }

    @Override
    public void mainAboutMenuItemClicked(MainWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final AboutDialog dialog = new AboutDialog(event.window, "About");

            dialog.setTitle(ServerMain.APP_NAME);
            dialog.setContent("Oaoaoaoaoa");
            dialog.setVisible(true);
        });
    }

    @Override
    public void elementChanged(MainWindow.Event event) {
        final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(mainWindow);

        new Thread(() -> {
            renderer.render(collectionController
                    .replaceElement(event.selectedElement, event.newElement, currentUser.get()));

            refreshElements();
            disabler.revert();
        }).start();
    }

    @Override
    public void userNotLoggedLoginButtonClicked(MainWindow.Event event) {
        final UserDialog loginDialog = new UserDialog(event.window, "Login", UserDialog.Kind.LOGIN, previousUser);

        loginDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        loginDialog.addListener(new UserDialog.Listener() {

            @Override
            public void okButtonClicked(UserDialog.Event userDialogEvent) {
                final GuiDisabler<UserDialog> disabler = GuiDisabler.disable(userDialogEvent.dialog);

                new Thread(() -> {
                    final Credentials credentials = new Credentials(userDialogEvent.username, new String(userDialogEvent.password));

                    final View view = usersController.checkPassword(credentials);
                    if (!(view instanceof CheckPasswordView) || !((CheckPasswordView) view).ok) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(event.window,
                                "Wrong password or user is not exists"));

                        disabler.revert();
                        return;
                    }

                    currentUser.set(credentials);
                    previousUser = userDialogEvent.username;
                    SwingUtilities.invokeLater(() -> {
                        event.window.setCurrentUser(userDialogEvent.username);
                        userDialogEvent.dialog.setVisible(false);
                    });
                }).start();
            }

            @Override
            public void cancelButtonClicked(UserDialog.Event event) {
                loginDialog.setVisible(false);
                loginDialog.dispose();
            }
        });

        loginDialog.setVisible(true);
    }

    @Override
    public void userLoggedInCurrentUserLogoutButtonClicked(MainWindow.Event event) {
        event.window.setCurrentUser(null);
        currentUser.reset();
    }

    @Override
    public void infoButtonClicked(MainWindow.Event event) {
        new Thread(() -> renderer.render(collectionController.info())).start();
    }

    @Override
    public void addButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = requestElement(event.window, "Add element", event.selectedElement);

            if (element != null) {
                renderer.render(collectionController.add(element, currentUser.get()));
                refreshElements();
            }
        }).start();
    }

    @Override
    public void removeButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = requestElement(event.window, "Remove element", event.selectedElement);

            if (element != null) {
                renderer.render(collectionController.remove(element, currentUser.get()));
                refreshElements();
            }
        }).start();
    }

    @Override
    public void removeLowerButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = requestElement(event.window, "Remove lower elements", event.selectedElement);

            if (element != null) {
                renderer.render(collectionController.removeLower(element, currentUser.get()));
                refreshElements();
            }
        }).start();
    }

    @Override
    public void removeGreaterButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = requestElement(event.window, "Remove greater elements", event.selectedElement);

            if (element != null) {
                renderer.render(collectionController.removeGreater(element, currentUser.get()));
                refreshElements();
            }
        }).start();
    }

    private LivingObject requestElement(Window parentWindow, String title, LivingObject selectedElement) {
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
    public void changeUsernameButtonClicked(UsersWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final UserDialog dialog = new UserDialog(event.window, "Change username of " + event.selectedUser,
                    UserDialog.Kind.USERNAME, event.selectedUser);

            dialog.addListener(new UserDialog.Listener() {

                @Override
                public void okButtonClicked(UserDialog.Event dialogEvent) {
                    GuiDisabler.disable(dialogEvent.dialog);

                    new Thread(() -> {
                        renderer.render(usersController.changeUsername(event.selectedUser,
                                dialogEvent.username, currentUser.get()));

                        refreshUsers();
                        SwingUtilities.invokeLater(() -> cancelButtonClicked(dialogEvent));
                    }).start();
                }

                @Override
                public void cancelButtonClicked(UserDialog.Event event) {
                    event.dialog.setVisible(false);
                    event.dialog.dispose();
                }
            });

            dialog.setVisible(true);
        });
    }

    @Override
    public void changePasswordButtonClicked(UsersWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final UserDialog dialog = new UserDialog(event.window, "Change password of " + event.selectedUser,
                    UserDialog.Kind.PASSWORD, event.selectedUser);

            dialog.addListener(new UserDialog.Listener() {

                @Override
                public void okButtonClicked(UserDialog.Event dialogEvent) {
                    GuiDisabler.disable(dialogEvent.dialog);

                    new Thread(() -> {
                        renderer.render(usersController.changePassword(event.selectedUser,
                                new String(dialogEvent.password), currentUser.get()));

                        SwingUtilities.invokeLater(() -> cancelButtonClicked(dialogEvent));
                    }).start();
                }

                @Override
                public void cancelButtonClicked(UserDialog.Event event) {
                    event.dialog.setVisible(false);
                    event.dialog.dispose();
                }
            });

            dialog.setVisible(true);
        });
    }

    @Override
    public void permissionsButtonClicked(UsersWindow.Event event) {
        new Thread(() ->
                SwingUtilities.invokeLater(() -> {
                    final PermissionsDialog dialog = new PermissionsDialog(event.window,
                            "Permissions of " + event.selectedUser);

                    dialog.addListener(new PermissionsDialog.Listener() {

                        {
                            refreshPermissions(dialog);
                        }

                        @Override
                        public void addButtonClicked(PermissionsDialog.Event permissionsEvent) {
                            new Thread(() -> {
                                final String permission = JOptionPane.showInputDialog(permissionsEvent.dialog, "Permission name: ");

                                if (permission != null) {
                                    renderer.render(usersController.givePermission(event.selectedUser,
                                            permission, currentUser.get()));

                                    refreshPermissions(permissionsEvent.dialog);
                                }
                            }).start();
                        }

                        @Override
                        public void removeButtonClicked(PermissionsDialog.Event permissionsEvent) {
                            final GuiDisabler<PermissionsDialog> disabler = GuiDisabler.disable(permissionsEvent.dialog);

                            new Thread(() -> {
                                renderer.render(usersController.takePermission(event.selectedUser,
                                        permissionsEvent.permission, currentUser.get()));

                                refreshPermissions(permissionsEvent.dialog);
                                SwingUtilities.invokeLater(disabler::revert);
                            }).start();
                        }

                        @Override
                        public void okButtonClicked(PermissionsDialog.Event event) {
                            event.dialog.setVisible(false);
                            event.dialog.dispose();
                        }

                        private void refreshPermissions(PermissionsDialog dialog) {
                            final Set<String> permissions = usersController.getPermissions(event.selectedUser).permissions;

                            SwingUtilities.invokeLater(() -> {
                                final GuiDisabler<PermissionsDialog> disabler = GuiDisabler.disable(dialog);

                                dialog.setPermissions(permissions);
                                disabler.revert();
                            });
                        }
                    });

                    dialog.setVisible(true);
                })
        ).start();
    }

    @Override
    public void registerButtonClicked(UsersWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final UserDialog dialog = new UserDialog(event.window, "Register user", UserDialog.Kind.REGISTER, "");

            dialog.addListener(new UserDialog.Listener() {

                @Override
                public void okButtonClicked(UserDialog.Event dialogEvent) {
                    GuiDisabler.disable(dialogEvent.dialog);

                    new Thread(() -> {
                        renderer.render(usersController.register(dialogEvent.username, dialogEvent.email, currentUser.get()));
                        refreshUsers();

                        cancelButtonClicked(dialogEvent);
                    }).start();
                }

                @Override
                public void cancelButtonClicked(UserDialog.Event event) {
                    event.dialog.setVisible(false);
                    event.dialog.dispose();
                }
            });

            dialog.setVisible(true);
        });
    }

    @Override
    public void removeButtonClicked(UsersWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            if (JOptionPane.showConfirmDialog(usersWindow, "Are you sure? This action cannot be undone!") == JOptionPane.YES_OPTION) {
                new Thread(() -> {
                    renderer.render(usersController.removeUser(event.selectedUser, currentUser.get()));
                    refreshUsers();
                }).start();
            }
        });
    }

    @Override
    public void okButtonClicked(UsersWindow.Event event) {
        event.window.setVisible(false);
    }
}

package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.*;
import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.controllers.UsersController;
import ru.byprogminer.Lab7_Programming.renderers.GuiRenderer;
import ru.byprogminer.Lab7_Programming.views.CheckPasswordView;
import ru.byprogminer.Lab8_Programming.gui.MainWindow;
import ru.byprogminer.Lab8_Programming.gui.ObjectDialog;
import ru.byprogminer.Lab8_Programming.gui.UserDialog;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public class GuiFrontend implements Frontend, MainWindow.Listener {

    private final MainWindow mainWindow;
    private final CollectionController collectionController;
    private final UsersController usersController;
    private final Renderer renderer;

    private final CurrentUser currentUser = new CurrentUser();
    private volatile String previousUser = "";

    public GuiFrontend(MainWindow mainWindow, CollectionController collectionController, UsersController usersController) {
        this.mainWindow = mainWindow;
        this.collectionController = collectionController;
        this.usersController = usersController;
        this.renderer = new GuiRenderer(mainWindow);

        mainWindow.addListener(this);
    }

    @Override
    public void exec() throws IllegalStateException {
        SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));

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
        // TODO
    }

    @Override
    public void mainFileSaveMenuItemClicked(MainWindow.Event event) {
        // TODO
    }

    @Override
    public void mainFileImportMenuItemClicked(MainWindow.Event event) {
        // TODO
    }

    @Override
    public void mainFileUsersMenuItemClicked(MainWindow.Event event) {
        // TODO
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
        // TODO
    }

    @Override
    public void userNotLoggedLoginButtonClicked(MainWindow.Event event) {
        final UserDialog loginDialog = new UserDialog(event.window, "Login", UserDialog.Kind.LOGIN, previousUser);

        loginDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        loginDialog.addListener(new UserDialog.Listener() {

            @Override
            public void okButtonClicked(UserDialog.Event userDialogEvent) {
                new Thread(() -> {
                    final Credentials credentials = new Credentials(userDialogEvent.username, new String(userDialogEvent.password));

                    final View view = usersController.checkPassword(credentials);
                    if (!(view instanceof CheckPasswordView) || !((CheckPasswordView) view).ok) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(event.window,
                                "Wrong password or user is not exists"));

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
            final LivingObject element = requestElement(event.window, "Add element");

            if (element != null) {
                renderer.render(collectionController.add(element, currentUser.get()));
            }
        }).start();
    }

    @Override
    public void removeButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = requestElement(event.window, "Remove element");

            if (element != null) {
                renderer.render(collectionController.remove(element, currentUser.get()));
            }
        }).start();
    }

    @Override
    public void removeLowerButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = requestElement(event.window, "Remove lower elements");

            if (element != null) {
                renderer.render(collectionController.removeLower(element, currentUser.get()));
            }
        }).start();
    }

    @Override
    public void removeGreaterButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = requestElement(event.window, "Remove greater elements");

            if (element != null) {
                renderer.render(collectionController.removeGreater(element, currentUser.get()));
            }
        }).start();
    }

    private LivingObject requestElement(Window parentWindow, String title) {
        final AtomicReference<LivingObject> elementReference = new AtomicReference<>();

        final AtomicReference<ObjectDialog<LivingObject>> dialogReference = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            final ObjectDialog<LivingObject> dialog = new ObjectDialog<>(parentWindow, title, ObjectDialog.Kind.LIVING_OBJECT, null);
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
}

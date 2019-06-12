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
import ru.byprogminer.Lab8_Programming.gui.MainWindow;
import ru.byprogminer.Lab8_Programming.gui.ObjectDialog;
import ru.byprogminer.Lab8_Programming.gui.UserDialog;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.util.Scanner;
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

    private void refreshElements() {
        renderer.render(collectionController.show());
    }

    @Override
    public void exec() throws IllegalStateException {
        SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));

        while (!mainWindow.isVisible()) {
            Thread.yield();
        }

        refreshElements();
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
        new Thread(() -> {
            final JFileChooser fileChooser = new JFileChooser("Load from file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (fileChooser.showOpenDialog(event.window) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            renderer.render(collectionController.load(fileChooser.getSelectedFile().getAbsolutePath(), currentUser.get()));
            refreshElements();
        }).start();
    }

    @Override
    public void mainFileSaveMenuItemClicked(MainWindow.Event event) {
        new Thread(() -> {
            final JFileChooser fileChooser = new JFileChooser("Save to file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (fileChooser.showSaveDialog(event.window) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            renderer.render(collectionController.save(fileChooser.getSelectedFile().getAbsolutePath(), currentUser.get()));
            refreshElements();
        }).start();
    }

    @Override
    public void mainFileImportMenuItemClicked(MainWindow.Event event) {
        new Thread(() -> {
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

            renderer.render(collectionController.importObjects(new CsvLivingObjectReader(new CsvReaderWithHeader(
                    new CsvReader(scanner))).getObjects(), currentUser.get()));
            refreshElements();
        }).start();
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
    public void elementChanged(MainWindow.Event event) {
        new Thread(() -> {
            renderer.render(collectionController
                    .replaceElement(event.selectedElement, event.newElement, currentUser.get()));

            refreshElements();
        }).start();
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
}

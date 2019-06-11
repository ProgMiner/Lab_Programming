package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab7_Programming.Credentials;
import ru.byprogminer.Lab7_Programming.CurrentUser;
import ru.byprogminer.Lab7_Programming.Frontend;
import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.renderers.GuiRenderer;
import ru.byprogminer.Lab8_Programming.gui.MainWindow;
import ru.byprogminer.Lab8_Programming.gui.UserDialog;

import javax.swing.*;

public class GuiFrontend implements Frontend, MainWindow.Listener {

    private final MainWindow mainWindow;
    private final Renderer renderer;

    private final CurrentUser currentUser = new CurrentUser();
    private volatile String previousUser = "";

    public GuiFrontend(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
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
                    currentUser.set(new Credentials(userDialogEvent.username, new String(userDialogEvent.password)));

                    // TODO check password

                    SwingUtilities.invokeLater(() -> {
                        event.window.setCurrentUser(userDialogEvent.username);
                        userDialogEvent.dialog.setVisible(false);
                    });
                }).start();
            }

            @Override
            public void cancelButtonClicked(UserDialog.Event event) {
                loginDialog.setVisible(false);
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
        // TODO
    }

    @Override
    public void addButtonClicked(MainWindow.Event event) {
        // TODO
    }

    @Override
    public void removeButtonClicked(MainWindow.Event event) {
        // TODO
    }

    @Override
    public void removeLowerButtonClicked(MainWindow.Event event) {
        // TODO
    }

    @Override
    public void removeGreaterButtonClicked(MainWindow.Event event) {
        // TODO
    }
}

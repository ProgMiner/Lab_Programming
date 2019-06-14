package ru.byprogminer.Lab8_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab5_Programming.csv.CsvReader;
import ru.byprogminer.Lab5_Programming.csv.CsvReaderWithHeader;
import ru.byprogminer.Lab6_Programming.Packet.Request;
import ru.byprogminer.Lab6_Programming.Packet.Response;
import ru.byprogminer.Lab6_Programming.udp.PacketUtils;
import ru.byprogminer.Lab6_Programming.udp.SocketUdpSocket;
import ru.byprogminer.Lab6_Programming.udp.UdpSocket;
import ru.byprogminer.Lab7_Programming.Credentials;
import ru.byprogminer.Lab7_Programming.CurrentUser;
import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.ServerMain;
import ru.byprogminer.Lab7_Programming.csv.CsvLivingObjectReader;
import ru.byprogminer.Lab7_Programming.logging.Loggers;
import ru.byprogminer.Lab7_Programming.renderers.GuiRenderer;
import ru.byprogminer.Lab7_Programming.views.CheckPasswordView;
import ru.byprogminer.Lab7_Programming.views.PermissionsView;
import ru.byprogminer.Lab8_Programming.gui.*;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.byprogminer.Lab5_Programming.LabUtils.validatePort;
import static ru.byprogminer.Lab7_Programming.frontends.RemoteFrontend.CLIENT_TIMEOUT;
import static ru.byprogminer.Lab7_Programming.frontends.RemoteFrontend.SO_TIMEOUT;

public class ClientMain implements MainWindow.Listener, UsersWindow.Listener {

    private static class Status {

        public static final int UNKNOWN_ERROR = -1;

        private static final int LOGGING_CONFIG_ERROR = 1;
        private static final int PORT_NOT_PROVIDED = 2;
        private static final int BAD_PORT_PROVIDED = 3;
    }

    private static final String APP_NAME = "Lab8_Programming";

    private static final String USAGE = "" +
            "Usage: java -jar lab7_client.jar [--cui] <port> [address]\n" +
            "  - --cui\n" +
            "    Option for enable the CUI\n" +
            "  - port\n" +
            "    Port number of remote server\n" +
            "  - hostname\n" +
            "    Not required hostname of remote server";

    private static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 3565;

    private static final int CONNECT_DELAY = 3000;
    public static final int SERVER_TIMEOUT = CLIENT_TIMEOUT / 10;

    private static final Logger log = Loggers.getClassLogger(ClientMain.class);

    private static GuiRenderer renderer;

    private static final CurrentUser currentUser = new CurrentUser();
    private static SocketAddress address = null;
    private static UdpSocket<?> socket = null;

    private static final MainWindow mainWindow;
    private static final UsersWindow usersWindow;

    private String previousUser = null;

    static {
        mainWindow = new MainWindow(APP_NAME);
        usersWindow = new UsersWindow("Users");
        renderer = new GuiRenderer(mainWindow, usersWindow);

        mainWindow.setLanguages(Translations.AVAILABLE_LANGUAGES.stream()
                .map(locale -> locale.getDisplayName(locale)).collect(Collectors.toList()));

        final ClientMain clientMain = new ClientMain();
        mainWindow.addListener(clientMain);
        usersWindow.addListener(clientMain);
    }

    public static void main(String[] args) {
        try {
            Loggers.configureLoggers();
        } catch (Throwable e) {
            System.err.println("Unable to start logging!");
            System.exit(Status.LOGGING_CONFIG_ERROR);
        }

        try {
            log.info("Start");
            final int result = throwingMain(parseArguments(args));
            log.info("Finish");

            System.exit(result);
        } catch (Throwable e) {
            System.err.println("An unknown error occurred. See logs for details or try again.");
            log.log(Level.SEVERE, "Unknown error", e);
            System.exit(Status.UNKNOWN_ERROR);
        }
    }

    private static Map<String, Object> parseArguments(String[] args) {
        final Map<String, Object> ret = new HashMap<>();

        int pointer = 0;
        if (args.length > 0 && "--cui".equals(args[pointer])) {
            ret.put("gui", false);
            ++pointer;
        }

        if (args.length < pointer + 1) {
            log.log(Level.SEVERE, "port isn't provided");
            System.err.println("Port isn't provided.");
            System.err.println(USAGE);

            System.exit(Status.PORT_NOT_PROVIDED);
            return ret;
        }

        try {
            final int port = validatePort(args[pointer]);
            if (port == 0) {
                throw new IllegalArgumentException("port \"0\" is not allowed");
            }

            ret.put("port", port);
            ++pointer;
        } catch (Throwable e) {
            log.log(Level.SEVERE, "bad port provided: " + args[0], e);
            System.err.printf("Bad port provided: %s.\n", args[0]);
            System.err.println(USAGE);

            System.exit(Status.BAD_PORT_PROVIDED);
            return ret;
        }

        if (args.length < pointer + 1) {
            return ret;
        }

        ret.put("hostname", args[pointer]);
        return ret;
    }

    private static int throwingMain(Map<String, Object> args) {
        final AtomicBoolean exec = new AtomicBoolean();

        final AtomicReference<IpAddressDialog> connectDialogReference = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            final IpAddressDialog connectDialog = new IpAddressDialog(mainWindow, APP_NAME,
                    IpAddressDialog.Kind.CONNECT, (String) args.getOrDefault("address", DEFAULT_SERVER_ADDRESS),
                    (Integer) args.getOrDefault("port", DEFAULT_SERVER_PORT));

            connectDialog.addListener(new IpAddressDialog.Listener() {

                @Override
                public void okButtonClicked(IpAddressDialog.Event event) {
                    final GuiDisabler<IpAddressDialog> dialogDisabler = GuiDisabler.disable(event.dialog);

                    new Thread(() -> {
                        try {
                            address = new InetSocketAddress(event.address, event.port);

                            try {
                                if (!connect(false)) {
                                    throw new RuntimeException();
                                }
                            } catch (Throwable e) {
                                log.log(Level.SEVERE, "unable to connect to the server", e);
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                                        "Unable to connect to server"));

                                return;
                            }

                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(mainWindow, "Successfully connected");
                                cancelButtonClicked(event);
                            });
                        } finally {
                            SwingUtilities.invokeLater(dialogDisabler::revert);
                        }
                    }).start();
                }

                @Override
                public void cancelButtonClicked(IpAddressDialog.Event event) {
                    event.dialog.setVisible(false);
                    exec.set(true);

                    event.dialog.dispose();
                }
            });

            connectDialog.setVisible(true);
            connectDialogReference.set(connectDialog);
        });

        IpAddressDialog connectDialog;
        while ((connectDialog = connectDialogReference.get()) == null) {
            Thread.yield();
        }

        while (connectDialog.isVisible()) {
            Thread.yield();
        }

        if (exec.get()) {
            exec();
        }

        return 0;
    }

    private static void exec() {
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

    private static boolean connect(boolean retry) throws IOException {
        final UdpSocket<?> oldSocket = socket;
        if (oldSocket != null) {
            oldSocket.close();
        }

        final DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(SO_TIMEOUT);

        socket = new SocketUdpSocket<>(datagramSocket, PacketUtils.OPTIMAL_PACKET_SIZE);

        do {
            try {
                socket.connect(address, CONNECT_DELAY);
            } catch (SocketTimeoutException e) {
                if (retry) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow, "Server is unavailable. Retry"));
                } else {
                    return false;
                }
            }
        } while (!socket.isConnected() && retry);

        return true;
    }

    private static void send(Request request) {
        if (request == null) {
            return;
        }

        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            log.info("illegal socket state");
            reconnect();
        }

        try {
            socket.send(request, SERVER_TIMEOUT);
            while (!socket.isClosed()) {
                try {
                    final Response response = socket.receive(Response.class, SERVER_TIMEOUT);

                    if (response instanceof Response.Done) {
                        final Response.Done doneResponse = (Response.Done) response;

                        renderer.render(doneResponse.view);
                        break;
                    }
                } catch (InterruptedException e) {
                    log.log(Level.INFO, "exception thrown", e);
                }
            }
        } catch (SocketTimeoutException e) {
            log.log(Level.INFO, "server is unavailable", e);

            reconnect();
        } catch (InterruptedException | IOException e) {
            log.log(Level.INFO, "exception thrown", e);
        }
    }

    private static void reconnect() {
        try {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow,
                    "It looks like the server is unavailable. Reconnect"));

            connect(true);

            log.info("reconnected");
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainWindow, "Reconnected"));
        } catch (IOException ex) {
            log.log(Level.INFO, "exception thrown", ex);
        }
    }

    private static void refreshElements() {
        renderer.setCurrentDialogWindow(mainWindow);
        send(new Request.ShowAll(currentUser.get()));
    }

    private static void refreshUsers() {
        renderer.setCurrentDialogWindow(usersWindow);
        send(new Request.GetUsers(currentUser.get()));
    }

    @Override
    public void mainFileLoadMenuItemClicked(MainWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final String filename = JOptionPane.showInputDialog(event.window, "Input filename to load: ");

            final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(event.window);
            new Thread(() -> {
                renderer.setCurrentDialogWindow(event.window);
                send(new Request.Load(filename, currentUser.get()));

                refreshElements();
                SwingUtilities.invokeLater(disabler::revert);
            }).start();
        });
    }

    @Override
    public void mainFileSaveMenuItemClicked(MainWindow.Event event) {
        SwingUtilities.invokeLater(() -> {
            final String filename = JOptionPane.showInputDialog(event.window, "Input filename to save: ");

            final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(event.window);
            new Thread(() -> {
                renderer.setCurrentDialogWindow(event.window);
                send(new Request.Save(filename, currentUser.get()));

                refreshElements();
                SwingUtilities.invokeLater(disabler::revert);
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

            final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(event.window);
            new Thread(() -> {
                renderer.setCurrentDialogWindow(event.window);
                send(new Request.Import(new CsvLivingObjectReader(new CsvReaderWithHeader(
                        new CsvReader(scanner))).getObjects(), currentUser.get()));

                refreshElements();
                SwingUtilities.invokeLater(disabler::revert);
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
        SwingUtilities.invokeLater(() -> {
            event.window.setVisible(false);
            usersWindow.setVisible(false);
        });
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
        final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(event.window);

        new Thread(() -> {
            renderer.setCurrentDialogWindow(event.window);
            send(new Request.ReplaceElement(event.selectedElement, event.newElement, currentUser.get()));

            refreshElements();
            SwingUtilities.invokeLater(disabler::revert);
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
                    final Renderer.Listener rendererListener = new Renderer.Listener() {

                        @Override
                        public void viewRendering(Renderer.Event rendererEvent) {
                            if (rendererEvent.view.error != null) {
                                return;
                            }

                            if (!(rendererEvent.view instanceof CheckPasswordView)) {
                                return;
                            }

                            final CheckPasswordView checkPasswordView = (CheckPasswordView) rendererEvent.view;
                            if (!checkPasswordView.ok) {
                                return;
                            }

                            currentUser.set(credentials);
                            previousUser = userDialogEvent.username;

                            SwingUtilities.invokeLater(() -> {
                                event.window.setCurrentUser(userDialogEvent.username);
                                userDialogEvent.dialog.setVisible(false);
                            });
                        }

                        @Override
                        public void viewRendered(Renderer.Event e) {
                            disabler.revert();
                        }
                    };

                    renderer.addListener(rendererListener);
                    renderer.setCurrentDialogWindow(event.window);
                    send(new Request.CheckPassword(credentials));

                    renderer.removeListener(rendererListener);
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
        new Thread(() -> {
            renderer.setCurrentDialogWindow(event.window);
            send(new Request.Info(currentUser.get()));
        }).start();
    }

    @Override
    public void addButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = GuiRenderer.requestElement(event.window, "Add element", event.selectedElement);

            if (element != null) {
                renderer.setCurrentDialogWindow(event.window);
                send(new Request.Add(element, currentUser.get()));
                refreshElements();
            }
        }).start();
    }

    @Override
    public void removeButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = GuiRenderer.requestElement(event.window, "Remove element", event.selectedElement);

            if (element != null) {
                renderer.setCurrentDialogWindow(event.window);
                send(new Request.Remove(element, currentUser.get()));
                refreshElements();
            }
        }).start();
    }

    @Override
    public void removeLowerButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = GuiRenderer.requestElement(event.window, "Remove lower elements", event.selectedElement);

            if (element != null) {
                renderer.setCurrentDialogWindow(event.window);
                send(new Request.RemoveLower(element, currentUser.get()));
                refreshElements();
            }
        }).start();
    }

    @Override
    public void removeGreaterButtonClicked(MainWindow.Event event) {
        new Thread(() -> {
            final LivingObject element = GuiRenderer.requestElement(event.window, "Remove greater elements", event.selectedElement);

            if (element != null) {
                renderer.setCurrentDialogWindow(event.window);
                send(new Request.RemoveGreater(element, currentUser.get()));
                refreshElements();
            }
        }).start();
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
                        renderer.setCurrentDialogWindow(event.window);
                        send(new Request.ChangeUsername(event.selectedUser, dialogEvent.username, currentUser.get()));

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
                        renderer.setCurrentDialogWindow(event.window);
                        send(new Request.ChangePassword(event.selectedUser,
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
                                    renderer.setCurrentDialogWindow(event.window);
                                    send(new Request.GivePermissions(event.selectedUser, permission, currentUser.get()));

                                    refreshPermissions(permissionsEvent.dialog);
                                }
                            }).start();
                        }

                        @Override
                        public void removeButtonClicked(PermissionsDialog.Event permissionsEvent) {
                            final GuiDisabler<PermissionsDialog> disabler = GuiDisabler.disable(permissionsEvent.dialog);

                            new Thread(() -> {
                                renderer.setCurrentDialogWindow(event.window);
                                send(new Request.TakePermission(event.selectedUser, permissionsEvent.permission, currentUser.get()));

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
                            final Renderer.Listener rendererListener = new Renderer.Adapter() {

                                @Override
                                public void viewRendering(Renderer.Event rendererEvent) {
                                    if (rendererEvent.view.error != null) {
                                        return;
                                    }

                                    if (!(rendererEvent.view instanceof PermissionsView)) {
                                        return;
                                    }

                                    SwingUtilities.invokeLater(() -> {
                                        final GuiDisabler<PermissionsDialog> disabler = GuiDisabler.disable(dialog);

                                        dialog.setPermissions(((PermissionsView) rendererEvent.view).permissions);
                                        disabler.revert();
                                    });
                                }
                            };

                            renderer.addListener(rendererListener);
                            renderer.setCurrentDialogWindow(event.window);
                            send(new Request.GetPermissions(event.selectedUser, currentUser.get()));

                            renderer.removeListener(rendererListener);
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
                        renderer.setCurrentDialogWindow(event.window);
                        send(new Request.Register(dialogEvent.username, dialogEvent.email, currentUser.get()));
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
                    renderer.setCurrentDialogWindow(event.window);
                    send(new Request.RemoveUser(event.selectedUser, currentUser.get()));
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

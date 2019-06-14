package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab6_Programming.Packet;
import ru.byprogminer.Lab6_Programming.Packet.Request;
import ru.byprogminer.Lab6_Programming.udp.UdpServerSocket;
import ru.byprogminer.Lab6_Programming.udp.UdpSocket;
import ru.byprogminer.Lab7_Programming.Frontend;
import ru.byprogminer.Lab7_Programming.RunMutex;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.controllers.UsersController;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.byprogminer.Lab5_Programming.LabUtils.callIfNotNull;
import static ru.byprogminer.Lab5_Programming.LabUtils.throwing;
import static ru.byprogminer.Lab7_Programming.ClientMain.SERVER_TIMEOUT;

public class RemoteFrontend implements Frontend {

    private class ClientHandler implements Runnable {

        private final Logger log = Loggers.getObjectLogger(this);

        private final UdpSocket<?> socket;

        public ClientHandler(UdpSocket<?> socket) {
            this.socket = socket;

            try {
                setDeviceSoTimeout(socket.getDevice());
            } catch (SocketException e) {
                log.log(Level.INFO, "exception thrown", e);
            }
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed() && runMutex.isRunning()) {
                    try {
                        final Request request = socket.receive(Request.class, SO_TIMEOUT);

                        final AtomicBoolean finish = new AtomicBoolean();
                        new Thread(() -> {
                            while (!finish.get() && !socket.isClosed()) {
                                try {
                                    socket.send(new Packet.Response.Ping(), PING_TIMEOUT);
                                    Thread.sleep(PING_TIMEOUT);
                                } catch (InterruptedException | SocketTimeoutException ignored) {
                                } catch (Throwable e) {
                                    log.log(Level.INFO, "exception thrown", e);
                                }
                            }
                        }).start();

                        final View view;
                        if (request instanceof Request.Add) {
                            final Request.Add addRequest = (Request.Add) request;

                            view = collectionController.add(addRequest.element, addRequest.credentials);
                        } else if (request instanceof Request.Remove) {
                            final Request.Remove removeRequest = (Request.Remove) request;

                            view = collectionController.remove(removeRequest.element, removeRequest.credentials);
                        } else if (request instanceof Request.RemoveLower) {
                            final Request.RemoveLower removeLowerRequest = (Request.RemoveLower) request;

                            view = collectionController.removeLower(
                                    removeLowerRequest.element,
                                    removeLowerRequest.credentials
                            );
                        } else if (request instanceof Request.RemoveGreater) {
                            final Request.RemoveGreater removeGreaterRequest = (Request.RemoveGreater) request;

                            view = collectionController.removeGreater(
                                    removeGreaterRequest.element,
                                    removeGreaterRequest.credentials
                            );
                        } else if (request instanceof Request.ReplaceElement) {
                            final Request.ReplaceElement replaceElementRequest = (Request.ReplaceElement) request;

                            view = collectionController.replaceElement(
                                    replaceElementRequest.element,
                                    replaceElementRequest.newElement,
                                    replaceElementRequest.credentials
                            );
                        } else if (request instanceof Request.Info) {
                            view = collectionController.info();
                        } else if (request instanceof Request.ShowAll) {
                            view = collectionController.show();
                        } else if (request instanceof Request.Show) {
                            final Request.Show showRequest = (Request.Show) request;

                            view = collectionController.show(showRequest.count);
                        } else if (request instanceof Request.Save) {
                            final Request.Save saveRequest = (Request.Save) request;

                            view = collectionController.save(saveRequest.filename, saveRequest.credentials);
                        } else if (request instanceof Request.Load) {
                            final Request.Load loadRequest = (Request.Load) request;

                            view = collectionController.load(loadRequest.filename, loadRequest.credentials);
                        } else if (request instanceof Request.Import) {
                            final Request.Import importRequest = (Request.Import) request;

                            view = collectionController.importObjects(importRequest.content, importRequest.credentials);
                        } else if (request instanceof Request.CheckPassword) {
                            view = usersController.checkPassword(request.credentials);
                        } else if (request instanceof Request.GetUsers) {
                            view = usersController.get();
                        } else if (request instanceof Request.ChangeUsername) {
                            final Request.ChangeUsername changeUsernameRequest = (Request.ChangeUsername) request;

                            view = usersController.changeUsername(changeUsernameRequest.username,
                                    changeUsernameRequest.newUsername, changeUsernameRequest.credentials);
                        } else if (request instanceof Request.ChangePassword) {
                            final Request.ChangePassword changePasswordRequest = (Request.ChangePassword) request;

                            view = usersController.changePassword(changePasswordRequest.username,
                                    changePasswordRequest.password, changePasswordRequest.credentials);
                        } else if (request instanceof Request.Register) {
                            final Request.Register registerRequest = (Request.Register) request;

                            view = usersController.register(registerRequest.username,
                                    registerRequest.email, registerRequest.credentials);
                        } else if (request instanceof Request.RemoveUser) {
                            final Request.RemoveUser removeUserRequest = (Request.RemoveUser) request;

                            view = usersController.removeUser(removeUserRequest.username, removeUserRequest.credentials);
                        } else if (request instanceof Request.GetPermissions) {
                            final Request.GetPermissions getPermissionsRequest = (Request.GetPermissions) request;

                            view = usersController.getPermissions(getPermissionsRequest.username,
                                    getPermissionsRequest.credentials);
                        } else if (request instanceof Request.GivePermissions) {
                            final Request.GivePermissions givePermissionsRequest = (Request.GivePermissions) request;

                            view = usersController.givePermission(givePermissionsRequest.username,
                                    givePermissionsRequest.permission, givePermissionsRequest.credentials);
                        } else if (request instanceof Request.TakePermission) {
                            final Request.TakePermission takePermissionsRequest = (Request.TakePermission) request;

                            view = usersController.takePermission(takePermissionsRequest.username,
                                    takePermissionsRequest.permission, takePermissionsRequest.credentials);
                        } else {
                            view = null;
                        }

                        finish.set(true);
                        socket.send(new Packet.Response.Done(view), CLIENT_TIMEOUT);
                    } catch (InterruptedException | SocketTimeoutException ignored) {
                    } catch (Throwable e) {
                        log.log(Level.INFO, "exception thrown", e);
                    }
                }

                runMutex.finish();
            } finally {
                log.info("handler finished");
                socket.close();
            }
        }
    }

    public static final int SO_TIMEOUT = 1000;
    public static final int CLIENT_TIMEOUT = 10000;
    public static final int PING_TIMEOUT = SERVER_TIMEOUT / 10;

    private final Logger log = Loggers.getObjectLogger(this);

    private final RunMutex runMutex = new RunMutex();
    private final Map<Thread, UdpSocket<?>> threadSockets = new ConcurrentHashMap<>();
    private final CollectionController collectionController;
    private final UsersController usersController;
    private final UdpServerSocket<?> serverSocket;

    public RemoteFrontend(
            UdpServerSocket<?> serverSocket,
            UsersController usersController,
            CollectionController collectionController
    ) {
        this.collectionController = collectionController;
        this.usersController = usersController;
        this.serverSocket = serverSocket;

        try {
            setDeviceSoTimeout(serverSocket.getDevice());
        } catch (SocketException e) {
            log.log(Level.INFO, "exception thrown", e);
        }
    }

    @Override
    public synchronized void exec() {
        if (!runMutex.tryRun()) {
            log.warning("trying to execute running remote frontend");
            throw new IllegalStateException("this frontend is running already");
        }

        log.info("frontend executed");
        while (runMutex.isRunning()) {
            try {
                final UdpSocket<?> socket = serverSocket.accept();
                final ClientHandler clientHandler = new ClientHandler(socket);
                final Thread clientHandlerThread = new Thread(clientHandler);

                clientHandlerThread.setName("Remote frontend's client handler #" + System.identityHashCode(clientHandler));
                runMutex.shareRun(clientHandlerThread);
                threadSockets.put(clientHandlerThread, socket);
                clientHandlerThread.start();
            } catch (SocketTimeoutException ignored) {
            } catch (Throwable e) {
                log.log(Level.INFO, "exception in accepting loop", e);
            }
        }

        runMutex.finish();
    }

    @Override
    public void stop() {
        runMutex.stop();
        runMutex.getCurrentThreads().forEach(thread -> {
            try {
                throwing().unwrap(IOException.class, () ->
                        callIfNotNull(threadSockets.get(thread),
                                throwing().consumer(UdpSocket::close)));
            } catch (IOException e) {
                log.log(Level.SEVERE, "exception thrown", e);
            } finally {
                thread.interrupt();
            }
        });
    }

    private <D> void setDeviceSoTimeout(D device) throws SocketException {
        final DatagramSocket datagramSocket;

        if (device instanceof DatagramChannel) {
            datagramSocket = ((DatagramChannel) device).socket();
        } else if (device instanceof DatagramSocket) {
            datagramSocket = (DatagramSocket) device;
        } else {
            throw new UnsupportedOperationException("unknown device");
        }

        datagramSocket.setSoTimeout(SO_TIMEOUT);
    }
}

package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab6_Programming.Packet;
import ru.byprogminer.Lab6_Programming.Packet.Request;
import ru.byprogminer.Lab6_Programming.udp.UdpServerSocket;
import ru.byprogminer.Lab6_Programming.udp.UdpSocket;
import ru.byprogminer.Lab7_Programming.Frontend;
import ru.byprogminer.Lab7_Programming.RunMutex;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab7_Programming.controllers.CollectionController;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoteFrontend implements Frontend {

    private class ClientHandler implements Runnable {

        private final Logger log = clientHandlerLog;

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
                while (runMutex.isRunning()) {
                    try {
                        final Request request = socket.receive(Request.class, SO_TIMEOUT);

                        final View view;
                        if (request instanceof Request.Add) {
                            final Request.Add addRequest = (Request.Add) request;

                            view = collectionController.add(addRequest.element);
                        } else if (request instanceof Request.Remove) {
                            final Request.Remove removeRequest = (Request.Remove) request;

                            view = collectionController.remove(removeRequest.element);
                        } else if (request instanceof Request.RemoveLower) {
                            final Request.RemoveLower removeLowerRequest = (Request.RemoveLower) request;

                            view = collectionController.removeLower(removeLowerRequest.element);
                        } else if (request instanceof Request.RemoveGreater) {
                            final Request.RemoveGreater removeGreaterRequest = (Request.RemoveGreater) request;

                            view = collectionController.removeGreater(removeGreaterRequest.element);
                        } else if (request instanceof Request.Info) {
                            view = collectionController.info();
                        } else if (request instanceof Request.ShowAll) {
                            view = collectionController.show();
                        } else if (request instanceof Request.Show) {
                            final Request.Show showRequest = (Request.Show) request;

                            view = collectionController.show(showRequest.count);
                        } else if (request instanceof Request.Save) {
                            final Request.Save saveRequest = (Request.Save) request;

                            view = collectionController.save(saveRequest.filename);
                        } else if (request instanceof Request.Load) {
                            final Request.Load loadRequest = (Request.Load) request;

                            view = collectionController.load(loadRequest.filename);
                        } else if (request instanceof Request.Import) {
                            final Request.Import importRequest = (Request.Import) request;

                            view = collectionController.importObjects(importRequest.content);
                        } else {
                            view = null;
                        }

                        socket.send(new Packet.Response.Done(view), CLIENT_TIMEOUT);
                    } catch (Throwable e) {
                        log.log(Level.INFO, "exception thrown", e);
                    }
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.log(Level.INFO, "exception thrown", e);
                }
            }
        }
    }

    private static final int SO_TIMEOUT = 1000;
    private static final int CLIENT_TIMEOUT = 10000;

    private static final Logger log = Loggers.getLogger(RemoteFrontend.class.getName());
    private static final Logger clientHandlerLog = Loggers.getLogger(ClientHandler.class.getName());

    private final RunMutex runMutex = new RunMutex();

    private final CollectionController collectionController;
    private final UdpServerSocket<?> serverSocket;

    public RemoteFrontend(UdpServerSocket<?> serverSocket, CollectionController collectionController) {
        this.collectionController = collectionController;
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

        while (runMutex.isRunning()) {
            try {
                final Thread clientHandlerThread = new Thread(new ClientHandler(serverSocket.accept()));
                runMutex.shareRun(clientHandlerThread);
                clientHandlerThread.start();
            } catch (SocketTimeoutException ignored) {
            } catch (Throwable e) {
                log.log(Level.INFO, "exception in accept loop", e);
            }
        }

        runMutex.finish();
    }

    @Override
    public void stop() {
        runMutex.stop();
        runMutex.getCurrentThreads().forEach(Thread::interrupt);
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

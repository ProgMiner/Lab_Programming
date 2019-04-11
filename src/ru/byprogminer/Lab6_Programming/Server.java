package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab5_Programming.CollectionManager;
import ru.byprogminer.Lab5_Programming.command.StatusPrinter;
import ru.byprogminer.Lab6_Programming.Packet.Request;
import ru.byprogminer.Lab6_Programming.Packet.Response;
import ru.byprogminer.Lab6_Programming.udp.UDPServerSocket;
import ru.byprogminer.Lab6_Programming.udp.UDPSocket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.Formatter;

public class Server<C extends DatagramChannel> implements Runnable {

    private final C channel;
    private final UDPServerSocket<C> serverSocket;
    private final CollectionManager collection;

    private class ClientWorker implements Runnable {

        private final UDPSocket<?> socket;

        public ClientWorker(UDPSocket<?> socket) {
            this.socket = socket;

            final DatagramSocket datagramSocket;
            final Object device = socket.getDevice();
            if (device instanceof DatagramChannel) {
                datagramSocket = ((DatagramChannel) device).socket();
            } else if (device instanceof DatagramSocket) {
                datagramSocket = (DatagramSocket) device;
            } else {
                return;
            }

            try {
                datagramSocket.setSoTimeout(3000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            final StatusPrinter printer = new StatusPrinter() {

                @Override
                public void print(Object text) {
                    try {
                        socket.send(new Response.Message(text.toString(), Response.Message.Status.OK));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void println(Object text) {
                    print(text.toString() + '\n');
                }

                @Override
                public void printf(String format, Object... text) {
                    final Formatter formatter = new Formatter();
                    formatter.format(format, text);
                    print(formatter.toString());
                }

                @Override
                public void printWarning(Object text) {
                    try {
                        socket.send(new Response.Message(text.toString(), Response.Message.Status.WARN));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void printError(Object text) {
                    try {
                        socket.send(new Response.Message(text.toString(), Response.Message.Status.ERR));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            try {
                while (!socket.isClosed()) {
                    try {
                        final Request packet = socket.receive(Request.class, 600000);

                        if (packet instanceof Request.Add) {
                            collection.add(((Request.Add) packet).getElement(), printer);
                        } else if (packet instanceof Request.Load) {
                            collection.load(printer);
                        } else if (packet instanceof Request.Save) {
                            collection.save(printer);
                        } else if (packet instanceof Request.Show) {
                            collection.show(printer);
                        } else if (packet instanceof Request.RemoveLower) {
                            collection.removeLower(((Request.RemoveLower) packet).getElement(), printer);
                        } else if (packet instanceof Request.RemoveGreater) {
                            collection.removeGreater(((Request.RemoveGreater) packet).getElement(), printer);
                        } else if (packet instanceof Request.Remove) {
                            collection.remove(((Request.Remove) packet).getElement(), printer);
                        } else if (packet instanceof Request.Info) {
                            collection.info(printer);
                        } else if (packet instanceof Request.Import) {
                            // TODO
                        }

                        socket.send(new Response.Done());
                    } catch (SocketTimeoutException ignored) {
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Server(CollectionManager collection, C channel, int partSize) {
        this.collection = collection;
        this.channel = channel;

        serverSocket = UDPServerSocket.by(channel, partSize);
    }

    @Override
    public void run() {
        while (true) {
            try {
                new Thread(new ClientWorker(serverSocket.accept())).start();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public DatagramChannel getChannel() {
        return channel;
    }
}

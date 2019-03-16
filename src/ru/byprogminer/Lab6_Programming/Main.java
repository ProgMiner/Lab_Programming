package ru.byprogminer.Lab6_Programming;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Moveable;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab4_Programming.NotFoundException;
import ru.byprogminer.Lab6_Programming.udp.PacketReceiver;
import ru.byprogminer.Lab6_Programming.udp.PacketSender;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class Main implements Runnable {

    public static final int PART_SIZE = 10240;
    public static final int SEND_INTERVAL = 1000;

    private static final String USAGE = "Usage: java -jar lab6_client.jar <port> [server]\n" +
            "  - port\n" +
            "    Port number\n" +
            "  - server\n" +
            "    Not required server address";

    private volatile boolean responseReceived = false;
    private final PacketReceiver receiver;
    private final SocketAddress address;

    public static void main(String[] args) {
        // Check is argument provided

        if (args.length < 1) {
            System.err.println("Port is not provided");
            System.err.println(USAGE);
            System.exit(1);
        }

        try {
            final int port = Integer.parseInt(args[0]);

            final SocketAddress address;
            if (args.length > 1) {
                address = new InetSocketAddress(args[1], port);
            } else {
                address = new InetSocketAddress(port);
            }

            final DatagramSocket socket = new DatagramSocket();
            final PacketSender sender = PacketSender.by(socket, PART_SIZE);
            final PacketReceiver receiver = PacketReceiver.by(socket, PART_SIZE);

            final Main main = new Main(address, receiver);
            final Thread receiverThread = new Thread(main);
            receiverThread.start();

            final Packet packet = new Packet.Request.CurrentState();
            while (!main.responseReceived) {
                sender.send(packet, address);
                Thread.sleep(SEND_INTERVAL);
            }
        } catch (Throwable e) {
            System.err.printf("Execution error: %s\n", e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
        }
    }

    private Main(SocketAddress address, PacketReceiver receiver) {
        this.receiver = receiver;
        this.address = address;
    }

    @Override
    public void run() {
        Packet receivedPacket = null;

        do {
            try {
                receivedPacket = receiver.receive().getA();
            } catch (Throwable ignored) {}
        } while (!(receivedPacket instanceof Packet.Response.CurrentState));
        responseReceived = true;

        Packet.Response.CurrentState response = (Packet.Response.CurrentState) receivedPacket;
        generateScript(response.getContent());

        System.exit(0);
    }

    private void generateScript(List<LivingObject> livingObjects) {
        if (livingObjects.isEmpty()) {
            System.out.println("Перекати-поле...");
            return;
        }

        Queue<LivingObject> queue = new LinkedList<>(livingObjects);
        LivingObject skuperfield = queue.remove();
        if (livingObjects.size() < 2) {
            if (!skuperfield.getItems().isEmpty()) {
                System.out.print("Прихватив с собой объекты: ");
                skuperfield.getItems().parallelStream()
                        .forEach(object -> System.out.printf("%s, ", object.getName()));
            }

            skuperfield.moveFrom("огорода", Moveable.Move.GO);
            return;
        }

        Random random = new Random();
        LivingObject security = queue.remove();
        List<Object> securityItems = new ArrayList<>(security.getItems());
        for (int i = 0, countI = random.nextInt(3) + 2; i < countI; ++i) {
            security.moveFor(skuperfield, Moveable.Move.RUN);

            if (!security.getItems().isEmpty()) {
                security.hit(skuperfield, securityItems.get(random.nextInt(securityItems.size())));
            }

            skuperfield.moveTo("оврагу", Moveable.Move.RUN);

            if (!skuperfield.getItems().isEmpty()) {
                int potatoes = skuperfield.getItems().size();

                if (i != countI - 1) {
                    potatoes = new Random().nextInt(i * skuperfield.getItems().size() / countI + 1) - 1;
                }

                for (int potato = 0; potato < potatoes; ++potato) {
                    try {
                        skuperfield.lose(skuperfield.getItems().iterator().next());
                    } catch (NotFoundException ignored) {}
                }
            }
        }

        System.out.printf("%s добежал до дна оврага.\n", skuperfield.getName());
        skuperfield.think("куда пойти, вверх по оврагу или вниз");
        skuperfield.moveTo("вершине оврага", Moveable.Move.GO);

        if (!queue.isEmpty()) {
            System.out.print(queue.parallelStream().map(Object::getName)
                    .collect(Collectors.joining(", ")));

            if (queue.size() == 1) {
                System.out.println(" молча наблюдал за происходящим.");
            } else {
                System.out.println(" молча наблюдали за происходящим.");
            }
        }
    }
}

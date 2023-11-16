package ca.bcit.comp7005;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Receiver {

    private final int port;
    private final int packetBufferSize;

    public Receiver(int port, int packetBufferSize) {
        System.out.println("Receiver started on port " + port);

        this.port = port;
        this.packetBufferSize = packetBufferSize;
    }

    public void listen() {

        try(DatagramSocket socket = new DatagramSocket(port)) {
            printSocketDetails(socket);

            processReceivingData(socket);

        } catch(Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void processReceivingData(DatagramSocket socket) throws IOException {
        byte[] packetBuffer = new byte[packetBufferSize];

        boolean isRunning = true;
        while(isRunning) {
            System.out.println("\nWaiting for a message...");

            DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);
            socket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength());

            if (message.equals("exit")) {
                isRunning = false;
                continue;
            }

            System.out.println("Received: " + message);
            packetBuffer = new byte[packetBufferSize];
        }
    }

    private void printSocketDetails(DatagramSocket socket) throws SocketException {
        System.out.println("- Local Port: " + socket.getLocalPort());
        System.out.println("- Timeout: " + socket.getSoTimeout());
        System.out.println("- Is bound: " + socket.isBound());
        System.out.println("- Is connected: " + socket.isConnected());
        System.out.println("- Is closed: " + socket.isClosed());
        System.out.println("- Receive buffer size: " + socket.getReceiveBufferSize());
        System.out.println("- Send buffer size: " + socket.getSendBufferSize());
    }
}

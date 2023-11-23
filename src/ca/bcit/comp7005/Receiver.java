package ca.bcit.comp7005;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

public class Receiver {

    private final DatagramSocket socket;

    private int sequenceNumber;
    private int senderSequenceNumber;

    private InetAddress senderAddress;

    private int senderPort;

    public Receiver(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        printSocketDetails();
    }

    public void listen() throws IOException, NoSuchAlgorithmException {
        acceptConnection();

        processReceivingData();

        disconnectFromSender();
    }

    public void acceptConnection() throws IOException, NoSuchAlgorithmException {
        // receive syn
        DatagramPacket packet = receiveDatagramPacket();
        AZRP synAzrp = AZRP.fromBytes(packet.getData());

        if (!synAzrp.isSYN()) {
            throw new RuntimeException("The received packet is not a SYN packet.");
        } else {
            this.senderSequenceNumber = synAzrp.getSequenceNumber();
            this.sequenceNumber = AZRP.generateInitialSequenceNumber();
            this.senderAddress = packet.getAddress();
            this.senderPort = packet.getPort();

            System.out.println("Received SYN AZRP: " + synAzrp);
        }

        // send syn ack
        senderSequenceNumber++;
        AZRP aknowledgementAzrp = AZRP.synAck(sequenceNumber, senderSequenceNumber);
        aknowledgementAzrp.send(socket, senderAddress, senderPort);
        System.out.println("Sent SYN-ACK: " + aknowledgementAzrp);

        // receive ack
        DatagramPacket ackPacket = receiveDatagramPacket();
        AZRP ackAzrp = AZRP.fromBytes(ackPacket.getData());
        if (!ackAzrp.isACK()) {
            throw new RuntimeException("The received packet is not a ACK packet.");
        }
        System.out.println("Received ACK AZRP: " + ackAzrp);
    }

    private void disconnectFromSender() throws IOException {
        // Send a FIN packet to the sender
        AZRP finAzrp = AZRP.fin(sequenceNumber, 0);
        finAzrp.send(socket, senderAddress, senderPort);

        // Wait for the sender to acknowledge the FIN packet
        DatagramPacket finAckPacket = receiveDatagramPacket();
        AZRP finAckAzrp = AZRP.fromBytes(finAckPacket.getData());

        if (!finAckAzrp.isACK()) {
            throw new RuntimeException("The received packet is not a ACK packet.");
        }
        System.out.println("Received FIN-ACK AZRP: " + finAckAzrp);

        // Close the socket
        socket.close();

        this.sequenceNumber = 0;
        this.senderSequenceNumber = 0;
        this.senderAddress = null;
        this.senderPort = 0;

        System.out.println("Disconnected from sender.");
    }



    private DatagramPacket receiveDatagramPacket() throws IOException {
        byte[] packetBuffer = new byte[AZRP.MAXIMUM_PACKET_SIZE_IN_BYTES];
        DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);
        socket.receive(packet);
        return packet;
    }


    private void processReceivingData() throws IOException {

        boolean isRunning = true;
        while(isRunning) {
            DatagramPacket packet = receiveDatagramPacket();
            AZRP azrp = AZRP.fromBytes(packet.getData());

            if (azrp.isFIN()) {
                System.out.println("Received FIN AZRP: " + azrp);
                isRunning = false;

                this.senderSequenceNumber = azrp.getSequenceNumber() + 1;
                AZRP ackAzrp = AZRP.ack(this.sequenceNumber, this.senderSequenceNumber);
                ackAzrp.send(socket, senderAddress, senderPort);

            } else {
                System.out.println("Received: (" + azrp.validateChecksum() + "): " + azrp);

                this.sequenceNumber++;
                this.senderSequenceNumber = azrp.getSequenceNumber() + azrp.getData().length;
                AZRP ackAzrp = AZRP.ack(this.sequenceNumber, this.senderSequenceNumber);
                ackAzrp.send(socket, senderAddress, senderPort);
            }
        }
    }


    private void printSocketDetails() throws SocketException {
        System.out.println("- Local Port: " + socket.getLocalPort());
        System.out.println("- Timeout: " + socket.getSoTimeout());
        System.out.println("- Is bound: " + socket.isBound());
        System.out.println("- Is connected: " + socket.isConnected());
        System.out.println("- Is closed: " + socket.isClosed());
        System.out.println("- Receive buffer size: " + socket.getReceiveBufferSize());
        System.out.println("- Send buffer size: " + socket.getSendBufferSize());
    }
}

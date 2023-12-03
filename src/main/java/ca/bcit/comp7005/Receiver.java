package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;

/**
 * A class that receives data from a sender.
 */
public class Receiver {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // The socket used to send and receive UDP datagrams
    private final DatagramSocket datagramSocket;

    /**
     * @param port - the port to listen on.
     * @throws SocketException - if the socket could not be opened.
     */
    public Receiver(int port) throws IOException {
        this.datagramSocket = new DatagramSocket(port);
        logger.info("UDP socket created on port " + datagramSocket.getLocalPort());
    }

    /**
     * Receive a message from a sender.
     * 1. Wait for a SYN packet from the sender. It contains the initial sequence number and the length of the whole message.
     * 2. Send a SYN-ACK packet to the sender.
     * 3. Wait for data packets from the sender. Once a data packet is received, insert it into the message array at the correct position.
     * 4. Send an ACK packet to the sender for each data packet received.
     * @param timeout - the timeout for receiving data packets.
     * @return the received message.
     * @throws IOException - if an I/O error occurs.
     */
    public byte[] receiveMessage(int timeout) throws IOException {
        this.datagramSocket.setSoTimeout(0); // Wait forever for the SYN packet

        int initialSequenceNumber;
        int messageLength;

        int senderPort;
        InetAddress senderAddress;

        // Wait for a SYN packet from the sender
        while (true) {
            final DatagramPacket receivedDatagram = receiveDatagram();
            AZRP synAzrp = AZRP.fromBytes(receivedDatagram.getData());

            // Validate that the AZRP packet in the datagram is a SYN packet
            if (synAzrp.isValidSyn()) {
                senderPort = receivedDatagram.getPort();
                senderAddress = receivedDatagram.getAddress();

                initialSequenceNumber = synAzrp.getSequenceNumber();
                messageLength = synAzrp.getLength();

                // Send a SYN-ACK packet to the sender
                final AZRP synAckAzrp = AZRP.generateSynAckPacket(synAzrp);
                this.sendDatagram(synAckAzrp.toBytes(), senderAddress, senderPort);
                break;
            } else {
                // Drop the packet
                logger.debug("Invalid packet received while waiting for a SYN packet");
            }
        }

        // Wait for data packets from the sender
        this.datagramSocket.setSoTimeout(timeout); // Set the timeout for receiving data packets
        int receivedDataLength = 0; // The length of the data received so far
        final byte[] message = new byte[messageLength]; // The whole message

        while(receivedDataLength < messageLength) {
            // Receive a data packet from the sender
            DatagramPacket receiveDatagram = receiveDatagram();
            // Validate that the AZRP packet in the datagram is an ACK packet with the correct sequence number
            final AZRP dataAzrp = AZRP.fromBytes(receiveDatagram.getData());
            if (dataAzrp.isValidData()) {

                // Insert this packet into the message array at the correct position
                final int dataPosition = dataAzrp.getSequenceNumber() - initialSequenceNumber;
                System.arraycopy(dataAzrp.getData(), 0, message, dataPosition, dataAzrp.getData().length);
                receivedDataLength += dataAzrp.getData().length;
                logger.info("Downloaded: " + receivedDataLength + "/" + messageLength + " bytes");

                // Send an ACK packet to the sender
                final boolean[] ackFlags = new boolean[]{false, true};
                AZRP ackAzrp = new AZRP(new byte[0], dataAzrp.getSequenceNumber() + dataAzrp.getData().length, dataAzrp.getLength(), ackFlags);
                sendDatagram(ackAzrp.toBytes(), senderAddress, senderPort);
            } else {
                // Drop the packet
                logger.error("Invalid packet received while waiting for a data packet");
            }
        }

        return message;
    }


    /**
     * Receives a UDP datagram.
     * @return the received datagram packet.
     * @throws IOException - if an I/O error occurs.
     */
    private DatagramPacket receiveDatagram() throws IOException {
        byte[] packetBuffer = new byte[AZRP.MAXIMUM_PACKET_SIZE_IN_BYTES];
        DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);
        this.datagramSocket.receive(packet);
        return packet;
    }

    /**
     * Sends a UDP datagram.
     * @param data - the data to be sent.
     * @throws IOException - if an I/O error occurs.
     */
    private void sendDatagram(byte[] data, InetAddress address, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(
                data, data.length, address, port
        );
        datagramSocket.send(packet);
    }

    public void close() {
        this.datagramSocket.close();
    }
}

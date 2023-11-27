package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

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
    private Receiver(int port) throws SocketException {
        this.datagramSocket = new DatagramSocket(port);
        logger.info("UDP socket created on port " + port);
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
        logger.debug("Received a datagram packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
        return packet;
    }

    /**
     * Sends a UDP datagram.
     * @param data - the data to be sent.
     * @throws IOException - if an I/O error occurs.
     */
    private void sendDatagram(byte[] data, InetAddress senderAddress, int senderPort) throws IOException {
        DatagramPacket packet = new DatagramPacket(
                data, data.length, senderAddress, senderPort
        );
        datagramSocket.send(packet);
        logger.debug("Sent a datagram packet to " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
    }

    /**
     * Listen for a connection from the sender and receives data packets.
     * @param port - the port to listen on.
     * @throws IOException - if an I/O error occurs.
     */
    public static void listen(int port) throws IOException {
        Receiver receiver = new Receiver(port);

        receiver.receiveMessage();
    }
    
    /**
     * Accept a connection from the sender.
     * Receive a SYN packet from the sender, set the sequence number and the message length, and send a SYN-ACK packet.
     * @throws IOException - if an I/O error occurs.
     */
    private void receiveMessage() throws IOException {
        logger.info("Waiting for new messages...");

        // 1. Receive a SYN packet from the sender
        // The first received packet should have the SYN flag set
        // This packet contains the initial sequence number and the length of the whole message that consists of a few AZRP packets
        final DatagramPacket synDatagram = receiveDatagram();
        final AZRP synAzrp = AZRP.fromBytes(synDatagram.getData());
        // Validate the SYN packet
        if (!synAzrp.isSYN()) {
            throw new IOException("Unable to connect to the receiver: Receiver responded with invalid packet type");
        } else if (!synAzrp.isChecksumValid()) {
            throw new IOException("Unable to connect to the receiver: The packet is corrupted");
        }

        // Set the address and port of the server that sent the packet
        // This is needed for sending acknowledgement packets back to the server
        final InetAddress senderAddress = InetAddress.getByName(synDatagram.getAddress().getHostAddress());
        final int senderPort = synDatagram.getPort();

        // The SYN packet contains the initial sequence number and the length of the whole message
        final int messageInitialSequenceNumber = synAzrp.getSequenceNumber();
        final int messageLength = synAzrp.getLength();
        int receivedDataLength = 0; // The length of the data received so far
        final byte[] message = new byte[messageLength];
        logger.debug("SYN packet with sequence number " + messageInitialSequenceNumber + " and length " + messageLength);


        // 2. Send a SYN-ACK packet back to the sender
        final boolean[] synAckFlags = new boolean[]{true, true};
        final AZRP synAckAzrp = new AZRP(new byte[0], messageInitialSequenceNumber, messageLength, synAckFlags);
        sendDatagram(synAckAzrp.toBytes(), senderAddress, senderPort);
        logger.debug("Sent a SYN-ACK packet with sequence number " + messageInitialSequenceNumber + " and length " + messageLength);


        // 3. Receive a data packet from the sender
        logger.debug("Waiting for data packets...");
        while(receivedDataLength < messageLength) {
            // Receive a data packet from the sender
            final DatagramPacket datagram = this.receiveDatagram();
            final AZRP dataAzrp = AZRP.fromBytes(datagram.getData());

            if (dataAzrp.isData() && dataAzrp.isChecksumValid()) {
                logger.debug("Data packet at sequence number " + dataAzrp.getSequenceNumber() + " with length " + dataAzrp.getLength() + " bytes: " + new String(dataAzrp.getData()));

                // Insert this packet into the message array at the correct position
                final int dataPosition = dataAzrp.getSequenceNumber() - messageInitialSequenceNumber;
                System.arraycopy(dataAzrp.getData(), 0, message, dataPosition, dataAzrp.getData().length);
                receivedDataLength += dataAzrp.getData().length;
                logger.debug("*** Inserted " + new String(dataAzrp.getData()) + " into the message array at position " + dataPosition);
                logger.debug("Message: " + new String(message));

                // Send an ACK packet to the sender
                final boolean[] ackFlags = new boolean[]{false, true};
                AZRP ackAzrp = new AZRP(new byte[0], dataAzrp.getSequenceNumber(), dataAzrp.getLength(), ackFlags);
                sendDatagram(ackAzrp.toBytes(), senderAddress, senderPort);
                logger.debug("Sent acknowledgement with sequence number " + ackAzrp.getSequenceNumber() + " and length " + ackAzrp.getLength());

            } else {
                logger.debug("   Invalid packet");
                throw new RuntimeException("Received invalid packet type");
            }
        }

        logger.info("Received message: " + new String(message));
    }
}

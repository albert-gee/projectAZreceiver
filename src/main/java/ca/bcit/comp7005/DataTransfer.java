package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class DataTransfer {

    // The socket used to send and receive UDP datagrams

    private final DatagramSocket datagramSocket;

    private int senderPort;
    private InetAddress senderAddress;

    private final int readDataTimeOut;

    private int initialSequenceNumber;
    private int messageLength;
    private String fileType;

    private static final Logger logger = LoggerFactory.getLogger(DataTransfer.class);

    private DataTransfer(int readDataTimeOut, int port) throws SocketException {
        this.readDataTimeOut = readDataTimeOut;
        this.datagramSocket = new DatagramSocket(port);
        logger.info("UDP socket created on port " + datagramSocket.getLocalPort());

    }

    // 1. Accept new data transfer connection from the sender
    public static DataTransfer accept(int readDataTimeOut, int port) throws IOException {
        DataTransfer dataTransfer = new DataTransfer(readDataTimeOut, port);

        AZRP synAzrp = dataTransfer.receiveConnectionRequest();
        dataTransfer.acknowledgeConnectionRequest(synAzrp);

        return dataTransfer;
    }

    private AZRP receiveConnectionRequest() throws IOException {
        // Loop and wait for a valid SYN packet from the sender
        this.datagramSocket.setSoTimeout(0); // Wait forever for the SYN packet
        while (true) {
            final DatagramPacket receivedDatagram = receiveDatagram();
            AZRP synAzrp = AZRP.fromBytes(receivedDatagram.getData());

            // Validate that the AZRP packet in the datagram is a SYN packet
            if (synAzrp.isValidSyn()) {
                this.senderPort = receivedDatagram.getPort();
                this.senderAddress = receivedDatagram.getAddress();

                this.initialSequenceNumber = synAzrp.getSequenceNumber();
                this.messageLength = synAzrp.getLength();
                this.fileType = synAzrp.getData().length > 0 ? new String(synAzrp.getData()) : "";

                return synAzrp;
            } else {
                // Drop invalid packets
                logger.debug("Invalid packet received while waiting for a SYN packet");
            }
        }
    }

    private void acknowledgeConnectionRequest(AZRP synAzrp) throws IOException {
        // Send a SYN-ACK packet to the sender
        final AZRP synAckAzrp = AZRP.generateSynAckPacket(synAzrp);
        this.sendDatagram(synAckAzrp.toBytes());
    }

    public byte[] readData() throws IOException {

        // 2. Receive the data packets after the connection is established
        this.datagramSocket.setSoTimeout(this.readDataTimeOut); // Timeout for data packets is limited
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
                AZRP ackAzrp = new AZRP(new byte[0], dataAzrp.getSequenceNumber() + dataAzrp.getData().length, dataAzrp.getLength(), dataAzrp.getCheckSum(), ackFlags);
                sendDatagram(ackAzrp.toBytes());
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
    private void sendDatagram(byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(
                data, data.length, this.senderAddress, this.senderPort
        );
        datagramSocket.send(packet);
    }


    public void close() {
        this.datagramSocket.close();
    }

    public String getFileType() {
        return fileType;
    }
}

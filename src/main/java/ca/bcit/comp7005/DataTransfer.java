package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class describes the data transfer from the sender.
 */
public class DataTransfer {

    // The socket used to send and receive UDP datagrams
    private final DatagramSocket datagramSocket;
    private final int readDataTimeOut;
    private int senderPort;
    private InetAddress senderAddress;

    // The following properties are used for data transfer meta-data
    private int initialSequenceNumber;
    private int dataLength;
    private String fileType;

    // The following properties are used for statistics:
    private int packetsSent;
    private int packetsReceived;

    private static final Logger logger = LoggerFactory.getLogger(DataTransfer.class);

    public DataTransfer(int readDataTimeOut, int port) throws SocketException {
        this.readDataTimeOut = readDataTimeOut;
        this.datagramSocket = new DatagramSocket(port);
        logger.info("UDP socket created on port " + datagramSocket.getLocalPort());
    }

    /**
     * Accepts a connection from the sender:
     * 1. Receive a SYN packet from the sender
     * 2. Send a SYN-ACK packet to the sender
     * @throws IOException - if an I/O error occurs.
     */
    public void accept() throws IOException {

        // Receive a SYN packet from the sender
        AZRP synAzrp = this.receiveSyn();
        // Send a SYN-ACK packet to the sender
        this.acknowledgeConnectionRequest(synAzrp);
    }

    /**
     * Receives a SYN packet from the sender.
     * @return the received SYN packet.
     * @throws IOException - if an I/O error occurs.
     */
    private AZRP receiveSyn() throws IOException {
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
                this.dataLength = synAzrp.getLength();

                this.setFileType(new String(synAzrp.getData()));

                logger.debug("Received SYN packet from " + senderAddress + ":" + senderPort);
                return synAzrp;
            } else {
                // Drop invalid packets
                logger.error("Invalid SYN packet received");
            }
        }
    }

    /**
     * Sends a SYN-ACK packet to the sender.
     * @param synAzrp - the SYN packet received from the sender.
     * @throws IOException - if an I/O error occurs.
     */
    private void acknowledgeConnectionRequest(AZRP synAzrp) throws IOException {
        // Send a SYN-ACK packet to the sender
        final AZRP synAckAzrp = AZRP.generateSynAckPacket(synAzrp);
        this.sendDatagram(synAckAzrp.toBytes());
        logger.debug("Sent SYN-ACK packet to " + senderAddress + ":" + senderPort);
    }

    /**
     * After the connection is established, receives the data from the sender.
     * @return the received data.
     * @throws IOException - if an I/O error occurs.
     */
    public byte[] readData() throws IOException {

        this.datagramSocket.setSoTimeout(this.readDataTimeOut); // Timeout for data packets is limited unlike the SYN packet

        final byte[] wholeData = new byte[this.dataLength]; // The whole message
        int receivedDataLength = 0; // The length of the data received so far

        while(receivedDataLength < this.dataLength) {
            // Receive a data packet from the sender
            AZRP dataAzrp = receiveDataAzrp();

            // If the packet is not null, it's valid. Otherwise, it's invalid and we drop it
            if (dataAzrp != null) {
                // Insert this packet into the wholeData array at the correct position
                final int dataPosition = dataAzrp.getSequenceNumber() - initialSequenceNumber;
                System.arraycopy(dataAzrp.getData(), 0, wholeData, dataPosition, dataAzrp.getData().length);
                receivedDataLength += dataAzrp.getData().length;
                logger.info("Downloaded: " + receivedDataLength + "/" + dataLength + " bytes");

                // Send an ACK packet to the sender
                final boolean[] ackFlags = new boolean[]{false, true};
                AZRP ackAzrp = new AZRP(new byte[0], dataAzrp.getSequenceNumber() + dataAzrp.getData().length, dataAzrp.getLength(), dataAzrp.getCheckSum(), ackFlags);
                sendDatagram(ackAzrp.toBytes());
            }
        }

        return wholeData;
    }

    /**
     * Receives a data AZRP packet from the sender and validates it.
     * @return the received data packet or null if the packet is invalid.
     * @throws IOException - if an I/O error occurs.
     */
    private AZRP receiveDataAzrp() throws IOException {
        AZRP dataAzrp = null;

        // Receive a data packet from the sender
        DatagramPacket receivedDatagram = receiveDatagram();
        // Validate that the AZRP packet in the datagram is an ACK packet with the correct sequence number
        AZRP azrp = AZRP.fromBytes(receivedDatagram.getData());
        if (azrp.isValidData()) {
            dataAzrp = azrp;
        } else if (azrp.isSYN() && azrp.isValidSyn()) {
            // Sender sent another SYN packet, so we have to start session again
            logger.error("Received SYN packet while waiting for data packet. Start session again.");

            this.senderPort = receivedDatagram.getPort();
            this.senderAddress = receivedDatagram.getAddress();
            this.initialSequenceNumber = azrp.getSequenceNumber();
            this.dataLength = azrp.getLength();
            this.setFileType(new String(azrp.getData()));
            // Send a SYN-ACK packet to the sender
            this.acknowledgeConnectionRequest(azrp);

        } else {
            // Drop the packet
            logger.error("Received invalid data packet");
        }

        return dataAzrp;
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

        this.packetsReceived++;

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

        this.packetsSent++;
    }

    /**
     * Closes the socket.
     */
    public void close() {
        this.datagramSocket.close();
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String mimeType) throws IOException {
        if (mimeType != null) {

            // Custom mapping for common MIME types
            Map<String, String> mimeToExtension = getStringStringMap();

            // Extract the file extension from the MIME type
            String defaultExtension = "textstring"; // Default extension if not found in the mapping
            this.fileType = mimeToExtension.getOrDefault(mimeType.trim(), defaultExtension);
        } else {
            throw new IOException("The file type could not be determined");
        }
    }

    private static Map<String, String> getStringStringMap() {
        Map<String, String> mimeToExtension = new HashMap<>();
        mimeToExtension.put("text/plain", "txt");
        mimeToExtension.put("application/pdf", "pdf");
        mimeToExtension.put("image/jpeg", "jpg");
        mimeToExtension.put("image/png", "png");
        mimeToExtension.put("application/json", "json");
        mimeToExtension.put("application/xml", "xml");
        mimeToExtension.put("application/msword", "doc");
        mimeToExtension.put("application/vnd.ms-excel", "xls");
        return mimeToExtension;
    }

    public String getStatistics() {
        return "Packets sent: " + this.packetsSent + "; packets received: " + this.packetsReceived;
    }
}

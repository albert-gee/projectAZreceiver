package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A class that receives data from a sender.
 */
public class Receiver {
    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    private final String directoryPath;

    private static final String logFilePath = "log.txt";

    /**
     * @param directoryPath - the path to the directory where the received files will be saved.
     */
    public Receiver(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    /**
     * Run the receiver.
     * @param port - the port to listen on.
     * @param timeout - the timeout for receiving data packets.
     */
    public void run(int port, int timeout) throws SocketException {
        logger.info("Receiver started");

        DataTransfer dataTransfer = new DataTransfer(timeout, port);

        try {

            boolean isRunning = true;
            while (isRunning) {
                // Accept a SYN packet from the sender and respond with a SYN-ACK packet
                dataTransfer.accept();

                // Receive data packets from the sender
                // If the sender sends another SYN packet, DataTransferRestartException is thrown and the connection is restarted
                byte[] data = dataTransfer.readData();

                // If the file type is tex, it's a text string
                if (dataTransfer.getFileType().equals("textstring")) {
                    String receivedMessage = new String(data);
                    logger.info("Received message: " + receivedMessage + "\n");

                    if (receivedMessage.equals("quit")) {
                        isRunning = false;
                    }
                } else {
                    // If the file type is different, it's a file and we save it
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
                    String timestamp = LocalDateTime.now().format(formatter);

                    String filePath = this.directoryPath + "/" + timestamp + "." + dataTransfer.getFileType(); // Replace with the desired path for the output file
                    Path path = Paths.get(filePath);
                    Files.write(path, data);
                    logger.info("Received file: " + filePath + "\n");
                }

                this.writeStatistics(dataTransfer.getStatistics());
            }
            dataTransfer.close();

        } catch (SocketTimeoutException e) {
            logger.error("Message data was not received");
        } catch (IOException e) {
            logger.error("Error receiving message: " + e.getMessage());
        }

        logger.info("Receiver stopped");
    }

    /**
     * Writes the statistics to the log file.
     * The entry in the log file contains the number of sent and received packets
     * @param line - the statistics to write.
     */
    private void writeStatistics(String line) {
        // Get the current timestamp for the log entry
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = currentTime.format(formatter);

        // Append the log message with timestamp
        String logEntry = timestamp + " - " + line;

        // Write the log entry to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(logEntry);
            writer.newLine(); // Add a newline for the next entry
        } catch (IOException e) {
            System.err.println("Error writing to the log file: " + e.getMessage());
        }
    }
}

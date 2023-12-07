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
    public void run(int port, int timeout) throws DataTransferRestartException {
        try {
            // Accept a SYN packet from the sender and respond with a SYN-ACK packet
            DataTransfer dataTransfer = DataTransfer.accept(timeout, port);

            // Receive data packets from the sender
            // If the sender sends another SYN packet, DataTransferRestartException is thrown and the connection is restarted
            byte[] data = dataTransfer.readData();

            // If the file type is tex, it's a text string
            if (dataTransfer.getFileType().equals("textstring")) {
                logger.info("Received message: " + new String(data) + "\n");
            } else {
                // If the file type is different, it's a file and we save it
                String filePath = this.directoryPath + "/fileName." + dataTransfer.getFileType(); // Replace with the desired path for the output file
                Path path = Paths.get(filePath);
                Files.write(path, data);
                logger.info("Received file: " + filePath + "\n");
            }
            dataTransfer.close();

            this.writeStatistics(dataTransfer.getStatistics());


        } catch (SocketTimeoutException e) {
            logger.info("Message data was not received");
        } catch (IOException e) {
            logger.info("Error receiving message: " + e.getMessage());
        }
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

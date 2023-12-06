package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A class that receives data from a sender.
 */
public class Receiver {
    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    private final String directoryPath;

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
    public void run(int port, int timeout) {
        try {
            DataTransfer dataTransfer = DataTransfer.accept(timeout, port);
            byte[] data = dataTransfer.readData();

            // If the file type is tex, it's a text string
            if (dataTransfer.getFileType().equals("textstring")) {
                logger.info("Received message: " + new String(data) + "\n");
            } else {
                // If the file type is different, it's a file and we save it
                String filePath = this.directoryPath + "fileName." + dataTransfer.getFileType(); // Replace with the desired path for the output file
                Path path = Paths.get(filePath);
                Files.write(path, data);
                logger.info("Received file: " + filePath + "\n");
            }
            dataTransfer.close();

        } catch (SocketTimeoutException e) {
            logger.info("Message data was not received");
        } catch (IOException e) {
            logger.info("Error receiving message: " + e.getMessage());
        }


    }

}

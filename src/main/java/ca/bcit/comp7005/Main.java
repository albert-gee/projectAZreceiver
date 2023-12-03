package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.exit;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int TIMEOUT_MILLISECONDS = 5000;

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);

            // Create a new receiver and wait for a message
            Receiver receiver = new Receiver(port);
            String stringMessage;
            do {
                logger.info("Waiting for a new message...");

                // Once a connection with sender is established, receive the message
                byte[] data = receiver.receiveMessage(TIMEOUT_MILLISECONDS);
                stringMessage = new String(data);
                logger.info("Received message: " + stringMessage + "\n");
            } while (!stringMessage.equals("quit"));

            receiver.close();

        } catch (NumberFormatException e) {
            exitWithError("Invalid port number", e);
        } catch (Exception e) {
            e.printStackTrace();
            exitWithError("Error", e);
        }
    }

    /**
     * Exits the program with an error code and throws a RuntimeException with the specified message and exception.
     *
     * @param message   - error message.
     * @param exception - exception that caused the error.
     */
    private static void exitWithError(String message, Exception exception) {
        logger.error(message + ": " + exception.getMessage());
        exit(1);
    }
}

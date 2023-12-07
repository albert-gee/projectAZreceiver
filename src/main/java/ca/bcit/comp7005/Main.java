package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.exit;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int TIMEOUT_MILLISECONDS = 50000;

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String directoryPath = args[1];
        Receiver receiver = new Receiver(directoryPath);

        while (true) {
            try {
                receiver.run(port, TIMEOUT_MILLISECONDS);
            } catch (NumberFormatException e) {
                exitWithError("Invalid port number", e);
            } catch (DataTransferRestartException e) {
                logger.info("Connection restarted");
            } catch (Exception e) {
                exitWithError("Error", e);
            }
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

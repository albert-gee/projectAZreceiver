package ca.bcit.comp7005;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.exit;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);

            // Create a new receiver and start listening
            Receiver.listen(port);

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

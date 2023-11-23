package ca.bcit.comp7005;

public class Main {

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            Receiver receiver = new Receiver(port);

            System.out.println("Receiver started on port " + port);
            System.out.println("Waiting for a connection...");

            receiver.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

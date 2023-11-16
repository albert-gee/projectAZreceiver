package ca.bcit.comp7005;

import java.util.Scanner;

public class Main {

    private static final int PACKET_BUFFER_SIZE = 65535;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the port number: ");
        int port = scanner.nextInt();

        Receiver receiver = new Receiver(port, PACKET_BUFFER_SIZE);
        receiver.listen();


    }

}

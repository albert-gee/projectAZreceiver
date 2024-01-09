# Receiver

## Overview

**The Receiver** is a Java network application in "Project AZ". It reads data from a UDP socket and writes to the console:

- Establish connection by receiving a SYN packet and responding with SYN-ACK packet.
- Receive the data as a set of AZRP packets that can be delayed or lost.
- The data transfer ends when the length of all data received is equal to the length from the SYN packet.

The receiver sends an acknowledgement back to the sender (via the proxy).

The Receiver supports the following mime types and corresponding file extensions:

| MIME Type            | File Extension   |
|----------------------|-------------------|
| text/plain           | txt               |
| application/pdf      | pdf               |
| image/jpeg           | jpg               |
| image/png            | png               |
| application/json     | json              |
| application/xml      | xml               |
| application/msword   | doc               |
| application/vnd.ms-excel | xls            |


## Components

The application consists of four main components: `Main`, `AZRP`, `DataTransfer`, and `Receiver`.

### Main Class

This is the entry point of the application. It reads command-line arguments for the port and directory path, creates an instance of the `Receiver` class, and runs an infinite loop to continuously listen for incoming connections and handle exceptions appropriately.

### AZRP Class

Implements the AZRP protocol and represents an AZRP packet. It provides methods for packet serialization and deserialization, calculates and validates checksums for data integrity verification, and generates SYN and SYN-ACK packets.

It uses the CRC32 algorithm to calculate the checksum for data integrity verification.

The `toBytes` method serializes an AZRP object into a byte array, and the `fromBytes` method deserializes a byte array back into an AZRP object.

The class provides methods to generate SYN and SYN-ACK packets, including the initialization of sequence numbers and flags.

The class includes a method (`isChecksumValid`) to validate the checksum of received data.

### DataTransfer Class

Manages the transfer of data between sender and receiver using UDP:

- Accepts connections
- Receives SYN packets
- Sends SYN-ACK packets.
- Reads data packets, validates them, and sends ACK packets.
- Closes the socket when the transfer is complete.

### Receiver Class

Implements an abstraction for the communication with the sender by utilizing the `DataTransfer` class. It manages the reception of data on the receiver side:

- Accepts incoming connections
- Receives data packets
- Saves the data into a file or prints it on the screen depending on the file type.
- Handles timeouts and exceptions during data reception.
- Writes statistics to a log file, including the number of sent and received packets.
  

# "Project AZ" 

## Overview

"Project AZ" is an implementation of a custom reliable data transfer protocol named **AZRP**. This protocol is built on top of UDP and designed to provide reliability and efficient communication in diverse network environments.

The project comprises four key applications:

- **[Sender](https://github.com/albert-gee/projectAZsender):** The initiator of data transmission, responsible for sending packets using the AZRP protocol.
- **[Receiver](https://github.com/albert-gee/projectAZreceiver):** Responsible for receiving packets transmitted using the AZRP protocol.
- **[Proxy](https://github.com/albert-gee/projectAZproxy):** Acts as an intermediary between the Sender and Receiver, simulating a lossy network environment.
- **[GUI](https://github.com/albert-gee/projectAZproxy):** Provides a user-friendly interface for interacting with the AZRP protocol.

The project is compatible with both IPv4 and IPv6, ensuring that it can work in different network environments.

## AZRP Protocol

**AZRP** is built on top of UDP, leveraging its simplicity and low overhead. The integration ensures compatibility with existing UDP-based applications while enhancing reliability. By addressing the challenges inherent in UDP and incorporating sophisticated error recovery mechanisms, AZRP aims to provide a dependable solution for data transmission in diverse network environments.

### Packet Structure

Each packet consists of a header and a data payload. The header includes four integer fields:

- Sequence number
- Length
- Checksum
- Flags representing packet type (e.g., SYN, ACK)

The total size of the packet header is 19 bytes, and the maximum packet size is 1500 bytes.

### Packet Types

Flags in the header indicate the type of packet (SYN, ACK). The protocol supports four types of packets:

- **Data:** Used for transmitting data (both flags are false).
- **SYN (Synchronise):** Used for initiating a connection. Includes the initial sequence number, the length of the whole message to be sent, and the file extension in the data payload.
- **SYN-ACK (Synchronise-Acknowledge):** Used to acknowledge a SYN packet. It must include the same sequence number, length, checksum, and the file extension as the corresponding SYN packet.
- **ACK (Acknowledgement):** Used to acknowledge receiving a data packet. Must include the same sequence number and checksum as the corresponding data packet.

### Checksum Calculation

The checksum of the packet's data is calculated using the CRC32 algorithm. It is used for error detection, ensuring data integrity during transmission.

### Security

The `generateInitialSequenceNumber` method in the AZRP protocol ensures the secure creation of an initial sequence number for SYN packets. It employs the `SecureRandom` class to generate cryptographically secure random bytes, which are then combined to form a non-negative integer. This secure sequence number is vital for the AZRP protocol, enhancing the unpredictability and resistance to attacks during the establishment of connections, contributing to the overall security and reliability of the communication channel.

### Example of Usage

```java
// Creating a data packet
AZRP dataPacket = AZRP.data("Hello, World!".getBytes(), 1, 13);

// Serializing the packet to bytes
byte[] serializedData = dataPacket.toBytes();

// Deserializing the bytes back into an AZRP object
AZRP receivedPacket = AZRP.fromBytes(serializedData);
```

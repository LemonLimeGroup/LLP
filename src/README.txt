Names & Email Addresses:

    Se Yeon Kim             skim870@gatech.edu
    Yamilex Avila-Stanley   yjas3@gatech.edu

Class Name:             CS-3251
Section:                A
Date:                   11/23/2016
Assignment Title:       Sockets Programming Assignment 2

Name & Descriptions of All Files Submitted:

    FTA_Client.java
        * Start the client application with: java FTA_Client [ip_address_of_server] [port_number]
        * Able receive and send a file to the server. Has the following commands:

            Command: connect
                - Connects to the IP address and port number specified when you instantiated the class
                - After you instantiate the client, this command can be typed in to initiate the 3-way
                  handshake with the server.
                - NOTE: We handled multithreading in our server which means that multiple clients can
                  connect to the same server simultaneously.

            Command: get [filename]
                - Where [filename] is the name of the file to be downloaded from the server.
                - If the server does not have the specified file, then the client will receive
                  a response from the server that the file is not found.
                - When the file is downloaded, it will be titled "downloaded_[filename]" and will
                  be placed in the same directory as the *.java and *.class files

            Command: post [filename]
                - Upload a file named [filename] from the client to the server.
                - The file to be uploaded must be in the same directory as the source files.

            Command: window [max_window_size]
                - Specifies the maximum receiver's window size for the FTA_Client (in number of segments).

            Command: disconnect
                - Terminates the FTA_Client terminates gracefully from the FTA_Server.

    FTA_Server.java
        * Start the server application with: java FTA_Server [port_number]
        * The server can send files to the clients connected to it as well as have clients
          upload files to the server.
        * Then the following commands can be used:

            Command: window [max_window_size]
                - Specifies the maximum receiver's window size for the FTA_Server (in number of segments).

            Command: terminate
                - Shut down the FTA_Server gracefully.

    LLP_Packet.java
        * Represents a an LLP packet. Has functions for generating and checking the checksum
          as well as functions for getting/setting the header fields. Also has function to
          convert the header+data into a byte array to send over UDP.

    LLP_Socket.java
        * Provides API functions for our protocol. The functions exported are used both by the
          client and server applications. The commands the LLP_Socket provides
          can be found in the description of the LLP Protocol provided.

Compiling and Running our Program:

    1. Place any images you want the client/server to use inside the same directory as the source files
    2. Compile the program by typing in the command:
        javac *.java
    3. Start the FTA_Server with the command with any unused port number:
        java FTA_Server port_number
    4. Start the FTA_Client with a valid ip_address (can be IPv4 or IPv6) and port_number of the server like so:
        java FTA_Client ip_address port_number
    5. Now you can use the commands described in the file descriptions to proceed.

Any Known Bugs and Limitations of Design/Program:

    End of file determined by parsing the last 4 bytes of a received packet and checking if the last four bytes
    are "EOF4". If a packet is received which is not intended to be the last packet, but has the last four bytes
    as "EOF4" then our program will incorrectly think the entire file has been received. However, we are hoping
    this is highly unlikely to happen.

Extra Credit:

    * Server can handle multiple clients simultaneously (multithreaded)
    * POST method for the client; client can upload files to the server
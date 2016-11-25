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
                - Default window size is 1.


            Command: disconnect
                - Terminates the FTA_Client gracefully from the FTA_Server.

    FTA_Server.java
        * Start the server application with: java FTA_Server [port_number]
        * The server can send files to the clients connected to it as well as have clients
          upload files to the server.
        * Then the following commands can be used:

            Command: window [max_window_size]
                - Specifies the maximum receiver's window size for the FTA_Server (in number of segments).
                - Default window size is 1.

            Command: terminate
                - Shut down the FTA_Server gracefully.
		- (when called, client side needs to call some command to proceed terminating)
		- (disclaimer: several failed to receive may print out before server actually closes)

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

========================================================================================================
====================================== Sample Output ===================================================
========================================================================================================
(More output in output txt files)
IPv6 (Showing post, get, disconnect, window):

(Client Side)
root@8a640bf04c41:/home/CS3251/myfiles# ./netemtest.sh java FTA_Client fe80::42:acff:fe11:3 8000 -d
===== START TEST: Print out network emulation status
===== Expecting to see:
qdisc noqueue 0: root refcnt 2
===== otherwise you might need to cleanup your existing rules
qdisc noqueue 0: root refcnt 2 
===== TEST COMPLETED ==================================================================

===== START TEST: without network emulation
=== Ready to connect. ===
connect
=== Connecting... ===
SENDING SYN TO SERVER
WAITING FOR SYN-ACK FROM SERVER
Did not receive SYN_ACK. Waiting...
5��
SENDING ACK TO SERVER
Did not receive SYN_ACK. Waiting...
Timeout
RECEIVE WINDOW SIZE SET TO: 1
CONNECTION ESTABLISHED
get alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 0
7�`
packet received: 7�`
RECEIVED ACK FOR SEQ NUM: 1
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== Downloading File... ===
��Hi Sally :) It's me Yami!EOF
Received: ��Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 0
RECEIVED DATA: EXPECTED SEQ: 0RECEIVED SEQ 0
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
post alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 1
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 2
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== PACKETS TO BE SENT: 1 ===
SENT DATA 2
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 3
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
window 30
=== Window size set to: 30 ===
get alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 3
7�`
packet received: 7�`
RECEIVED ACK FOR SEQ NUM: 4
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== Downloading File... ===
�Hi Sally :) It's me Yami!EOF
Received: �Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 1
RECEIVED DATA: EXPECTED SEQ: 1RECEIVED SEQ 1
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
post alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 4
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 5
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== PACKETS TO BE SENT: 1 ===
SENT DATA 5
7�`
packet received: 7�`
RECEIVED ACK FOR SEQ NUM: 6
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
disconnect
SENDING FIN
FIN-WAIT-1 STATE. WAITING FOR FIN OR ACK.
7� 
ACK received. Waiting for FIN.
Did not receive FIN. Waiting...
>��
Received FIN. Sending ACK.
Did not receive FIN. Waiting...
Timeout
Timed-Wait State.
===== TEST COMPLETED ==================================================================

(Server Side)
root@438dc937ae55:/home/CS3251_Proj2/myfiles# ./netemtest.sh java FTA_Server 8000 -d
===== START TEST: Print out network emulation status
===== Expecting to see:
qdisc noqueue 0: root refcnt 2
===== otherwise you might need to cleanup your existing rules
qdisc noqueue 0: root refcnt 2 
===== TEST COMPLETED ==================================================================

===== START TEST: without network emulation
WAITING FOR SYN FROM CLIENT
Did not receive SYN. Waiting...
=��
RECEIVE WINDOW SIZE SET TO: 1
SEND A SYN-ACK TO CLIENT
Receive ACK from Client.
Did not receive ACK. Waiting...
7��
CONNECTION ACCEPTED
New thread
WAITING FOR SYN FROM CLIENT
Did not receive SYN. Waiting...
=== New Thread Started ===
"9@alice.txt
Received: "9@alice.txt
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 0
RECEIVED DATA: EXPECTED SEQ: 0RECEIVED SEQ 0
alice.txt
alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 0
7� 
packet received: 7� 
RECEIVED ACK FOR SEQ NUM: 1
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== New Thread Started ===
!��alice.txt
Received: !��alice.txt
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 1
RECEIVED DATA: EXPECTED SEQ: 1RECEIVED SEQ 1
alice.txt
RECEIVED FILENAME alice.txt
��Hi Sally :) It's me Yami!EOF
Received: ��Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 2
RECEIVED DATA: EXPECTED SEQ: 2RECEIVED SEQ 2
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
=== New Thread Started ===
"0�alice.txt
Received: "0�alice.txt
RECEIVE WINDOW SIZE SET TO: 30
RECEIVED SEQ 3
RECEIVED DATA: EXPECTED SEQ: 3RECEIVED SEQ 3
alice.txt
alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 1
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 2
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== New Thread Started ===
window 30
Setting window size to: 30
!�alice.txt
Received: !�alice.txt
RECEIVE WINDOW SIZE SET TO: 30
RECEIVED SEQ 4
RECEIVED DATA: EXPECTED SEQ: 4RECEIVED SEQ 4
alice.txt
RECEIVED FILENAME alice.txt
��Hi Sally :) It's me Yami!EOF
Received: ��Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 30
RECEIVED SEQ 5
RECEIVED DATA: EXPECTED SEQ: 5RECEIVED SEQ 5
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
=== New Thread Started ===
>�D
Received: >�D
RECEIVE WINDOW SIZE SET TO: 30
Received FIN. Trying to send ACK
Sending ACK. In CLOSE-WAIT state.
Sending FIN
Successfully sent FIN. In LAST-ACK state.
Did not receive ACK. Waiting...
7��
Received ACK for FIN. Closing...
CLOSED

IPv4 (post, get, window, disconnect):
(Client Side)
root@6ff4b38a6509:/home/CS3251_Proj2/myfiles# ./netemtest.sh java FTA_Client 172.17.0.3 8000 -d
===== START TEST: Print out network emulation status
===== Expecting to see:
qdisc noqueue 0: root refcnt 2
===== otherwise you might need to cleanup your existing rules
qdisc noqueue 0: root refcnt 2 
===== TEST COMPLETED ==================================================================

===== START TEST: without network emulation
=== Ready to connect. ===
connect
=== Connecting... ===
SENDING SYN TO SERVER
WAITING FOR SYN-ACK FROM SERVER
Did not receive SYN_ACK. Waiting...
5��
SENDING ACK TO SERVER
Did not receive SYN_ACK. Waiting...
Timeout
RECEIVE WINDOW SIZE SET TO: 1
CONNECTION ESTABLISHED
get alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 0
7�`
packet received: 7�`
RECEIVED ACK FOR SEQ NUM: 1
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== Downloading File... ===
��Hi Sally :) It's me Yami!EOF
Received: ��Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 0
RECEIVED DATA: EXPECTED SEQ: 0RECEIVED SEQ 0
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
post alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 1
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 2
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== PACKETS TO BE SENT: 1 ===
SENT DATA 2
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 3
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
window 30
=== Window size set to: 30 ===
get alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 3
7�`
packet received: 7�`
RECEIVED ACK FOR SEQ NUM: 4
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== Downloading File... ===
�Hi Sally :) It's me Yami!EOF
Received: �Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 1
RECEIVED DATA: EXPECTED SEQ: 1RECEIVED SEQ 1
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
post alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 4
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 5
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== PACKETS TO BE SENT: 1 ===
SENT DATA 5
7�`
packet received: 7�`
RECEIVED ACK FOR SEQ NUM: 6
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
disconnect
SENDING FIN
FIN-WAIT-1 STATE. WAITING FOR FIN OR ACK.
7� 
ACK received. Waiting for FIN.
Did not receive FIN. Waiting...
>��
Received FIN. Sending ACK.
Did not receive FIN. Waiting...
Timeout
Timed-Wait State.
===== TEST COMPLETED ==================================================================

(Server Side)
root@438dc937ae55:/home/CS3251_Proj2/myfiles# ./netemtest.sh java FTA_Server 8000 -d
===== START TEST: Print out network emulation status
===== Expecting to see:
qdisc noqueue 0: root refcnt 2
===== otherwise you might need to cleanup your existing rules
qdisc noqueue 0: root refcnt 2 
===== TEST COMPLETED ==================================================================

===== START TEST: without network emulation
WAITING FOR SYN FROM CLIENT
Did not receive SYN. Waiting...
=��
RECEIVE WINDOW SIZE SET TO: 1
SEND A SYN-ACK TO CLIENT
Receive ACK from Client.
Did not receive ACK. Waiting...
7��
CONNECTION ACCEPTED
New thread
WAITING FOR SYN FROM CLIENT
Did not receive SYN. Waiting...
=== New Thread Started ===
"9@alice.txt
Received: "9@alice.txt
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 0
RECEIVED DATA: EXPECTED SEQ: 0RECEIVED SEQ 0
alice.txt
alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 0
7� 
packet received: 7� 
RECEIVED ACK FOR SEQ NUM: 1
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== New Thread Started ===
!��alice.txt
Received: !��alice.txt
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 1
RECEIVED DATA: EXPECTED SEQ: 1RECEIVED SEQ 1
alice.txt
RECEIVED FILENAME alice.txt
��Hi Sally :) It's me Yami!EOF
Received: ��Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 2
RECEIVED DATA: EXPECTED SEQ: 2RECEIVED SEQ 2
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
=== New Thread Started ===
"0�alice.txt
Received: "0�alice.txt
RECEIVE WINDOW SIZE SET TO: 30
RECEIVED SEQ 3
RECEIVED DATA: EXPECTED SEQ: 3RECEIVED SEQ 3
alice.txt
alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 1
7��
packet received: 7��
RECEIVED ACK FOR SEQ NUM: 2
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== New Thread Started ===
window 30
Setting window size to: 30
!�alice.txt
Received: !�alice.txt
RECEIVE WINDOW SIZE SET TO: 30
RECEIVED SEQ 4
RECEIVED DATA: EXPECTED SEQ: 4RECEIVED SEQ 4
alice.txt
RECEIVED FILENAME alice.txt
��Hi Sally :) It's me Yami!EOF
Received: ��Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 30
RECEIVED SEQ 5
RECEIVED DATA: EXPECTED SEQ: 5RECEIVED SEQ 5
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
=== New Thread Started ===
>�D
Received: >�D
RECEIVE WINDOW SIZE SET TO: 30
Received FIN. Trying to send ACK
Sending ACK. In CLOSE-WAIT state.
Sending FIN
Successfully sent FIN. In LAST-ACK state.
Did not receive ACK. Waiting...
7��
Received ACK for FIN. Closing...
CLOSED

(Client Side: connect, get, disconnect)
===== START TEST: drop 20% of packets outbound

=== Ready to connect. ===
connect
=== Connecting... ===
SENDING SYN TO SERVER
WAITING FOR SYN-ACK FROM SERVER
Did not receive SYN_ACK. Waiting...
5��
SENDING ACK TO SERVER
Did not receive SYN_ACK. Waiting...
5��
SENDING ACK TO SERVER
Did not receive SYN_ACK. Waiting...
5��
SENDING ACK TO SERVER
Did not receive SYN_ACK. Waiting...
Timeout
RECEIVE WINDOW SIZE SET TO: 1
CONNECTION ESTABLISHED
get alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 0
Timeout
Re-transmitting Packets...
Resending packet Seq: 0
Timeout
Re-transmitting Packets...
Resending packet Seq: 0
Timeout
Re-transmitting Packets...
Resending packet Seq: 0
Timeout
Re-transmitting Packets...
Resending packet Seq: 0
7�`
packet received: 7�`
RECEIVED ACK FOR SEQ NUM: 1
SEQ NUM OF LAST PACKET: 1
=== SENDING COMPLETE ===
=== Downloading File... ===
��Hi Sally :) It's me Yami!EOF
Received: ��Hi Sally :) It's me Yami!EOF
RECEIVE WINDOW SIZE SET TO: 1
RECEIVED SEQ 0
RECEIVED DATA: EXPECTED SEQ: 0RECEIVED SEQ 0
Hi Sally :) It's me Yami!EOF
Timeout
Timeout
=== FILE DOWNLOAD COMPLETE ===
disconnect
SENDING FIN
FIN-WAIT-1 STATE. WAITING FOR FIN OR ACK.
7��
ACK received. Waiting for FIN.
Did not receive FIN. Waiting...
>��
Received FIN. Sending ACK.
Did not receive FIN. Waiting...
7��
packet seq num: 1 Ack num: 2
>��
Did not receive FIN. Waiting...
Timeout
Timed-Wait State.
CLOSED
===== TEST COMPLETED ==================================================================

Terminate:
(Client Side)

qdisc netem 8023: root refcnt 2 limit 1000 loss 5% corrupt 1%
===== TEST COMPLETED ==================================================================

===== START TEST: without network emulation
=== Ready to connect. ===
connect
=== Connecting... ===
SENDING SYN TO SERVER
WAITING FOR SYN-ACK FROM SERVER
Did not receive SYN_ACK. Waiting...
5��
SENDING ACK TO SERVER
Did not receive SYN_ACK. Waiting...
Timeout
RECEIVE WINDOW SIZE SET TO: 1
CONNECTION ESTABLISHED
get alice.txt
=== PACKETS TO BE SENT: 1 ===
SENT DATA 0
>��
packet received: >��
FIN RECEIVED.
Received FIN. Trying to send ACK
Sending ACK. In CLOSE-WAIT state.
Sending FIN
Successfully sent FIN. In LAST-ACK state.
Did not receive ACK. Waiting...
7� 
Received ACK for FIN. Closing...
CLOSED
Server closed.
===== TEST COMPLETED ==================================================================

(Server)
===== TEST COMPLETED ==================================================================

===== START TEST: without network emulation
WAITING FOR SYN FROM CLIENT
Did not receive SYN. Waiting...
=��
RECEIVE WINDOW SIZE SET TO: 1
SEND A SYN-ACK TO CLIENT
Receive ACK from Client.
Did not receive ACK. Waiting...
7��
CONNECTION ACCEPTED
New thread
WAITING FOR SYN FROM CLIENT
Did not receive SYN. Waiting...
=== New Thread Started ===
terminate
Setting terminate for all of the sockets connected to a client
SENDING FIN
FIN-WAIT-1 STATE. WAITING FOR FIN OR ACK.
"9@alice.txt
7�`
ACK received. Waiting for FIN.
Did not receive FIN. Waiting...
>�
Received FIN. Sending ACK.
Did not receive FIN. Waiting...
Timeout
Did not receive ACK. Waiting...
Timeout
Timed-Wait State.
CLOSED
Successfully terminated the server.
Failed to receive. Retrying...
Failed to receive. Retrying...
Failed to receive. Retrying...===== TEST COMPLETED ==================================================================

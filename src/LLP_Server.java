import javax.xml.soap.SOAPPart;
import java.io.*;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Sally, Yami on 11/9/16.
 */
public  class LLP_Server {
    private static boolean debug = false;
    private static ArrayList<Thread> clientThread = new ArrayList<>();

    public static void terminate(LLP_Socket socket) {
        ArrayList<LLP_Socket> clientList =LLPThread.getClients();
        //TODO: need to fix client side so that it can receive FIN from server
        for (int iClient = 0; iClient < clientList.size(); iClient++) {
            clientList.remove(iClient).close();
        }
        socket.closeServer();
        printDebug("Successfully terminated the server.");
        // TODO: lots of "Failed to receive. Retrying..." because of threading
        System.exit(0);
    }
    public static void setDebug() {
        debug = true;
    }

    public static void window(LLP_Socket socket, int num) {
        socket.setWindowSize(num);
    }
    private static class LLPThread extends Thread {
        private FileInputStream fis;
        private BufferedInputStream bis;
        private LLP_Socket conn;
        private static ArrayList<LLP_Socket> clients = new ArrayList<>();
        private boolean terminate;

        public LLPThread(LLP_Socket conn) {
            super();
            fis = null;
            bis = null;
            this.conn = conn;
            terminate = false;
            clients.add(conn);
        }

        public static ArrayList<LLP_Socket> getClients() {
            return clients;
        }
        public void run() {
            while (!terminate) {
                System.out.println("In thread yay!");
                System.out.println("is closed? " + conn.isClosed());
                byte[] bytes = conn.receive(1024);
                //Arays.equals(bytes, "closed".getBytes())
                if (bytes == null) {
                    clients.remove(conn);
                    return;
                }
                String filename = new String(bytes);
                System.out.println(filename);

                File file = new File(filename);
                byte[] mybytearray = new byte[(int) file.length() + 1];
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                bis = new BufferedInputStream(fis);
                try {
                    bis.read(mybytearray, 0, mybytearray.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mybytearray[mybytearray.length - 1] = 4;
                try {
                    conn.send(mybytearray);
                } catch (SocketException e) {
                    System.out.println("Send Failed.");
                }
            }
        }
        public LLP_Socket getConnSocket() {
            return conn;
        }
        public void setTerminate() {
            terminate = true;
        }
    }

    private static class CommandThread extends Thread {
        private Scanner sc;
        private LLP_Socket serverSocket;
        private ArrayList<LLP_Socket> clients;
        public CommandThread(Scanner sc, LLP_Socket serverSocket) {
            super();
            this.sc = sc;
            this.serverSocket = serverSocket;
        }
        public void run() {
            String input = sc.next().toLowerCase();
            clients = LLPThread.getClients();
            switch (input) {
                case "window":
                    for (int iClient = 0; iClient < clients.size(); iClient++) {
                        window(clients.get(iClient), Integer.parseInt(sc.next()));
                    }
                    break;
                case "terminate":
                    terminate(serverSocket);
                    break;
                default:
                    System.out.println("Command not recognized.");
            }
        }
    }
    public static void main(String[] args) {
        if (args.length > 2) {
            System.out.println("Invalid arguments");
            System.exit(-1);
        }
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number passed in.");
            System.exit(-1);
        }
        if (port > 65535 || port <= 1024) {
            System.out.println("Invalid port number passed in.");
            System.exit(-1);
        }
        if (args.length == 2) {
            if ("-d".equals(args[1])) {
                setDebug();
            } else {
                System.out.println("Invalid arguments");
                System.exit(-1);
            }
        }
        LLP_Socket serverSocket = new LLP_Socket(port, debug);
        Scanner sc = new Scanner(System.in);
        Thread command = new CommandThread(sc, serverSocket);
        command.start();

        while (true) {
            LLP_Socket conn = serverSocket.accept();
            //TODO: Multithreading
            Thread thread = new LLPThread(conn);
            System.out.println("New thread");
            clientThread.add(thread);
            thread.start();
        }



    }

    private static void printDebug(String statement) {
        if (debug) {
            System.out.println(statement);
        }
    }
}
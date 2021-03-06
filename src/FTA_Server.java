import java.io.*;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Created by Sally, Yami on 11/9/16.
 */
public  class FTA_Server {
    private static boolean debug = false;
    private static ArrayList<Thread> clientThread = new ArrayList<>();

    public static void terminate(LLP_Socket socket) {
        ArrayList<LLP_Socket> clientList =LLPThread.getClients();
        for (int iClient = 0; iClient < clientList.size(); iClient++) {
            printDebug("Setting terminate for all of the sockets connected to a client");
            ((LLPThread) clientThread.get(iClient)).setTerminate();
        }
        for (int iClient = 0; iClient < clientList.size(); iClient++) {
            clientList.remove(iClient).close();
        }
        printDebug("Before closing server socket");
        socket.closeServer();
        printDebug("Successfully terminated the server.");
        System.exit(0);
    }
    public static void setDebug() {
        debug = true;
    }

    public static void window(LLP_Socket socket, int num) {
        System.out.println("Setting window size to: " + num);
        socket.setMyWindowSize(num);
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
                printDebug("=== New Thread Started ===");
                byte[] bytes = conn.receive(1024);
                if (terminate || bytes == null || bytes.length < 1) {
                    clients.remove(conn);
                    return;
                }

                if (bytes[0] == 1) { // POST
                    FileOutputStream out = null;

                    // Parse Filename
                    byte[] fileLocBytes = Arrays.copyOfRange(bytes, 1, bytes.length);
                    String fileloc = new String(fileLocBytes);
                    printDebug("RECEIVED FILENAME " + fileloc);

                    // Receive file
                    try {
                        out = new FileOutputStream("downloaded_" + fileloc);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    boolean eof = false;

                    while (!eof) {
                        byte[] buff = conn.receive(1024);
                        if (buff != null && buff.length > 0) { // since receive may return null
                            try {
                                if (buff[buff.length - 1] == 4
                                        && buff[buff.length - 2] == 'F'
                                        && buff[buff.length - 3] == 'O'
                                        && buff[buff.length - 4] == 'E') {
                                    out.write(buff, 0, buff.length - 4);
                                    conn.setTimeout(true);
                                } else if (Arrays.equals(buff, "timeout".getBytes())) {
                                    // timeout
                                    printDebug("Timeout");
                                    System.out.println("=== FILE DOWNLOAD COMPLETE ===");
                                    eof = true;
                                } else {
                                    out.write(buff, 0, buff.length);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else if (buff == null) {
                            printDebug("Server closed."); //TODO: think more about this case
                            try {
                                out.close();
                                new File("downloaded_" + fileloc).delete();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            System.exit(0);
                        } else {
                            //empty array
                            printDebug("Discarded packets");
                        }
                    }
                    conn.setTimeout(false);
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // GET
                    byte[] fileLocBytes = Arrays.copyOfRange(bytes, 1, bytes.length);
                    String filename = new String(fileLocBytes);
                    System.out.println(filename);

                    File file = new File(filename);
                    byte[] mybytearray = new byte[(int) file.length() + 4];
                    try {
                        fis = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        try {
                            conn.send("filenotfound".getBytes());
                        } catch (SocketException e1) {
                            printDebug("Failed to catch");
                        }
                        continue;
                    }
                    bis = new BufferedInputStream(fis);
                    try {
                        bis.read(mybytearray, 0, mybytearray.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mybytearray[mybytearray.length - 1] = 4;
                    mybytearray[mybytearray.length - 2] = 'F';
                    mybytearray[mybytearray.length - 3] = 'O';
                    mybytearray[mybytearray.length - 4] = 'E';

                    try {
                        conn.send(mybytearray);
                    } catch (SocketException e) {
                        System.out.println("Send Failed.");
                    }
                }
            }
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
            while (true) {
                String input = sc.next().toLowerCase();
                clients = LLPThread.getClients();
                switch (input) {
                    case "window":
                        try {
                            int windowSz = sc.nextInt();
                            if (windowSz <= 0) {
                                System.out.println("Window size must be a positive integer.");
                                break;
                            }
                            for (int iClient = 0; iClient < clients.size(); iClient++) {
                                window(clients.get(iClient), windowSz);
                            }
                            break;
                        } catch (InputMismatchException e) {
                            System.out.println("Not a valid window size. Window size must be an integer.");
                            break;
                        }
                    case "terminate":
                        terminate(serverSocket);
                        break;
                    default:
                        System.out.println("Command not recognized.");
                }
            }
        }
    }
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Not enough input");
            System.exit(-1);
        }
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
            Thread thread = new LLPThread(conn);
            printDebug("New thread");
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
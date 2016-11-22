import javax.xml.soap.SOAPPart;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Sally, Yami on 11/9/16.
 */
public  class LLP_Server {
    private static boolean debug = false;

    public static void window(LLP_Socket socket, int num) {
        socket.setWindowSize(num);
    }

    public static void terminate(LLP_Socket socket) {
        ArrayList<LLP_Socket> clientList =LLPThread.getClients();
        //TODO: need to fix client side so that it can receive FIN from server
        for (int iClient = 0; iClient < clientList.size(); iClient++) {
            clientList.remove(iClient).close();
        }
        socket.closeServer();
        printDebug("Successfully terminated the server.");
        // TODO: lots of "Failed to receive. Retyring..." because of threading
        System.exit(0);
    }
    public static void setDebug() {
        debug = true;
    }

    private static class LLPThread extends Thread {
        private FileInputStream fis;
        private BufferedInputStream bis;
        private LLP_Socket conn;
        private static ArrayList<LLP_Socket> clients = new ArrayList<>();

        public LLPThread(LLP_Socket conn) {
            super();
            fis = null;
            bis = null;
            this.conn = conn;
            clients.add(conn);
        }
        public static ArrayList<LLP_Socket> getClients() {
            return clients;
        }
        public void run() {
            while (true) {
                System.out.println("In thread yay!");
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
                conn.send(mybytearray);
            }
        }
    }

    private static class CommandThread extends Thread {
        private Scanner sc;
        private LLP_Socket serverSocket;
        public CommandThread(Scanner sc, LLP_Socket serverSocket) {
            super();
            this.sc = sc;
            this.serverSocket = serverSocket;
        }
        public void run() {
            String input = sc.next().toLowerCase();
            switch (input) {
                case "window":
                    window(serverSocket, Integer.parseInt(sc.next()));
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
            thread.start();
        }



    }

    private static void printDebug(String statement) {
        if (debug) {
            System.out.println(statement);
        }
    }
}
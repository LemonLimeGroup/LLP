import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Created by Sally, Yami on 11/12/16.
 */
public class FTA_Client {
    LLP_Socket socket;
    private InetAddress ipAddress;
    private int port;
    boolean debug;

    public FTA_Client(){
        this(null, 0);
    }

    public FTA_Client(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        debug = false;
        // TODO: add bind
    }
    public void setDebug() {
        debug = true;
    }

    public void connect() {
        socket = new LLP_Socket(debug);
        socket.connect(ipAddress, port);
    }

    public void setWindowSize(int windowSize) {
        System.out.println("=== Window size set to: " + windowSize + " ===");
        socket.setMyWindowSize(windowSize);
    }

    public void get(String fileloc){
        if (fileloc == null) {
            printDebug("File cannot be null.");
            return;
        }

        byte[] filelocBytes = fileloc.getBytes();
        byte[] getByte = {0};
        byte[] getFile = new byte[fileloc.getBytes().length + 1]; // [1, .. filename ..]
        System.arraycopy(getByte, 0, getFile, 0, getByte.length);
        System.arraycopy(filelocBytes, 0, getFile, getByte.length, filelocBytes.length);

        try {
            socket.send(getFile);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        FileOutputStream out = null;

        // Receive file
        try {
            out = new FileOutputStream("downloaded_" + fileloc);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        boolean eof = false;

        System.out.println("=== Downloading File... ===");
        while (!eof) {
            byte[] buff = socket.receive(1024);
            if (buff != null && buff.length > 0) { // since receive may return null
                try {
                    if (buff[buff.length - 1] == 4
                            && buff[buff.length - 2] == 'F'
                            && buff[buff.length - 3] == 'O'
                            && buff[buff.length - 4] == 'E') {
                        out.write(buff, 0, buff.length - 4);
                        socket.setTimeout(true);
                    } else if (Arrays.equals(buff, "timeout".getBytes())) {
                        // timeout
                        printDebug("Timeout");
                        System.out.println("=== FILE DOWNLOAD COMPLETE ===");
                        eof = true;
                    } else if (Arrays.equals(buff, "filenotfound".getBytes())){
                        System.out.println("This file does not exist. Please try another file.");
                        out.close();
                        new File("downloaded_" + fileloc).delete();
                        eof = true;
                    } else {
                        out.write(buff, 0, buff.length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (buff == null) {
                printDebug("Server closed.");
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
        socket.setTimeout(false);
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void post(String fileloc){
        FileInputStream fis;
        BufferedInputStream bis;

        File file = new File(fileloc);
        byte[] mybytearray = new byte[(int) file.length() + 4];
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            try {
                socket.send("filenotfound".getBytes());
            } catch (SocketException e1) {
                e1.printStackTrace();
            }
            return;
        }

        byte[] filelocBytes = fileloc.getBytes();
        byte[] getByte = {1};
        byte[] postFile = new byte[fileloc.getBytes().length + 1]; // [1, .. filename ..]
        System.arraycopy(getByte, 0, postFile, 0, getByte.length);
        System.arraycopy(filelocBytes, 0, postFile, getByte.length, filelocBytes.length);

        // Notify server of post
        try {
            socket.send(postFile);
        } catch (SocketException e) {
            e.printStackTrace();
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
            socket.send(mybytearray);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        socket.close();
    }
    private void printDebug(String statement) {
        if (debug) {
            System.out.println(statement);
        }
    }

    public static void main(String[] args) {
        if (args.length > 3) {
            System.out.println("Invalid arguments");
            System.exit(-1);
        }
        //parse command line args
        FTA_Client client = new FTA_Client();

        try{
            client = new FTA_Client(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
        } catch (UnknownHostException e){
            System.err.println("Caught UnknownHostException " + e.getMessage());
            System.exit(-1);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number passed in.");
            System.exit(-1);
        }
        if (args.length > 2) {
            if (args[2].equals("-d")) {
                client.setDebug();
            } else {
                System.out.println("Invalid argument(s) passed in.");
                System.exit(-1);
            }
        }

        boolean exit = false;
        boolean isConnected = false;
        System.out.println("=== Ready to connect. ===");
        while(!exit) {
            Scanner sc = new Scanner(System.in);
            String input = sc.next().toLowerCase();
            if (!isConnected) {
                switch (input) {
                    case "connect":
                        System.out.println("=== Connecting... ===");
                        client.connect();
                        isConnected = true;
                        break;
                    default:
                        System.out.println("Command not recognized.");
                }
            } else {
                switch (input) {
                    case "post":
                        client.post(sc.next());
                        break;
                    case "get":
                        client.get(sc.next());
                        break;
                    case "connect":
                        System.out.println("Connection already established.");
                        break;
                    case "disconnect":
                        client.disconnect();
                        exit = true;
                        break;
                    case "window":
                        try {
                            int window = sc.nextInt();
                            if (window <= 0) {
                                System.out.println("Window size must be a positive integer.");
                                break;
                            }
                            client.setWindowSize(window);
                            break;
                        } catch(InputMismatchException e) {
                            System.out.println("Not a valid window size. Window size must be an integer.");
                            break;
                        }
                    default:
                        System.out.println("Command not recognized.");
                }
            }
        }
    }
}
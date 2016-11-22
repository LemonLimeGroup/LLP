import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Sally, Yami on 11/12/16.
 */
public class LLP_Client {
    LLP_Socket socket;
    private InetAddress ipAddress;
    private int port;
    boolean debug;

    public LLP_Client(){
        this(null, 0);
    }

    public LLP_Client(InetAddress ipAddress, int port) {
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

    public void get(String fileloc){
        socket.send(fileloc.getBytes());
        FileOutputStream out = null;

        // Receive file
        try {
            out = new FileOutputStream("downloaded_" + fileloc);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        boolean eof = false;

        while (!eof) {
            byte[] buff = socket.receive(1024);
            if (buff != null && buff.length > 0) { // since receive may return null
                try {
                    if(buff[buff.length-1] == 4){
                        eof = true;
                        out.write(buff, 0, buff.length-1);
                    }else{
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
            }
        }
        System.out.println("FILE DOWNLOAD COMPLETE");
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void post(String fileloc){
        // extra credit
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
        LLP_Client client = new LLP_Client();

        try{
            client = new LLP_Client(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
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
        while(!exit) {
            Scanner sc = new Scanner(System.in);
            String input = sc.next().toLowerCase();
            switch (input){
                case "post": client.post(sc.next()); break;
                case "get": client.get(sc.next()); break;
                case "connect": client.connect(); break;
                case "disconnect": client.disconnect(); exit = true; break;
                default: System.out.println("Command not recognized.");
            }
        }
    }
}
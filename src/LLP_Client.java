import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Client {
    LLP_Socket socket;
    private InetAddress ipAddress;
    private int port;
    boolean isConnected; //??

    public LLP_Client(){
        this.ipAddress = null;
        this.port = 0;
    }

    public LLP_Client(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        // TODO: add bind
    }

    public void connect() {
        socket = new LLP_Socket();
        socket.connect(ipAddress, port);
    }

    public void get(String fileloc){
        socket.send(fileloc.getBytes());
//        byte[] buff = new byte[1024];
//        ByteArrayInputStream in = new ByteArrayInputStream(buff); // should be from socket buffer I think
//        int data;
//        try {
//            while((data = in.read(buff)) > 0){
//                socket.send(buff); //wat
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void post(String fileloc){
        // extra credit
    }

    public void disconnect() {
        socket.close();
    }

    public static void main(String[] args) {

        //parse command line args
        LLP_Client client = new LLP_Client();

        try{
            client = new LLP_Client(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
        } catch (UnknownHostException e){
            System.err.println("Caught UnknownHostException " + e.getMessage());
            System.exit(-1);
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
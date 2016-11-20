import javax.xml.soap.SOAPPart;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Sally, Yami on 11/9/16.
 */
public  class LLP_Server {
    public static void window(LLP_Socket socket, int num) {
        socket.setWindowSize(num);
    }

    public static void terminate(LLP_Socket socket) {
        socket.close();
    }

    public static void start(LLP_Socket conn) {

        byte[] bytes = conn.receive(1024);

        String data = new String(bytes);
        System.out.println(data);
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            System.out.println("Invalid arguments");
            System.exit(-1);
        }
        int port = Integer.parseInt(args[0]);
        boolean debug = false;
        if (args.length == 2) {
            if ("-d".equals(args[1])) {
                debug = true;
            } else {
                System.out.println("Invalid arguments");
                System.exit(-1);
            }
        }
        LLP_Socket serverSocket = new LLP_Socket(port, debug);
        while (true) {
            LLP_Socket conn = serverSocket.accept();
            //TODO: Multithreading
            Thread thread = new Thread() {
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                public void run() {
                    while (true) {
                        System.out.println("In thread yay!");
                        byte[] bytes = conn.receive(1024);
                        if (bytes == null) {
                            return;
                        }
                        String filename = new String(bytes);

                        File file = new File(filename);
                        byte[] mybytearray = new byte[(int)file.length()+1];
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
                        mybytearray[mybytearray.length-1] = 4;
                        conn.send(mybytearray);
                    }
                }
            };
            System.out.println("New thread");
            thread.start();
        }

//        Scanner sc = new Scanner(System.in);
//        String input = sc.next().toLowerCase();
//        switch (input) {
//            case "window":
//                window(serverSocket, Integer.parseInt(sc.next()));
//                break;
//            case "terminate":
//                terminate(serverSocket);
//                break;
//            default:
//                System.out.println("Command not recognized.");
//        }


    }
}
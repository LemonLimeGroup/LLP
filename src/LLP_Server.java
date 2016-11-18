import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Sally on 11/9/16.
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
//        System.out.println(bytes.length);
//        for(int i = 0; i < bytes.length / 2; i++)
//        {
//            byte temp = bytes[i];
//            bytes[i] = bytes[bytes.length - i - 1];
//            bytes[bytes.length - i - 1] = temp;
//        }
        String data = new String(bytes);
        System.out.println(data);
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        LLP_Socket serverSocket = new LLP_Socket(port);
        while (true) {
            LLP_Socket conn = serverSocket.accept();
            //TODO: Multithreading
            Thread thread = new Thread() {
                public void run() {
                    while (true) {
                        System.out.println("In thread yay!");
                        byte[] bytes = conn.receive(1024);
                        String data = new String(bytes);
                        System.out.println(data);
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
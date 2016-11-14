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
    LLP_Socket socket;
    private int port;

    public LLP_Server(int port) {
        this.port = port;
    }

    public void window(int num) {
        socket.setWindowSize(num);
    }

    public void terminate() {
        socket.close();
    }

    public void start() {
        socket = new LLP_Socket(port);
        socket.accept();
        byte[] bytes = socket.receive(1024);
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
        LLP_Server server = new LLP_Server(port);
        server.start();

        Scanner sc = new Scanner(System.in);
        String input = sc.next().toLowerCase();
        switch (input) {
            case "window":
                server.window(Integer.parseInt(sc.next()));
                break;
            case "terminate":
                server.terminate();
                break;
            default:
                System.out.println("Command not recognized.");
        }


    }
}
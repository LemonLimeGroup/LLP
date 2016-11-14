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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(true) {
            baos.write(socket.receive(1024), 0, 1024);
            System.out.println(new String(baos.toByteArray(), StandardCharsets.UTF_8));
        }
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
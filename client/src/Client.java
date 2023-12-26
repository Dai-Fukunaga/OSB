import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws Exception {
        try {
            // int PORT = Integer.parseInt(args[0]);
            int PORT = 8080;
            InetAddress inetAddress = InetAddress.getByName("localhost");
            System.out.println("IP address: " + inetAddress);
            InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, PORT);
            Socket socket = new Socket();
            socket.connect(socketAddress, 10000);
            InetAddress inadr;
            if ((inadr = socket.getInetAddress()) != null) {
                System.out.println("Connect to " + inadr);
            } else {
                System.out.println("Connection failed");
                socket.close();
                return;
            }

            String msg = "test";
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                    true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer.println(msg);
            writer.flush();
            String getLine = reader.readLine();
            System.out.println("Message from Server: " + getLine);
            writer.close();
            socket.close();
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
}

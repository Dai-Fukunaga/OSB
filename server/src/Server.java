import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws Exception {
        // int PORT = Integer.parseInt(args[0]);
        int PORT = 8080;
        ServerSocket serverSocket = new ServerSocket(PORT);
        try {
            System.out.println("Wait for the connection...");
            while (true) {
                Socket socket = serverSocket.accept();
                new ServerThread(socket).start();
                System.out.println("Waiting for new connection");
            }
        } catch (Error error) {
            error.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }
}

class ServerThread extends Thread {
    private Socket socket;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            String line;
            line = reader.readLine();
            System.out.println(line);
            writer.println("Message is received");
        } catch (IOException error) {
            error.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }
};

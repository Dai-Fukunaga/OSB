import java.io.*;
import java.net.*;

public class FileServer {
    static int PORT = 8080;

    public static void main(String[] args) throws IOException {
        ServerSocket s = new ServerSocket(PORT);
        try{
            System.out.println("Wait for the connection...");
            while (true) {
                Socket socket = s.accept();
                new ServerThread(socket).start();
                System.out.println("Waiting for new connection");
            }
        } catch (Error e) {
            System.err.println(e);
        } finally {
            try {
                if(s != null) {
                    s.close();
                }
            } catch (IOException e) {
                System.err.println(e);
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
            String line;
            line = reader.readLine();
            System.out.println(line);
            String[] msg = line.split(":");
            if (msg[0].equals("save")) {
                /* クライアントからfileを受け取る */
                int result = receive(msg[1], socket);
                System.out.println("result = " + result);
            } else if(msg[0].equals("fetch")) {
                try {
                    FileInputStream fileInputStream = new FileInputStream(msg[1]);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    fileInputStream.close();
                    byteArrayOutputStream.close();
                    byte[] fileContent = byteArrayOutputStream.toByteArray();
                    // ファイルの内容をサーバーに送信
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(fileContent);
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            reader.close();
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

    public static int receive(String fileName, Socket socket) throws IOException {
        if (socket.isClosed()) {
            System.out.println("Socket is closed!!");
            return -1;
        }
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        byte[] fileContent = byteArrayOutputStream.toByteArray();

        // 受信したファイルの内容をファイルに保存
        // file_nameに保存
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        fileOutputStream.write(fileContent);
        fileOutputStream.close();
        return 0;
    }
}
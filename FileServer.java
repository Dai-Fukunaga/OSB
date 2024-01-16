import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class FileServer {
    static int PORT = 8080;

    private static Set<String> used = new HashSet<>();

    public static void main(String[] args) throws IOException {
        ServerSocket s = new ServerSocket(PORT);
        try {
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
                if (s != null) {
                    s.close();
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public static void addUsed(String file_name) {
        used.add(file_name);
    }

    public static void removeUsed(String file_name) {
        used.remove(file_name);
    }

    public static boolean containsUsed(String file_name) {
        return used.contains(file_name);
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
            String file_name = msg[1];
            String rw = msg[2];
            if (msg[0].equals("save")) {
                System.out.println(rw);
                /* クライアントからfileを受け取る */
                int result = receive(msg[1], socket);
                System.out.println("result = " + result);
                if (!rw.equals("O_RDONLY")) {
                    FileServer.removeUsed(file_name);
                }
            } else if (msg[0].equals("fetch")) {
                boolean F_create = false;
                boolean F_trunc = false;
                if (!rw.equals("O_RDONLY")) {
                    System.err.println("not readonly");
                    if (FileServer.containsUsed(file_name)) {
                        System.err.println("permission denied");
                        String error_message = "permission denied";
                        byte[] fileContent = error_message.getBytes();
                        // ファイルの内容をサーバーに送信
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(fileContent);
                        outputStream.flush();
                        outputStream.close();
                        return;
                    }
                }

                FileServer.addUsed(file_name);

                System.out.println(rw);

                if (msg.length >= 4) {
                    for (int i = 3; i < msg.length; i++) {
                        if (msg[i].equals("O_CREAT")) {
                            F_create = true;
                        }
                        if (msg[i].equals("O_TRUNC")) {
                            F_trunc = true;
                        }
                    }
                }
                /* 実際にファイルを開ける（作る） */
                File f = null;
                try {
                    f = new File(file_name);
                    if (!f.exists() && F_create) {
                        if (!f.createNewFile()) {
                            System.err.println("failed to create file");
                            return;
                        }
                    } else if (!f.exists()) {
                        System.err.println("file not found");
                        return;
                    } else if (F_trunc) {
                        f.delete();
                        if (!f.createNewFile()) {
                            return;
                        }
                    }
                } catch (IOException e) {
                    System.err.println(e);
                    return;
                }
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

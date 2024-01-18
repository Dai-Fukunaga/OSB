import java.io.*;
import java.net.*;
import java.util.*;

public class FileServer {
    private static Set<String> used = new HashSet<>();

    public static void main(String[] args) throws IOException {
        int PORT = Integer.parseInt(args[0]);
        String server_name = "";
        if (PORT == 8080) {
            server_name = "A";
        } else if (PORT == 8081) {
            server_name = "B";
        } else {
            System.err.println("Bad port number = " + PORT);
            return;
        }
        ServerSocket s = new ServerSocket(PORT);
        try {
            System.out.println("Wait for the connection...");
            while (true) {
                Socket socket = s.accept();
                new ServerThread(socket, server_name).start();
                System.out.println("Waiting for new connection...");
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
    private String server_name;

    public ServerThread(Socket socket, String server_name) {
        this.socket = socket;
        this.server_name = server_name;
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
                // System.out.println(rw); // debug
                /* クライアントからfileを受け取る */
                if (receive(file_name, socket, server_name) == -1) {
                    System.err.println("Receive error");
                }
                if (!rw.equals("O_RDONLY")) {
                    FileServer.removeUsed(file_name);
                }
            } else if (msg[0].equals("fetch")) {
                boolean F_create = false;
                boolean F_trunc = false;
                OutputStream outputStream = socket.getOutputStream(); /* fileやエラーを渡す */
                // System.out.println(rw); // debug
                if (!rw.equals("O_RDONLY")) {
                    // System.out.println("not readonly); // debug
                    /* wait for 100ms */
                    /* これがないと、close時のusedからの削除する前に、openしてしまう */
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.err.println(e);
                        // エラーメッセージをサーバーに送信
                        send(outputStream, "e".getBytes());
                        return;
                    }
                    if (FileServer.containsUsed(file_name)) {
                        System.err.println("permission denied");
                        // エラーメッセージをサーバーに送信
                        send(outputStream, "p".getBytes());
                        return;
                    }
                }

                FileServer.addUsed(file_name);

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
                String file = server_name + "/" + file_name;
                try {
                    f = new File(file);
                    if (!f.exists() && F_create) {
                        if (!f.createNewFile()) {
                            System.err.println("failed to create file");
                            // エラーメッセージをサーバーに送信
                            send(outputStream, "c".getBytes());
                            return;
                        }
                    } else if (!f.exists()) {
                        System.err.println("file not found");
                        // エラーメッセージをサーバーに送信
                        send(outputStream, "f".getBytes());
                        return;
                    } else if (F_trunc) {
                        f.delete();
                        if (!f.createNewFile()) {
                            System.err.println("failed to create file");
                            // エラーメッセージをサーバーに送信
                            send(outputStream, "c".getBytes());
                            return;
                        }
                    }
                } catch (IOException e) {
                    System.err.println(e);
                    // エラーメッセージをサーバーに送信
                    send(outputStream, "e".getBytes());
                    return;
                }
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                fileInputStream.close();
                byteArrayOutputStream.close();
                byte[] fileContent = byteArrayOutputStream.toByteArray();
                String success = "s";
                byte[] success_bytes = success.getBytes();
                /* messageContentをsuccess_bytes + fileContentにする */
                byte[] messageContent = new byte[success_bytes.length + fileContent.length];
                System.arraycopy(success_bytes, 0, messageContent, 0, success_bytes.length);
                System.arraycopy(fileContent, 0, messageContent, success_bytes.length, fileContent.length);
                // ファイルの内容をサーバーに送信
                send(outputStream, messageContent);
            }
            reader.close();
        } catch (IOException e) {
            System.err.println(e);
            System.err.println("this error is not send to Client");
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println(e);
                System.err.println("this error is not send to Client");
            }
        }
    }

    public static int receive(String fileName, Socket socket, String server_name) throws IOException {
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
        fileName = server_name + "/" + fileName;
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        fileOutputStream.write(fileContent);
        fileOutputStream.close();
        return 0;
    }

    public static void send(OutputStream outputStream, byte[] messageContent) {
        try{
            outputStream.write(messageContent);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            System.err.println(e);
            System.err.println("this error is not send to Client");
            return;
        }
        return;
    }
}

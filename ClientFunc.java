import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class ClientFunc {
    private String username = "";
    private static Map<Integer, MyFile> fd_dict = new HashMap<>();
    /* <fd, "path:flag:"> */
    /* <fd, "path:flag:flag:"> */

    private static Socket socket = null;

    public ClientFunc(String username) {
        this.username = username;
        /* create folder */
        File client = new File("client");
        if (!client.exists()) {
            client.mkdirs();
        }
        File dir = new File("client/" + this.username);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File A = new File("client/" + this.username + "/A");
        if (!A.exists()) {
            A.mkdirs();
        }
        File B = new File("client/" + this.username + "/B");
        if (!B.exists()) {
            B.mkdirs();
        }
        /* stdin,stdout,stderr */
        MyFile file1 = new MyFile("stdin:O_RDONLY:", null); /* stdinからのreadは未実装 */
        fd_dict.put(0, file1);
        File f2 = new File("/dev/pts/0");
        MyFile file2 = new MyFile("stdout:O_WRONLY:", f2); /* stdoutへのwriteはできる */
        fd_dict.put(1, file2);
        File f3 = new File("/dev/pts/0");
        MyFile file3 = new MyFile("stderr:O_WRONLY:", f3); /* stderrへのwriteはできる */
        fd_dict.put(2, file3);
    }

    public int myOpen(String fileName, int flags) {
        /* String fileName is const */

        /* 最初に./があるかも　→　削除 */
        if (fileName.charAt(0) == '.') {
            fileName = fileName.substring(1);
        }
        if (fileName.charAt(0) == '/') {
            fileName = fileName.substring(1);
        }
        /* fileNameがsever名/ファイル名の形である */
        if (fileName.split("/").length != 2) {
            System.err.println("Bad file name");
            return -1;
        }
        String selectSever = fileName.split("/")[0];
        if (connect(selectSever) == -1) {
            return -1;
        }
        String path = "./client/" + username + "/" + fileName;
        /* fileName = A/abc.txt */
        /* path = ./client/username/A/abc.txt */

        /* flagsを2進数6桁埋めで */
        String flags_bin = Integer.toBinaryString(flags);
        flags_bin = String.format("%6s", flags_bin).replace(" ", "0");
        int flags_bin_len = flags_bin.length();
        if (flags_bin_len != 6) {
            System.err.println("flags_bin_len = " + flags_bin_len);
            return -1;
        }

        /* flags(2進数)の1桁目から3桁目の和は1である */
        int sum = 0;
        for (int i = 1; i <= 3; i++) {
            sum += flags_bin.charAt(flags_bin_len - i) - '0';
        }
        if (sum != 1) {
            System.err.println("flags_sum = " + sum);
            return -1;
        }

        /* info */
        System.out.print("Open: path=" + path + "\t\t");
        String info = path + ":";
        System.out.print("flags_bin =" + flags_bin + "\t");

        Boolean F_create = false;
        Boolean F_trunc = false;
        for (int i = flags_bin_len - 1; i >= 0; i--) {
            if (flags_bin.charAt(i) == '1') {
                System.out.print(MyFlags.flags[flags_bin_len - i - 1] + "\t");
                if (MyFlags.flags[flags_bin_len - i - 1].equals("O_CREAT")) {
                    F_create = true;
                } else if (MyFlags.flags[flags_bin_len - i - 1].equals("O_TRUNC")) {
                    F_trunc = true;
                } else {
                    info += MyFlags.flags[flags_bin_len - i - 1] + ":";
                }
            }
        }
        System.out.println();

        try {
            String msg = "fetch:" + path.split("/")[path.split("/").length - 1];
            msg += ":" + info.split(":")[1];
            if (F_create) {
                msg += ":O_CREAT";
            }
            if (F_trunc) {
                msg += ":O_TRUNC";
            }
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                    true);
            writer.println(msg);
            writer.flush();
            if (receive(path, socket) == -1) {
                System.err.println("receive error");
                return -1;
            }
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
            return -1;
        }

        /* 実際にファイルを開ける（作る） */
        File f = null;
        f = new File(path);
        if (!f.exists()) {
            System.err.println("file not found");
            return -1;
        }

        MyFile info_file = new MyFile(info, f);

        /* fd_dict */
        int fd = 0;
        for (fd = 0; fd < fd_dict.size(); fd++) {
            if (fd_dict.get(fd) == null) {
                fd_dict.put(fd, info_file);
                return fd;
            }
        }
        fd_dict.put(fd, info_file);
        return fd;
    }

    public int myClose(int fd) {
        /* fd_dict */
        if (fd_dict.get(fd) == null) {
            System.err.println("not found fd = " + fd);
            return -1;
        }
        /* <fd, "path:mode:flag:"> */
        String[] info = fd_dict.get(fd).info.split(":");
        String path = info[0];
        System.out.println("Close: path=" + path);
        /* path = ./client/username/A/abc.txt */
        String selectSever = path.split("/")[3];
        if (connect(selectSever) == -1) {
            return -1;
        }
        String[] flags = new String[info.length - 1];
        for (int i = 1; i < info.length; i++) {
            flags[i - 1] = info[i];
        }
        if (!flags[0].equals("O_RDONLY")) {
            /* ローカルキャッシュの内容をサーバーに書き込む */
            File f = fd_dict.get(fd).file;
            if (!f.exists()) {
                System.err.println("file not found");
                return -1;
            }

            try {
                String msg = "save:" + path.split("/")[path.split("/").length - 1];
                msg += ":" + info[1];
                PrintWriter writer = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                        true);
                writer.println(msg);
                writer.flush();

                FileInputStream fileInputStream = new FileInputStream(f);
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
                writer.close();
            } catch (IOException e) {
                System.err.println(e);
                return -1;
            }
        }

        System.out.println("close \"" + fd_dict.get(fd) + "\"");

        // delete file
        fd_dict.get(fd).file.delete();

        fd_dict.put(fd, null);

        return 0;
    }

    public int myRead(int fd, String[] buf, int nbytes) {
        if (fd_dict.get(fd) == null) {
            System.err.println("not found fd = " + fd);
            return -1;
        }
        /* <fd, "path:mode:flag:"> */
        String[] info = fd_dict.get(fd).info.split(":");
        String path = info[0];
        System.out.println("Read: path=" + path);
        String[] flags = new String[info.length - 1];
        for (int i = 1; i < info.length; i++) {
            flags[i - 1] = info[i];
        }
        if (flags[0].equals("O_WRONLY") == true) {
            System.err.println(" Bad file descriptor");
            return -1;
        }

        File f = fd_dict.get(fd).file;
        try {
            FileReader fr = new FileReader(f);
            int ch;
            /* while((ch = fr.read()) != -1){ */
            for (int i = 0; i < nbytes; i++) {
                ch = fr.read();
                if (ch == -1) {
                    System.err.println("*** stack smashing detected ***: terminated");
                    System.err.println("Aborted");
                    break;
                }
                buf[0] += (char) ch;
            }
            fr.close();
        } catch (IOException e) {
            System.err.println(e);
            return -1;
        }
        return nbytes;
    }

    public int myWrite(int fd, String[] buf, int nbytes) {
        /* String buf is const */
        String tmp_buf = buf[0];
        if (fd_dict.get(fd) == null) {
            System.err.println("not found fd = " + fd);
            return -1;
        }
        /* <fd, "path:mode:flag:"> */
        String[] info = fd_dict.get(fd).info.split(":");
        String path = info[0];
        System.out.println("Write: path=" + path);
        String[] flags = new String[info.length - 1];
        for (int i = 1; i < info.length; i++) {
            flags[i - 1] = info[i];
        }
        if (flags[0].equals("O_RDONLY") == true) {
            System.err.println(" Bad file descriptor");
            return -1;
        }
        boolean F_append = false;
        if ((flags.length == 2 && flags[1].equals("O_APPEND")) == true) {
            F_append = true;
        }

        File f = fd_dict.get(fd).file;
        try {
            FileWriter fw;
            if (F_append) {
                fw = new FileWriter(f, true);
                if (nbytes > tmp_buf.length()) {
                    System.err.println("nbytes > buf[0].length");
                    for (int i = 0; i < nbytes - tmp_buf.length(); i++) {
                        tmp_buf += (char) 0;
                    }
                    fw.write(tmp_buf);
                } else if (nbytes < tmp_buf.length()) {
                    fw.write(tmp_buf, 0, nbytes);
                } else {
                    fw.write(tmp_buf);
                }
            } else {
                int add_bytes = 0;
                try {
                    /*
                     * 上書きする文字をnbytes分だけにする （例）String buf[0] = "xyz", nbytes = 2 --> buf[0] = "xy"
                     */
                    /* String buf[0] = "xyz", nbytes = 5 --> buf[0] = "xyz??" */
                    if (nbytes < tmp_buf.length()) {
                        tmp_buf = tmp_buf.substring(0, nbytes);
                    } else if (nbytes > tmp_buf.length()) {
                        System.err.println("nbytes > buf[0].length");
                        for (int i = 0; i < nbytes - tmp_buf.length(); i++) {
                            tmp_buf += (char) 0;
                        }
                    }
                    FileReader fr = new FileReader(f);
                    int ch;
                    /* 上書きされる部分を空読み (例) 元のfile内容 "abcdefghi", nbytes = 3 --> "123"の部分を空読み */
                    for (int i = 0; i < nbytes; i++) {
                        ch = fr.read();
                        if (ch == -1) {
                            break;
                        }
                    }
                    /* 元の部分の読み込み (例) 元のfile内容 "abcdefghi", nbytes = 3 --> "defghi"の部分を読み込み */
                    while ((ch = fr.read()) != -1) {
                        tmp_buf += (char) ch;
                        add_bytes++;
                    }
                    fr.close();
                } catch (IOException e) {
                    System.err.println(e);
                    return -1;
                }
                /* write */
                fw = new FileWriter(f);
                if (nbytes + add_bytes > tmp_buf.length()) {
                    System.err.println("OH!! This error should not happen!!");
                } else if (nbytes + add_bytes < tmp_buf.length()) {
                    fw.write(tmp_buf, 0, nbytes + add_bytes);
                } else {
                    fw.write(tmp_buf);
                }
            }
            fw.close();
        } catch (IOException e) {
            System.err.println(e);
            return -1;
        }
        return nbytes;
    }

    public static int receive(String fileName, Socket r_socket) throws IOException {
        if (r_socket.isClosed()) {
            System.out.println("Socket is closed!!");
            return -1;
        }
        DataInputStream inputStream = new DataInputStream(r_socket.getInputStream());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        byte[] fileContent = null;
        byte[] messageContent = byteArrayOutputStream.toByteArray();
        if ((messageContent.length == 1) && (messageContent[0] != ("s".getBytes())[0])) {
            if (messageContent[0] == ("p".getBytes())[0]) {
                System.err.println("permission denied");
                return -1;
            } else if (messageContent[0] == ("f".getBytes())[0]) {
                System.err.println("file not found");
                return -1;
            } else if (messageContent[0] == ("e".getBytes())[0]) {
                System.err.println("error");
                return -1;
            } else if (messageContent[0] == ("c".getBytes())[0]) {
                System.out.println("failed to create file");
                return -1;
            } else {
                System.err.println("Bad message");
                return -1;
            }
        } else {
            /* fileContentの[0]を無くす */
            fileContent = Arrays.copyOfRange(messageContent, 1, messageContent.length);
        }

        // 受信したファイルの内容をファイルに保存
        // fileNameに保存
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        fileOutputStream.write(fileContent);
        fileOutputStream.close();
        return 0;
    }

    public static int connect(String serverName) {
        int PORT;
        if (serverName.equals("A")) {
            PORT = 8080;
        } else if (serverName.equals("B")) {
            PORT = 8081;
        } else {
            System.err.println("Bad server name = " + serverName);
            return -1;
        }
        try {
            InetAddress addr = InetAddress.getByName("localhost"); // IP アドレスへの変換
            // System.out.println("IP address: " + addr); // debug
            socket = new Socket(addr, PORT); // ソケットの生成
        } catch (IOException e) {
            System.out.println(e);
            System.out.println(serverName + " is not running!!");
            return -1;
        }
        // socketがnullなら待つ
        while (socket == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println(e);
                return -1;
            }
        }
        return 0;
    }

}

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Set<String> files;

    public Client(Socket sock) {
        this.socket = sock;
        this.files = new HashSet<>();
    }

    /**
     * this is a function for opening a file
     * @param name file name
     * @param path file path
     * @param mode open option
     */
    public void open(String name, String path, String mode) {
        if (files.contains(path)) {
            try {
                File f = new File("./" + path);
                FileReader fr = new FileReader(f);
                BufferedReader br = new BufferedReader(fr);
                String str;
                while ((str = br.readLine()) != null) {
                    System.out.println(str);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String[] file = new String[4];
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            String[] msg = {"open", name, path, mode};
            out.writeObject(msg);
            out.flush();
            file = (String[])in.readObject();
            System.out.println(file[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mode == "r") {
            try {
                File f = new File("./" + path);
                FileWriter fw = new FileWriter(f);
                fw.write(file[0]);
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

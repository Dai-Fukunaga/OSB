import java.io.File;

public class MyFile {
    public String info;
    public File file = null;

    public MyFile() {
        this("", null);
    }

    public MyFile(String info) {
        this(info, null);
    }

    public MyFile(String info, File file) {
        this.info = info;
        this.file = file;
    }
}

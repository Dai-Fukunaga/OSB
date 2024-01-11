import java.util.*;
import java.io.*;

public class Main {
    public static Map<Integer, MyFile> fd_dict = new HashMap<>();
    /* <fd, "name:mode:flag:"> */
    /* <fd, "name:mode:flag:flag:"> */

    public static void main(String[] args) {
        MyFile file1 = new MyFile("stdin:a:O_RDONLY:", null); /*stdinからのreadは未実装 */
        fd_dict.put(0, file1);
        File f2 = new File("/dev/pts/0");
        MyFile file2 = new MyFile("stdout:a:O_WRONLY:", f2); /*stdoutへのwriteはできる */
        fd_dict.put(1, file2);
        MyFile file3 = new MyFile("stderr:a:O_WRONLY:", f2); /*stderrへのwriteはできる */
        fd_dict.put(2, file3);
        int i = 0;
        if (myClose(2) == -1){
            System.err.println("close err");
        }
        i = myOpen("abc.txt",MyFlags.O_RDONLY | MyFlags.O_CREAT,"a");
        System.out.println("fd = " + i);
        i = myOpen("abc.txt",MyFlags.O_RDONLY | MyFlags.O_CREAT,"a");
        System.out.println("fd = " + i);

        if (myClose(i) == -1){
            System.err.println("close err");
        }
        i = myOpen("abc.txt",MyFlags.O_RDWR | MyFlags.O_CREAT,"a");
        System.out.println("fd = " + i);
        String[] buf = new String[] {""}; /*参照渡しのためlistに */
        if (myRead(i, buf,3) != 3){
            System.err.println("read err");
        }
        System.out.println("buf = " + buf[0]);

        buf[0] = "hoge";
        if (myWrite(1, buf, buf[0].length()) != 4){
            System.err.println("write err");
        }
    }

    public static int myOpen(String name,int flags,String mode) {
        /* String name is const */
        /* mode is 8進数 3桁 */

        /* 2進数6桁埋めで */
        String flags_bin = Integer.toBinaryString(flags);
        flags_bin = String.format("%6s",flags_bin).replace(" ","0");
        int flags_bin_len = flags_bin.length();
        if (flags_bin_len != 6) {
            System.err.println("flags_bin_len = " + flags_bin_len);
            return -1;
        }

        /* flags(2進数)の1桁目から3桁目の和は1である */
        int sum = 0;
        for (int i=1; i<=3; i++) {
            sum += flags_bin.charAt(flags_bin_len-i) - '0';
        }
        if (sum != 1) {
            System.err.println("flags_sum = " + sum);
            return -1;
        }
        Boolean F_create = false;
        Boolean F_trunc = false;

        /*info */
        String info = "";
        System.out.print("file_name = " + name + "\t\t");
        info = name + ":";
        System.out.println("mode = " + mode);
        info += mode + ":";
        System.out.print("flags_bin = " + flags_bin + "\t");
        for (int i=flags_bin_len-1; i>=0; i--) {
            if (flags_bin.charAt(i) == '1') {
                System.out.print( MyFlags.flags[flags_bin_len-i-1]+"\t");
                if (MyFlags.flags[flags_bin_len-i-1].equals("O_CREAT") == true) {
                    F_create = true;
                } else if (MyFlags.flags[flags_bin_len-i-1].equals("O_TRUNC") == true) {
                    F_trunc = true;
                } else {
                    info += MyFlags.flags[flags_bin_len-i-1] + ":";
                }
            }
        }
        System.out.println();

        /*実際にファイルを開ける（作る） */
        File f;
        try{
            f = new File(name);
            if (!f.exists() && F_create) {
                boolean result = f.createNewFile();
                if (!result) {
                    return -1;
                }
            } else if (!f.exists()) {
                System.err.println("file not found");
                return -1;
            } else if (F_trunc) {
                f.delete();
                boolean result = f.createNewFile();
                if (!result) {
                    return -1;
                }
            }
        } catch (IOException e) {
            System.err.println(e);
            return -1;
        }
        MyFile info_file = new MyFile(info, f);

        /* fd_dict */
        int j=0;
        for (j=0; j<fd_dict.size(); j++) {
            if (fd_dict.get(j) == null) {
                fd_dict.put(j, info_file);
                return j;
            }
        }
        fd_dict.put(j, info_file);
        return j;
    }

    public static int myClose(int fd){
        /* fd_dict */
        if (fd_dict.get(fd) != null) {
            System.out.println("close \""+fd_dict.get(fd)+"\"");
            fd_dict.put(fd, null);
            return 0;
        }
        return -1;
    }

    public static int myRead (int fd , String[] buf, int nbytes) {
        if (fd_dict.get(fd) == null) {
            System.err.println("not found fd = " + fd);
            return -1;
        }
        /* <fd, "name:mode:flag:"> */
        String[] info = fd_dict.get(fd).info.split(":");
        File f = fd_dict.get(fd).file;
        String name = info[0];
        String mode = info[1];
        System.out.println("file_name = " + name);
        String[] flags = new String[info.length-2];
        for (int i=2; i<info.length; i++) {
            flags[i-2] = info[i];
        }
        if (flags[0].equals("O_WRONLY") == true) {
            System.err.println("this file is not readable");
            return -1;
        }

        try{
            FileReader fr = new FileReader(f);
            int ch;
            /*while((ch = fr.read()) != -1){*/
            for (int i=0; i<nbytes; i++) {
                ch = fr.read();
                if (ch == -1) {
                    System.err.println("*** stack smashing detected ***: terminated");
                    System.err.println("Aborted");
                    break;
                }
                buf[0] += (char)ch;
            }
            fr.close();
        } catch (IOException e) {
            System.err.println(e);
            return -1;
        }
        return nbytes;
    }

    public static int myWrite (int fd , String[] buf, int nbytes) {
        /* String buf is const */
        if (fd_dict.get(fd) == null) {
            System.err.println("not found fd = " + fd);
            return -1;
        }
        /* <fd, "name:mode:flag:"> */
        String[] info = fd_dict.get(fd).info.split(":");
        File f = fd_dict.get(fd).file;
        String name = info[0];
        String mode = info[1];
        System.out.println("file_name = " + name);
        String[] flags = new String[info.length-2];
        for (int i=2; i<info.length; i++) {
            flags[i-2] = info[i];
        }
        if (flags[0].equals("O_RDONLY") == true) {
            System.err.println("this file is not writable");
            return -1;
        }

        try{
            FileWriter fw = new FileWriter(f);
            if (nbytes > buf[0].length()){
                System.err.println("nbytes > buf[0].length");
                fw.write(buf[0]);
            } else if (nbytes < buf[0].length()) {
                fw.write(buf[0].substring(0, nbytes));
            } else {
                fw.write(buf[0]);
            }
            fw.close();
        } catch (IOException e) {
            System.err.println(e);
            return -1;
        }
        return nbytes;
    }
}

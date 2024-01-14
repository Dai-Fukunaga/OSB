import java.util.*;
import java.io.*;

public class Main {
    public static Map<Integer, MyFile> fd_dict = new HashMap<>();
    /* <fd, "name:flag:"> */
    /* <fd, "name:flag:flag:"> */

    public static void main(String[] args) {
        /*stdin,stdout,stderr */
        MyFile file1 = new MyFile("stdin:O_RDONLY:", null); /*stdinからのreadは未実装 */
        fd_dict.put(0, file1);
        File f2 = new File("/dev/pts/0");
        MyFile file2 = new MyFile("stdout:O_WRONLY:", f2); /*stdoutへのwriteはできる */
        fd_dict.put(1, file2);
        File f3 = new File("/dev/pts/0");
        MyFile file3 = new MyFile("stderr:O_WRONLY:", f3); /*stderrへのwriteはできる */
        fd_dict.put(2, file3);

        int i = 0;
        //if (myClose(2) == -1){
        //    System.err.println("close err");
        //}
        //i = myOpen("abc.txt",MyFlags.O_RDONLY);
        //System.out.println("fd = " + i);
        //i = myOpen("abc.txt",MyFlags.O_RDONLY);
        //System.out.println("fd = " + i);

        //if (myClose(i) == -1){
        //    System.err.println("close err");
        //}
        i = myOpen("abc.txt",MyFlags.O_RDWR | MyFlags.O_APPEND);
        System.out.println("fd = " + i);
        String[] buf = new String[] {""}; /*参照渡しのためlistに */
        if (myRead(i, buf,3) != 3){
            System.err.println("read err");
        }
        System.out.println("buf = " + buf[0]);

        buf[0] = "hoge";
        if (myWrite(i, buf,buf[0].length()) != buf[0].length()){
            System.err.println("write err");
        }
        if (myClose(i) == -1){
            System.err.println("close err");
        }
    }

    public static int myOpen(String name,int flags) {
        /* String name is const */

        /* flagsを2進数6桁埋めで */
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
        String name = info[0];
        System.out.println("file_name = " + name);
        String[] flags = new String[info.length-1];
        for (int i=1; i<info.length; i++) {
            flags[i-1] = info[i];
        }
        if (flags[0].equals("O_WRONLY") == true) {
            System.err.println(" Bad file descriptor");
            return -1;
        }

        File f = fd_dict.get(fd).file;
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
        String tmp_buf = buf[0];
        if (fd_dict.get(fd) == null) {
            System.err.println("not found fd = " + fd);
            return -1;
        }
        /* <fd, "name:mode:flag:"> */
        String[] info = fd_dict.get(fd).info.split(":");
        String name = info[0];
        System.out.println("file_name = " + name);
        String[] flags = new String[info.length-1];
        for (int i=1; i<info.length; i++) {
            flags[i-1] = info[i];
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
        try{
            FileWriter fw;
            if (F_append) {
                fw = new FileWriter(f, true);
                if (nbytes > tmp_buf.length()){
                    System.err.println("nbytes > buf[0].length");
                    for (int i=0; i< nbytes-tmp_buf.length(); i++) {
                            tmp_buf += (char)0;
                    }
                    fw.write(tmp_buf);
                } else if (nbytes < tmp_buf.length()) {
                    fw.write(tmp_buf, 0, nbytes);
                } else {
                    fw.write(tmp_buf);
                }
            } else {
                int add_bytes = 0;
                try{
                    /* 上書きする文字をnbytes分だけにする （例）String buf[0] = "xyz", nbytes = 2 --> buf[0] = "xy" */
                    /*                                   String buf[0] = "xyz", nbytes = 5 --> buf[0] = "xyz??" */
                    if (nbytes < tmp_buf.length()){
                        tmp_buf = tmp_buf.substring(0, nbytes);
                    } else if (nbytes > tmp_buf.length()){
                        System.err.println("nbytes > buf[0].length");
                        for (int i=0; i< nbytes-tmp_buf.length(); i++) {
                            tmp_buf += (char)0;
                        }
                    }
                    FileReader fr = new FileReader(f);
                    int ch;
                    /* 上書きされる部分を空読み (例) 元のfile内容 "abcdefghi", nbytes = 3 --> "123"の部分を空読み */
                    for (int i=0; i<nbytes; i++) {
                        ch = fr.read();
                        if (ch == -1) {
                            break;
                        }
                    }
                    /* 元の部分の読み込み (例) 元のfile内容 "abcdefghi", nbytes = 3 --> "defghi"の部分を読み込み */
                    while((ch = fr.read()) != -1){
                        tmp_buf += (char)ch;
                        add_bytes++;
                    }
                    fr.close();
                } catch (IOException e) {
                    System.err.println(e);
                    return -1;
                }
                /* write */
                fw = new FileWriter(f);
                if (nbytes+add_bytes > tmp_buf.length()){
                    System.err.println("OH!! This error should not happen!!");
                } else if (nbytes+add_bytes < tmp_buf.length()) {
                    fw.write(tmp_buf, 0, nbytes+add_bytes);
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
}

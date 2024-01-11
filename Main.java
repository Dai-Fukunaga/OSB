import java.util.*;
import java.io.*;
import java.net.*;

public class Main {
    public static Map<Integer, String> fd_dict = new HashMap<>();
    /* <fd, "name:mode:flag:"> */
    /* <fd, "name:mode:flag:flag:"> */

    public static void main(String[] args) {
        fd_dict.put(0, "stdin:a:O_RDONLY:");
        fd_dict.put(1, "stdout:a:O_WRONLY:");
        fd_dict.put(2, "stderr:a:O_WRONLY:");
        int i = 0;
        if (myClose(1) == -1){
            System.err.println("close err");
        }
        i = myOpen("abc",MyFlags.O_RDONLY | MyFlags.O_TRUNC,"a");
        System.out.println("fd = " + i);
        i = myOpen("abc",MyFlags.O_RDONLY | MyFlags.O_TRUNC,"a");
        System.out.println("fd = " + i);

        if (myClose(i) == -1){
            System.err.println("close err");
        }
        i = myOpen("abc",MyFlags.O_RDONLY | MyFlags.O_TRUNC,"a");
        System.out.println("fd = " + i);
        String[] buf = new String[] {""}; /*参照渡しのためlistに */
        i = myRead(i, buf,10);
        System.out.println("buf = " + buf[0]);
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
                info += MyFlags.flags[flags_bin_len-i-1] + ":";
            }
        }
        System.out.println();
        /*この辺で実際にファイルを開ける */

        /* fd_dict */
        int j=0;
        for (j=0; j<fd_dict.size(); j++) {
            if (fd_dict.get(j) == null) {
                fd_dict.put(j, info);
                return j;
            }
        }
        fd_dict.put(j, info);
        return j;
    }

    public static int myClose(int fd){
        /* fd_dict */
        if (fd_dict.get(fd) != null) {
            System.out.println("close \""+fd_dict.get(fd)+"\"");
            fd_dict.put(fd, null);
            /*この辺で実際にファイルを閉じる（保存？） */
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
        String[] info = fd_dict.get(fd).split(":");
        String name = info[0];
        String mode = info[1];
        String[] flags = new String[info.length-2];
        for (int i=2; i<info.length; i++) {
            flags[i-2] = info[i];
        }
        if (flags[0].equals("O_WRONLY") == true) {
            System.err.println("this file is not readable");
            return -1;
        }

        buf[0] = "hello world";
        return nbytes;
    }

    public static int myWrite (int fd , String[] buf, int nbytes) {
        /* String buf is const */
        return nbytes;
    }
}

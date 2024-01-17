
public class Client {
    public static void main(String[] args) {
        String username = args[0];
        ClientFunc cf = new ClientFunc(username);
        int i = 0;
        i = cf.myOpen("abc.txt", MyFlags.O_RDWR | MyFlags.O_APPEND);
        if (i == -1) {
            System.err.println("open err");
            return;
        }
        /*wait for 1000ms */
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[] buf = new String[] { "" }; /* 参照渡しのためlistに */
        if (cf.myRead(i, buf, 3) != 3) {
            System.err.println("read err");
            return;
        }
        System.out.println("buf = " + buf[0]);

        buf[0] = "hoge";
        if (cf.myWrite(i, buf, buf[0].length()) != buf[0].length()) {
            System.err.println("write err");
            return;
        }
        if (cf.myClose(i) == -1) {
            System.err.println("close err");
            return;
        }

        i = cf.myOpen("abc.txt", MyFlags.O_RDWR | MyFlags.O_APPEND);
        if (i == -1) {
            System.err.println("open err");
            return;
        }
        buf[0] = "aaaa";
        if (cf.myWrite(i, buf, buf[0].length()) != buf[0].length()) {
            System.err.println("write err");
            return;
        }
        if (cf.myClose(i) == -1) {
            System.err.println("close err");
            return;
        }
    }
}


public class Client {
    public static void main(String[] args) {
        ClientFunc cf = new ClientFunc();
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
        i = cf.myOpen("./client/abc.txt",MyFlags.O_RDWR | MyFlags.O_APPEND);
        System.out.println("fd = " + i);
        String[] buf = new String[] {""}; /*参照渡しのためlistに */
        if (cf.myRead(i, buf,3) != 3){
            System.err.println("read err");
        }
        System.out.println("buf = " + buf[0]);

        buf[0] = "hoge";
        if (cf.myWrite(i, buf,buf[0].length()) != buf[0].length()){
            System.err.println("write err");
        }
        if (cf.myClose(i) == -1){
            System.err.println("close err");
        }
    }
}
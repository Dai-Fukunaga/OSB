public class MyFlags {
    public static final int O_RDONLY = 0b000001;
    public static final int O_WRONLY = 0b000010;
    public static final int O_RDWR =   0b000100;

    public static final int O_APPEND = 0b001000;
    public static final int O_CREAT =  0b010000;
    public static final int O_TRUNC =  0b100000;

    public static final String[] flags = {"O_RDONLY","O_WRONLY","O_RDWR","O_APPEND","O_CREAT","O_TRUNC"};
}

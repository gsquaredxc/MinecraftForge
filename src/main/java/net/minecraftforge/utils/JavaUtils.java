package net.minecraftforge.utils;

public class JavaUtils {
    private final String javaVersion = System.getProperty("java.version");
    public static JavaUtils INSTANCE = new JavaUtils();

    public boolean isJava8() {
        return javaVersion.startsWith("1.8");
    }

    public boolean isJava7() {
        return javaVersion.startsWith("1.7");
    }

    public boolean isJava16() {
        return javaVersion.startsWith("16");
    }
}

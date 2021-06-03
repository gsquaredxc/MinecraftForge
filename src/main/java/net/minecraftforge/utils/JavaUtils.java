package net.minecraftforge.utils;

public class JavaUtils {
    public static JavaUtils INSTANCE = new JavaUtils();

    private final String javaVersion = System.getProperty("java.version");

    /**
     * Logic: In Java 9+ AppClassLoader is no longer extends URLClassLoader meaning we can check if the version by seeing if it still extends it or not
     *
     * @return Whether the current JRE is Java 8/Whatever extended URLClassLoader
     */
    public boolean isJava8() {
        return javaVersion.startsWith("1.8");
    }

    public boolean isJava7() {
        return javaVersion.startsWith("1.7");
    }
}

package net.minecraftforge.utils;

import net.minecraftforge.launch.ForgeLaunchWrapper;

import java.net.URLClassLoader;

public class JavaUtils {
    public static JavaUtils INSTANCE = new JavaUtils();

    /**
     * Logic: In Java 9+ AppClassLoader is no longer extends URLClassLoader meaning we can check if the version by seeing if it still extends it or not
     *
     * @return Whether the current JRE is Java 8/Whatever extended URLClassLoader
     */
    public boolean isJava8() {
        return ForgeLaunchWrapper.class.getClassLoader() instanceof URLClassLoader;
    }

    public boolean isJava7() {
        return System.getProperty("java.version", "1.8.0_251").startsWith("1.7");
    }
}

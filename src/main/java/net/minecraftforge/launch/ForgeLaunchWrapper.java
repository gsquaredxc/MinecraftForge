package net.minecraftforge.launch;

import net.minecraftforge.launch.loader.ForgeLaunchWrapperClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForgeLaunchWrapper {
    private static final Logger LOGGER = LogManager.getLogger(ForgeLaunchWrapper.class);

    private static final ForgeLaunchWrapperClassLoader classLoader = new ForgeLaunchWrapperClassLoader();
    ;

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        String mainClass;

        int mainClassIndex = arguments.indexOf("--main_class");

        if (mainClassIndex != -1) {
            mainClass = arguments.get(mainClassIndex + 1);
            arguments.remove("--main_class");
            arguments.remove(mainClass);
        } else {
            mainClass = isDevelopment() ? "GradleStart" : "net.minecraft.launchwrapper.Launch";
        }

        if (isDevelopment()) {
            System.setProperty("mixin.env.remapRefMap", "true"); // Fix mixin remapping issues
        }

        try {
            classLoader.addDefaultTransformers();
            Thread.currentThread().setContextClassLoader(classLoader);

            Class<?> clazz = Class.forName(mainClass, true, classLoader);
            Method entrypointMethod = clazz.getDeclaredMethod("main", String[].class);
            entrypointMethod.invoke(null, new Object[]{arguments.toArray(new String[0])});
        } catch (Exception e) {
            LOGGER.error("An error occurred when attempting to launch", e);
        }
    }

    public static ForgeLaunchWrapperClassLoader getClassLoader() {
        return classLoader;
    }

    public static boolean isDevelopment() {
        try {
            Class.forName("net.minecraft.client.Minecraft", false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

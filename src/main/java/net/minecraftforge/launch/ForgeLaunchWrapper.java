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

    private static ForgeLaunchWrapperClassLoader classLoader;

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        int mainClassIndex = arguments.indexOf("--main_class") + 1;

        if ((mainClassIndex - 1) == -1) {
            throw new IllegalStateException("No launch target has been specified. (Use --main_class argument)");
        }

        String mainClass = arguments.get(mainClassIndex);

        arguments.remove("--main_class");
        arguments.remove(mainClass);

        classLoader = new ForgeLaunchWrapperClassLoader();

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
}

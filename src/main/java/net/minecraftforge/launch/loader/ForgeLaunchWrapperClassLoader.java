package net.minecraftforge.launch.loader;

import net.minecraft.launchwrapper.LogWrapper;
import net.minecraft.launchwrapper.utils.Classpath;
import net.minecraftforge.launch.loader.transformer.ForgeTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ForgeLaunchWrapperClassLoader extends URLClassLoader {
    private final ClassLoader parent = getClass().getClassLoader();

    private static final List<String> exclusions = new ArrayList<>();

    private static final List<ForgeTransformer> transformers = new ArrayList<>();

    public ForgeLaunchWrapperClassLoader() {
        super(Classpath.getClasspath(), getSystemClassLoader());

        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("javax.");
        addClassLoaderExclusion("com.sun.");
        addClassLoaderExclusion("jdk.internal.");

        addClassLoaderExclusion("net.minecraftforge.launch.");
        addClassLoaderExclusion("org.apache.logging.");
        addClassLoaderExclusion("org.lwjgl.");
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);

            if (c != null) {
                return c;
            }

            try {
                return findClass(name);
            } catch (ClassNotFoundException ignored) {
                return super.loadClass(name, resolve);
            }
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (String exclusion : exclusions) {
            if (name.startsWith(exclusion)) {
                return parent.loadClass(name);
            }
        }

        String resource = name.replace('.', '/') + ".class";
        URL url = getResource(resource);
        if (url == null) {
            throw new ClassNotFoundException(name);
        }
        try (InputStream stream = url.openStream()) {
            byte[] bytes = readAllBytes(stream);

            if (bytes != null) {
                CodeSource source = getCodeSource(name);

                for (ForgeTransformer forgeTransformer : transformers) {
                    bytes = forgeTransformer.transform(name, bytes);
                }

                return defineClass(name, bytes, 0, bytes.length, source);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        return super.findClass(name);
    }

    @Override
    public URL getResource(String name) {
        URL resource = parent.getResource(name);
        return resource == null ? super.getResource(name) : resource;
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    public static void addClassLoaderExclusion(String toExclude) {
        exclusions.add(toExclude);
    }

    public void addTransformer(ForgeTransformer transformer) {
        transformers.add(transformer);
    }

    /**
     * @author Mojang
     * @source https://github.com/Mojang/LegacyLauncher
     */
    private CodeSource getCodeSource(String name) throws IOException {
        final int lastDot = name.lastIndexOf('.');
        final String fileName = name.replace('.', '/').concat(".class");
        URLConnection urlConnection = findCodeSourceConnectionFor(fileName);

        CodeSigner[] signers = null;

        if (lastDot > -1 && !name.startsWith("net.minecraft.")) {
            if (urlConnection instanceof JarURLConnection) {
                final JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
                final JarFile jarFile = jarURLConnection.getJarFile();

                if (jarFile != null && jarFile.getManifest() != null) {
                    final JarEntry entry = jarFile.getJarEntry(fileName);

                    if (entry != null) {
                        signers = entry.getCodeSigners();
                    }
                }
            }
        }

        return urlConnection == null ? null : new CodeSource(fixJarURL(urlConnection instanceof JarURLConnection ? ((JarURLConnection) urlConnection).getJarFileURL() : urlConnection.getURL(), name), signers);
    }

    private URL fixJarURL(URL url, String className) throws MalformedURLException {
        String toString = url.toString();

        if (toString.startsWith("jar:")) {
            return new URL(toString.substring(4));
        }

        String classNamePath = "/".concat(className.replace(".", "/")).concat(".class");

        if (toString.endsWith(classNamePath)) {
            return new URL(toString.substring(0, toString.length() - classNamePath.length()));
        }

        return url;
    }

    private URLConnection findCodeSourceConnectionFor(String name) {
        final URL resource = getResource(name);
        if (resource != null) {
            try {
                return resource.openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private byte[] readAllBytes(InputStream stream) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int read;

            byte[] data = new byte[1024];

            while ((read = stream.read(data, 0, data.length)) != -1) {
                bos.write(data, 0, read);
            }

            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Oh Forge, how you are full of hacks! Even to maintain you, we must hack it together...
     * This fix was one I didn't really want to make because it's super messy, but here we are
     * <p>
     * This classloader needs to apply these patches as early as possible, to do this we must manually load the classes
     * and then add them as a transformer, so that they are not loaded under the AppClassLoader, causing incompatibilities.
     */
    public void addDefaultTransformers() throws Exception {
        Class<?> mixinServiceLaunchWrapperTransformer = Class.forName("net.minecraftforge.patching.MixinServiceLaunchWrapperTransformer", true, this);
        Class<?> invokeDynamicTransformer = Class.forName("net.minecraftforge.patching.InvokeDynamicTransformer", true, this);
        Class<?> classReaderTransformer = Class.forName("net.minecraftforge.patching.ClassReaderTransformer", true, this);
        addTransformer((ForgeTransformer) mixinServiceLaunchWrapperTransformer.newInstance());
        addTransformer((ForgeTransformer) invokeDynamicTransformer.newInstance());
        addTransformer((ForgeTransformer) classReaderTransformer.newInstance());
    }
}

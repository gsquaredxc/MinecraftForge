package net.minecraftforge.launch.loader;

import com.google.common.collect.ArrayListMultimap;
import cpw.mods.gross.Java9ClassLoaderUtil;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraftforge.launch.ForgeLaunchWrapper;
import net.minecraftforge.launch.loader.transformer.ForgeTransformer;
import net.minecraftforge.patching.ClassReaderTransformer;
import net.minecraftforge.patching.MixinServiceLaunchWrapperTransformer;
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
    private final Logger LOGGER = LogManager.getLogger(this);

    private final ClassLoader parent = getClass().getClassLoader();

    private static final List<String> exclusions = new ArrayList<>();

    private static final List<ForgeTransformer> transformers = new ArrayList<>();

    public ForgeLaunchWrapperClassLoader() {
        super(Java9ClassLoaderUtil.getSystemClassPathURLs(), null);

        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("javax.");
        addClassLoaderExclusion("com.sun.");
        addClassLoaderExclusion(getClass().getName());
        addClassLoaderExclusion("org.lwjgl.");
//        addClassLoaderExclusion("org.objectweb.asm.");
//        addClassLoaderExclusion(ForgeTransformer.class.getName());
        addClassLoaderExclusion(ForgeLaunchWrapper.class.getName());

        addTransformer(new MixinServiceLaunchWrapperTransformer());
        addTransformer(new ClassReaderTransformer());
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
        final String packageName = lastDot == -1 ? "" : name.substring(0, lastDot);
        final String fileName = name.replace('.', '/').concat(".class");
        URLConnection urlConnection = findCodeSourceConnectionFor(fileName);

        CodeSigner[] signers = null;

        if (lastDot > -1 && !name.startsWith("net.minecraft.")) {
            if (urlConnection instanceof JarURLConnection) {
                final JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
                final JarFile jarFile = jarURLConnection.getJarFile();

                if (jarFile != null && jarFile.getManifest() != null) {
                    final Manifest manifest = jarFile.getManifest();
                    final JarEntry entry = jarFile.getJarEntry(fileName);

                    if (entry != null) {
                        Package pkg = getPackage(packageName);
                        signers = entry.getCodeSigners();
                        if (pkg == null) {
                            pkg = definePackage(packageName, manifest, jarURLConnection.getJarFileURL());
                        } else {
                            if (pkg.isSealed() && !pkg.isSealed(jarURLConnection.getJarFileURL())) {
                                LogWrapper.severe("The jar file %s is trying to seal already secured path %s", jarFile.getName(), packageName);
                            }
                        }
                    }
                }
            } else {
                Package pkg = getPackage(packageName);
                if (pkg == null) {
                    pkg = definePackage(packageName, null, null, null, null, null, null, null);
                } else if (pkg.isSealed()) {
                    LogWrapper.severe("The URL %s is defining elements for sealed path %s", Objects.requireNonNull(urlConnection).getURL(), packageName);
                }
            }
        }

        return urlConnection == null ? null : new CodeSource(fixJarURL(urlConnection.getURL()), signers);
    }

    private URL fixJarURL(URL url) throws MalformedURLException {
        String toString = url.toString();

        return toString.startsWith("jar:") ? new URL(toString.substring(4)) : url;
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
}

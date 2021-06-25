package net.minecraftforge.fml.common.asm.transformers;

import com.google.common.collect.ImmutableSet;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.utils.JavaUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class BlamingTransformer implements IClassTransformer {
    private static final Map<String, String> classMap = new HashMap<>();
    private static final Set<String> naughtyMods = new HashSet<>();
    private static final Set<String> naughtyClasses = new TreeSet<>();
    private static final Set<String> orphanNaughtyClasses = new HashSet<>();

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        ClassReader classReader = new ClassReader(bytes);
        VersionVisitor visitor = new VersionVisitor();
        classReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        return bytes;
    }

    public static void blame(String modId, String cls) {
        naughtyClasses.add(cls);
        naughtyMods.add(modId);
        FMLLog.severe("Unsupported class format in mod %s: class %s", modId, cls);
    }

    public static class VersionVisitor extends ClassVisitor {
        public VersionVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            // Obviously this causes problems with later versions so let's keep it out for now
//            if (version == Opcodes.V1_8 && !JavaUtils.INSTANCE.isJava8() || version == Opcodes.V1_7 && !JavaUtils.INSTANCE.isJava7()) {
//                if (classMap.containsKey(name)) blame(classMap.get(name), name);
//                else orphanNaughtyClasses.add(name);
//            }
        }
    }

    private static void checkPendingNaughty() {
        ImmutableSet.Builder<String> toRemove = ImmutableSet.builder();
        for (String cls : orphanNaughtyClasses) {
            if (classMap.containsKey(cls)) {
                String modId = classMap.get(cls);
                blame(modId, cls);
                toRemove.add(cls);
            }
        }
        orphanNaughtyClasses.removeAll(toRemove.build());
    }

    public static void addClasses(String modId, Set<String> classList) {
        for (String cls : classList) {
            classMap.put(cls, modId);
        }
        checkPendingNaughty();
    }

    public static void onCrash(StringBuilder builder) {
        checkPendingNaughty();
        if (!naughtyClasses.isEmpty()) {
            builder.append("\n*** ATTENTION: detected classes with unsupported format ***\n");
            builder.append("*** DO NOT SUBMIT THIS CRASH REPORT TO FORGE ***\n\n");
            if (!naughtyMods.isEmpty()) {
                builder.append("Contact authors of the following mods: \n");
                for (String modId : naughtyMods) {
                    builder.append("  ").append(modId).append("\n");
                }
            }
            if (!orphanNaughtyClasses.isEmpty()) {
                builder.append("Unidentified unsupported classes: \n");
                for (String cls : orphanNaughtyClasses) {
                    builder.append("  ").append(cls).append("\n");
                }
            }
            builder.append('\n');
        }
    }
}

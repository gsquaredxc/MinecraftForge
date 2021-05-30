package net.minecraftforge.patching;

import net.minecraftforge.launch.loader.transformer.ForgeTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Iterator;

public class InvokeDynamicTransformer implements ForgeTransformer {
    private final String[] exclusions = new String[]{"jdk.internal.", "org.objectweb.", "org.apache.logging.", "com.google.", "net.minecraftforge."};

    @Override
    public byte[] transform(String name, byte[] bytes) {
        try {
            // Stop ClassCircularityException
            for (String exclusion : exclusions) {
                if (name.startsWith(exclusion)) {
                    return bytes;
                }
            }

            ClassReader reader = new ClassReader(bytes);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);

            for (MethodNode method : classNode.methods) {
                InsnList instructions = method.instructions;

                Iterator<AbstractInsnNode> nodeIterator = instructions.iterator();

                while (nodeIterator.hasNext()) {
                    AbstractInsnNode insn = nodeIterator.next();
                    if (insn instanceof InvokeDynamicInsnNode) {
                        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;

                        int index = 0;
                        for (Object obj : indy.bsmArgs) {
                            if (obj instanceof Handle) {
                                Handle handle = (Handle) obj;
                                if (handle.isInterface()) {
                                    indy.bsmArgs[index] = new Handle(Opcodes.H_INVOKEINTERFACE, handle.getOwner(), handle.getName(), handle.getDesc(), true);
                                }
                            }
                            index++;
                        }
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);

            return writer.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            return bytes;
        }
    }
}

package net.minecraftforge.patching;

import net.minecraftforge.launch.loader.transformer.ForgeTransformer;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.List;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

import java.util.ListIterator;

public class MixinDoubleMadness implements ForgeTransformer {
    @Override
    public byte[] transform(String name, byte[] bytes) {
        if (name.equals("org.spongepowered.asm.util.JavaVersion")) {
            ClassReader reader = new ClassReader(bytes);
            ClassNode classNode = new ClassNode();

            reader.accept(classNode, ClassReader.EXPAND_FRAMES);

            for (MethodNode methodNode : classNode.methods) {
                if (methodNode.name.equals("resolveCurrentVersion")) {
                    InsnList list = methodNode.instructions;
                    ListIterator<AbstractInsnNode> nodeIterator = list.iterator();
                    while (nodeIterator.hasNext()) {
                        AbstractInsnNode insnNode = nodeIterator.next();

                        if (insnNode instanceof LdcInsnNode && ((LdcInsnNode) insnNode).cst instanceof String) {
                            if (((String)((LdcInsnNode) insnNode).cst).equals("[0-9]+\\.[0-9]+")) {

                                list.set(insnNode, new LdcInsnNode("^.?.?(?<=\\.|^)([0-9]+)"));
                                break;
                            }
                        }
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);

            return writer.toByteArray();
        }

        return bytes;
    }
}
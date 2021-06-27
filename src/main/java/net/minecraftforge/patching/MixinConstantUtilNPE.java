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

public class MixinConstantUtilNPE implements ForgeTransformer {
    @Override
    public byte[] transform(String name, byte[] bytes) {
        if (name.equals("org.spongepowered.asm.util.Constants")) {
            ClassReader reader = new ClassReader(bytes);
            ClassNode classNode = new ClassNode();

            reader.accept(classNode, ClassReader.EXPAND_FRAMES);

            for (MethodNode methodNode : classNode.methods) {
                if (methodNode.name.equals("<clinit>")) {
                    InsnList list = methodNode.instructions;
                    ListIterator<AbstractInsnNode> nodeIterator = list.iterator();
                    while (nodeIterator.hasNext()) {
                        AbstractInsnNode insnNode = nodeIterator.next();

                        if (insnNode instanceof LdcInsnNode && ((LdcInsnNode) insnNode).cst instanceof Type) {
                            if (((Type)((LdcInsnNode) insnNode).cst).getInternalName().equals("org/spongepowered/asm/mixin/Mixin")) {
                                AbstractInsnNode invokePackage = insnNode.getNext();
                                AbstractInsnNode invokeName = invokePackage.getNext();

                                list.set(insnNode, new LdcInsnNode("org.spongepowered.asm.mixin.Mixin"));
                                list.remove(invokePackage);
                                list.remove(invokeName);
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
package net.minecraftforge.patching;

import net.minecraftforge.launch.loader.transformer.ForgeTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class ClassReaderTransformer implements ForgeTransformer {
    @Override
    public byte[] transform(String name, byte[] bytes) {
        if (name.equals("org.spongepowered.asm.lib.ClassReader")) {
            ClassReader reader = new ClassReader(bytes);
            ClassNode classNode = new ClassNode();

            reader.accept(classNode, ClassReader.EXPAND_FRAMES);

            for (MethodNode methodNode : classNode.methods) {
                InsnList list = methodNode.instructions;

                ListIterator<AbstractInsnNode> nodeIterator = list.iterator();

                while (nodeIterator.hasNext()) {
                    AbstractInsnNode insnNode = nodeIterator.next();


                    if (insnNode instanceof TypeInsnNode && insnNode.getOpcode() == Opcodes.NEW) {
                        if (((TypeInsnNode) insnNode).desc.equals("java/lang/IllegalArgumentException")) {
                            AbstractInsnNode dup = insnNode.getNext();
                            AbstractInsnNode invokeSpecial = dup.getNext();
                            AbstractInsnNode aThrow = invokeSpecial.getNext();

                            list.remove(insnNode);
                            list.remove(dup);
                            list.remove(invokeSpecial);
                            list.remove(aThrow);
                            break;
                        }
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
        }

        return bytes;
    }
}

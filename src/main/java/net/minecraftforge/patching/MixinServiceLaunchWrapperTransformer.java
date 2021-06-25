package net.minecraftforge.patching;

import net.minecraftforge.launch.loader.transformer.ForgeTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.ListIterator;

public class MixinServiceLaunchWrapperTransformer implements ForgeTransformer {
    @Override
    public byte[] transform(String name, byte[] bytes) {
        if (name.equals("org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper")) {
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();

            reader.accept(node, ClassReader.EXPAND_FRAMES);

            for (MethodNode methodNode : node.methods) {
                if (methodNode.name.equals("getClassBytes")) {
                    InsnList list = methodNode.instructions;
                    Iterator<AbstractInsnNode> iterator = list.iterator();

                    while (iterator.hasNext()) {
                        AbstractInsnNode insnNode = iterator.next();

                        // Remove the cast to URLClassLoader as AppClassLoader on 9+ does not use URLClassLoader
                        if (insnNode instanceof TypeInsnNode && insnNode.getOpcode() == Opcodes.CHECKCAST && ((TypeInsnNode) insnNode).desc.equals("java/net/URLClassLoader")) {
                            list.remove(insnNode);
                        }

                        // Remove references to URLClassLoader in the getResourceAsStream invocation.
                        if (insnNode instanceof MethodInsnNode && insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) insnNode).owner.equals("java/net/URLClassLoader")) {
                            ((MethodInsnNode) insnNode).owner = "java/lang/ClassLoader";
                            break;
                        }
                    }

                    // Fix references to URLClassLoader in the LVT. This is just to make the bytecode look nicer.
                    for (LocalVariableNode variable : methodNode.localVariables) {
                        if (variable.desc.equals("Ljava/net/URLClassLoader;")) {
                            variable.desc = "Ljava/lang/ClassLoader;";
                            break;
                        }
                    }

                    break;
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            return writer.toByteArray();
        }

        return bytes;
    }

    private void doASMTransformation(ClassNode node) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals("<init>")) {
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
        }
    }
}

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
                System.out.println(methodNode.name);
                if (methodNode.name.equals("<clinit>")) {
                    System.out.println("we made it1");
                    InsnList list = methodNode.instructions;
                    System.out.println("we made it2");
                    ListIterator<AbstractInsnNode> nodeIterator = list.iterator();
                    System.out.println("we made it3");
                    System.out.println(list.size());
                    while (nodeIterator.hasNext()) {
                        AbstractInsnNode insnNode = nodeIterator.next();
                        System.out.println(insnToString(insnNode));

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
                        System.out.println(insnToString(insnNode));
                    }
                    System.out.println(list.size());
                    for(int i = 0; i< list.size(); i++){
                        System.out.print(insnToString(list.get(i)));
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);

            return writer.toByteArray();
        }

        return bytes;
    }

    public static String insnToString(AbstractInsnNode insn){
        insn.accept(mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    private static Printer printer = new Textifier();
    private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);
}
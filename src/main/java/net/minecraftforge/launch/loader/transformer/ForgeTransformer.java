package net.minecraftforge.launch.loader.transformer;

public interface ForgeTransformer {
    byte[] transform(String name, byte[] bytes);
}

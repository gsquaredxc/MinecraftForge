package net.minecraftforge.fml.common.registry;

import com.google.common.base.Throwables;
import dev.xdark.deencapsulation.Deencapsulation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.utils.JavaUtils;
import org.apache.logging.log4j.Level;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * Internal class used in tracking {@link net.minecraftforge.fml.common.registry.GameRegistry.ItemStackHolder} references
 *
 * @author cpw
 */
class ItemStackHolderRef {
    private final Field field;
    private final String itemName;
    private final int meta;
    private final String serializednbt;

    private static Unsafe unsafe;

    ItemStackHolderRef(Field field, String itemName, int meta, String serializednbt) {
        this.field = field;
        this.itemName = itemName;
        this.meta = meta;
        this.serializednbt = serializednbt;
    }

    public void apply() {
        ItemStack is;
        try {
            is = GameRegistry.makeItemStack(itemName, meta, 1, serializednbt);
        } catch (RuntimeException e) {
            FMLLog.getLogger().log(Level.ERROR, "Caught exception processing itemstack {},{},{} in annotation at {}.{}", itemName, meta, serializednbt, field.getClass().getName(), field.getName());
            throw e;
        }
        try {
            Object base = unsafe.staticFieldBase(field);
            long offset = unsafe.staticFieldOffset(field);
            unsafe.putObject(base, offset, is);
        } catch (Exception e) {
            FMLLog.getLogger().log(Level.WARN, "Unable to set {} with value {},{},{}", this.field, this.itemName, this.meta, this.serializednbt);
        }
    }

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}

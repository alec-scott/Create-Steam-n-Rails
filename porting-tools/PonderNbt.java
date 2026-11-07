import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/**
 * Dumps or converts Ponder scene files from pre-1.21 block entity data to what Create Fly reads on 26.1.
 * Positions become int arrays, {V:[...]} vectors become lists, item stacks use count and drop air, upper case enum
 * names become lower case, and empty old-format tanks are removed.
 * Run with Minecraft and its libraries on the classpath: java -cp ... PonderNbt.java dump in.nbt | convert in.nbt out.nbt
 */
public class PonderNbt {
    public static void main(String[] a) throws Exception {
        CompoundTag root = NbtIo.readCompressed(Path.of(a[1]), NbtAccounter.unlimitedHeap());
        ListTag blocks = root.getList("blocks").orElseThrow();
        if (a[0].equals("dump")) {
            Set<String> seen = new LinkedHashSet<>();
            blocks.forEach(t -> ((CompoundTag) t).getCompound("nbt").ifPresent(n -> {
                if (seen.add(n.getStringOr("id", "?"))) System.out.println(n);
            }));
            return;
        }
        int[] changed = {0};
        for (Tag t : blocks) {
            CompoundTag b = (CompoundTag) t;
            Optional<CompoundTag> nbt = b.getCompound("nbt");
            if (nbt.isEmpty()) continue;
            String before = nbt.get().toString();
            CompoundTag after = (CompoundTag) conv(nbt.get(), nbt.get().getStringOr("id", ""));
            if (after.getStringOr("id", "").equals("create:deployer")) {
                after.remove("Inventory");
                after.remove("Owner");
            }
            if (after.getStringOr("id", "").equals("create:toolbox")) after.getCompound("Inventory").ifPresent(i -> i.remove("Items"));
            b.put("nbt", after);
            if (!before.equals(after.toString())) changed[0]++;
        }
        NbtIo.writeCompressed(root, Path.of(a[2]));
        System.out.println(a[1] + ": converted " + changed[0] + " block entities");
    }

    static Tag conv(Tag t, String be) {
        if (t instanceof CompoundTag c) {
            Set<String> keys = c.keySet();
            if (keys.size() == 3 && keys.containsAll(List.of("X", "Y", "Z")))
                return new IntArrayTag(new int[]{c.getIntOr("X", 0), c.getIntOr("Y", 0), c.getIntOr("Z", 0)});
            if (keys.size() == 1 && keys.contains("V")) return c.get("V");
            if (keys.contains("id") && keys.contains("Count")) {
                String id = c.getStringOr("id", "");
                int count = c.getIntOr("Count", 0);
                if (id.equals("minecraft:air") || count <= 0) return null;
                CompoundTag item = new CompoundTag();
                item.putString("id", id);
                item.putInt("count", count);
                c.getByte("Slot").ifPresent(s -> item.putByte("Slot", s));
                return item;
            }
            if (keys.contains("Variant") && keys.contains("Amount")) {
                String fluid = c.getCompound("Variant").flatMap(v -> v.getString("fluid")).orElse("minecraft:empty");
                long amount = c.getLongOr("Amount", 0);
                if (fluid.equals("minecraft:empty") || amount <= 0) return null;
                CompoundTag f = new CompoundTag();
                f.putString("id", fluid);
                f.putInt("amount", (int) amount);
                return f;
            }
            CompoundTag out = new CompoundTag();
            for (String k : new ArrayList<>(keys)) {
                Tag v = c.get(k);
                if (k.equals("SecondaryTargetTrack")) {
                    out.put(k, v);
                    continue;
                }
                if (k.equals("Material") && v instanceof StringTag s && !s.value().contains(":"))
                    v = StringTag.valueOf("railways:" + s.value());
                Tag n = conv(v, be);
                if (n != null) out.put(k, n);
            }
            return out;
        }
        if (t instanceof ListTag l) {
            ListTag out = new ListTag();
            for (Tag e : l) {
                Tag n = conv(e, be);
                if (n != null) out.add(n);
            }
            return out;
        }
        if (t instanceof StringTag s && s.value().matches("[A-Z][A-Z_]+"))
            return StringTag.valueOf(s.value().toLowerCase(Locale.ROOT));
        return t;
    }
}

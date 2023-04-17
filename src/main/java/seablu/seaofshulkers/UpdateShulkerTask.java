package seablu.seaofshulkers;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import static org.bukkit.Bukkit.getLogger;
import static seablu.seaofshulkers.SeaOfShulkers.updateTileEntity;

public class UpdateShulkerTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final World world;
    private final Location shulkerLocation;

    public UpdateShulkerTask(JavaPlugin plugin, World world, Location shulkerLocation) {
        this.plugin = plugin;
        this.world = world;
        this.shulkerLocation = shulkerLocation;
    }

    @Override
    public void run() {
        Block facingBlock = world.getBlockAt(shulkerLocation);

        if (facingBlock != null && facingBlock.getType() == Material.SHULKER_BOX) {
            ShulkerBox shulker = (ShulkerBox) facingBlock.getState();
            if (shulker != null) {
                NbtCompound compound = SeaOfShulkers.buildShulkerNbt(shulker);
                updateTileEntity(shulker.getLocation(), compound);
            } else {
                getLogger().info("not a shulker entity");
            }
        }
    }

}

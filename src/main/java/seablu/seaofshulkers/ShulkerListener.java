package seablu.seaofshulkers;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import static org.bukkit.Bukkit.getLogger;
import static seablu.seaofshulkers.SeaOfShulkers.buildShulkerNbt;
import static seablu.seaofshulkers.SeaOfShulkers.updateTileEntity;

public class ShulkerListener implements Listener {
    Block block;
    Entity entity;

    private final SeaOfShulkers plugin;

    public ShulkerListener(SeaOfShulkers plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        InventoryView inventory = event.getPlayer().getOpenInventory();
        if (inventory.getType() == InventoryType.SHULKER_BOX){
            block = event.getInventory().getLocation().getBlock();

            if (block != null && block.getType() == Material.SHULKER_BOX){
                ShulkerBox shulker = (ShulkerBox) block.getState();
                if (shulker != null) {
                    shulker.update(true);

                    NbtCompound compound = buildShulkerNbt(shulker);
                    updateTileEntity(shulker.getLocation(), compound);
                }
                else{
                    getLogger().info("not a shulker entity");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnBlockPlacedEvent(BlockPlaceEvent event){
        Block block = event.getBlock();
        if (block != null && block.getType() == Material.SHULKER_BOX){
            ShulkerBox shulker = (ShulkerBox) block.getState();
            if (shulker != null) {
                NbtCompound compound = buildShulkerNbt(shulker);
                updateTileEntity(shulker.getLocation(), compound);
            }
            else{
                getLogger().info("not a shulker entity");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnDispenseEvent(BlockDispenseEvent event){
        Block block = event.getBlock();
        if (block != null && block.getType() == Material.DISPENSER){
            Vector facingDirection = ((Directional)block.getBlockData()).getFacing().getDirection();

            Location facingLocation = (block.getLocation().toVector().add(facingDirection)).toLocation(block.getWorld());
            BukkitTask task = new UpdateShulkerTask(this.plugin, block.getWorld(), facingLocation).runTaskLater(this.plugin, 1);
        }
    }


}
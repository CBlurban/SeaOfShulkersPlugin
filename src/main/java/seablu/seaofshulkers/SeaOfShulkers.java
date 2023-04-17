package seablu.seaofshulkers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketListenerPlayOut;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

import static com.comphenix.protocol.utility.MinecraftReflection.getBlockEntityTypeClass;
import static seablu.seaofshulkers.SeaOfShulkers.updateTileEntity;


public final class SeaOfShulkers extends JavaPlugin {

    @Override
    public void onEnable() {
        new ShulkerListener(this);

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        // Listen for chunk packets
        manager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.MAP_CHUNK
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                Optional chunkXOption = packet.getIntegers().optionRead(0);
                Optional chunkYOption = packet.getIntegers().optionRead(1);
                if (chunkXOption.isPresent() && chunkYOption.isPresent()){
                    int chunkX = (int)chunkXOption.get();
                    int chunkY = (int)chunkYOption.get();

                    Chunk packetChunk = player.getWorld().getChunkAt(chunkX, chunkY);
                    BlockState[] chunkTileEntities = packetChunk.getTileEntities();

                    if (chunkTileEntities.length <= 0) {
                        return;
                    }

                    Optional chunkOptionRead = packet.getLevelChunkData().optionRead(0);
                    if (!chunkOptionRead.isPresent()) {
                        return;
                    }

                    WrappedLevelChunkData.ChunkData ChunkData = (WrappedLevelChunkData.ChunkData) chunkOptionRead.get();
                    List<WrappedLevelChunkData.BlockEntityInfo> blockEntityInfo = ChunkData.getBlockEntityInfo();

                    MinecraftKey shulkerKey = new MinecraftKey("shulker_box");

                    ArrayList<WrappedLevelChunkData.BlockEntityInfo> newBlockEntityInfo = new ArrayList<>();

                    for (WrappedLevelChunkData.BlockEntityInfo EntityInfo : blockEntityInfo) {
                        if (EntityInfo.getTypeKey().equals(shulkerKey)) {

                            for (BlockState entity : chunkTileEntities) {
                                if (entity.getType() != Material.SHULKER_BOX) {
                                    continue;
                                }

                                ShulkerBox shulkerEntity = (ShulkerBox) entity;
                                int sectionX = (int) Math.signum(shulkerEntity.getX()) * shulkerEntity.getX() % 16;
                                int sectionZ = (int) Math.signum(shulkerEntity.getZ()) * shulkerEntity.getZ() % 16;

                                if (!(sectionX == EntityInfo.getSectionX() && sectionZ == EntityInfo.getSectionZ() && shulkerEntity.getY() == EntityInfo.getY())){
                                    continue;
                                }

                                // Chunk entity is same as packet entity so replace the shulker nbt data with one containing custom name
                                NbtCompound newCompound = buildShulkerNbt(shulkerEntity);

                                if (newCompound != null) {
                                    WrappedLevelChunkData.BlockEntityInfo newEntityInfo = WrappedLevelChunkData.BlockEntityInfo.fromValues(sectionX, sectionZ, shulkerEntity.getY(), shulkerKey, newCompound);
                                    //System.out.println("adding new block entity [" + shulkerKey.getFullKey() + "]: " + newCompound);
                                    if (newEntityInfo != null) {
                                        EntityInfo = newEntityInfo;
                                    }
                                }
                            }
                        }
                        newBlockEntityInfo.add(EntityInfo);
                    }

                    ChunkData.setBlockEntityInfo(newBlockEntityInfo);
                }
            }
        });

        // Listen for chunk packets
        manager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.TILE_ENTITY_DATA
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                Optional blockLocationOptional = packet.getBlockPositionModifier().optionRead(0);
                if (blockLocationOptional.isPresent()) {
                    BlockPosition pos = (BlockPosition) blockLocationOptional.get();
                    Block block = player.getWorld().getBlockAt(pos.toLocation(player.getWorld()));

                    // If updated block is shulker then replace the packet with one containing nbt
                    if (block != null && block.getType() == Material.SHULKER_BOX) {
                        ShulkerBox shulker = (ShulkerBox) block.getState();
                        if (shulker != null) {
                            NbtCompound compound = buildShulkerNbt(shulker);
                            packet = getTileEntityUpdatePacket(pos.toLocation(block.getWorld()), compound);
                        }
                    }
                }

                event.setPacket(packet);
            }
        });
    }

    static public NbtCompound buildShulkerNbt(ShulkerBox shulkerEntity){
        NbtCompound newCompound = NbtFactory.ofCompound("");
        newCompound.put("CustomName", "{\"text\":\"" + shulkerEntity.getCustomName() + "\"}");

        // Write in items

        // Only add the items that are relevant for SeaOfShulkers display
        List<Integer> RelevantItemSlots = Arrays.asList(0, 2, 5, 10, 15, 20, 25);

        // Shulker Interior Items
        if (!shulkerEntity.getInventory().isEmpty()) {
            ArrayList<NbtCompound> ItemList = new ArrayList<>();
            for (int pos : RelevantItemSlots) {
                ItemStack stack = shulkerEntity.getInventory().getItem(pos);
                ;

                if (stack != null) {
                    NbtBase<?> countNbt = NbtFactory.of("Count", (byte)1 );
                    NbtBase<?> slotNbt = NbtFactory.of("Slot", (byte)pos );
                    NbtBase<?> idNbt = NbtFactory.of("id", stack.getType().getKey().asString());
                    NbtCompound itemNbt = NbtFactory.ofCompound("");
                    itemNbt.put(countNbt);
                    itemNbt.put(slotNbt);
                    itemNbt.put(idNbt);
                    ItemList.add(itemNbt);
                }
            }

            newCompound.put("Items", NbtFactory.ofList("Items", ItemList));
        }

        return newCompound;
    }

    static public void updateTileEntity(Player player, Location pos, NbtCompound compound) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        PacketContainer updatePacket = getTileEntityUpdatePacket(pos, compound);

        manager.sendServerPacket(player, updatePacket);
    }

    static public void updateTileEntity(Player player, TileEntity entity) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        Packet<PacketListenerPlayOut> updatePacket = PacketPlayOutTileEntityData.a(entity);

        manager.sendServerPacket(player, (PacketContainer)updatePacket);
    }

    static public void updateTileEntity(Location pos, NbtCompound compound) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        PacketContainer updatePacket = getTileEntityUpdatePacket(pos, compound);

        manager.broadcastServerPacket(updatePacket, pos, 16*8);
    }

    static public PacketContainer getTileEntityUpdatePacket(Location pos, NbtCompound compound) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        PacketContainer updatePacket = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);

        updatePacket.getNbtModifier().write(0, compound);
        updatePacket.getBlockPositionModifier().write(0, new BlockPosition(pos.toVector()));
        updatePacket.getStructures().withType(getBlockEntityTypeClass()).write(0, TileEntityTypes.x);

        return updatePacket;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

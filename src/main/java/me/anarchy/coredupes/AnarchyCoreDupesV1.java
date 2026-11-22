package me.anarchy.coredupes;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.entity.item.EntityMinecartAbstract;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.player.PlayerDropItemEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.inventory.ChestInventory;
import cn.nukkit.inventory.DoubleChestInventory;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AnarchyCoreDupesV1 extends PluginBase implements Listener {

    private final Set<UUID> activeChestDesyncs = new HashSet<>();
    private final Map<UUID, Item> savedPlayerDataDupes = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final double MIN_CHEST_DISTANCE = 9.0; // ~10 blocks away
    private static final long COOLDOWN_MS = 5000;         // 5s cooldown

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("2b2be-dupes-core enabled! (Corrupt Chest, Inventory Slot, Player Data)");
    }

    private boolean checkCooldown(Player player) {
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) {
            return false;
        }
        cooldowns.put(player.getUniqueId(), now);
        return true;
    }

    // =========================================================
    // 1. CORRUPT CHEST DUPE (Minecart + Packet-Move 10 blocks)
    // =========================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (block == null || (block.getId() != BlockID.CHEST && block.getId() != BlockID.TRAPPED_CHEST)) {
            return;
        }

        if (player.riding instanceof EntityMinecartAbstract) {
            double dist = player.riding.distance(block);
            if (dist >= MIN_CHEST_DISTANCE && dist <= 32.0) {
                event.setCancelled(false);
                activeChestDesyncs.add(player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChestOpen(InventoryOpenEvent event) {
        Player player = event.getPlayer();
        Inventory inv = event.getInventory();

        if (!(inv instanceof ChestInventory) && !(inv instanceof DoubleChestInventory)) {
            return;
        }

        if (player.riding instanceof EntityMinecartAbstract) {
            Vector3 chestPos = getChestPosition(inv);
            if (chestPos != null) {
                double dist = player.riding.distance(chestPos);
                if (dist >= MIN_CHEST_DISTANCE && dist <= 32.0) {
                    event.setCancelled(false);
                    activeChestDesyncs.add(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        Player player = event.getPlayer();
        Inventory inv = event.getInventory();

        if (!activeChestDesyncs.remove(player.getUniqueId())) {
            return;
        }

        if (!(inv instanceof ChestInventory) && !(inv instanceof DoubleChestInventory)) {
            return;
        }

        List<Item> shulkersToDupe = new ArrayList<>();
        for (Item item : inv.getContents().values()) {
            if (item != null && (item.getId() == Item.UNDYED_SHULKER_BOX || item.getId() == Item.SHULKER_BOX)) {
                shulkersToDupe.add(item.clone());
            }
        }

        if (!shulkersToDupe.isEmpty() && checkCooldown(player)) {
            Vector3 chestPos = getChestPosition(inv);
            for (Item clone : shulkersToDupe) {
                Item[] leftover = inv.addItem(clone);
                if (leftover.length > 0 && chestPos != null) {
                    player.getLevel().dropItem(chestPos.add(0.5, 1.0, 0.5), leftover);
                }
            }
        }
    }

    // =========================================================
    // 2. INVENTORY SLOT DUPE (Hotbar Slot 1 & Slot 9 + Crouch Drop)
    // =========================================================
    @EventHandler
    public void onInventorySlotDupe(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        if (player.getInventory().getHeldItemIndex() != 0) return;

        Item dropped = event.getItem();
        Item slotNine = player.getInventory().getItem(8);

        if (dropped != null && slotNine != null && dropped.equals(slotNine, true, false)) {
            if (checkCooldown(player)) {
                player.getInventory().setItem(0, dropped.clone());
            }
        }
    }

    // =========================================================
    // 3. PLAYER DATA DUPE (Logout inside Portal with Offhand Item)
    // =========================================================
    @EventHandler
    public void onPlayerDataSave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Item offhand = player.getOffhandInventory().getItem(0);

        if (offhand == null || offhand.getId() == Item.AIR) return;

        int blockId = player.getLevel().getBlockIdAt(player.getFloorX(), player.getFloorY(), player.getFloorZ());
        if (blockId == BlockID.NETHER_PORTAL) {
            savedPlayerDataDupes.put(player.getUniqueId(), offhand.clone());
        }
    }

    @EventHandler
    public void onPlayerDataRestore(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Item dupedItem = savedPlayerDataDupes.remove(player.getUniqueId());

        if (dupedItem != null) {
            player.getLevel().dropItem(player.getPosition(), dupedItem);
        }
    }

    private Vector3 getChestPosition(Inventory inv) {
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof BlockEntityChest) {
            return ((BlockEntityChest) holder).getLocation();
        } else if (holder instanceof DoubleChestInventory) {
            InventoryHolder left = ((DoubleChestInventory) holder).getLeftSide().getHolder();
            if (left instanceof BlockEntityChest) {
                return ((BlockEntityChest) left).getLocation();
            }
        }
        return null;
    }
}

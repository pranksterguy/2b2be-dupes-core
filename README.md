# 2b2be-dupes-core
Core Files of duplication glitches of 2b2e
# 2b2be-dupes-core

A lightweight Nukkit (`NukkitPetteriM1Edition`) anarchy plugin that recreates classic Bedrock/PE duplication mechanics safely without corrupting chunk region files or crashing the server.

## Active Dupes

### 1. Corrupt Chest Dupe (Minecart Packet-Move)
* **How it works:** Place a chest, sit inside a **Minecart** at least **9–10 blocks away**, use **Packet-Move / Freecam** to reach and open the chest, place **Shulker Boxes** inside, and close the chest.
* **Result:** Triggers a simulated container desync that clones the shulker boxes inside the chest (or drops overflow on top).

### 2. Inventory Slot Dupe
* **How it works:** Place matching items in **Hotbar Slot 1** and **Hotbar Slot 9**, crouch (`sneak`), and drop (`Q`) the item from Slot 1.
* **Result:** Drops the item while restoring a cloned copy back into Slot 1.

### 3. Player Data Dupe
* **How it works:** Hold an item or shulker box in your **Offhand**, stand inside an active **Nether Portal**, and disconnect from the server.
* **Result:** Simulates a `.dat` player-save rollback—dropping a cloned copy of your offhand item at your feet when you log back in.

---

## Requirements
* **Server Software:** [NukkitPetteriM1Edition](https://github.com) (Java 17 or Java 21)
* **Client:** Minecraft Bedrock / Pocket Edition

## Installation
1. Download `2b2be-dupes-core.jar` from the **Actions** (or **Releases**) tab.
2. Upload the `.jar` file into your server's `plugins/` folder.
3. Restart your server.
   

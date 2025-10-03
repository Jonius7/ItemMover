package jonius7.itemmover.gui;

import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container for the ItemMover block.
 * Has 9 ghost slots for filtering, 1 real internal slot, and player inventory.
 */
public class ContainerItemMover extends Container {

    private final TileEntityItemMover tile;

    public ContainerItemMover(InventoryPlayer playerInv, TileEntityItemMover tile) {
        this.tile = tile;

        // --- Pull Ghost slots (3x3 grid) ---
        int pullStartX = 8;
        int pullStartY = 35;
        int pullSpacing = 32;

        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = pullStartX + col * pullSpacing;
            int y = pullStartY + row * pullSpacing;

            this.addSlotToContainer(new SlotGhostPull(tile.getPullGhostInventory(), i, x, y));
        }
        
        // --- Push Ghost slots (3x3 grid) ---
        int pushStartX = 135;
        int pushStartY = 35;
        int pushSpacing = 32;

        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = pushStartX + col * pushSpacing;
            int y = pushStartY + row * pushSpacing;

            this.addSlotToContainer(new SlotGhostPush(tile.getPushGhostInventory(), i, x, y));
        }

     // --- Real internal slots (18 slots above player inventory) ---
        int internalStartX = 48;
        int internalStartY = 130; // adjust as needed
        int internalCols = 9;
        int internalRows = 2;
        int internalSpacing = 18;

        for (int row = 0; row < internalRows; row++) {
            for (int col = 0; col < internalCols; col++) {
                int invIndex = row * internalCols + col; // 0..17
                int x = internalStartX + col * internalSpacing;
                int y = internalStartY + row * internalSpacing;
                this.addSlotToContainer(new SlotInternal(tile.getInternalInventory(), invIndex, x, y));
            }
        }


        // --- Player Inventory (3 rows x 9 columns) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 8 + 40 + col * 18;
                int y = 84 + 90 + row * 18;
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        // --- Player Hotbar (1 row x 9 columns) ---
        for (int col = 0; col < 9; col++) {
            int x = 8 + 40 + col * 18;
            int y = 142 + 90;
            this.addSlotToContainer(new Slot(playerInv, col, x, y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUseableByPlayer(player);
    }

    // --- Shift-click handling ---
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            int ghostPullStart = 0;
            int ghostPullEnd = 9;
            int ghostPushStart = 9;
            int ghostPushEnd = 18;
            int internalStart = 18;
            int internalEnd = 36;
            int playerStart = 36;
            int playerEnd = this.inventorySlots.size();

            // --- Internal slots -> player inventory ---
            if (index >= internalStart && index < internalEnd) {
                if (!mergeItemStack(stackInSlot, playerStart, playerEnd, true)) return null;
            }
            // --- Player inventory -> internal slots ---
            else if (index >= playerStart) {
                if (!mergeItemStack(stackInSlot, internalStart, internalEnd, false)) return null;
            }
            // --- Ghost slots are not shift-clickable ---
            else if (index >= ghostPullStart && index < ghostPushEnd) {
                return null;
            }

            if (stackInSlot.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }

        return itemstack;
    }


    @Override
    public ItemStack slotClick(int slotId, int mouseButton, int modifier, EntityPlayer player) {
        if (slotId < 0 || slotId >= this.inventorySlots.size()) return super.slotClick(slotId, mouseButton, modifier, player);

        Slot slot = (Slot) this.inventorySlots.get(slotId);

        if (slot instanceof SlotGhost) {
            ItemStack current = slot.getStack();
            ItemStack held = player.inventory.getItemStack();

            if (mouseButton == 0) { // Left click
                if (held != null) {
                    if (current != null && current.isItemEqual(held) &&
                        ItemStack.areItemStackTagsEqual(current, held)) {
                        current.stackSize = Math.min(current.getMaxStackSize(), current.stackSize + 1);
                    } else {
                        ItemStack copy = held.copy();
                        copy.stackSize = 1;
                        slot.putStack(copy);
                    }
                } else slot.putStack(null);
            } else if (mouseButton == 1) { // Right click
                if (held != null) {
                    if (current != null && current.isItemEqual(held) &&
                        ItemStack.areItemStackTagsEqual(current, held)) {
                        current.stackSize--;
                        if (current.stackSize <= 0) slot.putStack(null);
                    } else {
                        ItemStack copy = held.copy();
                        copy.stackSize = 1;
                        slot.putStack(copy);
                    }
                } else slot.putStack(null);
            }

            return held;
        }

        return super.slotClick(slotId, mouseButton, modifier, player);
    }
}

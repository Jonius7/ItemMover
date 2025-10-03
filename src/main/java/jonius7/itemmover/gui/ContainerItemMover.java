package jonius7.itemmover.gui;

import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerItemMover extends Container {

    private TileEntityItemMover tile;

    public ContainerItemMover(InventoryPlayer playerInv, TileEntityItemMover tile) {
        this.tile = tile;

        // --- ItemMover internal inventory ---
        //this.addSlotToContainer(new Slot(tile, 0, 8, 35));
        this.addSlotToContainer(new SlotGhost(tile, 0, 8, 35));
        
        
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

            // Internal slot -> player inventory
            if (index == 0) {
                if (!this.mergeItemStack(stackInSlot, 1, 37, true)) {
                    return null;
                }
            } 
            // Player inventory -> internal slot
            else {
                if (!this.mergeItemStack(stackInSlot, 0, 1, false)) {
                    return null;
                }
            }

            if (stackInSlot.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
    
    @Override
    public ItemStack slotClick(int slotId, int mouseButton, int modifier, EntityPlayer player) {
        if (slotId < 0 || slotId >= this.inventorySlots.size()) return null;

        Slot slot = (Slot) this.inventorySlots.get(slotId);

        // Only handle ghost slots
        if (slot instanceof SlotGhost) {
            ItemStack current = slot.getStack();
            ItemStack held = player.inventory.getItemStack();

            if (mouseButton == 0) { // Left click
                if (held != null) {
                    if (current != null && current.isItemEqual(held) &&
                        ItemStack.areItemStackTagsEqual(current, held)) {
                        // Increase count
                        current.stackSize++;
                    } else {
                        // Set new ghost item with count 1
                        ItemStack copy = held.copy();
                        copy.stackSize = 1;
                        slot.putStack(copy);
                    }
                } else if (held == null) {
                    // Clear on left-click empty hand
                    slot.putStack(null);
                }
            } else if (mouseButton == 1) { // Right click
                if (held != null) {
                    if (current != null && current.isItemEqual(held) &&
                        ItemStack.areItemStackTagsEqual(current, held)) {
                        // Decrease count
                        current.stackSize--;
                        if (current.stackSize <= 0) slot.putStack(null);
                    } else {
                        // Set new ghost item with count 1
                        ItemStack copy = held.copy();
                        copy.stackSize = 1;
                        slot.putStack(copy);
                    }
                } else if (held == null) {
                    // Right-click empty hand also clears
                    slot.putStack(null);
                }
            }

            return null; // Don't do normal slotClick processing
        }

        // Normal slotClick for real slots
        return super.slotClick(slotId, mouseButton, modifier, player);
    }
}

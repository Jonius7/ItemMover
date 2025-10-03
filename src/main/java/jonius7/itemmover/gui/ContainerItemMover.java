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

        // --- Ghost slots (3x3 grid) ---
        int startX = 8;
        int startY = 35;
        int spacing = 32;

        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = startX + col * spacing;
            int y = startY + row * spacing;

            this.addSlotToContainer(new SlotGhost(tile, i, x, y));
        }

        // --- Real internal slot (slot 9 in container indexing) ---
        this.addSlotToContainer(new Slot(tile, 0, 140, 35));

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
                        current.stackSize = Math.min(current.getMaxStackSize(), current.stackSize + 1);
                    } else {
                        ItemStack copy = held.copy();
                        copy.stackSize = 1;
                        slot.putStack(copy);
                    }
                } else {
                    // Left-click empty hand clears
                    slot.putStack(null);
                }
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
                } else {
                    // Right-click empty hand clears
                    slot.putStack(null);
                }
            }

            // Important: tell the container the held stack didn't change
            return held;
        }

        // Normal slotClick for real slots
        return super.slotClick(slotId, mouseButton, modifier, player);
    }

}

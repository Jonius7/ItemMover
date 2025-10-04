package jonius7.itemmover.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotGhost extends Slot {

    public SlotGhost(IInventory ignored, int index, int x, int y) {
        // Pass a dummy IInventory since ghosts aren't real storage
        super(ignored, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        // Always allow items to be shown here
        return true;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        // Prevent players from picking items out
        return false;
    }

    @Override
    public void putStack(ItemStack stack) {
        // Don't call super (would try to write to inventory!)
        this.inventory.setInventorySlotContents(this.getSlotIndex(), stack);
        this.onSlotChanged();
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        // Ghost slots don’t split like normal inventories
        ItemStack stack = getStack();
        if (stack != null) {
            ItemStack copy = stack.copy();
            copy.stackSize = Math.min(amount, stack.stackSize);
            // Clear slot completely
            putStack(null);
            return copy;
        }
        return null;
    }

    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
        // Disable pickup logic
    }
}
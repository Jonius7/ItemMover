package jonius7.itemmover.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SimpleInventory implements IInventory {
    private final ItemStack[] stacks;
    private final int size;

    public SimpleInventory(int size, ItemStack[] backing) {
        this.size = size;
        this.stacks = backing;
    }

    @Override
    public int getSizeInventory() {
        return size;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return stacks[index];
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        stacks[index] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (stacks[index] != null) {
            if (stacks[index].stackSize <= count) {
                ItemStack itemstack = stacks[index];
                stacks[index] = null;
                markDirty();
                return itemstack;
            }
            ItemStack itemstack = stacks[index].splitStack(count);
            if (stacks[index].stackSize == 0) {
                stacks[index] = null;
            }
            markDirty();
            return itemstack;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        if (stacks[index] != null) {
            ItemStack itemstack = stacks[index];
            stacks[index] = null;
            return itemstack;
        }
        return null;
    }

    @Override
    public String getInventoryName() {
        return "container.simpleInventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64; // important!
    }

    @Override
    public void markDirty() {
        // Optional: notify tile entity if needed
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true; // or do distance checks against a tile entity
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true; // important! internal slots must accept items
    }
}


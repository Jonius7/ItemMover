package jonius7.itemmover.blocks;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;

public class TileEntityItemMover extends TileEntity implements IInventory {

    // --- Internal inventory ---
    private ItemStack[] inventory = new ItemStack[1];

    // --- Configurable fields ---
    private int inputSlot = 0;
    private int outputSlot = 0;
    private int inputSide = 2;  // 0=DOWN,1=UP,2=NORTH,3=SOUTH,4=WEST,5=EAST
    private int outputSide = 3;

    // --- Update each tick ---
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        // --- Pull from input side ---
        IInventory inInv = getAdjacentInventory(inputSide);
        if (inInv != null && inputSlot >= 0 && inputSlot < inInv.getSizeInventory()) {
            ItemStack inStack = inInv.getStackInSlot(inputSlot);
            if (inStack != null) {
                if (inventory[0] == null) {
                    // Internal empty → take 1 item
                    inventory[0] = inInv.decrStackSize(inputSlot, 1);
                    markDirty();
                } else if (inventory[0].isItemEqual(inStack) &&
                           ItemStack.areItemStackTagsEqual(inventory[0], inStack) &&
                           inventory[0].stackSize < Math.min(inventory[0].getMaxStackSize(), getInventoryStackLimit())) {
                    // Same item → take 1 item if there is room
                    inInv.decrStackSize(inputSlot, 1);
                    inventory[0].stackSize += 1;
                    markDirty();
                }
            }
        }

        // --- Push to output side ---
        if (inventory[0] != null) {
            IInventory outInv = getAdjacentInventory(outputSide);
            if (outInv != null) {
                int slot = Math.min(outputSlot, outInv.getSizeInventory() - 1);
                if (slot >= 0) {
                    inventory[0] = insertIntoInventorySlot(outInv, slot, inventory[0]);
                    markDirty();
                }
            }
        }
    }


    // --- Helper: Get adjacent inventory by side ---
    private IInventory getAdjacentInventory(int side) {
        int x = xCoord + Facing.offsetsXForSide[side];
        int y = yCoord + (side == 0 ? -1 : side == 1 ? 1 : 0);
        int z = zCoord + Facing.offsetsZForSide[side];
        TileEntity te = worldObj.getTileEntity(x, y, z);
        return te instanceof IInventory ? (IInventory) te : null;
    }

    // --- Helper: Insert into internal inventory ---
    private void insertIntoInternal(ItemStack stack) {
        if (inventory[0] == null) {
            inventory[0] = stack;
        } else if (inventory[0].isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inventory[0], stack)) {
            int max = Math.min(inventory[0].getMaxStackSize(), getInventoryStackLimit());
            int newSize = inventory[0].stackSize + stack.stackSize;
            if (newSize <= max) {
                inventory[0].stackSize = newSize;
            } else {
                inventory[0].stackSize = max;
                stack.stackSize = newSize - max;
            }
        }
        markDirty();
    }

    /**
     * Tries to insert the given stack into the specified slot of the inventory.
     * Merges if possible, leaves leftover in the returned ItemStack.
     *
     * @param inv   The target inventory
     * @param slot  The target slot
     * @param stack The stack to insert
     * @return The leftover stack (null if completely inserted)
     */
    private ItemStack insertIntoInventorySlot(IInventory inv, int slot, ItemStack stack) {
        if (stack == null || slot < 0 || slot >= inv.getSizeInventory()) return stack;

        ItemStack target = inv.getStackInSlot(slot);

        // Empty slot → insert completely
        if (target == null) {
            inv.setInventorySlotContents(slot, stack);
            return null;
        }

        // Same item & NBT → merge
        if (target.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(target, stack)) {
            int max = Math.min(target.getMaxStackSize(), inv.getInventoryStackLimit());
            int newSize = target.stackSize + stack.stackSize;

            if (newSize <= max) {
                target.stackSize = newSize;
                return null; // completely inserted
            } else {
                target.stackSize = max;
                stack.stackSize = newSize - max; // leftover
                return stack;
            }
        }

        // Different item → cannot insert
        return stack;
    }
    
    // --- Helper: get max valid slot for a given side ---
    public int getMaxSlot(int side) {
        IInventory inv = getAdjacentInventory(side);
        return inv != null ? Math.max(inv.getSizeInventory() - 1, 0) : 0;
    }

    // --- IInventory implementation ---
    @Override
    public int getSizeInventory() { return inventory.length; }
    
    @Override
    public ItemStack getStackInSlot(int slot) { return inventory[slot]; }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (inventory[slot] != null) {
            ItemStack itemstack;
            if (inventory[slot].stackSize <= amount) {
                itemstack = inventory[slot];
                inventory[slot] = null;
                markDirty();
                return itemstack;
            } else {
                itemstack = inventory[slot].splitStack(amount);
                if (inventory[slot].stackSize == 0) inventory[slot] = null;
                markDirty();
                return itemstack;
            }
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    @Override
    public String getInventoryName() { return "container.itemMover"; }

    @Override
    public boolean hasCustomInventoryName() { return false; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean isUseableByPlayer(net.minecraft.entity.player.EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this &&
                player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override public void openInventory() {}
    @Override public void closeInventory() {}
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (inventory[slot] != null) {
            ItemStack stack = inventory[slot];
            inventory[slot] = null;
            return stack;
        }
        return null;
    }

    // --- Config getters/setters ---
    public int getInputSlot() { return inputSlot; }
    public void setInputSlot(int slot) { inputSlot = slot; markDirty(); }

    public int getOutputSlot() { return outputSlot; }
    public void setOutputSlot(int slot) { outputSlot = slot; markDirty(); }

    public int getInputSide() { return inputSide; }
    public void setInputSide(int side) { inputSide = side; markDirty(); }

    public int getOutputSide() { return outputSide; }
    public void setOutputSide(int side) { outputSide = side; markDirty(); }

    // --- NBT save/load ---
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("InputSlot", inputSlot);
        tag.setInteger("OutputSlot", outputSlot);
        tag.setInteger("InputSide", inputSide);
        tag.setInteger("OutputSide", outputSide);
        if (inventory[0] != null) {
            NBTTagCompound itemTag = new NBTTagCompound();
            inventory[0].writeToNBT(itemTag);
            tag.setTag("ItemSlot0", itemTag);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        inputSlot = tag.getInteger("InputSlot");
        outputSlot = tag.getInteger("OutputSlot");
        inputSide = tag.getInteger("InputSide");
        outputSide = tag.getInteger("OutputSide");
        if (tag.hasKey("ItemSlot0")) {
            inventory[0] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("ItemSlot0"));
        }
    }

    // --- Side names helper ---
    public static String getSideName(int side) {
        switch (side) {
            case 0: return "DOWN";
            case 1: return "UP";
            case 2: return "NORTH";
            case 3: return "SOUTH";
            case 4: return "WEST";
            case 5: return "EAST";
            default: return "UNKNOWN";
        }
    }
}

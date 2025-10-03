package jonius7.itemmover.blocks;

import jonius7.itemmover.gui.SimpleInventory;
import jonius7.itemmover.gui.SimpleInventoryGhost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;

public class TileEntityItemMover extends TileEntity implements IInventory {

    // --- Inventories ---
    //private ItemStack[] ghostInventory = new ItemStack[9];     // Ghost slots
	private ItemStack[] ghostPull = new ItemStack[9]; // Pull Ghost Slots
	private ItemStack[] ghostPush = new ItemStack[9]; // Push Ghost Slots
    private ItemStack[] internalInventory = new ItemStack[18]; // Real internal slots

    // --- Configurable fields ---
    private int inputSlot = 0;
    private int outputSlot = 0;
    private int inputSide = 2;
    private int outputSide = 3;
    
    /*
    // --- Update each tick ---
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        // Pull from input side
        IInventory inInv = getAdjacentInventory(inputSide);
        if (inInv != null && inputSlot >= 0 && inputSlot < inInv.getSizeInventory()) {
            ItemStack inStack = inInv.getStackInSlot(inputSlot);
            if (inStack != null) {
                for (int i = 0; i < internalInventory.length; i++) {
                    if (internalInventory[i] == null) {
                        internalInventory[i] = inInv.decrStackSize(inputSlot, 1);
                        markDirty();
                        break;
                    } else if (internalInventory[i].isItemEqual(inStack) &&
                               ItemStack.areItemStackTagsEqual(internalInventory[i], inStack) &&
                               internalInventory[i].stackSize < Math.min(internalInventory[i].getMaxStackSize(), getInventoryStackLimit())) {
                        inInv.decrStackSize(inputSlot, 1);
                        internalInventory[i].stackSize += 1;
                        markDirty();
                        break;
                    }
                }
            }
        }

        // Push to output side
        for (int i = 0; i < internalInventory.length; i++) {
            ItemStack stack = internalInventory[i];
            if (stack != null) {
                IInventory outInv = getAdjacentInventory(outputSide);
                if (outInv != null) {
                    stack = insertIntoInventory(outInv, outputSlot, stack);
                    internalInventory[i] = stack;
                    markDirty();
                }
            }
        }
    }
    

    // --- Helpers ---
    private IInventory getAdjacentInventory(int side) {
        int x = xCoord + Facing.offsetsXForSide[side];
        int y = yCoord + (side == 0 ? -1 : side == 1 ? 1 : 0);
        int z = zCoord + Facing.offsetsZForSide[side];
        TileEntity te = worldObj.getTileEntity(x, y, z);
        return te instanceof IInventory ? (IInventory) te : null;
    }

    private ItemStack insertIntoInventory(IInventory inv, int slot, ItemStack stack) {
        if (stack == null || slot < 0 || slot >= inv.getSizeInventory()) return stack;

        ItemStack target = inv.getStackInSlot(slot);
        if (target == null) {
            inv.setInventorySlotContents(slot, stack);
            return null;
        }

        if (target.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(target, stack)) {
            int max = Math.min(target.getMaxStackSize(), inv.getInventoryStackLimit());
            int total = target.stackSize + stack.stackSize;
            if (total <= max) {
                target.stackSize = total;
                return null;
            } else {
                target.stackSize = max;
                stack.stackSize = total - max;
                return stack;
            }
        }

        return stack;
    }
	*/
    
 // Expose them as sub-inventories:
    public IInventory getInternalInventory() {
        return new SimpleInventory(internalInventory.length, internalInventory);
    }

    public IInventory getPullGhostInventory() {
        return new SimpleInventoryGhost(ghostPull.length);
    }

    public IInventory getPushGhostInventory() {
        return new SimpleInventoryGhost(ghostPush.length);
    }
    
    
    // --- IInventory methods ---
    @Override
    public int getSizeInventory() {
        return internalInventory.length; // Only real inventory
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= internalInventory.length) return null;
        return internalInventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot < 0 || slot >= internalInventory.length) return null;

        ItemStack stack = internalInventory[slot];
        if (stack != null) {
            if (stack.stackSize <= amount) {
                internalInventory[slot] = null;
                markDirty();
                sendUpdatePacket();
                return stack;
            } else {
                ItemStack split = stack.splitStack(amount);
                if (stack.stackSize == 0) internalInventory[slot] = null;
                markDirty();
                sendUpdatePacket();
                return split;
            }
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= internalInventory.length) return;
        internalInventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit())
            stack.stackSize = getInventoryStackLimit();
        markDirty();
        sendUpdatePacket();
    }

    @Override
    public String getInventoryName() { return "container.itemMover"; }

    @Override
    public boolean hasCustomInventoryName() { return false; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this &&
               player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}
    @Override
    public void closeInventory() {}
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot < 0 || slot >= internalInventory.length) return null;
        ItemStack stack = internalInventory[slot];
        internalInventory[slot] = null;
        return stack;
    }

 // --- Ghost inventory access ---
    public int getGhostSize(boolean pull) {
        return pull ? ghostPull.length : ghostPush.length;
    }

    public ItemStack getGhostStack(int slot, boolean pull) {
        if (pull) {
            return ghostPull[slot];
        } else {
            return ghostPush[slot];
        }
    }

    public void setGhostStack(int slot, ItemStack stack, boolean pull) {
        if (pull) {
            ghostPull[slot] = stack;
        } else {
            ghostPush[slot] = stack;
        }
        markDirty();
        sendUpdatePacket();
    }

    // Internal Inventory get/set
    public ItemStack getInternalStack(int slot) { return internalInventory[slot]; }
    public void setInternalStack(int slot, ItemStack stack) { internalInventory[slot] = stack; }
    
    // Ghost slots methods
    public ItemStack getGhostPull(int slot) {
        if (slot < 0 || slot >= ghostPull.length) return null;
        return ghostPull[slot];
    }

    public void setGhostPull(int slot, ItemStack stack) {
        if (slot < 0 || slot >= ghostPull.length) return;
        ghostPull[slot] = stack;
        markDirty();
    }

    public ItemStack getGhostPush(int slot) {
        if (slot < 0 || slot >= ghostPush.length) return null;
        return ghostPush[slot];
    }

    public void setGhostPush(int slot, ItemStack stack) {
        if (slot < 0 || slot >= ghostPush.length) return;
        ghostPush[slot] = stack;
        markDirty();
    }


    // --- TileEntity sync ---
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    private void sendUpdatePacket() {
        if (!worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    // --- NBT ---
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        // --- Config ---
        tag.setInteger("InputSlot", inputSlot);
        tag.setInteger("OutputSlot", outputSlot);
        tag.setInteger("InputSide", inputSide);
        tag.setInteger("OutputSide", outputSide);

        // --- Internal inventory ---
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                internalInventory[i].writeToNBT(itemTag);
                tag.setTag("InternalSlot" + i, itemTag);
            }
        }

        // --- Ghost pull slots ---
        for (int i = 0; i < ghostPull.length; i++) {
            if (ghostPull[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                ghostPull[i].writeToNBT(itemTag);
                tag.setTag("GhostPull" + i, itemTag);
            }
        }

        // --- Ghost push slots ---
        for (int i = 0; i < ghostPush.length; i++) {
            if (ghostPush[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                ghostPush[i].writeToNBT(itemTag);
                tag.setTag("GhostPush" + i, itemTag);
            }
        }
    }


    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        // --- Config ---
        inputSlot = tag.getInteger("InputSlot");
        outputSlot = tag.getInteger("OutputSlot");
        inputSide = tag.getInteger("InputSide");
        outputSide = tag.getInteger("OutputSide");

        // --- Internal inventory ---
        for (int i = 0; i < internalInventory.length; i++) {
            if (tag.hasKey("InternalSlot" + i)) {
                internalInventory[i] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("InternalSlot" + i));
            } else {
                internalInventory[i] = null;
            }
        }

        // --- Ghost pull slots ---
        for (int i = 0; i < ghostPull.length; i++) {
            if (tag.hasKey("GhostPull" + i)) {
                ghostPull[i] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("GhostPull" + i));
            } else {
                ghostPull[i] = null;
            }
        }

        // --- Ghost push slots ---
        for (int i = 0; i < ghostPush.length; i++) {
            if (tag.hasKey("GhostPush" + i)) {
                ghostPush[i] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("GhostPush" + i));
            } else {
                ghostPush[i] = null;
            }
        }
    }


    // --- Config getters/setters ---
    public int getInputSlot() { return inputSlot; }
    public void setInputSlot(int slot) { inputSlot = slot; markDirty(); sendUpdatePacket(); }

    public int getOutputSlot() { return outputSlot; }
    public void setOutputSlot(int slot) { outputSlot = slot; markDirty(); sendUpdatePacket(); }

    public int getInputSide() { return inputSide; }
    public void setInputSide(int side) { inputSide = side; markDirty(); sendUpdatePacket(); }

    public int getOutputSide() { return outputSide; }
    public void setOutputSide(int side) { outputSide = side; markDirty(); sendUpdatePacket(); }
}

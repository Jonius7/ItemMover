package jonius7.itemmover.blocks;

import java.util.HashMap;
import java.util.Map;

import jonius7.itemmover.gui.ContainerItemMover;
import jonius7.itemmover.gui.SimpleInventory;
import jonius7.itemmover.gui.SimpleInventoryGhost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityItemMover extends TileEntity implements IInventory {

    // --- Inventories ---
    //private ItemStack[] ghostInventory = new ItemStack[9];     // Ghost slots
	private ItemStack[] ghostPull; // Pull Ghost Slots
	private ItemStack[] ghostPush; // Push Ghost Slots
    private ItemStack[] internalInventory; // Real internal slots
    private int[] ghostPullSlotNumbers;
    private int[] ghostPushSlotNumbers;

    // --- Configurable fields ---
    //private int inputSlot = 0;
    //private int outputSlot = 0;
    private int inputSide = 2;
    private int outputSide = 3;
    private final int NUMBER_OF_GHOST_SLOTS = 12;
    private int[] pullSlotMapping;
    private int[] pushSlotMapping;
    
    // Smart push mode to wait for all ghost slot items to be in internal inventory
    private boolean pushMode = false;
    
    public TileEntityItemMover() {
    	super();
        ghostPull = new ItemStack[NUMBER_OF_GHOST_SLOTS];
        ghostPush = new ItemStack[NUMBER_OF_GHOST_SLOTS];
        internalInventory = new ItemStack[18];
        ghostPullSlotNumbers = new int[NUMBER_OF_GHOST_SLOTS];
        ghostPushSlotNumbers = new int[NUMBER_OF_GHOST_SLOTS];
        // For pull and push ghost slots
        pullSlotMapping = new int[NUMBER_OF_GHOST_SLOTS];
        pushSlotMapping = new int[NUMBER_OF_GHOST_SLOTS];
        // Populate default values
        for (int i = 0; i < pullSlotMapping.length; i++) pullSlotMapping[i] = i;
        for (int i = 0; i < pushSlotMapping.length; i++) pushSlotMapping[i] = i;
        
    }
    
    public void validateSlotMappings() {
        IInventory inputInv = getInputInventory();
        IInventory outputInv = getOutputInventory();

        int inputSize = (inputInv != null) ? inputInv.getSizeInventory() : 0;
        int outputSize = (outputInv != null) ? outputInv.getSizeInventory() : 0;

        // Pull mappings
        for (int i = 0; i < pullSlotMapping.length; i++) {
            if (pullSlotMapping[i] >= inputSize) {
                pullSlotMapping[i] = i; // reset to 0 if out of range
            }
        }

        // Push mappings
        for (int i = 0; i < pushSlotMapping.length; i++) {
            if (pushSlotMapping[i] >= outputSize) {
                pushSlotMapping[i] = i;
            }
        }
    }

    
    @Override
    public void updateEntity() {
    	if (!worldObj.isRemote) {
            tryPullItems();
            tryPushItems();
        }
    }
    
    /**
     * Attempts to pull items from input block to the internal inventory
     * according to ghost slot configuration and slot mappings.
     */
    public void tryPullItems() {
        // 1. Get the adjacent inventory on the input side
        ForgeDirection dir = ForgeDirection.getOrientation(inputSide);
        TileEntity adjacent = worldObj.getTileEntity(
                xCoord + dir.offsetX,
                yCoord + dir.offsetY,
                zCoord + dir.offsetZ
        );
        if (!(adjacent instanceof IInventory)) return;
        IInventory source = (IInventory) adjacent;

        // --- Step 2: Iterate over each ghost slot (which corresponds to one source slot) ---
        for (int ghostIndex = 0; ghostIndex < ghostPull.length; ghostIndex++) {
            ItemStack ghostFilter = ghostPull[ghostIndex];

            // A. Check if the ghost slot is configured (not null)
            if (ghostFilter == null) continue;
            
            // B. Define the corresponding source slot
            int srcSlot = getPullSlotMapping(ghostIndex);
            if (srcSlot < 0 || srcSlot >= source.getSizeInventory()) continue; 

            ItemStack srcStack = source.getStackInSlot(srcSlot);
            
            // C. Check if the source slot has the matching item
            if (srcStack == null) continue;
            if (!srcStack.isItemEqual(ghostFilter) || !ItemStack.areItemStackTagsEqual(srcStack, ghostFilter)) continue;
            
            // D. Calculate the need for THIS specific ghost slot's target
            
            // The total count of the item currently in the internal inventory (Global Count)
            int currentCount = countMatchingInInternal(ghostFilter); 
            
            // The target count for THIS specific item type is the sum of ALL ghost slots.
          
            // Re-calculate the total desired count for this item type across ALL ghost slots
            int totalDesired = 0;
            for (ItemStack otherFilter : ghostPull) {
                if (otherFilter != null && otherFilter.isItemEqual(ghostFilter) &&
                    ItemStack.areItemStackTagsEqual(otherFilter, ghostFilter)) {
                    totalDesired += otherFilter.stackSize;
                }
            }
            
            // If we have already met the total requirement for this item type, skip pulling.
            if (currentCount >= totalDesired) continue;
            
            // The maximum amount we are allowed to pull across all sources.
            int needed = totalDesired - currentCount; 

            // E. Pull the item from the corresponding source slot (srcSlot)
            
            // Pull whatever is available in the source stack, up to the global need limit.
            int toMove = Math.min(srcStack.stackSize, needed);
            
            // Check if there is anything to pull from this specific source slot
            if (toMove <= 0) continue; 
            
            // Extract items from the restricted source slot
            ItemStack extracted = srcStack.splitStack(toMove);

            // Insert into internal inventory
            insertIntoInternal(extracted);

            // Update source inventory
            if (srcStack.stackSize <= 0) source.setInventorySlotContents(srcSlot, null);
            else source.setInventorySlotContents(srcSlot, srcStack);

            source.markDirty();

            currentCount += extracted.stackSize; // Update the effective internal count
        }

        markDirty(); // mark TileEntity dirty for saving & updates
    }
    
    /**
     * Attempts to push items from internal inventory to the output block
     * according to ghost slot configuration and slot mappings.
     */
    public void tryPushItems() {
        // --- Find adjacent inventory on the output side ---
        ForgeDirection dir = ForgeDirection.getOrientation(outputSide);
        TileEntity adjacent = worldObj.getTileEntity(
                xCoord + dir.offsetX,
                yCoord + dir.offsetY,
                zCoord + dir.offsetZ
        );

        if (!(adjacent instanceof IInventory)) return;
        IInventory target = (IInventory) adjacent;
        
        // If Smart Mode Toggle is on
        if (pushMode && !areAllGhostPushRequirementsMet()) {
            return; // wait until all required items are present
        }
        
        // --- Loop through ghostPush slots ---
        for (int ghostIndex = 0; ghostIndex < ghostPush.length; ghostIndex++) {
            ItemStack filter = ghostPush[ghostIndex];
            if (filter == null) continue;

            int mappedSlot = getPushSlotMapping(ghostIndex); // slot number in target inventory
            if (mappedSlot < 0 || mappedSlot >= target.getSizeInventory()) continue;

            ItemStack existing = target.getStackInSlot(mappedSlot);

            if (existing == null) {
                // Slot empty → pull up to ghost stack size from internal
                ItemStack extracted = extractFromInternal(filter, filter.stackSize);
                if (extracted != null) target.setInventorySlotContents(mappedSlot, extracted);
            } else if (existing.isItemEqual(filter) && ItemStack.areItemStackTagsEqual(existing, filter)) {
                // Slot has correct item → top up to ghost stack size
                int needed = filter.stackSize - existing.stackSize;
                if (needed > 0) {
                    ItemStack extracted = extractFromInternal(filter, needed);
                    if (extracted != null) {
                        existing.stackSize += extracted.stackSize;
                        target.setInventorySlotContents(mappedSlot, existing);
                    }
                }
                // else already has enough → do nothing
            } else {
                // Slot has wrong item → skip
            }
        }

        target.markDirty();
        markDirty();
    }

    /**
     * Returns true if, for every unique item listed in ghostPush,
     * the total count of that item across the internal inventory is
     * >= the total desired count (sum of stackSizes in all push ghost slots).
     */
    private boolean areAllGhostPushRequirementsMet() {
        // --- 1) Build desired totals from push ghost slots ---
        Map<String, Integer> desired = new HashMap<>();
        Map<String, ItemStack> representative = new HashMap<>();
        for (ItemStack ghost : ghostPush) {
            if (ghost == null) continue;
            String key = getItemKey(ghost);
            int want = desired.getOrDefault(key, 0);
            desired.put(key, want + ghost.stackSize);
            if (!representative.containsKey(key)) representative.put(key, ghost);
        }

        // If no push ghosts configured, consider requirement met
        if (desired.isEmpty()) return true;

        // --- 2) Count totals present in internal inventory ---
        Map<String, Integer> have = new HashMap<>();
        for (ItemStack in : internalInventory) {
            if (in == null) continue;
            String key = getItemKey(in);
            int cur = have.getOrDefault(key, 0);
            have.put(key, cur + in.stackSize);
        }

        // Optionally include held stack by viewer if your design requires it:
        // for (Map.Entry<String, ItemStack> rep : representative.entrySet()) {
        //     ItemStack held = getHeldStackIfViewerMatches(rep.getValue());
        //     if (held != null) {
        //         String key = rep.getKey();
        //         have.put(key, have.getOrDefault(key, 0) + held.stackSize);
        //     }
        // }

        // --- 3) Compare desired vs have ---
        for (Map.Entry<String, Integer> e : desired.entrySet()) {
            String key = e.getKey();
            int want = e.getValue();
            int got = have.getOrDefault(key, 0);
            if (got < want) return false; // not enough of this item
        }

        return true; // all requirements satisfied
    }

    
    // Helper: produce a string key for an ItemStack that includes item id, damage and NBT
    private String getItemKey(ItemStack stack) {
        if (stack == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append(stack.getItem().getUnlocalizedName()); // or use GameData.getItemRegistry().getNameForObject(...) if available
        sb.append(":").append(stack.getItemDamage());
        if (stack.hasTagCompound()) {
            sb.append(":").append(stack.getTagCompound().toString());
        }
        return sb.toString();
    }

    /**
     * Extracts up to 'amount' of items matching 'filter' from the internal inventory.
     */
    public ItemStack extractFromInternal(ItemStack filter, int amount) {
        if (filter == null || amount <= 0) return null;

        ItemStack result = null;
        int remaining = amount;

        for (int i = 0; i < internalInventory.length; i++) {
            ItemStack stack = internalInventory[i];
            if (stack == null) continue;

            if (stack.isItemEqual(filter) && ItemStack.areItemStackTagsEqual(stack, filter)) {
                int toTake = Math.min(stack.stackSize, remaining);

                if (result == null) {
                    result = stack.copy();
                    result.stackSize = toTake;
                } else {
                    result.stackSize += toTake;
                }

                stack.stackSize -= toTake;
                if (stack.stackSize <= 0) internalInventory[i] = null;

                remaining -= toTake;
                if (remaining <= 0) break;
            }
        }

        if (result != null) markDirty();
        return result;
    }    
    
    // Helper to count matching items in internal inventory
    private int countMatchingInInternal(ItemStack filter) {
        int count = 0;
        for (ItemStack slotStack : internalInventory) {
            if (slotStack != null &&
                slotStack.isItemEqual(filter) &&
                ItemStack.areItemStackTagsEqual(slotStack, filter)) {
                count += slotStack.stackSize;
            }
        }
        return count;
    }


    // Helper to insert items into internal inventory
    private void insertIntoInternal(ItemStack toInsert) {
        for (int i = 0; i < internalInventory.length; i++) {
            ItemStack slotStack = internalInventory[i];
            if (slotStack == null) {
                internalInventory[i] = toInsert.copy();
                return;
            } else if (slotStack.isItemEqual(toInsert) &&
                       ItemStack.areItemStackTagsEqual(slotStack, toInsert) &&
                       slotStack.stackSize < slotStack.getMaxStackSize()) {
                int space = slotStack.getMaxStackSize() - slotStack.stackSize;
                int added = Math.min(space, toInsert.stackSize);
                slotStack.stackSize += added;
                toInsert.stackSize -= added;
                if (toInsert.stackSize <= 0) return;
            }
        }
    }
    
    // Account for a player holding a matching stack
    private ItemStack getHeldStackIfViewerMatches(ItemStack filter) {
        if (worldObj.isRemote) return null; // only server-side
        for (Object obj : worldObj.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player.openContainer instanceof ContainerItemMover) {
                TileEntityItemMover tile = ((ContainerItemMover) player.openContainer).getTile();
                if (tile == this) {
                    ItemStack held = player.inventory.getItemStack();
                    if (held != null &&
                        held.isItemEqual(filter) &&
                        ItemStack.areItemStackTagsEqual(held, filter)) {
                        return held;
                    }
                }
            }
        }
        return null;
    }
    
    
    // Expose them as sub-inventories:
    public IInventory getInternalInventory() {
        return new SimpleInventory(internalInventory.length, internalInventory);
    }

    // Return IInventory views for GUI
    public IInventory getPullGhostInventory() {
        return new GhostWrapperInventory(ghostPull);
    }

    public IInventory getPushGhostInventory() {
        return new GhostWrapperInventory(ghostPush);
    }
    
    /** Internal class: wrap an existing ItemStack[] as a ghost IInventory **/
    private static class GhostWrapperInventory extends SimpleInventoryGhost {
        private final ItemStack[] backing;

        public GhostWrapperInventory(ItemStack[] backing) {
            super(backing.length);
            this.backing = backing;
            // Initialize from backing
            for (int i = 0; i < backing.length; i++) {
                if (backing[i] != null) {
                    super.setInventorySlotContents(i, backing[i]);
                }
            }
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            super.setInventorySlotContents(index, stack);
            if (index >= 0 && index < backing.length) {
                backing[index] = stack != null ? stack.copy() : null;
            }
        }
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
        return pull ? getGhostPull().length : getGhostPush().length;
    }

    public ItemStack getGhostStack(int slot, boolean pull) {
        if (pull) {
            return getGhostPull()[slot];
        } else {
            return getGhostPush()[slot];
        }
    }

    public void setGhostStack(int slot, ItemStack stack, boolean pull) {
        if (pull) {
            getGhostPull()[slot] = stack;
        } else {
            getGhostPush()[slot] = stack;
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
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    private void sendUpdatePacket() {
        if (!worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    // --- NBT ---
    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("InputSide", inputSide);
        compound.setInteger("OutputSide", outputSide);
        compound.setIntArray("GhostPullSlotNumbers", ghostPullSlotNumbers);
        compound.setIntArray("GhostPullSlotNumbers", ghostPushSlotNumbers);
        // Internal inventory
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i] != null) {
                NBTTagCompound tag = new NBTTagCompound();
                internalInventory[i].writeToNBT(tag);
                compound.setTag("InternalSlot" + i, tag);
            }
        }

        // Ghost Pull
        NBTTagList pullList = new NBTTagList();
        for (int i = 0; i < ghostPull.length; i++) {
            if (ghostPull[i] != null) {
                NBTTagCompound t = new NBTTagCompound();
                t.setByte("Slot", (byte) i);
                ghostPull[i].writeToNBT(t);
                pullList.appendTag(t);
            }
        }
        compound.setTag("GhostPull", pullList);

        // Ghost Push
        NBTTagList pushList = new NBTTagList();
        for (int i = 0; i < ghostPush.length; i++) {
            if (ghostPush[i] != null) {
                NBTTagCompound t = new NBTTagCompound();
                t.setByte("Slot", (byte) i);
                ghostPush[i].writeToNBT(t);
                pushList.appendTag(t);
            }
        }
        compound.setTag("GhostPush", pushList);
        
        NBTTagList pullMappingList = new NBTTagList();
        for (int i = 0; i < pullSlotMapping.length; i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("GhostIndex", i);
            tag.setInteger("SlotNumber", pullSlotMapping[i]);
            pullMappingList.appendTag(tag);
        }
        compound.setTag("PullSlotMapping", pullMappingList);

        NBTTagList pushMappingList = new NBTTagList();
        for (int i = 0; i < pushSlotMapping.length; i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("GhostIndex", i);
            tag.setInteger("SlotNumber", pushSlotMapping[i]);
            pushMappingList.appendTag(tag);
        }
        compound.setTag("PushSlotMapping", pushMappingList);
        
        compound.setBoolean("PushMode", pushMode);
    }


    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        // Ensure arrays exist
        if (ghostPull == null) ghostPull = new ItemStack[12];
        if (ghostPush == null) ghostPush = new ItemStack[12];
        if (internalInventory == null) internalInventory = new ItemStack[18];

        if (compound.hasKey("InputSide")) inputSide = compound.getInteger("InputSide");
        if (compound.hasKey("OutputSide")) outputSide = compound.getInteger("OutputSide");
        if (compound.hasKey("GhostPullSlotNumbers")) ghostPullSlotNumbers = compound.getIntArray("GhostPullSlotNumbers");
        if (compound.hasKey("GhostPushSlotNumbers")) ghostPushSlotNumbers = compound.getIntArray("GhostPushSlotNumbers");
        
        for (int i = 0; i < internalInventory.length; i++) {
            String key = "InternalSlot" + i;
            if (compound.hasKey(key)) {
                internalInventory[i] = ItemStack.loadItemStackFromNBT(compound.getCompoundTag(key));
            } else {
                internalInventory[i] = null;
            }
        }

        NBTTagList pullList = compound.getTagList("GhostPull", 10);
        for (int i = 0; i < pullList.tagCount(); i++) {
            NBTTagCompound t = pullList.getCompoundTagAt(i);
            int slot = t.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < ghostPull.length) {
                ghostPull[slot] = ItemStack.loadItemStackFromNBT(t);
            }
        }

        NBTTagList pushList = compound.getTagList("GhostPush", 10);
        for (int i = 0; i < pushList.tagCount(); i++) {
            NBTTagCompound t = pushList.getCompoundTagAt(i);
            int slot = t.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < ghostPush.length) {
                ghostPush[slot] = ItemStack.loadItemStackFromNBT(t);
            }
        }
        
        NBTTagList pullMappingList = compound.getTagList("PullSlotMapping", 10);
        for (int i = 0; i < pullMappingList.tagCount(); i++) {
            NBTTagCompound tag = pullMappingList.getCompoundTagAt(i);
            int ghostIndex = tag.getInteger("GhostIndex");
            pullSlotMapping[ghostIndex] = tag.getInteger("SlotNumber");
        }

        NBTTagList pushMappingList = compound.getTagList("PushSlotMapping", 10);
        for (int i = 0; i < pushMappingList.tagCount(); i++) {
            NBTTagCompound tag = pushMappingList.getCompoundTagAt(i);
            int ghostIndex = tag.getInteger("GhostIndex");
            pushSlotMapping[ghostIndex] = tag.getInteger("SlotNumber");
        }
        
        if (compound.hasKey("PushMode")) pushMode = compound.getBoolean("PushMode");
    }

    // --- Config getters/setters ---
    public int getInputSide() { return inputSide; }
    public int getOutputSide() { return outputSide; }
    
    public void setInputSide(int side) {
        inputSide = (side + 6) % 6; // wrap 0-5
        markDirty();
        sendUpdatePacket(); // optional, only if you want instant GUI update
    }
    public void setOutputSide(int side) {
        outputSide = (side + 6) % 6;
        markDirty();
        sendUpdatePacket();
    }
    public void cycleInputSide(boolean forward) {
        setInputSide(inputSide + (forward ? 1 : -1));
    }
    public void cycleOutputSide(boolean forward) {
        setOutputSide(outputSide + (forward ? 1 : -1));
    }

    public ItemStack[] getGhostPull() {
        return ghostPull;
    }

    public void setGhostPull(ItemStack[] arr) {
        if (arr == null) return;
        // Copy array safely
        int len = Math.min(arr.length, ghostPull.length);
        for (int i = 0; i < len; i++) {
            ghostPull[i] = arr[i] != null ? arr[i].copy() : null;
        }
        markDirty();
    }

    public ItemStack[] getGhostPush() {
        return ghostPush;
    }

    public void setGhostPush(ItemStack[] arr) {
        if (arr == null) return;
        int len = Math.min(arr.length, ghostPush.length);
        for (int i = 0; i < len; i++) {
            ghostPush[i] = arr[i] != null ? arr[i].copy() : null;
        }
        markDirty();
    }

    public int getPullSlotMapping(int index) {
        if (pullSlotMapping == null || pullSlotMapping.length == 0) return 0;
        if (index < 0) return 0;
        if (index >= pullSlotMapping.length) return pullSlotMapping.length - 1; // clamp
        return pullSlotMapping[index];
    }

    public void setPullSlotMapping(int index, int value) {
        if (pullSlotMapping == null) return;
        if (index < 0 || index >= pullSlotMapping.length) return;
        pullSlotMapping[index] = value;
    }

    public int getPushSlotMapping(int index) {
        if (pushSlotMapping == null || pushSlotMapping.length == 0) return 0;
        if (index < 0) return 0;
        if (index >= pushSlotMapping.length) return pushSlotMapping.length - 1; // clamp
        return pushSlotMapping[index];
    }

    public void setPushSlotMapping(int index, int value) {
        if (pushSlotMapping == null) return;
        if (index < 0 || index >= pushSlotMapping.length) return;
        pushSlotMapping[index] = value;
    }

    // Optional helpers to expose lengths
    public int getPullMappingLength() {
        return pullSlotMapping == null ? 0 : pullSlotMapping.length;
    }
    public int getPushMappingLength() {
        return pushSlotMapping == null ? 0 : pushSlotMapping.length;
    }
    
    public int getInternalInventoryLength () {
    	return internalInventory.length;
    }
    
    public IInventory getInputInventory() {
        ForgeDirection dir = ForgeDirection.getOrientation(inputSide);
        int x = xCoord + dir.offsetX;
        int y = yCoord + dir.offsetY;
        int z = zCoord + dir.offsetZ;

        TileEntity te = worldObj.getTileEntity(x, y, z);
        if (te instanceof IInventory) return (IInventory) te;
        return null;
    }

    public IInventory getOutputInventory() {
        ForgeDirection dir = ForgeDirection.getOrientation(outputSide);
        int x = xCoord + dir.offsetX;
        int y = yCoord + dir.offsetY;
        int z = zCoord + dir.offsetZ;

        TileEntity te = worldObj.getTileEntity(x, y, z);
        if (te instanceof IInventory) return (IInventory) te;
        return null;
    }
    
    public int getSizeInternalInventory() {
        return internalInventory.length;
    }

    public ItemStack getInternalStackInSlot(int slot) {
        if (slot < 0 || slot >= internalInventory.length) return null;
        return internalInventory[slot];
    }
    
    public boolean getPushMode() {
        return pushMode;
    }

    public void setPushMode(boolean value) {
        pushMode = value;
        markDirty();
    }
}

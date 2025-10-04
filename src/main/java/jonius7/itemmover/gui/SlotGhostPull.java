package jonius7.itemmover.gui;

import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotGhostPull extends SlotGhost {
    private TileEntityItemMover tile;

    public SlotGhostPull(IInventory inv, int index, int x, int y, TileEntityItemMover tile) {
        super(inv, index, x, y);
        this.tile = tile;
    }

    @Override
    public void putStack(ItemStack stack) {
        super.putStack(stack); // updates fake inventory
        tile.getGhostPull()[this.getSlotIndex()] = stack != null ? stack.copy() : null;
        tile.markDirty();
    }
}
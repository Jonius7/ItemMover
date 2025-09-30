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

        // Add the ItemMover's single slot at (8, 35)
        this.addSlotToContainer(new Slot(tile, 0, 8, 35));

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUseableByPlayer(player);
    }

    // Optional: implement shift-click if desired
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        return null;
    }
}

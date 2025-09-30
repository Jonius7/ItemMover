package jonius7.itemmover.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import jonius7.itemmover.ItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class GuiItemMover extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("itemmover", "textures/gui/item_mover.png");
    private TileEntityItemMover tile;

    private GuiButton btnInputSlotUp, btnInputSlotDown;
    private GuiButton btnOutputSlotUp, btnOutputSlotDown;
    private GuiButton btnInputSideUp, btnInputSideDown;
    private GuiButton btnOutputSideUp, btnOutputSideDown;

    public GuiItemMover(InventoryPlayer playerInv, TileEntityItemMover tile) {
        super(new ContainerItemMover(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        // --- Center column: Input Slot ---
        btnInputSlotUp = new GuiButton(0, guiLeft + 40, guiTop + 30, 20, 20, "+");
        btnInputSlotDown = new GuiButton(1, guiLeft + 60, guiTop + 30, 20, 20, "-");

        // --- Center column: Output Slot ---
        btnOutputSlotUp = new GuiButton(2, guiLeft + 40, guiTop + 60, 20, 20, "+");
        btnOutputSlotDown = new GuiButton(3, guiLeft + 60, guiTop + 60, 20, 20, "-");

        // --- Right column: Input Side ---
        btnInputSideUp = new GuiButton(4, guiLeft + 110, guiTop + 30, 20, 20, "+");
        btnInputSideDown = new GuiButton(5, guiLeft + 130, guiTop + 30, 20, 20, "-");

        // --- Right column: Output Side ---
        btnOutputSideUp = new GuiButton(6, guiLeft + 110, guiTop + 60, 20, 20, "+");
        btnOutputSideDown = new GuiButton(7, guiLeft + 130, guiTop + 60, 20, 20, "-");

        buttonList.add(btnInputSlotUp);
        buttonList.add(btnInputSlotDown);
        buttonList.add(btnOutputSlotUp);
        buttonList.add(btnOutputSlotDown);
        buttonList.add(btnInputSideUp);
        buttonList.add(btnInputSideDown);
        buttonList.add(btnOutputSideUp);
        buttonList.add(btnOutputSideDown);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            // --- Input Slot ---
            case 0: {
                int maxInput = tile.getMaxSlot(tile.getInputSide());
                tile.setInputSlot(Math.min(tile.getInputSlot() + 1, maxInput));
                break;
            }
            case 1: {
                tile.setInputSlot(Math.max(0, tile.getInputSlot() - 1));
                break;
            }

            // --- Output Slot ---
            case 2: {
                int maxOutput = tile.getMaxSlot(tile.getOutputSide());
                tile.setOutputSlot(Math.min(tile.getOutputSlot() + 1, maxOutput));
                break;
            }
            case 3: {
                tile.setOutputSlot(Math.max(0, tile.getOutputSlot() - 1));
                break;
            }

            // --- Input Side ---
            case 4:
                tile.setInputSide((tile.getInputSide() + 1) % 6);
                break;
            case 5:
                tile.setInputSide((tile.getInputSide() + 5) % 6); // -1 mod 6
                break;

            // --- Output Side ---
            case 6:
                tile.setOutputSide((tile.getOutputSide() + 1) % 6);
                break;
            case 7:
                tile.setOutputSide((tile.getOutputSide() + 5) % 6); // -1 mod 6
                break;
        }

        // Send updated values to server
        ItemMover.network.sendToServer(new jonius7.itemmover.network.PacketUpdateItemMover(tile));
    }

    private int getMaxSlot(int side) {
        int x = tile.xCoord + net.minecraft.util.Facing.offsetsXForSide[side];
        int y = tile.yCoord + (side == 0 ? -1 : side == 1 ? 1 : 0);
        int z = tile.zCoord + net.minecraft.util.Facing.offsetsZForSide[side];

        TileEntity te = tile.getWorldObj().getTileEntity(x, y, z);
        if (te instanceof IInventory) {
            int size = ((IInventory) te).getSizeInventory();
            return size > 0 ? size - 1 : 0;
        }
        return 0; // default if no inventory present
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        fontRendererObj.drawString("Item Mover", 8, 6, 0x404040);

        // Center column: Input/Output Slots
        fontRendererObj.drawString("Input Slot: " + tile.getInputSlot(), 40, 20, 0x404040);
        fontRendererObj.drawString("Output Slot: " + tile.getOutputSlot(), 40, 50, 0x404040);

        // Right column: Input/Output sides
        fontRendererObj.drawString("Input Side: " + TileEntityItemMover.getSideName(tile.getInputSide()), 110, 20, 0x404040);
        fontRendererObj.drawString("Output Side: " + TileEntityItemMover.getSideName(tile.getOutputSide()), 110, 50, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}

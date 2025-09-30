package jonius7.itemmover.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import jonius7.itemmover.ItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class GuiItemMover extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("itemmover", "textures/gui/item_mover.png");
    private TileEntityItemMover tile;

    private GuiButton btnSlotUp, btnSlotDown, btnInputUp, btnInputDown, btnOutputUp, btnOutputDown;

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

        // --- Slot on the left ---
        // Already handled by Container at (8, 35) in vanilla-style, so no button needed here

        // --- Center column: Target Slot + label ---
        btnSlotUp = new GuiButton(0, guiLeft + 40, guiTop + 30, 20, 20, "+");
        btnSlotDown = new GuiButton(1, guiLeft + 60, guiTop + 30, 20, 20, "-");

        // --- Right column: Input Side buttons ---
        btnInputUp = new GuiButton(2, guiLeft + 110, guiTop + 30, 20, 20, "+");
        btnInputDown = new GuiButton(3, guiLeft + 130, guiTop + 30, 20, 20, "-");

        // --- Right column: Output Side buttons ---
        btnOutputUp = new GuiButton(4, guiLeft + 110, guiTop + 60, 20, 20, "+");
        btnOutputDown = new GuiButton(5, guiLeft + 130, guiTop + 60, 20, 20, "-");

        buttonList.add(btnSlotUp);
        buttonList.add(btnSlotDown);
        buttonList.add(btnInputUp);
        buttonList.add(btnInputDown);
        buttonList.add(btnOutputUp);
        buttonList.add(btnOutputDown);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: tile.setTargetSlot(tile.getTargetSlot() + 1); break;
            case 1: tile.setTargetSlot(Math.max(0, tile.getTargetSlot() - 1)); break;
            case 2: tile.setInputSide((tile.getInputSide() + 1) % 6); break;
            case 3: tile.setInputSide((tile.getInputSide() + 5) % 6); break; // -1 mod 6
            case 4: tile.setOutputSide((tile.getOutputSide() + 1) % 6); break;
            case 5: tile.setOutputSide((tile.getOutputSide() + 5) % 6); break; // -1 mod 6
        }

        // Send packet to server to sync settings
        ItemMover.network.sendToServer(new jonius7.itemmover.network.PacketUpdateItemMover(tile));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        // Title
        fontRendererObj.drawString("Item Mover", 8, 6, 0x404040);

        // Center column: Target Slot label
        fontRendererObj.drawString("Target Slot: " + tile.getTargetSlot(), 40, 20, 0x404040);

        // Right column: Input / Output sides
        fontRendererObj.drawString("Input Side: " + TileEntityItemMover.getSideName(tile.getInputSide()), 110, 20, 0x404040);
        fontRendererObj.drawString("Output Side: " + TileEntityItemMover.getSideName(tile.getOutputSide()), 110, 50, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}

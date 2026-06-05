package jonius7.itemmover.client.render;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import jonius7.itemmover.blocks.BlockItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.IIcon;

public class RenderItemMover implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public RenderItemMover(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        // Render base block only for inventory
        Tessellator tessellator = Tessellator.instance;
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(0, metadata));
        renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(1, metadata));
        renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(2, metadata));
        renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(3, metadata));
        renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(4, metadata));
        renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(5, metadata));
        tessellator.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        // 1. Let Minecraft draw the standard base cube
        renderer.renderStandardBlock(block, x, y, z);

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityItemMover) {
            TileEntityItemMover mover = (TileEntityItemMover) te;
            BlockItemMover moverBlock = (BlockItemMover) block;

            // 2. Draw overlays ONLY where configured
            drawFace(renderer, block, x, y, z, mover.getInputSide(), moverBlock.getPullIcon());
            drawFace(renderer, block, x, y, z, mover.getOutputSide(), moverBlock.getPushIcon());
        }
        return true;
    }

    private void drawFace(RenderBlocks renderer, Block block, int x, int y, int z, int side, IIcon icon) {
        if (icon == null) return;
        
        // Tell the renderer "For the next command, use THIS texture"
        renderer.setOverrideBlockTexture(icon);
        System.out.println("Rendering side: " + side);
        
        if (side == 0) renderer.renderFaceYNeg(block, x, y, z, icon);
        if (side == 1) renderer.renderFaceYPos(block, x, y, z, icon);
        if (side == 2) renderer.renderFaceZNeg(block, x, y, z, icon);
        if (side == 3) renderer.renderFaceZPos(block, x, y, z, icon);
        if (side == 4) renderer.renderFaceXNeg(block, x, y, z, icon);
        if (side == 5) renderer.renderFaceXPos(block, x, y, z, icon);
        
        // IMPORTANT: Clear the override so you don't overwrite everything else
        renderer.clearOverrideBlockTexture();
    }
    
    @Override
    public int getRenderId() {
        return this.renderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }
}
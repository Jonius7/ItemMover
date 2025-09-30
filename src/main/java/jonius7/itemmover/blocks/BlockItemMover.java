package jonius7.itemmover.blocks;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockItemMover extends BlockContainer {

    public BlockItemMover() {
        super(Material.iron);
        setBlockName("itemMover");
        setBlockTextureName("itemmover:item_mover");
        setHardness(2.0F);
        setCreativeTab(jonius7.itemmover.ItemMover.tabItemMover);
    }
        
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityItemMover();
    }
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(jonius7.itemmover.ItemMover.instance, 0, world, x, y, z);
        }
        return true;
    }
}

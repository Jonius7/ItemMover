package jonius7.itemmover.blocks;

import java.util.Random;

import jonius7.itemmover.proxy.ClientProxy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockItemMover extends BlockContainer {
	private IIcon baseIcon;
    private IIcon pullIcon;
    private IIcon pushIcon;
	
	
    public BlockItemMover() {
        super(Material.iron);
        setBlockName("itemMover");
        setBlockTextureName("itemmover:item_mover");
        setHardness(2.0F);
        setCreativeTab(jonius7.itemmover.ItemMover.tabItemMover);
    }
    
    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; } // Use custom ISimpleBlockRenderingHandler
    
    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
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
    
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z,
                                EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityItemMover) {
            TileEntityItemMover mover = (TileEntityItemMover) te;
            mover.validateSlotMappings();
            mover.markDirty();
        }
    }
    
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityItemMover) {
            dropInventoryItems(world, x, y, z, (TileEntityItemMover) tile);
            world.func_147453_f(x, y, z, block); // vanilla method: updates comparators
        }

        super.breakBlock(world, x, y, z, block, meta);
    }
    
    private void dropInventoryItems(World world, int x, int y, int z, TileEntityItemMover te) {
        Random rand = world.rand;

        for (int i = 0; i < te.getSizeInternalInventory(); i++) {
            ItemStack stack = te.getInternalStackInSlot(i);
            if (stack == null) continue;

            float offsetX = rand.nextFloat() * 0.8F + 0.1F;
            float offsetY = rand.nextFloat() * 0.8F + 0.1F;
            float offsetZ = rand.nextFloat() * 0.8F + 0.1F;

            EntityItem entityItem = new EntityItem(
                    world,
                    x + offsetX,
                    y + offsetY,
                    z + offsetZ,
                    stack.copy()
            );

            if (stack.hasTagCompound()) {
                entityItem.getEntityItem().setTagCompound(
                        (NBTTagCompound) stack.getTagCompound().copy()
                );
            }

            float velocity = 0.05F;
            entityItem.motionX = rand.nextGaussian() * velocity;
            entityItem.motionY = rand.nextGaussian() * velocity + 0.2F;
            entityItem.motionZ = rand.nextGaussian() * velocity;

            world.spawnEntityInWorld(entityItem);
        }
    }
    
    @Override
    public void registerBlockIcons(IIconRegister reg) {
    	this.baseIcon = reg.registerIcon("itemmover:itemmover");
        this.setPullIcon(reg.registerIcon("itemmover:itemmover_pull"));
        this.setPushIcon(reg.registerIcon("itemmover:itemmover_push"));
    }
    
    @Override
    public IIcon getIcon(int side, int metadata) {
        return baseIcon; // Default
    }
    
    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return baseIcon;
    }
    
	public IIcon getPullIcon() {
		return pullIcon;
	}

	public void setPullIcon(IIcon pullIcon) {
		this.pullIcon = pullIcon;
	}

	public IIcon getPushIcon() {
		return pushIcon;
	}

	public void setPushIcon(IIcon pushIcon) {
		this.pushIcon = pushIcon;
	}
    
	@Override
	public int getRenderType() {
	    return ClientProxy.itemMoverRenderId; // Must match exactly what you registered in your proxy
	}
}

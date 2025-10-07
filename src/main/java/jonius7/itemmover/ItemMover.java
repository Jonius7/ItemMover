package jonius7.itemmover;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import jonius7.itemmover.blocks.BlockItemMover;
import jonius7.itemmover.blocks.TileEntityItemMover;
import jonius7.itemmover.gui.ContainerItemMover;
import jonius7.itemmover.gui.GuiItemMover;
import jonius7.itemmover.network.PacketUpdateItemMover;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

@Mod(
		modid = ItemMover.MODID,
		name = "Item Mover",
		version = ItemMover.VERSION
)
public class ItemMover implements IGuiHandler {
	
    @Instance
    public static ItemMover instance;
    
    public static SimpleNetworkWrapper network;
	
	public static final String MODID = "itemmover";
    public static final String VERSION = "0.4-pre1";
	
    public static Block itemMover;
    
    // Custom creative tab
    public static final CreativeTabs tabItemMover = new CreativeTabs("itemMoverTab") {
    	@Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(itemMover);
        }
    };
    
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	// Setup networking
        network = NetworkRegistry.INSTANCE.newSimpleChannel("itemmover");
        network.registerMessage(PacketUpdateItemMover.Handler.class, PacketUpdateItemMover.class, 0, Side.SERVER);

        itemMover = new BlockItemMover();
        GameRegistry.registerBlock(itemMover, "itemMover");
        GameRegistry.registerTileEntity(TileEntityItemMover.class, "item_mover");
        NetworkRegistry.INSTANCE.registerGuiHandler(this, this);
    }
    
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityItemMover) {
            return new ContainerItemMover(player.inventory, (TileEntityItemMover) te);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityItemMover) {
            return new GuiItemMover(player.inventory, (TileEntityItemMover) te);
        }
        return null;
    }
}

package jonius7.itemmover.proxy;
import cpw.mods.fml.client.registry.RenderingRegistry;
import jonius7.itemmover.client.render.RenderItemMover;

public class ClientProxy extends CommonProxy {
    public static int itemMoverRenderId;

    @Override
    public void registerRenderers() {
        // Assign a unique ID for your custom block model
        itemMoverRenderId = RenderingRegistry.getNextAvailableRenderId();
        
        // Tell Forge to use your RenderItemMover class for that ID
        RenderingRegistry.registerBlockHandler(new RenderItemMover(itemMoverRenderId));
    }
}
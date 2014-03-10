package pl.asie.computronics.tile;

import openperipheral.api.Arg;
import openperipheral.api.LuaCallable;
import openperipheral.api.LuaType;
import dan200.computer.api.IComputerAccess;
import net.minecraftforge.common.ForgeDirection;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.SimpleComponent;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.BlockCamera;
import pl.asie.computronics.util.Camera;
import pl.asie.computronics.util.CollisionFinder;
import pl.asie.lib.block.TileEntityBase;

public class TileCamera extends TileEntityBase implements SimpleComponent {
	private static final int CALL_LIMIT = 20;
	private final Camera camera = new Camera();
	
	@Override
	public boolean canUpdate() { return true; }
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		camera.reset();
	}
	
	// OpenComputers
    @Callback(direct = true, limit = CALL_LIMIT)
    public Object[] setRayDirection(Context context, Arguments args) {
    	if(args.count() == 2) {
    		return new Object[]{
    			camera.setRayDirection(worldObj, xCoord, yCoord, zCoord,
    					Computronics.instance.camera.getFacingDirection(worldObj, xCoord, yCoord, zCoord),
    					(float)args.checkDouble(0), (float)args.checkDouble(1))
    		};
    	}
    	return null;
    }
    
    @Callback(direct = true, limit = CALL_LIMIT)
    public Object[] distance(Context context, Arguments args) {
    	setRayDirection(context, args);
    	return new Object[]{camera.getDistance()};
    }
    
    @Callback(direct = true, limit = CALL_LIMIT / 2)
    public Object[] block(Context context, Arguments args) {
    	setRayDirection(context, args);
    	return new Object[]{camera.getBlockData()};
    }
 
	@Override
	public String getComponentName() {
		return "camera";
	}
	
	// OpenPeripheral
	
    @LuaCallable(description = "Gets the distance for a specified direction.", returnTypes = {LuaType.NUMBER})
	public Float distance(
		IComputerAccess computer,
		@Arg(name = "x", type = LuaType.NUMBER, description = "The X direction (-1.0 to 1.0)") Float x,
		@Arg(name = "y", type = LuaType.NUMBER, description = "The Y direction (-1.0 to 1.0)") Float y
	) {
    	camera.setRayDirection(worldObj, xCoord, yCoord, zCoord,
    			Computronics.instance.camera.getFacingDirection(worldObj, xCoord, yCoord, zCoord),
    			x, y);
    	return (float)camera.getDistance();
    }
}

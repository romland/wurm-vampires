package com.friya.wurmonline.server.vamps.events;

import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import com.wurmonline.server.structures.DbFence;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.StructureConstantsEnum;

public class GrowMazeHedgeEvent extends EventOnce
{
	private DbFence fence = null;
	private StructureConstantsEnum stage = StructureConstantsEnum.NO_WALL;
	
	
	public GrowMazeHedgeEvent(int fromNow, DbFence fence, StructureConstantsEnum stage)
	{
        super(fromNow, EventOnce.Unit.MILLISECONDS);

        this.fence = fence;
        this.stage = stage;
	}

	@Override
	public boolean invoke()
	{
		try {
			fence.setType(stage);
			fence.save();
			VolaTile tile = Zones.getTileOrNull(fence.getTileX(), fence.getTileY(), true);
			tile.updateFence(fence);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}

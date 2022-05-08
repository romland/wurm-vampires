package com.friya.wurmonline.server.vamps.events;

import com.friya.wurmonline.server.vamps.Vampires;

public class RemoveStakedTeleportEvent extends EventOnce
{
	public RemoveStakedTeleportEvent(int fromNow, Unit unit)
	{
        super(fromNow, unit);
	}

	@Override
	public boolean invoke()
	{
		Vampires.clearStakedTeleportPosition();
		return true;
	}
}

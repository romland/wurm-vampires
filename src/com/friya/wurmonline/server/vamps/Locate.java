package com.friya.wurmonline.server.vamps;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class Locate
{
	static private String formatBearing(double bearing)
	{
		if (bearing < 0 && bearing > -180) {
			bearing = 360.0 + bearing;
		}

		if (bearing > 360 || bearing < -180) {
			return "Unknown";
	    }

		String directions[] = {
			"north", "north-northeast", "northeast", "east-northeast", "east", "east-southeast", "southeast", "south-southeast",
			"south", "south-southwest", "southwest", "west-southwest", "west", "west-northwest", "northwest", "north-northwest",
			"north"
		};

		String cardinal = directions[(int) Math.floor(((bearing + 11.25) % 360) / 22.5)];

		return cardinal;
	}


	static public String getCompassDirection(Creature fromCreature, Creature toCreature)
	{
		return getCompassDirection(new int[]{fromCreature.getTileX(), fromCreature.getTileY()}, new int[]{toCreature.getTileX(), toCreature.getTileY()});
	}

	static public String getCompassDirection(int[] from, int[] to)
	{
		double myBearing = 90 - (180 / Math.PI) * Math.atan2((double)from[1] - (double)to[1], (double)to[0] - (double)from[0]);
		return formatBearing(myBearing);
	}
	
	static public String getCompassDirection(Item fromItem, Creature toCreature)
	{
		return getCompassDirection(new int[]{fromItem.getTileX(), fromItem.getTileY()}, new int[]{toCreature.getTileX(), toCreature.getTileY()});
	}
	
	static public String getCompassDirection(int fromX, int fromY, int toX, int toY)
	{
		return getCompassDirection(new int[]{fromX, fromY}, new int[]{toX, toY});
	}
}

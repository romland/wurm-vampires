package com.friya.wurmonline.server.vamps.events;

import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import com.friya.wurmonline.server.maze.Maze;
import com.wurmonline.mesh.Tiles.TileBorderDirection;
import com.wurmonline.server.zones.NoSuchZoneException;

public class CreateMazeHedgeEvent extends EventOnce
{
	private int tileX = 0;
	private int tileY = 0;
	private TileBorderDirection border = null;
	private Maze maze = null;
	private boolean animateGrowth = false;
	
	
	public CreateMazeHedgeEvent(int fromNow, Maze maze, TileBorderDirection border, int tileX, int tileY, boolean animateGrowth)
	{
        super(fromNow, EventOnce.Unit.MILLISECONDS);

        this.maze = maze;
        this.tileX = tileX;
        this.tileY = tileY;
        this.border = border;
        this.animateGrowth = animateGrowth;
	}

	@Override
	public boolean invoke()
	{
		try {
			maze.createHedge(this.border, this.tileX, this.tileY, animateGrowth);

		} catch (NoSuchZoneException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}

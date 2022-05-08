package com.friya.wurmonline.server.maze;


import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.tools.StdRandom;
import com.friya.wurmonline.server.vamps.ShortEventDispatcher;
import com.friya.wurmonline.server.vamps.events.CreateMazeHedgeEvent;
import com.friya.wurmonline.server.vamps.events.GrowMazeHedgeEvent;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.mesh.Tiles.TileBorderDirection;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.MethodsStructure;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.sounds.SoundPlayer;
import com.wurmonline.server.structures.DbFence;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.FenceConstants;
import com.wurmonline.shared.constants.StructureConstantsEnum;
import com.wurmonline.shared.constants.StructureStateEnum;

public class Maze
{
	private static Logger logger = Logger.getLogger(Maze.class.getName());

	private int mazeSize;                 // dimension of maze
    private boolean[][] north;     // is there a wall to north of cell i, j
    private boolean[][] east;
    private boolean[][] south;
    private boolean[][] west;
    private boolean[][] visited;
    @SuppressWarnings("unused")
	private boolean done = false;
    private StructureConstantsEnum fenceType; 
    
    private int offsetX = 0;
    private int offsetY = 0;
    private float fenceQl = 85.152f;
    
    public Maze(int startX, int startY, int size, StructureConstantsEnum fenceType)
    {
        this.mazeSize = size;
        this.offsetX = startX;
        this.offsetY = startY;
        this.fenceType = fenceType;
    }

    public void create(boolean animateCreation, boolean animateGrowth)
    {
//      StdDraw.setXscale(0, n+2);
//      StdDraw.setYscale(0, n+2);

      init();
      generate();
      draw(animateCreation, animateGrowth);
//      solve();
    }
    
    private void init()
    {
        // initialize border cells as already visited
        visited = new boolean[mazeSize+2][mazeSize+2];
        for (int x = 0; x < mazeSize+2; x++) {
            visited[x][0] = true;
            visited[x][mazeSize+1] = true;
        }
        for (int y = 0; y < mazeSize+2; y++) {
            visited[0][y] = true;
            visited[mazeSize+1][y] = true;
        }

        // initialze all walls as present
        north = new boolean[mazeSize+2][mazeSize+2];
        east  = new boolean[mazeSize+2][mazeSize+2];
        south = new boolean[mazeSize+2][mazeSize+2];
        west  = new boolean[mazeSize+2][mazeSize+2];
        for (int x = 0; x < mazeSize+2; x++) {
            for (int y = 0; y < mazeSize+2; y++) {
                north[x][y] = true;
                east[x][y]  = true;
                south[x][y] = true;
                west[x][y]  = true;
            }
        }
    }


    private void generate(int x, int y)
    {
        visited[x][y] = true;

        while (!visited[x][y+1] || !visited[x+1][y] || !visited[x][y-1] || !visited[x-1][y]) {
            while (true) {
                double r = StdRandom.uniform(4);
                if (r == 0 && !visited[x][y+1]) {
                    north[x][y] = false;
                    south[x][y+1] = false;
                    generate(x, y + 1);
                    break;
                }
                else if (r == 1 && !visited[x+1][y]) {
                    east[x][y] = false;
                    west[x+1][y] = false;
                    generate(x+1, y);
                    break;
                }
                else if (r == 2 && !visited[x][y-1]) {
                    south[x][y] = false;
                    north[x][y-1] = false;
                    generate(x, y-1);
                    break;
                }
                else if (r == 3 && !visited[x-1][y]) {
                    west[x][y] = false;
                    east[x-1][y] = false;
                    generate(x-1, y);
                    break;
                }
            }
        }
    }

    // generate the maze starting from lower left
    @SuppressWarnings("unused")
	private void generate()
    {
        generate(1, 1);

    	if(false) {
	    	logger.info("USING HARDCODED MAZE!");
	        north = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, false, true, false, false, false, true, true }, new boolean[]{ true, false, true, true, true, false, true, true }, new boolean[]{ true, true, true, false, true, true, true, true }, new boolean[]{ true, true, false, true, true, true, true, true }, new boolean[]{ true, true, true, false, true, true, true, true }, new boolean[]{ true, false, false, false, false, true, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
	        east = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, false, false, true, true, false, true }, new boolean[]{ true, false, true, false, false, false, false, true }, new boolean[]{ true, false, false, true, false, false, false, true }, new boolean[]{ true, false, false, false, false, false, false, true }, new boolean[]{ true, false, true, true, true, false, false, true }, new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
	        south = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, false, true, false, false, false, true }, new boolean[]{ true, true, false, true, true, true, false, true }, new boolean[]{ true, true, true, true, false, true, true, true }, new boolean[]{ true, true, true, false, true, true, true, true }, new boolean[]{ true, true, true, true, false, true, true, true }, new boolean[]{ true, true, false, false, false, false, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
	        west = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, false, false, true, true, false, true }, new boolean[]{ true, false, true, false, false, false, false, true }, new boolean[]{ true, false, false, true, false, false, false, true }, new boolean[]{ true, false, false, false, false, false, false, true }, new boolean[]{ true, false, true, true, true, false, false, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
    	}
    	
    	if(false) {
	    	logger.info("USING HARDCODED MAZE!");
    		north = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, false, true, false, false, false, true, true }, new boolean[]{ true, false, true, true, true, false, true, true }, new boolean[]{ true, true, true, false, true, true, true, true }, new boolean[]{ true, true, false, true, true, true, true, true }, new boolean[]{ true, true, true, false, true, true, true, true }, new boolean[]{ true, false, false, false, false, true, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
    		east = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, false, false, true, true, false, true }, new boolean[]{ true, false, true, false, false, false, false, true }, new boolean[]{ true, false, false, true, false, false, false, true }, new boolean[]{ true, false, false, false, false, false, false, true }, new boolean[]{ true, false, true, true, true, false, false, true }, new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
    		south = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, false, true, false, false, false, true }, new boolean[]{ true, true, false, true, true, true, false, true }, new boolean[]{ true, true, true, true, false, true, true, true }, new boolean[]{ true, true, true, false, true, true, true, true }, new boolean[]{ true, true, true, true, false, true, true, true }, new boolean[]{ true, true, false, false, false, false, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
    		west = new boolean[][] { new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, true, true, true, true, true, true }, new boolean[]{ true, true, false, false, true, true, false, true }, new boolean[]{ true, false, true, false, false, false, false, true }, new boolean[]{ true, false, false, true, false, false, false, true }, new boolean[]{ true, false, false, false, false, false, false, true }, new boolean[]{ true, false, true, true, true, false, false, true }, new boolean[]{ true, true, true, true, true, true, true, true }};
    	}
/*
        // delete some random walls
        for (int i = 0; i < n; i++) {
            int x = 1 + StdRandom.uniform(n-1);
            int y = 1 + StdRandom.uniform(n-1);
            north[x][y] = south[x][y+1] = false;
        }

        // add some random walls
        for (int i = 0; i < 10; i++) {
            int x = n/2 + StdRandom.uniform(n/2);
            int y = n/2 + StdRandom.uniform(n/2);
            east[x][y] = west[x+1][y] = true;
        }
*/
    }



    public void clear()
    {
    	for(int x = 0; x < mazeSize; x++) {
        	for(int y = 0; y < mazeSize; y++) {
        		clearHedges(offsetX + x - (mazeSize/2), offsetY + y - (mazeSize/2));
        	}
    	}
    }
    
    private void clearHedges(int x, int y)
    {
    	Fence f = MethodsStructure.getFenceAtTileBorderOrNull(x, y, TileBorderDirection.DIR_HORIZ, 0, true);
    	if(f != null && f.getQualityLevel() == fenceQl) {
    		f.destroy();
    	}
    	
    	f = MethodsStructure.getFenceAtTileBorderOrNull(x, y, TileBorderDirection.DIR_DOWN, 0, true);
    	if(f != null && f.getQualityLevel() == fenceQl) {
    		f.destroy();
    	}
    }
    

    private void draw(boolean animateCreation, boolean animateGrowth)
    {
    	int delay = 0;

    	try {
	    	for (int x = 1; x <= mazeSize; x++) {
			    for (int yIter = 1; yIter <= mazeSize; yIter++) {

			    	// Reverse the order so maze will will "draw" correctly. Generation "draws" bottom -> top, we want creation to be top to bottom.
			    	int y = mazeSize - yIter;

			    	// shortest delay on middle tiles
			    	delay = Math.max( Math.abs(x - (mazeSize / 2)) * 150, Math.abs(y - (mazeSize / 2)) * 150);
			    	
			    	if(x != 1 && y == (mazeSize-1)) {
			        	// Draw southern border (unless it's the absolute SW corner (1,1), which is always entrance/exit)
			    		if(animateCreation) {
			    			drawDelayedHedge(TileBorderDirection.DIR_HORIZ, x, y+1, delay, animateGrowth);
			    		} else {
			    			createHedge(TileBorderDirection.DIR_HORIZ, x, y+1, animateGrowth);
			    		}
			        }
	
			        if(x == mazeSize) {
			        	// Draw eastern border
			    		if(animateCreation) {
			    			drawDelayedHedge(TileBorderDirection.DIR_DOWN, x+1, y, delay, animateGrowth);
			    		} else {
			    			createHedge(TileBorderDirection.DIR_DOWN, x+1, y, animateGrowth);
			    		}
			        }
			        
			        if (north[x][yIter]) {
			        	//StdDraw.line(x, y+1, x+1, y+1);
			    		if(animateCreation) {
			    			drawDelayedHedge(TileBorderDirection.DIR_HORIZ, x, y, delay, animateGrowth);
			    		} else {
			    			createHedge(TileBorderDirection.DIR_HORIZ, x, y, animateGrowth);
			    		}
			        }
			        
			        if (west[x][yIter]) {
			        	//StdDraw.line(x, y, x, y+1);
			    		if(animateCreation) {
			    			drawDelayedHedge(TileBorderDirection.DIR_DOWN, x, y, delay, animateGrowth);
			    		} else {
			    			createHedge(TileBorderDirection.DIR_DOWN, x, y, animateGrowth);
			    		}
			        }
			        
			        if(Server.rand.nextInt(mazeSize) == 1) {
			        	SoundPlayer.playSound("sound.forest.branchsnap", x, y, true, 0.0f);
			        }

			    }
			}
		} catch (NoSuchZoneException | IOException e) {
			logger.log(Level.SEVERE, "Could not create maze", e);
		}
	}


    private void drawDelayedHedge(TileBorderDirection border, int x, int y, int fromNow, boolean animateGrowth) 
    {
		ShortEventDispatcher.add(new CreateMazeHedgeEvent(fromNow, this, border, x, y, animateGrowth));
    }

    @SuppressWarnings("unused")
	private void drawDelayedHedge(TileBorderDirection border, int x, int y, int minDelay, int maxDelay, boolean animateGrowth) 
    {
		ShortEventDispatcher.add(new CreateMazeHedgeEvent(minDelay + Server.rand.nextInt(maxDelay - minDelay), this, border, x, y, animateGrowth));
    }
    
    
    @SuppressWarnings("unused")
    private boolean isFenceAllowed(TileBorderDirection border, int x, int y)
    {
		int diffx = 0;
		int diffy = 0;
		
		if (border == TileBorderDirection.DIR_DOWN) {
			diffx = -1;
		} else {
			diffy = -1;
		}

		int tile = Server.surfaceMesh.getTile(x, y);
		byte type = Tiles.decodeType(tile);

		// Get bordering tile
		int tile2 = Server.surfaceMesh.getTile(x + diffx, y + diffy);
    	byte type2 = Tiles.decodeType(tile2);
    	
    	// Verify that fence is allowed to exist on this and bordering tile.
    	if(hasFence(border, x, y) == false
    		|| Terraforming.isCornerUnderWater(x, y, true)
    		|| isTileGrowHedge(type) == false
    		|| isTileGrowHedge(type2) == false
    		|| Zones.containsVillage(x, y, true) == true
    		|| Zones.containsVillage(x+diffx, y+diffy, true) == true
    	) {
    		return false;
    	}

    	return true;
    }
    
    private boolean isTileGrowHedge(byte type)
    {
		return type == Tiles.Tile.TILE_DIRT.id || type == Tiles.Tile.TILE_GRASS.id || type == Tiles.Tile.TILE_MYCELIUM.id 
			|| type == Tiles.Tile.TILE_MARSH.id || type == Tiles.Tile.TILE_STEPPE.id || type == Tiles.Tile.TILE_MOSS.id 
			|| Tiles.isTree(type) || Tiles.isBush(type) || type == Tiles.Tile.TILE_CLAY.id || type == Tiles.Tile.TILE_REED.id 
			|| type == Tiles.Tile.TILE_KELP.id || type == Tiles.Tile.TILE_LAWN.id || type == Tiles.Tile.TILE_MYCELIUM_LAWN.id 
			|| type == Tiles.Tile.TILE_ENCHANTED_GRASS.id;
    }
    
    private boolean hasFence(TileBorderDirection tbDir, int x, int y)
    {
    	int startX = offsetX - (mazeSize/2);
    	int startY = offsetY - (mazeSize/2);

    	Fence f = MethodsStructure.getFenceAtTileBorderOrNull(startX + x, startY + y, tbDir, 0, true);
    	if(f != null) {
    		return true;
    	}
    	
    	return false;
    }


    public boolean createHedge(TileBorderDirection tbDir, int x, int y, boolean animateGrowth) throws NoSuchZoneException, IOException
    {
    	//if(animateGrowth && fenceType != FenceConstants.HEDGE_FLOWER3_HIGH) {
       	if(animateGrowth && fenceType != StructureConstantsEnum.HEDGE_FLOWER3_HIGH) {
    		throw new RuntimeException("Can only animate growth on flower 3 hedges");
    	}
    	
    	// Center maze around start location
    	int startX = offsetX - (mazeSize/2);
    	int startY = offsetY - (mazeSize/2);
    	
    	int layer = 0;
		Zone zone = Zones.getZone(startX + x, startY + y, true);
		DbFence fence = new DbFence(
			(animateGrowth ? StructureConstantsEnum.HEDGE_FLOWER3_LOW : fenceType),
			startX + x,
			startY + y,
			0,
			1.0f,
			tbDir,
			zone.getId(),
			layer
		);

		if(fenceType != StructureConstantsEnum.FENCE_MAGIC_STONE) {
			fence.setHasNoDecay(true);
		}

		fence.setState(fence.getFinishState());
		fence.setQualityLevel(fenceQl);
		fence.improveOrigQualityLevel(fenceQl);
		fence.save();
		zone.addFence(fence);

		if(animateGrowth) {
			// invoke two GrowMazeHedgeEvent here (with fence and growth stage as argument), say, 100ms between them
			ShortEventDispatcher.add(new GrowMazeHedgeEvent(150, fence, StructureConstantsEnum.HEDGE_FLOWER3_MEDIUM));
			ShortEventDispatcher.add(new GrowMazeHedgeEvent(300, fence, StructureConstantsEnum.HEDGE_FLOWER3_HIGH));
		}
			
		return true;
    }
}

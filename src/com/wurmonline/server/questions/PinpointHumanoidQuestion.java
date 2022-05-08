package com.wurmonline.server.questions;

import java.util.Properties;
import java.util.logging.Level;

import com.friya.wurmonline.server.vamps.Locate;
import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.actions.CrownFindAction;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.spells.PinpointHumanoid;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;

public class PinpointHumanoidQuestion extends Question 
{
	private boolean properlySent = false;
	private double power;
	
	public boolean ignoreNoLo = false;
	public boolean reverseFind = false;
	public String extraQuestionNote = "";
	
	private Item locateItem = null;

	public PinpointHumanoidQuestion(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, aType != 79 ? 79 : 79, aTarget);		// hard code all to locate-question (79), no idea what the drawbacks are
		
		try {
			locateItem = Items.getItem(aTarget);
		} catch (NoSuchItemException e) {
			logger.log(Level.SEVERE, "Passed in item id into ctor is null");
		}
	}

	public PinpointHumanoidQuestion(Creature aResponder, String aTitle, String aQuestion, long aTarget, boolean eyeVyn, double power)
	{
		super(aResponder, aTitle, aQuestion, 79, aTarget);				// 79: locate-question

		this.power = power;
	}

	private Village getNearestDeedOf(int tileX, int tileY)
	{
		Village[] villages = Villages.getVillages();
		
		int closestX = 100000;
		int closestY = 100000;
		
		Village deed = null;
		for(Village v : villages) {
            int dx = Math.abs(v.getTokenX() - tileX);
            int dy = Math.abs(v.getTokenY() - tileY);
			
			if(dx <= closestX && dy <= closestY) {
				closestX = dx;
				closestY = dy;
				deed = v;
			}
		}

		return deed;
	}


	/**
	 * I want my own copy of this. And holy shit; the original -- I hope it was an artifact of decompiling!
	 * 
	 * Taken from EndGameItems.
	 * 
	 * @param mindist
	 * @param name
	 * @param direction
	 * @param includeThe
	 * @return
	 */
	private String getDistanceString(int mindist, String name, String direction, boolean includeThe)
	{
		String toReturn = "";
		
		if(mindist < 1) {
			toReturn = "You are practically standing on " + name;
			
		} else if(mindist < 4) {
			toReturn = name + " is " + direction + " a few steps away";
			
		} else if(mindist < 6) {
			toReturn = name + " is " + direction + " a stone's throw away";
			
		} else if(mindist < 10) {
			toReturn = name + " is " + direction + " very close";

		} else if(mindist < 20) {
			toReturn = name + " is " + direction + " fairly close by";
			
		} else if(mindist < 50) {
			toReturn = name + " is some distance away " + direction;
			
		} else if(mindist < 200) {
			toReturn = name + " is quite some distance away " + direction;
			
		} else if(mindist < 500) {
			toReturn = name + " is rather a long distance away " + direction;
			
		} else if(mindist < 1000) {
			toReturn = name + " is pretty far away " + direction;
			
		} else if(mindist < 2000) {
			toReturn = name + " is far away " + direction;
			
		} else {
			toReturn = name + " is very far away " + direction;
		}
		return toReturn;
	}


	// taken from MethodsCreatures
	private String getLocationStringFor(float rot, int dir, String performername)
    {
        int turnDir = 0;
        //float degree = 22.5f;
        float lRot = Creature.normalizeAngle(rot);
        if ((double)lRot >= 337.5 || lRot < 22.5f) {
            turnDir = 0;
        } else {
            for (int x = 0; x < 8; ++x) {
                if (lRot >= 22.5f + (float)(45 * x)) continue;
                turnDir = x;
                break;
            }
        }
        String direction = "in front of " + performername;
        if (dir == turnDir + 1 || dir == turnDir - 7) {
            direction = "ahead of " + performername + " to the right";
        } else if (dir == turnDir + 2 || dir == turnDir - 6) {
            direction = "to the right of " + performername;
        } else if (dir == turnDir + 3 || dir == turnDir - 5) {
            direction = "behind " + performername + " to the right";
        } else if (dir == turnDir + 4 || dir == turnDir - 4) {
            direction = "behind " + performername;
        } else if (dir == turnDir + 5 || dir == turnDir - 3) {
            direction = "behind " + performername + " to the left";
        } else if (dir == turnDir + 6 || dir == turnDir - 2) {
            direction = "to the left of " + performername;
        } else if (dir == turnDir + 7 || dir == turnDir - 1) {
            direction = "ahead of " + performername + " to the left";
        }
        return direction;
    }


    // taken from MethodsCreatures
    private int getDir(Creature performer, int targetX, int targetY)
    {
		double newrot = Math.atan2((targetY << 2) + 2 - (int)performer.getStatus().getPositionY(), (targetX << 2) + 2 - (int)performer.getStatus().getPositionX());
		float attAngle = (float)(newrot * 57.29577951308232) + 90.0f;
		attAngle = Creature.normalizeAngle(attAngle);
		//float degree = 22.5f;
		
		if ((double)attAngle >= 337.5 || attAngle < 22.5f) {
		    return 0;
		}
		for (int x = 0; x < 8; ++x) {
		    if (attAngle >= 22.5f + (float)(45 * x)) continue;
		    return x;
		}
		return 0;
    }

    
    private int getMeterDistance(int fromX, int fromY, int toX, int toY)
    {
    	 // given a tile is four meters
        int dx = Math.abs(fromX - toX);
        int dy = Math.abs(fromY - toY);
    	int ret = (int)(Math.sqrt(dx * dx + dy * dy)) * 4;
    	return ret;
    }
    

    private boolean isOnWaterTile(Creature player)
    {
    	return Terraforming.isTileUnderWater(player.getCurrentTileNum(), player.getTileX(), player.getTileY(), player.isOnSurface());
    }
    

    private String getHeightDifference(Creature performer, Creature target)
    {
    	int diff = (int)(( (float)(performer.getPosZDirts()) - (float)(target.getPosZDirts()) ) / 10.0f);
    	if(diff > 0) {
    		return Math.abs(diff) + " meters below";
    	} else if(diff < 0) {
    		return Math.abs(diff) + " meters above";
    	} else {
    		return "at same altitude as";
    	}
    }


    /*
	 *	finding / locating (skilled up by doing locate soul or using crown of amber); spell can always fail
	 *
	 *	Locate perks at skill-levels (Perception)
	 *	-  1 nothing
	 *		[20:44:42] In front of youThe Artemis is in front of you very close. 
	 *	-  5 whether in cave or on surface
	 *		[20:45:26] The Artemis is in front of you pretty close by. 
	 *	- 10 whether in enclosure/house/on a deed (as in, their hunted timer is not going down)
	 *		[20:46:13] Is not in a caveis in a safe areain front of youThe Artemis is in front of you very close. 
	 *	- 20 cheaper cast
	 *		[20:47:02] Is not in a caveis in a safe areain front of youThe Artemis is in front of you very close. 
	 *	- 25 whether on water tile
	 *		[20:47:39] Is not in a caveis in a safe areain front of youThe Artemis is in front of you very close. 
	 *	- 30 closer estimation of direction
	 *		[20:48:12] Is not in a caveis in a safe areain front of youThe Artemis is in front of you very close. 
	 *	- 40 faster cast
	 *		n/a
	 *	- 50 closer estimation of distance
	 *		[08:07:13] Is not in a caveis in a safe areais west-southwest of youis 24 meters away, and at same altitude as you
	 *	- 60 say whether target is online or whether the locate was a fail
	 *		[08:08:03] Is in this worldis not in a caveis in a safe areais west-southwest of youis 24 meters away, and at same altitude as you
	 *	- 70 say nearest deed
	 *		[08:13:17] Is in this worldis not in a caveis in a safe areais west-southwest of youis 24 meters away, and at same altitude as youis in the proximity of the settlement Vamps Hq
	 *	- 80 say nearest deed and approximate distance from it
	 *		[08:14:03] Is in this worldis not in a caveis in a safe areais west-southwest of youis 24 meters away, and at same altitude as youis 88 meters from Vamps Hq, the closest settlement
	 *	- 90 say nearest deed and distance and direction from it
	 *		[08:18:27] Is in this worldis not in a caveis in a safe areais west-southwest of youis 24 meters away, and at same altitude as youis 88 meters south-southwest of the closest settlement, Vamps Hq
	 *		
     */
    private boolean pinpointHumanoid(String name, Creature performer, double power)
    {
    	boolean found = false;

        Skill perception = performer.getSkills().getSkillOrLearn(VampSkills.PERCEPTION);
        double perceptionLevel = perception.getKnowledge();

        // give some skill for doing...
		perception.skillCheck(1.0f, 0.0f, false, 1.0f);

        Creature target = null;
        PlayerInfo pinf = PlayerInfoFactory.createPlayerInfo(name);
        
        if(pinf == null || pinf.loaded == false) {
        	return false;
        }

        try {
			target = Server.getInstance().getCreature(pinf.wurmId);
		} catch (NoSuchPlayerException | NoSuchCreatureException e) {
			e.printStackTrace();
		}
        
        if(target == null) {
            if(perceptionLevel >= 60) {
            	performer.getCommunicator().sendNormalServerMessage(name + " is not logged in.");
            }
        	return found;
        }

        int centerx = target.getTileX();
        int centery = target.getTileY();
        int dx = Math.abs(centerx - performer.getTileX());
        int dy = Math.abs(centery - performer.getTileY());
        int mindist = (int)Math.sqrt(dx * dx + dy * dy);
        int dir = getDir(performer, centerx, centery);
        String simpleDirection = getLocationStringFor(performer.getStatus().getRotation(), dir, "you");
        String simpleDistance = getDistanceString(mindist, target.getName(), simpleDirection, false);

        StringBuffer str = new StringBuffer();
        
        found = true;
        
        //logger.log(Level.INFO, "PPH Power (TODO: add perception!): " + power);
        //power = (byte)(Math.min(100, target.getSkills().getSkillOrLearn(VampSkills.PERCEPTION).getKnowledge() + Server.rand.nextInt(50)));

        if (ignoreNoLo == false && (double)(target.getBonusForSpellEffect((byte) 29)) > power) {
            // Online check (if we get here they ARE online)
            if(perceptionLevel >= 60) {
            	// 60+
    			str.append(target.getName() + " is around, but could not be found this time.");
    			return true;
            } else {
            	return false;
            }
        }
        
        // Surface check
    	if(perceptionLevel >= 5) {
    		// 5+
            str.append(target.getName() + " ");

            if(target.isOnSurface() == false) {
    			str.append("is in a cave");
    		} else if(perceptionLevel >= 25 && isOnWaterTile(target)) {
    			// 25+
    			str.append("is in the water");
    		} else {
    			str.append("is not in a cave");
    		}
    	}
    	
    	// Enclosure check
    	if(perceptionLevel >= 10) {
    		// 10+
    		boolean legalLoc = Stakers.isAtLegalLocation(target);
    		
    		if(legalLoc == false) {
    			str.append(" but is in a safe area. ");
    		} else {
    			str.append(" but is in the wilderness. ");
    		}
    	} else {
    		str.append(". ");
    	}
    	
		// Direction check
    	if(perceptionLevel < 30) {
    		// 0+
    		str.append(simpleDistance);
    	} else {
    		// 30+
    		str.append((target.isNotFemale() ? "He" : "She") + " is " + Locate.getCompassDirection(performer, target) + " of you");
    	}

    	// Distance check
    	if(perceptionLevel < 50) {
    		// 0+
    		//str.append(". " + simpleDistance);
        	if(perceptionLevel >= 5 && str.length() > 0) {
        		str.append(". ");
        	}
    	} else {
    		// 50+  
    		// 1 meter height = 10 dirts.
    		str.append(", " + getMeterDistance(performer.getTileX(), performer.getTileY(), target.getTileX(), target.getTileY()) + " meters away, " + getHeightDifference(performer, target) + " you. ");
    	}
    	
    	Village deed = getNearestDeedOf(target.getTileX(), target.getTileY());
    	
    	// Nearest deed check
    	if(deed == null && perceptionLevel >= 70) {
    		// do nothing
    		str.append(target.getName() + " is near no known settlement.");
    	} else if(perceptionLevel >= 90) {
    		// 90 say nearest deed and distance and direction from it
    		str.append(target.getName() + " is " + getMeterDistance(target.getTileX(), target.getTileY(), deed.getTokenX(), deed.getTokenY()) + " meters " + Locate.getCompassDirection(deed.getTokenX(), deed.getTokenY(), target.getTileX(), target.getTileY()) + " of the closest settlement, " + deed.getName() + ".");
    	} else if(perceptionLevel >= 80) {
    		// 80 say nearest deed and approximate distance from it
    		str.append(target.getName() + " is " + getMeterDistance(target.getTileX(), target.getTileY(), deed.getTokenX(), deed.getTokenY()) + " meters from " + deed.getName() + ", the closest settlement.");
    	} else if(perceptionLevel >= 70) {
    		// 70 say nearest deed
    		str.append(target.getName() + " is in the proximity of the settlement " + deed.getName() + ".");
    	}
    	
		Skill s = target.getSkills().getSkillOrLearn(VampSkills.PERCEPTION);
		s.skillCheck(1.0f, 0.0f, false, 1.0f);

		//if(Mod.isTestEnv() && performer.getPower() > 1) {
	    //	performer.getCommunicator().sendNormalServerMessage((int)(performer.getSkills().getSkillOrLearn(VampSkills.PERCEPTION).getKnowledge()) + ": " + str.toString());
		//}

		performer.getCommunicator().sendNormalServerMessage(str.toString());
    	
		return found;
    }


    @Override
	public void answer(Properties aAnswers)
	{
        if (!this.properlySent) {
            return;
        }
        boolean found = false;
        String name = aAnswers.getProperty("name");
        if (name != null && name.length() > 1) {
        	/*
        	if(Mod.isTestEnv() && this.getResponder().getPower() > 1) {
	        	for(int i = 0; i < 100; i += 5) {
	        		this.getResponder().getSkills().getSkillOrLearn(VampSkills.PERCEPTION).setKnowledge((double)i, false);
	        		found = pinpointHumanoid(name, this.getResponder(), this.power, this.override);
	        	}
        	}
        	*/
       		found = pinpointHumanoid(name, this.getResponder(), this.power);
       		
       		// This will let whomever you are looking for know exactly where YOU are too
       		if(found && reverseFind) {
       	        PlayerInfo pinf = PlayerInfoFactory.createPlayerInfo(name);
       	        
       	        if(pinf != null && pinf.loaded == true) {
           	        try {
           				Creature newTarget = Server.getInstance().getCreature(pinf.wurmId);
               			pinpointHumanoid(this.getResponder().getName(), newTarget, this.power);

                   		// Damage whatever item (Crown usually!) we are using to find this person...
                   		if(locateItem != null) {
        	           		float dmg = 0;
        	           		if(Server.rand.nextBoolean()) {
        	           			dmg = Server.rand.nextInt(8);
        	           		} else {
        	           			dmg = Server.rand.nextInt((int)locateItem.getDamage() + 1);
        	       			}
        	           		locateItem.setDamage(locateItem.getDamage() + dmg);
                   		}
           			} catch (NoSuchPlayerException | NoSuchCreatureException e) {
           				e.printStackTrace();
           			}
       	        }

       		}
        }

        if (!found) {
            this.getResponder().getCommunicator().sendNormalServerMessage("Could not find " + name);
        }
	}


	@Override
	public void sendQuestion()
	{
		boolean ok = true;

		if (this.getResponder().getPower() <= 0) {
		    try {
		        ok = false;
		        Action act = this.getResponder().getCurrentAction();
		        if (act.getNumber() == PinpointHumanoid.actionId || act.getNumber() == CrownFindAction.getActionId()) {	// was 419 = SPELL_LOCATE_PLAYER
		            ok = true;
		        }
		    }
		    catch (NoSuchActionException act) {
		    	throw new RuntimeException("No such action", act);
		    }
		}

		if (ok) {
			this.properlySent = true;
			
			StringBuilder sb = new StringBuilder();
			sb.append(this.getBmlHeader());

			if(extraQuestionNote.equals("") == false) {
				sb.append("text{text='" + extraQuestionNote + "'};");
			}

			sb.append("label{text='Name:'};input{id='name';maxchars='40';text=\"\"};");
			sb.append(this.createAnswerButton2());
			
			this.getResponder().getCommunicator().sendBml(
					300, 
					300, 
					true, 
					true, 
					sb.toString(), 
					200, 
					200, 
					200, 
					this.title
			);
		}
	}
}

package com.wurmonline.server.questions;

import java.util.Properties;
import java.util.logging.Level;

import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.actions.AidAction;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.zones.NoSuchZoneException;

public class AidQuestion extends Question 
{
	private boolean properlySent = false;
	private double power;
	private Item rat = null;

	AidQuestion(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, aType, aTarget);
	}

	public AidQuestion(Creature aResponder, String aTitle, String aQuestion, long aTarget, double power, Item rat)
	{
		super(aResponder, aTitle, aQuestion, 79, aTarget);		// 79: locate-question

		this.power = power;
		this.rat = rat;
	}
	

    private boolean aid(String name, Creature performer, double power)
    {
        PlayerInfo pinf = PlayerInfoFactory.createPlayerInfo(name);
        
        if(pinf == null || pinf.loaded == false) {
        	return false;
        }
        
        Creature target = null;
        
        try {
			target = Server.getInstance().getCreature(pinf.wurmId);
		} catch (NoSuchPlayerException | NoSuchCreatureException e) {
			performer.getCommunicator().sendNormalServerMessage("Your vampire senses reach out into the world, but you can't feel the presence of " + name + " anywhere.");
			return true;
		}
        
        try {
        	if(rat == null || performer.getInventory() == null || rat.getParent() == null || rat.getParent().getWurmId() != performer.getInventory().getWurmId()) {
                performer.getCommunicator().sendNormalServerMessage("You must carry it to be able to send it.");
        		return true;
        	}
        } catch (NoSuchItemException e) {
            performer.getCommunicator().sendNormalServerMessage("You must carry it to be able to send it.");
            return true;
		}
        
		performer.getStatus().modifyStamina((int)( performer.getStatus().getStamina() * 0.3f ));

        try {
			rat.putItemInfrontof(target);

			performer.getCommunicator().sendNormalServerMessage("You argue a bit with the vermin and request its help. Disgruntledly, the rat begins to change form as you see wings sprout from it's body.");
	        Mod.actionNotify(
	        	performer,
	        	"Transformed into a vampire bat, it quickly flies away in search of " + target.getName() + ".",
	        	"A vampire bat swoops away from " + performer.getName() + ".",
	        	"A vampire bat swoops away from a shadowy form."
	        );

	        Mod.actionNotify(
	        	target,
	        	"You see a vampire bat flying towards you from a distance.  After circling several times, it abruptly lands in front of you. Before your eyes, it transforms into a small rat. You see that it's been branded by " + Vampires.getVampire(performer.getWurmId()).getAlias() + ".",
	        	"A vampire bat swoops in and lands in front of %NAME.",
	        	"A vampire bat swoops in and lands in front of a shadowy form."
	        );
	        
			Skill s = performer.getSkills().getSkillOrLearn(VampSkills.AIDING);
			s.skillCheck(1.0f, 0.0f, false, 1.0f);

        } catch (NoSuchCreatureException | NoSuchItemException | NoSuchPlayerException | NoSuchZoneException e) {
			performer.getCommunicator().sendNormalServerMessage("Could not send the vermin. This could be a Friya booboo, tell admins or something!");
			logger.log(Level.SEVERE, "Could not send rat, for some reason", e);
			e.printStackTrace();
		}

		return true;
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
            found = aid(name, this.getResponder(), this.power);
        }

        if (!found) {
            this.getResponder().getCommunicator().sendNormalServerMessage("Not found.");
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
		        if (act.getNumber() == AidAction.actionId) {
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
			sb.append("text{text='Who are you looking to aid?'};");
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

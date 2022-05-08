package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreaturesProxy;
import com.wurmonline.server.creatures.Offspring;
import com.wurmonline.server.creatures.Traits;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;


public class SenseAction implements ModAction
{
	private static Logger logger = Logger.getLogger(SenseAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	
	public SenseAction()
	{
		logger.log(Level.INFO, "SenseAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Sense offspring", 
			"senses offspring",
			new int[] { 6 }	// 6 /* ACTION_TYPE_NOMOVE */
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Creature object)
			{
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(performer instanceof Player && target instanceof Creature && Vampires.isVampire(performer.getWurmId()) && target.isAnimal()) {
					return Arrays.asList(actionEntry);
				}
				
				return null;
			}
		};
	}

	
	@Override
	public ActionPerformer getActionPerformer()
	{
		return new ActionPerformer() {

			@Override
			public short getActionId() {
				return actionId;
			}

			// Without activated object
			@Override
			public boolean action(Action act, Creature performer, Creature target, short action, float counter)
			{
				return action(act, performer, null, target, action, counter);
			}
		
			@Override
			public boolean action(Action act, Creature performer, Item source, Creature target, short action, float counter)
			{
				int castTime = 5 * 10;		// 10ths of seconds
				
				if(performer instanceof Player == false || Vampires.isVampire(performer.getWurmId()) == false || target instanceof Creature == false || target.isAnimal() == false) {
					return true;
				}

				if(Vampires.isVampire(performer.getWurmId()) == false && performer.getPower() < 1) {
					// vampire spell
					return true;
				}
				
				try {
					if(counter == 1.0f) {
						performer.getCommunicator().sendNormalServerMessage("You close your eyes and focus your senses on " + target.getName() + "...");
						performer.getCurrentAction().setTimeLeft(castTime);
						performer.sendActionControl("Sensing offspring", true, castTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}
				
				Offspring offspring = target.getOffspring();
				
				if(offspring == null) {
					performer.getCommunicator().sendNormalServerMessage(target.getName() + " is not pregnant.");
					return true;
				}
				
				BitSet traits = toBitSet(CreaturesProxy.getTraits(offspring));
				//long father = CreaturesProxy.getFather(offspring);
				StringBuffer ret = new StringBuffer();
				String color = null;
				
				for(int i = 0; i < 27; i++) {
					if(traits.get(i) == true) {
						ret.append(Traits.getTraitString(i));
						ret.append(" ");
					}
				}
				
				if(traits.get(15)) {
					color = "brown";
				} else if(traits.get(16)) {
					color = "gold";
				} else if(traits.get(17)) {
					color = "black";
				} else if(traits.get(18)) {
					color = "white";
				} else if(traits.get(23)) {
					color = "ebony black";
				} else if(traits.get(24)) {
					color = "piebald pinto";
				} else if(traits.get(25)) {
					color = "blood bay";
				} else {
					color = "gray";
				}
				
				ret.append("It will be ");
				ret.append(color);
				ret.append(".");
				
				performer.getCommunicator().sendNormalServerMessage("You sense the coming offspring of " + target.getName() + ": " + ret.toString());

				Skill anatomy = target.getSkills().getSkillOrLearn(VampSkills.ANATOMY);
				anatomy.skillCheck(1.0f, 0.0f, false, 1.0f);
				
				return true;
			}
		}; // ActionPerformer
	}

	
	private BitSet toBitSet(long bits)
	{
		BitSet traitbits = new BitSet(64);
		
		for (int x = 0; x < 64; ++x) {
			if (x == 0) {
				if ((bits & 1) == 1) {
					traitbits.set(x, true);
					continue;
				}
				traitbits.set(x, false);
				continue;
			}
			if ((bits >> x & 1) == 1) {
				traitbits.set(x, true);
				continue;
			}
	
			traitbits.set(x, false);
		}

		return traitbits;
	}

}

package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Vampire;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;


public class AdminVampAction implements ModAction
{
	private static Logger logger = Logger.getLogger(AdminVampAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;

	public AdminVampAction()
	{
		logger.log(Level.INFO, "AdminVampAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Admin: Make a Vampire", 
			"fiddling",
			new int[] {}
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider()
		{
			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(performer.getPower() > 1 && Vampires.isHalfOrFullVampire(target.getWurmId()) == false && target.isPlayer() && subject != null && subject.getTemplateId() == ItemList.wandDeity) {
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
			public boolean action(Action act, Creature performer, Creature object, short action, float counter)
			{
				return true;
			}


			@Override
			public boolean action(Action act, Creature performer, Item source, Creature target, short action, float counter)
			{
				if(target.isPlayer() == false || performer.getPower() < 2 || source.getTemplateId() != ItemList.wandDeity) {
					return true;
				}

				if(Vampires.isHalfOrFullVampire(target.getWurmId()) == true) {
					performer.getCommunicator().sendNormalServerMessage("They are a half vampire or vampire already.");
					return true;
				}
				
				performer.getCommunicator().sendNormalServerMessage("Adding vampire status.");

				Vampire vampire = Vampires.createVampire((Player)target, false);

				if(vampire == null) {
					performer.getCommunicator().sendNormalServerMessage("FAILED! They might now be partially vampire, but not quite. They should probably log out and log back in at least.");
				} else {
					Mod.loginVampire((Player)target);
	                target.getCommunicator().sendAlertServerMessage("You are now a vampire - the boring way.", (byte)4);
					performer.getCommunicator().sendNormalServerMessage("Success! " + target.getName() + " is now a vampire. The boring way.");
				}

		    	return true;
			}
		}; // ActionPerformer
	}
	
}

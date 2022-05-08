package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;


public class AdminDevampAction implements ModAction
{
	private static Logger logger = Logger.getLogger(AdminDevampAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;

	public AdminDevampAction()
	{
		logger.log(Level.INFO, "AdminDevampAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Admin: Remove Vampire Status", 
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
				if(performer.getPower() > 1 && Vampires.isHalfOrFullVampire(target.getWurmId()) && target.isPlayer() && subject != null && subject.getTemplateId() == ItemList.wandDeity) {
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

				if(Vampires.isHalfOrFullVampire(target.getWurmId()) == false) {
					performer.getCommunicator().sendNormalServerMessage("That is not a half vampire or vampire.");
					return true;
				}
				
				performer.getCommunicator().sendNormalServerMessage("Removing vampire status .");
				boolean success = Vampires.deVampWithoutLoss(target);

				if(success == false) {
					performer.getCommunicator().sendNormalServerMessage("FAILED! They are probably fully or partially still vampire.");
				} else {
	                target.getCommunicator().sendAlertServerMessage("You are no longer a vampire.", (byte)4);
					performer.getCommunicator().sendNormalServerMessage("Success! " + target.getName() + " is no longer a vampire.");
				}

		    	return true;
			}
		}; // ActionPerformer
	}
	
}

package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.VampAchievements;
import com.friya.wurmonline.server.vamps.VampTitles;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.items.Amulet;
import com.friya.wurmonline.server.vamps.items.Crown;
import com.friya.wurmonline.server.vamps.items.Mirror;
import com.friya.wurmonline.server.vamps.items.Pouch;
import com.friya.wurmonline.server.vamps.items.Stake;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Achievements;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.zones.NoSuchZoneException;


public class BuyKitAction implements ModAction
{
	private static Logger logger = Logger.getLogger(BuyKitAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	
	public BuyKitAction()
	{
		logger.log(Level.INFO, "BuyKitAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Buy a black velvet pouch (5 silver)", 
			"buying a black velvet pouch",
			new int[] { 6 }	// ACTION_TYPE_NOMOVE
		);
		ModActions.registerAction(actionEntry);
	}

//  
	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Creature object)
			{
				// We want them to have a coin activated, give them a handy error in the action...
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(performer instanceof Player && target instanceof Creature && target.getName().equals(Vampires.kitSalesManName)) {
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
				if(performer instanceof Player && target instanceof Creature && target.getName().equals(Vampires.kitSalesManName)) {
					// Give warning that they need to activate a coin (1s, 5s, 20s, 1g, ...)
					performer.getCommunicator().sendNormalServerMessage(Vampires.kitSalesManName + " says, \"Buy a pouch with what? Think this is charity? Activate a coin of correct value. I'll accept some amulets and crowns too...\"");
				}

				return true;
			}

			@Override
			public boolean action(Action act, Creature performer, Item source, Creature target, short action, float counter)
			{
				if(performer.isPlayer() == false || target == null || target.getName().equals(Vampires.kitSalesManName) == false) {
					return true;
				}

				if(Vampires.isHalfOrFullVampire(performer.getWurmId()) && performer.getPower() < 2) {
					performer.getCommunicator().sendNormalServerMessage(Vampires.kitSalesManName + " yells, \"Be GONE foul beast! I do not do business with your kind!\"");
					return true;
				}
				
				if(source.getTemplateId() != ItemList.coinSilverFive && source.getTemplateId() != Amulet.getId() && source.getTemplateId() != Crown.getId()) {
					performer.getCommunicator().sendNormalServerMessage(Vampires.kitSalesManName + " says, \"You need to pay me and I don't have any change! I accept amulets and crowns as payment too...\"");
					return true;
				}

				try {
					String creator = Vampires.kitSalesManName;
					
					Item pouch = ItemFactory.createItem(Pouch.getId(), 5.0f, (byte)0, creator);
					pouch.setColor(0);	// dye it as black as we can

					Item stake = ItemFactory.createItem(Stake.getId(), 10.0f, (byte)1, creator);
					stake.setColor(255);
					pouch.insertItem(stake);

					Item mallet = ItemFactory.createItem(ItemList.hammerWood, 10.0f, (byte)0, creator);
					pouch.insertItem(mallet);

					Item mirror = ItemFactory.createItem(Mirror.getId(), 10.0f, (byte)0, creator);
					pouch.insertItem(mirror);

					// TODO: Make it so that new line works...
					Item papyrus = ItemFactory.createItem(ItemList.papyrusSheet, 10.0f, (byte)0, creator);
					String str = "\";maxlines=\"0\"}text{text=\""
								+ "I don't care what everyone says, there ARE vampires around!\n"
								+ "\n"
								+ "I have seen them...\n"
								+ "\n"
								+ "You must help me rid the world of these foul creatures. You will be rewarded!\n"
								+ "\n"
								+ "They seem to be largely unaffected by normal weapons, but these magical stakes "
								+ "seem to work. Mostly. Use the mallet to drive your wielded stake through the "
								+ "heart of the Vampire.\n"
								+ "\n"
								+ "Beware! Should you be successful in banishing one of these foul beasts, you will "
								+ "have their blood on your hands. They WILL hunt you as long as it is there. "
//								+ "Eventually you can wash your hands with Source.\n"
								+ "\n"
								+ "The magical stake that you bought from me will not work on ordinary humans. In "
								+ "fact, using them on a human will probably kill you.\n"
								+ "\n"
								+ "Oh, almost forgot... You cannot see their reflection in a mirror.\n"
								+ "\n"
								+ " \n"
								;
					papyrus.setInscription(str, creator);
					papyrus.setAuxData((byte)152);		// need this to easily separate it from other papyrus when people ask to become half-vampire

					//papyrus.setIsAlwaysLit(true);
					
					pouch.insertItem(papyrus);

					logger.log(Level.INFO, "DESTROYING " + source.getName() + " (material: " + source.getMaterial() + ") because they bought a black velvet pouch with it");
					Items.destroyItem(source.getWurmId());
					
					if (performer.getInventory().getNumItemsNotCoins() < 100) {
						performer.getInventory().insertItem(pouch, true);
					} else {
						pouch.putItemInfrontof(performer);
					}

					performer.getCommunicator().sendNormalServerMessage("You buy a black velvet pouch.");
					Achievements.triggerAchievement(performer.getWurmId(), VampAchievements.POUCHES);

					if(VampTitles.hasTitle(performer, VampTitles.VAMPIRE_HUNTER) == false) {
						((Player)performer).addTitle(VampTitles.getTitle(VampTitles.VAMPIRE_HUNTER));
					}

				} catch (FailedException | NoSuchTemplateException | NoSuchCreatureException | NoSuchItemException | NoSuchPlayerException | NoSuchZoneException e) {
					logger.log(Level.SEVERE, "Problem selling pouch", e);
					throw new RuntimeException(e);
				}

				return true;
			}
		}; // ActionPerformer
	}


}

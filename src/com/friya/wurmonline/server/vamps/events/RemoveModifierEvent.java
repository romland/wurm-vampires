package com.friya.wurmonline.server.vamps.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.SpellEffectsEnum;
import com.wurmonline.server.modifiers.DoubleValueModifier;

public class RemoveModifierEvent extends EventOnce
{
	private static Logger logger = Logger.getLogger(RemoveModifierEvent.class.getName());
	
	private Creature creature;
	private DoubleValueModifier modifier;
	private SpellEffectsEnum spellEffect = null;
	
	public RemoveModifierEvent(int fromNow, Unit unit, Creature c, DoubleValueModifier modifier, SpellEffectsEnum effectEnum)
	{
        super(fromNow, unit);

        this.creature = c;
        this.modifier = modifier;
        this.spellEffect = effectEnum;
        
		logger.log(Level.INFO, "RemoveModifierEvent created");
	}

	@Override
	public boolean invoke()
	{
		creature.getMovementScheme().removeModifier(modifier);
		
		if(spellEffect != null) {
			creature.getCommunicator().sendRemoveSpellEffect(spellEffect);
		}
		
		if(creature.isPlayer()) {
			creature.getCommunicator().sendNormalServerMessage("The crippling effect fades.");
		}
		
		return true;
	}
}

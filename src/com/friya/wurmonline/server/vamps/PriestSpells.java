package com.friya.wurmonline.server.vamps;

import java.lang.reflect.InvocationTargetException;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import com.wurmonline.server.deities.Deities;
import com.wurmonline.server.deities.Deity;
import com.wurmonline.server.spells.DispelShadows;
import com.wurmonline.server.spells.PinpointHumanoid;
import com.wurmonline.server.spells.Spell;
import com.wurmonline.server.spells.Spells;


public class PriestSpells
{
	static void onServerStarted()
	{
		addSpell(new DispelShadows());
		addSpell(new PinpointHumanoid());
	}
	
	
	static private void addSpell(Spell spell)
	{
        try {
            ReflectionUtil.callPrivateMethod(Spells.class, ReflectionUtil.getMethod(Spells.class, "addSpell"), spell);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        
        for (Deity deity : Deities.getDeities()) {
            deity.addSpell(spell);
        }
	}
}

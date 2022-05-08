package com.friya.wurmonline.server.vamps;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public interface DynamicExaminable
{
	public String examine(Item item, Creature performer);
	public int getTemplateId();
}

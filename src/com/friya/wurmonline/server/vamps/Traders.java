/**
 * This was originally a demonstration for: 
 * http://forum.wurmonline.com/index.php?/topic/147154-new-trader-item/
 * 
 * You want to run this from your onItemTemplatesCreated() hook in your Ago's
 * modloader Mod.
 * 
 * Basically, we are constrained by when database connections become available, so
 * the easiest way to go about it is really go in and create an entry in the IDS 
 * table of Ago's modsupport database. The important bit here is really seeing
 * to it that you book up a unique ID for your item.
 * 
 * I already had an item which I tested with, but you wanted a completely new
 * item, so...
 * 
 * Anyhow, add the following row to the table (but make sure 22762 is free in 
 * your DB, if not, take the next in sequence):
 * "22762"	"ITEMTEMPLATE"	"friya.tradertest"
 * 
 * I guess if I was going to do it proper I'd probably add something in preInit()
 * which gave me an opportunity to act as soon as connections became available.
 * 
 * What I tested:
 * - With a freshly cloned trader: buying entire stock of the newly added item,
 *   then reopening trade and buying the restocked item.
 * 
 */

package com.friya.wurmonline.server.vamps;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.friya.wurmonline.server.vamps.items.SmallRat;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

public class Traders
{
    private static Logger logger = Logger.getLogger(Mod.class.getName());


    static public void onServerStarted()
	{
		addItemToTrader(SmallRat.getId());
	}


	static private void addItemToTrader(int itemId)
	{
		try {
			String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature")
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.economy.Shop", "createShop", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							Object result = method.invoke(proxy, args);

							Item inventory = ((Creature)args[0]).getInventory();
							for(int x = 0; x < 3; ++x) {
								// this is our item...
								Item item = Creature.createItem(itemId, 50.0f);
								inventory.insertItem(item);
							}

							return result;
						}
					};
				}
			});
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed to add item to shop", e);
		}
	}

}

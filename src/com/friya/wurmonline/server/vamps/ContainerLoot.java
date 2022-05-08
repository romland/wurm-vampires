package com.friya.wurmonline.server.vamps;

/*
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.wurmonline.server.items.ItemList;

import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
*/

public class ContainerLoot
{
/*
	private static Logger logger = Logger.getLogger(ContainerLoot.class.getName());

	public ContainerLoot()
	{
	}

	private static void onItemTemplatesCreated()
	{
		String descriptor;
		try {
			// hook up to com/wurmonline/server/items/ItemFactory.java createItem() -- if it's a chest we could throw things in it
			// public static Item createItem(
			//		int templateId, float qualityLevel, float posX, float posY, 
			//		float rot, boolean onSurface, byte material, byte aRarity, 
			//		long bridgeId, @Nullable String creator, byte initialAuxData
			//	)
			descriptor = Descriptor.ofMethod(HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"), new CtClass[] {
				CtPrimitiveType.intType,
				CtPrimitiveType.floatType,
				CtPrimitiveType.floatType,
				CtPrimitiveType.floatType,
				
				CtPrimitiveType.floatType,
				CtPrimitiveType.booleanType,
				CtPrimitiveType.byteType,
				CtPrimitiveType.byteType,
				
				CtPrimitiveType.longType,
				HookManager.getInstance().getClassPool().get("java.lang.String"),
				CtPrimitiveType.byteType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.items.ItemFactory", "createItem", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Object result = method.invoke(proxy, args);
							postCreateItem(proxy, args, result);
							return result;
						}
					};
				}
			});
			
			//public static Item createItem(int templateId, float qualityLevel, byte material, byte aRarity, long bridgeId, @Nullable String creator)
			descriptor = Descriptor.ofMethod(HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"), new CtClass[] {
				CtPrimitiveType.intType,
				CtPrimitiveType.floatType,
				CtPrimitiveType.byteType,
				CtPrimitiveType.byteType,
				CtPrimitiveType.longType,
				HookManager.getInstance().getClassPool().get("java.lang.String"),
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.items.ItemFactory", "createItem", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Object result = method.invoke(proxy, args);
							postCreateItem(proxy, args, result);
							return result;
						}
					};
				}
			});
		
			
			
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed!", e);
			throw new RuntimeException("Failed to intercept checkAdvance()");
		}
	}
	
	static private void postCreateItem(Object proxy, Object[] args, Object createdObject)
	{
		if(((int)args[0]) == ItemList.treasureChest) {
			logger.log(Level.INFO, "A treasure chest was created, here we could fill it with random stuff...");
		}
	}
*/
}

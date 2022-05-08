package com.friya.wurmonline.server.vamps;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

public class DynamicExamine
{
	private static Logger logger = Logger.getLogger(DynamicExamine.class.getName());
	private List<DynamicExaminable> listeners = new ArrayList<DynamicExaminable>();
	private static DynamicExamine instance;


	static void onItemTemplatesCreated()
	{
		getInstance().setupExamineInterception();
	}


	public static DynamicExamine getInstance()
	{
		if(instance == null) {
			instance = new DynamicExamine();
		}

		return instance; 
	}


	private void setupExamineInterception()
    {
		try {
			String descriptor = Descriptor.ofMethod(HookManager.getInstance().getClassPool().get("java.lang.String"), new CtClass[] {
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature")
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.items.Item", "examine", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							String res = callListeners((Item)proxy, (Creature)args[0]);
							if(res != null) {
								return (String)(method.invoke(proxy, args)) + res;
							}

							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed", e);
		}
    }


	/**
	 * Note: If the item is plantable, dynamic examine will not work. Because Wurm.
	 * 
	 * @param listener
	 */
	public void listen(DynamicExaminable listener)
    {
        listeners.add(listener);
    }
    

    public int getListenerCount()
    {
    	return listeners.size();
    }


    /**
     * Any exceptions should be caught upstream by mod-loader.
     * 
     * @param lr
     * @return
     */
	private String callListeners(Item item, Creature performer)
    {
		for(DynamicExaminable listener : listeners) { 
    		if(listener != null && item.getTemplateId() == listener.getTemplateId()) {
    			return " " + listener.examine(item, performer);
    		}
    	}

    	return null;
    }
}

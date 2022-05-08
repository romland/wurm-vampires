package com.friya.wurmonline.server.vamps;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import com.wurmonline.server.items.Item;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class BloodlessHusk
{
    private static Logger logger = Logger.getLogger(BloodlessHusk.class.getName());

	static public void onItemTemplatesCreated()
	{
        CtClass theClass;
		try {
	        ClassPool classPool = HookManager.getInstance().getClassPool();

	        //
			// Add variable to Item saying who sucked the blood out of this poor corpse.
			// This will be forgotten between reboots; which I consider fine.
	        //
	        theClass = classPool.get("com.wurmonline.server.items.DbItem");
            CtField f = CtField.make("long bloodSucker = (long)0;", theClass);
            theClass.addField(f);

			String str = "public void setBloodSucker(long vampireId)"
	        		+ "	{"
	        		+ "		bloodSucker = (long)vampireId;"
	        		+ "	}";
	        CtMethod theMethod = CtNewMethod.make(str, theClass);
	        theClass.addMethod(theMethod);
	        logger.info("added setBloodSucker");
            
			str = "public long getBloodSucker()"
	        		+ "	{"
	        		+ "		return bloodSucker;"
	        		+ "	}";
	        theMethod = CtNewMethod.make(str, theClass);
	        theClass.addMethod(theMethod);
	        logger.info("added getBloodSucker");

			str = "public boolean isBloodlessHusk()"
	        		+ "	{"
	        		+ "		return bloodSucker != (long)0;"
	        		+ "	}";
	        theMethod = CtNewMethod.make(str, theClass);
	        theClass.addMethod(theMethod);
	        logger.info("added isBloodlessHusk");

		} catch (NotFoundException | CannotCompileException e) {
            Mod.appendToFile((Exception)e);
            throw new RuntimeException((Throwable)e);
		}

        logger.log(Level.INFO, "preInit completed");
	}


	static public boolean isBloodlessHusk(Item c)
	{
		try {
			Method m = c.getClass().getMethod("isBloodlessHusk", new Class[]{});
			return (boolean)m.invoke(c);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

	}
	

	static public void setBloodSucker(Item c, long vampireId)
	{
		try {
			Method m = c.getClass().getMethod("setBloodSucker", new Class[]{long.class});
			m.invoke(c, vampireId);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
	}


	static public long getBloodSucker(Item c)
	{
		try {
			Method m = c.getClass().getMethod("getBloodSucker", new Class[]{});
			return (long)m.invoke(c);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
	}
}

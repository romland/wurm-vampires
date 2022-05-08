/**
 * I was going to implement my own version of the Elite naming scheme, but bumped into this when trying to
 * find the C code: http://codegolf.stackexchange.com/questions/48077/generate-the-galaxies-of-elite
 * 
 * So, eh, who am I to argue... Borrowed it.
 * 
 * I removed pseudo-randomness and made it so that the name generation is always based on player's ID and 
 * creation timestamp.
 * 
 */
package com.friya.wurmonline.server.vamps;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.Message;
import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;


public class CovenChat
{
	private static Logger logger = Logger.getLogger(CovenChat.class.getName());

    private final static int[][] frequencies = new int[][]{   // Generated with names from http://www.minorplanetcenter.net/iau/lists/MPNames.html
        {96, 323, 512, 445, 222, 113, 407, 249, 514, 134, 740, 1287, 975, 3188, 212, 235, 13, 2131, 997, 909, 501, 398, 226, 50, 355, 165, 3223},
        {626, 61, 8, 8, 877, 3, 3, 28, 322, 5, 7, 114, 9, 8, 425, 4, 0, 396, 44, 6, 260, 0, 7, 0, 51, 3, 40},
        {607, 4, 129, 16, 287, 9, 14, 1728, 248, 4, 364, 139, 9, 8, 518, 7, 18, 99, 27, 41, 94, 2, 0, 0, 49, 42, 102},
        {842, 49, 13, 46, 784, 17, 38, 45, 425, 18, 21, 65, 56, 31, 503, 10, 2, 224, 90, 70, 169, 13, 50, 0, 86, 37, 421},
        {326, 203, 264, 375, 284, 128, 231, 143, 513, 67, 261, 1436, 399, 1874, 159, 179, 9, 2873, 940, 680, 206, 402, 180, 64, 317, 125, 1527},
        {161, 7, 3, 1, 246, 170, 6, 6, 128, 5, 2, 61, 16, 5, 122, 3, 0, 253, 16, 28, 105, 3, 3, 0, 4, 0, 122},
        {596, 16, 27, 27, 595, 8, 62, 150, 250, 17, 16, 134, 32, 100, 319, 11, 6, 272, 53, 28, 258, 3, 18, 10, 46, 18, 507},
        {1360, 24, 33, 16, 1001, 2, 9, 14, 1219, 9, 55, 123, 93, 204, 683, 8, 0, 178, 55, 88, 383, 21, 50, 0, 96, 0, 453},
        {1104, 148, 802, 362, 646, 85, 333, 97, 47, 160, 477, 789, 486, 2022, 309, 149, 15, 501, 1007, 614, 137, 142, 65, 43, 146, 147, 1598},
        {309, 5, 6, 13, 183, 1, 2, 2, 224, 1, 15, 2, 4, 11, 354, 1, 0, 3, 15, 6, 132, 2, 0, 0, 8, 2, 103},
        {1094, 15, 10, 9, 614, 6, 7, 166, 718, 9, 39, 133, 32, 36, 737, 9, 0, 137, 92, 20, 343, 17, 25, 1, 153, 2, 494},
        {1219, 105, 66, 280, 1319, 87, 73, 55, 1259, 17, 77, 1034, 142, 35, 622, 80, 4, 38, 209, 146, 271, 67, 21, 0, 224, 33, 612},
        {1790, 139, 135, 14, 591, 6, 6, 21, 859, 9, 16, 16, 120, 23, 599, 121, 5, 24, 87, 11, 252, 3, 12, 0, 78, 7, 249},
        {1139, 159, 291, 744, 1103, 55, 881, 124, 1014, 72, 298, 80, 108, 519, 735, 52, 12, 110, 543, 495, 140, 27, 59, 10, 203, 120, 2308},
        {72, 299, 215, 258, 167, 138, 150, 288, 124, 62, 282, 824, 456, 1643, 200, 199, 5, 1054, 815, 457, 408, 698, 201, 26, 145, 96, 1181},
        {517, 9, 5, 2, 474, 17, 3, 204, 263, 0, 16, 95, 10, 6, 311, 131, 0, 147, 45, 34, 87, 0, 1, 0, 24, 2, 66},
        {1, 0, 0, 0, 1, 0, 0, 0, 30, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 116, 2, 1, 0, 0, 0, 5},
        {1518, 162, 175, 366, 1113, 44, 325, 86, 1469, 18, 204, 176, 211, 294, 1196, 58, 11, 294, 331, 505, 439, 57, 37, 4, 311, 44, 1349},
        {904, 66, 452, 26, 770, 22, 17, 935, 522, 13, 439, 132, 109, 56, 570, 131, 10, 28, 442, 932, 437, 46, 44, 0, 68, 50, 1616},
        {1009, 24, 60, 13, 1026, 20, 21, 607, 618, 29, 47, 74, 52, 35, 862, 12, 0, 399, 365, 481, 180, 18, 30, 1, 90, 125, 657},
        {164, 191, 273, 207, 233, 51, 186, 75, 170, 55, 238, 344, 292, 555, 55, 72, 5, 694, 672, 274, 13, 35, 23, 32, 76, 82, 421},
        {535, 3, 4, 4, 441, 0, 10, 3, 577, 1, 16, 28, 1, 12, 153, 4, 0, 26, 91, 4, 12, 1, 0, 0, 30, 1, 475},
        {516, 5, 2, 5, 289, 4, 2, 43, 290, 0, 17, 32, 11, 26, 109, 2, 0, 25, 57, 1, 33, 0, 1, 0, 13, 0, 49},
        {35, 2, 7, 2, 25, 2, 2, 3, 63, 1, 3, 5, 0, 0, 9, 1, 0, 4, 3, 5, 26, 1, 3, 0, 0, 0, 82},
        {557, 38, 55, 46, 138, 22, 26, 31, 72, 18, 44, 126, 75, 143, 238, 31, 1, 81, 108, 57, 214, 9, 27, 9, 6, 8, 706},
        {241, 12, 4, 22, 196, 0, 8, 146, 142, 0, 17, 11, 16, 15, 87, 2, 1, 11, 23, 11, 122, 10, 13, 0, 19, 49, 206},
        {1282, 1246, 1014, 818, 540, 466, 757, 928, 351, 680, 1207, 835, 1479, 552, 375, 957, 40, 795, 1710, 952, 150, 455, 435, 34, 288, 226, 0}
    };

    private final static int MIN_NAME_LENGTH = 4;
    public final static String CHANNEL_NAME = "Coven";

    CovenChat()
    {
    }
    
    
    public static void onPlayerLogin(Player p)
    {
    	sendStart(p);
    }


	public static String generateAlias(Player player)
	{
		return generateAlias(player.getWurmId(), player.getSaveFile().creationDate);
	}
	

	public static String generateAlias(long playerId, long creationDate)
	{
		// We base the Vampire alias on creation timestamp and the player ID,
		// ID will definitely be unique, and both are very unlikely to change between sessions.
        long seed = hash( playerId ^ creationDate );

        // On test-server: <Vampire Friya Homshurogh> kek
        String name = generateName(new Random(seed));
        
		logger.log(Level.INFO, "generateAlias(): " + name);
		return name;
	}


	private static String generateName(Random rand)
	{
        int previousChar = randomIndex(frequencies[26], rand);
        String name = "" + (char)(previousChar + 'A');

        for (;;) {

            int[] frequency = frequencies[previousChar].clone();

            frequency[26] *= Math.max(name.length() - (MIN_NAME_LENGTH - 1), 0) ; //Hack, reduces name lengths.

            int nextChar = randomIndex(frequency, rand);

            if (nextChar == 26){
                return name;
            }

            previousChar = nextChar;

            name += (char) (nextChar + 'a');
        }
    }


	private static int randomIndex(int[] frequencies, Random rand) {

        int total = Arrays.stream(frequencies).sum();

        int n = rand.nextInt(total);
        int i = 0;

        for (int frequency : frequencies){
            n -= frequency;
            if (n < 0){
                return i;
            }
            i++;
        }

        throw new IllegalStateException();
    }


    /**
     * http://stackoverflow.com/a/9640543/4230423
     * 
     * @param i
     * @return
     */
    private static long hash(long i)
    {
        return (int) ((131111L*i)^i^(1973*i)%7919);
    }
    
    
    /**
     * 
     * 
     * @param title
     * @param message
     * @param player
     */
    public static void message(String title, String message, Player player)
	{
		if(!title.equals(CHANNEL_NAME)) {
			// bail quietly
			return;
		}

		if(Mod.logExecutionCost) {
			logger.log(Level.INFO, "message called");
			Mod.tmpExecutionStartTime = System.nanoTime();
		}

		Communicator comm = player.getCommunicator();

		if(Vampires.isVampire(player.getWurmId()) == false && player.getPower() <= 0 && Vampires.fakeGMs.contains(player.getName()) == false) {
			comm.sendNormalServerMessage("You may not use this channel.");
			return;
		}

		if (player.isMute()) {
			comm.sendNormalServerMessage("You cannot chat while muted.");
			return;
		}

		if(player.isDead()) {
			comm.sendNormalServerMessage("Only Death would hear you. You doubt he'd listen...");
			return;
		}
		
		if(comm.isInvulnerable()) {
			comm.sendNormalServerMessage("You cannot chat until you have lost invulnerability.");
			return;
		}

		if(message.startsWith("/me ")) {
			comm.sendNormalServerMessage("You can't emote here.");
			return;
		}
		
		Vampire v = Vampires.getVampire(player.getWurmId());

		String sendStr;
		Message adminMess;
		
		if(player.getPower() > 0 || Vampires.fakeGMs.contains(player.getName()) == true) {
			// Admin names are not aliased and prefixed with GM
			sendStr = "<GM " + player.getName() + "> " + message;
	        adminMess = new Message(player, (byte) 10, title, sendStr);
	        
		} else {
			sendStr = (player.isNotFemale() ? "<Sir " : "<Lady ") + v.getAlias() + "> " + message;
	        adminMess = new Message(player, (byte) 10, title, (player.isNotFemale() ? "<Sir " : "<Lady ") + v.getAlias() + "> [" + player.getName() + "] " + message);
		}

		Message mess = new Message(player, (byte) 10, title, sendStr);
        
        Player[] playarr = Players.getInstance().getPlayers();
        for (int x = 0; x < playarr.length; ++x) {
            if (playarr[x].getCommunicator().isInvulnerable() && playarr[x].getPower() <= 0) {
            	continue;
            }
            
			if(playarr[x].getPower() > 0 || Vampires.fakeGMs.contains(playarr[x].getName()) == true) {
				playarr[x].getCommunicator().sendMessage(adminMess);
			} else if(Vampires.isVampire(playarr[x].getWurmId())) {
				playarr[x].getCommunicator().sendMessage(mess);
			}
        }

        player.chatted();

		if(Mod.logExecutionCost) {
			logger.log(Level.INFO, "message done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}
	}


	static void sendStart(Player player)
	{
		if(Vampires.isVampire(player.getWurmId()) || Vampires.fakeGMs.contains(player.getName()) == true || player.getPower() > 2) {
			logger.log(Level.FINE, "sendStart called for vampire!");
			
			Vampire v = Vampires.getVampire(player.getWurmId());

			String[] msgs = new String[]{
				"",
				"      Your lust for blood is growing.  You are a Vampire.",
				"      This channel is shared with your brethren. Your name here is an alias, as is everyone elses.",
				"      You probably want to keep your alias (" + (v == null ? "n/a" : v.getAlias()) +") disconnected from your real name and that you are a Vampire ... to yourself.",
				"      Watch out for traitors and slayers...",
				"      " + ((player.getPower() > 1 || Vampires.fakeGMs.contains(player.getName()) == true) ? "NOTE: You are an admin, you can see real names!" : "")
			};

			for(int n = 0; n < msgs.length; n++) {
				Message mess = new Message(player, (byte) 16, CHANNEL_NAME, msgs[n], 250, 150, 250);
				player.getCommunicator().sendMessage(mess);
			}
			
		} else {
			logger.log(Level.FINE, "sendStart called for non-vampire");
		}
	}
}

The Game
--------
The primary goal of the mod is to add an element of opt-in PvP to PvE servers. The "currency" stolen between the competing parties are skills and affinities.

There are Vampires and there are Vampire slayers. Slayers can "stake" vampires whenever they find one, when successful, they will get tagged as "hunted", meaning all vampires online can punish this slayer during a set time period. That is, provided they can find and catch them.

Stakers are generally opportunistic and will have the upper hand since they are the ones deciding when and if they want to punish a vampire. For the vampire, everyone is a potential enemy. Always.

This is not a game of fighting in the traditional Wurm sense, it's a game of stealth, betrayal / social engineering, tracking and hunting.

Together with bloodlust, vampire slayers are the ones balancing vampires. There should be people ridding this world of the pesky vampires. Balance was and is a primary concern when I add or change any functionality. The intention is to add *more* perks to Vampires as I feel they should be more attractive.

All in all, this mod sits next to everything else in the game. It is not disruptive nor intrusive, the people that choose to participate are really the only ones involved.

Enabling this mod on PvP servers will screw up some aspects of PvP. For instance, it will enable same-faction one-shot "killing", improved locate spells and probably heaps of other things. Feel free to try, of course. :)


The code
--------
It's a fair amount of code, and the following is merely an overview and is likely far from complete. The project is somewhere around 20,000 lines of code. I will move it from a private repo to a public one after I have cleaned up the code a bit. It will most likely be MIT licensed. For now, though, you can just decompile it.

If you have ideas or do any changes, it would be appreciated if you return the favor and share it too. :)

The entire mod is pretty safe, no dodgy hooks are being made using line numbers or similar. Likewise, direct bytecode manipulation was avoided and is not used anywhere. There are a few locations where I could not inject classes in the right package, so had to resort to reflection to make some data public. This takes place at rarely executed code, though.

As a testament to safety, when 1.3 was released, I did not have to change a single line of code in the mod. I did however add new functionality around CCFP and timed affinities, of course.

Very few things are configurable as it stands (it's not been a priority to make this generic). But introducing it on a server is as easy as installing any other mod, removing it is equally easy.

Some links related to the project:
[] The initial announcement (it was pretty far progressed at this point)
   http://www.filterbubbles.com/wurm-unlimited/vamps/announcement.html
   (I apologize for formatting of this one -- but ... lazy)

[] http://zenath.net/forums/forumdisplay.php?fid=3
   Check the various patch notes as they contain quite a bit of information at times. For instance: http://zenath.net/forums/showthread.php?tid=122

[] http://zenath.net/forums/showthread.php?tid=107
   A feedback thread containing quite a few informational posts.

There are probably more links on the forums there, search and you shall find.

Feel free to see it all as example code.


Dependencies
------------
It depends on another mod of mine, LootTables -- that mod can be found on Wurm Online forums.
 

How to become a Vampire
-----------------------
It's a mini-quest consisting of the following:
[] Talk to Dhampira the Ponderer, she will want a papyrus containing some information about Vampires
[] Vampire hunter D is a salesman, buy a staker's kit from him, in there you will find a papyrus sheet
[] Hand the papyrus sheeto Dhampira
[] You now get a clue containing a piece of the map (if you look at the Zenath map in flat view, you will see where this is)
[] Go to the location and you will find Orlok, he will want you to sacrifice a corpse of a champion creature
[] Once sacrificed, you will find yourself a full vampire.

<<1 TODO: screenshots of location clue>>


Skill stealing
--------------
[ActionSkillGain ActionSkillGains]

The primary goal of Vampires is to add an opt-in PVP aspect to PVE servers. The "currency" stolen between the competing parties are skills. That is, a Vampire Slayer stakes a Vampire and a roll says: Take from Vampire's blacksmithing skill.

Technically this works like this: On startup of server several lookup tables are generated stating that e.g. 90 blacksmithing requires 120,000 actions to get. 

When a vampire gets staked, I say that the cost of getting punished by a slayer is e.g. 5,000 actions. The amount of skill removed from the vampire at their current blacksmithing would then be a varying amount depending on how high their skill is. The slayer is rewarded the same way, depending on their skill level, 5,000 actions will give them a variable amount of skill.

There are several other factors at play as well. Such as:
[] some skills are excluded (e.g. fighting related skills),
[] might not get the full action count because the skill was too low,
[] caps to prevent someone being knocked down too low in that skill,
[] difficulty weights (think: meditation vs digging, ...),
[] preferred skills (i.e. a bit higher chance to steal dexterity),
[] a weight that will make you prefer going after high skilled players

In addition, there's a chance to steal an affinity of a target.

Vampires punishing hunted slayers works pretty much the same way. Difference is that the amount of skill stolen for one bite is a lot less than for one staking. The reason for this being much lower is because all vampires can repeatedly punish a hunted slayer for the duration of the hunt.

One important thing to note here is that there is a net loss "to the system" in skill transfer to prevent abuse. That is, only 80-90% of what is stolen is transferred to the other party.

Bloodlust
---------
[BloodlessHusk BloodLust SmallRat]

Bloodlust is the biggest drawback to being a vampire. They have to feed on fresh corpses regularly in order to keep their bloodlust down. If bloodlust gets too high, it will start feeding on the vampire, eventually killing it. That said, there are also a number of positive things to high bloodlust.

Bloodlust works in a similar fashion to favor in that it uses a skill to keep track of its current status. Bloodlust at zero means that there is no need to feed at all, bloodlust at 100 means you are getting hurt.

Bloodlust generates slower the higher it gets and if a vampire dies, its bloodlust will be set to 60 if it is higher. For every affinity in Bloodlust, there will be a 10% slowed down increase. If you currently have sleepbonus active, bloodlust will generate at half the pace, this is to be a bit more forgiving towards skillers.

All in all, if you have under 95 Bloodlust you are in no particular danger except that you are drawing close to the fact that you must feed. Above 95 the vampire will take start taking damage at an increasing rate the higher it gets.

The upsides to high bloodlust are: The higher it is, the more your wounds will heal and you will get increased number of timed affinities, CCFP, nutrition. There are more perks to it spread out through the codebase, but these are the things that happen immediately as you feed with a high bloodlust.

At the end of the day: Vampires will never have to worry about nutrition, CCFP, cooking or timed affinities.

When feeding, a bloodless husk is left behind in stead of a corpse. Every bloodless husk will keep a reference to the vampire that fed, enabling slayers to track vampires.

Vampires can purchase small rats from traders (these can be carried) for emergency feeding. They are expensive.

<<2 TODO: Screenshot of bloodlust damaging>>
<<3 TODO: Screenshot of small rat>>

The console
-----------
[ChatCommands]

/slayers
/hunted
/toplist


The Coven (chat channel)
------------------------
[CovenChat]

Vampires are encouraged to stay anonymous as that is their best defense against slayers. Vampires have plenty of actions to stay under the radar as long as they modify their playing style and use stealth to their advantage.

And they can't even trust their own kind... so when they first become a vampire, an alias is assigned to them. This alias will be used for all interaction on this channel. As GM you will see the real name behind every alias, no one else can. It's up to the vampires themselves if they want to reveal who they really are.

Names are automatically generated based on user's ID and the timestamp character was created. If someone is incredibly unhappy with the automatically generated alias, it can be modified in the "Vampires" table of the database. I recommend against changing someone's alias for other reasons, though (it's their fault if they were found out).

<<4 TODO: Screenshot of chat tab>>

Random Loot
-----------
[CreatureLoot]

Vampires depend on another mod of mine, LootTables. It uses that mod to plant random drops of a couple of items that are needed:
[] Crown of Friya (the name of this one is not my fault!)
[] Ancient Amulet
[] Small Rat

Three loot rules are created that add a chance to all NPC's to drop these (~0.3% chance).

In addition to adding the rules to LootTables, it will also intercept all drops and force Crown of Friya to always be rare.


Creatures
---------
[Creatures]

A number of creatures are added to the game. A lot of the code in "Creatures" actually went into shutting up and forcing the "NPC" template to stand still. At the end of the day, this was a bad choice of mine, I should have just hooked in and allowed other templates to show up in Local. But, oh well, what's done is done.

The creature templates are:
[] Vampire (used for Orlok, head vamp)
[] Vampire Guard (used for guards next to Orlok)
[] Protected Human (used for Dhampira, Vampire hunter D and van Helsing)
[] Domestic black dragon (not currently in use, was part of a plan to let Vampires breed black dragons)

Orlok will always spawn in on the center tile of a GM created zone called "The Coven". If that zone does not exist it will default to a fixed position which should be visible in the server log.

The humans around it (Dhampira, D, Helsing) will spawn near first spawn point of the server. But can be placed anywhere.


Dynamic Examine
---------------
[DynamicExaminable, DynamicExamine]

For altar of souls I needed to be able to easily attach dynamic long descriptions for items. In order to use it, implement the interface and call listen() in DynamicExamine. See items.AltarOfSouls for the example.

Events
------
[EventDispatcher ShortEventDispatcher]
The mod has its own event dispatching system, these two classes are the ones implementing the calling of them. There is one for longer events and one for shorter (a maximum of 10 seconds). They are separated into two classes because short events are polled a lot more frequently (every 1/20 of a second).

Execution Cost
--------------
[ExecutionCostFormat]
Since this is a pretty large code-base as far as Mods go, you can enable execution cost of everything in the mod to see how it affects the game. I'll say it right here: it does *not* affect the performance of the game at all. But if you want proof, enable it in the properties file.

Locate
------
[Locate]
This is just a static helper for the various locate spells related to Vampires and their Slayers.

Entry point
-----------
[Mod]
Entry point of the code. It utilizes pretty much every hook in the modloader, I think. There are some WU code changes applied from this class as well, mainly because I was lazy and did not find proper homes for them in other classes. The file is a minor mess and should be cleaned up.

Priest spells
-------------
Vampires add a few priest spells that are available to all religions. This class adds functionality to easily add priest spells.

The spells added are (more info on them at another place):
[] Dispel shadows - dispels every stealthed character in a large radius of the caster
[] Pinpoint humanoid - a much more versatile "locate soul" that depend on caster's skill in Perception.

Stakers (Vampire Slayers)
-------------------------
[Staker Stakers StakeAction Stake]
Vampires opt in to the PVP when they become a vampire. Stakers opt in when they stake a vampire. When that happens a ninety minute free-for-all biting party starts on the slayer.

A staker is also a valid target as long as they are wielding a Stake of Vampire Banishment, more on this below.

The countdown of the ninety minutes will only tick if the slayer is in reachable wilderness and logged in. That is, time will not tick if slayer:
[] is within an enclosure (an enclosure can consist of fences, house walls, bridges)
[] is in a cave
[] is on deed
[] is not on ground
[] is dead
[] is mounted
[] is floating
[] is ghost
[] is logged out
[] is teleporting
[] is in water
[] is within a banned zone (manually set by GMs)
[] is on a perimeter

The within enclosure is the tricky bit here since Wurmians are generally rather ... curious beings. There's quite a bit of code around this, but to put it briefly: I use rays going out from player, if too many rays hit either fence, bridge or house wall, the time will not tick. The rays are checked against a 2D boolean matrix containing obstacles. It's very cheap if initial check of four rays pass. That said, fetching the obstacles can definitely be optimized (it now queries DB).

In addition to above, a hunted staker will have a few more limitations:
[] Their mount will be treated as trait-less (gear will still count the same)
[] They can only lead their mount at most a few seconds (enough to embark, basically)
[] They cannot teleport (on our server, Zenath, Farwalkers are disabled, if they exist in your server they will work)

All vampires will be notified on the Coven channel when hunted slayers log out or in. When the 90 minute hunted period is up for a staker (slayer), they will get the following message:
> Your hands finally wash clean of the blood. You are no longer marked as a vampire slayer.

and the vampires will get the following announcement on the Coven channel:
> Friya is no longer marked as a vampire slayer. The time of the hunt will now cease!

If a vampire punishes a slayer by biting them after this period is up, the vampire will lose skill points.

Actual staking
--------------

A staking kit is obtained by talking to Vampire hunter D, he'll sell a black velvet pouch containing:
[] a mirror
[] a papyrus sheet with instructions / warnings
[] a mallet
[] a stake of vampire banishment

In order to stake someone, the slayer needs to wield the stake, activate the mallet and then right click the target (hopefully a vampire). It is adviced that they check out the instructions first, and also, they probably want to make sure that target is indeed a vampire. This can be done by using the mirror on the target. Staking a non-vampire will result in death.

Once a stake is equipped, it can not be dropped and can only be unequipped in the following ways:
[] Use on a target
[] Get disarmed by a vampire
[] Die
[] Toss in a garbage heap

This is to force stakers to commit and make them unable to cheese it to get the vampires to bite them while they juggle the stake between equipped and unequipped. The reason garbage heap was chosen as the default way to get rid of them is that these items are not portable. You can't bring them with you when you are out hunting.

Things that get checked when you attempt to slam that stake into a vampire:
[] Slayers can wield a stake in either hand
[] Mallet must be equipped
[] Target must be vulnerable
[] GMs can stake NPCs (for testing)
[] Vampires cannot be staked in "The Coven"
[] Stakers must have at least 35 FS (this is to put a threshold for possible alts)
[] After wielding a stake, slayer have to wait a few seconds for it to settle before being able to use it
[] Slayer must wait a few seconds between staking attempts (in case vampire dodged)
[] Vampires cannot use the stake
[] If slayer is already hunted, they cannot stake
[] They cannot be mounted
[] There can be at most one tile between slayer and the target
[] Half vampires cannot be punished, staking one will inflict a wound on the staker and the stake is destroyed.
[] Staking a human being will result in the death of the slayer and destruction of the stake

If a vampire is wearing an ancient amulet over their heart, it will protect the vampire. When the amulet is struck by a stake, both stake and the amulet are destroyed.

Striking an amulet of a vampire will not result in a full hunt, but the following *will* happen: The (would-be) slayer will get flagged as biteable by all vampires for a few minutes, but unlike a normal hunt, no other restrictions apply: They can still stake vampires, for instance. But there will be a gap where the would-be slayer is 100% safe to bite since they wielding a stake takes a little while before being usable. Announcements will go out on Coven channel when these "mini hunts" take place, but don't confuse them with a full-fledged hunt since you can get staked.

If I was the vampire here, my immediate action would be: Enter stealth, then bite as many times as I can. The counter by the slayer here would probably have to be leg it, cast dispel shadows, wield new stake, go after the vampire. But ... risky business.

If a stake is made of seryll, this is done by using ancient amulet (see MakeSeryllStake section), a vampire cannot dodge the attempt. If they were an amulet they will still be protected, however. Bottom line is: Ancient amulets are handy for both parties.

A skillcheck is performed between the staker and the vampire. Vampires get a bonus to dodge if it is during the night. The check involves: Dexterity, current stamina level and Perception.

If all of the above is successful, the following will said to the slayer:
> You stake the vampire through the heart! The stake banishes it back to the realm of darkness and rewards you for your successful hunt!

Surrounding will see, e.g.:
> Friya stakes Cenotaph through the heart! Cenotaph is revealed as a vampire and banished to the realm of darkness!

All vampires online will get the following announcement on the Coven channel:
> Friya has revealed herself as a vampire slayer!
> LET THE HUNT BEGIN!

This happens next:
[] a bloody vampire fang is dropped on the ground (this is intended to be part of a repeatable quest in the future)
[] a red beam is spawned on the location (essentially pinpointing exactly where slayer is to all vampires)
   Vampires can teleport to this location using "assist slain" for the next 10-15 minutes. This can of course be a trap.
   But whomever staked the vampire cannot stake again, but who says he did not have friends.
[] Skill theft is rolled for and transferred from vampire to staker
[] A roll is done for whether an affinity should be stolen
[] Achievements handed out
[] Titles handed out
[] Vampire is teleported to the Coven
[] Vampire's bloodlust is set to 1
[] Flag slayer as hunted (and everything that entails)


Toplist
-------
[Toplist]
Vampires have a shared toplist where you can see who the best hunters are, this is calculated by checking how much skill they have managed to steal from slayers. The name shown in the toplist is the vampire's alias (so they can stay anonymous). There are some checks to determine whether someone is cheesing it from same steam account and such. But the way I see it is: if someone abuse this to get higher on some list -- just remove them from the list and call it a day.

There is currently no toplist for stakers, but it is easily added.

<<5 TODO: Screenshot of toplist>>


Achievements
------------
There are a bunch of achievements added for both stakers and vampires, some are meta achievements, some are counters and some are normal achievements. Using the method VampAchievements.addAchievement(), it's very easy to add new ones.

<<6 TODO: screenshot of an achievement ticking>>


Vampire
-------
[Vampire Vampires DevampAction]
In general, most of the traits around a vampire is spread out through this document, this blurb here outlines the Vampire/Vampires class.

Before becoming a vampire you are a half vampire; which is sort of a lead-in to give hints on what is to come around bloodlust, healing, eating. You can at any point stop being a vampire by talking to the NPC van Helsing, but it comes at a skill cost and the cost is higher if you are a full vampire. The skill cost is calculated the same way as staking/biting in that it will deduct an action count from you and this will be spread over multiple skills.

A few interceptions in these classes are: Vampires cannot eat and their bury action is a ritual that takes 1-2 minutes. It's not that vampires are that superstitious, but it was added to make vampires more likely to leave bloodless husks around. Which means people will be able to track them. A creative vampire will still get around this by bringing a cart or a wagon or simply put up with the bury timer.

Stealth for the vampire is an improved tool and is outlined a bit further down.

In Vampires.java there is functionality to create and remove vampires even if they are not online.

<<7 TODO: screenshot of devamp dialog>>


Skills
------
[VampSkills VampSkillTemplate]
There are a number of spells related to vampires (and in a few cases stakers). What all skills have in common is that they get "boosted" by skilling up other skills. Pretty much how characteristics work. The skills are intentionally set to be slow.

[] Bloodlust
   Works like favor for vampires, see separate section above.

[] Dexterity
   Of use to both sides. Increases dodge and hit rate when staking
   Skilled up with ropemaking, clothtailoring, chain armour, staff, small shields, weaponless fighting, lockpicking.

[] Perception
   Of use to both vampires and slayers. Increases usefulness of locate spells.
   Skilled up with tracking, traps, channeling.

[] Anatomy
   Useful to vampires only. More efficient feeding/devour.
   Skilled up with devouring, butchering, breeding, milking, taming.
   
[] Crippling
   Useful to vampires only. Increases usefulness of the "cripple living" spell.
   Skilled up by using and alchemy.

[] Disarming
   Useful to vampires only. Increases chance to successfully disarm vampire slayers.
   Skilled up with fighting defensively.

[] Aiding
   Useful to vampires only. Improves the "aid vampire" spell.
   Skilled up with first aid.


Titles
------
[VampTitles]
It's such a simple thing, really. But dear god, the utter pain you have to go through to be able to add titles. I do say that this is a ... working hack. However, considering the time invested in this, few titles were added despite them now being really easy to add. I have lots of TODOs to add new titles for a variety of things but never got around to it.

There are normal titles and there is also functionality showcasing how to bind titles to achievements such as: "stake 10 vampires".

One thing worth noting here is: There are *no* titles awarded to Vampires since they are encouraged to stay anonymous.


Zones
-----
[VampZones]
As a GM on a server, you must create a zone called "The Coven". This is where vampires will spawn when slain, and also where Orlok (vamp boss) will spawn. You can set this to be anywhere, but you may want to put it somewhat central on the map since vampires will often go from being staked to hunting down the slayer that got them.

This zone is also part of the "becoming vampire" quest.


Stealth
-------
A vampire have their stealth modified:
[] They can cast instant stealth every N Minutes
[] Much faster
[] Undetectable by other players (regardless of range)
[] Can use a number of actions in stealth without them breaking it: bite, cripple, crown-find, devour, disarm, fly, sprint, trace, smash, assist slain

Stealth still has a high chance of breaking on pavement. But in general, plank and packed dirt are excellent: increased speed that will not break stealth.

Most actions that can be performed in stealth will give messages to the surrounding, but they will never include the actual name of the performer. Instead it will be something like "a shadowy form" or similar.


General Actions
---------------
[vamps.actions.*]

	Abort
		Vampires only. Ability to abort offspring on a pregnant creature every so many hours.
	AdminDevamp
		GMs only. Devamp a vampire for free.
	AdminVamp
		GMs only. Make a player a vampire without them having to go through the quest.
	Aid
		Vampires only. Send a small rat to another player, name is not revealed, the vampire alias is used as sender.
	AssistSlain
		Vampires only. Teleport to the location where the last vampire slaying took place, lasts for 10-15 minutes.
	Bite
		Vampires only. Punish a hunted vampire slayer or someone wielding a stake of vampire banishment.
	BuyKit
		Everyone. Talk to Vampire hunter D to buy a vampire staking kit. These kits can be purchased with either a 5 silver coin, an ancient amulet or a Crown of Friya.
	Cripple
		Vampires only. Cast "cripple living" on a hunted vampire slayer or their mount to slow them down. Long range.
	CrownFind
		Everyone. Wear Crown of Friya, then use "Find" on it to locate a two legged friend. The useage is free, but the one you
		locate will get a message saying where *you* are when you use it.
	Devamp
		Half vampires and vampires only. Talk to van Helsing to devamp.
	Devour
		Vampires only. Used to feed on a corpse. See bloodlust section.
	Disarm
		Vampires only. Used to disarm stake of vampire banishment from a vampire hunter. 
	Fly
		Vampires only. Teleport to the Coven.
	HalfVamp
		Everyone. Talk to Dhampira the Ponderer.
	HalfVampClue
		Everyone. Quest clue you got from Dhampira the Ponderer.
	Labyrinth
		GMs only. This is supposed to be a vampire spell down the line, but still TODO. Do not use in populated areas ;)
	LabyrinthRemove
		GMs only. This is supposed to be a vampire spell down the line.
	MakeSeryllStake
		Everyone. Stakes can be converted to seryll when combined with an ancient amulet. A seryll stake is undodgable.
	Mirror
		Everyone. Use on a player to determine whether they are a vampire or not.
	PolishMirror
		Everyone. If you mirrored a non-vampire, you'll need to polish the mirror. Polishing has a chance to consume the pelt.
	SacrificeAltarOfSouls
		Vampires only. Sacrifice corpses to altar of souls to use at a later stage. See Altar of Souls section.
	SacrificeOrlok
		Half Vampires only. In order to become a vampire you have to sacrifice a champion corpse to Orlok.
	Sense
		Vampires only. Use on a pregnant animal to see what offspring will look like when born.
	Smash
		Vampires only. A fast attack spell (can only be used as fight starter) that hits hard. Requires 30 dexterity.
	Sprint
		Everyone. It has a cooldown, for vampires this cooldown is reduced.
	Stake
		See separate section.
	Stealth
		See separate section
	ToplistVamps
		See separate section
	Trace
		Everyone. Use on bloodless husks to see direction and distance of whomever sucked this corpse dry.

Items
-----
[vamps.items.*]

	AltarOfSouls
		A mechanic added around bloodlust's soulfeeding. It is intended to make it easier to AFK but still not make soulfeed irrelevant.
	    Acquire or create an Altar of Souls (create with high level masonry and improve likewise, rock + clay to start). This is a luxury item, really. You don't NEED one.
	    Clear a 5x5 area in a mine (it must be free from any other items).
	    Flatten the area.
	    Place altar in the middle.
	    Sacrifice corpses at the altar.
	    Stand near the altar.
	    When soulfeed starts, provided the altar has power, you will no longer get hurt. The vampiric beast within you will feed from the corpses in the altar. WTB old corpses.
	Amulet
		Ancient amulet, used by vampires to protect their hearts from stakes. Used by slayers to buy stakers or to convert wooden stakers to seryll stakes.
	Crown
		Free locate spell, but it will also reveal your position to the one you are locating.
	HalfVampireClue
		A quest item that gives a hint where Orlok is located (currently hardcoded for Zenath, will probably make configurable)
	Mirror
		Used by slayers to identify vampires.
	Pouch
		Container holding a staker's kit
	SmallRat
		Portable food for Vampires, vampires can also send these to remote players using the "Aid vampire" spell
	Stake
		The stake of vampire banishment. See stakers/staking.
	VampireFang
		Dropped by a vampire when they get staked. Stakers should pick these up. They will be used for a repeatable quest.

Questions and dialogs
---------------------
[com.wurmonline.server.questions.*]

This is somewhat dodgy, and a TODO, they all use the same question ID for now. I have actually not checked how this influences security negatively. Feel free to inform me.
 
	Aid
		Form to submit who should be aided (with a small rat).
	DeVamp
		Warning dialog for when you de-vamp.
	HalfVampClue
		Information dialog where Orlok is located.
	PinpointHumanoid
		Used by Crown of Friya and the "Pinpoint Humanoid" spell. This class contains the bulk of the code that improves the locate spells depending on the Perception skill.
	Toplist
		Information dialog containing the toplist of vampires that bit slayers.

Priest spells
-------------
	Dispel Shadows
		Dispel any stealthed targets in a large radius around caster.
	Pinpoint Humanoid
		A much more precise locate spell that depend on the Perception skill.

<<8 TODO: screenshot of a "good" locate>>

Proxies
-------
Scattered through the codebase you'll see proxies. These are injected classes into Wurm packages to get around access restrictions.

Even shorter functionality overview
-----------------------------------
Browsing through the source tree I made some quick notes of what functionality is included in the mod:

forms and windows, enum modification, loggable prepared SQLite statements, SQLite script runner, real maze generator, skill gain simulation, favor-like skill, console commands, cooldowns, anonymous chat channel, alias generator, useage of LootTables mod, creature templates, dynamic long descriptions, locate methods, new priest spells, event dispatchers, persistent data (incl. upgrading), enclosure detection, a lot of interceptions in WU's code base, useage of virtual zones, player toplist, new items in traders, new achievements, stealth modifications, bury/eat modifications, actions that will not break stealth, multiple broadcast methods, a heap of custom skills, skillup system based on other skills, new titles, abort offspring, teleportation, simplified trade action, cripple action, skill dependant locate spells, random timed affinities from feeding, CCFP modification, wound healing, disarm, quest, sense offspring, damage spell, sprint spell, proper skill and affinity stealing, particle effects, animation and sound invocation, dodge/hit implementation, new tracking spell, craftable items, chargable items, new containers, bunch of questions/forms, dispel stealth, more sophisticated locate... and heaps more.

<<9 TODO: screenshot of source tree>>

But mostly ... it is a fully functional game element added to WU.

Enjoy!

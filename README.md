# megastrike

A Clojure App to play Alphastrike on the computer.

## Usage

### Package Install

1. Have Java 17 installed
2. Download the package file from Github for your OS
3. Make the appropriate `startup` file executable for your OS
4. Execute the file

### Manual Installation

1. Have Java 17 installed
2. Install Clojure >= 1.11 on your computer
3. Clone this repository and `cd` into the directory
4. Run `clojure -M:build uber`
5. Make the appropriate `startup` file executable
6. Execute the file

### Developer Mode
If you want to run it from source, rather than compiling, follow steps 1-3 from the Manual Installation steps, but at step 4, you should run `clojure -M:run`.

This will start up and run more slowly, but it would allow you to develop the program.

## What works

Right now (v0.6), the game allows you to simulate combat between armies of any size using units exported from Megamek via their AlphaStrike stat generator. While the game won't stop you from using them, note that *FLYING UNITS DO NOT FLY* and none of the rules for them have been implemented yet. I am currently hiding all the Aero elements and many of the conventional fighters, but VTOLs and Support Vehicles which fly are still in the lists, so you could "use" them.

Terrain and attacks work. Most special abilities are not implemented. Exceptions are listed below.

Scenario reading (from MegaMek Scenario files, .mms) works but not everything works. Things known not to work include:

1. Rotating the boards
2. Applying damage to unit
3. Anything to do with teams (it will be a free for all)
4. Minefields
5. Planetary Conditions
6. Some units may not be selected correctly (I think I've got all the Mechs working).

### Working Abilities

- CASE
- CASEII
- ENE
- JMPW#
- JMPS#
- SRM/LRM/AC Attacks not in turrets
- HT Attacks not in turrets
- MEL

## **NEW! We have an AI** Kevin
This is Kevin. We found him at a bar on Donegal with a PPC in each hand (the drink, not the weapon) and a frankly astounding bar tab. We still don't know why the bartender didn't cut him off once the tab hit 5 digits. Anyway, it turns out he's a down-on-his-luck Mercenary (is there another kind?) and in exchange for paying down his bar tab and free access to the company liquor cabinet, Kevin has agreed to play the OpFor for us.

Now, we love Kevin. He's a great guy, but he isn't exactly a master tactician. But that said, he's a way to play the game without needing to play both sides of the table, so here he is!

### How to Play with Kevin
In the Lobby Screen, set the Player for any (or all) of the forces to "Kevin". Kevin will then control those foreces.

For the Movement Phase, you will need to click "Next Turn" if Kevin is the last to move so that you have time to review the position of the units before advancing to the Combat Phase.

For the Combat Phase, you will need to click "Finish Attacks" and "Next Turn" if Kevin is the last to attack so that you have time to review the results of the attacks before advancing to the End Phase.

### What Kevin can do
Kevin can move and shoot. 

His movement algorithm is... Very rough. He'll probably generally move roughly in the direction of the enemy and/or cover. Most of the time. Also, sometimes he walks in place, which isn't getting flagged correctly as "standing still". That is a bug that's top of the list to patch.

His targeting algorithm is a bit better, but not by much. He only makes regular attacks, no physical attacks or special attacks. He also will sometimes take impossible shots (i.e. ones who TN is Infinity because terrain is blocking the shot).

### What Kevin can't do

#### Kevin can't deploy
You will need to place Kevin's units for him.

#### Kevin can't turn
I'm working on an alogorithm to determine which direction most of the enemies are and turn him to face them, but I'm struggling to wrap my head around the math for that (I think I need to add together some vectors or something? I don't know).

#### Kevin's movement is bad
Like really bad. A big part of why I'm rushing this release is to get better information movement. If you see Kevin doing something particularly dumb, please take a screenshot of it and and where you think he should have moved. I'm going to try to fix Kevin's algorithm for the next release in addition to the UI improvements.

#### Kevin's shot choice is random
Literally. He just picks a random shot. This needs improving.

#### Kevin can't advance the turn
If you're playing Kevin vs. Kevin, you will still need to click "Next Phase" and "Finish Attacks" for him.

## How to Play


**SUPER Quickstart**
On the lobby screen, click "Load Test Game". It will load in a scenario with you against Kevin. Now click "Launch Game" and you're playing.

### Lobby Screen

When you first launch the game, it will launch into a lobby screen. In this lobby screen, you will see buttons to add BattleForces and maps on the left and a space for a unit list on the right. Once you add a Battle Force via the pop-up menu, you can add units to that force by double-clicking it in the list and then clicking the "Add Unit" button below the list.

**Quickstart**
If you want to test it out right away, click the "Load Scenario" button, select a scenario from the folder. Then click each force in the list of forces and click the "Add/Edit Force" button, give them a Camo (You'll know you've done it when the mech sprites change color) and, if you want to play against the AI, assigned Kevin as the player for one of them and click Launch game.

#### Adding Forces

Forces need a name, a deployment zone (Same options as Megamek, N, NE, E, SE, S, SW, W, NW, EDG, CTR), a player, and a camo. Note that the deployment zone is not enforced as of 0.2.

**NOTE: Behavior is undefined when using more than two forces, it should work, but I make no promises. Teams are not implemented at all.**

Once you have added a force, you can select it by clicking on its name. If you click it and then click "Add/Edit Force", you will see that force's information in the popup. If you need to change their camo, you can click on the force, change the camo, and then add it again to change the info. 

#### Adding Units

To add a unit, you need to have a force selected by clicking it in the Forces list. Click the "Add new unit to selected force" button. Now you can search for the unit you want and select them in the list. Enter the pilot data and click OK button. You should see the unit appear in the Army Unit List window to the bottom left. You can add as many units as you want. Currently, there is no way to remove a unit added by mistake, be sure to click carefully.

#### Setting Up the Map

Finally, initialize the map by entering number of boards wide by high you want the map to be. This will populate a grid of buttons. Click each button and select a board to fill that space.

#### Ready to Play

Once you have completed all of these steps, click the "Launch Game" button.

### The Game Screen
#### Zooming the map

In addition to the zoom in and zoom out buttons, the keyboard shortcuts for + and - also zoom the map.

#### Initiative Phase

Initiative is rolled automatically. In this phase, simply press "Next Phase".

#### Deployment Phase

**NOTE**: Kevin does not know how to deploy units. You will need to deploy his units for him.

To deploy a unit, click on the unit in the list on the right, then click on the hex you want to deploy in. Right now, you can deploy anywhere, you will have to manually enforce deployment zones. When you are happy with that unit's deployment, click "Deploy unit". If you want to deploy a different unit, click "Undeploy" **before** you select the next unit. When everyone has deployed, the "Next phase" will become active. 

Deploying units later in the game is not yet supported yet.

#### Movement Phase

To move a unit, select the unit you want to move and then the button for the movement type you want (Stand Still, Walk, or Jump). The walk button is also the button for tracked, wheeled, etc. If a unit only has a single move type, click the "walk" button to move. Then click a hex. You will see a black line indicating which hexes the unit will pass through and the cost to move there.

If you want to turn the unit, click "Turn" and then click anywhere on the map. The unit will turn to face where you clicked. If you have already clicked the hex you want to move to, then the turn will be calculated based on your destination.

When you're ready to move that unit, click "Move Unit". If a unit is standing still, click them, click "Stand Still", and then click "Move Unit". When everyone has moved, click "Next Phase".

##### Charging
If you want to charge or DFA a target, move next to them (selecting the Jump movement mode, for DFA), and then click them. It will pop up a window to select your attack mode. Then, follow the instructions in the Combat Phase.

#### Combat Phase

##### Resolving Charges
If you declared a charge during the movement phase, use the "Resolve Charges/DFAs" buttons to resolve those attacks. Units will not be marked "done" with their turn until you click this button, so I recommend clicking it first. Then go ahead with the rest of the attacks. The attacks will not be resolved unless you click this button.

##### Other attacks
To declare an attack, select the unit on the right and then select their target. This will pop up a window where you can select what kind of attack. A `:regular` attack uses the default damage line. `:physical` attacks are calculated per the physical attack rules. The other attacks use their relevant special abilities lines. To see the effects of the attack, click on the targeted unit in the unit view on the right sidebar. You will be able to see the effects at the bottom.

If you do not have line of sight to the target, the To Hit number will be infinity. You will see detailed calculation details for the attack in the round report.

The attack menu does not filter out invalid attacks yet.

#### End Phase

Destroyed units will be removed automatically at the end of the end phase, movement-modes will be reset, and heat effects will be applied. Click "Next Phase" to start the next round.

#### Next round

Initiative for the next round is rolled automatically. Simply click "Next Phase" twice (until you reach the movement phase) and play on. Play continues until one side is destroyed. When you are finished, click "Quit Game" to exit.

# Project History

A brief overview of the project

## 0.1.0

This was the first "functional" release. It was a very primitive implementation of the rules, allowing a hot-seat 1 on 1 game. The map was a featureless plain. Facing was not calculated. Critical hits were not applied. Only standard attack types worked. Basically, you could build two forces, and then have them move, shoot, and lose armor/structure until everyone died.

## 0.2.0

This release focuses on adding support for Scenarios, terrain, and movement. The scenario files that ship with MegaMek provide a super convenient "test bank" to ensure that everything is working. Assuming every scenario can load and run, everything is working. Therefore, I have prioritized getting scenarios working.

The next step is to make the terrain on the map actually mean something and make the map easier to read. It affects movement, but does not yet affect combat

## 0.3.0

This release focuses on the combat phase. It adds LOS, terrain affecting To-hit rolls, and critical hits. Still missing are abilities, physicals, and rear attacks.

## 0.4.0

This release focuses on round reports and on physical attacks.

## 0.5.0

This release focuses on some slight GUI reworks and on Special Physical Attacks. 

## 0.5.1 Previous Release

While working on cleanup and bugfixes, I realized how to do attack abilities (and how to set up all future abilities), so implemented that independently.

## 0.6.0 This release

This release adds a VERY simple AI to the game. See the AI notes above.

# Roadmap

Next steps

## Next Release

This release will overhaul the UI and make it harder for users to "freeze" the game by misclicking. It will also address any issues with Kevin crashing the game. It will not (yet) attempt to make Kevin smarter.

## R+2

This will be either an expansion of Computer Players or an initial attempt at network play.

# How to Contribute

How you can help depends on how much you know Clojure.

## I'm a keeper of the Braces, I wield them like the lightning bolts of Zeus.

Awesome! Your help is definitely welcome! This is the largest project I've ever written in any language and help would be appreciated. We can chat about specifics but areas I know I need help with are:

1. GUI redesign. cljfx is challenging. I've made a lot of progress, but there are some frustrating bugs (which I'll be logging as issues in Github) that I could use some help with.
2. AI. Eventually, I want to have a bot you can play against. I know a tiny bit about how to implement that, but I'd welcome help.
3. Network. I want this to eventually be like MegaMek, able to be played over the network... I realize this will require a client-server setup and likely some extensive rewriting (though I've tried to separate things out as best I can to make that easier), but that's all I know.

## I don't know Clojure, but I've heard of Java

Fantastic, Clojure runs on the JVM, so if you can write one of those items above in Java, I can learn how to hook them into Clojure. Testers are also welcome, as this is a large project!

## Clojure? Never heard of it. But I do know my Battletech!

Playtesting is greatly appreciated. Submit issues to Github and I'll do my best to address them! Especially places where I've got the rules wrong. I'm writing this specifically because I have nobody to play Alphastrike with, so my understanding of the rules is largely theoretical.

# Credits

Most of the files found in the data folder are used with permission from Megamek [Megamek](https://github.com/MegaMek/megamek).

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

_MechWarrior, BattleMech, ‘Mech and AeroTech are registered trademarks of The Topps Company, Inc. Original BattleTech material Copyright by Catalyst Game Labs All Rights Reserved. Used without permission._

## License

Copyright © 2023 GPL3

# megastrike

A Clojure App to play Alphastrike on the computer.

## Usage

1. Have Java 17 installed
2. Make the appropriate `startup` file executable for your OS
3. Execute the file

## What works

Right now (v0.5), the game allows you to simulate combat between armies of any size using units exported from Megamek via their AlphaStrike stat generator. While the game won't stop you from using them, note that *FLYING UNITS DO NOT FLY* and none of the rules for them have been implemented yet. I am currently hiding all the Aero elements and many of the conventional fighters, but VTOLs and Support Vehicles which fly are still in the lists, so you could "use" them.

Terrain and attacks work. Attacks from special abilities do not work (other than the MEL special ability).

Scenario reading (from MegaMek Scenario files, .mms) works but not everything works. Things known not to work include:

1. Rotating the boards
2. Applying damage to unit
3. Anything to do with teams (it will be a free for all)
4. Minefields
5. Planetary Conditions
6. Some units may not be selected correctly (I think I've got all the Mechs working).

## How to Play

### Lobby Screen

When you first launch the game, it will launch into a lobby screen. In this lobby screen, you will see 4 window spaces. These windows are the Unit Selection window, the Force Creation window, the Unit list window, and the Map setup window.

**Quickstart**
If you want to test it out right away, click the "Load Scenario" button, select a scenario from the folder. Then click each force in the list above that button, give them a Camo (You'll know you've done it when the mech sprites change color) and click Launch game.

#### Adding Forces

Forces need a name, a deployment zone (Same options as Megamek, N, NE, E, SE, S, SW, W, NW, EDG, CTR), and a camo. Note that the deployment zone is not enforced as of 0.2.

**NOTE: Behavior is undefined when using more than two forces, it should work, but I make no promises. Teams are not implemented at all.**

Once you have added a force, you can select it by clicking on its name. When you click it, you will see the force's information appear in the boxes above. If you need to change their camo, you can click on the force, change the camo, and then add it again to change the info. 

#### Adding Units

To add a unit, you need to have a force selected and a mek selected. Enter the pilot data and click the Add Unit button. You should see the unit appear in the Army Unit List window to the bottom left. You can add as many units as you want. Currently (as of 0.3), there is no way to remove a unit added by mistake, be sure to click carefully.

#### Setting Up the Map

Finally, initialize the map by entering number of boards wide by high you want the map to be. This will populate a grid of buttons. Click each button and select a board to fill that space.

#### Ready to Play

If you have completed all of these steps, click the "Launch Game" button.

#### Zooming the map

In addition to the zoom in and zoom out buttons, the keyboard shortcuts for + and - should also zoom the map.

### Initiative Phase

Initiative is rolled automatically. In this phase, simply press "Next Phase".

### Deployment Phase

To deploy a unit, click on the unit in the list on the right, then click on the hex you want to deploy in. Right now, you can deploy anywhere, you will have to manually enforce deployment zones. When you are happy with that unit's deployment, click "Deploy unit". If you want to deploy a different unit, click "Undeploy" **before** you select the next unit. When everyone has deployed, the "Next phase" will become active. 

Deploying units later in the game is not yet supported.

### Movement Phase

To move a unit, select the unit you want to move and then the button for the movement type you want (Stand Still, Walk, or Jump). The walk button is also the button for tracked, wheeled, etc. If a unit only has a single move type, click the "walk" button to move. Then click a hex. You will see a black line indicating which hexes the unit will pass through and the cost to move there.

If you want to turn the unit, click "Turn" and then click anywhere on the map. The mech will turn to face where you clicked. If you have already clicked the hex you want to move to, then the turn will be calculated based on your destination.

When you're ready to move that unit, click "Move Unit". If a unit is standing still, click them, click "Stand Still", and then click "Move Unit". When everyone has moved, click "Next Phase".

#### Charging
If you want to charge or DFA a target, move next to them (selecting the Jump movement mode, for DFA), and then click them. It will pop up a window to select your attack mode. Then, follow the instructions in the Combat Phase.

### Combat Phase

#### Resolving Charges
Once you've charged, at the start of your attacks, click the "Resolve Charges/DFAs" buttons to resolve those attacks. Then go ahead with the rest of the attacks. The attacks will not be resolved unless you click this button.

#### Other attacks
To declare an attack, select the unit on the right and then select their target. This will pop up a window where you can select what kind of attack. A `:regular` attack uses the default damage line. `:physical` attacks are calculated per the physical attack rules. To see the effects of the attack, click on the targeted unit in the unit view on the right sidebar. You will be able to see the effects at the bottom.

If you do not have line of sight to the target, the To Hit number will be infinity. You will see detailed calculation details for the attack in the round report.

### End Phase

Destroyed units will be removed automatically at the end of the end phase, movement-modes will be reset, and heat from the engine will be applied. Click "Next Phase" to start the next round.

### Next round

Initiative for the next round is rolled automatically. Simply click "Next Phase" twice (until you reach the movement phase) and play on. Play continues until one side is destroyed. When you are finished, click "Quit Game" to exit.

## Project History

### 0.1.0

This was the first "functional" release. It was a very primitive implementation of the rules, allowing a hot-seat 1 on 1 game. The map was a featureless plain. Facing was not calculated. Critical hits were not applied. Only standard attack types worked. Basically, you could build two forces, and then have them move, shoot, and lose armor/structure until everyone died.

### 0.2.0

This release focuses on adding support for Scenarios, terrain, and movement. The scenario files that ship with MegaMek provide a super convenient "test bank" to ensure that everything is working. Assuming every scenario can load and run, everything is working. Therefore, I have prioritized getting scenarios working.

The next step is to make the terrain on the map actually mean something and make the map easier to read. It affects movement, but does not yet affect combat

### 0.3.0

This release focuses on the combat phase. It adds LOS, terrain affecting To-hit rolls, and critical hits. Still missing are abilities, physicals, and rear attacks.

### 0.4.0 Previous Release

This release focuses on round reports and on physical attacks.

### 0.5.0 This Release

This release focuses on some slight GUI reworks and on Special Physical Attacks. 

## Roadmap
### Next Release

This release will focus on cleanup and bugfixes. If possible, it will also begin to implement a UI overhaul.

### R+2

There will be an intial attemp at implementing specials.

### R+3

I'm unsure. I want this to either be networked play or Computer Players, but I'm not sure which one it will be. Depends which one I can actually do.

## How to Contribute

How you can help depends on how much you know Clojure.

### I'm a keeper of the Braces, I wield them like the lightning bolts of Zeus.

Awesome! Your help is definitely welcome! This is the largest project I've ever written in any language and help would be appreciated. We can chat about specifics but areas I know I need help with are:

1. GUI redesign. cljfx is challenging. I've made a lot of progress, but there are some frustrating bugs (which I'll be logging as issues in Github) that I could use some help with.
2. AI. Eventually, I want to have a bot you can play against. I know a tiny bit about how to implement that, but I'd welcome help.
3. Network. I want this to eventually be like MegaMek, able to be played over the network... I realize this will require a client-server setup and likely some extensive rewriting (though I've tried to separate things out as best I can to make that easier), but that's all I know.

### I don't know Clojure, but I've heard of Java

Fantastic, Clojure runs on the JVM, so if you can write one of those items above in Java, I can learn how to hook them into Clojure. Testers are also welcome, as this is a large project!

### Clojure? Never heard of it. But I do know my Battletech!

Playtesting is greatly appreciated. Submit issues to Github and I'll do my best to address them! Especially places where I've got the rules wrong. I'm writing this specifically because I have nobody to play Alphastrike with, so my understanding of the rules is largely theoretical.

# Credits

Most of the files found in the data folder are used with permission from Megamek [Megamek](https://github.com/MegaMek/megamek).

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

_MechWarrior, BattleMech, ‘Mech and AeroTech are registered trademarks of The Topps Company, Inc. Original BattleTech material Copyright by Catalyst Game Labs All Rights Reserved. Used without permission._

## License

Copyright © 2023 GPL3

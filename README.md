# megastrike

A Clojure App to play Alphastrike on the computer.

## Usage

## What works

Right now (v0.1), the game allows you to simulate combat between armies of any size using units exported from Megamek via their AlphaStrike stat generator. While the game won't stop you from using them, note that *FLYING UNITS DO NOT FLY* and none of the rules for them have been implemented yet. I am currently hiding all the Aero elements and many of the conventional fighters, but VTOLs and Support Vehicles which fly are still in the lists, so you could "use" them. The combat happens on a featureless plain.

## How to Play

### Lobby Screen

When you first launch the game, it will launch into a lobby screen. In this lobby screen, you will see 4 window spaces. These windows are the Unit Selection window, the Force Creation window, the Unit list window, and the Map setup window.

**Quickstart**
If you want to test it out right away, click the "Load Test Game" button, followed by the "Launch Game" button to load a 4v4 of AFFS vs. DCMS units adapted from the Fox and the Dragon scenario.

#### Adding Forces

Forces need a name, a deployment zone (Same options as Megamek, N, NE, E, SE, S, SW, W, NW, EDG, CTR), and a color. Note that the deployment zone is not enforced as of 0.1.

**NOTE: The game currenly only supports 2 forces in a head to head, do not add more than two forces.**

Once you have added a force, you can select it by clicking on its name. You'll need both forces before you can select one.

#### Adding Units

To add a unit, you need to have a force selected and a mek selected. Enter the pilot data and click the Add Unit button. You should see the unit appear in the Army Unit List window to the bottom left. You can add as many units as you want. Currently (as of 0.1), there is no way to remove a unit added by mistake, be sure to click carefully.

#### Setting Up the Map

Finally, initialize the map by entering the width and height of the map. Once you are happy with the map size, click "Update Map Size".

#### Ready to Play

If you have completed all of these steps, you will see the "Game Not Ready" button change to "Game Ready". Click it to begin playing.

### Initiative Phase

Initiative should roll automatically. If it does not, click "Reroll Initiative" to roll it. You can see the initiative order at the bottom of the screen above the buttons. Additionally, you can see the active force has their units at the top of the stats area to the right.

### Deployment Phase

To deploy a unit, click on the unit in the list on the right, then click on the hex you want to deploy in. Right now, you can deploy anywhere, you will have to manually enforce deployment zones. When you are happy with that unit's deployment, click "Deploy unit". If you want to deploy a different unit, click "Undeploy" **before** you select the next unit. When everyone has deployed, the "Next phase" will become active. 

Deploying units later in the game is not yet supported.

### Movement Phase

To move a unit, select the unit you want to move and then the button for the movement type you want (Stand Still, Walk, or Jump). The walk button is also the button for tracked, wheeled, etc. If a unit only has a single move type, click the "walk" button to move. Then click a hex. If you cannot reach that hex, nothing will happen. If you can reach that hex, then a ghost will appear in that hex. When you're ready to move that unit, click "Move Unit". If a unit is standing still, click them, click "Stand Still", and then click "Move Unit". When everyone has moved, click "Next Phase".

### Combat Phase

To declare an attack, select the unit on the right and then select their target. In Alpha Strike, all attacks for a side are declared and resolved simultaneously, therefore you should declare all your attacks before clicking "Resolve Attacks". Once both sides have resolved their attacks, click "Next Phase".

### End Phase

Destroyed units will be removed automatically at the end of the end phase. Click "Next Phase" to start the next round.

### Next round

Initiative for the next round should roll automatically! If it does not, click "Reroll Initiative" and "Next Phase". Play continues until one side is destroyed. When you are finished, click "Quit Game" to exit.

## What's coming

### 0.1.0 This release

This, the first "functional" release, is a very primitive implementation of the rules, allowing a hot-seat 2v2. The map is a featureless plain. Facing will not be calculated. Critical hits will not be applied. Only standard attack types will work. Basically, you will be able to build two forces, and then have them move, shoot, and lose armor/structure until everyone dies.

### 0.2.0

Bugfixes for 0.1 and simpler installation. Add support for saving and loading multiple games.

### 0.3.0

Initial implementation of terrain rules for movement (line of sight and attacks will not be implemented).

### 0.4.0

Initial implementation of terrain rules for attacks.

### 0.5.0

Implementation of physical attacks, heat, and criticals.

### 0.6.0

Cleanup and bugfixes before the 1.0 release, which will cover a subset of the rules required to play 
- Any specials not directly related to damage will not be implemented.
- Flying and aerial movement will not be implemented.

## How to Contribute

How you can help depends on how much you know Clojure.

### I'm a keeper of the Braces, I wield them like the lightning bolts of Zeus.

Awesome! Your help is definitely welcome! This is the largest project I've ever written in any language and help would be appreciated. We can chat about specifics but areas I know I need help with are:

1. GUI redesign. cljfx is challenging. I've made a lot of progress, but there are some frustrating bugs (which I'll be logging as issues in Github) that I could use some help with.
2. Movement Algorithm. Movement will probably require some variation on Astar or a heat map. I'm an amateur, Astar is really hard for me. I'd love some help implementing it.
3. AI. Eventually, I want to have a bot you can play against. I know a tiny bit about how to implement that, but I'd welcome help.
4. Network. I want this to eventually be like MegaMek, able to be played over the network... I realize this will require a client-server setup and likely some extensive rewriting (though I've tried to separate things out as best I can to make that easier), but that's all I know.

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
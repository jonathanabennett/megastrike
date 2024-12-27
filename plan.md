# Refactoring Combat Units into something more structured
Based on the contents of this video: https://ericnormand.me/speaking/you-are-in-a-maze-of-deeply-nested-maps-all-alike-talk

Right now, Combat Unit has a ton of keys in it. Additionally, my namespaces are all over the place, which is causing me some confusion as well. I want to refactor so that everything is easier to understand and also so that I have a defined API.

This ignores a number of things I will need when I add aerospace combat. This may not even be complete, as I don't have all of the game data stuff in one spot. How can I begin to combine some of these things into a few namespaces?

## Possible namespaces
### Pilot DONE
Will hold data for the pilot, currently just
- name
- skill
but will add
- abilities

### Abilities DONE
Will hold all the abilities a unit has. This is basically just a single map where each entry looks like this:

:ability {:value 1 :output-str (the original string pulled from the MUL) :other-keys-as-needed}

### Heat (depends on abilities) DONE
Needs to be here because it affects both movement and attacks.

### Movement (depends on abilities) DONE
Will hold everything related to movement.
- tmm
- current-tmm
- move-options
- current-move-options
- selected-movement-type
- location
- destination
- path
- facing

### Combat (depends on pilot, abilities, and movement)
Will hold everything related to attacks, both giving and receiving

On the receiving side, it looks like this:
- crits-taken
- size
- armor
- structure
- current-armor
- current-structure
- A changes map for what changes to apply

On the attacking side, we will have a map of attacks where each attack looks like this:
- $attack (each attack will be a map with the following structure)
  - s
  - s*
  - m
  - m*
  - l
  - l*
  - e
  - e*
  - name (same value as $attack)

Then an attack-data map which contains the relevant data to resolve the current attack.
- attack-data (the map containing the relevant data to resolve an attack (this will probably get refactored))

### Game Data (depends on all of the above)
This may just live at the top-level of combat-unit.

Will hold everything else, basically
- type
- role
- force
- point-value

# Adding bars to unit displays.
I want to add a health bar and a heat bar to the unit displays. I need to decide where/how they go
Probably damage on the right and heat to the left.

# How do I handle charges and DFAs?
## Easy Mode
Move next to the target, charge/dfa button activates (based on conditions), click and handle using dialog
## Hard Mode
Click the target, move is calculated up to adjacent to the target and the dialog pops up, handle the dialog

# Fixing attacks
This fix didn't work for specials. Here are the lists of possible attacks I need to handle:
1) Regular attacks
2) Melee attacks
3) Charge/DFA attacks
4) Ability attacks
5) Indirect attacks
6) Artillery attacks

Currently, 1-3 work. I next need to add 4. But the way I have the code written, I do not have an easy way to print and roll the attacks.

## Issues
First, I cannot print or roll attacks from specials, they depend on having the whole unit map.
Second, I have melee and charge attacks blended together, they should be separated (using airity? Or using different methods).

## Options
I could move all attacks of any sort into an "attacks" map. Putting them in the map would make processing attacks much easier (including WEAP hits) but makes processing abilities harder.
this is probably fine, though I need to plan it out before I do it.

### Step 1
Create an attacks map that looks like this:
```clojure
{:regular {:s 3 :s* false :m 1 :m* false :l 0 :l* true}, 
 :physical {:s 3}, 
 :charge {:s 5 :self 2}, 
 :dfa {:s 4 :self 3}, 
 :ht {:s 1 :s* true :m 0 :m* false :l 0 :l* false}}
```

### Step 2
Modify the unit card to list all attacks in the correct order (regular attacks, physical attacks, special attacks)

### Step 3
Modify the attack-dialog so that it generates confirmation choices from the attacks map (eliminating impossible options)

### Step 4
Edit attack-dialog so that it preserves which attack is selected.

### Step 5
Edit make-attack as needed

### Step 6
Edit take-weapons crit so it loops through all values and decrements each one once.

# Server Split
## Client State vs. Game State
I will begin by separating out `game-state` from `client-state`

- Client state :: Anything used ONLY by the client (currently selected unit, layout size, etc)
- Game State :: Anything that needs to be viewed by both players (current-phase, unit stats, turn-order, etc)

Eventually, the `game-state` atom will be managed 100% by server inputs whereas the `client-state` atom will be managed 100% by CLJFX events.
## Rewrite Event Handler
Next, I will rewrite the event handler to split these two states using the suggestion [here](https://www.perplexity.ai/search/i-have-a-clojure-project-at-ht-spbldjhaTuKNWAorOBk_Ng). This split will allow me to interact with and update the two independently.

To begin with, I will just have the `game-server` function return a new version of the `game-state` atom and just merge them together.

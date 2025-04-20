# Driving updates via events
In order to reduce the amount of data I have to test with my unit tests, I want to transition away from passing full
units around. Unit maps are too big and cumbersome and they make reading the tests hard (particularly spotting the
specific changes that should be happening).

This is most painful with attacks, where my current data structure passes around 2 complete copies of each unit for
every attack. A reasonably formatted version of this involves nearly 2000 lines of code in my testing code.

## Passing Deltas
The approach I want to go with here is an `update-unit` method that gets called as an event. This function would take
data that looks approximately like this:
```clojure
{:unit-id "Wolfhound WLF-2 #2"
 :changes {:unit/armor {:toughness/current 1 :toughness/maximum 4 :toughness/unapplied 1}}
           :unit/structure {:toughness/current 4 :toughness/maximum 4 :toughness/unapplied 2}
           :unit/crits {:crits/applied [] :crits/unapplied [:crit/engine]}}
```
Then, `update-unit` simply calls `(update-in game-state [:units unit-id] merge changes)`
We can add an `update-units` method which loops through multiple units and applies `update-unit` to it.

Here is a worked example of what attacks might have:



# Adding bars to unit displays.
I want to add a health bar and a heat bar to the unit displays. I need to decide where/how they go
Probably damage on the right and heat to the left.

# Fixing attacks
This fix didn't work for specials. Here are the lists of possible attacks I need to handle:
1) Regular attacks
2) Melee attacks
3) Charge/DFA attacks
4) Ability attacks
5) Indirect attacks
6) Artillery attacks

Currently, 1-4 work. But the way I have the code written, I do not have an easy way to print and roll the attacks.

## Issues
First, I cannot print or roll attacks from specials, they depend on having the whole unit map.
Second, I have melee and charge attacks blended together, they should be separated (using airity? Or using different
methods).

## Options
I could move all attacks of any sort into an "attacks" map. Putting them in the map would make processing attacks much
easier (including WEAP hits) but makes processing abilities harder. This is probably fine, though I need to plan it out
before I do it.

### DONE Step 1
Create an attacks map that looks like this:
```clojure
{:regular {:s 3 :s* false :m 1 :m* false :l 0 :l* true}, 
 :physical {:s 3}, 
 :charge {:s 5 :self 2}, 
 :dfa {:s 4 :self 3}, 
 :ht {:s 1 :s* true :m 0 :m* false :l 0 :l* false}}
```

### DONE Step 2
Modify the unit card to list all attacks in the correct order (regular attacks, physical attacks, special attacks)

### DONE Step 3
Modify the attack-dialog so that it generates confirmation choices from the attacks map (eliminating impossible
options)

### DONE Step 4
Edit attack-dialog so that it preserves which attack is selected.

### DONE Step 5
Edit make-attack as needed

### DONE Step 6
Edit take-weapons crit so it loops through all values and decrements each one once.

# Server Split
## Client State vs. Game State
I will begin by separating out `game-state` from `client-state`

- Client state :: Anything used ONLY by the client (currently selected unit, layout size, etc)
- Game State :: Anything that needs to be viewed by both players (current-phase, unit stats, turn-order, etc)

Eventually, the `game-state` atom will be managed 100% by server inputs whereas the `client-state` atom will be managed
100% by CLJFX events.
## Rewrite Event Handler
Next, I will rewrite the event handler to split these two states using the suggestion
[here](https://www.perplexity.ai/search/i-have-a-clojure-project-at-ht-spbldjhaTuKNWAorOBk_Ng). This split will allow
me to interact with and update the two independently.

To begin with, I will just have the `game-server` function return a new version of the `game-state` atom and just merge
them together.

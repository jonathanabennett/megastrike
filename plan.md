# Adding bars to unit displays.
I want to add a health bar and a heat bar to the unit displays. I need to decide where/how they go
Probably damage on the right and heat to the left.

# How do I handle charges and DFAs?
## Easy Mode
Move next to the target, charge/dfa button activates (based on conditions), click and handle using dialog
## Hard Mode
Click the target, move is calculated up to adjacent to the target and the dialog pops up, handle the dialog

# Fixing attacks
We are producing attack rolls. Instead of grabbing the Key from that data, grab the VALUE and set that into the :attack value for the unit. Then, we can just use what we already generated to make the attack.

## Step 1
Edit Create the attacks/\*confirmation-choice\* methods so they preserve targeting data

## Step 2
Edit attack-dialog so that it parses the targeting data from confirmation choices and displays it correctly.

## Step 3
Edit attack-dialog so that it sets :selected to a targeting data instead of what it currently does

## Step 4
Edit make-attack event so expects a targeting data.

## Step 5
Edit make-attack fn so it uses existing targeting data.

# Server Split
## Client State vs. Game State
I will begin by separating out `game-state` from `client-state`

- Client state :: Anything used ONLY by the client (currently selected unit, layout size, etc)
- Game State :: Anything that needs to be viewed by both players (current-phase, unit stats, turn-order, etc)

Eventually, the `game-state` atom will be managed 100% by server inputs whereas the `client-state` atom will be managed 100% by CLJFX events.
## Rewrite Event Handler
Next, I will rewrite the event handler to split these two states using the suggestion [here](https://www.perplexity.ai/search/i-have-a-clojure-project-at-ht-spbldjhaTuKNWAorOBk_Ng). This split will allow me to interact with and update the two independently.

To begin with, I will just have the `game-server` function return a new version of the `game-state` atom and just merge them together.

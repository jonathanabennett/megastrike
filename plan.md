# Server Split
<<<<<<< Updated upstream

## Authentication Process

Process to connect to a server.

1. Server is running already
2. Client connects to server, ws connection is saved with `nil` as the player
3. Client sends its player-id to the server
4. If the player-id doesn't clash with an existing player, create the battle-force
5. If the player-id does clash, change it to `observer` and give them no battle-force

## Client State vs. Game State

I will begin by separating out `game-state` from `client-state`

- Client state :: Anything used ONLY by the client (currently selected unit,
  layout size, etc)
- Game State :: Anything that needs to be viewed by both players
  (current-phase, unit stats, turn-order, etc)

Eventually, the `game-state` atom will be managed 100% by server inputs whereas
the `client-state` atom will be managed 100% by CLJFX events.

## Rewrite Event Handler

Next, I will rewrite the event handler to split these two states using
[this suggestion](https://www.perplexity.ai/search/i-have-a-clojure-project-at-ht-spbldjhaTuKNWAorOBk_Ng).
This split will allow me to interact with and update the two independently.

To begin with, I will just have the `game-server` function return a new version
of the `game-state` atom and just merge them together.

## Splitting Events between Client and Server

I need to create a separate server namespace so that server events can be separated from client events since they will probably share a number of event names.

### List of all events

::hex-clicked - Currently merged

:default - Client
::no-op - Client
::server-update-received - Client
::close-attack-selection - Client
::show-confirmation - Client
::on-confirmation-dialog-hidden - Client
::close-dialog - Client
::set-attack - Client
::text-input - Client
::change-size - Client
::auto-save - Client
::quit-game - Client
::open-round-dialog - Client
::close-round-dialog - Client
::stats-clicked - Client
::unit-clicked - Client
::set-movement-mode - Client
::cancel-move - Client
::undeploy-unit - Client

::connect-server - Client/Server
::close-server - Client/Server
::message-server - Client/Server

::turn-button-clicked - Server
::deploy-unit - Server
::confirm-move - Server
::next-phase - Server
::finish-attacks - Server
::make-attack - Server
::resolve-physicals - Server

### Client Events

Client events can update anything inside `:gui` and `:lobby` freely.

#### Handling LOS

The Client must handle the conversion from relative to absolute hexes. The server won't know what the client's layout is.

### Server Events

Server events can update everything, including the `:gui`.
=======
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

## Convert Events listed below
### DONE Convert load-scenario for client/server :focus:coding:
:PROPERTIES:
:EFFORT:   1:00
:END:
Currently converting this into a select/create flow. Next step will be to create a connect/join flow, then people can join scenarios.
### load-mapboard
### select-camo (should select a local image and then push that image up to the server)
### filter-changed (does this actually need to be client/server? I think not)
### launch-game
### load-save
### change-player
### add-force
### mul-selection-changed (probably also needs to be client-side only)
### add-unit
### filter-mul (also probably client-side only)
### force-selection-changed (probably client-side only)
### unit-selection-changed (probably client-side only)
### host-game (not fully working yet)
### connection-established (I think this works, but confirm)
### show-confirmation (client-side only, but I need a way to trigger it from server-side)
### on-confirmation-dialog-hidden (client-side only, but I need a way for it to message to server-side)
### close-dialog (client-side only)
### hex-clicked (client-side only, but make sure there's no logic here that should be server-side)
### text-input (client-side only)
### change-size (client-side only)
### auto-save (client-side only)
### quit-game (needs a way to terminate server connections cleanly)
### open-round-dialog (client-side only)
### close-round-dialog (client-side only)
### stats-clicked (client-side only, but make sure there's no logic here that should be server-side)
### unit-clicked (client-side only, but make sure there's no logic here that should be server-side)
### undeploy-unit (client-side only, but make sure there's a catch to prevent undeploying a unit which is now deployed)
### turn-button-clicked
### set-movement-mode (client-side only)
### cancel-move (client-side only, but make sure there's a catch to prevent canceling a move that has already happened)
### set-attack
### close-attack-selection (client-side with a server-side hook)
### connect-server
### close-server
### message-server
### server-message
### next-phase
### deploy-unit
### confirm-move
### finish-attacks
### make-attack
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
>>>>>>> Stashed changes

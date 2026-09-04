# Server Split

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

### Complexity Ranking Scale (1 - 5)
- **1 - Trivial / Client-Side Only:** Local UI state updates (filtering, view scaling, dialog toggling, text input) or existing client-side network lifecycle wrappers. No server state logic required.
- **2 - Low Complexity:** Simple dispatch messages or basic client-server signaling (e.g. toggles, client join/quit, dialog callbacks, undeploy reset) with minimal validation.
- **3 - Medium Complexity:** Server-side state mutation with validation and broadcast (e.g. adding forces/units, loading map boards, camo syncing, attack targeting queue).
- **4 - High Complexity:** Turn and phase management logic (e.g. launching game, phase transitions, movement path validation with terrain and MP costs, turn order progression).
- **5 - Very High Complexity:** Authoritative combat calculation, RNG, and distributed state resolution (e.g. weapon attack resolution with modifier calculations and critical hits, physical attack resolution with pushbacks and fall checks).

### Conversion Complexity Summary

| Event / Function | Location in `src/megastrike/gui/` | Scope | Rank (1-5) |
|---|---|---|---|
| `filter-changed` | `events.clj:80`, `lobby/events.clj:56` | Client | 1 |
| `mul-selection-changed` | `events.clj:129`, `lobby/events.clj:101` | Client | 1 |
| `filter-mul` | `events.clj:147`, `lobby/events.clj:118` | Client | 1 |
| `force-selection-changed` | `events.clj:152`, `lobby/events.clj:123` | Client | 1 |
| `unit-selection-changed` | `events.clj:161`, `lobby/events.clj:132` | Client | 1 |
| `connection-established` | `events.clj:172` | Client | 1 |
| `show-confirmation` | `events.clj:188` | Client | 1 |
| `on-confirmation-dialog-hidden` | `events.clj:192` | Client | 1 |
| `close-dialog` | `events.clj:202` | Client | 1 |
| `text-input` | `events.clj:217` | Client | 1 |
| `change-size` | `events.clj:221` | Client | 1 |
| `open-round-dialog` | `events.clj:242` | Client | 1 |
| `close-round-dialog` | `events.clj:249` | Client | 1 |
| `stats-clicked` | `events.clj:255` | Client | 1 |
| `turn-button-clicked` | `events.clj:277` | Client | 1 |
| `cancel-move` | `events.clj:286` | Client | 1 |
| `connect-server` | `events.clj:320` | Client | 1 |
| `close-server` | `events.clj:324` | Client | 1 |
| `message-server` | `events.clj:328` | Client | 1 |
| `server-message` | `events.clj:333` | Client | 1 |
| `load-scenario` | `events.clj:57`, `lobby/events.clj:32` | Client/Server | 2 |
| `change-player` | `events.clj:110`, `lobby/events.clj:83` | Client/Server | 2 |
| `host-game` | `events.clj:167` | Client/Server | 2 |
| `auto-save` | `events.clj:230` | Server | 2 |
| `quit-game` | `events.clj:235` | Client/Server | 2 |
| `unit-clicked` | `events.clj:260` | Client | 2 |
| `undeploy-unit` | `events.clj:269` | Server | 2 |
| `set-movement-mode` | `events.clj:281` | Client | 2 |
| `close-attack-selection` | `events.clj:300` | Client | 2 |
| `load-mapboard` | `events.clj:70`, `lobby/events.clj:46` | Client/Server | 3 |
| `select-camo` | `events.clj:47`, `lobby/events.clj:23` | Client/Server | 3 |
| `load-save` | `events.clj:103`, `lobby/events.clj:77` | Server | 3 |
| `add-force` | `events.clj:115`, `lobby/events.clj:87` | Server | 3 |
| `add-unit` | `events.clj:134`, `lobby/events.clj:105` | Server | 3 |
| `set-attack` | `events.clj:292` | Server | 3 |
| `deploy-unit` | `events.clj:348` | Server | 3 |
| `finish-attacks` | `events.clj:365` | Server | 3 |
| `launch-game` | `events.clj:85`, `lobby/events.clj:60` | Server | 4 |
| `hex-clicked` | `events.clj:207` | Client/Server | 4 |
| `next-phase` | `events.clj:339` | Server | 4 |
| `confirm-move` | `events.clj:358` | Server | 4 |
| `make-attack` | `events.clj:372` | Server | 5 |
| `resolve-physicals` | `events.clj:378` | Server | 5 |

---

### DONE Convert load-scenario for client/server :focus:coding:
- **Complexity Rank:** 2 / 5
- **Scope:** Client / Server
- **Location:** `src/megastrike/gui/events.clj:57`, `src/megastrike/gui/lobby/events.clj:32`
- **Details:** Client opens scenario file and dispatches `::server/load-scenario` message. Server handler already exists in `src/megastrike/server/server.clj:38`. Next step is completing the connect/join flow so other clients receive updated state on connect.

### load-mapboard
- **Complexity Rank:** 3 / 5
- **Scope:** Client / Server
- **Location:** `src/megastrike/gui/events.clj:70`, `src/megastrike/gui/lobby/events.clj:46`
- **Details:** Client selects local board file; server needs a handler to accept board definition/layout dimensions, generate authoritative board structure, and broadcast the mapboard layout to all connected clients.

### select-camo (should select a local image and then push that image up to the server)
- **Complexity Rank:** 3 / 5
- **Scope:** Client / Server
- **Location:** `src/megastrike/gui/events.clj:47`, `src/megastrike/gui/lobby/events.clj:23`
- **Details:** Client selects a local image file; camo image data or identifier must be sent to the server and synchronized so opposing players can render matching unit sprites.

### filter-changed (does this actually need to be client/server? I think not)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:80`, `src/megastrike/gui/lobby/events.clj:56`
- **Details:** Filters the local Master Unit List view. Purely client-side UI operation with no server interaction needed.

### launch-game
- **Complexity Rank:** 4 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:85`, `src/megastrike/gui/lobby/events.clj:60`
- **Details:** Host requests game launch; server constructs authoritative game board, initializes turn order/initiative, transitions phase from `:lobby` to `:deployment`, and broadcasts updated game state to transition all clients to game view.

### load-save
- **Complexity Rank:** 3 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:103`, `src/megastrike/gui/lobby/events.clj:77`
- **Details:** Server reads saved EDN game state file, maps existing forces to connected client player IDs, and broadcasts the restored `@game-state`.

### change-player
- **Complexity Rank:** 2 / 5
- **Scope:** Client / Server
- **Location:** `src/megastrike/gui/events.clj:110`, `src/megastrike/gui/lobby/events.clj:83`
- **Details:** Client selects player type in force creation dialog; server validates player-to-force assignment against active websocket channels.

### add-force
- **Complexity Rank:** 3 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:115`, `src/megastrike/gui/lobby/events.clj:87`
- **Details:** Client sends force creation request (name, deployment zone, camo, player); server validates uniqueness, adds force to `@game-state :forces`, and broadcasts to lobby.

### mul-selection-changed (probably also needs to be client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:129`, `src/megastrike/gui/lobby/events.clj:101`
- **Details:** Updates active MUL selection in client's local context. No server communication needed.

### add-unit
- **Complexity Rank:** 3 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:134`, `src/megastrike/gui/lobby/events.clj:105`
- **Details:** Client submits unit configuration (MUL template, pilot name, skill, target force); server creates `combat-unit`, appends to `@game-state :units`, and broadcasts updated state.

### filter-mul (also probably client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:147`, `src/megastrike/gui/lobby/events.clj:118`
- **Details:** Filters local MUL list by search term. Pure client-side UI logic.

### force-selection-changed (probably client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:152`, `src/megastrike/gui/lobby/events.clj:123`
- **Details:** Local selection state for active force in lobby tables. Pure client-side UI logic.

### unit-selection-changed (probably client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:161`, `src/megastrike/gui/lobby/events.clj:132`
- **Details:** Updates active unit ID in local `:gui` context. Pure client-side UI logic.

### host-game (not fully working yet)
- **Complexity Rank:** 2 / 5
- **Scope:** Client / Server
- **Location:** `src/megastrike/gui/events.clj:167`
- **Details:** Starts http-kit server on port 8080 via `server/launch-server` and establishes local websocket connection for the host.

### connection-established (I think this works, but confirm)
- **Complexity Rank:** 1 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:172`
- **Details:** Stores connection in client state and sends initial `{:action :join-server :player-id ...}` message. Already implemented.

### show-confirmation (client-side only, but I need a way to trigger it from server-side)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:188`
- **Details:** Sets `:showing true` for target dialog ID in `:gui :dialogs`. Can be triggered locally or upon receiving a server notification.

### on-confirmation-dialog-hidden (client-side only, but I need a way for it to message to server-side)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:192`
- **Details:** Closes confirmation dialog and dispatches the configured `on-confirmed` action (which may send a server message).

### close-dialog (client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:202`
- **Details:** Closes dialog by setting `:showing false` in client GUI context.

### hex-clicked (client-side only, but make sure there's no logic here that should be server-side)
- **Complexity Rank:** 4 / 5
- **Scope:** Client / Server
- **Location:** `src/megastrike/gui/events.clj:207`
- **Details:** Client handles screen pixel-to-hex conversion and local path planning preview. Authoritative deployment coordinates, facing changes, and final movement path execution must be validated and executed on the server.

### text-input (client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:217`
- **Details:** Updates form fields in client context at specified path keys.

### change-size (client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:221`
- **Details:** Adjusts local board zoom/scale factor in `:gui :layout`.

### auto-save (client-side only)
- **Complexity Rank:** 2 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:230`
- **Details:** Move to server or trigger server command so the authoritative `@game-state` is serialized to disk via `scenario/edn-scenario-writer`.

### quit-game (needs a way to terminate server connections cleanly)
- **Complexity Rank:** 2 / 5
- **Scope:** Client / Server
- **Location:** `src/megastrike/gui/events.clj:235`
- **Details:** Client closes websocket; server `on-close` handler cleanly unregisters channel from `@channels`, clears player bindings, and shuts down server if no clients remain.

### open-round-dialog (client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:242`
- **Details:** Displays round/turn summary dialog locally.

### close-round-dialog (client-side only)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:249`
- **Details:** Closes round summary dialog locally.

### stats-clicked (client-side only, but make sure there's no logic here that should be server-side)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:255`
- **Details:** Switches displayed unit stats card in local `:gui` context via `turn-manager/switch-unit`.

### unit-clicked (client-side only, but make sure there's no logic here that should be server-side)
- **Complexity Rank:** 2 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:260`
- **Details:** Local selection/targeting of units in GUI; validate that the selected unit belongs to the active force or is a legal target in the current phase.

### undeploy-unit (client-side only, but make sure there's a catch to prevent undeploying a unit which is now deployed)
- **Complexity Rank:** 2 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:269`
- **Details:** Client requests unit undeployment; server checks that current phase is `:deployment` and unit has not committed (`:unit/acted?` is false), clears location, and broadcasts state.

### turn-button-clicked
- **Complexity Rank:** 1 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:277`
- **Details:** Toggles `:turn-flag` for interactive facing rotation in client UI.

### set-movement-mode (client-side only)
- **Complexity Rank:** 2 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:281`
- **Details:** Client sets desired movement mode (walk/run/jump) for the active unit, sent along with the path upon movement confirmation.

### cancel-move (client-side only, but make sure there's a catch to prevent canceling a move that has already happened)
- **Complexity Rank:** 1 / 5
- **Scope:** Client-only
- **Location:** `src/megastrike/gui/events.clj:286`
- **Details:** Clears local unconfirmed path planning for the active unit.

### set-attack
- **Complexity Rank:** 3 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:292`
- **Details:** Records planned special/physical attack targeting; server validates range, arc, and unit capabilities, then queues attack for resolution.

### close-attack-selection (client-side with a server-side hook)
- **Complexity Rank:** 2 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:300`
- **Details:** Closes attack selection dialog and dispatches either `::set-attack` (for charge/DFA) or `::make-attack` to the server.

### connect-server
- **Complexity Rank:** 1 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:320`
- **Details:** Opens websocket connection to server address/port. Already implemented via `client/create-connection`.

### close-server
- **Complexity Rank:** 1 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:324`
- **Details:** Closes active websocket connection cleanly.

### message-server
- **Complexity Rank:** 1 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:328`
- **Details:** Utility helper for sending EDN message payload over websocket to server.

### server-message
- **Complexity Rank:** 1 / 5
- **Scope:** Client
- **Location:** `src/megastrike/gui/events.clj:333`
- **Details:** Client debug logger for received server messages.

### next-phase
- **Complexity Rank:** 4 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:339`
- **Details:** Authoritative phase engine on server: advances phases (`:deployment` -> `:movement` -> `:weapon-combat` -> `:physical-combat` -> `:end-phase`), generates new initiative/turn-orders, rolls heat dissipation, and broadcasts updated state.

### deploy-unit
- **Complexity Rank:** 3 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:348`
- **Details:** Server validates player ownership, legal deployment hex zone, and turn order; sets unit location, marks `:unit/acted? true`, advances turn order, and broadcasts state.

### confirm-move
- **Complexity Rank:** 4 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:358`
- **Details:** Server validates movement path legality against terrain and available MP, sets final position and facing, updates TMM, applies movement heat, marks unit acted, advances turn order, and broadcasts state.

### finish-attacks
- **Complexity Rank:** 3 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:365`
- **Details:** Server checks all attacks for the phase are declared/completed, advances combat turn order or transitions to physical/end phase.

### make-attack
- **Complexity Rank:** 5 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:372`
- **Details:** Server calculates authoritative to-hit target number (gunnery skill + range bracket + attacker/target movement mods + terrain + heat penalties), performs server-side 2d6 roll, applies damage to armor/structure, rolls on critical hits table, updates combat round report, marks unit acted, and broadcasts state.

### resolve-physicals
- **Complexity Rank:** 5 / 5
- **Scope:** Server
- **Location:** `src/megastrike/gui/events.clj:378`
- **Details:** Server resolves charge, DFA, and melee attacks; calculates damage to target and attacker, resolves piloting skill checks for falls/displacement, updates combat report, and broadcasts state.
# Adding bars to unit displays.
I want to add a health bar and a heat bar to the unit displays. I need to decide where/how they go
Probably damage on the right and heat to the left.

# Test Failures
- **Test:** `megastrike.attacks-test/print-attack-roll`
- **Error:** `java.lang.Exception: Unable to resolve spec: :attack/melee-types`
- **Plan to Resolve:** Investigate why `:attack/melee-types` spec is not registered in the testing environment. Check `src/megastrike/schemas.clj` or wherever specs are defined to ensure they are loaded by the test suite before running the tests. It seems likely a missing `(require ...)` or registration call in the test setup.

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

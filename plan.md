# Server Split

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

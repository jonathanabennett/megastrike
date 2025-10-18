# Server Split

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

# Better Data Structures

Currently most of my things are stored as maps of maps, for example:

```clojure
{:archer1 {:unit/full-name "Archer 1" :rest :of :unit :data}
 :archer2 {:unit/full-name "Archer 2" :rest :of :unit :data}}
```

This is clumsy to access. I want to switch to using a vector of maps

```clojure
[{:unit/full-name "Archer 1" :unit/keyword :archer1 :rest :of :unit :data}
 {:unit/full-name "Archer 2" :unit/keyword :archer2 :rest :of :unit :data}]
```

This will make it easier to map over every unit but will require a pretty big
rewrite of my interfaces.

## Notes

This way, I look units up using the idiom `(some #(= value (:key %)) units-vec)`.

The changes I made to how attacks are processed means I need a much bigger rewrite. `unit-updates` is completely wrong and I will need a new algorithm for it. That will also probably trickle down through a number of other web pages.

Rather than using current `reduce` approach, I need to do something different.

## Step 3

Then I will look for convenience functions to add, for example I probably want
a getter function that uses that `some` idiom explained above.

# Adding bars to unit displays

I want to add a health bar and a heat bar to the unit displays. I need to
decide where/how they go Probably damage on the right and heat to the left.

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

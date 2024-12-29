(ns user
  (:require [megastrike.core :as core]
            [cljfx.dev :as dev]
            [clojure.tools.namespace.repl :refer [set-refresh-dirs]]))

(println
 "Set REPL refresh directories to "
 (set-refresh-dirs "src" "resources"))

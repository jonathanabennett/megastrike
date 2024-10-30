(ns build
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file (format "target/megastrike.jar"))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/java-command {:jvm-opts ["-Dcljfx.skip-javafx-initialization=true"]
                   :basis basis
                   :main 'megastrike.core})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :src-dirs ["src/megastrike"]
                  :jvm-opts ["-Dcljfx.skip-javafx-initialization=true"]
                  :ns-compile '[megastrike.core]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'megastrike.core}))

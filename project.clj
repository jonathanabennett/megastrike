(defproject megastrike "0.4.0"
  :description "This is an implementation of the Alpha Strike board game for the computer."
  :url ""
  :plugins [[lein-cloverage "1.2.2"]]
  :license {:name "GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [net.sekao/odoyle-rules "1.3.1"]
                 [org.clojure/core.cache "1.0.225"]
                 [com.brunobonacci/mulog "0.9.0"]
                 [cljfx "1.7.24"]
                 [clojure-csv/clojure-csv "2.0.1"]]
  :main ^:skip-aot megastrike.core
  :repl-options {:init-ns megastrike.core}
  :profiles {:uberjar {:aot :all
                       :uberjar-name "megastrike.jar"
                       :jvm-opts ["-Dcljfx.skip-javafx-initialization=true"]}
             :dev {:dependencies [[io.github.cljfx/dev "1.0.38"]]}})

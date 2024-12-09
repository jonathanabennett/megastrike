(ns megastrike.abilities
  (:require
   [clojure.string :as str]))

(defn parse-ability
  [str]
  (cond
    (str/includes? "LRM" str) {:ability/type :lrm :s 0 :m 0 :l 0}
    (str/includes? "ENE" str) {:ability/type :ene}
    (str/includes? "OMNI" str) {:ability/type :omni}
    (str/includes? "JMPS" str) {:ability/type :jmps :value 0}
    (str/includes? "ECM" str) {:ability/type :ecm}
    (str/includes? "CASE" str) {:ability/type :case}
    (str/includes? "MEL" str) {:ability/type :mel}
    (str/includes? "REAR" str) {:ability/type :rear :s 0 :m 0 :l 0}
    (str/includes? "IF" str) {:ability/type :if :value 0}
    (str/includes? "HEAT" str) {:ability/type :heat :s 0 :m 0 :l 0}))

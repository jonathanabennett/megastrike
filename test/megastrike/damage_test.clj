(ns megastrike.damage-test
  (:require
   [clojure.pprint :as pprint]
   [clojure.test :as t]
   [megastrike.combat-unit :as cu]
   [megastrike.damage :as sut]))

(def target (cu/->combat-unit (cu/get-unit "Wolfhound WLF-2")
                              {:pilot/full-name "Lieutenant Ciro Ramirez" :pilot/skill 4 :pilot/kills 0}
                              :direction/n {:hex/p 1 :hex/q 1 :hex/r -2} :1stsomersetstrikers 0))
(def armor-damaged {:unit/location #:hex{:p 1, :q 1, :r -2}, :unit/pilot #:pilot{:full-name "Lieutenant Ciro Ramirez", :skill 4, :kills 0}, :unit/base-pv 28, :unit/mul-id 3563, :unit/acted? false, :unit/path [], :unit/attacks #:attack{:regular #:attack{:s 3, :s* false, :m 3, :m* false, :l 1, :l* false, :e 0, :e* false}, :physical #:attack{:type :attack/physical, :damage 1, :self false}, :charge #:attack{:type :attack/charge, :damage 0, :self true}, :rear #:attack{:s 1, :s* false, :m 1, :m* false, :l 0, :l* false, :e 0, :e* false}}, :unit/battle-force :1stsomersetstrikers, :unit/facing :direction/n, :move/default :move/walk, :unit/abilities {:ene #:ability{:output "ENE"}, :attack/rear {:ability/output "REAR1/1/-", :s 1, :s* false, :m 1, :m* false, :l 0, :l* false}}, :unit/threshold -1, :unit/sprite "/home/jonathanb/src/megastrike/data/images/units/mechs/Wolfhound_2H.png", :unit/move-modes #:move{:walk 6}, :unit/structure #:toughness{:current 4, :maximum 4, :unapplied 0}, :unit/overheat-used 0, :unit/criticals #:crits{:taken [], :unapplied []}, :unit/id "Wolfhound WLF-2", :unit/type :type/bm, :unit/size 1, :unit/armor #:toughness{:current 2, :maximum 4, :unapplied 0}, :move/selected nil, :unit/model "WLF-2", :unit/overheat 0, :unit/chassis "Wolfhound", :unit/full-name "Wolfhound WLF-2", :unit/tmm 2, :unit/role :role/striker, :unit/current-heat 0})

(def armor-destroyed {:unit/location #:hex{:p 1, :q 1, :r -2}, :unit/pilot #:pilot{:full-name "Lieutenant Ciro Ramirez", :skill 4, :kills 0}, :unit/base-pv 28, :unit/mul-id 3563, :unit/acted? false, :unit/path [], :unit/attacks #:attack{:regular #:attack{:s 3, :s* false, :m 3, :m* false, :l 1, :l* false, :e 0, :e* false}, :physical #:attack{:type :attack/physical, :damage 1, :self false}, :charge #:attack{:type :attack/charge, :damage 0, :self true}, :rear #:attack{:s 1, :s* false, :m 1, :m* false, :l 0, :l* false, :e 0, :e* false}}, :unit/battle-force :1stsomersetstrikers, :unit/facing :direction/n, :move/default :move/walk, :unit/abilities {:ene #:ability{:output "ENE"}, :attack/rear {:ability/output "REAR1/1/-", :s 1, :s* false, :m 1, :m* false, :l 0, :l* false}}, :unit/threshold -1, :unit/sprite "/home/jonathanb/src/megastrike/data/images/units/mechs/Wolfhound_2H.png", :unit/move-modes #:move{:walk 6}, :unit/structure #:toughness{:current 4, :maximum 4, :unapplied 0}, :unit/overheat-used 0, :unit/criticals #:crits{:taken [], :unapplied []}, :unit/id "Wolfhound WLF-2", :unit/type :type/bm, :unit/size 1, :unit/armor #:toughness{:current 0, :maximum 4, :unapplied 0}, :move/selected nil, :unit/model "WLF-2", :unit/overheat 0, :unit/chassis "Wolfhound", :unit/full-name "Wolfhound WLF-2", :unit/tmm 2, :unit/role :role/striker, :unit/current-heat 0})

(def internal-damage {:unit/location #:hex{:p 1, :q 1, :r -2}, :unit/pilot #:pilot{:full-name "Lieutenant Ciro Ramirez", :skill 4, :kills 0}, :unit/base-pv 28, :unit/mul-id 3563, :unit/acted? false, :unit/path [], :unit/attacks #:attack{:regular #:attack{:s 3, :s* false, :m 3, :m* false, :l 1, :l* false, :e 0, :e* false}, :physical #:attack{:type :attack/physical, :damage 1, :self false}, :charge #:attack{:type :attack/charge, :damage 0, :self true}, :rear #:attack{:s 1, :s* false, :m 1, :m* false, :l 0, :l* false, :e 0, :e* false}}, :unit/battle-force :1stsomersetstrikers, :unit/facing :direction/n, :move/default :move/walk, :unit/abilities {:ene #:ability{:output "ENE"}, :attack/rear {:ability/output "REAR1/1/-", :s 1, :s* false, :m 1, :m* false, :l 0, :l* false}}, :unit/threshold -1, :unit/sprite "/home/jonathanb/src/megastrike/data/images/units/mechs/Wolfhound_2H.png", :unit/move-modes #:move{:walk 6}, :unit/structure #:toughness{:current 3, :maximum 4, :unapplied 0}, :unit/overheat-used 0, :unit/criticals #:crits{:taken [], :unapplied []}, :unit/id "Wolfhound WLF-2", :unit/type :type/bm, :unit/size 1, :unit/armor #:toughness{:current 0, :maximum 4, :unapplied 0}, :move/selected nil, :unit/model "WLF-2", :unit/overheat 0, :unit/chassis "Wolfhound", :unit/full-name "Wolfhound WLF-2", :unit/tmm 2, :unit/role :role/striker, :unit/current-heat 0})

(t/deftest remaining-armor
  (t/testing "valid responses"
    (t/is (= (sut/remaining-armor target) 4))
    (t/is (= (sut/remaining-armor armor-damaged) 2))
    (t/is (= (sut/remaining-armor armor-destroyed) 0))
    (t/is (= (sut/remaining-armor internal-damage) 0))))

(t/deftest remaining-structure
  (t/testing "valid responses"
    (t/is (= (sut/remaining-structure target) 4))
    (t/is (= (sut/remaining-structure armor-damaged) 4))
    (t/is (= (sut/remaining-structure armor-destroyed) 4))
    (t/is (= (sut/remaining-structure internal-damage) 3))))

(ns megastrike.schemas
  (:require
   [clojure.spec.alpha :as s]))

;; Hexagon Definitions
(s/def :hex/p int?)
(s/def :hex/q int?)
(s/def :hex/r int?)
(s/def :hex/location (s/keys :req [:hex/p :hex/q :hex/r]))

;; Unit Type definitions
(s/def :mul/bm #{:type/bm})
(s/def :mul/mechs #{:type/bm :type/im :type/pm})
(s/def :mul/vehicle #{:type/sv :type/cv})
(s/def :mul/infantry #{:type/ba :type/ci})
(s/def :mul/conventional #{:type/sv :type/cv :type/ba :type/ci})
(s/def :mul/ground-units #{:type/bm :type/im :type/pm :type/sv :type/cv :type/ba :type/ci})
(s/def :mul/aero #{:type/ss :type/ws :type/js :type/ds :type/da :type/sc :type/cf :type/af})
(s/def :mul/all #{:type/bm :type/im :type/pm :type/sv :type/cv :type/ba :type/ci :type/ss :type/ws :type/js :type/ds :type/da :type/sc :type/cf :type/af})

;; Battle Force Definitions
(s/def :unit-group/keyword keyword?)
(s/def :unit-group/name string?)
(s/def :unit-group/deployment #{:deployment/n :deployment/ne :deployment/e :deployment/se
                                :deployment/s :deployment/sw :deployment/w :deployment/nw})
(s/def :unit-group/camo keyword?)
(s/def :unit-group/parent keyword?) ;; Used to denote parent formations or organizations
(s/def :unit-group/player keyword?)
(s/def :unit-group/battleforce (s/keys :req [:unit-group/keyword :unit-group/name :unit-group/deployment
                                             :unit-group/camo :unit-group/parent :unit-group/player]))

;; "General" definitions
(s/def :unit/id string?)
(s/def :unit/full-name string?)
(s/def :unit/chassis string?)
(s/def :unit/model string?)
(s/def :unit/mul-id int?)
(s/def :unit/threshold int?)
(s/def :unit/base-pv int?)
(s/def :unit/role #{:role/ambusher :role/brawler :role/juggernaut :role/missile-boat :role/scout :role/skirmisher
                    :role/sniper :role/striker ; Ground roles
                    :role/attack-fighter :role/dogfighter :role/fast-dogfighter :role/fire-support :role/interceptor
                    :role/transport ; Aero roles
                    :role/none :role/undetermined}) ;unknown roles
(s/def :unit/type :mul/all)
(s/def :unit/size (s/int-in 1 5))

;; Movement definitions
(s/def :movement/modes #{:move/walk :move/jump :move/h :move/n :move/s :move/t :move/v :move/w :move/wb
                         :move/wm :move/g :move/f :move/j :move/m :move/immobilized :move/stand-still})

(s/def :unit/move-modes map?)
(s/def :unit/tmm int?)
(s/def :move/selected (s/nilable keyword?))
(s/def :move/default (s/nilable keyword?))
(s/def :unit/location (s/nilable :hex/location))
(s/def :unit/path (s/nilable vector?))
(s/def :unit/facing
  #{:direction/n :direction/ne :direction/se :direction/s :direction/sw :direction/nw :direction/none})

;; Abiltiies defintions
(s/def :ability/output string?)
(s/def :ability/record (s/keys :req [:ability/output]))
(s/def :unit/abilities (s/map-of keyword? :ability/record))

;Attack/damage definitions
(s/def :attack/s nat-int?)
(s/def :attack/s* boolean?)
(s/def :attack/m nat-int?)
(s/def :attack/m* boolean?)
(s/def :attack/l nat-int?)
(s/def :attack/l* boolean?)
(s/def :attack/e nat-int?)
(s/def :attack/e* boolean?)
(s/def :attack/damage nat-int?)
(s/def :attack/self boolean?)
(s/def :attack/melee-types #{:attack/physical :attack/charge :attack/dfa})
(s/def :attack/type #{:attack/regular :attack/physical :attack/charge :attack/dfa :attack/ht :attack/rear :attack/lrm :attack/srm :attack/ac})
(s/def :attack/ranged-info
  (s/keys :req [:attack/s :attack/s*
                :attack/m :attack/m*
                :attack/l :attack/l*
                :attack/e :attack/e*]))
(s/def :attack/melee-info
  (s/keys :req [:attack/type
                :attack/damage
                :attack/self]))
(s/def :attack/record
  (s/or :record/ranged :attack/ranged-info
        :record/melee :attack/melee-info))
(s/def :unit/attacks (s/map-of :attack/type :attack/record))
(s/def :toughness/current nat-int?)
(s/def :toughness/maximum nat-int?)
(s/def :toughness/unapplied nat-int?)
(s/def :unit/armor
  (s/keys :req [:toughness/current
                :toughness/maximum
                :toughness/unapplied]))
(s/def :unit/structure
  (s/keys :req [:toughness/current
                :toughness/maximum
                :toughness/unapplied]))
(s/def :crits/type
  #{:crits/ammo :crits/engine :crits/fire-control :crits/weapon :crits/mv :crits/destroyed})
(s/def :crits/taken (s/nilable (s/coll-of :crits/type)))
(s/def :crits/unapplied (s/nilable (s/coll-of :crits/type)))
(s/def :unit/criticals
  (s/keys :req [:crits/taken
                :crits/unapplied]))

;; Heat definitions
(s/def :unit/current-heat (s/int-in 0 5))
(s/def :unit/overheat int?)
(s/def :unit/overheat-used (s/int-in 0 5))
(s/def :unit/unapplied-heat (s/int-in 0 2))

;; Pilot Definitions
(s/def :pilot/full-name string?)
(s/def :pilot/skill (s/int-in 0 9))
(s/def :pilot/kills nat-int?)
;; Combat Unit definitions
(s/def :unit/battle-force keyword?)
(s/def :unit/pilot (s/keys :req [:pilot/full-name :pilot/skill :pilot/kills]))
(s/def :unit/acted? boolean?)
(s/def :unit/mul (s/keys :req [:unit/full-name :unit/chassis :unit/model :unit/mul-id :unit/threshold
                               :unit/base-pv :unit/role :unit/type :unit/abilities :unit/move-modes :unit/tmm
                               :unit/size :unit/attacks :unit/overheat :unit/armor :unit/structure]))

(s/def :unit/combat-unit (s/merge :unit/mul
                                  (s/keys :req [:unit/id :unit/pilot :unit/battle-force :unit/acted?
                                                :move/selected :move/default :unit/location :unit/path :unit/facing
                                                :unit/current-heat :unit/overheat-used
                                                :unit/criticals])))

;; Targeting definitions
(s/def :targeting/value (s/or
                         :targeting/possible int?
                         :targeting/imposible (s/double-in :infinite? true)))
(s/def :targeting/description string?)
(s/def :targeting/modifier (s/keys :req [:targeting/value :targeting/description]))
(s/def :targeting/skill :targeting/modifier)
(s/def :targeting/fc-damage :targeting/modifier)
(s/def :targeting/amm :targeting/modifier)
(s/def :targeting/tmm :targeting/modifier)
(s/def :targeting/heat :targeting/modifier)
(s/def :targeting/los :targeting/modifier)
(s/def :targeting/woods :targeting/modifier)
(s/def :targeting/range-mod :targeting/modifier)
(s/def :targeting/attack-type :attack/type)
(s/def :targeting/attack-data
  (s/keys :req [:targeting/skill :targeting/fc-damage :targeting/amm :targeting/tmm :targeting/heat :targeting/los :targeting/woods
                :targeting/range-mod]))
(s/def :targeting/distance nat-int?)
(s/def :targeting/rear-attack? boolean?)
(s/def :targeting/damage string?)
(s/def :targeting/firing-solution
  (s/keys :req [:targeting/attacker :targeting/target :targeting/attack-type :targeting/attack-data :targeting/distance
                :targeting/rear-attack? :targeting/damage]))


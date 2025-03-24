(ns megastrike.schemas
  (:require
   [clojure.spec.alpha :as s]))

(def bm-units #{:bm})
(def mech-units #{:bm :im :pm})
(def vehicle-units #{:sv :cv})
(def infantry-units #{:ba :ci})
(def conventional-units (into #{} (concat vehicle-units infantry-units)))
(def ground-units (into #{} (concat mech-units conventional-units)))
(def aero-units #{:ss :ws :js :ds :da :sc :cf :af})
(def all-types (into #{} (concat ground-units aero-units)))

;; "General" definitions
(s/check-asserts true)
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
(s/def :unit/type #{:type/bm :type/im :type/pm :type/sv :type/cv :type/ba :type/ci
                    :type/sc :type/ss :type/ws :type/js :type/ds :type/da :type/cf :type/af})
(s/def :unit/size (s/int-in 1 5))

;; Movement definitions

(s/def :movement/modes #{:move/walk :move/jump :move/h :move/n :move/s :move/t :move/v :move/w :move/wb
                         :move/wm :move/g :move/f :move/j :move/m})

(s/def :unit/move-modes map?)
(s/def :unit/tmm int?)
(s/def :move/selected keyword?)
(s/def :move/default (s/nilable keyword?))
(s/def :unit/location (s/nilable keyword?))
(s/def :unit/path (s/nilable vector?))
(s/def :unit/facing #{:direction/n :direction/ne :direction/se :direction/s :direction/sw :direction/nw :direction/none})

;; Attack/damage definitions
(s/def :unit/attacks map?)
(s/def :toughness/current int?)
(s/def :toughness/maximum int?)
(s/def :toughness/unapplied int?)
(s/def :unit/armor (s/keys :req [:toughness/current
                                 :toughness/maximum
                                 :toughness/unapplied]))
(s/def :unit/structure (s/keys :req [:toughness/current
                                     :toughness/maximum
                                     :toughness/unapplied]))
(s/def :crits/types #{:crits/ammo :crits/engine :crits/fire-control :crits/weapon :crits/mv :crits/destroyed})
(s/def :crits/taken (s/nilable (s/coll-of :crits/types)))
(s/def :crits/unapplied (s/nilable (s/coll-of :crits/types)))
(s/def :unit/criticals (s/keys :req [:crits/taken
                                     :crits/unapplied]))

;; Heat definitions
(s/def :unit/current-heat (s/int-in 0 5))
(s/def :unit/overheat int?)
(s/def :unit/overheat-used (s/int-in 0 5))

;; Abiltiies defintions
(s/def :unit/abilities map?)

;; Combat Unit definitions
(s/def :unit/battle-force keyword?)
(s/def :unit/pilot map?)
(s/def :unit/acted? boolean?)
(s/def :unit/mul (s/keys :req [:unit/full-name :unit/chassis :unit/model :unit/mul-id :unit/threshold
                               :unit/base-pv :unit/role :unit/type :unit/abilities :unit/move-modes :unit/tmm
                               :unit/size :unit/attacks :unit/overheat :unit/armor :unit/structure]))

(s/def :unit/combat-unit (s/merge :unit/mul
                                  (s/keys :req [:unit/id :unit/pilot :unit/battle-force :unit/acted?
                                                :move/selected :move/default :unit/location :unit/path :unit/facing
                                                :unit/current-heat :unit/overheat-used
                                                :unit/criticals])))

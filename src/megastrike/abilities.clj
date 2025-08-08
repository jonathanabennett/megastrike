(ns megastrike.abilities
  "Namespace for interacting with abilities."
  (:require
   [clojure.string :as str]))

(defn has? [abilities ability]
  (ability abilities))

(defn neg-number
  [n]
  (* n -1))

(defn parse-value
  [kword ability-str default]
  (let [[_ value] (re-matches #"(\d+)" ability-str)]
    {kword {:value (if value (parse-double value) default) :ability/output ability-str}}))

(defn parse-damage [range damage-str]
  {range (if (= damage-str "-") 0 (Integer/parseInt (str/replace  damage-str "*" "")))
   (keyword (str (name range) "*")) (str/ends-with? damage-str "*")})

(defn parse-damages
  ([s m l]
   (merge {} (parse-damage :s s) (parse-damage :m m) (parse-damage :l l)))
  ([s m l e]
   (merge {} (parse-damage :s s) (parse-damage :m m) (parse-damage :l l) (parse-damage :e e))))

(defn parse-ability [ability-str]
  (cond
    (re-matches #"ECS" ability-str)
    {:ecs {:ability/output ability-str}}

    (re-matches #"QV" ability-str)
    {:qv {:ability/output ability-str}}

    (re-matches #"IRA" ability-str)
    {:ira {:ability/output ability-str}}

    (re-matches #"ABA" ability-str)
    {:aba {:ability/output ability-str}}

    (re-matches #"RHS" ability-str)
    {:rhs {:ability/output ability-str}}

    (re-matches #"RBT" ability-str)
    {:rbt {:ability/output ability-str}}

    (re-matches #"DN" ability-str)
    {:dn {:ability/output ability-str}}

    (re-matches #"ES" ability-str)
    {:es {:ability/output ability-str}}

    (re-matches #"MAS" ability-str)
    {:mas {:ability/output ability-str}}

    (re-matches #"NOVA" ability-str)
    {:nova {:ability/output ability-str}}

    (re-matches #"SHLD" ability-str)
    {:shld {:ability/output ability-str}}

    (re-matches #"ARM" ability-str)
    {:arm {:ability/output ability-str}}

    (re-matches #"I-TSM" ability-str)
    {:itsm {:ability/output ability-str}}

    (re-matches #"FF" ability-str)
    {:ff {:ability/output ability-str}}

    (re-matches #"LMAS" ability-str)
    {:lmas {:ability/output ability-str}}

    (re-matches #"LECM" ability-str)
    {:lecm {:ability/output ability-str}}

    (re-matches #"FR" ability-str)
    {:fr {:ability/output ability-str}}

    (re-matches #"MEC" ability-str)
    {:mec {:ability/output ability-str}}

    (re-matches #"XMEC" ability-str)
    {:xmec {:ability/output ability-str}}

    (re-matches #"MTN" ability-str)
    {:mtn {:ability/output ability-str}}

    (re-matches #"TSI" ability-str)
    {:tsi {:ability/output ability-str}}

    (re-matches #"PAR" ability-str)
    {:par {:ability/output ability-str}}

    (re-matches #"TRN" ability-str)
    {:trn {:ability/output ability-str}}

    (re-matches #"UMU" ability-str)
    {:umu {:ability/output ability-str}}

    (re-matches #"AM" ability-str)
    {:am {:ability/output ability-str}}

    (re-matches #"GLD" ability-str)
    {:gld {:ability/output ability-str}}

    (re-matches #"MCS" ability-str)
    {:mcs {:ability/output ability-str}}

    (re-matches #"UCS" ability-str)
    {:ucs {:ability/output ability-str}}

    (re-matches #"VSTOL" ability-str)
    {:vstol {:ability/output ability-str}}

    (re-matches #"CASEII" ability-str)
    {:caseii {:ability/output ability-str}}

    (re-matches #"CASE" ability-str)
    {:case {:ability/output ability-str}}

    (re-matches #"AECM" ability-str)
    {:aecm {:ability/output ability-str}}

    (re-matches #"ECM" ability-str)
    {:ecm {:ability/output ability-str}}

    (re-matches #"SAW" ability-str)
    {:saw {:ability/output ability-str}}

    (re-matches #"DUN" ability-str)
    {:dun {:ability/output ability-str}}

    (re-matches #"BRID" ability-str)
    {:brid {:ability/output ability-str}}

    (re-matches #"BT" ability-str)
    {:bt {:ability/output ability-str}}

    (re-matches #"MSW" ability-str)
    {:msw {:ability/output ability-str}}

    (re-matches #"DRO" ability-str)
    {:dro {:ability/output ability-str}}

    (re-matches #"WAT" ability-str)
    {:wat {:ability/output ability-str}}

    (re-matches #"BRA" ability-str)
    {:bra {:ability/output ability-str}}

    (re-matches #"CR" ability-str)
    {:cr {:ability/output ability-str}}

    (re-matches #"RAMS" ability-str)
    {:rams {:ability/output ability-str}}

    (re-matches #"RCA" ability-str)
    {:rca {:ability/output ability-str}}

    (re-matches #"REL" ability-str)
    {:rel {:ability/output ability-str}}

    (re-matches #"RFA" ability-str)
    {:rfa {:ability/output ability-str}}

    (re-matches #"SOA" ability-str)
    {:soa {:ability/output ability-str}}

    (re-matches #"SEAL" ability-str)
    {:seal {:ability/output ability-str}}

    (re-matches #"MFB" ability-str)
    {:mfb {:ability/output ability-str}}

    (re-matches #"AMP" ability-str)
    {:amp {:ability/output ability-str}}

    (re-matches #"HTC" ability-str)
    {:htc {:ability/output ability-str}}

    (re-matches #"HPG" ability-str)
    {:hpg {:ability/output ability-str}}

    (re-matches #"ENG" ability-str)
    {:eng {:ability/output ability-str}}

    (re-matches #"LG" ability-str)
    {:lg {:ability/output ability-str}}

    (re-matches #"ORO" ability-str)
    {:oro {:ability/output ability-str}}

    (re-matches #"BH" ability-str)
    {:bh {:ability/output ability-str}}

    (re-matches #"AFC" ability-str)
    {:afc {:ability/output ability-str}}

    (re-matches #"BFC" ability-str)
    {:bfc {:ability/output ability-str}}

    (re-matches #"BAR" ability-str)
    {:bar {:ability/output ability-str}}

    (re-matches #"CNARC.*?" ability-str)
    (parse-value :cnarc ability-str 1)

    (re-matches #"INARC.*?" ability-str)
    (parse-value :inarc ability-str 1)

    (re-matches #"SNARC.*?" ability-str)
    (parse-value :snarc ability-str 1)

    (re-matches #"ATMO" ability-str)
    {:atmo {:ability/output ability-str}}

    (re-matches #"EE" ability-str)
    {:ee {:ability/output ability-str}}

    (re-matches #"AMS" ability-str)
    {:ams {:ability/output ability-str}}

    (re-matches #"MEL" ability-str)
    {:mel {:ability/output ability-str}}

    (re-matches #"TSM" ability-str)
    {:tsm {:ability/output ability-str}}

    (re-matches #"TAG" ability-str)
    {:tag {:ability/output ability-str}}

    (re-matches #"STL" ability-str)
    {:stl {:ability/output ability-str}}

    (re-matches #"OMNI" ability-str)
    {:omni {:ability/output ability-str}}

    (re-matches #"LPRB" ability-str)
    {:lprb {:ability/output ability-str}}

    (re-matches #"PRB" ability-str)
    {:prb {:ability/output ability-str}}

    (re-matches #"RCN" ability-str)
    {:rcn {:ability/output ability-str}}

    (re-matches #"ARS" ability-str)
    {:ars {:ability/output ability-str}}

    (re-matches #"FC" ability-str)
    {:fc {:ability/output ability-str}}

    (re-matches #"LTAG" ability-str)
    {:ltag {:ability/output ability-str}}

    (re-matches #"ENE" ability-str)
    {:ene {:ability/output ability-str}}

    (re-matches #"OVL" ability-str)
    {:ovl {:ability/output ability-str}}

    (re-matches #"SRCH" ability-str)
    {:srch {:ability/output ability-str}}

    (re-matches #"BIM.*" ability-str)
    {:bim {:ability/output ability-str}}

    (re-matches #"LAM.*" ability-str)
    {:lam {:ability/output ability-str}}

    (re-matches #"BHJ(\d+)?" ability-str)
    (parse-value :bhj ability-str 1)

    (re-matches #"FUEL(\d+)?" ability-str)
    (parse-value :fuel ability-str 1)

    (re-matches #"SUBW(\d+)?" ability-str)
    (parse-value :subw ability-str 1)

    (re-matches #"BTAS(\d+)?" ability-str)
    (parse-value :btas ability-str 1)

    (re-matches #"CAR(\d+)?" ability-str)
    (parse-value :car ability-str 1)

    (re-matches #"MTAS(\d+)?" ability-str)
    (parse-value :mtas ability-str 1)

    (re-matches #"VTM(\d+)?" ability-str)
    (parse-value :vtm ability-str 1)

    (re-matches #"TSEMP-O(\d+)?" ability-str)
    (parse-value :tsemp-o ability-str 1)

    (re-matches #"TSEMP(\d+)?" ability-str)
    (parse-value :tsemp ability-str 1)

    (re-matches #"PNT(\d+)?" ability-str)
    (parse-value :pnt ability-str 1)

    (re-matches #"BOMB(\d+)?" ability-str)
    (parse-value :bomb ability-str 1)

    (re-matches #"C3EM(\d+)?" ability-str)
    (parse-value :c3em ability-str 1)

    (re-matches #"C3M(\d+)?" ability-str)
    (parse-value :c3m ability-str 1)

    (re-matches #"C3BSS(\d+)?" ability-str)
    (parse-value :c3bss ability-str 1)

    (re-matches #"C3BSM(\d+)?" ability-str)
    (parse-value :c3bsm ability-str 1)

    (re-matches #"C3S(\d+)?" ability-str)
    (parse-value :c3s ability-str 1)

    (re-matches #"C3I(\d+)?" ability-str)
    (parse-value :c3i ability-str 1)

    (re-matches #"MDS(\d+)?" ability-str)
    (parse-value :mds ability-str 1)

    (re-matches #"DCC(\d+)?" ability-str)
    (parse-value :dcc ability-str 1)

    (re-matches #"RSD(\d+)?" ability-str)
    (parse-value :rsd ability-str 1)

    (re-matches #"JMPW(\d+)?" ability-str)
    (-> (parse-value :jmpw ability-str 1)
        (update-in [:jmpw :value] neg-number))

    (re-matches #"JMPS(\d+)?" ability-str)
    (parse-value :jmps ability-str 1)

    (re-matches #"MHQ(\d+)?" ability-str)
    (parse-value :mhq ability-str 1)

    (re-matches #"CT(\d+(?:\.\d+)?)" ability-str)
    (parse-value :ct ability-str 1)

    (re-matches #"MASH(\d+)" ability-str)
    (parse-value :mash ability-str 1)

    (re-matches #"IT(\d+(?:\.\d+)?)" ability-str)
    (parse-value :it ability-str 1)

    (re-matches #"IF(\d+|\d+\*|-)" ability-str)
    (let [[_ value] (re-matches #"IF(\d+|\d+\*|-)" ability-str)
          damage (parse-damage :value value)
          inner-map (merge {:ability/output ability-str} damage)]
      {:if inner-map})

    (re-matches #"TOR(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
    (let [[_ s m l] (re-matches #"TOR(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
          damage (parse-damages s m l)
          inner-map (merge {:ability/output ability-str} damage)]
      {:tor inner-map})

    (re-matches #"HT(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
    (let [[_ s m l] (re-matches #"HT(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
          damage (parse-damages s m l)
          inner-map (merge {:ability/output ability-str} damage)]
      {:attack/ht inner-map})

    (re-matches #"AC(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
    (let [[_ s m l] (re-matches #"AC(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
          damage (parse-damages s m l)
          inner-map (merge {:ability/output ability-str} damage)]
      {:attack/ac inner-map})

    (re-matches #"FLK(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
    (let [[_ s m l] (re-matches #"FLK(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
          damage (parse-damages s m l)
          inner-map (merge {:ability/output ability-str} damage)]
      {:attack/flk inner-map})

    (re-matches #"LRM(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
    (let [[_ s m l] (re-matches #"LRM(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
          damage (parse-damages s m l)
          inner-map (merge {:ability/output ability-str} damage)]
      {:attack/lrm inner-map})

    (re-matches #"IATM(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)?" ability-str)
    {::type :iatm :ability/output ability-str}
    ; (let [[_ s m l] (re-matches #"ITAM(\d+|\d+\*|-)/(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)]
    ;   (merge {::type :itam :ability/output ability-str} (parse-damages s m l)))

    (re-matches #"SRM(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
    (let [[_ s m] (re-matches #"SRM(\d+|\d+\*|-)/(\d+|\d+\*|-)" ability-str)
          damage (parse-damages s m "0")
          inner-map (merge {:ability/output ability-str} damage)]
      {:attack/srm inner-map})

    (re-matches #"REAR(\d+\*?|-)/(\d+\*?|-)/(\d+\*?|-)" ability-str)
    (let [[_ s m l] (re-matches #"REAR(\d+\*?|-)/(\d+\*?|-)/(\d+\*?|-)" ability-str)
          damage (parse-damages s m l)
          inner-map (merge {:ability/output ability-str} damage)]
      {:attack/rear inner-map})

    (re-matches #"TUR\((.*?)\)" ability-str)
    {:tur {:ability/output ability-str}}
    ; (let [[_ content] (re-matches #"TUR\((.*?)\)" ability-str)
    ;       [range abilities] (str/split content #", " 2)
    ;       range-map (parse-range range)]
    ;   (if abilities
    ;     (assoc {::type :tur} :abilities (parse-ability abilities) :s (:s range-map) :m (:m range-map) :l (:l range-map) :ability/output ability-str)
    ;     (assoc {::type :tur} :s (:s range-map) :m (:m range-map) :l (:l range-map) :ability/output ability-str)))

    (re-matches #"ARTCM5-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTCM5-(\d+)" ability-str)]
      {:artcm5 {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTAIS-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTAIS-(\d+)" ability-str)]
      {:artais {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTLT-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTLT-(\d+)" ability-str)]
      {:artlt {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTLTC-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTLTC-(\d+)" ability-str)]
      {:artltc {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTAC-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTAC-(\d+)" ability-str)]
      {:artac {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTS-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTS-(\d+)" ability-str)]
      {:arts {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTSC-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTSC-(\d+)" ability-str)]
      {:artsc {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTT-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTT-(\d+)" ability-str)]
      {:artt {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTTC-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTTC-(\d+)" ability-str)]
      {:arttc {:value (Integer/parseInt value) :ability/output ability-str}})

    (re-matches #"ARTBA-(\d+)" ability-str)
    (let [[_ value] (re-matches #"ARTBA-(\d+)" ability-str)]
      {:artba {:value (Integer/parseInt value) :ability/output ability-str}})

    :else
    {:unknown {:ability/output ability-str}}))

(defn parse-abilities [input]
  (let [regex #",\s*(?![^()]*\))"
        abilities (->> (str/split input regex)
                       (map str/trim)
                       (mapv parse-ability))]
    (apply merge abilities)))

(defn print-ability
  [abilities ability]
  (str (or (:ability/output (has? abilities ability)) "None") ", "))

(defn print-abilities
  [abilities]
  (apply str (map #(print-ability abilities %) (keys abilities))))

(ns megastrike.schemas)

(def Hexagon
  [:and [:map [:p number?] [:q number?] [:r number?]]
   [:fn #(= (:r %) (- (+ (:p %) (:q %))))]])

(def Attack-Roll
  [:map
   [:targeting vector?]
   [:flag keyword?]
   [:rear-attack? boolean?]
   [:damage int?]])

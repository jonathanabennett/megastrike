(ns megastrike.schemas)

(def Hexagon
  [:and [:map [:p number?] [:q number?] [:r number?]]
   [:fn #(= (:r %) (- (+ (:p %) (:q %))))]])


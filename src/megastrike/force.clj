(ns megastrike.force)

(defn create-force
  ([name color deploy initiative]
   {:name name :color color :deploy deploy :initiative initiative})
  ([name color deploy]
   (create-force name color deploy 0))
  ([name color]
   (create-force name color :any 0)))

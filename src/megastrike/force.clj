(ns megastrike.force
  (:require
   [megastrike.utils :as utils]))

(defn ->force
  [fname deployment camo team player]
  {:name fname :deployment deployment :camo camo :team team :player player})

(defn get-name
  [{:keys [name]}]
  name)

(defn set-name
  [force new-name]
  (assoc force :name new-name))

(defn get-deployment
  [{:keys [deployment]}]
  deployment)

(defn set-deployment
  [force deployment]
  (assoc force :deployment deployment))

(defn get-camo
  [{:keys [camo]}]
  camo)

(defn set-camo
  [force new-camo]
  (assoc force :camo new-camo))

(defn get-team
  [{:keys [team]}]
  team)

(defn get-player
  [{:keys [player]}]
  player)

(defn set-team
  [force new-team]
  (assoc force :team new-team))

(defn same-team?
  [{:keys [team]} other]
  (= team (get-team other)))

(ns megastrike.force
  (:require
   [megastrike.utils :as utils]))

(defn ->force
  [fname deployment color camo team]
  {:name fname :deployment deployment :color color :camo camo :team team})

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

(defn get-color
  [{:keys [color]}]
  color)

(defn set-color
  [force new-color]
  (assoc force :color new-color))

(defn get-camo
  [{:keys [camo]}]
  camo)

(defn set-camo
  [force new-camo]
  (assoc force :camo new-camo))

(defn get-team
  [{:keys [team]}]
  team)

(defn set-team
  [force new-team]
  (assoc force :team new-team))

(defn same-team?
  [{:keys [team]} other]
  (= team (get-team other)))


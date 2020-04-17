(ns chlorine-koans.clippy
  (:require [promesa.core :as p]
            [chlorine-koans.ui.atom :as atom]))

(defonce clippy (atom nil))

(defn consume [service]
  (reset! clippy service))

(defn- prompt-clippy [message choices]
  (let [p (p/deferred)
        choices (->> choices
                     (map (fn [c] [c #(p/resolve! p c)]))
                     (into {}))]
    (.speak @clippy message (clj->js {:prompt choices}))
    p))

(defn prompt [message & choices]
  (if @clippy
    (prompt-clippy message choices)
    (atom/prompt {:title message
                  :arguments (->> choices
                                  (juxt identity identity)
                                  (into {}))})))

(defn notify [message]
  (if @clippy
    (.speak @clippy message)
    (atom/info message nil)))

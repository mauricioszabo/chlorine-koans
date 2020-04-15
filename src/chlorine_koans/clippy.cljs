(ns chlorine-koans.clippy
  (:require [promesa.core :as p]))

(defonce clippy (atom nil))

(defn consume [service]
  (reset! clippy service))

(defn prompt [message & choices]
  (let [p (p/deferred)
        choices (->> choices
                     (map (fn [c] [c #(p/resolve! p c)]))
                     (into {}))]
    (.speak @clippy message (clj->js {:prompt choices}))
    p))

; @clippy
;
; (.speak @clippy "Hello, world", #js {:prompt #js {:yes #(prn :LOL)}})

(ns chlorine-koans.koans.cljs-koans
  (:require [koans.meditations :as meditations]
            [chlorine-koans.unify :as u]
            [clojure.walk :as walk]
            [meander.epsilon :as e]))

(def state {:current-meditation 0
            :current-koan 0})

(defn make-unification [code]
 (walk/prewalk-replace {:__ '?code} code))

(defn to-koan [[message assert]]
  {:message (str ";; " message "\n;; " assert "\n\n" assert)
   :assert (make-unification assert)})

(defn meditate [{:keys [name koans fns]}]
  (->> koans
       (partition 2 2)))
       ; (map to-koan)))

#_
(meditate (first meditations/categories))

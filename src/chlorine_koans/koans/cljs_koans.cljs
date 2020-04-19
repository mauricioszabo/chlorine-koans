(ns chlorine-koans.koans.cljs-koans
  (:require [koans.meditations :as meditations]
            [chlorine-koans.ui.atom :as atom]
            [chlorine-koans.clippy :as clippy]
            [chlorine-koans.unify :as u]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [promesa.core :as p]
            [chlorine-koans.koans.cljs :as cljs]))

(defonce state (atom {:current-meditation 0
                      :current-koan 0}))

#_
(reset! state {:current-meditation 6 :current-koan 0})

(defn- make-unification [code]
 (walk/prewalk-replace {:__ '?code} code))

(defn- to-koan [[message assert]]
  {:message (str ";; " message "\n;; " assert "\n\n" assert)
   :assert (make-unification assert)})

(defn- prepare-meditations [{:keys [koans fns]}]
  (->> koans
       (partition 2 2)
       (mapv to-koan)))

(declare meditate-over!)
(defn- score-meditation [meditations {:keys [assert]} {:keys [res code]}]
  (prn :RES res)
  (prn :code code)
  (prn :assert assert)
  (prn :UNIFY (u/unify code assert))
  (when-let [u (and (= res true)
                    (u/unify code assert))]
    (clippy/notify (str "You have achived enlightenment with code\n\""
                        (get u '?code) "\""))
    (p/do!
     (p/delay 300)
     (swap! state update :current-meditation inc)
     (meditate-over! meditations))))

(defn- meditate-over! [meditations]
  (let [meditation (nth meditations (:current-meditation @state))]
    (atom/append-on-editor {:contents (str "\n\n" (:message meditation))})
    (cljs/handle-result! #(score-meditation meditations meditation %))))

(defn start! []
  (let [curr-koan (nth meditations/categories (:current-koan @state))
        name (:name curr-koan)
        meditations (prepare-meditations curr-koan)
        file-name (str "src/koans/" name ".cljs")]
    (p/let [editor (atom/open-editor {:file-name file-name})]
      (when (-> ^js editor .getText str/trim empty?)
        (atom/append-on-editor {:contents (str ";; " (str/capitalize name))}))
      (meditate-over! meditations))))

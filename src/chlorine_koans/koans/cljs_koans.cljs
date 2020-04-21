(ns chlorine-koans.koans.cljs-koans
  (:require [koans.meditations :as meditations]
            [chlorine-koans.ui.atom :as atom]
            [chlorine-koans.clippy :as clippy]
            [chlorine-koans.unify :as u]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [promesa.core :as p]
            [chlorine-koans.koans.cljs :as cljs]
            [clojure.test :refer [deftest is run-tests]]))

(defonce state (atom {:current-meditation 0
                      :current-koan 0
                      :meditations []}))

#_
(swap! state merge {:current-meditation 7 :current-koan 0})

(defn- make-unification [code]
  (walk/prewalk #(cond
                   (= % '(:__)) (gensym "?code")
                   (= % :__) (gensym "?code")
                   :else  %)
                code))

(deftest unification-test
  (is (= '(= (foo ?code))
         (make-unification '(= (foo :__)))))

  (is (= '(= ?code)
         (make-unification '(= (:__))))))

(defn- to-koan [[message assert]]
  {:message (str ";; " message "\n;; " assert "\n\n" assert)
   :assert (make-unification assert)})

(defn- prepare-meditations [{:keys [koans fns]}]
  (->> koans
       (partition 2 2)
       (mapv to-koan)))

(defn gotos [{:keys [code]}]
  (let [koan-code (u/unify '(goto-koan ?koan) code)
        meditation-code (u/unify '(goto-exercise ?ex) code)]
    (prn :KOAN koan-code)
    (prn :EX meditation-code)))

(defn- enlightenmnent! []
  (cljs/handle-result! gotos)
  (atom/append-on-editor {:contents "\n\n;; You have archived enlightenment!"}))

(declare meditate-over! start!)
(defn- new-koan! []
  (if (-> meditations/categories count dec (> (:current-koan @state)))
    (swap! state #(-> % (update :current-koan inc) (assoc :current-meditation 0)))
    (swap! state assoc :current-koan :end))
  (start!))

(defn- score-meditation [{:keys [assert]} {:keys [res code]}]
  (prn :assert assert)
  (prn :code code)
  (prn :U (u/unify code assert))
  (let [u (and (= res true) (u/unify code assert))
        codes (when u (->> u (sort-by first) (map second) not-empty))]
    (when (and codes (not (some #{:__} codes)))
      (clippy/notify (str "You have achived enlightenment with code(s)\n"
                          (->> codes (map #(str "\"" % "\"")) (str/join ","))))

      (p/do!
       (p/delay 300)
       (if (-> @state :meditations count dec (> (:current-meditation @state)))
         (do
           (swap! state update :current-meditation inc)
           (meditate-over!))
         (new-koan!))))))

(defn- meditate-over! []
  (let [meditation (nth (:meditations @state) (:current-meditation @state))]
    (atom/append-on-editor {:contents (str "\n\n" (:message meditation))})
    (cljs/handle-result! #(score-meditation meditation %))))

(defn start! []
  (if (-> @state :current-koan (= :end))
    (enlightenmnent!)
    (let [curr-koan (nth meditations/categories (:current-koan @state))
          name (:name curr-koan)
          file-name (str "src/koans/" name ".cljs")]
      (swap! state assoc :meditations (prepare-meditations curr-koan))
      (p/let [editor (atom/open-editor {:file-name file-name})]
        (when (-> ^js editor .getText str/trim empty?)
          (atom/append-on-editor {:contents (str ";; " (str/capitalize name))}))
        (meditate-over!)))))

(ns chlorine-koans.koans.intro
  (:require [chlorine-koans.ui.atom :as atom]
            [chlorine-koans.clippy :refer [prompt notify]]
            [chlorine-koans.koans.cljs :as cljs]
            [promesa.core :as p]))

(def txt "; Welcome to Koans
; This guided tutorial uses ClojureScript to guide you to enlightenment
; Let's look at the first steps:
;
; You already can run ClojureScript processes. Position your cursor on
; the block below, and hit CTRL+Enter
(+ 1 2)")

(def cont "; There's a difference between evaluations
; Put your cursor inside the (+ 2 3) and hit CTRL+SHIFT+ENTER
; and you'll evaluate the \"inner block\".
; Or, you can hit CTRL+ENTER and evaluate the whole expression
;
; So, for now, try to evaluate inner block...
(* 10 (+ 2 3))")

(defn- check-cont2 [{:keys [res]}]
  (case res
    5 (notify "You're evaluating the INNER block. For now, evaluate TOP block.")
    50 (do
         (notify "You're done! Congrats! Now, play a little more with more commands!")
         (cljs/handle-result! identity))
    (notify "You're evaluating some other thing....")))

(defn- check-cont [{:keys [res]}]
  (case res
    5 (do
        (notify "Okay, you evaluated the inner block, which is 5. Now, try to evaluate the full block")
        (cljs/handle-result! check-cont2))
    50 (notify "You're evaluating the top block. For now, evaluate the inner block.")
    (notify "You're evaluating some other thing....")))

(defn- check-eval [{:keys [res]}]
  (if (= 3 res)
    (do
      (notify "Great job! Now you know how to evaluate!")
      (cljs/handle-result! check-cont)
      (atom/open-editor {:file-name "src/intro.cljs"
                         :contents cont
                         :line 6
                         :column 9}))
    (notify "I think you're evaluating the wrong thing...")))

(defn koan! []
  (p/let [editor (atom/open-editor {:file-name "src/intro.cljs"
                                    :contents txt
                                    :line 1})]
    (cljs/handle-result! check-eval)))

(ns chlorine-koans.core
  (:require [chlorine-koans.aux :refer [subscriptions] :as aux]
            [chlorine-koans.koans.cljs :as cljs]
            [chlorine-koans.ui.inline :as inline]
            [chlorine-koans.ui.atom :as atom]
            [chlorine-koans.koans.intro :as intro]
            ["atom" :refer [CompositeDisposable]]))

(defonce atom-state (atom nil))

(defn- info! [message]
  (.. js/atom -notifications (addInfo message)))

(defn activate [state]
  (reset! aux/plugin-dir js/__dirname)
  (reset! atom-state state)
  (cljs/connect!)
  (.add @aux/subscriptions (.. js/atom -commands
                               (add "atom-text-editor"
                                    "chlorine-koans:clear-inline-results"
                                    #(inline/clear-results! (atom/current-editor)))))
  (.add @aux/subscriptions (.. js/atom -commands
                               (add "atom-text-editor"
                                    "chlorine-koans:introduction"
                                    intro/koan!))))

(defn deactivate [state]
  (.dispose ^js @subscriptions))

(defn ^:dev/before-load reset-subs []
  (deactivate @atom-state))

(defn ^:dev/after-load re-activate []
  (reset! subscriptions (CompositeDisposable.))
  (activate @atom-state)
  (info! "Reloaded plug-in"))

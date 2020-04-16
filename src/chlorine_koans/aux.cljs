(ns chlorine-koans.aux
  (:require-macros [chlorine-koans.aux :as a])
  (:require ["atom" :refer [CompositeDisposable]]))

(def subscriptions (atom (CompositeDisposable.)))
(defonce plugin-dir (atom nil))

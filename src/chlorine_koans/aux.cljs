(ns chlorine-koans.aux
  (:require ["atom" :refer [CompositeDisposable]]))

(def subscriptions (atom (CompositeDisposable.)))
(defonce plugin-dir (atom nil))

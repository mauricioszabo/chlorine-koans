;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "A unification library for Clojure."
      :author "Michael Fogus"}
  chlorine-koans.unify-macro
  #?(:cljs (:require-macros [chlorine-koans.unify-macro]))
  (:require [clojure.zip :as zip]
            [clojure.walk :as walk]))

(defn ignore-variable? [sym] (= '_ sym))

(defn- bind-phase
  [binds variable expr]
  (if (or (nil? expr)
          (ignore-variable? variable))
    binds
    (assoc binds variable expr)))

(defn- determine-occursness
  [want-occurs? variable? v expr binds]
  (if want-occurs?
    `(if (~'occurs? ~variable? ~v ~expr ~binds)
       (throw (ex-info (str "Cycle found in the path " ~expr) {}))
       (bind-phase ~binds ~v ~expr))
    `(bind-phase ~binds ~v ~expr)))

(defmacro create-var-unification-fn
  [want-occurs?]
  (let [varp  (gensym)
        v     (gensym)
        expr  (gensym)
        binds (gensym)]
    `(fn ~'var-unify
       [~varp ~v ~expr ~binds]
       (if-let [vb# (~binds ~v)]
         (~'garner-unifiers ~varp vb# ~expr ~binds)
         (if-let [vexpr# (and (~varp ~expr) (~binds ~expr))]
           (~'garner-unifiers ~varp ~v vexpr# ~binds)
           ~(determine-occursness want-occurs? varp v expr binds))))))

#_
(clojure.pprint/pprint
 (macroexpand-1 '(create-var-unification-fn true)))

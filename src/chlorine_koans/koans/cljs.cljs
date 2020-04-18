(ns chlorine-koans.koans.cljs
  (:require [cljs.js :as cljs]
            [clojure.reader :as edn]
            [clojure.set :as set]
            [promesa.core :as p]
            [repl-tooling.eval :as eval]
            [shadow.cljs.bootstrap.env :as env]
            [shadow.cljs.bootstrap.browser :as boot]
            [chlorine-koans.aux :as aux]
            [chlorine-koans.ui.inline :as inline]
            [chlorine-koans.aux :as aux]
            [chlorine-koans.ui.atom :as atom]
            [repl-tooling.editor-integration.connection :as conn]
            ["path" :refer [join]]))

(defonce c-state (cljs/empty-state))
(defonce editor-state (atom nil))

(defn eval-str [source cb]
  (cljs/eval-str
    c-state
    source
    "[test]"
    {:eval cljs/js-eval
     :load (partial boot/load c-state)
     :ns   (symbol "chlorine-koans.bootstrap")}
    cb))

(defonce init-cljs!
  (delay (boot/init c-state
                    {:path (join @aux/plugin-dir "bootstrapped")
                     :load-on-init '#{chlorine-koans.bootstrap}}
                    identity)))

(def ^:private callbacks (atom {:error identity :result identity}))

(defn- edn-read [string]
  (try
    (edn/read-string {:default tagged-literal} string)
    (catch :default _ (symbol (str string)))))

(def repl
  (reify eval/Evaluator
    (evaluate [_ command opts callback]
      (let [code-edn (edn-read command)
            {:keys [namespace filename row col]} opts]
        (cljs/eval-str c-state
                       (str "(pr-str "command "\n)")
                       filename
                       {:eval cljs/js-eval
                        :source-map true
                        :load (partial boot/load c-state)
                        :ns (symbol (or namespace "chlorine-koans.bootstrap"))}
                       (fn [result]
                         (if (contains? result :value)
                           (let [v (:value result)]
                             ((:result @callbacks) {:res (edn-read v) :code code-edn})
                             (callback {:as-text v :result v}))
                           (let [v (:error result)]
                             ((:error @callbacks) {:res (edn-read v) :code code-edn})
                             (callback {:as-text v :error v})))))))
    (break [_ repl])))

(defn- notify! [{:keys [type title message]}]
  (case type
    :info (atom/info title message)
    :warn (atom/warn title message)
    (atom/error title message)))

(defn- get-config []
  {:eval-mode :cljs
   :project-paths (->> js/atom
                       .-project
                       .getDirectories
                       (map #(.getPath ^js %)))})

(defn- update-inline-result! [{:keys [range editor-data] :as result}]
  (let [editor (:editor editor-data)
        parse (-> @@editor-state :editor/features :result-for-renderer)]
    (when editor
      (inline/update-with-result editor (-> range last first) (parse result)))))

(defn connect! []
  @init-cljs!
  (p/let [state (conn/connect-evaluator!
                 {:cljs/repl repl :clj/aux repl}
                 :cljs
                 {:on-start-eval inline/create-inline-result!
                  :editor-data atom/get-editor-data
                  :notify notify!
                  :get-config get-config
                  :on-eval update-inline-result!
                  :get-rendered-results inline/all-parsed-results
                  :open-editor atom/open-editor})]
    (reset! editor-state state)
    (doseq [[k {:keys [command]}] (:editor/commands @state)]
      (.add @aux/subscriptions (.. js/atom -commands
                                   (add "atom-text-editor"
                                        (str "chlorine-koans" k)
                                        command))))))

(defn handle-result! [callback] (swap! callbacks assoc :result callback))
(defn handle-error! [callback] (swap! callbacks assoc :error callback))

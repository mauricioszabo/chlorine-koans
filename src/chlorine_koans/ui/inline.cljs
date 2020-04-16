(ns chlorine-koans.ui.inline
  (:require [reagent.dom :as dom]
            [repl-tooling.editor-integration.renderer :as render]))

; FORMAT:
; {<editor-id> {<row> {:result <marker-or-ink>
;                      :div <div>
;                      :parsed-ratom <ratom>}}}
(defonce results (atom {}))
(defonce atom-results (atom {}))

(defn get-result [editor row]
  ; TODO: Remove this, use only div maybe?
  (get-in @results [(.-id editor) row :result]))

(defn all-parsed-results []
  (for [[editor-id v] @results
        [row {:keys [parsed-ratom]}] v
        :when parsed-ratom]
    parsed-ratom))

(defn- destroyed? [^js result]
  (and (some-> result .-isDestroyed)
       (or (= true (.-isDestroyed result))
           (.isDestroyed result))))

(defn- discard-old-results! []
  (doseq [[editor-id v] @results
          [row {:keys [result div listener]}] v
          :when (and (not div) (not (some-> result .-view .-view .-isConnected)))]
    (when div (.dispose ^js listener))
    (swap! results update editor-id dissoc row)))

(defn clear-results! [^js editor]
  (doseq [{:keys [result listener]} (get @atom-results (.-id editor))]
    (.destroy ^js result)
    (.dispose ^js listener))
  (swap! atom-results assoc (.-id editor) []))

(defn- update-marker-on-result! [^js change ^js editor]
  (let [old (.. change -oldHeadBufferPosition -row)
        new (.. change -newHeadBufferPosition -row)]
    (swap! results update (.-id editor)
           #(-> % (assoc new (get % old)) (dissoc old)))))

(defn ^js new-inline-result [^js editor [[r1 c1] [r2 c2]]]
  (discard-old-results!)
  (let [marker (. editor markBufferRange
                 (clj->js [[r1 c1] [r2 c2]])
                 #js {:invalidate "inside"})
        div (doto (. js/document createElement "div")
                  (aset "classList" "chlorine-koans result-overlay")
                  (aset "innerHTML" "<div><span class='chlorine-koans icon loading'></span></div>"))
        dispose (.onDidChange marker #(update-marker-on-result! % editor))
        result (get-result editor r2)]
    ; TODO: Remove ink, this will be default
    (when (destroyed? result)
      (.destroy result)
      (.dispose (get-in @results [(.-id editor) r2 :listener])))

    (some-> result .destroy)

    (swap! results assoc-in [(.-id editor) r2]
           {:result marker :div div :listener dispose})
    (swap! atom-results update (.-id editor) conj {:result marker :listener dispose})
    (. editor decorateMarker marker #js {:type "block" :position "after" :item div})))

(defn- create-div! []
  (doto (. js/document createElement "div")
    (.. -classList (add "result" "chlorine-koans"))))

(defn- get-or-create-div! [editor row parsed-ratom]
  (let [div (or (get-in @results [(.-id editor) row :div])
                (create-div!))]
    (when (-> parsed-ratom meta :error) (.. div -classList (add "error")))
    (.. div -classList (add "result"))
    (dom/render [:div [render/view-for-result parsed-ratom]] div)
    div))

(defn update-with-result [editor row parsed-ratom]
  (when-let [inline-result ^js (get-result editor row)]
    (swap! results assoc-in [(.-id editor) row :parsed-ratom] parsed-ratom)
    (let [div (get-or-create-div! editor row parsed-ratom)]
      (when (.-setContent inline-result)
        (.setContent inline-result div #js {:error (-> parsed-ratom meta :error)})))))

(defn- create-inline-result! [{:keys [range editor-data]}]
  (when-let [editor (:editor editor-data)]
    (new-inline-result editor range)))

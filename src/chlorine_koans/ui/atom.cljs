(ns chlorine-koans.ui.atom)

(defn warn [title text]
  (.. js/atom -notifications (addWarning title #js {:detail text})))

(defn error [title text]
  (.. js/atom -notifications (addError title #js {:detail text})))

(defn info [title text]
  (.. js/atom -notifications (addInfo title #js {:detail text})))

(defn current-editor []
  (.. js/atom -workspace getActiveTextEditor))

(defn get-editor-data []
  (when-let [editor (current-editor)]
    (let [range (.getSelectedBufferRange editor)
          start (.-start range)
          end (.-end range)]
      {:editor editor
       :contents (.getText editor)
       :filename (.getPath editor)
       :range [[(.-row start) (.-column start)]
               [(.-row end) (cond-> (.-column end)
                                    (not= (.-column start) (.-column end)) dec)]]})))

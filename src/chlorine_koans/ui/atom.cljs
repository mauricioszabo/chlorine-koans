(ns chlorine-koans.ui.atom
  (:require [promesa.core :as p]))

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

(defn open-editor [{:keys [file-name line contents column]}]
  (p/let [position (clj->js (cond-> {:searchAllPanes true}
                                    line (assoc :initialLine line)
                                    column (assoc :initialColumn column)))
          editor (.. js/atom -workspace (open file-name position))]
    (when contents
      (.setText ^js editor contents)
      (.setCursorBufferPosition ^js editor #js [line (or column 0)]))
    editor))

(defn prompt [{:keys [title message arguments]}]
  (js/Promise.
   (fn [resolve]
     (let [notification (atom nil)
           buttons (->> arguments (map (fn [{:keys [key value]}]
                                         {:text value
                                          :onDidClick (fn []
                                                        (resolve key)
                                                        (.dismiss ^js @notification))})))]

       (reset! notification (.. js/atom -notifications
                                (addInfo title (clj->js {:detail message
                                                         :dismissable true
                                                         :buttons buttons}))))
       (.onDidDismiss ^js @notification #(fn [] (resolve nil) true))))))

(defn append-on-editor [{:keys [line contents column]}]
  (let [editor (current-editor)]
    (. editor setText (str (.getText editor) contents))
    (when line
      (.setCursorBufferPosition ^js editor #js [line (or column 0)]))))

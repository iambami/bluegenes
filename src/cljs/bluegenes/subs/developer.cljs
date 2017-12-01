(ns bluegenes.subs.developer
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]))

(reg-sub
  ::panel
  (fn [db]
    (get-in db [:debug-panel])))

(reg-sub
  ::tools
  (fn [db]
    (get-in db [:developer :tools])))

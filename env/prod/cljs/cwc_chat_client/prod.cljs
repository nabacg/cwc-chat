(ns cwc-chat-client.prod
  (:require [cwc-chat-client.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)

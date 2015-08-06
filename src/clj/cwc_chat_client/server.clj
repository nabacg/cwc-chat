(ns cwc-chat-client.server
  (:require [cwc-chat-client.handler :refer [app]]
            [environ.core :refer [env]]
            [immutant.web :as web])
  (:gen-class))

 (defn -main [& args]
   (let [[port-arg & rest] args
         port (Integer/parseInt (or port-arg (env :port)  "3000"))]
     (web/run app {:port port})))

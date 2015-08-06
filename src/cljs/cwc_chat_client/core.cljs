(ns cwc-chat-client.core
    (:require [reagent.core :as reagent]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljs.core.async :as async :refer [<! >! put! chan]]
              [taoensso.sente :as sente :refer [cb-success?]])
    (:require-macros [cljs.core.async.macros :as asyncm :refer [go go-loop]])
    (:import goog.History))

(def state (reagent/atom {:chat-history []}))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
       {:type :auto ; e/o #{:auto :ajax :ws}
       })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(defmulti event-msg-handler :id)
;; Wrap for logging, catching, etc.:
(defn     event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (println "Event " event)
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default [{:keys [event] :as msg}]
  (print event msg))

(defmethod event-msg-handler :chsk/state [{:keys [?data] :as msg}]
  (if (= ?data {:first-open? true})
    (println "channel socket opened!")
    (println "Channel socket state change: " ?data)))

(defmethod event-msg-handler :chsk/handshake [{:keys [?data] :as msg}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake " ?data)))

(defmethod event-msg-handler :chsk/recv [{:keys [data] :as msg}]
  (println "Push data from server " msg)
  (swap! state update-in [:chat-history] conj data)
  (println  @state))

(defmethod event-msg-handler :cwc-chat-client.handler/cwc-chat-broadcast [{:keys [user msg] :as payload}]
  (println payload)
  (swap! state update-in [:chat-history] conj msg)
  (println @state))


(def router_ (atom nil))

(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to cwc-chat-client"]
   [:div [:a {:href "#/about"} "go to about page!!"]]])

(defn about-page []
  [:div [:h2 "About cwc-chat-client"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))

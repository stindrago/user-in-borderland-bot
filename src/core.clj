(ns core
  (:require [clojure.edn :as edn]
            [telegrambot-lib.core :as tbot]
            [user :as u]))

(def mybot (tbot/create u/bot-token))

(tbot/get-me mybot)

(defn -main []
  (println "It works"))

(ns user
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def bot-token
  (->> "configs.edn"
       io/resource
       slurp
       edn/read-string
       :dev
       :bot-token))

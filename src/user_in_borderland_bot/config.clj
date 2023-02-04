(ns user-in-borderland-bot.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def filename-data (io/resource "data.edn"))
(def filename-config (io/resource "config.edn"))

(defn read-large-file
  "Read large `file' safely."
  [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (edn/read r))))

(defn write-large-file
  "Write large `data' to `file'."
  [data file]
  (with-open [w (clojure.java.io/writer file)]
    (binding [*out* w]
      (pr data))))

(defn read-small-file
  "Read small `file' safely."
  [file]
  (edn/read-string (slurp file)))

(defn load-data
  []
  (read-large-file filename-data))

(def configs (read-small-file filename-config))

(def token (-> configs
               :bot-token))

(def timeout (-> configs
                 :timeout))

(def sleep (-> configs
               :sleep))

(def games (-> configs
               :games))

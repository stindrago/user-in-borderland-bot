(ns user-in-borderland-bot.logic
  "Handle some general logic for the app"
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as s]
   [user-in-borderland-bot.config :as cfg]
   [user-in-borderland-bot.fields :as fields]
   [user-in-borderland-bot.games.core :as games.core]
   [user-in-borderland-bot.games.shared :as games.shared]))

(defn find-game
  "Parse `config.edn' and check if the string contained in `msg' is a valid. Return the game."
  [msg]
  (first (for [[k v] (:games cfg/configs)
               :when (some (:matches v)
                           (s/split (s/lower-case (fields/text msg)) #" "))]
           k)))

(defn sign-up!
  "Start the appropriate game."
  [bot msg game]
  (log/info "Running: sign-up!")
  (let [chat-id (fields/chat-id msg)
        player (fields/from-id msg)
        username (fields/from-user msg)
        group-title (fields/group-title msg)]
    (games.shared/player-started-playing player chat-id group-title game)
    ((games.core/game-list game) bot msg)))

(defn chose-action
  "Point to the appropriate action handler."
  [bot msg]
  (let [players @games.shared/players
        player (fields/from-id msg)
        game-played (get-in players [:players player :playing :game])]
    ((games.core/game-actions game-played) bot msg)))

(ns user-in-borderland-bot.games.shared
  "Shared fn within the `games' namespace."
  (:require [user-in-borderland-bot.config :as cfg]))

(def players (atom (cfg/load-data)))

(defn save-data-to-file
  "Write @players state to disk."
  []
  (let [data @players
        file cfg/filename-data]
    (cfg/write-large-file data file)))

(defn player-stopped-playing
  "Player cleared the game."
  [player]
  (swap! players assoc-in [:players player :playing] {})
  (save-data-to-file))

(defn player-started-playing
  "Player registered to a game."
  [player chat-id title game]
  (swap! players assoc-in [:players player :playing] {:id chat-id :title title :game game})
  (when (nil? (get-in @players [:groups chat-id]))
    (swap! players assoc-in [:groups chat-id] {:title title :game game}))
  (save-data-to-file))

(defn game-over
  "Remove the cards from the player's deck."
  [player]
  (swap! players assoc-in [:players player :cards] #{})
  (save-data-to-file))

(defn player-wins
  "Add the won card to the player's deck."
  [player]
  (let [card (get-in @players [:players player :playing :game])
        deck (get-in @players [:players player :cards])]
    (swap! players assoc-in [:players player :cards] (conj deck card)))
  (save-data-to-file))

(defn game-cleared
  "Remove the group from the database."
  [group]
  (swap! players update :groups dissoc group)
  (save-data-to-file))

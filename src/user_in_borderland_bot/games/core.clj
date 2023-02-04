(ns user-in-borderland-bot.games.core
  "List of games and actions."
  (:require [user-in-borderland-bot.games.h2 :as games.h2]))

(def game-list {:h2 (fn [bot msg] (future (games.h2/init bot msg)))})

(def game-actions {:h2 (fn [bot msg] (games.h2/actions bot msg))})

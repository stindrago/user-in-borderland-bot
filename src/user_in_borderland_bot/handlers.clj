(ns user-in-borderland-bot.handlers
  (:require
   [clojure.tools.logging :as log]
   [telegrambot-lib.core :as tbot]))

(defn send-msg-mdv2
  "Send a message to the chat in MardownV2."
  [bot chat-id text]
  (try
    (tbot/send-message (assoc bot :async false) {:chat_id chat-id :text text :parse_mode "MarkdownV2"})
    (catch Exception e
      (log/error "tbot/send-message exception:" e))))

(defn send-msg-md
  "Send a message to the chat in Mardown."
  [bot chat-id text]
  (try
    (tbot/send-message (assoc bot :async false) {:chat_id chat-id :text text :parse_mode "Markdown"})
    (catch Exception e
      (log/error "tbot/send-message exception:" e))))

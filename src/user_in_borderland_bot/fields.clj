(ns user-in-borderland-bot.fields
  "Functions that retrieve message fields.")

(defn chat-id
  "Returns the chat-id from a message."
  [msg]
  (-> msg
      :message
      :chat
      :id))

(defn chat-type
  "Returns the chat-type from a message."
  [msg]
  (-> msg
      :message
      :chat
      :type))

(defn chat-results
  "Get results from the chat updates map.
   Returns a list of maps."
  [msg]
  (-> msg
      :result))

(defn group-title
  "Returns the chat-type from a message."
  [msg]
  (-> msg
      :message
      :chat
      :title))

(defn from-id
  "Returns the username of the user that the message is from."
  [msg]
  (-> msg
      :message
      :from
      :id))

(defn from-user
  "Returns the username of the user that the message is from."
  [msg]
  (-> msg
      :message
      :from
      :username))

(defn text
  "Retrieve the text part of the message."
  [msg]
  (-> msg
      :message
      :text))

(defn update-id
  "Returns the update_id part of the message."
  [msg]
  (-> msg
      :update_id))

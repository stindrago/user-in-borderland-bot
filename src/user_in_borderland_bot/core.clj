(ns user-in-borderland-bot.core
  (:require
   [clojure.pprint :as p]
   [clojure.tools.logging :as log]
   [telegrambot-lib.core :as tbot]
   [user-in-borderland-bot.commands :as commands]
   [user-in-borderland-bot.config :as cfg])
  (:gen-class))

(def bot (tbot/create cfg/token))

(defonce update-id (atom nil))

(defn poll-updates
  "Long poll for recent chat messages from Telegram."
  ([bot]
   (poll-updates bot nil))

  ([bot offset]
   (let [resp (tbot/get-updates bot {:offset offset :timeout cfg/timeout})]
     (if (contains? resp :error)
       (log/error "tbot/get-updates error:" (:error resp)) resp))))

(defn set-id!
  "Sets the update `id' to process next as the the passed in `id'."
  [id]
  (reset! update-id id))

(defn app
  "Retrieve and process chat messages."
  [bot]
  (log/info "user-in-borderland-bot service started.")
  (loop []
    (log/debug "checking for chat updates.")
    (let [updates (poll-updates bot @update-id)
          messages (:result updates)]
      ;; Check all messages, if any, for commands/keywords.
      (doseq [msg messages]
        (p/pprint msg)
        (commands/used-command bot msg)
        ;; your functions goes here
        ;; Increment the next update-id to process.
        (-> msg
            :update_id
            inc
            set-id!))

      ;; Wait a while before checking for updates again.
      (Thread/sleep cfg/sleep))
    (recur)))

(defn shutdown-app
  "Shutdown the service cleanly."
  []
  (shutdown-agents)
  (log/info "user-in-borderland-bot service exited."))

(defn -main
  "Create the Telegram bot and run the application."
  []
  (app bot))

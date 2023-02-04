(ns user-in-borderland-bot.games.h2
  "Two of hearts. The toxic wagon."
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as s]
   [java-time.api :as jt]
   [user-in-borderland-bot.fields :as fields]
   [user-in-borderland-bot.games.shared :as games.shared]
   [user-in-borderland-bot.handlers :as handlers]))

(def game (atom {:players {}
                 :train {:wagons 2
                         :toxic-wagon 1
                         :doors-closed? true}
                 :stages {:game-started? false
                          :train-departed? false
                          :train-arrived? false}}))

(defn print-art
  "Print the ASCII art of a train with the number of wagons being based on the number of players."
  [players]
  (str "\n```"
       "\n       ("
       "\n   '( '"
       "\n  \"'  //))"
       "\n ( ''\""
       (apply str
              (let [a (vec
                       (map clojure.string/join
                            (partition-all 4 (take players
                                                   (for [i (range players)]
                                                     (if (zero? i)
                                                       (str " _||__ " "____ ")
                                                       (if (zero? (mod i 4))
                                                         (str "       " "____ ")
                                                         (str "____ "))))))))
                    b (vec (map clojure.string/join
                                (partition-all 4 (take players (for [i (range players)]
                                                                 (if (zero? i)
                                                                   (str "(o)___)" "}" (inc i) "__}")
                                                                   (if (< i 9)
                                                                     (if (zero? (mod i 4))
                                                                       (str "       " "}" (inc i) "__}")
                                                                       (str "}" (inc i) "__}"))
                                                                     (if (zero? (mod i 4))
                                                                       (str "       " "}" (inc i) "_}")
                                                                       (str "}" (inc i) "_}")))))))))
                    c (vec (map clojure.string/join
                                (partition-all 4 (take players
                                                       (for [i (range players)]
                                                         (if (zero? i)
                                                           (str "'U'0 0 " " 0 0 ")
                                                           (if (zero? (mod i 4))
                                                             (str "       " " 0 0 ")
                                                             (str " 0 0 "))))))))]
                (for [x (range (count a))]
                  (str  "\n" (get a x) "\n"
                        (get b x) "\n"
                        (get c x)))))
       "\n```"))

(defn show-rules
  "Print the rules."
  [bot chat-id players]
  (handlers/send-msg-md bot chat-id (str "*📢 Regole del gioco 2️⃣♥️*"
                                         "\n"
                                         (print-art (+ players 2))
                                         "\n\nSei nel ultimo di un treno lungo *" (+ players 2) " vagoni*."
                                         "\nIl vagone in cui ti trovi é sicuro."
                                         "\nPer completare il gioco dovrai raggiungere la locomotiva prima che il treno arrivi a destinazione."
                                         "\nLa locomotiva è sicura."
                                         "\nUno dei prossimi *" (inc players) " vagoni* contiene un gas tossico che ti ucciderà se non indossi la maschera anti-gas."
                                         "\nHai a disposizione *" players " cartucce* usa e getta per la maschera che ti forniscono un minuto di ossigeno."
                                         "\nÈ _GAME CLEARED_ quando il treno arriva a destinazione."
                                         "\nÈ _GAME OVER_ quando respiri il gas tossico o non hai raggiunto la locomotiva quando il treno sarà arrivato a destinazione."
                                         "\n\nAzioni possibili"
                                         "\n  `/do get on board`, sali sul treno."
                                         "\n  `/do next`, spostati nel prossimo vagone *non indossando* la maschera."
                                         "\n  `/do next mask`, spostati nel prossimo vagone *indossando* la maschera."
                                         ;; "\n  `/do wear mask`, *indossa* la maschera se ti sei scordato."
                                         "\n  `/do help`, mostra questo messaggio di aiuto."
                                         "\n")))

(defn add-player!
  "Add the player that joined the game."
  [player]
  (swap! game assoc :players (conj (:players @game) {player {:wagon nil :moved nil :ammo nil :game-over? false}}))
  (swap! game assoc-in [:train :wagons] (inc (get-in @game [:train :wagons]))))

(defn game-start
  "Perform the actions to start the game."
  [bot chat-id timestamp countdown]
  (let [elapsed-time (jt/time-between (jt/local-date-time) (jt/plus timestamp (jt/minutes countdown)) :seconds)
        elapsed-time-map (jt/convert-amount elapsed-time :seconds :minutes)]
    (if (> elapsed-time 0)
      (do (handlers/send-msg-md bot chat-id (str "📢 Sono rimasti *" (:whole elapsed-time-map)
                                                 " minuti* e *" (:remainder elapsed-time-map)
                                                 " secondi* al inizio della partita."))
          (Thread/sleep 20000)
          (recur bot chat-id timestamp countdown))
      (do (handlers/send-msg-md bot chat-id (str "📢 I treni sono in partenza, tutti i passeggeri sono pregati di salire ogni uno in un treno separato."
                                                 "\n\nUsa `/do get on board` per salire sul treno."))
          (swap! game assoc-in [:stages :game-started?] true)))))

(defn train-departure
  "Perform the action for the train teparture."
  [bot chat-id timestamp countdown]
  (log/info "train-departure")
  (let [elapsed-time (jt/time-between (jt/local-date-time) (jt/plus timestamp (jt/minutes countdown)) :seconds)
        elapsed-time-map (jt/convert-amount elapsed-time :seconds :minutes)]
    (if (> elapsed-time 0)
      (do (handlers/send-msg-md bot chat-id (str "📢 Sono rimasti *" (:whole elapsed-time-map)
                                                 " minuti* e *" (:remainder elapsed-time-map)
                                                 " secondi* alla partenza del treno."))
          (Thread/sleep 20000)
          (recur bot chat-id timestamp countdown))
      (do (handlers/send-msg-md bot chat-id (str "📢 Il treno è partito. Tutte le porte sono state 🔴 *chiuse*."))
          (swap! game assoc-in [:stages :train-departed?] true)
          (doseq [[k v] (get @game :players)
                  :when (nil? (:wagon v))]
            (swap! game assoc-in [:players k :game-over?] true)
            (handlers/send-msg-md bot chat-id (str "📢 @" (get-in @games.shared/players [:players k :username])
                                                   ", _GAME OVER_. Non sei salito sul treno.")))))))

(defn doors-closed->opened
  "Perform the actions to transfom the state of the doors from closed to opened."
  [bot chat-id timestamp countdown]
  (log/info "doors-closed->opened")
  (let [elapsed-time (jt/time-between (jt/local-date-time) (jt/plus timestamp (jt/minutes countdown)) :seconds)
        elapsed-time-map (jt/convert-amount elapsed-time :seconds :minutes)]
    (if (> elapsed-time 0)
      (do (handlers/send-msg-md bot chat-id (str "📢 Sono rimasti *" (:whole elapsed-time-map)
                                                 " minuti* e *" (:remainder elapsed-time-map)
                                                 " secondi* all'*apertura* delle porte."))
          (Thread/sleep 20000)
          (recur bot chat-id timestamp countdown))
      (do (handlers/send-msg-md bot chat-id (str "📢 Tutte le porte sono state 🟢 *aperte*, ora è possibile circolare."
                                                 "\n\n Usa `/do next` o `/do next mask` per raggiungere la prossima carrozza."))
          (swap! game assoc-in [:train :doors-closed?] false)
          (doseq [[k v] (get @game :players)
                  :when (and (false? (:game-over? v)) (not= (:wagon v) 0))]
            (swap! game assoc-in [:players k :moved] false))
          (doseq [[k v] (get @game :players)
                  :when (and (:game-over? v) (= (:wagon v) 1))]
            (handlers/send-msg-md bot chat-id (str "📢 @" (get-in @games.shared/players [:players k :username])
                                                   ", si trovava nel vagone *" (get-in @game [:players k :wagon])
                                                   "* quando è stato ucciso dal gas tossico.")))))))

(defn doors-opened->closed
  "Perform the actions to transfom the state of the doors from opened to closed."
  [bot chat-id timestamp countdown]
  (log/info "doors-opened->closed")
  (let [elapsed-time (jt/time-between (jt/local-date-time) (jt/plus timestamp (jt/minutes countdown)) :seconds)
        elapsed-time-map (jt/convert-amount elapsed-time :seconds :minutes)]
    (if (> elapsed-time 0)
      (do (handlers/send-msg-md bot chat-id (str "📢 Sono rimasti *" (:whole elapsed-time-map)
                                                 " minuti* e *" (:remainder elapsed-time-map)
                                                 " secondi* al *chiusura* delle porte."))
          (Thread/sleep 20000)
          (recur bot chat-id timestamp countdown))
      (do (handlers/send-msg-md bot chat-id (str "📢 Tutte le porte sono state 🔴 *chiuse*, ora non è più possibile circolare."))
          (swap! game assoc-in [:train :doors-closed?] true)))))

(defn game-loop
  "Perform the actions while the train is in motion."
  [bot chat-id timestamp countdown]
  (log/info "game-loop" @game)
  (let [elapsed-time (jt/time-between (jt/local-date-time) (jt/plus timestamp (jt/minutes countdown)) :seconds)
        elapsed-time-map (jt/convert-amount elapsed-time :seconds :minutes)]
    (if (> elapsed-time 0)
      (do (handlers/send-msg-md bot chat-id (str "📢 Sono rimasti *" (:whole elapsed-time-map)
                                                 " minuti* e *" (:remainder elapsed-time-map)
                                                 " secondi* al *arrivo* del treno a destinazione."))
          (if (get-in @game [:train :doors-closed?])
            (doors-closed->opened bot chat-id (jt/local-date-time) 1)
            (doors-opened->closed bot chat-id (jt/local-date-time) 1))
          (recur bot chat-id timestamp countdown))
      (handlers/send-msg-md bot chat-id (str "📢 Il treno è arrivato a destinazione.")))))

(defn game-cleared!
  "Perform the actions when the train arrived to destination and it is time to reset the game."
  [bot chat-id]
  ;; Tell the players that it is game over because they did not arrived in the locomotive before the train arrived to destination.
  (doseq [[k v] (get @game :players)
          :when (and (false? (:game-over? v)) (not= (:wagon v) 0))]
    (handlers/send-msg-md bot chat-id (str "📢 @" (get-in @games.shared/players [:players k :username])
                                           ", _GAME OVER_. Non sei riuscito a raggiungere la locomotiva.")))
  ;; Perform the appropriate actions when is game over for a player.
  (doseq [[k v] (get @game :players)
          :when (:game-over? v)]
    (games.shared/game-over k))
  ;; Perform the appropiate actions when a player wins.
  (doseq [[k v] (get @game :players)
          :when (and (false? (:game-over? v)) (= (:wagon v) 0))]
    (games.shared/player-wins k)
    (handlers/send-msg-md bot chat-id (str "📢 @" (get-in @games.shared/players [:players k :username])
                                           ", congratulazioni hai vinto, la carta 2️⃣♥️ è stata aggiunta al tuo mazzo."
                                           "\n\n Usa /deck per vedere il mazzo.")))
  ;; Change state of the players before the game is cleared.
  (doseq [[k v] (get @game :players)]
    (games.shared/player-stopped-playing k))
  (games.shared/game-cleared chat-id)
  (reset! game {:players {}
                :train {:wagons 2}
                :toxic-wagon 1
                :doors-closed? true
                :stages {:game-started? false
                         :train-departed? false
                         :train-arrived? false}}))

(defn init
  "Initiliase the game."
  [bot msg]
  (add-player! (fields/from-id msg))
  (let [chat-id (fields/chat-id msg)
        players (count (get @game :players))
        username (fields/from-user msg)]
    (log/info "Running: games.h2/init!")
    (handlers/send-msg-md bot chat-id (str "*📢 @" username ", benvenuto nel gioco 2️⃣♥️*."))
    (when (<= players 1)
      (do (handlers/send-msg-md bot chat-id (str "📢 Un giocatore si è registrato al gioco 2️⃣♥️, la partita comincerà a breve. In attesa di ulteriori giocatori..."
                                                 "\n\nUsa /play per registrarti a questo gioco."))
          (game-start bot chat-id (jt/local-date-time) 1)
          (train-departure bot chat-id (jt/local-date-time) 1)
          (show-rules bot chat-id players)
          (game-loop bot chat-id (jt/local-date-time) (+ (* players 2) 4))
          (game-cleared! bot chat-id)))))

(defn action-get-on-board!
  "Change the state of the players when they get on board."
  [bot chat-id msg]
  (let [player (fields/from-id msg)
        username (fields/from-user msg)]
    (swap! game assoc-in [:players player :wagon] (get-in @game [:train :wagons]))
    (swap! game assoc-in [:players player :moved] false)
    (swap! game assoc-in [:players player :ammo] (- (get-in @game [:train :wagons]) 2))
    (handlers/send-msg-md bot chat-id (str "📢 @" username
                                           " è salito nel ultimo vagone. Gli è stato fornito una maschera e *"
                                           (get-in @game [:players player :ammo]) "* cartucce."))))

(defn action-next-wagon!
  "Change the state of the players when they move to the next wagon."
  ([bot chat-id msg]
   (let [player (fields/from-id msg)]
     (swap! game assoc-in [:players player :moved] true)
     (swap! game assoc-in [:players player :wagon] (dec (get-in @game [:players player :wagon])))
     (handlers/send-msg-md bot chat-id (str "📢 @" (fields/from-user msg)
                                            " si è spostato nel vagone numero *" (get-in @game [:players player :wagon])
                                            "* non indossndo la maschera."))
     (when (= (get-in @game [:players player :wagon]) (get-in @game [:train :toxic-wagon]))
       (swap! game assoc-in [:players player :game-over?] true))))
  ([bot chat-id msg mask?]
   (let [player (fields/from-id msg)
         username (fields/from-user msg)]
     (swap! game assoc-in [:players player :moved] true)
     (swap! game assoc-in [:players player :ammo] (dec (get-in @game [:players player :ammo])))
     (swap! game assoc-in [:players player :wagon] (dec (get-in @game [:players player :wagon])))
     (handlers/send-msg-md bot chat-id (str "📢 @" username
                                            " si è spostato nel vagone numero *" (get-in @game [:players player :wagon])
                                            "* indossando la maschera. Gli rimangono *" (get-in @game [:players player :ammo])
                                            "* cartucce.")))))

(defn actions
  "The possible actions that can be made in game."
  [bot msg]
  (log/info "Running: games/actions.")
  (let [chat-id (fields/chat-id msg)
        substr (s/lower-case (s/trim (fields/text msg)))
        players (count (get @game :players))
        player (fields/from-id msg)
        username (fields/from-user msg)]
    (cond
      (true? (get-in @game [:stages :stages-arrived?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", la partita è finita."))
      (false? (get-in @game [:stages :game-started?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", la partita non è iniziata."))
      (true? (get-in @game [:players player :game-over?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", è _GAME OVER_, non puoi piu eseguire altre azioni."))
      (true? (= (get-in @game [:players player :wagon]) 0)) (handlers/send-msg-md bot chat-id (str "📢 @" username ", sei arrivato al sicuro nella locomotiva, aspetta che il treno arrivi a destinazione."))
      :else (cond
              (s/includes? substr "get on board") (cond
                                                    (some? (get-in @game [:players player :wagon])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", sei gia sul treno."))
                                                    (true? (get-in @game [:stages :train-departed?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", il treno è partito non puoi piu salire sul treno."))
                                                    :else (action-get-on-board! bot chat-id msg))
              (s/includes? substr "next mask") (cond
                                                 (false? (get-in @game [:stages :train-departed?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", il treno non è partito."))
                                                 (nil? (get-in @game [:players player :wagon])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", non sei salito sul treno."))
                                                 (true? (get-in @game [:train :doors-closed?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", le porte sono chiuse."))
                                                 (true? (get-in @game [:players player :moved])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", questo turno non puoi proseguire oltre."))
                                                 (zero? (get-in @game [:players player :ammo])) (handlers/send-msg-md bot chat-id (str "📢 @" username " hai finito le cartucce, non puoi più usare la maschera."))
                                                 :else (action-next-wagon! bot chat-id msg true))
              (s/includes? substr "next") (cond
                                            (false? (get-in @game [:stages :train-departed?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", il treno non è partito."))
                                            (nil? (get-in @game [:players player :wagon])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", non sei salito sul treno."))
                                            (true? (get-in @game [:train :doors-closed?])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", le porte sono chiuse."))
                                            (true? (get-in @game [:players player :moved])) (handlers/send-msg-md bot chat-id (str "📢 @" username ", questo turno non puoi proseguire oltre."))
                                            :else (action-next-wagon! bot chat-id msg))
              (s/includes? substr "help") (show-rules bot chat-id players)
              :else (handlers/send-msg-md bot chat-id (str "📢 Il commando inserito non è valido."))))))

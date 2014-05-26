(ns hn.core
  (:require [cheshire.core :as json]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :refer [resource]])
  (:import [java.awt SystemTray TrayIcon PopupMenu MenuItem Toolkit Frame]
           [java.awt.event ActionListener MouseEvent MouseAdapter]
           [java.io IOException])
  (:gen-class))

(def history (atom #{}))

(defn is-new? [{id :id}]
  (not (contains? @history id)))

(defn mark-as-read [id]
  (swap! history conj id))

(defn new-menu-item [label callback & {id :id}]
  (let [menu (MenuItem. label)
        listener (proxy [ActionListener] []
                   (actionPerformed [event]
                     (callback)
                     (if id (mark-as-read id))))]
    (.addActionListener menu listener)
    menu))

(def hn-api-url "http://api.ihackernews.com/page")

(defn hn-items []
  (-> hn-api-url
      slurp
      (json/parse-string true)
      :items))

(defn exit []
  (shutdown-agents)
  (System/exit 0))

(defn add-spacer! [menu]
  (.add menu (MenuItem. "-")))

(defn add-exit! [menu]
  (.add menu (new-menu-item "Exit" exit)))

(defn add-hn-to-menu! [menu]
  (let [{new-items true old-items false} (group-by is-new? (hn-items))]
    (letfn [(mapfn [{:keys [id title url commentCount points]}]
              (let [full-title (format "%s - %s (%s)" title points commentCount)
                    menu-item (new-menu-item full-title #(browse-url url) :id id)]
                (println full-title)
                (.add menu menu-item)))]
      (doall (map mapfn new-items))
      (when (seq old-items)
        (add-spacer! menu))
      (doall (map mapfn old-items)))))

(defn add-left-click! [icon frame]
  (.setResizable frame false)
  (.setVisible frame true)
  (let [listener (proxy [MouseAdapter] []
                   (mouseClicked [event]
                     (when (== (.getButton event)
                               MouseEvent/BUTTON1)
                       (let [popup (.getPopupMenu icon)
                             x (.getXOnScreen event)
                             y (.getYOnScreen event)]
                         (.add frame popup)
                         (.show popup frame x y)))))]
    (.addMouseListener icon listener)))

(defn -main [& args]
  (let [tray (SystemTray/getSystemTray)
        frame (Frame. "")
        image (.getImage (Toolkit/getDefaultToolkit)
                         (resource "icon.png"))
        icon (TrayIcon. image) ]
    (.setImageAutoSize icon true)
    (.add tray icon)
    (add-left-click! icon frame)
    (when-not (SystemTray/isSupported)
      (throw (Exception. "System tray is not supported.")))
    (loop []
      (try
        (let [popup (PopupMenu.)]
          (println "Updating items")
          (add-hn-to-menu! popup)
          (add-spacer! popup)
          (add-exit! popup)
          (.setPopupMenu icon popup)
          (Thread/sleep (* 5 60 1000)))
        (catch IOException e
          (println (str "Exception during update " e))))
      (recur))))

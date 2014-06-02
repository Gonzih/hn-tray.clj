(ns hn.core
  (:require [cheshire.core :as json]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :refer [resource]])
  (:import [java.awt SystemTray TrayIcon Toolkit]
           [java.awt.event ActionListener MouseAdapter]
           [javax.swing JPopupMenu JMenuItem]
           [java.io IOException])
  (:gen-class))

(def history (atom #{}))
(def current-popup (atom nil))

(defn is-new? [{id :id}]
  (not (contains? @history id)))

(defn mark-as-read [id]
  (swap! history conj id))

(defn new-menu-item [label callback & {id :id}]
  (let [menu (JMenuItem. label)
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

(defn add-separator! [menu]
  (.addSeparator menu))

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
        (add-separator! menu))
      (doall (map mapfn old-items)))))

(defn add-left-click! [icon]
  (let [listener (proxy [MouseAdapter] []
                   (mouseClicked [event]
                     (let [popup @current-popup
                           x (.getXOnScreen event)
                           y (.getYOnScreen event)]
                       (.setLocation popup x y)
                       (.setInvoker popup popup)
                       (.setVisible popup true))))]
    (.addMouseListener icon listener)))

(defn -main [& args]
  (let [tray (SystemTray/getSystemTray)
        image (.getImage (Toolkit/getDefaultToolkit)
                         (resource "icon.png"))
        icon (TrayIcon. image) ]
    (.setImageAutoSize icon true)
    (.add tray icon)
    (add-left-click! icon)
    (when-not (SystemTray/isSupported)
      (throw (Exception. "System tray is not supported.")))
    (loop []
      (try
        (let [popup (JPopupMenu.)]
          (println "Updating items")
          (add-hn-to-menu! popup)
          (add-separator! popup)
          (add-exit! popup)
          (reset! current-popup popup)
          (Thread/sleep (* 5 60 1000)))
        (catch IOException e
          (println (str "Exception during update " e))))
      (recur))))

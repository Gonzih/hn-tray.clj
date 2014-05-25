(ns hn.core
  (:require [cheshire.core :as json]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :refer [resource]])
  (:import [java.awt SystemTray TrayIcon PopupMenu MenuItem Toolkit]
           [java.awt.event ActionListener])
  (:gen-class))

(defn menu-item [label callback]
  (let [menu (MenuItem. label)
        listener (proxy [ActionListener] []
                   (actionPerformed [event] (callback)))]
    (.addActionListener menu listener)
    menu))

(def hn-api-url "http://api.ihackernews.com/page")

(defn hn-items []
  (-> hn-api-url
      slurp
      (json/parse-string true)
      :items))

(defn add-hn-to-menu! [menu]
  (letfn [(mapfn [{:keys [title url commentCount points]}]
            (let [full-title (format "%s - %s(%s)" title points commentCount)
                  menu-item (menu-item full-title #(browse-url url))]
              (println full-title)
              (.add menu menu-item)))]
  (doall (map mapfn (hn-items)))))

(defn exit []
  (shutdown-agents)
  (System/exit 0))

(defn -main [& args]
  (let [tray (SystemTray/getSystemTray)
        image (.getImage (Toolkit/getDefaultToolkit)
                         (resource "icon.png"))
        icon (TrayIcon. image)
        exit (menu-item "Exit" exit)]
    (.setImageAutoSize icon true)
    (.add tray icon)
    (loop []
      (let [popup (PopupMenu.)]
        (println "Updating items")
        (add-hn-to-menu! popup)
        (.add popup exit)
        (.setPopupMenu icon popup)
        (Thread/sleep (* 5 60 1000))
        (recur)))))

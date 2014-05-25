(defproject hn "0.1.1-SNAPSHOT"
  :description "HN Tray"
  :url "https://github.com/Gonzih/hn-tray.clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.3.1"]]
  :profiles {:uberjar {:aot :all}}
  :main hn.core)

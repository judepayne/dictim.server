(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.java.io :as io])) 


(def build-folder "target")


(def jar-content (str build-folder "/classes"))


(def basis (b/create-basis {:project "deps.edn"}))


(def version (clojure.string/trim (slurp (io/resource "VERSION"))))


(def app-name "dictimserver")


(def uber-file-name (format "%s/%s-standalone.jar" build-folder app-name))


(defn clean [_]
  (b/delete {:path build-folder})
  (println (format "Build folder \"%s\" removed" build-folder)))


(defn uber [_]
  (clean nil)

  (b/copy-dir {:src-dirs   ["resources"]         ; copy resources
               :target-dir jar-content})

  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src"]
                  :class-dir jar-content})

  (b/uber {:class-dir jar-content                ; create uber file
           :uber-file uber-file-name
           :basis     basis
           :main      'app.core})                ; here we specify the entry point for uberjar
  
  (println (format "Uber file created: \"%s\" version: \"%s\"" uber-file-name version)))

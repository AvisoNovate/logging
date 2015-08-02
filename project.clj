(defproject io.aviso/logging "0.1.0"
  :description "Clojure logging with Logback and SLF4J plus request correlation across servers."
  :url "https://github.com/AvisoNovate/logging"
  :license {:name "Apache Sofware License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :jvm-opts ^:replace ["-Xmx1G" "-Xms1G" "-XX:+UseG1GC"]

  :javac-options ["-target" "1.7" "-source" "1.7"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [io.aviso/pretty "0.1.18"]
                 [org.slf4j/jcl-over-slf4j "1.7.12"]]

  :java-source-paths ["java-src"]

  :plugins [[lein-shell "0.4.0"]]

  :shell {:commands {"scp" {:dir "doc"}}}
  :aliases {"deploy-doc" ["shell"
                          "scp" "-r" "." "hlship_howardlewisship@ssh.phx.nearlyfreespeech.net:io.aviso/logging"]
            "release"    ["do"
                          "clean,"
                          "spec,",
                          "doc,"
                          "deploy-doc,"
                          "deploy" "clojars"]}

  :codox {:defaults                  {:doc/format :markdown}
          :src-dir-uri               "https://github.com/AvisoNovate/logging/blob/master/"
          :src-linenum-anchor-prefix "L"})
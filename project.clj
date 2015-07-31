(defproject io.aviso/logging "0.0.1"
            :description "Pre-configured logging."
            :url "https://stash.annadaletech.com/projects/FLZ"

            :jvm-opts ^:replace ["-Xmx1G" "-Xms1G" "-XX:+UseG1GC"]

            :javac-options ["-target" "1.7" "-source" "1.7"]

            :dependencies [[org.clojure/clojure "1.7.0"]
                           [org.slf4j/slf4j-api "1.7.12"]
                           [ch.qos.logback/logback-classic "1.1.3"]
                           [org.clojure/tools.logging "0.3.1"]
                           [io.aviso/pretty "0.1.18"]
                           [org.slf4j/jcl-over-slf4j "1.7.12"]]

            :java-source-paths ["java-src"]
           
                       :shell {:commands {"scp" {:dir "doc"}}}
            :aliases {"deploy-doc" ["shell"
                                    "scp" "-r" "." "hlship_howardlewisship@ssh.phx.nearlyfreespeech.net:io.aviso/logging"]
                      "release"    ["do"
                                    "clean,"
                                    "spec,",
                                    "doc,"
                                    "deploy-doc,"
                                    "deploy" "clojars"]}
            
            :codox {:defaults   {:doc/format :markdown}
                    :output-dir "target/docs"})

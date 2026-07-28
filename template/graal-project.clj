(defproject gloat/graal-build "0.0.0"
  :dependencies [[org.clojure/clojure "CLOJURE-VERSION"]]
  :source-paths ["src"]
  :main gloat.graal.main
  :profiles
  {:uberjar
   {:aot :all
    :uberjar-name "gloat-graal.jar"
    :global-vars {*assert* false}
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"
               "-Dclojure.spec.skip-macros=true"]}})

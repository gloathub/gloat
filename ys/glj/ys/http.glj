;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; HTTP client library for Gloat
;; Uses Go's net/http package

(ns ys.http
  (:require [clojure.string :as str])
  (:refer-clojure :exclude [get]))

;;------------------------------------------------------------------------------
;; Helper functions
;;------------------------------------------------------------------------------

(defn- bytes-to-str [b]
  "Convert Go []byte to string"
  (fmt.Sprintf "%s" b))

;;------------------------------------------------------------------------------
;; HTTP functions
;;------------------------------------------------------------------------------

(defn post
  "HTTP POST request
   url: string
   opts: {:headers {\"Key\" \"Value\"} :body \"...\"}
   returns: {:status N :body \"...\"}"
  [url opts]
  (let [body-str (or (:body opts) "")
        reader (strings.NewReader body-str)
        [req req-err] (net:http.NewRequest "POST" url reader)]
    (if (not (nil? req-err))
      {:status 500 :body (str "Failed to create request: " (.Error req-err))}
      (do
        ;; Set headers (convert keyword keys to strings)
        (doseq [[k v] (:headers opts)]
          (.Set (.Header req) (name k) (str v)))
        ;; Execute request
        (let [client net:http.DefaultClient
              [resp resp-err] (.Do client req)]
          (if (not (nil? resp-err))
            {:status 500 :body (str "Request failed: " (.Error resp-err))}
            (let [[body-bytes body-err] (io.ReadAll (.Body resp))]
              (.Close (.Body resp))
              {:status (.StatusCode resp)
               :body (if (nil? body-err)
                       (bytes-to-str body-bytes)
                       (str "Failed to read response: " (.Error body-err)))})))))))

(defn get
  "HTTP GET request
   url: string
   opts (optional): {:headers {\"Key\" \"Value\"}}
   returns: {:status N :body \"...\"}"
  ([url] (get url {}))
  ([url opts]
   (let [[req req-err] (net:http.NewRequest "GET" url nil)]
     (if (not (nil? req-err))
       {:status 500 :body (str "Failed to create request: " req-err)}
       (do
         ;; Set headers (convert keyword keys to strings)
         (doseq [[k v] (:headers opts)]
           (.Set (.Header req) (str k) (str v)))
         ;; Execute request
         (let [client net:http.DefaultClient
               [resp resp-err] (.Do client req)]
           (if (not (nil? resp-err))
             {:status 500 :body (str "Request failed: " resp-err)}
             (let [[body-bytes body-err] (io.ReadAll (.Body resp))]
               (.Close (.Body resp))
               {:status (.StatusCode resp)
                :body (if (nil? body-err)
                        (bytes-to-str body-bytes)
                        (str "Failed to read response: " (.Error body-err)))}))))))))

(comment
  ;; Test examples
  (get "https://api.github.com/zen")
  (post "https://httpbin.org/post"
        {:headers {"Content-Type" "application/json"}
         :body "{\"test\": true}"})
  )

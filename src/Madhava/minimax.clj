(ns minimax.core
  (:require [Madhava.core :as Madhava :refer :all]))

;; ???:
;; tangent
;; bundle
;; distance
;; write
;; v+
;; v-
;; k*v

(defn gradient [f & x]
  (let [n (.lastIndexOf x)]
    (map #(tangent ((j* f) (bundle x (e % n)))) n)))  

(defn argmin [f x]
  (let [g (gradient f)
        w (Math/pow 10 -5)]
    (letfn [(argmin-loop [x fx gx eta i]
              (let [x-prime (v- x (k*v eta gx)) 
                    fx-prime (f x-prime)]
                (cond
                  (<= (magnitude gx) w)        x
                  (= i 10)                     (argmin-loop x fx gx (* 2 eta) 0)
                  (<= (distance x x-prime) w)  x
                  (< fx-prime fx)              (argmin-loop x-prime fx-prime (g x-prime) eta (inc i))
                  :else                        (recur x fx gx (/ eta 2) 0))))]
      (argmin-loop x (f x) (g x) w 0))))

(defn argmax [f x]
  (argmin #(- (f %)) x))

(defn max [f x]
  (f (argmax f x)))

(defn saddle []
  (let [start [1 1]
        f #(- (+ %1 %2) (+ %3 %4))
        [x1 y1] (argmin (fn [[x1 y1]]
                          (max (fn [[x2 y2]]
                                 (f x1 y1 x2 y2))
                               start))
                        start)
        [x2 y2] (argmax (fn [x2 y2]
                          (f (first [x1 y1]) (fnext [x1 y1]) x2 y2)
                          start))]
    [[(write x1) (write y1)]
     [(write x2) (write y2)]]))

(defn naive-euler [w]
  (let [charges [[10 (- 10 w)]
                 [10 0]]
        x-initial [0 8]
        xdot-initial [0.75 0]
        delta-t (Math/pow 10 -1)
        p (fn [x]
            (reduce + 0
                    (map (fn [c]
                           (/ 1 (distance x c)))
                         charges)))]
    (letfn [(euler-loop [x xdot]
              (let [xddot (k*v -1 (gradient p x))
                    x-new (v+ x (k*v delta-t xdot))]
                (if (pos? (second x-new))
                  (euler-loop x-new (v+ xdot (k*v delta-t xddot)))
                  (let [delta-t-f (/ (- (second x))
                                     (second xdot))
                        x-t-f (first (v+ x (k*v delta-t-f xdot)))]
                    (* x-t-f x-t-f)))))]
      (euler-loop x-initial xdot-initial))))

(defn particle []
  (let [w0 0
        [w*] (argmin (fn [[w]]
                       (naive-euler w))
                     [w0])]
    (write w*)))

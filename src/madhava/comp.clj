(ns madhava.comp
  (:require [madhava.arithmetic :refer :all]
            [madhava.diff :refer :all]
            [madhava.util :refer :all]
            [madhava.vectormath :refer :all]
            [clojure.math.combinatorics :as combo]
            [clojure.data.avl :refer [sorted-map-by]]
            [clj-tuple :refer [vector]])
  (:refer-clojure :exclude [vector sorted-map-by]))

(defn compose
  "Functional composition. 
  Third argument is index of variable, starting from 1."
  [f g idx]
  (let [idx (dec idx)]  ;; x == 1st var, 0th element in tuple 
    (loop [f f
           result {}]
      (let [term (first f)
            vars (first term)
            coeff (second term)
            v (nth vars idx)]
        (cond
          (nil? term) (into (sorted-map-by grevlex) result)
          (zero? v) (recur (dissoc f vars) (add {vars coeff} result))
          :else (recur (dissoc f vars) (add (apply mul
                                                   {(assoc vars idx 0) coeff}
                                                   (repeat v g))  ;; raise g to exponent
                                            result)))))))

(defn revert 
  "Computes compositional inverse, aka Horner's rule.
  See Knuth TAOCP vol 2 pp. 486-488."
  [f]
  (let [dense-f (atom f)]
    (run! #(let [term1 (first %1)]
             (when (not= 0 (- (reduce +' term1)
                              (reduce +' (first %2))))
               (let [max-exp (max term1)]
                 (swap! dense-f assoc
                        (update term1 (.indexOf max-exp) (dec max-exp))
                        0))))
          f
          (next f))
    @dense-f))

(defn ruffini-horner [f & rest]
  (->> f
       (map #(->> %2
                  (revert)
                  (reduce (fn [x y]
                            (+ y (* x %1))))) ;; substitute values in rest args for variables in f
            rest)
       (reduce *'))) ;; multiply result of evaluating each variable

(defn chain1
  "Faster implementation of chain rule for univariate functions."
  [f g idx]
  (let [i (dec idx)]
    (-> f
        (grad)
        (nth i)
        (compose g idx)
        (mul (nth (grad g) i)))))

(defn chain
  "Chain rule. 
  Variadic, unary version returns transducer."
  ([f]  ;; completion
   (fn
     ([] f)
     ([g] (chain f g))
     ([g & more] (chain f g more))))
  ([f g] 
   (->> f
        (grad)
        (map-indexed #(compose %2 g (inc %1)))
        (map mul (grad g))
        (reduce add)))
  ([f g & more]
   (reduce chain (chain f g) more)))

(defn chain-higher1
  "Faster implementation of higher-order chain rule for univariate functions."
  [f g order]
  (let [f' (diff f order)
        g' (diff g order)
        partitions (->> (repeat order 1)
                        (combo/partitions)
                        (map (fn [x] (map #(reduce +' %) x)))
                        (reverse)) 
        coeff (map (fn [p] (->> order     ;; number of partitions of set with elements
                               (inc)     ;; equal to order into sets of x elements
                               (range 1)
                               (combo/partitions)
                               (map sort)  ;; TO DO: don't sort
                               (filter #(= (map count %) p))
                               (count))) 
                   (map sort partitions))] ;; TO DO: don't sort
    ;; (do (println partitions)
    ;;     (println coeff)
    ;;     (println (map frequencies partitions)))))
    (reduce add
            (map #(mul {[0] %1}
                       (compose (get f' %2) g 1)
                       (reduce mul
                               (map (fn [x] (pow (get g' (first x))
                                                (second x)))
                                    %3)))
                 coeff
                 (reverse (range 1 (inc order))) 
                 (map frequencies partitions)))))

;; (defn chain-higher
;;   "Higher-order chain rule using Faà di Bruno's formula."
;;   [f g order]
;;   (let [dims (count (ffirst f))
;;         n! (reduce *' (range 1 (dec order)))
;;         m! '()
;;         f' (vals (diff f order))
;;         g' (vals (diff g order))
;;         f*g (map-indexed #(compose %2 g (inc %1)) f')
;;         g'' (->> g'
;;                  (map #(mul %1 (repeat %1 (reduce *' %2)))
;;                       g'
;;                       (take 10 (mapcat #(repeat dims %) (range))))  ;; exponentiation by order
;;                  (reduce *'))]
;;     (scale (mul f*g g'')
;;            (/ n! m!))))


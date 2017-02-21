# Madhava v2

>”Multiply the arc by the square of the arc, and take the result of repeating that (any number of times). Divide (each of the above numerators) by the squares of the successive even numbers increased by that number and multiplied by the square of the radius. Place the arc and the successive results so obtained one below the other, and subtract each from the one above. These together give the jiva, as collected together in the verse beginning with "vidvan" etc."

-[Madhava of Sangamagrama](https://en.wikipedia.org/wiki/Madhava_of_Sangamagrama) (c. 1350 – c. 1425), founder of the Kerala school of astronomy and mathematics

---

Madhava is a Clojure library for forward mode [automatic differentiation](https://en.wikipedia.org/wiki/Automatic_differentiation) and integration of partial differential equations. As opposed to many other functional AD libraries, Madhava takes a stream processing approach by generating linear maps of all partials up to a given order at once and storing them by keys in hash-maps. As functions are represented as dense collections of n-tuples stored in Clojure vectors, this approach is both simple and extremely fast: capable of generating four orders of partial derivatives from hairy three dimensional functions in around one microsecond on commodity CPUs. Additional functions are included for basic arithmetic operations, linear transformations, and several common Taylor series.

Many thanks to Doug McIlroy for feedback and encouragement along the way. His [Power Serious](http://www.cs.dartmouth.edu/~doug/powser.html) package for Haskell will always be an inspiration for elegant software design.

---

##Usage

Generating linear maps of partial derivatives:

```
;; 2xy + 3x + 5y + 7
=> (def diff-map (atom {}))
=> (diff [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] diff-map 2)
=> (pprint diff-map)
#<Atom@31648880: 
  {0 [[2 1 1] [3 1 0] [5 0 1] [7 0 0]],
   1 {1 [[2 0 1] [3 0 0]], 2 [[2 1 0] [5 0 0]]},
   2 {1 {1 [], 2 [[2 0 0]]}, 2 {1 [[2 0 0]], 2 []}}}>
```

...and integrals:

```
=> (def int-map (atom {}))
=> (int [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] int-map 3)
=> (pprint int-map)
#<Atom@18802109: 
  {0 [[2 1 1] [3 1 0] [5 0 1] [7 0 0]],
   1 {1 [[1 2 1] [3/2 2 0]], 2 [[1 1 2] [5/2 0 2]]},
   2
   {1 {1 [[1/3 3 1] [1/2 3 0]], 2 [[1/2 2 2]]},
    2 {1 [[1/2 2 2]], 2 [[1/3 1 3] [5/6 0 3]]}},
   3
   {1
    {1 {1 [[1/12 4 1] [1/8 4 0]], 2 [[1/6 3 2]]},
     2 {1 [[1/6 3 2]], 2 [[1/6 2 3]]}},
    2
    {1 {1 [[1/6 3 2]], 2 [[1/6 2 3]]},
     2 {1 [[1/6 2 3]], 2 [[1/12 1 4] [5/24 0 4]]}}}}>
```

Arithmetic:

```
;; (2xy + 3x + 5y + 7) + (x^2y + 4x + y) = x^2y + 2xy + 7x + 6y +7
=> (add [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 2 1] [4 1 0] [1 0 1]])
[[1 2 1] [2 1 1] [7 1 0] [6 0 1] [7 0 0]]

;; (2xy + 3x + 5y + 7) - (2xy + 3x + 5y + 7) = 0
=> (sub [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[2 1 1] [3 1 0] [5 0 1] [7 0 0]])
[]

;; 2 * (2xy + 3x + 5y + 7) = 4xy + 6x + 10y + 14
=> (scale [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] 2)
[[4 1 1] [6 1 0] [10 0 1] [14 0 0]]

;; (2xy + 3x + 5y + 7) * (x^2y + 4x + y) =
;; 2x^3y^2 + 3x^3y + 5x^2y^2 + 15x^2y + 2xy^2 + 12x^2 + 23xy + 5y^2 + 28x + 7y
=> (mul [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 2 1] [4 1 0] [1 0 1]])
[[2 3 2] [3 3 1] [5 2 2] [15 2 1] [2 1 2] [12 2 0] [23 1 1] [5 0 2] [28 1 0] [7 0 1]]
```

Linear transforms:

```
=> (def diff-map (atom {}))
=> (diff [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] diff-map 1)
=> (linear-transform diff-map [(Math/cos 0.5) 1 1] [(Math/sin 0.5) 1 2])
[[1.682941969615793 1 1] [2.5244129544236897 1 0] [4.207354924039483 0 1] [6.311032386059225 0 0]]
```

Taylor Series:

```
=> (sparse-to-dense (take 10 (exp-series)))
[[1/362880 9] [1/40320 8] [1/5040 7] [1/720 6] [1/120 5] [1/24 4] [1/6 3] [1/2 2] [1 1] [1 0]]

=> (sparse-to-dense (take 10 (sin-series)))
[[1/362880 9] [-1/5040 7] [1/120 5] [-1/6 3] [1 1]]

=> (sparse-to-dense (take 10 (cos-series)))
[[1/40320 8] [-1/720 6] [1/24 4] [-1/2 2] [1 0]]

=> (sparse-to-dense (take 10 (atan-series)))
[[1/9 8] [-1/7 6] [1/5 4] [-1/3 2] [1 0]]

=> (sparse-to-dense (take 10 (sinh-series)))
[[1/362880 9] [1/5040 7] [1/120 5] [1/6 3] [1 1]]

=> (sparse-to-dense (take 10 (cosh-series)))
[[1/40320 8] [1/720 6] [1/24 4] [1/2 2] [1 0]]
```

Benchmarking:

```
;; 3 dimensions, 5 terms, 4 orders tested on 2.6GHz Core i7 
=> (use 'criterium.core)
=> (quick-bench (doall (diff [[5 4 3 3] [8 2 1 2] [1 0 4 0] [2 0 0 3] [5 1 0 0]] (atom {}) 4)))
Evaluation count : 559764 in 6 samples of 93294 calls.
             Execution time mean : 1.049041 µs
    Execution time std-deviation : 24.794316 ns
   Execution time lower quantile : 1.027316 µs ( 2.5%)
   Execution time upper quantile : 1.077457 µs (97.5%)
                   Overhead used : 7.265547 ns
```

---

*Features planned for future versions:*

+ *Division using Buchberger's algorithm*
+ *Multivariate Horner Scheme*
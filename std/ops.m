(def + +i)
(def - -i)
(def * *i)
(def / /i)
(def % %i)
(def < <i)
(def > >i)

(def >=
  (lambda (x y)
    (! (< x y))))

(def <=
  (lambda (x y)
    (! (> x y))))

(def !=
  (lambda (x y)
    (! (= x y))))

(def neg (- 0))

(def nil? (= nil))

(def >=i (lambda (x y) (! (<i x y))))
(def >=l (lambda (x y) (! (<l x y))))
(def >=f (lambda (x y) (! (<f x y))))
(def >=d (lambda (x y) (! (<d x y))))

(def <=i (lambda (x y) (! (>i x y))))
(def <=l (lambda (x y) (! (>l x y))))
(def <=f (lambda (x y) (! (>f x y))))
(def <=d (lambda (x y) (! (>d x y))))

(def negi (-i 0))
(def negl (-l 0))
(def negf (-f 0))
(def negd (-d 0))

(def !=
  (lambda (x y)
    (! (= x y))))

(def nil? (= nil))

(def + +i)
(def - -i)
(def * *i)
(def / /i)
(def % %i)
(def < <i)
(def > >i)
(def >= >=i)
(def <= <=i)
(def neg negi)

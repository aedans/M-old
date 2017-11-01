(defmacro defm
  (lambda (x)
    `(defmacro ,(car x) (lambda ,(car (cdr x)) ~(cdr (cdr x))))))

(defm defn (x)
  `(def ,(car x) (lambda ,(car (cdr x)) ~(cdr (cdr x)))))

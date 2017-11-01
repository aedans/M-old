(def print
  (lambda (out string)
    (if (! (= string nil))
      (do
        (write out (car string))
        (print out (cdr string))))))

(def println
  (lambda (out string)
    (do
      (print out string)
      (write out newline))))

(def sout
  (println stdout))

(def serr
  (println stderr))

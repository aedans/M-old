# M

A Minimal Lisp.

### Fundamental Forms

```
(def <identifier> <expression>)
```

Defines a global variable with name \<identifier\> and value \<expression\>.
Overrides previously defined variables. 

```
(lambda (<identifier>+) <expression>+)
```

Evaluates to an anonymous function which evaluates each \<expression\>.
Each \<identifier\> is bound locally when the function is called.
The return value of the function is equal to the return value of the last \<expression\>.

Lambdas are curried; (lambda (x y) (+ x y)) is expanded to (lambda (x) (lambda (y) (+ x y))).
Closures are supported.

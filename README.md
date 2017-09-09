# M

A Minimal Lisp.

### Fundamental Forms

```
(def <identifier> <expression>)
```

Evaluates to expression.
Defines a global variable with name identifier and value expression.
Shadows previously defined variables.
Identifier must be an unbound atom.

```
(lambda (<identifier>+) <expression>+)
```

Evaluates to an anonymous function which evaluates each expression.
Each identifier is bound locally when the function is called.
The return value of the function is equal to the return value of the last expression.

Lambdas are curried; (lambda (x y) (+ x y)) is expanded to (lambda (x) (lambda (y) (+ x y))).
Closures are supported.

```
(if <condition> <then> <else>?)
```

If condition is true, evaluates to then, otherwise evaluates to else.
If there is no else, and condition is false, evaluates to nil instead.

```
(do <expression>+)
```

Evaluates to the last expression.
Each expression is evaluated sequentially.

```
'(<expression>+)
```

Evaluates to a list of each expression.

```
`(<expression>+)
```

Evaluates to a list of each expression.

Each expression in the list preceded with a comma will be evaluated and put into to the list.

Each expression in the list preceded by a tilde will be evaluated and merged into the list.

### Fundamental Values

- `true` Evaluates to true
- `false` Evaluates to false
- `nil` Evaluates to nil
- `macro` Takes a lambda as an argument, returns a macro
- `macroexpand` Takes an expression as an argument, returns the expression with all macros expanded
- `cons` Creates a cons cell from two values
- `car` Gets the first value from a cons cell
- `cdr` Gets the second value from a cons cell
- `!` Inverts a boolean
- `|` Takes two arguments, returns true if both arguments are true
- `&` Takes two arguments, returns true if either argument is true
- `=` Takes two arguments, returns true if both arguments are equal
- `+(i/l/f/d)?` Takes two arguments, returns their sum. i/l/f/d to specify the return type.
- `-(i/l/f/d)?` Takes two arguments, returns their difference. i/l/f/d to specify the return type.
- `*(i/l/f/d)?` Takes two arguments, returns their product. i/l/f/d to specify the return type.
- `/(i/l/f/d)?` Takes two arguments, returns their quotient. i/l/f/d to specify the return type.
- `read` Takes an input stream, returns the next char in the input stream
- `write` Takes an output stream and a char, writes the char to the output stream
- `stdout` The standard output stream
- `stderr` The standard error stream
- `stdin` The standard input stream

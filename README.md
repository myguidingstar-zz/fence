# fence

Effortless way to avoid manual extern files when doing javascript
interop from Clojurescript.

From a **compromised** Clojurik dictionary:

(Warning: This section is just for fun. Feel free to skip to main
documentation ahead.)

<img align="right" width="300" src="http://i1.kym-cdn.com/photos/images/original/000/412/815/2be.jpg">

> *fence* (noun)

> 1. a structure made of w̶o̶o̶d̶ ̶o̶r̶ ̶m̶e̶t̶a̶l̶ keystrokes etc that surrounds a
>    piece of l̶a̶n̶d̶ code or prevents p̶e̶o̶p̶l̶e̶ ̶o̶r̶ ̶a̶n̶i̶m̶a̶l̶s̶ ̶f̶r̶o̶m̶ ̶e̶n̶t̶e̶r̶i̶n̶g̶
>    ̶o̶r̶ ̶l̶e̶a̶v̶i̶n̶g̶ **javascript symbols** from getting renamed

> 2. someone who b̶u̶y̶s̶ ̶a̶n̶d̶ ̶s̶e̶l̶l̶s̶ ̶s̶t̶o̶l̶e̶n̶ ̶g̶o̶o̶d̶s̶ uses macros from a
>    library with stolen code in it

<img align="right" width="300" src="http://imgs.xkcd.com/comics/compiling.png">

> *fence* (verb)

> 1. to put a̶ ̶f̶e̶n̶c̶e̶ parentheses around something

> 2. to fight with a l̶o̶n̶g̶ ̶t̶h̶i̶n̶ ̶s̶w̶o̶r̶d̶ short line of dots as a s̶p̶o̶r̶t̶ way
>    to cure headaches

> 3. to answer someone's questions in a c̶l̶e̶v̶e̶r̶ lazy way in order to
>    g̶e̶t̶ ̶a̶n̶ ̶a̶d̶v̶a̶n̶t̶a̶g̶e̶ ̶i̶n̶ ̶a̶n̶ ̶a̶r̶g̶u̶m̶e̶n̶t̶ answer in advance with an FAQs
>    section in README.


That random name is a complete mess ;)

**Fence** provides a `fence.core/+++` macro that works like `do`
special form. Wrap it around all your javascript interop forms to
transform them automatically so you don't have to add extern files
manually.

## Usage

Add `fence` to your Clojurescript project:

```cljs
[fence "0.2.0"]
```
Refer to `fence.core/+++` in your namespace:

```cljs
(ns hello
  "Calling property symbols that won't be renamed."
  (:require [fence.core :refer-macros [+++]]))
```

and wrap all renaming-sensitive forms inside `fence.core/+++` which
works like `do` special form.

forms that requires extern | forms that works without extern
-------------------------- | -------------------------------------------
`(. js/foo bar)`           | `(+++ (.. js/foo -bar))`
`(.-boo js/foo)`           | `(+++ (.. js/foo -boo))`
`(.bla js/foo)`            | `(+++ (.. js/foo bla))`
`(.bla js/foo x y z)`      | `(+++ (.. js/foo (bla x y z)))`

Please note `+++` use `clojure.walk` to transform all interop forms
inside its body so the following should work too:

```clj
(defn foo []
  (+++
    (.-bar js/foo)
    (.moreForms js/foo)
    (at (any (level (.execute js/foo))))))
```

## How it  works

Imagine you have this piece of Clojurescript code:

```clj
(ns hello
  "Calling property symbols that WILL be renamed."
  )
(.. js/something -someAttributes aMethod (anotherMethod "arg1" "arg2"))
```

without extern file(s), the above code will end up with this:

```js
something.d.b().c("arg1", "arg2");
```
which will fail to execute.

Instead of writing some extern files manually, just wrap that sensitive
form inside `fence.core/+++` like this:

```clj
(ns hello
  "Calling property symbols that won't be renamed."
  (:require-macros [fence.core :refer [+++]]))

(+++ (.. js/something -someAttributes aMethod (anotherMethod "arg1" "arg2")))
```
and here's (part of) the result:

```js
(function() {
  var a;
  a = something.someAttributes;
  a = a.aMethod.call(a);
  return a.anotherMethod.call(a, "arg1", "arg2");
})();
```

Never be afraid of javascript interop again! ^^

### More in-depth technical details:

`fence.core/+++` transforms all interop forms found in its body code
into `fence.core/dot` and `fence.core/..` macros (`fence` version of `.` and
`..`) which in turn will expand to `aget` forms.

Clojurescript forms   | Output javascript         | Optimized in :advanced mode without extern     | Optimized with extern
----------------------|---------------------------|------------------------------------------------|-------------------
`(. js/foo bar)`      | `foo.bar`                 | renamed to a shorter name like `foo.a`         | `foo.bar`
`(.-boo js/foo )`     | `foo.boo`                 | renamed to a shorter name like `foo.b`         | `foo.boo`
`(.bla js/foo)`       | `foo.bla()`               | renamed to a shorter name like `foo.c()`       | `foo.bla()`
`(.bla js/foo "x" 1)` | `foo.bla("x", 1)`         | renamed to a shorter name like `foo.d("x", 1)` | `foo.bla("x", 1)`


Clojurescript forms             | Output javascript              | Optimized with/without extern
--------------------------------|--------------------------------|-----------------------------
`(+++ (.. js/foo bar))`         | `foo["bar"]`                   | `foo.bar`
`(+++ (.. js/foo -boo))`        | `foo["boo"]`                   | `foo.boo`
`(+++ (.. js/foo bla))`         | `foo["bla"].call(foo)`         | `foo.bla.call(foo)`
`(+++ (.. js/foo (bla "x" 1)))` | `foo["bla"].call(foo, "x", 1)` | `foo.bla.call(foo, "x", 1)`

## FAQs

1. Why no `.` macro?
 - I tried to put the `fence.core`'s equivalent macro version of `.`
   under the same name, but the Clojurescript compiler seems to ignore
   the referred symbol `.` from `fence.core`.

   You may still use `fence.core/dot` in place of `.` but it's kinda
   lengthy and introduces "alien" code which is against `fence`'s
   goals itself.

2. Are there any performance pitfalls?
 - No. Google Closure compiler will replace string versions of
   properties to symbol versions so the final javascript will be the
   same as its counterpart using *extern* files.

3. Isn't it better to write code like this? `(.. foo (a-method arg-1 arg-2))`
 - `fence` will never camelize symbols for you because it aims to be
   100% compatible with `clojure.core/..`. Once Clojurescript compiler
   is smart enough to be able to automatically produce extern files
   itself, people can come back to `clojure.core/..` from
   `fence.core/..` with no extra effort.

## Copyright

Copyright ©2014 Hoang Minh Thang

Distributed under the Eclipse Public License, the same as Clojure. Please see the `epl-v10.html` file at the top level of this repo.

This repository, Scala HTML, is an experiment on Scala's language power.
It provides a DSL to generate plain text html and compile-time linting by abusing type system.
Scala HTML aims at both syntax level flexibility and type level safety, at the cost of poor runtime performance and compiling time.

Feature:
===

1. Nothing but Plain Scala:

  ```scala
  div.test.heheh |(
    p.testCls(id:="heh") | (
      div.child | (
        p.whathaha | a()
  )))
  ```

2. Class shortcut:

  `div.test` will render `<div class="test"></div>`

3. Attribute:

  Every attribute is an object that has method `:=` and that can be applied to tags.
  `form(action:="test")`, `div(id:="hehe")` and implicit conversion for `(String, String)` Pair like `div("data-convert":="implicit")`

4. Linting

  User can only pass elements of specific tag to another tag.
  ```scala
    val a = a() | p()
    // won't compile because `a` accept inline element
    // not true in html5 though, just for example
    val ul = ul() | li()
  ```

4. Type Level Element Finder

  Scala HTML has a `jQ` object mocking jQuery.

  ```scala
  val k =
    div.test.heheh |(
      p.testCls(id:="heh") | (
        div.child("data-test":="h") | (
          p.whathaha | a()
    )))

  val pElement = jQ(k).has[P]
  // return an P[Div[P[A[Nothing]]]]
  ```

  However, element finding is done in compile-time. Finding a non-existing element will cause compiling failure.

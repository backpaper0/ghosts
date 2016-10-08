class: center, middle

# 詳解 ::

---

### 自己紹介

* うらがみ⛄️
* 大阪でSIerをしているJavaプログラマ
* Scalaは2010年〜2012年頃にコップ本とか読んで学んでいました

---

### 二つの`::`

Scalaでは

```scala
val xs = "a" :: "b" :: Nil
```

とするとリストを構築できます。

---

### 二つの`::`

また、

```scala
xs match {
  case h :: t => ...
}
```

とするとパターンマッチでリストを先頭の要素とそれ以外に分割できます。

---

### 二つの`::`

このリストの**構築**と**分解**に用いる二つの`::`はScalaの文法上どのような扱いになっているのか、一緒に見ていきましょう！

---

class: center, middle

## リストを構築する`::`

---

### リストを構築する`::`

リストを構築するコードを再掲します。

```scala
val xs = "a" :: "b" :: Nil
```

このコードはScalaコンパイラにはどのように解釈されているのでしょうか？

---

### リストを構築する`::`

次のコードを`sample1.scala`という名前で保存して、

```scala
class Sample1 {
  def toList = "a" :: "b" :: Nil
}
```

`scalac -Xprint:constructors sample1.scala`してみました。

---

### リストを構築する`::`

```none
package <empty> {
  class Sample1 extends Object {
    def toList(): List = {
      <synthetic> <artifact> val x$2: String = "a";
      {
  <synthetic> <artifact> val x$1: String = "b";
  immutable.this.Nil.::(x$1)
}.::(x$2)
    };
    def <init>(): Sample1 = {
      Sample1.super.<init>();
      ()
    }
  }
}
```

---

### リストを構築する`::`

該当箇所を抜粋。

```none
      <synthetic> <artifact> val x$2: String = "a";
      {
  <synthetic> <artifact> val x$1: String = "b";
  immutable.this.Nil.::(x$1)
}.::(x$2)
```

`Nil`の`::`メソッドに`"b"`、`"a"`の順番に渡しています。

---

### リストを構築する`::`

つまり、

```scala
"a" :: "b" :: Nil
```

は

```scala
Nil.::("b").::("a")
```

と解釈されていました……なぜ？？？

---

### リストを構築する`::`

Scalaでは単一引数のメソッド呼び出しは`.`を省略できます。

```scala
x.and(y) //これを

x and y //こう書ける
```

このように`.`を省略したメソッド呼び出しは**中置記法**（ちゅうちきほう）と言います。

---

### リストを構築する`::`

そして、中置演算子の名前の末尾が`:`の場合は**右結合**になります。

```scala
val xs = Seq(2, 3)

xs :+ 4 //これは左結合。xs.:+(4)と同じ

1 +: xs //これは右結合。xs.+:(1)と同じ
```

---

### リストを構築する`::`

以上のことから次のコードは

```scala
val xs = "a" :: "b" :: Nil
```

中置記法で右結合になっているので

```scala
val xs = Nil.::("b").::("a")
```

と解釈されるのです。

---

class: center, middle

## リストを分解する`::`

---

### リストを分解する`::`

次はパターンマッチでリストを分解する`::`です。

```scala
xs match {
  case h :: t => ...
}
```

そもそもパターンマッチで値を抽出する場合、どういうクラス定義になっているのでしょうか？

---

### リストを分解する`::`

例として、平面座標を表すクラスを定義して、

```scala
class Point(val x: Int, val y: Int)
```

`new Point(12, 34)`として作成した平面座標からパターンマッチでx座標、y座標を取り出すための定義をしましょう。

---

### リストを分解する`::`

まず、`Point`のコンパニオンオブジェクトを定義します。

```scala
object Point
```

---

### リストを分解する`::`

次に`Point`を受け取り`Some`で包んだ`(Int, Int)`を返すメソッドを**`unapply`**という名前で定義します。

```scala
object Point {
  def unapply(p: Point) = Some((p.x, p.y))
}
```

これだけで`Point`の値をパターンマッチで取り出せるようになります。

---

### リストを分解する`::`

```scala
val p = new Point(12, 34)

p match {
  case Point(x, y) => println("x, y = " + x + ", " + y)
}
```

```none
x, y = 12, 34
```

値を分解できました。

---

### リストを分解する`::`

蛇足ですが、`unapply`と対になるようなメソッドとして**`apply`**があります。

```scala
object Point {
  def apply(x: Int, y: Int) = new Point(x, y)
}
```

コンパニオンオブジェクトに`apply`メソッドを定義しておくと、

---

### リストを分解する`::`

値の構築に`new`が不要になります。

```scala
val p = Point(12, 34)
```

`unapply`メソッドも合わせて定義しておけば値の構築と分解を同じ表現（書き方）で行えます。

---

### リストを分解する`::`

```scala
//applyメソッドが使用される
val p = Point(12, 34)

p match {
  //unapplyメソッドが使用される
  case Point(x, y) => x + ", " + y
}
```

値の構築・分解の表現がどちらも`Point(x, y)`ですね。

---

### リストを分解する`::`

さて、パターンマッチで値を抽出する例として座標クラスを作成し、そのコンパニオンオブジェクトを作って、`unapply`メソッドと（ついでに）`apply`メソッドを定義しました。

これらをもっと簡単に行う方法が`case class`です。

---

### リストを分解する`::`

`case class`を使えばコンパイル時に自動でコンパニオンオブジェクトを作成して`unapply`メソッドや`apply`メソッドを定義してくれます。
（他にも`copy`や`hashCode`、`equals`、`toString`なども）

```scala
case class Point(x: Int, y: Int)
```

閑話休題。

---

### リストを分解する`::`

ここまででパターンマッチで値を取り出すためのクラス定義は分かりました。

リストを分解する`::`も同じように`unapply`メソッドが定義されているんだな、と想像して貰えていると思います。

---

### リストを分解する`::`

ただ、パターンマッチは次のように書くはずです。

```scala
xs match {
  case ::(h, t) => ...
}
```

---

### リストを分解する`::`

なぜ次のような書き方ができるのでしょうか？

```scala
xs match {
  case h :: t => ...
}
```

---

### リストを分解する`::`

実はパターンマッチにおける中置記法`p op q`は`op(p, q)`と同じ意味なのです。

```scala
p match {
  case x Point y => ...
  //case Point(x, y) => ... と同じ意味
}
```

---

### リストを分解する`::`

以上のことから次のコードは

```scala
xs match {
  case x :: Nil => ...
}
```

次のように解釈されるのです。

```scala
xs match {
  case ::(x, Nil) => ...
}
```

---

class: center, middle

## まとめ

---

### まとめ

* リストを構築する`::`は`Nil`のメソッドであり、名前の末尾が`:`で終わるメソッドで中置記法をした場合は右結合になるので、`"a" :: "b" :: Nil`と書ける
* リストを分解する`::`は`case class`であり、`op(p, q)`というパターンマッチは`p op q`とも書けるので、`case h :: t => ...`と書ける

---

### まとめ

今回、値の構築と分解に同じ表現を使えるように仕様が工夫されていることを見ましたが、Scalaは他にも色々な工夫がなされています。

なので、仕様を気にしなければ取っ付きやすいし、仕様を学んで「だからこういう表現ができるのか！」と感動できるタイプの方でも楽しめるのではと思っています。（個人の感想です）

---

### まとめ

例えば`Map(k -> v)`と書いて`Map`を構築するときの`->`や、`Predef.scala`に定義されている`=:=`も面白かった記憶があります。

気になった方は調べてみてください！

---

### まとめ

ちなみに`unapply`メソッドや`apply`はコンパニオンオブジェクトじゃなくても定義できます。

```scala
//Pointのコンパニオンオブジェクト以外にunapplyを定義した例
object and {
  def unapply(p: Point) = Some((p.x, p.y))
}

Point(12, 34) match {
  case x and y => ...
}
```

---

### 参考資料

* [List.::](https://github.com/scala/scala/blob/v2.11.8/src/library/scala/collection/immutable/List.scala#L111)
* [case class ::](https://github.com/scala/scala/blob/v2.11.8/src/library/scala/collection/immutable/List.scala#L439)
* [6.12.3 Infix Operations](http://scala-lang.org/files/archive/spec/2.11/06-expressions.html#infix-operations)
* [8.1.8 Extractor Patterns](http://scala-lang.org/files/archive/spec/2.11/08-pattern-matching.html#extractor-patterns)
* [8.1.10 Infix Operation Patterns](http://scala-lang.org/files/archive/spec/2.11/08-pattern-matching.html#infix-operation-patterns)

---

## この資料について

* Author: [@backpaper0](https://github.com/backpaper0)
* License:  [The MIT License](https://opensource.org/licenses/MIT)

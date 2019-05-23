class: center, middle

# Scalaで足し算する<br>（ただし型レベルで）

---

## 目次

- 真偽値を定義する（ただし型レベルで）
- 論理演算をする（ただし型レベルで）
- 整数を定義する（ただし型レベルで）
- インクリメントをする（ただし型レベルで）
- 足し算をする（ただし型レベルで）

---

## 本日のゴール

このコードをコンパイル＆実行する

```scala
toInt[_1 + _2 + _3]
```

--
count: false
- `_1` ← 型
- `_2` ← 型
- `_3` ← 型
- `+` ← 型

これらは`toInt`関数にバインドされた型パラメーター

---

class: center, middle

# 真偽値を定義する<br>（ただし型レベルで）

---

## 真偽値を定義する（ただし型レベルで）

まず真偽値を定義する。
簡単。

```scala
trait Bool

trait True extends Bool

trait False extends Bool
```

ただ、型のままだと動作確認できないので値に変換したい。

---

## 真偽値を定義する（ただし型レベルで）

こんな感じの`trait`を定義して……

```scala
trait ToBool[A <: Bool] {
  def apply(): Boolean
}
```

???

BoolのサブタイプAを型変数に取るToBoolトレイトを定義します。

Aは使われないのでファントムタイプというやつですね。

---

## 真偽値を定義する（ただし型レベルで）

こんな感じの`implicit val`を定義して……

```scala
implicit val toTrue = new ToBool[True] {
  def apply() = true
}

implicit val toFalse = new ToBool[False] {
  def apply() = false
}
```

???

それからこんな感じのimplicit valを定義します。

---

## 真偽値を定義する（ただし型レベルで）

こんな感じの関数を定義すれば……

```scala
def toBool[A <: Bool](implicit f: ToBool[A]) = f()
```

???

5分

ToBool型のimplicit parameterを取るtoBool関数を定義します。

---

## 真偽値を定義する（ただし型レベルで）

型を値に変換できる。

```scala
assert(toBool[True] == true)

assert(toBool[False] == false)
```

???

このように書けばBool型をBooleanの値へ変換できます。

---

```scala
trait Bool
trait True extends Bool
trait False extends Bool

trait ToBool[A <: Bool] {
  def apply(): Boolean
}
implicit val toTrue = new ToBool[True] {
  def apply() = true
}
implicit val toFalse = new ToBool[False] {
  def apply() = false
}

def toBool[A <: Bool](implicit f: ToBool[A]) = f()
assert(toBool[True] == true)
assert(toBool[False] == false)
```

???

最初なので全体を見てみましょう。

ここまでは普通のScalaを逸脱していないので何をやっているのか理解していただけると思っていますがいかがでしょうか。

---

class: center, middle

# 論理演算をする<br>（ただし型レベルで）

---

## 論理演算をする（ただし型レベルで）

次の論理演算を定義してみる。

- `!`
- `&&`
- `||`

???

否定、論理積、論理和を型で表現してみましょう。

---

## 論理演算をする（ただし型レベルで）

まずは否定を実装する。

```scala
trait Bool {
  type Not <: Bool
}
```

次のように使う。

```scala
True#Not
```

???

Boolにネストした型、Notを宣言します。
NotはBoolのサブタイプです。

---

## 論理演算をする（ただし型レベルで）

`True`の否定は`False`、`False`の否定は`True`。

```scala
trait True extends Bool {
  type Not = False
}
trait False extends Bool {
  type Not = True
}
```

---

## 論理演算をする（ただし型レベルで）

```scala
assert(toBool[True#Not] == false)

assert(toBool[False#Not] == true)

assert(toBool[True#Not#Not] == true)
```

???

Booleanに変換して確認してみましょう。

---

## 論理演算をする（ただし型レベルで）

より演算子っぽい物を導入。

```scala
object Bools {
  type ![A <: Bool] = A#Not
}
```

???

このままだと演算っぽくないのでエクスクラメーションマークを使って否定演算子を型で表現します。

---

## 論理演算をする（ただし型レベルで）

```scala
import Bools._

assert(toBool[![True]] == false)

assert(toBool[![False]] == true)

assert(toBool[![![True]]] == true)
```

???

だいぶ否定演算子っぽくなりましたね。

---

## 論理演算をする（ただし型レベルで）

次は論理積と論理和。

```scala
trait Bool {
  type Not <: Bool
  type And[A <: Bool] <: Bool
  type Or[A <: Bool] <: Bool
}
```

次のように使う

```scala
True#And[False]
```

???

論理積と論理和はBoolのサブタイプを型引数に取ります。
論理積と論理和自体もBoolのサブタイプです。

---

## 論理演算をする（ただし型レベルで）

```scala
trait True extends Bool {
  type Not = False
  type And[A <: Bool] = A
  type Or[A <: Bool] = True
}

trait False extends Bool {
  type Not = True
  type And[A <: Bool] = False
  type Or[A <: Bool] = A
}
```

???

Trueの論理積は自身がTrueなので型引数Aを返しています。

……これ、型エイリアスなので「返す」と表現するのはおかしいんだろうと思いますが、ここでは「返す」と表現しますね。

Trueの論理和は自身がTrueなので型引数AがTrueであろうとFalseであろうとTrueですよね。
なのでTrueを返しています。

同じようにFalseの論理積と論理和も定義します。

---

## 論理演算をする（ただし型レベルで）

ここでもより演算子っぽい物を導入。

```scala
object Bools {
  type ![A <: Bool] = A#Not
  type &&[A <: Bool, B <: Bool] = A#And[B]
  type ||[A <: Bool, B <: Bool] = A#Or[B]
}
```

???

型引数AとBを取って、先ほど定義したAndやOrに置き換えています。

---

## 論理演算をする（ただし型レベルで）

```scala
import Bools._

assert(toBool[&&[True, False]] == false)

assert(toBool[||[True, False]] == true)
```

???

Booleanに変換して動作確認してみましょう。

---

## 論理演算をする（ただし型レベルで）

ここでScalaの文法をひとつ学ぶ。

型引数を2つ取る型があるとする。

```scala
trait to[A, B]
```

この場合、中置演算子のように書ける。

```scala
def f(t: to[Int, String]) = ???

def f(t: Int to String) = ??? //中置演算子のように書ける
```

???

10分

---

## 論理演算をする（ただし型レベルで）

つまり、

```scala
toBool[&&[True, False]]
toBool[||[True, False]]
```

↑は↓のように書ける。

```scala
toBool[True && False]
toBool[True || False]
```

- cf. `scala.Predef`の`=:=`や`<:<`

???

先ほど定義した論理積と論理和も中置演算子のように書けます。

この文法を使っているものとしてPredefに定義されているイコールコロンイコールや小なりコロン小なりが挙げられます。
実際の読み方は分からない……

---

## 論理演算をする（ただし型レベルで）

これで真偽値と論理演算を型で表現できた。

`if-then-else`を表現するのも面白いのでチャレンジしてみてください！

---

class: center, middle

# 整数を定義する<br>（ただし型レベルで）

---

## 整数を定義する（ただし型レベルで）

整数を型で表現するのは真偽値より少しだけややこしい。

--
count: false

```scala
trait Nat
trait _0 extends Nat
trait _1 extends Nat
trait _2 extends Nat
.
.
.
trait _99 extends Nat
```

???

ベースとなるNatトレイトを定義して、Natのサブタイプとして0、1、2から99まで定義します。

--
count: false

必要となる値を全て定義なんてやってられない。

???

なんてことはやってられないですよね。

---

## 整数を定義する（ただし型レベルで）

`0`より大きい値について考えてみる。

???

0はNatのサブタイプとして定義するとして、0より大きい値について考えてみましょう。

--
count: false
- `1 = 0 + 1` 
???
1は0 + 1と言えますよね。
--
count: false
- `2 = 1 + 1` 
???
2は1 + 1。
--
count: false
- `3 = 2 + 1` 
--
count: false
- `4 = 3 + 1` 
--
count: false
- `5 = 4 + 1` 
--
count: false

値に`1`を足すと次の値になる。

???

つまり0より大きい値は1を足して次の値にすることで求められるんですね。

---

## 整数を定義する（ただし型レベルで）

つまり整数は次のように表せられる。

--
count: false

```scala
trait Nat

trait _0 extends Nat

trait Suc[A <: Nat] extends Nat
```

※ちなみに今回は負の値については考慮しない

c.f.後者関数

???

0は単にNatのサブタイプ。

0より大きい値はNatのサブタイプAを型引数に取るSucトレイトで表現しています。
これは「Aの次の値」を表しています。

Sucはsuccessor functionの略です。
日本語だと後者関数と言います。
これはペアノの公理で自然数を定義するのに使われるものです。

---

## 整数を定義する（ただし型レベルで）

1〜5は`Suc`を使って次のように定義できる。

```scala
type _1 = Suc[_0]

type _2 = Suc[_1]

type _3 = Suc[_2]

type _4 = Suc[_3]

type _5 = Suc[_4]
```

---

## 整数を定義する（ただし型レベルで）

`Int`値に変換する関数を定義する。

```scala
trait ToInt[A <: Nat] {
  def apply(): Int
}

def toInt[A <: Nat](implicit f: ToInt[A]) = f()

implicit val toZero = new ToInt[_0] {
  def apply() = 0
}
```

???

真偽値のときと同じくIntの値に変換する関数を定義しましょう。

ToIntトレイトとそれをimplicit parameterに取るtoInt関数、それから0を変換するimplicit valを定義します。

---

## 整数を定義する（ただし型レベルで）

`Suc`を`Int`に変換する関数は少しだけ複雑。

```scala
implicit def toSuc[A <: Nat](implicit f: ToInt[A]) =
  new ToInt[Suc[A]] {
    def apply() = f() + 1
  }
```

???

SucをIntに変換する関数は少しだけ複雑です。

toSuc関数はNatのサブタイプAを型引数に取ります。
またToInt Aをimplicit parameterに取ります。
返されるToIntインスタンスはSuc Aを型引数に取っていますね。

つまりSucを剥がしながら再帰的にimplicit parameterが適用され、Sucを剥がすたびに1が加算されるので正しくIntの値に変換できる、という感じです。

---

## 整数を定義する（ただし型レベルで）

これで整数を型で表現できた。

```scala
assert(toInt[_0] == 0)

assert(toInt[_1] == 1)

assert(toInt[_2] == 2)

assert(toInt[Suc[_2]] == 3)
```

???

これで整数を型で表現でき、実行して検証することができました。

---

class: center, middle

# インクリメントをする<br>（ただし型レベルで）

---

## インクリメントをする（ただし型レベルで）

いきなり足し算を定義するのは難しい。

まずはインクリメントを定義する。

```scala
trait Nat {
  type Inc <: Nat
}
```

???

インクリメントは真偽値の否定演算と同じく単項演算ですね。

NatのサブタイプであるInc型を宣言します。

---

## インクリメントをする（ただし型レベルで）

`Suc[A]`は`A`に`1`を足したものなので、インクリメントそのもの。

```scala
trait _0 extends Nat {
  type Inc = Suc[_0]
}

trait Suc[A <: Nat] extends Nat {
  type Inc = Suc[Suc[A]]
}
```

???

先ほどSucは型引数に1加えたものを表すと述べました。

0をインクリメントするとSuc 0になります。
Suc AをインクリメントするとSuc Suc Aになります。

これでインクリメントを表現できました。
簡単ですよね？

---

## インクリメントをする（ただし型レベルで）

より演算子っぽくなるいつものやつを導入。

```scala
object Nats {
  type ++[A <: Nat] = A#Inc
}
```

???

15分

真偽値のときと同じように演算子っぽい型を定義します。

---

## インクリメントをする（ただし型レベルで）

インクリメントを型で表現できた。

```scala
import Nats._

assert(toInt[++[_0]] == 1)

assert(toInt[++[_1]] == 2)

assert(toInt[++[_2]] == 3)
```

---

class: center, middle

# 足し算をする<br>（ただし型レベルで）

---

## 足し算をする（ただし型レベルで）

いよいよ足し算を定義する。

```scala
trait Nat {
  type Inc <: Nat
  type Add[A <: Nat] <: Nat
}
```

次のように使う。

```scala
_2#Add[_3]
```


???

AddはNatのサブタイプAを型引数に取ります。

---

## 足し算をする（ただし型レベルで）

`_0#Add`は簡単。

`0 + n = n`、つまり足される数を返せば良い。

```scala
trait _0 extends Nat {
  type Inc = Suc[_0]
  type Add[A <: Nat] = A
}
```

???

0 + n = nですよね。

ですので0は足される数、つまりAを返すだけですね。

---

## 足し算をする（ただし型レベルで）

`Suc#Add`はちょっと難しい。

```scala
trait Suc[A <: Nat] extends Nat {
  type Inc = Suc[Suc[A]]
  type Add[B <: Nat] = どうすれば良い？
}
```

???

どうでしょうか？
みなさん、どうすればいいかおわかりになりますか？

---

## 足し算をする（ただし型レベルで）

`2 + 1`を考えてみる。

???

2 + 1を考えてみましょう。

--
count: false
- `2 + 1`
--
count: false
- `(1 + 1) + 1`
???
2 + 1は、括弧開く・いち・足す・いち・括弧閉じる・足す・いち、ですよね。
--
count: false
- `1 + (1 + 1)`
???
括弧の位置を変えて、いち・足す・括弧開く・いち・足す・いち・括弧閉じる、とも言えます。
--
count: false
- `1 + 2`
???
括弧内を足すと1 + 2と表現できますね。
--
count: false
- `(0 + 1) + 2`
???
同じように、...
--
count: false
- `0 + (2 + 1)`
--
count: false
- `0 + 3`
--
count: false
- `3`

---

## 足し算をする（ただし型レベルで）

つまり左の値が`0`になるまでデクリメントしながら右の値をインクリメントすれば良い。

--
count: false

インクリメントは既に実装ずみ。

--
count: false

`A`をインクリメントすれば`Suc[A]`になる。

--
count: false

逆に考えると`Suc[A]`をデクリメントすると`A`になる。

---

## 足し算をする（ただし型レベルで）

以上のことから`Suc#Add`は次のように定義できる。

```scala
trait Suc[A <: Nat] extends Nat {
  type Inc = Suc[Suc[A]]
  type Add[B <: Nat] = A#Add[B#Inc]
}
```

???

元の数がSuc AなのでデクリメントすればAですね。

それにBをインクリメントしたものをAddするわけです。

これで足し算の左の値をデクリメントしながら右の値をインクリメントしていることになりますよね。

---

## 足し算をする（ただし型レベルで）

仕上げにいつもの便利なやつを定義すれば……

```scala
object Nats {
  type ++[A <: Nat] = A#Inc
  type +[A <: Nat, B <: Nat] = A#Add[B]
}
```

???

演算子っぽいものを定義しましょう。
これも中置記法で使う想定です。

---

## 足し算をする（ただし型レベルで）

足し算を型レベルで表現できた。

```scala
import Nats._

assert(toInt[_0 + _1] == 1)

assert(toInt[_3 + _2] == 5)

assert(toInt[_1 + _2 + _3] == 6)
```

???

これで足し算を型レベルで表現できました。

やったー！

---

class: center, middle

# まとめ

---

## まとめ

- Scalaは型で論理値や整数を表現できる（できるからといってやって良いかどうかは別の話）
- 型で表現した論理値や整数はimplicit parameterで実行時の値に変換して確認ができる
- 二つの型引数を取る型は中置演算子のように書ける仕様を利用して論理演算や足し算を表現
- 掛け算、引き算、割り算にチャレンジしてみよう！

class: center, middle

# GroovyでFizzBuzz<br>やってみた

@backpaper0

---

class: center, middle

## みなさん<br>Groovy<br>を知っていますか？

---

class: center, middle

## それでは<br>ラムダ計算<br>を知っていますか？

---

## Wikipediaより

> ラムダ計算は、計算模型のひとつで、計算の実行を関数への引数の評価と適用としてモデル化・抽象化した計算体系である。

---

## ラムダ計算の構成要素

* 識別子
* ラムダ抽象
* 関数適用

---

## ラムダ計算の構成要素

* `x`（識別子）
* `λx.x`（ラムダ抽象）
* `fx`（関数適用）

---

## すべては関数

* クラスなし
* 文字列なし
* 数値なし
* 真偽値なし

---

## 数値の表現

* チャーチ数
* 関数を適用した回数を数とみなす

???

数値も無いのは不便なので関数で数値を表現する。

---

class: short-code

## チャーチ数で0

```lambda
λf.λx.x
```

???

fを受け取ってxを受け取ってxを返す関数を返す。

fとxを受け取ってxを返す関数を1引数関数にしたものと思ってもらっても良さそう。
1引数関数にすることをカリー化という。

---

class: short-code

## チャーチ数で1

```lambda
λf.λx.fx
```

???

fを1回適用している

---

class: short-code

## チャーチ数で2

```lambda
λf.λx.f(fx)
```

???

fを2回適用している

---

class: short-code

## チャーチ数で3

```lambda
λf.λx.f(f(fx))
```

???

fを3回適用している

---

class: short-code

## チャーチ数で4

```lambda
λf.λx.f(f(f(fx)))
```

???

fを4回適用している

---

class: short-code

## チャーチ数で5

```lambda
λf.λx.f(f(f(f(fx))))
```

???

fを5回適用している

---

class: short-code

## インクリメントする関数

```lambda
λn.λf.λx.f(nfx)
```

---

class: short-code

## 1をインクリメント

インクリメント関数を`1`に適用してみる

```lambda
λn.λf.λx.f(nfx)
```

まず`n`を`λf.λx.fx`で置き換え（ベータ簡約）

---

class: short-code

## 1をインクリメント

まず`n`を`λf.λx.fx`で置き換え（ベータ簡約）

```lambda
λf.λx.f((λf.λx.fx)fx)
```

自由変数（関数スコープ外の変数）と束縛変数（関数の引数）の名前がかぶっているのでアルファ変換する

---

class: short-code

## 1をインクリメント

アルファ変換

```lambda
λf.λx.f((λg.λy.gy)fx)
```

`f`に`(λg.λy.gy)`を適用する

---

class: short-code

## 1をインクリメント

`f`に`(λg.λy.gy)`を適用する

```lambda
λf.λx.f((λy.fy)x)
```

`x`に`(λy.fy)`を適用する

---

class: short-code

## 1をインクリメント

これ以上の簡約はできない

```lambda
λf.λx.f(fx)
```

`2`を表すチャーチ数になった

---

### インクリメントの関数はわかった

でもラムダ計算に慣れていないと<br>読みづらい……

---

## そこでGroovy

JVM界隈のプログラマがラムダ計算をエミュレーションするなら普通に考えるとGroovyになる

---

### ラムダ計算をGroovyでエミュレート

インクリメントする関数は……

```lambda
λn.λf.λx.f(nfx)
```

--

こう書ける

```groovy
{ n -> { f -> { x -> f(n(f)(x)) }}}
```

--

読める！！！

???

あとGroovyを使うと式を変数に入れてあとで使うことができるので書きやすい。
ラムダ計算は関数の引数にはできるけどローカル変数とかクラスのフィールドみたいなものはない。

---

## Groovyで普通にFizzBuzz

```groovy
def ret = (1..100).collect { i ->
    if (i % 15 == 0) 'FizzBuzz'
    else if (i % 3 == 0) 'Fizz'
    else if (i % 5 == 0) 'Buzz'
    else i.toString()
}
println(ret)
```

---

## FizzBuzzを構成する要素

* 数値
* 範囲（`1..100`）
* 射影（`collect`）
* 条件分岐（`if-else`）
* 剰余算（`%`）
* 同値検証（`==`）
* 文字列（`'FizzBuzz'`）
* 数値を文字列へ変換（`i.toString()`）

???

それぞれラムダ計算で実装すれば、ラムダ式でFizzBuzzを実装できる。

大きな問題は小さく分割する。

---

## 例：条件分岐

```groovy
TRUE  = { t -> { f -> t }}
FALSE = { t -> { f -> f }}
IF = { c -> { t -> { e -> c(t)(e) }}}
```

---

### 全部説明するには時間が足りない

[以前作ったFizzBuzzがこちら](https://gist.github.com/backpaper0/62e0d1caca72fb0c07ca/)

---

class: center, middle

## デモ

---

## まとめ

* ラムダ計算は関数脳が鍛えられる
* Groovyのクロージャーは括弧省略できるようになってほしい

---

class: center, middle

### 次回

「ScalaでFizzBuzzやってみた」

---

## この資料について

* Author: [@backpaper0](https://github.com/backpaper0)
* License:  [The MIT License](https://opensource.org/licenses/MIT)


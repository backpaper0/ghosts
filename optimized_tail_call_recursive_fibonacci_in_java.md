# Javaでフィボナッチ数を求めるメソッドを末尾再帰最適化する

[@backpaper0](https://twitter.com/backpaper0)



## フィボナッチ数とは

n番目の項をF<sub>n</sub>とすると

* F<sub>0</sub> = 0
* F<sub>1</sub> = 1
* F<sub>n + 2</sub> = F<sub>n</sub> + F<sub>n - 1</sub> (n >= 0)

となるような数列

```
0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, ...
```



## まずは普通にフィボナッチ数を求めるメソッドを書く



```java
int fib(int n) {
    if (n == 0) { return 0; }
    if (n == 1) { return 1; }
    return fib(n - 1) + fib(n - 2);
}

//動作確認用のmainメソッド。以降は省略します。
public static void main(String[] args) {
    try (Scanner s = new Scanner(System.in)) {
        long n = s.nextLong();
        long ret = new Fib().fib(n);
        System.out.printf("fib(%d) = %d%n", n, ret);
    }
}
```



## ``n = 10`` で実行



実行結果

```
fib(10) = 55
```



## 次は ``n = 100`` で実行



## (:3[＿]



## 返って来ない。。。



## 計算量がものすごいので処理が終わらない！



## メモ化する

※今回のテーマには関係無いけどメソッドの実行が終わらないのでメモ化します



```java
Map<Integer, Integer> values = new HashMap<>();
int fib(int n) {
    if (n == 0) { return 0; }
    if (n == 1) { return 1; }
    Integer m = values.get(n);
    if (m != null) { return m; }
    m = fib(n - 1) + fib(n - 2);
    values.put(n, m);
    return m;
}
```



## あらためて ``n = 100`` で実行



実行結果

```
fib(100) = -1869596475
```



## 桁溢れた。。。



## ``BigInteger``を使う



```java
Map<BigInteger, BigInteger> values = new HashMap<>();
BigInteger zero = BigInteger.ZERO;
BigInteger one = BigInteger.ONE;
BigInteger two = one.add(one);
BigInteger fib(BigInteger n) {
    if (n.compareTo(zero) == 0) { return zero; }
    if (n.compareTo(one) == 0) { return one; }
    BigInteger m = values.get(n);
    if (m != null) { return m; }
    m = fib(n.subtract(one)).add(fib(n.subtract(two)));
    values.put(n, m);
    return m;
}
```



## つらみ溢れるコードになってきた。。。

演算子オーバーロードしたい！！！

まあそれはさておき、



## 再度 ``n = 100`` で実行



実行結果

```
fib(100) = 354224848179261915075
```



## 無事に実行結果を得られた



## 次は ``n = 10000`` で実行



```
Exception in thread "main" java.lang.StackOverflowError
	at java.math.BigInteger.subtract(BigInteger.java:1425)
	at Fib.fib(Fib.java:24)
	at Fib.fib(Fib.java:24)
	at Fib.fib(Fib.java:24)
	at Fib.fib(Fib.java:24)
	at Fib.fib(Fib.java:24)
	at Fib.fib(Fib.java:24)
	at Fib.fib(Fib.java:24)
(以下略)
```



## スタックオーバーフロー！！！



## スタックを消費しない形式に変形する

1. 末尾再帰呼び出しに変形して
2. 末尾再帰呼び出しを最適化



## まずは簡単な例で考えてみる



## ``1`` から ``n`` まで足す再帰関数

```java
long sum(long n) {
    if (n == 0) { return 0; }
    return n + sum(n - 1);
}
```

やはり ``n = 10000`` ぐらいでスタックオーバーフロー

```
Exception in thread "main" java.lang.StackOverflowError
    at Sum.sum(Sum.java:11)
    at Sum.sum(Sum.java:11)
    at Sum.sum(Sum.java:11)
(以下略)
```



## 末尾再帰呼び出しに変形

```java
long sum(long n) { return sum(n, 0); }
long sum(long n, long m) {
    if (n == 0) { return m; }
    return sum(n - 1, n + m);
}
```

この時点ではまだスタックオーバーフロー



## 末尾再帰呼び出しを最適化する

Javaコンパイラは末尾再帰呼び出しの最適化を行わないので
[Javaによる関数型プログラミング](http://www.oreilly.co.jp/books/9784873117041/)
の7章を参考に末尾再帰呼び出しを手動で最適化する



## 末尾再帰呼び出し用のクラスを作成する

```java
public class TailRec<T> {
    private final Supplier<TailRec<T>> next;
    private final boolean done;
    private final T result;
    private TailRec(Supplier<TailRec<T>> next, boolean done, T result) {
        this.next = next;
        this.done = done;
        this.result = result;
    }
    public T get() {
        return Stream.iterate(this, a -> a.next.get())
                     .filter(a -> a.done)
                     .map(a -> a.result)
                     .findFirst()
                     .get();
    }
    public static <T> TailRec<T> call(Supplier<TailRec<T>> next) {
        return new TailRec<>(next, false, null);
    }
    public static <T> TailRec<T> done(T result) {
        return new TailRec<>(() -> null, true, result);
    }
}
```



## ``TailRec``を使ってsum関数の末尾再帰呼び出しを最適化する

```java
long sum(long n) { return sum(n, 0).get(); }
TailRec<Long> sum(long n, long m) {
    if (n == 0) { return TailRec.done(m); }
    return TailRec.call(() -> sum(n - 1, n + m));
}
```

戻り値を``TailRec``にして``return``してる所を``TailRec.call``と``TailRec.done``で包んだだけ



## ``TailRec``使用前後のコードを並べて見てみる

```java
long sum(long n) { return sum(n, 0); }
long sum(long n, long m) {
    if (n == 0) { return m; }
    return sum(n - 1, n + m);
}
```

```java
long sum(long n) { return sum(n, 0).get(); }
TailRec<Long> sum(long n, long m) {
    if (n == 0) { return TailRec.done(m); }
    return TailRec.call(() -> sum(n - 1, n + m));
}
```



## ``n = 10000`` で実行してみると

無事に結果を得られた！

```
sum(10000) = 50005000
```



## 何がどうなってスタックが消費されないようになっているのか？



### ``TailRec``を使わない例だと再帰呼び出しをすることでどんどんスタックが消費されていった

```java
long sum(long n) { return sum(n, 0); }
long sum(long n, long m) {
    if (n == 0) { return m; }
    //↓再帰呼び出しでスタック消費
    return sum(n - 1, n + m);
}
```



### この再帰呼び出し部分を``TailRec.call``でラップして返し、実行は呼び出し元に任せる事でスタックを消費しなくなった

```java
long sum(long n) { return sum(n, 0).get(); }
TailRec<Long> sum(long n, long m) {
    if (n == 0) { return TailRec.done(m); }
    //↓再帰呼び出しをTailRecで表現して呼び出し元に返す
    //↓ここではメソッド実行はしていない
    return TailRec.call(() -> sum(n - 1, n + m));
}
```



### また、再帰の終了部分は``TailRec.done``でラップしている

```java
long sum(long n) { return sum(n, 0).get(); }
TailRec<Long> sum(long n, long m) {
    //↓再帰の終了部分
    if (n == 0) { return TailRec.done(m); }
    return TailRec.call(() -> sum(n - 1, n + m));
}
```



### 呼び出し元では``TailRec.get``を使って再帰呼び出し部分の実行と再帰呼び出し終了時の値の取り出しを行っている

```java
//nextがTailRec.callでラップした部分
//Stream.iterateでTailRecを次々に生成 = 再帰呼び出し
Stream.iterate(this, a -> a.next.get())
      .filter(a -> a.done) //再帰呼び出し終了部分だけに絞る
      .map(a -> a.result) //値を取り出す
      .findFirst() //再帰呼び出し終了部分はひとつだけで良い
      .get();
```



## 処理の流れのイメージ

* ``sum``を実行して``TailRec.call``で``TailRec``を返す
* ``Stream.iterate``で``TailRec.next.get``を実行する
* これを再帰呼び出しの分だけ繰り返す
* 再帰呼び出しが終了したら``TailRec.done``で``TailRec``を返す
* ``TailRec``から値を取り出して処理終了

これが再帰呼び出しではなくループで実現されている



## 見た目は再帰呼び出しっぽいけど実はループ！

```java
long sum(long n) { return sum(n, 0).get(); }
TailRec<Long> sum(long n, long m) {
    if (n == 0) { return TailRec.done(m); }
    //↓再帰呼び出しっぽい
    return TailRec.call(() -> sum(n - 1, n + m));
}
```



## それでは``TailRec``を使ってfibメソッドをループ化しよう！



## これが、

```java
BigInteger fib(BigInteger n) {
    if (n.compareTo(zero) == 0) { return zero; }
    if (n.compareTo(one) == 0) { return one; }
    BigInteger m = values.get(n);
    if (m != null) { return m; }
    m = fib(n.subtract(one)).add(fib(n.subtract(two)));
    values.put(n, m);
    return m;
}
```



## こうじゃ

```java
TailRec<BigInteger> fib(BigInteger n) {
    if (n.compareTo(zero) == 0) { return TailRec.done(zero); }
    if (n.compareTo(one) == 0) { return TailRec.done(one); }
    BigInteger m1 = memo.get(n);
    if (m1 != null) { return TailRec.done(m1); }
    return TailRec.call(() -> {
        BigInteger m2 = fib(n.subtract(one)).add(fib(n.subtract(two)));
        memo.put(n, m2);
        return m2;
    });
}
```



## コンパイルエラー！！！

```java
TailRec<BigInteger> fib(BigInteger n) {
    (中略)
    return TailRec.call(() -> {
        //fibの戻り値はTailRecなのでaddメソッドは無い！！！
        BigInteger m2 = fib(n.subtract(one)).add(fib(n.subtract(two)));
        memo.put(n, m2);
        return m2;
    });
}
```



### そもそも、再帰呼び出しが二つ存在し、末尾再帰呼び出しになっていない！！！

変形前のコード

```java
BigInteger fib(BigInteger n) {
    if (n.compareTo(zero) == 0) { return zero; }
    if (n.compareTo(one) == 0) { return one; }
    BigInteger m = values.get(n);
    if (m != null) { return m; }
    //↓再帰呼び出しが二つある！！！
    m = fib(n.subtract(one)).add(fib(n.subtract(two)));
    values.put(n, m);
    return m;
}
```



## まずは末尾再帰呼び出しに変形しよう！



## CPS変換



### CPS変換とは

* 継続渡しスタイルに変換すること
* 継続とはある時点における残りの処理
* 関数を**計算結果を返す**から**計算結果を継続に渡す**よう変換する
* 詳しくは [再帰関数のスタックオーバーフローを倒す話 その1 - ぐるぐる～](http://bleis-tift.hatenablog.com/entry/cps) を読もう！(めっちゃわかりやすい)



## 計算結果を返す通常の形式

```java
int increment(int n) {
    return n + 1;
}
int twice(int n) {
    return n * 2;
}
void run() {
    System.out.println(twice(increment(1)));
}
```

これを呼び出し元の後続処理を継続として受け取り、計算結果を渡すようにすると……



## 継続渡しスタイル

```java
//IntConsumer は int -> void な関数インターフェース
void increment(int n, IntConsumer k) {
    int ret = n + 1; k.accept(ret);
}
void twice(int n, IntConsumer k) {
    int ret = n * 2; k.accept(ret);
}
void run() {
    increment(1, x -> twice(x, y -> System.out.println(y)));
}
```

こうなる。



## 継続渡しスタイルにすると

* 継続は**ある時点における残りの処理**なので自然と**末尾で呼び出す**ことになる
* つまり、再帰呼び出しを行う関数を継続渡しスタイルで書けば**末尾再帰呼び出しを自然と書く事ができる！！！**



## CPS変換の例

簡単な例のため再び末尾再帰呼び出しになっていないsum関数に登場してもらう。

```java
long sum(long n) {
    if (n == 0) { return 0; }
    //再帰呼び出ししてからnを足している！！！
    return n + sum(n - 1);
}
```

```java
//こんな感じで呼び出す
System.out.println(sum(100));
```



### sum関数の引数に継続を追加する

```java
void sum(int n, IntConsumer k) {
    if (n == 0) { return 0; }
    return n + sum(n - 1);
}
```



### ``0``を返している箇所を継続に渡すようにする

```java
void sum(int n, IntConsumer k) {
    if (n == 0) { k.accept(0); }
    //returnしなくなったのでelseブロックを導入した
    else {
        return n + sum(n - 1);
    }
}
```



### sum関数の実行結果を変数で受け取る

```java
void sum(int n, IntConsumer k) {
    if (n == 0) { k.accept(0); }
    else {
        int ret = sum(n - 1);
        return n + ret;
    }
}
```



### ``n``とsum関数の結果の足し算を関数化する

```java
void sum(int n, IntConsumer k) {
    if (n == 0) { k.accept(0); }
    else {
        int ret = sum(n - 1);
        IntConsumer k = ret -> n + ret;
    }
}
```



### sum関数に継続を渡す

```java
void sum(int n, IntConsumer k) {
    if (n == 0) { k.accept(0); }
    else {
        //n + retをkに渡す必要がある
        sum(n - 1, ret -> n + ret);
    }
}
```



### 継続渡しスタイルのsum関数の完成

```java
void sum(int n, IntConsumer k) {
    if (n == 0) {
        k.accept(0);
    } else {
        sum(n - 1, ret -> k.accept(n + ret));
    }
}
```

```java
//こんな感じで呼び出す
sum(100, System.out::println);
```

末尾再帰呼び出しになっている！！！



### でも戻り値``void``……

```java
void sum(int n, IntConsumer k) { ... }
```

じゃなくて

```java
int sum(int n, IntConsumer k) { ... }
```

にしたいんや！



## できます！



### 戻り値を``int``にして

```java
int sum(int n, IntConsumer k) {
    if (n == 0) {
        k.accept(0);
    } else {
        sum(n - 1, ret -> k.accept(n + ret));
    }
}
```



### ``return``を付けて

```java
int sum(int n, IntConsumer k) {
    if (n == 0) {
        return k.accept(0);
    } else {
        return sum(n - 1, ret -> k.accept(n + ret));
    }
}
```



### ``IntConsumer``を``IntUnaryOperator``にして

```java
//IntUnaryOperator は int -> int な関数インターフェース
int sum(int n, IntUnaryOperator k) {
    if (n == 0) {
        return k.accept(0);
    } else {
        return sum(n - 1, ret -> k.accept(n + ret));
    }
}
```



### ``accept``を``applyAsInt``にする

```java
int sum(int n, IntUnaryOperator k) {
    if (n == 0) {
        return k.applyAsInt(0);
    } else {
        return sum(n - 1, ret -> k.applyAsInt(n + ret));
    }
}
```

これで戻り値が``int``になった！



### でもこれどうやって呼び出すの？

もともとはこんな感じで呼び出していた。

```java
//System.out::println が IntConsumer
sum(100, System.out::println);
```

この呼び出し方で計算結果が出力されていた



## つまり

* 計算結果(``int``)を受け取って消費していた(``void``)ところを
* 計算結果(``int``)を受け取って返す(``int``)ようにすればいい



## 恒等関数を渡して呼び出せば良い！

```java
int ret = sum(100, IntUnaryOperator.identity());
System.out.println(ret);
```

```java
//こんな感じの呼び出し用メソッドを作っておくと便利
int sum(int n) {
    return sum(n, IntUnaryOperator.identity());
}
```



### 再帰呼び出しをCPS変換するポイント

* ``return``している箇所を継続の実行に変える
* 再帰呼び出しから後の処理を継続として渡すように変える



## さあ、CPS変換を使ってfib関数を末尾再帰呼び出しにしよう！



## fib関数再掲

```java
BigInteger fib(BigInteger n) {
    if (n.compareTo(zero) == 0) { return zero; }
    if (n.compareTo(one) == 0) { return one; }
    BigInteger m = values.get(n);
    if (m != null) { return m; }
    m = fib(n.subtract(one)).add(fib(n.subtract(two)));
    values.put(n, m);
    return m;
}
```



### まず引数に継続を追加する

```java
BigInteger fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return zero; }
    if (n.compareTo(one) == 0) { return one; }
    BigInteger m = values.get(n);
    if (m != null) { return m; }
    m = fib(n.subtract(one)).add(fib(n.subtract(two)));
    values.put(n, m);
    return m;
}
```



### 値を返している箇所を継続に渡すようにする

```java
BigInteger fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return k.apply(zero); }
    if (n.compareTo(one) == 0) { return k.apply(one); }
    BigInteger m = values.get(n);
    if (m != null) { return k.apply(m); }
    m = fib(n.subtract(one)).add(fib(n.subtract(two)));
    values.put(n, m);
    return k.apply(m);
}
```



### fib関数の実行結果を変数で受け取る

```java
BigInteger fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return k.apply(zero); }
    if (n.compareTo(one) == 0) { return k.apply(one); }
    BigInteger m = values.get(n);
    if (m != null) { return k.apply(m); }
    BigInteger x = fib(n.subtract(one));
    BigInteger y = fib(n.subtract(two));
    BigInteger z = x.add(y);
    values.put(n, z);
    return k.apply(z);
}
```



### ``x``と``y``の足し算(とメモ化)を関数化する

```java
BigInteger fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return k.apply(zero); }
    if (n.compareTo(one) == 0) { return k.apply(one); }
    BigInteger m = values.get(n);
    if (m != null) { return k.apply(m); }
    BigInteger x = fib(n.subtract(one));
    BigInteger y = fib(n.subtract(two));
    UnaryOperator<BigInteger> k = y -> {
        BigInteger z = x.add(y);
        values.put(n, z);
        return k.apply(z);
    };
}
```



### fib関数に継続を渡す

```java
BigInteger fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return k.apply(zero); }
    if (n.compareTo(one) == 0) { return k.apply(one); }
    BigInteger m = values.get(n);
    if (m != null) { return k.apply(m); }
    BigInteger x = fib(n.subtract(one));
    fib(n.subtract(two), y -> {
        BigInteger z = x.add(y);
        values.put(n, z);
        return k.apply(z);
    });
}
```



### ひとつめのfib関数呼び出しの後続処理を関数化する

```java
BigInteger fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return k.apply(zero); }
    if (n.compareTo(one) == 0) { return k.apply(one); }
    BigInteger m = values.get(n);
    if (m != null) { return k.apply(m); }
    BigInteger x = fib(n.subtract(one));
    UnaryOperator<BigInteger> k = x -> {
        return fib(n.subtract(two), y -> {
            BigInteger z = x.add(y);
            values.put(n, z);
            return k.apply(z);
        });
    });
}
```



### ひとつめのfib関数に継続を渡す

```java
BigInteger fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return k.apply(zero); }
    if (n.compareTo(one) == 0) { return k.apply(one); }
    BigInteger m = values.get(n);
    if (m != null) { return k.apply(m); }
    return fib(n.subtract(one), x -> {
        return fib(n.subtract(two), y -> {
            BigInteger z = x.add(y);
            values.put(n, z);
            return k.apply(z);
        });
    });
}
```

これで継続渡しスタイルになった！



## 次は``TailRec``を使って末尾再帰呼び出しを最適化だ！



### 戻り値を``TailRec<BigInteger>``にする

```java
TailRec<BigInteger> fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return k.apply(zero); }
    if (n.compareTo(one) == 0) { return k.apply(one); }
    BigInteger m = values.get(n);
    if (m != null) { return k.apply(m); }
    return fib(n.subtract(one), x -> {
        return fib(n.subtract(two), y -> {
            BigInteger z = x.add(y);
            values.put(n, z);
            return k.apply(z);
        });
    });
}
```



### ``return``している箇所を``TailRec.call``で包む

```java
TailRec<BigInteger> fib(BigInteger n, UnaryOperator<BigInteger> k) {
    if (n.compareTo(zero) == 0) { return TailRec.call(() -> k.apply(zero)); }
    if (n.compareTo(one) == 0) { return TailRec.call(() -> k.apply(one)); }
    BigInteger m = values.get(n);
    if (m != null) { return TailRec.call(() -> k.apply(m)); }
    return TailRec.call(() -> fib(n.subtract(one), x -> {
        return TailRec.call(() -> fib(n.subtract(two), y -> {
            BigInteger z = x.add(y);
            values.put(n, z);
            return TailRec.call(() -> k.apply(z));
        }));
    }));
}
```



### ``UnaryOperator<BigInteger>``を``Function<BigInteger, TailRec<BigInteger>>``にする

```java
TailRec<BigInteger> fib(BigInteger n, Function<BigInteger, TailRec<BigInteger>> k) {
    if (n.compareTo(zero) == 0) { return TailRec.call(() -> k.apply(zero)); }
    if (n.compareTo(one) == 0) { return TailRec.call(() -> k.apply(one)); }
    BigInteger m = values.get(n);
    if (m != null) { return TailRec.call(() -> k.apply(m)); }
    return TailRec.call(() -> fib(n.subtract(one), x -> {
        return TailRec.call(() -> fib(n.subtract(two), y -> {
            BigInteger z = x.add(y);
            values.put(n, z);
            return TailRec.call(() -> k.apply(z));
        }));
    }));
}
```



## これで末尾再帰呼び出しが最適化された！



## ``n = 10000`` で実行

ちなみに呼び出し方はこんな感じになる

```java
BigInteger n = new BigInteger("10000");
//UnaryOperator.identity()をTailRec::doneに変えた
TailRec<BigInteger> tailRec = fib(n, TailRec::done);
BigInteger ret = tailRec.get();
System.out.println(ret);
```



## 実行結果

```
33644764876431783266621612005107543310302148460680063906564769974680081442166662368155595513633734025582065332680836159373734790483865268263040892463056431887354544369559827491606602099884183933864652731300088830269235673613135117579297437854413752130520504347701602264758318906527890855154366159582987279682987510631200575428783453215515103870818298969791613127856265033195487140214287532698187962046936097879900350962302291026368131493195275630227837628441540360584402572114334961180023091208287046088923962328835461505776583271252546093591128203925285393434620904245248929403901706233888991085841065183173360437470737908552631764325733993712871937587746897479926305837065742830161637408969178426378624212835258112820516370298089332099905707920064367426202389783111470054074998459250360633560933883831923386783056136435351892133279732908133732642652633989763922723407882928177953580570993691049175470808931841056146322338217465637321248226383092103297701648054726243842374862411453093812206564914032751086643394517512161526545361333111314042436854805106765843493523836959653428071768775328348234345557366719731392746273629108210679280784718035329131176778924659089938635459327894523777674406192240337638674004021330343297496902028328145933418826817683893072003634795623117103101291953169794607632737589253530772552375943788434504067715555779056450443016640119462580972216729758615026968443146952034614932291105970676243268515992834709891284706740862008587135016260312071903172086094081298321581077282076353186624611278245537208532365305775956430072517744315051539600905168603220349163222640885248852433158051534849622434848299380905070483482449327453732624567755879089187190803662058009594743150052402532709746995318770724376825907419939632265984147498193609285223945039707165443156421328157688908058783183404917434556270520223564846495196112460268313970975069382648706613264507665074611512677522748621598642530711298441182622661057163515069260029861704945425047491378115154139941550671256271197133252763631939606902895650288268608362241082050562430701794976171121233066073310059947366875
```



## スタックオーバーフローせずに再帰で ``n = 10000`` フィボナッチ数を計算できた！！！



## まとめ

* Javaコンパイラは末尾再帰呼び出しの最適化をしてくれない
* でも手動で最適化できる
* 再帰呼び出しを末尾再帰呼び出しの形にするにはCPS変換が分かりやすい
* ていうかScalaやれ



## おまけ



### fib関数のCPS変換＆末尾再帰呼び出しの最適化を……

*before*

```java
TailRec<BigInteger> fib(BigInteger n, Function<BigInteger, TailRec<BigInteger>> k) {
    if (n.compareTo(zero) == 0) { return TailRec.call(() -> k.apply(zero)); }
    if (n.compareTo(one) == 0) { return TailRec.call(() -> k.apply(one)); }
    BigInteger m = values.get(n);
    if (m != null) { return TailRec.call(() -> k.apply(m)); }
    return TailRec.call(() -> fib(n.subtract(one), x -> {
        return TailRec.call(() -> fib(n.subtract(two), y -> {
            BigInteger z = x.add(y);
            values.put(n, z);
            return TailRec.call(() -> k.apply(z));
        }));
    }));
}
```



### セミコロンレスJavaで

*after*

```java
public class SemicolonlessFibonacci {

    public static void main(String[] args) {

        if (java.util.stream.Stream
            .of(new java.math.BigInteger("100"))
            .flatMap(a -> java.util.stream.Stream.<F> of(f -> n -> values -> k -> n.compareTo(java.math.BigInteger.ZERO) == 0
                ? () -> new javafx.util.Pair<>(k.apply(java.math.BigInteger.ZERO), java.util.Optional.empty())
                : n.compareTo(java.math.BigInteger.ONE) == 0
                ? () -> new javafx.util.Pair<>(k.apply(java.math.BigInteger.ONE), java.util.Optional .empty())
                : values.get(n) != null
                ? () -> new javafx.util.Pair<>(k.apply(values.get(n)), java.util.Optional.empty())
                : () -> new javafx.util.Pair<>(
                    f.apply(f)
                     .apply(n.subtract(java.math.BigInteger.ONE))
                     .apply(values)
                     .apply(x -> () -> new javafx.util.Pair<>(
                         f.apply(f)
                          .apply(n.subtract(new java.math.BigInteger("2")))
                          .apply(values)
                          .apply(y -> values.put(n, x.add(y)) == null
                         ? () -> new javafx.util.Pair<>( k.apply(x .add(y)), java.util.Optional .empty())
                         : () -> new javafx.util.Pair<>( k.apply(x .add(y)), java.util.Optional .empty())),
                         java.util.Optional.empty())),
                    java.util.Optional.empty()))
                .map(fib -> java.util.stream.Stream.iterate(
                     fib.apply(fib)
                        .apply(a)
                        .apply(new java.util.HashMap<>())
                        .apply(m -> () -> new javafx.util.Pair<>(null, java.util.Optional.of(m))), t -> t.get().getKey())
                    .filter(t -> t.get().getValue().isPresent())
                    .map(t -> t.get().getValue().get())
                    .findFirst().get()))
            .peek(System.out::println).count() == 0) {}
    }

    interface F extends java.util.function.Function<F, java.util.function.Function<java.math.BigInteger, java.util.function.Function<java.util.Map<java.math.BigInteger, java.math.BigInteger>, java.util.function.Function<java.util.function.Function<java.math.BigInteger, TailRec<java.math.BigInteger>>, TailRec<java.math.BigInteger>>>>> {
    }

    interface TailRec<T> extends java.util.function.Supplier<javafx.util.Pair<TailRec<T>, java.util.Optional<T>>> {
    }
}
```



## おわり☃



## 参考資料

* [Javaによる関数型プログラミング](http://www.oreilly.co.jp/books/9784873117041/)
* [再帰関数のスタックオーバーフローを倒す話 その1 - ぐるぐる～](http://bleis-tift.hatenablog.com/entry/cps)
* [セミコロンレスJavaで末尾再帰の最適化 — 裏紙](http://backpaper0.github.io/2014/11/08/semicolonless_tail_call_optimization.html)

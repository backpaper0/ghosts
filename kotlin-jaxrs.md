## KotlinでJAX-RS + おまけ

[@backpaper0](https://twitter.com/backpaper0)

2014-09-13 [関西Kotlin勉強会](http://www.zusaar.com/event/5527003)



## 趣旨

JAX-RSで使うアレとかソレをKotlinで書いてみよう！



## JAX-RSとは

* [JSR 339](https://jcp.org/en/jsr/detail?id=339)
* The Java API for RESTful Web Services
* POJOにアノテーション付けて嬉しいHTTPの薄いやつ



## 一番簡単なJAX-RSアプリケーション

* 名前受けとってこんにちは返すやつ
* 必要なクラスはふたつ

  * Applicationのサブクラス
  * リソースクラス



## Applicationのサブクラス

```java
@ApplicationPath("rest")
public class HelloApplication extends Application {
}
```



## リソースクラス

```java
@Path("hello")
public class HelloResource {

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello(
            @QueryParam("name") @DefaultValue("world") String name) {
        return String.format("Hello, %s!", name);
    }

    @Path("path")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getPath() {
        return uriInfo.getPath();
    }
}
```



## これをKotlinで書く

* セミコロン要らない
* アノテーションに@要らない
* importで別名付けられる
* 型推論効く
* メソッド一行で書ける
* 他にもいろいろ



## Applicationサブクラス

```
ApplicationPath("rest")
public class HelloApplication : Application() 
```



## リソースクラス

```kotlin
Path("hello")
public class HelloResource {

  Context
  var uriInfo : UriInfo? = null

  GET
  Produces(MediaType.TEXT_PLAIN)
  fun sayHello(
      //デフォルト引数は使えず、@DefaultValueを使うしかない
      QueryParam("name") DefaultValue("world") name: String
    ): String = "Hello, $name!"

  Path("path")
  GET
  Produces(MediaType.TEXT_PLAIN)
  fun getPath(): String = uriInfo?.getPath()!!
}
```



## @Contextなフィールドをvalにしたい

コンストラクタインジェクションを使えば良い……？

```kotlin
Path("hello")
public class HelloResource(
    Context val uriInfo : UriInfo
  ) {

  GET
  Produces(MediaType.TEXT_PLAIN)
  fun sayHello(
      QueryParam("name") DefaultValue("world") name: String
    ): String = "Hello, $name!"

  Path("path")
  GET
  Produces(MediaType.TEXT_PLAIN)
  fun getPath(): String? = uriInfo.getPath()
}
```



## と、思ったけれど

* コンストラクタ引数だけでなくフィールドにも@Contextが付いてしまう
* フィールドはfinalなのでフィールドインジェクションは出来ない
* でも@Contextが付いているのでインジェクション対象
* エラー乁( ˙ω˙ )厂
* フィールドには@Contextを付けないようにできるの？(trsn)



## オレオレクラスで受けとる

* JAX-RSはクエリパラメータやリクエストヘッダなどをオレオレクラスで受けとれる
* 詳しくは[JAX-RSでパラメータの受け取り方をいろいろ試す](http://backpaper0.github.io/2013/07/17/jaxrs_parameter.html)を参照ください。



## Stringの引数をひとつだけ受け取るpublicなコンストラクタを持つクラス

```java
public class ValueObject1 {

    private final String value;

    public ValueObject1(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```



## をKotlinで

```kotlin
class ValueObject1(val value: String)
```



## Data Classでも大丈夫だった

```kotlin
data class ValueObject1(val value: String)
```



## Stringの引数をひとつだけ受け取る”valueOf”という名前のstaticファクトリメソッドを持つクラス

```java
public class ValueObject2 {

    private final String value;

    private ValueObject2(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ValueObject2 valueOf(String value) {
        return new ValueObject2(value);
    }
}
```



## をKotlinで

staticメソッドの定義の仕方が分からない\_(:3｣∠)_ (trsn)



## ParamConverterを使用する

```java
public interface ValueObject3 {

    String getValue();
}
```



## インターフェース実装クラス

```java
public class ValueObject3Impl implements ValueObject3 {

    private final String value;

    public ValueObject3Impl(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
```



## ParamConverter実装クラス

```java
public class ValueObject3Converter implements ParamConverter<ValueObject3> {

    @Override
    public ValueObject3 fromString(String value) {
        return new ValueObject3Impl(value);
    }

    @Override
    public String toString(ValueObject3 value) {
        return value.getValue();
    }
}
```



## ParamConverterProvider実装クラス

```java
@Provider
public class ValueObject3ConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType,
            Type genericType, Annotation[] annotations) {
        if (rawType == ValueObject3.class) {
            return (ParamConverter<T>) new ValueObject3Converter();
        }
        return null;
    }
}
```



## をKotlinで

```kotlin
public trait ValueObject3 {

  val value: String
}
```



## インターフェース実装クラス

```kotlin
public class ValueObject3Impl(
    override val value: String
  ) : ValueObject3
```



## ParamConverter実装クラス

```kotlin
public class ValueObject3Converter : ParamConverter<ValueObject3> {

  override fun fromString(value: String?): ValueObject3 = ValueObject3Impl(value!!)

  override fun toString(value: ValueObject3?): String = value!!.value
}
```



## ParamConverterProvider実装クラス

```kotlin
Provider
public class ValueObject3ConverterProvider : ParamConverterProvider {

  override fun <T> getConverter(rawType: Class<T>?,
            genericType: Type?, annotations: Array<out Annotation>?): ParamConverter<T>? {
    if (rawType!! == javaClass<ValueObject3>()) {
      return ValueObject3Converter() as ParamConverter<T>
    }
    return null
  }
}
```



## リクエストフィルター

* Cookieを確認して認証してなかったら403返す
* 特定のクエリパラメータがあればPUTメソッドに変える

などができるやつ



## まずはアノテーションを作る

```java
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Greedy {
}
```



## 次にフィルター実装クラス

```java
@Provider
@Greedy //最初に作ったアノテーション
public class GreedyFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        String paid = requestContext.getHeaderString("payment");
        if (paid == null || Integer.parseInt(paid) < 1000) {
            throw new WebApplicationException(Response.Status.PAYMENT_REQUIRED);
        }
    }
}
```



## 最後にリソースクラス

```java
@Greedy //ここにもアノテーション。これでフィルターが適用される
@Path("CuteGirl")
public class CuteGirlResource {

    @Path("Smile")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSmile() {
        return "(๑•̀ᴗ-♡ॢ)";
    }
}
```



## をKotlinで

```kotlin
NameBinding
Retention(RetentionPolicy.RUNTIME)
//アノテーションの要素に配列を設定する方法が分からない。(trsn)
//Target(array(ElementType.TYPE, ElementType.METHOD))
public annotation class Greedy
```



## フィルター実装クラス

```kotlin
Provider
Greedy
public class GreedyFilter : ContainerRequestFilter {

  override fun filter(requestContext: ContainerRequestContext?): Unit {
    val paid = requestContext!!.getHeaderString("payment")
    if (paid == null || paid.toInt() < 1000) {
      throw WebApplicationException(Response.Status.PAYMENT_REQUIRED)
    }
  }
}
```



## リソースクラス

```kotlin
Greedy
Path("CuteGirl")
public class CuteGirlResource {

  Path("Smile")
  GET
  Produces(MediaType.TEXT_PLAIN)
  fun getSmile(): String = "(๑•̀ᴗ-♡ॢ)"
}
```



## まとめ

* JAX-RS関連のクラス定義するだけじゃあんまりKotlinの恩恵を受けられない
* でもガワじゃなくて中身を実装するときはKotlinは楽そうで良い
* JAX-RSはJava EEのわりに良い



## おまけ



## Kotlinコードをjavapする



## 趣旨

* kotlincして出来たクラスファイルをjavapしてどんな感じか見る



## Data Class

```kotlin
public data class Point(val x: Int, val y: Int)
```



## をjavap

```
Compiled from "data-class.kt"
public final class Point implements kotlin.jvm.internal.KObject {
  public static final kotlin.reflect.jvm.internal.KClassImpl $kotlinClass;
  static {};
  public final int getX();
  public final int getY();
  public Point(int, int);
  public final int component1();
  public final int component2();
  public final Point copy(int, int);
  public static Point copy$default(Point, int, int, int);
  public java.lang.String toString();
  public int hashCode();
  public boolean equals(java.lang.Object);
}
```



## Data Classを使う例

```
val p1 = Point(2, 3)

val p2 = p1.copy(4)

val p3 = p2.copy(y = 5)

val (x, y) = p3

println("p1=$p1 p2=$p2 p3=$p3 x=$x y=$y")

// p1=Point(x=2, y=3) p2=Point(x=4, y=3) p3=Point(x=4, y=5) x=4 y=5
```



## パラメータのデフォルト値

```kotlin
public class Hello {
  fun say(name: String = "world") = "Hello, $name!"
}
```



## をjavap

```
Compiled from "default-parameter.kt"
public final class Hello implements kotlin.jvm.internal.KObject {
  public static final kotlin.reflect.jvm.internal.KClassImpl $kotlinClass;
  static {};
  public final java.lang.String say(java.lang.String);
  public static java.lang.String say$default(Hello, java.lang.String, int);
  public Hello();
}
```



## say$defaultを詳しく見る

```
public static java.lang.String say$default(Hello, java.lang.String, int);
  descriptor: (LHello;Ljava/lang/String;I)Ljava/lang/String;
  flags: ACC_PUBLIC, ACC_STATIC
  Code:
    stack=3, locals=3, args_size=3
       0: aload_0       
       1: iload_2       
       2: iconst_1      
       3: iand          
       4: ifeq          10
       7: ldc           #48                 // String world
       9: astore_1      
      10: aload_1       
      11: invokevirtual #50                 // Method say:(Ljava/lang/String;)Ljava/lang/String;
      14: areturn       
    LineNumberTable:
      line 2: 7
    StackMapTable: number_of_entries = 1
         frame_type = 74 /* same_locals_1_stack_item */
        stack = [ class Hello ]
```



## 命令のおおまかな説明

* aload_0 : 第1引数をプッシュする
* iload_2 : 第3引数をプッシュする
* iconst_1 : 定数1をプッシュする
* iand : ポップした2つのintの論理積をプッシュ
* ifeq : ポップしたintが0に等しければジャンプする
* ldc : コンスタントプールから取った項目をプッシュする
* astore_1 : ポップしたオブジェクトをローカル変数に入れる
* aload_1 : ローカル変数をプッシュする
* invokevirtual : インスタンスメソッドを実行する
* areturn : ポップしたオブジェクトを返す



## つまり

* デフォルト値の要・不要は最後の引数との論理積で決まる



## ところで

* intは32ビット
* フラグにすると32個分
* 33個の引数があるとどうなんの？



## やってみる

```kotlin
fun hoge(
   a1: Int =  1,
   a2: Int =  2,
   a3: Int =  3,
   a4: Int =  4,
   a5: Int =  5,
   a6: Int =  6,
   a7: Int =  7,
   a8: Int =  8,
   a9: Int =  9,
  a10: Int = 10,
  a11: Int = 11,
  a12: Int = 12,
  a13: Int = 13,
  a14: Int = 14,
  a15: Int = 15,
  a16: Int = 16,
  a17: Int = 17,
  a18: Int = 18,
  a19: Int = 19,
  a20: Int = 20,
  a21: Int = 21,
  a22: Int = 22,
  a23: Int = 23,
  a24: Int = 24,
  a25: Int = 25,
  a26: Int = 26,
  a27: Int = 27,
  a28: Int = 28,
  a29: Int = 29,
  a30: Int = 30,
  a31: Int = 31,
  a32: Int = 32,
  a33: Int = 33) {
  println( a1)
  println( a2)
  println( a3)
  println( a4)
  println( a5)
  println( a6)
  println( a7)
  println( a8)
  println( a9)
  println(a10)
  println(a11)
  println(a12)
  println(a13)
  println(a14)
  println(a15)
  println(a16)
  println(a17)
  println(a18)
  println(a19)
  println(a20)
  println(a21)
  println(a22)
  println(a23)
  println(a24)
  println(a25)
  println(a26)
  println(a27)
  println(a28)
  println(a29)
  println(a30)
  println(a31)
  println(a32)
  println(a33)
}

fun main(args: Array<String>) {
  hoge(a1 = 100)
}
```
  



## 実行結果

```
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
```



## ＼(^o^)／



## まとめ

* 33も引数使うな



## おまけ2



## KotlinのNullableでMaybeモナド



## 趣旨

* NullableをMaybeモナドとみなしていくつかの関数を書いてモナド則を満たそう



## 参考資料

* [Kotlin Nullable型をモナドっぽくしてみた // Speaker Deck](https://speakerdeck.com/ntaro/kotlin-nullablexing-womonadotupokusitemita)
* [KotlinのNullable型をモナドっぽくしてみた - 算譜王におれはなる!!!!](http://taro.hatenablog.jp/entry/2014/04/06/144811)



## モナド則

* これを満たすとモナドである
* のかな？
* よく分からないけどたぶんそう

```haskell
(return x) >>= f == f x

m >>= return == m

(m >>= f) >>= g == m >>= (\x -> f x >>= g)
```



## returnと>>=を実装する

```kotlin
// return
fun <T> some(t: T): T? = t

// >>=
fun <T, U> T?.flatMap(f: (T) -> U?): U? = if(this != null) f(this) else null
```



## モナド則を満たすか試す

```kotlin
some(x).flatMap(f) == f(x)

m.flatMap { some(it) } == m

m.flatMap(f).flatMap(g) == m.flatMap { x -> f(x).flatMap(g) }
```



## ╭( ・ㅂ・)و ̑̑ ｸﾞｯ ! 



## でも……

* Scalaのfor式に相当するものがないと嬉しさが少ない

```scala
//これはScala
val a = Some(2)
val b = Some(3)

val c = a.flatMap { x => b.map { y => x + y }}

//for式で書ける
val c = for {
  x <- a
  y <- b
} yield x + y
```



## \_(:3｣∠)_



## まとめ

* たぶんKotlinにモナドは要らん
* Nullableは便利だけどなんかキモい

```kotlin
// flatMapとmapが同じコードで書けて変な感じ……
fun <T, U> T?.flatMap(f: (T) -> U?): U? = if(this != null) f(this) else null

fun <T, U> T?.map(f: (T) -> U): U? = if(this != null) f(this) else null
```



## 全体のまとめ

* JAX-RSは素敵
* 引数は32個以下でお願いします
* Nullableは奥が深そう
* 今回紹介したのはKotlinの能力のほんの一部
* なかなか実用的な気がした



## Kotlinを学ぶには

* [公式リファレンス](kotlinlang.org/docs/reference/)が結構充実している
* Kotlinの日本語の資料はほとんどたろうさん作
* アイドルにKotlinのこと聞いても何も返ってこない

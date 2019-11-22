class: center, middle

# DIコンテナ入門

---

## 概要

Javaでサーバーサイドアプリケーション開発をしていると必ずといっていいほど登場するのが「DIコンテナ」です。 例えばSpring Frameworkを学ぶ過程で「SpringはDIコンテナです」といった説明を目にしたことがある人もいると思います。

ではDIコンテナとは一体何なのか？　使う利点は？　より良い使い方は？

本セッションでは上記のような疑問について、実際のDIコンテナを例に取って解説を行います。

---

## 想定している聴講者層

DIコンテナが何なのか、どう役立つのかを知りたい人

---

## 自己紹介

- 名前：うらがみ
- SNSアカウント：backpaper0
- 所属企業：TIS株式会社
- コミュニティ：関Java
- DIコンテナ経験：
  - Seasar2 / CDI / Guice / HK2 / Spring

---

class: center, middle

# DIコンテナとは何なのか

---

## 「DI」と「コンテナ」を分けて理解する

「DIコンテナ」について考える前に、まずは「DI」と「コンテナ」をそれぞれ説明します。

---

## DI - Dependency Injection

依存性注入と日本語訳されることが多いです。

- Dependency = 依存
- Injection = 注入

「依存」と「注入」がどういうことを指すのか、ソースコードを見ながら説明します。

---

## 「依存」とは何なのか

まずは依存です。
次のコードを見てください。

```java
public class Parser {
    private Tokenizer tokenizer = new Tokenizer();
    private void consume() {
        token = tokenizer.next();
    }
    //以下略
```

`Parser`クラスはトークンを読むために`Tokenizer`クラスを必要としています。
つまり`Parser`クラスは`Tokenizer`クラスに依存していると言えます。

---

## 「注入」とは何なのか

次は注入です。
DIでいう注入は依存するクラスのインスタンスを外部から渡すことを指します。

```java
public class Parser {
    private Tokenizer tokenizer = new Tokenizer();
    //以下略
```

例として引き続き`Parser`クラスを見ていきましょう。
`Parser`クラスは前述の通り`Tokenizer`クラスに依存しています。

---

## 「注入」とは何なのか

```java
public class Parser {
    private Tokenizer tokenizer = new Tokenizer();
    //以下略
```

フィールド`tokenizer`は宣言と同時に初期化されていますね。
`Tokenizer`クラスのインスタンスは`Parser`クラス内部で生成されており注入されていません。

これをコンストラクタを通じて注入するコードへ変更してみましょう。

---

## 「注入」とは何なのか

```java
public class Parser {
    private Tokenizer tokenizer;
    public Parser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }
    //以下略
```

`Tokenizer`クラスのインスタンスをコンストラクタを通じて注入するようにしました。

「注入しないコード」と「注入するコード」を見比べてみましょう。

---

## 「注入」とは何なのか

```java
//注入しないコード
public class Parser {
    private Tokenizer tokenizer = new Tokenizer();
    //以下略
```

```java
//注入するコード
public class Parser {
    private Tokenizer tokenizer;
    public Parser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }
    //以下略
```

---

## 「注入」とは何なのか

「注入するコード」の方はコンストラクタが増えた分、コード量も増えていますね。

ただし、そのおかげで`Tokenizer`クラスの実装が何なのかを隠せるようになりました。

言い換えると`Tokenizer`クラスのサブクラスを渡すことができるということです。

---

## 「注入」とは何なのか

これはラッパークラスを作ってログ出力を差し込めたり、テスト時にモックに差し替えられるメリットがあります。

```java
//LoggingTokenizerはTokenizerのサブクラス
Tokenizer tokenizer = new LoggingTokenizer(new Tokenizer());
Parser parser = new Parser(tokenizer);
var ast = parser.parse();
```

```java
//テストコード
Tokenizer tokenizer = new MockTokenizer();
Parser parser = new Parser(tokenizer);
assertEquals(expected, parser.parse());
```

---

## 「注入」とは何なのか

注入しないコードでは、コードの書き換え無しにサブクラスで置き換えることができません。

```java
//注入しないコード
public class Parser {
    //テストを実行するたびに書き換える……？
    //private Tokenizer tokenizer = new Tokenizer();
    private Tokenizer tokenizer = new MockTokenizer();
    //以下略
```

---

## 「DI」とは何なのか

ここまで依存と注入の説明をしました。

ここでDIとは何なのかまとめましょう。

---

## 「DI」とは何なのか

DIとは

**あるクラスが使用する他のクラスのインスタンスをコンストラクタなどを通じて外部から渡すようなクラス設計**

です。

---

## 「DI」とは何なのか

これはつまり

**ごく普通のJavaクラス設計**

だと言えます。

---

## 例：java.io.PrintWriter

`java.io.PrintWriter`を例にとってみましょう。
`PrintWriter`には`java.io.Writer`を受け取るコンストラクタがあります。

```java
public PrintWriter(Writer out)
```

`java.io.FileWriter`を注入するとファイルに書き出す`PrintWriter`になりますし、`java.io.StringWriter`を注入するとインメモリに書き出して`String`で取り出せる`PrintWriter`になります。

---

## 例：java.io.PrintWriter

```java
try (FileWriter out = new FileWriter(new File("out.txt"));
        PrintWriter writer = new PrintWriter()) {
    writer.println("hello world");
    writer.flush();
}
```

```java
StringWriter out = new StringWriter();
try (PrintWriter writer = new PrintWriter(out)) {
    writer.println("hello world");
    writer.flush();
}
String text = out.toString();
```

---

## 「コンテナ」とは何なのか

さて、「DI」が何なのかはわかりました。
続いて「コンテナ」とは何なのかを見ていきましょう。

- Container = 容器、入れ物

我々が扱う技術には「DIコンテナ」以外にも「Servletコンテナ」や「Dockerコンテナ」など、いろいろな「コンテナ」があります。
それぞれ別のものであり、今回お話するのは「DIコンテナ」における「コンテナ」です。

---

## 「コンテナ」とは何なのか

コンテナは日本語にすると「容器、入れ物」でした。

DIコンテナも何かの入れ物になっています。

その何かとは「コンポーネント」です。

---

## 「コンテナ」とは何なのか

コンポーネントというのはインスタンスです。
他にもbeanと呼ばれることもあります。

Spring Frameworkのドキュメントを見ても、あるセクションではコンポーネントと表現したり、異なるセクションではbeanと表現したりしています。
またCDIではCDI管理beanと表現されます。

DIコンテナの文脈においてコンポーネントやbeanという言葉を厳密に捉えなくても理解を妨げるものではありません。

---

## DIコンテナの嬉しいところ

DIコンテナがコンポーネントの入れ物であると説明しましたが、それだけでは何が嬉しいのかわかりませんよね。

DIコンテナの嬉しいところを一言で述べると「自動でDIをしてインスタンスを構築してくれること」です。

---

## DIコンテナの嬉しいところ

DIコンテナを使わずにインスタンスを構築するコード例を見てみましょう。

```java
Tokenizer tokenizer = new Tokenizer();
Parser parser = new Parser(tokenizer);
```

`Parser`クラスのインスタンスを構築するためにまず`Tokenizer`クラスのインスタンスを構築しています。

---

## DIコンテナの嬉しいところ

DIコンテナを使って構築済みのインスタンスを得るコード例は次のようになります。

```java
@Autowired
Parser parser;
```

インスタンスを構築するコードは書かなくても良いです。

---

## DIコンテナの嬉しいところ

両者を見比べてみましょう。

```java
Tokenizer tokenizer = new Tokenizer();
Parser parser = new Parser(tokenizer);
```

```java
@Autowired
Parser parser;
```

---

## DIコンテナの嬉しいところ

この例ではたった2クラスなので嬉しさを感じにくいかもしれませんが、実際のプロジェクトでは数十クラスやそれ以上に増えることを考えると嬉しさがわかって貰えるのではないでしょうか。

また、インスタンスを構築するコードは引数を渡して`new`をする、setterメソッドを呼び出す、といった定型的なコードであることが多くDIコンテナに任せることでプロジェクトのコード全体が見通しよくなります。

---

## DIコンテナの嬉しいところ

DIコンテナがインスタンスを管理していることで実現できる嬉しい機能もあります。

それが次の2つです。

- スコープ
- AOP

---

## DIコンテナならではの機能：スコープ

プログラミング言語には変数のスコープがありますが、DIコンテナにもコンポーネントのスコープがあります。

主なスコープは次の通りです。

- シングルトン
- プロトタイプ
- リクエストスコープ
- セッションスコープ

---

## DIコンテナならではの機能：スコープ

シングルトンなコンポーネントはDIコンテナ内に1つのインスタンスしか持ちません。

例えばシングルトンな`Foo`というクラスがあり、`Foo`に依存している`Bar`クラスのインスタンスを2つ作るとします。

---

## DIコンテナならではの機能：スコープ

DIコンテナが行うインスタンス構築をあえて明示的に書くと次のようなコードになります。

```java
Foo foo = new Foo();
Bar bar1 = new Bar(foo);
Bar bar2 = new Bar(foo);
```

`bar1`と`bar2`共に1つの`Foo`インスタンスを参照しています。

---

## DIコンテナならではの機能：スコープ

それに対してプロトタイプなコンポーネントは注入された数だけインスタンスを持ちます。

先ほどのコード例における`Foo`をプロトタイプにすると次のようになります。

```java
Foo foo1 = new Foo();
Bar bar1 = new Bar(foo1);
Foo foo2 = new Foo();
Bar bar2 = new Bar(foo2);
```

---

## DIコンテナならではの機能：スコープ

リクエストスコープはWebアプリケーションならではのスコープです。
Webアプリケーションが1つのHTTPリクエストを受け取ってHTTPレスポンスを返すまでの間を表すスコープです。

これをDIコンテナを使わず実現しようとすると少し手間がかかります。

---

## DIコンテナならではの機能：スコープ

サーブレットAPIを使用してリクエストスコープを実現する場合は次のようになります。

```java
//HttpServletRequest request;
Foo foo = (Foo) request.getAttribute("Foo");
if (foo == null) {
    foo = new Foo();
    request.setAttribute("Foo", foo);
}
Bar bar = new Bar(foo);
```

---

## DIコンテナならではの機能：スコープ

セッションスコープも前ページのコードと似た方法で実現できます。
その場合は`HttpServletRequest`ではなく`HttpSession`を使うことになります。

なお、DIコンテナはスコープの開始・終了に応じて特定のメソッドを呼び出すような機能を持っていることがあり、それをDIコンテナを使わず実現するにはこれまでのコードでは不足しています。

DIコンテナを使うとスコープを扱うのも楽になります。

???

例えばServletRequestListenerを実装しないといけない。

---

## DIコンテナならではの機能：スコープ

Spring Frameworkを使ったコード例を見てみましょう。

```java
@Component
@RequestScoped
public class Foo {

    @PreDestroy
    public void destroy() { ... }
}
```

たったこれだけで`Foo`をリクエストスコープにできますし、前述のようなライフサイクルに応じたメソッド呼び出しもできます。

---

## DIコンテナならではの機能：AOP

- AOP
- インターセプタ
- バイトコードエンハンス

---

class: center, middle

# DIコンテナの良い使い方

---

## ルックアップではなくインジェクションを選ぶ

- 起動時エラーチェックできる

---

## コンストラクタインジェクションを選ぶ

- 完全コンストラクタ
- 依存過多防止


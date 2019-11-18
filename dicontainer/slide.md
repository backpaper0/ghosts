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

「依存性注入」と日本語訳されることが多いです。

- Dependency = 依存
- Injection = 注入

「依存」と「注入」がどういうことを指すのか、ソースコードを見ながら説明します。

---

## 「依存」とは何なのか

まずは「依存」です。
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

次は「注入」です。
DIでいう「注入」は依存するクラスのインスタンスを外部から渡すことを指します。

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
`Tokenizer`クラスのインスタンスは`Parser`クラス内部で生成されており「注入」されていません。

これをコンストラクタを通じて「注入」するコードへ変更してみましょう。

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

`Tokenizer`クラスのインスタンスをコンストラクタを通じて「注入」するようにしました。

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

「注入」しないコードでは、コードの書き換え無しにサブクラスで置き換えることができません。

```java
//注入しないコード
public class Parser {
    //テストを実行するたびに書き換える……？
    //private Tokenizer tokenizer = new Tokenizer();
    private Tokenizer tokenizer = new MockTokenizer();
    //以下略
```

---

## 「DI」とは何なのか？

ここまで「依存」と「注入」の説明をしました。

ここで「DI」とは何なのかまとめましょう。

---

## 「DI」とは何なのか？

「DI」とは

**あるクラスが使用する他のクラスのインスタンスをコンストラクタなどを通じて外部から渡すようなクラス設計**

です。

---

## 「DI」とは何なのか？

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

## DIまとめ 

---

## インスタンスをコンテナで管理する

- DIコンテナ
- Servletコンテナ
- Dockerコンテナ

---

## DIコンテナのメリット

- 依存関係を考慮してインスタンスを構築してくれる

---

## DIコンテナならではの機能：スコープ

- スコープ
- ライフサイクル

---

## DIコンテナならではの機能：AOP

- AOP
- インターセプタ
- バイトコードエンハンス

---

## ルックアップではなくインジェクションを選ぶ

- 起動時エラーチェックできる

---

## コンストラクタインジェクションを選ぶ

- 完全コンストラクタ
- 依存過多防止


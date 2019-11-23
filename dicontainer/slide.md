class: center, middle

# DIコンテナ入門

手元でスライドを見たい方は次のURLからどうぞ

https://bit.ly/2D9n8WY

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
- コミュニティ：関西Javaエンジニアの会（関Java）
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

## ※「注入」という言葉について

ここまで依存コンポーネントを渡すことを「注入する」と表現してきました。

しかし実際の会話では「インジェクションする」や「DIする」と表現することが多いと感じています。

そのためここからは「インジェクションする」と表現することとします。

---

## 「コンテナ」とは何なのか

さて、「DI」が何なのかはわかりました。
続いて「コンテナ」とは何なのかを見ていきましょう。

我々が扱う技術には「DIコンテナ」以外にも「Servletコンテナ」や「Dockerコンテナ」など、いろいろな「コンテナ」があります。
それぞれ別のものであり、今回お話するのは「DIコンテナ」における「コンテナ」です。

---

## 「コンテナ」とは何なのか

コンテナを日本語にすると次の通りです。

- Container = 容器、入れ物

DIコンテナも何かの入れ物になっています。

その何かとは「コンポーネント」です。

---

## 「コンテナ」とは何なのか

コンポーネントというのはインジェクションされるインスタンスです。
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

シングルトンはDIコンテナの開始から終了までがスコープになります。
1つのコンポーネントにつき1つのインスタンスしか持ちません。

例えばSpring Frameworkでシングルトンなコンポーネント`Foo`を定義してみましょう。

```java
@Component
public class Foo { ... }
```

※Spring Frameworkではデフォルトのスコープはシングルトンです

---

## DIコンテナならではの機能：スコープ

2つのコンポーネントに`Foo`をインジェクションします。

```java
@Component
public class Bar {
    private Foo foo;
    public Bar(Foo foo) { this.foo = foo; }
}

@Component
public class Hoge {
    private Foo foo;
    public Hoge(Foo foo) { this.foo = foo; }
}
```

---

## DIコンテナならではの機能：スコープ

どちらにも同じ`Foo`インスタンスがインジェクションされます。

```java
//Bar bar
//Hoge hoge
bar.getFoo().equals(hoge.getFoo()); //true
```

---

## DIコンテナならではの機能：スコープ

プロトタイプはDIコンテナによってインスタンス構築されるけれど、その後は管理されないスコープです。
DIコンテナに管理されないスコープと理解しても良いでしょう。
インジェクションされた数だけインスタンスがあります。

Spring Frameworkでプロトタイプなコンポーネント`Foo`を定義するコード例です。

```java
@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Foo { ... }
```

---

## DIコンテナならではの機能：スコープ

シングルトンの例と同じようにインジェクションしてみましょう。

```java
@Component
public class Bar {
    private Foo foo;
    public Bar(Foo foo) { this.foo = foo; }
}

@Component
public class Hoge {
    private Foo foo;
    public Hoge(Foo foo) { this.foo = foo; }
}
```

---

## DIコンテナならではの機能：スコープ

プロトタイプはインジェクションされるごとに異なるインスタンスが生成されます。

```java
//Bar bar
//Hoge hoge
bar.getFoo().equals(hoge.getFoo()); //false
```

---

## DIコンテナならではの機能：スコープ

リクエストスコープとセッションスコープはWebアプリケーションならではのスコープです。

リクエストスコープはHTTPリクエストの開始から終了までの間がインスタンスの生存期間です。

セッションスコープはセッションが生成されて破棄されるまでの間がインスタンスの生存期間です。

---

## DIコンテナならではの機能：スコープ

このように多彩なスコープをDIコンテナは実現していますが、これがなぜ「DIコンテナならではの機能」なのでしょうか。

既にお話したようにDIコンテナはインスタンスを構築して、コンテナ内に保持します。
そのためインスタンスのライフサイクルを管理しやすいのです。

---

## DIコンテナならではの機能：スコープ

例えばコンテナが破棄されるときにシングルトンなインスタンスを破棄できますし、サーブレットAPIと組み合わせればリクエストスコープやセッションスコープを実現できます。

これをDIコンテナなしに実現しようとするとそれなりの仕組みが必要になりますし、結局その仕組みはDIコンテナになるはずです。

---

## DIコンテナならではの機能：スコープ

スコープのセクションは私の使い分け方をご紹介して締めることにします。

- なるべくシングルトンを使う
- なるべくプロトタイプは使わない
- `HttpServletRequest`を使うぐらいならリクエストスコープを使う
- `HttpSession`を使うぐらいならセッションスコープを使う

---

## DIコンテナならではの機能：AOP

次はAOPについて触れましょう。

AOPはAspect Oriented Programming（アスペクト指向プログラミング）の頭文字語で、複数のコンポーネントにまたがる横断的な関心事を扱うものだと説明されることが多いです。

と言われても何のことだかわかりませんよね。

---

## DIコンテナならではの機能：AOP

簡単に言うとAOPとはコンポーネントのメソッドにコードを変えずに処理を追加できる機能です。

例えば次のように`"hello"`と返すだけのメソッドにAOPで処理を追加すると`"hello world"`と返すようにもできます。

```java
public class Greeting {
    public String say() {
        return "hello";
    }
}
```

```java
greeting.say(); //hello world
```

---

## DIコンテナならではの機能：AOP

これはDIコンテナがコンポーネントのインスタンスを生成するときに動的にサブクラスを生成する、といった方法で実現されています。

```java
//※動的に生成されるサブクラスのイメージ
public class GreetingAOP extends Greeting {
    public String say() {
        String ret = super.say();
        return ret + " world";
    }
}
```

---

## DIコンテナならではの機能：AOP

ところで`"hello"`を`"hello world"`にするぐらいでは嬉しさはわかりませんよね。
実際のプロジェクトでAOPを活用している主な例はトランザクションです。

メソッドに`@Transactional`を付けるとそのメソッドがトランザクション境界になるといった機能を使ったことはありませんか。
あれはトランザクションを開始したりコミットする処理をAOPで追加して実現しています。

---

## DIコンテナならではの機能：AOP

AOPがどのようにしてメソッドに処理を追加しているのか、先ほどイメージをお伝えしましたが具体的な方法は少し難しい話になります。

そのため「ステップアップセッション」の枠で語るテーマではありませんが、[来週の関Java](https://kanjava.connpass.com/event/147145/)でお話する予定なので興味があれば聞きにきてください（会場は大阪だけど）。

---

class: center, middle

# DIコンテナの良い使い方

---

## DIコンテナの良い使い方

最後に私が思うDIコンテナの良い使い方を2つ紹介します。

- ルックアップではなくインジェクションを選ぶ
- コンストラクタインジェクションを選ぶ

---

## ルックアップではなくインジェクションを選ぶ

前半にお話したようにDIコンテナは依存するコンポーネントをインジェクションする機能があります。

そして多くのDIコンテナは型や名前でコンポーネントを検索して取得するルックアップ機能も持っています。

---

## ルックアップではなくインジェクションを選ぶ

例えばSpring Frameworkでは`ApplicationContext`からコンポーネントをルックアップできます。

```java
//ApplicationContext context
Foo foo = context.getBean(Foo.class);
```

---

## ルックアップではなくインジェクションを選ぶ

このようにルックアップができるAPIがあることを覚えておいて損はありませんが、可能な限りルックアップではなくインジェクションを選びましょう。

インジェクションの方が優れている点は「依存がシグネチャに表れる」ことです。

---

## ルックアップではなくインジェクションを選ぶ

Java言語におけるシグネチャとはメソッド名と引数の型の組み合わせです。

例えば次のコード例にあるメソッドはすべてシグネチャが異なります。

```java
public List<Entity> findById(Long id) { ... }
public List<Entity> findById(Long id, long limit) { ... }
public List<Entity> findById(Integer id) { ... }
public long countById(Long id) { ... }
```

---

## ルックアップではなくインジェクションを選ぶ

再び`Tokenizer`と`Parser`のコード例を見てみましょう。
`Parser`の依存コンポーネントである`Tokenizer`がコンストラクタのシグネチャに現れていますね。

```java
private Tokenizer tokenizer;
public Parser(Tokenizer tokenizer) {
    this.tokenizer = tokenizer;
}
```

インジェクションをしている場合、DIコンテナはシグネチャを読み取ってコンポーネント間の依存関係を解決します。

---

## ルックアップではなくインジェクションを選ぶ

それに対してルックアップは依存コンポーネントを取得しようとして初めて依存関係にあることがわかります。

```java
public AstNode parse() {
    //このコードを実行して初めてTokenizerに依存していることがわかる
    Tokenizer tokenizer = context.getBean(Tokenizer.class);
    ...
}
```

---

## ルックアップではなくインジェクションを選ぶ

これはコードが正常動作しているうちは問題ありませんが、依存コンポーネントが存在しない場合（バグ）の発見に差が出てきます。

- インジェクション……DIコンテナ起動時に依存関係をチェックできる
- ルックアップ……実行されるその時まで依存関係はチェックできない

---

## コンストラクタインジェクションを選ぶ

- 完全コンストラクタ
- 依存過多防止


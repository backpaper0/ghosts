# JAX-RS入門および実践

[/backpaper0](https://twitter.com/backpaper0)

2015-11-28 [JJUG CCC 2015 Fall](http://www.java-users.jp/?page_id=2056)

---

## 自己紹介

* うらがみと申します
* 大阪でSIerやっています
* プログラマ歴8年半ぐらい(ほとんどJava)
* JAX-RSでのお仕事歴は4年ぐらい

---

## 本日の発表内容

* JAX-RSの簡単な解説
* 実践の話をいくつか

---

## JAX-RSとは


## JAX-RSとは

* Java EE 6からEEに仲間入りしたWebフレームワーク
* アノテーションを使用して宣言的にHTTPリクエスト・レスポンスをJavaコードにマッピングする
* RESTful APIを作るのに特化している


### でもJava EEには既にServletやJSFといったWebフレームワークがあるのでは？


## ServletとJAX-RS

* 数多のWebフレームワークのベースとなっている
  * JAX-RSはServletをベースにしているフレームワーク群と立ち位置は近い
* ベースとなるだけあって提供されるAPIも低レベルのものが多い
  * JAX-RSの方がより宣言的に書く事ができる


## JSFとJAX-RS

* Servlet上に構築され、より抽象化されたフレームワーク
  * JAX-RSはServlet APIに依存しない
* HTTPを意識しない作りになっておりデスクトップGUIに近い感覚で画面が作れる
  * JAX-RSはHTTPをJavaコードにマッピングするAPI
* XHTMLのテンプレートエンジンがあり、画面がある事が前提
  * JAX-RSはRESTful APIが主戦場


## Servlet、JSFとの棲み分け

* ServletとはAPIのレベル感が違う
* JSFとは方向性が異なる


## 簡単な例

* サーブレットとJAX-RSで足し算してみてコードの違いを確認
* /addition?a=1&b=2 にアクセスすると 3 が返ってくるといった仕様


## Servletで足し算

```java
@WebServlet(urlPatterns = "/addition")
public class Addition extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int a = Integer.parseInt(req.getParameter("a"));
        int b = Integer.parseInt(req.getParameter("b"));
        resp.setContentType("text/plain");
        try (PrintWriter out = resp.getWriter()) {
            out.print(a + b);
        }
    }
}
```


## JAX-RSで足し算

```java
@Path("/addition")
public class Addition {
    @GET
    @Produces("text/plain")
    public int calculate(@QueryParam("a") int a,
                         @QueryParam("b") int b) {
        return a + b;
    }
}
```


## 宣言だけ抜粋

Servletは/additionにGETメソッドでアクセスする、としか読み取れないのに対して……

```java
@WebServlet(urlPatterns = "/addition")
public class Addition extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { ... }
```

JAX-RSは /additionにクエリパラメータaとbを付与してGETメソッドでアクセスするとtext/plainなエンティティが返ってくる、と読み取れる。

```java
@Path("/addition")
public class Addition {
    @GET
    @Produces("text/plain")
    public int calculate(@QueryParam("a") int a,
                         @QueryParam("b") int b) { ... }
```

---

## JAX-RSの仕様について


### JAX-RSアプリケーションを構成する要素

* **リソースクラス**
* エンティティボディの読み込み・書き出し
* フィルター・インターセプター
* 例外ハンドラー
* アプリケーションの定義

* クライアントAPI


## リソースクラス

* HTTPリクエストを受け取るエンドポイントとなるクラス
* @Path で注釈し、少なくとも1つのリソースメソッドを持つ

* リソースメソッドは @GET や @POST などHTTPメソッドを表すアノテーションで注釈したメソッド
* 他にもアノテーションを利用して受け取るパラメータやContent-Typeなどを設定できる


## Additionクラス再掲

```java
@Path("/addition")
public class Addition {
    @GET
    @Produces("text/plain")
    public int calculate(@QueryParam("a") int a,
                         @QueryParam("b") int b) {
        return a + b;
    }
}
```

* @Path でパスを設定する
* @GET でGETメソッドを受け付ける事を設定する
* @Produces でレスポンスのエンティティのContent-Typeを設定する
* @QueryParam でクエリパラメータを受け取る事を設定する


## Additionクラス再掲

```java
@Path("/addition")
public class Addition {
    @GET
    @Produces("text/plain")
    public int calculate(@QueryParam("a") int a,
                         @QueryParam("b") int b) {
        return a + b;
    }
}
```

このAddition.calculateは次のようなHTTPリクエストを処理する。

```
GET /addition?a=1&b=2 HTTP/1.1
Accept: text/plain

```


## パスのマッピング

@Path はメソッドにも付けて階層を表せられる。

```java
@Path("hello")
public class Hello {

    @GET
    public String say() { ... }

    @Path("world")
    @GET
    public String world() { ... }
}
```

この場合 /hello へのリクエストは sayメソッドに、

/hello/world へのリクエストは worldメソッドにマッピングされる。


## パスのマッピング

また、パスは { と } で囲む事で変数として使える。

```java
@GET
@Path("hello/{name}")
public String sayHello(
        @PathParam("name") String name) {
    ...
```

変数に取れる文字は正規表現で制限できる。

次の例だと変数 userId は数字のみで構成される。

```java
@Path("users/{userId:\\d+}")
```


## パラメータのマッピング

HTTPリクエストに含まれる次の項目をパラメータにできる。

* クエリパラメータ
* パスの一部
* Matrix URI
* リクエストヘッダ
* Cookie
* フォームコントロールの値


### クエリパラメータとパスの一部

すでに紹介している機能。

```java
@Path("foo/{foo}")
public class Foo {
    @GET
    public String bar(
            @PathParam("foo") String foo,
            @QueryParam("bar") String bar) {
        ...
```

これは次のようなリクエストパスに対応する。

```
/foo/xxx?bar=yyy
```


## Matrix URI

```java
@Path("addition")
public class Addition {
    @GET
    public String calculate(
            @MatrixParam("left") int left,
            @MatrixParam("right") int right) {
        ...
```

これは次のようなリクエストパスに対応する。

```
/addition;left=2;right=3
```


## リクエストヘッダ、Cookie

```
@GET
public String get(
        @HeaderParam("foo") String foo,
        @Cookie("bar") String bar) {
    ...
```

これは次のようなHTTPリクエストヘッダに対応する。

```
GET /hoge HTTP/1.1
Foo: xxx
Cookie: bar=yyy
```


## フォームコントロールの値

HTMLのform要素から送信される値をマッピングする。

```java
@POST
@Produces("application/x-www-form-urlencoded")
public String sayHello(
        @FormParam("name") String name) {
    ...
```

これは次のようなフォームコントロールに対応する。

```html
<form method="POST" action="/hello">
  <input type="text" name="name">
  ...
```


## バリューオブジェクトで受け取る

また、これまで紹介したアノテーションを使ったパラメータのマッピングではバリューオブジェクトを利用できる。

```java
public class ValueObject {
    private final String value;
    public ValueObject(String value) {
        this.value = value;
    }
    public String getValue() { return value; }
}
```

```java
@GET
public String get(
    @QueryParam("value") ValueObject vo) { ... }
```


## パラメータとして使えるバリューオブジェクト

* Stringの引数をひとつだけ受け取るpublicなコンストラクタを持つクラス
* Stringの引数をひとつだけ受け取る”valueOf”という名前のstaticファクトリメソッドを持つクラス
* Stringの引数をひとつだけ受け取る”fromString”という名前のstaticファクトリメソッドを持つクラス


## ParamConverter

また、ParamConverterを実装したクラスを用意すればどんなクラスもパラメータとして使える。

```java
public class LocalDateConverter implements ParamConverter<LocalDate> {

    @Override
    public LocalDate fromString(String value) {
        return Optional.ofNullable(value)
                       .map(LocalDate::parse)
                       .orElse(null);
    }

    @Override
    public String toString(LocalDate value) { ... }
}
```


### エンティティボディをそのまま受け取る

```java
@Path("echo")
public static class Echo {
    @POST @Consumes("text/plain")
    public String echo(String text) { //アノテーションを付けない
        return text;
    }
}
```

これは次のようなHTTPリクエストに対応する。

この場合は引数 text には "HelloWorld" が渡される。

```
POST /echo HTTP/1.1
Content-Type: text/plain
...(略)...

HelloWorld
```


## XMLをPOJOで受け取る

XMLならJAXBでPOJOに変換して受け取る機能が標準装備されている。

```java
@XmlRootElement
public static class Person {
    public String name;
    public int age;
}
```

```java
@POST
@Consumes("application/xml")
public void post(Person person) {
```

これは次のようなXMLを受け取る事ができる。

```xml
<person>
  <name>うらがみ</name>
  <age>31</age>
</person>
```


## JSONをPOJOで受け取る

@Consumes の中身を "application/json" に変えればJSONを受け取る事もできる。

```java
@POST
@Consumes("application/json")
public void post(Person person) {
```

これは次のようなXMLを受け取る事ができる。

```json
{"name":"うらがみ","age":31}
```

※ただしJAX-RSの仕様ではない


### JAX-RSアプリケーションを構成する要素

* リソースクラス
* **エンティティボディの読み込み・書き出し**
* **フィルター・インターセプター**
* 例外ハンドラー
* アプリケーションの定義

* クライアントAPI


### リクエストを処理する流れ

![a](/assets/flow00.png)

TODO: あとで絵をきれいにする


### 1. リクエストを受け付ける

![a](/assets/flow01.png)


### 2. @PreMatchingが付いたContainerRequestFilterでリクエストをフィルタリング

![a](/assets/flow02.png)

* この時点では実行されるリソースメソッドは決定していない
* HTTPメソッドやリクエストURLの上書きが可能


### 例：HTTPメソッドを上書きするContainerRequestFilter

```java
@PreMatching
public class PseudoHttpMethodFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        String method = rc.getHeaderString("X-Http-Method-Override");
        if (method != null) {
            rc.setMethod(method);
        }
    }
}
```


### 3. HTTPメソッドやURLでリソースメソッドとマッチング

![a](/assets/flow03.png)

ここでリクエストをハンドリングするリソースメソッドが決定される。


### 4. 残りのContainerRequestFilterでリクエストをフィルタリング

![a](/assets/flow04.png)


### 例：認証するContainerRequestFilter

```java
public class BasicAuthProvider implements ContainerRequestFilter {

    public void filter(ContainerRequestContext rc) throws IOException {
        String header = rc.getHeaderString("Authorization");
        if (header == null || authenticate(header) == false) {
            //認証失敗
            Response response = Response.status(401)
                    .header("WWW-Authenticate", "Basic Realm=secret")
                    .build();
            rc.abortWith(response);
        }
    }

    ...(略)...
}
```


### 5. ReaderInterceptorでエンティティボディを加工

![a](/assets/flow05.png)

* エンティティボディをJavaのオブジェクトに加工する前に加工できる
* 暗号化されたデータの複号化など


### 例：複号化するReaderInterceptor

```java
public class Decrypter implements ReaderInterceptor {

    public Object aroundReadFrom(ReaderInterceptorContext context)
            throws IOException, WebApplicationException {
        try {
            Cipher c = ...(略)...
            InputStream in = context.getInputStream();
            context.setInputStream(new CipherInputStream(in, c));
            return context.proceed();
        } catch (GeneralSecurityException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
```


### 6. MessageBodyReaderでエンティティボディをJavaのオブジェクトに変換

![a](/assets/flow06.png)

* 先に説明をしたXMLやJSONをPOJOで受け取る仕組みの正体
* これを実装すれば好きなフォーマットをPOJOで受け取れる


### 例：エンティティボディをPropertiesに変換するMessageBodyReader

```java
public class PropertiesReader implements MessageBodyReader<Properties> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Properties readFrom(Class<Properties> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException, WebApplicationException {
        Properties props = new Properties();
        props.load(entityStream);
        return props;
    }
}
```


### 7. リソースメソッドを実行

![a](/assets/flow07.png)


### 8. ContainerResponseFilterを実行

![a](/assets/flow08.png)

* リソースメソッドの戻り値がエンティティボディに書き出される前に加工できる


### 例：

TODO: あとで例コード書く


### 9. WriterInterceptorでエンティティを加工

![a](/assets/flow09.png)


### 例：

TODO: あとで例コード書く


### 10. MessageBodyWriterでエンティティをエンティティボディに書き出し

![a](/assets/flow10.png)

* MessageBodyReaderのレスポンス版
* リソースメソッドの戻り値のエンティティボディへの書き出し方をカスタマイズできる


### 例：Propertiesをエンティティボディに書き出すMessageBodyWriter

```java
public class PropertiesWriter implements MessageBodyWriter<Properties> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public long getSize(Properties t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public void writeTo(Properties t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException, WebApplicationException {
        t.store(entityStream, "");
    }
}
```


### 11. レスポンスを返す

![a](/assets/flow11.png)


### JAX-RSアプリケーションを構成する要素

* リソースクラス
* エンティティボディの読み込み・書き出し
* フィルター・インターセプター
* **例外ハンドラー**
* アプリケーションの定義

* クライアントAPI


## ExceptionMapper

* リソースメソッドで投げられた例外をハンドリングする

TODO: あとで例コード書く


### JAX-RSアプリケーションを構成する要素

* リソースクラス
* エンティティボディの読み込み・書き出し
* フィルター・インターセプター
* 例外ハンドラー
* **アプリケーションの定義**

* クライアントAPI


## Application

* JAX-RSアプリケーションの構成を表す
* サブクラスを作ってリソースクラスや各種プロバイダーを返すメソッドをオーバーライドする


## 例：Applicationサブクラス

```java
@ApplicationPath("api")
public class MyApplication extends Application {
    private final Set<Class<?>> classes = new HashSet<>();

    public MyApplication() {
        classes.add(Addition.class);
        classes.add(BasicAuthProvider.class);
        classes.add(PropertiesReader.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
```


### Java EE環境でのApplicationサブクラス

* Java EE環境下では @Path で注釈されたクラスをリソースクラス、@Provider で注釈されたクラスをプロバイダーと認識して自動で登録してくれる
* メソッドをオーバーライドしてリソースクラス・プロバイダーを登録する必要が無い

```java
@ApplicationPath("api")
public class MyApplication extends Application {
}
```


### JAX-RSアプリケーションを構成する要素

* リソースクラス
* エンティティボディの読み込み・書き出し
* フィルター・インターセプター
* 例外ハンドラー
* アプリケーションの定義

* **クライアントAPI**


## クライアントAPI

ビルダーパターンで書けるHTTPクライアント。

TODO: コード例を書く

---

## ここから実践の話


## ※おことわり

* 私の経験をお話するので、すべての人にとって良い方法というわけではありません
* Jersey、GlassFishに特化したお話もあります


## JAX-RS案件たち

* JAX-RS1, GlassFish3, HTML, Knockout
* JAX-RS1, GlassFish3, Android
* JAX-RS2, Tomcat7, Jersey MVC, JSP
* JAX-RS2, Payara, Android


## 実践の話

* 認証
* バリデーション
* 宣言的トランザクション
* 大量データのダウンロード
* MVCと状態の持ち方
* テストコード

---

## 実践の話

* **認証**
* バリデーション
* 宣言的トランザクション
* 大量データのダウンロード
* MVCと状態の持ち方
* テストコード


### 認証はどのように行うのが良いか？


### Java EEにはレルム認証がある


## レルム認証の手順

* アプリケーションサーバ側でレルムの設定を行う
* web.xmlでsecurity-constraint要素を書いてURLで認証対象範囲を指定する
* コードを変更する必要は無い


## レルム認証への期待

* アプリケーションサーバに依存しなくて高ポータビリティ
* デプロイされたすべてのアプリケーションに適用できて高再利用性
* アプリケーションを変更せず認証ロジックを変更できる


## レルム認証の実際……

* アプリケーションサーバを変更する事は無い
* 1つのWARしかデプロイしない
* 認証ロジックを変更したい事は無い


## JAX-RSのフィルタで認証

* ContainerRequestFilterで認証を行う
* 認証対象範囲はアノテーションで指定する


### 再掲：認証するContainerRequestFilter

```java
public class BasicAuthProvider implements ContainerRequestFilter {

    public void filter(ContainerRequestContext rc) throws IOException {
        String header = rc.getHeaderString("Authorization");
        if (header == null || authenticate(header) == false) {
            //認証失敗
            Response response = Response.status(401)
                    .header("WWW-Authenticate", "Basic Realm=secret")
                    .build();
            rc.abortWith(response);
        }
    }

    ...(略)...
}
```


### ContainerRequestFilterで認証を行うメリット

* 認証対象範囲をURLではなくアノテーションで指定できる
  (対象範囲がより明確になる)
* テストコードが書きやすい


### ContainerRequestFilterで認証を行うメリット

* **認証対象範囲をURLではなくアノテーションで指定できる**
  (対象範囲がより明確になる)
* テストコードが書きやすい


### フィルターの適用範囲をアノテーションで指定

@NameBinding で注釈したアノテーションを用意する。

```java
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface Secure {
}
```

このアノテーションでフィルターとリソースクラスを結びつける。

```java
@Secure
public class BasicAuthProvider implements ContainerRequestFilter { ... }
```

```java
@Secure
@Path("hoge")
public class HogeResource { ... }
```

---

## 実践の話

* 認証
* **バリデーション**
* 宣言的トランザクション
* 大量データのダウンロード
* MVCと状態の持ち方
* テストコード


### バリデーションをどのように行えば良いか？


### JAX-RSのバリデーション事情

* JAX-RSにはバリデーションの仕様が無い
* Java EEにはBean Validationというバリデーションの仕様がある
* Java EE環境であればリソースメソッド実行時に引数に対してBean Validationでバリデーション出来る


### 例：引数を直接バリデーションする

@NotNull で name が null でない事をバリデーションする。

```java
@GET
public String sayHello(
        @NotNull @QueryParam("name") String name) {
    return String.format("Hello, %s!", name);
}
```


### 例：引数のフィールドをバリデーションする

```java
@POST
@Produces(MediaType.APPLICATION_XML)
public void create(@Valid Person person) { ... }
```

```java
@XmlRootElement
public static class Person {

    @NotNull
    public String name;

    @Min(0)
    public int age;
}
```


### バリデーションでinvalidだった場合

* ConstraintViolationException が投げられる
* 何もしなければ 400 Bad Request が返される
* エラーメッセージを返したい場合などは ExceptionMapper を使う

---

## 実践の話

* 認証
* バリデーション
* **宣言的トランザクション**
* 大量データのダウンロード
* MVCと状態の持ち方
* テストコード


## 宣言的トランザクションとは

* あるメソッドを呼び出したときにトランザクションを開始
* メソッドを終了するときにトランザクションをコミット
* メソッドから例外が投げられたときはロールバック


### リソースメソッドで宣言的トランザクションをする方法

1. リソースクラスをCDI管理ビーンにして
2. @Transactional を付けるのが簡単

なお、リソースクラスをCDI管理ビーンにするには @RequestScoped で注釈する。


### 例：宣言的トランザクション

```java
@RequestScoped
@Transactional
@Path("sample")
public class SampleResource { ... }
```

このクラスに定義されるリソースメソッドはすべて宣言的トランザクションを行う。


### リソースメソッドで宣言的トランザクションをした場合……

* 1リクエスト内で複数のトランザクションを扱いたい場合やそもそもトランザクションを開始したくない場合に面倒
* 宣言的トランザクションを行うリソースクラスと行わないリソースクラスの混在


### 改善案

1. 宣言的トランザクションを行うCDI管理ビーンを作成し
2. リソースクラスにインジェクションする
3. リソースメソッドからはそのクラスのメソッドを呼ぶ


### 例

```java
@ApplicationScoped
@Transactional
public class SampleService { ... }
```

```java
@RequestScoped
@Path("sample")
public class SampleResource {
    @Inject
    private SampleService service;

    @POST
    @Consumes("application/xml")
    public void create(Sample sample) {
        service.create(sample);
    }
}
```


### リソースクラスと宣言的トランザクション

* リソースクラスはトランザクション境界にしない方が良い
* あくまでもHTTPとJavaコードを繋ぐものと捉える
* ビジネスロジックはリソースクラス以降のレイヤーにする


## TransactionalException

* @Transactionalが付いたメソッド内で投げられた例外はTransactionalExceptionでラップされる
* ExceptionMapperで狙った例外を捕捉できない
* ProvidersとException.getCauseでハンドラを探索する

---

## 実践の話

* 認証
* バリデーション
* 宣言的トランザクション
* **大量データのダウンロード**
* MVCと状態の持ち方
* テストコード


## 大量データのダウンロード

* CSVダウンロードなど
* 素直に書くとリソースメソッド内で時間をかけて取得した大量データを戻り値にする


## コード例

```java
@GET @Produces("text/csv")
public Response downloadCsv() {

    // 長い時間がかかり、大量データを扱う処理
    String csv = createCsv();

    String filename = "sample.csv";
    return Response.ok(csv).header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + filename).build();
}
```


## 問題点

* 長い時間がかかるのでリクエストを捌くスレッドを拘束してしまう
* 大量データを扱うのでヒープを圧迫してしまう


## 解決策：非同期処理

* まずはスレッドを占有する問題を解決する
* リソースメソッドは別スレッドで処理を行ってレスポンスを構築する事ができる

```java
@GET @Produces("text/csv")
public void downloadCsv(@Suspended AsyncResponse ar) {
    // 別スレッドで処理を実行する
    runOtherThread(() -> {
        String csv = createCsv();

        Response response = ...(省略)...
        // 戻り値にしていたオブジェクトを
        // AsyncResponse.resumeに渡す
        ar.resume(response);
    });
}
```


## でもJava EEでは……

* データソースやトランザクションをスレッドに関連付けて管理しているのでカジュアルにスレッドを生成してはいけない

```java
private void runOtherThread(Runnable r) {
    // ×new Threadしてはいけません！
    new Thread(r).start();
}
```


## そこで

* Concurrency Utilities for Java EEとCDIを使う
* Java EE環境でExecutorServiceを使えるようにする仕様
* ManagedExecutorServiceをCDI管理ビーンにしてリソースクラスにインジェクションする

```java
@Inject
private ManagedExecutorService executor;

private void runOtherThread(Runnable r) {
    executor.submit(r);
}
```

これで長い時間がかかる処理を別スレッドで実行できる


## ※注意点

* リクエストを捌くスレッドではないので @RequestScoped の範囲外
* @RequestScoped な情報は非同期タスクをインスタンス化するときに渡す必要がある


## 解決策：StreamingOutput

* 次に大量データでヒープを圧迫する問題を解決する
* 少しずつデータをフェッチする事が出来るならStreamingOutputを利用すればヒープを節約できる

```java
StreamingOutput so = (OutputStream out) -> {
    while(csv.fetch()) {
        out.write(csv.nextLine());
    }
};

Response response = Response.ok(so).build();
```


## StreamingOutputのデメリット

* OutputStreamにレスポンスのエンティティボディを直接書き出すのでMessageBodyWriterの恩恵を受けられない


## 最終的なコード例

```java
@Inject private ManagedExecutorService executor;

@GET @Produces("text/csv")
public void downloadCsv(@Suspended AsyncResponse ar) {
    executor.submit(() -> {
        StreamingOutput so = (OutputStream out) -> {
            Csv csv = createCsv(); // 長い時間がかかる処理
            while (csv.fetch()) { // 少しずつフェッチして書き出す
                out.write(csv.nextLine());
            }
        };
        String filename = "sample.csv";
        Response response = Response.ok(so).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename).build();
        ar.resume(response);
    });
}
```

---

## 実践の話

* 認証
* バリデーション
* 宣言的トランザクション
* 大量データのダウンロード
* **MVCと状態の持ち方**
* テストコード


## MVCと状態の持ち方

* 冒頭で述べたようにJAX-RSはRESTful APIを作るのに向いている
* RESTful APIでは状態を持たない
* でもテンプレートエンジンと組み合わせて画面有りのアプリケーションを作れない事はない
* 画面がある業務アプリケーションを作っていると状態を持ちたい事がある


## Jersey MVC

* テンプレートエンジンと組み合わせるJersey拡張
* *JAX-RSの仕様ではない*
* リソースメソッドの戻り値を Viewable にしてテンプレートのパスとバインドする値を返す


## コード例

```java
@GET
public Viewable index() {
    Parameters params = ...(略)...
    return new Viewable("path/to/template", params);
}
```

テンプレート側ではバインドされた値を it という名前で参照できる。


## 状態の持ち方

* HttpSessionを使う


## コード例

```java
@GET
public Viewable get(@Context HttpServletRequest request) {
    HttpSession session = request.getSession();
    String userId = session.getAttribute("userId");
    return new Viewable("path/to/template", userId);
}
```


## ※この方法はおすすめしません

* ServletはJAX-RSよりも低レイヤーなAPI
* HttpSessionへの値の登録・取得は型安全ではない
* 個人的にはServlet APIは使ったら負け、ぐらいに考えている


## おすすめの方法

1. リソースクラスをCDI管理ビーンにして
2. @SessionScopedなCDI管理ビーンをインジェクションする


## コード例

```java
@RequestScoped
@Path("sample")
public class Sample {

    @Inject
    private UserInfo user;

    @GET
    public Viewable get() {
        return new Viewable("path/to/template", user.getId());
    }
}
```


## メリット

* 型安全
* モックをインジェクションし易く、テストコードが書き易い

---

## 実践の話

* 認証
* バリデーション
* 宣言的トランザクション
* 大量データのダウンロード
* MVCと状態の持ち方
* **テストコード**


## テストしたい対象

* ルーティング
* クエリパラメータやフォームパラメータの受け取り
* エンティティの変換
* フィルタやインターセプタの適用


## Jersey Test Frameworkを使う


## Jersey Test Frameworkとは

* JUnit、TestNGでJerseyのテストコードを書くためのフレームワーク
* テスト実行前後にJerseyアプリケーションの起動・終了を行ってくれる
* クライアントAPIへのショートカットとなるメソッドを用意


## コード例

```java
public class AdditionTest extends JerseyTest {

    // テスト対象となるアプリケーションの定義
    @Override protected Application configure() {
        return new ResourceConfig(Addition.class);
    }

    @Test public void testCalculate() throws Exception {
        // クライアントAPIを使って検証
        int result = target("/addition")
                        .queryParam("a", 1).queryParam("b", 2)
                        .request().get(int.class);
        assertThat(result, is(3));
    }
}
```


### Jersey Test Frameworkのテストについて

* ルーティングやパラメータのバインドをテスト
* ビジネスロジックはCDI管理ビーンに切り出してテストではモックをインジェクションする
* JAX-RSはHTTPとJavaコードのマッピングに徹してビジネスロジックは別のレイヤーで行う事が大切


## テスト時のDIについて

* HK2を利用すると良い
* Jerseyが依存しているDIコンテナでJSR 330を実装している
* CDIと同じく@Injectでインジェクションができる


## コード例

```java
@RequestScoped
@Path("todo/{id}")
public class TodoResource {

    @Inject
    private TodoService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Todo get(@PathParam("id") int id) {
        return service.find(id);
    }
}
```


## コード例

```java
public class TodoResourceTest extends JerseyTest {

    @Test
    public void testGet() { ...(略)... }

    protected Application configure() {
        Binder binder = new AbstractBinder() {
            protected void configure() {
                bind(MockTodoService.class).to(TodoService.class);
            }
        };
        return new ResourceConfig(TodoResource.class).register(binder);
    }
}
```


### DIを伴うテストを書くときのポイント

* テストが終わったらインスタンスは破棄されるのでライフサイクルは気にしない
* 裏を返せばライフサイクルに依存しないコードを書くよう心がける
* HTTPとのマッピング以上のテストはしない

---

## まとめ

TODO: あとで書く

* JAX-RSはRESTful APIを作るのに特化したフレームワーク
* あくまでもHTTPをJavaコードにマッピングするだけ
* 他のレイヤーと統合するにはDIコンテナを利用する
* Jersey Test Frameworkでテストは簡単に書ける


## まとめ

TODO: あとで書く

* JAX-RSだけではカバーできない領域がある
* 工夫と試行錯誤でそれらを乗り越えてきた
* 好きになれるフレームワークを使う事が大切


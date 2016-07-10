class: center, middle

# Domaの紹介

---

### の前に、自己紹介

* うらがみ⛄️
* 大阪の中小SIer
* Doma、JAX-RS(Java EE)

---

### Domaとは

* JDBCを利用したデータベースアクセスライブラリ
--

* Pluggable Annotation Processing APIを使ってコンパイル時にコード生成したり色んなチェックを行う
--

* コピペでそのまま試せるSQLテンプレート(2 Way SQL)

---

class: center, middle

## まずはセットアップ

---

### セットアップ

* JARファイルを1つ入れるだけ(`doma-x.y.z.jar`)
--

* 他のJARへの依存なし(嬉しい)
--

* Eclipseでは注釈処理の設定をする必要がある
--

* [Doma Tools(Eclipse)](http://doma.readthedocs.io/ja/stable/getting-started/#eclipse-doma-tools)
--

* [DomaSupport(IntelliJ IDEA)](https://github.com/siosio/DomaSupport)
--

* NetBeansは……😢

---

### Gradleでビルド

ちょっと工夫が必要
--

* デフォルトではJavaファイルのコンパイル、リソースのコピー、という順
--

* 注釈処理が動く時にSQLファイルのコピーが済んでいなくてコンパイルエラーになる
--

* コンパイルとリソースコピーの順を入れ替える必要がある
--


```groovy
compileJava.dependsOn processResources
```

---

### Gradleでビルド

* デフォルトではクラスファイルの出力先とリソースのコピー先が異なる
--

* コンパイル時、リソースのコピー先はクラスパスに含まれず、SQLファイルを検出できない
--

* リソースのコピー先をクラスファイルの出力先と同じにする
--


```groovy
processResources.destinationDir = compileJava.destinationDir
```

---

class: center, middle

## 各クラスと役割

---

### 作るクラスとか

* 設定クラス
* エンティティ
* エンティティリスナー
* DAO
* SQLファイル
* ドメインクラス
* エンベッダブルクラス

---

### 絶対に必要なもの

* **設定クラス**👈
* **エンティティ**👈
* エンティティリスナー
* **DAO**👈
* **SQLファイル**👈
* ドメインクラス
* エンベッダブルクラス

---

### 無くても良いけどあると便利なもの

* 設定クラス
* エンティティ
* **エンティティリスナー**👈
* DAO
* SQLファイル
* ドメインクラス
* **エンベッダブルクラス**👈

---

### 無くても良いけど無いと耐えられないもの

* 設定クラス
* エンティティ
* エンティティリスナー
* DAO
* SQLファイル
* **ドメインクラス**👈
* エンベッダブルクラス

---

### 設定クラス

* `DataSource`や`Dialect`(ページネーションなどでRDBMS間の方言を吸収するための`interface`)を設定するクラス
* 通常、アプリケーション内で使用するDBにつき1つ必要

---

### 設定クラスの作り方

* `Config`を実装する

```java
public class MyConfig implements Config {

    @Override
    public DataSource getDataSource() { ... }

    @Override
    public Dialect getDialect() { ... }

    //必要に応じて他のメソッドもoverrideする
}
```

---

### シングルトンな設定クラス

* `@SingletonConfig`で注釈すればファクトリーメソッドでインスタンスを取得することもできる

```java
@SingletonConfig
public class MyConfig implements Config {

    public static MyConfig singleton() { ... }

    //コンストラクタはprivateにする
    private MyConfig() {}

    ...
}
```

---

### エンティティ

* テーブルや検索結果にマッピングするクラス
* クラスがテーブル(検索結果)、フィールドがカラムに対応する

---

### エンティティの作り方

* `@Entity`で注釈する
* アクセサは無くてもOK

```java
@Entity
public class Hoge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String foo;
    public LocalDate bar;
}
```

---

### エンティティのフィールドに使える型

* `int`、`String`、`byte[]`、`java.sql.Date`など
* `LocalDate`、`LocalTime`、`LocalDateTime`
* ドメインクラス
* エンベッダブルクラス
* これらの`Optional<T>`

---

### イミュータブルなエンティティ

* `immutable`要素を`true`にする

```java
@Entity(immutable = true)
public class Hoge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public final Long id;
    public final String foo;
    public final LocalDate bar;
    public Hoge(Long id, String foo, LocalDate bar) {
        ...
    }
}
```

---

### エンティティリスナー

* 挿入・更新・削除の前後に処理を挟む

---

### エンティティリスナーの作り方

```java
public class HogeListener implements EntityListener<Hoge> {

    @Override
    public void preInsert(Hoge entity,
                          PreInsertContext<Hoge> context) {
        entity.createdAt = LocalDateTime.now();
    }

    //preUpdate, preDelete
    //postInsert, postUpdate, postDelete
}
```

---

### エンティティリスナーの利用設定

* エンティティの`@Entity`で利用の設定を行う

```java
@Entity(listener = HogeListener.class)
public class Hoge {
    ...
}
```

---

### DAO

* エンティティを操作するための`interface`
* クエリ発行のエントリポイントにもなる

---

### DAOの作り方

* `@Dao`で注釈する
* `config`要素に`Config`実装クラスを指定する

```java
@Dao(config = MyConfig.class)
public interface HogeDao {
    ...
}
```

---

### Configを指定しないDAO

* `Config`を指定しなかったら生成される実装クラスに`Config`を引数にとるコンストラクタが作られる

```java
@Dao
public interface HogeDao {
    ...
}
```

```java
public class HogeDaoImpl extends AbstractDao implements HogeDao {
    public HogeDaoImpl(Config config) { ... }
    ...
}
```

---

### 検索系DAOメソッド

* 引数はわりといろんな型がいける
* 戻り値は基本型、エンティティなど

```java
@Select
Hoge selectById(Long id);
```

```java
@Select
List<Hoge> selectAll();
```

---

### Java 8に対応したDAOメソッド

* 戻り値の型を`Optional`で包んだり
* `Stream`や`Collector`を使用できる

```java
@Select
Optional<Hoge> selectById(Long id);
```

```java
@Select(strategy = SelectType.STREAM)
<R> R selectAll(Function<Stream<Hoge>, R> f);
```

```java
@Select(strategy = SelectType.COLLECT)
<R> R selectAll(Collector<Hoge, ?, R> collector);
```

---

### Stream検索

`Stream`を使用した検索は2種類

```java
@Select(strategy = SelectType.STREAM)
<R> R selectAll(Function<Stream<Hoge>, R> f);
```

```java
@Select
Stream<Hoge> selectAll();
```
--

後者の方がシグネチャがすっきり？

---

### Stream検索

使ってみると大差ない

```java
//<R> R selectAll(Function<Stream<Hoge>, R> f);
int total = selectAll(s -> s.mapToInt(Hoge::getBar).sum());
```

```java
//Stream<Hoge> selectAll();
int total = selectAll().mapToInt(Hoge::getBar).sum();
```
--

前者は`ResultSet`を自動で`close`してくれる。

後者はしてくれない！

---

### Stream検索

`try-finally`で囲って使うなど、必ず`close`するよう心がけましょう。

```java
//Stream<Hoge> selectAll();
int total;
try (Stream<Hoge> s = selectAll()) {
    total = s.mapToInt(Hoge::getBar).sum();
}
```

---

### 更新系DAOメソッド

```java
@Insert //@Update @Delete
int insert(Hoge entity);
```

```java
@Insert(sqlFile = true)
int insert(String foo, LocalDate bar);
```

```java
@BatchInsert
int[] insert(List<Hoge> entities);
```

---

### SQLファイル

* 2-way SQLが書かれたファイル
* DAOメソッド名に対応したファイルパス
* 例えば`foo.bar.HogeDao.select`メソッドなら`META-INF/foo/bar/HogeDao/select.sql`

---

### 2-way SQLの書き方

```sql
  SELECT /*%expand*/*
    FROM Hoge
   WHERE foo = /* foo */'a'
     AND bar LIKE /* @prefix(bar) */'b%'
     AND baz IN /* cond.bazList */(1, 2, 3)
```

```java
@Select
List<Hoge> select(String foo, String bar, Cond cond);

public static class Cond {
    public List<Integer> bazList;
}
```

---

### 発行されるプリペアードSQL

```java
Cond cond = new Cond();
cond.bazList = Arrays.asList(100, 200);
dao.select("a", "b", cond);
```

```sql
  SELECT foo, bar, baz
    FROM Hoge
   WHERE foo = ?
     AND bar LIKE ?
     AND baz IN (?, ?)
```

`"a"`、`"b%"`、`100`、`200`がバインドされる。

---

### その他の埋め込み

リテラル変数コメント、埋め込み変数コメント

```sql
WHERE foo = /*^ foo */'x'
/*# orderBy */
```

```java
dao.select("a", "ORDER BY foo ASC");
```
--

プレースホルダではなく、SQLに直接埋め込まれる。

```sql
WHERE foo = 'a'
ORDER BY foo ASC
```

---

### 条件分岐、ループ

```sql
WHERE
/*if foo != null*/
    foo = /* foo */'x'
/*end*/

and (
/*%for bar : bars */
    bar = /* bar */'y'
    /*%if bar_has_next */
        /*# "or" */
    /*%end */
/*%end*/
)
```

---

### 他にも

* バインドされる変数のメソッドを呼べたり
--

* `static`メソッドを呼べたり
--

* `if`コメントの条件を論理演算で幾つも書けたり
--

* もちろん、コンパイル時に型チェックしてくれる
--

* でも、やりすぎると大変なのでほどほどに

---

### ドメインクラス

* 基本型を具体的な型に落とし込むためのクラス
* 例：「郵便番号」を`String`ではなく`ZipCode`で

---

### ドメインクラスの作り方

* `@Domain`で注釈して`valueType`要素に基本型を指定する

```java
@Domain(valueType = String.class)
public class Fuga {
    private final String value;
    public Fuga(String value) {
        this.value = Objects.requireNonNull(value);
    }
    public String getValue() {
        return value;
    }
}
```

---

### 外部ドメイン

* 編集できない既存クラスもドメインクラス化

```java
@ExternalDomain
public class PathConverter implements
                DomainConverter<Path, String> {
    @Override
    public String fromDomainToValue(Path domain) {
        return Optional.ofNullable(domain)
            .map(Path::toString).orElse(null);
    }
    @Override
    public Path fromValueToDomain(String value) {
        return Optional.ofNullable(value)
            .map(Paths::get).orElse(null);
    }
}
```

---

### 外部ドメインの利用方法

コンバータをまとめたクラスを作って、


```java
@DomainConverters({ PathConverter.class })
public class DomainConvertersProvider {
}
```
--

コンパイル時の注釈処理オプションで指定する。

```sh
javac -Adoma.domain.converters=foo.bar.DomainConvertersProvider ...
```

---

### エンベッダブルクラス

* 複数のカラムをまとめたクラス
* 例：「都道府県」「市区町村」「番地」をまとめて「住所」のエンベッダブルクラスを作る

---

### エンベッダブルクラスの作り方

* `@Embeddable`で注釈する

```java
@Embeddable
public class Password {
    private final PasswordHash hash;
    private final Salt salt;
    private final HashAlgorithm hashAlgorithm;

    public Password(PasswordHash hash, Salt salt, HashAlgorithm hashAlgorithm) {
        ...
    }
```

---

class: center, middle

## コンパイル時チェック

---

### 注釈処理でコンパイル時に色々チェック

* Domaに慣れないうちはコンパイルエラーを解消していくことでDomaを学べる(はず)
* 手厚いチェックと分かりやすいエラーメッセージ

---

### エンティティのフィールドが扱えない型だとコンパイルエラー

```java
@Entity
public class SomeEntity {
    //基本型・ドメインクラス・エンベッダブルクラス
    //でなければコンパイルエラー
    public InvalidClass field;
}
```

---

### エンティティのフィールドが扱えない型だとコンパイルエラー

> [DOMA4096] クラス[InvalidClass]は、永続対象の型としてサポートされていません。 at SomeEntity.field。@ExternalDomainでマッピングすることを意図している場合、登録や設定が不足している可能性があります。@DomainConvertersを注釈したクラスと注釈処理のオプション（doma.domain.converters）を見直してください。

---

### DAOメソッドの戻り値が扱えない型だとコンパイルエラー

```java
@Dao
public interface SomeDao {

    //基本型・ドメインクラス・それらのリスト
    //それらのOptionalでなければコンパイルエラー
    @Select
    List<InvalidClass> selectAll();
}
```

---

### DAOメソッドの戻り値が扱えない型だとコンパイルエラー

> [DOMA4007] 戻り値のjava.util.Listに対する実型引数の型[InvalidClass]はサポートされていません。

---

### DAOメソッドに対応したSQLファイルがクラスパス上に無いとコンパイルエラー

```java
package foo.bar;

@Dao
public interface HogeDao {

    //META-INF/foo/bar/HogeDao/selectAll.sql
    //が無ければコンパイルエラー
    @Select
    List<SomeEntity> selectAll();
}
```

---

### DAOメソッドに対応したSQLファイルがクラスパス上に無いとコンパイルエラー

> [DOMA4019] ファイル[META-INF/foo/bar/HogeDao/selectAll.sql]がクラスパスから見つかりませんでした。ファイルの絶対パスは"/path/to/META-INF/foo/bar/HogeDao/selectAll.sql"です。	

---

### DAOメソッドの引数をクエリで使用していないとコンパイルエラー

```java
@Select
Hoge selectById(Long id);
```

```sql
  SELECT /*%expand*/*
    FROM Hoge
-- 下記のように引数を使わないとコンパイルエラー
-- WHERE id = /*id*/1
```

---

### DAOメソッドの引数をクエリで使用していないとコンパイルエラー

> [DOMA4122] SQLファイル[META-INF/foo/bar/HogeDao/selectById.sql]の妥当検査に失敗しました。メソッドのパラメータ[id]がSQLファイルで参照されていません。

---

### クエリで使用しているバインド変数がDAOメソッドで宣言されていないとコンパイルエラー

```sql
SELECT /*%expand*/*
  FROM Hoge
 WHERE id = /*id*/1
```

```java
//引数idが宣言されていないのでコンパイルエラー
@Select
Hoge selectById();
```

---

### クエリで使用しているバインド変数がDAOメソッドで宣言されていないとコンパイルエラー

> [DOMA4092] SQLファイル[META-INF/foo/bar/HogeDao/selectById.sql]の妥当検査に失敗しました（[3]行目[18]番目の文字付近）。詳細は次のものです。[DOMA4067] SQL内の変数[id]に対応するパラメータがメソッドに存在しません（[2]番目の文字付近）。 SQL[SELECT /*%expand*/*
>   FROM Hoge
>  WHERE id = /*id*/1
> ]。

---

### ドメインクラスに必要なファクトリーメソッドや、アクセサが無いとコンパイルエラー

```java
@Domain(valueType = String.class)
public class Hoge {
    private String value;

    //Stringを受け取るコンストラクタが
    //無いとコンパイルエラー

    //getValueが無いとコンパイルエラー
}
```

---

### ドメインクラスに必要なファクトリーメソッドや、アクセサが無いとコンパイルエラー

> [DOMA4103] 型[java.lang.String]をパラメータにもつ非privateなコンストラクタが見つかりません。コンストラクタを定義するか、ファクトリメソッドを利用したい場合は@DomainのfactoryMethod属性にメソッド名を指定してください。

---

### ドメインクラスに必要なファクトリーメソッドや、アクセサが無いとコンパイルエラー

> [DOMA4104] アクセッサメソッド[getValue]が見つかりません。アクセッサメソッドは、型[java.lang.String]を戻り値とする非privateで引数なしのインスタンスメソッドでなければいけません。 at Hoge

---

class: center, middle

## その他の話題

---

### ローカルトランザクション

* スレッドに紐付けてトランザクションを扱うAPI
--

* Java SE環境で使う場合なんかに便利
--

* `DataSource`を`LocalTransactionDataSource`でラップする
--

* `TransactionManager`経由で使うのが楽

---

### ローカルトランザクションを使う準備

```java
private LocalTransactionDataSource dataSource;

public MyConfig() {
    DataSource original = ...
    dataSource = new LocalTransactionDataSource(original);
}

@Override
public DataSource getDataSource() { return dataSource; }

@Override
public TransactionManager getTransactionManager() {
    return new LocalTransactionManager(
        dataSource.getLocalTransaction(getJdbcLogger()));
}
```

---

### ローカルトランザクションを使う

ローンパターンでトランザクション境界を作っている。

```java
Config config = Config.get(dao);
TransactionManager tm = config.getTransactionManager();
tm.required(() -> {
    //ブロックを抜ければコミット
    //ただし、例外で抜ければロールバック
});
```

---

### 拡張ポイント

* `QueryImplementers`と`CommandImplementers`
--

* DAOメソッドは`Query`を組み立てて、それを引数にして`Command`を組み立てて、`Command`を実行する、という流れ
--

* `Query`や`Command`のインスタンス化処理をオーバーライドできる。
--

* SQLの変換や、クエリ発行先の切り替え(検索はスレーブ、更新はマスター、とか)など

---

### 最近のDoma
--

* エンベッダブルクラス
--

* Kotlinサポート

---

### DIコンテナとDoma
--

* 管理すべきもの: 設定クラス、DAO
--

* 管理した方が良さそう: エンティティリスナー
--

* 管理してはいけないもの: エンティティ、ドメインクラス、エンベッダブルクラス

---

### Doma統合
--


* Spring Boot([doma-spring-boot-starter](https://github.com/domaframework/doma-spring-boot))
--

* [enkan](https://github.com/kawasima/enkan)

---

### 最後にしれっと個人的な要望

* エンティティをDAO内で定義したい
* 主キー検索クエリは自動で組み立てたい
* <del>SQLファイルは`foo/bar/HogeDao_select.sql`という名前にしたい</del>

  * https://twitter.com/glory_of/status/751655497397264384
  * https://twitter.com/glory_of/status/751656837301170176

---

## この資料について

* Author: [@backpaper0](https://github.com/backpaper0)
* License:  [The MIT License](https://opensource.org/licenses/MIT)

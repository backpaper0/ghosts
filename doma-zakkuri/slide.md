class: center, middle

# ざっくりわかるDoma

---

### 自己紹介

* うらがみ⛄️
* 大阪でSIerをしているJavaプログラマ
* Domaヘビーユーザー・アーリーアダプター

???

仕事で初めてDomaを使ったのが2010-07-11。

それ以来、自分に決定権がある場合はほとんどDomaを使っている。
ほとんどの場合、自分に決定権がある。
ちなみにDoma以外だとJPAを使う場合もある。

---

### Domaとは

* Pluggable Annotation Processing APIを利用
* コンパイル時にコード検証、コード生成
* 他のJARに依存しない
* Java 8以降に対応
* ORMではない
* あえて言うならResultSet Mapper
* SELECTクエリはSQLファイルを書く
* 型を大事に

???

Java 6から入った機能。Processorというインターフェースを実装して、指定したアノテーションを付けたクラスの情報を元にしてコンパイル時に追加処理を行える。
有名なところで言えば、Dagger 2、Lombokがある。

それを利用してコンパイル時にコードの規約やSQLファイルの内容を検証したり、後述するDaoインターフェースの実装クラスを生成したり。

必要なのはdoma-x.y.z.jarだけ。

日付を扱う場合にLocalDateが使える。
ちなみにDoma 1はJava 6以降に対応。

---

### 今日話すこと

* 機能一巡り
* ドメインクラス
* コンパイル時検査
* インテグレーション

---

class: center, middle

## 機能一巡り

---

### 最初の一歩

* `Config`実装クラスを作る
* エンティティを作る
* Daoインターフェースを作る
* それらを使ってDB操作

---

### Config

```java
public class MyConfig implements Config {

    @Override
    public DataSource getDataSource() { ... }

    @Override
    public Dialect getDialect() { ... }
}
```

???

Domaの設定を行うクラス。Domaは設定ファイルではなくJavaコードで設定を行う。

最低限getDataSourceとgetDialectを実装すれば良い。

Dialectはクエリの自動構築などでRDBMSの差異を吸収するためのインターフェース。

Configは他にも色々な設定ができる。

---

### エンティティ

```java
@Entity
public class Account {

    @Id
    public Long id;

    public String name;

    @Version
    public Long version;
}
```

???

テーブルを表したり、検索結果をマッピングするクラス。

---

### エンティティ

```java
@Entity(immutable = true)
public class Account {
    @Id
    public final Long id;
    public final String name;
    @Version
    public final Long version;

    public Account(Long id, String name, Long version) {
        ...
    }
}
```

???

@Entityのimmutable要素をtrueにすればイミュータブルなエンティティを作ることもできる。

---

### Dao

```java
@Dao(config = MyConfig.class)
public interface AccountDao {

    @Select
    Account selectById(Long id);

    @Insert
    int insert(Account entity);
}
```

???

**目安：6分**

データベース操作を行うためのインターフェース。

実装クラスはAnnotationProcessorでコンパイル時に生成される。

@Daoのconfig要素でConfig実装クラスを指定しており、これは生成されるDao実装クラス内でnewされる。
config要素を指定しない場合はConfigを受け取るコンストラクタが生成されるので、DIコンテナ環境ならコンストラクタインジェクションすると良い。

---

### Daoとエンティティを使ってDB操作

```java
AccountDao dao = new AccountDaoImpl();

Account entity = dao.selectById(id);

entity.name = "うらがみ";

dao.update(entity);
```

???

Dao実装クラスをnewしているけれど、DIコンテナ環境ならインジェクションするのが良い。

---

### 2 way SQL

```java
package com.example.dao;

@Dao
public interface AccountDao {

    @Select
    Account selectById(Long id);
}
```

???

Domaで検索クエリを発行する場合、Daoインターフェースに@Selectを付けたメソッドを定義して、対応するSQLファイルを準備する。

---

### 2 way SQL

`com.example.dao.AccountDao#selectById`

に対応するSQLファイルは

`META-INF/com/example/dao/AccountDao/selectById.sql`

???

SQLファイルのファイルパスはDaoインターフェースの完全修飾名とメソッド名から決定される。

---

### 2 way SQL

```sql
SELECT /*%expand*/*
  FROM account
 WHERE id = /* id */0
```

* SQLのコメントの形式で値のバインドや制御文を記述
* SQLファイルの中身をコピペでクエリ発行できる

???

Domaの2 way SQLはSQLのコメントの形式で値のバインドや制御文を記述する。

この例だと1行目、expandは戻り値がエンティティの場合に使える。
エンティティのフィールドを列挙したクエリに変換される。

3行目、メソッドのパラメータをバインドする。

---

### 2 way SQL

```sql
SELECT /*%expand*/*
  FROM account
 WHERE id = /* id */0
```

👇

```sql
SELECT id, name, version
  FROM account
 WHERE id = ?
```

???

このようなプリペアードクエリになる。

バインドなどをコメント形式で行うので、SQLファイルの中身をコピペしてosqleditとかManagement Studioでクエリ発行できる。
大まかな確認が簡単。

私はすべてのSQLファイルをとりあえずそのまま流すテストを書く。
データベースの変更に対する不安軽減。

---

### 2 way SQL

```sql
 WHERE id IN /* idList */(0)
   AND name LIKE /* @prefix(name) */'x%'
/*%if version != null*/
   AND version = /* version */1
/%end*/
```

???

他にもIterableをIN句にバインドしたり、LIKE用の関数を使ったり、分岐したり、サンプルは書いていないけれどforループしたりできる。

prefix関数は変数をバインドする時に自動で末尾に%をつけてくれる。

ただしifやforなどの制御文はやりすぎ注意。

---

### 2 way SQL

```sql
 WHERE id IN /* idList */(0)
```

```sql
 -- Arrays.asList(1, 2, 3)
 WHERE id IN (?, ?, ?)
```

```sql
 -- Collections.emptyList();
 WHERE id IN (NULL)
```

???

ちなみにIN句へバインドする値が空ならIN (NULL)となり条件の評価が必ずTRUEにならないように変換される。

NodePreparedSqlBuilder#handleIterableValueNode

---

### Daoでできること

* 1件検索（エンティティ、`Optional`）
* 複数件検索（`List`、`Stream`、`Collector`）
* 検索オプション（`offset`、`limit`、悲観排他、カウント）
* 挿入/更新/削除のクエリ自動構築
* 挿入/更新/削除をSQLファイルで
* 挿入/更新/削除のイベンドハンドリング
* バッチ挿入/バッチ更新/バッチ削除
* ストアドファンクション/ストアドプロシージャ
* SQLファイルを好きに使う

???

これらを1つずつ見ていこう。

---

### 1件検索

```java
@Select
Account selectById(Long id);
```

```sql
SELECT /*%expand*/*
  FROM account
 WHERE id = /* id */0
```

???

結果が1件以上ならインスタンスが、0件ならnullが返る。

2件以上ならNonUniqueResultExceptionが投げられる。

また、@SelectのensureResultをtrueにすると結果が0件の場合にNoResultExceptionが投げられる。

---

### `Optional`で1件検索

```java
@Select
Optional<Account> selectById(Long id);
```

```sql
SELECT /*%expand*/*
  FROM account
 WHERE id = /* id */0
```

???

**目安：12分**

結果が0件の場合はemptyになる。

nullを排除。

---

### 複数件検索

```java
@Select
List<Account> selectAll();
```

```sql
SELECT /*%expand*/*
  FROM account
```

???

---

### `Stream`で複数件検索

```java
@Select(strategy = SelectType.STREAM)
<R> R selectAll(Function<Stream<Account>, R> f);
```

使い方

```java
Optional<Account> any = dao.selectAll(s -> s.findAny());
```

`s`は`Stream<Account>`

ラムダ式では`findAny`で`Optional<Account>`を返しており、それがそのまま`selectAll`の戻り値になっている

???

Streamを受け取って値を返す関数を受け取る。

ちょっとシグネチャが見た目にややこしい。

---

### `Stream`で複数件検索

```java
@Select
Stream<Account> selectAll();
```

使い方

```java
Optional<Account> any = dao.selectAll().findAny();
```

ただし警告（`DOMA4274`）が出る

???

Streamを返す形式も使えるけれど、警告が出る

なぜか？

---

### `Stream`で複数件検索

`Function<Stream<Account>, R>`の場合

```java
dao.selectAll(s -> {
    return s.collect(toList());
    //ここを抜けるとResultSetはclose
});
```

`Stream<Account>`を返す場合

```java
Stream<Account> stream = dao.selectAll();
//まだResultSetはcloseしていない
stream.collect(toList());
```

???

ResultSetのオープン、フェッチ、クローズが前者はDaoメソッドに閉じている。

後者はフェッチ、クローズはDaoメソッドに閉じていないのでコードの書き方によってはリークする可能性がある。

基本的には前者の形式を利用しよう。

---

### `Collector`で複数件検索

```java
@Select(strategy = SelectType.COLLECT)
<R> R selectAll(Collector<Account, ?, R> collector);
```

使い方

```java
List<Account> accounts = dao.selectAll(toList());
```

???

Stream検索のショートカットっぽい感じ。

ちなみにCollectorは内部で利用する中間状態が2つめの型変数として表面に出ている。
ぶっちゃけ利用者としては中間状態なんてどうでも良いので2番目の型変数はワイルドカードをバインドしておく。

---

### `offset`と`limit`を指定して検索

```java
@Select
List<Account> selectAll(SelectOptions selectOptions);
```

使い方

```java
SelectOptions selectOptions =
    SelectOptions.get().offset(5).limit(10);
List<Account> accounts = dao.selectAll(selectOptions);
```

???
Daoメソッドの引数にSelectOptionsを定義すると範囲指定などの検索処理をSQLファイルを変更せずに行える。

offsetとlimitはDomaがクエリを範囲指定したものに自動で変換してくれる。

PostgreSQLならoffsetとlimitを指定クエリに、Oracleならrownumを利用したクエリに変換される。
RDBMSの差異をDialectで吸収するのはこういう時。

---

### 悲観排他検索

```java
@Select
Optional<Account> selectById(Long id,
        SelectOptions selectOptions);
```

使い方

```java
SelectOptions selectOptions =
        SelectOptions.get().forUpdate();
a dao.selectById(id, selectOptions);
```

???

Domaがクエリを悲観排他制御用のものに自動で変換してくれる。

---

### カウント

```java
@Select
List<Account> selectAll(SelectOptions selectOptions);
```

使い方

```java
SelectOptions selectOptions =
        SelectOptions.get().count();
List<Account> accounts = dao.selectAll(selectOptions);
long count = selectOptions.getCount();
```

???

Domaが元のクエリを利用してカウントを取るクエリも自動で発行してくれる。

---

### 挿入/更新/削除クエリの自動構築

```java
@Insert
int insert(Account entity);

@Update
int update(Account entity);

@Delete
int delete(Account entity);
```

???

insert文はDomaが自動で構築してくれる。
update、deleteも同様。

---

### 挿入/更新/削除クエリの自動構築

```java
@Entity
public class Account {

    @Id
    public Long id;

    public String name;

    @Version
    public Long version;
}
```

???

**目安：18分**

update、deleteの場合は@Idが付いたフィールドに対応するカラムをprimary keyと見なしてwhere句を構築する。

また、@Versionで楽観排他制御も行える。

---

### 挿入/更新/削除でSQLファイル

```java
@Insert(sqlFile = true)
int insert(Long id, String name);
```

```sql
INSERT INTO account (id, name)
  VALUES (/* id */0, /* name */'dummy')
```

???

SQLファイルを利用して挿入/更新/削除を行うことも可能。

---

### 挿入/更新/削除のイベントハンドリング

```java
public class AccountListener
        implements EntityListener<Account> {

    @Override
    public void preInsert(Account entity,
            PreInsertContext<Account> context) {
        ...
    }
```

???

挿入/更新/削除のクエリが発行される前後で追加の処理を行えるエンティティリスナーという仕組み。

---

### 挿入/更新/削除のイベントハンドリング


```java
@Entity(listener = AccountListener.class)
public class Account {
```

* `preInsert`
* `preUpdate`
* `preDelete`
* `postInsert`
* `postUpdate`
* `postDelete`

???

@Entityのlistener要素にエンティティリスナー実装クラスを指定する。

ハンドリングができるタイミングは6つ。

全テーブル共通で持つタイムスタンプに値を設定する、など。

---

### 挿入/更新/削除のイベントハンドリング


```java
//リスナーが呼ばれる
@Insert
int insert(Account entity);

//リスナーは呼ばれない
@Insert(sqlFile = true)
int insert(Long id, String name);
```

???

注意点として、エンティティリスナーはクエリを自動構築する場合にのみ呼ばれる。

SQLファイルで挿入/更新/削除を行う場合は呼ばれない。

---

### バッチ挿入/バッチ更新/バッチ削除

```java
@BatchInsert
int[] insert(List<Account> entities);

@BatchUpdate
int[] update(List<Account> entities);

@BatchDelete
int[] delete(List<Account> entities);
```

???

複数エンティティをまとめて挿入/更新/削除する。

JDBCのexecuteBatchが使われる。

エンティティリスナーにも対応。

---

### ストアドファンクション/ストアドプロシージャ

```java
@Function
String getNameById(@In Long id);

@Procedure
void getNameById(@In Long id, @Out Reference<String> name);
```

???

ストアドファンクション名、ストアドプロシージャ名はデフォルトではDaoインターフェースのメソッド名になる。

@Function、@Procedureのname要素で指定もできる。

---

### SQLファイルを好きに使う

```java
@SqlProcessor
<R> R doSomething(Long id,
    BiFunction<Config, PreparedSql, R> handler);
```

```java
dao.doSomething(id, (config, sql) -> {
    DataSource ds = config.getDataSource();
    String preparedQuery = sql.getRawSql();
    ...
});
```

???

DomaがSQLファイルのパースまでやってくれて、そこから先を自由に処理できる。

JDBC接続はConfigからDataSourceを取得してそこから取得できる。

PreparedSqlからはプリペアードSQL、プレースホルダが?になったクエリや、Daoメソッドに渡したパラメータを取得できる。

---

class: center, middle

## ドメインクラス

---

### ドメインクラスとは

* Domaで扱える値オブジェクト
* `String`や`Integer`などの基本型だらけになりがちなカラムのマッピングに値オブジェクトが使える
* 「ドメイン」と冠しているけどDDDとは無関係

???

ここから少し値オブジェクトのメリットについて話をする。

---

### 引数の型に見る値オブジェクトのメリット

部門IDとロールでユーザーを検索する。

```java
List<Users> select(String deptId, String role);
```

```java
String deptId = ...
String role = ...
List<User> users = dao.select(deptId, role);
```

???

**目安：24分**

---

### 値オブジェクトを使わなかったら

引数の順番を間違えてもコンパイルエラーにならない。

```java
List<Users> select(String deptId, String role);
```

```java
String deptId = ...
String role = ...
List<User> users = dao.select(role, deptId);
```

実行時エラーにもならず気付きにくい😰

???

---

### 値オブジェクトを使ったら

部門IDとロールを異なるドメインクラスにすると……

```java
List<Users> select(DeptId deptId, Role role);
```

```java
DeptId deptId = ...
Role role = ...
List<User> users = dao.findBy(deptId, role);
```

---

### 値オブジェクトを使ったら

引数の順番を間違えたらコンパイルエラーになる。

```java
List<Users> select(DeptId deptId, Role role);
```

```java
DeptId deptId = ...
Role role = ...
List<User> users = dao.findBy(role, deptId);
```

実装中にすぐに気がつく😉

---

### 値の導出に見る値オブジェクトのメリット

住所から市区町村を取得する。

```java
String address = ...
String city = /* addressから導出したい */
```

---

### 値オブジェクトを使わなかったら

文字列をごにょごにょしたり😧

```java
String address = ...
String city = address.split(" ")[1];
```

???

せめてロジックをまとめようとして……（次ページへ）

---

### 値オブジェクトを使わなかったら

ユーティリティを作ったり😨

```java
String address = ...
String city = AddressUtil.extractCity(address);
```

???

でも……（次ページへ）

---

### 値オブジェクトを使わなかったら

でも全部`String`だから……😭

```java
String anotherCity = /* 住所じゃなくて市区町村を表す値 */
String city = AddressUtil.extractCity(anotherCity);
```

???

住所以外のものが渡されてもコンパイル時には気付かない。

実行してみて「何かおかしい」と思うやつ。

---

### 値オブジェクトを使ったら

住所そのものに導出メソッドを持たせられる😎

```java
Address address = ...
City city = address.getCity();
```

```java
public class Address {

    public City getCity() { ... }

    ...
}
```

???

「これをするにはどのユーティリティだったっけ？」と探す必要も無いし、市区町村から市区町村を導出しようとするような間違いもしなくなる。

---

### 値オブジェクトの他のメリット

* 引数や戻り値に使っていればシグネチャに現れる
* Javadocや変数名を見なくてもメソッドの把握がしやすい
* デバッグ情報無しにコンパイルされて`method(String arg0, String arg)`とかになってると悲惨
* IDEの補完
* 型で候補が絞られて素早く補完できる

---

### 値オブジェクトのデメリット

* クラス数が多くなる
* 作るのが面倒
* メンバーが値オブジェクトを多用するコーディングに慣れていない

???

クラス数は多少増えたところでコンパイルがめちゃくちゃ遅くなるわけでは無いし、クラス数という意味では実行時に何万とロードされるクラス群のほんの一部なので気にしないで良いと思う。

作るのが面倒なのはその通りだけど、まずはExcelから自動生成とかでも良いから値の種類を型で分けることから始めてみて欲しい。

メンバーに対しては「慣れろ」の一言。それでいつまで経ってもメリットを感じてもらえなかったら価値観が異なるということなので違うチームへ行きましょう。

???

**目安：30分**

---

### ドメインクラスの作り方

```java
@Domain(valueType = String.class)
public final class ValueObject {

    private final String value;

    public ValueObject(final String value) {
        this.value = Objects.requireNonNull(value);
    }

    public String getValue() { return value; }
}
```

???

クラスに@Domainを付けてvalueType要素に基本型を指定する。

基本型を受け取るコンストラクタと、基本型を返すgetValueという名前のアクセサメソッドが必要。

---

### ドメインクラスの作り方

```java
@Domain(valueType = String.class,
        factoryMethod = "of",
        accessorMethod = "value")
public final class ValueObject {

    public static ValueObject of(final String value) {
        ...
    }

    public String value() { ... }
}
```

???

@DomainのfactoryMethod要素にメソッド名を指定すれば、ドメインクラスの生成方法をstaticメソッドにも変更できる。

また、accessorMethod要素にメソッド名を指定すれば、アクセサメソッドもgetValue以外のものに変更できる。

---

### ドメインクラスを使える場所

エンティティのフィールド

```java
@Entity
public class Hoge {

    public ValueObject vo;

    ...
}
```

---

### ドメインクラスを使える場所

Daoメソッドの引数

```java
@Select
int selectCount(ValueObject vo);
```

---

### ドメインクラスを使える場所

Daoメソッドの戻り値

```java
@Select
List<ValueObject> select();
```

---

### ドメインクラスの発展的な使い方

ジェネリックなドメインクラス

```java
@Domain(valueType = Long.class)
public class Weight<UNIT> { ... }
```

```java
Weight<kg> x = ...
Weight<g> y = ...
x = y; //コンパイルエラー
```

???

ドメインクラスに型変数をバインドすることで表現力が増す。

---

### ドメインクラスの発展的な使い方

インターフェースなドメインクラス 

```java
@Domain(valueType = String.class,
        factoryMethod = "of")
public interface Color {

    static Color of(String value) { ... }

    ...
}
```

???

Java 8からインターフェースにstaticメソッドを持たせられるようになったのでインターフェースをドメインクラスにできる。

より詳しく知りたい場合は、後ほど資料を紹介するのでそちらを参照。

---

class: center, middle

## コンパイル時検査

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

???

**目安：36分**

---

### Daoメソッドの戻り値が扱えない型だとコンパイルエラー

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

### Daoメソッドの戻り値が扱えない型だとコンパイルエラー

> [DOMA4007] 戻り値のjava.util.Listに対する実型引数の型[InvalidClass]はサポートされていません。

---

### Daoメソッドに対応したSQLファイルがクラスパス上に無いとコンパイルエラー

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

### Daoメソッドに対応したSQLファイルがクラスパス上に無いとコンパイルエラー

> [DOMA4019] ファイル[META-INF/foo/bar/HogeDao/selectAll.sql]がクラスパスから見つかりませんでした。ファイルの絶対パスは"/path/to/META-INF/foo/bar/HogeDao/selectAll.sql"です。	

---

### Daoメソッドの引数をクエリで使用していないとコンパイルエラー

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

### Daoメソッドの引数をクエリで使用していないとコンパイルエラー

> [DOMA4122] SQLファイル[META-INF/foo/bar/HogeDao/selectById.sql]の妥当検査に失敗しました。メソッドのパラメータ[id]がSQLファイルで参照されていません。

---

### クエリで使用しているバインド変数がDaoメソッドで宣言されていないとコンパイルエラー

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

### クエリで使用しているバインド変数がDaoメソッドで宣言されていないとコンパイルエラー

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

???

**目安：42分**

---

### ドメインクラスに必要なファクトリーメソッドや、アクセサが無いとコンパイルエラー

> [DOMA4104] アクセッサメソッド[getValue]が見つかりません。アクセッサメソッドは、型[java.lang.String]を戻り値とする非privateで引数なしのインスタンスメソッドでなければいけません。 at Hoge


---

class: center, middle

## インテグレーション

???

Domaは他のライブラリ・フレームワークへの依存が一切無いのでどのフレームワークとも組み合わせられるけれど、組み合わせるための仕組みが用意されていて今すぐDomaが使えるフレームワークを紹介。

---

### Spring Boot

doma-spring-boot

https://github.com/domaframework/doma-spring-boot

???

作者は@making。

今は私もコミッター。

---

## doma-spring-bootが提供するもの

* `Config`のauto configuration
* Daoをコンテナ管理するためのアノテーション
* Domaの例外👉Springの例外 読み替える仕組み
* `EntityListener`をコンテナからlookupする仕組み

???

Dialectの設定などをSpring Bootの設定ファイルで行える。

DomaPersistenceExceptionTranslator

EntityListenerは通常はDomaにnewされるけれど、Springコンテナからインスタンスを取得できる。
Springコンテナ管理なのでEntityListenerにコンポーネントをインジェクションできるということ。便利。

doma-spring-boot-starterをpom.xmlやbuild.gradleのdependencyに入れるだけで使える。
Spring Bootユーザーは是非とも試して欲しい。

---

### Enkan

https://enkan.github.io/reference/components.html#doma2

???

TIS川島さんが作っているミドルウェアパターンを使ったマイクロウェブフレームワーク。

---

## その他の話題

* 外部ドメイン
* エンベッダブルクラス
* ローカルトランザクション
* プリミティブな調整（`JdbcMappingVisitor`）
* 拡張ポイント（`QueryImplementors`、`CommandImplementors`）
* 実験的なKotlinサポート
* IDEプラグイン
  * [Doma Tools(Eclipse)](http://doma.readthedocs.io/ja/stable/getting-started/#eclipse-doma-tools)
  * [DomaSupport(IntelliJ IDEA)](https://github.com/siosio/DomaSupport)

???

外部ドメインは@Domainを付けられないクラス、例えばjava.nio.file.Pathとかをドメインクラスとして扱うための仕組み。

エンベッダブルクラスは複数カラムをまとめて1つのクラスとして扱うための仕組み。

ローカルトランザクションはスレッドに紐付けたトランザクション管理の仕組み。
Java SEで使う場合に有用。

JdbcMappingVisitorはResultSet#getStringやPreparedStatement#setIntなどを行うインターフェース。
VARCHARでフラグを表現する場合などにカスタマイズする。

QueryImplementorsはDomaのSQLパーサーが組み立てたクエリのASTを変換できるポイント。

CommandImplementersはクエリの発行に処理を差し込めるポイント。

---

## その他の話題

Doma 3 (WIP)

https://github.com/domaframework/doma/pull/198

* Java 9
* `@Domain`👉`@Holder`
* 内部的なコード改善
* ドキュメント英語化の話も……？

※これらすべて確定ではありません

???

Java 9対応。例えばinterfaceのprivateメソッドを許可する。

日本語ドキュメントがあるのはDomaのメリットだが、日本語ドキュメントしか無いのはDomaのデメリット。

---

## 参考リンク

* http://doma.readthedocs.io/
* https://gitter.im/domaframework/doma
* http://backpaper0.github.io/ghosts/doma-domainclass.html
* http://gakuzzzz.github.io/slides/doma_practice/
* http://qiita.com/nakamura-to/items/099cf72f5465d0323521

---

class: center, middle

## 質問など

---

## この資料について

* Author: [@backpaper0](https://github.com/backpaper0)
* License:  [The MIT License](https://opensource.org/licenses/MIT)


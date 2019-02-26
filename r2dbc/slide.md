class: center, middle

# R2DBCの話

---

## 自己紹介

* うらがみ⛄️
* Javaプログラマー
* リアクティブシステムまじわからん

---

class: center, middle

# 前回のあらすじ

---

## 前回のあらすじ

- バックプレッシャーを持つノンブロッキングな非同期ストリーム
- Reactor（`Flux`、`Mono`）
- Spring WebFlux
- JDBCはブロッキング……
- 悲しい

---

class: center, middle

# そこでR2DBC

---

## R2DBCとは

- "Reactive Relational Database Connectivity"
- powered by Pivotal
- リアクティブなJDBCをReactorで、っていう感じ

---

## 以降の例で使用するテーブル定義

```sql
CREATE TABLE msg (
    id SERIAL PRIMARY KEY,
    txt VARCHAR(100)
);
```

---

## コード例：pom.xml

```xml
<repositories>
  <repository>
    <id>spring-snapshots</id>
    <name>Spring Snapshots</name>
    <url>https://repo.spring.io/snapshot</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
  <repository>
    <id>spring-milestones</id>
    <name>Spring Milestones</name>
    <url>https://repo.spring.io/milestone</url>
  </repository>
</repositories>
```

---

## コード例：pom.xml

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.r2dbc</groupId>
      <artifactId>r2dbc-bom</artifactId>
      <version>1.0.0.BUILD-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## コード例：pom.xml

```xml
<dependencies>
  <dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
  </dependency>
</dependencies>
```

---

## 処理の流れ

- `ConnectionFactory`を準備する
- `create`メソッドで`Publisher`を作る
- `Mono`と`Flux`で良い感じに繋げる

---

## コード例：ConnectionFactory

```java
ConnectionFactory factory = new PostgresqlConnectionFactory(
    PostgresqlConnectionConfiguration.builder()
            .host("localhost")
            .database("demo")
            .username("demo")
            .password("demo")
            .build());
```
---

## コード例：SELECT

```java
Flux<Map<String, Object>> records =
    Mono.from(factory.create())
        .flatMapMany(con -> con
            .createStatement("SELECT id, txt FROM msg").execute())
        .flatMap(result ->
            result.map((row, meta) -> Map.of(
                "id", row.get("id", Integer.class),
                "txt", row.get("txt", String.class))));
```

---

## コード例：INSERT

```java
Mono<Integer> rowsUpdated =
    Mono.from(factory.create())
        .flatMap(con -> Mono.from(
            con.createStatement(
                   "INSERT INTO msg (txt) VALUES ($1)")
               .bind("$1", txt)
               .execute()))
        .flatMap(result -> Mono.from(
            result.getRowsUpdated()));
```

戻り値は挿入件数

---

## コード例：INSERT

```java
Mono<Integer> generatedId =
    Mono.from(factory.create())
        .flatMap(con -> Mono.from(
            con.createStatement(
                   "INSERT INTO msg (txt) VALUES ($1)")
               .bind("$1", txt)
               .returnGeneratedValues().execute()))
        .flatMap(result -> Mono.from(
            result.map((row, meta) -> row.get("id", Integer.class))));
```

戻り値は生成されたID

---

## コード例：INSERT

```java
Flux<Integer> generatedId =
    Mono.from(factory.create())
        .flatMapMany(con ->
            con.createStatement(
                   "INSERT INTO msg (txt) VALUES ($1)")
               .bind("$1", txt)
               .returnGeneratedValues().execute())
        .flatMap(result ->
            result.map((row, meta) -> row.get("id", Integer.class)));
```

戻り値は`Flux`にもできる

---

## コード例：トランザクション

```java
Flux<Integer> generatedId =
    Mono.from(factory.create())
        .flatMapMany(con -> Flux.from(con.beginTransaction())
            .thenMany(con.createStatement(
                    "INSERT INTO msg (txt) VALUES ($1)")
                .bind("$1", txt).returnGeneratedValues().execute())
            .flatMap(result ->
                result.map((row, meta) -> row.get("id", Integer.class)))
            .delayUntil(id -> con.commitTransaction()));
```

---

class: center, middle

# やってられない

---

## そこで高レベルAPI

- 素のR2DBCは低レベルAPIすぎる
- 高レベルAPIを使うのが良い
- `R2dbc` ([r2dbc-client](https://github.com/r2dbc/r2dbc-client))
- `DatabaseClient` ([Spring Data R2DBC](https://github.com/spring-projects/spring-data-r2dbc))
- `R2dbcRepository` ([Spring Data R2DBC](https://github.com/spring-projects/spring-data-r2dbc))

---

class: center, middle

# r2dbc-client

---

## r2dbc-client

- R2DBCのOrganizationに置かれている高レベルAPI
- そこまで高レベルじゃないけど素の`ConnectionFactory`よりはまし

---

## コード例：pom.xml

```xml
<dependencies>
  <dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-client</artifactId>
  </dependency>
</dependencies>
```

---

## コード例：R2dbc

```java
ConnectionFactory factory = ...
R2dbc client = new R2dbc(factory);
```

---

## コード例：SELECT

```java
Flux<Map<String, Object>> records = client
    .withHandle(handle ->
        handle.createQuery("SELECT id, txt FROM msg")
              .mapRow(row -> Map.of(
                  "id", row.get("id", Integer.class),
                  "txt", row.get("txt", String.class))));
```

---

## コード例：INSERT

```java
Flux<Integer> rowsUpdated = client
    .withHandle(handle ->
        handle.execute(
            "INSERT INTO msg (txt) VALUES ($1)", txt));
```

---

## コード例：トランザクション

```java
Flux<Integer> rowsUpdated = client
    .inTransaction(handle ->
        handle.execute(
            "INSERT INTO msg (txt) VALUES ($1)", txt));
```

---

class: center, middle

# DatabaseClient

---

## ここからSpring Data R2DBC

- Spring Data R2DBCに含まれるR2DBCラッパー
- 後述する`R2dbcRepository`も内部で`DatabaseClient`が使われている

---

## コード例：pom.xml

```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-r2dbc</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
</dependency>
```

---

## コード例：DatabaseClient

```java
ConnectionFactory factory = ...
DatabaseClient client = DatabaseClient.create(factory);
```

---

## コード例：SELECT

```java
Flux<Map<String, Object>> records =
    client.execute().sql(
        "SELECT id, txt FROM msg").fetch().all();
```

```java
Flux<Map<String, Object>> records =
    client.select().from("msg").fetch().all();
```

---

## コード例：SELECT

```java
public final class Msg {

    private final Integer id;
    private final String txt;

    public Msg(Integer id, String txt) {
        this.id = id;
        this.txt = txt;
    }

    //他のメソッドは省略
}
```

このようなクラスを作って……

---

## コード例：SELECT

```java
Flux<Msg> records =
    client.execute().sql(
        "SELECT id, txt FROM msg").as(Msg.class).fetch().all();
```

```java
Flux<Msg> records =
    client.select().from(Msg.class).fetch().all();
```

こういう風にも書ける

---

## コード例：INSERT

```java
client.execute()
    .sql("INSERT INTO msg (txt) VALUES ($1)")
    .bind("$1", txt).then();
```

```java
client.insert().into("msg")
    .value("txt", txt).then();
```

---

## コード例：INSERT

```java
Msg msg = ...
client.insert().into(Msg.class).using(msg).then();
```

---

## コード例：トランザクション

```java
ConnectionFactory factory = ...
TransactionalDatabaseClient tx =
    TransactionalDatabaseClient.create(factory);
tx.inTransaction(client ->
    client.insert().into(Msg.class).using(msg).then());
```

`DatabaseClient`ではなくて`TransactionalDatabaseClient`を使う

---

class: center, middle

# R2dbcRepository

---

## R2dbcRepository

- `JpaRepository`みたいなやつ
- interfaceを定義しておけばバイトコードエンハンスでいい感じにしてくれる

---

## コード例：準備

```java
@EnableR2dbcRepositories
@SpringBootApplication
public class App {

    public static void main(final String[] args) {
        SpringApplication.run(App.class, args);
    }
}
```


---

## コード例：ドメイン

```java
public final class Msg {
    @Id
    private final Integer id;
    private final String txt;

    public Msg(Integer id, String txt) {
        this.id = id;
        this.txt = txt;
    }

    public Msg withId(final Integer id) {
        return new Msg(id, txt);
    }
}
```

---

## コード例：リポジトリ

```java
public interface MsgRepository
    extends R2dbcRepository<Msg, Integer> {
}
```

---

## コード例：SELECT

```java
Flux<Msg> records = repo.findAll();
```

---

## コード例：SELECT

```java
@Query("SELECT id, txt FROM msg WHERE id > $1")
Flux<Msg> findCustom(Integer id);
```

```java
Integer id = ...
Flux<Msg> records = repo.findCustom(id);
```

---

## コード例：INSERT

```java
Msg entity = ...
Mono<Msg> saved = repository.save(entity);
 ```

---

## コード例：トランザクション

やり方が分からん

---

class: center, middle

# ここまでのまとめ

---

## ここまでのまとめ

- Spring Data R2DBCを使おう
- `DatabaseClient` と `R2dbcRepository` があれば大体OKな気がする

---

class: center, middle

# RDBもリアクティブ<br>できることが分かった

---

class: center, middle

# これでオレも<br>リアクティブできる！

---

class: center, middle

# ただしFluxとMonoを<br>使いこなせれば

---

## FluxとMono

- 元も子もないけどRDBがどうとか言う前にFluxとMonoを使ったプログラミングスタイルに慣れないといけない
- 例えばinterfaceでDBアクセスを隠蔽してPOJOでロジックを構築することを考える

---

## 例：こんなinterfaceがあって

```java
interface TaxRepository {

    BigDecimal findTaxRate(LocalDate date);
}
```

---

## 例：こんな感じで使えば


```java
class TaxService {

    final TaxRepository repo;

    //コンストラクタ略

    BigDecimal calcTax(BigDecimal price, LocalDate date) {
        BigDecimal taxRate = repo.findTaxRate(date);
        return price.multiply(taxRate);
    }
}
```

DBアクセスを隠蔽してPOJOでロジックが書ける

---

## 例：リアクティブで同じことを考える

```java
interface TaxRepository {

    Mono<BigDecimal> findTaxRate(LocalDate date);
}
```

```
＿人人人人人人人＿
＞　突然のMono　＜
￣Y^Y^Y^Y^Y^Y^Y^￣
```

---

## 例：こんな感じで使う

```java
class TaxService {

    final TaxRepository repo;

    //コンストラクター略

    Mono<BigDecimal> calcTax(BigDecimal price, LocalDate date) {
        Mono<BigDecimal> taxRate = repo.findTaxRate(date);
        return taxRate.map(price::multiply);
    }
}
```

POJOでロジックを書く層にも`Flux`と`Mono`が出てこざるを得ない

---

## どうしたもんか

- POJOでロジックを書く層
- ファーストクラスコレクション

なんか良いアイデアください！

---

class: center, middle

# その他の話題

---

## Spring Data R2DBCの中身を見よう

Repository実装クラスが生成されるまでに辿るクラス

- `@EnableR2dbcRepositories`
- `R2dbcRepositoriesRegistrar`
- `R2dbcRepositoryConfigurationExtension`
- `R2dbcRepositoryFactoryBean`
- `R2dbcRepositoryFactory`

Repository実装クラスのベース

- `SimpleR2dbcRepository`

この辺を読んでいじればカスタマイズできる

---

## 例：SQLをファイルから読むカスタマイズ

```java
public interface MsgRepository
        extends R2dbcRepository<Msg, Long> {

    @SqlFile
    Flux<Msg> findAllMsgs();
}
```

こんな感じのメソッドを書くと……

---

## 例：SQLをファイルから読むカスタマイズ

```sql
SELECT
    id,
    txt
FROM
    msg
```

こんな感じのSQLファイルを読んでクエリ発行してくれるやつを作り込む

---

## 例：SQLをファイルから読むカスタマイズ

※コードをスライドに良い感じに収める自信が無いので各自でご覧ください

- https://github.com/backpaper0/spring-data-r2dbc-example/tree/master/spring-data-r2dbc-sqlfile-example

---

class: center, middle

# まとめ

---

## まとめ

- R2DBCでRDBアクセスもリアクティブ
- だがしかしロジックに`Flux`と`Mono`が露出
- 生R2DBCじゃなくてSpring Data R2DBCを使おう
- 2 way SQLをファイルから読み込むような拡張も夢ではない
- 新しいオモチャ楽しい

---

## おわり

コード例

- https://github.com/backpaper0/spring-data-r2dbc-example

参考情報

- https://github.com/r2dbc
- https://github.com/spring-projects/spring-data-r2dbc


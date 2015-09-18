# KotlinでDoma

[@backpaper0](https://twitter.com/backpaper0)

2015-09-19 [関西Kotlin勉強会](http://connpass.com/event/18102/)



### 趣旨

KotlinでDomaを使ってみよう！



### [Doma](http://doma.readthedocs.org/)とは

* Java 8で動作するDBアクセスフレームワーク
* [Pluggable Annotation Processing API](https://jcp.org/en/jsr/detail?id=269)を利用してコンパイル時にコードの生成や検証を行う
* Pluggable Annotation Processing APIは他には[AndroidAnnotations](http://androidannotations.org/)や[Lombok](https://projectlombok.org/)、[Dagger](http://square.github.io/dagger/)などで使用されている
* カラムを好きなクラスにマッピングできる
* 2-way SQL
* 依存ライブラリが無い



### Domaを簡単にご紹介



### [エンティティクラス](http://doma.readthedocs.org/ja/stable/entity/)

テーブルや検索結果をマッピングするやつ。

```
@Entity
public class Book {

    @Id
    public String isbn;

    public String title;

    public String author;

}
```



### [DAOインターフェース](http://doma.readthedocs.org/ja/stable/dao/)

SQL投げて結果を返すやつ。

```
@Dao
public interface BookDao {

    @Select
    List<Book> select(String title, String author);

    @Insert
    int insert(Book book);
}
```



### [SQLファイル(2-way SQL)](http://doma.readthedocs.org/ja/stable/sql/)

主にSELECT文で使う。
DAOのメソッドに対応したパスに置く。
例えば `META-INF/app/dao/BookDao/select.sql`

```
SELECT /*%expand*/*
  FROM book
 WHERE title = /* title */'x'
   /*%if author != null */
   AND author = /* author */'y'
   /*%end*/
```



### コンパイル時に色々検出

* `@Select` を付けたメソッドに対応するSQLファイルがないとコンパイルエラー
* DAOクラスのメソッドに `@Select` や `@Insert` などのアノテーションが付いていないとコンパイルエラー
* SQLファイルの中身が空っぽだとコンパイルエラー
* メソッドの引数がSQLファイル内で使用されていないとコンパイルエラー
* SQLファイル内の `/*%if ...*/` や `/*%end*/` が変な位置にあるとコンパイルエラー



### [ドメインクラス](http://doma.readthedocs.org/ja/stable/domain/#id3)

エンティティのフィールドやDAOのメソッドの引数、戻り値にStringなどの基本型ではなくてユーザー定義のクラスを利用できる仕組み。

```
@Domain(valueType = String.class)
public class Isbn {

    private final String value;

    public Isbn(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```



### エンティティでドメインクラスを使う

フィールドに使用できる。

```
@Entity
public class Book {

    @Id
    public Isbn isbn;

    public Title title;

    public Author author;
}
```



### DAOでドメインクラスを使う

メソッドの引数や戻り値に使える。
(この例は引数だけ)

```
@Dao
public interface BookDao {

    @Select
    List<Book> select(Title title, Author author);

}
```



### SQLファイルでドメインクラスを使う

SQLファイルはドメインクラスを使用しない場合と何も変わらない。

```
SELECT /*%expand*/*
  FROM book
 WHERE title = /* title */'x'
   /*%if author != null */
   AND author = /* author */'y'
   /*%end*/
```



### ドメインクラスの有無を比較

```
//ドメインクラスがある
@Select
List<Book> select(Title title, Author author);

//ドメインクラスがない
@Select
List<Book> select(String title, String author);
```

ドメインクラスを使用していると

```
dao.select(author, title); //引数が逆
```

はコンパイルエラーになる。

(どちらも`String`ならコンパイルエラーにならない)



### [StreamやOptionalへの対応](http://doma.readthedocs.org/ja/stable/query/select/#id8)

```
@Select(strategy = SelectType.STREAM)
<R> R select(Function<Stream<Book>, R> f);

@Select(strategy = SelectType.COLLECT)
<R> R select(Collector<Book, ?, R> collector);

@Select
Optional<Book> select(Isbn isbn);
``` 

Stream検索は次のように使う。

```
Optional<Book> book = dao.select(stream -> stream.findFirst());

List<Book> books = dao.select(Collectors.toList());
```



### その他の機能

* [ローカルトランザクション](http://doma.readthedocs.org/ja/stable/transaction/)
  ```
  transactionManager.required(() -> {
      //トランザクション内で実行される
      //例外が投げられたらロールバック
  });
  ```
* java.awt.Colorなど自由に変更できないクラスをドメインとして扱える([外部ドメイン](http://doma.readthedocs.org/ja/stable/domain/#id7))
* [LocalDateTimeなど](https://jcp.org/en/jsr/detail?id=310)を扱える
* エンティティをイミュータブルにできる
  ```
  @Entity(immutable = true)
  public class Book {
      public final Isbn isbn;
      public final Title title;
      public Book(Isbn isbn, Title title) {
          this.isbn = isbn;
          this.title = title;
      }
  }
  ```



### ここまでほとんどDomaの説明



## 本題



### Pluggable Annotation Processing APIの欠点



### Java言語でしか使えない



### Kotlinerはどうすれば……



## そこでkaptですよ！



## kaptとは

* [kapt: Annotation Processing for Kotlin](http://blog.jetbrains.com/kotlin/2015/05/kapt-annotation-processing-for-kotlin/)
* KotlinでPluggable Annotation Processing APIが使える機能



### kaptの仕組み

TODO コード読んで概ねどんな感じで動作するのか説明したい



### Gradleでkaptを使う設定

```
buildscript {
    repositories.jcenter()
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:0.13.1513'
    }
}

apply plugin: 'kotlin'

repositories.jcenter()

dependencies {
    compile 'org.jetbrains.kotlin:kotlin-stdlib:0.13.1513'
    compile 'org.seasar.doma:doma:2.4.1'
    kapt 'org.seasar.doma:doma:2.4.1'
}
```



### KotlinでDomaを使うとどういう感じになるのか見てみる



### Kotlinでエンティティ

```
Entity
public class Book {

    Id
    val isbn: String? = null

    val title: String? = null

    val author: String? = null
}
```

(´-`).oO(あんまり変わらん)



### Kotlinでドメインクラス 

```
Domain(valueType = String::class)
public class Isbn(val value: String)
```

短くなった！



### KotlinでDAOクラス

```
Dao
public interface BookDao {

    @Select
    fun select(isbn: Isbn): Book

    @Insert
    fun insert(user: User): Int
}
```

(´-`).oO(あんまり変わらん)



### 困ったこと

* 少なくとも1つ注釈処理が動くJavaコード(エンティティクラスとかドメインクラス)が無いとkapt動いてくれない
* 注釈処理で取得できる引数名が `arg0`, `arg1` ... になるのでSQLファイルで参照するとき死ぬ。
  ```
  /** 理想 */
  SELECT * FROM book WHERE isbn = /* isbn */'dummy'
  ```

  ```
  /** 現実 */
  SELECT * FROM book WHERE isbn = /* arg0 */'dummy'
  ```
  あとイミュータブルなエンティティが使い物にならない。
  ```
  //理想
  Entity(immutable = true)
  public class Book(Id val isbn: Isbn, val title: Title)
  ```

  ```
  //現実
  Entity(immutable = true)
  public class Book(Id val arg0: Isbn, val arg1: Title)
  ```



## まとめ

* KotlinでPluggable Annotation Processing APIが使えるkaptすごい
* Domaで使うにはあと一歩(注釈処理中に引数名がちゃんと取れたら完璧。なんか設定とかで出来るのであれば調査足らずですごめんちゃい)
* Kotlinかわいい
* Doma大好き
* Gradleぺろぺろ



### サンプルコード

https://github.com/backpaper0/sandbox/tree/master/kotlin-doma-sample


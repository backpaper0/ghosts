# KotlinでDoma

[@backpaper0](https://twitter.com/backpaper0)

2015-09-19 [関西Kotlin勉強会](http://connpass.com/event/18102/)



### 趣旨

KotlinでDomaを使ってみよう！



### [Doma](http://doma.readthedocs.org/)とは

* Java 8で動作するDBアクセスフレームワーク
* 依存ライブラリが無い
* [Pluggable Annotation Processing API](https://jcp.org/en/jsr/detail?id=269)を利用してコンパイル時にコードの生成や検証を行う
* Pluggable Annotation Processing APIは他には[Lombok](https://projectlombok.org/)や[Dagger](http://square.github.io/dagger/)などで使用されている



### Domaを簡単にご紹介



### エンティティ

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



### Daoインターフェース

SQL投げて結果を返すやつ。

```
@Dao
public interface BookDao {

    @Select
    List<Book> select(String title, String author);

}
```



### SQLファイル

主にSELECT文で使う。
Daoのメソッドに対応したパスに置く。
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
* Daoクラスのメソッドに `@Select` や `@Insert` などのアノテーションが付いていないとコンパイルエラー
* SQLファイルの中身が空っぽだとコンパイルエラー
* メソッドの引数がSQLファイル内で使用されていないとコンパイルエラー
* SQLファイル内の `/*%if ...*/` や `/*%end*/` が変な位置にあるとコンパイルエラー



### ドメインクラス

エンティティのフィールドやDaoのメソッドの引数、戻り値にStringなどの基本型ではなくてユーザー定義のクラスを利用できる仕組み。

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



### Daoでドメインクラスを使う

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



### StreamやOptionalへの対応

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

* ローカルトランザクション
  ```
  transactionManager.required(() -> {
      //トランザクション内で実行される
      //例外が投げられたらロールバック
  });
  ```
* `java.awt.Color`など自由に変更できないクラスをドメインとして扱える
* [LocalDateTimeなど](https://jcp.org/en/jsr/detail?id=310)を扱える
* エンティティをイミュータブルにできる
  ```
  @Entity(immutable = true)
  ```
* エンティティ作成の支援ツールdoma-genを使ってテーブル定義からエンティティを生成できる



### ここまでほとんどDomaの説明



## 本題



### Pluggable Annotation Processing APIの欠点



### Java言語でしか使えない



### Kotlinerはどうすれば……



## そこでkaptですよ！



## kaptとは

* [kapt: Annotation Processing for Kotlin](http://blog.jetbrains.com/kotlin/2015/05/kapt-annotation-processing-for-kotlin/)
* KotlinでPluggable Annotation Processing APIが使える機能



### Gradleでkaptを使う設定

```
buildscript {
    repositories.jcenter()
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:0.12.1230'
    }
}

apply plugin: 'kotlin'

repositories.jcenter()

dependencies {
    compile 'org.jetbrains.kotlin:kotlin-stdlib:0.12.1230'
    compile 'org.seasar.doma:doma:2.4.1'
    kapt 'org.seasar.doma:doma:2.4.1'
}
```



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



### KotlinでDaoクラス

```
Dao
public interface BookDao {

    @Select
    fun select(isbn: Isbn): Book

}
```



### 困ったこと

* 少なくとも1つ注釈処理が動くJavaコード(エンティティクラスとかドメインクラス)が無いとkapt動いてくれない
* 注釈処理で取得できる引数名が `arg0`, `arg1` ... になる(SQLファイルで参照するとき死ぬ。
  ```
  SELECT * FROM book WHERE isbn = /* arg0 */'dummy'
  ```
  あとイミュータブルなエンティティが実質的に作れない)
  ```
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


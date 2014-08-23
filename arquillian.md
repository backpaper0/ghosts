## Arquillianではじめるコンテナを使ったテスト

[@backpaper0](https://twitter.com/backpaper0)

2014-08-23 [関西WildFly勉強会](http://connpass.com/event/7529/)



## Arquillian

* http://arquillian.org/

コンテナの起動、アーカイブの作成・デプロイ、テスト実行を行ってくれるすごいやつ。



### 使い方

Mavenを使用するのが楽。



### pom.xmlにdependencyManagement要素にbom突っ込む

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.jboss.arquillian</groupId>
      <artifactId>arquillian-bom</artifactId>
      <version>1.1.5.Final</version>
      <scope>import</scope>
      <type>pom</type>
    </dependency>
  </dependencies>
</dependencyManagement>
```



### pom.xmlにarquillian-junit-containerと使用するコンテナアダプタ突っ込む

```xml
<dependencies>
  <dependency>
    <groupId>org.jboss.arquillian.junit</groupId>
    <artifactId>arquillian-junit-container</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.wildfly</groupId>
    <artifactId>wildfly-arquillian-container-managed</artifactId>
    <version>8.1.0.Final</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

* ArquillianはJUnitに依存しているわけではない
* [TestNGを使う事もできる](http://repo1.maven.org/maven2/org/jboss/arquillian/testng/)
* [Spockも？](http://repo1.maven.org/maven2/org/jboss/arquillian/spock/)



### arquillian.xmlを書く

```xml
<?xml version="1.0" encoding="UTF-8"?>
<arquillian xmlns="http://jboss.org/schema/arquillian"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd">

  <container qualifier="wildfly-managed" default="true">
    <configuration>
      <property name="jbossHome">/Users/backpaper0/wildfly</property>
    </configuration>
  </container>

</arquillian>
```

* container要素は複数書ける
* テスト実行時にMavenのprofileで使うcontainerを指定する



### コードを書く

テスト対象のコードを書く。

今回はこんにちはするだけのJAX-RSを用意。



### App.java

```java
package app;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("rest")
public class App extends Application {
}
```



### Hello.java

```java
package app;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Path("hello")
@Produces(MediaType.TEXT_PLAIN)
public class Hello {

    @GET
    public String say(@QueryParam("name") String name) {
        return String.format("Hello, %s!", name);
    }
}
```



### テストコードを書く

```java
package app;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class HelloTest {

    @Inject
    private Hello hello;

    @Test
    public void testHello() throws Exception {
        assertThat(hello.say("backpaper0"), is("Hello, backpaper0!"));
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(App.class, Hello.class);
    }
}
```

* テストランナーにArquillianを指定する
* staticメソッドでデプロイするアーカイブを組み立てる



### 実際にやってみる



### コンテナでテストが実行される仕組み

* テストメソッドがHTTPリクエストを投げる
* [ServletTestRunner](http://docs.jboss.org/arquillian/aggregate/latest/org/jboss/arquillian/protocol/servlet/runner/ServletTestRunner.html)というサーブレットでリクエストを受けとる
* リクエストの内容に沿ってテストを実行する
* 結果をレスポンスに乗せて返す



### 依存してるJARもアーカイブに含めたい場合

ShrinkWrapのMavenリゾルバーを使うと良い。



### 依存JARもアーカイブに含めたい場合のpom.xml

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.jboss.shrinkwrap.resolver</groupId>
      <artifactId>shrinkwrap-resolver-bom</artifactId>
      <version>2.1.1</version>
      <scope>import</scope>
      <type>pom</type>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.jboss.shrinkwrap.resolver</groupId>
    <artifactId>shrinkwrap-resolver-impl-maven</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```



### 依存JARをアーカイブに含めるコード

```java
WebArchive war = ...

File[] jars = org.jboss.shrinkwrap.resolver.api.maven.Maven
                .resolver()
                .loadPomFromFile("pom.xml")  //pom.xmlを読む
                .importRuntimeDependencies() //runtimeスコープの依存JARを使う
                .resolve()
                .withTransitivity()          //依存を推移的に解決する
                .asFile();

war.addAsLibraries(jars);
``` 



### インジェクションをカスタマイズしたい

* [TestEnricher](http://docs.jboss.org/arquillian/aggregate/latest/org/jboss/arquillian/test/spi/TestEnricher.html)の実装クラスを作る
* [LoadableExtension](http://docs.jboss.org/arquillian/aggregate/latest/org/jboss/arquillian/core/spi/LoadableExtension.html)の実装クラスを作る
* LoadableExtension実装クラスの[registerメソッド](http://docs.jboss.org/arquillian/aggregate/latest/org/jboss/arquillian/core/spi/LoadableExtension.html#register(org.jboss.arquillian.core.spi.LoadableExtension.ExtensionBuilder))でTestEnricher実装クラスを登録する
* META-INF/services/org.jboss.arquillian.core.spi.LoadableExtensionに実装クラスのFQDNを書く
* http://repo1.maven.org/maven2/org/jboss/arquillian/testenricher/ らへんを参考にする



## ☃

おわり。

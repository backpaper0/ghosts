class: center, middle

# リフレクションで遊ぼう

---

## 自己紹介

* うらがみ⛄️
* Javaとゼルダの伝説 ブレス オブ ザ ワイルドを愛するプログラマー

---

## アジェンダ

- リフレクションとは？
- リフレクションのAPI
- 合成メソッド・ブリッジメソッド
- オーバーライドの判定
- その他便利なAPI
- まとめ

???

最近DIコンテナを書いたので、そこで扱った事柄を中心に喋ります。

---

class: center, middle

# リフレクションとは？

---

## リフレクションとは？

[java.lang.reflect (Java SE 11 & JDK 11 )](https://docs.oracle.com/javase/jp/11/docs/api/java.base/java/lang/reflect/package-summary.html)

> リフレクションでは、ロードされたクラスのフィールド、メソッドおよびコンストラクタに関する情報へのプログラムによるアクセス、およびカプセル化とセキュリティの制限内で、基になる対応オブジェクトで動作するフィールド、メソッド、およびコンストラクタの使用が可能です。

---

## リフレクションとは？

例）このコードを（敢えて）リフレクションを使って書くと……

```java
String name = ...
Hello hello = new Hello();
String message = hello.say(name);
```

---

## リフレクションとは？

こうなる。

```java
String name = ...
Constructor<?> constructor = Hello.class.getConstructor();
Object hello = constructor.newInstance();
Method method = Hello.class.getMethod("say", String.class);
Object message = method.invoke(hello, name);
```

---

class: center, middle

# リフレクションのAPI

---

## コンストラクターを取得するAPI

- `Class.getConstructor(Class<?>... parameterTypes)`
- `Class.getConstructors()`
- `Class.getDeclaredConstructor(Class<?>... parameterTypes)`
- `Class.getDeclaredConstructors()`

???

5分

declaredあり・なしの2バージョンある。

---

## メソッドを取得するAPI

- `Class.getMethod(String name, Class<?>... parameterTypes)`
- `Class.getMethods()`
- `Class.getDeclaredMethod(String name, Class<?>... parameterTypes)`
- `Class.getDeclaredMethods()`

---

## フィールドを取得するAPI

- `Class.getField(String name)`
- `Class.getFields()`
- `Class.getDeclaredField(String name)`
- `Class.getDeclaredFields()`

---

## Declaredあり・なしの違い

Declaredあり

- `public`以外も取得できる
- レシーバーとなる`Class`に属するもののみ取得できる

Declaredなし

- `public`のみ取得できる
- 祖先クラスも含めて属するものすべて取得できる

---

## Declaredあり・なしの違い

```java
public class Parent {
    public String field1;
}
```

```java
public class Child extends Parent {
    public String field2;
    private String field3;
}
```

- `Child.getFields()` ... `field1` `field2`が取得できる
- `Child.getDeclaredFields()` ... `field2` `field3`が取得できる

---

## 可視性

```java
public class ExampleObj {
    private String example;
}
```

```java
ExampleObj obj = ...
Field field = ExampleObj.class.getDeclaredField("example");
Object value = field.get(obj); //IllegalAccessException
```

---

## 可視性

```java
package com.example.sub;

public class SubPkgObj {
    String example;
}
```

```java
package com.example;

(中略)

SubPkgObj obj = ...
Field field = SubPkgObj.class.getDeclaredField("example");
Object value = field.get(obj); //IllegalAccessException
```

---

## 可視性

可視性を越えるおまじない、`setAccessible`

```java
ExampleObj obj = ...
Field field = ExampleObj.class.getDeclaredField("example");
if (field.canAccess(obj) == false) {
    field.setAccessible(true);
}
Object value = field.get(obj);
```

「これで`private`メソッドもテストし放題や！」🤔

???

Java 8まではisAccessibleを使っていたけれど9で非推奨になりcanAccessが追加された。

---

class: center, middle

# 合成メソッド<br/>ブリッジメソッド

---

## 合成メソッド

「クラスが持つメソッドすべて取得してほにゃららしたい」

```java
public class Nested {
    public void call(Nested.Obj obj) {
        obj.method();
    }

    public static class Obj { //こいつが対象
        private void method() { }
    }
}
```

???

10分

---

## 合成メソッド

```java
Method[] methods = Nested.Obj.class.getDeclaredMethods();

Object[] names = Arrays.stream(methods)
        .map(Method::getName).toArray();

System.out.println(Arrays.toString(names));
```

```sh
[method, access$0]
```

謎の`access$0`メソッド。

---

## 合成メソッド

```sh
javap -v Nested\$Obj.class
```

```none
static void access$0(example.reflection.Nested$Obj);
  descriptor: (Lexample/reflection/Nested$Obj;)V
  flags: (0x1008) ACC_STATIC, ACC_SYNTHETIC
  Code:
    stack=1, locals=1, args_size=1
       0: aload_0
       1: invokespecial #17                 // Method method:()V
       4: return
```

---

## 合成メソッド

`isSynthetic`で合成メソッドを除いて取得。

```java
Method[] methods = Nested.Obj.class.getDeclaredMethods();

Object[] names = Arrays.stream(methods)
        .filter(m -> m.isSynthetic() == false) //合成メソッドを除く
        .map(Method::getName).toArray();

System.out.println(Arrays.toString(names));
```

```sh
[method]
```

---

## ブリッジメソッド

```java
public class BridgeParent {
    public Object method() {
        return "parent";
    }
}
```

```java
public class BridgeChild extends BridgeParent {
    @Override
    public String method() { //戻り値の型を変えている
        return "child";
    }
}
```

???

戻り値の型を変えても問題は無いのか？

---

## ブリッジメソッド

オーバーライドしたメソッドの戻り値の型をサブタイプに変えても問題ない。

```java
BridgeParent b = new BridgeParent();
Object o = b.method();
```

```java
BridgeParent b = new BridgeChild();
Object o = b.method(); //StringはObject型の変数に代入できる
```

---

## ブリッジメソッド

```java
Method[] methods = BridgeChild.class.getDeclaredMethods();

Object[] names = Arrays.stream(methods)
        .map(Method::getName).toArray();

System.out.println(Arrays.toString(names));
```

```none
[method, method]
```

謎の2つの`method`メソッド。

---

## ブリッジメソッド

`Method.getName`ではなく`Method.toString`してみる。

```java
Method[] methods = BridgeChild.class.getDeclaredMethods();

for (Method method : methods) {
    System.out.println(method);
}
```

```none
public java.lang.String example.reflection.BridgeChild.method()
public java.lang.Object example.reflection.BridgeChild.method()
```

???

戻り値の型がObjectのmethodメソッドは何者なのか？

---

## ブリッジメソッド

```sh
javap -v BridgeChild.class
```

```none
public java.lang.Object method();
  descriptor: ()Ljava/lang/Object;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  Code:
    stack=1, locals=1, args_size=1
       0: aload_0
       1: invokevirtual #19                 // Method method:()Ljava/lang/String;
       4: areturn
```

---

## ブリッジメソッド

このコードの裏で行われていることの解説。

```java
BridgeParent b = new BridgeChild();
Object o = b.method();
```

- `BridgeParent`の`Object method()`を実行しようとする
- レシーバは`BridgeChild`なので`BridgeChild`の`Object method()`を実行する（ブリッジメソッド）
- `String method()`が実行される

???

15分

---

## ブリッジメソッド

`isBridge`でブリッジメソッドを除いて取得。


```java
Method[] methods = BridgeChild.class.getDeclaredMethods();

Object[] names = Arrays.stream(methods)
        .filter(m -> m.isBridge() == false) //ブリッジメソッドを除く
        .map(Method::getName).toArray();

System.out.println(Arrays.toString(names));
```

なお、ブリッジメソッドは合成メソッドでもあるので`isSynthetic`でも除ける。

---

class: center, middle

# オーバーライドの判定

---

## オーバーライドの判定

リフレクションのAPIに一発でオーバーライドを判定できるものは無い。

Java言語ではメソッド名と引数の型・並びがメソッドのシグネチャなので、それらを比較すればオーバーライドしているかどうかの判定ができるのでは？

---

## オーバーライドの判定

```java
public class Parent {
    public void method() { }
}
```

```java
public class Child extends Parent {
    public void method() { }
}
```

---

## オーバーライドの判定

これでOK？

```java
Method m1 = Parent.class.getDeclaredMethod("method");
Method m2 = Child.class.getDeclaredMethod("method");

boolean isOverrideMethod = m2.getName().equals(m1.getName())
  && Arrays.equals(m2.getParameterTypes(), m1.getParameterTypes());
```

---

## オーバーライドの判定

ｳｯ

```java
public class Parent {
    private void method() { }
}
```

```java
public class Child extends Parent {
    public void method() { }
}
```

親クラスのメソッドが`private`だとオーバーライドにならない。

???

よしわかった、親クラス側がprivateやったらfalseにしたらええんやな？

---

## オーバーライドの判定

これでOK？

```java
Method m1 = Parent.class.getDeclaredMethod("method");
Method m2 = Child.class.getDeclaredMethod("method");

boolean isOverrideMethod = m2.getName().equals(m1.getName())
  && Arrays.equals(m2.getParameterTypes(), m1.getParameterTypes())
  && Modifier.isPrivate(m1.getModifiers()) == false;
```

---

## オーバーライドの判定

ｳｯ

```java
package com.example;
public class Parent {
    void method() { }
}
```

```java
package com.example.sub;
public class Child extends Parent {
    void method() { }
}
```

親クラスのメソッドがパッケージプライベートだと子クラスのパッケージによってオーバーライドかどうかが決まる。

---

## オーバーライドの判定

```java
boolean b = m2.getName().equals(m1.getName())
  && Arrays.equals(m2.getParameterTypes(), m1.getParameterTypes());
boolean isOverrideMethod;
if (b) {
    if (Modifier.isPrivate(m1.getModifiers())) {
        isOverrideMethod = false;
    } else if (Modifier.isProtected(m1.getModifiers()) == false
            && Modifier.isPublic(m1.getModifiers()) == false
            && Modifier.isPrivate(m1.getModifiers()) == false) {
        isOverrideMethod = Objects.equals(
                m2.getDeclaringClass().getPackageName(),
                m1.getDeclaringClass().getPackageName());
    } else { isOverrideMethod = true; }
} else { isOverrideMethod = false; }
```

???

20分

---

class: center, middle

# その他便利なAPI

---

## その他便利なAPI

- `Proxy`
- `ParameterizedType`

---

class: center, middle

# Proxy

---

## Proxy

- Dynamic Proxyと呼ばれるもの
- `interface`の実装クラスを動的に生成できる

```java
public interface Hello {
    String say(String name);
}
```

---

## Proxy

```java
ClassLoader loader = getClass().getClassLoader();
Class<?>[] interfaces = { Hello.class };
InvocationHandler h = (Object proxy, Method method,
                       Object[] args) -> {
    return "Hello " + args[0];
};

Hello hello = (Hello) Proxy.newProxyInstance(loader, interfaces, h);

System.out.println(hello.say("World")); //Hello World
```

---

## Proxy

- 仕組みは単純で、愚直にバイトコードを書き出しているだけ
- 詳細は`java.lang.reflect.ProxyGenerator`を読めば良い
- 次のようにシステムプロパティをセットすると書き出されたバイトコードがファイルに保存される（保存先は`com/sun/proxy`以下）

```sh
-Djdk.proxy.ProxyGenerator.saveGeneratedFiles=true
```

???

保存先はパッケージを表すディレクトリ階層の下に出力されます。
つまりProxyはcom.sun.proxyパッケージに属します。
ただパッケージプライベートなinterfaceを実装する場合はそのパッケージに属するようになります。

---

## Proxy

なお、アノテーションは`Proxy`で実現されている。

例えば次のコードで取得した`RestController`アノテーションのインスタンスはDynamic Proxy。

```java
RestController annotation =
  HelloController.class.getAnnotation(RestController.class);
```

つまりアノテーションは`interface`なので`implements`できる。

---

class: center, middle

# ParameterizedType

---

## ParameterizedType

- 「Javaのジェネリクスはコンパイルしたら消える」
- Type Erasure
- 次のようなオーバーロードは認められていない

```java
void method(List<String> l);

void method(List<Integer> l);
```

コンパイルするとこうなる。

```java
void method(List l);

void method(List l);
```

???

25分

---

## ParameterizedType

でもクラス、フィールド、メソッド引数・戻り値の型変数は実行時に取得できる。

```java
public class Foo extends Bar<String> {

    private List<Integer> field;

    public List<Boolean> method(List<String> l) { ... }
}
```

---

## ParameterizedType

- `Class.getGenericSuperclass()`
- `Field.getGenericType()`
- `Method.getGenericParameterTypes()`
- `Method.getGenericReturnType()`

いずれも`java.lang.reflect.Type`(もしくはその配列)を返す。

`Type`は`Class`や`ParameterizedType`が実装・継承しているinterface。

対象がジェネリックな型だった場合は`ParameterizedType`が返ってくる。

---

## ParameterizedType

```java
public class Foo extends Bar<String> {
```

```java
ParameterizedType pt =
    (ParameterizedType) Foo.class.getGenericSuperclass();

Class<?> c = (Class<?>) pt.getActualTypeArguments()[0];

System.out.println(c); //class java.lang.String
```

例えばDIコンテナを作っていて`javax.inject.Provider<T>`をインジェクションしたい場合なんかに役立つ。

???

javax.inject.ProviderはJSR330で定義されているinterface。

---

class: center, middle

# まとめ

---

## まとめ

- リフレクションをする場合は合成メソッドとブリッジメソッドに気をつけよう
- Java言語ではメソッド名と引数の型・並びがシグネチャ、クラスファイルではメソッド名と引数の型・並び、戻り値の型がシグネチャ
- オーバーライドの判定マジつらい、Java言語ややこしい
- `Proxy`とか`ParameterizedType`は便利
- とはいえ普通にWebアプリケーションとか作る場合にはリフレクション使ったコードは書かない

???

まあ、でも面白いから興味を持ってくださった方はリフレクションで遊んでみてください！

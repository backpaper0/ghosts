class: center, middle

# DIコンテナ入門<br/>（裏面）

---

## 自己紹介

- 名前：うらがみ
- SNSアカウント：backpaper0
- 所属企業：TIS株式会社
- 2020年1月末に採用イベントやります。来てね

---

## 概要

裏面と題した本セッションではDIコンテナを支える技術であるバイトコードエンハンスを中心にお話します。

なお、[表面はこちら](https://jjug-cfp.cfapps.io/submissions/a5ba98a8-bad4-4122-bd61-85f89716c81e)。

裏面からもDIコンテナへ入門しましょう！

---

## バイトコードエンハンス

- バイトコード = クラスファイルの中身。JVMが読み取るコード
- エンハンス（enhance） = （能力などを）高める

---

## バイトコードエンハンス

```java
public class Calc {
    public int add(int a, int b) {
        return a + b;
    }
}
```

```sh
javac Calc.java
od -t x1 Calc.class
```

---

## バイトコードエンハンス

.small[
```none
0000000    ca  fe  ba  be  00  00  00  37  00  0f  0a  00  03  00  0c  07
0000020    00  0d  07  00  0e  01  00  06  3c  69  6e  69  74  3e  01  00
0000040    03  28  29  56  01  00  04  43  6f  64  65  01  00  0f  4c  69
0000060    6e  65  4e  75  6d  62  65  72  54  61  62  6c  65  01  00  03
0000100    61  64  64  01  00  05  28  49  49  29  49  01  00  0a  53  6f
0000120    75  72  63  65  46  69  6c  65  01  00  09  43  61  6c  63  2e
0000140    6a  61  76  61  0c  00  04  00  05  01  00  04  43  61  6c  63
0000160    01  00  10  6a  61  76  61  2f  6c  61  6e  67  2f  4f  62  6a
0000200    65  63  74  00  21  00  02  00  03  00  00  00  00  00  02  00
0000220    01  00  04  00  05  00  01  00  06  00  00  00  1d  00  01  00
0000240    01  00  00  00  05  2a  b7  00  01  b1  00  00  00  01  00  07
0000260    00  00  00  06  00  01  00  00  00  01  00  01  00  08  00  09
0000300    00  01  00  06  00  00  00  1c  00  02  00  03  00  00  00  04
0000320    1b  1c  60  ac  00  00  00  01  00  07  00  00  00  06  00  01
0000340    00  00  00  03  00  01  00  0a  00  00  00  02  00  0b
0000356
```
]

---

## バイトコードエンハンス

```sh
javap -v Calc
```

[ページに収まらないのでGistに貼り付けた](https://gist.githubusercontent.com/backpaper0/eefeb6c842e4c951322224a43e2a7ec3/raw/63729bb3908e01308016856af35d4d4c1395ac61/javap_-v_Calc.txt)

---

## バイトコードエンハンス

```java
    public int add(int a, int b) {
        return a + b;
    }
```

```none
  public int add(int, int);
    descriptor: (II)I
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=2, locals=3, args_size=3
         0: iload_1
         1: iload_2
         2: iadd
         3: ireturn
      LineNumberTable:
        line 3: 0
```

---

## バイトコードエンハンス

- バイトコードはクラスローダーでロードされる
- バイトコードエンハンスはバイトコードを動的に書き出してクラスローダーにロードさせる技術
- DIコンテナは既にあるバイトコードを改変するのではなくサブクラスを書き出す方式が多い
- JMockitはInstrumentation APIを使用して既にあるバイトコードを改変する
- Javaは静的型付言語なのに動的にバイトコードを弄るので「黒魔術」と表現されたりする
- Springでバイトコードエンハンスをする方法はいくつかあるっぽい
- ここでは`MethodInterceptor`を使う方法を紹介

---

## インターセプター

```java
public interface MethodInterceptor extends Interceptor {

	Object invoke(MethodInvocation invocation) throws Throwable;
}
```

```java
public interface Interceptor extends Advice {
}
```

```java
public interface Advice {
}
```

---

## インターセプター

```java
public class LoggingInterceptor implements MethodInterceptor {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Object invoke(MethodInvocation inv) throws Throwable {
        logger.debug("begin {}", inv.getMethod());
        Object ret = inv.proceed();
        logger.debug("end {}", inv.getMethod());
        return ret;
    }
}
```

---

## インターセプター

```java
    @Bean
    public BeanPostProcessor autoProxyCreator() {
        return new DefaultAdvisorAutoProxyCreator();
    }

    @Bean
    public Advisor advisor() {
        var a = new DefaultPointcutAdvisor();
        a.setAdvice(new LoggingInterceptor());
        a.setPointcut(AnnotationMatchingPointcut
                .forMethodAnnotation(WithLogging.class));
        return a;
    }
```

---

- スコープとproxy
- コンストラクタとシリアライズ

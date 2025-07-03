# AndroidJavascriptBridge

[![](https://jitpack.io/v/yieldica/AndroidJavascrptBridge.svg)](https://jitpack.io/#yieldica/AndroidJavascrptBridge)

An elegant way to send message between `Kotlin` and `WebView`.

You don't need to write any javascript code.

And the web developer don't need write any extra javascript code either.

[Swift Version](https://github.com/octree/SwiftWKBridge)

## Setup

Add repository

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
```

Add dependency

lastVersion: [![](https://jitpack.io/v/yieldica/AndroidJavascrptBridge.svg)](https://jitpack.io/#yieldica/AndroidJavascrptBridge)
```kotlin
dependencies {
    implementation("com.github.yieldica:jsbridge:lastVersion")
}
```

## Notes

* **Before using `injector`, you should set `WebViewClient` to support some low version `WebView`which
  doesn't support `addDocumentStart`.**
* **You need to enable javaScript: `javaScriptEnabled = true`**

## Example

It's very easy to define a javascript function with native code

```kotlin
  // simple test
val testHello: () -> Unit = {
    println("hello")
}
webView.injector.inject("hello", testHello)
// js: hello()

val testOneArg: (String) -> Unit = {
    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
}
webView.injector.inject("testOneArg", testOneArg)
// js: testOneArg("hello")

val testTwoArg: (String, Int) -> Unit = { arg0, arg1 ->
    Toast.makeText(this, "$arg0, $arg1", Toast.LENGTH_SHORT).show()
}
webView.injector.inject("testTwoArg", testTwoArg)
// js: testTwoArg("hello", 1)

val testNullableArg: (String?) -> Unit = {
    Toast.makeText(this, it ?: "got null", Toast.LENGTH_SHORT).show()
}
webView.injector.inject("testNullableArg", testNullableArg)
// js: testNullableArg("hello")
// or: js: testNullableArg()
// or: js: testNullableArg(null)
```

define a function with callbacks

```kotlin
 val testCallback: (String, Callback) -> Unit = { arg0, callback ->
    callback.invoke("arg0: $arg0")
}
webView.injector.inject("testCallback", testCallback)
// js: testCallback("hello", (result) => { console.log(result) })

val testScriptCallback: (String, Callback) -> Unit = { arg0, callback ->
    callback.invokeScript("{arg0: '$arg0'}")
}
webView.injector.inject("testScriptCallback", testScriptCallback)
// js: testScriptCallback("hello", (result) => { console.log(result.arg0) })
```

Serializable

```kotlin
@Serializable
data class User(
    var name: String,
    var age: Int,
    var nickname: String? = null
)

val testSerializableArgs: (User, Callback) -> Unit = { user, callback ->
    user.nickname = user.nickname?.uppercase() ?: "Yieldica"
    user.age++
    user.name = user.name.uppercase()
    callback.invoke(user)
}
webView.injector.inject("testSerializableArgs", testSerializableArgs)
// testSerializableArgs({ name: "Octree", age: 100 }, (user) => { /* ... */ })
```

Async

```kotlin
 val testVoidPromise: suspend () -> Unit = {
    withContext(Dispatchers.Main) {
        Toast.makeText(
            this@MainActivity,
            "hello world",
            Toast.LENGTH_SHORT
        ).show()
    }
}
webView.injector.injectPromise("testVoidPromise", testVoidPromise)
// js: await testVoidPromise()

val testThrowPromise: suspend () -> Unit = {
    throw Exception("error")
}
webView.injector.injectPromise("testThrowPromise", testThrowPromise)
// js: testThrowPromise().catch( (e) => console.log(e) )

val testSerializableThrowPromise: suspend () -> Unit = {
    throw TestSerializableError(123, "This is a SerializableError")
}
webView.injector.injectPromise("testSerializableThrowPromise", testSerializableThrowPromise)
// js: testSerializableThrowPromise().catch( (e) => console.log(e) )

val testFunctionPromise: suspend () -> PromiseScript = {
    PromiseScript("function() { console.log('This is a Function result'); }")
}
webView.injector.injectPromise("testFunctionPromise", testFunctionPromise)
// js: func = await testFunctionPromise()
// js: func()
```



# Multipart

HTTP 其实并不关心我们想要传输的是什么类型的数据，因为它什么类型的数据都可以传输！对于客户端或者服务器来说，只需要设置好合适的 `Content-Type`，让另一端能够理解数据就可以了。在一个请求中，客户端发送给服务器的数据可以是文本，也可以是图片或者音视频等等。 如果客户端想在一个请求中给服务器发送多种类型的数据呢，该怎么办呢？例如，同时发送用户的基本信息和头像。方法很简单，就是使用 HTTP 的 multipart。

## Multipart消息结构

Multipart 允许客户端在一次 HTTP 请求中发送多个部分（part）数据，每部分数据之间的类型可以不同。Multipart 并不是一种专一的数据类型，它有很多子类型，例如：`multipart/mixed`，`multipart/form-data` 等。 通俗来讲，一个 multipart 消息就是一个大包裹，包裹里面有多个不同类型的消息，每一个消息就是一个 part，每个 part 都会声明自己的消息类型（Content-Type）。除了消息类型，part 还可以附加一些元数据。 Multipart 消息的基本语法结构可以在 [RFC2046](https://datatracker.ietf.org/doc/html/rfc2046#section-5.1.1) 中找到：

* 每个 multipart 消息的 `Content-Type` 都必须包含一个叫做 `boundary` 的参数，`boundary` 声明了各个 part 之间的边界，记为 `${boundary}`。实际上，完整的边界定义为：一行由两个 `-` 加上 `${boundary}` 组成的字符串。假设我们在 `Content-Type` 里面指定的 `boundary=example-part-boundary`，那么按照协议规定，每个 part 之间的分隔行就是：`--example-part-boundary`。
* 每个边界之后是一个 `CRLF` 加下一个 part 的头部信息。如果下一个 part 没有头部信息，边界之后就应该跟两个 `CRLF`，这样下一个 part 的消息类型就会被认为是 `text/plain`。
* `${boundary}` 不能出现在边界之间，并且长度不能超过 70 个字符。
* 最后一个 part 之后的边界在末尾多了两个 `-`，表示后面不会再有其它的 part 了。这个边界的完整格式为：`--${boundary}--`，例如 `--example-part-boundary--`。


## multipart/form-data

Multipart 的使用在 Web 应用程序中很常见。使用 multipart 频率最高的地方大概就是 Web 表单了，在表单提交时，文件的上传就是通过 Multipart 来实现的。 并不是所有的表单提交都会使用 multipart，如果表单只包含基于文本的输入组件（例如输入框、单选框等），浏览器会将这些数据以 `key=value` 的形式组织，使用一种被称为 `application/x-www-form-urlencoded` 的 `Content-Type` 传输。 如果表单中包含文件或图片等不能被编码成文本的元素，浏览器就会使用 `multipart/form-data` 向服务器传输数据。

下面是一个使用 `multipart/form-data` 传输用户信息的例子：

```http
POST /profile HTTP/1.1
HOST: example.com
Content-Type: multipart/form-data; boundary=example-part-boundary

--example-part-boundary
Content-Disposition: form-data; name="username"
Content-Type: text/plain

Nicholas
--example-part-boundary
Content-Disposition: form-data; name="address"
Content-Type: application/json

{
    "country": "China",
    "city": "Beijing"
}
--example-part-boundary
Content-Disposition: form-data; name="avatar"; filename="my_avatar.jpeg"
Content-Type: image/jpeg

<binary-image data>
--example-part-boundary--
```

在上面这个请求中：

* `Content-Type: multipart/form-data; boundary=example-part-boundary` 表示这个请求的的消息类型是 `multipart-form-data`，每个 part 之间的边界为 `example-part-boundary`。
* 这个请求总共包含三个 part：
  * 第一个 part 的类型为 `text/plain`，它在表单上对应的 `key` 为 `username`，`value` 为 `Nicholas`。
  * 第二个 part 的类型为 `application/json`，它在表单上对应的 `key` 为 `address`。
  * 第三个 part 的数据类型为 `image/jpeg`，它在表单上对应的 `key` 为 `avatar`，并且 part 的头部还附加了文件名相关的元数据 `filename="my_avatar.jpeg`。
* 最后面的 `--example-part-boundary--` 表示整个 multipart 消息的结束。


## **multipart/byteranges**



# 分块传输

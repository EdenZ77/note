Web 应用是基于 Web 技术开发的具体应用程序，其中包含了多个 Web 服务。Web 应用可以部署在 Web 服务器上，而 Web 服务器则为 Web 应用提供运行与管理所需的基础设施。

<img src="image/FvClPq96jO4n4x4MPOcx19IZAKCO" alt="img" style="zoom:50%;" />

## 如何实现一个 Web 服务

为了高效开发一个 Web 服务，通常需要进行以下技术选型：

- **通信协议：**根据具体的业务场景和需求选择合适的通信协议；
- **数据交换格式：**根据具体的业务场景和需求选择适用的数据交换格式。在选择数据交换格式时，还需考虑通信协议的支持情况，因为不同的通信协议支持的数据交换格式可能不同；
- **Web 框架：**由于 Web 服务通常包含多个 API 接口，选择合适的 Web 框架可以提高开发效率和代码复用率。我们可以选择从零自行设计开发一个框架，也可以直接使用业界成熟的开源 Web 框架。在选择 Web 框架时，需要考虑通信协议和数据交换格式，因为每个 Web 框架支持的通信协议和数据交换格式可能有所不同。

## 如何选择合适的通信协议和数据交换格式

开发 Web 服务的第一步是根据业务场景和需求选择适用的通信协议与数据交换格式，二者的定义如下：

- **通信协议：**通信协议是规定计算机或设备之间通信规则的协议，定义了数据传输的格式、传输方式、错误检测及纠正机制等。常见的通信协议包括 HTTP、RPC、WebSocket、TCP/IP、FTP 等。不同的通信协议支持的数据交换格式也会有所不同；
- **数据交换格式：**数据交换格式（也称数据序列化格式）是为不同系统之间传输和解析数据制定的规范，定义了数据的结构、编码方式及解析方法。常见的数据交换格式包括 JSON、Protobuf、XML 等。

首先，我们需要根据业务场景和需求，选择合适的通信协议。在 Go 项目开发中，常用的通信协议包括 HTTP、RPC 和 WebSocket，其中使用最频繁的是 HTTP 和 RPC。在实际开发中，通常选择 REST API 接口规范来开发 API 接口，这些 API 接口的底层通信基于 HTTP 协议。而实现 RPC 通信时，则通常使用 gRPC 框架。gRPC 是由谷歌开源的一个 RPC 框架。

> 提示：RPC 也可以理解为一种通信协议，但它是基于其他协议（例如 TCP、UDP、HTTP）封装而成的通信协议。

接下来，根据所选的通信协议，选择最佳适配的数据交换格式。HTTP 和 RPC 各自有其推荐使用的数据交换格式，这可以视为事实上的标准。在无特殊需求的情况下，一般不需要改变这种适配关系：HTTP 协议通常采用 JSON 数据格式，而 RPC 通常采用 Protobuf 数据格式。

HTTP 和 RPC 在不同的场景下各有适配。在企业应用开发中，通常会结合两种通信协议，共同构建一个高效的 Go 应用：

- **对外：**REST（基于 HTTP 协议）+JSON 的组合。由于 REST API 接口规范清晰直观，JSON 数据格式易于理解和使用，并且客户端和服务端通过 HTTP 协议通信时无需使用相同的编程语言，因此 REST+JSON 更适合用于对外提供 API 接口；
- **对内：**gRPC（基于 RPC 协议）+Protobuf 的组合。由于 RPC 协议调用便捷、Protobuf 格式的数据传输效率更高，因此 gRPC+Protobuf 更适合用于对内提供高性能的 API 接口。

为了更好地开发 Web 服务，通常不会直接使用裸 HTTP 或 RPC 协议，而是基于这些协议封装一层框架来使用。

REST+JSON 和 RPC+Protobuf 这两种组合在企业级应用中应用广泛。二者并非相互取代，而是各自适用于不同的场景，相辅相成。在企业应用中，REST 与 RPC 的组合方式通常如图 7-2 所示。

<img src="image/FgJZ8_LogcIwr4aCfDyybtYx8q0e" alt="img" style="zoom:50%;" />



## miniblog 项目中实现的 Web 服务类型

miniblog 是一个小而美的项目，虽然项目不大，却同时实现了 HTTP 和 gRPC 两种 Web 服务类型，miniblog 具体的服务类型如图 7-3 所示。

<img src="image/FoC6e5yVZji20MGdXhpBHdA9P5Ma" alt="img" style="zoom:50%;" />

miniblog 项目使用 Gin 框架实现了 HTTP 服务，使用 gRPC 框架实现了 gRPC 服务，使用 grpc-gateway 实现了 HTTP 反向代理服务，用来将 HTTP 请求转发给 gRPC 服务。同时，miniblog 项目支持通过配置文件中的 tls.use-tls 配置项开启 TLS 认证。mb-apiserver 服务启动时，可通过配置文件中的 server-mode 配置项来配置启动的 Web 服务类型：

- server-mode=gin：启动使用 Gin Web 框架开发的 HTTP 服务；
- server-mode=grpc：启动使用 grpc+grpc-gateway 框架开发的 gRPC 服务，同时支持 HTTP 请求。在 mb-apiserver 接收到 HTTP 请求后，HTTP 反向代理服务，会将 HTTP 请求转换为 gRPC 请求，并转发给 gRPC 服务接口。




为了让你更好的学习本课程。本节课，我来简单介绍下 fastgo 项目的源码组成及目录结构。

## 目录结构规范

项目开发的第一步便是制定一个合理的目录结构。因为目录结构直接影响着项目的可维护性。

当前社区并没有官方的 Go 项目目录规范。社区中也有不同的目录结构设计方案，当前接受度最高的方案是 [golang-standards/project-layout](https://github.com/golang-standards/project-layout)，中文文档为：[Go项目标准布局](https://github.com/golang-standards/project-layout/blob/master/README_zh-CN.md)。

很多大型的开源项目都采用了 project-layout项目所建议的目录规范。

## fastgo 项目目录结构介绍

fastgo 项目及云原生 AI 实战营中的所有项目也都采用了 project-layout 目录规范。fastgo 目录结构及说明如下：

```shell
├── build.sh*                      # 构建脚本，用于编译 fg-apiserver 源码  
├── cmd/                           # 放置可独立运行的可执行程序入口  
│   └── fg-apiserver/             # fg-apiserver 服务的主入口目录  
│       ├── app/                  # 存放与该服务启动配置和初始化相关的代码  
│       │   ├── config.go         # 读取和管理配置信息的核心逻辑  
│       │   ├── options/          # 定义命令行或其他参数选项  
│       │   │   └── options.go    # 具体的选项结构体及参数解析逻辑  
│       │   └── server.go         # 启动并运行服务器的核心逻辑  
│       └── main.go               # 程序入口点，加载配置、启动服务等核心流程  
├── configs/                       # 配置文件及数据库脚本  
│   ├── fastgo.sql                # 数据库初始化脚本  
│   └── fg-apiserver.yaml         # fg-apiserver 服务配置文件  
├── docs/                          # 项目文档、说明文件或接口文档等  
├── go.mod                         # Go 模块管理文件，记录依赖模块与版本  
├── go.sum                         # 存放 go.mod 中依赖的精确版本哈希  
├── go.work                        # Go 工作区文件，用于管理多个模块  
├── go.work.sum                    # go.work 的依赖校验文件  
├── internal/                      # 不希望被外部引用的包，存放主要业务逻辑  
│   ├── apiserver/                # API 服务器的核心实现  
│   │   ├── biz/                  # 业务层相关逻辑  
│   │   │   ├── biz.go            # 核心业务逻辑入口  
│   │   │   ├── doc.go            # 业务层相关文档注释  
│   │   │   ├── README.md         # 业务层的说明文档  
│   │   │   ├── v1/               # v1 版本的业务实现  
│   │   │   │   ├── post/         # 博客相关业务  
│   │   │   │   │   └── post.go   # 博客业务相关逻辑  
│   │   │   │   └── user/         # 用户相关业务  
│   │   │   │       └── user.go   # 用户业务相关逻辑  
│   │   │   └── v2/               # 新版本（v2）业务逻辑（示例或预留）  
│   │   ├── handler/              # HTTP 处理层，用于接收、返回数据  
│   │   │   ├── handler.go        # 通用处理逻辑或公共处理方法  
│   │   │   ├── post.go           # 博客相关接口的 HTTP 处理函数  
│   │   │   └── user.go           # 用户相关接口的 HTTP 处理函数  
│   │   ├── model/                # 数据模型定义与自动生成的代码  
│   │   │   ├── hook.go           # 模型钩子函数，如果使用了 GORM hook  
│   │   │   ├── post.gen.go       # 博客模型的自动生成代码  
│   │   │   └── user.gen.go       # 用户模型的自动生成代码  
│   │   ├── pkg/                  # 存放与当前 apiserver 业务紧密相关的通用函数或包  
│   │   │   ├── conversion/       # 数据转换，用于结构体之间的转换逻辑  
│   │   │   │   ├── post.go       # 文章数据转换逻辑  
│   │   │   │   └── user.go       # 用户数据转换逻辑  
│   │   │   └── validation/       # 数据校验相关  
│   │   │       ├── post.go       # 文章校验逻辑  
│   │   │       ├── user.go       # 用户校验逻辑  
│   │   │       └── validation.go # 通用校验工具和方法  
│   │   ├── server.go             # 整个 apiserver 的核心启动逻辑  
│   │   ├── server.go~            # 临时文件或备份文件  
│   │   └── store/                # 数据存储层接口与实现  
│   │       ├── doc.go            # 存储层文档注释  
│   │       ├── logger.go         # 存储相关日志逻辑  
│   │       ├── post.go           # 文章存储操作  
│   │       ├── README.md         # 存储层的说明文档  
│   │       ├── store.go          # 存储层接口定义或核心实现  
│   │       └── user.go           # 用户存储操作  
│   └── pkg/                      # 内部公共包，通用功能组件  
│       ├── contextx/             # 扩展和封装与上下文相关的功能  
│       │   ├── contextx.go       # 上下文工具函数  
│       │   └── doc.go            # 文档注释  
│       ├── core/                 # 核心方法或基础工具  
│       │   └── core.go           # 核心功能实现  
│       ├── errorsx/              # 错误处理扩展  
│       │   ├── code.go           # 错误码定义  
│       │   ├── errorsx.go        # 自定义错误类型和逻辑  
│       │   ├── post.go           # 文章相关错误定义  
│       │   └── user.go           # 用户相关错误定义  
│       ├── known/                # 存放常量或可复用的“已知”配置  
│       │   └── known.go          # 常量定义  
│       ├── middleware/           # HTTP 中间件  
│       │   ├── authn.go          # 认证相关中间件  
│       │   ├── header.go         # 处理头部信息的中间件  
│       │   └── requestid.go      # 处理请求 ID 的中间件  
│       └── rid/                  # Request ID 生成和管理  
│           ├── doc.go            # 文档注释  
│           ├── rid.go            # Request ID 的生成逻辑  
│           ├── rid_test.go       # 测试文件  
│           └── salt.go           # 生成 Request ID 时的盐值相关逻辑  
├── _output/  
│   └── fg-apiserver*             # 编译输出的可执行文件  
├── pkg/                           # 可被对外使用的公共包  
│   ├── api/                       # API 定义与结构体  
│   │   └── apiserver/  
│   │       └── v1/  
│   │           ├── post.go       # v1 版文章相关 API 定义  
│   │           └── user.go       # v1 版用户相关 API 定义  
│   ├── auth/                      # 鉴权相关逻辑  
│   │   └── authn.go              # 登录验证或 Token 验证逻辑  
│   ├── options/                   # 项目通用选项配置  
│   │   └── mysql_options.go       # MySQL 连接相关配置  
│   ├── token/                     # Token 生成、解析等  
│   │   ├── doc.go                # 文档注释  
│   │   ├── token.go              # Token 相关主逻辑  
│   │   └── token_test.go         # 测试文件  
│   ├── token~/                    # 临时或备份目录  
│   │   └── token.go              # 备份的 Token 逻辑文件  
│   └── version/                   # 版本信息  
│       ├── doc.go                # 文档注释  
│       ├── flag.go               # 版本标识相关的命令行标识  
│       └── version.go            # 版本信息相关逻辑  
├── README.md                      # 项目说明文档  
└── scripts/  
    └── test.sh*                  # 测试命令脚本或自动化测试入口
```


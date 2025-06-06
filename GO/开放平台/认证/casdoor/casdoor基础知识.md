# 概述

Casdoor 是一个以用户界面（UI）为导向的身份访问管理（IAM）/单点登录（SSO）平台，支持 OAuth 2.0、OIDC、SAML、CAS、LDAP、SCIM、WebAuthn、TOTP、MFA、RADIUS、Google Workspace、Active Directory 和 Kerberos 等协议。



在身份认证和授权的领域中，服务提供商（Service Provider, SP）和身份提供商（Identity Provider, IdP）是两个核心概念：

1. **身份提供商 (Identity Provider, IdP)**：
   - 身份提供商是负责维护和管理用户身份信息的系统，通常提供认证服务，允许用户登录并验证其身份。
   - IdP存储有关用户的信息，如用户名、密码、角色和其他个人识别信息，并使用这些信息来对用户进行身份验证。
   - 当用户尝试登录应用程序时，身份提供商会验证用户的凭据，并根据验证结果向应用程序提供信息，例如安全令牌或断言。
   - 一个常见的身份提供商的例子是OAuth和OpenID Connect服务，如Google、Facebook或企业级的解决方案如Okta、Microsoft Azure Active Directory等。

2. **服务提供商 (Service Provider, SP)**：
   - 服务提供商是提供应用程序或服务的系统，它依赖于IdP来对用户进行身份验证。
   - SP与IdP协作，将认证过程委托给IdP，从而允许用户使用单一的凭据登录多个不同的服务，这被称为单点登录（Single Sign-On, SSO）。用户登录一次后，可以在不同的系统和服务中无缝切换，而无需重复登录。
   - 当用户尝试访问SP提供的服务时，SP通常会重定向用户到IdP进行身份验证。一旦用户被成功验证，IdP会向SP提供一个认证的标记（token），如SAML断言或JWT令牌，SP将使用这个标记来创建用户会话。

在SSO和联合身份管理系统中，SP和IdP可以通过标准化的协议进行通信，如安全断言标记语言（Security Assertion Markup Language, SAML）或OpenID Connect。这些协议定义了如何在SP和IdP之间安全地传递身份信息和认证凭据。

使用SP和IdP的好处是提高了安全性和用户体验。用户不需要为每个服务创建和记住独立的登录凭据，同时服务提供商也不需要处理用户的身份验证和凭据管理，可以将这些工作委托给专门的身份提供商。这样还可以减少密码泄露的风险，因为用户的登录信息只在IdP中存储。










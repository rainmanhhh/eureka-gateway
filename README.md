# eureka-gateway

#### 介绍
spring cloud网关
- 基于jwt的用户鉴权（不包括登录），优先从http header `Authorization`中取token，取不到则尝试从cookie取（默认的cookie name为`JWT`），token必须包含schema前缀（默认为`Bearer`，用空格与token值分隔）
- 支持微服务共享密钥service-api-key（SAK），集群内的微服务通过此网关互相调用时，可通过SAK互认，跳过用户权限控制，直接放行。使用此功能的微服务需要引入fy:auth-spring-boot-starter依赖，配置好SAK，再用feign客户端互访，请求必须通过网关中转
- 可通过block users黑名单暂时禁用某些用户访问（用caffeine缓存实现，通过设置有效期与jwt token一致使对应用户的客户端token持续被阻止，直到它们自动过期）

#### 软件架构
spring cloud

#### 使用说明
- 生产环境配置模板（注意修改参数的值）
```yaml
eureka:
  client:
    enabled: true
    service-url:
      defaultZone: http://127.0.0.1:8761/eureka #若eureka server被设置为其他端口，或与gateway不在同一台服务器，此处需要修改
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-methods: '*'
            allowed-headers: '*'
            allowed-origin-patterns: #cors允许的域名表达式，不可配置为'*'否则前端请求无法带cookie
              - 'http://localhost'
              - 'http://localhost:3000'
      routes:
        - id: some-service #此处为手动挂载的微服务，类似nginx反向代理（因为某些服务可能不是springboot架构，无法通过eureka client自动注册到gateway）
          uri: http://127.0.0.1:10000
          predicates:
            - Path=/M-some-path/** #若首字母大写，则请求路径的第一段（此处为`M-some-path`）会被解析为rule group，否则该服务将只匹配common group中定义的公共规则
          filters:
            - StripPrefix=1 #表示去掉前缀后再转发，例如`/M-some-path/foo/bar`转发后，微服务实际接收到的请求路径为`/foo/bar`
  r2dbc: #异步数据库客户端配置，目前官方还只支持一部分数据库，一般用`mysql`或`pg`
    url: r2dbc:mysql://${mysql.host}:${mysql.port:3306}
    username: ${mysql.username}
    password: ${mysql.password}
  cache:
    caffeine: #为提高访问速度，使用进程内缓存caffeine而不是redis
      spec: ${caffeineSpec:'expireAfterWrite=86400s'} #字符串格式，逗号分隔的caffeine参数，类似于合并到一行的java properties文件，具体说明见caffeine官方文档，注意这里的时间参数都带有单位后缀，一般用s（秒）
logging:
  level:
    root: info
    fy: info
ez:
  jwt:
    token-expire-seconds: ${jwt.ttl:86400} #需要与caffeine写入过期时间（expireAfterWrite）保持一致，以使block users黑名单正常运作
    secret-key: ${jwt.key:12345678901234567890123456789012} #某些算法对key的格式有要求，比如默认的HS256就要求key长度至少为256个字节（即32个字符）
gateway:
  auth:
    service-api-key:
      value: ${sak.value:9876543210987654321021} #service-api-key密钥，生产环境建议用随机生成的hash值，实际发送请求时，会与salt拼接，然后进行一次md5（32位小写hex），最后再与salt拼接发送。故最终的报文头中，前32位是加密后的key，32位之后是明文salt
      salt: ${sak.salt:Math.random().toString()} #service-api-key盐，如果不配置，则每次app启动时随机生成一个值，推荐留空
```

#### 请求路径规则说明
- 采用`AntPathMatcher`对请求路径进行匹配，规则优先级大于0的称为`硬规则`，反之为`软规则`
- `软规则`校验不通过时，允许继续匹配下一条规则（白名单模式）
- `硬规则`校验不通过时，立即抛出http 403错误（黑名单模式）
- 规则类型（type）与shiro基本一致，但只有`user`而没有`authc`
- `rest`类型规则说明：`param`为权限名，与`perm`的不同之处在于可按"权限名:HttpMethod"方式授权，
  例如权限名为"some-right"，给某用户或角色授权时，
  "some-right"表示授予全部的增删改查权限(HttpMethod为GET,POST等任意值均放行)，
  "some-right:GET"表示只授予GET权限
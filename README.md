# eureka-gateway

#### 介绍
spring cloud eureka网关
- 请先阅读[gateway-server-spring-boot-starter](https://github.com/rainmanhhh/gateway-server-spring-boot-starter)说明文档
- 本项目在`gateway-server-spring-boot-starter`的基础上适配eureka服务中心

#### 软件架构
spring cloud

#### 生产环境配置模板
- 基本内容见`gateway-server-spring-boot-starter`
- 额外添加以下部分
```yaml
eureka:
  client:
    enabled: true
    service-url:
      defaultZone: http://127.0.0.1:8761/eureka #若eureka server被设置为其他端口，或与gateway不在同一台服务器，此处需要修改
logging:
  level:
    root: info
```
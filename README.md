# code-generator-persion使用

#### 介绍

Mybatis plus的code-generator-persion代码生成器，自动生成车车项目基础代码

#### 软件架构

spring boot，mybatis plus，swagger2


#### 安装教程

1. 导入项目至idea

2. `application.yml`修改数据库信息

   ![image-20220422164131949](https://minio.service.cf/img/image-20220422164131949.png)

3. `generator.properties`修改模块名，作者名

   ![image-20220422164217151](https://minio.service.cf/img/image-20220422164217151.png)

#### 使用说明

1. 启动CodeGeneratorPeserionApp

2. postman中：保存`localhost:8081/generator/code?tables=auto`响应文件（压缩包）

   ![image-20220422164357492](https://minio.service.cf/img/image-20220422164357492.png)

3. 将压缩包内文件依次移入到项目中使用：

   ![image-20220422165058390](https://minio.service.cf/img/image-20220422165058390.png)

4. 测试生成代码接口

   ![image-20220422165221057](https://minio.service.cf/img/image-20220422165221057.png)

#### 附加功能

1.  一次生成多表，表名参数之间使用英文“,”分割
2.  配置模板在resources中的template，可自行定义

#### 存在问题

+ 自行检验实体中的更新时间和创建时间继承关系，默认自动继承`BaseTimeTrackEntity`

+ xml中`from`之前`,`需要手动删除


#### 参与贡献

1.  huibq
2.  https://mybatis.plus/

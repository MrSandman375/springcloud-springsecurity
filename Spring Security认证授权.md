[TOC]

# Spring Security 简单的快速开始

## 基于yml配置文件

- 引入依赖

```xml
 <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
 </dependency>
```

- 配置文件

```yml
spring:
  security:
    user:
      name: renjie
      password: 123456
```

- controller类

```java
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public String test(){
        return "hello,Security";
    }

}
```

- 测试（http://localhost:8080/test/hello）

![image-20201116134019931](C:\Users\Fan\AppData\Roaming\Typora\typora-user-images\image-20201116134019931.png)

输入配置的账号密码即可访问成功

## 基于config配置类的

- SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    //基于内存的用户存储
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //密码加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String password = passwordEncoder.encode("123456");
        //认证
        auth.inMemoryAuthentication()
                .withUser("user").password(password).roles("USER").and()//账号，密码，权限
                .withUser("admin").password(password).roles("USER","ADMIN");
    }

    //不允许密码明文,没有这段会报错
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
```

- 测试（同上，略）

## 查询数据库中的账号密码（常用的情况）

- 引入mybatis-plus和java

```xml
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.22</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.4.1</version>
        </dependency>
```

- 数据库&实体类

*这里的数据库比较简单，也就id，username，password这仨，就不再赘述*

- yml文件中添加数据库配置

```yml
spring:
  datasource:
    username: root
    password: 123456
    url: jdbc:mysql://localhost:3306/security?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT
    driver-class-name: com.mysql.cj.jdbc.Driver
```

- mybatis-plus的UsersMapper接口

```java
@Repository
public interface UsersMapper extends BaseMapper<Users> {
}
```

- config配置

```java
@Configuration
@EnableWebSecurity
public class DBSecurityConfig extends WebSecurityConfigurerAdapter {

    //这里的重点是实现UserDetailsService
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

}
```

- 实现UserDetailsService

```java
@Service("userDetailsService")
public class MyUserDetailsService implements UserDetailsService {

    //注入UsersMapper
    @Autowired
    private UsersMapper usersMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //从数据库中查到username对应的password
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("username",username);
        Users users = usersMapper.selectOne(wrapper);
        //判断是否为空
        if(users == null){
            throw new UsernameNotFoundException("用户名不存在");
        }else {
            List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ADMIN");
            //从数据库中返回users对象，得到用户名密码，返回
            return new User(users.getUsername(),new BCryptPasswordEncoder().encode(users.getPassword()),authorities);
        }
    }
}
```

- 测试（略）

## 自定义设置登录页面不需要认证可以访问

- 在config中添加

```java
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.formLogin() //自定义自己编写的登录页面
            .loginPage("/login.html") //登录页面设置
            .loginProcessingUrl("/user/login") //等访问路径
            .defaultSuccessUrl("/test/index").permitAll() //登录成功后跳转的路径
            .and().authorizeRequests()
                .antMatchers("/","/test/hello","/user/login").permitAll() //那些路径可以直接访问不需要认证
            .anyRequest().authenticated()
            .and().csrf().disable(); //关闭CSRF防护

    }
```

- Controller中加一个跳转的路径

```java
@GetMapping("/index")
public String index(){
    return "hello index";
}
```

- 自定义的登录页面/static/login.html

```html
    <form action="/user/login" method="post">
        <!--这里的name如果不是username的话security拿不到-->
        用户名：<input type="text" name="username" />
        <br/>
        密码：<input type="password" name="password" />
        <br/>
        <input type="submit" value="Login"/>
    </form>
```

- 测试（略）

## 基于角色或权限进行访问控制

- 主要还是加一个hasAuthority方法

```java
    //自定义用户登录页面
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.formLogin() //自定义自己编写的登录页面
            .loginPage("/login.html") //登录页面设置
            .loginProcessingUrl("/user/login") //等访问路径
            .defaultSuccessUrl("/test/index").permitAll() //登录成功后跳转的路径
            .and().authorizeRequests()
                .antMatchers("/","/test/hello","/user/login").permitAll() //那些路径可以直接访问不需要认证
                //当前登录用户，只有具有相应的权限才可以访问这个路径
                //.antMatchers("/test/index").hasAuthority("admin")
                //当前登录用户，只有具有其中任意的的权限都可以访问这个路径
                .antMatchers("/test/index").hasAnyAuthority("admin,user")
                .anyRequest().authenticated()
            .and().csrf().disable(); //关闭CSRF防护

    }
```

- 关于类似的hasRole和hasAnyRole

*使用这个hasRole需要在用户service那边查到的权限前拼接一个ROLE_，除此之外和hasAuthority一致，hasAnyRole同上*

## 自定义权限不足的403界面

- 直接在配置类中设置

```java
 //没有访问权限跳转的页面
 http.exceptionHandling().accessDeniedPage("/unauth.html");
```

## 相关注解使用

- @Secured() 

  *使用的前提条件是在启动类或者配置类上添加  <u>@EnableGlobalMethodSecurity(securedEnabled = true)</u> 开启注解功能*

  使用方法：在controller的方法上添加 @secured("能够访问这个接口的角色")

- @PreAuthorize() （方法执行之前校验）

  *使用的前提条件是在启动类或者配置类上添加  <u>@EnableGlobalMethodSecurity(prePostEnabled = true)</u> 开启注解功能*

  使用方法：在controller上添加注解 @PreAuthorize("hasAnyAuthority('admin,user')")

- @PostAuthorize()  （方法执行之后校验，一般用于返回值）

  *使用的前提条件是在启动类或者配置类上添加  <u>@EnableGlobalMethodSecurity(prePostEnabled = true)</u> 开启注解功能*

  使用方法：在controller的方法上添加 @PostAuthorize("hasAnyAuthority('ROLE_admin,user')")

- @PostFilter()  （对方法返回的数据进行过滤）

  用法示例：在controller的方法上添加注解

  ```java
      @GetMapping("/getAll")
      @PostFilter("filterObject.username == 'Jack'")
      @PostAuthorize("hasAnyAuthority('ROLE_admin,ROLE_user')") //对返回数据进行过滤
      public List<Users> getAllUser(){
  
          ArrayList<Users> list = new ArrayList<>();
          list.add(new Users(11,"Jack","123456","admin"));
          list.add(new Users(21,"Mary","123456","user"));
          System.out.println(list);
          return list;
      }
  ```

- @PreFilter()   （对方法传入的数据进行过滤）

  如下面代码就通过filterTarget指定了当前@PreFilter是用来过滤参数ids的。

  ```java
  	@PreFilter(filterTarget="ids", value="filterObject%2==0")
   	public void delete(List<Integer> ids, List<String> usernames) {
   
        ...
   
     	}
  ```

## 用户注销

- 在配置类中添加推出映射地址

  ```java
  http.logout()
      .logoutUrl("/logout")  //退出映射地址 
      .logoutSuccessUrl("/test/hello").permitAll();  //退出后返回到哪个页面
  ```

- 注销登录的时候请求 http://xxxxxxx/logout 即可

## 自动登录

- 实现原理：

![image-20201118095439378](C:\Users\Fan\AppData\Roaming\Typora\typora-user-images\image-20201118095439378.png)

- 实现示例

  - 在JdbcTokenRepositoryImpl.class中有建表的相关SQL（会自动创建，也可手动创建）

  ```sql
  CREATE TABLE persistent_logins (
  	username VARCHAR ( 64 ) NOT NULL,
  	series VARCHAR ( 64 ) PRIMARY KEY,
  	token VARCHAR ( 64 ) NOT NULL,
  	last_used TIMESTAMP NOT NULL
  	)
  ```

  - 配置类，注入数据源，配置操作数据库对象

  ```yml
  spring:
    datasource:
      username: root
      password: 123456
      url: jdbc:mysql://localhost:3306/security?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT
      driver-class-name: com.mysql.cj.jdbc.Driver
  ```

   ```java
  /**
   * @Author Fan
   * @Date 2020/11/18
   * @Description: 配置类
   */
  @Configuration
  public class SecurityDataSource {
  
      //注入数据源
      @Autowired
      private DataSource dataSource;
  
      //配置对象
      @Bean
      public PersistentTokenRepository persistentTokenRepository(){
          JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
          jdbcTokenRepository.setDataSource(dataSource);
          //jdbcTokenRepository.setCreateTableOnStartup(true);//这个配置可以自动生成表，因为这里手动建了，所以注掉
          return jdbcTokenRepository;
      }
  
  }
   ```

  - 配置自动登录

  ```java
  .and().rememberMe().tokenRepository(securityDataSource.persistentTokenRepository())
  .tokenValiditySeconds(60) //设置有效时长 单位秒
  .userDetailsService(userDetailsService)
  ```

  - 在登陆页面中添加复选框

  ```html
  <!--这里的name只能叫remember-me，否则SpringSecurity无法识别-->
  <input type="checkbox" name="remember-me" title="记住密码"/>自动登录
  ```

## CSRF

- 概念：跨站请求攻击，简单地说，是攻击者通过一些技术手段欺骗用户的浏览器去访问一个自己曾经认证过的网站并运行一些操作（如发邮件，发消息，甚至财产操作如转账和购买商品）。由于浏览器曾经认证过，所以被访问的网站会认为是真正的用户操作而去运行。这利用了web中用户身份验证的一个漏洞:简单的身份验证只能保证请求发自某个用户的浏览器，却不能保证请求本身是用户自愿发出的。
- 需要注意的是：从Spring Security 4.0 开始，默认情况下会启用CSRF保护，以防止CSRF攻击应用程序，Spring Security CSRF 会针对PATCH，POST，PUT，DELETE方法进行防护（GET不在其中）。

# Spring Security 的微服务方案

![ ](C:\Users\Fan\AppData\Roaming\Typora\typora-user-images\image-20201118110856864.png)

## 登录（认证）

![image-20201118141445442](C:\Users\Fan\AppData\Roaming\Typora\typora-user-images\image-20201118141445442.png)

## 添加角色

## 为角色分配菜单

## 添加用户

## 为用户分配角色


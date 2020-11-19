package renjie.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renjie.utils.utils.R;
import com.renjie.utils.utils.ResponseUtil;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import renjie.entity.SecurityUser;
import renjie.entity.User;
import renjie.security.TokenManager;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @Author Fan
 * @Date 2020/11/18
 * @Description: 认证过滤器
 */
public class TokenLoginFilter extends UsernamePasswordAuthenticationFilter {

    private RedisTemplate redisTemplate;
    private TokenManager tokenManager;
    private AuthenticationManager authenticationManager;

    public TokenLoginFilter(RedisTemplate redisTemplate, TokenManager tokenManager, AuthenticationManager authenticationManager) {
        this.redisTemplate = redisTemplate;
        this.tokenManager = tokenManager;
        this.authenticationManager = authenticationManager;
        this.setPostOnly(false);
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/admin/acl/login","POST"));
    }

    //获取表单提交过来的用户名密码
    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        //获取表单提交的数据
        User user = new ObjectMapper().readValue(request.getInputStream(), User.class);
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(),user.getPassword(),new ArrayList<>()));
    }

    //认证成功调用的方法
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        //认证成功后，得到认证成功之后的用户信息
        SecurityUser user = (SecurityUser) authResult.getPrincipal();
        //根据用户名生成Token
        String token = tokenManager.createToken(user.getCurrentUserInfo().getUsername());
        //把用户名称和用户权限列表放到redis
        redisTemplate.opsForValue().set(user.getCurrentUserInfo().getUsername(),user.getPermissionValueList());
        //返回Token
        ResponseUtil.out(response, R.ok().data("token",token));


    }

    //认证失败调用的方法
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        ResponseUtil.out(response,R.error());
    }
}

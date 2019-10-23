package com.freelycar.saas.jwt;

import com.freelycar.saas.jwt.bean.JSONResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * JWT生成和验签的类
 *
 * @author tangwei - Toby
 * @date 2018-12-17
 * @email toby911115@gmail.com
 */
public class TokenAuthenticationUtil {

    private static Logger logger = LoggerFactory.getLogger(TokenAuthenticationUtil.class);

    private static final long EXPIRATION_TIME = 7776000000L;     // 4周
    //        private static final long EXPIRATIONTIME = 300_000;     // 5分钟测试用
    private static final String SECRET = "FreelyC@r";            // JWT密码
    private static final String TOKEN_PREFIX = "Bearer";        // Token前缀
    private static final String HEADER_STRING = "Authorization";// 存放Token的Header Key

    public static String generateAuthentication(String username) {
        return Jwts.builder()
                // 保存权限（角色）
                .claim("authorities", "ROLE_ADMIN,AUTH_WRITE")
                // 用户名写入标题
                .setSubject(username)
                // 有效期设置
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                // 签名设置
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    // JWT生成方法
    static void addAuthentication(HttpServletResponse response, String username) {

        // 生成JWT
        String JWT = generateAuthentication(username);

        // 将 JWT 写入 body
        try {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().println(JSONResult.fillResultString(0, "", JWT));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    // JWT验证方法
    static Authentication getAuthentication(HttpServletRequest request) {
        // 从Header中拿到token
        String token = request.getHeader(HEADER_STRING);

        if (token != null) {
            // 解析 Token
            Claims claims = Jwts.parser()
                    // 验签
                    .setSigningKey(SECRET)
                    // 去掉 Bearer
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody();

            // 拿用户名
            String user = claims.getSubject();

            // 得到 权限（角色）
            List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList((String) claims.get("authorities"));

            // 返回验证令牌
            return user != null ?
                    new UsernamePasswordAuthenticationToken(user, null, authorities) :
                    null;
        }
        return null;
    }
}

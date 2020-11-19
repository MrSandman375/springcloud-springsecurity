package renjie.security;

import com.renjie.utils.utils.MD5;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @Author Fan
 * @Date 2020/11/18
 * @Description: 密码处理
 */
@Component
public class DefaultPasswordEncoder implements PasswordEncoder {


    public DefaultPasswordEncoder() {
        this(-1);
    }

    public DefaultPasswordEncoder(int strenth) {
    }


    //进行MD5加密
    @Override
    public String encode(CharSequence charSequence) {
        return MD5.encrypt(charSequence.toString());
    }

    //进行密码的比对
    @Override
    public boolean matches(CharSequence charSequence, String encodePassword) {
        return encodePassword.equals(MD5.encrypt(charSequence.toString()));
        //return charSequence.equals(MD5.encrypt(encodePassword));
    }
}

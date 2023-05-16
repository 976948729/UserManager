package com.example.controller;

import com.example.entity.RestBean;
import com.example.service.AuthorizeService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthorizeController {

    private final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$";
//    private final String USERNAME_REGEX = "/^[a-zA-Z0-9一-龥]+$/";
    @Resource
    AuthorizeService service;

    @PostMapping("/valid-register-email")
    public RestBean<String> validateRegisterEmail(@Pattern (regexp = EMAIL_REGEX)@RequestParam("email") String email,
                                          HttpSession session){
        String s = service.sendValidateEmail(email, session.getId(), false);
        if ( s == null)
            return RestBean.success("邮件已发送，请注意查收");
        else
            return RestBean.failure(400, s);
    }

    @PostMapping("/valid-reset-email")
    public RestBean<String> validateResetEmail(@Pattern (regexp = EMAIL_REGEX)@RequestParam("email") String email,
                                          HttpSession session){
        String s = service.sendValidateEmail(email, session.getId(), true);
        if ( s == null)
            return RestBean.success("邮件已发送，请注意查收");
        else
            return RestBean.failure(400, s);
    }

    //@Pattern(regexp = USERNAME_REGEX)
    //遇到一个奇怪的bug，前端能识别符合正则表达式的user名称，放到后端就报错无法识别
    @PostMapping("/register")
    public RestBean<String> registerUser(@Length(min = 2, max = 8) @RequestParam("username") String username,
                                         @Length(min = 6, max = 16) @RequestParam("password") String password,
                                         @Pattern(regexp = EMAIL_REGEX) @RequestParam("email") String email,
                                         @Length(min = 6, max = 6) @RequestParam("code") String code,
                                         HttpSession session) {
        String s = service.validateAndRegister(username, password, email, code, session.getId());
            if ( s == null)
                return RestBean.success("注册成功");
            else
                return RestBean.failure(400, s);
    }

    /**
     * 1.发送验证邮件
     * 2.检查验证码是否正确，正确就在Session中存一个标记
     * 3.用户发起重置密码请求，如果存在标记，就成功重置
     */
    @PostMapping("/start-reset")
    public RestBean<String> startReset (@Pattern(regexp = EMAIL_REGEX) @RequestParam("email") String email,
                                        @Length(min = 6, max = 6) @RequestParam("code") String code,
                                        HttpSession session){
        String s = service.validateOnly(email, code, session.getId());
        if (s == null) {
            session.setAttribute("reset-password", email);
            return RestBean.success();
        } else {
            return RestBean.failure(400, s);
        }
    }

    @PostMapping("/do-reset")
    public RestBean<String> resetPassword(@Length(min = 6, max = 16) @RequestParam("password") String password,
                                          HttpSession session){
        String email = (String) session.getAttribute("reset-password");
        if (email == null) {
            return RestBean.failure(401, "请先完成邮箱验证");
        } else if (service.resetPassword(password, email)){
            session.removeAttribute("reset-password");
            return RestBean.success("密码重置成功");
        } else {
            return RestBean.failure(500, "内部错误，请联系管理员");
        }
    }
}

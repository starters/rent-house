package com.harry.renthouse.web.controller.user;

import com.harry.renthouse.base.ApiResponse;
import com.harry.renthouse.base.ApiResponseEnum;
import com.harry.renthouse.base.AuthenticatedUserUtil;
import com.harry.renthouse.base.UserRoleEnum;
import com.harry.renthouse.service.auth.AuthenticationService;
import com.harry.renthouse.service.auth.SmsCodeService;
import com.harry.renthouse.service.auth.UserService;
import com.harry.renthouse.service.house.QiniuService;
import com.harry.renthouse.validate.code.ValidateCodeTypeEnum;
import com.harry.renthouse.web.dto.AuthenticationDTO;
import com.harry.renthouse.web.dto.QiniuUploadResult;
import com.harry.renthouse.web.dto.UserDTO;
import com.harry.renthouse.web.form.SendSmsForm;
import com.harry.renthouse.web.form.UserBasicInfoForm;
import com.harry.renthouse.web.form.PhonePasswordLoginForm;
import com.harry.renthouse.web.form.UserPhoneRegisterForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collections;

/**
 * @author Harry Xu
 * @date 2020/5/20 16:05
 */
@RestController
@RequestMapping("user")
@Api(tags = "用户接口")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private QiniuService qiniuService;

    @Resource
    private AuthenticationService authenticationService;

    @Value("${qiniu.cdnPrefix}")
    private String cndPrefix;

    @Resource
    private SmsCodeService smsCodeService;

    @GetMapping
    @ApiOperation("获取当前用户信息")
    public ApiResponse<UserDTO> getUserInfo(){
        Long userId = AuthenticatedUserUtil.getUserId();
        UserDTO userDTO = userService.findUserById(userId);
        return ApiResponse.ofSuccess(userDTO);
    }

    @PutMapping("avatar/{path}")
    @ApiOperation("更新头像")
    public ApiResponse updateAvatar(@ApiParam(value = "图片上传的地址")  @PathVariable String path){
        String avatar = cndPrefix + path;
        userService.updateAvatar(avatar);
        return ApiResponse.ofSuccess(avatar);
    }

    @PostMapping(value = "upload/photo")
    @ApiOperation(value = "上传图片接口")
    public ApiResponse<QiniuUploadResult> uploadPhoto(@ApiParam(value = "图片文件") MultipartFile file){
        if(file == null){
            return ApiResponse.ofStatus(ApiResponseEnum.NOT_VALID_PARAM);
        }
        try {
           return ApiResponse.ofSuccess(qiniuService.uploadFile(file.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.ofStatus(ApiResponseEnum.FILE_UPLOAD_ERROR);
        }
    }

    @PutMapping("basicInfo")
    @ApiOperation("更新用户基本信息")
    public ApiResponse<UserDTO> updateUser(@RequestBody UserBasicInfoForm userForm){
        Long userId = AuthenticatedUserUtil.getUserId();
        return ApiResponse.ofSuccess(userService.updateUserInfo(userId, userForm));
    }

    @PostMapping("registryByPhone")
    @ApiOperation("通过手机号注册用户")
    public ApiResponse<UserDTO> phoneRegistry(@Validated @RequestBody UserPhoneRegisterForm userPhoneRegisterForm){
        smsCodeService.validate(userPhoneRegisterForm.getPhoneNumber(),
                userPhoneRegisterForm.getVerifyCode(),
                ValidateCodeTypeEnum.SIGN_UP.getValue());
        return ApiResponse.ofSuccess(userService.registerUserByPhone(userPhoneRegisterForm, Collections.singletonList(UserRoleEnum.ADMIN)));
    }

    @PostMapping("login")
    @ApiOperation("用户登录")
    public ApiResponse<AuthenticationDTO> login(@Validated @RequestBody PhonePasswordLoginForm form){
        AuthenticationDTO authenticationDTO = authenticationService.loginByPhone(form.getPhone(), form.getPassword());
        return ApiResponse.ofSuccess(authenticationDTO);
    }

    @PostMapping("sendSmsToPhone")
    @ApiOperation("发送短信")
    public ApiResponse sendSmsToPhone(@Validated @RequestBody SendSmsForm sendSmsForm){
        smsCodeService.sendSms(sendSmsForm);
        return ApiResponse.ofSuccess();
    }
}
package com.hefng.mynocodebackend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.hefng.mynocodebackend.annotation.AuthCheck;
import com.hefng.mynocodebackend.common.BaseResponse;
import com.hefng.mynocodebackend.common.DeleteRequest;
import com.hefng.mynocodebackend.common.ErrorCode;
import com.hefng.mynocodebackend.common.ResultUtils;
import com.hefng.mynocodebackend.constant.UserConstant;
import com.hefng.mynocodebackend.exception.BusinessException;
import com.hefng.mynocodebackend.exception.ThrowUtils;
import com.hefng.mynocodebackend.config.CosClientConfig;
import com.hefng.mynocodebackend.manager.CosManager;
import com.hefng.mynocodebackend.model.dto.user.*;
import com.hefng.mynocodebackend.model.entity.User;
import com.hefng.mynocodebackend.model.vo.LoginUserVO;
import com.hefng.mynocodebackend.model.vo.UserVO;
import com.hefng.mynocodebackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hefng.mynocodebackend.service.impl.UserServiceImpl.SALT;


/**
 * 用户 控制层。
 *
 * @author https://github.com/hefng
 * @since 2026-03-05
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String CAPTCHA_REDIS_PREFIX = "captcha:";

    // region 登录相关

    /**
     * 获取图形验证码
     *
     * @return captchaKey + base64 captchaImg
     */
    @GetMapping("/captcha")
    public BaseResponse<Map<String, String>> getCaptcha() {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 80, 4, 20);
        String captchaKey = IdUtil.simpleUUID();
        String captchaCode = lineCaptcha.getCode();
        stringRedisTemplate.opsForValue().set(CAPTCHA_REDIS_PREFIX + captchaKey, captchaCode, 5, TimeUnit.MINUTES);
        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImg", lineCaptcha.getImageBase64Data());
        return ResultUtils.success(result);
    }

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String captchaKey = userRegisterRequest.getCaptchaKey();
        String captchaCode = userRegisterRequest.getCaptchaCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, captchaKey, captchaCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, captchaKey, captchaCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotalRow());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.updateMyUser(loginUser, userUpdateMyRequest);
        return ResultUtils.success(result);
    }

    /**
     * 上传头像到 COS
     *
     * @param file    图片文件
     * @param request 当前请求
     * @return 图片访问地址
     */
    @PostMapping("/avatar/upload")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");

        String contentType = file.getContentType();
        ThrowUtils.throwIf(StringUtils.isBlank(contentType) || !StringUtils.startsWith(contentType, "image/"),
                ErrorCode.PARAMS_ERROR, "只允许上传图片");

        User loginUser = userService.getLoginUser(request);
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename);
        if (StringUtils.isBlank(extension) || extension.length() > 10) {
            extension = "png";
        }
        extension = extension.toLowerCase();

        File tempFile = null;
        try {
            // 先落到本地临时文件，再复用项目里现有的 COS 上传方式。
            tempFile = File.createTempFile("avatar-", "." + extension);
            file.transferTo(tempFile);

            String cosKey = "avatars/" + loginUser.getId() + "/" + IdUtil.simpleUUID() + "." + extension;
            ThrowUtils.throwIf(cosManager.putPictureObject(cosKey, tempFile) == null,
                    ErrorCode.OPERATION_ERROR, "头像上传失败");

            String avatarUrl = cosClientConfig.getHost() + "/" + cosKey;
            return ResultUtils.success(avatarUrl);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败：" + e.getMessage());
        } finally {
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }
}

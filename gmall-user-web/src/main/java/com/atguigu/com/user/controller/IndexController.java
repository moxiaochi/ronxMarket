package com.atguigu.com.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IndexController {

    @Reference
    UserService userService;

    @RequestMapping("index")
    public ResponseEntity<List<UserAddress>> index(){
        List<UserAddress> addressList = userService.getAddressList();

        return ResponseEntity.ok(addressList);
    }
}

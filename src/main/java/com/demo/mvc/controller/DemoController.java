package com.demo.mvc.controller;

import com.demo.framework.annotation.CAutowired;
import com.demo.framework.annotation.CController;
import com.demo.framework.annotation.CRequestMapping;
import com.demo.mvc.service.DemoService;
import com.demo.mvc.service.TestService;

/**
 * Program Name: spring-design
 * <p>
 * Description:
 * <p>
 *
 * @author zhangjianwei
 * @version 1.0
 * @date 2019/4/24 11:47 AM
 */
@CController
@CRequestMapping("/democontroller")
public class DemoController {
    @CAutowired
    private DemoService demoService;

    @CAutowired
    private TestService testService;

    @CRequestMapping("test")
    public String test() {

        System.out.println("调用controller中的方法");

        return null;

    }

}

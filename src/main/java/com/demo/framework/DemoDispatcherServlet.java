package com.demo.framework;

import com.demo.framework.annotation.CAutowired;
import com.demo.framework.annotation.CController;
import com.demo.framework.annotation.CRequestMapping;
import com.demo.framework.annotation.CService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Program Name: spring-design
 * <p>
 * Description: servlet tomcat启动后会执行servlet的实现类。
 * <p>
 *
 * @author zhangjianwei
 * @version 1.0
 * @date 2019/4/24 10:14 AM
 */
public class DemoDispatcherServlet extends HttpServlet {

    private List<String> beanDefinationList = new ArrayList<String>();

    private Map<String, Object> beanContainer = new HashMap<String, Object>();

    private Map<String, Handler> handlerMapping = new HashMap<String, Handler>();

    @Override

    public void init(ServletConfig config) throws ServletException {
        System.out.println(config);
        // spring的实现基本可以分为两部分。1 容器启动，2对象注册与依赖关系的绑定

        // IOC
        // 1、IOC容器启动阶段，读取元数据，加载bean定义
        // 2、对象的注册与绑定
        iocInit(config);

        // 3、web容器加的启动、handler、mapping等

        webContainerInit();

        super.init(config);

    }

    /**
     * 包扫描，扫描可实例化的类
     * @param packageName
     */
    private void componentScan(String packageName) {
        URL resourceUrl = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(resourceUrl.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                componentScan(packageName + "." + file.getName());
            } else {
                // 如果是文件
                String className = packageName + "." + file.getName().replace(".class", "");
                beanDefinationList.add(className);
            }
        }
    }

    /**
     * 加载bean对象
     */
    private void registBean() {
        // 将扫描到的类实例化
        for (String className : beanDefinationList) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(CController.class)) {
                    String simpleName = clazz.getSimpleName();
                    beanContainer.put(simpleName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(CService.class)) {
                    CService annotation = clazz.getAnnotation(CService.class);
                    String serviceName = annotation.value();

                    // 全部按class名称做为bean的名称
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        beanContainer.put(i.getName(), clazz.newInstance());
                    }

                    // if ("".equals(serviceName)) {
                    //     // beanContainer.put(clazz.getSimpleName(), clazz.newInstance());
                    // } else {
                    //     beanContainer.put(serviceName, clazz.newInstance());
                    // }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ioc容器启动，bean加载，对象依赖注入等
     *
     * @param config
     */
    private void iocInit(ServletConfig config) {

        // 元数据加载
        String rootPackage = config.getInitParameter("rootPackage");
        this.componentScan(rootPackage);

        System.out.println(">>>>>>>打印bean信息");
        System.out.println(beanDefinationList);

        // 实例化bean
        this.registBean();
        // 依赖注入
        this.dependencyInject();

        System.out.println("依赖注入完成");
    }

    /**
     * 依赖注入
     */
    private void dependencyInject() {
        for (Map.Entry<String, Object> entry : beanContainer.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
            Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(CAutowired.class)) {
                    // CAutowired annotation = field.getAnnotation(CAutowired.class);
                    // String value = annotation.value();

                    // 目前是根据类的类型注入，未实现根据bean的ID注入。
                    String className = field.getType().getName();
                    field.setAccessible(true);
                    try {
                        field.set(entry.getValue(), beanContainer.get(className));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * web 容器启动
     * 扫描requestMapping，映射类和方法组成的url存储到handler容器中
     */
    private void webContainerInit() {
        for (Map.Entry<String, Object> item : beanContainer.entrySet()) {
            Class<?> aClass = item.getValue().getClass();
            StringBuilder stringBuilder = new StringBuilder();
            if (aClass.isAnnotationPresent(CRequestMapping.class)) {
                CRequestMapping annotation = aClass.getAnnotation(CRequestMapping.class);
                String urlOnClass = annotation.value();
                Method[] methods = aClass.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(CRequestMapping.class)) {
                        String urlOnMehod = method.getAnnotation(CRequestMapping.class).value();
                        stringBuilder.append("/").append(urlOnClass).append("/").append(urlOnMehod);
                        String string = new String(stringBuilder).replaceAll("/+", "/");
                        Handler handler = new Handler();
                        handler.method = method;
                        handler.controller = item.getValue();

                        // try {
                        //     // handler.controller = item.getValue().getClass().newInstance();
                        //
                        // } catch (InstantiationException e) {
                        //     e.printStackTrace();
                        // } catch (IllegalAccessException e) {
                        //     e.printStackTrace();
                        // }
                        handlerMapping.put(string, handler);
                    }
                }
            }
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 根据url invoke 相应的方法
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.replace(contextPath, "").replaceAll("/+", "/").replace(".json", "");
        for (Map.Entry<String, Handler> entry : handlerMapping.entrySet()) {
            String key = entry.getKey();
            if (key.equals(url)) {
                Method method = entry.getValue().method;
                if (null != method) {
                    Map<String, String[]> params = req.getParameterMap();
                    try {
                        method.invoke(entry.getValue().controller, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    /**
     * 封装一个请求处理的handler
     */
    private class Handler{

        protected Object controller;
        protected Method method;

        protected Handler(){
            this.controller = controller;
            this.method = method;
        }

    }

}

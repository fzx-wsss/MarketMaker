package com.wsss.market.maker.center;

import com.wsss.market.maker.config.BiAnConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class BootStrap implements ApplicationListener<ContextRefreshedEvent>, InitializingBean, DisposableBean, ApplicationContextAware {
    private static boolean start = false;
    @Resource
    private BiAnConfig biAnConfig;
    @Resource
    private DataCenter dataCenter;
    private static ApplicationContext applicationContext;
    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ConfigCenter.setApolloConfig(biAnConfig);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if(start) {
            return;
        }
        log.info("启动应用");
        start = true;
        dataCenter.register("xrpusdt");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static <T> T createSpringBean(Class<T> clazz, Object... args) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
//        if(!beanFactory.containsBeanDefinition(clazz.getSimpleName())) {
//            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
//                    .genericBeanDefinition(clazz);
//            BeanDefinition beanDefinition =  beanDefinitionBuilder.getBeanDefinition();
//            beanDefinition.setScope("prototype");
//            beanFactory.registerBeanDefinition(clazz.getSimpleName(), beanDefinition);
//        }
        if(args == null) {
            return beanFactory.getBean(clazz);
        }
        return beanFactory.getBean(clazz,args);
    }
}

package com.wsss.market.maker.model.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static <T> T getSpringBean(Class<T> clazz, Object... args) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
//        if(!beanFactory.containsBeanDefinition(clazz.getSimpleName())) {
//            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
//                    .genericBeanDefinition(clazz);
//            BeanDefinition beanDefinition =  beanDefinitionBuilder.getBeanDefinition();
//            beanDefinition.setScope("prototype");
//            beanFactory.registerBeanDefinition(clazz.getSimpleName(), beanDefinition);
//        }
        if (args == null) {
            return beanFactory.getBean(clazz);
        }
        return beanFactory.getBean(clazz, args);
    }

}

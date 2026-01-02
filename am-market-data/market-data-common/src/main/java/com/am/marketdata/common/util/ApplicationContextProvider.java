package com.am.marketdata.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility class to access Spring beans from non-Spring managed classes
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextProvider.applicationContext = applicationContext;
    }

    /**
     * Get a bean by class
     * 
     * @param <T> The bean type
     * @param beanClass The bean class
     * @return The bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext has not been set");
        }
        return applicationContext.getBean(beanClass);
    }

    /**
     * Get a bean by name and class
     * 
     * @param <T> The bean type
     * @param name The bean name
     * @param beanClass The bean class
     * @return The bean instance
     */
    public static <T> T getBean(String name, Class<T> beanClass) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext has not been set");
        }
        return applicationContext.getBean(name, beanClass);
    }
}

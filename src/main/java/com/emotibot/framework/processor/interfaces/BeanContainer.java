package com.emotibot.framework.processor.interfaces;

import com.emotibot.framework.processor.exception.BeanOperationNotSupportException;
import com.google.common.collect.Maps;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

public interface BeanContainer {

    //通过name获取 Bean.
    Object getBean(String name);

    //通过class获取Bean.
    <T> T getBean(Class<T> clazz);

    <T> T getBean(String name, Class<T> clazz);

    void registerBean(String name, Class<?> clazz, Object bean);

    @Slf4j
    final class DefaultBeanContainer implements BeanContainer {

        private Map<Class, Map<String, Object>> beanHolder = Maps.newHashMap();

        @Override
        public Object getBean(String name) {
            throw new BeanOperationNotSupportException();
        }

        @Override
        public <T> T getBean(Class<T> clazz) {
            return getBean(null, clazz);
        }

        @Override
        public <T> T getBean(String name, Class<T> clazz) {
            if (beanHolder.containsKey(clazz)) {
                String n = StringUtils.isNotEmpty(name) ? name : clazz.getSimpleName();
                return (T) beanHolder.get(clazz).get(n);
            }
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return null;
        }

        @Override
        public void registerBean(String name, Class<?> clazz, Object bean) {
            Map<String, Object> holder = beanHolder.computeIfAbsent(clazz, x -> Maps.newHashMap());
            if (StringUtils.isEmpty(name)) {
                holder.put(clazz.getSimpleName(), bean);
            } else {
                holder.put(name, bean);
            }
        }
    }
}

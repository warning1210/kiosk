package com.kiosk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import com.kiosk.global.config.MyBatisConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class MyBatisStatementRegistrationTest {

    @Test
    void everyMapperMethodIsRegistered() throws Exception {
        SqlSessionFactory factory = new MyBatisConfig().sqlSessionFactory(mock(javax.sql.DataSource.class));
        Configuration configuration = factory.getConfiguration();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Mapper.class));
        for (var bean : scanner.findCandidateComponents("com.kiosk")) {
            Class<?> mapperType = Class.forName(bean.getBeanClassName());
            if (!configuration.hasMapper(mapperType)) {
                configuration.addMapper(mapperType);
            }
            for (Method method : mapperType.getMethods()) {
                if (method.isDefault() || Modifier.isStatic(method.getModifiers()) || hasSqlAnnotation(method)) {
                    continue;
                }
                assertThat(configuration.hasStatement(mapperType.getName() + "." + method.getName()))
                        .as("MyBatis statement for %s#%s", mapperType.getName(), method.getName())
                        .isTrue();
            }
        }

        assertThat(configuration.hasStatement(
                "com.kiosk.domain.order.OrderRepository.findByOrderStatusInAndCreatedAtBefore"))
                .isTrue();
    }

    private boolean hasSqlAnnotation(Method method) {
        return List.of(Select.class, Insert.class, Update.class, Delete.class,
                        SelectProvider.class, InsertProvider.class, UpdateProvider.class, DeleteProvider.class)
                .stream()
                .anyMatch(method::isAnnotationPresent);
    }
}

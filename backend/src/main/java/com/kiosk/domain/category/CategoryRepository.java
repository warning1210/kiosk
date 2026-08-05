package com.kiosk.domain.category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CategoryRepository {
    String COLUMNS="category_id, category_name, display_order, is_visible, created_at, updated_at";
    @Select("SELECT "+COLUMNS+" FROM category WHERE category_id=#{id}") Optional<Category> findById(Long id);
    @Select("SELECT "+COLUMNS+" FROM category ORDER BY category_name ASC") List<Category> findAllByOrderByCategoryNameAsc();
    @Select("SELECT "+COLUMNS+" FROM category ORDER BY category_id") List<Category> findAll();
    @Insert("INSERT INTO category(category_name,display_order,is_visible,created_at,updated_at) VALUES(#{categoryName},#{displayOrder},#{isVisible},#{createdAt},#{updatedAt})") @Options(useGeneratedKeys=true,keyProperty="categoryId",keyColumn="category_id") int insert(Category value);
    @Update("UPDATE category SET category_name=#{categoryName},display_order=#{displayOrder},is_visible=#{isVisible},updated_at=#{updatedAt} WHERE category_id=#{categoryId}") int update(Category value);
    default Category save(Category value){LocalDateTime now=LocalDateTime.now();value.setUpdatedAt(now);if(value.getCategoryId()==null){value.setCreatedAt(now);insert(value);}else update(value);return value;}
}

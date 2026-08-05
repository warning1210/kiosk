package com.kiosk.domain.customer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerRepository {
    String COLUMNS = "customer_id, mobile_number, point_balance, grade, created_at, updated_at";
    @Select("SELECT " + COLUMNS + " FROM customer WHERE customer_id=#{id}") Optional<Customer> findById(Long id);
    @Select("SELECT " + COLUMNS + " FROM customer WHERE mobile_number=#{mobileNumber}") Optional<Customer> findByMobileNumber(String mobileNumber);
    @Select("SELECT " + COLUMNS + " FROM customer WHERE grade=#{grade} ORDER BY customer_id") List<Customer> findByGrade(CustomerGrade grade);
    @Select("SELECT " + COLUMNS + " FROM customer ORDER BY customer_id") List<Customer> findAll();
    @Insert("INSERT INTO customer (mobile_number,point_balance,grade,created_at,updated_at) VALUES (#{mobileNumber},#{pointBalance},#{grade},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys=true,keyProperty="customerId",keyColumn="customer_id") int insert(Customer customer);
    @Update("UPDATE customer SET mobile_number=#{mobileNumber},point_balance=#{pointBalance},grade=#{grade},updated_at=#{updatedAt} WHERE customer_id=#{customerId}") int update(Customer customer);
    default Customer save(Customer customer) { LocalDateTime now=LocalDateTime.now(); customer.setUpdatedAt(now); if(customer.getCustomerId()==null){customer.setCreatedAt(now);insert(customer);}else update(customer); return customer; }
}

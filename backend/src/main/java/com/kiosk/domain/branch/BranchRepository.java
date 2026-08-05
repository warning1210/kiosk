package com.kiosk.domain.branch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BranchRepository {
    String COLUMNS = "branch_id, branch_name, region, address, phone, email, manager_name, operation_status, opening_date, is_busy, estimated_wait_minutes, kiosk_code, kiosk_status, kiosk_last_access_at, created_at, updated_at";

    @Select("SELECT " + COLUMNS + " FROM branch WHERE branch_id=#{id}")
    Optional<Branch> findById(Long id);

    @Select("SELECT " + COLUMNS + " FROM branch ORDER BY branch_id")
    List<Branch> findAll();

    @Insert("INSERT INTO branch (branch_name,region,address,phone,email,manager_name,operation_status,opening_date,is_busy,estimated_wait_minutes,kiosk_code,kiosk_status,kiosk_last_access_at,created_at,updated_at) VALUES (#{branchName},#{region},#{address},#{phone},#{email},#{managerName},#{operationStatus},#{openingDate},#{isBusy},#{estimatedWaitMinutes},#{kioskCode},#{kioskStatus},#{kioskLastAccessAt},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "branchId", keyColumn = "branch_id")
    int insert(Branch branch);

    @Update("UPDATE branch SET branch_name=#{branchName},region=#{region},address=#{address},phone=#{phone},email=#{email},manager_name=#{managerName},operation_status=#{operationStatus},opening_date=#{openingDate},is_busy=#{isBusy},estimated_wait_minutes=#{estimatedWaitMinutes},kiosk_code=#{kioskCode},kiosk_status=#{kioskStatus},kiosk_last_access_at=#{kioskLastAccessAt},updated_at=#{updatedAt} WHERE branch_id=#{branchId}")
    int update(Branch branch);

    @Delete("DELETE FROM branch WHERE branch_id=#{branchId}")
    int deleteById(Long branchId);

    default Branch save(Branch branch) {
        LocalDateTime now = LocalDateTime.now();
        branch.setUpdatedAt(now);
        if (branch.getBranchId() == null) { branch.setCreatedAt(now); insert(branch); }
        else update(branch);
        return branch;
    }

    default void delete(Branch branch) { if (branch != null && branch.getBranchId() != null) deleteById(branch.getBranchId()); }
}

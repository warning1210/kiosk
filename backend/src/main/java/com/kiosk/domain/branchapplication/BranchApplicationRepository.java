package com.kiosk.domain.branchapplication;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BranchApplicationRepository {
    String SELECT="SELECT branch_application_id,branch_name,manager_name,phone,email,address,business_number,invite_token,invite_expires_at,issued_by_admin_id,approval_status,rejection_reason,processed_admin_id,processed_at,approved_branch_id,applied_at,created_at,updated_at FROM branch_application ";
    @Results(id="applicationMap",value={
      @Result(column="branch_application_id",property="branchApplicationId",id=true),
      @Result(column="issued_by_admin_id",property="issuedByAdmin",javaType=Admin.class,one=@One(select="com.kiosk.domain.admin.AdminRepository.findById")),
      @Result(column="processed_admin_id",property="processedAdmin",javaType=Admin.class,one=@One(select="com.kiosk.domain.admin.AdminRepository.findById")),
      @Result(column="approved_branch_id",property="approvedBranch",javaType=Branch.class,one=@One(select="com.kiosk.domain.branch.BranchRepository.findById"))})
    @Select(SELECT+"WHERE branch_application_id=#{id}") Optional<BranchApplication> findById(Long id);
    @ResultMap("applicationMap") @Select(SELECT+"ORDER BY applied_at DESC") List<BranchApplication> findAllByOrderByAppliedAtDesc();
    @ResultMap("applicationMap") @Select(SELECT+"WHERE invite_token=#{inviteToken}") Optional<BranchApplication> findByInviteToken(String inviteToken);
    @Select({"<script>SELECT EXISTS(SELECT 1 FROM branch_application WHERE email=#{email} AND approval_status IN ","<foreach item='s' collection='statuses' open='(' separator=',' close=')'>#{s}</foreach>)</script>"}) boolean existsByEmailAndApprovalStatusIn(@Param("email") String email,@Param("statuses") List<ApprovalStatus> statuses);
    @ResultMap("applicationMap") @Select(SELECT+"WHERE email=#{email} ORDER BY created_at DESC LIMIT 1") Optional<BranchApplication> findFirstByEmailOrderByCreatedAtDesc(String email);
    @Insert({"<script>INSERT INTO branch_application(branch_name,manager_name,phone,email,address,business_number,invite_token,invite_expires_at,issued_by_admin_id,approval_status,rejection_reason,processed_admin_id,processed_at,approved_branch_id,applied_at,created_at,updated_at) VALUES(#{branchName},#{managerName},#{phone},#{email},#{address},#{businessNumber},#{inviteToken},#{inviteExpiresAt},<choose><when test='issuedByAdmin != null'>#{issuedByAdmin.adminId}</when><otherwise>NULL</otherwise></choose>,#{approvalStatus},#{rejectionReason},<choose><when test='processedAdmin != null'>#{processedAdmin.adminId}</when><otherwise>NULL</otherwise></choose>,#{processedAt},<choose><when test='approvedBranch != null'>#{approvedBranch.branchId}</when><otherwise>NULL</otherwise></choose>,#{appliedAt},#{createdAt},#{updatedAt})</script>"}) @Options(useGeneratedKeys=true,keyProperty="branchApplicationId",keyColumn="branch_application_id") int insert(BranchApplication value);
    @Update({"<script>UPDATE branch_application SET branch_name=#{branchName},manager_name=#{managerName},phone=#{phone},email=#{email},address=#{address},business_number=#{businessNumber},invite_token=#{inviteToken},invite_expires_at=#{inviteExpiresAt},issued_by_admin_id=<choose><when test='issuedByAdmin != null'>#{issuedByAdmin.adminId}</when><otherwise>NULL</otherwise></choose>,approval_status=#{approvalStatus},rejection_reason=#{rejectionReason},processed_admin_id=<choose><when test='processedAdmin != null'>#{processedAdmin.adminId}</when><otherwise>NULL</otherwise></choose>,processed_at=#{processedAt},approved_branch_id=<choose><when test='approvedBranch != null'>#{approvedBranch.branchId}</when><otherwise>NULL</otherwise></choose>,applied_at=#{appliedAt},updated_at=#{updatedAt} WHERE branch_application_id=#{branchApplicationId}</script>"}) int update(BranchApplication value);
    @Delete("DELETE FROM branch_application WHERE branch_application_id=#{id}") int deleteById(Long id);
    default BranchApplication save(BranchApplication value){LocalDateTime now=LocalDateTime.now();value.setUpdatedAt(now);if(value.getAppliedAt()==null)value.setAppliedAt(now);if(value.getBranchApplicationId()==null){value.setCreatedAt(now);insert(value);}else update(value);return value;}
    default void delete(BranchApplication value){if(value!=null&&value.getBranchApplicationId()!=null)deleteById(value.getBranchApplicationId());}
}

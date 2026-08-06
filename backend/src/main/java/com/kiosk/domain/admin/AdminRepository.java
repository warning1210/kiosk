package com.kiosk.domain.admin;

import com.kiosk.domain.branch.Branch;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminRepository {
    String SELECT = "SELECT a.admin_id,a.branch_id,a.login_id,a.password_hash,a.name,a.phone,a.email,a.role,a.account_status,a.invite_token,a.invite_expires_at,a.inviter_admin_id,a.last_login_at,a.created_at,a.updated_at FROM admin a ";

    @Results(id="adminMap", value={
        @Result(column="admin_id",property="adminId",id=true),
        @Result(column="login_id",property="loginId"),
        @Result(column="password_hash",property="passwordHash"),
        @Result(column="name",property="name"),
        @Result(column="phone",property="phone"),
        @Result(column="email",property="email"),
        @Result(column="role",property="role"),
        @Result(column="account_status",property="accountStatus"),
        @Result(column="invite_token",property="inviteToken"),
        @Result(column="invite_expires_at",property="inviteExpiresAt"),
        @Result(column="last_login_at",property="lastLoginAt"),
        @Result(column="created_at",property="createdAt"),
        @Result(column="updated_at",property="updatedAt"),
        @Result(column="branch_id",property="branch",javaType=Branch.class,one=@One(select="com.kiosk.domain.branch.BranchRepository.findById")),
        @Result(column="inviter_admin_id",property="inviterAdmin",javaType=Admin.class,one=@One(select="findById"))
    })
    @Select(SELECT+"WHERE a.admin_id=#{id}") Optional<Admin> findById(Long id);
    @ResultMap("adminMap") @Select(SELECT+"WHERE a.login_id=#{loginId}") Optional<Admin> findByLoginId(String loginId);
    @Select("SELECT EXISTS(SELECT 1 FROM admin WHERE login_id=#{loginId})") boolean existsByLoginId(String loginId);
    @ResultMap("adminMap") @Select(SELECT+"WHERE a.email=#{email} ORDER BY a.admin_id LIMIT 1") Optional<Admin> findByEmail(String email);
    @ResultMap("adminMap") @Select(SELECT+"WHERE a.branch_id=#{branchId} ORDER BY a.admin_id LIMIT 1") Optional<Admin> findFirstByBranch_BranchIdOrderByAdminIdAsc(Long branchId);
    @ResultMap("adminMap") @Select(SELECT+"ORDER BY a.admin_id LIMIT 1") Optional<Admin> findFirstByOrderByAdminIdAsc();
    @ResultMap("adminMap") @Select(SELECT+"WHERE a.role=#{role} ORDER BY a.admin_id") List<Admin> findByRoleOrderByAdminIdAsc(AdminRole role);
    @ResultMap("adminMap") @Select(SELECT+"ORDER BY a.admin_id") List<Admin> findAll();

    @Insert({"<script>INSERT INTO admin(branch_id,login_id,password_hash,name,phone,email,role,account_status,invite_token,invite_expires_at,inviter_admin_id,last_login_at,created_at,updated_at) VALUES(<choose><when test='branch != null'>#{branch.branchId}</when><otherwise>NULL</otherwise></choose>,#{loginId},#{passwordHash},#{name},#{phone},#{email},#{role},#{accountStatus},#{inviteToken},#{inviteExpiresAt},<choose><when test='inviterAdmin != null'>#{inviterAdmin.adminId}</when><otherwise>NULL</otherwise></choose>,#{lastLoginAt},#{createdAt},#{updatedAt})</script>"})
    @Options(useGeneratedKeys=true,keyProperty="adminId",keyColumn="admin_id") int insert(Admin value);
    @Update({"<script>UPDATE admin SET branch_id=<choose><when test='branch != null'>#{branch.branchId}</when><otherwise>NULL</otherwise></choose>,login_id=#{loginId},password_hash=#{passwordHash},name=#{name},phone=#{phone},email=#{email},role=#{role},account_status=#{accountStatus},invite_token=#{inviteToken},invite_expires_at=#{inviteExpiresAt},inviter_admin_id=<choose><when test='inviterAdmin != null'>#{inviterAdmin.adminId}</when><otherwise>NULL</otherwise></choose>,last_login_at=#{lastLoginAt},updated_at=#{updatedAt} WHERE admin_id=#{adminId}</script>"}) int update(Admin value);
    @Delete("DELETE FROM admin WHERE admin_id=#{adminId}") int deleteById(Long adminId);
    default Admin save(Admin value){LocalDateTime now=LocalDateTime.now();value.setUpdatedAt(now);if(value.getAdminId()==null){value.setCreatedAt(now);insert(value);}else update(value);return value;}
    default void delete(Admin value){if(value!=null&&value.getAdminId()!=null)deleteById(value.getAdminId());}
}

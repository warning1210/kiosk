package com.kiosk.domain.auditlog;

import com.kiosk.domain.admin.Admin;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuditLogRepository {
    String SELECT="SELECT audit_log_id,entity_type,entity_id,admin_id,action,field_name,old_value,new_value,reason,changed_at FROM audit_log ";
    @Results(id="auditMap",value={@Result(column="audit_log_id",property="auditLogId",id=true),@Result(column="admin_id",property="admin",javaType=Admin.class,one=@One(select="com.kiosk.domain.admin.AdminRepository.findById"))})
    @Select(SELECT+"WHERE audit_log_id=#{id}") Optional<AuditLog> findById(Long id);
    @ResultMap("auditMap") @Select(SELECT+"ORDER BY audit_log_id") List<AuditLog> findAll();
    @Insert({"<script>INSERT INTO audit_log(entity_type,entity_id,admin_id,action,field_name,old_value,new_value,reason,changed_at) VALUES(#{entityType},#{entityId},<choose><when test='admin != null'>#{admin.adminId}</when><otherwise>NULL</otherwise></choose>,#{action},#{fieldName},#{oldValue},#{newValue},#{reason},#{changedAt})</script>"}) @Options(useGeneratedKeys=true,keyProperty="auditLogId",keyColumn="audit_log_id") int insert(AuditLog value);
    default AuditLog save(AuditLog value){if(value.getChangedAt()==null)value.setChangedAt(LocalDateTime.now());insert(value);return value;}
}

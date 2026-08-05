package com.kiosk.hq.branch.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HqBranchAccountMapper {
    @Delete("DELETE cm FROM chat_message cm JOIN chat_room cr ON cm.chat_room_id=cr.chat_room_id WHERE cr.branch_id=#{branchId}") int deleteBranchChatMessages(Long branchId);
    @Delete("DELETE FROM chat_message WHERE sender_admin_id=#{adminId}") int deleteAdminChatMessages(Long adminId);
    @Delete("DELETE FROM chat_room WHERE branch_id=#{branchId}") int deleteChatRooms(Long branchId);
    @Delete("DELETE FROM inventory_transaction WHERE branch_id=#{branchId}") int deleteInventoryTransactions(Long branchId);
    @Delete("DELETE sri FROM stock_request_item sri JOIN stock_request sr ON sri.stock_request_id=sr.stock_request_id WHERE sr.branch_id=#{branchId}") int deleteStockRequestItems(Long branchId);
    @Delete("DELETE FROM stock_request WHERE branch_id=#{branchId}") int deleteStockRequests(Long branchId);
    @Update("UPDATE coupon c JOIN `order` o ON c.used_order_id=o.order_id SET c.used_order_id=NULL WHERE o.branch_id=#{branchId}") int clearCouponOrders(Long branchId);
    @Delete("DELETE p FROM payment p JOIN `order` o ON p.order_id=o.order_id WHERE o.branch_id=#{branchId}") int deletePayments(Long branchId);
    @Delete("DELETE oif FROM order_item_flavor oif JOIN order_item oi ON oif.order_item_id=oi.order_item_id JOIN `order` o ON oi.order_id=o.order_id WHERE o.branch_id=#{branchId}") int deleteOrderItemFlavors(Long branchId);
    @Delete("DELETE oi FROM order_item oi JOIN `order` o ON oi.order_id=o.order_id WHERE o.branch_id=#{branchId}") int deleteOrderItems(Long branchId);
    @Delete("DELETE FROM `order` WHERE branch_id=#{branchId}") int deleteOrders(Long branchId);
    @Delete("DELETE FROM branch_inventory WHERE branch_id=#{branchId}") int deleteBranchInventory(Long branchId);
    @Delete("DELETE FROM branch_product WHERE branch_id=#{branchId}") int deleteBranchProducts(Long branchId);
    @Delete("DELETE FROM event_branch_flavor WHERE branch_id=#{branchId}") int deleteEventBranchFlavors(Long branchId);
    @Delete("DELETE FROM audit_log WHERE admin_id=#{adminId}") int deleteAuditLogs(Long adminId);
    @Update("UPDATE branch_application SET issued_by_admin_id=NULL WHERE issued_by_admin_id=#{adminId}") int clearIssuedByAdmin(Long adminId);
    @Update("UPDATE branch_application SET processed_admin_id=NULL WHERE processed_admin_id=#{adminId}") int clearApplicationProcessedAdmin(Long adminId);
    @Update("UPDATE stock_request SET processed_admin_id=NULL WHERE processed_admin_id=#{adminId}") int clearStockRequestProcessedAdmin(Long adminId);
    @Update("UPDATE stock_request SET receipt_confirmed_admin_id=NULL WHERE receipt_confirmed_admin_id=#{adminId}") int clearReceiptConfirmedAdmin(Long adminId);
    @Update("UPDATE inventory_transaction SET processed_admin_id=NULL WHERE processed_admin_id=#{adminId}") int clearInventoryProcessedAdmin(Long adminId);
    @Update("UPDATE chat_room SET assigned_admin_id=NULL WHERE assigned_admin_id=#{adminId}") int clearAssignedAdmin(Long adminId);
    @Update("UPDATE admin SET inviter_admin_id=NULL WHERE inviter_admin_id=#{adminId}") int clearInviterAdmin(Long adminId);
    @Delete("DELETE FROM branch_application WHERE approved_branch_id=#{branchId}") int deleteBranchApplications(Long branchId);
    @Delete("DELETE FROM admin WHERE admin_id=#{adminId}") int deleteAdmin(Long adminId);
    @Delete("DELETE FROM branch WHERE branch_id=#{branchId}") int deleteBranch(Long branchId);
}

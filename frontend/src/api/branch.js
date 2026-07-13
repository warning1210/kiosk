import axios from 'axios';

const api = axios.create({
    baseURL: '/api/branch',
    headers: {
        'Content-Type': 'application/json'
    }
});

// 1. 대시보드 요약 데이터 가져오기
export const fetchDashboardSummary = async (branchId) => {
    const response = await api.get(`/${branchId}/dashboard/summary`);
    return response.data;
};

// 2. 현재 진행/대기 중인 주문 목록 가져오기
export const fetchBranchOrders = async (branchId) => {
    const response = await api.get(`/${branchId}/orders`);
    return response.data;
};

// 3. 주문 상태 변경하기 (완료/취소, 취소 시 사유 포함)
export const updateOrderStatus = async (branchId, orderId, status, cancelReason) => {
    const response = await api.patch(`/${branchId}/orders/${orderId}/status`, {
        status: status,
        cancelReason: cancelReason
    });
    return response.data;
};

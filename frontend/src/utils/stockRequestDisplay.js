/**
 * 여러 신청 상품을 표와 카드 한 줄에 들어갈 짧은 문장으로 만든다.
 * 예: 바닐라 한 종류면 "바닐라", 세 종류면 "바닐라 외 2종"으로 표시한다.
 */
export function summarizeStockRequestItems(request) {
  // 빈 배열을 그대로 읽으면 첫 번째 원소가 없어 오류가 나므로 먼저 처리한다.
  if (request.items.length === 0) return '-'

  const firstFlavorName = request.items[0].flavorName
  const otherItemCount = request.items.length - 1

  // 한 종류뿐인 경우 불필요한 "외 0종" 문구를 붙이지 않는다.
  if (otherItemCount === 0) return firstFlavorName
  return `${firstFlavorName} 외 ${otherItemCount}종`
}

/**
 * 신청에 포함된 전체 상품 수량을 합산한다.
 * 본점 승인 후에는 승인 수량을 보여 주고, 아직 승인 전이면 원래 신청 수량을 보여 준다.
 */
export function calculateStockRequestQuantity(request) {
  return request.items.reduce((total, item) => {
    // ?? 연산자는 approvedQuantity가 null/undefined일 때만 requestedQuantity를 대신 사용한다.
    // 따라서 승인 수량이 0인 경우에도 임의로 신청 수량으로 바뀌지 않는다.
    const quantity = item.approvedQuantity ?? item.requestedQuantity
    return total + quantity
  }, 0)
}

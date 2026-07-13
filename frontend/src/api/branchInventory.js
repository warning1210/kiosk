import http from './http'

export function fetchBranchInventory({ categoryId, keyword } = {}) {
  return http
    .get('/branch/inventory', { params: { categoryId, keyword } })
    .then((res) => res.data.data)
}

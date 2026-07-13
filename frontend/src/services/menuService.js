import http from '../api/http'
import { DUMMY_CATEGORIES, DUMMY_PRODUCTS } from '../data/menuDummy'

// 카테고리/상품은 아직 더미 데이터. 실제 DB 연동 시 이 두 함수 내부만
// 아래 주석 처리된 axios 호출로 바꾸면 되고, 호출하는 쪽(OrderView 등)은 수정할 필요 없다.
export async function fetchCategories() {
  // return DUMMY_CATEGORIES
  return (await http.get('/categories')).data
}

export async function fetchProducts() {
  // return DUMMY_PRODUCTS/
  return (await http.get('/products')).data
}

// 맛은 이미 실제 DB(GET /api/flavors)와 연동되어 있어 그대로 둔다.
export async function fetchFlavors() {
  return (await http.get('/flavors')).data
}

import http from '../api/http'
import { DUMMY_CATEGORIES, DUMMY_PRODUCTS } from '../data/menuDummy'

export async function fetchCategories() {
  return (await http.get('/categories')).data
}

export async function fetchProducts(branchId) {
  return (await http.get('/products', { params: { branchId } })).data
}

export async function fetchFlavors(branchId) {
  return (await http.get('/flavors', { params: { branchId } })).data
}

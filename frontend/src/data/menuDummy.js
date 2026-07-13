// 카테고리/상품 더미 데이터.
// 필드명은 백엔드 실제 API(GET /api/products, /api/flavors) 응답 구조와 동일하게 맞춰뒀다.
// 추후 DB 연동 시 services/menuService.js의 fetchCategories/fetchProducts 내부만
// 실제 axios 호출로 바꾸면, 이 파일과 화면 코드는 그대로 재사용 가능하다.

export const DUMMY_CATEGORIES = [
  { categoryId: 1, categoryName: '아이스크림', hasSizeTiers: true },
  { categoryId: 2, categoryName: '커피', hasSizeTiers: false }
]

export const DUMMY_PRODUCTS = [
  // 아이스크림 - 레귤러 사이즈
  {
    productId: 101,
    categoryId: 1,
    productName: '싱글레귤러',
    basePrice: 4200,
    requiresFlavorSelection: true,
    selectableFlavorCount: 1,
    containerPolicy: 'CUP_OR_CONE',
    isLarge: false,
    isNew: false
  },
  {
    productId: 102,
    categoryId: 1,
    productName: '싱글킹',
    basePrice: 4700,
    requiresFlavorSelection: true,
    selectableFlavorCount: 1,
    containerPolicy: 'CUP_OR_CONE',
    isLarge: false,
    isNew: false
  },
  {
    productId: 103,
    categoryId: 1,
    productName: '더블주니어',
    basePrice: 6500,
    requiresFlavorSelection: true,
    selectableFlavorCount: 2,
    containerPolicy: 'CUP_OR_CONE',
    isLarge: false,
    isNew: false
  },
  {
    productId: 104,
    categoryId: 1,
    productName: '더블레귤러',
    basePrice: 7500,
    requiresFlavorSelection: true,
    selectableFlavorCount: 2,
    containerPolicy: 'CUP_OR_CONE',
    isLarge: false,
    isNew: false
  },
  {
    productId: 105,
    categoryId: 1,
    productName: '파인트',
    basePrice: 12000,
    requiresFlavorSelection: true,
    selectableFlavorCount: 3,
    containerPolicy: 'CUP_ONLY',
    isLarge: false,
    isNew: false
  },
  {
    productId: 106,
    categoryId: 1,
    productName: '쿼터',
    basePrice: 21000,
    requiresFlavorSelection: true,
    selectableFlavorCount: 4,
    containerPolicy: 'CUP_ONLY',
    isLarge: false,
    isNew: false
  },
  // 아이스크림 - 대용량
  {
    productId: 107,
    categoryId: 1,
    productName: '패밀리',
    basePrice: 28000,
    requiresFlavorSelection: true,
    selectableFlavorCount: 5,
    containerPolicy: 'CUP_ONLY',
    isLarge: true,
    isNew: false
  },
  {
    productId: 108,
    categoryId: 1,
    productName: '하프갤런',
    basePrice: 32000,
    requiresFlavorSelection: true,
    selectableFlavorCount: 5,
    containerPolicy: 'CUP_ONLY',
    isLarge: true,
    isNew: false
  },
  // 커피
  {
    productId: 201,
    categoryId: 2,
    productName: '아메리카노',
    basePrice: 3000,
    requiresFlavorSelection: false,
    selectableFlavorCount: 0,
    containerPolicy: 'NONE',
    isLarge: false,
    isNew: false
  },
  {
    productId: 202,
    categoryId: 2,
    productName: '카페라떼',
    basePrice: 3800,
    requiresFlavorSelection: false,
    selectableFlavorCount: 0,
    containerPolicy: 'NONE',
    isLarge: false,
    isNew: false
  },
  {
    productId: 203,
    categoryId: 2,
    productName: '바닐라라떼',
    basePrice: 4200,
    requiresFlavorSelection: false,
    selectableFlavorCount: 0,
    containerPolicy: 'NONE',
    isLarge: false,
    isNew: false
  },
  {
    productId: 204,
    categoryId: 2,
    productName: '카페모카',
    basePrice: 4500,
    requiresFlavorSelection: false,
    selectableFlavorCount: 0,
    containerPolicy: 'NONE',
    isLarge: false,
    isNew: false
  }
]

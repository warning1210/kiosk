import singleRegular from '../assets/kiosk/products/single-regular.png'
import singleKing from '../assets/kiosk/products/single-king.png'
import doubleJunior from '../assets/kiosk/products/double-junior.png'
import doubleRegular from '../assets/kiosk/products/double-regular.png'
import pint from '../assets/kiosk/products/pint.png'
import quart from '../assets/kiosk/products/quart.png'
import family from '../assets/kiosk/products/family.png'
import halfGallon from '../assets/kiosk/products/half-gallon.png'

// Figma 목업의 컵/콘 아이콘을 실제 상품명(seed 데이터 기준)에 매칭
const PRODUCT_IMAGES = {
  싱글레귤러: singleRegular,
  싱글킹: singleKing,
  더블주니어: doubleJunior,
  더블레귤러: doubleRegular,
  파인트: pint,
  쿼터: quart,
  패밀리: family,
  하프갤런: halfGallon
}

export function productImage(productName) {
  return PRODUCT_IMAGES[productName] ?? null
}

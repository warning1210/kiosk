export function setCookie(name, value, days) {
  let expires = ''
  if (days) {
    const date = new Date()
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000)
    expires = `; expires=${date.toUTCString()}`
  }
  // days 생략 시 세션 쿠키(브라우저/탭 종료 시 삭제) - 키오스크는 다음 고객이
  // 이전 고객 장바구니를 이어받으면 안 되므로 기본값으로 세션 쿠키를 사용한다.
  document.cookie = `${name}=${encodeURIComponent(value)}${expires}; path=/; SameSite=Lax`
}

export function getCookie(name) {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

export function removeCookie(name) {
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`
}

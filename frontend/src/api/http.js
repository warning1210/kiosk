import axios from 'axios'
import { firebaseAuth } from '../firebase'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use(async (config) => {
  await firebaseAuth.authStateReady()
  if (firebaseAuth.currentUser) {
    config.headers.Authorization = `Bearer ${await firebaseAuth.currentUser.getIdToken()}`
  }
  return config
})

export default http

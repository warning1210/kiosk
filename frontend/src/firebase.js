import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'

const firebaseConfig = {
  apiKey: 'AIzaSyBmVmlztANvEw-nohPJtVKxlUZeubeYbCc',
  authDomain: 'kiosk-br-a3511.firebaseapp.com',
  projectId: 'kiosk-br-a3511',
  storageBucket: 'kiosk-br-a3511.firebasestorage.app',
  messagingSenderId: '560048489233',
  appId: '1:560048489233:web:92daf9f1d753e0594302b0',
  measurementId: 'G-XY22B2ZW7W'
}

export const firebaseApp = initializeApp(firebaseConfig)
export const firebaseAuth = getAuth(firebaseApp)

import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'

const firebaseConfig = {
  apiKey: 'AIzaSyDjsPQS9Clykvw8P63nV_7SZXzo7qNTc_I',
  authDomain: 'gen-lang-client-0587041047.firebaseapp.com',
  projectId: 'gen-lang-client-0587041047',
  storageBucket: 'gen-lang-client-0587041047.firebasestorage.app',
  messagingSenderId: '883895319446',
  appId: '1:883895319446:web:9d56207049ab04799c7bb7',
  measurementId: 'G-J58NRF513S'
}

export const firebaseApp = initializeApp(firebaseConfig)
export const firebaseAuth = getAuth(firebaseApp)

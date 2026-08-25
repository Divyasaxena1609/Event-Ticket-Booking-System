/** @type {import('tailwindcss').Config} */
export default { darkMode: 'class', content: ['./index.html', './src/**/*.{js,jsx}'], theme: { extend: { colors: { ink: '#111827', brand: { 50: '#f1f5ff', 100: '#e4e8ff', 200: '#ccd4ff', 500: '#5b5ce2', 600: '#4c46d5', 700: '#3e36bb' } }, boxShadow: { glow: '0 16px 50px rgba(91,92,226,.22)' } } }, plugins: [] };

/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f1f6ff',
          100: '#e1ecff',
          200: '#c7dcff',
          300: '#9fc3ff',
          400: '#6f9eff',
          500: '#4a84ff',
          600: '#2768F5',
          700: '#1f58ce',
          800: '#1a49a9',
          900: '#163d8d'
        }
      }
    },
  },
  plugins: [],
};



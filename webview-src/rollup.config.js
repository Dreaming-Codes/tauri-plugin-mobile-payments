// rollup.config.js (inside src-tauri/plugins/tauri-plugin-mobile-payments)
import { nodeResolve } from '@rollup/plugin-node-resolve';
import typescript from 'rollup-plugin-typescript2';
import terser from '@rollup/plugin-terser'; // Corrected import name if using named import

export default {
  input: './webview-src/index.ts',
  output: {
    dir: './webview-dist',
    entryFileNames: '[name].js',
    format: 'es',
    exports: 'auto'
  },
  plugins: [
    nodeResolve(),
    typescript({ // Run TypeScript compilation first
      tsconfig: './webview-src/tsconfig.json',
      moduleResolution: 'node',
      clean: true // Optional: clean cache on rebuild
    }),
    terser() // Run Terser minification last on the generated JavaScript
  ]
};
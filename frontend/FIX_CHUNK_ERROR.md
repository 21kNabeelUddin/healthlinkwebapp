# Fixing Next.js ChunkLoadError

If you encounter `ChunkLoadError: Loading chunk app/layout failed`, try these steps:

1. **Clean the build directory:**
   ```bash
   rm -rf .next
   # Or on Windows PowerShell:
   Remove-Item -Recurse -Force .next
   ```

2. **Clear Next.js cache:**
   ```bash
   rm -rf node_modules/.cache
   ```

3. **Reinstall dependencies (if needed):**
   ```bash
   npm install
   ```

4. **Restart the dev server:**
   ```bash
   npm run dev
   ```

5. **If the issue persists, try a full rebuild:**
   ```bash
   npm run build
   npm run dev
   ```

The `.next` directory has been cleaned. Try running `npm run dev` again.


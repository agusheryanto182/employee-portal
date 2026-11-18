import { defineConfig, minimal2023Preset } from '@vite-pwa/assets-generator/config'

// `npm run generate-pwa-assets` turns public/logo.svg into the favicon, app and Apple touch icons.
export default defineConfig({
  headLinkOptions: { preset: '2023' },
  preset: {
    ...minimal2023Preset,
    // Maskable icons get cropped to a circle or squircle: keep the logo inside the safe zone on its own background.
    maskable: { ...minimal2023Preset.maskable, resizeOptions: { background: '#171717' } },
    apple: { ...minimal2023Preset.apple, resizeOptions: { background: '#171717' } },
  },
  images: ['public/logo.svg'],
})

import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.aistudio.app',
  appName: 'AI Studio',
  webDir: 'dist',
  server: {
    // Use http (not https) so that http://10.0.2.2:3000 / http://192.168.x.x:3000
    // backends are same-scheme and not blocked as "Mixed Content" by the
    // WebView (origin would be https://localhost with androidScheme: 'https').
    // For production with an HTTPS API, switch back to 'https' and point
    // VITE_API_URL at the https backend.
    androidScheme: 'http',
    cleartext: true,
    allowMixedContent: true,
  },
  plugins: {
    Keyboard: {
      resize: 'body',
      resizeOnFullScreen: true,
    },
    StatusBar: {
      // Used by Android 14 and below. Android 15+ is edge-to-edge by
      // platform policy and the header uses the injected safe-area inset.
      overlaysWebView: false,
    },
  },
};

export default config;

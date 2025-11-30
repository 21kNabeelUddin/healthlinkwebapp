// Simple client-side navigation helper
export function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

// Listen to browser back/forward buttons
if (typeof window !== 'undefined') {
  window.addEventListener('popstate', () => {
    window.location.reload();
  });
}

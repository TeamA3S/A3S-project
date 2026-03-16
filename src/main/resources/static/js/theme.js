/**
 * 테마 토글 (다크 모드)
 */

// localStorage에서 테마 로드 (기본값: light)
const currentTheme = localStorage.getItem('theme') || 'light';
document.documentElement.setAttribute('data-theme', currentTheme);

/**
 * 라이트/다크 테마 전환
 */
function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);

    // 토글 버튼 아이콘 업데이트
    updateThemeIcon(newTheme);
}

/**
 * 테마 토글 버튼 아이콘 업데이트
 */
function updateThemeIcon(theme) {
    const themeToggle = document.getElementById('theme-toggle');
    if (themeToggle) {
        themeToggle.textContent = theme === 'dark' ? '☀️' : '🌙';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    updateThemeIcon(currentTheme);
});

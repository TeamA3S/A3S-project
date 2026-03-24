/**
 * 인증 체크 스크립트
 * JWT 토큰 확인 및 로그아웃 처리
 */

/**
 * 로그인 여부 확인
 * JWT 토큰이 없으면 로그인 페이지로 리다이렉트
 */
function checkAuthentication() {
    // 쿠키에서 토큰 확인
    const token = typeof getToken === 'function' ? getToken() : null;
    const currentPath = window.location.pathname;

    // 로그인/회원가입 페이지는 체크 제외
    if (currentPath === '/pages/login' || currentPath === '/pages/register') {
        return;
    }

    // 토큰이 없으면 로그인 페이지로 이동
    if (!token) {
        window.location.href = '/pages/login';
        return;
    }

    // 사용자 정보 표시
    displayUserInfo();
}

/**
 * 네비게이션 바에 사용자 정보 표시
 */
function displayUserInfo() {
    // JWT 토큰에서 이메일 추출
    const email = typeof getEmailFromToken === 'function' ? getEmailFromToken() : null;
    const navbarActions = document.querySelector('.navbar-actions');

    if (email && navbarActions) {
        // 사용자 정보 요소 추가
        const userInfo = document.createElement('div');
        userInfo.style.cssText = 'display: flex; align-items: center; gap: 1rem; margin-right: 1rem;';
        userInfo.innerHTML = `
            <span style="color: var(--text-secondary); font-size: 0.9rem;">
                👤 ${email}
            </span>
            <button onclick="handleLogout()" class="btn btn-outline" style="padding: 0.4rem 1rem; font-size: 0.875rem;">
                로그아웃
            </button>
        `;

        // navbar-actions 맨 앞에 삽입
        navbarActions.insertBefore(userInfo, navbarActions.firstChild);
    }
}

/**
 * 로그아웃 처리
 */
function handleLogout() {
    // 쿠키에서 토큰 제거
    if (typeof removeToken === 'function') removeToken();

    // 로그인 페이지로 이동
    window.location.href = '/pages/login';
}

// 페이지 로드 시 인증 체크
document.addEventListener('DOMContentLoaded', function() {
    checkAuthentication();
});

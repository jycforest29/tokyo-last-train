import './Header.css';

export function Header() {
  return (
    <header className="header">
      <div className="header-icon">
        <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
          <rect x="6" y="4" width="16" height="18" rx="4" stroke="var(--accent-primary)" strokeWidth="2" />
          <rect x="10" y="8" width="8" height="5" rx="1" fill="var(--accent-secondary)" opacity="0.7" />
          <circle cx="10.5" cy="17.5" r="1.5" fill="var(--text-secondary)" />
          <circle cx="17.5" cy="17.5" r="1.5" fill="var(--text-secondary)" />
          <line x1="8" y1="24" x2="8" y2="26" stroke="var(--text-secondary)" strokeWidth="2" strokeLinecap="round" />
          <line x1="20" y1="24" x2="20" y2="26" stroke="var(--text-secondary)" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </div>
      <div className="header-text">
        <h1 className="header-title">도쿄 막차 검색</h1>
        <p className="header-subtitle">Tokyo Last Train Finder</p>
      </div>
    </header>
  );
}

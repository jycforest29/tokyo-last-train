import { useTranslation } from '../i18n/LanguageContext';
import type { Language } from '../i18n/translations';
import { useTheme } from '../hooks/useTheme';
import './Header.css';

const LANGUAGES: { code: Language; label: string }[] = [
  { code: 'ja', label: '日本語' },
  { code: 'en', label: 'EN' },
  { code: 'ko', label: '한국어' },
];

export function Header() {
  const { t, language, setLanguage } = useTranslation();
  const { theme, toggle } = useTheme();
  const isDark = theme === 'dark';

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
        <h1 className="header-title">{t('app.title')}</h1>
        <p className="header-subtitle">{t('app.subtitle')}</p>
      </div>
      <button
        type="button"
        className="theme-toggle"
        onClick={toggle}
        aria-label={t(isDark ? 'theme.toLight' : 'theme.toDark')}
        title={t(isDark ? 'theme.toLight' : 'theme.toDark')}
      >
        {isDark ? (
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
          </svg>
        ) : (
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
        )}
      </button>
      <div className="language-switcher" role="group" aria-label={t('language.label')}>
        {LANGUAGES.map(({ code, label }) => (
          <button
            key={code}
            type="button"
            className={`language-button ${language === code ? 'active' : ''}`}
            onClick={() => setLanguage(code)}
            aria-pressed={language === code}
          >
            {label}
          </button>
        ))}
      </div>
    </header>
  );
}

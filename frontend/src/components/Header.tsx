import { useTranslation } from '../i18n/LanguageContext';
import type { Language } from '../i18n/translations';
import './Header.css';

const LANGUAGES: { code: Language; label: string }[] = [
  { code: 'ja', label: '日本語' },
  { code: 'en', label: 'EN' },
  { code: 'ko', label: '한국어' },
];

export function Header() {
  const { t, language, setLanguage } = useTranslation();

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

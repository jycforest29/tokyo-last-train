import { useTranslation } from '../i18n/LanguageContext';
import './Footer.css';

export function Footer() {
  const { t } = useTranslation();
  return (
    <footer className="footer">
      <p className="footer-disclaimer">{t('footer.disclaimer')}</p>
      <p className="footer-attribution">
        <a
          href="https://developer.odpt.org/"
          target="_blank"
          rel="noopener noreferrer"
        >
          {t('footer.dataSource')}
        </a>
        {' — '}
        {t('footer.attribution')}
      </p>
      <p className="footer-links">
        <a href="/privacy">{t('footer.privacy')}</a>
      </p>
    </footer>
  );
}

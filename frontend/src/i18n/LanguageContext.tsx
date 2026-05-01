import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { translations } from './translations';
import type { Language, TranslationKey } from './translations';
import type { StationInfo, LastTrainRoute, Transfer } from '../types';

const STORAGE_KEY = 'tokyo-last-train.lang';

function detectInitialLanguage(): Language {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === 'ja' || stored === 'en' || stored === 'ko') return stored;
  const nav = navigator.language.toLowerCase();
  if (nav.startsWith('ko')) return 'ko';
  if (nav.startsWith('en')) return 'en';
  return 'ja';
}

interface LanguageContextValue {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: TranslationKey) => string;
  /** 현재 언어 기준으로 역명을 반환. 한국어가 없으면 일본어로 폴백. */
  stationName: (s: Pick<StationInfo, 'nameJa' | 'nameEn' | 'nameKo'>) => string;
  /** 보조 표시(메인이 아닌) 역명. 메인 언어와 다른 언어로 보여줌. */
  stationNameSecondary: (s: Pick<StationInfo, 'nameJa' | 'nameEn' | 'nameKo'>) => string | null;
  /** 노선명. 한국어가 없으면 일본어로 폴백. */
  railwayName: (
    r: Pick<StationInfo, 'railwayNameJa' | 'railwayNameEn' | 'railwayNameKo'>
        | Pick<LastTrainRoute, 'railwayNameJa' | 'railwayNameEn' | 'railwayNameKo'>
  ) => string;
  /** 환승역 역명. */
  transferStationName: (t: Transfer) => string;
  /** 행선지 (LastTrainRoute) 역명. */
  destinationName: (r: LastTrainRoute) => string;
}

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() => detectInitialLanguage());

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, language);
    document.documentElement.lang = language;
  }, [language]);

  const value = useMemo<LanguageContextValue>(() => {
    const setLanguage = (lang: Language) => setLanguageState(lang);

    const t = (key: TranslationKey) => translations[language][key] ?? key;

    const pickStation = (s: { nameJa: string; nameEn: string; nameKo: string | null }) => {
      if (language === 'ko') return s.nameKo ?? s.nameJa;
      if (language === 'en') return s.nameEn ?? s.nameJa;
      return s.nameJa;
    };

    const stationNameSecondary = (s: { nameJa: string; nameEn: string; nameKo: string | null }) => {
      // ja 메인일 때는 영문 부제, en 메인일 때는 일문, ko 메인일 때는 영문
      if (language === 'ja') return s.nameEn || null;
      if (language === 'en') return s.nameJa || null;
      return s.nameEn || null;
    };

    const railwayName = (r: {
      railwayNameJa: string;
      railwayNameEn: string;
      railwayNameKo: string | null;
    }) => {
      if (language === 'ko') return r.railwayNameKo ?? r.railwayNameJa;
      if (language === 'en') return r.railwayNameEn ?? r.railwayNameJa;
      return r.railwayNameJa;
    };

    const transferStationName = (transfer: Transfer) => {
      if (language === 'ko') return transfer.stationNameKo ?? transfer.stationNameJa;
      if (language === 'en') return transfer.stationNameEn ?? transfer.stationNameJa;
      return transfer.stationNameJa;
    };

    const destinationName = (route: LastTrainRoute) => {
      if (language === 'ko') return route.destinationNameKo ?? route.destinationNameJa;
      if (language === 'en') return route.destinationNameEn ?? route.destinationNameJa;
      return route.destinationNameJa;
    };

    return {
      language,
      setLanguage,
      t,
      stationName: pickStation,
      stationNameSecondary,
      railwayName,
      transferStationName,
      destinationName,
    };
  }, [language]);

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useTranslation() {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    throw new Error('useTranslation must be used within LanguageProvider');
  }
  return ctx;
}

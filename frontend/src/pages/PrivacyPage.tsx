import { Header } from '../components/Header';
import { useTranslation } from '../i18n/LanguageContext';
import type { Language } from '../i18n/translations';
import './PrivacyPage.css';

type Section = { title: string; body: string };

type LangCopy = {
  pageTitle: string;
  intro: string;
  sections: Section[];
  back: string;
  lastUpdated: string;
};

const COPY: Record<Language, LangCopy> = {
  ja: {
    pageTitle: 'プライバシーポリシー',
    intro:
      '本サービスは、利用者を識別できる個人情報を収集していません。最低限のクライアント側ストレージのみで動作します。',
    sections: [
      {
        title: '1. 個人情報の収集',
        body:
          '氏名・メールアドレス・連絡先などの個人情報は一切収集していません。アカウント登録機能もありません。',
      },
      {
        title: '2. ローカルストレージ',
        body:
          '言語設定および「自宅」として登録した駅情報のみ、ご利用ブラウザのlocalStorageに保存します。これらの情報はサーバーに送信されません。',
      },
      {
        title: '3. 位置情報 (GPS)',
        body:
          '「現在地から検索」をご利用いただく場合、その瞬間の緯度・経度を最寄り駅検索のためにサーバーへ送信します。サーバーは結果を返した後、座標を保存・記録しません。アクセスログのクエリ文字列もマスクされます。',
      },
      {
        title: '4. 第三者トラッカー',
        body:
          'Google Analytics などの第三者トラッカー、広告タグ、Cookie ベースの追跡技術は一切使用していません。',
      },
    ],
    back: '← トップページへ戻る',
    lastUpdated: '最終更新: 2026-05-03',
  },
  en: {
    pageTitle: 'Privacy Policy',
    intro:
      'This service does not collect personally identifying information. It operates with minimal client-side storage only.',
    sections: [
      {
        title: '1. Personal Information',
        body:
          'We do not collect names, email addresses, phone numbers, or any other personal information. There are no user accounts.',
      },
      {
        title: '2. Local Storage',
        body:
          'Only your language preference and a saved "home" station are stored in your browser\'s localStorage. These never leave your device.',
      },
      {
        title: '3. Location (GPS)',
        body:
          'When you use "find from current location", your latitude and longitude are sent once to look up the nearest station. The server does not store or log the coordinates, and access-log query strings are masked.',
      },
      {
        title: '4. Third-party trackers',
        body:
          'No Google Analytics, no ad tags, no cookie-based tracking of any kind.',
      },
    ],
    back: '← Back to home',
    lastUpdated: 'Last updated: 2026-05-03',
  },
  ko: {
    pageTitle: '개인정보 처리 방침',
    intro:
      '본 서비스는 이용자를 식별할 수 있는 개인정보를 수집하지 않습니다. 최소한의 클라이언트 저장소만 사용합니다.',
    sections: [
      {
        title: '1. 개인정보 수집',
        body:
          '이름, 이메일, 연락처 등 어떤 개인정보도 수집하지 않습니다. 회원 가입 기능도 없습니다.',
      },
      {
        title: '2. 로컬스토리지',
        body:
          '언어 설정과 "자택"으로 등록한 역 정보만 사용자의 브라우저 localStorage에 저장됩니다. 이 정보는 서버로 전송되지 않습니다.',
      },
      {
        title: '3. 위치 정보 (GPS)',
        body:
          '"현재 위치로 검색"을 사용할 때, 해당 순간의 위·경도가 가장 가까운 역을 찾기 위해 서버로 전송됩니다. 서버는 결과 반환 후 좌표를 저장·기록하지 않으며, 액세스 로그의 쿼리 문자열도 마스킹됩니다.',
      },
      {
        title: '4. 제3자 트래커',
        body:
          'Google Analytics 등 제3자 트래커, 광고 태그, 쿠키 기반 추적 기술을 일절 사용하지 않습니다.',
      },
    ],
    back: '← 메인으로 돌아가기',
    lastUpdated: '최종 갱신: 2026-05-03',
  },
};

export function PrivacyPage() {
  const { language } = useTranslation();
  const copy = COPY[language];

  return (
    <div className="app">
      <div className="app-container">
        <Header />
        <article className="privacy-page">
          <h1 className="privacy-title">{copy.pageTitle}</h1>
          <p className="privacy-intro">{copy.intro}</p>
          {copy.sections.map((s) => (
            <section className="privacy-section" key={s.title}>
              <h2 className="privacy-section-title">{s.title}</h2>
              <p className="privacy-section-body">{s.body}</p>
            </section>
          ))}
          <p className="privacy-updated">{copy.lastUpdated}</p>
          <a className="privacy-back" href="/">{copy.back}</a>
        </article>
      </div>
    </div>
  );
}

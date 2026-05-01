export type Language = 'ja' | 'en' | 'ko';

export type TranslationKey =
  | 'app.title'
  | 'app.subtitle'
  | 'search.from'
  | 'search.to'
  | 'search.placeholder'
  | 'search.submit'
  | 'search.swap'
  | 'search.searching'
  | 'route.transfer'
  | 'route.transferAt'
  | 'route.fare'
  | 'route.minutes'
  | 'route.directRoute'
  | 'route.transferCount'
  | 'route.depart'
  | 'route.arrive'
  | 'route.bound'
  | 'route.trainType.local'
  | 'route.trainType.rapid'
  | 'route.trainType.express'
  | 'result.noRoutes'
  | 'result.weekday'
  | 'result.saturday'
  | 'result.holiday'
  | 'result.calendar'
  | 'error.searchFailed'
  | 'error.connection'
  | 'loading'
  | 'language.label'
  | 'footer.attribution'
  | 'footer.dataSource';

export const translations: Record<Language, Record<TranslationKey, string>> = {
  ja: {
    'app.title': '東京終電検索',
    'app.subtitle': 'Tokyo Last Train Finder',
    'search.from': '出発駅',
    'search.to': '到着駅',
    'search.placeholder': '駅名を入力',
    'search.submit': '終電を検索',
    'search.swap': '駅を入れ替え',
    'search.searching': '検索中…',
    'route.transfer': '乗換',
    'route.transferAt': '乗換',
    'route.fare': '運賃',
    'route.minutes': '分',
    'route.directRoute': '直通',
    'route.transferCount': '回乗換',
    'route.depart': '発',
    'route.arrive': '着',
    'route.bound': '行',
    'route.trainType.local': '各停',
    'route.trainType.rapid': '快速',
    'route.trainType.express': '急行',
    'result.noRoutes': '経路が見つかりませんでした',
    'result.weekday': '平日',
    'result.saturday': '土曜',
    'result.holiday': '休日',
    'result.calendar': 'カレンダー',
    'error.searchFailed': '検索に失敗しました',
    'error.connection': 'サーバーに接続できません',
    'loading': '読み込み中…',
    'language.label': '言語',
    'footer.attribution': '本サービスで使用している交通データは、公共交通オープンデータセンターから提供されたデータを利用しています。本サービスの内容は公共交通事業者および公共交通オープンデータセンターによって保証されたものではありません。',
    'footer.dataSource': '公共交通オープンデータセンター',
  },
  en: {
    'app.title': 'Tokyo Last Train Finder',
    'app.subtitle': 'Find the last train home',
    'search.from': 'From',
    'search.to': 'To',
    'search.placeholder': 'Search a station',
    'search.submit': 'Find last train',
    'search.swap': 'Swap stations',
    'search.searching': 'Searching…',
    'route.transfer': 'Transfer',
    'route.transferAt': 'Transfer at',
    'route.fare': 'Fare',
    'route.minutes': 'min',
    'route.directRoute': 'Direct',
    'route.transferCount': ' transfers',
    'route.depart': 'Depart',
    'route.arrive': 'Arrive',
    'route.bound': 'bound',
    'route.trainType.local': 'Local',
    'route.trainType.rapid': 'Rapid',
    'route.trainType.express': 'Express',
    'result.noRoutes': 'No routes found',
    'result.weekday': 'Weekday',
    'result.saturday': 'Saturday',
    'result.holiday': 'Holiday',
    'result.calendar': 'Calendar',
    'error.searchFailed': 'Search failed',
    'error.connection': 'Cannot connect to server',
    'loading': 'Loading…',
    'language.label': 'Language',
    'footer.attribution': 'Transit data used by this service is provided by the Public Transportation Open Data Center. The content is not endorsed by the public transportation operators or by the Open Data Center.',
    'footer.dataSource': 'Public Transportation Open Data Center',
  },
  ko: {
    'app.title': '도쿄 막차 검색',
    'app.subtitle': 'Tokyo Last Train Finder',
    'search.from': '출발역',
    'search.to': '도착역',
    'search.placeholder': '역 이름 입력',
    'search.submit': '막차 검색',
    'search.swap': '역 교환',
    'search.searching': '검색 중…',
    'route.transfer': '환승',
    'route.transferAt': '환승',
    'route.fare': '운임',
    'route.minutes': '분',
    'route.directRoute': '직통',
    'route.transferCount': '회 환승',
    'route.depart': '출발',
    'route.arrive': '도착',
    'route.bound': '행',
    'route.trainType.local': '각역정차',
    'route.trainType.rapid': '쾌속',
    'route.trainType.express': '급행',
    'result.noRoutes': '경로를 찾을 수 없습니다',
    'result.weekday': '평일',
    'result.saturday': '토요일',
    'result.holiday': '휴일',
    'result.calendar': '캘린더',
    'error.searchFailed': '검색에 실패했습니다',
    'error.connection': '서버에 연결할 수 없습니다',
    'loading': '로딩 중…',
    'language.label': '언어',
    'footer.attribution': '본 서비스에서 사용하는 교통 데이터는 공공교통 오픈데이터센터에서 제공된 데이터를 이용하고 있습니다. 본 서비스의 내용은 공공교통 사업자 및 공공교통 오픈데이터센터에 의해 보증된 것이 아닙니다.',
    'footer.dataSource': '공공교통 오픈데이터센터',
  },
};

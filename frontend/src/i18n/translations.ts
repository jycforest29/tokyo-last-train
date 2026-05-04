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
  | 'footer.dataSource'
  | 'countdown.departed'
  | 'countdown.soon'
  | 'countdown.minutes'
  | 'countdown.hourMinutes'
  | 'alert.title'
  | 'alert.notify30'
  | 'alert.notify15'
  | 'alert.notify5'
  | 'alert.scheduled'
  | 'alert.cancel'
  | 'alert.tooLate'
  | 'alert.denied'
  | 'alert.keepTabOpen'
  | 'alert.notifBody'
  | 'home.set'
  | 'home.unset'
  | 'home.saved'
  | 'gps.useCurrent'
  | 'gps.locating'
  | 'gps.denied'
  | 'gps.unsupported'
  | 'gps.error'
  | 'quickLaunch.cta'
  | 'quickLaunch.subtitle'
  | 'footer.privacy'
  | 'footer.disclaimer'
  | 'search.noResults'
  | 'search.disabledHint'
  | 'theme.toLight'
  | 'theme.toDark'
  | 'station.noTimetable'
  | 'route.fareIc'
  | 'route.fareTicket'
  | 'transfer.wait'
  | 'transfer.platform'
  | 'alternatives.heading'
  | 'alternatives.before'
  | 'alternatives.after'
  | 'taxi.heading'
  | 'taxi.estimate'
  | 'taxi.distance'
  | 'taxi.nightNote';

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
    'countdown.departed': '出発済み',
    'countdown.soon': 'まもなく発車',
    'countdown.minutes': 'あと{n}分',
    'countdown.hourMinutes': 'あと{h}時間{m}分',
    'alert.title': '通知',
    'alert.notify30': '30分前',
    'alert.notify15': '15分前',
    'alert.notify5': '5分前',
    'alert.scheduled': '通知予約済み',
    'alert.cancel': '取消',
    'alert.tooLate': 'もう間に合いません',
    'alert.denied': '通知が許可されていません',
    'alert.keepTabOpen': 'このタブを開いたままにしてください',
    'alert.notifBody': '{line} {dest}行 {time}発まで{n}分',
    'home.set': '🏠 自宅に設定',
    'home.unset': '🏠 自宅を解除',
    'home.saved': '自宅: {station}',
    'gps.useCurrent': '📍 現在地から',
    'gps.locating': '📍 位置情報取得中…',
    'gps.denied': '位置情報の使用が許可されていません',
    'gps.unsupported': 'このブラウザは位置情報に対応していません',
    'gps.error': '現在地を取得できませんでした',
    'quickLaunch.cta': '📍🏠 今すぐ {home}へ',
    'quickLaunch.subtitle': '現在地から終電を検索',
    'footer.privacy': 'プライバシーポリシー',
    'footer.disclaimer': '本サービスは参考情報です。実際の運行は各鉄道事業者の公式情報をご確認ください。',
    'search.noResults': '該当する駅が見つかりません',
    'search.disabledHint': '出発駅と到着駅を選択してください',
    'theme.toLight': 'ライトモードに切替',
    'theme.toDark': 'ダークモードに切替',
    'station.noTimetable': 'この路線は時刻表データが提供されていません',
    'route.fareIc': 'IC',
    'route.fareTicket': '紙券',
    'transfer.wait': '乗換待ち {n}分',
    'transfer.platform': '{n}番線',
    'alternatives.heading': '近隣駅の終電',
    'alternatives.before': '{station}まで（一駅手前）',
    'alternatives.after': '{station}まで（一駅先）',
    'taxi.heading': '🚕 タクシー目安',
    'taxi.estimate': '約 ¥{day} (深夜 約 ¥{night})',
    'taxi.distance': '直線距離 約 {km} km',
    'taxi.nightNote': '現在は深夜割増時間帯（22:00–05:00）',
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
    'countdown.departed': 'Departed',
    'countdown.soon': 'Departing soon',
    'countdown.minutes': '{n} min left',
    'countdown.hourMinutes': '{h}h {m}m left',
    'alert.title': 'Notify',
    'alert.notify30': '30 min before',
    'alert.notify15': '15 min before',
    'alert.notify5': '5 min before',
    'alert.scheduled': 'Notification set',
    'alert.cancel': 'Cancel',
    'alert.tooLate': 'Too late to set',
    'alert.denied': 'Notifications blocked',
    'alert.keepTabOpen': 'Keep this tab open',
    'alert.notifBody': '{line} bound for {dest} departs in {n} min ({time})',
    'home.set': '🏠 Save as home',
    'home.unset': '🏠 Remove home',
    'home.saved': 'Home: {station}',
    'gps.useCurrent': '📍 Use current location',
    'gps.locating': '📍 Locating…',
    'gps.denied': 'Location access denied',
    'gps.unsupported': 'Geolocation not supported',
    'gps.error': 'Could not get your location',
    'quickLaunch.cta': '📍🏠 Last train to {home}',
    'quickLaunch.subtitle': 'From your current location',
    'footer.privacy': 'Privacy',
    'footer.disclaimer': 'This service provides reference information only. Please confirm actual operations with each rail operator.',
    'search.noResults': 'No matching station',
    'search.disabledHint': 'Select both From and To stations',
    'theme.toLight': 'Switch to light mode',
    'theme.toDark': 'Switch to dark mode',
    'station.noTimetable': 'Timetable data not available for this line',
    'route.fareIc': 'IC',
    'route.fareTicket': 'Ticket',
    'transfer.wait': '{n} min wait',
    'transfer.platform': 'Platform {n}',
    'alternatives.heading': 'Last trains to nearby stations',
    'alternatives.before': 'to {station} (one stop before)',
    'alternatives.after': 'to {station} (one stop after)',
    'taxi.heading': '🚕 Taxi estimate',
    'taxi.estimate': '~¥{day} (~¥{night} late night)',
    'taxi.distance': 'Straight-line ~{km} km',
    'taxi.nightNote': 'Late-night surcharge in effect (22:00–05:00)',
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
    'countdown.departed': '출발 완료',
    'countdown.soon': '곧 출발',
    'countdown.minutes': '{n}분 남음',
    'countdown.hourMinutes': '{h}시간 {m}분 남음',
    'alert.title': '알림',
    'alert.notify30': '30분 전',
    'alert.notify15': '15분 전',
    'alert.notify5': '5분 전',
    'alert.scheduled': '알림 예약됨',
    'alert.cancel': '취소',
    'alert.tooLate': '시간이 부족합니다',
    'alert.denied': '알림이 차단되어 있습니다',
    'alert.keepTabOpen': '이 탭을 열어두세요',
    'alert.notifBody': '{line} {dest}행 {time} 출발 {n}분 전',
    'home.set': '🏠 자택으로 등록',
    'home.unset': '🏠 자택 등록 해제',
    'home.saved': '자택: {station}',
    'gps.useCurrent': '📍 현재 위치로',
    'gps.locating': '📍 위치 확인 중…',
    'gps.denied': '위치 정보 접근이 거부되었습니다',
    'gps.unsupported': '브라우저가 위치 정보를 지원하지 않습니다',
    'gps.error': '현재 위치를 가져올 수 없습니다',
    'quickLaunch.cta': '📍🏠 지금 {home}까지',
    'quickLaunch.subtitle': '현재 위치에서 막차 검색',
    'footer.privacy': '개인정보 처리 방침',
    'footer.disclaimer': '본 서비스는 참고 정보입니다. 실제 운행은 각 철도 사업자의 공식 정보를 확인해 주세요.',
    'search.noResults': '일치하는 역이 없습니다',
    'search.disabledHint': '출발역과 도착역을 선택하세요',
    'theme.toLight': '라이트 모드로 전환',
    'theme.toDark': '다크 모드로 전환',
    'station.noTimetable': '이 노선은 시간표 데이터가 제공되지 않습니다',
    'route.fareIc': 'IC',
    'route.fareTicket': '지폐',
    'transfer.wait': '환승 대기 {n}분',
    'transfer.platform': '{n}번 플랫폼',
    'alternatives.heading': '인접 역의 막차',
    'alternatives.before': '{station}까지 (한 정거장 전)',
    'alternatives.after': '{station}까지 (한 정거장 후)',
    'taxi.heading': '🚕 택시 추정',
    'taxi.estimate': '약 ¥{day} (심야 약 ¥{night})',
    'taxi.distance': '직선거리 약 {km} km',
    'taxi.nightNote': '현재 심야할증 시간대 (22:00–05:00)',
  },
};

import type { Transfer } from '../types';
import { getRailwayColor, formatTime } from '../utils/format';
import { useTranslation } from '../i18n/LanguageContext';
import './TransferStep.css';

interface Props {
  transfer: Transfer;
}

export function TransferStep({ transfer }: Props) {
  const { t, transferStationName, language } = useTranslation();
  const time = formatTime(transfer.departureTime);
  const stationName = transferStationName(transfer);
  const nextDayLabel = language === 'ja' ? '翌日' : language === 'ko' ? '익일' : 'next day';

  return (
    <div className="transfer-step">
      <div className="transfer-line">
        <div
          className="transfer-dot"
          style={{ backgroundColor: getRailwayColor(transfer.toRailway) }}
        />
        <div className="transfer-connector" />
      </div>
      <div className="transfer-info">
        <div className="transfer-label">{t('route.transferAt')}</div>
        <div className="transfer-station">
          <span className="transfer-station-ja">{stationName}</span>
        </div>
        <div className="transfer-time">
          {time.display}
          {time.isNextDay && <span className="next-day">{nextDayLabel}</span>}
        </div>
      </div>
    </div>
  );
}

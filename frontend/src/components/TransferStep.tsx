import type { Transfer } from '../types';
import { getRailwayColor, formatTime } from '../utils/format';
import { useTranslation } from '../i18n/LanguageContext';
import './TransferStep.css';

interface Props {
  transfer: Transfer;
}

export function TransferStep({ transfer }: Props) {
  const { t, transferStationName, transferToRailwayName, language } = useTranslation();
  const depTimeStr = transfer.departureTime ?? transfer.arrivalTime ?? '';
  const time = depTimeStr ? formatTime(depTimeStr) : null;
  const stationName = transferStationName(transfer);
  const toLine = transferToRailwayName(transfer);
  const toColor = getRailwayColor(transfer.toRailway);
  const nextDayLabel = language === 'ja' ? '翌日' : language === 'ko' ? '익일' : 'next day';
  const waitLabel = transfer.waitMinutes > 0
    ? t('transfer.wait').replace('{n}', String(transfer.waitMinutes))
    : null;
  const platformLabel = transfer.toPlatform
    ? t('transfer.platform').replace('{n}', transfer.toPlatform)
    : null;

  return (
    <div className="transfer-step">
      <div className="transfer-line">
        <div
          className="transfer-dot"
          style={{ backgroundColor: toColor }}
        />
        <div className="transfer-connector" />
      </div>
      <div className="transfer-info">
        <div className="transfer-label">{t('route.transferAt')}</div>
        <div className="transfer-station">
          <span className="transfer-station-ja">{stationName}</span>
        </div>
        {toLine && (
          <div className="transfer-to-line">
            <span className="transfer-to-line-dot" style={{ backgroundColor: toColor }} />
            <span className="transfer-to-line-name">{toLine}</span>
            {platformLabel && <span className="transfer-platform">{platformLabel}</span>}
          </div>
        )}
        {time && (
          <div className="transfer-time">
            {time.display}
            {time.isNextDay && <span className="next-day">{nextDayLabel}</span>}
            {waitLabel && <span className="transfer-wait">· {waitLabel}</span>}
          </div>
        )}
      </div>
    </div>
  );
}

import type { Transfer } from '../types';
import { getRailwayColor, formatTime } from '../utils/format';
import './TransferStep.css';

interface Props {
  transfer: Transfer;
}

export function TransferStep({ transfer }: Props) {
  const time = formatTime(transfer.departureTime);

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
        <div className="transfer-label">환승</div>
        <div className="transfer-station">
          <span className="transfer-station-ja">{transfer.stationNameJa}</span>
          <span className="transfer-station-en">{transfer.stationNameEn}</span>
        </div>
        <div className="transfer-time">
          {time.display}
          {time.isNextDay && <span className="next-day">익일</span>}
        </div>
      </div>
    </div>
  );
}

const RAILWAY_COLORS: Record<string, string> = {
  // Tokyo Metro
  'odpt.Railway:TokyoMetro.Ginza': '#F39700',
  'odpt.Railway:TokyoMetro.Marunouchi': '#E60012',
  'odpt.Railway:TokyoMetro.Hibiya': '#B5B5AC',
  'odpt.Railway:TokyoMetro.Tozai': '#009BBF',
  'odpt.Railway:TokyoMetro.Chiyoda': '#00A650',
  'odpt.Railway:TokyoMetro.Yurakucho': '#C1A470',
  'odpt.Railway:TokyoMetro.Hanzomon': '#8B76D0',
  'odpt.Railway:TokyoMetro.Namboku': '#00ADA9',
  'odpt.Railway:TokyoMetro.Fukutoshin': '#9C5E31',
  // Toei
  'odpt.Railway:Toei.Asakusa': '#E85298',
  'odpt.Railway:Toei.Mita': '#0079C2',
  'odpt.Railway:Toei.Shinjuku': '#6CBB5A',
  'odpt.Railway:Toei.Oedo': '#B6007A',
  // JR East
  'odpt.Railway:JR-East.Yamanote': '#80C241',
  'odpt.Railway:JR-East.ChuoRapid': '#F15A22',
  'odpt.Railway:JR-East.ChuoSobuLocal': '#FFD400',
  'odpt.Railway:JR-East.KeihinTohokuNegishi': '#00B2E5',
  'odpt.Railway:JR-East.SaikyoKawagoe': '#00AC9B',
  'odpt.Railway:JR-East.ShonanShinjuku': '#E96B2D',
  'odpt.Railway:JR-East.Tokaido': '#F68B1E',
  'odpt.Railway:JR-East.Takasaki': '#F68B1E',
  'odpt.Railway:JR-East.Utsunomiya': '#F68B1E',
  'odpt.Railway:JR-East.Joban': '#00B261',
  'odpt.Railway:JR-East.Musashino': '#F15A22',
  // Keio
  'odpt.Railway:Keio.Keio': '#DD0077',
  'odpt.Railway:Keio.Inokashira': '#BB77CC',
  // Odakyu
  'odpt.Railway:Odakyu.Odawara': '#2B59C3',
  // Tokyu
  'odpt.Railway:Tokyu.Toyoko': '#DA0442',
  'odpt.Railway:Tokyu.DenEnToshi': '#2CAB5E',
  'odpt.Railway:Tokyu.Meguro': '#009CD2',
  // Seibu
  'odpt.Railway:Seibu.Ikebukuro': '#0068B7',
  'odpt.Railway:Seibu.Shinjuku': '#E60072',
  // Tobu
  'odpt.Railway:Tobu.TobuSkytree': '#EE7B1A',
  'odpt.Railway:Tobu.Tojo': '#473FA8',
  // Keisei
  'odpt.Railway:Keisei.Main': '#1A3B87',
  // Rinkai
  'odpt.Railway:TWR.Rinkai': '#00A4DA',
  // Yurikamome
  'odpt.Railway:Yurikamome.Yurikamome': '#00C3D9',
  // TX
  'odpt.Railway:MIR.TsurobaExpress': '#0053A0',
};

export function getRailwayColor(railway: string): string {
  return RAILWAY_COLORS[railway] ?? '#9ca3af';
}

export function formatTime(time: string): { display: string; isNextDay: boolean } {
  const [h, m] = time.split(':').map(Number);
  const isNextDay = h < 4;
  return {
    display: `${h}:${m.toString().padStart(2, '0')}`,
    isNextDay,
  };
}

export function formatFare(yen: number): string {
  return `¥${yen.toLocaleString()}`;
}

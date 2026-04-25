import './Footer.css';

export function Footer() {
  return (
    <footer className="footer">
      <p className="footer-attribution">
        本サービスで使用している交通データは、
        <a
          href="https://developer.odpt.org/"
          target="_blank"
          rel="noopener noreferrer"
        >
          公共交通オープンデータセンター
        </a>
        から提供されたデータを利用しています。本サービスの内容は公共交通事業者および公共交通オープンデータセンターによって保証されたものではありません。
      </p>
    </footer>
  );
}

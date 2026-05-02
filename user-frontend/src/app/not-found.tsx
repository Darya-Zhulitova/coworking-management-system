import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-6">
        <div className="card border-0 shadow-sm">
          <div className="card-body p-5 text-center">
            <h1 className="display-6 mb-3">Страница не найдена</h1>
            <p className="text-body-secondary mb-4">Проверьте адрес страницы или вернитесь к списку доступных
              коворкингов.</p>
            <Link href="/" className="btn btn-primary">
              На главную
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

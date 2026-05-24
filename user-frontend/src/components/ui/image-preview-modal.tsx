'use client';

type ImagePreviewModalProps = {
  image: { url: string; title: string } | null;
  onClose: () => void;
};

export function ImagePreviewModal({ image, onClose }: ImagePreviewModalProps) {
  if (!image) return null;

  return (
    <>
      <div
        className="modal fade show d-block"
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby="imagePreviewModalTitle"
        onClick={onClose}
      >
        <div className="modal-dialog modal-xl modal-dialog-centered" onClick={(event) => event.stopPropagation()}>
          <div className="modal-content">
            <div className="modal-header">
              <h1 className="modal-title fs-5" id="imagePreviewModalTitle">
                {image.title}
              </h1>
              <button type="button" className="btn-close" aria-label="Закрыть" onClick={onClose}/>
            </div>
            <div className="modal-body text-center">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={image.url} alt={image.title} className="img-fluid rounded"/>
            </div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop fade show"/>
    </>
  );
}

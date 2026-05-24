declare module 'bootstrap/js/dist/carousel' {
  export default class Carousel {
    constructor(element: Element, options?: Record<string, unknown>);
    static getInstance(element: Element): Carousel | null;
    static getOrCreateInstance(element: Element, options?: Record<string, unknown>): Carousel;
    cycle(): void;
    dispose(): void;
    next(): void;
    prev(): void;
    to(index: number): void;
  }
}

declare module 'bootstrap/dist/js/bootstrap.bundle.min.js' {
  const bootstrapBundle: unknown;
  export default bootstrapBundle;
}

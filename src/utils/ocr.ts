import { createWorker } from 'tesseract.js';

export async function recognizeTextFromImage(imageSource: string | File | Blob): Promise<string> {
  try {
    const worker = await createWorker('eng');
    const ret = await worker.recognize(imageSource);
    await worker.terminate();
    return ret.data.text.trim();
  } catch (err) {
    console.error('OCR recognition error:', err);
    throw err;
  }
}

export function fileToBase64(file: File): Promise<{ base64: string; mimeType: string }> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      resolve({
        base64: result,
        mimeType: file.type || 'image/jpeg',
      });
    };
    reader.onerror = (error) => reject(error);
    reader.readAsDataURL(file);
  });
}

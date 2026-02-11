import { STYLE_TAG_OPTIONS, type StyleTagValue } from '@/constants/styleTags';

type TranslateFn = (key: string) => string;

const STYLE_TAG_LABEL_KEY_MAP: Record<StyleTagValue, string> = STYLE_TAG_OPTIONS.reduce(
  (accumulator, option) => {
    accumulator[option.value] = option.labelKey;
    return accumulator;
  },
  {} as Record<StyleTagValue, string>
);

export function splitStyleTags(raw: string): string[] {
  if (!raw) {
    return [];
  }
  return raw
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

export function toLocalizedStyleTag(tag: string, t: TranslateFn): string {
  const normalized = tag.trim();
  if (!normalized) {
    return '';
  }
  const labelKey = STYLE_TAG_LABEL_KEY_MAP[normalized as StyleTagValue];
  return labelKey ? t(labelKey) : normalized;
}

export function formatLocalizedStyleTags(raw: string, t: TranslateFn, separator = ', '): string {
  return splitStyleTags(raw)
    .map((tag) => toLocalizedStyleTag(tag, t))
    .filter((tag) => tag.length > 0)
    .join(separator);
}
